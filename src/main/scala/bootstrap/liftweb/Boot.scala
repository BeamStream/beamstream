package bootstrap.liftweb

import scala.xml._

import net.liftweb._
import common._
import http._
import json._
import util._
import util.Helpers._

import com.beamstream._
import config.{ApiConfig, MongoConfig}
import lib.{ErrorHandler, Gravatar, SmtpMailer}
import locs.Sitemap
import model.User

import com.eltimn.auth.mongo._

/**
 * A class that's instantiated early and run. It allows the application
 * to modify lift's environment
 */
class Boot extends Loggable {
  def boot {
    logger.info("Run Mode: "+Props.mode.toString)

    // init mongodb
    MongoConfig.init()

    // init auth-mongo
    AuthRules.authUserMeta.default.set(User)
    AuthRules.indexUrl.default.set(Sitemap.home.path)
    AuthRules.loginTokenAfterUrl.default.set(Sitemap.password.path)
    AuthRules.siteName.default.set("BeamStream")
    AuthRules.systemEmail.default.set("info@beamstream.com")
    AuthRules.systemUsername.default.set("BeamStream Staff")

    // For S.loggedIn_? and TestCond.loggedIn/Out
    LiftRules.loggedInTest = Full(() => User.isLoggedIn)

    // checks for ExtSession cookie
    LiftRules.earlyInStateful.append(User.testForExtSession)

    // Gravatar
    Gravatar.defaultImage.default.set("wavatar")

    // config an email sender
    SmtpMailer.init

    // api
    ApiConfig.init

    // where to search snippet
    LiftRules.addToPackages("com.beamstream")

    // set the default htmlProperties
    LiftRules.htmlProperties.default.set((r: Req) => new Html5Properties(r.userAgent))

    // Build SiteMap
    LiftRules.setSiteMap(Sitemap.siteMap)

    // Error handler
    ErrorHandler.init

    // 404 handler
    LiftRules.uriNotFound.prepend(NamedPF("404handler") {
      case (req, failure) =>
        NotFoundAsTemplate(ParsePath(List("404"), "html", false, false))
    })

    // Show the spinny image when an Ajax call starts
    LiftRules.ajaxStart =
      Full(() => LiftRules.jsArtifacts.show("ajax-loader").cmd)

    // Make the spinny image go away when it ends
    LiftRules.ajaxEnd =
      Full(() => LiftRules.jsArtifacts.hide("ajax-loader").cmd)

    // Force the request to be UTF-8
    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))
  }
}
