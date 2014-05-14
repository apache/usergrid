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


/**
 * @description
 * Active menu item directive: Adds an 'active' class to a clicked item (for static menus)
 *
 * @param {string} can be anything, just gives a unique context to a given
 * menu.
 * Allows for reuse and multiple unordered list menus on one page.
 */
AppServices.Directives.directive('menu', ['$location', '$rootScope', '$log', function ($location, $rootScope, $log) {
  return{
    link: function linkFn(scope, lElement, attrs) {


      var menuContext, parentMenuItems, activeParentElement, menuItems, activeMenuElement, locationPath, subMenuContext;

      function setActiveElement(ele, locationPath, $rootScope, isParentClick) {
        if(!ele){
          return;
        }
        ele.removeClass('active');


        //now search all menu items to find the correct location path
        var newActiveElement = ele.parent().find('a[href*="#!' + locationPath + '"]'),
            menuItem,
            parentMenuItem;

        //there isn't a link in the menu anchors for the locationPath.. resolve to default
        //todo - duplicating code here.. needs rework
        if (newActiveElement.length === 0) {
          parentMenuItem = ele;
        } else {

          menuItem = newActiveElement.parent();
          //set the menu item to active
//

          //check if this is a top level (parent) menuitem without children
          if (menuItem.hasClass('option')) {
            parentMenuItem = menuItem[0];
          } else {
            if (menuItem.size() === 1) {
              //todo - omg this is terrible (please fix)
              parentMenuItem = newActiveElement.parent().parent().parent();
              //set the menu items parent to active
              parentMenuItem.addClass('active');
            } else {
              parentMenuItem = menuItem[0];
              menuItem = menuItem[1];
            }
          }

          try {
            //this try block is strictly for setting height for css transitions
            var menuItemCompare = (parentMenuItem[0] || parentMenuItem);

            //here we compare the selected parent element against the one clicked
            if (ele[0] !== menuItemCompare && isParentClick) {
              //reset back to 0 for css transition
              if (ele.find('ul')[0]) {
                ele.find('ul')[0].style.height = 0;
              }
            }

            var subMenuSizer = angular.element(parentMenuItem).find('.nav-list')[0];
            //if this menu item has a submenu then go
            if (subMenuSizer) {
              var clientHeight = subMenuSizer.getAttribute('data-height'),
                  heightCounter = 1,
                  heightChecker;

              if (!clientHeight && !heightChecker) {
                //hack for animation to get height. Have to set a data attribute to store the client height and use
                // it as a global var (data-*) for further nav
                heightChecker = setInterval(function () {
                  var tempHeight = (subMenuSizer.getAttribute('data-height') || subMenuSizer.clientHeight);
                  heightCounter = subMenuSizer.clientHeight;
                  //ugh, setting the heightCounter incase the dom does not instantly render the container height
                  //this allows the condition to be (or not be) met when both are equal on the interval
                  if(heightCounter === 0){
                    heightCounter = 1;
                  }

                  if (typeof tempHeight === 'string') {
                    tempHeight = parseInt(tempHeight,10)
                  }
                  //if you uncomment the below log statement, you'll visually see the dom/element height expand as the page loads
//            console.log(tempHeight,heightCounter)
                  if (tempHeight > 0 && heightCounter === tempHeight) {
                    subMenuSizer.setAttribute('data-height', tempHeight);
                    menuItem.addClass('active');
                    subMenuSizer.style.height = tempHeight + 'px';
                    clearInterval(heightChecker)
                  }
                  heightCounter = tempHeight;
                }, 20)

              } else {
                menuItem.addClass('active');
                subMenuSizer.style.height = clientHeight + 'px';
              }

              $rootScope.menuExecute = true;
            } else {
              //last and final case (if the parent menu item does not have a submenu we must set active)
              menuItem.addClass('active');
            }

          } catch (e) {
            $log.error('Problem calculating size of menu', e)
          }
        }

        //return the active element
        return {
          menuitem: menuItem,
          parentMenuItem: parentMenuItem
        };
      };
      function setupMenuState() {
        //setup a menuContext (passed in as an attr) to avoid namespace collisions and allow reuse
        menuContext = attrs.menu;

        //find all parent menu items (since this is a static list)
        parentMenuItems = lElement.find('li.option');

        //see if there is an already active parent element
        activeParentElement;

        if (lElement.find('li.option.active').length !== 0) {
          $rootScope[menuContext + 'Parent'] = lElement.find('li.option.active')
        }

        activeParentElement = ($rootScope[menuContext + 'Parent'] || null);
        if (activeParentElement) {
          activeParentElement = angular.element(activeParentElement);
        }

        //find all menu items (since this is a static list)
        menuItems = lElement.find('li.option li');

        locationPath = $location.path();

        if (activeParentElement) {
          activeMenuElement = angular.element(activeParentElement);
          activeMenuElement = activeMenuElement.find('li.active');
          activeMenuElement.removeClass('active');

          if (activeParentElement.find('a')[0]) {
            //find the link
            subMenuContext = activeParentElement.find('a')[0].href.split('#!')[1];
            //find the parent part of the submenu link (this is for tertiary linking where a menu option does not exist, but
            // we still want to keep the current menu active)
            var tempMenuContext = subMenuContext.split('/');
            subMenuContext = '/' + tempMenuContext[1];
            if (tempMenuContext.length > 2) {
              subMenuContext += '/' + tempMenuContext[2];
            }
          }
        }

        var activeElements;

        //see if the url location is the same as the default active parent element
        if (locationPath !== '' && locationPath.indexOf(subMenuContext) === -1) {
          activeElements = setActiveElement(activeParentElement, locationPath, scope);
          if(activeElements){
            $rootScope[menuContext + 'Parent'] = activeElements.parentMenuItem;
            $rootScope[menuContext + 'Menu'] = activeElements.menuitem;
          }
        } else {
          setActiveElement(activeParentElement, subMenuContext, scope);
        }

      }
      var bound = false;
      setTimeout(setupMenuState,500);
      //need the listener here to detect location changes and keep the menu highlighted
      scope.$on('$routeChangeSuccess', function () {
        setTimeout(function(){
          setupMenuState();
          if (!bound) {
            bound = true;
            //bind parent items to a click event
            parentMenuItems.bind('click', function (cevent) {
              //pull the previous selected menu item
              var previousParentSelection = angular.element($rootScope[menuContext + 'Parent']),
                  targetPath = angular.element(cevent.currentTarget).find('> a')[0].href.split('#!')[1];
              //first, when a top level menu is selected, remove any active children
              previousParentSelection.find('.nav > li').removeClass('active');
              //then, set the active
              var activeElements = setActiveElement(previousParentSelection, targetPath, scope, true);
              $rootScope[menuContext + 'Parent'] = activeElements.parentMenuItem;
              $rootScope[menuContext + 'Menu'] = activeElements.menuitem;


              //broadcast so other elements can deactivate
              scope.$broadcast('menu-selection');

            });

            //bind all menu items to a click event
            menuItems.bind('click', function (cevent) {
              //pull the previous selected menu item
              var previousMenuSelection = $rootScope[menuContext + 'Menu'],
                  targetElement = cevent.currentTarget;

              if (previousMenuSelection !== targetElement) {
                if (previousMenuSelection) {
                  angular.element(previousMenuSelection).removeClass('active')
                } else {
                  //remove the default active element on case of first click
                  activeMenuElement.removeClass('active');
                }

                scope.$apply(function () {
                  angular.element($rootScope[menuContext]).addClass('active');
                })

                $rootScope[menuContext + 'Menu'] = targetElement;
                //remember where we were at when clicking to another parent menu item
                angular.element($rootScope[menuContext + 'Parent']).find('a')[0].setAttribute('href', angular.element(cevent.currentTarget).find('a')[0].href)
              }
            })
          }
        },500);



      });



    }
  }
}])




