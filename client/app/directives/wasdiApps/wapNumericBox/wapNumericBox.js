angular.module('wasdi.wapNumericBox', [])
    .directive('wapnumericbox', function () {
        "use strict";
        return {
            restrict: 'E',
            scope: {},
            bindToController: {
                inputText: '='
            },
            template: `<input type="number" class="form-control"  ng-model="$ctrl.inputText">`,
            controller: function() {

            },
            controllerAs: '$ctrl'
        };
    });

