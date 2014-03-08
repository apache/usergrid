'use strict'

AppServices.Controllers.controller('RolesGroupsCtrl', ['ug', '$scope', '$rootScope', '$location',
  function (ug, $scope, $rootScope, $location) {

    $scope.groupsSelected = 'active';
    $scope.previous_display = 'none';
    $scope.next_display = 'none';
    $scope.path = '';
    $scope.hasGroups = false;


    ug.getGroupsTypeAhead();

    $scope.groupsTypeaheadValues = [];
    $scope.$on('groups-typeahead-received', function(event, groups) {
      $scope.groupsTypeaheadValues = groups;
      $scope.applyScope();
    });

    $scope.addRoleToGroupDialog = function(modalId){
      if ($scope.path) {
        var name =  $rootScope.selectedRole._data.uuid;
        ug.addGroupToRole($scope.path, name);
        $scope.hideModal(modalId);
        $scope.path = '';
        $scope.title = '';
      } else {
        $rootScope.$broadcast('alert', 'error', 'You must specify a group.');
      }
    };

    $scope.setRoleModal = function(group){
      $scope.path = group.path;
      $scope.title = group.title;
    }

    $scope.removeGroupFromRoleDialog = function(modalId){
      var roleName =  $rootScope.selectedRole._data.uuid;
      var groups = $scope.rolesCollection.groups._list;
      for (var i=0;i<groups.length;i++) {
        if (groups[i].checked) {
          ug.removeUserFromGroup(groups[i]._data.path, roleName);
        }
      }
      $scope.hideModal(modalId);
    };

    $scope.get = function() {
      var options = {
        type:'roles/'+$rootScope.selectedRole._data.name +'/groups',
        qs:{ql:'order by title'}
      }
      $scope.rolesCollection.addCollection('groups', options, function(err) {
        $scope.roleGroupsSelected = false;
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error getting groups for role');
        } else {
          $scope.hasGroups = $scope.rolesCollection.groups._list.length;
          $scope.checkNextPrev();
          $scope.applyScope();
        }
      });
    }

    $scope.resetNextPrev = function() {
      $scope.previous_display = 'none';
      $scope.next_display = 'none';
    }
    $scope.checkNextPrev = function() {
      $scope.resetNextPrev();
      if ($scope.rolesCollection.groups.hasPreviousPage()) {
        $scope.previous_display = 'block';
      }

      if($scope.rolesCollection.groups.hasNextPage()) {
        $scope.next_display = 'block';
      }
    }

    if (!$rootScope.selectedRole) {
      $location.path('/roles');
      return;
    } else {
      $scope.get();
    }

    $scope.getPrevious = function () {
      $scope.rolesCollection.groups.getPreviousPage(function(err) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error getting previous page of groups');
        }
        $scope.checkNextPrev();
        $scope.applyScope();
      });
    };

    $scope.getNext = function () {

      $scope.rolesCollection.groups.getNextPage(function(err) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error getting next page of groups');
        }
        $scope.checkNextPrev();
        $scope.applyScope();
      });
    };

    $scope.$on('role-update-received', function(event) {
      $scope.get();
    });

}]);