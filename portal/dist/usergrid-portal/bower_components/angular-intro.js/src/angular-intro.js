var ngIntroDirective = angular.module('angular-intro', []);

/**
 * TODO: Use isolate scope, but requires angular 1.2: http://plnkr.co/edit/a2c14O?p=preview
 * See: http://stackoverflow.com/q/18796023/237209
 */

ngIntroDirective.directive('ngIntroOptions', ['$timeout', '$parse', function ($timeout, $parse) {

    return {
        restrict: 'A',
        link: function(scope, element, attrs) {

            scope[attrs.ngIntroMethod] = function(step) {

                var intro;

                if(typeof(step) === 'string') {
                    intro = introJs(step);
                } else {
                    intro = introJs();
                }

                intro.setOptions(scope.$eval(attrs.ngIntroOptions));

                if(attrs.ngIntroOncomplete) {
                    intro.oncomplete($parse(attrs.ngIntroOncomplete)(scope));
                }

                if(attrs.ngIntroOnexit) {
                    intro.onexit($parse(attrs.ngIntroOnexit)(scope));
                }

                if(attrs.ngIntroOnchange) {
                    intro.onchange($parse(attrs.ngIntroOnchange)(scope));
                }

                if(attrs.ngIntroOnbeforechange) {
                    intro.onbeforechange($parse(attrs.ngIntroOnbeforechange)(scope));
                }

                if(attrs.ngIntroOnafterchange) {
                    intro.onafterchange($parse(attrs.ngIntroOnafterchange)(scope));
                }

                if(typeof(step) === 'number') {
                    intro.goToStep(step).start();
                } else {
                    intro.start();
                }
            };

            if(attrs.ngIntroAutostart == 'true') {
                $timeout(function() {
                    $parse(attrs.ngIntroMethod)(scope)();
                });
            }
        }
    };
}]);
