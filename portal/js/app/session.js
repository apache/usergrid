/*
 * This file contains all the variables for a user's session.
 * It should contain 
 * These variables can modified by the client.
 *
 */

var usergrid = usergrid || {};

usergrid.session = (function() {

  /* This code block *WILL* load before the document is complete */

  function loggedIn() {
    return self.loggedInUser && self.accessToken;
  }

  function readIt() {
    self.currentOrganization = localStorage.getObject('currentOrganization');
    self.currentApplicationId = localStorage.getItem('currentApplicationId');
    self.loggedInUser = localStorage.getObject('usergridUser');
    self.accessToken = localStorage.getItem('accessToken');
  }

  function clearIt() {
    self.accessToken = null;
    self.loggedInUser = null;
    self.currentOrganizationName = null;
    self.currentApplicationId = null;
    self.currentOrganization = null;
    localStorage.removeItem('accessToken');
    localStorage.removeItem('usergridUser');
    localStorage.removeItem('currentOrganization');
    localStorage.removeItem('currentApplicationId');

    if (usergrid.client.useSSO()){
      usergrid.client.sendToSSOLogoutPage();
    }
  }

  function saveIt() {
    localStorage.setObject('currentOrganization', self.currentOrganization);
    localStorage.setItem('currentApplicationId', self.currentApplicationId);
    localStorage.setObject('usergridUser', self.loggedInUser);
    localStorage.setItem('accessToken', self.accessToken);
  }

  var self = {
    loggedInUser: {},
    accessToken: "",
    currentOrganization: {},
    currentApplicationId: "",

    loggedIn: loggedIn,
    readIt: readIt,
    clearIt: clearIt,
    saveIt: saveIt
  }

  return self

})();
