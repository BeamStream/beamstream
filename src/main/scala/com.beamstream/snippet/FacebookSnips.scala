package com.beamstream
package snippet

import scala.xml.{NodeSeq, Text}

import net.liftweb._
import common._
import http.{Factory, NoticeType, S, SHtml}
import util.Props
import util.Helpers._

object Facebook extends Loggable {
  val key = Props.get("facebook.key", "")
  val secret = Props.get("facebook.secret", "")

  val authUrl = "http://www.facebook.com/dialog/oauth/?scope=email&client_id=%s&redirect_uri=%s"
    .format(key, urlEncode("http://pad.eltimn.com/api/facebook/auth"))

  //def link = SHtml.link(authUrl, handleLogin, Text("FB Auth"))
  def link = <a href={authUrl}>Login With Facebook</a>

}