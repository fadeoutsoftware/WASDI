angular.module('wasdi.ChipsListDirective', [])
    .directive('chipslistdirective', function () {
        "use strict";
        return {
            restrict: 'E',
            scope: {},

            // * Text binding ('@' or '@?') *
            // * One-way binding ('<' or '<?') *
            // * Two-way binding ('=' or '=?') *
            // * Function binding ('&' or '&?') *
            bindToController: {
                listOfChips : '='
                // dateTime : '='
                // inputText: '=',
                // deleted: '&'
            },

            template: `
            <div ng-repeat = "chip in $ctrl.listOfChips">

            </div>
            <div class="chip">
<!--                <img src="img_avatar.png" alt="Person" width="96" height="96">-->
                John Doe
                <span class="closebtn" onclick="this.parentElement.style.display='none'">&times;</span>
            </div>
            `,
            controller: function() {


            },
            controllerAs: '$ctrl'
        };
    });


