var COUNTER_RESOLUTIONS = {
  'ALL': 'all',
  'MINUTE': 'minute',
  'FIVE_MINUTES': 'five_minutes',
  'HALF_HOUR': 'half_hour',
  'HOUR': 'hour',
  'SIX_DAY': 'six_day',
  'DAY': 'day',
  'WEEK': 'week',
  'MONTH': 'month'
};
COUNTER_RESOLUTIONS.valueOf=function(str){
  Object.keys(COUNTER_RESOLUTIONS).forEach(function(res){
    if(COUNTER_RESOLUTIONS[res]===str){
      return COUNTER_RESOLUTIONS[res];
    }
  });
  return COUNTER_RESOLUTIONS.ALL;
};

/*
 *  A class to model a Usergrid event.
 *
 *  @constructor
 *  @param {object} options {timestamp:0, category:'value', counters:{name : value}}
 *  @returns {callback} callback(err, event)
 */
Usergrid.Event = function(options, callback) {
  var self=this;
  this._client = options.client;
  this._data = options.data || {};
  this._data.category = options.category||"UNKNOWN";
  this._data.timestamp = options.timestamp||0;
  this._data.type = "events";
  this._data.counters=options.counters||{};
  if(typeof(callback) === 'function') {
    callback.call(self, false, self);
  }
  //this.save(callback);
};

/*
 *  Inherit from Usergrid.Entity.
 *  Note: This only accounts for data on the group object itself.
 *  You need to use add and remove to manipulate group membership.
 */
Usergrid.Event.prototype = new Usergrid.Entity();

Usergrid.Event.prototype.fetch=function(callback){
  this.getData(null, null, null, null, callback);
}
/*
 * increments the counter for a specific event
 *
 * options object: {name: counter_name}
 *
 * @public
 * @method increment
 * @params {object} options
 * @param {function} callback
 * @returns {callback} callback(err, event)
 */
Usergrid.Event.prototype.increment=function(name, value, callback){
  var self=this;
  if(isNaN(value)){
    if(typeof(callback) === 'function') {
      return callback.call(self, true, "'value' for increment, decrement must be a number");
    }
  }
  self._data.counters[name]=parseInt(value);
  return self.save(callback);
};
/*
 * decrements the counter for a specific event
 *
 * options object: {name: counter_name}
 *
 * @public
 * @method decrement
 * @params {object} options
 * @param {function} callback
 * @returns {callback} callback(err, event)
 */

Usergrid.Event.prototype.decrement=function(name, value, callback){
  this.increment(name, -(value), callback);
};
/*
 * resets the counter for a specific event
 *
 * options object: {name: counter_name}
 *
 * @public
 * @method reset
 * @params {object} options
 * @param {function} callback
 * @returns {callback} callback(err, event)
 */

Usergrid.Event.prototype.reset=function(name, callback){
  this.increment(name, 0, callback);
};

Usergrid.Event.prototype.getData=function(start, end, resolution, counters, callback){
  var start_time, 
      end_time,
      res=COUNTER_RESOLUTIONS.valueOf(resolution);
  if(start){
    switch(typeof start){
      case "undefined":
        start_time=0;
        break;
      case "number":
        start_time=start;
        break;
      case "string":
        start_time=(isNaN(start))?Date.parse(start):parseInt(start);
        break;
      default:
        start_time=Date.parse(start.toString());
    }
  }
  if(end){
    switch(typeof end){
      case "undefined":
        end_time=Date.now();
        break;
      case "number":
        end_time=end;
        break;
      case "string":
        end_time=(isNaN(end))?Date.parse(end):parseInt(end);
        break;
      default:
        end_time=Date.parse(end.toString());
    }
  }
  var self=this;
  //https://api.usergrid.com/yourorgname/sandbox/counters?counter=test_counter
  if(counters===null || "undefined"===typeof counters)
  counters=Object.keys(this._data.counters);
  var params=Object.keys(counters).map(function(counter){
      return ["counter", encodeURIComponent(counters[counter])].join('=');
    });
  params.push('resolution='+res)
  params.push('start_time='+String(start_time))
  params.push('end_time='+String(end_time))
    
  var endpoint="counters?"+params.join('&');
  var options= {
    endpoint:endpoint
  };
  this._client.request(options, function(err, data){
    if(data.counters && data.counters.length){
      data.counters.forEach(function(counter){
        self._data.counters[counter.name]=counter.value||counter.values;
      })
    }
    if(typeof(callback) === 'function') {
      callback.call(self, err, data);
    }
  })
};
