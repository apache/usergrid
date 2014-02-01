if (!XMLHttpRequest.prototype.sendAsBinary) {
	XMLHttpRequest.prototype.sendAsBinary = function(sData) {
		var nBytes = sData.length,
			ui8Data = new Uint8Array(nBytes);
		for (var nIdx = 0; nIdx < nBytes; nIdx++) {
			ui8Data[nIdx] = sData.charCodeAt(nIdx) & 0xff;
		}
		/* send as ArrayBufferView...: */
		this.send(ui8Data);
		/* ...or as ArrayBuffer (legacy)...: this.send(ui8Data.buffer); */
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
		okToSave = true,
		messages = [];
	self._client = options.client;
	self._data = options.data || {};
	self._data.type = "assets";
	["name", "owner", "path"].forEach(function(required) {
		if (!(required in self._data)) {
			messages.push(required + " is a required data element.");
			okToSave = false;
		}
	});
	if (okToSave) {
		self.save(callback);
	} else {
		doCallback(callback, [!okToSave, new Usergrid.Error(messages.join("\n"))], self);
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
				return callback.call(self, err, folder);
			}
			var endpoint = ["folders", folder.get("uuid"), "assets", self.get("uuid")].join('/');
			var options = {
				method: 'POST',
				endpoint: endpoint
			};
			this._client.request(options, function(err, data) {
				if (typeof(callback) === 'function') {
					doCallback(callback, [!okToSave, new Usergrid.Error(data)], self);
					callback(err, data);
				}
			});
		});
	} else {
		if (callback && typeof(callback) === 'function') {
			callback.call(self, true, {
				error_description: "folder not specified"
			});
		}
	}
};

Usergrid.Asset.prototype.upload = function(data, callback) {
	if (!(window.File && window.FileReader && window.FileList && window.Blob)) {
		return doCallback(callback, [true, new Usergrid.Error('The File APIs are not fully supported by your browser.')], self);
	}
	var self = this;
	var endpoint = [this._client.URI, this._client.orgName, this._client.appName, "assets", self.get("uuid"), 'data'].join('/'); //self._client.buildAssetURL(self.get("uuid"));

	var xhr = new XMLHttpRequest();
	xhr.open("POST", endpoint, true);
	xhr.onerror = function(err) {
		//callback(true, err);
		doCallback(callback, [true, new Usergrid.Error('The File APIs are not fully supported by your browser.')], self)
	};
	xhr.onload = function(ev) {
		if (xhr.status >= 300) {
			doCallback(callback, [null, JSON.parse(xhr.responseText)], self)
		} else {
			doCallback(callback, [null, self], self)
		}
	};
	var fr = new FileReader();
	fr.onload = function() {
		var binary = fr.result;
		xhr.overrideMimeType('application/octet-stream');
		setTimeout(function() {
			xhr.sendAsBinary(binary);
		}, 1000);
	};
	fr.readAsBinaryString(data);
}
Usergrid.Asset.prototype.download = function(callback) {
	var self = this;
	var endpoint = [this._client.URI, this._client.orgName, this._client.appName, "assets", self.get("uuid"), 'data'].join('/');
	var xhr = new XMLHttpRequest();
	xhr.open("GET", endpoint, true);
	xhr.responseType = "blob";
	xhr.onload = function(ev) {
		var blob = xhr.response;
		//callback(null, blob);
		doCallback(callback, [false, blob], self)
	};
	xhr.onerror = function(err) {
		callback(true, err);
		doCallback(callback, [true, new Usergrid.Error(err)], self)
	};

	xhr.send();
}
