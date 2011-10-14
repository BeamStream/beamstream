package com.beamstream
package lib

import scala.xml._
import net.liftweb._
import common._
import util.CssSel
import util.Helpers._

trait SnippetHelpers {
  /*
  * Allows for the following to be used when building snippets and it will handle
  * errors according to handleError:
  *
  *   for {
  *     user <- User.currentUser ?~ "You must be logged in to edit your profile."
  *   } yield ({
  *   ...
  *   }): NodeSeq
  */
  implicit def boxNodeSeqToNodeSeq(in: Box[NodeSeq]): NodeSeq = in match {
    case Full(ns) => ns
    case Failure(msg, _, _) => handleNodeSeqError(msg)
    case Empty => handleNodeSeqError("Empty snippet")
  }
  def handleNodeSeqError(msg: String): NodeSeq = Comment("ERROR: %s".format(msg))

  /*
  * Allows for the following to be used when building snippets and it will handle
  * errors according to handleError:
  *
  *   for {
  *     user <- User.currentUser ?~ "You must be logged in to edit your profile."
  *   } yield ({
  *   ...
  *   }): CssSel
  */
  implicit def boxCssSelToCssSel(in: Box[CssSel]): CssSel = in match {
    case Full(csssel) => csssel
    case Failure(msg, _, _) => handleCssSelError(msg)
    case Empty => handleCssSelError("Empty snippet")
  }
  def handleCssSelError(msg: String): CssSel = "*" #> Text("ERROR: %s".format(msg))
}
