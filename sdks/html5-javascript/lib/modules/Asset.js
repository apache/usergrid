/*
 *Licensed to the Apache Software Foundation (ASF) under one
 *or more contributor license agreements.  See the NOTICE file
 *distributed with this work for additional information
 *regarding copyright ownership.  The ASF licenses this file
 *to you under the Apache License, Version 2.0 (the
 *"License"); you may not use this file except in compliance
 *with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0

 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */



/*
 *  XMLHttpRequest.prototype.sendAsBinary polyfill
 *  from: https://developer.mozilla.org/en-US/docs/DOM/XMLHttpRequest#sendAsBinary()
 *
 *  @method sendAsBinary
 *  @param {string} sData
 */
if (!XMLHttpRequest.prototype.sendAsBinary) {
	XMLHttpRequest.prototype.sendAsBinary = function(sData) {
		var nBytes = sData.length,
			ui8Data = new Uint8Array(nBytes);
		for (var nIdx = 0; nIdx < nBytes; nIdx++) {
			ui8Data[nIdx] = sData.charCodeAt(nIdx) & 0xff;
		}
		this.send(ui8Data);
	};
}


/*
 *  A class to model a Usergrid asset.
 *
 *  @constructor
 *  @param {object} options {name:"photo.jpg", path:"/user/uploads", "content-type":"image/jpeg", owner:"F01DE600-0000-0000-0000-000000000000" }
 *  @returns {callback} callback(err, asset)
 */
Usergrid.Asset = function(options, callback) {
	var self = this,
		messages = [];
	self._client = options.client;
	self._data = options.data || {};
	self._data.type = "assets";
	var missingData = ["name", "owner", "path"].some(function(required) {
		return !(required in self._data);
	});
	if (missingData) {
		doCallback(callback, [new UsergridError("Invalid asset data: 'name', 'owner', and 'path' are required properties."), null, self], self);
	} else {
		self.save(function(err, data) {
			if (err) {
				doCallback(callback, [new UsergridError(data), data, self], self);
			} else {
				if (data && data.entities && data.entities.length) {
					self.set(data.entities[0]);
				}
				doCallback(callback, [null, data, self], self);
			}
		});
	}
};

/*
 *  Inherit from Usergrid.Entity.
 */
Usergrid.Asset.prototype = new Usergrid.Entity();

/*
 *  Add an asset to a folder.
 *
 *  @method connect
 *  @public
 *  @param {object} options {folder:"F01DE600-0000-0000-0000-000000000000"}
 *  @returns {callback} callback(err, asset)
 */

Usergrid.Asset.prototype.addToFolder = function(options, callback) {
	var self = this,
		error = null;
	if (('folder' in options) && isUUID(options.folder)) {
		//we got a valid UUID
		var folder = Usergrid.Folder({
			uuid: options.folder
		}, function(err, folder) {
			if (err) {
				doCallback(callback, [UsergridError.fromResponse(folder), folder, self], self);
			} else {
				var endpoint = ["folders", folder.get("uuid"), "assets", self.get("uuid")].join('/');
				var options = {
					method: 'POST',
					endpoint: endpoint
				};
				this._client.request(options, function(err, response) {
					if (err) {
						doCallback(callback, [UsergridError.fromResponse(folder), response, self], self);
					} else {
						doCallback(callback, [null, folder, self], self);
					}


				});
			}
		});
	} else {
		doCallback(callback, [new UsergridError('folder not specified'), null, self], self);
	}
};

