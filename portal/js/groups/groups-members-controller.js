'use strict'

AppServices.Controllers.controller('GroupsMembersCtrl', ['ug', '$scope', '$rootScope', '$location',
  function (ug, $scope, $rootScope, $location) {

    $scope.membersSelected = 'active';
    $scope.previous_display = 'none';
    $scope.next_display = 'none';
    $scope.user = '';
    $scope.master = '';
    $scope.hasMembers = false


    //todo find others and combine this into one controller
    ug.getUsersTypeAhead();

    $scope.usersTypeaheadValues = [];
    $scope.$on('users-typeahead-received', function(event, users) {
      $scope.usersTypeaheadValues = users;
      $scope.applyScope();
    });

    $scope.addGroupToUserDialog = function(modalId){
      if ($scope.user) {
        var path =  $rootScope.selectedGroup.get('path');
        ug.addUserToGroup($scope.user.uuid, path);
        $scope.user = '';
        $scope.hideModal(modalId)
      } else {
        $rootScope.$broadcast('alert', 'error', 'Please select a user.');
      }
    };

    $scope.removeUsersFromGroupDialog = function(modalId){
      $scope.deleteEntities($scope.groupsCollection.users, 'group-update-received', 'Error removing user from group');
      $scope.hideModal(modalId)
    };

    $scope.get = function() {
      if(!$rootScope.selectedGroup.get){
        return;
      }
      var options = {
        type:'groups/'+$rootScope.selectedGroup.get('path') +'/users'
      }
      $scope.groupsCollection.addCollection('users', options, function(err) {
          $scope.groupMembersSelected = false;
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error getting users for group');
        } else {
          $scope.hasMembers =  $scope.groupsCollection.users._list.length > 0;
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
      if ($scope.groupsCollection.users.hasPreviousPage()) {
        $scope.previous_display = 'block';
      }

      if($scope.groupsCollection.users.hasNextPage()) {
        $scope.next_display = 'block';
      }
    }

    if (!$rootScope.selectedGroup) {
      $location.path('/groups');
      return;
    } else {
      $scope.get();
    }

    $scope.getPrevious = function () {
      $scope.groupsCollection.users.getPreviousPage(function(err) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error getting previous page of users');
        }
        $scope.checkNextPrev();
        if(!$rootScope.$$phase) {
          $rootScope.$apply();
        }
      });
    };

    $scope.getNext = function () {
      $scope.groupsCollection.users.getNextPage(function(err) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error getting next page of users');
        }
        $scope.checkNextPrev();
        if(!$rootScope.$$phase) {
          $rootScope.$apply();
        }
      });
    };

    $scope.$on('group-update-received', function(event) {
      $scope.get();
    });

    $scope.$on('user-added-to-group-received', function(event) {
      $scope.get();
    });


  }]);