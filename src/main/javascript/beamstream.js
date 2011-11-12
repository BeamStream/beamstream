var BeamStream = {
  // api
  api: {
    facebook: {
      init: function(data, success) {
        $.ajax({
          type: "POST",
          url: "/api/facebook/init",
          data: data,
          success: success
        });
      },
      login: function(success) {
        $.ajax({
          type: "POST",
          url: "/api/facebook/login",
          success: success
        });
      }
    }
  },
  // facebook
  facebook: {
    init: function(input, func) {
      window.fbAsyncInit = function() {
        FB.init({
          appId : input.appId, // App ID
          channelURL : input.channelUrl,
          status : true, // check login status
          cookie : true, // enable cookies to allow the server to access the session
          oauth : true, // enable OAuth 2.0
          xfbml : false // parse XFBML
        });
        // Additional initialization code here
        func();
      };
    }
  },
  // Util
  util: {
    /*
     * Open a popup window centered on the screen
     * http://www.boutell.com/newfaq/creating/windowcenter.html
     */
    wopen: function (url, name, w, h) {
      // Fudge factors for window decoration space.
      // In my tests these work well on all platforms & browsers.
      w += 32;
      h += 96;
      wleft = (screen.width - w) / 2;
      wtop = (screen.height - h) / 2;
      var win = window.open(url,
        name,
        'width=' + w + ', height=' + h + ', ' +
        'left=' + wleft + ', top=' + wtop + ', ' +
        'location=no, menubar=no, ' +
        'status=no, toolbar=no, scrollbars=no, resizable=no');
      // Just in case width and height are ignored
      win.resizeTo(w, h);
      // Just in case left and top are ignored
      win.moveTo(wleft, wtop);
      win.focus();
    }
  }
}