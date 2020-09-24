angular.module('wasdi.angularLightSlider', [])
    .directive('angularlightslider', function () {
        "use strict";
        return {
            restrict: 'A',
            link: function(scope, element, attrs) {
                if (scope.$last) {
                    // ng-repeat is completed
                    element.parent().lightSlider(scope.$eval(attrs.angularlightslider));
                }
            }
        };
    });
