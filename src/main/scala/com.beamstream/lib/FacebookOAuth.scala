package com.beamstream
package lib

import net.liftweb._
import http.Factory
import util.Props

import org.scribe.builder.ServiceBuilder
import org.scribe.builder.api.FacebookApi

object FacebookOAuth extends Factory {
  val key = new FactoryMaker[String](Props.get("facebook.key", "")) {}
  val secret = new FactoryMaker[String](Props.get("facebook.secret", "")) {}
  val baseUrl = new FactoryMaker[String](Props.get("facebook.baseurl", "http://pad.eltimn.com")) {}
  val callbackUrl = new FactoryMaker[String](Props.get("facebook.callbackurl", "/api/facebook/auth")) {}

  def fullCallbackUrl = baseUrl.vend+callbackUrl.vend

  lazy val service = new ServiceBuilder()
    .provider(classOf[FacebookApi])
    .apiKey(key.vend)
    .apiSecret(secret.vend)
    .callback(fullCallbackUrl)
    .build

  def authorizationUrl: String = service.getAuthorizationUrl(null)

  //def accessToken
}