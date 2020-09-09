angular.module('wasdi.wapSlider', [])
    .directive('wapslider', function () {
        "use strict";
        return{
            restrict : 'EAC',
            templateUrl:"directives/wasdiApps/wapSlider/wapSlider.html",
            // * Text binding ('@' or '@?') *
            // * One-way binding ('<' or '<?') *
            // * Two-way binding ('=' or '=?') *
            // * Function binding ('&' or '&?') *
            scope :{
                value:"=",
                min:"=",
                max:"="
            },
            link: function(scope, elem, attrs)
            {
                scope.sliderOptions = {
                    showSelectionBar: true,
                    floor: scope.min,
                    ceil: scope.max,
                    showTicksValues: true
                }
            }
        };
    });
