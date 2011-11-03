package com.beamstream
package locs

import config.AppConfig
import lib.FacebookGraph
import model.User

import net.liftweb._
import common._
import http._
import sitemap._
import sitemap.Loc._
import util.Helpers

import net.liftmodules.mongoauth.Locs

object MenuGroups {
  val SettingsGroup = LocGroup("settings")
  val TopBarGroup = LocGroup("topbar")
}

/*
 * Wrapper for Menu locations
 */
case class MenuLoc(menu: Menu) {
  lazy val url: String = menu.loc.link.uriList.mkString("/","/","")
  lazy val fullUrl: String = S.hostAndPath+url
}

object Sitemap extends Locs {
  import MenuGroups._

  // LocParam Guards
  val ConnectedToFacebook = If(
    () => User.isConnectedToFaceBook,
    () => RedirectResponse(home.url)
  )

  val NotConnectedToFacebook = If(
    () => !User.isConnectedToFaceBook,
    () => RedirectResponse(home.url)
  )

  //override def logoutLocParams = NotConnectedToFacebook :: super.logoutLocParams

  // locations (menu entries)
  val home = MenuLoc(Menu.i("Home") / "index" >> TopBarGroup)
  val loginToken = MenuLoc(buildLoginTokenMenu)
  val logout = MenuLoc(buildLogoutMenu)
  private val profileParamMenu = Menu.param[User]("User", "Profile",
    User.findByUsername _,
    _.username.is
  ) / "user" >> Loc.CalcValue(() => User.currentUser)
  val profile = MenuLoc(profileParamMenu)
  lazy val profileLoc = profileParamMenu.toLoc

  val password = MenuLoc(Menu.i("Password") / "settings" / "password" >> RequireLoggedIn >> SettingsGroup)
  val account = MenuLoc(Menu.i("Account") / "settings" / "account" >> SettingsGroup >> RequireLoggedIn)
  val editProfile = MenuLoc(Menu("EditProfile", "Profile") / "settings" / "profile" >> SettingsGroup >> RequireLoggedIn)
  val networks = MenuLoc(Menu.i("Networks") / "settings" / "networks" >> SettingsGroup >> RequireLoggedIn)
  val register = MenuLoc(Menu.i("Register") / "register" >> RequireNotLoggedIn)
  val error = MenuLoc(Menu.i("Error") / "error" >> Hidden)

  // facebook
  val facebookConnect = MenuLoc(
    Menu.i("FacebookConnect") / "facebook" / "connect" >> EarlyResponse(() => {
      FacebookGraph.csrf(Helpers.nextFuncName)
      Full(RedirectResponse(FacebookGraph.authUrl, S.responseCookies:_*))
      //Full(RedirectWithState(facebookError.url, RedirectState(() => { S.error("this is only a test") })))
    })
  )
  // popup page
  val facebookChannel = MenuLoc(Menu.i("FacebookChannel") / "facebook" / "channel" >> Hidden)
  val facebookClose = MenuLoc(Menu.i("FacebookClose") / "facebook" / "close" >> Hidden)
  val facebookError = MenuLoc(Menu("FacebookError", "Error") / "facebook" / "error" >> Hidden)
  val facebookRegister = MenuLoc(Menu("FacebookRegister", "Register") / "facebook" / "register" >> Hidden)

  private def commonMenus = List(
    home.menu,
    Menu.i("Throw") / "throw" >> Hidden,
    error.menu,
    logout.menu,
    Menu.i("404") / "404" >> Hidden,
    facebookConnect.menu,
    facebookChannel.menu,
    facebookClose.menu,
    facebookError.menu,
    facebookRegister.menu
  )

  private def mainMenus = List(
    Menu.i("Login") / "login" >> RequireNotLoggedIn,
    register.menu,
    loginToken.menu,
    profile.menu,
    account.menu,
    password.menu,
    editProfile.menu,
    networks.menu,
    Menu.i("About") / "about" >> TopBarGroup,
    Menu.i("Contact") / "contact" >> TopBarGroup
  )

  /*
   * Return a SiteMap needed for Lift
   */
  def siteMap: SiteMap =
    if (AppConfig.isPreBeta) SiteMap(commonMenus:_*)
    else SiteMap(commonMenus ::: mainMenus :_*)
}
