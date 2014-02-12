'use strict';

AppServices.Directives.directive('alerti', ["$rootScope","$timeout", function ($rootScope,$timeout) {
  return{
    restrict: 'ECA',
    scope: {
      type: '=type',
      closeable: '@closeable',
      index: '&index'
    },
    template: '<div class="alert" ng-class="type && \'alert-\' + type">' +
              '    <button ng-show="closeable" type="button" class="close" ng-click="closeAlert(index)">&times;</button>' +
              '    <i ng-if="type === \'warning\'" class="pictogram pull-left" style="font-size:3em;line-height:0.4">&#128165;</i>' +
      '    <i ng-if="type === \'info\'" class="pictogram pull-left">&#8505;</i>' +
      '    <i ng-if="type === \'error\'" class="pictogram pull-left">&#9889;</i>' +
      '    <i ng-if="type === \'success\'" class="pictogram pull-left">&#128077;</i>' +
      '<div ng-transclude></div>' +
              '</div>',
    replace: true,
    transclude: true,
    link: function linkFn(scope, lElement, attrs) {

//      if($rootScope.alertClick){

      $timeout(function(){lElement.addClass('fade-out')},4000)

      lElement.click(function(){

        if(attrs.index){
          scope.$parent.closeAlert(attrs.index);
        }

      });

//      }
      //need to do a 10ms timeout to apply animation :(
      setTimeout(function(){lElement.addClass('alert-animate');},10)
    }
  }
}])