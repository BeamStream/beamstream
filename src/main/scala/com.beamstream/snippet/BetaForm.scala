package com.beamstream
package snippet

import model.User

import net.liftweb._
import http._
import http.js._
import JsCmds._
import JE._
import common._
import util.FieldError
import util.Helpers._
import scala.xml.NodeSeq

import com.eltimn.auth.mongo.Role

object BetaForm {
  def render = {
    var email = "E-mail"
    var role: Box[Role] = Empty

    val roleList: List[(Box[Role], String)] = (Empty, "I'm a") :: User.standardRoles.map(r => (Full(r), r.id.is))

    def process(): JsCmd = {
      val user = User.createRecord
        .email(email)
        .roles(role.toList.map(_.id.is))

      user.validate match {
        case Nil =>
          user.save
          Call("popup", Str("Thanks for signing up!"))
        case errs => Call("popup", Str(errsToList(errs)))
      }
    }

    "name=email" #> (SHtml.text(email, email = _)) &
    "name=role" #> SHtml.selectObj[Box[Role]](roleList, Full(role), role = _, "class" -> "sel80", "id" -> "country") &
    "name=hidden" #> SHtml.hidden(process)
  }

  private def errsToList(errs: List[FieldError]): String =
    //(<span><p>Please fix the following errors:</p><ul>{errs.flatMap(e => <li>{e.msg}</li>)}</ul></span>).toString
    (<ul>{errs.flatMap(e => <li>{e.msg}</li>)}</ul>).toString
}
