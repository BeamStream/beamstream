package com.beamstream
package model

import org.bson.types.ObjectId

import net.liftweb._
import common._
import mongodb.record._
import mongodb.record.field._
import record.field._

class UserStream private () extends MongoRecord[UserStream] with ObjectIdPk[UserStream] {
  def meta = UserStream

  object userId extends ObjectIdRefField(this, User)
  object streamId extends ObjectIdRefField(this, Stream)
}

object UserStream extends UserStream with MongoMetaRecord[UserStream] {
  import mongodb.BsonDSL._

  override def collectionName = "stream.userstreams"

  ensureIndex((userId.name -> 1))
  ensureIndex((streamId.name -> 1))

  def join(userId: ObjectId, streamId: ObjectId): UserStream =
    createRecord
      .userId(userId)
      .streamId(streamId)
      .save

  def findAllByStreamId(in: ObjectId): List[UserStream] = findAll(streamId.name, in)
  def findAllByUserId(in: ObjectId): List[UserStream] = findAll(userId.name, in)
}
