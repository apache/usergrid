
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
describe('Forgot Password', function () {
  beforeEach(function(){
    browser.driver.get(browser.baseUrl + '');
    browser.waitForAngular();
    util.logout();
  });
  it('should have correct iframe url', function () {
    if(browser.params.useSso){
      //this will not work with sso since its an enterprise config.
      return;
    }
    browser.wait(function () {
      return browser.driver.getCurrentUrl().then(function (url) {
        return /login/.test(url);
      });
    });
    element(by.id('button-forgot-password')).isPresent().then(function () {
      element(by.id('button-forgot-password')).click();
      browser.driver.get(browser.baseUrl+'#!/forgot-password')
    });
    browser.wait(function () {
      return browser.driver.getCurrentUrl().then(function (url) {
        return /forgot-password/.test(url);
      });
    });
    element(by.id('forgot-password-frame')).isPresent().then(function () {
      expect(element(by.id('forgot-password-frame')).getAttribute('src')).toEqual('https://api.usergrid.com/management/users/resetpw');
    });

  });
});