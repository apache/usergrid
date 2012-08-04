/**
 *  App SDK is a collection of classes designed to make working with
 *  the Appigee App Services API as easy as possible.
 *  Learn more at http://apigee.com/docs
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

//define the console.log for IE
window.console = window.console || {};
window.console.log = window.console.log || function() {};

//apigee namespace encapsulates this SDK
window.apigee = window.apigee || {};
apigee = apigee || {};
apigee.SDK_VERSION = '0.9.1';

/*
 *  apigee.ApiClient
 *
 *  A Singleton that is the main client for making calls to the API. Maintains
 *  state between calls for the following items:
 *
 *  Token
 *  Organization Name
 *  Application Name
 *  Application User userame
 *  Application User email
 *  Application User UUID
 *
 *  Main methods for making calls to the API are:
 *
 *  runAppQuery (queryObj)
 *  runManagementQuery(queryObj)
 *
 *  Create a new apigee.QueryObject and then pass it to either of these
 *  two methods for making calls directly to the API.
 *
 *  Several convenience methods exist for making app user related calls easier.  These are:
 *
 *  loginAppUser (username, password, successCallback, failureCallback)
 *  updateAppUser(uuid, name, email, username, oldpassword, newpassword, data, successCallback, failureCallback)
 *  createAppUser(name, email, username, password, data, successCallback, failureCallback)
 *
 *  @class apigee.ApiClient
 *  @author Rod Simpson (rod@apigee.com)
 *
 */
