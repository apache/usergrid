/*
 * usergrid.client.js
 *
 * Usage: This class makes the actual calls to the Usergrid API.
 *
 *
 */

usergrid.client = (function() {

  // This code block *WILL* load before the document is complete

  var session = usergrid.session;

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
  function Init(applicationId, clientId, clientSecret, apiUrl) {

    session.applicationId = applicationId || null;
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

    // This hack is necesary for IE9. IE is too strict when it comes to cross-domain.
    if (onIE) {
      ajaxOptions.dataType = "jsonp";
      if (session.accessToken) { ajaxOptions.data['access_token'] = session.accessToken }
    } else {
      ajaxOptions.beforeSend = function(xhr) {
        if (session.accessToken) { xhr.setRequestHeader("Authorization", "Bearer " + session.accessToken) }
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
   * Management endpoints
   *
   ******************************************************************/
  function requestApplications(success, failure) {
    if (!session.currentOrganization) {
      failure();
    }
    apiRequest("GET", "/management/organizations/" + session.currentOrganization.uuid + "/applications", null, success, failure);
  }

  function createApplication(data, success, failure) {
    if (!session.currentOrganization) {
      failure();
    }
    apiRequest("POST", "/management/organizations/" + session.currentOrganization.uuid + "/applications", JSON.stringify(data), success, failure);
  }

  function requestAdmins(success, failure) {
    if (!session.currentOrganization) {
      failure();
    }
    apiRequest("GET", "/management/organizations/" + session.currentOrganization.uuid + "/users", null, success, failure);
  }

  function createOrganization(data, success, failure) {
    if (!session.loggedInUser) {
      failure();
    }
    apiRequest("POST", "/management/users/" + session.loggedInUser.uuid + "/organizations", JSON.stringify(data), success, failure);
  }

  function leaveOrganization(organizationUUID, success, failure) {
    if (!session.loggedInUser) {
      failure();
    }
    apiRequest("DELETE", "/management/users/" + session.loggedInUser.uuid + "/organizations/" + organizationUUID, null, success, failure);
  }

  function requestOrganizations(success, failure) {
    apiRequest("GET", "/management/users/" + session.loggedInUser.uuid + "/organizations", null, success, failure);
  }

  function requestOrganizationCredentials(success, failure) {
    if (!session.currentOrganization) {
      failure();
    }
    apiRequest("GET", "/management/organizations/" + session.currentOrganization.uuid + "/credentials", null, success, failure);
  }

  function regenerateOrganizationCredentials(success, failure) {
    if (!session.currentOrganization) {
      failure();
    }
    apiRequest("POST", "/management/organizations/" + session.currentOrganization.uuid + "/credentials", null, success, failure);
  }

  function createAdmin(data, success, failure) {
    if (!session.currentOrganization) {
      failure();
    }
    apiRequest("POST", "/management/organizations/" + session.currentOrganization.uuid + "/users", JSON.stringify(data), success, failure);
  }

  function requestAdminUser(success, failure) {
    if (!session.loggedInUser) {
      failure();
    }
    apiRequest("GET", "/management/users/" + session.loggedInUser.uuid, null, success, failure);
  }

  function updateAdminUser(properties, success, failure) {
    if (!session.loggedInUser) {
      failure();
    }
    apiRequest("PUT", "/management/users/" + session.loggedInUser.uuid, JSON.stringify(properties), success, failure);
  }

  function requestAdminFeed(success, failure) {
    if (!session.loggedInUser) {
      failure();
    }
    apiRequest("GET", "/management/users/" + session.loggedInUser.uuid + "/feed", null, success, failure);
  }

  /*******************************************************************
   *
   * Complex App endpoints
   *
   ******************************************************************/
  function createCollection(applicationId, data, success, failure) {
    var collections = {};
    collections[data.name] = {};
    var metadata = {
      metadata: {
        collections: collections
      }
    };
    apiRequest("PUT", "/" + session.currentOrganization.uuid + "/" + applicationId, JSON.stringify(metadata), success, failure);
  }

  function requestApplicationCounters(applicationId, start_time, end_time, resolution, counter, success, failure) {
    var params = {};
    if (start_time) params.start_time = start_time;
    if (end_time) params.end_time = end_time;
    if (resolution) params.resolution = resolution;
    if (counter) params.counter = counter;
    params.pad = true;
    apiRequest("GET", "/" + session.currentOrganization.uuid + "/" + applicationId + "/counters", params, success, failure);
  }

  function loginAdmin(email, password, successCallback, errorCallback) {
    session.clearIt();
    var formdata = {
      grant_type: "password",
      username: email,
      password: password
    };
    apiRequest("GET", "/management/token", formdata,
      function(data, textStatus, xhr) {
        if (!data) {
          errorCallback();
          return
        }
        session.loggedInUser = data.user;
        session.accessToken = data.access_token;
        setCurrentOrganization();
        session.saveIt();
        if (successCallback) {
          successCallback(data, textStatus, xhr);
        }
      },
      errorCallback
    );
  }

  function loginAppUser(applicationId, email, password, success, failure) {
    session.clearIt();
    var formdata = {
      username: email,
      password: '',
      invite: true
    };
    apiRequest("POST", "/" + session.currentOrganization.uuid + "/" + applicationId + "/token", formdata,
               function(response) {
                 if (response && response.access_token && response.user) {
                   session.loggedInUser = response.user;
                   session.accessToken = response.access_token;
                   setCurrentOrganization();
                   localStorage.setObject('usergridUser', session.loggedInUser);
                   localStorage.setItem('accessToken', session.accessToken);
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

  function renewToken(successCallback, errorCallback) {
    apiRequest("GET", "/management/users/" + session.loggedInUser.email, null,
                function(data, status, xhr) {
                  if (!data || !data.data) {
                    errorCallback();
                    return
                  }
                  session.loggedInUser = data.data;
		  setCurrentOrganization();
		  session.saveIt();

                  if (successCallback) {
                    successCallback(data);
                  }
                },
                errorCallback
               );
  }

  function signup(organization, username, name, email, password, success, failure) {
    var formdata = {
      organization: organization,
      username: username,
      name: name,
      email: email,
      password: password
    };
    apiRequest("POST", "/management/organizations", formdata,
               function(response) {
                 if (response && response.data) {
                   if (success) {
                     success(response);
                   }
                 } else if (failure) {
                   failure(response);
                 }
               },
               function(XMLHttpRequest, textStatus, errorThrown) {
                 if (failure) {
                   failure();
                 }
               }
              );
  }

  function setCurrentOrganization(orgName) {
    session.currentOrganization = null;
    if (!session.loggedInUser || !session.loggedInUser.organizations) {
      return;
    }

    if (orgName) {
      session.currentOrganization = session.loggedInUser.organizations[orgName];
    } else {
      session.currentOrganization = session.loggedInUser.organizations[localStorage.getObject('currentOrganization')];
    }

    if (!session.currentOrganization) {
      var firstOrg = null;
      for (firstOrg in session.loggedInUser.organizations) {break;}
      if (firstOrg) {
        session.currentOrganization = session.loggedInUser.organizations[firstOrg];
      }
    }

    localStorage.currentOrganization = session.currentOrganization;
    session.saveIt();
  }

  function autoLogin(successCallback, errorCallback) {
    session.readIt();
    // check to see if the user has a valid token
    if (!session.loggedIn()) {
      // test to see if the Portal is running on Apigee, if so, send to SSO, if not, fall through to login screen
      if ( useSSO() ){
        Pages.clearPage();
        sendToSSOLoginPage();
      }
    } else if (session.loggedIn()) {
      renewToken(
        function() {
          session.readIt();
          successCallback();
        },
        function() {
          session.clearIt();
          errorCallback();
        }
      );
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
   *  @param method REQUIRED - GET, POST, PUT, DELETE
   *  @param path REQUIRED - API resource (e.g. "users" or "users/rod", should not include http URL or org_name/app_name)
   *  @param jsonObj NULLABLE - a json data object to be passed to the API
   *  @param params NULLABLE - query parameters to be encoded and added to the API URL
   *
   */
  queryObj = (function(method, path, jsonObj, params, successCallback, failureCallback) {
    //query vars (are all private)
    var method = method;
    var path = path;
    var jsonObj = jsonObj;
    var params = params;
    var successCallback = successCallback;
    var failureCallback = failureCallback;

    //paging vars (are all private)
    var cursor = null;
    var next = null
    var previous = [];

    //methods for accessing query vars
    queryObj.prototype.getMethod = function getMethod() { return method; }
    queryObj.prototype.setMethod = function setMethod(_method) { method = _method; }

    queryObj.prototype.getPath = function getPath() { return path; }
    queryObj.prototype.setPath = function setPath(_path) { path = _path; }

    queryObj.prototype.getJsonObj = function getJsonObj() { return jsonObj; }
    queryObj.prototype.setJsonObj = function setJsonObj(_jsonObj) { jsonObj = _jsonObj; }

    queryObj.prototype.getParams = function getParams() { return params; }
    queryObj.prototype.setParams = function setParams(_params) { params = _params; }

    queryObj.prototype.getSuccessCallback = function getSuccessCallback() { return successCallback; }
    queryObj.prototype.callSuccessCallback = function setsuccessCallback(response) { successCallback(response); }

    queryObj.prototype.getFailureCallback = function getFailureCallback() { return failureCallback; }
    queryObj.prototype.setFailureCallback = function setFailureCallback(response) { failureCallback(response); }

    //methods for accessing paging functions
    queryObj.prototype.resetPaging = function resetPaging() {
      previous = [];
      next = null;
      cursor = null;
    }

    queryObj.prototype.hasPrevious = function hasPrevious() {
      return (previous.length > 0);
    }

    queryObj.prototype.getPrevious = function getPrevious() {
      next=null; //clear out next so the comparison will find the next item
      cursor = previous.pop();
    }

    queryObj.prototype.hasNext = function hasNext(){
      return (next);
    }

    queryObj.prototype.getNext = function getNext() {
      previous.push(cursor);
      cursor = next;
    }

    queryObj.prototype.saveCursor = function saveCursor(_cursor) {
      cursor = next; //what was new is old again
      //if current cursor is different, grab it for next cursor
      if (next != _cursor) {
        next = _cursor;
      } else {
        next = null;
      }
    }

    queryObj.prototype.getCursor = function getCursor() {
      return cursor;
    }
  });

  /*
   *  @function runAppQuery
   *  @purpose public function to run calls against the app endpoint
   *  @params queryObj - {method, path, jsonObj, params, successCallback, failureCallback}
   *
   */
  function runAppQuery(_queryObj) {
    var endpoint = "/" + session.currentOrganization.uuid + "/" + session.currentApplicationId + "/";
    processQuery(_queryObj, endpoint);
  }

  /*
   *  @function runManagementQuery
   *  @purpose public function to run calls against the management endpoint
   *  @params queryObj - {method, path, jsonObj, params, successCallback, failureCallback}
   *
   */
  function runManagementQuery(_queryObj) {
    var endpoint = "/management/users/";
    processQuery(_queryObj, endpoint)
  }

  /*
   *  @function processQuery
   *  @purpose to validate and prepare a call to the API
   *  @params queryObj - {method, path, jsonObj, params, successCallback, failureCallback}
   *
   */
  function processQuery(_queryObj, endpoint) {
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

      //add in a timestamp for gets and deletes
      if ((method == "GET") || (method == "DELETE")) {
        params['_'] = new Date().getTime();
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

      //path - append params and build out
      path += "?" + encodeParams(params);
      path = endpoint + path;
      //make sure path never has more than one / together
      if (path) {
        //regex to strip multiple slashes
        while(path.indexOf('//') != -1){
          path = path.replace('//', '/');
        }
      }

      //jsonObj - make sure we have a valid json object
      jsonObj = JSON.stringify(jsonObj)
      if (!jsonlint.parse(jsonObj)) {
        throw(new Error('JSON object is not valid.'));
      }
      if (jsonObj == '{}') {
        jsonObj = null;
      }

    } catch (e) {
      //parameter was invalid
      console.log('processQuery - error occured -' + e.message);
      return false;
    }

    //log the activity
    console.log("processQuery - " + method + " - " + path);

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
    requestApplications: requestApplications,
    createApplication: createApplication,
    requestAdmins: requestAdmins,
    createOrganization: createOrganization,
    leaveOrganization: leaveOrganization,
    requestOrganizations: requestOrganizations,
    requestOrganizationCredentials: requestOrganizationCredentials,
    regenerateOrganizationCredentials: regenerateOrganizationCredentials,
    createAdmin: createAdmin,
    createCollection: createCollection,
    requestApplicationCounters: requestApplicationCounters,
    requestAdminUser: requestAdminUser,
    updateAdminUser: updateAdminUser,
    requestAdminFeed: requestAdminFeed,
    loginAdmin: loginAdmin,
    loginAppUser: loginAppUser,
    useSSO: useSSO,
    sendToSSOLogoutPage: sendToSSOLogoutPage,
    sendToSSOLoginPage: sendToSSOLoginPage,
    sendToSSOProfilePage: sendToSSOProfilePage,
    getSSOCallback: getSSOCallback,
    signup: signup,
    requestCollectionIndexes: requestCollectionIndexes,
    setCurrentOrganization: setCurrentOrganization,
    autoLogin: autoLogin,
    runAppQuery:runAppQuery,
    runManagementQuery:runManagementQuery,
    queryObj:queryObj
  }

  return self
})();
