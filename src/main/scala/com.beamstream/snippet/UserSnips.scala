package com.beamstream
package snippet

import lib.Gravatar
import locs.Sitemap
import model.User

import scala.xml.NodeSeq

import org.apache.shiro.authc.UsernamePasswordToken

import net.liftweb._
import common._
import http.{S, SHtml, StatefulSnippet}
import util._
import Helpers._

object UserTopbar {
  def render = {
    User.currentUser match {
      case Full(user) =>
        //<img src={Gravatar.imageUrl(user.email.is, 13)}></img>
        //<span class="username">{user.username.is}</span>
        <ul class="nav secondary-nav">
          <li class="dropdown" data-dropdown="dropdown">
            <a href="#" class="dropdown-toggle">
              <!--<img src={Gravatar.imageUrl(user.email.is, 13)}></img>-->
              <span class="username">{user.username.is}</span>
            </a>
            <ul class="dropdown-menu">
              <li><span lift="Menu.item?name=Settings;donthide=true;linktoself=true">Settings</span></li>
              <li><span lift="Menu.item?name=About;donthide=true;linktoself=true">Help</span></li>
              <li class="divider"></li>
              <li><a href="/logout">Log Out</a></li>
            </ul>
          </li>
        </ul>
      case _ if (S.request.flatMap(_.location).map(_.name).filterNot(it => List("Login", "Register").contains(it)).isDefined) =>
        <form action="/login" style="float: right">
          <button class="btn">Sign In</button>
        </form>
      case _ => NodeSeq.Empty
    }
  }
}

class UserLogin extends StatefulSnippet with shiro.SubjectLifeCycle with Loggable {
  def dispatch = { case "render" => render }

  // form vars
  private var password = ""
  private var hasPassword = false
  private var remember = User.loginToken.is.isRememberMe

  val radios = SHtml.radioElem[Boolean](
    Seq(false, true),
    Full(hasPassword)
  )(it => it.foreach(hasPassword = _))

  def render = {
    "#id_email [value]" #> User.loginToken.is.getUsername &
    "#id_password" #> SHtml.password(password, password = _) &
    "#no_password" #> radios(0) &
    "#yes_password" #> radios(1) &
    "name=remember" #> SHtml.checkbox(remember, remember = _) &
    "#id_submit" #> SHtml.onSubmitUnit(process) &
    "#id_cancel" #> SHtml.onSubmitUnit(cancel)
  }

  private def process(): Unit = S.param("email").map(e => {
    val email = e.toLowerCase.trim
    // save the password and remember entered
    val loginToken = new UsernamePasswordToken(email, "", remember)
    User.loginToken(loginToken) // set session var in case we need it

    if (hasPassword && email.length > 0 && password.length > 0) {
      loginToken.setPassword(password.toArray)
      logger.debug("loginToken: %s".format(loginToken.toString))
      loginAndDo(loginToken) {
        User.loginToken.remove()
        seeOther
      }
    }
    else if (hasPassword && email.length <= 0 && password.length > 0)
      S.error("Please enter an email.")
    else if (hasPassword && password.length <= 0 && email.length > 0)
      S.error("Please enter a password.")
    else if (email.length > 0) {
      // see if email exists in the database
      User.findByEmail(email.toLowerCase) match {
        case Full(user) => {
          User.sendAuthLink(user)
          User.loginToken.remove()
          S.notice("An email has been sent to you with instructions for accessing your account.")
          S.seeOther(Sitemap.homeUrl)
        }
        case _ => S.seeOther("/register") // send to register page
      }
    }
    else
      S.error("Please enter an email address")
  }) openOr S.error("Please enter an email address")

  private def cancel() = S.seeOther(Sitemap.homeUrl)
}
