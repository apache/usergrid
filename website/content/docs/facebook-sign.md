---
title: Facebook sign in
category: docs
layout: docs
---

Facebook sign in
================

You can authenticate your Apache Usergrid requests by logging into
Facebook. To access Apache Usergrid resources, you need to provide an
access token with each request (unless you use the sandbox app). You can
get an access token by connecting to an appropriate web service endpoint
and providing the correct client credentials — this is further described
in [Authenticating users and application
clients](/authenticating-users-and-application-clients). However, you
can also obtain an access token by logging into Facebook.

To enable authentication to Apache Usergrid through Facebook, do the
following in your app:

1.  Make a login call to the Facebook API (do this using the [Facebook
    SDK](https://developers.facebook.com/docs/sdks/) or
    [API](https://developers.facebook.com/docs/facebook-login/)). If the
    login succeeds, a Facebook access token is returned.
2.  Send the Facebook access token to Apache Usergrid. If the Facebook
    access token is valid and the user does not already exist in App
    Services, Apache Usergrid provisions a new Apache Usergrid user. It also
    returns an Apache Usergrid access token, which you can use for
    subsequent Apache Usergrid API calls. Behind the scenes, Apache Usergrid
    uses the Facebook access token to retrieve the user's profile
    information from Facebook.

    If the Facebook access token is invalid, Facebook returns an OAuth
    authentication error, and the login does not succeed.

The request to authenticate to Apache Usergrid using a Facebook access
token is:

    GET https://api.usergrid.com/{my_org}/{my_app}/auth/facebook?fb_access_token={fb_access_token}

where:

{my\_org} is the organization UUID or organization name.\
{my\_app} is the application UUID or application name.\
{fb\_access\_token} is the Facebook access token.

Facebook login example
----------------------

The [Facebook technical guides for
login](https://developers.facebook.com/docs/technical-guides/login/)
present detailed information on how to add Facebook login to your app.
Instructions are provided for JavaScript, iOS, and Android.

In brief, here are the steps for JavaScript. You can see these steps
implemented in the Facebook login example packaged with the JavaScript
SDK for Apache Usergrid (which you can download in
[ZIP](https://github.com/apigee/usergrid-javascript-sdk/archive/master.zip)
format or
[tar.gz](https://github.com/apigee/usergrid-javascript-sdk/archive/master.tar.gz)
format). The Facebook login example is in the /examples/facebook
directory of the extracted download. The code example snippets shown
below are taken from the Facebook login example.

### Step 1: Create a Facebook app

Create a new app on the [Facebook App
Dashboard](https://developers.facebook.com/apps/). Enter your app's
basic information. Once created, note the app ID shown at the top of the
dashboard page.

### Step 2: Invoke the Facebook OAuth dialog

Invoke the Facebook OAuth Dialog. To do that, redirect the user's
browser to a URL by inserting the following Javascript code after the
opening \<body\> tag in your app’s HTML file:

    https://www.facebook.com/dialog/oauth/?
        client_id={YOUR_APP_ID}
        &redirect_uri={YOUR_REDIRECT_URL}
        &state={YOUR_STATE_VALUE}
        &scope={COMMA_SEPARATED_LIST_OF_PERMISSION_NAMES}
        &response_type={YOUR_RESPONSE_TYPE}

where:

{YOUR\_APP\_ID} is the app ID.\
{YOUR\_REDIRECT\_URL} is the application UUID or application name.\
{YOUR\_STATE\_VALUE} is a unique string used to maintain application
state between the request and callback.\
{COMMA\_SEPARATED\_LIST\_OF\_PERMISSION\_NAMES} is a comma separated
list of permission names which you would like the user to grant your
application.\
{YOUR\_RESPONSE\_TYPE}is the requested response type, either code or
token. Defaults to code. Set the response type to token. With the
response type set to token, the Dialog's response will include an OAuth
user access token in the fragment of the URL the user is redirected to,
as per the client-side authentication flow.

Here is how it’s done in the Facebook login example:

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

Notice that the response type is set to token. As a result, a Facebook
access token will be appended to the URL to which the user is
redirected.

### Step 3: Add the JavaScript SDK for Facebook

Add the following Javascript SDK initialization code after the code that
invokes the Facebook OAuth Dialog. The code will load and initialize the
JavaScript SDK in your HTML page. Replace YOUR\_APP\_ID with the App ID
noted in Step 1, and WWW.YOUR\_DOMAIN.COM with your own domain.

    window.fbAsyncInit = function() {
        FB.init({
          appId      : 'YOUR_APP_ID', // App ID
          channelUrl : '//WWW.YOUR_DOMAIN.COM/channel.html', // Channel File
          status     : true, // check login status
          cookie     : true, // enable cookies to allow the server to access the session
          xfbml      : true  // parse XFBML
        });

Here is how the window.fbAsynchInit() function is implemented in the
Facebook login example:

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

### Step 4. Setup FB.login

Whenever a user is either not logged into Facebook or not authorized for
an app, it is useful to prompt them with the relevant dialog. The
FB.login() Javascript SDK function automatically displays the correct
one to the user.

To integrate FB.login()Fwindow.fbAsyncInit() function in your existing
code:

    function login() {
        FB.login(function(response) {
            if (response.authResponse) {
                // connected
            } else {
                // cancelled
            }
        });
    }

Here is how window.fbAsynchInit()FB.login() is implemented in the
Facebook login example:

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

The client.loginFacebook() function is provided by the Apache Usergrid
JavaScript SDK. It uses the Facebook auth token to obtain an App
Services auth token. If the Facebook access token is valid and the user
does not already exist in Apache Usergrid, the function creates a user
entity for the user. It also uses the Facebook access token to retrieve
the user's profile information from Facebook.

Here is what the client.loginFacebook() function looks like:

    Usergrid.Client.prototype.loginFacebook = function (facebookToken, callback) {
      var self = this;
      var options = {
        method:'GET',
        endpoint:'auth/facebook',
        qs:{
          fb_access_token: facebookToken
        }
      };
      this.request(options, function(err, data) {
        var user = {};
        if (err && self.logging) {
          console.log('error trying to log user in');
        } else {
          user = new Usergrid.Entity('users', data.user);
          self.setToken(data.access_token);
        }
        if (typeof(callback) === 'function') {
          callback(err, data, user);
        }
      });
    }

Notice that the function also returns an Apache Usergrid access token,
which you can use for subsequent Apache Usergrid API calls.

Remember to create a client for your app, which is the main entry point
to the JavaScript SDK for Apache Usergrid. You need to do this before you
can use the SDK. Here’s the code to create a client:

    var client = new Usergrid.Client({
        orgName:'yourorgname',
        appName:'yourappname',
        logging: true, //optional - turn on logging, off by default
        buildCurl: true //optional - turn on curl commands, off by default
    });
