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
 *  A class to model a Usergrid event.
 *
 *  @constructor
 *  @param {object} options {timestamp:0, category:'value', counters:{name : value}}
 *  @returns {callback} callback(err, event)
 */
Usergrid.Counter = function(options) {
  // var self=this;
  this._client = options.client;
  this._data = options.data || {};
  this._data.category = options.category || "UNKNOWN";
  this._data.timestamp = options.timestamp || 0;
  this._data.type = "events";
  this._data.counters = options.counters || {};
  // doCallback(callback, [false, this], this);
  //this.save(callback);
};
var COUNTER_RESOLUTIONS = [
  'all', 'minute', 'five_minutes', 'half_hour',
  'hour', 'six_day', 'day', 'week', 'month'
];
/*
 *  Inherit from Usergrid.Entity.
 *  Note: This only accounts for data on the group object itself.
 *  You need to use add and remove to manipulate group membership.
 */
Usergrid.Counter.prototype = new Usergrid.Entity();

/*
 * overrides Entity.prototype.fetch. Returns all data for counters
 * associated with the object as specified in the constructor
 *
 * @public
 * @method increment
 * @param {function} callback
 * @returns {callback} callback(err, event)
 */
Usergrid.Counter.prototype.fetch = function(callback) {
  this.getData({}, callback);
};
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
Usergrid.Counter.prototype.increment = function(options, callback) {
  var self = this,
    name = options.name,
    value = options.value;
  if (!name) {
    return doCallback(callback, [new UsergridInvalidArgumentError("'name' for increment, decrement must be a number"), null, self], self);
  } else if (isNaN(value)) {
    return doCallback(callback, [new UsergridInvalidArgumentError("'value' for increment, decrement must be a number"), null, self], self);
  } else {
    self._data.counters[name] = (parseInt(value)) || 1;
    return self.save(callback);
  }
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

Usergrid.Counter.prototype.decrement = function(options, callback) {
  var self = this,
    name = options.name,
    value = options.value;
  self.increment({
    name: name,
    value: -((parseInt(value)) || 1)
  }, callback);
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

Usergrid.Counter.prototype.reset = function(options, callback) {
  var self = this,
    name = options.name;
  self.increment({
    name: name,
    value: 0
  }, callback);
};

/*
 * gets data for one or more counters over a given
 * time period at a specified resolution
 *
 * options object: {
 *                   counters: ['counter1', 'counter2', ...],
 *                   start: epoch timestamp or ISO date string,
 *                   end: epoch timestamp or ISO date string,
 *                   resolution: one of ('all', 'minute', 'five_minutes', 'half_hour', 'hour', 'six_day', 'day', 'week', or 'month')
 *                   }
 *
 * @public
 * @method getData
 * @params {object} options
 * @param {function} callback
 * @returns {callback} callback(err, event)
 */
Usergrid.Counter.prototype.getData = function(options, callback) {
  var start_time,
    end_time,
    start = options.start || 0,
    end = options.end || Date.now(),
    resolution = (options.resolution || 'all').toLowerCase(),
    counters = options.counters || Object.keys(this._data.counters),
    res = (resolution || 'all').toLowerCase();
  if (COUNTER_RESOLUTIONS.indexOf(res) === -1) {
    res = 'all';
  }
  start_time = getSafeTime(start);
  end_time = getSafeTime(end);
  var self = this;
  //https://api.usergrid.com/yourorgname/sandbox/counters?counter=test_counter
  var params = Object.keys(counters).map(function(counter) {
    return ["counter", encodeURIComponent(counters[counter])].join('=');
  });
  params.push('resolution=' + res);
  params.push('start_time=' + String(start_time));
  params.push('end_time=' + String(end_time));

  var endpoint = "counters?" + params.join('&');
  this._client.request({
    endpoint: endpoint
  }, function(err, data) {
    if (data.counters && data.counters.length) {
      data.counters.forEach(function(counter) {
        self._data.counters[counter.name] = counter.value || counter.values;
      });
    }
    return doCallback(callback, [err, data, self], self);
  });
};

function getSafeTime(prop) {
  var time;
  switch (typeof prop) {
    case "undefined":
      time = Date.now();
      break;
    case "number":
      time = prop;
      break;
    case "string":
      time = (isNaN(prop)) ? Date.parse(prop) : parseInt(prop);
      break;
    default:
      time = Date.parse(prop.toString());
  }
  return time;
}
