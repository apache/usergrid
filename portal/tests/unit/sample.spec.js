/*
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
        'help':{},
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