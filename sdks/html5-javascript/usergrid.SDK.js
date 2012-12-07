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

