package com.beamstream
package snippet

import model.BetaUser

import net.liftweb._
import http._
import http.js._
import JsCmds._
import JE._
import common._
import util.Helpers._
import scala.xml.NodeSeq

object BetaForm {
  def render = {
    var email = "E-mail"
    var role = "empty"

    val roleList = ("empty", "I'm a") :: BetaUser.roles.map(r => (r, r))

    def process(): JsCmd = {
      val user = BetaUser.createRecord
        .email(email)
        .userRole(role)

      user.validate match {
        case Nil =>
          user.save
          S.notice("Thanks for signing up!")
        case errs => S.error(errs)
      }

      Noop
    }

    "name=email" #> (SHtml.text(email, email = _)) &
    "name=role" #> SHtml.selectObj[String](roleList, Full(role), role = _,
            "class" -> "sel80", "id" -> "country") &
    "name=hidden" #> SHtml.hidden(process)

  }
}
