/**
    @application Holds global Usergrid JS functionality.
*/
var usergrid = usergrid || {};

/**
 * Creates a new Usergrid Client.
 * @class Represents a Usergrid client. 
 * @param {string} applicationId The id of the application (optional)
 */
usergrid.Client = function(applicationId, clientId, clientSecret) {
    //This code block *WILL* load before the document is complete
    /** @property applicationId */
    this.applicationId = applicationId || null;
    this.clientId = clientId || null;
    this.clientSecret = clientSecret || null;

    var self = this;

    var query_params = {};
    (function () {
        var e,
            a = /\+/g,
            r = /([^&=]+)=?([^&]*)/g,
            d = function (s) { return decodeURIComponent(s.replace(a, " ")); },
            q = window.location.search.substring(1);

        while (e = r.exec(q))
           query_params[d(e[1])] = d(e[2]);
    })();

    // Always use public API
    var FORCE_PUBLIC_API = false;

    // Public API
    var PUBLIC_API_URL = "http://api.usergrid.com";

    // Local API of standalone server
    var LOCAL_STANDALONE_API_URL = "http://localhost:8080";

    // Local API of Tomcat server in Eclipse
    var LOCAL_TOMCAT_API_URL = "http://localhost:8080/ROOT";

    // Local API
    var LOCAL_API_URL = LOCAL_STANDALONE_API_URL;
    
    /** @property apiUrl */
    this.apiUrl = PUBLIC_API_URL;

    // If not forcing public API, then use the local API
    // if we've loaded from filesystem or local web server
    if (!FORCE_PUBLIC_API && ((document.domain || "localhost") == "localhost")) {
        this.apiUrl = LOCAL_API_URL;
    }

    if (query_params.api_url) {
        this.apiUrl = query_params.api_url;
    }

    /** @property resetPasswordUrl */
    this.resetPasswordUrl = this.apiUrl + "/management/users/resetpw";

    if (!Storage.prototype.setObject) {
        Storage.prototype.setObject = function(key, value) {
            this.setItem(key, JSON.stringify(value));
        };
    }

    if (!Storage.prototype.getObject) {
        Storage.prototype.getObject = function(key) {
            try {
                return this.getItem(key) && JSON.parse(this.getItem(key));
            } catch(err) {
                }
            return null;
        };
    }

    var uuidValueRegex = /\"([\w]{8}-[\w]{4}-[\w]{4}-[\w]{4}-[\w]{12})\"/gm;

    function isUUID(uuid) {
        if (!uuid) return false;
        return uuidValueRegex.test(uuid);
    }
    /**
     * Tests if the string is a uuid
     * @public
     * @function
     * @param {string} uuid The string to test
     * @returns {Boolean} true if string is uuid
     */
    this.isUUID = isUUID;

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
            }
            else if (typeof args[j] != type) return null;
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
            }
            else if (typeof args[j] != type) return c;
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
        }
        else {
            for (var key in params) {
                if (params.hasOwnProperty(key)) {
                    var value = params[key];
                    if (value instanceof Array) {
                        for (i in value) {
                            var item = value[i];
                            tail.push(key + "=" + encodeURIComponent(item));
                        }
                    }
                    else {
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
                    }
                    else if (c == '}') {
                         bracket_count--;
                    }
                    i++;
                }
                if (i > bracket_start) {
                    var segment = path.substring(bracket_start, i);
                    segments.push(JSON.parse(segment));
                } 
                continue;
            }
            else if (c == '/') {
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
            }
            else if (c == ' ') {
                i++;
                var payload_start = i;
                while (i < path.length) {
                    c = path.charAt(i);
                    i++;
                }
                if (i > payload_start) {
                    var json = path.substring(payload_start, i).trim();
                    console.log(json);
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
            }
            else {
                if (i == (segments.length - 1)) {
                    if (returnParams) {
                        return {path : newPath, params: segment, payload: payload};
                    }
                    newPath += "?";
                }
                else {
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
    this.encodePathString = encodePathString;

    /**
     * @property {Object} error The last client error
     * <pre>
     * {
     *   type : "an_error_type",
     *   message : "An error message",
     *   detail : "An detailed error description",
     *   exception : "A Java exception"
     * }
     * </pre>
     */
    this.error = null;

    function setLastError(error) {
        if (error) {
            self.error = error;
            if (error.type) {
                console.log(error.type);
            }
            if (error.message) {
                console.log(error.message);
            }
            if (error.detail) {
                console.log(error.detail);
            }
            if (error.exception) {
                console.log(error.exception);
            }
        }
    }

    function getLastErrorMessage(defaultMsg) {
        var errorMsg = defaultMsg;
        if (self.error) {
            if (self.error.message) {
                errorMsg = self.error.message;
                if (self.error.detail) {
                    errorMsg += " - " + self.error.detail;
                }
            }
        }
        return errorMsg;
    }
    /**
     * Gets the error message of the last client error
     * 
     * @public
     * @function
     * @param {String} defaultMsg the default error message
     * @returns {String} error message
     */
    this.getLastErrorMessage = getLastErrorMessage;

    /** @property loggedInUser */
    this.loggedInUser = null;
    this.accessToken = null;
    /** @property currentOrganization */
    this.currentOrganization = null;
    var response = {};

    this.activeRequests = 0;
    this.onActiveRequest = null;

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

        if (method == "GET") {
            params['_'] = new Date().getTime();
        }

        var authorizationHeader = null;
        if (self.accessToken) {
            authorizationHeader = "Bearer " + self.accessToken;
        }

        var content_type = "application/json; charset=utf-8";
        if ((method == 'POST') && !data) {
            data = encodeParams(params);
            content_type = "application/x-www-form-urlencoded";
        }
        else {
            url += "?" + encodeParams(params);
        }

        console.log(url);
        xhr.open(method, url, true);
        xhr.setRequestHeader("Content-Type", content_type);
        if (authorizationHeader) {
            xhr.setRequestHeader("Authorization", authorizationHeader);
            xhr.withCredentials = true;
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
            }
            else if (xhr.readyState == 4) {
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
                            setLastError(error);
                            if (error.type == "auth_expired_session_token") {
                                force_logout = true;
                            }
                            else if (error.type == "auth_missing_credentials") {
                                force_logout = true;
                            }
                            else if (error.type == "auth_invalid") {
                                force_logout = true;
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

                success(response);
            }
        };

        var timeout = setTimeout(function() {
            xhr.abort();
        },
        10000);

        xhr.send(data);
    }
    /**
     * <p>API Request using Cross Origin Resource Sharing</p>
     * <p>Using hand-rolled XHR makes it easier to debug peculiarities of browser CORS implementations
     * Tested on FF, Chrome, Safari, Webkit, Mobile Webkit</p>
     * @see <a href="http://www.w3.org/TR/cors/">W3 CORS</a>
     * 
     * @public
     * @function
     * @param {String} method
     * @param {String} path
     * @param {Object} params
     * @param {String} data
     * @param {Function} success function called with response: <pre>
     * {
     *   data : {
     *     "..." : "...",
     *   }
     * }
     * @param {Function} failure function called with response: <pre>
     * {
     *   error: {
     *     type : "an_error_type",
     *     message : "An error message",
     *     detail : "An detailed error description",
     *     exception : "A Java exception"
     *   }
     * }
     * </pre>
     */
    this.apiRequest = apiRequest;

    function apiGetRequest(path, params, success, failure) {
        apiRequest("GET", path, params, null, success, failure);
    }
    /**
     * API Get request
     * 
     * @public
     * @function
     * @param {String} path
     * @param {Object} params
     * @param {Function} success function called with response: <pre>
     * {
     *   data : {
     *     "..." : "...",
     *   }
     * }
     * @param {Function} failure function called with response: <pre>
     * {
     *   error: {
     *     type : "an_error_type",
     *     message : "An error message",
     *     detail : "An detailed error description",
     *     exception : "A Java exception"
     *   }
     * }
     * </pre>
     */
    this.apiGetRequest = apiGetRequest;

    //
    // Get applications for organization
    //
    // GET: /management/organizations/<organization-name>/applications
    //
    function requestApplications(success, failure) {
        if (!self.currentOrganization) {
            failure();
        }
        apiGetRequest("/management/organizations/" + self.currentOrganization + "/applications", null, success, failure);
    }
    /**
     * <p>Get applications for organization</p>
     * 
     * @public
     * @function
     * @param {Function} success function called with response: <pre>
     * {
     *   data : {
     *     "application-1" : "00000000-0000-0000-0000-000000000001",
     *     "application-2" : "00000000-0000-0000-0000-000000000002",
     *   }
     * }
     * @param {Function} failure function called with response: <pre>
     * {
     *   error: {
     *     type : "an_error_type",
     *     message : "An error message",
     *     detail : "An detailed error description",
     *     exception : "A Java exception"
     *   }
     * }
     * </pre>
     */
    this.requestApplications = requestApplications;

    //
    // Add application to organization
    //
    // POST: /management/organizations/<organization-name>/applications
    //
    function createApplication(name, success, failure) {
        if (!self.currentOrganization) {
            failure();
        }
        apiRequest("POST", "/management/organizations/" + self.currentOrganization + "/applications", null, JSON.stringify({
            name: name
        }), success, failure);
    }
    this.createApplication = createApplication;

    //
    // Get admin users for organization
    //
    // GET: /management/organizations/<organization-name>/users
    //
    function requestAdmins(success, failure) {
        if (!self.currentOrganization) {
            failure();
        }
        apiGetRequest("/management/organizations/" + self.currentOrganization + "/users", null, success, failure);
    }
    this.requestAdmins = requestAdmins;

    //
    // Get access keys for organization
    //
    // GET: /management/organizations/<organization-name>/keys
    //
    function requestOrganizationCredentials(success, failure) {
        if (!self.currentOrganization) {
            failure();
        }
        apiGetRequest("/management/organizations/" + self.currentOrganization + "/credentials", null, success, failure);
    }
    this.requestOrganizationCredentials = requestOrganizationCredentials;

    //
    // Generate new access keys for organization
    //
    // POST: /management/organizations/<organization-name>/keys
    //
    function regenerateOrganizationCredentials(success, failure) {
        if (!self.currentOrganization) {
            failure();
        }
        apiRequest("POST", "/management/organizations/" + self.currentOrganization + "/credentials", null, null, success, failure);
    }
    this.regenerateOrganizationCredentials = regenerateOrganizationCredentials;

    //
    // Create new admin user for organization
    //
    // POST: /management/organizations/<organization-name>/users
    //
    function createAdmin(email, password, success, failure) {
        if (!self.currentOrganization) {
            failure();
        }
        apiRequest("POST", "/management/organizations/" + self.currentOrganization + "/users", null, JSON.stringify({
            email: email,
            password: password
        }), success, failure);
    }
    this.createAdmin = createAdmin;

    //
    // Get collections for application
    //
    // GET: /<application-id>
    //
    function requestCollections(applicationId, success, failure) {
        apiGetRequest("/" + applicationId, null, success, failure);
    }
    this.requestCollections = requestCollections;

    //
    // Create collection for application
    //
    // POST: /<application-id>
    //
    function createCollection(applicationId, collectionName, success, failure) {
        var collections = {};
        collections[collectionName] = {};
        var metadata = {
            metadata: {
                collections: collections
            }
        };
        apiRequest("PUT", "/" + applicationId, null, JSON.stringify(metadata), success, failure);
    }
    this.createCollection = createCollection;

    //
    // Get application keys
    //
    // GET: /<application-id>/auth/keys
    //
    function requestApplicationCredentials(applicationId, success, failure) {
        apiGetRequest("/" + applicationId + "/credentials", null, success, failure);
    }
    this.requestApplicationCredentials = requestApplicationCredentials;

    //
    // Get new application keys
    //
    // POST: /<application-id>/auth/keys
    //
    function regenerateApplicationCredentials(applicationId, success, failure) {
        apiRequest("POST", "/" + applicationId + "/credentials", null, null, success, failure);
    }
    this.regenerateApplicationCredentials = regenerateApplicationCredentials;

    //
    // Get application roles
    //
    // GET: /<application-id>/rolenames
    //
    function requestApplicationRoles(applicationId, success, failure) {
        apiGetRequest("/" + applicationId + "/rolenames", null, success, failure);
    }
    this.requestApplicationRoles = requestApplicationRoles;

    //
    // Get application counters
    //
    // GET: /<application-id>/counters
    //
    function requestApplicationCounterNames(applicationId, success, failure) {
        apiGetRequest("/" + applicationId + "/counters", null, success, failure);
    }
    this.requestApplicationCounterNames = requestApplicationCounterNames;

    //
    // Get application counters
    //
    // GET: /<application-id>/counters
    //
    function requestApplicationCounters(applicationId, start_time, end_time, resolution, counter, success, failure) {
        var params = {};
        if (start_time) params.start_time = start_time;
        if (end_time) params.end_time = end_time;
        if (resolution) params.resolution = resolution;
        if (counter) params.counter = counter;
        params.pad = true;
        apiGetRequest("/" + applicationId + "/counters", params, success, failure);
    }
    this.requestApplicationCounters = requestApplicationCounters;

    //
    // Get admin user
    //
    // GET: /management/users/<user-id>
    //
    function requestAdminUser(success, failure) {
        if (!self.loggedInUser) {
            failure();
        }
        apiGetRequest("/management/users/" + self.loggedInUser.uuid, null, success, failure);
    }
    this.requestAdminUser = requestAdminUser;

    //
    // Get admin user
    //
    // GET: /management/users/<user-id>
    //
    function updateAdminUser(properties, success, failure) {
        if (!self.loggedInUser) {
            failure();
        }
        apiRequest("PUT", "/management/users/" + self.loggedInUser.uuid, null, JSON.stringify(properties), success, failure);
    }
    this.updateAdminUser = updateAdminUser;

    //
    // Get admin feed
    //
    // GET: /management/users/<user-id>/feed
    //
    function requestAdminFeed(success, failure) {
        if (!self.loggedInUser) {
            failure();
        }
        apiGetRequest("/management/users/" + self.loggedInUser.uuid + "/feed", null, success, failure);
    }
    this.requestAdminFeed = requestAdminFeed;

    //
    // Perform user login, get session token
    //
    // POST: /management/users
    //
    function loginAdmin(email, password, success, failure) {
        self.loggedInUser = null;
        self.accessToken = null;
        self.currentOrganization = null;
        localStorage.removeItem('usergrid_user');
        localStorage.removeItem('usergrid_access_token');
        var formdata = {
            grant_type: "password",
            username: email,
            password: password
        };
        apiRequest("POST", "/management/token", formdata, null,
        function(response) {
            if (response && response.access_token && response.user) {
                self.loggedInUser = response.user;
                self.accessToken = response.access_token;
                self.currentOrganization = null;
                if (self.loggedInUser.organizations) {
                    for (first in self.loggedInUser.organizations) break;
                    if (first) {
                        self.currentOrganization = self.loggedInUser.organizations[first].uuid;
                    }
                }
                localStorage.setObject('usergrid_user', self.loggedInUser);
                localStorage.setObject('usergrid_access_token', self.accessToken);
                if (success) {
                    success();
                }
            } else if (failure) {
                failure();
            }
        },
        function(XMLHttpRequest, textStatus, errorThrown) {
            if (failure) {
                failure();
            }
        }
        );
    }
    this.loginAdmin = loginAdmin;

   function loginAppUser(applicationId, email, password, success, failure) {
       self.loggedInUser = null;
       self.accessToken = null;
       self.currentOrganization = null;
       localStorage.removeItem('usergrid_user');
       localStorage.removeItem('usergrid_access_token');
        var formdata = {
            grant_type: "password",
            username: email,
            password: password
        };
        apiRequest("POST", "/"+ applicationId + "/token", formdata, null,
        function(response) {
            if (response && response.access_token && response.user) {
                self.loggedInUser = response.user;
                self.accessToken = response.access_token;
                self.currentOrganization = null;
                localStorage.setObject('usergrid_user', self.loggedInUser);
                localStorage.setObject('usergrid_access_token', self.accessToken);
                if (success) {
                    success();
                }
            } else if (failure) {
                failure();
            }
        },
        function(XMLHttpRequest, textStatus, errorThrown) {
            if (failure) {
                failure();
            }
        }
        );
    }
    this.loginAppUser = loginAppUser;

    function loginWithAccessToken(email, accessToken, success, failure) {
        self.accessToken = accessToken;
        apiRequest("GET", "/management/users/" + email, null, null,
        function(response) {
            if (response && response.data) {
                self.loggedInUser = response.data;
                self.currentOrganization = null;
                if (self.loggedInUser.organizations) {
                    for (first in self.loggedInUser.organizations) break;
                    if (first) {
                        self.currentOrganization = self.loggedInUser.organizations[first].uuid;
                    }
                }
                localStorage.setObject('usergrid_user', self.loggedInUser);
                localStorage.setObject('usergrid_access_token', self.accessToken);
                if (success) {
                    success();
                }
            } else if (failure) {
                failure();
            }
        },
        function(XMLHttpRequest, textStatus, errorThrown) {
            if (failure) {
                failure();
            }
        }
        );
    }
    this.loginWithAccessToken = loginWithAccessToken;

    this.onLogout = null;
    function logout() {
        self.loggedInUser = null;
        self.accessToken = null;
        localStorage.removeItem('usergrid_user');
        localStorage.removeItem('usergrid_access_token');
        if (self.onLogout) {
            self.onLogout();
        }
    }
    this.logout = logout;

    function loggedIn() {
        return self.loggedInUser && self.accessToken;
    }
    this.loggedIn = loggedIn;

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
    this.signup = signup;

    function getUser(a) {
        var ns = self.applicationId;
        var id = arguments[0];
        if (countByType("string", arguments) >= 2) {
            ns = getByType("string", 0, arguments);
            id = getByType("string", 1, arguments);
        }
        var success = getByType("function", 0, arguments);
        var failure = getByType("function", 1, arguments);
        if (!ns) {
            return;
        }
        var params = getByType("object", 0, arguments);

        var path = "/" + ns + "/users/" + id;
        apiGetRequest(path, params, success, failure);
    }
    this.getUser = getUser;

    function queryEntities(root_collection, a) {
        var ns = self.applicationId;
        if (countByType("string", a) > 0) {
            ns = getByType("string", 0, a);
            if (!ns) ns = self.applicationId;
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
    this.queryUsers = queryUsers;

    function queryEntityCollection(root_collection, entity_collection, a) {
        var ns = self.applicationId;
        var id = a[0];
        if (countByType("string", a) >= 2) {
            ns = getByType("string", 0, a);
            id = getByType("string", 1, a);
        }
        var success = getByType("function", 0, a);
        var failure = getByType("function", 1, a);
        if (!ns) {
            return;
        }
        var options = getByType("object", 0, a) || {};

        var path = "/" + ns + "/" + root_collection + "/" + id + "/" + entity_collection;
        var q = new Query(ns, path, null, options, success, failure);
        q.send("GET", null);
        return q;
    }

    function queryUserMemberships(a) {
        return queryEntityCollection("users", "groups", arguments);
    }
    this.queryUserMemberships = queryUserMemberships;

    function queryUserActivities(a) {
        return queryEntityCollection("users", "activities", arguments);
    }
    this.queryUserActivities = queryUserActivities;

    function queryUserRoles(a) {
        return queryEntityCollection("users", "roles", arguments);
    }
    this.queryUserRoles = queryUserRoles;

    function queryUserPermissions(a) {
        return queryEntityCollection("users", "permissions", arguments);
    }
    this.queryUserPermissions = queryUserPermissions;

    function queryUserFollowing(a) {
        return queryEntityCollection("users", "following", arguments);
    }
    this.queryUserFollowing = queryUserFollowing;

    //
    // Create new application user for organization
    //
    // POST: /<application-id/users
    //
    function createUser(applicationId, username, fullname, email, password, success, failure) {
        apiRequest("POST", "/" + applicationId + "/users", null, JSON.stringify({
            username: username,
            name: fullname,
            email: email,
            password: password
        }), success, failure);
    }
    this.createUser = createUser;

    function queryActivities(a) {
        var ns = self.applicationId;
        if (countByType("string", arguments) > 0) {
            ns = getByType("string", 0, arguments);
            if (!ns) ns = self.applicationId;
        }
        var success = getByType("function", 0, arguments);
        var failure = getByType("function", 1, arguments);
        if (!ns) {
            return;
        }
        var options = getByType("object", 0, arguments) || {};
        if (!options.reversed) {
            options.reversed = true;
        }

        var q = new Query(ns, "/activities", null, options, success, failure);
        q.send("GET", null);
        return q;
    }
    this.queryActivities = queryActivities;

    function requestCollectionIndexes(applicationId, path, success, failure) {
        if (path.lastIndexOf("/", 0) !== 0) {
            path = "/" + path;
        }
        path = "/" + applicationId + path + "/indexes";
        apiGetRequest(path, null, success, failure);
    }
    this.requestCollectionIndexes = requestCollectionIndexes;

    /**
        Creates a new Query.
        @class Represents a Query. 
     */
    function Query(applicationId, path, ql, options, success, failure) {

        if (path.lastIndexOf("/", 0) !== 0) {
            path = "/" + path;
        }
        path = "/" + applicationId + path;

        var client = self;
        var self = this;
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
            apiRequest(method, path, params, data,
            function(data) {
                if (data.entities && data.entities.length > 1) {
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
                    success(data, self);
                }
            },
            function(data) {
                if (failure) {
                    failure(data, self);
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
    this.Query = Query;

    this.onAutoLogin = null;
    if (query_params.access_token && query_params.admin_email) {
        logout();
        loginWithAccessToken(query_params.admin_email, query_params.access_token,
        function(response) {
            console.log("Auto-logged in");
            if (self.onAutoLogin) self.onAutoLogin();
        },
        function() {
	        logout();
        });
        return;
    }

    self.loggedInUser = localStorage.getObject('usergrid_user');
    self.accessToken = localStorage.getObject('usergrid_access_token');
    if (self.loggedInUser && self.loggedInUser.organizations) {
        for (first in self.loggedInUser.organizations) break;
        if (first) {
            self.currentOrganization = self.loggedInUser.organizations[first].uuid;
        }
    }

};

