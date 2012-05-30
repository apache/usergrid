/**
    @application Holds global Usergrid JS functionality.
*/
var usergrid = usergrid || {};

/**
 * Creates a new Usergrid Client.
 * @class Represents a Usergrid client. 
 * @param {string} applicationId The id of the application (optional)
 */
usergrid.Client = function(options) {
    //This code block *WILL* load before the document is complete
    /** @property applicationId */

    if (!options) options = {};
    
    this.applicationId = options.applicationId || null;
    this.clientId = options.clientId || null;
    this.clientSecret = options.clientSecret || null;

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
    var PUBLIC_API_URL = "https://api.usergrid.com";

    //base tld
    APIGEE_TLD = "apigee.com";
    GHPAGES_TLD = "apigee.github.com";

    //flag to overide use SSO if needed set to ?use_sso=no
    USE_SSO = 'no';
    
    //Apigee SSO url
    var APIGEE_SSO_URL = "https://accounts.apigee.com/accounts/sign_in";

    //Apigee SSO Logout page
    var SSO_LOGOUT_PAGE = 'https://accounts.apigee.com/accounts/sign_out';
    self.sso_logout_page = SSO_LOGOUT_PAGE;

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
    if (!FORCE_PUBLIC_API && (document.domain.substring(0,9) == "localhost")) {
        this.apiUrl = LOCAL_API_URL;
        self.apiUrl = LOCAL_API_URL;
    }

    if (query_params.api_url) {
        this.apiUrl = query_params.api_url;
        self.apiUrl = query_params.api_url;
    }

    this.use_sso = USE_SSO;
    if (query_params.use_sso) {
        self.use_sso = query_params.use_sso;
    }

    this.apigee_sso_url = APIGEE_SSO_URL;
    if (query_params.apigee_sso_url) {
        this.apigee_sso_url = query_params.apigee_sso_url;
        self.apigee_sso_url = query_params.apigee_sso_url;
    }


    if (options.apiUrl) {
        this.apiUrl = options.apiUrl;
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
        var errorMsg = defaultMsg;
        if (self.error) {
            if (self.error.error_description) {
                errorMsg = self.error.error_description;
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
    /** @property accessToken */
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

        if ((method == "GET") || (method == "DELETE")) {
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
        else if (self.clientId && self.clientSecret) {
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
                            //var error = response.error_description;
                            setLastError(response);
                            if (error == "auth_expired_session_token") {
                                force_logout = true;
                            }
                            else if (error == "auth_missing_credentials") {
                                force_logout = true;
                            }
                            else if (error == "auth_invalid") {
                                force_logout = true;
                            }
                            else if (error == "unauthorized") {
                                force_logout = true;
                            }                            
                            else if (error == "expired_token") {
                                force_logout = true;
                            }
                            else if (error == "auth_bad_access_token") {
                                force_logout = true;
                            }
                            else if (error == "web_application") {
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
        apiGetRequest("/management/organizations/" + self.currentOrganization.uuid + "/applications", null, success, failure);
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
     *   }JSON.stringify(
     * }
     * </pre>
     */
    this.requestApplications = requestApplications;

    //
    // Add application to organization
    //
    // POST: /management/organizations/<organization-name>/applications
    // data: {name}
    //
    function createApplication(data, success, failure) {
        if (!self.currentOrganization) {
            failure();
        }
        apiRequest("POST", "/management/organizations/" + self.currentOrganization.uuid + "/applications", null, JSON.stringify(data), success, failure);
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
        apiGetRequest("/management/organizations/" + self.currentOrganization.uuid + "/users", null, success, failure);
    }
    this.requestAdmins = requestAdmins;

    //
    // Create new organization for admin user
    //
    // POST: /management/users/<user-id>/organizations
    // data: {organization}
    //
    function createOrganization(data, success, failure) {
        if (!self.loggedInUser) {
            failure();
        }
        apiRequest("POST", "/management/users/" + self.loggedInUser.uuid + "/organizations", null, JSON.stringify(data), success, failure);
    }
    this.createOrganization = createOrganization;
    
    
    function leaveOrganization(organizationUUID, success, failure) {
        if (!self.loggedInUser) {
            failure();
        }
        apiRequest("DELETE", "/management/users/" + self.loggedInUser.uuid + "/organizations/" + organizationUUID, null, null, success, failure);
    }
    this.leaveOrganization = leaveOrganization;
    
    
    
    //
    // Get admin users for organization
    //
    // GET: /management/organizations
    //
    function requestOrganizations(success, failure) {        
        apiGetRequest("/management/users/" + self.loggedInUser.uuid + "/organizations", null, success, failure);
    }
    this.requestOrganizations = requestOrganizations;
    
    //
    // Get access keys for organization
    //
    // GET: /management/organizations/<organization-name>/keys
    //
    function requestOrganizationCredentials(success, failure) {
        if (!self.currentOrganization) {
            failure();
        }
        apiGetRequest("/management/organizations/" + self.currentOrganization.uuid + "/credentials", null, success, failure);
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
        apiRequest("POST", "/management/organizations/" + self.currentOrganization.uuid + "/credentials", null, null, success, failure);
    }
    this.regenerateOrganizationCredentials = regenerateOrganizationCredentials;

    //
    // Create new admin user for organization
    //
    // POST: /management/organizations/<organization-name>/users
    // data: {email, password}
    //
    function createAdmin(data, success, failure) {
        if (!self.currentOrganization) {
            failure();
        }
        apiRequest("POST", "/management/organizations/" + self.currentOrganization.uuid + "/users", null, JSON.stringify(data), success, failure);
    }
    this.createAdmin = createAdmin;

    //
    // Get collections for application
    //
    // GET: /<application-id>
    //
    function requestCollections(applicationId, success, failure) {
        apiGetRequest("/" + self.currentOrganization.uuid + "/" + applicationId, null, success, failure);
    }
    this.requestCollections = requestCollections;

    //
    // Create collection for application
    //
    // POST: /<application-id>
    // data:{name}
    //
    function createCollection(applicationId, data, success, failure) {
        var collections = {};
        collections[data.name] = {};
        var metadata = {
            metadata: {
                collections: collections
            }
        };
        apiRequest("PUT", "/" + self.currentOrganization.uuid + "/" + applicationId, null, JSON.stringify(metadata), success, failure);
    }
    this.createCollection = createCollection;

    //
    // Get application keys
    //
    // GET: /<application-id>/auth/keys
    //
    function requestApplicationCredentials(applicationId, success, failure) {
        apiGetRequest("/" + self.currentOrganization.uuid + "/" + applicationId + "/credentials", null, success, failure);
    }
    this.requestApplicationCredentials = requestApplicationCredentials;

    //
    // Get new application keys
    //
    // POST: /<application-id>/auth/keys
    //
    function regenerateApplicationCredentials(applicationId, success, failure) {
        apiRequest("POST", "/" + self.currentOrganization.uuid + "/" + applicationId + "/credentials", null, null, success, failure);
    }
    this.regenerateApplicationCredentials = regenerateApplicationCredentials;

    //
    // Get application roles
    //
    // GET: /<application-id>/rolenames
    //
    function requestApplicationRoles(applicationId, success, failure) {
        apiGetRequest("/" + self.currentOrganization.uuid + "/" + applicationId + "/rolenames", null, success, failure);
    }
    this.requestApplicationRoles = requestApplicationRoles;

    //
    // Get application role permissions
    //
    // GET: /<application-id>/rolenames/<rolename>
    //
    function requestApplicationRolePermissions(applicationId, roleName, success, failure) {
        apiGetRequest("/" + self.currentOrganization.uuid + "/" + applicationId + "/rolenames/" + roleName, null, success, failure);
    }
    this.requestApplicationRolePermissions = requestApplicationRolePermissions;

    function requestApplicationRoleUsers(applicationId, roleId, success, failure) {
        apiGetRequest("/" + self.currentOrganization.uuid + "/" + applicationId + "/roles/" + roleId + "/users/", null, success, failure);
    }
    this.requestApplicationRoleUsers = requestApplicationRoleUsers;

    function addApplicationRolePermission(applicationId, roleName, permission, success, failure) {
        apiRequest("POST", "/" + self.currentOrganization.uuid + "/" + applicationId + "/rolenames/" + roleName, null, JSON.stringify({
            permission : permission
        }), success, failure);
    }
    this.addApplicationRolePermission = addApplicationRolePermission;

    function deleteApplicationRolePermission(applicationId, roleName, permission, success, failure) {
        apiRequest("DELETE", "/" + self.currentOrganization.uuid + "/" + applicationId + "/rolenames/" + roleName, {
            permission : permission
        }, null, success, failure);
    }
    this.deleteApplicationRolePermission = deleteApplicationRolePermission;

    function addApplicationUserPermission(applicationId, userName, permission, success, failure) {
        apiRequest("POST", "/" + self.currentOrganization.uuid + "/" + applicationId + "/users/" + userName + "/permissions", null, JSON.stringify({
            permission : permission
        }), success, failure);
    }
    this.addApplicationUserPermission = addApplicationUserPermission;

    function deleteApplicationUserPermission(applicationId, userName, permission, success, failure) {
        apiRequest("DELETE", "/" + self.currentOrganization.uuid + "/" + applicationId + "/users/" + userName + "/permissions", {
            permission : permission
        }, null, success, failure);
    }
    this.deleteApplicationUserPermission = deleteApplicationUserPermission;

    //
    // Get application counters
    //
    // GET: /<application-id>/counters
    //
    function requestApplicationCounterNames(applicationId, success, failure) {
        apiGetRequest("/" + self.currentOrganization.uuid + "/" + applicationId + "/counters", null, success, failure);
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
        apiGetRequest("/" + self.currentOrganization.uuid + "/" + applicationId + "/counters", params, success, failure);
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
	            setCurrentOrganization();
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
            username: email,
            password: '',
            invite: true
        };
        apiRequest("POST", "/" + self.currentOrganization.uuid + "/"+ applicationId + "/token", formdata, null,
        function(response) {
            if (response && response.access_token && response.user) {
                self.loggedInUser = response.user;
                self.accessToken = response.access_token;
                setCurrentOrganization();
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
	            setCurrentOrganization();
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

    function getAccessToken(){
        return self.accessToken;
    }
    this.getAccessToken = getAccessToken;


    function handleAutoLogin(email, token) {
        loginWithAccessToken(email, token,
        function(response) {
            console.log("Auto-logged in");
            if (self.onAutoLogin) {
                self.onAutoLogin();
            }
        },
        function() {
            logout();
        });
    }

    if (this.apiUrl != localStorage.getItem('usergrid_api_url')) {
        localStorage.setItem('usergrid_api_url', this.apiUrl);
    }

    function useSSO(){
        if (apigeeUser() || self.use_sso=='yes'){
            return true;
        } else {
            return false;
        }
    }
    this.useSSO = useSSO;

    function apigeeUser(){
        if (window.location.host == APIGEE_TLD || window.location.host == GHPAGES_TLD ) {
            return true;
        }
        return false;
    }

    this.onLogout = null;
    function logout() {
        clearLoginCredentials();
        if ( useSSO() ){
            sendToSSOLogoutPage();
        } else {
            self.onLogout();
        }
    }
    this.logout = logout;

    function clearLoginCredentials(){
        self.loggedInUser = null;
        self.accessToken = null;
        localStorage.removeItem('usergrid_user');
        localStorage.removeItem('usergrid_access_token');
    }
    this.clearLoginCredentials = clearLoginCredentials;

    function sendToSSOLogoutPage() {
        var newLoc= self.sso_logout_page + '?callback=' + getSSOCallback();
        window.location = newLoc;
        return false;
    }
    this.sendToSSOLogoutPage = sendToSSOLogoutPage;

    function sendToSSOLoginPage() {
        var newLoc = self.apigee_sso_url + '?callback=' + getSSOCallback();
        window.location = newLoc;
        throw "stop!";
        return false;
    }
    this.sendToSSOLoginPage = sendToSSOLoginPage;

    function getSSOCallback() {
        var callback = window.location.protocol+'//'+ window.location.host + window.location.pathname;
        var separatorMark = '?';
        if (self.use_sso) {
            callback = callback + separatorMark + 'use_sso=' + self.use_sso;
            separatorMark = '&';
        }
        if (self.apiUrl != PUBLIC_API_URL) {
            callback = callback + separatorMark +'api_url=' + self.apiUrl;
            separatorMark = '&';
        }
        return encodeURIComponent(callback);
    }
    this.getSSOCallback = getSSOCallback;

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

    function getEntity(collection, a) {
        var ns = self.currentOrganization.uuid + "/" +self.applicationId;
        var id = a[0];
        if (countByType("string", a) >= 2) {
            ns = self.currentOrganization.uuid + "/" + getByType("string", 0, a);
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
    this.getEntity = getEntity;

    function getUser(a) {
        return getEntity("users", arguments);
    }
    this.getUser = getUser;

    function getGroup(a) {
        return getEntity("groups", arguments);
    }
    this.getGroup = getGroup;

    function queryEntities(root_collection, a) {
        var ns = self.currentOrganization.uuid + "/" +self.applicationId;
        if (countByType("string", a) > 0) {
            ns = getByType("string", 0, a);
            if (!ns) ns = self.currentOrganization.uuid + "/" +self.applicationId;
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
        var ns = self.currentOrganization.uuid + "/" +self.applicationId;
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

        var path = "/" + root_collection + "/" + id + "/" + entity_collection;
        var q = new Query(ns, path, null, options, success, failure);
        q.send("GET", null);
        return q;
    }

    function deleteEntity(applicationId, entityId, path, success, failure) {        
        apiRequest("DELETE", "/" + self.currentOrganization.uuid + "/" + applicationId + "/" + path + "/" + entityId, null, null, success, failure);
    }
    this.deleteEntity = deleteEntity;
    
    function queryUserMemberships(a) {
        return queryEntityCollection("users", "groups", arguments);
    }
    this.queryUserMemberships = queryUserMemberships;

    function queryUserActivities(a) {
        return queryEntityCollection("users", "activities", arguments);
    }
    this.queryUserActivities = queryUserActivities;

    function queryUserRoles(applicationId, entityId, success, failure) {
        apiGetRequest("/" + self.currentOrganization.uuid + "/" + applicationId + "/users/" + entityId + "/roles", null, success, failure);
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

    function queryUserFollowers(a) {
        return queryEntityCollection("users", "followers", arguments);
    }
    this.queryUserFollowers = queryUserFollowers;

    function requestUserList(applicationId, searchString, success, failure) {
        if (searchString != "*") searchString = searchString + '*';        
        apiRequest("GET", "/" + self.currentOrganization.uuid + "/" + applicationId + "/users", null, JSON.stringify({
            username: searchString
        }), success, failure);
    }
    this.requestUserList = requestUserList;

    function requestUsers(applicationId, success, failure) {
        apiGetRequest("/" + self.currentOrganization.uuid + "/" + applicationId + "/users", null, success, failure);
    }
    this.requestUsers = requestUsers;

    //
    // Create new application user for organization
    //
    // POST: /<application-id/users
    // data: {username,name,email,password}
    //
    function createUser(applicationId, data, success, failure) {
        apiRequest("POST", "/" + self.currentOrganization.uuid + "/" + applicationId + "/users", null, JSON.stringify(data), success, failure);
    }
    this.createUser = createUser;

    function deleteUser(applicationId, userId, success, failure) {        
        apiRequest("DELETE", "/" + self.currentOrganization.uuid + "/" + applicationId + "/users/" + userId, null, null, success, failure);
    }
    this.deleteUser = deleteUser;

    function requestCollectionIndexes(applicationId, path, success, failure) {
        if (path.lastIndexOf("/", 0) !== 0) {
            path = "/" + path;
        }
        path = "/" + self.currentOrganization.uuid + "/" + applicationId + path + "/indexes";
        apiGetRequest(path, null, success, failure);
    }
    this.requestCollectionIndexes = requestCollectionIndexes;

    function queryGroups(a) {
         return queryEntities("groups", arguments);
    }
    this.queryGroups = queryGroups;

    function queryRoles(a) {
        return queryEntities("roles", arguments);
    }
    this.queryRoles = queryRoles;

    function queryActivities(a) {
        return queryEntities("activities", arguments);
    }
    this.queryActivities = queryActivities;

    function queryCollections(a) {
        return queryEntities("/", arguments);
    }
    this.queryCollections = queryCollections;

    function queryGroupMemberships(a) {
        return queryEntityCollection("groups", "users", arguments);
    }
    this.queryGroupMemberships = queryGroupMemberships;

    function queryGroupActivities(a) {
        return queryEntityCollection("groups", "activities", arguments);
    }
    this.queryGroupActivities = queryGroupActivities;

    function requestGroups(applicationId, success, failure) {
        apiGetRequest("/" + self.currentOrganization.uuid + "/" + applicationId + "/groups", null, success, failure);
    }
    this.requestGroups = requestGroups;

    function requestGroupRoles(applicationId, entityId, success, failure) {
        apiGetRequest("/" + self.currentOrganization.uuid + "/" + applicationId + "/groups/" + entityId + "/rolenames", null, success, failure);
    }
    this.requestGroupRoles = requestGroupRoles;

    function saveUserProfile(applicationId, userid, payload, success,failure){
        apiRequest("PUT", "/" + self.currentOrganization.uuid + "/" + applicationId + "/users/" + userid, null, JSON.stringify(payload) , success, failure);
    }
    this.saveUserProfile = saveUserProfile;

    function saveGroupProfile(applicationId, groupid, payload, success,failure){
        apiRequest("PUT", "/" + self.currentOrganization.uuid + "/" + applicationId + "/groups/" + groupid, null, JSON.stringify(payload) , success, failure);
    }
    this.saveGroupProfile = saveGroupProfile;

    //
    // Create new group    //
    // POST: /<application-id/users
    // data: {path, title}
    //
    function createGroup(applicationId, data, success, failure) {
        apiRequest("POST", "/" + self.currentOrganization.uuid + "/" + applicationId + "/groups", null, JSON.stringify(data), success, failure);
    }
    this.createGroup = createGroup;

    function deleteGroup(applicationId, groupId, success, failure) {        
        apiRequest("DELETE", "/" + self.currentOrganization.uuid + "/" + applicationId + "/groups/" + groupId, null, null, success, failure);
    }
    this.deleteGroup = deleteGroup;

    function addUserToGroup(applicationId, groupId, username, success, failure) {        
        apiRequest("POST", "/" + self.currentOrganization.uuid + "/" + applicationId + "/groups/" + groupId + "/users/" + username, null, "{ }", success, failure);
    }
    this.addUserToGroup = addUserToGroup;

    function removeUserFromGroup(applicationId, groupId, username, success, failure) {
        if (!self.loggedInUser) {
            failure();
        }
        apiRequest("DELETE",  "/" + self.currentOrganization.uuid + "/" + applicationId + "/groups/" + groupId + "/users/" + username, null, null, success, failure);
    }
    this.removeUserFromGroup = removeUserFromGroup;

    function entitySearch(applicationId, searchType, searchString, success, failure) {
       return queryEntities(searchType, arguments);
    }
    this.entitySearch = entitySearch;

    //
    // Create new role    //
    // POST: /<application-id/users
    // data: {name,title}
    //
    function createRole(applicationId, data, success, failure) {
        apiRequest("POST", "/" + self.currentOrganization.uuid + "/" + applicationId + "/rolenames", null, JSON.stringify(data), success, failure);
    }
    this.createRole = createRole;

    function addUserToRole(applicationId, roleId, username, success, failure) {
        apiRequest("POST", "/" + self.currentOrganization.uuid + "/" + applicationId + "/roles/" + roleId + "/users/" + username, null, "{ }", success, failure);
    }
    this.addUserToRole = addUserToRole;

    function removeUserFromRole(applicationId, username, roleId, success, failure) {
        if (!self.loggedInUser) {
            failure();
        }
        apiRequest("DELETE",  "/" + self.currentOrganization.uuid + "/" + applicationId + "/users/" + username + "/roles/" + roleId, null, null, success, failure);
    }
    this.removeUserFromRole = removeUserFromRole;

    function requestRoles(applicationId, success, failure) {
        apiGetRequest("/" + self.currentOrganization.uuid + "/" + applicationId + "/rolenames", null, success, failure);
    }
    this.requestRoles = requestRoles;

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

	function setCurrentOrganization(orgName) {
        self.currentOrganization = null;
        if (!self.loggedInUser || !self.loggedInUser.organizations)
            return;

        if(orgName)
            self.currentOrganization = self.loggedInUser.organizations[orgName];
        else
            self.currentOrganization = self.loggedInUser.organizations[localStorage.currentOrganizationName];

        if(!self.currentOrganization){
            var firstOrg = null;
			for (firstOrg in self.loggedInUser.organizations) break;
			if (firstOrg) self.currentOrganization = self.loggedInUser.organizations[firstOrg];
        }

        localStorage.currentOrganizationName = self.currentOrganization.name;
	}
    this.setCurrentOrganization = setCurrentOrganization;

    existingUser = localStorage.getObject('usergrid_user');
    existingAccessToken = localStorage.getObject('usergrid_access_token');


    //check to see if the user has a valid token
    if (!existingUser && !existingAccessToken) {
        //test to see if the Portal is running on Apigee, if so, send to SSO, if not, fall through to login screen
        if ( useSSO() ){
            Pages.clearPage();
            sendToSSOLoginPage();
        }
    } else if (existingAccessToken && existingUser.email) {
        handleAutoLogin(existingUser.email, existingAccessToken);
        return;
    }

    self.loggedInUser = localStorage.getObject('usergrid_user');
    self.accessToken = localStorage.getObject('usergrid_access_token');
    setCurrentOrganization();

};

