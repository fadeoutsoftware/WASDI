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
                for (let j = 0; j < scope.tableVariables[1].length; j++) {
                    scope.aoInputs.push({
                        "column": index,
                        "row": j,
                        "value": ""
                    })
                }
            }
            scope.addValue = function (text, colIndex, rowIndex) {
                scope.aoInputs.find(oObject => {
                    if (oObject.column === colIndex && oObject.row === rowIndex) {
                        oObject.value = text;
                    }
                })
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