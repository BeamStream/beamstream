package bootstrap.liftweb

import net.liftweb._
import common._
import http._
import sitemap._
import sitemap.Loc._
import util._
import util.Helpers._

import com.beamstream.model.MongoConfig

/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot extends Loggable {
  def boot {
    logger.info("Run Mode: "+Props.mode.toString)

    // init mongodb
    MongoConfig.init()

    // where to search snippet
    LiftRules.addToPackages("com.beamstream")
    
    // set the default htmlProperties
    LiftRules.htmlProperties.default.set((r: Req) => new Html5Properties(r.userAgent))

    // Build SiteMap
    val TopbarGroup = LocGroup("topbar")
    LiftRules.setSiteMap(SiteMap(List(
      Menu.i("Home") / "index",
      Menu.i("About") / "about" >> TopbarGroup,
      Menu.i("Contact") / "contact" >> TopbarGroup
    ) :_*))

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
