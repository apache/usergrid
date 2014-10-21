
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
describe('Test Org Dropdown', function () {

  beforeEach(function(){
    util.login();
  });

  describe('Test Org Switching',function(){
    var appName = browser.params.orgName;
    it('should navigate to sandbox.',function(){
      browser.driver.get(browser.baseUrl+'#!/org-overview');
      browser.wait(function () {
        return element.all(by.repeater("(k,v) in organizations")).count().then(function(count){
          var appCount =  count >0;
          return appCount;
        });
      });
      browser.wait(function(){
        return element(by.id('current-org-selector')).isDisplayed().then(function(){
          element(by.id('current-org-selector')).click();
          return true;
        });
      });
      browser.wait(function(){
        return element(by.id('org-'+appName+'-selector')).isDisplayed().then(function(){
          element(by.id('org-'+appName+'-selector')).click();
          return true;
        });
      });

      browser.wait(function() {
        return element(by.id('org-overview-name')).getText().then(function(text) {
          return text === appName;
        });
      });

    });
  });
});