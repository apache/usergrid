/**
*  dogs is a sample app  that is powered by Usergrid
*  This app shows how to use the Usergrid SDK to connect
*  to Usergrid, how to add entities, and how to page through
*  a result set of entities
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
*  This file contains the main program logic for Dogs.
*/
$(document).ready(function () {
   //first set the org / app path (must be orgname / appname or org id / app id - can't mix names and uuids!!)
 //  Usergrid.ApiClient.init('Apigee', 'sandbox');

 
  usergrid.client.init( 
    {
      "org":"apigee",
      "app":"sandbox"   
    }, 
    function(){
      //success! 
    },
    function(){
      //failure
    }
    
  );
 
 function init(properties, success, failure) {
   _org = properties.org;
   _app = properties.app;
   
   //get list of all collections, save locally
   getCollections( function(collections) {
       // collections retrieved
       _collections = collections;
       
       success();
     },
     function(){
       failure();
     });
   }
 }
 
});