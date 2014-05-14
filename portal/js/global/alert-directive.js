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

AppServices.Directives.directive('alerti', ["$rootScope","$timeout", function ($rootScope,$timeout) {
  return{
    restrict: 'ECA',
    scope: {
      type: '=type',
      closeable: '@closeable',
      index: '&index'
    },
    template: '<div class="alert" ng-class="type && \'alert-\' + type">' +
              '    <button ng-show="closeable" type="button" class="close" ng-click="closeAlert(index)">&times;</button>' +
              '    <i ng-if="type === \'warning\'" class="pictogram pull-left" style="font-size:3em;line-height:0.4">&#128165;</i>' +
      '    <i ng-if="type === \'info\'" class="pictogram pull-left">&#8505;</i>' +
      '    <i ng-if="type === \'error\'" class="pictogram pull-left">&#9889;</i>' +
      '    <i ng-if="type === \'success\'" class="pictogram pull-left">&#128077;</i>' +
      '<div ng-transclude></div>' +
              '</div>',
    replace: true,
    transclude: true,
    link: function linkFn(scope, lElement, attrs) {

//      if($rootScope.alertClick){

      $timeout(function(){lElement.addClass('fade-out')},4000)

      lElement.click(function(){

        if(attrs.index){
          scope.$parent.closeAlert(attrs.index);
        }

      });

//      }
      //need to do a 10ms timeout to apply animation :(
      setTimeout(function(){lElement.addClass('alert-animate');},10)
    }
  }
}])