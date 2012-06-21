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
    self.loggedInUser = localStorage.getObject('usergrid_user');
    self.accessToken = localStorage.usergrid_access_token;
  }

  function clearIt() {
    self.loggedInUser = null;
    self.accessToken = null;
    self.currentOrganization = null;
    localStorage.removeItem('usergrid_user');
    localStorage.removeItem('usergrid_access_token');
    if (usergrid.client.useSSO()){
      sendToSSOLogoutPage();
    }
  }

  var self = {
    loggedInUser: null,
    accessToken: null,
    currentOrganization: null,
    currentApplicationId: null,

    loggedIn: loggedIn,
    readIt: readIt,
    clearIt: clearIt
  }

  return self
})();
