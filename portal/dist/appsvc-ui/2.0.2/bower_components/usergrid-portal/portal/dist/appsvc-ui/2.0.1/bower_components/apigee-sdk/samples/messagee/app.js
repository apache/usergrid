/**
 *  Messagee is a sample twitter-type app that is powered by Usergrid
 *  Usergrid.  This app shows how to use the Usergrid SDK to connect
 *  to Usergrid, and how to store and retrieve data using Collections.
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
 *  This file contains the main program logic for Messagee.
 */
$(document).ready(function () {

  /*******************************************************************
  * create client and set up vars
  ********************************************************************/
  
    if("undefined"===typeof APIGEE_ORGNAME){
        APIGEE_ORGNAME=prompt("What is the Organization Name you registered at http://apigee.com/usergrid ?\n(you can set this permanently in config.js)", "ORG NAME");
    }
    if("undefined"===typeof APIGEE_APPNAME){
        APIGEE_APPNAME="sandbox";
    }
  var client_creds = {
    orgName:APIGEE_ORGNAME, //your orgname goes here (not case sensitive)
    appName:APIGEE_APPNAME, //your appname goes here (not case sensitive)
    logging: true, //optional - turn on logging, off by default
    buildCurl: true //optional - turn on curl commands, off by default
  };
  //Instantiates the Apigee data client and Apigee.MonitoringClient
  var client = new Apigee.Client(client_creds); 

  var appUser;
  var fullFeedView = true;
  var fullActivityFeed;
  var userFeed;

  /*******************************************************************
  * bind the various click events
  ********************************************************************/
  $('#btn-login').bind('click', login);
  $('#btn-show-page-update-account').bind('click', pageUpdateAccount);
  $('#btn-logout').bind('click', logout);
  $('#btn-create-new-account').bind('click', createNewUser);
  $('#btn-update-account').bind('click', updateUser);
  $('#btn-show-my-feed').bind('click', showMyFeed);
  $('#btn-show-full-feed').bind('click', showFullFeed);
  $('#btn-show-create-message').bind('click', function() {;
    $("#content").val('');
    $("#content").focus();
  });
  $('#post-message').bind('click', postMessage);

  //bind the next and previous buttons
  $('#btn-previous').bind('click', function() {
    if (fullFeedView) {
      fullActivityFeed.getPreviousPage(function (err) {
        if (err) {
          alert('Could not get feed. Please try again.');
        } else {
          drawMessages(fullActivityFeed);
        }
      });
    } else {
      userFeed.getPreviousPage(function (err) {
        if (err) {
          alert('Could not get feed. Please try again.');
        } else {
          drawMessages(userFeed);
        }
      });
    }
  });

  $('#btn-next').bind('click', function() {
    if (fullFeedView) {
      fullActivityFeed.getNextPage(function (err) {
        if (err) {
          alert('Could not get feed. Please try again.');
        } else {
          drawMessages(fullActivityFeed);
        }
      });
    } else {
      userFeed.getNextPage(function (err) {
        if (err) {
          alert('Could not get feed. Please try again.');
        } else {
          drawMessages(userFeed);
        }
      });
    }
  });


  /*******************************************************************
  * default actions for page load
  ********************************************************************/
  //if the app was somehow loaded on another page, default to the login page
  window.location = "#page-login";

  //when the page loads, try to get the current user.  If a token is
  //stored and it is valid, then don't make them log in again
  client.getLoggedInUser(function(err, data, user) {
    if(err) {
      //error - could not get logged in user
    } else {
      if (client.isLoggedIn()){
        appUser = user;
        showFullFeed();
      }
    }
  });

  /*******************************************************************
  * main program functions
  ********************************************************************/

  /**
   *  function to log in the app user.  The API returns a token,
   *  which is stored in the client and used for all future
   *  calls.  We pass a username, password, and a callback function
   *  which is called when the api call returns (asynchronously).
   *
   *  Once the call is sucessful, we transition the user to the page
   *  that displays the list of messages.
   *
   *  @method login
   *  @return none
   */
  function login() {
    $('#login-section-error').html('');
    var username = $("#username").val();
    var password = $("#password").val();

    client.login(username, password,
      function (err) {
        if (err) {
          $('#login-section-error').html('There was an error logging you in.');
        } else {
          //login succeeded
          client.getLoggedInUser(function(err, data, user) {
            if(err) {
              //error - could not get logged in user
            } else {
              if (client.isLoggedIn()){
                appUser = user;
               // showFullFeed();
              }
            }
          });

          //clear out the login form so it is empty if the user chooses to log out
          $("#username").val('');
          $("#password").val('');

          //default to the full feed view (all messages in the system)
          showMyFeed();
        }
      }
    );
  }

  /**
   *  a simple function to verify if the user is logged in
   *
   *  @method isLoggedIn
   *  @return {bool}
   */
  function isLoggedIn() {
    if (!client.isLoggedIn()) {
      window.location = "#page-login";
      return false;
    }
    return true
  }

  /**
   * simple funciton to log out the app user, then return them to the login page
   *
   * @method logout
   * @return none
   */
  function logout() {
    client.logout();
    window.location = "#page-login";
  }

  /**
   *  Function that is called when the user clicks the settings button to
   *  see their account details.  We populate them with what we have on hand
   *
   *  @method pageUpdateAccount
   *  @return none
   */
  function pageUpdateAccount(){
    $("#update-name").val(appUser.get('name'));
    $("#update-email").val(appUser.get('email'));
    $("#update-username").val(appUser.get('username'));
  }

  /**
   *  Function to handle the create new user form submission.
   *
   *  First we make sure there are no errors on the form (in case they
   *  submitted prior and have corrected some data).
   *  Next, we get all the new data out of the form, validate it, then
   *  call the createEntity function to send it to the API
   *
   *  @method createNewUser
   *  @return none
   */
  function createNewUser() {

    $("#new-name").removeClass('error');
    $("#new-email").removeClass('error');
    $("#new-username").removeClass('error');
    $("#new-password").removeClass('error');

    var name     = $("#new-name").val();
    var email    = $("#new-email").val();
    var username = $("#new-username").val();
    var password = $("#new-password").val();

    if (Usergrid.validation.validateName(name, function (){
          $("#new-name").focus();
          $("#new-name").addClass('error');}) &&
        Usergrid.validation.validateEmail(email, function (){
          $("#new-email").focus();
          $("#new-email").addClass('error');})  &&
        Usergrid.validation.validateUsername(username, function (){
          $("#new-username").focus();
          $("#new-username").addClass('error');})  &&
         Usergrid.validation.validatePassword(password, function (){
          $("#new-password").focus();
           $("#new-password").addClass('error');})  ) {
      // build the options object to pass to the create entity function
      var options = {
        type:'users',
        username:username,
        password:password,
        name:name,
        email:email
      }

      client.createEntity(options, function (err, newUser) {
        if (err){
           window.location = "#login";
          $('#login-section-error').html('There was an error creating the new user.');
        } else {
          appUser = newUser;
          //new user is created, so set their values in the login form and call login
          $("#username").val(username);
          $("#password").val(password);
          login();
        }
      });
    }
  }

  /**
   *  Function to handle the update user form submission.
   *
   *  First we make sure there are no errors on the form (in case they
   *  submitted prior and have corrected some data).
   *  Next, we get all the new data out of the form, validate it, set
   *  it in the user Entity, then call the save function to send it to the API
   *
   *  @method updateUser
   *  @return none
   */
  function updateUser() {

    $("#update-name").removeClass('error');
    $("#update-email").removeClass('error');
    $("#update-username").removeClass('error');
    $("#update-oldpassword").removeClass('error');
    $("#update-newpassword").removeClass('error');

    var name        = $("#update-name").val();
    var email       = $("#update-email").val();
    var username    = $("#update-username").val();
    var oldpassword = '';
    var newpassword = '';
    if (username != "myuser") {
      var oldpassword = $("#update-oldpassword").val();
      var newpassword = $("#update-newpassword").val();
    }
    if (Usergrid.validation.validateName(name, function (){
          $("#update-name").focus();
          $("#update-name").addClass('error');}) &&
        Usergrid.validation.validateEmail(email, function (){
          $("#update-email").focus();
          $("#update-email").addClass('error');})  &&
        Usergrid.validation.validateUsername(username, function (){
          $("#update-username").focus();
          $("#update-username").addClass('error');})  &&
        (newpassword == '') ||
        Usergrid.validation.validatePassword(newpassword, function (){
          $("#update-newpassword").focus();
          $("#update-newpassword").addClass('error');})  ) {

      appUser.set({"name":name,"username":username,"email":email,"oldpassword":oldpassword, "newpassword":newpassword});
      appUser.save(function (err) {
        if (err) {
          window.location = "#login";
          $('#user-message-update-account').html('<strong>There was an error updating your account</strong>');
        } else {
          $('#user-message-update-account').html('<strong>Your account was updated</strong>');
        }
      });
    }
  }

  /**
   *  Function to get the user's feed from the API
   *
   *  First make sure the user is logged in, then we make sure we are on
   *  the messages list page.
   *
   *  Next, we reset the paging of the feed object, so that user will
   *  see the first page of the feed
   *
   *  Next, we check to see if the feed object exists.  If so, we we do a
   *  get on the feed object, which makes a call to the
   *  API to retrieve the messages in the feed
   *
   *  On success, the drawMessages method is invoked
   *
   *  @method showMyFeed
   *  @return none
   */
  function showMyFeed() {
    if (!isLoggedIn()) return;

    //make sure we are on the messages page
    window.location = "#page-messages-list";

    fullFeedView = false;
    $('#btn-show-full-feed').removeClass('ui-btn-up-c');
    $('#btn-show-my-feed').addClass('ui-btn-up-c');

    if  (userFeed) {
      userFeed.resetPaging();
      userFeed.fetch(function (err) {
        if (err) {
          alert('Could not get user feed. Please try again.');
        } else {
          drawMessages(userFeed);
        }
      });
    } else {
      //no feed obj yet, so make a new one
      var options = {
        type:'user/me/feed',
        qs:{"ql":"order by created desc"}
      }
      client.createCollection(options, function(err, collectionObj){
        if (err) {
         alert('Could not get user feed. Please try again.');
        } else {
          userFeed = collectionObj;
          drawMessages(userFeed);
        }
      });
    }
  }

  /**
   *  Function to get the user's feed from the API
   *
   *  First make sure the user is logged in, then we make sure we are on
   *  the messages list page.
   *
   *  Next, we reset the paging of the feed object, so that user will
   *  see the first page of the feed
   *
   *  Next, we check to see if the feed object exists.  If so, we we do a
   *  get on the feed object, which makes a call to the
   *  API to retrieve the messages in the feed
   *
   *  On success, the drawMessages method is invoked
   *
   *  @method showFullFeed
   *  @return none
   */
  function showFullFeed() {
    if (!isLoggedIn()) return;

    //make sure we are on the messages page
    window.location = "#page-messages-list";

    fullFeedView = true;
    $('#btn-show-full-feed').addClass('ui-btn-up-c');
    $('#btn-show-my-feed').removeClass('ui-btn-up-c');


    if  (fullActivityFeed) {
      fullActivityFeed.resetPaging();
      fullActivityFeed.fetch(function (err) {
        if (err) {
          alert('Could not get activity feed. Please try again.');
        } else {
          drawMessages(fullActivityFeed);
        }
      });
    } else {
      var options = {
        type:'activities',
        qs:{"ql":"order by created desc"}
      }
      //no feed obj yet, so make a new one
      client.createCollection(options, function(err, collectionObj){
        if (err) {
          alert('Could not get activity feed. Please try again.');
        } else {
          fullActivityFeed = collectionObj;
          drawMessages(fullActivityFeed);
        }
      });
    }
  }

  /**
   *  Function to parse the messages of the feed
   *
   *  First, we create an array that will hold a the username of each person
   *  who posted a message.  We will use this to bind click events for the
   *  "follow" feature on the page.  We will set up the click events at the end of the page refresh
   *
   *  At the end of the method, we show the next and previous buttons if applicable
   *
   *  @method drawMessages
   *  @param {object} feed -a Collection object
   *  @return none
   *
   */
  function drawMessages(feed) {
    var html = "";
    var usersToBind = [];
    feed.resetEntityPointer();
    while(feed.hasNextEntity()) {
      var message = feed.getNextEntity(),
      created = message.get('created'),
      content = message.get('content'),
      email = '',
      imageUrl = '',
      actor = message.get('actor'),
      name = actor.displayName || 'Anonymous',
      username = actor.displayName;

      if ('email' in actor) {
        email = actor.email;
        imageUrl = 'http://www.gravatar.com/avatar/' + MD5(email.toLowerCase()) + '?s=' + 50;
      }
      if (!email) {
        if ('image' in actor && 'url' in actor.image) {
          imageUrl = actor.image.url;
        }
      }
      if (!imageUrl) {
        imageUrl = 'http://www.gravatar.com/avatar/' + MD5('rod@apigee.com') + '?s=' + 50;
      }

      formattedTime = prettyDate(created);

      html += '<div style="border-bottom: 1px solid #444; padding: 5px; min-height: 60px;"><img src="' + imageUrl + '" style="border none; height: 50px; width: 50px; float: left;padding-right: 10px"> ';
      html += '<span style="float: right">'+formattedTime+'</span>';
      html += '<strong>' + name + '</strong>';
      if (username && username != appUser.get('username')) {
        html += '(<a href="#page-now-following" id="'+created+'" name="'+username+'" data-role="button" data-rel="dialog" data-transition="fade">Follow</a>)';
      }
      html += '<br><span>' + content + '</span> <br>';
      html += '</div>';
      usersToBind[created] = username;
    }
    if (html == "") { html = "No messages yet!"; }
    $("#messages-list").html(html);
    for(user in usersToBind) {
      $('#'+user).bind('click', function(event) {
        username = event.target.name;
        followUser(username);
      });
    }
    //next show the next / previous buttons
    if (feed.hasPreviousPage()) {
      $("#previous-btn-container").show();
    } else {
      $("#previous-btn-container").hide();
    }
    if (feed.hasNextPage()) {
      $("#next-btn-container").show();
    } else {
      $("#next-btn-container").hide();
    }

    $(this).scrollTop(0);
  }

  /**
   *  Method to create the following relationship between two users
   *
   *  @method followUser
   *  @param username - user to follow
   *  @return none
   *
   */
  function followUser(username) {
    if (!isLoggedIn()) return;

    //reset paging so we make sure our results start at the beginning
    fullActivityFeed.resetPaging();
    userFeed.resetPaging();

    var options = {
      method:'POST',
      endpoint:'users/me/following/users/' + username
    };
    client.request(options, function (err, data) {
      if (err) {
        $('#now-following-text').html('Aw Shucks!  There was a problem trying to follow <strong>' + username + '</strong>');
      } else {
        $('#now-following-text').html('Congratulations! You are now following <strong>' + username + '</strong>');
        showMyFeed();
      }
    });
  }

  /**
   *  Method to handle the create message form submission.  The
   *  method gets the content from the form, then builds the options
   *  object by using the data from the logged in user. The new activity
   *  is then saved to the database using the createUserActivity method.
   *
   *  Finally once the message has been saved, we refresh the user's feed
   *  based in which "mode" they are viewing feeds in - user or full.
   *
   *  @method postMessage
   *  @return none
   */
  function postMessage() {
    if (!isLoggedIn()) return;

    var options =
    {"actor" : {
      "displayName" : appUser.get('username'),
      "uuid" : appUser.get('uuid'),
      "username" : appUser.get('username'),
      "image" : {
        "duration" : 0,
        "height" : 80,
        "url" : "http://www.gravatar.com/avatar/",
        "width" : 80
      },
      "email" : appUser.get('email'),
      "picture": "fred"
    },
    "verb" : "post",
    "content" : $("#content").val(),
    "lat" : 48.856614,
    "lon" : 2.352222};

    client.createUserActivity('me', options, function(err, activity) { //first argument can be 'me', a uuid, or a username
      if (err) {
        alert('could not post message');
      } else {
         if (fullFeedView) {
          //reset the feed object so when we view it again, we will get the latest feed
          fullActivityFeed.resetPaging();
          showFullFeed();
        } else {
          //reset the feed object so when we view it again, we will get the latest feed
          userFeed.resetPaging();
          showMyFeed();
        }
        window.location = "#page-messages-list";
      }
    });
  }

  /**
   *  The following code is used to display the posted dates as "x minutes ago, etc"
   *  instead of just a date.
   *
   *  Thank you John Resig and long live JQuery!
   *
   * JavaScript Pretty Date
   * Copyright (c) 2011 John Resig (ejohn.org)
   * Licensed under the MIT and GPL licenses.
   */

  // Takes a numeric date value (in seconds) and returns a string
  // representing how long ago the date represents.
  function prettyDate(createdDateValue) {
    var diff = (((new Date()).getTime() - createdDateValue) / 1000)
    var day_diff = Math.floor(diff / 86400);

    if ( isNaN(day_diff) || day_diff < 0 || day_diff >= 31 )
      return 'just now';

    return fred = day_diff == 0 && (
      diff < 60 && "just now" ||
      diff < 120 && "1 minute ago" ||
      diff < 3600 && Math.floor( diff / 60 ) + " minutes ago" ||
      diff < 7200 && "1 hour ago" ||
      diff < 86400 && Math.floor( diff / 3600 ) + " hours ago") ||
      day_diff == 1 && "Yesterday" ||
      day_diff < 7 && day_diff + " days ago" ||
      day_diff < 31 && Math.ceil( day_diff / 7 ) + " weeks ago";
  }

  //MD5 function - used for parsing emails for Gravatar images
  var MD5=function(s){function L(k,d){return(k<<d)|(k>>>(32-d))}function K(G,k){var I,d,F,H,x;F=(G&2147483648);H=(k&2147483648);I=(G&1073741824);d=(k&1073741824);x=(G&1073741823)+(k&1073741823);if(I&d){return(x^2147483648^F^H)}if(I|d){if(x&1073741824){return(x^3221225472^F^H)}else{return(x^1073741824^F^H)}}else{return(x^F^H)}}function r(d,F,k){return(d&F)|((~d)&k)}function q(d,F,k){return(d&k)|(F&(~k))}function p(d,F,k){return(d^F^k)}function n(d,F,k){return(F^(d|(~k)))}function u(G,F,aa,Z,k,H,I){G=K(G,K(K(r(F,aa,Z),k),I));return K(L(G,H),F)}function f(G,F,aa,Z,k,H,I){G=K(G,K(K(q(F,aa,Z),k),I));return K(L(G,H),F)}function D(G,F,aa,Z,k,H,I){G=K(G,K(K(p(F,aa,Z),k),I));return K(L(G,H),F)}function t(G,F,aa,Z,k,H,I){G=K(G,K(K(n(F,aa,Z),k),I));return K(L(G,H),F)}function e(G){var Z;var F=G.length;var x=F+8;var k=(x-(x%64))/64;var I=(k+1)*16;var aa=Array(I-1);var d=0;var H=0;while(H<F){Z=(H-(H%4))/4;d=(H%4)*8;aa[Z]=(aa[Z]|(G.charCodeAt(H)<<d));H++}Z=(H-(H%4))/4;d=(H%4)*8;aa[Z]=aa[Z]|(128<<d);aa[I-2]=F<<3;aa[I-1]=F>>>29;return aa}function B(x){var k="",F="",G,d;for(d=0;d<=3;d++){G=(x>>>(d*8))&255;F="0"+G.toString(16);k=k+F.substr(F.length-2,2)}return k}function J(k){k=k.replace(/rn/g,"n");var d="";for(var F=0;F<k.length;F++){var x=k.charCodeAt(F);if(x<128){d+=String.fromCharCode(x)}else{if((x>127)&&(x<2048)){d+=String.fromCharCode((x>>6)|192);d+=String.fromCharCode((x&63)|128)}else{d+=String.fromCharCode((x>>12)|224);d+=String.fromCharCode(((x>>6)&63)|128);d+=String.fromCharCode((x&63)|128)}}}return d}var C=Array();var P,h,E,v,g,Y,X,W,V;var S=7,Q=12,N=17,M=22;var A=5,z=9,y=14,w=20;var o=4,m=11,l=16,j=23;var U=6,T=10,R=15,O=21;s=J(s);C=e(s);Y=1732584193;X=4023233417;W=2562383102;V=271733878;for(P=0;P<C.length;P+=16){h=Y;E=X;v=W;g=V;Y=u(Y,X,W,V,C[P+0],S,3614090360);V=u(V,Y,X,W,C[P+1],Q,3905402710);W=u(W,V,Y,X,C[P+2],N,606105819);X=u(X,W,V,Y,C[P+3],M,3250441966);Y=u(Y,X,W,V,C[P+4],S,4118548399);V=u(V,Y,X,W,C[P+5],Q,1200080426);W=u(W,V,Y,X,C[P+6],N,2821735955);X=u(X,W,V,Y,C[P+7],M,4249261313);Y=u(Y,X,W,V,C[P+8],S,1770035416);V=u(V,Y,X,W,C[P+9],Q,2336552879);W=u(W,V,Y,X,C[P+10],N,4294925233);X=u(X,W,V,Y,C[P+11],M,2304563134);Y=u(Y,X,W,V,C[P+12],S,1804603682);V=u(V,Y,X,W,C[P+13],Q,4254626195);W=u(W,V,Y,X,C[P+14],N,2792965006);X=u(X,W,V,Y,C[P+15],M,1236535329);Y=f(Y,X,W,V,C[P+1],A,4129170786);V=f(V,Y,X,W,C[P+6],z,3225465664);W=f(W,V,Y,X,C[P+11],y,643717713);X=f(X,W,V,Y,C[P+0],w,3921069994);Y=f(Y,X,W,V,C[P+5],A,3593408605);V=f(V,Y,X,W,C[P+10],z,38016083);W=f(W,V,Y,X,C[P+15],y,3634488961);X=f(X,W,V,Y,C[P+4],w,3889429448);Y=f(Y,X,W,V,C[P+9],A,568446438);V=f(V,Y,X,W,C[P+14],z,3275163606);W=f(W,V,Y,X,C[P+3],y,4107603335);X=f(X,W,V,Y,C[P+8],w,1163531501);Y=f(Y,X,W,V,C[P+13],A,2850285829);V=f(V,Y,X,W,C[P+2],z,4243563512);W=f(W,V,Y,X,C[P+7],y,1735328473);X=f(X,W,V,Y,C[P+12],w,2368359562);Y=D(Y,X,W,V,C[P+5],o,4294588738);V=D(V,Y,X,W,C[P+8],m,2272392833);W=D(W,V,Y,X,C[P+11],l,1839030562);X=D(X,W,V,Y,C[P+14],j,4259657740);Y=D(Y,X,W,V,C[P+1],o,2763975236);V=D(V,Y,X,W,C[P+4],m,1272893353);W=D(W,V,Y,X,C[P+7],l,4139469664);X=D(X,W,V,Y,C[P+10],j,3200236656);Y=D(Y,X,W,V,C[P+13],o,681279174);V=D(V,Y,X,W,C[P+0],m,3936430074);W=D(W,V,Y,X,C[P+3],l,3572445317);X=D(X,W,V,Y,C[P+6],j,76029189);Y=D(Y,X,W,V,C[P+9],o,3654602809);V=D(V,Y,X,W,C[P+12],m,3873151461);W=D(W,V,Y,X,C[P+15],l,530742520);X=D(X,W,V,Y,C[P+2],j,3299628645);Y=t(Y,X,W,V,C[P+0],U,4096336452);V=t(V,Y,X,W,C[P+7],T,1126891415);W=t(W,V,Y,X,C[P+14],R,2878612391);X=t(X,W,V,Y,C[P+5],O,4237533241);Y=t(Y,X,W,V,C[P+12],U,1700485571);V=t(V,Y,X,W,C[P+3],T,2399980690);W=t(W,V,Y,X,C[P+10],R,4293915773);X=t(X,W,V,Y,C[P+1],O,2240044497);Y=t(Y,X,W,V,C[P+8],U,1873313359);V=t(V,Y,X,W,C[P+15],T,4264355552);W=t(W,V,Y,X,C[P+6],R,2734768916);X=t(X,W,V,Y,C[P+13],O,1309151649);Y=t(Y,X,W,V,C[P+4],U,4149444226);V=t(V,Y,X,W,C[P+11],T,3174756917);W=t(W,V,Y,X,C[P+2],R,718787259);X=t(X,W,V,Y,C[P+9],O,3951481745);Y=K(Y,h);X=K(X,E);W=K(W,v);V=K(V,g)}var i=B(Y)+B(X)+B(W)+B(V);return i.toLowerCase()};

});

//abudda abudda abudda that's all folks!
