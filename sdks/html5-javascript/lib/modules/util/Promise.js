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
 *
 *  @author ryan bridges (rbridges@apigee.com)
 */

//Promise
(function(global) {
    var name = 'Promise', overwrittenName = global[name], exports;

        function Promise() {
            this.complete = false;
            this.error = null;
            this.result = null;
            this.callbacks = [];
        }
        Promise.prototype.then = function(callback, context) {
            var f = function() {
                return callback.apply(context, arguments);
            };
            if (this.complete) {
                f(this.error, this.result);
            } else {
                this.callbacks.push(f);
            }
        };
        Promise.prototype.done = function(error, result) {
            this.complete = true;
            this.error = error;
            this.result = result;
            if(this.callbacks){
                for (var i = 0; i < this.callbacks.length; i++) this.callbacks[i](error, result);
                this.callbacks.length = 0;
            }
        };
        Promise.join = function(promises) {
            var p = new Promise(),
                total = promises.length,
                completed = 0,
                errors = [],
                results = [];

            function notifier(i) {
                return function(error, result) {
                    completed += 1;
                    errors[i] = error;
                    results[i] = result;
                    if (completed === total) {
                        p.done(errors, results);
                    }
                };
            }
            for (var i = 0; i < total; i++) {
                promises[i]().then(notifier(i));
            }
            return p;
        };
        Promise.chain = function(promises, error, result) {
            var p = new Promise();
            if (promises===null||promises.length === 0) {
                p.done(error, result);
            } else {
                promises[0](error, result).then(function(res, err) {
                    promises.splice(0, 1);
                    //self.logger.info(promises.length)
                    if(promises){
                        Promise.chain(promises, res, err).then(function(r, e) {
                            p.done(r, e);
                        });
                    }else{
                        p.done(res, err);
                    }
                });
            }
            return p;
        };

    global[name] =  Promise;
    global[name].noConflict = function() {
        if(overwrittenName){
            global[name] = overwrittenName;
        }
        return Promise;
    };
    return global[name];
}(this));
