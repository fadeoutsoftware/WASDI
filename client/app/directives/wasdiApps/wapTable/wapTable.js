angular.module('wasdi.wapTable', []).directive('waptable', function () {
    'use strict';

    return {
        restrict: "E",
        scope: {
            tableVariables: "<",
            values: "=",
            aoInputs: "=?"
        },
        templateUrl: "directives/wasdiApps/wapTable/wapTable.html",
        link: function (scope, elem, attrs) {
            //create array of objects for table inputs
            scope.aoInputs = []
            for (let index = 0; index < scope.tableVariables[0].length; index++) {
                scope.aoInputs.push([])
                for (let j = 0; j < scope.tableVariables[1].length; j++) {
                    scope.aoInputs[index].push(undefined)
                }
            }
            scope.addValue = function (text, colIndex, rowIndex) {
                scope.aoInputs[rowIndex].splice(colIndex, 1, text);
                
                return scope.aoInputs
            }
            scope.printInputs = function () {
                console.log(scope.aoInputs);
            }
        },
        controller: function () {

        },
        controllerAs: "$crtl"

    }
})