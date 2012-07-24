
var usergrid = usergrid || {};

/**
 *  QueryObj is a class for holding all query information and paging state
 *
 *  The goal of the query object is to make it easy to run any
 *  kind of CRUD call against the API.  This is done as follows:
 *
 *  1.  Create a query object:
 *      queryObj = new client.queryObj("GET", "users", null, function() { alert("success"); }, function() { alert("failure"); });
 *
 *  2.  Run the query by calling the appropriate endpoint call
 *      runAppQuery(queryObj);
 *      or
 *      runManagementQuery(queryObj);
 *
 *  3.  Paging - The queryObj holds the cursor information.  To
 *      use, simply bind click events to functions that call the
 *      getNext and getPrevious methods of the query object.  This
 *      will set the cursor correctly, and the runAppQuery method
 *      can be called again using the same queryObj:
 *      runAppQuery(queryObj);
 *
 *  @class QueryObj
 *  @param method REQUIRED - GET, POST, PUT, DELETE
 *  @param path REQUIRED - API resource (e.g. "users" or "users/rod", should not include http URL or org_name/app_name)
 *  @param jsonObj NULLABLE - a json data object to be passed to the API
 *  @param params NULLABLE - query parameters to be encoded and added to the API URL
 *  @param successCallback REQUIRED - the success callback function
 *  @param failureCallback REQUIRED - the failure callback function
 *
 */
