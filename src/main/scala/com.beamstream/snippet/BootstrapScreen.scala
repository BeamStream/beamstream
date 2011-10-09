package com.beamstream
package snippet

import net.liftweb._
import http._
import util.Helpers._

/*
 * For use with bootstrap-screen.html
 */
trait BootstrapScreen extends LiftScreen {
  override def allTemplatePath = "templates-hidden" :: "bootstrap-screen" :: Nil
  override val cancelButton = super.cancelButton % ("class" -> "btn")
  override val finishButton = super.finishButton % ("class" -> "btn primary")
}