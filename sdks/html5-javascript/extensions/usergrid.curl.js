  
Usergrid.Curl = (function () {
  function buildCurlCall(Query, endpoint) {
    var curl = 'curl';
    try {
      //peel the data out of the query object
      var method = Query.getMethod().toUpperCase();
      var path = Query.getResource();
      var jsonObj = Query.getJsonObj() || {};
      var params = Query.getQueryParams() || {};
      
      //curl - add the method to the command (no need to add anything for GET)
      if (method == "POST") {curl += " -X POST"; }
      else if (method == "PUT") { curl += " -X PUT"; }
      else if (method == "DELETE") { curl += " -X DELETE"; }
      else { curl += " -X GET"; }
      
      //curl - append the bearer token if this is not the sandbox app
      var application_name = Usergrid.ApiClient.getApplicationName();
      if (application_name) {
        application_name = application_name.toUpperCase();
      }
      if ( ((application_name != 'SANDBOX' && Usergrid.ApiClient.getToken()) || (Usergrid.ApiClient.getQueryType() == Usergrid.M && Usergrid.ApiClient.getToken())) ) {
        curl += ' -i -H "Authorization: Bearer ' + Usergrid.ApiClient.getToken() + '"';
        Query.setToken(true);
      }
      
      path =  '/' + endpoint + '/' + path;
      
      //make sure path never has more than one / together
      if (path) {
        //regex to strip multiple slashes
        while(path.indexOf('//') != -1){
          path = path.replace('//', '/');
        }
      }
      
      //curl - append the path
      curl += ' "' + Usergrid.ApiClient.getApiUrl()+path;

      //curl - append params to the path for curl prior to adding the timestamp
      var curl_encoded_params = Usergrid.ApiClient.encodeParams(params);
      if (curl_encoded_params) {
        curl += "?" + curl_encoded_params;
      }
      curl += '"';
      
      jsonObj = JSON.stringify(jsonObj)
      if (jsonObj && jsonObj != '{}') {
        //curl - add in the json obj
        curl += " -d '" + jsonObj + "'";
      }
      
    } catch(e) {
     console.log('Unable to build curl call:' + e);
    }
    
    return curl; 
  }
  
  return {
    buildCurlCall:buildCurlCall,
  }
})();
