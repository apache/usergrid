/*
  This file enables the page to quickly redirect the user to the SSO login page.
  Requires:
  Usergrid.Params - params.js
  Usergrid.userSession -session.js

  Its prefered that Usergrid.Params loads parameters before QuickLogin.init() is called.
 */

(function(){
  Usergrid.QuickLogin = function(){};

  Usergrid.QuickLogin.prototype = {
    init : function(queryParams, sessionExists, useSSO){
      if(this.credentialsInParams(queryParams)){
        Usergrid.userSession.setUserUUID(queryParams.uuid);
        Usergrid.userSession.setUserEmail(queryParams.admin_email);
        Usergrid.userSession.setAccessToken(queryParams.access_token);
      }
      if (!sessionExists && useSSO){
        Usergrid.SSO.sendToSSOLoginPage();
      }
    },
    credentialsInParams : function(params){
      return(params.access_token && params.admin_email && params.uuid);
    }
  };
})(Usergrid);

Usergrid.QuickLogin = new Usergrid.QuickLogin();
