'use strict';

AppServices.Services.factory('help', function($rootScope, $http, $analytics) {

  $rootScope.help = {};
  $rootScope.help.helpButtonStatus = 'Enable Help';
  $rootScope.help.helpTooltipsEnabled = false;
  $rootScope.help.clicked = false;
  $rootScope.help.showHelpButtons = false;
  var tooltipStartTime;
  var helpStartTime;
  var introjs_step;    

  $rootScope.help.sendTooltipGA = function (tooltipName) {      
    $analytics.eventTrack('tooltip - ' + $rootScope.currentPath, {
      category: 'App Services', 
      label: tooltipName
    });
  }
  
  $rootScope.help.toggleTooltips = function() {
    if ($rootScope.help.helpTooltipsEnabled == false) {
      //turn on help tooltips
      $rootScope.help.helpButtonStatus = 'Disable Help';
      $rootScope.help.helpTooltipsEnabled = true;
      showHelpModal('tooltips');
    } else {
      //turn off help tooltips
      $rootScope.help.helpButtonStatus = 'Enable Help';
      $rootScope.help.helpTooltipsEnabled = false;
    }
  };

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

  $rootScope.$on("$routeChangeSuccess", function(event, current) {      
    //hide the help buttons if not on org-overview page
    var path = current.$$route ? current.$$route.originalPath : null;
    if (path == '/org-overview') {
      
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

  //pop modal if local storage 'ftu_tour'/'ftu_tooltip' is not set
  var showHelpModal = function(helpType) {
    //visitor is first time user
    var shouldHelp = location.search.indexOf('noHelp') <= 0;
    if (helpType == 'tour' && !getHelpStatus(helpType)) {
      shouldHelp && $rootScope.showModal('introjs');
    } else if (helpType == 'tooltips' && !getHelpStatus(helpType)) {
      shouldHelp && $rootScope.showModal('tooltips');
    }
  };

  //set help strings
  var setHelpStrings = function(helpJson) {
    //Intro.js steps
    $rootScope.help.IntroOptions.steps = helpJson.introjs;

    //Tooltips
    angular.forEach(helpJson.tooltip, function(value, binding) {
      $rootScope[binding] = value;
    });
  }

  

  //user starts introjs
  $rootScope.help.introjs_StartEvent = function() {
    helpStartTime = Date.now();
    introjs_step = 1;
  }

  //user exits introjs
  $rootScope.help.introjs_ExitEvent = function() {
    var introjs_time = Math.round((Date.now() - helpStartTime) / 1000);

    //capture time spent in introjs
    $analytics.eventTrack('introjs timing - ' + $rootScope.currentPath, {
      category: 'App Services',
      label: introjs_time + 's'
    });

    //capture what introjs step user exited on 
    $analytics.eventTrack('introjs exit - ' + $rootScope.currentPath, {
      category: 'App Services',
      label: 'step' + introjs_step
    });      
  };

  //increment the step tracking when user goes to next introjs step
  $rootScope.help.introjs_ChangeEvent = function() {
    introjs_step++;
  };

  //In-portal help end



  var getHelpJson = function(path) {
    return $http.jsonp('http://sdk.apigee.com.s3.amazonaws.com/portal_help' + path + '/helpJson.json?callback=JSON_CALLBACK');
  };

  var getHelpStatus = function(helpType) {
    var status;
    if (helpType == 'tour') {
      status = localStorage.getItem('ftu_tour');        
      localStorage.setItem('ftu_tour', 'false');
    } else if (helpType == 'tooltips') {
      status = localStorage.getItem('ftu_tooltips');        
      localStorage.setItem('ftu_tooltips', 'false');
    }
    return status;
  }

});