apigee.ApiClient = (function () {
  //API endpoint
  var _apiUrl = "http://api.usergrid.com/";
  //var _apiUrl = "http://ug-developer-testing.elasticbeanstalk.com/";
  var _orgName = null;
  var _orgUUID = null;
  var _appName = null;
  var _token = null;
  var _appUserUsername = null;
  var _appUserName = null;
  var _appUserEmail = null;
  var _appUserUUID = null;

  /*
   *  A method to set up the ApiClient with orgname and appname
   *  @method init
   *  @param {string} orgName
   *  @param {string} appName
   *  @return none
   *
   */
  function init(orgName, appName){
    _orgName = orgName;
    _appName = appName;
  }

  /*
    *  A public method to get the organization name to be used by the client
    *  @method getOrganizationName
    *  @return {string} the organization name
    */
  function getOrganizationName() {
    return _orgName;
  }

  /*
    *  A public method to set the organization name to be used by the client
    *  @method setOrganizationName
    *  @param orgName - the organization name
    *  @return none
    */
  function setOrganizationName(orgName) {
    _orgName = orgName;
  }

  /*
    *  A public method to get the organization UUID to be used by the client
    *  @method getOrganizationUUID
    *  @return {string} the organization UUID
    */
  function getOrganizationUUID() {
    return _orgUUID;
  }

  /*
    *  A public method to set the organization UUID to be used by the client
    *  @method setOrganizationUUID
    *  @param orgUUID - the organization UUID
    *  @return none
    */
  function setOrganizationUUID(orgUUID) {
    _orgUUID = orgUUID;
  }

  /*
  *  A public method to get the application name to be used by the client
  *  @method getApplicationName
  *  @return {string} the application name
  */
  function getApplicationName() {
    return _appName;
  }

  /*
  *  A public method to set the application name to be used by the client
  *  @method setApplicationName
  *  @param appName - the application name
  *  @return none
  */
  function setApplicationName(appName) {
    _appName = appName;
  }

  /*
  *  A public method to get the token to be used by the client
  *  @method getToken
  *  @return {string} the current token
  */
  function getToken() {
    return _token;
  }

  /*
  *  A public method to set the token to be used by the client
  *  @method setToken
  *  @param token - the bearer token
  *  @return none
  */
  function setToken(token) {
    _token = token;
  }

  /*
    *  A public method to get the app user's username to be used by the client
    *  @method getAppUserUsername
    *  @return {string} the app user's username
    */
  function getAppUserUsername() {
    return _appUserUsername;
  }

  /*
    *  A public method to set the app user's username to be used by the client
    *  @method setAppUserUsername
    *  @param appUserUsername - the app user's username
    *  @return none
    */
  function setAppUserUsername(appUserUsername) {
    _appUserUsername = appUserUsername;
  }

  /*
    *  method to get the app user's name to be used by the client
    *  @method getAppUserName
    *  @return {string} the app user's name
    */
  function getAppUserFullName() {
    return _appUserName;
  }

  /*
    *  A public method to set the app user's name to be used by the client
    *  @method setAppUserName
    *  @param appUserName - the app user's name
    *  @return none
    */
  function setAppUserFullName(appUserName) {
    _appUserName = appUserName;
  }

  /*
    *  A public method to get the app user's email to be used by the client
    *  @method getAppUserEmail
    *  @return {string} the app user's email
    */
  function getAppUserEmail() {
    return _appUserEmail;
  }

  /*
    *  A public method to set the app user's email to be used by the client
    *  @method setAppUserEmail
    *  @param appUserEmail - the app user's email
    *  @return none
    */
  function setAppUserEmail(appUserEmail) {
    _appUserEmail = appUserEmail;
  }

  /*
    *  A public method to get the app user's UUID to be used by the client
    *  @method getAppUserUUID
    *  @return {string} the app users' UUID
    */
  function getAppUserUUID() {
    return _appUserUUID;
  }

  /*
    *  A public method to set the app user's UUID to be used by the client
    *  @method setAppUserUUID
    *  @param appUserUUID - the app user's UUID
    *  @return none
    */
  function setAppUserUUID(appUserUUID) {
    _appUserUUID = appUserUUID;
  }

  /*
  *  A public method to return the API URL
  *  @method getApiUrl
  *  @return {string} the API url
  */
  function getApiUrl() {
    return _apiUrl
  }

  /*
  *  A public method to overide the API url
  *  @method setApiUrl
  *  @return none
  */
  function setApiUrl(apiUrl) {
    _apiUrl = apiUrl;
  }

  /*
  *  A public method to get the api url of the reset pasword endpoint
  *  @method getResetPasswordUrl
  *  @return {string} the api rul of the reset password endpoint
  */
  function getResetPasswordUrl() {
    this.getApiUrl() + "/management/users/resetpw"
  }

  /*
  *  A public method to run calls against the app endpoint
  *  @method runAppQuery
  *  @params {object} apigee.QueryObj - {method, path, jsonObj, params, successCallback, failureCallback}
  *  @return none
  */
  function runAppQuery (QueryObj) {
    var endpoint = "/" + this.getOrganizationName() + "/" + this.getApplicationName() + "/";
    this.processQuery(QueryObj, endpoint);
  }

  /*
  *  A public method to run calls against the management endpoint
  *  @method runManagementQuery
  *  @params {object} apigee.QueryObj - {method, path, jsonObj, params, successCallback, failureCallback}
  *  @return none
  */
  function runManagementQuery (QueryObj) {
    var endpoint = "/management/";
    this.processQuery(QueryObj, endpoint)
  }

  /*
  *  A public method to log in an app user - stores the token for later use
  *  @method loginAppUser
  *  @params {string} username
  *  @params {string} password
  *  @params {function} successCallback
  *  @params {function} failureCallback
  *  @return {response} callback functions return API response object
  */
  function loginAppUser (username, password, successCallback, failureCallback) {
    var self = this;
    var data = {"username": username, "password": password, "grant_type": "password"};
    this.runAppQuery(new apigee.QueryObj('GET', 'token', null, data,
      function (response) {
        self.setAppUserUsername(response.user.username);
        self.setAppUserFullName(response.user.name);
        self.setAppUserFullName(response.user.givenName + response.user.familyName);
        self.setAppUserEmail(response.user.email);
        self.setAppUserUUID(response.user.uuid);
        self.setToken(response.access_token);
        if (successCallback && typeof(successCallback) == "function") {
          successCallback(response);
        }
      },
      function (response) {
        if (failureCallback && typeof(failureCallback) == "function") {
          failureCallback(response);
        }
      }
     ));
  }

  /*
  *  A public method to update an app user - currently does password update and user
  *  data update as two separate calls, but this will be changed to one call when the
  *  API is updated to support this
  *  @method updateAppUser
  *  @params {string} uuid
  *  @params {string} name
  *  @params {string} email
  *  @params {string} username
  *  @params {string} oldpassword
  *  @params {string} newpassword
  *  @params {function} successCallback
  *  @params {function} failureCallback
  *  @return {response} callback functions return API response object
  */
  function updateAppUser(uuid, name, email, username, oldpassword, newpassword, data, successCallback, failureCallback) {
    var self = this;
    var data = data || {}
    data.username = username;
    data.email = email;
    data.name = name;

    var pwdata = {};
    if (oldpassword) { pwdata.oldpassword = oldpassword; }
    if (newpassword) { pwdata.newpassword = newpassword; }
    //Note: we have ticket in to change PUT calls to /users to accept the password change
    //      once that is done, we will remove this call and merge it all into one
    if (oldpassword && newpassword) {
      this.runAppQuery(new apigee.QueryObj('PUT', 'users/'+uuid+'/password', pwdata, null,
        function (response) {

        },
        function (response) {

        }
      ));
    }
    this.runAppQuery(new apigee.QueryObj('PUT', 'users/'+uuid+'', data, null,
      function (response) {
        var user = response.entities[0];
        self.setAppUserUsername(user.username);
        self.setAppUserFullName(user.givenName + user.familyName);
        self.setAppUserEmail(user.email);
        self.setAppUserUUID(user.uuid);
        if (successCallback && typeof(successCallback) == "function") {
          successCallback(response);
        }
      },
      function (response) {
        if (failureCallback && typeof(failureCallback) == "function") {
          failureCallback(response);
        }
      }
     ));
  }

  /**
   *  A public method to create an app user
   *  @method createAppUser
   *  @param {string} name
   *  @param {string} email
   *  @param {string} username
   *  @param {string} password
   *  @param {object} data
   *  @param {function} successCallback
   *  @param {function} failureCallback
   *  @return {response} callback functions return API response object
   */
  function createAppUser(name, email, username, password, data, successCallback, failureCallback) {
    var self = this;
    var data = data || {}
    data.username = username;
    data.password = password;
    data.email = email;
    data.name = name;
    this.runAppQuery(new apigee.QueryObj('POST', 'users', data, null,
      function (response) {
        var user = response.entities[0];
        self.setAppUserUsername(user.username);
        self.setAppUserFullName(user.givenName + user.familyName);
        self.setAppUserEmail(user.email);
        self.setAppUserUUID(user.uuid);
        if (successCallback && typeof(successCallback) == "function") {
          successCallback(response);
        }
      },
      function (response) {
        if (failureCallback && typeof(failureCallback) == "function") {
          failureCallback(response);
        }
      }
     ));
  }

  /*
   *  TODO:  NOT IMPLEMENTED YET - A method to renew an app user's token
   *  @method renewAppUserToken
   *  @return none
   */
  function renewAppUserToken() {

  }

  /**
   *  A public method to log out an app user - clears all user fields from client
   *  @method logoutAppUser
   *  @return none
   */
  function logoutAppUser() {
    this.setAppUserUsername(null);
    this.setAppUserFullName(null);
    this.setAppUserEmail(null);
    this.setAppUserUUID(null);
    this.setToken(null);
  }

  /**
   *  A public method to test if a user is logged in - does not guarantee that the token is still valid,
   *  but rather that one exists, and that there is a valid UUID
   *  @method isLoggedInAppUser
   *  @params {object} apigee.QueryObj - {method, path, jsonObj, params, successCallback, failureCallback}
   *  @return {boolean} Returns true the user is logged in (has token and uuid), false if not
   */
  function isLoggedInAppUser() {
    return (this.getToken() && apigee.validation.isUUID(this.getAppUserUUID()));
  }

  /**
   *  This private method should not be called directly!!
   *  It is the main function that validates and prepares a call to the API
   *  Use runAppQuery or runManagementQuery instead
   *  @method processQuery
   *  @params {object} apigee.QueryObj - {method, path, jsonObj, params, successCallback, failureCallback}
   *  @params {string} endpoint - used to differentiate between management and app queries
   *  @return {response} callback functions return API response object
   */
  function processQuery (QueryObj, endpoint) {
    var curl = "curl";
    //validate parameters
    try {
      //verify that the current rendering platform supports XMLHttpRequest
      if(typeof XMLHttpRequest === 'undefined') {
        throw(new Error('Ru-rho! XMLHttpRequest is not supported on this device.'));
      }
      //verify that the query object is valid
      if(!(QueryObj instanceof apigee.QueryObj)) {
        throw(new Error('QueryObj is not a valid object.'));
      }
      //peel the data out of the query object
      var method = QueryObj.getMethod().toUpperCase();
      var path = QueryObj.getPath();
      var jsonObj = QueryObj.getJsonObj() || {};
      var params = QueryObj.getQueryParams() || {};

      //method - should be GET, POST, PUT, or DELETE only
      if (method != 'GET' && method != 'POST' && method != 'PUT' && method != 'DELETE') {
        throw(new Error('Invalid method - should be GET, POST, PUT, or DELETE.'));
      }
      //curl - add the method to the command (no need to add anything for GET)
      if (method == "POST") {curl += " -X POST"; }
      else if (method == "PUT") { curl += " -X PUT"; }
      else if (method == "DELETE") { curl += " -X DELETE"; }
      else { curl += " -X GET"; }

      //curl - append the bearer token if this is not the sandbox app
      var application_name = this.getApplicationName();
      if (application_name) {
        application_name = application_name.toUpperCase();
      }
      if (application_name != 'SANDBOX' && this.getToken()) {
        curl += ' -i -H "Authorization: Bearer ' + this.getToken() + '"';
        QueryObj.setToken(true);
      }

      //params - make sure we have a valid json object
      _params = JSON.stringify(params)
      if (!JSON.parse(_params)) {
        throw(new Error('Params object is not valid.'));
      }

      //add in the cursor if one is available
      if (QueryObj.getCursor()) {
        params.cursor = QueryObj.getCursor();
      } else {
        delete params.cursor;
      }

      //strip off the leading slash of the endpoint if there is one
      endpoint = endpoint.indexOf('/') == 0 ? endpoint.substring(1) : endpoint;

      //add the endpoint to the path
      path = endpoint + path;

      //make sure path never has more than one / together
      if (path) {
        //regex to strip multiple slashes
        while(path.indexOf('//') != -1){
          path = path.replace('//', '/');
        }
      }

      //add the http:// bit on the front
      path = this.getApiUrl() + path;

      //curl - append the path
      curl += ' "' + path;

      //curl - append params to the path for curl prior to adding the timestamp
      var curl_encoded_params = this.encodeParams(params);
      if (curl_encoded_params) {
        curl += "?" + curl_encoded_params;
      }
      curl += '"';

      //add in a timestamp for gets and deletes - to avoid caching by the browser
      if ((method == "GET") || (method == "DELETE")) {
        params['_'] = new Date().getTime();
      }

      //append params to the path
      var encoded_params = this.encodeParams(params);
      if (encoded_params) {
        path += "?" + encoded_params;
      }

      //jsonObj - make sure we have a valid json object
      jsonObj = JSON.stringify(jsonObj)
      if (!JSON.parse(jsonObj)) {
        throw(new Error('JSON object is not valid.'));
      }
      if (jsonObj == '{}') {
        jsonObj = null;
      } else {
        //curl - add in the json obj
        curl += " -d '" + jsonObj + "'";
      }

    } catch (e) {
      //parameter was invalid
      console.log('processQuery - error occured -' + e.message);
      return false;
    }
    //log the curl command to the console
    console.log(curl);
    //store the curl command back in the object
    QueryObj.setCurl(curl);

    //so far so good, so run the query
    var xD = window.XDomainRequest ? true : false;
    var xM = window.XMLHttpRequest ? true : false;
    var xhr;

    if(xD)
    {
      xhr = new window.XDomainRequest();
      if (application_name != 'SANDBOX' && this.getToken()) {
        if (path.indexOf("?")) {
          path += '&access_token='+this.getToken();
        } else {
          path = '?access_token='+this.getToken();
        }
      }
      xhr.open(method, path, true);
    }
    else if (xM)
    {
      xhr = new XMLHttpRequest();
      xhr.open(method, path, true);
      if (application_name != 'SANDBOX' && this.getToken()) {
        xhr.setRequestHeader("Authorization", "Bearer " + this.getToken());
        xhr.withCredentials = true;
      }
    } else {
      xhr = new ActiveXObject("MSXML2.XMLHTTP.3.0");
      if (application_name != 'SANDBOX' && this.getToken()) {
        if (path.indexOf("?")) {
          path += '&access_token='+this.getToken();
        } else {
          path = '?access_token='+this.getToken();
        }
      }
      xhr.open(method, path, true);
    }

    // Handle response.
    xhr.onerror = function() {
      //network error
      clearTimeout(timeout);
      console.log('API call failed at the network level.');
      QueryObj.callFailureCallback({'error':'error'});
    };
    xhr.onload = function() {
      //call completed
      clearTimeout(timeout);
      response = JSON.parse(xhr.responseText);
      if (xhr.status != 200 && !xD)   {
        //there was an api error
        var error = response.error;
        console.log('API call failed: (status: '+xhr.status+').' + error.type);

        if ( (error.type == "auth_expired_session_token") ||
              (error.type == "auth_missing_credentials")   ||
              (error.type == "auth_invalid")) {
            //this error type means the user is not authorized. If a logout function is defined, call it
            if (apigee.console.logout) {
              apigee.console.logout();
              return;
            }
        }
        //otherwise, just call the failure callback
        QueryObj.callFailureCallback(response);
        return;
      } else {
        //success

        //query completed succesfully, so store cursor
        var cursor = response.cursor || null;
        QueryObj.saveCursor(cursor);
        //then call the original callback
        QueryObj.callSuccessCallback(response);
      }
    };
    var timeout = setTimeout(function() { xhr.abort(); }, 10000);

    xhr.send(jsonObj);
  }

  /**
   *  Private helper method to encode the query string parameters
   *  @method encodeParams
   *  @params {object} params - an object of name value pairs that will be urlencoded
   *  @return {string} Returns the encoded string
   */
  function encodeParams (params) {
    tail = [];
    var item = [];
    if (params instanceof Array) {
      for (i in params) {
        item = params[i];
        if ((item instanceof Array) && (item.length > 1)) {
          tail.push(item[0] + "=" + encodeURIComponent(item[1]));
        }
      }
    } else {
      for (var key in params) {
        if (params.hasOwnProperty(key)) {
          var value = params[key];
          if (value instanceof Array) {
            for (i in value) {
              item = value[i];
              tail.push(key + "=" + encodeURIComponent(item));
            }
          } else {
            tail.push(key + "=" + encodeURIComponent(value));
          }
        }
      }
    }
    return tail.join("&");
  }

  return {
    init:init,
    getOrganizationName:getOrganizationName,
    setOrganizationName:setOrganizationName,
    getOrganizationUUID:getOrganizationUUID,
    setOrganizationUUID:setOrganizationUUID,
    getApplicationName:getApplicationName,
    setApplicationName:setApplicationName,
    getToken:getToken,
    setToken:setToken,
    getAppUserUsername:getAppUserUsername,
    setAppUserUsername:setAppUserUsername,
    getAppUserFullName:getAppUserFullName,
    setAppUserFullName:setAppUserFullName,
    getAppUserEmail:getAppUserEmail,
    setAppUserEmail:setAppUserEmail,
    getAppUserUUID:getAppUserUUID,
    setAppUserUUID:setAppUserUUID,
    getApiUrl:getApiUrl,
    setApiUrl:setApiUrl,
    getResetPasswordUrl:getResetPasswordUrl,
    runAppQuery:runAppQuery,
    runManagementQuery:runManagementQuery,
    loginAppUser:loginAppUser,
    createAppUser:createAppUser,
    updateAppUser:updateAppUser,
    renewAppUserToken:renewAppUserToken,
    logoutAppUser:logoutAppUser,
    isLoggedInAppUser:isLoggedInAppUser,
    processQuery:processQuery,
    encodeParams:encodeParams
  }
})();


