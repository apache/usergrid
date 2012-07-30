/**
 *  App SDK is a collection of classes designed to make working with
 *  the Appigee App Services API as easy as possible.
 *  Learn more at http://apigee.com/docs
 */
//define the console.log for IE
window.console = window.console || {};
window.console.log = window.console.log || function() {};

window.apigee = window.apigee || {};
apigee = apigee || {};

var APIClient = (function () {
  //API endpoint
  var _apiUrl = "http://api.usergrid.com";
  var _orgName = null;
  var _orgUUID = null;
  var _appName = null;
  var _token = null;
  var clientId = null; //to be implemented
  var clientSecret = null; //to be implemented

  function init(orgName, appName){
    _orgName = orgName;
    _appName = appName;
  }
  /*
    *  method to get the organization name to be used by the client
    *  @method getOrganizationName
    */
  function getOrganizationName() {
    return _orgName;
  }
  /*
    *  method to set the organization name to be used by the client
    *  @method getOrganizationName
    *  @param orgName - the organization name
    */
  function setOrganizationName(orgName) {
    _orgName = orgName;
  }
  /*
    *  method to get the organization uuid to be used by the client
    *  @method getOrganizationUUID
    */
  function getOrganizationUUID() {
    return _orgUUID;
  }
  /*
    *  method to set the organization uuid to be used by the client
    *  @method setOrganizationUUID
    *  @param orgUUID - the organization uuid
    */
  function setOrganizationUUID(orgUUID) {
    _orgUUID = orgUUID;
  }

  /*
  *  method to set the application name to be used by the client
  *  @method getApplicationName
  */
  function getApplicationName() {
    return _appName;
  }
  /*
  *  method to get the application name to be used by the client
  *  @method getApplicationName
  *  @param appName - the application name
  */
  function setApplicationName(appName) {
    _appName = appName;
  }

  /*
  *  method to set the token to be used by the client
  *  @method getToken
  */
  function getToken() {
    return _token;
  }
  /*
  *  method to get the token to be used by the client
  *  @method getToken
  *  @param token - the bearer token
  */
  function setToken(token) {
    _token = token;
  }

  /*
  *  returns API URL
  *  @method getApiUrl
  */
  function getApiUrl() {
    return _apiUrl
  }
  /*
  *  allows API URL to be overridden
  *  @method setApiUrl
  */
  function setApiUrl(apiUrl) {
    _apiUrl = apiUrl;
  }

  /*
  *  returns the api url of the reset pasword endpoint
  *  @method getResetPasswordUrl
  */
  function getResetPasswordUrl() {
    this.getApiUrl() + "/management/users/resetpw"
  }

  /*
  *  public function to run calls against the app endpoint
  *  @method runAppQuery
  *  @params {object} apigee.QueryObj - {method, path, jsonObj, params, successCallback, failureCallback}
  *
  */
  function runAppQuery (QueryObj) {
    var endpoint = "/" + this.getOrganizationName() + "/" + this.getApplicationName() + "/";
    this.processQuery(QueryObj, endpoint);
  }

  /*
  *  public function to run calls against the management endpoint
  *  @method runManagementQuery
  *  @params {object} apigee.QueryObj - {method, path, jsonObj, params, successCallback, failureCallback}
  *
  */
  function runManagementQuery (QueryObj) {
    var endpoint = "/management/";
    this.processQuery(QueryObj, endpoint)
  }

  /*
  *  @method processQuery
  *  @purpose to validate and prepare a call to the API
  *  @params {object} apigee.QueryObj - {method, path, jsonObj, params, successCallback, failureCallback}
  *
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
      var params = QueryObj.getParams() || {};

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
		//TODO: double use of encodeParams ( is it necesary?)
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
      xhr.open(method, this.getApiUrl() + path, true);
    }
    else if (xM)
    {
      xhr = new XMLHttpRequest();
      xhr.open(method, this.getApiUrl() + path, true);
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
      xhr.open(method, this.getApiUrl() + path, true);
    }


    xhr.open(method, this.getApiUrl() + path, true);
    if (application_name != 'SANDBOX' && this.getToken()) {
      xhr.setRequestHeader("Authorization", "Bearer " + this.getToken());
      xhr.withCredentials = true;
    }
    // Handle response.
    xhr.onerror = function() {
      //network error
      clearTimeout(timeout);
      console.log('API call failed at the network level.');
      if (QueryObj.callFailureCallback) {
        QueryObj.callFailureCallback({'error':'error'});
      }
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
        if (QueryObj.callFailureCallback) {
          QueryObj.callFailureCallback(response);
        }

        return;
      } else {
        //success

        //query completed succesfully, so store cursor
        var cursor = response.cursor || null;
        QueryObj.saveCursor(cursor);
        //then call the original callback
        if (QueryObj.callSuccessCallback) {
          QueryObj.callSuccessCallback(response);
        }
      }
    };
    var timeout = setTimeout(function() { xhr.abort(); }, 10000);

    xhr.send(jsonObj);
  }


  /**
  *  @method encodeParams
  *  @purpose - to encode the query string parameters
  *  @params {object} params - an object of name value pairs that will be urlencoded
  *
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
  /**
    * Tests if the string is a uuid
    * @public
    * @function
    * @param {string} uuid The string to test
    * @returns {Boolean} true if string is uuid
    */
  function isUUID (uuid) {
    var uuidValueRegex = /\"([\w]{8}-[\w]{4}-[\w]{4}-[\w]{4}-[\w]{12})\"/gm;
    if (!uuid) return false;
    return uuidValueRegex.test(uuid);
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
    getApiUrl:getApiUrl,
    setApiUrl:setApiUrl,
    getResetPasswordUrl:getResetPasswordUrl,
    runAppQuery:runAppQuery,
    runManagementQuery:runManagementQuery,
    processQuery:processQuery,
    encodeParams:encodeParams,
    isUUID:isUUID
  }
})();



