
/*
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
*/

'use strict';
var util = require('./util');
describe('Shell ', function () {

  beforeEach(function () {
    util.login();
    util.navigate(browser.params.orgName,browser.params.appName1);
    browser.driver.get(browser.baseUrl + '#!/shell');
  });

  describe('do a post and delete', function () {
    it('should add and then delete', function () {
      var dateString = Date.now().toString();
      //browser.driver.get(browser.baseUrl + '#!/shell');

      //Make sure shell window is displayed
      browser.wait(function(){
        return element(by.id("shell-input")).isDisplayed();
      });
      //POST:::do a post to add a "/tests" entity
      element(by.id("shell-input")).isDisplayed().then(function(){
        element(by.id('shell-input')).clear();
        element(by.id('shell-input')).sendKeys('post /tests {"name":"' + dateString +'"}', protractor.Key.RETURN);
      });
      //make sure call has returned before proceeding
      browser.wait(function(){
        return element(by.id('lastshelloutput')).getInnerHtml().then(function(text) {
          return text.indexOf('{') === 0;
        });
      });
      //verify that return values are as expected
      element(by.id('lastshelloutput')).getInnerHtml().then(function(text){
        var json = JSON.parse(text);
        var entity = json.entities[0]["name"];
        expect(entity).toEqual(dateString);
      });

      //PUT:::clear the input field and then update the entity with a PUT command
      var newfield = "newfield";
      element(by.id("shell-input")).isDisplayed().then(function(){
        element(by.id('shell-input')).clear();
        element(by.id('shell-input')).sendKeys('put /tests/' + dateString +' {"'+newfield+'":"'+newfield+'"}', protractor.Key.RETURN);
      });
      //make sure call finished before proceeding
      browser.wait(function(){
        return element(by.id('lastshelloutput')).getInnerHtml().then(function(text) {
          return text.indexOf('{') === 0;
        });
      });
      //verify return values are good
      element(by.id('lastshelloutput')).getInnerHtml().then(function(text){
        var json = JSON.parse(text);
        var entity = json.entities[0][newfield];
        expect(entity).toEqual(newfield);
      });

      //GET:::clear input field, then do a GET to make sure entity is returned properly
      element(by.id("shell-input")).isDisplayed().then(function(){
        element(by.id('shell-input')).clear();
        element(by.id('shell-input')).sendKeys('get /tests/' + dateString, protractor.Key.RETURN);
      });
      //make sure call has finished before proceeding
      browser.wait(function(){
        return element(by.id('lastshelloutput')).getInnerHtml().then(function(text) {
          return text.indexOf('{') === 0;
        });
      });
      //verify that the output is correct
      element(by.id('lastshelloutput')).getInnerHtml().then(function(text){
        var json = JSON.parse(text);
        var field = json.entities[0]["name"];
        expect(field).toEqual(dateString);
        field = json.entities[0][newfield];
        expect(field).toEqual(newfield);
      });


      //DELETE:::clear the input field and then delete the entity
      element(by.id("shell-input")).isDisplayed().then(function(){
        element(by.id('shell-input')).clear();
        element(by.id('shell-input')).sendKeys('delete /tests/' + dateString, protractor.Key.RETURN);
      });
      //make sure the call finished before proceeding
      browser.wait(function(){
        return element(by.id('lastshelloutput')).getInnerHtml().then(function(text) {
          return text.indexOf('{') === 0;
        });
      });
      //make sure the output from the delete was good
      element(by.id('lastshelloutput')).getInnerHtml().then(function(text){
        var json = JSON.parse(text);
        var action = json.action;
        expect(action).toEqual('delete');
        var entity = json.entities[0]["name"];
        expect(entity).toEqual(dateString);
      });


      //GET:::clear input field, then do a GET to make sure entity has indeed been deleted
      element(by.id("shell-input")).isDisplayed().then(function(){
        element(by.id('shell-input')).clear();
        element(by.id('shell-input')).sendKeys('get /tests/' + dateString, protractor.Key.RETURN);
      });
      //make sure call has finished before proceeding
      browser.wait(function(){
        return element(by.id('lastshelloutput')).getInnerHtml().then(function(text) {
          return text.indexOf('{') === 0;
        });
      });
      //verify that the output is correct
      element(by.id('lastshelloutput')).getInnerHtml().then(function(text){
        var json = JSON.parse(text);
        var field = json.error;
        expect(field).toEqual('service_resource_not_found');
      });



    });
  });
});