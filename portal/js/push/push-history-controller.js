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
AppServices.Controllers.controller('PushHistoryCtrl', ['ug', '$scope', '$rootScope', '$location',
  function (ug, $scope, $rootScope, $location) {

    $scope.notificationCollection = {};
    $scope.previous_display = 'none';
    $scope.next_display = 'none';

    $scope.historyList = [
      {name:'All',value:''},
      {name:'Scheduled',value:'SCHEDULED'},
      {name:'Sending',value:'STARTED'},
      {name:'Sent',value:'FINISHED'},
      {name:'Failed',value:'FAILED'},
      {name:'Cancelled',value:'CANCELED'}
    ];

    var stateImages = {
      'FAILED': 'img/red_dot.png',
      'FINISHED':'img/green_dot.png',
      'STARTED':'img/green_dot.png',
      'CANCELED':'img/red_dot.png',
      'SCHEDULED':'img/green_dot.png',
      'FINISHED_ERRORS':'img/yellow_dot.png'
    };

    $scope.getStateImage = function(notification){
      var data = notification._data;
      var image = stateImages[data.state] || stateImages.STARTED;
      image = ( data.statistics && data.statistics.errors > 0) && image !== 'img/red_dot.png'
        ?  'img/yellow_dot.png'
        : image;
      return image;
    };
    $scope.getStateMessage = function(notification){
      var data = notification._data;
      var state = data.state === 'FINISHED' &&   ( data.statistics && data.statistics.errors > 0)
        ?  'FINISHED (WITH ERRORS)'
        : data.state;
      return state;
    };

    $scope.selectedHistory = $scope.historyList[0];

    ug.getNotificationHistory();

    $scope.$watch('currentApp',function(){
      ug.getNotificationHistory();
    });

    $scope.$on('notifications-received', function(event, collection) {
      $scope.notificationCollection = collection;
      $scope.checkNextPrev();
      if(!$scope.$$phase) {
        $scope.$apply();
      }
    });

    $scope.showHistory = function(option) {
      $scope.selectedHistory = option;
      $scope.notificationCollection = [];
      ug.getNotificationHistory(option.value);
    }

    $scope.viewReceipts = function(uuid){
      $rootScope.selectedNotification = $scope.notificationCollection.getEntityByUUID(uuid);
      $location.path('/push/history/receipts');
    }

    $scope.resetNextPrev = function() {
      $scope.previous_display = 'none';
      $scope.next_display = 'none';
    }

    $scope.checkNextPrev = function() {
      $scope.resetNextPrev();
      if ($scope.notificationCollection.hasPreviousPage()) {
        $scope.previous_display = 'block';
      }
      if($scope.notificationCollection.hasNextPage()) {
        $scope.next_display = 'block';
      }
    }

    $scope.getPrevious = function () {
      $scope.notificationCollection.getPreviousPage(function(err) {
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

      $scope.notificationCollection.getNextPage(function(err) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error getting next page');
        }
        $scope.checkNextPrev();
        if(!$scope.$$phase) {
          $scope.$apply();
        }
      });
    };

    $scope.getNotificationStartedDate = function(notification){
      return notification.started || notification.created;
    }

  }]);
