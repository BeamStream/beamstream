package com.beamstream
package model

import locs.Sitemap

import org.apache.shiro.authc.UsernamePasswordToken
import org.bson.types.ObjectId

import net.liftweb._
import common._
import http.{BooleanField => _, _}
import mongodb.record.field._
import record.field._
import util.FieldContainer

import com.eltimn.auth.mongo.{ProtoAuthUser, ProtoAuthUserMeta}

class User private () extends ProtoAuthUser[User] with ObjectIdPk[User] {
  def meta = User

  def isRegistered: Boolean = !id.is.isNew

  object locale extends LocaleField(this) {
    override def displayName = "Locale"
    override def defaultValue = "en_US"
  }
  object timezone extends TimeZoneField(this) {
    override def displayName = "Time Zone"
    override def defaultValue = "America/Chicago"
  }
  // email address has been verified by clicking on an AuthToken link
  object verified extends BooleanField(this) {
    override def shouldDisplay_? = false
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

  def findByEmail(email: String): Box[User] = find("email", email)

  def findPasswordForUser(login: String): Box[(ObjectId, String)] =
    findPasswordForUser(login, email.name, password.name) or findPasswordForUser(login, username.name, password.name)

  def createUser(username: String, email: String, password: String, permissions: List[String]): Box[User] = {
    val newUser = createRecord
      .username(username)
      .email(email)
      .password(password, true)
      .permissions(permissions)
      .save

    Full(newUser)
  }

  def deleteAllAuthTokens: Unit = currentUser
    .foreach { u => AuthToken.deleteAllByUser(u.id.is) }

  // this is set in auth and signin and used by register
  //object regUser extends SessionVar[User](User.createRecord)

  // used during login process
  object loginToken extends SessionVar[UsernamePasswordToken](new UsernamePasswordToken)

  def createUserFromToken = createRecord.email(loginToken.is.getUsername)

  val siteName = "BeamStream"
  val systemEmail = "info@beamstream.com"
  val systemUser: User = find("email", systemEmail) openOr {
    User.createRecord
      .username("%s Staff".format(siteName))
      .email(systemEmail)
      .verified(true)
      .password("[.^wOFr&QgZ6dOk6gg?2js_", true)
      .save
  }

  // send an email to the user with a link for authorization
  def sendAuthLink(user: User) {
    import net.liftweb.util.Mailer._

    val authToken = AuthToken.createForUser(user.id.is)

    val msgTxt =
      """
        |Someone requested a link to change your password on the %s website.
        |
        |If you did not request this, you can safely ignore it. It will expire 48 hours from the time this message was sent.
        |
        |Follow the link below or copy and paste it into your internet browser.
        |
        |%s
        |
        |Thanks,
        |%s Staff
      """.format(siteName, authToken.authLink, siteName).stripMargin

    sendMail(
      From(systemUser.fancyEmail),
      Subject("%s Password Help".format(siteName)),
      To(user.email.is),
      PlainMailBodyType(msgTxt)
    )
  }
}