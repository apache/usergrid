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
   Usergrid.ApiClient.init('Apigee', 'dogs');

   //make a new "dogs" Collection
   dogs = new Usergrid.Collection('dogs');

   //bind the next button to the proper method in the collection object
   $('#next-button').bind('click', function() {
      dogs.getNextPage();
   });

   //bind the previous button to the proper method in the collection object
   $('#previous-button').bind('click', function() {
      dogs.getPreviousPage();
   });

   //bind the new button to show the "create new dog" form
   $('#new-dog-button').bind('click', function() {
      $('#dogs-list').hide();
      $('#new-dog').show();
   });

   //bind the create new dog button
   $('#create-dog').bind('click', function() {
      newdog();
   });

   //bind the create new dog button
   $('#cancel-create-dog').bind('click', function() {
      $('#new-dog').hide();
      $('#dogs-list').show();
      getdogs();
   });

   function getdogs() {
      //add an additional parameter so last created item is shown first
      dogs.setQueryParams({'ql':'order by created DESC'});
      dogs.get(
         function() {
            //first empty out all the current dogs in the list
            $('#mydoglist').empty();
            //then hide the next / previous buttons
            $('#next-button').hide();
            $('#previous-button').hide();
            //iterate through all the items in this "page" of data
            while(dogs.hasNextEntity()) {
               //get a reference to the dog
               var dog = dogs.getNextEntity();
               //display the dog in the list
               $('#mydoglist').append('<li>'+ dog.get('name') + '</li>');
            }
            //if there is more data, display a "next" button
            if (dogs.hasNextPage()) {
               //show the button
               $('#next-button').show();
            }
            //if there are previous pages, show a "previous" button
            if (dogs.hasPreviousPage()) {
               //show the button
               $('#previous-button').show();
            }
         },
         function () { alert('error'); }
      );
   }

   function newdog() {
      $('#create-dog').addClass("disabled");
      //get the values from the form
      var name = $("#name").val();

      //make turn off all hints and errors
      $("#name-help").hide();
      $("#name-control").removeClass('error');

      //make sure the input was valid
      if (Usergrid.validation.validateName(name, function (){
            $("#name").focus();
            $("#name-help").show();
            $("#name-control").addClass('error');
            $("#name-help").html(Usergrid.validation.getNameAllowedChars());
            $('#create-dog').removeClass("disabled");})
         ) {
         //all is well, so make the new dog
         var dog = new Usergrid.Entity('dogs');
         //set its name
         dog.set('name', name);
         //save it to the database
         dogs.addNewEntity(
            //the new entity
            dog,
            //success callback function
            function() {
               //the save worked, so hide the new dog form
               $('#new-dog').hide();
               //then show the dogs list
               $('#dogs-list').show();
               //then call the function to get the list again
               getdogs();
               //finally enable the button so the form will be ready for next time
               $('#create-dog').removeClass("disabled");
            },
            //error callback function
            function() {
               //let the user know there was a problem
               alert('Oops! There was an error creating the dog.');
               //enable the button so the form will be ready for next time
               $('#create-dog').removeClass("disabled");
            }
         );
      }

   }

   //call the getdogs function to populate the list initially
   getdogs();
});