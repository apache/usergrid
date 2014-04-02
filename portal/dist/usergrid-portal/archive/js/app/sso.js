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
    test_top_level_domain: "appservices.apigee.com",
    local_domain: "localhost",
    use_sso: true, // flag to override use SSO if needed set to ?use_sso=no
    login_url: "https://accounts.apigee.com/accounts/sign_in",
    profile_url: "https://accounts.apigee.com/accounts/my_account",
    logout_url: "https://accounts.apigee.com/accounts/sign_out",
    api_url: "https://api.usergrid.com/"
    },

    isTopLevelDomain:function () {
      if ( window.location.hostname === this.default.top_level_domain ||
           window.location.hostname === this.default.test_top_level_domain ||
           window.location.hostname === this.default.local_domain ||
           location.pathname.indexOf('/dit') >= 0 ||
           location.pathname.indexOf('/mars') >= 0
         )
      {
        return true;
      }else{
        return false;
      }

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
      //url = encodeURIComponent(url);
      return'?callback=' + url;
    },

    buildBaseUrl:function () {
      var baseUrl = window.location.protocol + '//' + window.location.host + window.location.pathname;
      return baseUrl;
    },

    sendToSSOLogoutPage:function (callbackUrl) {
      var url = this.default.logout_url;
      if(window.location.host === 'localhost'){
        //DIT
        url = 'https://accounts.jupiter.apigee.net/accounts/sign_out';
      }if(window.location.host === 'appservices.apigee.com' && location.pathname.indexOf('/dit') >= 0){
        //DIT
        url = 'https://accounts.jupiter.apigee.net/accounts/sign_out';
      }else if(window.location.host === 'appservices.apigee.com' && location.pathname.indexOf('/mars') >= 0 ){
        //staging
        url = 'https://accounts.mars.apigee.net/accounts/sign_out';
      }else{
        url = this.default.logout_url;
      }
      url = url + this.getSSOCallback();
      window.location = url;
    },

    sendToSSOLoginPage:function () {
      var url = this.default.login_url;
      if(window.location.host === 'localhost'){
        //DIT
        url = 'https://accounts.jupiter.apigee.net/accounts/sign_in';
      }else if(window.location.host === 'appservices.apigee.com' && location.pathname.indexOf('/dit') >= 0){
        //DIT
        url = 'https://accounts.jupiter.apigee.net/accounts/sign_in';
      }else if(window.location.host === 'appservices.apigee.com' && location.pathname.indexOf('/mars') >= 0 ){
        //staging
        url = 'https://accounts.mars.apigee.net/accounts/sign_in';
      }else{
        url = this.default.login_url;
      }
      url = url + this.getSSOCallback();
      window.location = url;
    },

    sendToSSOProfilePage:function (callbackUrl) {
      var url = this.default.profile_url;
      if(window.location.host === 'localhost'){
        //DIT
        url = 'https://accounts.jupiter.apigee.net/accounts/my_account';
      } else if(window.location.host === 'appservices.apigee.com' && location.pathname.indexOf('/dit') >= 0){
        //DIT
        url = 'https://accounts.jupiter.apigee.net/accounts/my_account';
      } else if(window.location.host === 'appservices.apigee.com' && location.pathname.indexOf('/mars') >= 0 ){
        //staging
        url = 'https://accounts.mars.apigee.net/accounts/my_account';
      }else{
        url = this.default.profile_url;
      }
      url = url + this.getSSOCallback();
      window.location = url;
    },

    setUseSSO:function (sso) {
      if (sso == 'yes' || sso == 'true') {
        this.default.use_sso = true;
      } else if (sso == 'no' || sso == 'false') {
        this.default.use_sso = false;
      }
    }
  };
})(Usergrid);

Usergrid.SSO = new Usergrid.SSO();