package com.beamstream
package snippet

import locs.Sitemap
import model._

import scala.xml._

import net.liftweb._
import common._
import http.{LiftScreen, S}
import util.FieldError
import util.Helpers._

class CreateStreamScreen extends BaseScreen {
  object streamVar extends ScreenVar(Stream.createRecord)

  addFields(() => streamVar.is)

  def finish() {
    User.currentUser.foreach { user =>
      val stream = streamVar.is
      UserStream.join(user.id.is, stream.id.is)
      stream.save
      S.notice("Stream created")
      Referer(Sitemap.streamLoc.calcHref(stream))
    }
  }
}