/**
 * validation is a Singleton that provides methods for validating common field types
 *
 * @class apigee.validation
 * @author Rod Simpson (rod@apigee.com)
**/
apigee.validation = apigee.validation || {};

apigee.validation = (function () {

  var usernameRegex = new RegExp("^([0-9a-zA-Z\.\-])+$");
  var nameRegex     = new RegExp("^([0-9a-zA-Z@#$%^&!?;:.,'\"~*-=+_\[\\](){}/\\ |])+$");
  var emailRegex    = new RegExp("^(([0-9a-zA-Z]+[_\+.-]?)+@[0-9a-zA-Z]+[0-9,a-z,A-Z,.,-]*(.){1}[a-zA-Z]{2,4})+$");
  var passwordRegex = new RegExp("^([0-9a-zA-Z@#$%^&!?<>;:.,'\"~*-=+_\[\\](){}/\\ |])+$");
  var pathRegex     = new RegExp("^([0-9a-z./-])+$");
  var titleRegex    = new RegExp("^([0-9a-zA-Z.!-?/])+$");

  /**
    * Tests the string against the allowed chars regex
    * @public
    * @method validateUsername
    * @param {string} username - The string to test
    * @param {function} failureCallback - (optional), the function to call on a failure
    * @return {boolean} Returns true if string passes regex, false if not
    */
  function validateUsername(username, failureCallback) {
    if (usernameRegex.test(username) && checkLength(username, 4, 80)) {
      return true;
    } else {
      if (failureCallback && typeof(failureCallback) == "function") {
        failureCallback(this.getUsernameAllowedChars());
      }
      return false;
    }
  }

  /**
    * Returns the regex of allowed chars
    * @public
    * @method getUsernameAllowedChars
    * @return {string} Returns a string with the allowed chars
    */
  function getUsernameAllowedChars(){
    return 'Length: min 6, max 80. Allowed: A-Z, a-z, 0-9, dot, and dash';
  }

  /**
    * Tests the string against the allowed chars regex
    * @public
    * @method validateName
    * @param {string} name - The string to test
    * @param {function} failureCallback - (optional), the function to call on a failure
    * @return {boolean} Returns true if string passes regex, false if not
    */
  function validateName(name, failureCallback) {
    if (nameRegex.test(name) && checkLength(name, 5, 16)) {
      return true;
    } else {
      if (failureCallback && typeof(failureCallback) == "function") {
        failureCallback(this.getNameAllowedChars());
      }
      return false;
    }
  }

  /**
    * Returns the regex of allowed chars
    * @public
    * @method getNameAllowedChars
    * @return {string} Returns a string with the allowed chars
    */
  function getNameAllowedChars(){
    return 'Length: min 4, max 80. Allowed: A-Z, a-z, 0-9, ~ @ # % ^ & * ( ) - _ = + [ ] { } \\ | ; : \' " , . / ? !';
  }

  /**
    * Tests the string against the allowed chars regex
    * @public
    * @method validatePassword
    * @param {string} password - The string to test
    * @param {function} failureCallback - (optional), the function to call on a failure
    * @return {boolean} Returns true if string passes regex, false if not
    */
  function validatePassword(password, failureCallback) {
    if (passwordRegex.test(password) && checkLength(password, 5, 16)) {
      return true;
    } else {
      if (failureCallback && typeof(failureCallback) == "function") {
        failureCallback(this.getPasswordAllowedChars());
      }
      return false;
    }
  }

  /**
    * Returns the regex of allowed chars
    * @public
    * @method getPasswordAllowedChars
    * @return {string} Returns a string with the allowed chars
    */
  function getPasswordAllowedChars(){
    return 'Length: min 5, max 16. Allowed: A-Z, a-z, 0-9, ~ @ # % ^ & * ( ) - _ = + [ ] { } \\ | ; : \' " , . < > / ? !';
  }

  /**
    * Tests the string against the allowed chars regex
    * @public
    * @method validateEmail
    * @param {string} email - The string to test
    * @param {function} failureCallback - (optional), the function to call on a failure
    * @return {boolean} Returns true if string passes regex, false if not
    */
  function validateEmail(email, failureCallback) {
    if (emailRegex.test(email) && checkLength(email, 4, 80)) {
      return true;
    } else {
      if (failureCallback && typeof(failureCallback) == "function") {
        failureCallback(this.getEmailAllowedChars());
      }
      return false;
    }
  }

  /**
    * Returns the regex of allowed chars
    * @public
    * @method getEmailAllowedChars
    * @return {string} Returns a string with the allowed chars
    */
  function getEmailAllowedChars(){
    return 'Email must be in standard form: e.g. example@apigee.com';
  }

  /**
    * Tests the string against the allowed chars regex
    * @public
    * @method validatePath
    * @param {string} path - The string to test
    * @param {function} failureCallback - (optional), the function to call on a failure
    * @return {boolean} Returns true if string passes regex, false if not
    */
  function validatePath(path, failureCallback) {
    if (pathRegex.test(path) && checkLength(path, 4, 80)) {
      return true;
    } else {
      if (failureCallback && typeof(failureCallback) == "function") {
        failureCallback(this.getPathAllowedChars());
      }
      return false;
    }
  }

  /**
    * Returns the regex of allowed chars
    * @public
    * @method getPathAllowedChars
    * @return {string} Returns a string with the allowed chars
    */
  function getPathAllowedChars(){
    return 'Length: min 4, max 80. Allowed: /, a-z, 0-9, dot, and dash';
  }

  /**
    * Tests the string against the allowed chars regex
    * @public
    * @method validateTitle
    * @param {string} title - The string to test
    * @param {function} failureCallback - (optional), the function to call on a failure
    * @return {boolean} Returns true if string passes regex, false if not
    */
  function validateTitle(title, failureCallback) {
    if (titleRegex.test(title) && checkLength(title, 4, 80)) {
      return true;
    } else {
      if (failureCallback && typeof(failureCallback) == "function") {
        failureCallback(this.getTitleAllowedChars());
      }
      return false;
    }
  }

  /**
    * Returns the regex of allowed chars
    * @public
    * @method getTitleAllowedChars
    * @return {string} Returns a string with the allowed chars
    */
  function getTitleAllowedChars(){
    return 'Length: min 4, max 80. Allowed: space, A-Z, a-z, 0-9, dot, dash, /, !, and ?';
  }

  /**
    * Tests if the string is the correct length
    * @public
    * @method checkLength
    * @param {string} string - The string to test
    * @param {integer} min - the lower bound
    * @param {integer} max - the upper bound
    * @return {boolean} Returns true if string is correct length, false if not
    */
  function checkLength(string, min, max) {
    if (string.length > max || string.length < min) {
      return false;
    }
    return true;
  }

  /**
    * Tests if the string is a uuid
    * @public
    * @method isUUID
    * @param {string} uuid The string to test
    * @returns {Boolean} true if string is uuid
    */
  function isUUID (uuid) {
    var uuidValueRegex = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/;
    if (!uuid) return false;
    return uuidValueRegex.test(uuid);
  }

  return {
    validateUsername:validateUsername,
    getUsernameAllowedChars:getUsernameAllowedChars,
    validateName:validateName,
    getNameAllowedChars:getNameAllowedChars,
    validatePassword:validatePassword,
    getPasswordAllowedChars:getPasswordAllowedChars,
    validateEmail:validateEmail,
    getEmailAllowedChars:getEmailAllowedChars,
    validatePath:validatePath,
    getPathAllowedChars:getPathAllowedChars,
    validateTitle:validateTitle,
    getTitleAllowedChars:getTitleAllowedChars,
    isUUID:isUUID
  }
})();



