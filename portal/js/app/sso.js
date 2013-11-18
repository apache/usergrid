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
    logout_url: "https://accounts.apigee.com/accounts/sign_in",
    api_url: "https://api.usergrid.com/"
    },

    isTopLevelDomain:function () {
      return window.location.hostname === this.default.top_level_domain;
    },

    usingSSO:function () {
      return this.getSSO() && this.isTopLevelDomain();
    },

    getSSO:function (){
      return this.default.use_sso;
    },

    getSSOCallback:function (urlCallback) {

       var url = this.buildBaseUrl();

      if(urlCallback) {
        url += "#" + urlCallback;
      }


      if (Usergrid.ApiClient.getApiUrl() !== undefined && (Usergrid.ApiClient.getApiUrl() !== this.default.api_url)) {
        var separatorMark = '&';
        url += separatorMark + 'api_url=' + Usergrid.ApiClient.getApiUrl();
      }
      console.log(url);
      url = encodeURIComponent(url);
      return'?callback=' + url;
    },

    buildBaseUrl:function () {
      var baseUrl = window.location.protocol + '//' + window.location.host + window.location.pathname;
      return baseUrl;
    },

    //Private
    sendToPage:function (url, urlCallback) {
      var newPage = url;
        newPage += this.getSSOCallback(urlCallback);
      //TODO: remove debug
      console.log(newPage);
      window.location = newPage;
  },

    sendToSSOLogoutPage:function (callbackUrl) {
      this.sendToPage(this.default.logout_url, callbackUrl);
    },

    sendToSSOLoginPage:function () {
      this.sendToPage(this.default.login_url);
    },

    sendToSSOProfilePage:function (callbackUrl) {
      this.sendToPage(this.default.profile_url, callbackUrl);
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