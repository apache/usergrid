'use strict';
var util = require('./util');
describe('Users ', function () {

  beforeEach(function () {
    util.login();
    util.navigate(browser.params.orgName,browser.params.appName1);
    browser.driver.get(browser.baseUrl + '#!/users');
  });

  describe('add and delete', function () {
    it('should add and then delete', function () {
      var username = 'test' + Date.now().toString();
      util.createUser(username);
      util.deleteUser(username);
    });
  });
});