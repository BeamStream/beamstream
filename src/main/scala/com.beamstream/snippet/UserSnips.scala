package com.beamstream
package snippet

import config.AppConfig
import lib.{AppHelpers, FacebookGraph, Gravatar}
import locs.Sitemap
import model.{User, LoginCredentials}

import scala.xml._

import net.liftweb._
import common._
import http.{DispatchSnippet, S, SHtml, StatefulSnippet}
import http.js.JsCmd
import http.js.JE._
import util._
import Helpers._

object Register {
  def render =
    if (User.regUser.facebookId.is > 0)
      <div lift="facebook_register_screen"></div>
    else
      <div lift="register_screen"></div>
}

sealed trait UserSnippet extends DispatchSnippet with AppHelpers with Loggable {

  def dispatch = {
    case "header" => header
    case "gravatar" => gravatar
    case "name" => name
    case "username" => username
    case "title" => title
  }

  protected def user: Box[User]

  protected def serve(snip: User => NodeSeq): NodeSeq =
    (for {
      u <- user ?~ "User not found"
    } yield {
      snip(u)
    }): NodeSeq

  def header(xhtml: NodeSeq): NodeSeq = serve { user =>
    <div id="user-header">
      {gravatar(xhtml)}
      <h3>{name(xhtml)}</h3>
    </div>
  }

  def gravatar(xhtml: NodeSeq): NodeSeq = {
    val size = S.attr("size").map(toInt) openOr Gravatar.defaultSize.vend

    serve { user =>
      Gravatar.imgTag(user.email.is, size)
    }
  }

  def username(xhtml: NodeSeq): NodeSeq = serve { user =>
    Text(user.username.is)
  }

  def name(xhtml: NodeSeq): NodeSeq = serve { user =>
    if (user.name.length > 0)
      Text("%s (%s)".format(user.name, user.username.is))
    else
      Text(user.username.is)
  }

  def title(xhtml: NodeSeq): NodeSeq = serve { user =>
    <lift:head>
      <title lift="Menu.title">{"BeamStream: %*% - "+user.username.is}</title>
    </lift:head>
  }
}

object CurrentUser extends UserSnippet {
  override protected def user = User.currentUser
}

object ProfileLocUser extends UserSnippet {
  override def dispatch = super.dispatch orElse {
    case "profile" => profile
  }

  override protected def user = Sitemap.profileLoc.currentValue

  import java.text.SimpleDateFormat

  val df = new SimpleDateFormat("MMM d, yyyy")

  def profile(xhtml: NodeSeq): NodeSeq = serve { user =>
    val editLink: NodeSeq =
      if (User.currentUser.filter(_.id.is == user.id.is).isDefined)
        <a href={Sitemap.editProfile.url} class="btn info">Edit Your Profile</a>
      else
        NodeSeq.Empty

    val cssSel =
      "#id_avatar *" #> Gravatar.imgTag(user.email.is) &
      "#id_name *" #> <h3>{user.name}</h3> &
      "#id_location *" #> user.location.is &
      "#id_whencreated" #> df.format(user.whenCreated.toDate).toString &
      "#id_bio *" #> user.bio.is &
      "#id_editlink *" #> editLink

    cssSel.apply(xhtml)
  }
}

class UserLogin extends StatefulSnippet with Loggable {
  def dispatch = { case "render" => render }

  // form vars
  private var password = ""
  private var hasPassword = false
  private var remember = User.loginCredentials.is.isRememberMe

  val radios = SHtml.radioElem[Boolean](
    Seq(false, true),
    Full(hasPassword)
  )(it => it.foreach(hasPassword = _))

  def render = {
    "#id_email [value]" #> User.loginCredentials.is.email &
    "#id_password" #> SHtml.password(password, password = _) &
    "#no_password" #> radios(0) &
    "#yes_password" #> radios(1) &
    "name=remember" #> SHtml.checkbox(remember, remember = _) &
    "#id_submit" #> SHtml.onSubmitUnit(process) &
    "#id_cancel" #> SHtml.onSubmitUnit(cancel)
  }

