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
    top_level_domain: "apigee.com",
    use_sso: true, // flag to override use SSO if needed set to ?use_sso=no
    login_url: "https://accounts.apigee.com/accounts/sign_in",
    profile_url: "https://accounts.apigee.com/accounts/my_account",
    logout_url: "https://accounts.apigee.com/accounts/sign_out",
    api_url: "https://api.usergrid.com/"
    },

    isTopLevelDomain:function () {
      return window.location.host === this.default.top_level_domain;
    },

    usingSSO:function () {
      return this.getSSO() && this.isTopLevelDomain();
    },

    getSSO:function (){
      return this.default.use_sso;
    },

    getSSOCallback:function () {
      var callbackUrl = this.buildBaseUrl();
      var separatorMark = '?';
      if (Usergrid.ApiClient.getApiUrl() !== undefined && (Usergrid.ApiClient.getApiUrl() !== this.default.api_url)) {
        separatorMark = '&';
        callbackUrl = callbackUrl + separatorMark + 'api_url=' + Usergrid.ApiClient.getApiUrl();
      }
      return encodeURIComponent(callbackUrl);
    },

    buildBaseUrl:function () {
      var baseUrl = window.location.protocol + '//' + window.location.host + window.location.pathname;
      return baseUrl;
    },

    //Private
    sendToPage:function (url) {
      var newPage = url + '?callback=' + this.getSSOCallback();
      console.log(newPage);
      window.location = newPage;
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
    }
  };
})(Usergrid);

Usergrid.SSO = new Usergrid.SSO();