/**
 * @description
 * Filter menus (derived from menu directive) but this is dynamically driven from a model
 *
 * @param {string} can be anything, just gives a unique
 * context to a given filter.
 *
 */
AppServices.Directives.directive('timeFilter', ["$location", "$routeParams", "$rootScope", function ($location, $routeParams, $rootScope) {
  return{
    restrict: 'A',
    transclude: true,
    template: '<li ng-repeat="time in timeFilters" class="filterItem"><a ng-click="changeTimeFilter(time)">{{time.label}}</a></li>',
    link: function linkFn(scope, lElement, attrs) {
      var menuContext = attrs.filter;

      scope.changeTimeFilter = function (newTime) {
        $rootScope.selectedtimefilter = newTime;
        $routeParams.timeFilter = newTime.value;
      }

      lElement.bind('click', function (cevent) {
        menuBindClick(scope, lElement, cevent, menuContext)
      })

    }
  }
}])


//todo: I am copying the code above and making a new filter because the template markup below is tied directly to a model
//not sure if there is a way to parameterize and abstract out... thus only having one instance of this filter directive.
AppServices.Directives.directive('chartFilter', ["$location", "$routeParams", "$rootScope", function ($location, $routeParams, $rootScope) {
  return{
    restrict: 'ACE',
    scope: '=',
    template: '<li ng-repeat="chart in chartCriteriaOptions" class="filterItem"><a ng-click="changeChart(chart)">{{chart.chartName}}</a></li>',
    link: function linkFn(scope, lElement, attrs) {
      var menuContext = attrs.filter;

      scope.changeChart = function (newChart) {
        $rootScope.selectedChartCriteria = newChart;
//        $rootScope.selectedChartCriteriaChange = ($routeParams[newChart.type + 'ChartFilter'] !== newChart.chartCriteriaId);
        //reset to "NOW" for now :)
        $routeParams.currentCompare = 'NOW';
        $routeParams[newChart.type + 'ChartFilter'] = newChart.chartCriteriaId;
      }

      lElement.bind('click', function (cevent) {
        menuBindClick(scope, lElement, cevent, menuContext)
      })
    }
  }
}])


