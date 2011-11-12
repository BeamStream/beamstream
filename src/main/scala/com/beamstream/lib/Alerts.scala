package com.beamstream
package lib

import net.liftweb._
import common._
import http._
import http.js._
import json._
import util._

object Alerts extends Factory {
  val alertsContainerId = new FactoryMaker[String](LiftRules.noticesContainerId) {}

}

case class JsonAlert(val message: String, val level: AlertType.Value) {
  import JsonDSL._
  def asJValue: JValue = ("message" -> message) ~ ("level" -> level.lowerCaseTitle)
  def asJsExp: JsExp = new JsExp {
    def toJsCmd = compact(render(asJValue))
  }
}
object JsonAlert {
  def info(msg: String): JsonAlert = JsonAlert(msg, AlertType.Info)
  def error(msg: String): JsonAlert = JsonAlert(msg, AlertType.Error)
  def warning(msg: String): JsonAlert = JsonAlert(msg, AlertType.Warning)
  def success(msg: String): JsonAlert = JsonAlert(msg, AlertType.Warning)
}

object AlertType extends Serializable {
  sealed abstract class Value(val title: String, val noticeType: NoticeType.Value) {
    def lowerCaseTitle = title.toLowerCase

    // The element ID to use for notice divs
    def id: String = Alerts.alertsContainerId.vend + "_" + lowerCaseTitle
  }

  object Info extends Value("Info", NoticeType.Notice)
  object Warning extends Value("Warning", NoticeType.Warning)
  object Error extends Value("Error", NoticeType.Error)
  object Success extends Value("Success", NoticeType.Notice)
}
