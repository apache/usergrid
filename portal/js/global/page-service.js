AppServices.Services.factory('data', function (configuration, $q, $http, $resource, $rootScope, $log, $analytics) {

  function reportError(data,config){
    try {
      $analytics.eventTrack('error', {
        category: 'App Services', label: data + ':' + config.url + ':' + (sessionStorage['apigee_uuid'] || 'na')
      });
    } catch (e) {
      console.log(e)
    }
  };
  var getAccessToken = function(){
    return sessionStorage.getItem('accessToken');
  };

  return {

    /**
     * Retrieves page data
     * @param {string} id the name of the single page item to get (a key in the JSON) can be null.
     * @param {string} url location of the file/endpoint.
     * @return {Promise} Resolves to JSON.
     */
    get: function (id, url) {
      var items, deferred;

      deferred = $q.defer();

      $http.get((url || configuration.ITEMS_URL)).
          success(function (data, status, headers, config) {
            var result;
            if (id) {
              angular.forEach(data, function (obj, index) {
                if (obj.id === id) {
                  result = obj;
                }
              });
            } else {
              result = data;
            }
            deferred.resolve(result);
          }).
          error(function (data, status, headers, config) {
            $log.error(data, status, headers, config);
            reportError(data,config);
            deferred.reject(data);
          });

      return deferred.promise;
    },

//    getLocal: function (url) {
//      var retdata;
//      $http.get(url).
//          success(function (data, status, headers, config) {
//            retdata = data;
//          }).
//          error(function (data, status, headers, config) {
//          $log.error(data, status, headers, config);
//          });
//
//      return retdata;
//    },

    /**
     * Retrieves page data via jsonp
     * @param {string} url the location of the JSON/RESTful endpoint.
     * @param {string} successCallback function called on success.
     */
//    jsonp: function (url,successCallback) {
//      var self = this;
//      $http.jsonp(url).
//          success(function(data, status, headers, config) {
//            successCallback(data,status,headers,config);
//          }).
//          error(function(data, status, headers, config) {
//            console.log("ERROR: Could not get data. " + url);
//          });
//    },

    jsonp: function (objectType,criteriaId,params,successCallback) {
      if(!params){
        params = {};
      }

      params.demoApp = $rootScope.demoData;
      params.access_token = getAccessToken();
      params.callback = 'JSON_CALLBACK';


      $rootScope.$broadcast("ajax_loading", objectType);

      $http.jsonp($rootScope.urls().DATA_URL  + '/' + $rootScope.currentOrg + '/' + $rootScope.currentApp + '/apm/' + objectType + '/' + criteriaId,{params:params,headers:{}}).
        success(function(data, status, headers, config) {
          successCallback(data,status,headers,config);

          $rootScope.$broadcast("ajax_finished", objectType);


        }).
        error(function(data, status, headers, config) {
          $log.error("ERROR: Could not get jsonp data. " + $rootScope.urls().DATA_URL + objectType + '/' + criteriaId);
          reportError(data,config);
        });
    },

    jsonp_simple: function (objectType,appId,params) {
      if(!params){
        params = {};
      }

      params.access_token = getAccessToken();
      params.callback = 'JSON_CALLBACK';


      var deferred = $q.defer();

      $http.jsonp($rootScope.urls().DATA_URL  + '/' + $rootScope.currentOrg + '/' + $rootScope.currentApp + '/apm/' + objectType + "/" + appId,{params:params}).
        success(function(data, status, headers, config) {
          deferred.resolve(data);
        }).
        error(function(data, status, headers, config) {
          $log.error("ERROR: Could not get jsonp data. " + $rootScope.urls().DATA_URL  + '/' + $rootScope.currentOrg + '/' + $rootScope.currentApp + '/apm/' + objectType + "/" + appId);
          reportError(data,config);
          deferred.reject(data);
        });

      return deferred.promise;
    },

    jsonp_raw: function (objectType,appId,params) {
      if(!params){
        params = {};
      }

      params.access_token = getAccessToken();
      params.callback = 'JSON_CALLBACK';


      var deferred = $q.defer();

      $http.jsonp($rootScope.urls().DATA_URL  + '/' + $rootScope.currentOrg + '/' + $rootScope.currentApp + '/' + objectType,{params:params}).
        success(function(data, status, headers, config) {
          deferred.resolve(data);
        }).
        error(function(data, status, headers, config) {
          $log.error("ERROR: Could not get jsonp data. " + $rootScope.urls().DATA_URL + objectType + '/' + appId);
          reportError(data,config);
          deferred.reject(data);
        });

      return deferred.promise;
    },

    resource: function(params,isArray) {
      //temporary url for REST endpoints

      return $resource($rootScope.urls().DATA_URL + '/:orgname/:appname/:username/:endpoint',
          {

          },
          {
            get: {
              method:'JSONP',
              isArray: isArray,
              params: params
            },
            login: {
              method:'GET',
              url: $rootScope.urls().DATA_URL + '/management/token',
              isArray: false,
              params: params
            },
            save: {
              url: $rootScope.urls().DATA_URL + '/' + params.orgname + '/' + params.appname,
              method:'PUT',
              isArray: false,
              params: params
            }
          });
    },

    post: function(url,callback,payload,headers){

      var accessToken = getAccessToken();

      if(payload){
        payload.access_token = accessToken;
      }else{
        payload = {access_token:accessToken}
      }

      if(!headers){
        headers = {Bearer:accessToken};
      }

      $http({method: 'POST', url: url, data: payload, headers: headers}).
        success(function(data, status, headers, config) {
          callback(data)
        }).
        error(function(data, status, headers, config) {
          reportError(data,config);
          callback(data)
        });

    }



  }


});

