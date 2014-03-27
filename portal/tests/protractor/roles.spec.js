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