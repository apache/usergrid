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

AppServices.Directives.directive('bsmodal', ["$rootScope", function ($rootScope) {
  return{
    restrict: 'ECA',
    scope: {
      title: '@title',
      buttonid:'=buttonid',
      footertext: '=footertext',
      closelabel: '=closelabel'
    },
    transclude: true,
    templateUrl: 'dialogs/modal.html',
    replace: true,
    link: function linkFn(scope, lElement, attrs, parentCtrl) {
      scope.title = attrs.title;
      scope.footertext = attrs.footertext;
      scope.closelabel = attrs.closelabel;
      scope.close = attrs.close;
      scope.extrabutton = attrs.extrabutton;
      scope.extrabuttonlabel = attrs.extrabuttonlabel;
      scope.buttonId = attrs.buttonid;


      scope.closeDelegate = function(attr){
        //always call method in parent controller scope
        scope.$parent[attr](attrs.id,scope)
      }

      scope.extraDelegate = function(attr){
        if(scope.dialogForm.$valid){
          console.log(parentCtrl);
          //always call method in parent controller scope
          scope.$parent[attr](attrs.id)
        }else{
          $rootScope.$broadcast('alert','error','Please check your form input and resubmit.')
        }
      }
    }
  }
}]);
