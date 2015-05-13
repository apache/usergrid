/**
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

AppServices.Controllers.controller('DataCtrl', ['ug', '$scope', '$rootScope', '$location',
  function (ug, $scope, $rootScope, $location) {

    var init = function () {
      $scope.verb = 'GET';
      $scope.display = '';

      $scope.queryBodyDetail = {};

      $scope.queryBodyDisplay = 'none';
      $scope.queryLimitDisplay = 'block';
      $scope.queryStringDisplay = 'block';
      $scope.entitySelected = {};
      $scope.newCollection = {name:"", entity:'{"name":"test"}'};
      $rootScope.queryCollection = {};
      $scope.data = {};
      $scope.data.queryPath = '';
      $scope.data.queryBody = '{ "name":"value" }';
      $scope.data.searchString = '';
      $scope.data.queryLimit = '';
    };
    var runQuery = function(verb){
      $scope.loading = true;
      //get the parameters
      var queryPath = $scope.removeFirstSlash($scope.data.queryPath || '');
      var searchString = $scope.data.searchString || '';
      var queryLimit = $scope.data.queryLimit || '';
      var body = JSON.parse($scope.data.queryBody || '{}');

      if (verb == 'POST' && $scope.validateJson(true)) {
        ug.runDataPOSTQuery(queryPath, body);
      } else if (verb == 'PUT' && $scope.validateJson(true)) {
        ug.runDataPutQuery(queryPath, searchString, queryLimit, body);
      } else if (verb == 'DELETE') {
        ug.runDataDeleteQuery(queryPath, searchString, queryLimit);
      } else {
        //GET
        ug.runDataQuery(queryPath, searchString, queryLimit);
      }
    };

    $scope.$on('top-collections-received', function(event, collectionList) {
      $scope.loading = false;
      var ignoredCollections = ['events'];
      //remove some elements from the collection
      ignoredCollections.forEach(function(ignoredCollection){
         collectionList.hasOwnProperty(ignoredCollection)  //important check that this is objects own property, not from prototype prop inherited
          && delete collectionList[ignoredCollection]
         ;
      });
      $scope.collectionList = collectionList;
      $scope.queryBoxesSelected = false;
      if(!$scope.queryPath){
        $scope.loadCollection('/'+$scope.collectionList[Object.keys($scope.collectionList).sort()[0]].name);
      }
      $scope.applyScope();
    });
    $scope.$on('error-running-query', function(event) {
      $scope.loading = false;
      runQuery('GET');
      $scope.applyScope();
    });


    $scope.$on('entity-deleted', function(event) {
      $scope.deleteLoading = false;
      $rootScope.$broadcast('alert','success', 'Entities deleted sucessfully');
      $scope.queryBoxesSelected = false;
      $scope.checkNextPrev();
      $scope.applyScope();

    });
    $scope.$on('entity-deleted-error', function(event) {
      $scope.deleteLoading = false;
      runQuery('GET');
      $scope.applyScope();
    });

    $scope.$on('collection-created',function(){
      $scope.newCollection.name = '';
      $scope.newCollection.entity = '{ "name":"value" }';
    });

    $scope.$on('query-received', function(event, collection) {
      $scope.loading = false;
      $rootScope.queryCollection = collection;
      ug.getIndexes($scope.data.queryPath);
      //$rootScope.$broadcast('alert','success', 'API call completed sucessfully');

      $scope.setDisplayType();

      $scope.checkNextPrev();
      $scope.applyScope();
      $scope.queryBoxesSelected = false;

    });

    $scope.$on('query-error', function(event) {
      $scope.loading = false;
      $scope.applyScope();
      $scope.queryBoxesSelected = false;
    });

    $scope.$on('indexes-received', function(event, indexes) {
      //todo - do something with the indexes
      var fred = indexes;
    });

    $scope.$on('app-changed',function(){
      init();
    });

    $scope.setDisplayType = function() {
      $scope.display = 'generic';
    }

    $scope.deleteEntitiesDialog = function(modalId){
      $scope.deleteLoading = false;
      $scope.deleteEntities($rootScope.queryCollection, 'entity-deleted', 'error deleting entity');

      $scope.hideModal(modalId)
    };

    $scope.newCollectionDialog = function(modalId){
      if ($scope.newCollection.name) {
        ug.createCollection($scope.newCollection.name);
        ug.getTopCollections();
        $rootScope.$broadcast('alert', 'success', 'Collection created successfully.');
        $scope.hideModal(modalId)
      } else {
        $rootScope.$broadcast('alert', 'error', 'You must specify a collection name.');
      }
    };

    $scope.newCollectionWithEntityDialog = function(modalId){
      if(!($scope.newCollection.name && $scope.newCollection.name.length>0)){
        $rootScope.$broadcast('alert', 'error', 'You must specify a collection name.');
      }else if(!($scope.newCollection.entity && $scope.newCollection.entity.length>0)){
        $rootScope.$broadcast('alert', 'error', 'You must specify JSON data for the new entity.');
      }else if(!$scope.validateEntityData(true)){
        //Do Nothing. Alert is thrown in validateEntityData
      }else{
        ug.createCollectionWithEntity($scope.newCollection.name, $scope.newCollection.entity);
        ug.getTopCollections();
        $rootScope.$broadcast('alert', 'success', 'Collection created successfully.');
        $scope.hideModal(modalId)
      }
    };

    $scope.addToPath = function(uuid){
      $scope.data.queryPath = '/' + $rootScope.queryCollection._type + '/' + uuid;
    }

    $scope.removeFromPath = function(){
      $scope.data.queryPath = '/' + $rootScope.queryCollection._type;
    }

    $scope.isDeep = function(item){
      //can be better, just see if object for now
      return (Object.prototype.toString.call(item) === "[object Object]");
    };

    //get collection list
    $scope.loadCollection = function(type) {
      $scope.data.queryPath = '/'+type.substring(1,type.length);
      $scope.data.searchString = '';
      $scope.data.queryLimit = '';
      $scope.data.body = '{ "name":"value" }';
      $scope.selectGET();
      $scope.applyScope();
      $scope.run();
    }
    $scope.selectGET = function() {
      $scope.queryBodyDisplay = 'none';
      $scope.queryLimitDisplay = 'block';
      $scope.queryStringDisplay = 'block';
      $scope.verb = 'GET';
    }
    $scope.selectPOST = function() {
      $scope.queryBodyDisplay = 'block';
      $scope.queryLimitDisplay = 'none';
      $scope.queryStringDisplay = 'none';
      $scope.verb = 'POST';
    }
    $scope.selectPUT = function() {
      $scope.queryBodyDisplay = 'block';
      $scope.queryLimitDisplay = 'block';
      $scope.queryStringDisplay = 'block';
      $scope.verb = 'PUT';
    }
    $scope.selectDELETE = function() {
      $scope.queryBodyDisplay = 'none';
      $scope.queryLimitDisplay = 'block';
      $scope.queryStringDisplay = 'block';
      $scope.verb = 'DELETE';
    }

    $scope.validateEntityData = function(skipMessage) {
      var queryBody = $scope.newCollection.entity;

      try {
        queryBody = JSON.parse(queryBody);
      } catch (e) {
        $rootScope.$broadcast('alert', 'error', 'JSON is not valid');
        return false;
      }

      queryBody = JSON.stringify(queryBody,null,2);

      !skipMessage && $rootScope.$broadcast('alert','success', 'JSON is valid');

      $scope.newCollection.entity = queryBody;
      return true;
    }

    $scope.validateJson = function(skipMessage) {
      var queryBody = $scope.data.queryBody;

      try {
        queryBody = JSON.parse(queryBody);
      } catch (e) {
        $rootScope.$broadcast('alert', 'error', 'JSON is not valid');
        return false;
      }

      queryBody = JSON.stringify(queryBody,null,2);

      !skipMessage && $rootScope.$broadcast('alert','success', 'JSON is valid');

      $scope.data.queryBody = queryBody;
      return true;
    }

    $scope.isValidJSON = function(data) {
      try {
        data = JSON.parse(data);
      } catch (e) {
        return false;
      }
      return true;
    }

    $scope.formatJSON = function(data) {
      try {
        data = JSON.parse(data);
      } catch (e) {
        return data;
      }
      return JSON.stringify(data,null,2);
    }


    $scope.saveEntity = function(entity){
      if (!$scope.validateJson()) {
        return false;
      }
      var queryBody = entity._json;
      queryBody = JSON.parse(queryBody);
      $rootScope.selectedEntity.set(); //clears out all entities
      $rootScope.selectedEntity.set(queryBody);
      $rootScope.selectedEntity.set('type', entity._data.type);
      $rootScope.selectedEntity.set('uuid', entity._data.uuid);
      $rootScope.selectedEntity.save(function(err, data){
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error: ' + data.error_description);
        } else {
          $rootScope.$broadcast('alert', 'success', 'entity saved');
        }

      });
    }

    $scope.run = function() {
      //clear the current data
      $rootScope.queryCollection = '';

      var verb = $scope.verb;
      runQuery(verb);
    }

    $scope.hasProperty = function(prop){
      //checks each object in array for a given property - used for hiding/showing UI data in table
      var retval = false;
      if(typeof $rootScope.queryCollection._list !== 'undefined'){
        angular.forEach($rootScope.queryCollection._list, function(value, key){
          //no way to break angular loop :(
          if(!retval){
            if(value._data[prop]){
              retval = true;
            }
          }
        });
      }
      return retval;
    }

    $scope.resetNextPrev = function() {
      $scope.previous_display = 'none';
      $scope.next_display = 'none';
    }

    $scope.checkNextPrev = function() {
      $scope.resetNextPrev();
      if ($rootScope.queryCollection.hasPreviousPage()) {
        $scope.previous_display = 'default';
      }
      if($rootScope.queryCollection.hasNextPage()) {
        $scope.next_display = 'default';
      }
    }

    $scope.selectEntity = function(uuid, addToPath){
      $rootScope.selectedEntity = $rootScope.queryCollection.getEntityByUUID(uuid);
      if (addToPath) {
        $scope.addToPath(uuid);
      } else {
        $scope.removeFromPath();
      }
    }

    $scope.getJSONView = function(entity){
      var tempjson  = entity.get();

      //rip out the system elements.  Stringify first because we don't want to rip them out of the actual object
      var queryBody = JSON.stringify(tempjson, null, 2);
      queryBody = JSON.parse(queryBody);
      delete queryBody.metadata;
      delete queryBody.uuid;
      delete queryBody.created;
      delete queryBody.modified;
      delete queryBody.type;

      $scope.queryBody = JSON.stringify(queryBody, null, 2);
    }

    $scope.getPrevious = function () {
      $rootScope.queryCollection.getPreviousPage(function(err) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error getting previous page of data');
        }
        $scope.checkNextPrev();
        $scope.applyScope();
      });
    };

    $scope.getNext = function () {
      $rootScope.queryCollection.getNextPage(function(err) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error getting next page of data');
        }
        $scope.checkNextPrev();
        $scope.applyScope();
      });
    };

    init();
    //persistent vars (stay around when you drill down to an entity detail view)
    $rootScope.queryCollection = $rootScope.queryCollection  || {};
    $rootScope.selectedEntity = {};
    if ($rootScope.queryCollection && $rootScope.queryCollection._type) {
      $scope.loadCollection($rootScope.queryCollection._type);
      $scope.setDisplayType();
    }
    ug.getTopCollections();
    $scope.resetNextPrev();


  }]);
