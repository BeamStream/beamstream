package com.beamstream
package api

import lib.AppHelpers
import model._

import net.liftweb._
import common._
import http._
import http.rest.RestHelper
import json._
import util.Helpers._

object StreamApiStateful extends RestHelper with AppHelpers with Loggable {

  serve("api" / "stream" prefix {
    case "names" :: Nil Get _ =>  {
      import mongodb.BsonDSL._
      for {
        term <- S.param("term") ?~ "Search term not provided"
        user <- User.currentUser ?~ "User not found"
      } yield {
        val userStreamIds = user.userStreams.map(_.streamId.is)
        JArray(
          Stream.findAllByRegex(term)
            .filterNot(s => userStreamIds.contains(s.id.is))
            .map(s => ("id" -> s.id.toString) ~ ("value" -> s.name.is))
        )
      }
    }
  })
}
