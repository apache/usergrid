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
* 
*  Some code patterns were pulled from http://www.nodebeginner.org/.  Thank you!
*/
var sys = require('util');

var server = require("./server");
var router = require("./router");
var controller = require("./controller");


var handle = {}
handle["/"] = controller.main;
handle["/main"] = controller.main;

server.start(router.route, handle);

