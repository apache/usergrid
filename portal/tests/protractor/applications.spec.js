
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
var util = require('./util');
describe('Test Applications Dropdown', function () {

  var appName1 = browser.params.appName1
      , appName2 = browser.params.appName2;
  beforeEach(function(){
    util.login();
  });

  describe('Test Application Switching',function(){
    it('should navigate to sandbox.',function(){
      browser.driver.get(browser.baseUrl+'#!/app-overview/summary');
      browser.wait(function(){
        return element(by.id('app-overview-title')).getText().then(function(text){
          return text===appName2.toUpperCase();
        })
      })
      element(by.id('app-overview-title')).isPresent().then(function(){
        element(by.id('current-app-selector')).click();
        element(by.id('app-'+appName1+'-link-id')).click();
        browser.wait(function() {
          return element(by.id('app-overview-title')).getText().then(function(text) {
            return text === appName1.toUpperCase();
          });
        });
      });

    });
  });
});