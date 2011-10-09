package com.beamstream
package model

import locs.Sitemap

//import java.util.Date

import org.joda.time.DateTime

import net.liftweb._
import common._
import http._
import mongodb.record._
import mongodb.record.field._

import org.bson.types.ObjectId

import com.eltimn.auth.mongo.UserIdToken

class AuthToken extends MongoRecord[AuthToken] with ObjectIdPk[AuthToken] {
  def meta = AuthToken

  object userId extends ObjectIdRefField(this, User)
  object expires extends DateField(this) {
    override def defaultValue = ((new DateTime).plusHours(48)).toDate
  }

  def authLink = S.hostAndPath+"/auth?token="+id.toString
}
object AuthToken extends AuthToken with MongoMetaRecord[AuthToken] with shiro.SubjectLifeCycle {
  override def collectionName = "user.authtokens"

  def createForUser(uid: ObjectId): AuthToken = {
    createRecord.userId(uid).save
  }

  def deleteAllByUser(userId: ObjectId) {
    delete("userId", userId)
  }

  def handleToken: Box[LiftResponse] = {
    var retVar = RedirectResponse(Sitemap.homeUrl)
    S.param("token") match {
      case Full(t) if (ObjectId.isValid(t)) => find(new ObjectId(t)) match {
        case Full(at) if ((new DateTime).getMillis >= (new DateTime(at.expires.value)).getMillis) => {
          S.error("Auth token has expired.")
          at.delete_!
        }
        case Full(at) => {
          at.userId.obj match {
            case Full(u) => {
              u.verified(true)
              u.save
              at.delete_!
              loginAndDo(new UserIdToken(u.id.is)) {
                User.loginToken.remove()
                seeOther
              }
              retVar = RedirectResponse("/set_password") // need new transition page to allow user to select to set a password and/or to "remember me"
            }
            case _ => S.error("User not found.")
          }
        }
        case _ => S.error("Invalid auth token.")
      }
      case _ => S.warning("Auth token not provided.")
    }

    Full(retVar)
  }
}
