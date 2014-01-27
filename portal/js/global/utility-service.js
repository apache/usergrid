AppServices.Services.factory('utility', function (configuration, $q, $http, $resource) {

  return {

    keys: function(o) {
      var a = [];
      for (var propertyName in o) {
        a.push(propertyName);
      }
      return a;
    },
    get_gravatar: function(email, size) {
      try {
        var size = size || 50;
        if (email.length) {
          return 'https://secure.gravatar.com/avatar/' + MD5(email) + '?s=' + size ;
        } else {
          return 'https://apigee.com/usergrid/images/user_profile.png';
        }
      } catch(e) {
        return 'https://apigee.com/usergrid/images/user_profile.png';
      }
    },
    get_qs_params: function() {
      var queryParams = {};
      if (window.location.search) {
        // split up the query string and store in an associative array
        var params = window.location.search.slice(1).split("&");
        for (var i = 0; i < params.length; i++) {
          var tmp = params[i].split("=");
          queryParams[tmp[0]] = unescape(tmp[1]);
        }
      }
      return queryParams;
    },

    safeApply: function(fn) {
      var phase = this.$root.$$phase;
      if(phase == '$apply' || phase == '$digest') {
        if(fn && (typeof(fn) === 'function')) {
          fn();
        }
      } else {
        this.$apply(fn);
      }
    }
  };

})



