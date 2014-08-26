
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
var fs=require('fs');
var path=require('path');
module.exports = {
  loggedin:false,
  login: function(){
    if (this.loggedin) {
      return;
    }
    var self = this;
    browser.driver.get(browser.baseUrl + '');
    browser.wait(function () {
      return browser.driver.getCurrentUrl().then(function (url) {
        return /login/.test(url) || url.indexOf('accounts/sign_in')>0;
      });
    });
    if(browser.params.useSso){
      browser.wait(function(){
        return browser.driver.findElement(by.id('email')).isDisplayed();
      });
      browser.driver.findElement(by.id('email')).isDisplayed().then(function () {
        browser.driver.findElement(by.id('email')).sendKeys(browser.params.login.user);
        browser.driver.findElement(by.id('password')).sendKeys(browser.params.login.password);
        browser.driver.findElement(by.id('btnSubmit')).click();
      });
      browser.wait(function () {
        return browser.driver.getCurrentUrl().then(function (url) {
          return  url.indexOf('org-overview')>0;
        });
      });
    }else{
      element(by.model('login.username')).isPresent().then(function () {
        element(by.model('login.username')).sendKeys(browser.params.login.user);
        element(by.model('login.password')).sendKeys(browser.params.login.password);
        element(by.id('button-login')).submit();
      });
    }

    browser.wait(function(){
      return element(by.id('current-org-selector')).isDisplayed();
    });

    browser.wait(function () {
      return element.all(by.repeater("(k,v) in organizations")).count().then(function(count){
        var appCount =  count >0;
        return appCount;
      });
    });
    browser.wait(function () {
      return element.all(by.repeater("app in applications")).count().then(function(count){
        var appCount =  count >1;
        self.loggedin = appCount;
        return appCount;
      });
    });

  },
  logout:function(){
    if(this.loggedin){
      this.loggedin=false;
      browser.driver.get(browser.baseUrl+'#!/logout');
      browser.wait(function () {
        return browser.driver.getCurrentUrl().then(function (url) {
          var test =  /login/.test(url) || url.indexOf('accounts/sign_in')>0;
          test && browser.sleep(1000);
          return test;
        });
      });
    }
  },
  navigate:function(orgName,appName){
    browser.driver.get(browser.baseUrl+'#!/org-overview');
    browser.sleep(1000);
    browser.wait(function () {
      return element.all(by.repeater("(k,v) in organizations")).count().then(function(count){
        var appCount =  count >0;
        return appCount;
      });
    });
    browser.wait(function(){
      return element(by.id('current-org-selector')).isPresent().then(function(present){
        return present;
      });
    });
    element(by.id('current-org-selector')).click();

    browser.wait(function(){
      return element(by.id('org-'+orgName+'-selector')).isPresent().then(function(present){
        return present;
      });
    });
    element(by.id('org-'+orgName+'-selector')).click();

    browser.wait(function() {
      return element(by.id('org-overview-name')).getText().then(function(text) {
        return text === orgName;
      });
    });
    element(by.id('current-app-selector')).isPresent().then(function(){
      element(by.id('current-app-selector')).click();
      element(by.id('app-'+appName+'-link-id')).click();
      browser.sleep(1000);
    });
    browser.driver.get(browser.baseUrl+'#!/app-overview/summary');
    browser.wait(function(){
      return element(by.id('app-overview-title')).getText().then(function(text){
        return text===appName.toUpperCase();
      })
    });
  },
  createUser:function(username){
    this.login();
    this.navigate(browser.params.orgName,browser.params.appName1);
    browser.driver.get(browser.baseUrl + '#!/users');
    browser.wait(function(){
      return element(by.id("new-user-button")).isDisplayed();
    });
    element(by.id("new-user-button")).isDisplayed().then(function(){
      element(by.id("new-user-button")).click();
    });
    browser.wait(function(){
      return element(by.id("new-user-username")).isDisplayed();
    });
    element(by.id('new-user-username')).isDisplayed().then(function () {
      //fill in data
      browser.sleep(500);
      element(by.id('new-user-username')).clear();
      element(by.id('new-user-username')).sendKeys(username);
      element(by.id('new-user-fullname')).sendKeys('Test ' + username);
      element(by.id('new-user-email')).sendKeys('rsimpson+test' + username + '@apigee.com');
      element(by.id('new-user-password')).sendKeys('P@ssw0rd1');
      element(by.id('new-user-re-password')).sendKeys('P@ssw0rd1');
      browser.sleep(1000);
      element(by.id('dialogButton-users')).submit();
    });

    browser.wait(function () {
      return element(by.id('user-' + username + '-checkbox')).isPresent();
    });
  },
  deleteUser:function(username){
    this.login();
    this.navigate(browser.params.orgName,browser.params.appName1);
    browser.driver.get(browser.baseUrl + '#!/users');

    browser.wait(function () {
      return element(by.id('user-' + username + '-checkbox')).isPresent();
    });

    element(by.id('user-' + username + '-checkbox')).isPresent().then(function () {
      element(by.id('user-' + username + '-checkbox')).click();
      browser.sleep(1000);
      element(by.id('delete-user-button')).click();
      element(by.id('dialogButton-deleteusers')).submit();
    });
    //need to wait for the element not to be there.
    browser.wait(function(){
      return element(by.id('user-' + username + '-checkbox')).isPresent().then(function (present) {
        return !present;
      });
    });
  },
  getCoverage:function(){
    var dname=path.normalize(__dirname+"/../../reports");
    var fname=dname+'/coverage.json';
    var browserCoverageObject="('__coverage__' in window)?__coverage__:null";
    if(!fs.existsSync(dname)){
      fs.mkdirSync(dname);
    }
    // console.log(__dirname, dname, fname);
    browser.driver.executeScript("return "+browserCoverageObject+';').then(function(val){
      if(val){
        fs.writeFileSync(fname, JSON.stringify(val));
      }else{
        console.warn("No coverage object in the browser.")
      }
    });
  }
};
