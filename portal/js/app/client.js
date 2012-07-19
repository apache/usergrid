/*
 * usergrid.client.js
 *
 * Usage: This class makes the actual calls to the Usergrid API.
 *
 *
 */
/*
  *  @class session
  *  @purpose a class of standardized methods for accessing local storage
  *
  */
 var session = {
   getOrganizationObj: function () {
    var organization = localStorage.getObject('currentOrganization');
    return organization;
  },
  getOrganizationUUID : function() {
    var organization = localStorage.getObject('currentOrganization');
    return organization.uuid;
  },
  getOrganizationName : function() {
    var organization = localStorage.getObject('currentOrganization');
    return organization.name;
  },
  setOrganization : function(_organization) {
    localStorage.setObject('currentOrganization', _organization);
  },
  setOrganizationObj : function(_organization) {
    localStorage.setObject('currentOrganization', _organization);
  },
  setOrganizationName : function(_name) {
    organization = {};
    organization.name= _name;
    localStorage.setObject('currentOrganization', organization);
  },
  setOrganizationUUID : function(_uuid) {
    organization = {};
    organization.uuid = _uuid;
    localStorage.setObject('currentOrganization', organization);
  },

  //application id access and setter methods
  getApplicationId : function() {
    var applicationId = localStorage.getItem('currentApplicationId');
    return applicationId;
  },
  setApplicationId : function(_applicationId) {
    localStorage.setItem('currentApplicationId', _applicationId);
  },
  getApplicationName : function() {
    var applicationName = localStorage.getItem('currentApplicationName');
    return applicationName;
  },
  setApplicationName : function(_applicationName) {
    localStorage.setItem('currentApplicationName', _applicationName);
  },

  //logged in user access and setter methods
  getLoggedInUserObj : function() {
    var loggedInUser = localStorage.getObject('usergridUser');
    return loggedInUser;
  },
  getLoggedInUserUUID : function getLoggedInUserUUID() {
    var loggedInUser = localStorage.getObject('usergridUser');
    return loggedInUser.uuid;
  },
  getLoggedInUserEmail : function() {
    var loggedInUser = localStorage.getObject('usergridUser');
    return loggedInUser.email;
  },
  getLoggedInUserOrgs : function() {
    var loggedInUser = localStorage.getObject('usergridUser');
    return loggedInUser.organizations;
  },
  setLoggedInUser : function(_loggedInUser) {
    localStorage.setObject('usergridUser', _loggedInUser);
  },

  //access token access and setter methods
  getAccessToken : function() {
    var accessToken = localStorage.getItem('accessToken');
    return accessToken;
  },
  setAccessToken : function(_accessToken) {
    localStorage.setItem('accessToken', _accessToken);
  },

  //convenience method for saving all active user vars at once
  saveAll : function(_organization, _applicationId, _loggedInUser, _accessToken) {
    this.setOrganization(_organization);
    this.setApplicationId(_applicationId);
    this.setLoggedInUser(_loggedInUser);
    this.setAccessToken(_accessToken);
  },

  //convenience method for clearing all active user vars at once
  clearAll : function() {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('usergridUser');
    localStorage.removeItem('currentOrganization');
    localStorage.removeItem('currentApplicationId');
    localStorage.removeItem('currentApplicationName');
  },

  //method to check if user is logged in
  loggedIn : function() {
    var loggedInUser = this.getLoggedInUserObj();
    var accessToken = this.getAccessToken();
    return (loggedInUser && accessToken);
  }
}

var usergrid = usergrid || {};

