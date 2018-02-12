angular.module('wasdi.ToggleSwitch', [])
    .directive('toggleswitch', function () {
        "use strict";
        return{
            restrict:"E",
            scope :{
                // inputMatrix:'=matrix',
                // inputBorderColor:"@borderColor",
                // inputSide:"@side",
                // inputCursor:"@cursor"
                // * Text binding ('@' or '@?') *
                // * One-way binding ('<' or '<?') *
                // * Two-way binding ('=' or '=?') *
                // * Function binding ('&' or '&?') *
            },

            templateUrl:"directives/Toggle/ToggleSwitchView.html",
            link: function(scope, elem, attrs) {
                // console.log("ci sono");
                // scope.inputMatrix[0][0].click();
            }
        };
    });