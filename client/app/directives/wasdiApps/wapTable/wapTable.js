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
            //console.log(scope.colHeaders)
            scope.aoInputs = []
            for (let iIndex = 0; iIndex < scope.tableVariables.length; iIndex++) {
                scope.aoInputs.push([])
                for (let j = 0; j < scope.tableVariables[1].length; j++) {
                    scope.aoInputs[iIndex].push(undefined)
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