//helper function for menu filters
function menuBindClick(scope, lElement, cevent, menuContext) {

  var currentSelection = angular.element(cevent.srcElement).parent();
  var previousSelection = scope[menuContext];

  if (previousSelection !== currentSelection) {
    if (previousSelection) {
      angular.element(previousSelection).removeClass('active')
    }
    scope[menuContext] = currentSelection;

    scope.$apply(function () {
      currentSelection.addClass('active');
    })
  }

}

AppServices.Directives.directive('orgMenu', ["$location", "$routeParams", "$rootScope", "ug", function ($location, $routeParams, $rootScope, ug) {
  return{
    restrict: 'ACE',
    scope: '=',
    replace: true,

    templateUrl:'menus/orgMenu.html',

    link: function linkFn(scope, lElement, attrs) {


      scope.orgChange = function (orgName) {
        var oldOrg = ug.get('orgName');
        ug.set('orgName', orgName);
        $rootScope.currentOrg = orgName;
        $location.path('/org-overview');
        //make a call to get the users applications
        $rootScope.$broadcast('org-changed',oldOrg,orgName);
      };


      scope.$on('change-org', function(args,org){
        scope.orgChange(org);
      });
    }
  }
}]);
AppServices.Directives.directive('appMenu', ["$location", "$routeParams", "$rootScope", "ug", function ($location, $routeParams, $rootScope, ug) {
  return{
    restrict: 'ACE',
    scope: '=',
    replace: true,

    templateUrl:'menus/appMenu.html',

    link: function linkFn(scope, lElement, attrs) {

      scope.myApp = {};
      var bindApplications = function(applications){
        scope.applications = applications;

        var size = 0, key;
        for (key in applications) {
          if (applications.hasOwnProperty(key)) size++;
        }
        scope.hasApplications = Object.keys(applications).length > 0;

        if(!scope.myApp.currentApp){
          $rootScope.currentApp = scope.myApp.currentApp = ug.get('appName');
        }
        var hasApplications = Object.keys(applications).length > 0;
        if (!applications[scope.myApp.currentApp]) {
          if (hasApplications) {
            $rootScope.currentApp = scope.myApp.currentApp = applications[ Object.keys(applications)[0]].name;
          } else {
            $rootScope.currentApp = scope.myApp.currentApp = '';
          }
        }
        setTimeout(function(){
          if(!scope.hasApplications){
            scope.showModal('newApplication');
          }else{
            scope.hideModal('newApplication');
          }
        },1000);
      };

      //called from top-level menu when changing apps
      scope.appChange = function(newApp){
        var oldApp = scope.myApp.currentApp;
        ug.set('appName', newApp);
        $rootScope.currentApp = scope.myApp.currentApp = newApp;

        $rootScope.$broadcast('app-changed',oldApp,newApp)

      };
      scope.$on('app-initialized',function(){
        bindApplications(scope.applications);
        scope.applyScope();
      });
      scope.$on('applications-received', function (event, applications) {
        bindApplications(applications);

        scope.applyScope();
      });

      scope.$on('applications-created',function(evt,applications,name){
        $rootScope.$broadcast('alert', 'info', 'New application "' + scope.newApp.name + '" created!');
        scope.appChange(name);
        $location.path('/getting-started/setup');
        scope.newApp.name = '';
      });

      scope.newApplicationDialog = function(modalId){
        var createNewApp = function(){
          var found =false;
          if(scope.applications){
            for(var app in scope.applications){
              if( app === scope.newApp.name.toLowerCase()){
                found = true;
                break;
              }
            }
          }
          if (scope.newApp.name && !found) {
            ug.createApplication(scope.newApp.name);
          } else {
            $rootScope.$broadcast('alert', 'error', !found ? 'You must specify a name.' : 'Application already exists.');
          }
          return found;
        };
        //todo: put more validate here
        scope.hasCreateApplicationError = createNewApp();

        if(!scope.hasCreateApplicationError){
          scope.applyScope()
        }
        scope.hideModal(modalId);
      };
      if(scope.applications){
        bindApplications(scope.applications);
      }
    }
  }
}])
