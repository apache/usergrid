/*
 * Usergrid.SSO
 * SSO functions apigee specific,
 */
(function () {
  Usergrid.SSO = function () {
  };

  Usergrid.SSO.default = {
    top_level_domain:"apigee.com",
    use_sso:true, // flag to overide use SSO if needed set to ?use_sso=no
    login_url:"https://accounts.apigee.com/accounts/sign_in",
    profile_url:"https://accounts.apigee.com/accounts/my_account",
    logout_url:"https://accounts.apigee.com/accounts/sign_out",
    //this variable is repeated from console.js TODO: find a suitable container in which to place this variable.
    api_url:"https://api.usergrid.com/"
  };

  Usergrid.SSO.prototype = {
    isTopLevelDomain:function () {
      return window.location.host === this.default.top_level_domain;
    },
    usingSSO:function () {
      return this.default.use_sso;
    },
    getSSOCallback:function () {
      var callbackUrl = this.buildBaseUrl();
      var separatorMark = '?';
      if (this.usingSSO()) {
        callbackUrl = callbackUrl + separatorMark + 'use_sso=' + this.default.use_sso;
      }
      if (Usergrid.ApiClient.getApiUrl() !== undefined && (Usergrid.ApiClient.getApiUrl() !== this.default.api_url)) {
        separatorMark = '&';
        callbackUrl = callbackUrl + separatorMark + 'api_url=' + self.apiUrl;
      }
      return encodeURIComponent(callbackUrl);
    },
    buildBaseUrl:function () {
      baseUrl = window.location.protocol + '//' + window.location.host + window.location.pathname;
      return baseUrl;
    },
    //Private
    sendToPage:function (url) {
      //TODO: check if throwing an exception is necesary to stop page from loading after sending it to the SSO page
      window.location = url + '?callback=' + this.getSSOCallback();
      return false;
    },
    SendToSSOLogoutPage:function () {
      this.sendToPage(this.default.logout_url);
    },
    SendToSSOLoginPage:function () {
      this.sendToPage(this.default.login_url);
    },
    SendToSSOProfilePage:function () {
      this.sendToPage(this.default.profile_url);
    },
    setUseSSO:function (sso) {
      if (sso == 'yes' || 'true') {
        this.default.use_sso = true;
      } else if (sso == 'no' || 'false') {
        this.default.use_sso = false;
      }
    },
    setLoginUrl:function (url) {
      this.default.login_url = url;
    },
    setLogoutUrl:function (url) {
      this.default.logout_url = url;
    },
    setProfileUrl:function (url) {
      this.default.profile_url = url;
    }
  };
})(Usergrid);