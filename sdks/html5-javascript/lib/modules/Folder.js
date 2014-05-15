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
 *  A class to model a Usergrid folder.
 *
 *  @constructor
 *  @param {object} options {name:"MyPhotos", path:"/user/uploads", owner:"00000000-0000-0000-0000-000000000000" }
 *  @returns {callback} callback(err, folder)
 */
Usergrid.Folder = function(options, callback) {
	var self = this,
		messages = [];
	console.log("FOLDER OPTIONS", options);
	self._client = options.client;
	self._data = options.data || {};
	self._data.type = "folders";
	var missingData = ["name", "owner", "path"].some(function(required) {
		return !(required in self._data);
	});
	if (missingData) {
		return doCallback(callback, [new UsergridInvalidArgumentError("Invalid asset data: 'name', 'owner', and 'path' are required properties."), null, self], self);
	}
	self.save(function(err, response) {
		if (err) {
			doCallback(callback, [new UsergridError(response), response, self], self);
		} else {
			if (response && response.entities && response.entities.length) {
				self.set(response.entities[0]);
			}
			doCallback(callback, [null, response, self], self);
		}
	});
};
/*
 *  Inherit from Usergrid.Entity.
 */
Usergrid.Folder.prototype = new Usergrid.Entity();


/*
 *  fetch the folder and associated assets
 *
 *  @method fetch
 *  @public
 *  @param {function} callback(err, self)
 *  @returns {callback} callback(err, self)
 */
Usergrid.Folder.prototype.fetch = function(callback) {
	var self = this;
	Usergrid.Entity.prototype.fetch.call(self, function(err, data) {
		console.log("self", self.get());
		console.log("data", data);
		if (!err) {
			self.getAssets(function(err, response) {
				if (err) {
					doCallback(callback, [new UsergridError(response), resonse, self], self);
				} else {
					doCallback(callback, [null, self], self);
				}
			});
		} else {
			doCallback(callback, [null, data, self], self);
		}
	});
};
/*
 *  Add an asset to the folder.
 *
 *  @method addAsset
 *  @public
 *  @param {object} options {asset:(uuid || Usergrid.Asset || {name:"photo.jpg", path:"/user/uploads", "content-type":"image/jpeg", owner:"F01DE600-0000-0000-0000-000000000000" }) }
 *  @returns {callback} callback(err, folder)
 */
Usergrid.Folder.prototype.addAsset = function(options, callback) {
	var self = this;
	if (('asset' in options)) {
		var asset = null;
		switch (typeof options.asset) {
			case 'object':
				asset = options.asset;
				if (!(asset instanceof Usergrid.Entity)) {
					asset = new Usergrid.Asset(asset);
				}
				break;
			case 'string':
				if (isUUID(options.asset)) {
					asset = new Usergrid.Asset({
						client: self._client,
						data: {
							uuid: options.asset,
							type: "assets"
						}
					});
				}
				break;
		}
		if (asset && asset instanceof Usergrid.Entity) {
			asset.fetch(function(err, data) {
				if (err) {
					doCallback(callback, [new UsergridError(data), data, self], self);
				} else {
					var endpoint = ["folders", self.get("uuid"), "assets", asset.get("uuid")].join('/');
					var options = {
						method: 'POST',
						endpoint: endpoint
					};
					self._client.request(options, callback);
				}
			});
		}
	} else {
		//nothing to add
		doCallback(callback, [new UsergridInvalidArgumentError("No asset specified"), null, self], self);
	}
};

/*
 *  Remove an asset from the folder.
 *
 *  @method removeAsset
 *  @public
 *  @param {object} options {asset:(uuid || Usergrid.Asset || {name:"photo.jpg", path:"/user/uploads", "content-type":"image/jpeg", owner:"F01DE600-0000-0000-0000-000000000000" }) }
 *  @returns {callback} callback(err, folder)
 */
Usergrid.Folder.prototype.removeAsset = function(options, callback) {
	var self = this;
	if (('asset' in options)) {
		var asset = null;
		switch (typeof options.asset) {
			case 'object':
				asset = options.asset;
				break;
			case 'string':
				if (isUUID(options.asset)) {
					asset = new Usergrid.Asset({
						client: self._client,
						data: {
							uuid: options.asset,
							type: "assets"
						}
					});
				}
				break;
		}
		if (asset && asset !== null) {
			var endpoint = ["folders", self.get("uuid"), "assets", asset.get("uuid")].join('/');
			self._client.request({
				method: 'DELETE',
				endpoint: endpoint
			}, function(err, response) {
				if (err) {
					doCallback(callback, [new UsergridError(response), response, self], self);
				} else {
					doCallback(callback, [null, response, self], self);
				}
			});
		}
	} else {
		//nothing to add
		doCallback(callback, [new UsergridInvalidArgumentError("No asset specified"), null, self], self);
	}
};

/*
 *  List the assets in the folder.
 *
 *  @method getAssets
 *  @public
 *  @returns {callback} callback(err, assets)
 */
Usergrid.Folder.prototype.getAssets = function(callback) {
	return this.getConnections("assets", callback);
};
