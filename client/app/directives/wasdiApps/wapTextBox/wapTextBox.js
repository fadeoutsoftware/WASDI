angular.module('wasdi.wapTextBox', [])
    .directive('waptextbox', function () {
        "use strict";
        return {
            restrict: 'E',
            scope: {},
            bindToController: {
                inputText: '=',
                tooltip:'='
            },
            template: `<input type="text" class="form-control"  ng-model="$ctrl.inputText" uib-tooltip="{{$ctrl.tooltip}}" tooltip-placement="top" tooltip-class="customClass" tooltip-popup-delay='750'>`,
            controller: function() {

            },
            controllerAs: '$ctrl'
        };
    });


