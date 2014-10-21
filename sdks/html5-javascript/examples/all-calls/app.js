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

/*
*  All Calls is a sample app  that is powered by Usergrid
*  This app shows how to make the 4 REST calls (GET, POST,
*  PUT, DELETE) against the usergrid API.
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
*  This file contains the main program logic for All Calls App.
*/
$(document).ready(function () {
  //first set the org / app path (must be orgname / appname or org id / app id - can't mix names and uuids!!)

  var client = new Usergrid.Client({
    orgName:'yourorgname',
    appName:'yourappname',
    logging: true, //optional - turn on logging, off by default
    buildCurl: true //optional - turn on curl commands, off by default
  });

  function hideAllSections(){
    $('#get-page').hide();
    $('#get-nav').removeClass('active');
    $('#post-page').hide();
    $('#post-nav').removeClass('active');
    $('#put-page').hide();
    $('#put-nav').removeClass('active');
    $('#delete-page').hide();
    $('#delete-nav').removeClass('active');
    $('#login-page').hide();
    $('#login-nav').removeClass('active');
    $('#response').html("// Press 'Run Query' to send the API call.");
  }
  //bind the show buttons
  $('#show-get').bind('click', function() {
    hideAllSections();
    $('#get-nav').addClass('active');
    $('#get-page').show();
  });

  $('#show-post').bind('click', function() {
    hideAllSections();
    $('#post-nav').addClass('active');
    $('#post-page').show();
  });

  $('#show-put').bind('click', function() {
    hideAllSections();
    $('#put-nav').addClass('active');
    $('#put-page').show();
  });

  $('#show-delete').bind('click', function() {
    hideAllSections();
    $('#delete-nav').addClass('active');
    $('#delete-page').show();
  });

  $('#show-login').bind('click', function() {
    hideAllSections();
    $('#login-nav').addClass('active');
    $('#login-page').show();
  });

  $('#run-get').bind('click', function() {
    _get();
  });

  $('#run-post').bind('click', function() {
    _post();
  });

  $('#run-put').bind('click', function() {
    _put();
  });

  $('#run-delete').bind('click', function() {
    _delete();
  });

  $('#run-login').bind('click', function() {
    _login();
  });

  //start with the get page showing by default
  $('#get-page').show();

  //bind the create new dog button
  $('#main-menu').bind('click', function() {
    $('#get-page').hide();
    $('#post-page').hide();
    $('#put-page').hide();
    $('#delete-page').hide();
    $('#login-page').hide();
    $('#main').show();
    $("#response").html('');
  });

  function _get() {
    var endpoint = $("#get-path").val();

    var options = {
      method:'GET',
      endpoint:endpoint
    };
    client.request(options, function (err, data) {
      //data will contain raw results from API call
      if (err) {
        var output = JSON.stringify(data, null, 2);
        $("#response").html('<pre>ERROR: '+output+'</pre>');
      } else {
        var output = JSON.stringify(data, null, 2);
        $("#response").html('<pre>'+output+'</pre>');
      }
    });
  }

  function _post() {
    var endpoint = $("#post-path").val();
    var data = $("#post-data").val();
    data = JSON.parse(data);

    var options = {
      method:'POST',
      endpoint:endpoint,
      body:data
    };
    client.request(options, function (err, data) {
      //data will contain raw results from API call
      if (err) {
        var output = JSON.stringify(data, null, 2);
        $("#response").html('<pre>ERROR: '+output+'</pre>');
      } else {
        var output = JSON.stringify(data, null, 2);
        $("#response").html('<pre>'+output+'</pre>');
      }
    });
  }

  function _put() {
    var endpoint = $("#put-path").val();
    var data = $("#put-data").val();
    data = JSON.parse(data);

    var options = {
      method:'PUT',
      endpoint:endpoint,
      body:data
    };
    client.request(options, function (err, data) {
      //data will contain raw results from API call
      if (err) {
        var output = JSON.stringify(data, null, 2);
        $("#response").html('<pre>ERROR: '+output+'</pre>');
      } else {
        var output = JSON.stringify(data, null, 2);
        $("#response").html('<pre>'+output+'</pre>');
      }
    });
  }

  function _delete() {
    var endpoint = $("#delete-path").val();

    var options = {
      method:'DELETE',
      endpoint:endpoint
    };
    client.request(options, function (err, data) {
      //data will contain raw results from API call
      if (err) {
        var output = JSON.stringify(data, null, 2);
        $("#response").html('<pre>ERROR: '+output+'</pre>');
      } else {
        var output = JSON.stringify(data, null, 2);
        $("#response").html('<pre>'+output+'</pre>');
      }
    });
  }

  function _login() {
    var username = $("#username").val();
    var password = $("#password").val();

    client.login(username, password, function (err, data) {
      //at this point, the user has been logged in succesfully and the OAuth token for the user has been stored
      //however, in this example, we don't want to use that token for the rest of the API calls, so we will now
      //reset it.  In your app, you will most likely not want to do this, as you are effectively logging the user
      //out.  Our calls work because we are going against the Sandbox app, which has no restrictions on permissions.
      client.token = null; //delete the user's token by setting it to null
      if (err) {
        var output = JSON.stringify(data, null, 2);
        $("#response").html('<pre>ERROR: '+output+'</pre>');
      } else {
        var output = JSON.stringify(data, null, 2);
        $("#response").html('<pre>'+output+'</pre>');
      }
    });
  }

});