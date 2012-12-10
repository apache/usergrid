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

//Usergrid namespace encapsulates this SDK
window.Usergrid = window.Usergrid || {};
Usergrid = Usergrid || {};
Usergrid.SDK_VERSION = '0.9.10';

/**
 *  Usergrid.Query is a class for holding all query information and paging state
 *
 *  @class Query
 *  @author Rod Simpson (rod@apigee.com)
 */

(function () {

  /**
   *  @constructor
   *  @param {string} method
   *  @param {string} path
   *  @param {object} jsonObj
   *  @param {object} paramsObj
   *  @param {function} successCallback
   *  @param {function} failureCallback
   */
  Usergrid.Query = function(method, resource, jsonObj, paramsObj, successCallback, failureCallback) {
    //query vars
    this._method = method;
    this._resource = resource;
    this._jsonObj = jsonObj;
    this._paramsObj = paramsObj;
    this._successCallback = successCallback;
    this._failureCallback = failureCallback;

    //curl command - will be populated by runQuery function
    this._curl = '';
    this._token = false;

    //paging vars
    this._cursor = null;
    this._next = null;
    this._previous = [];
    this._start = 0;
    this._end = 0;
  };

  Usergrid.Query.prototype = {
     setQueryStartTime: function() {
       this._start = new Date().getTime();
     },

     setQueryEndTime: function() {
       this._end = new Date().getTime();
     },

     getQueryTotalTime: function() {
       var seconds = 0;
       var time = this._end - this._start;
       try {
          seconds = ((time/10) / 60).toFixed(2);
       } catch(e){ return 0; }
       return this.getMethod() + " " + this.getResource() + " - " + seconds + " seconds";
     },
    /**
     *  A method to set all settable parameters of the Query at one time
     *
     *  @public
     *  @method validateUsername
     *  @param {string} method
     *  @param {string} path
     *  @param {object} jsonObj
     *  @param {object} paramsObj
     *  @param {function} successCallback
     *  @param {function} failureCallback
     *  @return none
     */
    setAllQueryParams: function(method, resource, jsonObj, paramsObj, successCallback, failureCallback) {
      this._method = method;
      this._resource = resource;
      this._jsonObj = jsonObj;
      this._paramsObj = paramsObj;
      this._successCallback = successCallback;
      this._failureCallback = failureCallback;
    },

    /**
     *  A method to reset all the parameters in one call
     *
     *  @public
     *  @return none
     */
    clearAll: function() {
      this._method = null;
      this._resource = null;
      this._jsonObj = {};
      this._paramsObj = {};
      this._successCallback = null;
      this._failureCallback = null;
    },
    /**
    * Returns the method
    *
    * @public
    * @method getMethod
    * @return {string} Returns method
    */
    getMethod: function() {
      return this._method;
    },

    /**
    * sets the method (POST, PUT, DELETE, GET)
    *
    * @public
    * @method setMethod
    * @return none
    */
    setMethod: function(method) {
      this._method = method;
    },

    /**
    * Returns the resource
    *
    * @public
    * @method getResource
    * @return {string} the resource
    */
    getResource: function() {
      return this._resource;
    },

    /**
    * sets the resource
    *
    * @public
    * @method setResource
    * @return none
    */
    setResource: function(resource) {
      this._resource = resource;
    },

    /**
    * Returns the json Object
    *
    * @public
    * @method getJsonObj
    * @return {object} Returns the json Object
    */
    getJsonObj: function() {
      return this._jsonObj;
    },

    /**
    * sets the json object
    *
    * @public
    * @method setJsonObj
    * @return none
    */
    setJsonObj: function(jsonObj) {
      this._jsonObj = jsonObj;
    },
    /**
    * Returns the Query Parameters object
    *
    * @public
    * @method getQueryParams
    * @return {object} Returns Query Parameters object
    */
    getQueryParams: function() {
      return this._paramsObj;
    },

    /**
    * sets the query parameter object
    *
    * @public
    * @method setQueryParams
    * @return none
    */
    setQueryParams: function(paramsObj) {
      this._paramsObj = paramsObj;
    },

    /**
    * Returns the success callback function
    *
    * @public
    * @method getSuccessCallback
    * @return {function} Returns the successCallback
    */
    getSuccessCallback: function() {
      return this._successCallback;
    },

    /**
    * sets the success callback function
    *
    * @public
    * @method setSuccessCallback
    * @return none
    */
    setSuccessCallback: function(successCallback) {
      this._successCallback = successCallback;
    },

    /**
    * Calls the success callback function
    *
    * @public
    * @method callSuccessCallback
    * @return {boolean} Returns true or false based on if there was a callback to call
    */
    callSuccessCallback: function(response) {
      if (this._successCallback && typeof(this._successCallback ) === "function") {
        this._successCallback(response);
        return true;
      } else {
        return false;
      }
    },

    /**
    * Returns the failure callback function
    *
    * @public
    * @method getFailureCallback
    * @return {function} Returns the failureCallback
    */
    getFailureCallback: function() {
      return this._failureCallback;
    },

    /**
    * sets the failure callback function
    *
    * @public
    * @method setFailureCallback
    * @return none
    */
    setFailureCallback: function(failureCallback) {
      this._failureCallback = failureCallback;
    },

    /**
    * Calls the failure callback function
    *
    * @public
    * @method callFailureCallback
    * @return {boolean} Returns true or false based on if there was a callback to call
    */
    callFailureCallback: function(response) {
      if (this._failureCallback && typeof(this._failureCallback) === "function") {
        this._failureCallback(response);
        return true;
      } else {
        return false;
      }
    },

    /**
    * Returns the curl call
    *
    * @public
    * @method getCurl
    * @return {function} Returns the curl call
    */
    getCurl: function() {
      return this._curl;
    },

    /**
    * sets the curl call
    *
    * @public
    * @method setCurl
    * @return none
    */
    setCurl: function(curl) {
      this._curl = curl;
    },

    /**
    * Returns the Token
    *
    * @public
    * @method getToken
    * @return {function} Returns the Token
    */
    getToken: function() {
      return this._token;
    },

    /**
    * Method to set
    *
    * @public
    * @method setToken
    * @return none
    */
    setToken: function(token) {
      this._token = token;
    },

    /**
    * Resets the paging pointer (back to original page)
    *
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
    *
    * @public
    * @method hasPrevious
    * @return {boolean} true or false based on if there is a previous page
    */
    hasPrevious: function() {
      return (this._previous.length > 0);
    },

    /**
    * Method to set the paging object to get the previous page of data
    *
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
    *
    * @public
    * @method hasNext
    * @return {boolean} true or false based on if there is a next page
    */
    hasNext: function(){
      return (this._next);
    },

    /**
    * Method to set the paging object to get the next page of data
    *
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
    *
    * @public
    * @method saveCursor
    * @return none
    */
    saveCursor: function(cursor) {
      //if current cursor is different, grab it for next cursor
      if (this._next !== cursor) {
        this._next = cursor;
      }
    },

    /**
    * Method to determine if there is a next page of data
    *
    * @public
    * @method getCursor
    * @return {string} the current cursor
    */
    getCursor: function() {
      return this._cursor;
    },
    
    isUUID: function(uuid) {
      var uuidValueRegex = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/;
      if (!uuid) return false;
      return uuidValueRegex.test(uuid);
    }
  };
})(Usergrid);


