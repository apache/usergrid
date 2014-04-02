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