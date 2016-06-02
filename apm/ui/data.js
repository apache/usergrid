AppServices.MAX = AppServices.MAX || angular.module('appservices.max', []);

AppServices.MAX.factory('data', function (ug) {
  return {
    get: function (id, url) {
      return ug.httpGet(id,url);
    },
    jsonp: function (objectType,criteriaId,params,successCallback) {
      return ug.jsonp(objectType,criteriaId,params,successCallback);
    },

    jsonp_simple: function (objectType,appId,params) {
      return ug.jsonpSimple(objectType,appId,params);
    },

    jsonp_raw: function (objectType,appId,params) {
      return ug.jsonpRaw(objectType,appId,params);
    },

    resource: function(params,isArray) {
      return ug.resource(params,isArray);
    },

    post: function(url,callback,payload,headers){
      return ug.httpPost(url,callback,payload,headers);

    }
  };
});

