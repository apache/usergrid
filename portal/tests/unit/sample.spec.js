'use strict';

describe('PageCtrl', function(){
  var scope;//we'll use this scope in our tests

  //mock Application to allow us to inject our own dependencies
  beforeEach(angular.mock.module('appservices.controllers'));
  beforeEach(angular.mock.module('angulartics'));
  beforeEach(angular.mock.module('angulartics.google.analytics'));
  //mock the controller for the same reason and include $rootScope and $controller
  beforeEach(function(){
    angular.mock.inject(function($rootScope, $controller){
      //create an empty scope
      scope = $rootScope.$new();
      //declare the controller and inject our empty scope
      $controller('PageCtrl', {
        $scope: scope,
        'data':{},
        'ug':{
          getAppSettings:function(){}
        },
        'utility':{},
        '$rootScope':$rootScope,
        '$location':{
          path:function(){
            return '/performance'
          },
          search:function(){
            return {
              demo:'false'
            }
          }
        },
        '$routeParams':{},
        '$q':{},
        '$route':{}
      });
    });
  });
  // tests start here

  // tests start here
  it('should have correct scope values.', function(){
    expect(scope.activeUI).toBe(false);
  });
});