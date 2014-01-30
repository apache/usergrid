/*
 *  A class to model a Usergrid asset.
 *
 *  @constructor
 *  @param {object} options {name:"photo.jpg", path:"/user/uploads", "content-type":"image/jpeg", owner:"F01DE600-0000-0000-0000-000000000000" } 
 *  @returns {callback} callback(err, asset)
 */
Usergrid.Asset = function(options, callback) {
	var self=this, okToSave=true, messages=[];
	self._client = options.client;
	self._data = options.data || {};
	["name","owner","path"].forEach(function(required){
		if(!(required in this._data)){
			messages.push(required+" is a required data element.");
			okToSave=false;
		}
	});
	if(okToSave){
		self.save(callback);
	}else{
		if(callback && typeof(callback) === 'function') {
			callback.call(self, !okToSave, {error_description:messages.join("\n")});
		}
	}
};
/*
 *  Inherit from Usergrid.Entity.
 */
Usergrid.Asset.prototype = new Usergrid.Entity();

/*
 *  Add an asset to a folder.
 *
 *  @constructor
 *  @param {object} options {folder:"F01DE600-0000-0000-0000-000000000000"} 
 *  @returns {callback} callback(err, asset)
 */
Usergrid.Asset.prototype.addToFolder=function(options, callback){
	var self=this, error=null;
	if(('folder' in options) && isUUID (options.folder)){
		//we got a valid UUID
		var folder = Usergrid.Folder({uuid:options.folder}, function(err, folder){
			if(err){
				return callback.call(self, err, folder);
			}
			var endpoint=["folders", folder.get("uuid"), "assets", self.get("uuid")].join('/');
		});

	}
	if(callback && typeof(callback) === 'function') {
		callback.call(self, false, self);
	}
};

Usergrid.Asset.prototype.addToFolder=function(options, callback){};

