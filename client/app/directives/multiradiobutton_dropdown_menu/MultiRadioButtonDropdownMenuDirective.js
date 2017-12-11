/**
 * Created by a.corrado on 20/06/2017.
 */
angular.module('wasdi.MultiRadioButtonDropdownMenuDirective', [])
    .directive('multiradiobuttondropdownmenu', function () {
        "use strict";
        return{
            restrict:"E",
            scope :{
                optionsDirective:'=options',
                optionsName:"@name",
                onClick: '&'
                // optionsFunctionOnClick:"&functionOnClick"
                // selectedDirective:'=selected'
                // * Text binding ('@' or '@?') *
                // * One-way binding ('<' or '<?') *
                // * Two-way binding ('=' or '=?') *
                // * Function binding ('&' or '&?') *
            },
            // link: function(scope, elem, attrs) {
            //     var elementId = 123;
            //     var file = 124;
            //     scope.onDrop({elementId:'123',file:'125'});
            // },
            templateUrl:"directives/multiradiobutton_dropdown_menu/MultiRadioButtonDropdownMenuView.html",

        };
    });
