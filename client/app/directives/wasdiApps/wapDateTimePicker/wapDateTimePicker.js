angular.module('wasdi.wapDateTimePicker', [])
    .directive('wapdatetimepicker', function () {
        "use strict";
        return {
            restrict: 'E',
            scope: {},

            // * Text binding ('@' or '@?') *
            // * One-way binding ('<' or '<?') *
            // * Two-way binding ('=' or '=?') *
            // * Function binding ('&' or '&?') *
            bindToController: {
                dateTime : '='
            },
            template: `
            <div class="date-directive">
                <div class=" time-picker-container ">
                    <div    class="input-group"
                        moment-picker="$ctrl.dateTime"
                        format="YYYY-MM-DD"
                        today="true">

                        <input  class="form-control"
                            placeholder="Select Date"
                            ng-model="$ctrl.dateTime"
                            ng-model-options="{ updateOn: 'blur' }">
                            <span class="input-group-addon">
                                <i class="fa fa-calendar"></i>
                            </span>
                    </div>
                </div>
            </div>`,
            controller: function() {
                if( this.dateTime === null || this.dateTime === undefined){
                    this.dateTime = moment();
                }
            },
            controllerAs: '$ctrl'
        };
    });


