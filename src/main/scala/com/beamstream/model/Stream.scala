package com.beamstream
package model

import scala.xml._

import org.bson.types.ObjectId

import net.liftweb._
import common._
import mongodb.record._
import mongodb.record.field._
import record.field._
import util._

object StreamType extends Enumeration {
  val Class = Value(0, "Class")
  val Study = Value(1, "Study")
  val Research = Value(2, "Research")
  val Friends = Value(3, "Friends")
}

class Stream private () extends MongoRecord[Stream] with ObjectIdPk[Stream] {
  def meta = Stream

  object name extends StringField(this, 64) {
    override def displayName = "Stream Name"

    private def valUnique(msg: => String)(value: String): List[FieldError] = {
      if (value.length > 0)
        meta.findAll(name, value).filterNot(_.id.is == owner.id.is).map(u =>
          FieldError(this, Text(msg))
        )
      else
        Nil
    }

    override def validations =
      valUnique("That Stream Name already exists") _ ::
      super.validations
  }
  object creatorId extends ObjectIdRefField(this, User) {
    override def shouldDisplay_? = false
  }
  object streamType extends EnumNameField(this, StreamType) {
    override def displayName = "Stream Type"
  }
}

object Stream extends Stream with MongoMetaRecord[Stream] {
  import mongodb.BsonDSL._

  override def collectionName = "stream.streams"

  ensureIndex((name.name -> 1), true)

  def findByStringId(in: String): Box[Stream] =
    if (ObjectId.isValid(in)) find(new ObjectId(in))
    else Failure("Invalid ObjectId")

  // http://www.mongodb.org/display/DOCS/Advanced+Queries#AdvancedQueries-RegularExpressions
  def findAllByRegex(in: String) =
    findAll((name.name -> ("$regex" -> in.r) ~ ("$options" -> "i")))
}
