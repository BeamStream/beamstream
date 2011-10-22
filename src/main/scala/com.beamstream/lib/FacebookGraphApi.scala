package com.beamstream
package lib

import org.joda.time.DateTime

import net.liftweb._
import common._
import json._
import http.{Factory, S, SessionVar}
import util.{Helpers, Props}

import dispatch._
//import dispatch.liftjson.Js._

object FacebookGraphApi extends Factory with Loggable {
  /*
   * Config
   */
  val key = new FactoryMaker[String](Props.get("facebook.key", "")) {}
  val secret = new FactoryMaker[String](Props.get("facebook.secret", "")) {}
  val baseUrl = new FactoryMaker[String](Props.get("facebook.baseurl", "http://localhost:8080")) {}
  val callbackUrlPath = new FactoryMaker[String](Props.get("facebook.callbackurlpath", "/api/facebook/auth")) {}
  //val continueUrl = new FactoryMaker[String](Props.get("facebook.continueurl", "/")) {}

  //val key = Props.get("facebook.key", "")
  //val secret = Props.get("facebook.secret", "")

  /*
   * See: http://dispatch.databinder.net/Choose+an+Executor.html
   */
  private lazy val http = new Http with thread.Safety

  private def baseReq = :/("graph.facebook.com").secure

  //def currentAccessToken: Box[AccessToken] = curAccessToken.is
  object currentAccessToken extends SessionVar[Box[AccessToken]](Empty)

  // where to send the user after
  object continueUrl extends SessionVar[String]("/")

  /*
   * Request an access token from facebook
   */
  def accessToken(code: String): Box[AccessToken] = {
    val req = baseReq / "oauth/access_token" <<? Map(
      "client_id" -> key.vend,
      "client_secret" -> secret.vend,
      "redirect_uri" -> (baseUrl.vend+callbackUrlPath.vend),
      "code" -> code
    )

    http x (req as_str) {
      case (400, _, _, out) =>
        val err = FacebookError.fromString(out())
        Failure(err.message)
      case (200, _, _, out) =>
        val map = Map.empty ++ out().split("&").map { param =>
          val pair = param.split("=")
          (pair(0), pair(1))
        }

        (map.get("access_token"), map.get("expires")) match {
          case (Some(at), Some(exp)) => Helpers.asInt(exp)
            .map(e => AccessToken(at, code, (new DateTime).plusSeconds(e)))
          case _ => Failure("Unable to parse access_token: "+map.toString)
        }
      case (status, _, _, out) =>
        Failure("Unexpected status code: %s - %s".format(status, out()))
    }
  }

  /*
  private def paramStringToJson(in: String): JValue = {
    Map.empty ++ str.split("&").map { param =>
      val pair = param.split("=")
      (pair(0), pair(1))
    }
  }
  */

  private[lib] def doReq(req: Request): Box[JValue] =
    http x (req as_str) {
      case (400, _, _, out) =>
        val err = FacebookError.fromString(out())
        Failure(err.message)
      case (200, _, _, out) =>
        Full(JsonParser.parse(out()))
      case (status, b, c, out) =>
        //logger.debug("b: "+b.toString)
        //logger.debug("c: "+c.toString)
        Failure("Unexpected status code: %s - %s".format(status, out()))
    }

  private def doOauthReq(req: Request, token: AccessToken): Box[JValue] = {
    val params = Map("access_token" -> token.value)
    doReq(req <<? params)
  }

  def me(token: AccessToken): Box[JValue] = doOauthReq(baseReq / "me", token)

  def me(token: AccessToken, obj: String): Box[JValue] =
    doOauthReq(baseReq / "me" / obj, token)

  private def doWithToken[T](f: AccessToken => Box[T]): Box[T] = {
    currentAccessToken.is.flatMap { at =>
      if (at.isExpired) // refresh the token
        accessToken(at.code).flatMap(t => f(t))
      else
        f(at)
    }
  }
  /*
  def meWithToken: Box[JValue] = doWithToken {
    token => me(token)
  }

  def meWithToken(obj: String): Box[JValue] = doWithToken {
    token => me(token, obj)
  }

  def obj(id: String): Box[JValue] = doReq(baseReq / id)
  */
}

case class AccessToken(val value: String, code: String, val expires: DateTime) {
  def isExpired: Boolean = expires.isAfter(new DateTime)
}

case class FacebookError(val message: String, val errType: String)
object FacebookError {
  implicit val formats = DefaultFormats
  def fromString(in: String): FacebookError = {
    (JsonParser.parse(in) \\ "error" transform {
       case JField("type", JString(s)) => JField("errType", JString(s))
     }).extract[FacebookError]
  }
}