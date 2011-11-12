package com.beamstream
package lib

import scala.xml._

import net.liftweb._
import common._
import http.NoticeType
import http.js._
import http.js.JE._
import json._
import util.CssSel
import util.Helpers._

import org.bson.types.ObjectId

trait AppHelpers {
  /*
  * Allows for the following to be used when building snippets and it will handle
  * errors according to handleError:
  *
  *   for {
  *     user <- User.currentUser ?~ "You must be logged in to edit your profile."
  *   } yield ({
  *   ...
  *   }): NodeSeq
  */
  implicit protected def boxNodeSeqToNodeSeq(in: Box[NodeSeq]): NodeSeq = in match {
    case Full(ns) => ns
    case Failure(msg, _, _) => handleNodeSeqError(msg)
    case Empty => handleNodeSeqError("Empty snippet")
  }
  protected def handleNodeSeqError(msg: String): NodeSeq = Comment("ERROR: %s".format(msg))

  /*
  * Allows for the following to be used when building snippets and it will handle
  * errors according to handleError:
  *
  *   for {
  *     user <- User.currentUser ?~ "You must be logged in to edit your profile."
  *   } yield ({
  *   ...
  *   }): CssSel
  */
  implicit protected def boxCssSelToCssSel(in: Box[CssSel]): CssSel = in match {
    case Full(csssel) => csssel
    case Failure(msg, _, _) => handleCssSelError(msg)
    case Empty => handleCssSelError("Empty snippet")
  }
  protected def handleCssSelError(msg: String): CssSel = "*" #> Text("ERROR: %s".format(msg))

  /*
   * For use in for comprehensions
   */
  protected def boolToBox(b: Boolean): Box[Boolean] = if (b) Full(b) else Empty

  /*
   * For RestHelper API classes
   */
  implicit def boxJsonToJsonResponse(in: Box[JValue]): JValue = {
    import JsonDSL._
    in match {
      case Full(jv) => jv
      case Failure(msg, _, _) => ("alert" -> JsonAlert.error(msg).asJValue)
      case Empty => ("alert" -> JsonAlert.warning("Empty response").asJValue)
    }
  }

  /*
   * For Ajax classes
   */
  implicit def boxJsCmdToJsCmd(in: Box[JsCmd]): JsCmd = {
    import JsonDSL._
    in match {
      case Full(jc) => jc
      case Failure(msg, _, _) => JsFunc("AlertModel.addError", JsonAlert.error(msg).asJsExp).cmd
      case Empty => JsFunc("AlertModel.addError", JsonAlert.warning("Empty response").asJsExp).cmd
    }
  }

  object AsObjectId {
    def unapply(in: String): Option[ObjectId] = asObjectId(in)
     private def asObjectId(in: String): Option[ObjectId] =
      if (ObjectId.isValid(in)) Some(new ObjectId(in))
      else None
  }

  def lowerCaseTitle(noticeType: NoticeType.Value): String = noticeType match {
    case NoticeType.Notice => "info"
    case _ => noticeType.lowerCaseTitle
  }
}