/*
 *  Usergrid.ApiClient
 *
 *  A Singleton that is the main client for making calls to the API. Maintains
 *  state between calls for the following items:
 *
 *  Token
 *  User (username, email, name, uuid)
 *
 *  Main methods for making calls to the API are:
 *
 *  runAppQuery (Query)
 *  runManagementQuery(Query)
 *
 *  Create a new Usergrid.Query object and then pass it to either of these
 *  two methods for making calls directly to the API.
 *
 *  A method for logging in an app user (to get a OAuth token) also exists:
 *
 *  logInAppUser (username, password, successCallback, failureCallback)
 *
 *  @class Usergrid.ApiClient
 *  @author Rod Simpson (rod@apigee.com)
 *
 */
Usergrid.M = 'ManagementQuery';
Usergrid.A = 'ApplicationQuery';
Usergrid.ApiClient = (function () {
  //API endpoint
  var _apiUrl = "https://api.usergrid.com/";
  var _orgName = null;
  var _appName = null;
  var _token = null;
  var _callTimeout = 30000;
  var _queryType = null;
  var _loggedInUser = null;
  var _logoutCallback = null;
  var _callTimeoutCallback = null;

  /*
   *  A method to set up the ApiClient with orgname and appname
   *
   *  @method init
   *  @public
   *  @param {string} orgName
   *  @param {string} appName
   *  @return none
   *
   */
  function init(orgName, appName){
    this.setOrganizationName(orgName);
    this.setApplicationName(appName);
  }

  /*
  *  Public method to run calls against the app endpoint
  *
  *  @method runAppQuery
  *  @public
  *  @params {object} Usergrid.Query - {method, path, jsonObj, params, successCallback, failureCallback}
  *  @return none
  */
  function runAppQuery (Query) {
    var endpoint = "/" + this.getOrganizationName() + "/" + this.getApplicationName() + "/";
    setQueryType(Usergrid.A);
    run(Query, endpoint);
  }

  /*
  *  Public method to run calls against the management endpoint
  *
  *  @method runManagementQuery
  *  @public
  *  @params {object} Usergrid.Query - {method, path, jsonObj, params, successCallback, failureCallback}
  *  @return none
  */
  function runManagementQuery (Query) {
    var endpoint = "/management/";
    setQueryType(Usergrid.M);
    run(Query, endpoint)
  }

  /*
    *  A public method to get the organization name to be used by the client
    *
    *  @method getOrganizationName
    *  @public
    *  @return {string} the organization name
    */
  function getOrganizationName() {
    return _orgName;
  }

  /*
    *  A public method to set the organization name to be used by the client
    *
    *  @method setOrganizationName
    *  @param orgName - the organization name
    *  @return none
    */
  function setOrganizationName(orgName) {
    _orgName = orgName;
  }

  /*
  *  A public method to get the application name to be used by the client
  *
  *  @method getApplicationName
  *  @public
  *  @return {string} the application name
  */
  function getApplicationName() {
    return _appName;
  }

  /*
  *  A public method to set the application name to be used by the client
  *
  *  @method setApplicationName
  *  @public
  *  @param appName - the application name
  *  @return none
  */
  function setApplicationName(appName) {
    _appName = appName;
  }

  /*
  *  A public method to get current OAuth token
  *
  *  @method getToken
  *  @public
  *  @return {string} the current token
  */
  function getToken() {
    return _token;
  }

  /*
  *  A public method to set the current Oauth token
  *
  *  @method setToken
  *  @public
  *  @param token - the bearer token
  *  @return none
  */
  function setToken(token) {
    _token = token;
  }

  /*
   *  A public method to return the API URL
   *
   *  @method getApiUrl
   *  @public
   *  @return {string} the API url
   */
  function getApiUrl() {
    return _apiUrl;
  }

  /*
   *  A public method to overide the API url
   *
   *  @method setApiUrl
   *  @public
   *  @return none
   */
  function setApiUrl(apiUrl) {
    _apiUrl = apiUrl;
  }
  
  /*
   *  A public method to return the call timeout amount
   *
   *  @method getCallTimeout
   *  @public
   *  @return {string} the timeout value (an integer) 30000 = 30 seconds
   */
  function getCallTimeout() {
    return _callTimeout;
  }

  /*
   *  A public method to override the call timeout amount
   *
   *  @method setCallTimeout
   *  @public
   *  @return none
   */
  function setCallTimeout(callTimeout) {
    _callTimeout = callTimeout;
  }
  
  /*
   * Returns the call timeout callback function
   *
   * @public
   * @method setCallTimeoutCallback
   * @return none
   */ 
  function setCallTimeoutCallback(callback) {
    _callTimeoutCallback = callback; 
  }
  
  /*
   * Returns the call timeout callback function
   *
   * @public
   * @method getCallTimeoutCallback
   * @return {function} Returns the callTimeoutCallback
   */
  function getCallTimeoutCallback() {
    return _callTimeoutCallback; 
  }
  
  /*
   * Calls the call timeout callback function
   *
   * @public
   * @method callTimeoutCallback
   * @return {boolean} Returns true or false based on if there was a callback to call
   */
  function callTimeoutCallback(response) {
    if (_callTimeoutCallback && typeof(_callTimeoutCallback) === "function") {
      _callTimeoutCallback(response);
      return true;
    } else {
      return false;
    }
  }

  /*
   *  A public method to get the api url of the reset pasword endpoint
   *
   *  @method getResetPasswordUrl
   *  @public
   *  @return {string} the api rul of the reset password endpoint
   */
  function getResetPasswordUrl() {
    return getApiUrl() + "/management/users/resetpw"
  }

  /*
   *  A public method to get an Entity object for the current logged in user
   *
   *  @method getLoggedInUser
   *  @public
   *  @return {object} user - Entity object of type user
   */
  function getLoggedInUser() {
    return this._loggedInUser;
  }

  /*
   *  A public method to set an Entity object for the current logged in user
   *
   *  @method setLoggedInUser
   *  @public
   *  @param {object} user - Entity object of type user
   *  @return none
   */
  function setLoggedInUser(user) {
    this._loggedInUser = user;
  }

  /*
  *  A public method to log in an app user - stores the token for later use
  *
  *  @method logInAppUser
  *  @public
  *  @params {string} username
  *  @params {string} password
  *  @params {function} successCallback
  *  @params {function} failureCallback
  *  @return {response} callback functions return API response object
  */
  function logInAppUser (username, password, successCallback, failureCallback) {
    var self = this;
    var data = {"username": username, "password": password, "grant_type": "password"};
    this.runAppQuery(new Usergrid.Query('GET', 'token', null, data,
      function (response) {
        var user = new Usergrid.Entity('users');
        user.set('username', response.user.username);
        user.set('name', response.user.name);
        user.set('email', response.user.email);
        user.set('uuid', response.user.uuid);
        self.setLoggedInUser(user);
        self.setToken(response.access_token);
        if (successCallback && typeof(successCallback) === "function") {
          successCallback(response);
        }
      },
      function (response) {
        if (failureCallback && typeof(failureCallback) === "function") {
          failureCallback(response);
        }
      }
     ));
  }

  /*
   *  TODO:  NOT IMPLEMENTED YET - A method to renew an app user's token
   *  Note: waiting for API implementation
   *  @method renewAppUserToken
   *  @public
   *  @return none
   */
  function renewAppUserToken() {

  }

  /**
   *  A public method to log out an app user - clears all user fields from client
   *
   *  @method logoutAppUser
   *  @public
   *  @return none
   */
  function logoutAppUser() {
    this.setLoggedInUser(null);
    this.setToken(null);
  }

  /**
   *  A public method to test if a user is logged in - does not guarantee that the token is still valid,
   *  but rather that one exists, and that there is a valid UUID
   *
   *  @method isLoggedInAppUser
   *  @public
   *  @params {object} Usergrid.Query - {method, path, jsonObj, params, successCallback, failureCallback}
   *  @return {boolean} Returns true the user is logged in (has token and uuid), false if not
   */
  function isLoggedInAppUser() {
    var user = this.getLoggedInUser();
    return (this.getToken() && user.get('uuid'));
  }

   /*
   *  A public method to get the logout callback, which is called
   *
   *  when the token is found to be invalid
   *  @method getLogoutCallback
   *  @public
   *  @return {string} the api rul of the reset password endpoint
   */
  function getLogoutCallback() {
    return _logoutCallback;
  }

  /*
   *  A public method to set the logout callback, which is called
   *
   *  when the token is found to be invalid
   *  @method setLogoutCallback
   *  @public
   *  @param {function} logoutCallback
   *  @return none
   */
  function setLogoutCallback(logoutCallback) {
    _logoutCallback = logoutCallback;
  }

  /*
   *  A public method to call the logout callback, which is called
   *
   *  when the token is found to be invalid
   *  @method callLogoutCallback
   *  @public
   *  @return none
   */
  function callLogoutCallback() {
    if (_logoutCallback && typeof(_logoutCallback ) === "function") {
      _logoutCallback();
      return true;
    } else {
      return false;
    }
  }

  /**
   *  Private helper method to encode the query string parameters
   *
   *  @method encodeParams
   *  @public
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

  /*
   *  A private method to get the type of the current api call - (Management or Application)
   *
   *  @method getQueryType
   *  @private
   *  @return {string} the call type
   */
  function getQueryType() {
    return _queryType;
  }
  /*
   *  A private method to set the type of the current api call - (Management or Application)
   *
   *  @method setQueryType
   *  @private
   *  @param {string} call type
   *  @return none
   */
  function setQueryType(type) {
    _queryType = type;
  }
  
  

  /**
   *  A private method to validate, prepare,, and make the calls to the API
   *  Use runAppQuery or runManagementQuery to make your calls!
   *
   *  @method run
   *  @private
   *  @params {object} Usergrid.Query - {method, path, jsonObj, params, successCallback, failureCallback}
   *  @params {string} endpoint - used to differentiate between management and app queries
   *  @return {response} callback functions return API response object
   */
  function run (Query, endpoint) {
    //validate parameters
    try {
      //verify that the query object is valid
      if(!(Query instanceof Usergrid.Query)) {
        throw(new Error('Query is not a valid object.'));
      }
      //for timing, call start
      Query.setQueryStartTime();
      //peel the data out of the query object
      var method = Query.getMethod().toUpperCase();
      var path = Query.getResource();
      var jsonObj = Query.getJsonObj() || {};
      var params = Query.getQueryParams() || {};

      //method - should be GET, POST, PUT, or DELETE only
      if (method != 'GET' && method != 'POST' && method != 'PUT' && method != 'DELETE') {
        throw(new Error('Invalid method - should be GET, POST, PUT, or DELETE.'));
      }
    
      //curl - append the bearer token if this is not the sandbox app
      var application_name = Usergrid.ApiClient.getApplicationName();
      if (application_name) {
        application_name = application_name.toUpperCase();
      }
      //if (application_name != 'SANDBOX' && Usergrid.ApiClient.getToken()) {
      if ( (application_name != 'SANDBOX' && Usergrid.ApiClient.getToken()) || (getQueryType() == Usergrid.M && Usergrid.ApiClient.getToken())) {
        Query.setToken(true);
      }

      //params - make sure we have a valid json object
      _params = JSON.stringify(params);
      if (!JSON.parse(_params)) {
        throw(new Error('Params object is not valid.'));
      }

      //add in the cursor if one is available
      if (Query.getCursor()) {
        params.cursor = Query.getCursor();
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
      path = Usergrid.ApiClient.getApiUrl() + path;

      //add in a timestamp for gets and deletes - to avoid caching by the browser
      if ((method == "GET") || (method == "DELETE")) {
        params['_'] = new Date().getTime();
      }

      //append params to the path
      var encoded_params = encodeParams(params);
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
      }

    } catch (e) {
      //parameter was invalid
      console.log('error occured running query -' + e.message);
      return false;
    }
    
    try {
      curl = Usergrid.Curl.buildCurlCall(Query, endpoint);
      //log the curl call
      console.log(curl);
      //store the curl command back in the object
      Query.setCurl(curl);
    } catch(e) {
      //curl module not enabled
    }

    //so far so good, so run the query
    var xD = window.XDomainRequest ? true : false;
    var xhr = getXHR(method, path, jsonObj);
   
    // Handle response.
    xhr.onerror = function() {
      //for timing, call end
      Query.setQueryEndTime();
      //for timing, log the total call time
      console.log(Query.getQueryTotalTime());
      //network error
      clearTimeout(timeout);
      console.log('API call failed at the network level.');
      //send back an error (best we can do with what ie gives back)
      Query.callFailureCallback(response.innerText);
    };
    xhr.xdomainOnload = function (response) {
      //for timing, call end
      Query.setQueryEndTime();
      //for timing, log the total call time
      console.log('Call timing: ' + Query.getQueryTotalTime());
      //call completed
      clearTimeout(timeout);
      //decode the response
      response = JSON.parse(xhr.responseText);
      //if a cursor was present, grab it
      try {
        var cursor = response.cursor || null;
        Query.saveCursor(cursor);
      }catch(e) {}
      Query.callSuccessCallback(response);
    };
    xhr.onload = function(response) {
      //for timing, call end
      Query.setQueryEndTime();
      //for timing, log the total call time
      console.log('Call timing: ' + Query.getQueryTotalTime());
      //call completed
      clearTimeout(timeout);
      //decode the response
      response = JSON.parse(xhr.responseText);
      if (xhr.status != 200 && !xD)   {
        //there was an api error
        try {
          var error = response.error;
          console.log('API call failed: (status: '+xhr.status+').' + error.type);
          if ( (error.type == "auth_expired_session_token") ||
               (error.type == "unauthorized")   ||
               (error.type == "auth_missing_credentials")   ||
               (error.type == "auth_invalid")) {
            //this error type means the user is not authorized. If a logout function is defined, call it
            callLogoutCallback();
        }} catch(e){}
        //otherwise, just call the failure callback
        Query.callFailureCallback(response.error_description);
        return;
      } else {
        //query completed succesfully, so store cursor
        var cursor = response.cursor || null;
        Query.saveCursor(cursor);
        //then call the original callback
        Query.callSuccessCallback(response);
     }
    }; 
        
    var timeout = setTimeout(
      function() { 
        xhr.abort(); 
        if (Usergrid.ApiClient.getCallTimeoutCallback() === 'function') {
          Usergrid.ApiClient.callTimeoutCallback('API CALL TIMEOUT');
        } else {
          Query.callFailureCallback('API CALL TIMEOUT');
        }        
      }, 
      Usergrid.ApiClient.getCallTimeout()); //set for 30 seconds

    xhr.send(jsonObj);
  }
  
   /**
   *  A private method to return the XHR object
   *
   *  @method getXHR
   *  @private
   *  @params {string} method (GET,POST,PUT,DELETE)
   *  @params {string} path - api endpoint to call
   *  @return {object} jsonObj - the json object if there is one
   */
  function getXHR(method, path, jsonObj) {
    var xhr;
    if(window.XDomainRequest)
    {
      xhr = new window.XDomainRequest();
      if (Usergrid.ApiClient.getToken()) {
        if (path.indexOf("?")) {
          path += '&access_token='+Usergrid.ApiClient.getToken();
        } else {
          path = '?access_token='+Usergrid.ApiClient.getToken();
        }
      }
      xhr.open(method, path, true);
    }
    else 
    {
      xhr = new XMLHttpRequest();
      xhr.open(method, path, true);
      //add content type = json if there is a json payload
      if (jsonObj) {
        xhr.setRequestHeader("Content-Type", "application/json");
      }
      if (Usergrid.ApiClient.getToken()) {
        xhr.setRequestHeader("Authorization", "Bearer " + Usergrid.ApiClient.getToken());
        xhr.withCredentials = true;
      }
    }
    return xhr;
  }

  return {
    init:init,
    runAppQuery:runAppQuery,
    runManagementQuery:runManagementQuery,
    getOrganizationName:getOrganizationName,
    setOrganizationName:setOrganizationName,
    getApplicationName:getApplicationName,
    setApplicationName:setApplicationName,
    getToken:getToken,
    setToken:setToken,
    getQueryType:getQueryType,
    getCallTimeout:getCallTimeout,
    setCallTimeout:setCallTimeout,
    getCallTimeoutCallback:getCallTimeoutCallback,
    setCallTimeoutCallback:setCallTimeoutCallback,
    callTimeoutCallback:callTimeoutCallback,
    getApiUrl:getApiUrl,
    setApiUrl:setApiUrl,
    getResetPasswordUrl:getResetPasswordUrl,
    getLoggedInUser:getLoggedInUser,
    setLoggedInUser:setLoggedInUser,
    logInAppUser:logInAppUser,
    renewAppUserToken:renewAppUserToken,
    logoutAppUser:logoutAppUser,
    encodeParams:encodeParams,
    isLoggedInAppUser:isLoggedInAppUser,
    getLogoutCallback:getLogoutCallback,
    setLogoutCallback:setLogoutCallback,
    callLogoutCallback:callLogoutCallback
  }
})();


