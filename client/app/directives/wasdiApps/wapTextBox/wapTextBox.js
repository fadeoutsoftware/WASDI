angular.module('wasdi.wapTextBox', [])
    .directive('waptextbox', function () {
        "use strict";
        return {
            restrict: 'E',
            scope: {},
            bindToController: {
                inputText: '='
            },
            template: `<input type="text" class="form-control"  ng-model="$ctrl.inputText">`,
            controller: function() {

            },
            controllerAs: '$ctrl'
        };
    });


