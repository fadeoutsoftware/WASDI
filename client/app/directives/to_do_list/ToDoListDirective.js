// https://www.w3schools.com/howto/howto_js_todolist.asp
angular.module('wasdi.ToDoListDirective', [])
    .directive('todolist', function () {
        "use strict";
        return{
            restrict:"E",
            scope :{
                elementsDirective:'=elements',
                //options:'=',
                // selectedDirective:'=selected'
                // * Text binding ('@' or '@?') *
                // * One-way binding ('<' or '<?') *
                // * Two-way binding ('=' or '=?') *
                // * Function binding ('&' or '&?') *
            },
            templateUrl:"directives/to_do_list/ToDoListView.html",
            link: function(scope, elem, attrs) {

            }
        };
    });