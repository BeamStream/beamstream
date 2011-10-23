package com.beamstream
package api

import lib.{App, FacebookGraphApi}
import locs.Sitemap
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
      val redirectUrl: String =
        (for {
          code <- S.param("code") ?~ "Code not provided"
          accessToken <- FacebookGraphApi.accessToken(code)
          json <- FacebookGraphApi.me(accessToken)
          facebookId <- extractId(json)
        } yield {
          logger.debug("json: "+pretty(render(json)))
          // set the access token session var
          FacebookGraphApi.currentAccessToken(Full(accessToken))

          User.findByFacebookId(facebookId) match {
            case Full(user) => validateUser(user) // already connected
            case _ =>
              User.fromFacebookJson(json).map { facebookUser =>
                User.findByEmail(facebookUser.email.is) match {
                  case Full(user) => // needs merging
                    user.facebookId(facebookUser.facebookId.is)
                    user.verified(true)
                    if (user.location.is.length == 0)
                      user.location(facebookUser.location.is)
                    if (user.name.is.length == 0)
                      user.name(facebookUser.name.is)
                    if (user.gender.is.length == 0)
                      user.gender(facebookUser.gender.is)
                    user.save
                    validateUser(user)
                  case _ => // new user
                    if (App.isPreBeta) { // create a record for them
                      facebookUser.save
                      User.logUserIn(facebookUser, true)
                      Sitemap.home.url
                    }
                    else { // send to register page with form pre-filled
                      User.regUser(facebookUser)
                      Sitemap.register.url
                    }
                }
              } openOr handleError("Error creating user from facebook json")
          }
        }) match {
          case Full(url) => url
          case Failure(msg, _, _) => handleError(msg)
          case Empty => handleError("Unknown error")
        }
      RedirectResponse(redirectUrl)
  })

  private def extractId(jv: JValue): Box[Int] = Helpers.tryo {
    val JString(fbid) = jv \ "id"
    Helpers.toInt(fbid)
  }

  private def handleError(msg: String): String = {
    logger.error(msg)
    S.error(msg)
    Sitemap.error.url
  }

  private def validateUser(user: User): String = user.validate match {
    case Nil => User.logUserIn(user, true); Sitemap.home.url
    case errs if (App.isPreBeta) => User.logUserIn(user, true); Sitemap.home.url
    case errs => User.regUser(user); Sitemap.register.url
  }
}