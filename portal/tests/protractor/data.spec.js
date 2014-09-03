
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
describe('Test Data', function () {

  beforeEach(function () {
    util.login();
    util.navigate(browser.params.orgName,browser.params.appName1);
  });

  describe('Add and delete', function () {
    it('should add and then delete', function () {
      browser.driver.get(browser.baseUrl + '#!/data');

      var entityName = 'test_e2e';
      var dateString = Date.now().toString();
      browser.wait(function(){
        return element(by.id("new-collection-link")).isDisplayed();
      });
      element(by.id("new-collection-link")).isDisplayed().then(function(){
        element(by.id("new-collection-link")).click();
      });
      browser.wait(function(){
        return element(by.id("new-collection-name")).isDisplayed();
      });
      element(by.id('new-collection-name')).isDisplayed().then(function () {
        //fill in data
        browser.sleep(500);
        element(by.id('new-collection-name')).clear();
        element(by.id('new-collection-name')).sendKeys(entityName);
        browser.sleep(1000);
        element(by.id('dialogButton-collection')).submit();
      });

      var link = element(by.id('collection-' + entityName+'s'+ '-link'));

      browser.wait(function () {
        return link.isPresent();
      });
      link.isPresent().then(function () {
        link.click();
        browser.sleep(1000);
      });
      //need to wait for the element not to be there.
      browser.wait(function(){
        return element(by.id('create-rb')).isPresent().then(function (present) {
          return present;
        });
      });
    });
  });
});