/**
 *  apigee.QueryObj is a class for holding all query information and paging state
 *
 *  The goal of the query object is to make it easy to run any
 *  kind of CRUD call against the API.  This is done as follows:
 *
 *  1. Create a query object:
 *     queryObj = new apigee.QueryObj("GET", "users", null, function() { alert("success"); }, function() { alert("failure"); });
 *
 *  2. Run the query by calling the appropriate endpoint call
 *     runAppQuery(queryObj);
 *     or
 *     runManagementQuery(queryObj);
 *
 *  3. Paging - The apigee.QueryObj holds the cursor information.  To
 *     use, simply bind click events to functions that call the
 *     getNext and getPrevious methods of the query object.  This
 *     will set the cursor correctly, and the runAppQuery method
 *     can be called again using the same apigee.QueryObj:
 *     runAppQuery(queryObj);
 *
 *  @class apigee.QueryObj
 *  @param method REQUIRED - GET, POST, PUT, DELETE
 *  @param path REQUIRED - API resource (e.g. "users" or "users/rod", should not include http URL or org_name/app_name)
 *  @param jsonObj NULLABLE - a json data object to be passed to the API
 *  @param params NULLABLE - query parameters to be encoded and added to the API URL
 *  @param {Function} successCallback function called with response: <pre>
 *  {
 *    alert('Hurray! Everything worked.');
 *  }
 *  @param {Function} failureCallback function called with response if available: <pre>
 *  {
 *    alert('An error occured');
 *  }
 *  </pre>
 *
 *  @class QueryObj
 *  @author Rod Simpson (rod@apigee.com)
 */

