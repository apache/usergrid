'use strict';

module.exports = {
  loggedin:false,
  login: function(){
    if (this.loggedin) {
      return;
    }
    var self = this;
    browser.driver.get(browser.baseUrl + '/');
    browser.wait(function () {
      return browser.driver.getCurrentUrl().then(function (url) {
        return/login/.test(url) || url.indexOf('accounts/sign_in')>0;
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
      browser.driver.get(browser.baseUrl+'/#!/logout');
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
    browser.driver.get(browser.baseUrl+'/#!/org-overview');
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
    browser.driver.get(browser.baseUrl+'/#!/app-overview/summary');
    browser.wait(function(){
      return element(by.id('app-overview-title')).getText().then(function(text){
        return text===appName.toUpperCase();
      })
    });
  }
};
