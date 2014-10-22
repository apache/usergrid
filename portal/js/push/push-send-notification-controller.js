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

AppServices.Controllers.controller('PushSendNotificationCtrl', ['ug', '$scope',
  '$rootScope', '$location',
  function(ug, $scope, $rootScope, $location) {

    $scope.send = {};
    $scope.send.selectedNotifier = {};
    $scope.send.controlGroup = 'all';
    $scope.send.deliveryPeriod = {};
    $scope.controlGroup = 'all';
    $scope.notifiersCollection = {};

    ug.getNotifiers();



    $scope.$on('notifiers-received', function(event, collection) {
      $scope.notifiersCollection = collection._list;
      if (!$scope.$$phase) {
        $scope.$apply();
      }
    });


    $scope.selectDevices = function() {


    }

    $scope.scheduleNotification = function() {

      if ($scope.send.$valid) {

        var optionList = '';
        var type = $scope.send.controlGroup;
        var payload = {
          payloads: {},
          deliver: null,
          debug: true
        };
        payload.payloads[$scope.send.selectedNotifier._data.name] =
          $scope.send.notifierMessage;


        if (type !== 'all') {
          //get whatever is selected in the radio button options
          optionList = $scope.send[type]
          angular.forEach(optionList, function(value, index) {
            var path = type + '/' + value + '/notifications';
            ug.sendNotification(path, payload);
          });
        } else {
          ug.sendNotification('devices;ql=/notifications', payload);
        }

        $rootScope.$broadcast('alert', 'success',
          'Notifications have been queued.');

      }


    }

    //    todo - this is copied over from old portal for calendar component

    $('#notification-schedule-time-date').datepicker();
    $('#notification-schedule-time-date').datepicker('setDate', Date.last()
      .sunday());
    $('#notification-schedule-time-time').val("12:00 AM");
    $('#notification-schedule-time-time').timepicker({
      showPeriod: true,
      showLeadingZero: false
    });

    function pad(number, length) {
      var str = "" + number
      while (str.length < length) {
        str = '0' + str
      }
      return str
    }

    var offset = new Date().getTimezoneOffset();
    offset = ((offset < 0 ? '+' : '-') + pad(parseInt(Math.abs(offset / 60)),
      2) + pad(Math.abs(offset % 60), 2));

    $('#gmt_display').html('GMT ' + offset);

  }
]);
