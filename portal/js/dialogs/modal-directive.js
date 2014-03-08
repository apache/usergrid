'use strict';

AppServices.Directives.directive('bsmodal', ["$rootScope", function ($rootScope) {
  return{
    restrict: 'ECA',
    scope: {
      title: '@title',
      buttonid:'=buttonid',
      footertext: '=footertext',
      closelabel: '=closelabel'
    },
    transclude: true,
    templateUrl: 'dialogs/modal.html',
    replace: true,
    link: function linkFn(scope, lElement, attrs, parentCtrl) {
      scope.title = attrs.title;
      scope.footertext = attrs.footertext;
      scope.closelabel = attrs.closelabel;
      scope.close = attrs.close;
      scope.extrabutton = attrs.extrabutton;
      scope.extrabuttonlabel = attrs.extrabuttonlabel;
      scope.buttonId = attrs.buttonid;


      scope.closeDelegate = function(attr){
        //always call method in parent controller scope
        scope.$parent[attr](attrs.id,scope)
      }

      scope.extraDelegate = function(attr){
        if(scope.dialogForm.$valid){
          console.log(parentCtrl);
          //always call method in parent controller scope
          scope.$parent[attr](attrs.id)
        }else{
          $rootScope.$broadcast('alert','error','Please check your form input and resubmit.')
        }
      }
    }
  }
}]);