(function () {

  /**
   *  @constructor
   *  @param {string} method
   *  @param {string} path
   *  @param {object} jsonObj
   *  @param {object} queryParams
   *  @param {function} successCallback
   *  @param {function} failureCallback
   */
  apigee.QueryObj = function(method, path, jsonObj, queryParams, successCallback, failureCallback) {
    //query vars
    this._method = method;
    this._path = path;
    this._jsonObj = jsonObj;
    this._queryParams = queryParams;
    this._successCallback = successCallback;
    this._failureCallback = failureCallback;

    //curl command - will be populated by runQuery function
    this._curl = '';
    this._token = false;

    //paging vars
    this._cursor = null;
    this._next = null
    this._previous = [];
  };

  apigee.QueryObj.prototype = {
    /**
     *  A method to set all settable parameters of the QueryObj at one time
     *  @public
     *  @method validateUsername
     *  @param {string} method
     *  @param {string} path
     *  @param {object} jsonObj
     *  @param {object} queryParams
     *  @param {function} successCallback
     *  @param {function} failureCallback
     *  @return none
     */
    setAll: function(method, path, jsonObj, params, successCallback, failureCallback) {
      this._method = method;
      this._path = path;
      this._jsonObj = jsonObj;
      this._params = params;
      this._successCallback = successCallback;
      this._failureCallback = failureCallback;
    },
    /**
    * Returns the method
    * @public
    * @method getMethod
    * @return {string} Returns method
    */
    getMethod: function() {
      return this._method;
    },

    /**
    * sets the method (POST, PUT, DELETE, GET)
    * @public
    * @method setMethod
    * @return none
    */
    setMethod: function(method) {
      this._method = method;
    },
    /**
    * Returns the path
    * @public
    * @method getPath
    * @return {string} Returns path
    */
    getPath: function() {
      return this._path;
    },

    /**
    * sets the path
    * @public
    * @method setPath
    * @return none
    */
    setPath: function(path) {
      this._path = path;
    },

    /**
    * Returns the json Object
    * @public
    * @method getJsonObj
    * @return {object} Returns the json Object
    */
    getJsonObj: function() {
      return this._jsonObj;
    },

    /**
    * sets the json object
    * @public
    * @method setJsonObj
    * @return none
    */
    setJsonObj: function(jsonObj) {
      this._jsonObj = jsonObj;
    },
    /**
    * Returns the Query Parameters object
    * @public
    * @method getQueryParams
    * @return {object} Returns Query Parameters object
    */
    getQueryParams: function() {
      return this._queryParams;
    },

    /**
    * sets the query parameter object
    * @public
    * @method setQueryParams
    * @return none
    */
    setQueryParams: function(queryParams) {
      this._queryParams = queryParams;
    },

    /**
    * Returns the success callback function
    * @public
    * @method getSuccessCallback
    * @return {function} Returns the successCallback
    */
    getSuccessCallback: function() {
      return this._successCallback;
    },

    /**
    * sets the success callback function
    * @public
    * @method setSuccessCallback
    * @return none
    */
    setSuccessCallback: function(successCallback) {
      this._successCallback = successCallback;
    },

    /**
    * Calls the success callback function
    * @public
    * @method callSuccessCallback
    * @return {boolean} Returns true or false based on if there was a callback to call
    */
    callSuccessCallback: function(response) {
      if (this._successCallback && typeof(this._successCallback ) == "function") {
        this._successCallback(response);
        return true;
      } else {
        return false;
      }
    },

    /**
    * Returns the failure callback function
    * @public
    * @method getFailureCallback
    * @return {function} Returns the failureCallback
    */
    getFailureCallback: function() {
      return this._failureCallback;
    },

    /**
    * sets the failure callback function
    * @public
    * @method setFailureCallback
    * @return none
    */
    setFailureCallback: function(failureCallback) {
      this._failureCallback = failureCallback;
    },

    /**
    * Calls the failure callback function
    * @public
    * @method callFailureCallback
    * @return {boolean} Returns true or false based on if there was a callback to call
    */
    callFailureCallback: function(response) {
      if (this._failureCallback && typeof(this._failureCallback) == "function") {
        this._failureCallback(response);
        return true;
      } else {
        return false;
      }
    },

    /**
    * Returns the curl call
    * @public
    * @method getCurl
    * @return {function} Returns the curl call
    */
    getCurl: function() {
      return this._curl;
    },

    /**
    * sets the curl call
    * @public
    * @method setCurl
    * @return none
    */
    setCurl: function(curl) {
      this._curl = curl;
    },

    /**
    * Returns the Token
    * @public
    * @method getToken
    * @return {function} Returns the Token
    */
    getToken: function() {
      return this._token;
    },

    /**
    * Method to set
    * @public
    * @method setToken
    * @return none
    */
    setToken: function(token) {
      this._token = token;
    },

    /**
    * Resets the paging pointer (back to original page)
    * @public
    * @method resetPaging
    * @return none
    */
    resetPaging: function() {
      this._previous = [];
      this._next = null;
      this._cursor = null;
    },

    /**
    * Method to determine if there is a previous page of data
    * @public
    * @method hasPrevious
    * @return {boolean} true or false based on if there is a previous page
    */
    hasPrevious: function() {
      return (this._previous.length > 0);
    },

    /**
    * Method to set the paging object to get the previous page of data
    * @public
    * @method getPrevious
    * @return none
    */
    getPrevious: function() {
      this._next=null; //clear out next so the comparison will find the next item
      this._cursor = this._previous.pop();
    },

    /**
    * Method to determine if there is a next page of data
    * @public
    * @method hasNext
    * @return {boolean} true or false based on if there is a next page
    */
    hasNext: function(){
      return (this._next);
    },

    /**
    * Method to set the paging object to get the next page of data
    * @public
    * @method getNext
    * @return none
    */
    getNext: function() {
      this._previous.push(this._cursor);
      this._cursor = this._next;
    },

    /**
    * Method to save off the cursor just returned by the last API call
    * @public
    * @method saveCursor
    * @return none
    */
    saveCursor: function(cursor) {
      //if current cursor is different, grab it for next cursor
      if (this._next != cursor) {
        this._next = cursor;
      }
    },

    /**
    * Method to determine if there is a next page of data
    * @public
    * @method getCursor
    * @return {string} the current cursor
    */
    getCursor: function() {
      return this._cursor;
    }
  };
})(apigee);


