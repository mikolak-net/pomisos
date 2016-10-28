package net.mikolak.pomisos.dependencies

import com.softwaremill.tagging.@@


trait AppLifecycleModule {

  def openApp: (() => Unit) @@ AppOpenFunction
  def closeApp: (() => Unit) @@ AppCloseFunction

}


sealed trait AppCloseFunction

sealed trait AppOpenFunction