
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
describe('Roles ', function () {

  beforeEach(function () {
    util.login();
    util.navigate(browser.params.orgName,browser.params.appName1);
    browser.driver.get(browser.baseUrl + '#!/roles');
  });

  describe('add and delete', function () {
    it('should add and then delete', function () {
      var dateString = Date.now().toString();
      browser.wait(function(){
        return element(by.id('add-role-btn')).isDisplayed();
      });
      element(by.id('add-role-btn')).isDisplayed().then(function(){
        element(by.id('add-role-btn')).click();
      });
      element(by.id('new-role-roletitle')).isDisplayed().then(function(){
        element(by.id('new-role-roletitle')).sendKeys('title'+dateString);
        element(by.id('new-role-rolename')).sendKeys('name'+dateString);
        element(by.id('dialogButton-roles')).submit();
      });
      element(by.id('role-title'+dateString+'-link')).isDisplayed().then(function(){
        element(by.id('role-title'+dateString+'-link')).click();
      });
      element(by.id('role-title'+dateString+'-cb')).isDisplayed().then(function(){
        element(by.id('role-title'+dateString+'-cb')).click();
        element(by.id('delete-role-btn')).click();
        element(by.id('dialogButton-deleteroles')).submit();
      });
      browser.wait(function(){
        return element(by.id('role-title'+dateString+'-cb')).isPresent().then(function (present) {
          return !present;
        });
      });
    });
  });
});