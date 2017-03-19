package net.mikolak.pomisos.prefs.task

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.ActorMaterializer
import com.softwaremill.tagging.@@
import net.mikolak.pomisos
import net.mikolak.pomisos.data._
import net.mikolak.pomisos.prefs.ColumnType.ColumnType
import net.mikolak.pomisos.prefs._

import scala.concurrent.duration._
import language.postfixOps
import scala.concurrent.Await
import scalafx.application.Platform
import scalafx.collections.ObservableBuffer
import net.mikolak.pomisos.utils.Implicits._

class TrelloNetworkService(dao: PreferenceDao,
                           pomodoroDao: PomodoroDao,
                           syncProps: (TrelloNetworkService) => Props @@ TrelloSyncActor)(implicit system: ActorSystem) {

  import TrelloNetworkService._
  import HttpMethods._

  implicit val materializer = ActorMaterializer()

  import system.dispatcher
  system.actorOf(syncProps(this))

  import de.heikoseeberger.akkahttpupickle.UpickleSupport._

  def boards = apiQuery[List[Board]](_ => Some("/members/me/boards"), GET).toList.flatten

  private def apiQuery[T: UnmarshallerFor](apiEndPoint: TrelloPreferences => Option[String],
                                           method: HttpMethod,
                                           params: (String, String)*) =
    for {
      pref     <- prefs
      token    <- pref.authToken
      endpoint <- apiEndPoint(pref)
    } yield {
      Await.result(
        Http()
          .singleRequest {
            val uri: Uri      = s"$baseUrl$endpoint"
            val authQueryPart = List(("key", AppKey), ("token", token))

            val queryBuilder = Uri.Query.newBuilder
            queryBuilder ++= params
            queryBuilder ++= authQueryPart
            //TODO: log sanitized request
            val request = HttpRequest(uri = uri.withQuery(queryBuilder.result()), method = method)
            request
          }
          .flatMap(r => Unmarshal(r.entity).to[T]),
        30 seconds
      )

    }

  lazy val observableList    = ObservableBuffer[Pomodoro](pomodoroDao.getAll())
  lazy val observableColumns = ObservableBuffer[CardList]()
  lazy val observableBoards  = ObservableBuffer[Board]()

  def listPomodorosFromApi =
    prefs.toList.flatMap { pref =>
      val columns = pref.columns

      val lists = tasksForList(columns.values.toSeq: _*)
      columns.toList
        .flatMap {
          case (columnType, listId) =>
            lists
              .get(listId)
              .toList
              .flatMap(_.map(card => Pomodoro(None, card.name, columnType == ColumnType.Done, Some(card.id))))
        }
    }

  private def tasksForList(idList: IdOf[CardList]*): Map[IdOf[CardList], List[Card]] = {
    val listSet = idList.toSet

    apiQuery[List[Card]](_.board.map(boardId => s"/boards/$boardId/cards"), GET)
      .map(_.groupBy(_.idList).filterKeys(listSet.contains))
      .getOrElse(Map.empty)
  }

  def lists =
    apiQuery[List[CardList]](_.board.map(boardId => s"/boards/$boardId/lists"), GET).toList.flatten

  private def prefs = dao.get().trello

  def syncReady =
    prefs
      .exists { curPrefs =>
        curPrefs.authToken.nonEmpty && curPrefs.board.nonEmpty && curPrefs.columns.values.nonEmpty
      }

  def moveTask(task: Pomodoro, targetType: ColumnType) =
    for {
      prefs        <- prefs
      targetListId <- prefs.columns.get(targetType)
    } yield task.cardId.map(taskId => apiQuery[String](_ => Some(s"/cards/$taskId/$targetListId"), PUT))

  def addTask(task: Pomodoro): Option[Pomodoro] =
    for {
      pref   <- prefs
      listId <- pref.columns.get(StartColumn)
      card   <- apiQuery[Card](_ => Some(s"/cards"), POST, "name" -> task.name, "idList" -> listId)
    } yield {
      card.toPomodoro(Some(task))
    }

  def authorizeUrl =
    s"$baseUrl/authorize?response_type=token&scope=read,write&expiration=never&name=$AppName&key=$AppKey"

  def baseUrl = "https://trello.com/1"

}

import scala.concurrent.duration._
import language.postfixOps

class TrelloSyncActor(service: TrelloNetworkService) extends Actor with ActorLogging {

  import context.system
  import context.dispatcher

  var syncSub: Option[Cancellable] = None
  import TrelloNetworkService.mergeBuffers

  override def preStart(): Unit =
    syncSub = Some(system.scheduler.schedule(0 seconds, 15 seconds, self, MasterSync))

  override def postStop(): Unit = syncSub.foreach(_.cancel())

  override def receive: Receive = {
    case MasterSync =>
      log.debug("Syncing with Trello.")
      Platform.runLater {
        mergeBuffers(service.observableBoards, service.boards, implicitly[GenericIdable[Board]].idOf)
        mergeBuffers(service.observableColumns, service.lists, implicitly[GenericIdable[CardList]].idOf)
        mergeBuffers[Pomodoro, DbId](service.observableList, service.listPomodorosFromApi, _.id, Some(service.addTask _))
      }

      context.system.eventStream.publish(SyncNotify)
  }
}

object TrelloNetworkService {

  val AppKey = "b5e64f28291bbbc26be894df57a2bbf5"

  val AppName = "pomisos"

  val StartColumn = ColumnType.ToDo

  type UnmarshallerFor[T] = Unmarshaller[ResponseEntity, T]

  import upickle.default.Reader

  import shapeless.tag
  import shapeless.tag.@@

  implicit def tagReader[MainType, TagType](implicit baseReader: Reader[MainType]): Reader[MainType @@ TagType] =
    Reader[MainType @@ TagType] {
      case js if baseReader.read.isDefinedAt(js) => tag[TagType](baseReader.read(js))
    }

  implicit def idReader[TagType]: Reader[IdOf[TagType]] = tagReader[String, TagType]

  def mergeBuffers[Entity: GenericIdable, InternalId](internal: ObservableBuffer[Entity],
                                                      external: Seq[Entity],
                                                      internalIdGet: Entity => Option[InternalId],
                                                      uploadService: Option[Entity => Option[Entity]] = None): Unit = {

    val idOf = implicitly[GenericIdable[Entity]].idOf _

    val internalSet = internal.toSet
    val externalSet = external.toSet

    def toExternalIdSet(s: Set[Entity]) = s.map(idOf).flatMap(_.toList)

    val downloadedIds   = toExternalIdSet(internalSet)
    val itemsToDownload = externalSet.filterNot(e => idOf(e).exists(downloadedIds.contains))

    val idsOnExternal        = toExternalIdSet(externalSet)
    val itemsToDeleteLocally = internalSet.filterNot(e => idOf(e).forall(idsOnExternal.contains))

    for {
      upload       <- uploadService
      itemToUpload <- internalSet.filter(e => idOf(e).isEmpty)
      uploaded     <- upload(itemToUpload)
    } {
      internal.update(internal.indexOf(itemToUpload), uploaded)
    }

    //TODO: manage ordering
    internal --= itemsToDeleteLocally
    internal ++= itemsToDownload

  }
}
