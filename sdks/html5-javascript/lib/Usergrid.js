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
			this.database=database;
			this.version=version;
			this.storeName = storeName;
			this.keyPath = keyPath;
			this.init(callback)
			
		}
		KeyStore.prototype.init=function(callback) {
			var self=this;
			Promise.chain([
				function(){return self.openDatabase(null,self)},
				function(err,self){return self.getStore(err,self)}
			]).then(callback);
		};
		KeyStore.prototype.openDatabase=function(err, self) {
			var p = new Promise();
			var req = global.indexedDB.open(this.database, this.version);
			req.onerror=function(){done(this, self)};
			req.onsuccess = function (evt) {
				self.db = this.result;
				//self.objectStore=self.db.transaction([self.storeName], "readwrite").objectStore(self.storeName);
				p.done(null, self);
			};
			req.onupgradeneeded = function (evt) {
				try {
					self.objectStore = event.currentTarget.transaction.objectStore(self.storeName);
				} catch (e) {
					self.objectStore = self.db.createObjectStore(self.storeName, {keyPath: self.keyPath});
					self.objectStore.createIndex(self.keyPath, self.keyPath, {unique: true});
				}
			};
			return p;
		}
		KeyStore.prototype.getStore=function(err, self){
			var p = new Promise();
			self.objectStore = self.db.transaction([self.storeName], "readwrite").objectStore(self.storeName);
			p.done(null, self);
			return p;
		};
		KeyStore.prototype.delete = function(key, callback) {
			var self=this;
			var p = new Promise();
			self.get(key).then(function(err, value){
				var item = self.objectStore.get(key);
				item.onsuccess = function(event) {
					self.objectStore.delete(key);
					p.done(null, self);
				};
				item.onerror = function(event) {
					p.done(this, self);
				};
				isFunction(callback) && callback(err, self);
			});
			return p;
		};
		KeyStore.prototype.get = function(key, callback) {
			var self=this;
			var p = new Promise();
			Promise.chain([
				function(){return self.openDatabase(null,self)},
				function(err,self){return self.getStore(err,self)},
				function(err, self){
					var p = new Promise();
					var item = self.objectStore.get(key);
					item.onsuccess = function(event) {
						p.done(null, this.result ? this.result.value : null);
					};
					item.onerror = function(event) {
						p.done(this, null);
					};
					return p;
				}
			]).then(function(err, value){
				isFunction(callback) && callback(err, value);
				p.done(null, self);
			})
			return p;
		};
		KeyStore.prototype.set = function(key, value, callback) {
			var self=this;
			var data = {
				'key': key,
				'value': value
			};
			var p = new Promise();
			self.delete(key)
				.then(function(err, self){
					var item= self.objectStore.add(data);
					item.onsuccess = function(event) {
						p.done(null, data.value);
					};
					item.onerror=function(event){
						p.done(this, null);
					}
					isFunction(callback) && callback(err, self);
				})
			return p;
		};
		KeyStore.prototype.clear = function(callback) {
			var self=this;
		    self.getStore(function(err, store){
				var req=store.clear();
			    req.onsuccess = function(evt) {
					isFunction(callback) && callback(null, null);
			    };
			    req.onerror = function (evt) {
					isFunction(callback) && callback(new Usergrid.Error(this), null);
			    };
			});
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
		}
		Usergrid.isValidEndpoint = function(endpoint) {
			//TODO actually implement this
			return true;
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
  				return new Usergrid.Response(err, request, callback);
  			});
		};


		//TODO more granular handling of statusCodes
		Usergrid.Response=function(err, response, callback){
			this.logger=new global.Logger(name);
			this.success=true;
			this.err=err;
			this.response=response;
			this.text=this.response.responseText;
			try{
				this.data=JSON.parse(this.text);
			}catch(e){
				this.logger.error("Error parsing response text: ",this.text);
				this.data=null;
			}
			this.status=parseInt(response.status);
			this.statusGroup=(this.status - this.status%100);
			switch(this.statusGroup){
				case 200:
					this.success=true;
					break;
				case 400:
				case 500:
				case 300:
				case 100:
				default:
					//server error
					this.success=false;
					break;
			}
			var self=this;
			if(this.success){
				callback(null, self);
			}else{
				callback(new Usergrid.Error(this.data), self);
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

		Usergrid.Client=function(callback){
			this.logger=new global.Logger(name);
			var self=this;
			var keyStore=new KeyStore('usergrid-javascript-sdk', 2, "data", "key", function(err, ks){
				self.keyStore=ks;
				callback(err, self);
			});
		};
		Usergrid.Client.prototype.set = function(key, value, callback) {
			value = JSON.stringify(value);
			if (value) {
				this.keyStore.set(key, value, callback);
			} else {
				this.keyStore.delete(key, callback);
			}
		};
		Usergrid.Client.prototype.get = function(key, callback) {
			if ("undefined" !== typeof this.keyStore) {
				this.keyStore.get(key, callback);
			}
		};
		Usergrid.Entity=function(){};
		Usergrid.Collection=function(){};
		return Usergrid;
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


