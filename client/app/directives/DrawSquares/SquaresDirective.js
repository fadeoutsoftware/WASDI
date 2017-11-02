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
                inputBorderColor:"=borderColor",
                inputSide:"=side"
            },

            templateUrl:"directives/DrawSquares/SquaresView.html",
            link: function(scope, elem, attrs) {


            }
        };
    });