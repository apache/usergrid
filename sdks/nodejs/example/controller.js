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
function main(querydata, response, client) {            
  var view = require("./view"); 
  
  switch (querydata.action_type) {
    case 'get':
      _get(response, client, view, querydata.path);
      break;
    case 'post':
      _post(response, client, view, querydata.path, querydata.data);
      break;
    case 'put':
      _put(response, client, view, querydata.path, querydata.data);
      break;
    case 'delete':
      _delete(response, client, view, querydata.path);
      break;
    case 'login':
      _login(response, client, view, querydata.username, querydata.password);
      break;
    default:
      view.getBody(response, '');     
      break;
  }
}

function _get(response, client, view, path) {
  client.request(
    { method:"GET"
    , endpoint:path
    } ,
    function(err, data){
      //since the point of this app is to display the result that is given back
      //by the API, we want to do the same thing regardless of the err
      view.getBody(response, prepareOutput(data));
    }    
  );
}

function _post(response, client, view, path, data) {
  if (data) { 
    data = JSON.parse(data); 
  }
  client.request(
    { method:"POST"
    , endpoint:path
    , body:data
    } ,
    function(err, data){
      //since the point of this app is to display the result that is given back
      //by the API, we want to do the same thing regardless of the err
      view.getBody(response, prepareOutput(data));
    }    
  );
}

function _put(response, client, view, path, data) {
  if (data) { 
    data = JSON.parse(data); 
  }
  client.request(
    { method:"PUT"
    , endpoint:path
    , body:data
    } ,
    function(err, data){
      //since the point of this app is to display the result that is given back
      //by the API, we want to do the same thing regardless of the err
      view.getBody(response, prepareOutput(data));
    }    
  );
}

function _delete(response, client, view, path) {
  client.request(
    { method:"DELETE"
    , endpoint:path
    } ,
    function(err, data){
      //since the point of this app is to display the result that is given back
      //by the API, we want to do the same thing regardless of the err
      view.getBody(response, prepareOutput(data));
    }    
  );
}

function _login(response, client, view, username, password) {
  client.login(username, password,
    function(err, data){
      //since the point of this app is to display the result that is given back
      //by the API, we want to do the same thing regardless of the err
      view.getBody(response, prepareOutput(data));
    }    
  );
}

function prepareOutput(output){
  return '<pre>'+JSON.stringify(output, null, 2)+'</pre>'; 
}
exports.main = main;
