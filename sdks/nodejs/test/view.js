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
*  @file view.js
*  @author Rod Simpson (rod@apigee.com)
*
*  This file contains the main display code for the UI
*/
function getBody(response, output) {
  console.log('getting view...');
  response.writeHead(200, {"Content-Type": "text/html"});
  var content = '<!DOCTYPE html>'+
  '<html>'+
  '   <head>'+
  '      <title></title>'+
  '      <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">'+
  '      <link rel="stylesheet" href="/css/bootstrap-combined.min.css" />'+
  '      <link rel="stylesheet" href="/css/styles.css" />'+
  '      <script src="js/jquery.min.js" type="text/javascript"></script>'+
  '      <script src="js/app.js" type="text/javascript"></script>'+
  '   </head>'+
  '   <body>'+
  '      <div class="header">'+
  '         App Services (Usergrid) Node.js Example'+
  '      </div>'+
  '      <button class="btn btn-primary main-menu-button" id="main-menu">&lt;&lt; Main Menu</button>'+
  '      <div id="main" class="main">'+
  '         <h2>Select method to test:</h2>'+
  '         <button class="btn btn-primary" id="show-get" style="width: 90px;">GET</button>'+
  '         <button class="btn btn-primary" id="show-post" style="width: 90px;">POST</button>'+
  '         <button class="btn btn-primary" id="show-put" style="width: 90px;">PUT</button>'+
  '         <button class="btn btn-primary" id="show-delete" style="width: 90px;">DELETE</button>'+
  '         <button class="btn btn-primary" id="show-login" style="width: 90px;">LOG IN</button>'+
  '         <div id="output"></div>'+
  '      </div>'+
  '      <div id="get-page" class="form-block">'+
  '         <h2>GET</h2>'+
  '         <form name="get-form" id="get-form">'+
  '            <input name="action_type" type="hidden" value="get">'+
  '            <div id="name-control" class="control-group">'+
  '               <label class="control-label" for="path">Path</label>'+
  '               <div class="controls">'+
  '                  <input type="text" name="path" id="path" class="span4" value="users/fred" />'+
  '               </div>'+
  '            </div>'+ 
  '         </form>'+
  '         <button class="btn btn-primary" id="run-get" style="width: 90px;">Run Query</button>'+
  '         <br/>'+
  '         <br/>'+
  '         <p>'+
  '            To run a GET query against the API, enter the path you want to query, then push the "Run Query" button.  By default, the value is'+
  '            "users/fred", which will translate to a call to: https://api.usergrid.com/your-org/your-app/users/fred, and will retrieve the record for fred.'+
  '            <br/><br/>'+
  '            Hint: if you get an error here, it probably means "fred" doesn\'t exist.  Go to back to the main menu and choose "POST" to create a new "fred".'+
  '         </p>'+
  '      </div>'+
  '      <div id="post-page" class="form-block">'+
  '         <h2>POST</h2>'+
  '         <form name="post-form" id="post-form">'+
  '            <input name="action_type" type="hidden" value="post">'+
  '            <div id="name-control" class="control-group">'+
  '               <label class="control-label" for="post-path">Path</label>'+
  '               <div class="controls">'+
  '                  <input type="text" name="path" id="path" class="span4" value="users" />'+
  '               </div>'+
  '               <label class="control-label" for="data">Json Request Body</label>'+
  '               <div class="controls">'+
  '                  <input type="text" name="data" id="data" class="span4" value=\'{"username":"fred", "password":"barney"}\' />'+
  '               </div>'+
  '            </div>'+
  '         </form>'+
  '         <button class="btn btn-primary" id="run-post" style="width: 90px;">Run Query</button>'+
  '         <br/>'+
  '         <br/>'+
  '         <p>'+
  '            To run a POST query against the API, enter the path you want to call and the Json you want to send to the server, then push the "Run Query" button.'+
  '            By default, the path of users, and the Json request body of, "{"username":"fred"}" will create a new user called "fred".'+
  '            <br/><br/>'+
  '            Hint: if you get an error here, it probably means "fred" already exists.  Go to back to the main menu and choose "DELETE" to delete the "fred"'+
  '            entity, then come. back to this page to try again.'+
  '         </p>'+
  '      </div>'+
  '      <div id="put-page" class="form-block">'+
  '         <h2>PUT</h2>'+
  '         <form name="put-form" id="put-form">'+
  '            <input name="action_type" type="hidden" value="put">'+
  '            <div id="name-control" class="control-group">'+
  '               <label class="control-label" for="put-path">Path</label>'+
  '               <div class="controls">'+
  '                  <input type="text" name="path" id="path" class="span4" value="users/fred" />'+
  '               </div>'+
  '               <label class="control-label" for="data">Json Request Body</label>'+
  '               <div class="controls">'+
  '                  <input type="text" name="data" id="data" class="span4" value=\'{"othervalue":"12345"}\' />'+
  '               </div>'+
  '            </div>'+
  '         </form>'+
  '         <button class="btn btn-primary" id="run-put" style="width: 90px;">Run Query</button>'+
  '         <br/>'+
  '         <br/>'+
  '         <p>'+
  '            To run a PUT query against the API, enter the path you want to update and the Json you want to send to the server, then push the "Run Query" button.'+
  '            By default, the path of users, and the Json request body of, "{"username":"fred"}" will create a new user called "fred".'+
  '            <br/><br/>'+
  '            Hint: if you get an error here, it probably means "fred" doesn\'t exist.  Go to back to the main menu and choose "POST" to create a new "fred".'+
  '         </p>'+
  '      </div>'+
  '      <div id="delete-page" class="form-block">'+
  '         <h2>DELETE</h2>'+
  '         <form name="delete-form" id="delete-form">'+
  '            <input name="action_type" type="hidden" value="delete">'+
  '            <div id="name-control" class="control-group">'+
  '               <label class="control-label" for="path">Path</label>'+
  '               <div class="controls">'+
  '                  <input type="text" name="path" id="path" class="span4" value="users/fred" />'+
  '               </div>'+
  '            </div>'+
  '         </form>'+
  '         <button class="btn btn-primary" id="run-delete" style="width: 90px;">Run Query</button>'+
  '         <br/>'+
  '         <br/>'+
  '         <p>'+
  '            To run a DELETE query against the API, enter the path you want to delete, then push the "Run Query" button.  By default, the value is'+
  '            "users/fred", which will delete the entity located at: https://api.usergrid.com/your-org/your-app/users/fred .'+
  '            <br/><br/>'+
  '            Hint: if you get an error here, it probably means "fred" doesn\'t exist.  Go to back to the main menu and choose "POST" to create a new "fred".'+
  '         </p>'+
  '      </div>'+
  '      <div id="login-page" class="form-block">'+
  '         <h2>Log In</h2>'+
  '         <form name="login-form" id="login-form">'+
  '            <input name="action_type" type="hidden" value="login">'+
  '            <div id="name-control" class="control-group">'+
  '               <label class="control-label" for="username">Username</label>'+
  '               <div class="controls">'+
  '                  <input type="text" name="username" id="username" class="span4" value="fred"/>'+
  '               </div>'+ 
  '               <label class="control-label" for="password">Password</label>'+
  '               <div class="controls">'+
  '                  <input type="text" name="password" id="password" class="span4" value="barney"/>'+
  '               </div>'+
  '            </div>'+
  '         </form>'+
  '         <button class="btn btn-primary" id="run-login" style="width: 90px;">Run Query</button>'+
  '         <br/>'+
  '         <br/>'+
  '         <p>'+
  '            To run a login query against the API, enter the username and password, then push the "Run Query" button. '+
  '            <br/><br/>'+
  '            Hint: if you get an error here, it probably means either the user doesn\'t exist, or hasn\'t had the password set properly.  Go to back to the main menu and choose "POST" to create a new "fred".'+
  '         </p>'+
  '      </div>'+
  '      <div class="response-box">API RESPONSE:'+
  '      <div id="response">'+output+'</div>'+
  '      </div>'+
  '    </body>'+
  '</html>';  

  response.write(content);  
  response.end(); 
}
exports.getBody = getBody;