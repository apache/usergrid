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