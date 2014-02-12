'use strict';
var util = require('./util');
describe('Monitoring Suite', function () {
  beforeEach(function(){
    util.login();
  });
  describe('Monitoring',function(){
    it('should have demo data',function(){
      browser.driver.get(browser.baseUrl+'/#!/performance/app-usage?timeFilter=1h&sessionChartFilter=&currentCompare=NOW');
      browser.wait(function(){
        return element(by.id('demo-data-ctrl')).isDisplayed();
      });
    });
  });
});