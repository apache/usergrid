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
function main(querydata, response, usergrid) {            
  var view = require("./view"); 
  
  switch (querydata.action_type) {
    case 'get':
      _get(response, usergrid, view, querydata.path);
      break;
    case 'post':
      _post(response, usergrid, view, querydata.path, querydata.data);
      break;
    case 'put':
      _put(response, usergrid, view, querydata.path, querydata.data);
      break;
    case 'delete':
      _delete(response, usergrid, view, querydata.path);
      break;
    case 'login':
      _login(response, usergrid, view, querydata.username, querydata.password);
      break;
    default:
      view.getBody(response, '');     
      break;
  }
}

function _get(response, usergrid, view, path) {
  usergrid.ApiClient.runAppQuery(new usergrid.Query('GET', path, null, null,
     function(err, result) {
        //since the point of this app is to display the result that is given back
        //by the API, we want to do the same thing regardless of the err
        view.getBody(response, prepareOutput(result));  
     }
  ));
}

function _post(response, usergrid, view, path, data) {
  if (data) { 
    data = JSON.parse(data); 
  }
  usergrid.ApiClient.runAppQuery(new usergrid.Query('POST', path, data, null,
     function(err, result) {
       //since the point of this app is to display the result that is given back
        //by the API, we want to do the same thing regardless of the err
       view.getBody(response, prepareOutput(result));  
     }
  ));
}

function _put(response, usergrid, view, path, data) {
  if (data) { 
    data = JSON.parse(data); 
  }
  usergrid.ApiClient.runAppQuery(new usergrid.Query('PUT', path, data, null,
    function(err, result) {
       //since the point of this app is to display the result that is given back
        //by the API, we want to do the same thing regardless of the err
       view.getBody(response, prepareOutput(result));  
     }
  ));
}

function _delete(response, usergrid, view, path) {
  usergrid.ApiClient.runAppQuery(new usergrid.Query('DELETE', path, null, null,
    function(err, result) {
       //since the point of this app is to display the result that is given back
        //by the API, we want to do the same thing regardless of the err
       view.getBody(response, prepareOutput(result));  
     } 
  ));
}

function _login(response, usergrid, view, username, password) {
  usergrid.ApiClient.logInAppUser(username, password,
    function (err, result, user) {
      //since the point of this app is to display the result that is given back
      //by the API, we want to do the same thing regardless of the err
      view.getBody(response, prepareOutput(result));  
      
      //just for kicks, we will save a timestamp in the session, so you can see how to do it
      var timestamp = new Date().getTime()
      usergrid.session.setItem('useless_timestamp', timestamp);
      
      //this login call pulled back a user object and a token, so we need to save the session
      usergrid.session.save_session(response, 
        function(err, result){
          if (err) {
            console.log("Could not save session...");
          } else {
            console.log("Session saved...");
          }
        })               
    }
  );   
}

function prepareOutput(output){
  return '<pre>'+JSON.stringify(output, null, 2)+'</pre>'; 
}
exports.main = main;
