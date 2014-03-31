'use strict';
var util = require('./util');
describe('Test Org Dropdown', function () {

  beforeEach(function(){
    util.login();
  });

  describe('Test Org Switching',function(){
    var appName = browser.params.orgName;
    it('should navigate to sandbox.',function(){
      browser.driver.get(browser.baseUrl+'#!/org-overview');
      browser.wait(function () {
        return element.all(by.repeater("(k,v) in organizations")).count().then(function(count){
          var appCount =  count >0;
          return appCount;
        });
      });
      browser.wait(function(){
        return element(by.id('current-org-selector')).isDisplayed().then(function(){
          element(by.id('current-org-selector')).click();
          return true;
        });
      });
      browser.wait(function(){
        return element(by.id('org-'+appName+'-selector')).isDisplayed().then(function(){
          element(by.id('org-'+appName+'-selector')).click();
          return true;
        });
      });

      browser.wait(function() {
        return element(by.id('org-overview-name')).getText().then(function(text) {
          return text === appName;
        });
      });

    });
  });
});