'use strict'

AppServices.Controllers.controller('UsersGroupsCtrl', ['ug', '$scope', '$rootScope', '$location',
  function (ug, $scope, $rootScope, $location) {

    $scope.userGroupsCollection = {};
    $scope.groups_previous_display = 'none';
    $scope.groups_next_display = 'none';
    $scope.groups_check_all = '';
    $scope.groupsSelected = 'active';
    $scope.title = '';
    $scope.master = '';
    $scope.hasGroups = false;

    var init = function () {
      $scope.name = '';
      if (!$rootScope.selectedUser) {
        $location.path('/users');
        return;
      } else {
        ug.getGroupsForUser($rootScope.selectedUser.get('uuid'));
      }

      ug.getGroupsTypeAhead();
    };
    init();

    //---------modals
    $scope.addUserToGroupDialog = function(modalId){
      if ($scope.path) {
        var username =  $rootScope.selectedUser.get('uuid');
        ug.addUserToGroup(username, $scope.path);
        $scope.hideModal(modalId);
        $scope.path = '';
        $scope.title = '';
      } else {
        $rootScope.$broadcast('alert', 'error', 'You must specify a group.');
      }
    }
    $scope.selectGroup = function(group){
      $scope.path = group.path;
      $scope.title = group.title;
    }

    $scope.leaveGroupDialog = function(modalId){
      $scope.deleteEntities($scope.userGroupsCollection, 'user-left-group', 'error removing user from group');
      $scope.hideModal(modalId);
    }


//    $scope.groupsTypeaheadValues = [];



    $scope.$on('user-groups-received', function(event, groups) {
      $scope.userGroupsCollection = groups;
      $scope.userGroupsSelected =false;
      $scope.hasGroups = groups._list.length;
      $scope.checkNextPrev();
      if(!$rootScope.$$phase) {
        $rootScope.$apply();
      }
    });


    $scope.resetNextPrev = function() {
      $scope.previous_display = 'none';
      $scope.next_display = 'none';
    }
    $scope.checkNextPrev = function() {
      $scope.resetNextPrev();
      if ($scope.userGroupsCollection.hasPreviousPage()) {
        $scope.previous_display = 'block';
      }

      if($scope.userGroupsCollection.hasNextPage()) {
        $scope.next_display = 'block';
      }
    }


    $rootScope.getPrevious = function () {
      $scope.userGroupsCollection.getPreviousPage(function(err) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error getting previous page of groups');
        }
        $scope.checkNextPrev();
        if(!$rootScope.$$phase) {
          $rootScope.$apply();
        }
      });
    };

    $rootScope.getNext = function () {

      $scope.userGroupsCollection.getNextPage(function(err) {
        if (err) {
          $rootScope.$broadcast('alert', 'error', 'error getting next page of groups');
        }
        $scope.checkNextPrev();
        if(!$rootScope.$$phase) {
          $rootScope.$apply();
        }
      });
    };

    $scope.$on('user-left-group', function(event) {
      $scope.checkNextPrev();
      $scope.userGroupsSelected = false;
      if(!$rootScope.$$phase) {
        $rootScope.$apply();
      }
      init();
    });

    $scope.$on('user-added-to-group-received', function(event) {
      $scope.checkNextPrev();
      if(!$rootScope.$$phase) {
        $rootScope.$apply();
      }
      init();
    });

  }]);