/**
 *  apigee.services.QueryObj is a class for holding all query information and paging state
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
 * @param {Function} successCallback function called with response: <pre>
 * {
 *   data : {
 *     "..." : "...",
 *   }
 * }
 * @param {Function} failureCallback function called with response if available: <pre>
 * {
 *    alert('an error occured');
 * }
 * </pre>
 */

(function () {

  apigee.QueryObj = function(method, path, jsonObj, params, successCallback, failureCallback) {
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
  };

  apigee.QueryObj.prototype = {
    setAll: function(method, path, jsonObj, params, successCallback, failureCallback) {
      this._method = method;
      this._path = path;
      this._jsonObj = jsonObj;
      this._params = params;
      this._successCallback = successCallback;
      this._failureCallback = failureCallback;
    },
    getMethod: function() {
      return this._method;
    },
    setMethod: function(method) {
      this._method = method;
    },
    getPath: function() {
      return this._path;
    },
    setPath: function(path) {
      this._path = path;
    },
    getJsonObj: function() {
      return this._jsonObj;
    },
    setJsonObj: function(jsonObj) {
      this._jsonObj = jsonObj;
    },
    getParams: function() {
      return this._params;
    },
    setParams: function(params) {
      this._params = params;
    },
    getSuccessCallback: function() {
      return this._successCallback;
    },
    setSuccessCallback: function(successCallback) {
      this._successCallback = successCallback;
    },
    callSuccessCallback: function(response) {
      if (this._successCallback) {
        this._successCallback(response);
      }
    },
    getFailureCallback: function() {
      return this._failureCallback;
    },
    setFailureCallback: function(failureCallback) {
      this._failureCallback = failureCallback;
    },
    callFailureCallback: function(response) {
      if (this._failureCallback) {
        this._failureCallback(response);
      }
    },

    getCurl: function() {
      return this._curl;
    },
    setCurl: function(curl) {
      this._curl = curl;
    },

    getToken: function() {
      return this._token;
    },
    setToken: function(token) {
      this._token = token;
    },

    //methods for accessing paging functions
    resetPaging: function() {
      this._previous = [];
      this._next = null;
      this._cursor = null;
    },

    hasPrevious: function() {
      return (this._previous.length > 0);
    },

    getPrevious: function() {
      this._next=null; //clear out next so the comparison will find the next item
      this._cursor = this._previous.pop();
    },

    hasNext: function(){
      return (this._next);
    },

    getNext: function() {
      this._previous.push(this._cursor);
      this._cursor = this._next;
    },

    saveCursor: function(cursor) {
      this._cursor = this._next; //what was new is old again
      //if current cursor is different, grab it for next cursor
      if (this._next != cursor) {
        this._next = cursor;
      } else {
        this._next = null;
      }
    },

    getCursor: function() {
      return this._cursor;
    }
  };
})(apigee);



