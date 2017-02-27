package net.mikolak.pomisos.prefs

import akka.actor.{Actor, ActorSystem, Props}
import com.sun.org.apache.xalan.internal.xsltc.trax.DOM2SAX
import net.mikolak.pomisos.prefs.task.{Board, CardList, SyncNotify, TrelloNetworkService}

import scalafx.beans.property.{ObjectProperty, ReadOnlyObjectProperty}
import scalafx.event.ActionEvent
import scalafx.scene.control._
import scalafx.scene.layout.GridPane
import scalafxml.core.macros.sfxml
import net.mikolak.pomisos.utils.Implicits._
import org.w3c.dom.html.HTMLDocument

import scala.xml.Node
import scala.xml.parsing.NoBindingFactoryAdapter
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.scene.web.WebView
import scalafx.util.StringConverter
import shapeless._

trait TrelloPrefs {

  def prefs: Option[TrelloPreferences]

}

@sfxml
class TrelloPrefsController(dao: PreferenceDao,
                            prefPane: GridPane,
                            trelloService: TrelloNetworkService,
                            actorSystem: ActorSystem,
                            taskBoard: ComboBox[Board],
                            todoColumn: ComboBox[CardList],
                            doingColumn: ComboBox[CardList],
                            doneColumn: ComboBox[CardList],
                            syncButton: Button)
    extends TrelloPrefs {

  import TrelloPrefsControllerUtils._

  private lazy val columnControls = {
    import ColumnType._
    Map(ToDo -> todoColumn, Doing -> doingColumn, Done -> doneColumn)
  }

  private def dbPrefs = dao.get().trello

  val authToken = ObjectProperty(dbPrefs.flatMap(_.authToken))

  taskBoard.converter = StringConverter.toStringConverter(_.name)
  taskBoard.getSelectionModel
    .selectedItemProperty()
    .onChange((_, _, newVal) => {
      import com.softwaremill.quicklens._
      val newBoardId = Option(newVal).map(_.id)
      dao.saveWith(_.modify(_.trello.each.board).setTo(newBoardId))
      actorSystem.eventStream.publish(SyncNotify)
    })

  for ((columnType, columnBox) <- columnControls) {
    columnBox.converter = StringConverter.toStringConverter(_.name)
    columnBox.getSelectionModel
      .selectedItemProperty()
      .onChange((_, _, newVal) => {
        import com.softwaremill.quicklens._
        val newColumn = Option(newVal).map(_.id)
        dao.saveWith(
          _.modify(_.trello.each.columns).using(m => newColumn.map(c => m + (columnType -> c)).getOrElse(m - columnType)))
      })
  }

  def doSync(): Unit = {

    taskBoard.items = trelloService.observableBoards
    for {
      pref    <- dbPrefs
      boardId <- pref.board
      board   <- trelloService.boards.find(_.id == boardId)
    } {
      taskBoard.getSelectionModel.select(board)
    }

    for (columnBox <- columnControls.values) {
      columnBox.items = trelloService.observableColumns
    }

    for {
      pref                    <- dbPrefs
      (columnType, columnBox) <- columnControls
    } {
      //not in for preamble to preserve null selection safety
      val columnIdForType = pref.columns.get(columnType)
      val columnForType   = columnIdForType.flatMap(id => trelloService.lists.find(_.id == id))

      columnBox.getSelectionModel.select(columnForType.orNull)
    }
  }

  actorSystem.eventStream.subscribe(
    actorSystem.actorOf(Props(new Actor() {

      override def preStart(): Unit = {
        super.preStart()
        self ! SyncNotify
      }

      override def receive = {
        case SyncNotify => Platform.runLater(doSync())
      }
    })),
    SyncNotify.getClass
  )

  prefPane.visible <== authToken.map(_.nonEmpty).toBoolean
  syncButton.visible <== !prefPane.visible

  override def prefs: Option[TrelloPreferences] =
    authToken.value.map(
      token =>
        TrelloPreferences(
          Some(token),
          Option(taskBoard.getSelectionModel.getSelectedItem).map(_.id),
          columnControls.mapValues(_.getSelectionModel.getSelectedItem).collect {
            case (column, CardList(id, _)) => (column, id)
          }
      ))

  def doSync(event: ActionEvent): Unit = {

    val content = new WebView() {}

    val dialog = new Dialog[Option[String]]() {
      //initOwner(stage)
      title = "Trello Authorization"
    }

    dialog.dialogPane().content = content
    dialog.dialogPane().buttonTypes = Seq(ButtonType.Cancel)

    content.engine.load(trelloService.authorizeUrl)

    new ReadOnlyObjectProperty(content.engine.documentProperty()).onChange {
      val document = content.engine.document.asInstanceOf[HTMLDocument]

      /*
       * Ideally, we'd get the token from the response headers (as intended in the API),
       * however, access to the headers is extremely convoluted in the WebView's case,
       * making direct HTML parsing actually more convenient (and hopefully not more fragile).
       */

      //http://stackoverflow.com/questions/3922151/scala-convert-org-w3c-dom-document-to-scala-xml-nodeseq
      val saxParser    = new DOM2SAX(document)
      val scalaAdapter = new NoBindingFactoryAdapter
      saxParser.setContentHandler(scalaAdapter)
      saxParser.parse()

      authToken.value = Option(scalaAdapter.rootElem).flatMap(extractAuthText)
      if (authToken.value.nonEmpty) {
        dialog.close()
      }
    }

    dialog.show()
  }

  authToken.onChange((_, _, newVal) =>
    newVal match {
      case Some(newToken) =>
        dao.saveWith(_.copy(trello = Some(TrelloPreferences(Some(newToken), None, Map.empty))))
        trelloService.boards
      case None =>
  })

}

//different name required by @sfxml macro
object TrelloPrefsControllerUtils {

  def extractAuthText(document: Node): Option[String] =
    (document \\ "BODY").headOption
      .flatMap(_.child.grouped(3).collectFirst {
        case List(titleNode, _, textNode) if titleNode.text.contains("token") && textNode.label.toLowerCase.contains("pre") =>
          textNode.text.trim()
      })
}
