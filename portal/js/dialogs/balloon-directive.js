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

AppServices.Directives.directive('balloon', ['$window','$timeout', function ($window,$timeout) {
  return{
    restrict: 'ECA',
    scope: '=',
    template: '' +
      '<div class="baloon {{direction}}" ng-transclude>' +
      '</div>',
    replace: true,
    transclude: true,
    link: function linkFn(scope, lElement, attrs) {
      scope.direction = attrs.direction;
      var runScroll = true;
      var windowEl = angular.element($window);
      windowEl.on('scroll', function() {
        if(runScroll){
          lElement.addClass('fade-out');
          $timeout(function(){lElement.addClass('hide')},1000)
          runScroll = false;
        }
      });
    }
  }
}])