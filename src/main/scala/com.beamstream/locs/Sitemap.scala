package com.beamstream
package locs

import model.AuthToken

import net.liftweb._
import sitemap._
import sitemap.Loc._

import shiro.Shiro
import shiro.sitemap.Locs._

object Sitemap {
  implicit def locPathToShiroPath(in: List[LocPath]): Shiro.Path = in.map(_.pathItem)

  // groups
  val TopbarGroup = LocGroup("topbar")

  // locations (menu entries)
  val homeLoc = Menu.i("Home") / "home" >> TopbarGroup

  def homePath: Shiro.Path = homeLoc.path
  def homeUrl = listToPath(homePath)

  def authLoc = Menu.i("Auth") / "auth" >> RequireNoAuthentication >>
    EarlyResponse(() => AuthToken.handleToken)

  def logoutLoc = logoutMenu // from shiro.sitemap.Locs

  private def menus = List(
    Menu.i("Beta") / "index",
    homeLoc,
    Menu.i("About") / "about" >> TopbarGroup,
    Menu.i("Contact") / "contact" >> TopbarGroup,
    Menu.i("Login") / "login" >> RequireNoAuthentication,
    Menu.i("Register") / "register" >> RequireNoAuthentication,
    authLoc,
    logoutLoc
  )

  /*
   * Return a SiteMap needed for Lift
   */
  def siteMap: SiteMap = SiteMap(menus:_*)
}