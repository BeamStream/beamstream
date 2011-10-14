package com.beamstream
package model

import locs.Sitemap

import org.bson.types.ObjectId
import org.joda.time.DateTime

import net.liftweb._
import common._
import http.{LiftResponse, RedirectResponse, Req, S, SessionVar}
import mongodb.record.field._
import record.field._
import util.FieldContainer

import com.eltimn.auth.mongo._

class User private () extends ProtoAuthUser[User] with ObjectIdPk[User] {
  def meta = User

  def userIdAsString: String = id.toString

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

  def findByEmail(eml: String): Box[User] = find(email.name, eml)
  def findByUsername(uname: String): Box[User] = find(username.name, uname)

  def findByStringId(id: String): Box[User] =
    if (ObjectId.isValid(id)) find(new ObjectId(id))
    else Empty

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
  private lazy val afterloginTokenUrl = AuthRules.afterloginTokenUrl.vend

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
        case Full(_) => respUrl = afterloginTokenUrl.toString
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
          case Empty =>
        }
      }
    }
  }

  // used during login process
  object loginCredentials extends SessionVar[LoginCredentials](LoginCredentials(""))

  def createUserFromCredentials = createRecord.email(loginCredentials.is.email)
}

case class LoginCredentials(val email: String, val isRememberMe: Boolean = false)