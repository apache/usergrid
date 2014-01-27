'use strict';
var util = require('./util');
describe('Users ', function () {

  beforeEach(function () {
    util.login();
    util.navigate(browser.params.orgName,browser.params.appName1);
    browser.driver.get(browser.baseUrl + '/#!/users');
  });

  describe('add and delete', function () {
    it('should add and then delete', function () {
      var dateString = Date.now().toString();

      element(by.id("new-user-button")).isPresent().then(function(){
        element(by.id("new-user-button")).click();
      });
      element(by.id('new-user-username')).isDisplayed().then(function () {
        //fill in data
        browser.sleep(500);
        element(by.id('new-user-username')).clear();
        element(by.id('new-user-username')).sendKeys('test' + dateString);
        element(by.id('new-user-fullname')).sendKeys('Test ' + dateString);
        element(by.id('new-user-email')).sendKeys('test' + dateString + '@apigee.com');
        element(by.id('new-user-password')).sendKeys(dateString);
        element(by.id('new-user-re-password')).sendKeys(dateString);
        browser.sleep(1000);
        element(by.id('dialogButton-users')).submit();
      });

      var deleteCheckbox = element(by.id('user-' + 'test' + dateString + '-checkbox'));
      browser.wait(function () {
        return deleteCheckbox.isPresent();
      });
      deleteCheckbox.isPresent().then(function () {
        deleteCheckbox.click();
        browser.sleep(1000);
        element(by.id('delete-user-button')).click();
        element(by.id('dialogButton-deleteusers')).submit();
      });
      //need to wait for the element not to be there.
      browser.wait(function(){
        return deleteCheckbox.isPresent().then(function (present) {
          return !present;
        });
      });
    });
  });
});