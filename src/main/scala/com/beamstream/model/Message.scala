package com.beamstream
package model

import lib.Gravatar

import scala.xml.NodeSeq

import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

import net.liftweb._
import common._
import json._
import mongodb.Limit
import mongodb.record._
import mongodb.record.field._
import record.field._
import textile.TextileParser

object MessageType extends Enumeration {
  val Text = Value(0, "text")
  val Picture = Value(1, "Picture")
  val Video = Value(2, "Video")
  val Audio = Value(3, "Audio")
}

class Message private () extends MongoRecord[Message] with ObjectIdPk[Message] {
  def meta = Message

  object streamId extends ObjectIdRefField(this, Stream)
  object userId extends ObjectIdRefField(this, User)
  object messageType extends EnumNameField(this, MessageType)
  object text extends StringField(this, 140)
  object whenCreated extends DateField(this) {
    override def defaultValue = (new DateTime).toDate
  }
}

object Message extends Message with MongoMetaRecord[Message] {
  import mongodb.BsonDSL._

  val fmt = ISODateTimeFormat.dateTime

  override def collectionName = "stream.messages"

  ensureIndex((streamId.name -> 1))
  ensureIndex((userId.name -> 1))

  def findAllByStreamId(in: ObjectId, limit: Int = 0): List[Message] =
    findAll((streamId.name -> in), (whenCreated.name -> -1), Limit(limit))

  def asJValue(in: List[Message]): JValue = {
    val users = Map() ++ User.findAll(in.map(_.userId.is).distinct).map(u => (u.id.is, u))
    JArray(in.map(msg => {
      users.get(msg.userId.is).map(u => asJValue(msg, u)).getOrElse(JNothing)
    }))
  }

  def asJValue(in: Message, user: User): JValue = {
    val msgHtml = TextileParser.parse(in.text.is, Empty)
      .map(t => TextileParser.paraFixer(t.toHtml))
      .getOrElse(NodeSeq.Empty)

    ("id" -> in.id.toString) ~
    ("userName" -> user.nameAndUsername) ~
    ("avatarUrl" -> Gravatar.imageUrl(user.email.is, 40)) ~
    ("text" -> msgHtml.toString) ~
    ("date" -> fmt.print(new DateTime(in.whenCreated.is)))
  }
}
