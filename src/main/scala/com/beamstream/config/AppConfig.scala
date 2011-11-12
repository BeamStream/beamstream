package com.beamstream
package config

import net.liftweb._
import util.Props

object AppConfig {
  lazy val mode = Props.get("app.mode").openOr("prebeta") match {
    case "prebeta" => AppModes.PreBeta
    case _ => AppModes.Full
  }
  def isPreBeta = if (mode equals AppModes.PreBeta) true else false // email sign up only.
}

object AppModes extends Enumeration {
  val PreBeta = Value(1, "PreBeta")
  val Full = Value(2, "Full")
}
