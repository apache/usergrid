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

//Usergrid namespace encapsulates this SDK
/*window.Usergrid = window.Usergrid || {};
Usergrid = Usergrid || {};
Usergrid.USERGRID_SDK_VERSION = '0.10.07';*/


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
	var tail = [];
	var item = [];
	var i;
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

Function.prototype.extend = function(superClass) {
				var subClass=this,F = function() {};
				F.prototype = superClass.prototype;
				subClass.prototype = new F();
				subClass.prototype.constructor = subClass;
 
				subClass.superclass = superClass.prototype;
				if(superClass.prototype.constructor == Object.prototype.constructor) {
								superClass.prototype.constructor = superClass;
				}
				return subClass;
}
//Logger
(function() {
	var name = 'Logger', global = this, overwrittenName = global[name], exports;
	/* logging */
	exports = (function() {
		function Logger(name) {
			function tr(m) {
				return {
					'E': 'End',
					'%': 'group',
					'#': 'profile',
					'!': 'time'
				}[m];
			}
			var logEnabled = true,
				methods = "log error warn info debug assert clear count dir dirxml exception % %Collapsed %E # #E table ! !E trace".replace(/([E%#!])/g, tr).split(' '),
				con = window.console;
			if(!name){
				name="UNKNOWN";
			}
			function createLogMethod(method) {
				return function() {
					var args=[].slice.call(arguments);
					var prepend='['+method.toUpperCase()+']['+name+']:';
					if("string"===typeof args[0]){
						args[0]=prepend+args[0];
					}else{
						args.unshift(prepend);
					}
					logEnabled && con && con[method] && con[method].apply(con, args);
				}
			}
			for (var i = 0; i < methods.length; i++) {
				var method = methods[i];
				this[method] = createLogMethod(method);
			}
		}
		return Logger;
	}());

	global[name] =  exports;
	global[name].noConflict = function() {
		if(overwrittenName){
			global[name] = overwrittenName;
		}
		return exports;
	};
	return global[name];
}());
//Promise
(function() {
	var name = 'Promise', global = this, overwrittenName = global[name], exports;
	
	exports = (function(global) {
		function Promise() {
			this.complete = false;
			this.error = null;
			this.result = null;
			this.callbacks = [];
		}
		Promise.prototype.create = function() {
			return new Promise()
		};
		Promise.prototype.then = function(callback, context) {
			var f = function() {
				return callback.apply(context, arguments)
			};
			if (this.complete) {
				f(this.error, this.result);
			} else {
				this.callbacks.push(f);
			}
		};
		Promise.prototype.done = function(error, result) {
			this.complete = true;
			this.error = error;
			this.result = result;
			for (var i = 0; i < this.callbacks.length; i++) this.callbacks[i](error, result);
			this.callbacks.length = 0;
		};
		Promise.join = function(promises) {
			var p = new Promise(),
				total = promises.length,
				completed = 0,
				errors = [],
				results = [];

			function notifier(i) {
				return function(error, result) {
					completed += 1;
					errors[i] = error;
					results[i] = result;
					if (completed === total) {
						p.done(errors, results);
					}
				};
			}
			for (var i = 0; i < total; i++) {
				promises[i]().then(notifier(i));
			}
			return p;
		};
		Promise.chain = function(promises, error, result) {
			var p = new Promise();
			if (promises===null||promises.length === 0) {
				p.done(error, result);
			} else {
				promises[0](error, result).then(function(res, err) {
					promises.splice(0, 1);
					//self.logger.info(promises.length)
					if(promises){
						Promise.chain(promises, res, err).then(function(r, e) {
							p.done(r, e);
						});
					}else{
						p.done(res, err);
					}
				});
			}
			return p;
		};
		return Promise;
	}(window));


	global[name] =  exports;
	global[name].noConflict = function() {
		if(overwrittenName){
			global[name] = overwrittenName;
		}
		return exports;
	};
	return global[name];
}());
//Ajax
(function() {
	var name = 'Ajax', global = this, overwrittenName = global[name], exports;

	Function.prototype.partial = function() {
		var fn = this,
			b = [].slice.call(arguments);
		return function() {
			return fn.apply(this, b.concat([].slice.call(arguments)))
		}
	}
	
	exports = (function() {
		function Ajax() {
			this.logger=new global.Logger(name);
			var self=this;
			function encode(data) {
				var result = "";
				if (typeof data === "string") {
					result = data;
				} else {
					var e = encodeURIComponent;
					for (var i in data) {
						if (data.hasOwnProperty(i)) {
							result += '&' + e(i) + '=' + e(data[i]);
						}
					}
				}
				return result;
			}
			var request = function(m, u, d) {
				var p = new Promise();
				(function(xhr) {
					xhr.onreadystatechange = function() {
						this.readyState ^ 4 || (self.logger.timeEnd(m + ' ' + u), p.done(null, this));
					};
					xhr.onerror=function(response){
						p.done(response, null);
					}
					xhr.onecomplete=function(response){
						self.info("%s request to %s returned %s", m, u, this.status );
					}
					xhr.open(m, u);
					self.logger.time(m + ' ' + u);
					xhr.send(encode(d));
				}(new XMLHttpRequest()));
				return p;
			};
			this.request=request;
			this.get = request.partial('GET');
			this.post = request.partial('POST');
			this.put = request.partial('PUT');
			this.delete = request.partial('DELETE');
		}
		return new Ajax();
	}());

	global[name] =  exports;
	global[name].noConflict = function() {
		if(overwrittenName){
			global[name] = overwrittenName;
		}
		return exports;
	};
	return global[name];
}());
//KeyStore
(function() {
	var name = 'KeyStore', global = this, overwrittenName = global[name], exports;
	
	exports = (function() {
		global.indexedDB = global.indexedDB || global.mozIndexedDB || global.webkitIndexedDB || global.msIndexedDB;
		// (Mozilla has never prefixed these objects, so we don't need global.mozIDB*)
		global.IDBTransaction = global.IDBTransaction || global.webkitIDBTransaction || global.msIDBTransaction;
		global.IDBKeyRange = global.IDBKeyRange || global.webkitIDBKeyRange || global.msIDBKeyRange;

		function KeyStore(database, version, storeName, keyPath, callback) {
			this.logger=new global.Logger(name);
			this.db = null;
			this.error = null;
			this.ready = false;
			this.storeName = storeName;
			this.keyPath = keyPath;
			var self = this;
			var request = indexedDB.open(database, version);

			function useDatabase(db, callback) {
				db.onversionchange = function(event) {
					db.close();
					console.warn("A new version of this keystore has been loaded. Please restart your application!");
				};
				self.db = db;
				self.ready = true;
				isFunction(callback) && callback(null, self);
			}
			request.onerror = function(event) {
				console.error("internal keystore error: " + event.target.errorCode);
				self.ready = true;
				self.error = event.target;
				isFunction(callback) && callback(event.target, self);
			};
			request.onupgradeneeded = function(event) {
				console.warn("upgrading internal keystore");
				var db = event.target.result;
				var objectStore, index;
				try {
					objectStore = event.currentTarget.transaction.objectStore(self.storeName);
					//console.log(objectStore);
					if (!objectStore) throw "not found";
				} catch (e) {
					objectStore = db.createObjectStore(self.storeName, {
						keyPath: self.keyPath
					});
				} finally {
					objectStore.transaction.oncomplete = function(event) {
						console.info("created ObjectStore: '%s'", self.storeName);
						//useDatabase(db, callback);
					};
				}
				try {
					index = objectStore.index(self.keyPath);
					if (!index) throw "not found";
				} catch (e) {
					objectStore.createIndex(self.keyPath, self.keyPath, {
						unique: true
					});
				} finally {
					objectStore.transaction.oncomplete = function(event) {
						console.info("created ObjectStore index: '%s'", self.keyPath);
						//useDatabase(db, callback);
					};
				}
			};
			request.onsuccess = function(event) {
				console.info("successfully opened database %s", event.target.result);
				useDatabase(event.target.result, callback);
			};
		}
		KeyStore.prototype.delete = function(key, callback) {
			var transaction = this.db.transaction([this.storeName], "readwrite");
			var objectStore = transaction.objectStore(this.storeName);
			var item = objectStore.get(key);
			item.onsuccess = function(event) {
				objectStore.delete(key);
				isFunction(callback) && callback(null, key);
			};
			item.onerror = function(event) {
				console.warn("Attempt to delete nonexistent item from keystore: %s", key);
				isFunction(callback) && callback(null, key);
			};
		};

		KeyStore.prototype.get = function(key, callback) {
			var transaction = this.db.transaction([this.storeName], "readwrite");
			var objectStore = transaction.objectStore(this.storeName);
			var item = objectStore.get(key);
			item.onsuccess = function(event) {
				console.log(event.target.result);
				isFunction(callback) && callback(null, event.target.result ? event.target.result.value : null);
			};
			item.onerror = function(event) {
				isFunction(callback) && callback(event.target, event.target.result ? event.target.result.value : null);
			};
		};

		KeyStore.prototype.set = function(key, value, callback) {
			var transaction = this.db.transaction([this.storeName], "readwrite");
			var objectStore = transaction.objectStore(this.storeName);
			var data = {
				'key': key,
				'value': value
			};
			var item = objectStore.get(key);
			item.onsuccess = function(event) {
				data.created = (event.target.result) ? event.target.result.created : Date.now();
				data.modified = Date.now();
				objectStore.delete(key);
				objectStore.add(data).onsuccess = function(event) {
					//event.target.result is the 'key'
					console.info("KEYSTORE: update %s=%s", data.key, data.value);
					isFunction(callback) && callback(null, data.value);
				};
			};
			item.onerror = function(event) {
				data.created = Date.now();
				data.modified = Date.now();
				objectStore.add(data).onsuccess = function(event) {
					console.info("KEYSTORE: create %s=%s", data.key = data.value);
					isFunction(callback) && callback(null, data);
				};
			};
		};
		return KeyStore;
	}());
	
	global[name] =  exports;
	global[name].noConflict = function() {
		if(overwrittenName){
			global[name] = overwrittenName;
		}
		return exports;
	};
	return global[name];
}());
(function() {
	var name = 'Usergrid', global = this, overwrittenName = global[name], exports;
	
	exports = (function() {
		function Usergrid(){
			this.logger=new global.Logger(name);
			var self=this;
			this.keyStore=new KeyStore('usergrid-javascript-sdk', 2, "data", "key", function(err, ks){
				self.logger.info("'%s' keystore created.", 'usergrid-javascript-sdk');
			});
		}
		Usergrid.isValidEndpoint = function(endpoint) {
			//TODO actually implement this
			return true;
		};
		Usergrid.prototype.set = function(key, value, callback) {
			if ("object" === typeof value) {
				try {
					value = JSON.stringify(value);
				} catch (e) {
					self.logger.warn("unable to stringify object: %s", e.message, value);
				}
			}
			this[key] = value;
			if ("undefined" !== typeof this.keyStore) {
				if (value) {
					this.keyStore.set(key, value, callback);
				} else {
					this.keyStore.delete(key, callback);
				}
			}else{
				callback.apply(this, [null, null]);
			}
		};

		Usergrid.prototype.get = function(key, callback) {
			var keyStore = keyPrefix + key,
				value;
			if ("undefined" !== typeof this.keyStore) {
				this.keyStore.get(key, callback);
			}
			try {
				if (/^(\{|\[)/.test(value)) {
					value = JSON.parse(value);
				}
			} catch (e) {
				self.logger.warn("unable to parse object: %s", e.message, value);
			} finally {

			}
			return value;
		};
		var VALID_REQUEST_METHODS=['GET','POST','PUT','DELETE'];
		Usergrid.Request=function(method, endpoint, query_params, data, callback){
			var p = new Promise();
			this.method=method.toUpperCase();
			this.endpoint=endpoint;
			this.query_params=query_params;
			this.data=data;
			this.logger=new global.Logger("Usergrid.Request");
			var self=this;
			if(VALID_REQUEST_METHODS.indexOf(this.method)===-1){
				throw "invalid request method '"+this.method+"'";
			}
			if(!Usergrid.isValidEndpoint(this.endpoint)){
				throw "The provided endpoint is not valid: "+this.endpoint;
			}
			var encoded_params = encodeParams(this.query_params);
			if (encoded_params) {
				this.endpoint += "?" + encoded_params;
			}
  			Ajax.request(this.method, this.endpoint, this.data).then(function(err, request){
  				//TODO create Usergrid.Response object
  				var response=new Usergrid.Response(request.responseText);
  				if(err){
  					callback(new Usergrid.Error(err), null);
  				}else if (response.data.error){
  					callback(new Usergrid.Error(response.data), null);
  				}else{
  					callback(null, response);
  				}
  			});
		};



		Usergrid.Response=function(response_data){
			this.text=response_data;
			this.logger=new global.Logger(name);
			var self=this;
			try{
				this.data=JSON.parse(this.text);
			}catch(e){
				this.logger.error("Error parsing response text: ",this.text);
				this.data=null;
			}
		};

		Usergrid.Error=function(options){
			this.name="UsergridError"; 
			this.timestamp=Date.now();
			if(options instanceof Error){
				this.exception=options.name||"";
				this.message=options.message||"An error has occurred";
			}else if("object"===typeof options){
				this.exception=options.error||"unknown_error";
				this.message=options.error_description||"An error has occurred";
			}else if("string"===typeof options){
				this.exception="unknown_error";
				this.message=options||"An error has occurred";
			}
		}
		Usergrid.Error.prototype=new Error();

		Usergrid.Client=function(){};
		Usergrid.Entity=function(){};
		Usergrid.Collection=function(){};
		return Usergrid;
	}());
	//TODO create keystore interface (Function.length)
	

	//define a key prefix for our storage object keys
	/*Object.defineProperty(this, "keyPrefix", {
		enumerable: false,
		configurable: false,
		writable: false,
		value: "apigee_"
	});*/

	global[name] =  exports;
	global[name].noConflict = function() {
		if(overwrittenName){
			global[name] = overwrittenName;
		}
		return exports;
	};
	return global[name];
}());
/*(function(global, storage) {
	var name = 'Usergrid', overwrittenName = global[name];
	var Usergrid=Usergrid||global.Usergrid;

	global[name] =  {
			Request		: Usergrid.Request, 
			Response	: Usergrid.Response,
			Error 		: Usergrid.Error,
			Client 		: Usergrid.Client,		
			Entity 		: Usergrid.Entity,		
			Collection 	: Usergrid.Collection,		
			version   : '0.10.07'
	};
	global[name].noConflict = function() {
		if(overwrittenName){
			global[name] = overwrittenName;
		}
		return Usergrid;
	};
	return global[name];
}(this, localStorage));*/
