angular.module('wasdi.wapCheckBox', [])
    .directive('wapcheckbox', function () {
        "use strict";
        return {
            restrict: 'E',
            scope: {},
            bindToController: {
                boolValue: '=',
                labelText: '='
            },
            template: `<input type="checkbox" class="form-control"  ng-model="$ctrl.boolValue" value="$ctrl.labelText">`,
            controller: function() {

            },
            controllerAs: '$ctrl'
        };
    });