  private def process(): Unit = S.param("email").map(e => {
    val email = e.toLowerCase.trim
    // save the email and remember entered in the session var
    User.loginCredentials(LoginCredentials(email, remember))

    if (hasPassword && email.length > 0 && password.length > 0) {
      User.findByEmail(email) match {
        case Full(user) if (user.password.isMatch(password)) =>
          User.logUserIn(user, true, remember)
          S.seeOther(User.loginContinueUrl.is)
        case _ => S.error("Invalid credentials.")
      }
    }
    else if (hasPassword && email.length <= 0 && password.length > 0)
      S.error("Please enter an email.")
    else if (hasPassword && password.length <= 0 && email.length > 0)
      S.error("Please enter a password.")
    else if (hasPassword)
      S.error("Please enter an email and password.")
    else if (email.length > 0) {
      // see if email exists in the database
      User.findByEmail(email) match {
        case Full(user) => {
          User.sendLoginToken(user)
          User.loginCredentials.remove()
          S.notice("An email has been sent to you with instructions for accessing your account.")
          S.seeOther(Sitemap.home.url)
        }
        case _ => S.seeOther(Sitemap.register.url)
      }
    }
    else
      S.error("Please enter an email address")
  }) openOr S.error("Please enter an email address")

  private def cancel() = S.seeOther(Sitemap.home.url)
}

object UserNetworks extends AppHelpers with Loggable {
  def render = {
    User.currentUser match {
      case Full(user) if (user.isConnectedToFacebook) =>
        val disconnectHtml =
          //SHtml.ajaxButton("Disconnect", () => disconnect, ("class" -> "btn"))
          SHtml.link(Sitemap.networks.url, () => disconnect, Text("Disconnect"), ("class" -> "btn"), ("onclick" -> """return confirm("Disconnect Facebook?");"""))

        "#id_facebook" #>
          <span>
          Linked to: <a href={"http://www.facebook.com/profile.php?id="+user.facebookId.toString}>{user.name}</a><br />
          </span> &
        "#id_facebookdisconnect" #> disconnectHtml

      case _ =>
        User.loginContinueUrl(Sitemap.networks.url)
        "#id_facebook" #> Facebook.link &
        "#id_facebookdisconnect" #> ""
    }
  }

  private def disconnect: Unit =
    User.disconnectFacebook match {
      case Full(x) =>
        User.currentUser.foreach { user =>
          S.notice("You are no longer connected to Facebook")
          S.redirectTo(Sitemap.networks.url)
        }
      case Failure(msg, _, _) => S.error(msg); //JsRaw("window.location.reload").cmd
      case Empty => S.warning("Empty"); //JsRaw("window.location.reload").cmd
    }
}

object UserTopbar {
  def render = {
    User.currentUser match {
      case Full(user) =>
        <ul class="nav secondary-nav" id="user">
          <li class="dropdown" data-dropdown="dropdown">
            <a href="#" class="dropdown-toggle">
              {Gravatar.imgTag(user.email.is, 20)}
              <span>{user.username.is}</span>
            </a>
            <ul class="dropdown-menu">
              <li><a href={"/user/%s".format(user.username.is)}>Profile</a></li>
              <li><lift:Menu.item name="Account" donthide="true" linktoself="true">Settings</lift:Menu.item></li>
              <li><lift:Menu.item name="About" donthide="true" linktoself="true">Help</lift:Menu.item></li>
              <li class="divider"></li>
              <li><lift:Menu.item name="Logout" donthide="true" linktoself="true">Log Out</lift:Menu.item></li>
            </ul>
          </li>
        </ul>
      case _ if (AppConfig.isPreBeta) => NodeSeq.Empty
      case _ if (S.request.flatMap(_.location).map(_.name).filterNot(it => List("Login", "Register").contains(it)).isDefined) =>
        <form action="/login" style="float: right">
          <button class="btn">Sign In</button>
        </form>
      case _ => NodeSeq.Empty
    }
  }
}

object UserDebug {
  def render =
    if (Props.productionMode)
      NodeSeq.Empty
    else
      <span style="display: none;">
        User.currentUserId: {User.currentUserId.toString}<br />
        User.currentUser: {User.currentUser.toString}<br />
        AccessToken: {FacebookGraph.currentAccessToken.toString}<br />
      </span>
}