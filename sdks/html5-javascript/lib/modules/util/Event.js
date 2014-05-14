var UsergridEventable	= function(){
    throw Error("'UsergridEventable' is not intended to be invoked directly");
};
UsergridEventable.prototype	= {
    bind	: function(event, fn){
        this._events = this._events || {};
        this._events[event] = this._events[event]	|| [];
        this._events[event].push(fn);
    },
    unbind	: function(event, fn){
        this._events = this._events || {};
        if( event in this._events === false  )	return;
        this._events[event].splice(this._events[event].indexOf(fn), 1);
    },
    trigger	: function(event /* , args... */){
        this._events = this._events || {};
        if( event in this._events === false  )	return;
        for(var i = 0; i < this._events[event].length; i++){
            this._events[event][i].apply(this, Array.prototype.slice.call(arguments, 1));
        }
    }
};
UsergridEventable.mixin	= function(destObject){
    var props	= ['bind', 'unbind', 'trigger'];
    for(var i = 0; i < props.length; i ++){
        if(props[i] in destObject.prototype){
            console.warn("overwriting '"+props[i]+"' on '"+destObject.name+"'.");
            console.warn("the previous version can be found at '_"+props[i]+"' on '"+destObject.name+"'.");
            destObject.prototype['_'+props[i]]=destObject.prototype[props[i]];
        }
        destObject.prototype[props[i]]	= UsergridEventable.prototype[props[i]];
    }
};
