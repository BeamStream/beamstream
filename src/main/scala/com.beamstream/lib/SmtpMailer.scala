package com.beamstream
package lib

import javax.mail.{Authenticator, PasswordAuthentication}
import javax.mail.internet.MimeMessage

import net.liftweb._
import common._
import util._

object SmtpMailer extends Loggable {
  /*
  * Config mailer
  */
  def init {

    var isAuth = Props.get("mail.smtp.auth", "false").toBoolean

    Mailer.customProperties = Props.get("mail.smtp.host", "localhost") match {
      case "smtp.gmail.com" =>
        isAuth = true
        Map(
          "mail.smtp.host" -> "smtp.gmail.com",
          "mail.smtp.port" -> "587",
          "mail.smtp.auth" -> "true",
          "mail.smtp.starttls.enable" -> "true"
        )
      case h => Map(
        "mail.smtp.host" -> h,
        "mail.smtp.port" -> Props.get("mail.smtp.port", "25"),
        "mail.smtp.auth" -> isAuth.toString
      )
    }

    //Mailer.devModeSend.default.set((m : MimeMessage) => logger.info("Sending Mime Message: "+m))

    if (isAuth) {
      (Props.get("mail.smtp.user"), Props.get("mail.smtp.pass")) match {
        case (Full(username), Full(password)) =>
          logger.debug("Gmail user: %s".format(username))
          logger.debug("Gmail password: %s".format(password))
          Mailer.authenticator = Full(new Authenticator() {
            override def getPasswordAuthentication = new
              PasswordAuthentication(username, password)
          })
        case _ => logger.error("Username/password not supplied for Mailer.")
      }
    }
  }
}
