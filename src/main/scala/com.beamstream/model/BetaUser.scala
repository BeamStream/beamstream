package com.beamstream
package model

import scala.xml.Text

import net.liftweb._
import mongodb.record._
import mongodb.record.field.ObjectIdPk
import record.field.{EmailField, StringField}
import util.FieldError

class BetaUser private () extends MongoRecord[BetaUser] with ObjectIdPk[BetaUser] {
  def meta = BetaUser

  object email extends EmailField(this, 254) {
    override def setFilter = trim _ :: toLower _ :: super.setFilter

    private def valUnique(msg: => String)(value: String): List[FieldError] = {
      owner.meta.findAll("email", value).filter(_.id.is != owner.id.is).map(u =>
        FieldError(this, Text(msg))
      )
    }

    override def validations =
      valUnique("That email address has already added to the list") _	::
      valMaxLen(254, "Email must be 254 characters or fewer") _ ::
      super.validations
  }

  object userRole extends StringField(this, 20) {
    private def valIsSet(msg: => String)(value: String): List[FieldError] = {
      if (!meta.roles.contains(value))
        List(FieldError(this, Text(msg)))
      else
        Nil
    }

    override def validations =
      valIsSet("Please select a role") _ ::
      super.validations
  }
}
object BetaUser extends BetaUser with MongoMetaRecord[BetaUser] {
  import mongodb.BsonDSL._

  override def collectionName = "user.users"

  ensureIndex(("email" -> 1), true) // unique email

  val roles = List("Student", "Educator", "Professional")
}

/*
object UserRoles extends Enumeration {
  val Student = Value(1, "Student")
  val Educator = Value(2, "Educator")
  val Professional = Value(3, "Professional")
}
*/