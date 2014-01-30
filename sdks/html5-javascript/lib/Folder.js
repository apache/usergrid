/*
 *  A class to model a Usergrid folder.
 *
 *  @constructor
 *  @param {object} options {name:"MyPhotos", path:"/user/uploads", owner:"00000000-0000-0000-0000-000000000000" } 
 *  @returns {callback} callback(err, folder)
 */
Usergrid.Folder = function(options, callback) {
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
Usergrid.Folder.prototype = new Usergrid.Entity();