(function () {
  /**
   *  Entity is a base class for entities
   *
   *  @class Entity
   */
  apigee.Entity = function(collectionType, uuid) {
    this._collectionType = collectionType;
    this._data = {};
    this._uuid = uuid;
  };
  apigee.Entity.prototype = {
    getCollectionType: function() {
      return this._collectionType;
    },
    setCollectionType: function(collectionType) {
      this._collectionType = collectionType;
    },
    getUUID: function() {
      return this._uuid;
    },
    setUUID: function(uuid) {
      this._uuid = uuid;
    },
    getData: function() {
      return this._data;
    },
    setData: function(data) {
      for(item in data) {
        this._data[item] = data[item];
      }
    },
    clearData: function () {
      this._data = null;
    },
    getDataField: function(field) {
      return this._data[field];
    },
    setDataField: function(field, value) {
      this._data[field] = value;
    },
    deleteField: function(field) {
      delete this._data[field];
    },
    processResponse: function(self, response){
      if (response.entities[0]) { // && APIClient.isUUID(response.entities[0].uuid )){
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
    saveEntity: function(successCallback, errorCallback) {
      var path = this.getCollectionType();
      //do a post if there is no uuid
      var method = 'POST';
      if (this.getUUID()) {
        method = 'PUT';
        path += "/" + this.getUUID();
      }
      var self = this;
      APIClient.runAppQuery(new apigee.QueryObj(method, path, this.getData(), null,
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
    refreshEntity: function(successCallback, errorCallback) {
      var path = this.getCollectionType();
      //do a post if there is no uuid
      if (this.getUUID()) {
        path += "/" + this.getUUID();
      }
      var self = this;
      APIClient.runAppQuery(new apigee.QueryObj('GET', path, null, null,
        function(response) {
          if (processResponse(self, response)){
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
    deleteEntity: function(successCallback, errorCallback) {
      var path = this.getCollectionType();
      //do a post if there is no uuid
      if (this.getUUID()) {
        path += "/" + this.getUUID();
      }
      var self = this;
      APIClient.runAppQuery(new apigee.QueryObj('DELETE', path, null, null,
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


  /**
   *  Collection is a container class for holding entities
   *
   *  @class Collection
   */
  apigee.Collection = function(path) {
    this._path = path;
    this._uuid = null;
    this._list = [];
    this._queryObj = new apigee.QueryObj();
  };

  apigee.Collection.prototype = {
    getPath: function() {
      return this._path;
    },
    setPath: function(path) {
      this._path = path;
    },
    getUUID: function() {
      return this._uuid;
    },
    setUUID: function(uuid) {
      this._uuid = uuid;
    },
    addEntity: function(item) {
      var count = this._list.length;
      this._list[count] = item;
    },
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
    getFirstEntity: function() {
      var count = this._list.length;
      if (count > 0) {
        return this._list[0];
      }
      return null;
    },
    getEntityList: function() {
      return this._list;
    },
    setEntityList: function(list) {
      this._list = list;
    },
    hasNext:  function() {
      return this._queryObj.hasNext();
    },
    getNext: function() {
      if (this._queryObj.hasNext()) {
        this._queryObj.getNext();
        //empty the list
        this.setEntityList([]);
        APIClient.runAppQuery(this._queryObj);
      }
    },
    hasPrevious:  function() {
      return this._queryObj.hasPrevious();
    },
    getPrevious: function() {
      if (this._queryObj.hasPrevious()) {
        this._queryObj.getPrevious();
        //empty the list
        this.setEntityList([]);
        APIClient.runAppQuery(this._queryObj);
      }
    },
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
      APIClient.runAppQuery(this._queryObj);
    },
    /*
     * problems that still need to be solved with this method:
     * we only have the current cursor in the object. So
     * we can't update anything except the current items in the
     * collection
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