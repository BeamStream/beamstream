package com.beamstream
package locs

import net.liftweb._
import sitemap._
import sitemap.Loc._

import com.eltimn.auth.mongo._
import Locs._

object Sitemap {
  // groups
  val TopbarGroup = LocGroup("topbar")

  // locations (menu entries)
  val homeLoc = MenuLoc(Menu.i("Home") / "home" >> TopbarGroup)
  val authLoc = buildAuthLoc
  val logoutLoc = buildLogoutLoc

  private def menus = List(
    Menu.i("Beta") / "index",
    homeLoc.menu,
    Menu.i("About") / "about" >> TopbarGroup,
    Menu.i("Contact") / "contact" >> TopbarGroup,
    Menu.i("Login") / "login" >> RequireNoAuthentication,
    Menu.i("Register") / "register" >> RequireNoAuthentication,
    authLoc.menu,
    logoutLoc.menu
  )

  /*
   * Return a SiteMap needed for Lift
   */
  def siteMap: SiteMap = SiteMap(menus:_*)
}
