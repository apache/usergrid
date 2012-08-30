/*
 * Usergrid.SSO
 * SSO functions apigee specific
 *
 * Requires:
 * Usergrid.ApiClient
 */
(function () {
  Usergrid.SSO = function () {
  };

  Usergrid.SSO.prototype = {
    default : {
    top_level_domain:"apigee.com",
    use_sso:true, // flag to override use SSO if needed set to ?use_sso=no
    login_url:"https://accounts.apigee.com/accounts/sign_in",
    profile_url:"https://accounts.apigee.com/accounts/my_account",
    logout_url:"https://accounts.apigee.com/accounts/sign_out"
    },
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
      /* TODO: Check if this logic is still necessary
      if (Usergrid.ApiClient.getApiUrl() !== undefined && (Usergrid.ApiClient.getApiUrl() !== this.default.api_url)) {
        separatorMark = '&';
        callbackUrl = callbackUrl + separatorMark + 'api_url=' + Usergrid.ApiClient.getApiUrl();
      }
      */
      return encodeURIComponent(callbackUrl);
    },
    buildBaseUrl:function () {
      baseUrl = window.location.protocol + '//' + window.location.host + window.location.pathname;
      return baseUrl;
    },
    //Private
    sendToPage:function (url) {
      window.location = url + '?callback=' + this.getSSOCallback();
      throw ("Sending to : " + url );
    },
    sendToSSOLogoutPage:function () {
      this.sendToPage(this.default.logout_url);
    },
    sendToSSOLoginPage:function () {
      this.sendToPage(this.default.login_url);
    },
    sendToSSOProfilePage:function () {
      this.sendToPage(this.default.profile_url);
    },
    setUseSSO:function (sso) {
      if (sso ===( 'yes' || 'true')) {
        this.default.use_sso = true;
      } else if (sso === ('no' || 'false')) {
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

Usergrid.SSO = new Usergrid.SSO();