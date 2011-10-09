package com.beamstream
package snippet

import lib.Gravatar
import locs.Sitemap
import model._

import scala.xml._

import org.apache.shiro.authc.UsernamePasswordToken

import net.liftweb._
import common._
import http.S
import util.Helpers._

/*
 * Use for creating a new user.
 */
object RegisterScreen extends BootstrapScreen with shiro.SubjectLifeCycle {

  object userVar extends ScreenVar(User.createUserFromToken)

  //override def screenTop = Full(<h2 class="alt">Create New User Account</h2>)

  addFields(() => userVar.is.registerScreenFields)

  val remember = builder("", User.loginToken.is.isRememberMe)
    .help(Text("Remember me when I come back later."))
    .make

  override def localSetup {
    Referer(redirectPath)
  }

  def finish() {
    val user = userVar.is
    val loginToken = new UsernamePasswordToken(user.email.is, user.password.is, remember)
    user.password.hashIt
    user.save
    S.notice("Thanks for signing up!")
    login(loginToken)
  }
}

/*
 * Use for editing the currently logged in user only.

object UserScreen extends LiftScreen {
  object userVar extends ScreenVar(User.currentUser.open_!)

  override def screenTop = Full(<h2 class="alt">Edit User Account</h2>)

  addFields(() => userVar.is.gravatarContainer)
  addFields(() => userVar.is)

  def finish() {
    println("finish called")
    S.notice("Account settings saved")
    userVar.is.save
  }
}

object PasswordScreen extends LiftScreen {
  object userVar extends ScreenVar(User.currentUser.open_!)

  override def screenTop = Full(<h2 class="alt">Change Password</h2>)

  addFields(() => userVar.is.passwordScreenFields)

  def finish() {
    S.notice("New password saved")
    userVar.is.save
  }
}
 */
/*
 * "gravatar" -> Gravatar(user.email.value),
 * "gravatar_link" -> Gravatar.signupLink(user.email.value, "gravatar.com"),
 */
