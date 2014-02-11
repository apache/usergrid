function UsergridStorable(storage, keyPrefix){
    //throw Error("'UsergridStorable' is not intended to be invoked directly")
}
UsergridStorable.prototype.setStorage = function(storage) {
    if(!(storage instanceof Storage)){
        console.warn("parameter passed to 'setStorage' is not a browser Storage interface. using sessionStorage");
        storage=sessionStorage;
    }
    return this._storage=storage;
};
UsergridStorable.prototype.getStorage = function() {
    if(!(this._storage instanceof Storage)){
        console.warn("parameter passed to 'setStorage' is not a browser Storage interface. using sessionStorage");
        this._storage=sessionStorage;
    }
    return this._storage;
};
UsergridStorable.prototype.getStorageKey = function(key) {
    if(!this._keyPrefix)this._keyPrefix='usergrid_js_sdk_';
    return this._keyPrefix+key;
};
UsergridStorable.prototype.get=function(key){
    var value;
    if(arguments.length>1){
        key=[].slice.call(arguments).reduce(function(p,c,i,a){
            if(c instanceof Array){
                p= p.concat(c);
            }else{
                p.push(c);
            }
            return p;
        },[]);
    }
    if(this._storage instanceof Storage){
        if(key instanceof Array){
            var self=this;
            value=key.map(function(k){return self.get(k)});
        }else if("undefined" !== typeof key){
            this[key]=JSON.parse(this.getStorage().getItem(this.getStorageKey(key)));
            value=this[key];
        }
    }
    return value;
}
UsergridStorable.prototype.set=function(key, value){
    if(this._storage instanceof Storage){
        if(value){
            this[key]=JSON.stringify(value);
            this.getStorage().setItem(this.getStorageKey(key),this[key]);
        }else{
            delete this[key];
            this.getStorage().removeItem(this.getStorageKey(key));
        }
    }
}
/*
 *  A public method to store the OAuth token for later use - uses localstorage if available
 *
 *  @method setToken
 *  @public
 *  @params {string} token
 *  @return none
 */
UsergridStorable.prototype.setToken = function (token) {
    return this.set('token', token);
};

/*
 *  A public method to get the OAuth token
 *
 *  @method getToken
 *  @public
 *  @return {string} token
 */
UsergridStorable.prototype.getToken = function () {
    return this.get('token');
};
UsergridStorable.mixin	= function(destObject){
    var props	= ['getStorage', 'setStorage', 'getStorageKey', 'get', 'set', 'getToken', 'setToken'];
    for(var i = 0; i < props.length; i ++){
        if(props[i] in destObject.prototype){
            console.warn("overwriting '"+props[i]+"' on '"+destObject.name+"'.");
            console.warn("the previous version can be found at '_"+props[i]+"' on '"+destObject.name+"'.");
            destObject.prototype['_'+props[i]]=destObject.prototype[props[i]];
        }
        destObject.prototype[props[i]]	= UsergridStorable.prototype[props[i]];
    }
}
