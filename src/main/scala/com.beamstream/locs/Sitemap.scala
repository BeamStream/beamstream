package com.beamstream
package locs

import lib.App
import model.User

import net.liftweb._
import common._
import http.S
import sitemap._
import sitemap.Loc._

import com.eltimn.auth.mongo.{Locs, Path}
import Locs._

import omniauth.Omniauth

object MenuGroups {
  val SettingsGroup = LocGroup("settings")
  val TopBarGroup = LocGroup("topbar")
}

/*
 * Wrapper for Menu locations
 */
case class MenuLoc(menu: Menu) {
  lazy val path: Path = Path(menu.loc.link.uriList)
  lazy val url: String = path.toString
  lazy val fullUrl: String = S.hostAndPath+url
}

object Sitemap {
  import MenuGroups._

  private val preBetaHome = MenuLoc(Menu.i("Home") / "index")


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
  val register = MenuLoc(Menu.i("Register") / "register" >> RequireNotLoggedIn)
  val error = MenuLoc(Menu.i("Error") / "error" >> Hidden)

  private def commonMenus = List(
    home.menu,
    Menu.i("Throw") / "throw" >> Hidden,
    error.menu,
    Menu.i("404") / "404" >> Hidden
  )

  private def mainMenus = List(
    Menu.i("Login") / "login" >> RequireNotLoggedIn,
    register.menu,
    loginToken.menu,
    logout.menu,
    profile.menu,
    account.menu,
    password.menu,
    editProfile.menu,
    Menu.i("About") / "about" >> TopBarGroup,
    Menu.i("Contact") / "contact" >> TopBarGroup
  )

  /*
   * Return a SiteMap needed for Lift
   */
  def siteMap: SiteMap =
    if (App.isPreBeta) SiteMap(commonMenus:_*)
    else SiteMap(commonMenus ::: mainMenus :_*)
}
