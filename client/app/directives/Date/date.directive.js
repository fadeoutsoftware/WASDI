angular.module('wasdi.DateDirective', [])
    .directive('datedirective', function () {
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
                // inputText: '=',
                // deleted: '&'
            },
            //         template: `
            //   <h2>{{$ctrl.hero.name}} details!</h2>
            //   <div><label>id: </label>{{$ctrl.hero.id}}</div>
            //   <button ng-click="$ctrl.onDelete()">Delete</button>
            // `,
            template: `
            <div class=" time-picker-container ">
                <div    class="input-group"
                        moment-picker="$ctrl.dateTime"
                        format="YYYY-MM-DD HH"
                        today="true">

                    <input  class="form-control"
                            placeholder="Select time"
                            ng-model="$ctrl.dateTime"
                            ng-model-options="{ updateOn: 'blur' }">
                        <span class="input-group-addon">
                            <i class="fa fa-calendar"></i>
                        </span>
                </div>
            </div>

         `,
            controller: function() {

                if( this.dateTime === null || this.dateTime === undefined){
                    //TODO SET IT
                    this.dateTime = moment();
                }
            },
            controllerAs: '$ctrl'
        };
    });


