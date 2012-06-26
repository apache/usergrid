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

  /* flag to overide use SSO if needed set to ?use_sso=no */
  var USE_SSO = 'no';

  var APIGEE_SSO_URL = "https://accounts.apigee.com/accounts/sign_in";

  var APIGEE_SSO_PROFILE_URL = "https://accounts.apigee.com/accounts/my_account";

  var SSO_LOGOUT_PAGE = 'https://accounts.apigee.com/accounts/sign_out';

  var LOCAL_STANDALONE_API_URL = "http://localhost:8080";

  var LOCAL_TOMCAT_API_URL = "http://localhost:8080/ROOT";

  var LOCAL_API_URL = LOCAL_STANDALONE_API_URL;

  var response = {};

  var onIE = navigator.userAgent.indexOf("MSIE") >= 0;

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

  function indexOfFirstType(type, args) {
    for (var i = 0; i < args.length; i++) {
      if (!args[i]) return - 1;
      if (typeof args[i] == type) return i;
    }
    return - 1;
  }

  function getByType(type, i, args) {
    var j = indexOfFirstType(type, args);
    if (j < 0) return null;
    var k = 0;
    while ((j < args.length) && (k <= i)) {
      if (type == "object") {
        if (args[j].constructor != Object) return null;
      } else if (typeof args[j] != type) return null;
      if (k == i) return args[j];
      j++;
      k++;
    }
    return null;
  }

  function countByType(type, args) {
    var c = 0;
    var j = indexOfFirstType(type, args);
    if (j < 0) return c;
    while (j < args.length) {
      if (type == "object") {
        if (args[j].constructor != Object) return c;
      } else if (typeof args[j] != type) return c;
      j++;
      c++;
    }
    return null;
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

  function encodePathString(path, returnParams) {

    var i = 0;
    var segments = new Array();
    var payload = null;
    while (i < path.length) {
      var c = path.charAt(i);
      if (c == '{') {
        var bracket_start = i;
        i++;
        var bracket_count = 1;
        while ((i < path.length) && (bracket_count > 0)) {
          c = path.charAt(i);
          if (c == '{') {
            bracket_count++;
          } else if (c == '}') {
            bracket_count--;
          }
          i++;
        }
        if (i > bracket_start) {
          var segment = path.substring(bracket_start, i);
          segments.push(JSON.parse(segment));
        }
        continue;
      } else if (c == '/') {
        i++;
        var segment_start = i;
        while (i < path.length) {
          c = path.charAt(i);
          if ((c == ' ') || (c == '/') || (c == '{')) {
            break;
          }
          i++;
        }
        if (i > segment_start) {
          var segment = path.substring(segment_start, i);
          segments.push(segment);
        }
        continue;
      } else if (c == ' ') {
        i++;
        var payload_start = i;
        while (i < path.length) {
          c = path.charAt(i);
          i++;
        }
        if (i > payload_start) {
          var json = path.substring(payload_start, i).trim();
          // console.log(json);
          payload = JSON.parse(json);
        }
        break;
      }
      i++;
    }

    var newPath = "";
    for (i = 0; i < segments.length; i++) {
      var segment = segments[i];
      if (typeof segment === "string") {
        newPath += "/" + segment;
      } else {
        if (i == (segments.length - 1)) {
          if (returnParams) {
            return {path : newPath, params: segment, payload: payload};
          }
          newPath += "?";
        } else {
          newPath += ";";
        }
        newPath += encodeParams(segment);
      }
    }
    if (returnParams) {
      return {path : newPath, params: null, payload: payload};
    }
    return newPath;
  }

  /* TODO: These next two functions "*LastError*" MUST be deprecated eventually */

  function setLastError(error) {
    if (error) {
      self.error = error;
      if (error.error) {
        console.log(error.error);
      }
      if (error.error_description) {
        console.log(error.error_description);
      }
      if (error.exception) {
        console.log(error.exception);
      }
    }
  }

  function getLastErrorMessage(defaultMsg) {
    if (self.error && self.error.error_description) {
      return self.error.error_description;
    }
    return defaultMsg;
  }

  /* */

  /* The base for all API calls. HANDLE WITH CAUTION! */
  function apiRequest2(method, path, data, success, error) {

    var ajaxOptions = {
      type: method.toUpperCase(),
      url: self.apiUrl + path,
      success: success,
      error: error,
      data: data || {},
      contentType: "application/json; charset=utf-8",
      dataType: "json"
    }

    if (method.toUpperCase() == "POST") {
      ajaxOptions.contentType = "application/x-www-form-urlencoded";
    }

    /* This hack is necesary for IE9. IE is too strict when it comes to cross-domain. */
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

  /* OLD VERSION of "the base for all API calls" HANDLE WITH CAUTION! to be deprecated. */
  function apiRequest(method, path, params, data, success, failure) {
    method = method.toUpperCase();

    var encodedPath = encodePathString(path, true);
    if (encodedPath) {
      path = encodedPath.path;
      if (encodedPath.params) {
        params = encodedPath.params;
      }
      if (encodedPath.payload) {
        data = encodedPath.payload;
      }
    }

    var xhr = new XMLHttpRequest();
    var url = self.apiUrl + path;

    params = params || {};

    if ((method == "GET") || (method == "DELETE")) {
      params['_'] = new Date().getTime();
    }

    var authorizationHeader = null;
    if (session.accessToken) {
      authorizationHeader = "Bearer " + session.accessToken;
    }

    var content_type = "application/json; charset=utf-8";
    if ((method == 'POST') && !data) {
      data = encodeParams(params);
      content_type = "application/x-www-form-urlencoded";
    } else {
      url += "?" + encodeParams(params);
    }

    // console.log(url);
    xhr.open(method, url, true);
    xhr.setRequestHeader("Content-Type", content_type);
    if (authorizationHeader) {
      xhr.setRequestHeader("Authorization", authorizationHeader);
      xhr.withCredentials = true;
    } else if (self.clientId && self.clientSecret) {
      params["client_id"] = self.clientId;
      params["client_secret"] = self.clientSecret;
    }

    xhr.onreadystatechange = function() {
      if (xhr.readyState == 2) {
        self.activeRequests++;
        if (self.onActiveRequest) {
          try {
            self.onActiveRequest(self.activeRequests);
          } catch(err) {
          }
        }
      } else if (xhr.readyState == 4) {
        self.activeRequests--;
        if (self.onActiveRequest) {
          try {
            self.onActiveRequest(self.activeRequests);
          } catch(err) {
          }
        }
        clearTimeout(timeout);
        response = null;
        try {
          response = JSON.parse(xhr.responseText);
        } catch(err) {
        }

        if (!response || (xhr.status != 200)) {
          var force_logout = false;
          try {
            if (response && response.error) {
              var error = response.error;
              //var error = response.error_description;
              setLastError(response);
              if (error == "auth_expired_session_token") {
                force_logout = true;
              } else if (error == "auth_missing_credentials") {
                force_logout = true;
              } else if (error == "auth_invalid") {
                force_logout = true;
              } else if (error == "unauthorized") {
                force_logout = true;
              } else if (error == "expired_token") {
                force_logout = true;
              } else if (error == "auth_bad_access_token") {
                force_logout = true;
              } else if (error == "web_application") {
                //TBD::should we do something here?
              }
            }
            response = response || {};
            response.responseText = xhr.responseText;
            response.statusText = xhr.statusText;
          } catch(err) {
          }

          if (force_logout) {
            logout();
            return;
          }

          if (failure) {
            failure(response);
          }
          return;
        }
        try {
          success(response);
        } catch (e) {
          alert('There was an error:' + e);
        }
      }
    };

    var timeout = setTimeout(function() {
      xhr.abort();
    },
    10000);

    xhr.send(data);
  }

  function apiGetRequest(path, data, success, failure) {
    apiRequest2("GET", path, data, success, failure);
  }

  function requestApplications(success, failure) {
    if (!session.currentOrganization) {
      failure();
    }
    apiGetRequest("/management/organizations/" + session.currentOrganization.uuid + "/applications", null, success, failure);
  }

  function createApplication(data, success, failure) {
    if (!session.currentOrganization) {
      failure();
    }
    apiRequest("POST", "/management/organizations/" + session.currentOrganization.uuid + "/applications", null, JSON.stringify(data), success, failure);
  }

  function requestAdmins(success, failure) {
    if (!session.currentOrganization) {
      failure();
    }
    apiGetRequest("/management/organizations/" + session.currentOrganization.uuid + "/users", null, success, failure);
  }

  function createOrganization(data, success, failure) {
    if (!session.loggedInUser) {
      failure();
    }
    apiRequest("POST", "/management/users/" + session.loggedInUser.uuid + "/organizations", null, JSON.stringify(data), success, failure);
  }

  function leaveOrganization(organizationUUID, success, failure) {
    if (!session.loggedInUser) {
      failure();
    }
    apiRequest("DELETE", "/management/users/" + session.loggedInUser.uuid + "/organizations/" + organizationUUID, null, null, success, failure);
  }

  function requestOrganizations(success, failure) {
    apiGetRequest("/management/users/" + session.loggedInUser.uuid + "/organizations", null, success, failure);
  }

  function requestOrganizationCredentials(success, failure) {
    if (!session.currentOrganization) {
      failure();
    }
    apiGetRequest("/management/organizations/" + session.currentOrganization.uuid + "/credentials", null, success, failure);
  }

  function regenerateOrganizationCredentials(success, failure) {
    if (!session.currentOrganization) {
      failure();
    }
    apiRequest("POST", "/management/organizations/" + session.currentOrganization.uuid + "/credentials", null, null, success, failure);
  }

  function createAdmin(data, success, failure) {
    if (!session.currentOrganization) {
      failure();
    }
    apiRequest("POST", "/management/organizations/" + session.currentOrganization.uuid + "/users", null, JSON.stringify(data), success, failure);
  }

  function requestCollections(applicationId, success, failure) {
    apiGetRequest("/" + session.currentOrganization.uuid + "/" + applicationId, null, success, failure);
  }

  function createCollection(applicationId, data, success, failure) {
    var collections = {};
    collections[data.name] = {};
    var metadata = {
      metadata: {
        collections: collections
      }
    };
    apiRequest("PUT", "/" + session.currentOrganization.uuid + "/" + applicationId, null, JSON.stringify(metadata), success, failure);
  }

  function requestApplicationCredentials(applicationId, success, failure) {
    apiGetRequest("/" + session.currentOrganization.uuid + "/" + applicationId + "/credentials", null, success, failure);
  }

  function regenerateApplicationCredentials(applicationId, success, failure) {
    apiRequest("POST", "/" + session.currentOrganization.uuid + "/" + applicationId + "/credentials", null, null, success, failure);
  }

  function requestApplicationRoles(applicationId, success, failure) {
    apiGetRequest("/" + session.currentOrganization.uuid + "/" + applicationId + "/rolenames", null, success, failure);
  }

  function requestApplicationRolePermissions(applicationId, roleName, success, failure) {
    apiGetRequest("/" + session.currentOrganization.uuid + "/" + applicationId + "/rolenames/" + roleName, null, success, failure);
  }

  function requestApplicationRoleUsers(applicationId, roleId, success, failure) {
    apiGetRequest("/" + session.currentOrganization.uuid + "/" + applicationId + "/roles/" + roleId + "/users/", null, success, failure);
  }

  function addApplicationRolePermission(applicationId, roleName, permission, success, failure) {
    apiRequest("POST", "/" + session.currentOrganization.uuid + "/" + applicationId + "/rolenames/" + roleName, null, JSON.stringify({
      permission : permission
    }), success, failure);
  }

  function deleteApplicationRolePermission(applicationId, roleName, permission, success, failure) {
    apiRequest("DELETE", "/" + session.currentOrganization.uuid + "/" + applicationId + "/rolenames/" + roleName, {
      permission : permission
    }, null, success, failure);
  }

  function addApplicationUserPermission(applicationId, userName, permission, success, failure) {
    apiRequest("POST", "/" + session.currentOrganization.uuid + "/" + applicationId + "/users/" + userName + "/permissions", null, JSON.stringify({
      permission : permission
    }), success, failure);
  }

  function deleteApplicationUserPermission(applicationId, userName, permission, success, failure) {
    apiRequest("DELETE", "/" + session.currentOrganization.uuid + "/" + applicationId + "/users/" + userName + "/permissions", {
      permission : permission
    }, null, success, failure);
  }

  function requestApplicationCounterNames(applicationId, success, failure) {
    apiGetRequest("/" + session.currentOrganization.uuid + "/" + applicationId + "/counters", null, success, failure);
  }

  function requestApplicationCounters(applicationId, start_time, end_time, resolution, counter, success, failure) {
    var params = {};
    if (start_time) params.start_time = start_time;
    if (end_time) params.end_time = end_time;
    if (resolution) params.resolution = resolution;
    if (counter) params.counter = counter;
    params.pad = true;
    apiGetRequest("/" + session.currentOrganization.uuid + "/" + applicationId + "/counters", params, success, failure);
  }

  function requestAdminUser(success, failure) {
    if (!session.loggedInUser) {
      failure();
    }
    apiGetRequest("/management/users/" + session.loggedInUser.uuid, null, success, failure);
  }

  function updateAdminUser(properties, success, failure) {
    if (!session.loggedInUser) {
      failure();
    }
    apiRequest("PUT", "/management/users/" + session.loggedInUser.uuid, null, JSON.stringify(properties), success, failure);
  }

  function requestAdminFeed(success, failure) {
    if (!session.loggedInUser) {
      failure();
    }
    apiGetRequest("/management/users/" + session.loggedInUser.uuid + "/feed", null, success, failure);
  }

  function loginAdmin(email, password, successCallback, errorCallback) {
    session.clearIt();
    var formdata = {
      grant_type: "password",
      username: email,
      password: password
    };
    apiRequest2("POST", "/management/token", formdata,
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
    apiRequest("POST", "/" + session.currentOrganization.uuid + "/" + applicationId + "/token", formdata, null,
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
    apiRequest2("GET", "/management/users/" + session.loggedInUser.email, null,
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

  function useSSO(){
    return apigeeUser() || self.use_sso=='true' || self.use_sso=='yes'
  }

  function apigeeUser(){
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

  function signup(organization, username, name, email, password, success, failure) {
    var formdata = {
      organization: organization,
      username: username,
      name: name,
      email: email,
      password: password
    };
    apiRequest("POST", "/management/organizations", formdata, null,
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

  function getEntity(collection, a) {
    var ns = session.currentOrganization.uuid + "/" + session.currentApplicationId;
    var id = a[0];
    if (countByType("string", a) >= 2) {
      ns = session.currentOrganization.uuid + "/" + getByType("string", 0, a);
      id = getByType("string", 1, a);
    }
    var success = getByType("function", 0, a);
    var failure = getByType("function", 1, a);
    if (!ns) {
      return;
    }
    var params = getByType("object", 0, a);

    var path = "/" + ns + "/" + collection + "/" + id;
    apiGetRequest(path, params, success, failure);
  }

  function getUser(a) {
    return getEntity("users", arguments);
  }

  function getGroup(a) {
    return getEntity("groups", arguments);
  }

  function queryEntities(root_collection, a) {
    var ns = session.currentOrganization.uuid + "/" + session.currentApplicationId;
    if (countByType("string", a) > 0) {
      ns = session.currentOrganization.uuid + getByType("string", 0, a);
      if (!ns) ns = session.currentOrganization.uuid + "/" + session.currentApplicationId;
    }
    var success = getByType("function", 0, a);
    var failure = getByType("function", 1, a);
    if (!ns) {
      return;
    }
    var options = getByType("object", 0, a) || {};

    var q = new Query(ns, "/" + root_collection, null, options, success, failure);
    q.send("GET", null);
    return q;
  }

  function queryUsers(a) {
    return queryEntities("users", arguments);
  }

  function queryEntityCollection(root_collection, entity_collection, a) {
    var ns = session.currentOrganization.uuid + "/" + session.currentApplicationId;
    var id = a[0];
    if (countByType("string", a) >= 2) {
      ns = session.currentOrganization.uuid + "/" + getByType("string", 0, a);
      id = getByType("string", 1, a);
    }
    var success = getByType("function", 0, a);
    var failure = getByType("function", 1, a);
    if (!ns) {
      return;
    }
    var options = getByType("object", 0, a) || {};

    var path = "/" + root_collection + "/" + id + "/" + entity_collection;
    var q = new Query(ns, path, null, options, success, failure);
    q.send("GET", null);
    return q;
  }

  function deleteEntity(applicationId, entityId, path, success, failure) {
    apiRequest("DELETE", "/" + session.currentOrganization.uuid + "/" + applicationId + "/" + path + "/" + entityId, null, null, success, failure);
  }

  function queryUserMemberships(a) {
    return queryEntityCollection("users", "groups", arguments);
  }

  function queryUserActivities(a) {
    return queryEntityCollection("users", "activities", arguments);
  }

  function queryUserRoles(applicationId, entityId, success, failure) {
    apiGetRequest("/" + session.currentOrganization.uuid + "/" + applicationId + "/users/" + entityId + "/roles", null, success, failure);
  }

  function queryUserPermissions(a) {
    return queryEntityCollection("users", "permissions", arguments);
  }

  function queryUserFollowing(a) {
    return queryEntityCollection("users", "following", arguments);
  }

  function queryUserFollowers(a) {
    return queryEntityCollection("users", "followers", arguments);
  }

  function requestUserList(applicationId, searchString, success, failure) {
    if (searchString != "*") searchString = searchString + '*';
    apiRequest("GET", "/" + session.currentOrganization.uuid + "/" + applicationId + "/users", null, JSON.stringify({
      username: searchString
    }), success, failure);
  }

  function requestUsers(applicationId, success, failure) {
    apiGetRequest("/" + session.currentOrganization.uuid + "/" + applicationId + "/users", null, success, failure);
  }

  function createUser(applicationId, data, success, failure) {
    apiRequest("POST", "/" + session.currentOrganization.uuid + "/" + applicationId + "/users", null, JSON.stringify(data), success, failure);
  }

  function deleteUser(applicationId, userId, success, failure) {
    apiRequest("DELETE", "/" + session.currentOrganization.uuid + "/" + applicationId + "/users/" + userId, null, null, success, failure);
  }

  function requestCollectionIndexes(applicationId, path, success, failure) {
    if (path.lastIndexOf("/", 0) !== 0) {
      path = "/" + path;
    }
    path = "/" + session.currentOrganization.uuid + "/" + applicationId + path + "/indexes";
    apiGetRequest(path, null, success, failure);
  }

  function queryGroups(a) {
    return queryEntities("groups", arguments);
  }

  function queryRoles(a) {
    return queryEntities("roles", arguments);
  }

  function queryActivities(a) {
    return queryEntities("activities", arguments);
  }

  function queryCollections(a) {
    return queryEntities("/", arguments);
  }

  function queryGroupMemberships(a) {
    return queryEntityCollection("groups", "users", arguments);
  }

  function queryGroupActivities(a) {
    return queryEntityCollection("groups", "activities", arguments);
  }

  function requestGroups(applicationId, success, failure) {
    apiGetRequest("/" + session.currentOrganization.uuid + "/" + applicationId + "/groups", null, success, failure);
  }

  function requestGroupRoles(applicationId, entityId, success, failure) {
    apiGetRequest("/" + session.currentOrganization.uuid + "/" + applicationId + "/groups/" + entityId + "/rolenames", null, success, failure);
  }

  function saveUserProfile(applicationId, userid, payload, success,failure){
    apiRequest("PUT", "/" + session.currentOrganization.uuid + "/" + applicationId + "/users/" + userid, null, JSON.stringify(payload) , success, failure);
  }

  function saveGroupProfile(applicationId, groupid, payload, success,failure){
    apiRequest("PUT", "/" + session.currentOrganization.uuid + "/" + applicationId + "/groups/" + groupid, null, JSON.stringify(payload) , success, failure);
  }

  function createGroup(applicationId, data, success, failure) {
    apiRequest("POST", "/" + session.currentOrganization.uuid + "/" + applicationId + "/groups", null, JSON.stringify(data), success, failure);
  }

  function deleteGroup(applicationId, groupId, success, failure) {
    apiRequest("DELETE", "/" + session.currentOrganization.uuid + "/" + applicationId + "/groups/" + groupId, null, null, success, failure);
  }

  function addUserToGroup(applicationId, groupId, username, success, failure) {
    apiRequest("POST", "/" + session.currentOrganization.uuid + "/" + applicationId + "/groups/" + groupId + "/users/" + username, null, "{ }", success, failure);
  }

  function removeUserFromGroup(applicationId, groupId, username, success, failure) {
    if (!session.loggedInUser) {
      failure();
    }
    apiRequest("DELETE",  "/" + session.currentOrganization.uuid + "/" + applicationId + "/groups/" + groupId + "/users/" + username, null, null, success, failure);
  }

  function entitySearch(applicationId, searchType, searchString, success, failure) {
                return queryEntities(searchType, arguments);
  }

  function createRole(applicationId, data, success, failure) {
    apiRequest("POST", "/" + session.currentOrganization.uuid + "/" + applicationId + "/rolenames", null, JSON.stringify(data), success, failure);
  }

  function addUserToRole(applicationId, roleId, username, success, failure) {
    apiRequest("POST", "/" + session.currentOrganization.uuid + "/" + applicationId + "/roles/" + roleId + "/users/" + username, null, "{ }", success, failure);
  }

  function removeUserFromRole(applicationId, username, roleId, success, failure) {
    if (!session.loggedInUser) {
      failure();
    }
    apiRequest("DELETE",  "/" + session.currentOrganization.uuid + "/" + applicationId + "/users/" + username + "/roles/" + roleId, null, null, success, failure);
  }

  function requestRoles(applicationId, success, failure) {
    apiGetRequest("/" + session.currentOrganization.uuid + "/" + applicationId + "/rolenames", null, success, failure);
  }

  function Query(applicationId, path, ql, options, success, failure) {

    if (path.lastIndexOf("/", 0) !== 0) {
      path = "/" + path;
    }
    path = "/" + applicationId + path;

    var queryClient = this;
    var query = {};
    var start_cursor = null;
    var next_cursor = null;
    var prev_cursor = null;

    function getServiceParams() {
      var params = {};
      if (ql) {
        params['ql'] = ql;
      }
      if (start_cursor) {
        params['cursor'] = start_cursor;
      }
      if (prev_cursor) {
        params['prev'] = prev_cursor;
      }
      if (options) {
        for (var name in options) {
          params[name] = options[name];
        }
      }
      return params;
    }
    this.getServiceParams = getServiceParams;

    function hasPrevious() {
      return prev_cursor != null;
    }
    this.hasPrevious = hasPrevious;

    function getPrevious() {
      start_cursor = null;
      next_cursor = null;
      if (prev_cursor) {
        start_cursor = prev_cursor.pop();
        send("GET", null);
      }
    }
    this.getPrevious = getPrevious;

    function hasNext() {
      return next_cursor && start_cursor;
    }
    this.hasNext = hasNext;

    function getNext() {
      if (next_cursor && start_cursor) {
        prev_cursor = prev_cursor || [];
        prev_cursor.push(start_cursor);
        start_cursor = next_cursor;
        next_cursor = null;
        send("GET", null);
      }
    }
    this.getNext = getNext;

    function send(method, data) {
      var params = getServiceParams();
      prev_cursor = null;
      next_cursor = null;
      start_cursor = null;
      apiRequest2(method, path, data,
                 function(data) {
                   if (data.entities && data.entities.length > 0) {
                     start_cursor = data.entities[0].uuid;
                     if (data.params) {
                       if (data.params.prev) {
                         prev_cursor = data.params.prev;
                       }
                       if (data.params.cursor) {
                         start_cursor = data.params.cursor[0];
                       }
                     }
                     next_cursor = data.cursor;
                   }
                   if (success) {
                     success(data, queryClient);
                   } else if (data.error) {
		     failure(data, queryClient);
		   }
                 },
                 function(data) {
                   if (failure) {
                     failure(data, queryClient);
                   }
                 }
                );
    }
    this.send = send;

    function post(obj) {
      if (obj) {
        send("POST", JSON.stringify(obj));
      }
    }
    this.post = post;

    function put(obj) {
      if (obj) {
        send("PUT", JSON.stringify(obj));
      }
    }
    this.put = put;

    function delete_() {
      send("DELETE", null);
    }
    this.delete_ = delete_;
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

  /* These are the functions we want to be public. Almost all, really. */

  var self = {
    Init: Init,
    apiUrl: PUBLIC_API_URL,
    sso_logout_page: SSO_LOGOUT_PAGE,
    error: null,
    activeRequests: 0,
    onActiveRequest: null,
    encodePathString: encodePathString,
    getLastErrorMessage: getLastErrorMessage,
    apiRequest2: apiRequest2,
    apiRequest: apiRequest,
    apiGetRequest: apiGetRequest,
    requestApplications: requestApplications,
    createApplication: createApplication,
    requestAdmins: requestAdmins,
    createOrganization: createOrganization,
    leaveOrganization: leaveOrganization,
    requestOrganizations: requestOrganizations,
    requestOrganizationCredentials: requestOrganizationCredentials,
    regenerateOrganizationCredentials: regenerateOrganizationCredentials,
    createAdmin: createAdmin,
    requestCollections: requestCollections,
    createCollection: createCollection,
    requestApplicationCredentials: requestApplicationCredentials,
    regenerateApplicationCredentials: regenerateApplicationCredentials,
    requestApplicationRoles: requestApplicationRoles,
    requestApplicationRolePermissions: requestApplicationRolePermissions,
    requestApplicationRoleUsers: requestApplicationRoleUsers,
    addApplicationRolePermission: addApplicationRolePermission,
    deleteApplicationRolePermission: deleteApplicationRolePermission,
    addApplicationUserPermission: addApplicationUserPermission,
    deleteApplicationUserPermission: deleteApplicationUserPermission,
    requestApplicationCounterNames: requestApplicationCounterNames,
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
    getEntity: getEntity,
    getUser: getUser,
    getGroup: getGroup,
    queryEntities: queryEntities,
    queryUsers: queryUsers,
    queryEntityCollection: queryEntityCollection,
    deleteEntity: deleteEntity,
    queryUserMemberships: queryUserMemberships,
    queryUserActivities: queryUserActivities,
    queryUserRoles: queryUserRoles,
    queryUserPermissions: queryUserPermissions,
    queryUserFollowing: queryUserFollowing,
    queryUserFollowers: queryUserFollowers,
    requestUserList: requestUserList,
    requestUsers: requestUsers,
    createUser: createUser,
    deleteUser: deleteUser,
    requestCollectionIndexes: requestCollectionIndexes,
    queryGroups: queryGroups,
    queryRoles: queryRoles,
    queryActivities: queryActivities,
    queryCollections: queryCollections,
    queryGroupMemberships: queryGroupMemberships,
    queryGroupActivities: queryGroupActivities,
    requestGroups: requestGroups,
    requestGroupRoles: requestGroupRoles,
    saveUserProfile: saveUserProfile,
    saveGroupProfile: saveGroupProfile,
    createGroup: createGroup,
    deleteGroup: deleteGroup,
    addUserToGroup: addUserToGroup,
    removeUserFromGroup: removeUserFromGroup,
    entitySearch: entitySearch,
    createRole: createRole,
    addUserToRole: addUserToRole,
    removeUserFromRole: removeUserFromRole,
    requestRoles: requestRoles,
    Query: Query,
    setCurrentOrganization: setCurrentOrganization,
    autoLogin: autoLogin
  }

  return self
})();
