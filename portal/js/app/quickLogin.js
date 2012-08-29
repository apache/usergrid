/*
  This file enables the page to quickly redirect the user to the SSO login page.
  Requires:
  Usergrid.Params - params.js
  Usergrid.userSession -session.js

  Its prefered that Usergrid.Params loads parameters before QuickLogin.init() is called.
 */

(function(){
  Usergrid.QuickLogin = function(){};

  Usergrid.QuickLogin.default = {
  };

  Usergrid.QuickLogin.prototype = {
    init : function(){
      if(this.credentialsInParams()){
        Usergrid.userSession.setUserUUID(Usergrid.Params.default.queryParams.uuid);
        Usergrid.userSession.setUserEmail(Usergrid.Params.default.queryParams.admin_email);
        Usergrid.userSession.setAccessToken(Usergrid.Params.default.queryParams.access_token);
      }
      if (!Usergrid.userSession.loggedIn() && Usergrid.SSO.usingSSO()){
        Usergrid.SSO.sendToSSOLoginPage();
      }
    },
    credentialsInParams : function(){
      return(Usergrid.Params.default.queryParams.access_token && Usergrid.Params.default.queryParams.admin_email && Usergrid.Params.default.queryParams.uuid)
    }
  };
})(Usergrid);
