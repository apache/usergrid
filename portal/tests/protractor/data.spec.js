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