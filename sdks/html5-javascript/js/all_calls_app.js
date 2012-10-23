/**
*  All Calls is a sample app  that is powered by Usergrid
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
*  @file app.js
*  @author Rod Simpson (rod@apigee.com)
*
*  This file contains the main program logic for All Calls App.
*/
$(document).ready(function () {
   //first set the org / app path (must be orgname / appname or org id / app id - can't mix names and uuids!!)
   Usergrid.ApiClient.init('Apigee', 'sandbox');

   //Usergrid.ApiClient.setApiUrl('http://api.usergrid.com/');
   
   //bind the show buttons
   $('#show-get').bind('click', function() {
      $('#main').hide();
      $('#main-menu').show();
      $('#get-page').show();
   });

   $('#show-post').bind('click', function() {
      $('#main').hide();
      $('#main-menu').show();
      $('#post-page').show();
   });

   $('#show-put').bind('click', function() {
      $('#main').hide();
      $('#main-menu').show();
      $('#put-page').show();
   });

   $('#show-delete').bind('click', function() {
      $('#main').hide();
      $('#main-menu').show();
      $('#delete-page').show();
   });

   $('#run-get').bind('click', function() {
      _get();
   });

   $('#run-post').bind('click', function() {
      _post();
   });

   $('#run-put').bind('click', function() {
      _put();
   });

   $('#run-delete').bind('click', function() {
      _delete();
   });

   

   //bind the create new dog button
   $('#main-menu').bind('click', function() {
      $('#get-page').hide();
      $('#post-page').hide();
      $('#put-page').hide();
      $('#delete-page').hide();
      $('#main').show();
      $("#response").html('');
   });


   function _get() {
      var path = $("#get-path").val();
      Usergrid.ApiClient.runAppQuery (new Usergrid.Query('GET', path, null, null,
         function(response) {
           var output = JSON.stringify(response);
           $("#response").html('<pre>'+output+'</pre>');
         },
         function (response) {
           $("#response").html('<pre>ERROR: '+response+'</pre>');
         }
      ));
   }

   function _post() {
      var path = $("#post-path").val();
      var data = $("#post-data").val();
      data = JSON.parse(data);
      Usergrid.ApiClient.runAppQuery (new Usergrid.Query('POST', path, data, null,
         function(response) {
           var output = JSON.stringify(response);
           $("#response").html('<pre>'+output+'</pre>');
         },
         function (response) {
           $("#response").html('<pre>ERROR: '+response+'</pre>');
         }
      ));
   }

   function _put() {
      var path = $("#put-path").val();
      var data = $("#put-data").val();
      data = JSON.parse(data);
      Usergrid.ApiClient.runAppQuery (new Usergrid.Query('PUT', path, data, null,
         function(response) {
           var output = JSON.stringify(response);
           $("#response").html('<pre>'+output+'</pre>');
         },
         function (response) {
           $("#response").html('<pre>ERROR: '+response+'</pre>');
         }
      ));
   }

    function _delete() {
      var path = $("#delete-path").val();
      Usergrid.ApiClient.runAppQuery (new Usergrid.Query('DELETE', path, null, null,
         function(response) {
           var output = JSON.stringify(response);
           $("#response").html('<pre>'+output+'</pre>');
         },
         function (response) {
           $("#response").html('<pre>ERROR: '+response+'</pre>');
         }
      ));
   }

});