/**
 *  Entity class models a basic usergrid entity.
 *
 *  Usage:
 *  The Entity class can be used to model any entity:
 *  <pre>
 *  var timeMachine = new Entity("cars");
 *  timeMachine.setField("model","DeLorean");
 *  timeMachine.setField("license-plate","OUTATIME");
 *  timeMachine.setField("date","November 5, 1955");
 *  timeMachine.save();
 *
 *  // a new cars entity has been created in the database, and
 *  // the resulting UUID is saved to maintain a unique reference
 *  // to the entity.  We can then update the entity and resave:
 *
 *  timeMachine.setField("date", "July 3, 1985");
 *  timeMachine.save();
 *  // same timeMachine object is updated in the database
 *
 *  We can also refresh the object from the database once the UUID
 *  has been set.  In this way, multiple clients can update the same
 *  object in the database.  To refresh the object, call:
 *
 *  timeMachine.get();
 *
 *  If you need to get a property from the object, do this:
 *
 *  var date = timeMachine.getField("date");
 *
 *  If you don't need the object anymore, simply call the destroy
 *  method and it will be deleted from database:
 *
 *  timeMachine.delete();
 *
 *  //the object is now deleted from the database, although it remains
 *  //in your program.  Destroy it if needed by calling:
 *
 *  timeMachine = null;
 *
 *  </pre>
 *
 *
 *  @class Entity
 *  @author Rod Simpson (rod@apigee.com)
 */
(function () {
  /**
   *  Constructor for initializing an entity
   *
   *  @constructor
   *  @param {string} collectionType - the type of collection to model
   *  @param {uuid} uuid - (optional), the UUID of the collection if it is known
   */
  apigee.Entity = function(collectionType, uuid) {
    this._collectionType = collectionType;
    this._data = {};
    this._uuid = uuid;
  };
  apigee.Entity.prototype = {
    /**
     *  gets the current Entity type
     *
     *  @method getCollectionType
     *  @return {string} collection type
     *
     */
    getCollectionType: function() {
      return this._collectionType;
    },

     /**
     *  sets the collection type of the Entity
     *
     *  @method setCollectionType
     *  @param {string} collectionType
     *  @return none
     *
     */
    setCollectionType: function(collectionType) {
      this._collectionType = collectionType;
    },

    /**
     *  gets the current Entity UUID
     *
     *  @method getUUID
     *  @return {string} uuid
     *
     */
    getUUID: function() {
      return this._uuid;
    },

     /**
     *  sets the UUID of the Entity
     *
     *  @method setUUID
     *  @param {string} uuid
     *  @return none
     *
     */
    setUUID: function(uuid) {
      this._uuid = uuid;
    },

    /**
     *  gets the current Entity Name
     *
     *  @method getName
     *  @return {string} name
     *
     */
    getName: function() {
      return this.getField('name');
    },

     /**
     *  sets the Name of the Entity
     *
     *  @method setName
     *  @param {string} name
     *  @return none
     *
     */
    setName: function(name) {
      this.setField('name', name);
    },

    /**
     *  gets the current Entity data object
     *
     *  @method getData
     *  @return {object} data
     *
     */
    getData: function() {
      return this._data;
    },

     /**
     *  sets the data object
     *
     *  @method setData
     *  @param {object} data
     *  @return none
     *
     */
    setData: function(data) {
      for(item in data) {
        this._data[item] = data[item];
      }
    },

     /**
     *  clears out the data object
     *
     *  @method clearData
     *  @return none
     *
     */
    clearData: function () {
      this._data = null;
    },

    /**
     *  gets a specific field from the data object
     *
     *  @method getField
     *  @param {string} field
     *  @return {string} data field
     *
     */
    getField: function(field) {
      return this._data[field];
    },

    /**
     *  sets a specific field in the data object
     *
     *  @method setField
     *  @param {string} field
     *  @param {string} value
     *  @return none
     *
     */
    setField: function(field, value) {
      this._data[field] = value;
    },

    /**
     *  removes a specific field from the data object
     *
     *  @method deleteField
     *  @param {string} field
     *  @return none
     *
     */
    deleteField: function(field) {
      delete this._data[field];
    },

    /**
     *  Private method for handling the response from the server
     *
     *  @method processResponse
     *  @private
     *  @param {string} self
     *  @param {object} response
     *  @return {boolean} true if the response was populated, false otherwise
     *
     */
    processResponse: function(self, response){
      if (response.entities[0]) { // && apigee.ApiClient.isUUID(response.entities[0].uuid )){
        var entity = response.entities[0];
        //first save off the uuid
        if (entity.uuid) {
          self.setUUID(entity.uuid);
        }
        delete entity.uuid; //remove uuid from the object
        delete entity.metadata; //remove uuid from the object

        //store the rest of the fields
        self.setData(entity);
        return true;
      }
      return false;
    },

    /**
     *  Saves the entity back to the database
     *
     *  @method save
     *  @public
     *  @param {function} successCallback
     *  @param {function} errorCallback
     *  @return none
     *
     */
    save: function(successCallback, errorCallback) {
      var path = this.getCollectionType();
      //TODO:  API will be changed soon to accomodate PUTs via name which create new entities
      //       This function should be changed to PUT only at that time, and updated to use
      //       either uuid or name
      var method = 'POST';
      if (this.getUUID()) {
        method = 'PUT';
        if (apigee.validation.isUUID(this.getUUID())) {
          path += "/" + this.getUUID();
        }
      }
      var self = this;
      apigee.ApiClient.runAppQuery(new apigee.QueryObj(method, path, this.getData(), null,
        function(response) {
          if (self.processResponse(self, response)){
            if (typeof(successCallback) == "function"){
              successCallback(response);
            }
          } else {
            if (typeof(errorCallback) == "function"){
                errorCallback(response);
            }
          }
        },
        function(response) {
          if (typeof(errorCallback) == "function"){
              errorCallback(response);
          }
        }
      ));
    },

    /**
     *  refreshes the entity by making a GET call back to the database
     *
     *  @method get
     *  @public
     *  @param {function} successCallback
     *  @param {function} errorCallback
     *  @return none
     *
     */
    get: function(successCallback, errorCallback) {
      var path = this.getCollectionType();
      //if a uuid is available, use that, otherwise, use the name
      if (this.getUUID()) {
        path += "/" + this.getUUID();
      } else {
        if (path == "users") {
          if (this.getField("username")) {
            path += "/" + this.getField("username");
          } else {
            console.log('no username specified');
            if (typeof(errorCallback) == "function"){
              console.log('no username specified');
            }
          }
        } else {
          if (this.getName()) {
            path += "/" + this.getName();
          } else {
            console.log('no entity identifier specified');
            if (typeof(errorCallback) == "function"){
              console.log('no entity identifier specified');
            }
          }
        }
      }
      var self = this;
      apigee.ApiClient.runAppQuery(new apigee.QueryObj('GET', path, null, null,
        function(response) {
          if (self.processResponse(self, response)){
            if (typeof(successCallback) == "function"){
              successCallback(response);
            }
          } else {
            if (typeof(errorCallback) == "function"){
                errorCallback(response);
            }
          }
        },
        function(response) {
          if (typeof(errorCallback) == "function"){
              errorCallback(response);
          }
        }
      ));
    },

    /**
     *  deletes the entity from the database - will only delete
     *  if the object has a valid uuid
     *
     *  @method destroy
     *  @public
     *  @param {function} successCallback
     *  @param {function} errorCallback
     *  @return none
     *
     */
    destroy: function(successCallback, errorCallback) {
      var path = this.getCollectionType();
      if (this.getUUID()) {
        path += "/" + this.getUUID();
      } else {
        console.log('Error trying to delete object - no uuid specified.');
        if (typeof(errorCallback) == "function"){
          errorCallback('Error trying to delete object - no uuid specified.');
        }
      }
      var self = this;
      apigee.ApiClient.runAppQuery(new apigee.QueryObj('DELETE', path, null, null,
        function(response) {
          //clear out this object
          self.clearData();
          self.setUUID(null);
          if (typeof(successCallback) == "function"){
            successCallback(response);
          }
        },
        function(response) {
          if (typeof(errorCallback) == "function"){
              errorCallback(response);
          }
        }
      ));
    }
  };
})(apigee);


