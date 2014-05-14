/**
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */
'use strict';
AppServices.Services.factory('utility', function (configuration, $q, $http, $resource) {

  return {

    keys: function(o) {
      var a = [];
      for (var propertyName in o) {
        a.push(propertyName);
      }
      return a;
    },
    get_gravatar: function(email, size) {
      try {
        var size = size || 50;
        if (email.length) {
          return 'https://secure.gravatar.com/avatar/' + MD5(email) + '?s=' + size ;
        } else {
          return 'https://apigee.com/usergrid/img/user_profile.png';
        }
      } catch(e) {
        return 'https://apigee.com/usergrid/img/user_profile.png';
      }
    },
    get_qs_params: function() {
      var queryParams = {};
      if (window.location.search) {
        // split up the query string and store in an associative array
        var params = window.location.search.slice(1).split("&");
        for (var i = 0; i < params.length; i++) {
          var tmp = params[i].split("=");
          queryParams[tmp[0]] = unescape(tmp[1]);
        }
      }
      return queryParams;
    },

    safeApply: function(fn) {
      var phase = this.$root.$$phase;
      if(phase == '$apply' || phase == '$digest') {
        if(fn && (typeof(fn) === 'function')) {
          fn();
        }
      } else {
        this.$apply(fn);
      }
    }
  };

})



