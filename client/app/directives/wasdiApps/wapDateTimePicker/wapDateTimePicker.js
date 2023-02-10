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
                dateTime : '=',
                tooltip:'='
            },
            template: `
            <div class="date-directive" uib-tooltip="{{$ctrl.tooltip}}" tooltip-placement="top" tooltip-class="blueWasdi" tooltip-popup-delay='750'>
                <div class=" time-picker-container ">
                    <div    class="input-group"
                        moment-picker="$ctrl.dateTime"
                        format="YYYY-MM-DD"
                        today="true"
                        start-view="month"
                        >

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


