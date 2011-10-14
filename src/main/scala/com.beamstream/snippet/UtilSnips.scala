package com.beamstream
package snippet

import scala.xml.NodeSeq

import net.liftweb._
import util.Props

object ShowInProductionOnly {
  def render(in: NodeSeq): NodeSeq =
    if (Props.productionMode) in
    else NodeSeq.Empty
}

object ThrowException {
  def render =
    throw new Exception("This is only a test.")
}