package com.beamstream
package comet

import locs.Sitemap
import model._

import scala.xml._

import org.bson.types.ObjectId

import net.liftweb._
import actor.LiftActor
import common._
import http._
import http.js.JE._
import http.js.{JsCmd, JsExp}
import http.js.JsCmds._
import http.js.jquery.JqJsCmds._
import json._
import util.Helpers._

// comet messages
case class UsersUpdate(users: Set[User])
case class StreamUpdate(msg: Message)

// dispatcher messages
case class AddAListener(actor: StreamUpdater)
case class RemoveAListener(actor: StreamUpdater)
case class SendAMessage(msg: Message)

/**
  * StreamUpdater comet actor
  */
class StreamUpdater extends CometActor with Loggable {
  /**
    * Public fields
    */
  val user: Box[User] = User.currentUser

  /**
    * Internal state of comet actor data
    */
  private val stream: Box[Stream] = Sitemap.streamLoc.currentValue
  private var activeUsers: Set[User] = Set.empty

  private def dispatcher: Box[StreamDispatcher] = stream.map(s => StreamManager.dispatcherFor(s.id.is))

  // setup the comet actor
  override def localSetup() {
    dispatcher foreach { _ ! AddAListener(this) }
    super.localSetup()
  }

  // shutdown the comet actor
  override def localShutdown() {
    dispatcher foreach { _ ! RemoveAListener(this) }
    super.localShutdown()
  }

  /**
    * comet message handlers
    */
  override def mediumPriority = {
    case UsersUpdate(users) => {
      logger.debug("UsersUpdate: "+users.map(_.username.is).mkString(","))
      activeUsers = users
      partialUpdate(renderUsers)
    }
    case StreamUpdate(msg) => {
      logger.debug("StreamUpdate: "+msg.toString)
      partialUpdate(renderMessages(msg :: Nil))
    }
  }

  /**
    * Render the screen on first load
    */
  def render = {
    val msgs = stream.map(s => Message.findAllByStreamId(s.id.is, 10)) openOr Nil
    new RenderOut(NodeSeq.Empty,
      renderMessages(msgs) &
      renderUsers
    )
  }

  // render messages
  private def renderMessages(messages: List[Message]): JsCmd = {
    val msgs = JsRaw(compact(json.render(Message.asJValue(messages))))
    Call("StreamModel.renderMsgs", msgs)
  }

  // all active users
  private def renderUsers: JsCmd = {
    import JsonDSL._

    val users = JsRaw(compact(json.render(
      JArray(activeUsers.toList.reverse.map(u =>
        ("id" -> u.id.toString) ~
        ("name" -> u.nameAndUsername)
      ))
    )))

    Call("StreamModel.renderUsers", users)
  }
}

/**
  * Dispatcher for a particular stream
  */
class StreamDispatcher(streamId: ObjectId) extends LiftActor with Loggable {
  logger.debug("MessageDispatcher instantiated for streamId: %s.".format(streamId.toString))

  def listenerCount = listeners.size

  private var listeners = new scala.collection.mutable.HashSet[StreamUpdater]

  override def messageHandler = {
    /**
     * if we do not have this actor in the list, add it (register it)
     */
    case AddAListener(actor) =>
      if (!listeners.contains(actor)) {
        logger.debug("Adding comet actor: %s".format(actor))
        listeners += actor
        sendNewUsersList
      }

    case RemoveAListener(actor) =>
      logger.debug("Removing comet actor: %s".format(actor))
      listeners -= actor
      sendNewUsersList

    case SendAMessage(msg) =>
      logger.debug("Message received: %s".format(msg))
      sendMsg(msg)

    case x => logger.warn("Unknown message received: %s".format(x.toString))
  }

  // send everyone a message with new active users list
  private def sendNewUsersList {
    val users = listeners.flatMap(_.user.toList).toSet
    listeners foreach { l => l ! UsersUpdate(users) }
  }

  private def sendMsg(msg: Message) {
    listeners foreach (_ ! StreamUpdate(msg))
  }
}

/**
  * Dispatch Manager
  */
object StreamManager extends Loggable {
  private var dispatchers: Map[ObjectId, StreamDispatcher] = Map()

  def dispatcherFor(streamId: ObjectId): StreamDispatcher = synchronized {
    dispatchers.get(streamId) match {
      case Some(md) => md
      case None => {
        val newDispatcher = new StreamDispatcher(streamId)
        dispatchers += streamId -> newDispatcher
        logIt
        newDispatcher
      }
    }
  }

  def cleanup = {
    dispatchers.foreach { case (id, dis) => if (dis.listenerCount == 0) dispatchers = dispatchers - id }
  }

  private def logIt {
    logger.info("Dispatchers map is %s".format(dispatchers))
  }
}
