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

AppServices.Controllers.controller('AlertCtrl', ['$scope', '$rootScope', '$timeout',
  function ($scope, $rootScope, $timeout) {

    $scope.alertDisplay = 'none';
    $scope.alerts = [];

    //alert types error,success,warning,info

    $scope.$on('alert', function(event, type, message, permanent) {
      $scope.addAlert(type, message, permanent);
    });

    $scope.$on('clear-alerts', function(event, message) {
      $scope.alerts = [];
    });

    $scope.addAlert = function(type, message, permanent) {
      $scope.alertDisplay = 'block';
      $scope.alerts.push({type:type, msg:message});
      $scope.applyScope();
      if(!permanent){
        $timeout(function(){$scope.alerts.shift()},5000);
      }
    };

    $scope.closeAlert = function(index) {
      $scope.alerts.splice(index, 1);
    };


  }]);