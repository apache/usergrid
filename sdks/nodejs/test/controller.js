/**
*  All Calls is a Node.js sample app that is powered by Usergrid
*  This app shows how to make the 4 REST calls (GET, POST,
*  PUT, DELETE) against the usergrid API.  
*
*  Learn more at http://Usergrid.com/docs
*
*   Copyright 2012 Apigee Corporation
*
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*/

/**
*  @file controller.js
*  @author Rod Simpson (rod@apigee.com)
*
*  This file contains the handlers that make calls to the API
*/
function main(querydata, response, sdk) {            
  var view = require("./view"); 
  
  switch (querydata.action_type) {
    case 'get':
      _get(response, sdk, view, querydata.path);
      break;
    case 'post':
      _post(response, sdk, view, querydata.path, querydata.data);
      break;
    case 'put':
      _put(response, sdk, view, querydata.path, querydata.data);
      break;
    case 'delete':
      _delete(response, sdk, view, querydata.path);
      break;
    case 'login':
      _login(response, sdk, view, querydata.username, querydata.password);
      break;
    default:
      view.getBody(response, '');     
      break;
  }
}

function _get(response, sdk, view, path) {
  sdk.ApiClient.runAppQuery(new sdk.Query('GET', path, null, null,
     function(output) {
       view.getBody(response, prepareOutput(output));  
     },
     function (output) {
       view.getBody(response, prepareOutput(output));
     }
  ));
}

function _post(response, sdk, view, path, data) {
  if (data) { 
    data = JSON.parse(data); 
  }
  sdk.ApiClient.runAppQuery(new sdk.Query('POST', path, data, null,
     function(output) {
       view.getBody(response, prepareOutput(output));  
     },
     function (output) {
       view.getBody(response, prepareOutput(output));
     }
  ));
}

function _put(response, sdk, view, path, data) {
  if (data) { 
    data = JSON.parse(data); 
  }
  sdk.ApiClient.runAppQuery(new sdk.Query('PUT', path, data, null,
    function(output) {
      view.getBody(response, prepareOutput(output));  
    },
    function (output) {
      view.getBody(response, prepareOutput(output));
    }
  ));
}

function _delete(response, sdk, view, path) {
  sdk.ApiClient.runAppQuery(new sdk.Query('DELETE', path, null, null,
    function(output) {
      view.getBody(response, prepareOutput(output));  
    },
    function (output) {
      view.getBody(response, prepareOutput(output));
    }
  ));
}

function _login(response, sdk, view, username, password) {
  sdk.ApiClient.logInAppUser(username, password,
    function (output, user) {
      view.getBody(response, prepareOutput(output));      
    },
    function (output) {
      view.getBody(response, prepareOutput(output)); 
    }
  );   
}

function prepareOutput(output){
  return '<pre>'+JSON.stringify(output, null, 2)+'</pre>'; 
}
exports.main = main;
