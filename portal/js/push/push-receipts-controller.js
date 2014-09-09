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
 'use strict'
 
AppServices.Controllers.controller('PushReceiptsCtrl', ['ug', '$scope', '$rootScope', '$location',
  function (ug, $scope, $rootScope, $location) {

    $scope.receiptsCollection = {}
    $scope.previous_display = 'none';
    $scope.next_display = 'none';

    $scope.statusList = [
      {name:'All',value:''},
      {name:'Received',value:'RECEIVED'},
      {name:'Failed',value:'FAILED'}
    ]

    $scope.selectedStatus = $scope.statusList[0]

    if (!$rootScope.selectedNotification) {
      $location.path('/push/history');
    }

    ug.getNotificationReceipts($rootScope.selectedNotification.get('uuid'));

    $scope.$on('receipts-received', function(event, collection) {
      $scope.receiptsCollection = collection;
      $scope.checkNextPrev();
      if(!$scope.$$phase) {
        $scope.$apply();
      }
    });

    $scope.showHistory = function(type) {
      ug.getNotificationReceipts($rootScope.selectedNotification.get('uuid'));
    }

    $scope.showReceipts = function(option) {
      $scope.selectedStatus = option;
      ug.getNotificationReceipts($rootScope.selectedNotification.get('uuid'),option.value);
    }

    $scope.resetNextPrev = function() {
      $scope.previous_display = 'none';
      $scope.next_display = 'none';
    }
    $scope.checkNextPrev = function() {
      $scope.resetNextPrev();
      if ($scope.receiptsCollection.hasPreviousPage()) {
        $scope.previous_display = 'block';
      }
      if($scope.receiptsCollection.hasNextPage()) {
        $scope.next_display = 'block';
      }
    }

    $scope.getPrevious = function () {
      $scope.receiptsCollection.getPreviousPage(function(err) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error getting previous page');
        }
        $scope.checkNextPrev();
        if(!$scope.$$phase) {
          $scope.$apply();
        }
      });
    };

    $scope.getNext = function () {

      $scope.receiptsCollection.getNextPage(function(err) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error getting next page');
        }
        $scope.checkNextPrev();
        if(!$scope.$$phase) {
          $scope.$apply();
        }
      });
    };

  }]);