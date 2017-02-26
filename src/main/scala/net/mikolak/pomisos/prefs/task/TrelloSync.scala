package net.mikolak.pomisos.prefs.task

sealed trait TrelloSync

case object MasterSync extends TrelloSync

case object SyncNotify extends TrelloSync