/**
 *  User class models a usergrid user.
 *
 *  Usage:
 *  <pre>
 *  //first create a new user:
 *  var marty = new User("fred"); //<==argument is username)
 *  //next add more data if needed:
 *  marty.setName("Marty McFly");
 *  marty.setField("City", "Hill Valley");
 *  marty.setField("State", "California");
 *  //finally, create the user in the database:
 *  marty.create();
 *  //if the user is updated:
 *  marty.setField("girlfriend","Jennifer");
 *  //call save on the user:
 *  marty.save();
 *
 *  To refresh the user's info from the database:
 *  marty.get();
 *
 *  //to get properties from the user object:
 *  var city = marty.getField("city");
 *
 *  If you don't need the object anymore, simply call the destroy
 *  method and it will be deleted from database:
 *
 *  marty.delete();
 *
 *  //the object is now deleted from the database, although it remains
 *  //in your program.  Destroy it if needed by calling:
 *
 *  marty = null;
 *
 *  </pre>
 *
 *
 *  @class User
 *  @author Rod Simpson (rod@apigee.com)
 */
(function () {
  /**
   *  Constructor for initializing a User
   *
   *  @constructor
   *  @param {string} username - the username of the user
   *  @param {uuid} uuid - (optional), the UUID of the user if it is known
   */
  apigee.User = function(username, uuid) {
    this._collectionType = 'user';
    this._data = {};
    this.setUsername(username);
    this._uuid = uuid;
  };

  apigee.User.prototype = new apigee.Entity();

  /**
   *  Sets the user's username
   *
   *  @method setUsername
   *  @param {string} username - the username of the user
   *  @return none
   */
  apigee.User.prototype.setUsername = function (username){
    this.setField('username', username);
  }

  /**
   *  Gets the user's username
   *
   *  @method getUsername
   *  @return none
   */
  apigee.User.prototype.getUsername = function (){
    return this.getField('username');
  }

 })(apigee);


/**
 *
 *  The Collection class models Usergrid Collections.  It essentially
 *  acts as a container for holding Entity objects, while providing
 *  additional funcitonality such as paging, and saving
 *
 *  To use the Collection class, simply create a new Collection object:
 *
 *  var cars = new Collection('cars');
 *
 *  Once the collection is created, you can refresh it from the database
 *
 *  cars.get();
 *
 *  Say we want to display a list of all the cars in the collection:
 *
 *  var carlist = cars.getEntityList(); // gives us an array of Entity Objects
 *  for(var i=0; i<carlist.length; i++) {
 *    var car = carlist[i];
 *    $('#mycarlist').append('<li>'+ getName() + '</li>');
 *  }
 *
 *
 *
 *  @class Collection
 *  @author Rod Simpson (rod@apigee.com)
 */
