package com.beamstream
package model

import locs.Sitemap

import scala.xml.Text
import java.util.TimeZone

import org.bson.types.ObjectId
import org.joda.time.DateTime

import net.liftweb._
import common._
import http.{LiftResponse, RedirectResponse, Req, S, SessionVar}
import json._
import mongodb.record.field._
import record.field.{PasswordField => _, _}
import util.{FieldContainer, FieldError, Helpers}

import com.eltimn.auth.mongo._

class User private () extends MongoAuthUser[User] with ObjectIdPk[User] {
  def meta = User

  def userIdAsString: String = id.toString

  object username extends StringField(this, 32) {
    override def displayName = "Username"
    override def setFilter = trim _ :: super.setFilter

    private def valUnique(msg: => String)(value: String): List[FieldError] = {
      if (value.length > 0)
        meta.findAll(name, value).filterNot(_.id.is == owner.id.is).map(u =>
          FieldError(this, Text(msg))
        )
      else
        Nil
    }

    override def validations =
      valUnique("Another user is already using that username, please enter a different one") _ ::
      valMinLen(3, "Username must be at least 3 characters") _ ::
      valMaxLen(32, "Username must be less than 33 characters") _ ::
      super.validations
  }

  /*
  * http://www.dominicsayers.com/isemail/
  */
  object email extends EmailField(this, 254) {
    override def displayName = "Email"
    override def setFilter = trim _ :: toLower _ :: super.setFilter

    private def valUnique(msg: => String)(value: String): List[FieldError] = {
      owner.meta.findAll(name, value).filter(_.id.is != owner.id.is).map(u =>
        FieldError(this, Text(msg))
      )
    }

    override def validations =
      valUnique("That email address is already registered with us") _  ::
      valMaxLen(254, "Email must be 254 characters or less") _ ::
      super.validations
  }
  // email address has been verified
  object verified extends BooleanField(this) {
    override def displayName = "Verified"
  }
  object password extends PasswordField(this, 6, 32, Empty) {
    override def displayName = "Password"
  }
  object permissions extends PermissionListField(this)
  object roles extends ObjectIdRefListField(this, Role) {
    def permissions: List[Permission] = objs.flatMap(_.permissions.is)
    def names: List[String] = objs.map(_.id.is)
  }

  lazy val authPermissions: Set[Permission] = (permissions.is ::: roles.permissions).toSet
  lazy val authRoles: Set[String] = roles.names.toSet

  lazy val fancyEmail = AuthUtil.fancyEmail(username.is, email.is)

  object locale extends LocaleField(this) {
    override def displayName = "Locale"
    override def defaultValue = "en_US"
  }
  object timezone extends TimeZoneField(this) {
    override def displayName = "Time Zone"
    override def defaultValue = "America/Chicago"
  }

  object name extends StringField(this, 64) {
    override def displayName = "Name"

    override def validations =
      valMaxLen(64, "Name must be 64 characters or less") _ ::
      super.validations
  }
  object location extends StringField(this, 64) {
    override def displayName = "Location"

    override def validations =
      valMaxLen(64, "Location must be 64 characters or less") _ ::
      super.validations
  }
  object bio extends TextareaField(this, 160) {
    override def displayName = "Bio"

    override def validations =
      valMaxLen(160, "Bio must be 160 characters or less") _ ::
      super.validations
  }
  object gender extends StringField(this, 64) {
    override def displayName = "Gender"

    override def validations =
      valMaxLen(64, "Name must be 64 characters or less") _ ::
      super.validations
  }

  object facebookId extends IntField(this)

  /*
   * FieldContainers for various LiftScreeens.
   */
  def accountScreenFields = new FieldContainer {
    def allFields = List(username, email, locale, timezone)
  }

  def profileScreenFields = new FieldContainer {
    def allFields = List(name, location, bio)
  }

  def registerScreenFields = new FieldContainer {
    def allFields = List(username, email)
  }

  def whenCreated: DateTime = new DateTime(id.is.getTime)
}

object User extends User with ProtoAuthUserMeta[User] with Loggable {
  import mongodb.BsonDSL._

  override def collectionName = "user.users"

  ensureIndex((email.name -> 1), true)
  ensureIndex((username.name -> 1), true)
  ensureIndex((facebookId.name -> 1))

