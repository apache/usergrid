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

AppServices.Directives.directive('appswitcher', ["$rootScope", function ($rootScope) {
  return{
    restrict: 'ECA',
    scope: "=",
    templateUrl: 'global/appswitcher-template.html',
    replace: true,
    transclude: true,
    link: function linkFn(scope, lElement, attrs) {

      var classNameOpen = 'open';
      $('ul.nav li.dropdownContainingSubmenu').hover(
        function () {
          $(this).addClass(classNameOpen);
        },
        function () {
          $(this).removeClass(classNameOpen);
        }
      );
      $('#globalNav > a').mouseover(globalNavDetail);
      $('#globalNavDetail').mouseover(globalNavDetail);
      $('#globalNavSubmenuContainer ul li').mouseover(
        function () {
          $('#globalNavDetail > div').removeClass(classNameOpen);
          $('#' + this.getAttribute('data-globalNavDetail')).addClass(classNameOpen);
        }
      );
      function globalNavDetail() {
        $('#globalNavDetail > div').removeClass(classNameOpen);
        $('#globalNavDetailApiPlatform').addClass(classNameOpen);
      }
    }
  }
}])