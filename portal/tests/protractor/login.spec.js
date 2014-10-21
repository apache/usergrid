
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
describe('Login ', function () {

  beforeEach(function () {
    browser.driver.get(browser.baseUrl + '');
  });

  describe('should login', function () {
    it('should fail to login', function () {

      if (browser.params.useSso) {
        return;
      }
      util.logout();

      browser.wait(function () {
        return element(by.model('login.username')).isPresent();
      });

      element(by.model('login.username')).isPresent().then(function () {
        element(by.id('login-username')).sendKeys('baduser');
        element(by.id('login-password')).sendKeys('badpassword');
        element(by.id('button-login')).submit();
      });

      browser.wait(function(){
        return  element(by.id('loginError')).isPresent();
      })
    });
    it('should logout after login', function () {
      util.login();
      browser.wait(function(){
        return browser.driver.getCurrentUrl().then(function (url) {
          var test = /org-overview/.test(url) || url.indexOf('org-overview')>0;
          test && util.logout();
          return test;
        });
      })
      browser.wait(function(){
        return  element(by.id('login-username')).isPresent();
      })
    });
  });
});