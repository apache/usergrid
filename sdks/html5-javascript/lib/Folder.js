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
	["name", "owner", "path"].forEach(function(required) {
		if (!(required in self._data)) {
			messages.push(required + " is a required data element.");
			throw required + " is a required data element.";
		}
	});
	if (messages.length) {
		return doCallback(callback, [true, messages.join("\n")], self)
	}
	var errors = ['service_resource_not_found', 'no_name_specified', 'null_pointer'];
	self.save(function(err, data) {
		if (err) {
			doCallback(callback, [true, data], self);
		} else {
			console.log(data.entities[0]);
			self.set(data.entities[0]);
			doCallback(callback, [false, self], self);
		}
	});
};
/*
 *  Inherit from Usergrid.Entity.
 */
Usergrid.Folder.prototype = new Usergrid.Entity();


/*
 *  fetch the folder.
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
					doCallback(callback, [true, new Usergrid.Error(data)], self);
				} else {
					doCallback(callback, [null, self], self);
				}
			});
		} else {
			doCallback(callback, [true, new Usergrid.Error(data)], self)
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
					doCallback(callback, [err, new Usergrid.Error(data)], self)
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
Usergrid.Folder.prototype.getAssets = function(callback) {
	return this.getConnections("assets", callback);
};

//Usergrid.Folder.prototype.getAssets = function(options, callback){};
