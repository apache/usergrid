/*
 * This file contains the all API calls AND ONLY that.
 *
 * No behavioural code should be included here (as of now, there is some though... refactoring)
 * Session management is included here. It'll be separated in a near future
 *
 */

usergrid.client = (function() {

  /* This code block *WILL* load before the document is complete */

  var session = usergrid.session;

  /* Always use public API */
  var FORCE_PUBLIC_API = true;

  var PUBLIC_API_URL = "https://api.usergrid.com";

  var APIGEE_TLD = "apigee.com";

  /*  flag to overide use SSO if needed set to ?use_sso=false */
  var USE_SSO = 'false';

  var APIGEE_SSO_URL = "https://accounts.apigee.com/accounts/sign_in";

  var APIGEE_SSO_PROFILE_URL = "https://accounts.apigee.com/accounts/my_account";

  var SSO_LOGOUT_PAGE = 'https://accounts.apigee.com/accounts/sign_out';

  var LOCAL_STANDALONE_API_URL = "http://localhost:8080";

  var LOCAL_TOMCAT_API_URL = "http://localhost:8080/ROOT";

  var LOCAL_API_URL = LOCAL_STANDALONE_API_URL;

  var response = {};

  function appPath(){
      return session.currentOrganization.uuid + "/" + session.currentApplicationId;
  }

  function Init(options) {
    var options = options || {};

    session.applicationId = options.applicationId || null;
    self.clientId = options.clientId || null;
    self.clientSecret = options.clientSecret || null;

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

    if (options.apiUrl) {
      self.apiUrl = options.apiUrl;
    }

    self.resetPasswordUrl = self.apiUrl + "/management/users/resetpw";

    if (self.apiUrl != localStorage.getItem('usergrid_api_url')) {
      localStorage.setItem('usergrid_api_url', self.apiUrl);
    }

  }

  // The base for all API calls. HANDLE WITH CAUTION!
  function apiRequest(method, path, data, success, error) {

    var content_type = "application/json; charset=utf-8";
    /*
    if ((method == 'POST') && !data) {
      data = encodeParams(params);
      content_type = "application/x-www-form-urlencoded";
    } else {
      url += "?" + encodeParams(params);
    }*/
    

    var ajaxOptions = {
      type: method.toUpperCase(),
      url: self.apiUrl + path,
      success: success,
      error: error,
      data: data || {},
      contentType: content_type,
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

function encodeParams(params) {
    tail = [];
    if (params instanceof Array) {
      for (i in params) {
        var item = params[i];
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
              var item = value[i];
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
   ******************************************************************/
  
  /*
   *  @class queryObj
   *  @purpose a class for holding all query information and paging state
   *  @purpose a query object that will contain all relevant info for API call
   *  @param method REQUIRED - GET, POST, PUT, DELETE
   *  @param path REQUIRED - API resource (e.g. "users" or "users/rod", should not include http URL or org_name/app_name)
   *  @param jsonObj NULLABLE
   *
   */
  queryObj = (function(method, path, jsonObj, params, successCallback, failureCallback) {
  //function queryObj(method, path, jsonObj, params, successCallback, failureCallback) {
    //query vars
    this.method = method;
    this.path = path;
    this.jsonObj = jsonObj;
    this.params = params;

    //callbacks
    this.successCallback = successCallback;
    this.failureCallback = failureCallback;
    
    //paging vars
    this.cursor = null;
    this.next = null
    this.previous = [];

    //paging functions
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
      var method = _queryObj.method.toUpperCase();
      var path = _queryObj.path;
      var jsonObj = _queryObj.jsonObj || {};
      var params = _queryObj.params || {};

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
      if (_queryObj.cursor) {
        params.cursor = _queryObj.cursor;
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
        _queryObj.successCallback(response);
      },
      function(response) {
        console.log('API call failed - ' + response.responseText);
        _queryObj.failureCallback(response)
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
    appPath:appPath,
    runAppQuery:runAppQuery,
    runManagementQuery:runManagementQuery,
    queryObj:queryObj
  }

  return self
})();
