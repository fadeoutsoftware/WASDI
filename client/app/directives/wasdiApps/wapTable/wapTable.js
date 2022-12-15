angular.module('wasdi.wapTable', []).directive('waptable', function () {
    'use strict';

    return {
        restrict: "E",
        scope: {
            tableVariables: "=?",
            values: "=",
            aoInputs: "=?",
            aoTableVariables: "=value",
            cols: "=cols", 
            rows:"=rows"
        },
  
        templateUrl: "directives/wasdiApps/wapTable/wapTable.html",
        link: function (scope, elem, attrs) {
            //create array of objects for table inputs
            console.log(scope.colHeaders)
            scope.aoInputs = []
            for (let index = 0; index < scope.tableVariables.length; index++) {
                scope.aoInputs.push([])
                for (let j = 0; j < scope.tableVariables[1].length; j++) {
                    scope.aoInputs[index].push(undefined)
                }
            }
            scope.addValue = function (text, colIndex, rowIndex) {
                scope.aoTableVariables[rowIndex].splice(colIndex, 1, text);
                return scope.aoTableVariables
            }
        },
        controller: function () {

        },
        controllerAs: "$crtl"

    }
})