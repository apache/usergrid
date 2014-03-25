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
	var missingData = ["name", "owner", "path"].some(function(required) { return !(required in self._data)});
	if(missingData){
		return doCallback(callback, [true, new UsergridError("Invalid asset data: 'name', 'owner', and 'path' are required properties.")], self);
	}
	self.save(function(err, data) {
		if (err) {
			doCallback(callback, [true, new UsergridError(data)], self);
		} else {
			if (data && data.entities && data.entities.length){
				self.set(data.entities[0]);
			}
			doCallback(callback, [false, self], self);
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
			self.getAssets(function(err, data) {
				if (err) {
					doCallback(callback, [true, new UsergridError(data)], self);
				} else {
					doCallback(callback, [null, self], self);
				}
			});
		} else {
			doCallback(callback, [true, new UsergridError(data)], self)
		}
	})
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
					doCallback(callback, [err, new UsergridError(data)], self)
				} else {
					var endpoint = ["folders", self.get("uuid"), "assets", asset.get("uuid")].join('/');
					var options = {
						method: 'POST',
						endpoint: endpoint
					};
					self._client.request(options, callback);
				}
			})
		}
	} else {
		//nothing to add
		doCallback(callback, [true, {
			error_description: "No asset specified"
		}], self)
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
			}, callback);
		}
	} else {
		//nothing to add
		doCallback(callback, [true, {
			error_description: "No asset specified"
		}], self)
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
