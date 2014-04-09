'use strict';

describe('PageCtrl', function(){
  var scope;//we'll use this scope in our tests

  var rootscope;
  //mock Application to allow us to inject our own dependencies
  beforeEach(module('appservices.controllers'));
  //mock the controller for the same reason and include $rootScope and $controller
  beforeEach(function(){
    inject(function($rootScope, $controller){
      //create an empty scope
      rootscope = $rootScope;
      scope = $rootScope.$new();
      //declare the controller and inject our empty scope
      $controller('PageCtrl', {
        $scope: scope,
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
        '$route':{},
        '$log':{},
        '$analytics':{}
      });
    });
  });
  // tests start here

  // tests start here
  it('should have correct scope values.', function(){
    expect(scope.activeUI).toBe(false);
  });
});