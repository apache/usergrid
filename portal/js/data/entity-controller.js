'use strict'

AppServices.Controllers.controller('EntityCtrl', ['ug', '$scope', '$rootScope', '$location',
  function (ug, $scope, $rootScope, $location) {

    if (!$rootScope.selectedEntity) {
      $location.path('/data');
      return;
    }

    $scope.entityUUID  = $rootScope.selectedEntity.get('uuid');
    $scope.entityType  = $rootScope.selectedEntity.get('type');
    var tempjson  = $rootScope.selectedEntity.get();

    //rip out the system elements.  Stringify first because we don't want to rip them out of the actual object
    var queryBody = JSON.stringify(tempjson, null, 2);
    queryBody = JSON.parse(queryBody);
    delete queryBody.metadata;
    delete queryBody.uuid;
    delete queryBody.created;
    delete queryBody.modified;
    delete queryBody.type;

    $scope.queryBody = JSON.stringify(queryBody, null, 2);

    $scope.validateJson = function() {
      var queryBody = $scope.queryBody;

      try {
        queryBody = JSON.parse(queryBody);
      } catch (e) {
        $rootScope.$broadcast('alert', 'error', 'JSON is not valid');
        return false;
      }

      queryBody = JSON.stringify(queryBody,null,2);

      $rootScope.$broadcast('alert','success', 'JSON is valid');

      $scope.queryBody = queryBody;
      return true;

    }

    $scope.saveEntity = function(){
      if (!$scope.validateJson()) {
        return false;
      }
      var queryBody = $scope.queryBody;
      queryBody = JSON.parse(queryBody);
      $rootScope.selectedEntity.set(); //clears out all entities
      $rootScope.selectedEntity.set(queryBody);
      $rootScope.selectedEntity.set('type', $scope.entityType);
      $rootScope.selectedEntity.set('uuid', $scope.entityUUID);
      $rootScope.selectedEntity.save(function(err, data){
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error: ' + data.error_description);
        } else {
          $rootScope.$broadcast('alert', 'success', 'entity saved');
        }

      });
    }





  }]);