usergrid.client = (function() {
  // reference to the session manager - used to access local storage
  //API endpoint
  var FORCE_PUBLIC_API = true; // Always use public API
  var PUBLIC_API_URL = "https://api.usergrid.com";

  //SSO information - Apigee Specific
  var APIGEE_TLD = "apigee.com";
  var USE_SSO = 'no'; // flag to overide use SSO if needed set to ?use_sso=no
  var APIGEE_SSO_URL = "https://accounts.apigee.com/accounts/sign_in";
  var APIGEE_SSO_PROFILE_URL = "https://accounts.apigee.com/accounts/my_account";
  var SSO_LOGOUT_PAGE = 'https://accounts.apigee.com/accounts/sign_out';

  // for running Usergrid as a local server
  var LOCAL_STANDALONE_API_URL = "http://localhost:8080";
  var LOCAL_TOMCAT_API_URL = "http://localhost:8080/ROOT";
  var LOCAL_API_URL = LOCAL_STANDALONE_API_URL;

  //initialization method should be called up front
  function Init(clientId, clientSecret, apiUrl) {
    self.clientId = clientId || null;
    self.clientSecret = clientSecret || null;

    if (!FORCE_PUBLIC_API && (document.domain.substring(0,9) == "localhost")) {
      self.apiUrl = LOCAL_API_URL;
    }

    if (query_params.api_url) {
      self.apiUrl = query_params.api_url;
    }

    self.use_sso = USE_SSO;
    if (query_params.use_sso) {
      self.use_sso = query_params.use_sso;
    }

    self.apigee_sso_url = APIGEE_SSO_URL;
    if (query_params.apigee_sso_url) {
      self.apigee_sso_url = query_params.apigee_sso_url;
    }

    self.apigee_sso_profile_url = APIGEE_SSO_PROFILE_URL;
    if (query_params.apigee_sso_profile_url) {
      self.apigee_sso_profile_url = query_params.apigee_sso_profile_url;
    }

    if (apiUrl) {
      self.apiUrl = apiUrl;
    }

    self.resetPasswordUrl = self.apiUrl + "/management/users/resetpw";

    if (self.apiUrl != localStorage.getItem('usergrid_api_url')) {
      localStorage.setItem('usergrid_api_url', self.apiUrl);
    }

  }

  // The base for all API calls.
  function apiRequest(method, path, data, success, error) {

    var ajaxOptions = {
      type: method.toUpperCase(),
      url: self.apiUrl + path,
      success: success,
      error: error,
      data: data || {},
      contentType: "application/json; charset=utf-8",
      dataType: "json"
    }

    // work with ie for cross domain scripting
    var accessToken = session.getAccessToken();
    if (onIE) {
      ajaxOptions.dataType = "jsonp";
      if (accessToken) { ajaxOptions.data['access_token'] = accessToken }
    } else {
      ajaxOptions.beforeSend = function(xhr) {
        if (accessToken) { xhr.setRequestHeader("Authorization", "Bearer " + accessToken) }
      }
    }

    $.ajax(ajaxOptions);
  }

  //method to urlencode an array of parameters
  function encodeParams(params) {
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

  /*******************************************************************
   *
   * Complex App endpoints
   *
   ******************************************************************/
  function setCurrentOrganization(orgName) {
    var organizations = session.getLoggedInUserOrgs();
    if (!session.getLoggedInUserObj() || ! organizations) {
      return;
    }

    if (orgName) {
      session.setOrganization(organizations[orgName]);
    } else {
      var firstOrg = null;
      for (firstOrg in organizations) {break;}
      if (firstOrg) {
        session.setOrganization(organizations[firstOrg]);
      }
    }
  }


  function loginAdmin(email, password, successCallback, errorCallback) {
    session.clearAll();
    var formdata = {
      grant_type: "password",
      username: email,
      password: password
    };
    runManagementQuery(new queryObj('GET', 'token', null, formdata,
      function(response) {
        if (!response) {
          errorCallback;
          return
        }
        var firstOrg = null;
        var organization = null;
        for (firstOrg in response.user.organizations) {break;}
        if (firstOrg) {
          organization = response.user.organizations[firstOrg];
        }
        session.saveAll(organization, null, response.user, response.access_token);
        if (successCallback) {
          successCallback(response);
        }
      },
      errorCallback
    ));
  }

  function loginAppUser(applicationId, email, password, success, failure) {
    session.clearIt();
    var formdata = {
      username: email,
      password: '',
      invite: true
    };
    apiRequest("POST", "/" + session.getOrganizationUUID() + "/" + applicationId + "/token", formdata,
    function(response) {
      if (response && response.access_token && response.user) {
        session.setLoggedInUser(response.user) ;
        session.getAccessToken(response.access_token);
        setCurrentOrganization();
        if (success) {
          success();
        }
      } else if (failure) {
        failure();
      }
    },
    function(response, textStatus, xhr) {
      if (failure) {
        failure();
      }
    }
  );
  }

  function autoLogin(successCallback, errorCallback) {
    // check to see if the user has a valid token
    if (!session.loggedIn()) {
      // test to see if the Portal is running on Apigee, if so, send to SSO, if not, fall through to login screen
      if ( useSSO() ){
        Pages.clearPage();
        sendToSSOLoginPage();
      }
    } else if (session.loggedIn()) {
      runManagementQuery(new queryObj("GET","users/" + session.getLoggedInUserEmail(), null, null,
        function(response) {
          if (!response) {
            errorCallback;
            return
          }
          var firstOrg = null;
          var organization = null;
          for (firstOrg in response.data.organizations) {break;}
          if (firstOrg) {
            organization = response.data.organizations[firstOrg];
          }
          session.saveAll(organization, null, response.data, response.data.token);
          if (successCallback) {
            successCallback(response);
          }
        },
        function() {
          session.clearAll();
          errorCallback;
        }
      ));
      return;
    } else {
      errorCallback()
    }
  }

  function requestCollectionIndexes(applicationId, path, success, failure) {
    if (path.lastIndexOf("/", 0) !== 0) {
      path = path;
    }
    path = path + "/indexes";
    apiRequest("GET", path, null, success, failure);
  }

  /*******************************************************************
   *
   * Query object
   *
   * Usage: The goal of the query object is to make it easy to run any
   *        kind of CRUD call against the API.  This is done as follows:
   *
   *        1.  Create a query object:
   *        queryObj = new client.queryObj("GET", "users", null, function() { alert("success"); }, function() { alert("failure"); });
   *
   *        2.  Run the query by calling the appropriate endpoint call
   *        runAppQuery(queryObj);
   *        or
   *        runManagementQuery(queryObj);
   *
   *        3. Paging - The queryObj holds the cursor information.  To
   *        use, simply bind click events to functions that call the
   *        getNext and getPrevious methods of the query object.  This
   *        will set the cursor correctly, and the runAppQuery method
   *        can be called again using the same queryObj:
   *        runAppQuery(queryObj);
   *
   ******************************************************************/

  /*
   *  @class queryObj
   *  @purpose a class for holding all query information and paging state
   *  @purpose a query object that will contain all relevant info for API call
   *  @param _method REQUIRED - GET, POST, PUT, DELETE
   *  @param _path REQUIRED - API resource (e.g. "users" or "users/rod", should not include http URL or org_name/app_name)
   *  @param _jsonObj NULLABLE - a json data object to be passed to the API
   *  @param _params NULLABLE - query parameters to be encoded and added to the API URL
   *  @param _successCallback REQUIRED - the success callback function
   *  @param _failureCallback REQUIRED - the failure callback function
   *
   */
  queryObj = (function(_method, _path, _jsonObj, _params, _successCallback, _failureCallback) {
    //query vars
    this.method = _method;
    this.path = _path;
    this.jsonObj = _jsonObj;
    this.params = _params;
    this.successCallback = _successCallback;
    this.failureCallback = _failureCallback;

    //paging vars
    this.cursor = null;
    this.next = null
    this.previous = [];
  });
  //methods for accessing query vars
  queryObj.prototype.getMethod = function getMethod() { return this.method; }
  queryObj.prototype.setMethod = function setMethod(_method) { this.method = _method; }

  queryObj.prototype.getPath = function getPath() { return this.path; }
  queryObj.prototype.setPath = function setPath(_path) { this.path = _path; }

  queryObj.prototype.getJsonObj = function getJsonObj() { return this.jsonObj; }
  queryObj.prototype.setJsonObj = function setJsonObj(_jsonObj) { this.jsonObj = _jsonObj; }

  queryObj.prototype.getParams = function getParams() { return this.params; }
  queryObj.prototype.setParams = function setParams(_params) { this.params = _params; }

  queryObj.prototype.getSuccessCallback = function getSuccessCallback() { return this.successCallback; }
  queryObj.prototype.setSuccessCallback = function setSuccessCallback(_successCallback) { this.successCallback = _successCallback; }
  queryObj.prototype.callSuccessCallback = function callSuccessCallback(response) { this.successCallback(response); }

  queryObj.prototype.getFailureCallback = function getFailureCallback() { return this.failureCallback; }
  queryObj.prototype.setFailureCallback = function setFailureCallback(_failureCallback) { this.failureCallback = _failureCallback; }
  queryObj.prototype.callFailureCallback = function callFailureCallback(response) { this.failureCallback(response); }

  //methods for accessing paging functions
  queryObj.prototype.resetPaging = function resetPaging() {
    this.previous = [];
    this.next = null;
    this.cursor = null;
  }

  queryObj.prototype.hasPrevious = function hasPrevious() {
    return (this.previous.length > 0);
  }

  queryObj.prototype.getPrevious = function getPrevious() {
    this.next=null; //clear out next so the comparison will find the next item
    this.cursor = this.previous.pop();
  }

  queryObj.prototype.hasNext = function hasNext(){
    return (this.next);
  }

  queryObj.prototype.getNext = function getNext() {
    this.previous.push(this.cursor);
    this.cursor = this.next;
  }

  queryObj.prototype.saveCursor = function saveCursor(_cursor) {
    this.cursor = this.next; //what was new is old again
    //if current cursor is different, grab it for next cursor
    if (this.next != _cursor) {
      this.next = _cursor;
    } else {
      this.next = null;
    }
  }

  queryObj.prototype.getCursor = function getCursor() {
    return this.cursor;
  }


  /*
   *  @function runAppQuery
   *  @purpose public function to run calls against the app endpoint
   *  @params queryObj - {method, path, jsonObj, params, successCallback, failureCallback}
   *
   */
  function runAppQuery(_queryObj) {
    var endpoint = "/" + session.getOrganizationUUID() + "/" + session.getApplicationId() + "/";
    processQuery(_queryObj, endpoint);
  }

  /*
   *  @function runManagementQuery
   *  @purpose public function to run calls against the management endpoint
   *  @params queryObj - {method, path, jsonObj, params, successCallback, failureCallback}
   *
   */
  function runManagementQuery(_queryObj) {
    var endpoint = "/management/";
    processQuery(_queryObj, endpoint)
  }

  /*
   *  @function processQuery
   *  @purpose to validate and prepare a call to the API
   *  @params queryObj - {method, path, jsonObj, params, successCallback, failureCallback}
   *
   */
  function processQuery(_queryObj, endpoint) {
    var curl = "curl";
    //validate parameters
    try {
      var method = _queryObj.getMethod().toUpperCase();
      var path = _queryObj.getPath();
      var jsonObj = _queryObj.getJsonObj() || {};
      var params = _queryObj.getParams() || {};

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
      var application_name = session.getApplicationName();
      if (application_name) {
        application_name = application_nametoUpperCase();
      }
      if (application_name != 'SANDBOX' && session.getAccessToken()) {
        curl += ' -i -H "Authorization: Bearer ' + session.getAccessToken() + '"';
      }
      
      //params - make sure we have a valid json object
      _params = JSON.stringify(params)
      if (!jsonlint.parse(_params)) {
        throw(new Error('Params object is not valid.'));
      }

      //add in the cursor if one is available
      if (_queryObj.getCursor()) {
        params.cursor = _queryObj.getCursor();
      } else {
        delete params.cursor;
      }

      //add the endpoint to the path
      path = endpoint + path;

      //make sure path never has more than one / together
      if (path) {
        //regex to strip multiple slashes
        while(path.indexOf('//') != -1){
          path = path.replace('//', '/');
        }
      }

      //curl - append the path
      curl += " " + self.apiUrl + path;

      //curl - append params to the path for curl prior to adding the timestamp
      var encoded_params = encodeParams(params);
      if (encoded_params) {
        curl += "?" + encoded_params;
      }

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
      if (!jsonlint.parse(jsonObj)) {
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

    //send query
    apiRequest(method, path, jsonObj,
      function(response) {
        //query completed succesfully, so store cursor
        _queryObj.saveCursor(response.cursor);
        //then call the original callback
        _queryObj.callSuccessCallback(response);
      },
      function(response) {
        console.log('API call failed - ' + response.responseText);
        _queryObj.callFailureCallback(response)
      });
  }

  /*******************************************************************
   *
   * SSO functions
   *
   ******************************************************************/
  function useSSO() {
    return apigeeUser() || self.use_sso=='true' || self.use_sso=='yes'
  }

  function apigeeUser() {
    return window.location.host == APIGEE_TLD
  }

  function sendToSSOLogoutPage() {
    var newLoc= self.sso_logout_page + '?callback=' + getSSOCallback();
    window.location = newLoc;
    return false;
  }

  function sendToSSOLoginPage() {
    var newLoc = self.apigee_sso_url + '?callback=' + getSSOCallback();
    window.location = newLoc;
    throw "stop!";
    return false;
  }

  function sendToSSOProfilePage() {
    var newLoc = self.apigee_sso_profile_url + '?callback=' + getSSOCallback();
    window.location = newLoc;
    throw "stop!";
    return false;
  }

  function getSSOCallback() {
    var callback = window.location.protocol+'//'+ window.location.host + window.location.pathname;
    var separatorMark = '?';
    if (self.use_sso == 'true' || self.use_sso == 'yes') {
      callback = callback + separatorMark + 'use_sso=' + self.use_sso;
      separatorMark = '&';
    }
    if (self.apiUrl != PUBLIC_API_URL) {
      callback = callback + separatorMark + 'api_url=' + self.apiUrl;
      separatorMark = '&';
    }
    return encodeURIComponent(callback);
  }


  /*******************************************************************
   *
   * Public functions
   *
   ******************************************************************/
  var self = {
    Init: Init,
    apiUrl: PUBLIC_API_URL,
    sso_logout_page: SSO_LOGOUT_PAGE,
    error: null,
    activeRequests: 0,
    onActiveRequest: null,
    encodePathString: encodePathString,
    apiRequest: apiRequest,
    loginAdmin: loginAdmin,
    loginAppUser: loginAppUser,
    useSSO: useSSO,
    sendToSSOLogoutPage: sendToSSOLogoutPage,
    sendToSSOLoginPage: sendToSSOLoginPage,
    sendToSSOProfilePage: sendToSSOProfilePage,
    getSSOCallback: getSSOCallback,
    requestCollectionIndexes: requestCollectionIndexes,
    setCurrentOrganization: setCurrentOrganization,
    autoLogin: autoLogin,
    runAppQuery:runAppQuery,
    runManagementQuery:runManagementQuery,
    queryObj:queryObj,
    session:session
  }

  return self;
})();