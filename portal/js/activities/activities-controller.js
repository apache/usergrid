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
AppServices.Controllers.controller('ActivitiesCtrl', ['ug', '$scope', '$rootScope', '$location','$route',
  function (ug, $scope, $rootScope, $location, $route) {
    $scope.$on('app-activities-received',function(evt,data){
      $scope.activities = data;
      $scope.$apply();
    });
    $scope.$on('app-activities-error',function(evt,data){
      $rootScope.$broadcast('alert', 'error', 'Application failed to retreive activities data.');
    });
    ug.getActivities();
  }]);