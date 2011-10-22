package com.beamstream
package api

import lib.FacebookGraphApi
import model.User

import net.liftweb._
import common._
import http._
import http.rest.RestHelper
import json._
import util.Helpers

object FacebookApiStateful extends RestHelper with Loggable {
  serve("api" / "facebook" prefix {
    case "auth" :: _ Get _ =>
      (for {
        code <- S.param("code") ?~ "Code not provided"
        accessToken <- FacebookGraphApi.accessToken(code)
        json <- FacebookGraphApi.me(accessToken)
        facebookId <- extractId(json)
      } yield {
        logger.debug("json: "+pretty(render(json)))

        User.findByFacebookId(facebookId) match {
          case Full(user) => // already connected, log them in?
          case _ => // new user, create a record for them.
            User.fromFacebookJson(json) foreach { u =>
              val errs = u.email.validate
              if (errs.length == 0)
                u.save
              logger.debug("errs: "+errs.toString)
            }
        }
        accessToken
      }) match {
        case Full(at) => FacebookGraphApi.currentAccessToken(Full(at))
        case Failure(msg, _, _) => handleError(msg)
        case Empty => handleError("Unknown error")
      }
      RedirectResponse("/")
  })

  private def extractId(jv: JValue): Box[Int] = Helpers.tryo {
    val JString(fbid) = jv \ "id"
    Helpers.toInt(fbid)
  }

  private def handleError(msg: String): Unit = {
    logger.error(msg)
    S.error(msg)
  }
}