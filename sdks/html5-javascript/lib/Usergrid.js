/*
 *  This module is a collection of classes designed to make working with
 *  the Appigee App Services API as easy as possible.
 *  Learn more at http://Usergrid.com/docs/usergrid
 *
 *   Copyright 2012 Usergrid Corporation
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
 *
 *  @author rod simpson (rod@Usergrid.com)
 *  @author matt dobson (matt@Usergrid.com)
 *  @author ryan bridges (rbridges@Usergrid.com)
 */


//Hack around IE console.log
window.console = window.console || {};
window.console.log = window.console.log || function() {};


function extend(subClass, superClass) {
    var F = function() {};
    F.prototype = superClass.prototype;
    subClass.prototype = new F();
    subClass.prototype.constructor = subClass;

    subClass.superclass = superClass.prototype;
    if(superClass.prototype.constructor == Object.prototype.constructor) {
        superClass.prototype.constructor = superClass;
    }
    return subClass;
}

function NOOP(){}

//Usergrid namespace encapsulates this SDK
/*window.Usergrid = window.Usergrid || {};
Usergrid = Usergrid || {};
Usergrid.USERGRID_SDK_VERSION = '0.10.07';*/


function isValidUrl(url) {
    if (!url) return false;
    var doc, base, anchor, isValid=false;
    try{
        doc = document.implementation.createHTMLDocument('');
        base = doc.createElement('base');
        base.href = base || window.lo;
        doc.head.appendChild(base);
        anchor = doc.createElement('a');
        anchor.href = url;
        doc.body.appendChild(anchor);
        isValid=!(anchor.href === '')
    }catch(e){
        console.error(e);
    }finally{
        doc.head.removeChild(base);
        doc.body.removeChild(anchor);
        base=null;
        anchor=null;
        doc=null;
        return isValid;
    }
}

/*
 * Tests if the string is a uuid
 *
 * @public
 * @method isUUID
 * @param {string} uuid The string to test
 * @returns {Boolean} true if string is uuid
 */
var uuidValueRegex = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/;
function isUUID(uuid) {
	return (!uuid)?false:uuidValueRegex.test(uuid);
}

/*
 *  method to encode the query string parameters
 *
 *  @method encodeParams
 *  @public
 *  @params {object} params - an object of name value pairs that will be urlencoded
 *  @return {string} Returns the encoded string
 */
function encodeParams(params) {
    var queryString;
    if(params && Object.keys(params)){
        queryString = [].slice.call(arguments)
            .reduce(function(a, b) {return a.concat((b instanceof Array)?b:[b]);},[])
            .filter(function(c){return "object"===typeof c})
            .reduce(function(p,c){(!(c instanceof Array)) ? p= p.concat(Object.keys(c).map(function(key){return [key,c[key]]})): p.push(c); return p;},[])
            .reduce(function(p,c){((c.length===2) ? p.push(c) : p= p.concat(c)); return p;},[])
            .reduce(function(p,c){(c[1] instanceof Array) ? c[1].forEach(function(v){p.push([c[0],v])}): p.push(c); return p;},[])
            .map(function(c){c[1]=encodeURIComponent(c[1]); return c.join('=')})
            .join('&');
    }
    return queryString;
}


/*
 *  method to determine whether or not the passed variable is a function
 *
 *  @method isFunction
 *  @public
 *  @params {any} f - any variable
 *  @return {boolean} Returns true or false
 */
function isFunction(f) {
	return (f && f !== null && typeof(f) === 'function');
}

/*
 *  a safe wrapper for executing a callback
 *
 *  @method doCallback
 *  @public
 *  @params {Function} callback - the passed-in callback method
 *  @params {Array} params - an array of arguments to pass to the callback
 *  @params {Object} context - an optional calling context for the callback
 *  @return Returns whatever would be returned by the callback. or false.
 */
