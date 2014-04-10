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
AppServices.Directives.directive('pageTitle', ['$rootScope','ug', function ($rootScope,ug) {
  return{
    restrict: 'E',
    transclude: true,
    templateUrl:'global/page-title.html',
    link: function linkFn(scope, lElement, attrs) {
      scope.title = attrs.title;
      scope.icon = attrs.icon;
      scope.showHelp = function () {
        $('#need-help').modal('show');
      };
      scope.sendHelp = function () {
        data.jsonp_raw('apigeeuihelpemail', '', {useremail: $rootScope.userEmail}).then(
          function () {
            $rootScope.$broadcast('alert', 'success', 'Email sent. Our team will be in touch with you shortly.');
          },
          function () {
            $rootScope.$broadcast('alert', 'error', 'Problem Sending Email. Try sending an email to mobile@apigee.com.');
          }
        );
        $('#need-help').modal('hide');
      };
    }
  }
}]);
