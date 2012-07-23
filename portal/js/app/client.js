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
var usergrid = usergrid || {};

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
  usergrid.queryObj = (function(method, path, jsonObj, params, successCallback, failureCallback) {
    //query vars
    this._method = method;
    this._path = path;
    this._jsonObj = jsonObj;
    this._params = params;
    this._successCallback = successCallback;
    this._failureCallback = failureCallback;

    //curl command - will be populated by runQuery function
    this._curl = '';
    this._token = false;

    //paging vars
    this._cursor = null;
    this._next = null
    this._previous = [];
  });
  //methods for accessing query vars
  usergrid.queryObj.prototype.getMethod = function getMethod() { return this._method; }
  usergrid.queryObj.prototype.setMethod = function setMethod(method) { this._method = method; }

  usergrid.queryObj.prototype.getPath = function getPath() { return this._path; }
  usergrid.queryObj.prototype.setPath = function setPath(path) { this._path = path; }

  usergrid.queryObj.prototype.getJsonObj = function getJsonObj() { return this._jsonObj; }
  usergrid.queryObj.prototype.setJsonObj = function setJsonObj(jsonObj) { this._jsonObj = jsonObj; }

  usergrid.queryObj.prototype.getParams = function getParams() { return this._params; }
  usergrid.queryObj.prototype.setParams = function setParams(params) { this._params = params; }

  usergrid.queryObj.prototype.getSuccessCallback = function getSuccessCallback() { return this._successCallback; }
  usergrid.queryObj.prototype.setSuccessCallback = function setSuccessCallback(successCallback) { this._successCallback = successCallback; }
  usergrid.queryObj.prototype.callSuccessCallback = function callSuccessCallback(response) { this._successCallback(response); }

  usergrid.queryObj.prototype.getFailureCallback = function getFailureCallback() { return this._failureCallback; }
  usergrid.queryObj.prototype.setFailureCallback = function setFailureCallback(failureCallback) { this._failureCallback = failureCallback; }
  usergrid.queryObj.prototype.callFailureCallback = function callFailureCallback(response) { this._failureCallback(response); }

  usergrid.queryObj.prototype.getCurl = function getCurl() { return this._curl; }
  usergrid.queryObj.prototype.setCurl = function setCurl(curl) { this._curl = curl; }

  usergrid.queryObj.prototype.getToken = function getToken() { return this._token; }
  usergrid.queryObj.prototype.setToken = function setToken(token) { this._token = token; }

  //methods for accessing paging functions
  usergrid.queryObj.prototype.resetPaging = function resetPaging() {
    this._previous = [];
    this._next = null;
    this._cursor = null;
  }

  usergrid.queryObj.prototype.hasPrevious = function hasPrevious() {
    return (this._previous.length > 0);
  }

  usergrid.queryObj.prototype.getPrevious = function getPrevious() {
    this._next=null; //clear out next so the comparison will find the next item
    this._cursor = this._previous.pop();
  }

  usergrid.queryObj.prototype.hasNext = function hasNext(){
    return (this._next);
  }

  usergrid.queryObj.prototype.getNext = function getNext() {
    this._previous.push(this._cursor);
    this._cursor = this._next;
  }

  usergrid.queryObj.prototype.saveCursor = function saveCursor(cursor) {
    this._cursor = this._next; //what was new is old again
    //if current cursor is different, grab it for next cursor
    if (this._next != cursor) {
      this._next = cursor;
    } else {
      this._next = null;
    }
  }

  usergrid.queryObj.prototype.getCursor = function getCursor() {
    return this._cursor;
  }

  
 usergrid.session = (function() {
   function getOrganizationObj () {
    var organization = localStorage.getObject('currentOrganization');
    return organization;
  }
  function getOrganizationUUID () {
    var organization = localStorage.getObject('currentOrganization');
    return organization.uuid;
  }
  function getOrganizationName() {
    var organization = localStorage.getObject('currentOrganization');
    return organization.name;
  }
  function setOrganization(_organization) {
    localStorage.setObject('currentOrganization', _organization);
  }
  function setOrganizationObj(_organization) {
    localStorage.setObject('currentOrganization', _organization);
  }
  function setOrganizationName(_name) {
    organization = {};
    organization.name= _name;
    localStorage.setObject('currentOrganization', organization);
  }
  function setOrganizationUUID(_uuid) {
    organization = {};
    organization.uuid = _uuid;
    localStorage.setObject('currentOrganization', organization);
  }

  //application id access and setter methods
  function getApplicationId() {
    var applicationId = localStorage.getItem('currentApplicationId');
    return applicationId;
  }
  function setApplicationId(_applicationId) {
    localStorage.setItem('currentApplicationId', _applicationId);
  }
  function getApplicationName() {
    var applicationName = localStorage.getItem('currentApplicationName');
    return applicationName;
  }
  function setApplicationName(_applicationName) {
    localStorage.setItem('currentApplicationName', _applicationName);
  }

  //logged in user access and setter methods
  function getLoggedInUserObj() {
    var loggedInUser = localStorage.getObject('usergridUser');
    return loggedInUser;
  }
  function getLoggedInUserUUID() {
    var loggedInUser = localStorage.getObject('usergridUser');
    return loggedInUser.uuid;
  }
  function getLoggedInUserEmail() {
    var loggedInUser = localStorage.getObject('usergridUser');
    return loggedInUser.email;
  }
  function getLoggedInUserOrgs() {
    var loggedInUser = localStorage.getObject('usergridUser');
    return loggedInUser.organizations;
  }
  function setLoggedInUser(_loggedInUser) {
    localStorage.setObject('usergridUser', _loggedInUser);
  }

  //access token access and setter methods
  function getAccessToken() {
    var accessToken = localStorage.getItem('accessToken');
    return accessToken;
  }
  function setAccessToken(_accessToken) {
    localStorage.setItem('accessToken', _accessToken);
  }

  //convenience method for saving all active user vars at once
  function saveAll(_organization, _applicationId, _loggedInUser, _accessToken) {
    this.setOrganization(_organization);
    this.setApplicationId(_applicationId);
    this.setLoggedInUser(_loggedInUser);
    this.setAccessToken(_accessToken);
  }

  //convenience method for clearing all active user vars at once
  function clearAll() {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('usergridUser');
    localStorage.removeItem('currentOrganization');
    localStorage.removeItem('currentApplicationId');
    localStorage.removeItem('currentApplicationName');
  }

  //method to check if user is logged in
  function loggedIn() {
    var loggedInUser = this.getLoggedInUserObj();
    var accessToken = this.getAccessToken();
    return (loggedInUser && accessToken);
  }

  var self = {
    getOrganizationObj:getOrganizationObj,
    getOrganizationUUID:getOrganizationUUID,
    getOrganizationName: getOrganizationName,
    setOrganization: setOrganization,
    setOrganizationObj: setOrganizationObj,
    setOrganizationName: setOrganizationName,
    setOrganizationUUID:setOrganizationUUID,
    getApplicationId:getApplicationId,
    setApplicationId:setApplicationId,
    getApplicationName: getApplicationName,
    getLoggedInUserObj: getLoggedInUserObj,
    getLoggedInUserUUID: getLoggedInUserUUID,
    getLoggedInUserEmail: getLoggedInUserEmail,
    getLoggedInUserOrgs: getLoggedInUserOrgs,
    setLoggedInUser: setLoggedInUser,
    getAccessToken: getAccessToken,
    setAccessToken: setAccessToken,
    saveAll: saveAll,
    clearAll: clearAll,
    loggedIn: loggedIn
  }

  return self;
})();

