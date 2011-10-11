package bootstrap.liftweb

import scala.xml._

import net.liftweb._
import common._
import http._
import util._
import util.Helpers._

import com.beamstream.lib.SmtpMailer
import com.beamstream.locs.Sitemap
import com.beamstream.model.{MongoConfig, User}

import com.eltimn.auth.mongo._

/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot extends Loggable {
  def boot {
    logger.info("Run Mode: "+Props.mode.toString)

    // init mongodb
    MongoConfig.init()

    // init auth-mongo
    AuthRules.authUserMeta.default.set(User)
    AuthRules.indexURL.default.set(Sitemap.homeLoc.path)
    AuthRules.siteName.default.set("BeamStream")
    AuthRules.systemEmail.default.set("info@beamstream.com")
    AuthRules.systemUsername.default.set("BeamStream Staff")


    // config an email sender
    SmtpMailer.init

    // where to search snippet
    LiftRules.addToPackages("com.beamstream")

    // set the default htmlProperties
    LiftRules.htmlProperties.default.set((r: Req) => new Html5Properties(r.userAgent))

    /*
    LiftScreenRules.messageStyles.default.set(() => {
      case NoticeType.Notice => new UnprefixedAttribute("class", "lift_notice", Null)
      case NoticeType.Warning => new UnprefixedAttribute("class", "lift_warning", Null)
      case NoticeType.Error => new UnprefixedAttribute("class", "error", Null)
    }: PartialFunction[NoticeType.Value, MetaData])
    */

    // Build SiteMap
    LiftRules.setSiteMap(Sitemap.siteMap)

    //Show the spinny image when an Ajax call starts
    LiftRules.ajaxStart =
      Full(() => LiftRules.jsArtifacts.show("ajax-loader").cmd)

    // Make the spinny image go away when it ends
    LiftRules.ajaxEnd =
      Full(() => LiftRules.jsArtifacts.hide("ajax-loader").cmd)

    // Force the request to be UTF-8
    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))
  }
}
