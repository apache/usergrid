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

AppServices.Controllers.controller('GettingStartedCtrl',
    ['ug',
      '$scope',
      '$rootScope',
      '$location',
      '$timeout',
      '$anchorScroll', function (ug, $scope, $rootScope, $location, $timeout, $anchorScroll) {


      $scope.collections = [];
      $scope.graph = '';


      $scope.clientID = '';
      $scope.clientSecret = '';
      var getKeys = function () {
        return ug.jsonpRaw('credentials', '', {});
      }

      $scope.regenerateCredentialsDialog = function (modalId) {
        $scope.orgAPICredentials = {client_id: 'regenerating...', client_secret: 'regenerating...'};
        ug.regenerateAppCredentials();
        $scope.hideModal(modalId);
      };


      $scope.$on('app-creds-updated', function (event, credentials) {
        if (credentials) {
          $scope.clientID = credentials.client_id;
          $scope.clientSecret = credentials.client_secret;
          if (!$scope.$$phase) {
            $scope.$apply();
          }
        } else {
          setTimeout(function () {
            ug.getAppCredentials();
          }, 5000)
        }
      });

      ug.getAppCredentials();


      $scope.contentTitle;

      $scope.showSDKDetail = function (name) {
        var introContainer = document.getElementById('intro-container');

        //if no value then let link click happen and reset height to remove content
        if (name === 'nocontent') {
          introContainer.style.height = '0';
          return true;
        }

        introContainer.style.opacity = .1;
        introContainer.style.height = '0';
        var timeout = 0;
        if ($scope.contentTitle) {
          timeout = 500;
        }
        $timeout(function () {
          introContainer.style.height = '1000px';
          introContainer.style.opacity = 1;
        }, timeout);
        $scope.optionName = name;
        $scope.contentTitle = name;

        $scope.sdkLink = 'http://apigee.com/docs/content/' + name + '-sdk-redirect';
        $scope.docsLink = 'http://apigee.com/docs/app-services/content/installing-apigee-sdk-' + name;

        $scope.getIncludeURL = function () {
          return 'app-overview/doc-includes/' + $scope.optionName + '.html';
        }
//      $location.path('http://mktg-dev.apigee.com/docs/content/ios-sdk-redirect');
      }


      $scope.scrollToElement = function (elem) {
        // set the location.hash to the id of
        // the element you wish to scroll to.
        $location.hash(elem);

        // call $anchorScroll()
        $anchorScroll();
        return false;
      }
    }]);