function client() {
  // reference to the session manager - used to access local storage
  //API endpoint
  this._apiUrl = "https://api.usergrid.com";
  var clientId = null;
  var clientSecret = null;

}

client.prototype.setApiUrl = function setApiUrl(apiUrl) { this._apiUrl = apiUrl; }
client.prototype.getApiUrl = function getApiUrl() { return this._apiUrl }

client.prototype.getResetPasswordUrl = function getResetPasswordUrl() { this.getApiUrl() + "/management/users/resetpw" }

/*
  *  @function processQuery
  *  @purpose to validate and prepare a call to the API
  *  @params queryObj - {method, path, jsonObj, params, successCallback, failureCallback}
  *
  */
client.prototype.processQuery = function processQuery(_queryObj, endpoint) {
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
    var application_name = usergrid.session.getApplicationName();
    if (application_name) {
      application_name = application_nametoUpperCase();
    }
    if (application_name != 'SANDBOX' && usergrid.session.getAccessToken()) {
      curl += ' -i -H "Authorization: Bearer ' + usergrid.session.getAccessToken() + '"';
      _queryObj.setToken(true);
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
    curl += " " + this.getApiUrl() + path;

    //curl - append params to the path for curl prior to adding the timestamp
    var encoded_params = this.encodeParams(params);
    if (encoded_params) {
      curl += "?" + encoded_params;
    }

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
  //store the curl command back in the object
  _queryObj.setCurl(curl);

  //send query
  this.apiRequest(method, path, jsonObj,
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

/*
  *  @function encodeParams
  *  @purpose - to encode the query string parameters
  *  @params params - an object of name value pairs that will be urlencoded
  *
  */
client.prototype.encodeParams = function encodeParams(params) {
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
 *  @function runAppQuery
 *  @purpose public function to run calls against the app endpoint
 *  @params queryObj - {method, path, jsonObj, params, successCallback, failureCallback}
 *
 */
client.prototype.runAppQuery = function runAppQuery(_queryObj) {
  var endpoint = "/" + usergrid.session.getOrganizationUUID() + "/" + usergrid.session.getApplicationId() + "/";
  this.processQuery(_queryObj, endpoint);
}

/*
 *  @function runManagementQuery
 *  @purpose public function to run calls against the management endpoint
 *  @params queryObj - {method, path, jsonObj, params, successCallback, failureCallback}
 *
 */
client.prototype.runManagementQuery = function runManagementQuery(_queryObj) {
  var endpoint = "/management/";
  this.processQuery(_queryObj, endpoint)
}


/*
  *  @function apiRequest
  *  @purpose to run the API call
  *  @params queryObj - {method, path, data, successCallback, failureCallback}
  *  @notes - Do not call this method directly.  Use the runAppQuery and runManagementQuery funcitons instead
  *
  */
client.prototype.apiRequest = function apiRequest (method, path, data, successCallback, errorCallback) {

  var ajaxOptions = {
    type: method.toUpperCase(),
    url: this.getApiUrl() + path,
    success: successCallback,
    error: errorCallback,
    data: data || {},
    contentType: "application/json; charset=utf-8",
    dataType: "json"
  }

  // work with ie for cross domain scripting
  var accessToken = usergrid.session.getAccessToken();
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


/*******************************************************************
 *
 * login functions
 *
 ******************************************************************/
client.prototype.loginAdmin = function loginAdmin(email, password, successCallback, errorCallback) {   
  usergrid.session.clearAll();
  var formdata = {
    grant_type: "password",
    username: email,
    password: password
  };
  this.runManagementQuery(new usergrid.queryObj('GET', 'token', null, formdata,
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
      usergrid.session.saveAll(organization, null, response.user, response.access_token);
      if (successCallback) {
        successCallback(response);
      }
    },
    errorCallback
  ));
}

client.prototype.autoLogin = function autoLogin(successCallback, errorCallback) {
  this.runManagementQuery(new usergrid.queryObj("GET","users/" + usergrid.session.getLoggedInUserEmail(), null, null,
    function(response) {
      var firstOrg = null;
      var organization = null;
      for (firstOrg in response.data.organizations) {break;}
      if (firstOrg) {
        organization = response.data.organizations[firstOrg];
      }
      usergrid.session.saveAll(organization, null, response.data, response.data.token);
      if (successCallback) {
        successCallback(response);
      }
    },
    function(response) {
      usergrid.session.clearAll();
      if (errorCallback) {
        errorCallback(response);
      }
    }
  ));
  return;
}

usergrid.client = new client();