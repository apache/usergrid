'use strict';
AppServices.Directives.directive('pageTitle', ['$rootScope','ug', function ($rootScope,ug) {
  return{
    restrict: 'E',
    transclude: true,
    templateUrl:'global/page-title.html',
    link: function linkFn(scope, lElement, attrs) {
      scope.title = attrs.title;
      scope.icon = attrs.icon;
      scope.showHelp = function () {
        $('#need-help').modal('show');
      };
      scope.sendHelp = function () {
        data.jsonp_raw('apigeeuihelpemail', '', {useremail: $rootScope.userEmail}).then(
          function () {
            $rootScope.$broadcast('alert', 'success', 'Email sent. Our team will be in touch with you shortly.');
          },
          function () {
            $rootScope.$broadcast('alert', 'error', 'Problem Sending Email. Try sending an email to mobile@apigee.com.');
          }
        );
        $('#need-help').modal('hide');
      };
    }
  }
}]);
