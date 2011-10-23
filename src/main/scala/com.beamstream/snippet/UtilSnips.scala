package com.beamstream
package snippet

import lib.App

import scala.xml.NodeSeq

import net.liftweb._
import http.S
import util.Props

object ShowInProductionOnly {
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
    if (App.isPreBeta) <lift:embed what="/index-prebeta" />
    else <lift:embed what="/index-main" />
}