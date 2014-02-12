'use strict';

AppServices.Directives.directive('appswitcher', ["$rootScope", function ($rootScope) {
  return{
    restrict: 'ECA',
    scope: "=",
    templateUrl: 'global/appswitcher-template.html',
    replace: true,
    transclude: true,
    link: function linkFn(scope, lElement, attrs) {

      var classNameOpen = 'open';
      $('ul.nav li.dropdownContainingSubmenu').hover(
        function () {
          $(this).addClass(classNameOpen);
        },
        function () {
          $(this).removeClass(classNameOpen);
        }
      );
      $('#globalNav > a').mouseover(globalNavDetail);
      $('#globalNavDetail').mouseover(globalNavDetail);
      $('#globalNavSubmenuContainer ul li').mouseover(
        function () {
          $('#globalNavDetail > div').removeClass(classNameOpen);
          $('#' + this.getAttribute('data-globalNavDetail')).addClass(classNameOpen);
        }
      );
      function globalNavDetail() {
        $('#globalNavDetail > div').removeClass(classNameOpen);
        $('#globalNavDetailApiPlatform').addClass(classNameOpen);
      }
    }
  }
}])