Usergrid.Entity.prototype.attachAsset = function (file, callback) { 
    
    if (!(window.File && window.FileReader && window.FileList && window.Blob)) {
        doCallback(callback, [ new UsergridError("The File APIs are not fully supported by your browser."), null, this ], this);
        return;
    }
    var self = this;
    var args = arguments;
    var type = this._data.type;
    var attempts = self.get("attempts");
    if (isNaN(attempts)) {
        attempts = 3;
    }

    if(type != 'assets' && type != 'asset') {        
        var endpoint = [ this._client.URI, this._client.orgName, this._client.appName, type, self.get("uuid") ].join("/");            
    } else {
        self.set("content-type", file.type);
        self.set("size", file.size);
        var endpoint = [ this._client.URI, this._client.orgName, this._client.appName, "assets", self.get("uuid"), "data" ].join("/");    
    }

    var xhr = new XMLHttpRequest();
    xhr.open("POST", endpoint, true);
    xhr.onerror = function(err) {
        doCallback(callback, [ new UsergridError("The File APIs are not fully supported by your browser.") ], xhr, self);
    };
    xhr.onload = function(ev) {
        if (xhr.status >= 500 && attempts > 0) {
            self.set("attempts", --attempts);
            setTimeout(function() {
                self.attachAsset.apply(self, args);
            }, 100);
        } else if (xhr.status >= 300) {
            self.set("attempts");
            doCallback(callback, [ new UsergridError(JSON.parse(xhr.responseText)), xhr, self ], self);
        } else {
            self.set("attempts");
            self.fetch();
            doCallback(callback, [ null, xhr, self ], self);
        }
    };
    var fr = new FileReader();
    fr.onload = function() {
        var binary = fr.result;        
        if (type === 'assets' || type === 'asset') {
            xhr.overrideMimeType("application/octet-stream");
            xhr.setRequestHeader("Content-Type", "application/octet-stream");
        }
        xhr.sendAsBinary(binary);
    };
    fr.readAsBinaryString(file);

};

/*
 *  Upload Asset data
 *
 *  @method upload
 *  @public
 *  @param {object} data Can be a javascript Blob or File object
 *  @returns {callback} callback(err, asset)
 */
Usergrid.Asset.prototype.upload = function(data, callback) {
   this.attachAsset(data, function(err, response) {
        if(!err){
            doCallback(callback, [ null, response, self ], self);
        } else {
            doCallback(callback, [ new UsergridError(err), response, self ], self);
        }
    }); 
};

/*
 *  Download Asset data
 *
 *  @method download
 *  @public
 *  @returns {callback} callback(err, blob) blob is a javascript Blob object.
 */
Usergrid.Entity.prototype.downloadAsset = function(callback) {
    var self = this;    
    var endpoint;
    var type = this._data.type;

    var xhr = new XMLHttpRequest();
    if(type != "assets" && type != 'asset') {
        endpoint = [ this._client.URI, this._client.orgName, this._client.appName, type, self.get("uuid") ].join("/");     
    } else {        
        endpoint = [ this._client.URI, this._client.orgName, this._client.appName, "assets", self.get("uuid"), "data" ].join("/");        
    }
    xhr.open("GET", endpoint, true);
    xhr.responseType = "blob";
    xhr.onload = function(ev) {
        var blob = xhr.response;
        if(type != "assets" && type != 'asset') {
            doCallback(callback, [ null, blob, xhr ], self);
        } else {
            doCallback(callback, [ null, xhr, self ], self);
        }
    };
    xhr.onerror = function(err) {
        callback(true, err);
        doCallback(callback, [ new UsergridError(err), xhr, self ], self);
    };

    if(type != "assets" && type != 'asset') {            
        xhr.setRequestHeader("Accept", self._data["file-metadata"]["content-type"]);
    } else {        
        xhr.overrideMimeType(self.get("content-type"));
    }
    xhr.send();
};

/*
 *  Download Asset data
 *
 *  @method download
 *  @public
 *  @returns {callback} callback(err, blob) blob is a javascript Blob object.
 */
Usergrid.Asset.prototype.download = function(callback) {
    this.downloadAsset(function(err, response) {
        if(!err){
            doCallback(callback, [ null, response, self ], self);
        } else {
            doCallback(callback, [ new UsergridError(err), response, self ], self);
        }
    });
};