QueryObj = (function(method, path, jsonObj, params, successCallback, failureCallback) {
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
QueryObj.prototype.getMethod = function getMethod() { return this._method; }
QueryObj.prototype.setMethod = function setMethod(method) { this._method = method; }

QueryObj.prototype.getPath = function getPath() { return this._path; }
QueryObj.prototype.setPath = function setPath(path) { this._path = path; }

QueryObj.prototype.getJsonObj = function getJsonObj() { return this._jsonObj; }
QueryObj.prototype.setJsonObj = function setJsonObj(jsonObj) { this._jsonObj = jsonObj; }

QueryObj.prototype.getParams = function getParams() { return this._params; }
QueryObj.prototype.setParams = function setParams(params) { this._params = params; }

QueryObj.prototype.getSuccessCallback = function getSuccessCallback() { return this._successCallback; }
QueryObj.prototype.setSuccessCallback = function setSuccessCallback(successCallback) { this._successCallback = successCallback; }
QueryObj.prototype.callSuccessCallback = function callSuccessCallback(response) { this._successCallback(response); }

QueryObj.prototype.getFailureCallback = function getFailureCallback() { return this._failureCallback; }
QueryObj.prototype.setFailureCallback = function setFailureCallback(failureCallback) { this._failureCallback = failureCallback; }
QueryObj.prototype.callFailureCallback = function callFailureCallback(response) { this._failureCallback(response); }

QueryObj.prototype.getCurl = function getCurl() { return this._curl; }
QueryObj.prototype.setCurl = function setCurl(curl) { this._curl = curl; }

QueryObj.prototype.getToken = function getToken() { return this._token; }
QueryObj.prototype.setToken = function setToken(token) { this._token = token; }

//methods for accessing paging functions
QueryObj.prototype.resetPaging = function resetPaging() {
  this._previous = [];
  this._next = null;
  this._cursor = null;
}

QueryObj.prototype.hasPrevious = function hasPrevious() {
  return (this._previous.length > 0);
}

QueryObj.prototype.getPrevious = function getPrevious() {
  this._next=null; //clear out next so the comparison will find the next item
  this._cursor = this._previous.pop();
}

QueryObj.prototype.hasNext = function hasNext(){
  return (this._next);
}

QueryObj.prototype.getNext = function getNext() {
  this._previous.push(this._cursor);
  this._cursor = this._next;
}

QueryObj.prototype.saveCursor = function saveCursor(cursor) {
  this._cursor = this._next; //what was new is old again
  //if current cursor is different, grab it for next cursor
  if (this._next != cursor) {
    this._next = cursor;
  } else {
    this._next = null;
  }
}

QueryObj.prototype.getCursor = function getCursor() {
  return this._cursor;
}

/**
* Client class is charged with making calls to the API endpoint
*
* @class Client
* @constructor
*/
function Client() {
  //API endpoint
  this._apiUrl = "https://api.usergrid.com";
  var clientId = null;
  var clientSecret = null;
}

/*
 *  allows API URL to be overridden
 *  @method setApiUrl
 */
Client.prototype.setApiUrl = function setApiUrl(apiUrl) { this._apiUrl = apiUrl; }
/*
 *  returns API URL
 *  @method getApiUrl
 */
Client.prototype.getApiUrl = function getApiUrl() { return this._apiUrl }

/*
 *  returns the api url of the reset pasword endpoint
 *  @method getResetPasswordUrl
 */
Client.prototype.getResetPasswordUrl = function getResetPasswordUrl() { this.getApiUrl() + "/management/users/resetpw" }


/*
 *  public function to run calls against the app endpoint
 *  @method runAppQuery
 *  @params queryObj - {method, path, jsonObj, params, successCallback, failureCallback}
 *
 */
Client.prototype.runAppQuery = function runAppQuery(queryObj) {
  //var endpoint = "/" + usergrid.session.getOrganizationUUID() + "/" + usergrid.session.getApplicationId() + "/";
  var endpoint = "/" + usergrid.session.getOrganizationName() + "/" + usergrid.session.getApplicationName() + "/";
  this.processQuery(queryObj, endpoint);
}

/*
 *  public function to run calls against the management endpoint
 *  @method runManagementQuery
 *  @params queryObj - {method, path, jsonObj, params, successCallback, failureCallback}
 *
 */
Client.prototype.runManagementQuery = function runManagementQuery(queryObj) {
  var endpoint = "/management/";
  this.processQuery(queryObj, endpoint)
}

/*
 *  @method processQuery
 *  @purpose to validate and prepare a call to the API
 *  @params queryObj - {method, path, jsonObj, params, successCallback, failureCallback}
 *
 */
Client.prototype.processQuery = function processQuery(queryObj, endpoint) {
  var curl = "curl";
  //validate parameters
  try {
    var method = queryObj.getMethod().toUpperCase();
    var path = queryObj.getPath();
    var jsonObj = queryObj.getJsonObj() || {};
    var params = queryObj.getParams() || {};

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
      application_name = application_name.toUpperCase();
    }
    if (application_name != 'SANDBOX' && usergrid.session.getAccessToken()) {
      curl += ' -i -H "Authorization: Bearer ' + usergrid.session.getAccessToken() + '"';
      queryObj.setToken(true);
    }

    //params - make sure we have a valid json object
    _params = JSON.stringify(params)
    if (!jsonlint.parse(_params)) {
      throw(new Error('Params object is not valid.'));
    }

    //add in the cursor if one is available
    if (queryObj.getCursor()) {
      params.cursor = queryObj.getCursor();
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
  queryObj.setCurl(curl);

  var ajaxOptions = {
    type: method,
    url: this.getApiUrl() + path,
    success: function(response) {
      //query completed succesfully, so store cursor
      queryObj.saveCursor(response.cursor);
      //then call the original callback
      queryObj.callSuccessCallback(response);
    },
    error: function(response) {
      console.log('API call failed - ' + response.responseText);
      queryObj.callFailureCallback(response)
    },
    data: jsonObj || {},
    contentType: "application/json; charset=utf-8",
    dataType: "json"
  }

  // work with ie for cross domain scripting
  var accessToken = usergrid.session.getAccessToken();
  if (onIE) {
    ajaxOptions.dataType = "jsonp";
    if (application_name != 'SANDBOX' && accessToken) { ajaxOptions.data['access_token'] = accessToken }
  } else {
    ajaxOptions.beforeSend = function(xhr) {
      if (application_name != 'SANDBOX' && accessToken) { xhr.setRequestHeader("Authorization", "Bearer " + accessToken) }
    }
  }

  $.ajax(ajaxOptions);
}

/**
 *  @method encodeParams
 *  @purpose - to encode the query string parameters
 *  @params params - an object of name value pairs that will be urlencoded
 *
 */
Client.prototype.encodeParams = function encodeParams(params) {
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

/**
 *  @method loginAdmin
 *  @purpose - to authenticate an admin user and store the token and org list
 *  @params email - the admin's email (or username)
 *  @params password - the admin's password
 *  @params successCallback - callback function for success
 *  @params errorCallback - callback function for error
 */
Client.prototype.loginAdmin = function loginAdmin(email, password, successCallback, errorCallback) {
  usergrid.session.clearAll();
  var formdata = {
    grant_type: "password",
    username: email,
    password: password
  };
  this.runManagementQuery(new QueryObj('GET', 'token', null, formdata,
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

/**
 *  @method autoLogin
 *  @purpose - to reauthenticate an admin who already has a token
 *  @params successCallback - callback function for success
 *  @params errorCallback - callback function for error
 */
Client.prototype.autoLogin = function autoLogin(successCallback, errorCallback) {
  this.runManagementQuery(new QueryObj("GET","users/" + usergrid.session.getLoggedInUserEmail(), null, null,
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
usergrid.client = new Client();

/*
 *  @class session
 *  @purpose a class of standardized methods for accessing local storage
 *
 */
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
  setApplicationName: setApplicationName,
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