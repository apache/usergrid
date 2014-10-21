//
// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

/**
*  dogs is a sample app  that is powered by Usergrid
*  This app shows how to use the Usergrid SDK to connect
*  to Usergrid, how to add entities, and how to page through
*  a result set of entities
*
*  Learn more at http://Usergrid.com/docs
*
*   Copyright 2012 Apigee Corporation
*
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*/

/**
*  @file app.js
*  @author Rod Simpson (rod@apigee.com)
*
*  This file contains the main program logic for Facebook Login Example.
*/
$(document).ready(function () {
  //first set the org / app path (must be orgname / appname or org id / app id - can't mix names and uuids!!)

  var client = new Usergrid.Client({
    orgName:'yourorgname',
    appName:'sandbox',
    logging: true, //optional - turn on logging, off by default
    buildCurl: true //optional - turn on curl commands, off by default
  });

  //bind the login button so we can send the user to facebook
  $('#login-button').bind('click', function() {
    //get the
    var apiKey = $("#api-key").val();
    var location = window.location.protocol + '//' + window.location.host;
    var path = window.location.pathname;

    var link = "https://www.facebook.com/dialog/oauth?client_id=";
    link += apiKey;
    link += "&redirect_uri=";
    link += location+path
    link += "&scope&COMMA_SEPARATED_LIST_OF_PERMISSION_NAMES&response_type=token";

    //now forward the user to facebook
    window.location = link;
  });
  //bind the previous button to the proper method in the collection object
  $('#logout-button').bind('click', function() {
    logout();
  });

  //load up the facebook api sdk
  window.fbAsyncInit = function() {
    FB.init({
      appId      : '308790195893570', // App ID
      channelUrl : '//usergridsdk.dev//examples/channel.html', // Channel File
      status     : true, // check login status
      cookie     : true, // enable cookies to allow the server to access the session
      xfbml      : true  // parse XFBML
    });
  };

  function logout() {
    FB.logout(function(response) {
      client.logoutAppUser();
      var html = "User logged out. \r\n\r\n // Press 'Log in' to log in with Facebook.";
      $('#facebook-status').html(html);
    });
  }

  function login(facebookAccessToken) {
    client.loginFacebook(facebookAccessToken, function(err, response){
      var output = JSON.stringify(response, null, 2);
      if (err) {
        var html = '<pre>Oops!  There was an error logging you in. \r\n\r\n';
        html += 'Error: \r\n' + output+'</pre>';
      } else {
        var html = '<pre>Hurray!  You have been logged in. \r\n\r\n';
        html += 'Facebook Token: ' + '\r\n' + facebookAccessToken + '\r\n\r\n';
        html += 'Facebook Profile data stored in Usergrid: \r\n' + output+'</pre>';
      }
      $('#facebook-status').html(html);
    })
  }

  //pull the access token out of the query string
  var ql = [];
  if (window.location.hash) {
    // split up the query string and store in an associative array
    var params = window.location.hash.slice(1).split("#");
    var tmp = params[0].split("&");
    for (var i = 0; i < tmp.length; i++) {
      var vals = tmp[i].split("=");
      ql[vals[0]] = unescape(vals[1]);
    }
  }

  if (ql['access_token']) {
    var facebookAccessToken = ql['access_token']
    //try to log in with facebook
    login(facebookAccessToken);
  }
});