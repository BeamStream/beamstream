package com.beamstream
package snippet

import lib.FacebookGraph
import model.User

import scala.xml.{NodeSeq, Text}

import net.liftweb._
import common._
import http.{Factory, NoticeType, S, SHtml}
import http.js.JsCmds._
import http.js.JE._
import util.Props
import util.Helpers._

object Facebook extends Loggable {
  /**
    * If user is already connected, display a button with a direct login, otherwise
    * display a button that opens a facebook auth dialog window.
    */
  def link = {
    <span id="id_facebooklink">
      <a href="#"><img src="/img/fb.png" width="181" height="25" /></a>
    </span>
    <div lift="embed?what=/templates-hidden/parts/fb-init"></div>
    <script type="text/javascript">
    <![CDATA[
      $("#id_facebooklink").click(function() { onClick(); });

      var onClick = function() {
        BeamStream.util.wopen("/facebook/connect", "facebook_connect", 640, 360);
        return false;
      };

      BeamStream.facebook.init(Input, function() {
        FB.getLoginStatus(function(response) {
          if (response.authResponse) {
            BeamStream.api.facebook.init(response.authResponse, function(data) {
              if (data.alert) {
                console.log(data.alert.level+": "+data.alert.message);
              }
              else if (data.status) {
                onClick = function() {
                  BeamStream.api.facebook.login(function(resp) {
                    if (resp.alert) {
                      console.log(resp.alert.level+": "+resp.alert.message);
                    }
                    else if (resp.url) {
                      window.location=resp.url
                    }
                  })
                  return false;
                };
              }
            })
          }
        })
      });
    ]]>
    </script>
  }

  def popupLink = {
    <span id="id_facebooklink">
      <a href="#"><img src="/img/fb.png" width="181" height="25" /></a>
    </span>
    <script type="text/javascript">
    <![CDATA[
      $("#id_facebooklink").click(function() {
        BeamStream.util.wopen("/facebook/connect", "facebook_connect", 640, 360);
        return false;
      });
    ]]>
    </script>
  }

  /**
    * Inject the data Facebook needs for initialization
    */
  def init =
    "#id_jsinit" #>
      Script(
        JsCrVar("Input", JsObj(
          ("appId", Str(FacebookGraph.key.vend)),
          ("channelUrl", Str(S.hostAndPath+FacebookGraph.channelUrl.vend))
        ))
      )

  def close =
    Script(
      JsCrVar("Input", JsObj(
        ("url", Str(S.param("url").openOr(User.loginContinueUrl.is)))
      ))
    )

  /**
    * Only display if connected to facebook and access tokenis empty
    */
  def checkAuthToken(in: NodeSeq): NodeSeq =
    if(User.isConnectedToFaceBook && FacebookGraph.currentAccessToken.is.isEmpty)
      in
    else
      NodeSeq.Empty
}