/**
 * Created by a.corrado on 20/06/2017.
 */
angular.module('wasdi.MultiselectDropdownMenuDirective', [])
    .directive('multiselectdropdownmenu', function () {
        "use strict";
        return{
            restrict:"E",
            scope :{
                optionsDirective:'=options',
                optionsName:'@name'
                // selectedDirective:'=selected'
                // * Text binding ('@' or '@?') *
                // * One-way binding ('<' or '<?') *
                // * Two-way binding ('=' or '=?') *
                // * Function binding ('&' or '&?') *
            },

            templateUrl:"directives/multiselect_dropdown_menu/MultiselectDropdownMenuView.html"

        };
    });
