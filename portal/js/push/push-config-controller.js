
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

AppServices.Controllers.controller('PushConfigCtrl', ['ug', '$scope', '$rootScope', '$routeParams', '$location', function (ug, $scope, $rootScope, $routeParams, $location) {

  $scope.notifier = {};
  $scope.notifier.appleNotifierCert = [];
  $scope.notifier.appleNotifierName = '';
  $scope.notifier.appleEnvironment = '';
  $scope.notifier.notifierCertPassword = '';
  $scope.notifier.androidNotifierName = '';
  $scope.notifier.androidNotifierAPIKey = '';
    $scope.notifier.winNotifierLogging = true;

  $scope.notifiersCollection = {};

  ug.getNotifiers();
  $scope.$on('app-changed',function(){
    ug.getNotifiers();
  });

  $scope.deleteNotifiersDialog = function(modalId){
    $scope.deleteEntities($scope.notifiersCollection, 'notifier-deleted', 'error deleting notifier');
    $scope.hideModal(modalId)
  };
  $scope.$on('notifier-deleted', function(event, collection) {
    $rootScope.$broadcast('alert', 'success', 'Notifier deleted successfully.');
  });

  $scope.$on('notifiers-received', function(event, collection) {
    $scope.notifiersCollection = collection;
    $scope.queryBoxesSelected = false;
    $scope.applyScope();
  });

  $scope.createAppleNotifier = function() {
    //$scope.appleNotifierCert - this comes from the directive below
    ug.createAppleNotifier($scope.appleNotifierCert, $scope.notifier.appleNotifierName, $scope.notifier.appleEnvironment, $scope.notifier.appleCertPassword);
    $scope.notifier = {};
    $scope.clearNotificationFile();
    //angular.element("#ios-cert")[0].value = ""; // this is bad
  };

  $scope.createAndroidNotifier = function() {
    ug.createAndroidNotifier($scope.notifier.androidNotifierName, $scope.notifier.androidNotifierAPIKey);
    $scope.notifier = {};
  };

    $scope.createWinNotifier = function(){
        $scope.notifier.winNotifierLogging =  $scope.notifier.winNotifierLogging || false;
        ug.createWinNotifier($scope.notifier.winNotifierName,$scope.notifier.winNotifierSid, $scope.notifier.winNotifierAPISecret, $scope.notifier.winNotifierLogging);
        $scope.notifier = {};
    };

  $scope.$on('notifier-update', function(event) {
    ug.getNotifiers();
  });

}]);

AppServices.Controllers.directive('file', function(){
  return {
    scope: {
      file: '='
    },
    link: function(scope, el, attrs){
      el.bind('change', function(event){
        var files = event.target.files;
        scope.$parent.$parent.$parent.$parent.appleNotifierCert = files[0];
        scope.$parent.$parent.$parent.$parent.$apply();
        scope.$parent.$parent.$parent.$parent.clearNotificationFile = function(newVal,oldVal){
          event.target.value="";
          scope.$parent.$parent.$parent.$parent.appleNotifierCert = "";
        };

      });
    }
  };
});
