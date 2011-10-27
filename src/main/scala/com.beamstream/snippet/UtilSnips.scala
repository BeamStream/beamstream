package com.beamstream
package snippet

import config.AppConfig
import model.User

import scala.xml.NodeSeq

import net.liftweb._
import http.S
import util.Props

object ProductionOnly {
  def render(in: NodeSeq): NodeSeq =
    if (Props.productionMode) in
    else NodeSeq.Empty
}

object ThrowException {
  def render = {
    val msg = "This is only a test."
    S.error(msg)
    throw new Exception(msg)
  }
}

object HomePage {
  def render =
    if (AppConfig.isPreBeta) <lift:embed what="/index-prebeta" />
    else <lift:embed what="/index-main" />
}
