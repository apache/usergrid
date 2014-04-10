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
AppServices.Directives.directive('insecureBanner', ['$rootScope','ug', function ($rootScope,ug) {
  return{
    restrict: 'E',
    transclude: true,
    templateUrl:'global/insecure-banner.html',
    link: function linkFn(scope, lElement, attrs) {
      scope.securityWarning = false;

      scope.$on('roles-received',function(evt,roles){
        scope.securityWarning = false;
        if(!roles || !roles._list)
          return;
        roles._list.forEach(function(roleHolder){
          var role = roleHolder._data;
          if(role.name.toUpperCase()==='GUEST'){
            roleHolder.getPermissions(function(err, data){
              if (!err) {
                if(roleHolder.permissions){
                  roleHolder.permissions.forEach(function(permission){
                    if(permission.path.indexOf('/**')>=0){
                      scope.securityWarning = true;
                      scope.applyScope();
                    }
                  });
                }
              }
            });

          }
        });
      });

      var initialized = false;
      scope.$on('app-initialized',function(){
        !initialized && ug.getRoles();
        initialized=true;
      });

      scope.$on('app-changed',function(){
        scope.securityWarning = false;
        ug.getRoles();
      });
    }
  }
}]);