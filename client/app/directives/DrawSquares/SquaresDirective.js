/**
 * Created by a.corrado on 02/05/2017.
 */
angular.module('wasdi.SquaresDirective', [])
    .directive('squares', function () {
        "use strict";
        return{
            restrict:"E",
            scope :{
                inputMatrix:'=matrix',
                inputBorderColor:"@borderColor",
                inputSide:"@side",
                inputCursor:"@cursor"
                // * Text binding ('@' or '@?') *
                // * One-way binding ('<' or '<?') *
                // * Two-way binding ('=' or '=?') *
                // * Function binding ('&' or '&?') *
            },

            templateUrl:"directives/DrawSquares/SquaresView.html",
            link: function(scope, elem, attrs) {
                // console.log("ci sono");
                // scope.inputMatrix[0][0].click();
            }
        };
    });