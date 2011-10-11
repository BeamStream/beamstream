package com.beamstream
package model

import locs.Sitemap

import org.bson.types.ObjectId

import net.liftweb._
import common._
import http.{BooleanField => _, _}
import mongodb.record.field._
import record.field._
import util.FieldContainer

import com.eltimn.auth.mongo._

class User private () extends ProtoAuthUser[User] with ObjectIdPk[User] {
  def meta = User

  def isRegistered: Boolean = !id.is.isNew
  def userIdAsString: String = id.toString

  object locale extends LocaleField(this) {
    override def displayName = "Locale"
    override def defaultValue = "en_US"
  }
  object timezone extends TimeZoneField(this) {
    override def displayName = "Time Zone"
    override def defaultValue = "America/Chicago"
  }

  def registerScreenFields = new FieldContainer {
    def allFields = List(username, email, password, confirmPassword)
  }
}

object User extends User with ProtoAuthUserMeta[User, ObjectId] {
  import mongodb.BsonDSL._

  override def collectionName = "user.users"

  ensureIndex((email.name -> 1), true)
  ensureIndex((username.name -> 1), true)

  def findByEmail(eml: String): Box[User] = find(email.name, eml)

  override def onLogIn: List[User => Unit] = List(user => User.loginCredentials.remove())
  override def onLogOut: List[Box[User] => Unit] = List(
    boxedUser => boxedUser.foreach { u => deleteAllLoginTokens(u.id.is) }
  )

  def findByStringId(id: String): Box[User] =
    if (ObjectId.isValid(id)) find(new ObjectId(id))
    else Empty

  override def loginTokenMeta = Full(ObjectIdLoginToken)

  override def logUserInFromToken(id: String): Box[Unit] = findByStringId(id).map { user =>
    user.verified(true)
    user.save
    logUserIn(user, false)
  }

  def loginTokenForUserId(uid: ObjectId) = loginTokenMeta.map(ltm => ltm.createForUserId(uid))

  def sendAuthLink(user: User): Unit = loginTokenForUserId(user.id.is).foreach { lt =>
    sendAuthLink(user.email.is, lt)
  }

  // used during login process
  object loginCredentials extends SessionVar[LoginCredentials](LoginCredentials(""))

  def createUserFromCredentials = createRecord.email(loginCredentials.is.email)

  /*
  val systemUser: User = findByEmail(systemEmail) openOr {
    User.createRecord
      .username("%s Staff".format(siteName))
      .email(systemEmail)
      .verified(true)
      .password("[.^wOFr&QgZ6dOk6gg?2js_", true)
      .save
  }
  */
}

case class LoginCredentials(val email: String, val isRememberMe: Boolean = false)