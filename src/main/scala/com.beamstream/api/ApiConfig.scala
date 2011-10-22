package com.beamstream
package api

import net.liftweb._
import http.LiftRules

object ApiConfig {
  def init(): Unit = {
    // stateful -- associated with a servlet container session
    LiftRules.dispatch.append(FacebookApiStateful)
  }
}