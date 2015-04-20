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

AppServices.Services.factory('help', function($rootScope, $http, $location) {

  $rootScope.help = {};
  $rootScope.help.helpButtonStatus = 'Enable Help';
  $rootScope.help.helpTooltipsEnabled = false;
  $rootScope.help.clicked = false;
  $rootScope.help.showHelpButtons = false;
  $rootScope.help.introjs_shouldLaunch = false;
  $rootScope.help.showTabsId = 'invisible';
  $rootScope.help.showJsonId = 'invisible';
  var tooltipStartTime;
  var helpStartTime;
  var introjs_step;

  /** get introjs and tooltip json from s3 **/
  var getHelpJson = function(path) {
    //return $http.get('https://s3.amazonaws.com/sdk.apigee.com/portal_help' + path + '/helpJson.json');
    return $http.get('helpJson.json');
  };

  /** check if first-time user experience should launch **/
  var getHelpStatus = function(helpType) {
    var status;
    if (helpType == 'tour') {
      //ftu for introjs
      status = localStorage.getItem('ftu_tour');
      localStorage.setItem('ftu_tour', 'false');
    } else if (helpType == 'tooltips') {
      //ftu for tooltips
      status = localStorage.getItem('ftu_tooltips');
      localStorage.setItem('ftu_tooltips', 'false');
    }
    return status;
  }

  /** sends GA event on mouseover of tooltip **/
  $rootScope.help.sendTooltipGA = function (tooltipName) {

  }

  /** hides/shows tooltips **/
  $rootScope.help.toggleTooltips = function() {
    if ($rootScope.help.helpTooltipsEnabled == false) {
      //turn on help tooltips
      $rootScope.help.helpButtonStatus = 'Disable Help';
      $rootScope.help.helpTooltipsEnabled = true;
      $rootScope.$broadcast('tooltips-enabled');
      showHelpModal('tooltips');
    } else {
      //turn off help tooltips
      $rootScope.help.helpButtonStatus = 'Enable Help';
      $rootScope.help.helpTooltipsEnabled = false;
      $rootScope.$broadcast('tooltips-disabled');
    }
  };

  /** show/hide introjs id attrs in the users>profile tab **/
  $rootScope.$on('users-received', function(event, users) {

    if(users._list.length > 0){
      $rootScope.help.showTabsId = "intro-information-tabs";
      $rootScope.help.showJsonId = "intro-json-object";
    } else {
      $rootScope.help.showTabsId = "invisible";
      $rootScope.help.showJsonId = "invisible";
    }
  });

  /** show/hide introjs id attrs in the users>profile tab **/
  $rootScope.$on('groups-received', function(event, groups) {
    if(groups._list.length > 0){
      $rootScope.help.showTabsId = "intro-information-tabs";
      $rootScope.help.showJsonId = "intro-json-object";
    } else {
      $rootScope.help.showTabsId = "invisible";
      $rootScope.help.showJsonId = "invisible";
    }
  });

  $rootScope.$on('$routeChangeSuccess', function(event, current) {
    //hide the help buttons if not on org-overview page
    var path = current.$$route ? current.$$route.originalPath : null;
    if (path === '/org-overview' || (path && path.indexOf('/performance') >= 0) || path === '/users' || path === '/groups' || path === '/roles' || path === '/data') {

      $rootScope.help.showHelpButtons = true;

      //retrieve the introjs and tooltip json for the current route
      getHelpJson(path).success(function(json) {

        var helpJson = json;

        //set help strings
        setHelpStrings(helpJson);

        //show tour modal if first time user
        showHelpModal('tour');
      });
    } else {
      $rootScope.help.showHelpButtons = false;
    }
  });

  /** pop modal if local storage 'ftu_tour'/'ftu_tooltip' is not set **/
  var showHelpModal = function(helpType) {
    //visitor is first time user
    var shouldHelp = location.search.indexOf('noHelp') <= 0;
    if (helpType == 'tour' && !getHelpStatus(helpType)) {
      shouldHelp && $rootScope.showModal('introjs');
    } else if (helpType == 'tooltips' && !getHelpStatus(helpType)) {
      shouldHelp && $rootScope.showModal('tooltips');
    }
  };

  /** set help strings for tooltips and introjs **/
  var setHelpStrings = function(helpJson) {
    //Intro.js steps
    $rootScope.help.IntroOptions.steps = helpJson.introjs;

    //Tooltips
    angular.forEach(helpJson.tooltip, function(value, binding) {
      $rootScope[binding] = value;
    });
    $rootScope.help.tooltip = helpJson.tooltip;
    $rootScope.$broadcast('helpJsonLoaded');
  }

  /** Start introjs **/

  /** options for introjs - steps are loaded from help.setHelpStrings() **/
  $rootScope.help.IntroOptions = {
    steps: [],
    showStepNumbers: false,
    exitOnOverlayClick: true,
    exitOnEsc: true,
    nextLabel: 'Next',
    prevLabel: 'Back',
    skipLabel: 'Exit',
    doneLabel: 'Done'
  };

  //user starts introjs
  $rootScope.help.introjs_StartEvent = function() {
    helpStartTime = Date.now();
    introjs_step = 1;
  }

  //user exits introjs
  $rootScope.help.introjs_ExitEvent = function() {
    var introjs_time = Math.round((Date.now() - helpStartTime) / 1000);
  };

  //user completes all steps in introjs for page
  $rootScope.help.introjs_CompleteEvent = function() {
    //go to the next page in the section and start introjs
    switch ($rootScope.currentPath) {
      case "/performance/app-usage":
        introjs_PageTransitionEvent('/performance/errors-crashes');
        break;

      case "/performance/errors-crashes":
        introjs_PageTransitionEvent('/performance/api-perf');
        break;

      case "/users":
        introjs_PageTransitionEvent('/groups');
        break;

      case "/groups":
        introjs_PageTransitionEvent('/roles');
        break;
    }
  }

  //transition user to next tab in feature section
  var introjs_PageTransitionEvent = function(url) {
    $location.url(url);
    $rootScope.help.introjs_shouldLaunch = true;
    $rootScope.$apply();
  }

  //increment the step tracking when user goes to next introjs step
  $rootScope.help.introjs_ChangeEvent = function() {
    introjs_step++;
  };

  /** End introjs **/

});