/**
 *  A class to Model a Usergrid Entity.
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
  Usergrid.Entity = function(collectionType, uuid) {
    this._collectionType = collectionType;
    this._data = {};
    if (uuid) {
      this._data['uuid'] = uuid;
    }
  };

  //inherit prototype from Query
  Usergrid.Entity.prototype = new Usergrid.Query();

  /**
   *  gets the current Entity type
   *
   *  @method getCollectionType
   *  @return {string} collection type
   */
  Usergrid.Entity.prototype.getCollectionType = function (){
    return this._collectionType;
  }

  /**
   *  sets the current Entity type
   *
   *  @method setCollectionType
   *  @param {string} collectionType
   *  @return none
   */
  Usergrid.Entity.prototype.setCollectionType = function (collectionType){
    this._collectionType = collectionType;
  }

  /**
   *  gets a specific field or the entire data object. If null or no argument
   *  passed, will return all data, else, will return a specific field
   *
   *  @method get
   *  @param {string} field
   *  @return {string} || {object} data
   */
  Usergrid.Entity.prototype.get = function (field){
    if (field) {
      return this._data[field];
    } else {
      return this._data;
    }
  },

  /**
   *  adds a specific field or object to the Entity's data
   *
   *  @method set
   *  @param {string} item || {object}
   *  @param {string} value
   *  @return none
   */
  Usergrid.Entity.prototype.set = function (item, value){
    if (typeof item === 'object') {
      for(var field in item) {
        this._data[field] = item[field];
      }
    } else if (typeof item === 'string') {
      if (value === null) {
        delete this._data[item];
      } else {
        this._data[item] = value;
      }
    } else {
      this._data = null;
    }
  }

  /**
   *  Saves the entity back to the database
   *
   *  @method save
   *  @public
   *  @param {function} successCallback
   *  @param {function} errorCallback
   *  @return none
   */
  Usergrid.Entity.prototype.save = function (successCallback, errorCallback){
    var path = this.getCollectionType();
    //TODO:  API will be changed soon to accomodate PUTs via name which create new entities
    //       This function should be changed to PUT only at that time, and updated to use
    //       either uuid or name
    var method = 'POST';
    if (this.isUUID(this.get('uuid'))) {
      method = 'PUT';
      path += "/" + this.get('uuid');
    }

    //if this is a user, update the password if it has been specified
    var data = {};
    if (path == 'users') {
      data = this.get();
      var pwdata = {};
      //Note: we have a ticket in to change PUT calls to /users to accept the password change
      //      once that is done, we will remove this call and merge it all into one
      if (data.oldpassword && data.newpassword) {
        pwdata.oldpassword = data.oldpassword;
        pwdata.newpassword = data.newpassword;
        this.runAppQuery(new Usergrid.Query('PUT', 'users/'+uuid+'/password', pwdata, null,
          function (response) {
            //not calling any callbacks - this section will be merged as soon as API supports
            //   updating passwords in general PUT call
          },
          function (response) {

          }
        ));
      }
      //remove old and new password fields so they don't end up as part of the entity object
      delete data.oldpassword;
      delete data.newpassword;
    }

    //update the entity
    var self = this;

    data = {};
    var entityData = this.get();
    //remove system specific properties
    for (var item in entityData) {
      if (item == 'metadata' || item == 'created' || item == 'modified' ||
          item == 'type' || item == 'activatted' ) { continue; }
      data[item] = entityData[item];
    }

    this.setAllQueryParams(method, path, data, null,
      function(response) {
        try {
          var entity = response.entities[0];
          self.set(entity);
          if (typeof(successCallback) === "function"){
            successCallback(response);
          }
        } catch (e) {
          if (typeof(errorCallback) === "function"){
            errorCallback(response);
          }
        }
      },
      function(response) {
        if (typeof(errorCallback) === "function"){
          errorCallback(response);
        }
      }
    );
    Usergrid.ApiClient.runAppQuery(this);
  }

  /**
   *  refreshes the entity by making a GET call back to the database
   *
   *  @method fetch
   *  @public
   *  @param {function} successCallback
   *  @param {function} errorCallback
   *  @return none
   */
  Usergrid.Entity.prototype.fetch = function (successCallback, errorCallback){
    var path = this.getCollectionType();
    //if a uuid is available, use that, otherwise, use the name
    if (this.isUUID(this.get('uuid'))) {
      path += "/" + this.get('uuid');
    } else {
      if (path == "users") {
        if (this.get("username")) {
          path += "/" + this.get("username");
        } else {
          console.log('no username specified');
          if (typeof(errorCallback) === "function"){
            console.log('no username specified');
          }
        }
      } else {
        if (this.get()) {
          path += "/" + this.get();
        } else {
          console.log('no entity identifier specified');
          if (typeof(errorCallback) === "function"){
            console.log('no entity identifier specified');
          }
        }
      }
    }
    var self = this;
    this.setAllQueryParams('GET', path, null, null,
      function(response) {
        if (response.user) {
          self.set(response.user);
        } else if (response.entities[0]){
          self.set(response.entities[0]);
        }
        if (typeof(successCallback) === "function"){
          successCallback(response);
        }
      },
      function(response) {
        if (typeof(errorCallback) === "function"){
            errorCallback(response);
        }
      }
    );
    Usergrid.ApiClient.runAppQuery(this);
  }

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
  Usergrid.Entity.prototype.destroy = function (successCallback, errorCallback){
    var path = this.getCollectionType();
    if (this.isUUID(this.get('uuid'))) {
      path += "/" + this.get('uuid');
    } else {
      console.log('Error trying to delete object - no uuid specified.');
      if (typeof(errorCallback) === "function"){
        errorCallback('Error trying to delete object - no uuid specified.');
      }
    }
    var self = this;
    this.setAllQueryParams('DELETE', path, null, null,
      function(response) {
        //clear out this object
        self.set(null);
        if (typeof(successCallback) === "function"){
          successCallback(response);
        }
      },
      function(response) {
        if (typeof(errorCallback) === "function"){
            errorCallback(response);
        }
      }
    );
    Usergrid.ApiClient.runAppQuery(this);
  }

})(Usergrid);


