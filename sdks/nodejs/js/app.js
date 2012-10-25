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
*  @file app.js
*  @author Rod Simpson (rod@apigee.com)
*
*  This file contains handlers for display
*/
$(document).ready(function () {
   
   //bind the show buttons
   $('#show-get').bind('click', showGet);
   function showGet() {
      $('#main').hide();
      $('#main-menu').show();
      $('#get-page').show();
   }
   
   $('#show-post').bind('click', showPost);
   function showPost() {
      $('#main').hide();
      $('#main-menu').show();
      $('#post-page').show();
   }

   $('#show-put').bind('click', showPut);
   function showPut() {
      $('#main').hide();
      $('#main-menu').show();
      $('#put-page').show();
   }

   $('#show-delete').bind('click', showDelete);
   function showDelete() {
      $('#main').hide();
      $('#main-menu').show();
      $('#delete-page').show();
   }

   $('#show-login').bind('click', showLogin);
   function showLogin() {
      $('#main').hide();
      $('#main-menu').show();
      $('#login-page').show();
   }

   $('#run-get').bind('click', function() {
      $('#get-form').submit();
   });

   $('#run-post').bind('click', function() {
      $('#post-form').submit();
   });

   $('#run-put').bind('click', function() {
      $('#put-form').submit();
   });

   $('#run-delete').bind('click', function() {
      $('#delete-form').submit();
   });
   
   $('#run-login').bind('click', function() {
      $('#login-form').submit();
   });

   //bind the create new dog button
   $('#main-menu').bind('click', showMainForm);
   function showMainForm() {
      $('#get-page').hide();
      $('#post-page').hide();
      $('#put-page').hide();
      $('#delete-page').hide();
      $('#login-page').hide();
      $('#main').show();
      $("#response").html('');
   }
});