(function () {
  /**
   *  Collection is a container class for holding entities
   *
   *  @constructor
   *  @param {string} path - the type of collection to model
   *  @param {uuid} uuid - (optional), the UUID of the collection if it is known
   */
  apigee.Collection = function(path, uuid) {
    this._path = path;
    this._uuid = uuid;
    this._list = [];
    this._queryObj = new apigee.QueryObj();
    this._iterator = -1; //first thing we do is increment, so set to -1
  };

  apigee.Collection.prototype = {
    /**
     *  gets the current Collection path
     *
     *  @method getPath
     *  @return {string} path
     *
     */
    getPath: function() {
      return this._path;
    },

    /**
     *  sets the current Collection path
     *
     *  @method setPath
     *  @param {string} path
     *  @return none
     *
     */
    setPath: function(path) {
      this._path = path;
    },

    /**
     *  gets the current Collection UUID
     *
     *  @method getUUID
     *  @param {string} uuid
     *  @return {string} the uuid
     *
     */
    getUUID: function() {
      return this._uuid;
    },

    /**
     *  sets the current Collection UUID
     *
     *  @method setUUID
     *  @param {string} uuid
     *  @return none
     *
     */
    setUUID: function(uuid) {
      this._uuid = uuid;
    },

    /**
     *  Adds an Entity to the list
     *
     *  @method addEntity
     *  @param {object} entity
     *  @return none
     *
     */
    addEntity: function(entity) {
      var count = this._list.length;
      this._list[count] = entity;
    },

    /**
     *  Looks up an Entity by a specific field
     *
     *  @method getEntityByField
     *  @param {string} field
     *  @param {string} value
     *  @return {object} returns an entity object, or null if it is not found
     *
     */
    getEntityByField: function(field, value) {
      var count = this._list.length;
      var i=0;
      for (i=0; i<count; i++) {
        if (this._list[i].getField(field) == value) {
          return this._list[i];
        }
      }
      return null;
    },

    /**
     *  Looks up an Entity by UUID
     *
     *  @method getEntityByUUID
     *  @param {string} UUID
     *  @return {object} returns an entity object, or null if it is not found
     *
     */
    getEntityByUUID: function(UUID) {
      var count = this._list.length;
      var i=0;
      for (i=0; i<count; i++) {
        if (this._list[i].getUUID() == UUID) {
          return this._list[i];
        }
      }
      return null;
    },

    /**
     *  Returns the first Entity of the Entity list - does not affect the iterator
     *
     *  @method getFirstEntity
     *  @return {object} returns an entity object
     *
     */
    getFirstEntity: function() {
      var count = this._list.length;
      if (count > 0) {
        return this._list[0];
      }
      return null;
    },

    /**
     *  Returns the last Entity of the Entity list - does not affect the iterator
     *
     *  @method getLastEntity
     *  @return {object} returns an entity object
     *
     */
    getLastEntity: function() {
      var count = this._list.length;
      if (count > 0) {
        return this._list[count-1];
      }
      return null;
    },

    /**
     *  Entity iteration -Checks to see if there is a "next" entity
     *  in the list.  The first time this method is called on an entity
     *  list, or after the resetEntityPointer method is called, it will
     *  return true referencing the first entity in the list
     *
     *  @method hasNextEntity
     *  @return {boolean} true if there is a next entity, false if not
     *
     */
    hasNextEntity: function() {
      var next = this._iterator + 1;
      if(next >=0 && next < this._list.length) {
        return true;
      }
      return false;
    },

    /**
     *  Entity iteration - Gets the "next" entity in the list.  The first
     *  time this method is called on an entity list, or after the method
     *  resetEntityPointer is called, it will return the,
     *  first entity in the list
     *
     *  @method hasNextEntity
     *  @return {object} entity
     *
     */
    getNextEntity: function() {
      this._iterator++;
      if(this._iterator >= 0 && this._iterator <= this._list.length) {
        return this._list[this._iterator];
      }
      return false;
    },

    /**
     *  Entity iteration - Checks to see if there is a "previous"
     *  entity in the list.
     *
     *  @method hasPreviousEntity
     *  @return {boolean} true if there is a previous entity, false if not
     *
     */
    hasPreviousEntity: function() {
      var previous = this._iterator - 1;
      if(previous >=0 && previous < this._list.length) {
        return true;
      }
      return false;
    },

    /**
     *  Entity iteration - Gets the "previous" entity in the list.
     *
     *  @method getPreviousEntity
     *  @return {object} entity
     *
     */
    getPreviousEntity: function() {
      this._iterator--;
      if(this._iterator >= 0 && this._iterator <= this._list.length) {
        return this.list[this._iterator];
      }
      return false;
    },

    /**
     *  Entity iteration - Resets the iterator back to the beginning
     *  of the list
     *
     *  @method resetEntityPointer
     *  @return none
     *
     */
    resetEntityPointer: function() {
      this._iterator  = -1;
    },

    /**
     *  Paging -  checks to see if there is a next page of data
     *
     *  @method getEntityList
     *  @return {array} returns an array of entity objects
     *
     */
    getEntityList: function() {
      return this._list;
    },

    /**
     *  sets the entity list
     *
     *  @method setEntityList
     *  @param {array} list - an array of Entity objects
     *  @return none
     *
     */
    setEntityList: function(list) {
      this._list = list;
    },

    /**
     *  Paging -  checks to see if there is a next page od data
     *
     *  @method hasNext
     *  @return {boolean} returns true if there is a next page of data, false otherwise
     *
     */
    hasNextPage:  function() {
      return this._queryObj.hasNext();
    },

    /**
     *  Paging - advances the cursor and gets the next
     *  page of data from the API.  Stores returned entities
     *  in the Entity list.
     *
     *  @method getNext
     *  @return none
     *
     */
    getNextPage: function() {
      if (this._queryObj.hasNext()) {
        this._queryObj.getNext();
        //empty the list
        this.setEntityList([]);
        apigee.ApiClient.runAppQuery(this._queryObj);
      }
    },

    /**
     *  Paging -  checks to see if there is a previous page od data
     *
     *  @method hasPrevious
     *  @return {boolean} returns true if there is a previous page of data, false otherwise
     *
     */
    hasPreviousPage:  function() {
      return this._queryObj.hasPrevious();
    },

    /**
     *  Paging - reverts the cursor and gets the previous
     *  page of data from the API.  Stores returned entities
     *  in the Entity list.
     *
     *  @method getPrevious
     *  @return none
     *
     */
    getPreviousPage: function() {
      if (this._queryObj.hasPrevious()) {
        this._queryObj.getPrevious();
        //empty the list
        this.setEntityList([]);
        apigee.ApiClient.runAppQuery(this._queryObj);
      }
    },

    /**
     *  clears the query parameters object
     *
     *  @method clearQueryObj
     *  @return none
     *
     */
    clearQueryObj: function() {
      this._queryObj = new apigee.QueryObj();
    },

    /**
     *  sets the query parameters object
     *
     *  @method setQueryParams
     *  @param {object} query
     *  @return none
     *
     */
    setQueryParams: function(query) {
      this._queryObj.setQueryParams(query);
    },

    /**
     *  A method to get all items in the collection, as dictated by the
     *  cursor and the query.  By default, the API returns 10 items in
     *  a given call.  This can be overriden so that more or fewer items
     *  are returned.  The entities returned are all stored in the colleciton
     *  object's entity list, and can be retrieved by calling getEntityList()
     *
     *  @method get
     *  @param {function} successCallback
     *  @param {function} errorCallback
     *  @return none
     *
     */
    get: function(successCallback, errorCallback) {
      var path = this.getPath();
      var self = this;
      //empty the list
      this.setEntityList([]);
      this._queryObj.setAll('GET', path, null, null,
        function(response) {
          if (response.entities) {
            var count = response.entities.length;
            for (var i=0;i<count;i++) {
              var uuid = response.entities[i].uuid;
              if (uuid) {
                var entity = new apigee.Entity(self.getPath(), uuid);
                //store the data in the entity
                var data = response.entities[i] || {};
                delete data.uuid; //remove uuid from the object
                delete data.metadata; //remove uuid from the object
                entity.setData(data);
                //store the new entity in this collection
                self.addEntity(entity);
              }
            }
            if (typeof(successCallback) == "function"){
              successCallback(response);
            }
          } else {
            if (typeof(errorCallback) == "function"){
                errorCallback(response);
            }
          }
        },
        function(response) {
          if (typeof(errorCallback) == "function"){
              errorCallback(response);
          }
        }
      );
      apigee.ApiClient.runAppQuery(this._queryObj);
    },
    /**
     *  A method to save all items currently stored in the collection object
     *  caveat with this method: we can't update anything except the items
     *  currently stored in the collection.
     *
     *  @method save
     *  @param {function} successCallback
     *  @param {function} errorCallback
     *  @return none
     *
     */
    save: function(successCallback, errorCallback) {
      //loop across all entities and save each one
      var entities = this.getEntityList();
      var count = entities.length;
      var jsonObj = [];
      for (var i=0;i<count;i++) {
        entity = entities[i];
        data = entity.getData();
        if (entity.getUUID()) {
          data.uuid = entity.getUUID();
          jsonObj.push(data);
        } else {
          //the entity does not yet exist in the database
          // so save it via the entity so the uuid is captured

          entity.save(); //need to deal with callbacks
        }
      }
      this._queryObj.setAll('PUT', path, jsonObj, null,successCallback, errorCallback);
    }
  };

})(apigee);