'use strict';
var util = require('./util');
describe('Push Suite', function () {
  beforeEach(function(){
    util.login();
  });
  describe('Push',function(){
    it('should have Push fields',function(){
      browser.driver.get(browser.baseUrl+'/#!/push/sendNotification');
      browser.wait(function(){
        return element(by.id('notification-json')).isDisplayed();
      });
    });
  });
});