package com.beamstream
package snippet

import comet.{SendAMessage, StreamManager}
import lib.{AppHelpers, JsonAlert}
import locs.Sitemap
import model._

import scala.xml._

import net.liftweb._
import common._
import http._
import http.js._
import http.js.JsCmds._
import http.js.JE._
import util._
import Helpers._

object MyStreams {
  def render = {
    User.currentUser map { user =>
      user.streams.flatMap { s =>
        <li><a href={"/stream/"+s.id.toString}>{s.name.is}</a></li>
      }
    } openOr <li>No streams</li>
  }
}

object JoinStream extends Loggable {
  def render(in: NodeSeq): NodeSeq = {
    for {
      r <- S.request if r.post_? // make sure it's a post
      id <- S.param("stream_id") // get the stream_id field
      stream <- Stream.findByStringId(id)
      user <- User.currentUser
    } {
      UserStream.join(user.id.is, stream.id.is)
      S.notice("Thanks for joining "+stream.name.is)
      S.redirectTo(Sitemap.streamLoc.calcHref(stream))
    }

    in
  }
}

object PostMessage extends AppHelpers with Loggable {
  def render = {
    val currentStream = Sitemap.streamLoc.currentValue
    var msg = ""

    def process(): JsCmd = {
      logger.debug("msg: "+msg)
      for {
        stream <- currentStream ?~ "Invalid stream"
        user <- User.currentUser ?~ "Invalid user"
      } yield {
        val message = Message.createRecord
          .streamId(stream.id.is)
          .userId(user.id.is)
          .text(msg)
          .save

        // send to the comment server
        StreamManager.dispatcherFor(stream.id.is) ! SendAMessage(message)

        // clear form
        JsFunc("StreamModel.clearForm").cmd
      }
    }

    "#id_tab" #> <a href={Sitemap.streamLoc.calcDefaultHref}>{currentStream.map(_.name.is) openOr Text("Unknown Stream")}</a> &
    "name=message" #> (SHtml.textarea(msg, msg = _, ("class" -> "xxlarge"), ("rows" -> "4"), ("id", "id_message")) ++ SHtml.hidden(process))
  }
}

object Messages extends AppHelpers {
  import json._

  def updater(in: NodeSeq): NodeSeq = {
    for {
      stream <- Sitemap.streamLoc.currentValue ?~ "Invalid stream"
    }
    yield ({
      <lift:comet type="StreamUpdater" name={stream.id.toString}>
      { in }
      </lift:comet>
    }): NodeSeq
  }
}