  def findByEmail(in: String): Box[User] = find(email.name, in)
  def findByUsername(in: String): Box[User] = find(username.name, in)
  def findByFacebookId(in: Int): Box[User] = find(facebookId.name, in)

  def findByStringId(id: String): Box[User] =
    if (ObjectId.isValid(id)) find(new ObjectId(id))
    else Empty

  def fromFacebookJson(in: JValue): Box[User] = {
    Helpers.tryo(in transform {
      case JField("id", JString(id)) => JField("facebookId", JInt(Helpers.toInt(id)))
      case JField("location", JObject(List(JField("facebookId", JInt(id)), JField("name", JString(name))))) =>
        JField("location", JString(name))
      case JField("timezone", JInt(offset)) =>
        val offId = "GMT" + (
          if (offset > 0)
            "+%s".format(offset.toString)
          else
            "%s".format(offset.toString)
        )
        JField("timezone", JString(TimeZone.getTimeZone(offId).getID))
    }) flatMap { jv =>
      logger.debug(pretty(render(jv)))
      fromJValue(jv)
    }
  }

  override def onLogIn: List[User => Unit] = List(user => User.loginCredentials.remove())
  override def onLogOut: List[Box[User] => Unit] = List(
    x => logger.debug("User.onLogOut called."),
    boxedUser => boxedUser.foreach { u =>
      LoginToken.deleteAllByUserId(u.id.is)
      ExtSession.deleteExtCookie()
      ExtSession.deleteAllByUserId(u.id.is)
    }
  )

  /*
   * AuthRules vars
   */
  private lazy val siteName = AuthRules.siteName.vend
  private lazy val sysUsername = AuthRules.systemUsername.vend
  private lazy val indexUrl = AuthRules.indexUrl.vend
  private lazy val loginTokenAfterUrl = AuthRules.loginTokenAfterUrl.vend

  /*
   * LoginToken
   */
  private def logUserInFromToken(uid: ObjectId): Box[Unit] = find(uid).map { user =>
    user.verified(true)
    user.save
    logUserIn(user, false)
    LoginToken.deleteAllByUserId(user.id.is)
  }

  override def handleLoginToken: Box[LiftResponse] = {
    var respUrl = indexUrl.toString
    S.param("token").flatMap(LoginToken.findByStringId) match {
      case Full(at) if (at.expires.isExpired) => {
        S.error("Login token has expired")
        at.delete_!
      }
      case Full(at) => logUserInFromToken(at.userId.is) match {
        case Full(_) => respUrl = loginTokenAfterUrl.toString
        case _ => S.error("User not found")
      }
      case _ => S.warning("Login token not provided")
    }

    Full(RedirectResponse(respUrl))
  }

  // send an email to the user with a link for logging in
  def sendLoginToken(user: User): Unit = {
    import net.liftweb.util.Mailer._

    val token = LoginToken.createForUserId(user.id.is)

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
        |%s
      """.format(siteName, token.url, sysUsername).stripMargin

    sendMail(
      From(AuthRules.systemFancyEmail),
      Subject("%s Password Help".format(siteName)),
      To(user.fancyEmail),
      PlainMailBodyType(msgTxt)
    )
  }

  /*
   * ExtSession
   */
  private def logUserInFromExtSession(uid: ObjectId): Box[Unit] = find(uid).map { user =>
    logUserIn(user, false)
  }

  def createExtSession(uid: ObjectId) = ExtSession.createExtSession(uid)

  /*
  * Test for active ExtSession.
  */
  def testForExtSession: Box[Req] => Unit = {
    ignoredReq => {
      if (currentUserId.isEmpty) {
        ExtSession.handleExtSession match {
          case Full(es) => logUserInFromExtSession(es.userId.is)
          case Failure(msg, _, _) =>
            logger.warn("Error logging user in with ExtSession: %s".format(msg))
          case Empty => // cookie is not set
        }
      }
    }
  }

  // used during login process
  object loginCredentials extends SessionVar[LoginCredentials](LoginCredentials(""))

  def createUserFromCredentials = createRecord.email(loginCredentials.is.email)
}

case class LoginCredentials(val email: String, val isRememberMe: Boolean = false)