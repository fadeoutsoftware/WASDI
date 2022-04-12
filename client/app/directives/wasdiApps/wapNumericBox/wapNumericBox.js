angular.module('wasdi.wapNumericBox', [])
    .directive('wapnumericbox', function () {
        "use strict";
        return {
            restrict: 'E',
            scope: {},
            bindToController: {
                inputText: '=',
                tooltip:'='
            },
            template: `<input type="number" class="form-control"  ng-model="$ctrl.inputText" uib-tooltip="{{$ctrl.tooltip}}" tooltip-placement="top" tooltip-class="blueWasdi" tooltip-popup-delay='750'>`,
            controller: function() {

            },
            controllerAs: '$ctrl'
        };
    });

