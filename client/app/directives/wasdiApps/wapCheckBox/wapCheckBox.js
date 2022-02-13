angular.module('wasdi.wapCheckBox', [])
    .directive('wapcheckbox', function () {
        "use strict";
        return {
            restrict: 'E',
            scope: {},
            bindToController: {
                boolValue: '=',
                labelText: '=',
                tooltip:'='
            },
            template: `
            <label class="switch" >
            <input type="checkbox"  ng-model="$ctrl.boolValue" value="$ctrl.labelText">
            <span class="slider-checkbox round"></span>
            </label>
            `,
            controller: function() {

            },
            controllerAs: '$ctrl'
        };
    });


