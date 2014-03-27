'use strict';
var util = require('./util');
describe('Test Applications Dropdown', function () {

  var appName1 = browser.params.appName1
      , appName2 = browser.params.appName2;
  beforeEach(function(){
    util.login();
  });

  describe('Test Application Switching',function(){
    it('should navigate to sandbox.',function(){
      browser.driver.get(browser.baseUrl+'#!/app-overview/summary');
      browser.wait(function(){
        return element(by.id('app-overview-title')).getText().then(function(text){
          return text===appName2.toUpperCase();
        })
      })
      element(by.id('app-overview-title')).isPresent().then(function(){
        element(by.id('current-app-selector')).click();
        element(by.id('app-'+appName1+'-link-id')).click();
        browser.wait(function() {
          return element(by.id('app-overview-title')).getText().then(function(text) {
            return text === appName1.toUpperCase();
          });
        });
      });

    });
  });
});