function doCallback(callback, params, context) {
	var returnValue;
	if (isFunction(callback)) {
		if (!params) params = [];
		if (!context) context = this;
		params.push(context);
		try {
			returnValue = callback.apply(context, params);
		} catch (ex) {
			if (console && console.error) {
				console.error("Callback error:", ex);
			}
		}
	}
	return returnValue;
}

//noinspection ThisExpressionReferencesGlobalObjectJS
(function(global) {
	var name = 'Usergrid', overwrittenName = global[name];

    function Usergrid() {
        this.logger = new Logger(name);
    }

    Usergrid.isValidEndpoint = function (endpoint) {
        //TODO actually implement this
        return true;
    };
    var VALID_REQUEST_METHODS = ['GET', 'POST', 'PUT', 'DELETE'];
    Usergrid.Request = function (method, endpoint, query_params, data, callback) {
        var p = new Promise();
        /*
         Create a logger
         */
        this.logger = new global.Logger("Usergrid.Request");
        this.logger.time("process request " + method + " " + endpoint);
        /*
         Validate our input
         */
        this.endpoint=endpoint;
        this.method = method.toUpperCase();
        this.query_params = query_params;
        this.data = ("object" === typeof data) ? JSON.stringify(data) : data;

        if (VALID_REQUEST_METHODS.indexOf(this.method) === -1) {
            throw new UsergridInvalidHTTPMethodError("invalid request method '" + this.method + "'");
        }

        /*
         Prepare our request
         */
        if (!isValidUrl(this.endpoint)) {
            this.logger.error(endpoint, this.endpoint, /^https:\/\//.test(endpoint), api_uri, orgName, appName);
            throw new UsergridError("The provided endpoint is not valid: " + this.endpoint);
        }
        /* a callback to make the request */
        var request=function () {return Ajax.request(this.method, this.endpoint, this.data)}.bind(this);
        /* a callback to process the response */
        var response=function (err, request) {return new Usergrid.Response(err, request)}.bind(this);
        /* a callback to clean up and return data to the client */
        var oncomplete=function (err, response) {
            p.done(err, response);
            this.logger.info("REQUEST", err, response);
            doCallback(callback, [err, response]);
            this.logger.timeEnd("process request " + method + " " + endpoint);
        }.bind(this);
        /* and a promise to chain them all together */
        Promise.chain([request,response]).then(oncomplete);

        return p;
    };
    Usergrid.Request.prototype= new UsergridStorable();
    Usergrid.Request.prototype.validate=function(){
        var p = new Promise();
        p.done(null, this);
        return p;
    }
    Usergrid.Request.prototype.prepare=function(){
        var p = new Promise();
        p.done(null, this);
        return p;
    }
    Usergrid.Request.prototype.fire=function(){
        var p = new Promise();
        p.done(null, this);
        return p;
    }

    //TODO more granular handling of statusCodes
    Usergrid.Response = function (err, response) {
        var p = new Promise();
        this.logger = new global.Logger(name);
        this.success = true;
        this.err = err;
        this.data = {};
        var data;
        try {
            data = JSON.parse(response.responseText);
        } catch (e) {
            //this.logger.error("Error parsing response text: ",this.text);
            this.logger.error("Caught error ", e.message);
            data = {}
        } finally {
            this.data = data;
        }
        this.status = parseInt(response.status);
        this.statusGroup = (this.status - this.status % 100);
        switch (this.statusGroup) {
            case 200:
                this.success = true;
                break;
            case 400:
            case 500:
            case 300:
            case 100:
            default:
                //server error
                this.success = false;
                break;
        }
        var self = this;
        if (this.success) {
            p.done(null, this);
        } else {
            p.done(UsergridError.fromResponse(this.data), this);
        }
        return p;
    };
    Usergrid.Response.prototype.getEntities = function () {
        var entities=[]
        if (this.success && this.data.entities) {
            entities=this.data.entities;
        }
        return entities;
    }
    Usergrid.Response.prototype.getEntity = function () {
        var entities=this.getEntities();
        return entities[0];
    }
    Usergrid.Client = function (options) {
        this.logger = new global.Logger('Usergrid.Client');
        var self = this;
        this.getStorage();
        this.set('api_uri', options.URI || 'https://api.usergrid.com');
        this.set('orgName', options.orgName);
        this.set('appName', options.appName);
        //other options
        this.set('buildCurl', options.buildCurl || false);
        this.set('logging', options.logging || false);

        //Moved to Ajax transport layer
        /*this.set('api_call_timeout', options.callTimeout || 30000);
         this.set('api_call_timeout_callback', options.callTimeoutCallback || global.NOOP);
         this.set('api_logout_callback', options.logoutCallback || global.NOOP);*/


    };
    /*
     Add browser storage capability (defaults to sessionStorage,
     but you can inject anything that implements window.Storage
     eg. client._storage=localStorage;
     */
    UsergridStorable.mixin(Usergrid.Client);
    /*
     Add rudimentary eventing
     */
    UsergridEventable.mixin(Usergrid.Client);

    /*
     *  function for building asset urls
     *
     *  @method buildAssetURL
     *  @public
     *  @params {string} uuid
     *  @return {string} assetURL
     */
    Usergrid.Client.prototype.buildAssetURL = function(uuid) {
        var qs = {};
        var uri_elements=this.get('api_uri','orgName','appName');
        uri_elements=uri_elements.concat(['assets', uuid, 'data']);
        var assetURL = uri_elements.join('/');
        var token = this.getToken();
        if (token) {
            qs.access_token = token;
        }
        //append params to the path
        var encoded_params = encodeParams(qs);
        if (encoded_params) {
            assetURL += "?" + encoded_params;
        }

        return assetURL;
    };

    /*
     *  function for building asset urls
     *
     *  @method buildAssetURL
     *  @public
     *  @params {string} uuid
     *  @return {string} assetURL
     */
    Usergrid.Client.prototype.buildEndpointURL = function(endpoint, qs) {
        qs =qs|| {};
        var endSlashes=/(^\/|\/$)/g;
        var assetURL = [endpoint.replace(endSlashes,'')].concat(this.get('api_uri','orgName','appName'));
        var token = this.getToken();
        if (token) {qs.access_token = token;}
        var encoded_params = encodeParams(qs);
        if (encoded_params) {
            assetURL += "?" + encoded_params;
        }

        return assetURL;
    };


    /*
     *  Main function for making requests to the API.  Can be called directly.
     *
     *  options object:
     *  `method` - http method (GET, POST, PUT, or DELETE), defaults to GET
     *  `qs` - object containing querystring values to be appended to the uri
     *  `body` - object containing entity body for POST and PUT requests
     *  `endpoint` - API endpoint, for example 'users/fred'
     *  `mQuery` - boolean, set to true if running management query, defaults to false
     *
     *  @method request
     *  @public
     *  @params {object} options
     *  @param {function} callback
     *  @return {callback} callback(err, data)
     */
    Usergrid.Client.prototype.request = function (options, callback) {
        var p = new Promise();
        var _callback = function (err, data) {
            p.done(err, data);
            doCallback(callback, [err, data]);
        };
        this.logger = new Logger("Request");
        var self = this;
        var method = options.method || 'GET';
        var endpoint = options.endpoint;
        var body = options.body || {};
        var qs = options.qs || {};
        var mQuery = options.mQuery || false; //is this a query to the management endpoint?
        /*
         could also use headers for the token
         xhr.setRequestHeader("Authorization", "Bearer " + self.getToken());
         xhr.withCredentials = true;
         */
        var api_uri = this.get('api_uri');
        var orgName = this.get('orgName');
        var appName = this.get('appName');
        if (this.getToken())qs.access_token = this.getToken();

        //if(isValidUrl(endpoint)){
        if (api_uri && orgName && appName) {
            endpoint = [api_uri, orgName, appName, endpoint].join('/');
        } else {
            endpoint = [api_uri, endpoint].join('/');
            //throw new UsergridInvalidURIError('No Org name or App name specified.', 'no_org_or_app_name_specified');
        }

        var req = new Usergrid.Request(method, endpoint, qs, body, function (err, response) {
            if ([
                "auth_expired_session_token",
                "auth_missing_credentials",
                "auth_unverified_oath",
                "expired_token",
                "unauthorized",
                "auth_invalid"
            ].indexOf(response.error) !== -1) {
                //throw err;
            }
            doCallback(callback, [err, response]);
            p.done(err, response);
        });


        return p;
        /*}catch(e){
         if (typeof(this.logoutCallback) === 'function') {
         this.logoutCallback(true, 'no_org_or_app_name_specified');
         }
         _callback(true, e);

         }*/
    };

    /*
     *  Main function for creating new entities - should be called directly.
     *
     *  options object: options {data:{'type':'collection_type', 'key':'value'}, uuid:uuid}}
     *
     *  @method createEntity
     *  @public
     *  @params {object} options
     *  @param {function} callback
     *  @return {callback} callback(err, data)
     */
    Usergrid.Client.prototype.createEntity = function (options, callback) {
        // todo: replace the check for new / save on not found code with simple save
        // when users PUT on no user fix is in place.
        var getOnExist = options['getOnExist'] || false; //if true, will return entity if one already exists
        delete options['getOnExist']; //so it doesn't become part of our data model
        var entity_data = {
            client: this,
            data: options
        };
        var entity = new Usergrid.Entity(entity_data);
        var self = this;
        entity.fetch(function (err, data) {
            console.log(err, data);
            //if the fetch doesn't find what we are looking for, or there is no error, do a save
            var common_errors = ['service_resource_not_found', 'no_name_specified', 'null_pointer'];
            var okToSave = (err.name && common_errors.indexOf(err.name) !== -1) || (!err && getOnExist);

            if (okToSave) {
                entity.set(entity_data.data); //add the data again just in case
                entity.save(function (err, data) {
                    doCallback(callback, [err, entity, data]);
                });
            } else {
                doCallback(callback, [err, entity, data]);
            }
        });

    };

    /*
     *  Main function for getting existing entities - should be called directly.
     *
     *  You must supply a uuid or (username or name). Username only applies to users.
     *  Name applies to all custom entities
     *
     *  options object: options {data:{'type':'collection_type', 'name':'value', 'username':'value'}, uuid:uuid}}
     *
     *  @method createEntity
     *  @public
     *  @params {object} options
     *  @param {function} callback
     *  @return {callback} doCallback(callback, [err, data])
     */
    Usergrid.Client.prototype.getEntity = function (options, callback) {
        var entity_data = {
            client: this,
            data: options
        };
        var entity = new Usergrid.Entity(entity_data);
        entity.fetch(function (err, data) {
            doCallback(callback, [err, entity, data]);
        });
    };

    /*
     *  Main function for restoring an entity from serialized data.
     *
     *  serializedObject should have come from entityObject.serialize();
     *
     *  @method restoreEntity
     *  @public
     *  @param {string} serializedObject
     *  @return {object} Entity Object
     */
    Usergrid.Client.prototype.restoreEntity = function (serializedObject) {
        return new Usergrid.Entity({ client: this, data: JSON.parse(serializedObject)});
    };
    /*
     *  Main function for creating new collections - should be called directly.
     *
     *  options object: options {client:client, type: type, qs:qs}
     *
     *  @method createCollection
     *  @public
     *  @params {object} options
     *  @param {function} callback
     *  @return {callback} callback(err, data)
     */
    Usergrid.Client.prototype.createCollection = function (options, callback) {
        options.client = this;
        var collection = new Usergrid.Collection(options, function (err, data) {
            if (typeof(callback) === 'function') {
                callback(err, collection, data);
            }
        });
    };

    //Usergrid.Entity=function(){};
		//Usergrid.Collection=function(){};
	global[name] =  Usergrid;
	global[name].noConflict = function() {
		if(overwrittenName){
			global[name] = overwrittenName;
		}
		return Usergrid;
	};
	return global[name];
}(this));



