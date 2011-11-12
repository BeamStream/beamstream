package com.beamstream
package config

import api._

import net.liftweb._
import http.LiftRules

object ApiConfig {
  def init(): Unit = {
    // stateful -- associated with a servlet container session
    LiftRules.dispatch.append(FacebookApiStateful)
    LiftRules.dispatch.append(StreamApiStateful)

    // stateless
  }
}