/**
 *  The Collection class models Usergrid Collections.  It essentially
 *  acts as a container for holding Entity objects, while providing
 *  additional funcitonality such as paging, and saving
 *
 *  @class Collection
 *  @author Rod Simpson (rod@apigee.com)
 */
(function () {
  /**
   *  Collection is a container class for holding entities
   *
   *  @constructor
   *  @param {string} collectionPath - the type of collection to model
   *  @param {uuid} uuid - (optional), the UUID of the collection if it is known
   */
  Usergrid.Collection = function(path, uuid) {
    this._path = path;
    this._uuid = uuid;
    this._list = [];
    this._Query = new Usergrid.Query();
    this._iterator = -1; //first thing we do is increment, so set to -1
  };

  Usergrid.Collection.prototype = new Usergrid.Query();

  /**
   *  gets the current Collection path
   *
   *  @method getPath
   *  @return {string} path
   */
  Usergrid.Collection.prototype.getPath = function (){
    return this._path;
  }

  /**
   *  sets the Collection path
   *
   *  @method setPath
   *  @param {string} path
   *  @return none
   */
  Usergrid.Collection.prototype.setPath = function (path){
    this._path = path;
  }

  /**
   *  gets the current Collection UUID
   *
   *  @method getUUID
   *  @return {string} the uuid
   */
  Usergrid.Collection.prototype.getUUID = function (){
    return this._uuid;
  }

  /**
   *  sets the current Collection UUID
   *  @method setUUID
   *  @param {string} uuid
   *  @return none
   */
  Usergrid.Collection.prototype.setUUID = function (uuid){
    this._uuid = uuid;
  }

  /**
   *  Adds an Entity to the collection (adds to the local object)
   *
   *  @method addEntity
   *  @param {object} entity
   *  @param {function} successCallback
   *  @param {function} errorCallback
   *  @return none
   */
  Usergrid.Collection.prototype.addEntity = function (entity){
    //then add it to the list
    var count = this._list.length;
    this._list[count] = entity;
  }

  /**
   *  Adds a new Entity to the collection (saves, then adds to the local object)
   *
   *  @method addNewEntity
   *  @param {object} entity
   *  @return none
   */
  Usergrid.Collection.prototype.addNewEntity = function (entity, successCallback, errorCallback) {
    //add the entity to the list
    this.addEntity(entity);
    //then save the entity
    entity.save(successCallback, errorCallback);
  }

  Usergrid.Collection.prototype.destroyEntity = function (entity) {
    //first get the entities uuid
    var uuid = entity.get('uuid');
    //if the entity has a uuid, delete it
    if (uuid) {
      //then remove it from the list
      var count = this._list.length;
      var i=0;
      var reorder = false;
      for (i=0; i<count; i++) {
        if(reorder) {
          this._list[i-1] = this._list[i];
          this._list[i] = null;
        }
        if (this._list[i].get('uuid') == uuid) {
          this._list[i] = null;
          reorder=true;
        }
      }
    }
    //first destroy the entity on the server
    entity.destroy();
  }

  /**
   *  Looks up an Entity by a specific field - will return the first Entity that
   *  has a matching field
   *
   *  @method getEntityByField
   *  @param {string} field
   *  @param {string} value
   *  @return {object} returns an entity object, or null if it is not found
   */
  Usergrid.Collection.prototype.getEntityByField = function (field, value){
    var count = this._list.length;
    var i=0;
    for (i=0; i<count; i++) {
      if (this._list[i].getField(field) == value) {
        return this._list[i];
      }
    }
    return null;
  }

  /**
   *  Looks up an Entity by UUID
   *
   *  @method getEntityByUUID
   *  @param {string} UUID
   *  @return {object} returns an entity object, or null if it is not found
   */
  Usergrid.Collection.prototype.getEntityByUUID = function (UUID){
    var count = this._list.length;
    var i=0;
    for (i=0; i<count; i++) {
      if (this._list[i].get('uuid') == UUID) {
        return this._list[i];
      }
    }
    return null;
  }

  /**
   *  Returns the first Entity of the Entity list - does not affect the iterator
   *
   *  @method getFirstEntity
   *  @return {object} returns an entity object
   */
  Usergrid.Collection.prototype.getFirstEntity = function (){
    var count = this._list.length;
      if (count > 0) {
        return this._list[0];
      }
      return null;
  }

  /**
   *  Returns the last Entity of the Entity list - does not affect the iterator
   *
   *  @method getLastEntity
   *  @return {object} returns an entity object
   */
  Usergrid.Collection.prototype.getLastEntity = function (){
    var count = this._list.length;
      if (count > 0) {
        return this._list[count-1];
      }
      return null;
  }

  /**
   *  Entity iteration -Checks to see if there is a "next" entity
   *  in the list.  The first time this method is called on an entity
   *  list, or after the resetEntityPointer method is called, it will
   *  return true referencing the first entity in the list
   *
   *  @method hasNextEntity
   *  @return {boolean} true if there is a next entity, false if not
   */
  Usergrid.Collection.prototype.hasNextEntity = function (){
    var next = this._iterator + 1;
      if(next >=0 && next < this._list.length) {
        return true;
      }
      return false;
  }

  /**
   *  Entity iteration - Gets the "next" entity in the list.  The first
   *  time this method is called on an entity list, or after the method
   *  resetEntityPointer is called, it will return the,
   *  first entity in the list
   *
   *  @method hasNextEntity
   *  @return {object} entity
   */
  Usergrid.Collection.prototype.getNextEntity = function (){
    this._iterator++;
      if(this._iterator >= 0 && this._iterator <= this._list.length) {
        return this._list[this._iterator];
      }
      return false;
  }

  /**
   *  Entity iteration - Checks to see if there is a "previous"
   *  entity in the list.
   *
   *  @method hasPreviousEntity
   *  @return {boolean} true if there is a previous entity, false if not
   */
  Usergrid.Collection.prototype.hasPreviousEntity = function (){
    var previous = this._iterator - 1;
      if(previous >=0 && previous < this._list.length) {
        return true;
      }
      return false;
  }

  /**
   *  Entity iteration - Gets the "previous" entity in the list.
   *
   *  @method getPreviousEntity
   *  @return {object} entity
   */
  Usergrid.Collection.prototype.getPreviousEntity = function (){
     this._iterator--;
      if(this._iterator >= 0 && this._iterator <= this._list.length) {
        return this.list[this._iterator];
      }
      return false;
  }

  /**
   *  Entity iteration - Resets the iterator back to the beginning
   *  of the list
   *
   *  @method resetEntityPointer
   *  @return none
   */
  Usergrid.Collection.prototype.resetEntityPointer = function (){
     this._iterator  = -1;
  }

  /**
   *  gets and array of all entities currently in the colleciton object
   *
   *  @method getEntityList
   *  @return {array} returns an array of entity objects
   */
  Usergrid.Collection.prototype.getEntityList = function (){
     return this._list;
  }

  /**
   *  sets the entity list
   *
   *  @method setEntityList
   *  @param {array} list - an array of Entity objects
   *  @return none
   */
  Usergrid.Collection.prototype.setEntityList = function (list){
    this._list = list;
  }

  /**
   *  Paging -  checks to see if there is a next page od data
   *
   *  @method hasNextPage
   *  @return {boolean} returns true if there is a next page of data, false otherwise
   */
  Usergrid.Collection.prototype.hasNextPage = function (){
    return this.hasNext();
  }

  /**
   *  Paging - advances the cursor and gets the next
   *  page of data from the API.  Stores returned entities
   *  in the Entity list.
   *
   *  @method getNextPage
   *  @return none
   */
  Usergrid.Collection.prototype.getNextPage = function (){
    if (this.hasNext()) {
        //set the cursor to the next page of data
        this.getNext();
        //empty the list
        this.setEntityList([]);
        Usergrid.ApiClient.runAppQuery(this);
      }
  }

  /**
   *  Paging -  checks to see if there is a previous page od data
   *
   *  @method hasPreviousPage
   *  @return {boolean} returns true if there is a previous page of data, false otherwise
   */
  Usergrid.Collection.prototype.hasPreviousPage = function (){
    return this.hasPrevious();
  }

  /**
   *  Paging - reverts the cursor and gets the previous
   *  page of data from the API.  Stores returned entities
   *  in the Entity list.
   *
   *  @method getPreviousPage
   *  @return none
   */
  Usergrid.Collection.prototype.getPreviousPage = function (){
    if (this.hasPrevious()) {
        this.getPrevious();
        //empty the list
        this.setEntityList([]);
        Usergrid.ApiClient.runAppQuery(this);
      }
  }

  /**
   *  clears the query parameters object
   *
   *  @method clearQuery
   *  @return none
   */
  Usergrid.Collection.prototype.clearQuery = function (){
    this.clearAll();
  }

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
   */
  Usergrid.Collection.prototype.fetch = function (successCallback, errorCallback){
    var self = this;
    var queryParams = this.getQueryParams();
    //empty the list
    this.setEntityList([]);
    this.setAllQueryParams('GET', this.getPath(), null, queryParams,
      function(response) {
        if (response.entities) {
          this.resetEntityPointer();
          var count = response.entities.length;
          for (var i=0;i<count;i++) {
            var uuid = response.entities[i].uuid;
            if (uuid) {
              var entity = new Usergrid.Entity(self.getPath(), uuid);
              //store the data in the entity
              var data = response.entities[i] || {};
              delete data.uuid; //remove uuid from the object
              entity.set(data);
              //store the new entity in this collection
              self.addEntity(entity);
            }
          }
          if (typeof(successCallback) === "function"){
            successCallback(response);
          }
        } else {
          if (typeof(errorCallback) === "function"){
              errorCallback(response);
          }
        }
      },
      function(response) {
        if (typeof(errorCallback) === "function"){
            errorCallback(response);
        }
      }
    );
    Usergrid.ApiClient.runAppQuery(this);
  }

  /**
   *  A method to save all items currently stored in the collection object
   *  caveat with this method: we can't update anything except the items
   *  currently stored in the collection.
   *
   *  @method save
   *  @param {function} successCallback
   *  @param {function} errorCallback
   *  @return none
   */
  Usergrid.Collection.prototype.save = function (successCallback, errorCallback){
    //loop across all entities and save each one
    var entities = this.getEntityList();
    var count = entities.length;
    var jsonObj = [];
    for (var i=0;i<count;i++) {
      entity = entities[i];
      data = entity.get();
      if (entity.get('uuid')) {
        data.uuid = entity.get('uuid');
        jsonObj.push(data);
      }
      entity.save();
    }
    this.setAllQueryParams('PUT', this.getPath(), jsonObj, null,successCallback, errorCallback);
    Usergrid.ApiClient.runAppQuery(this);
  }
})(Usergrid);

