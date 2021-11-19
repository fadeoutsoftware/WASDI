angular.module('wasdi.DropdownMenuDirective', [])
    .directive('dropdownmenudirective', function () {
        "use strict";
        return{
            restrict : 'EAC',
            templateUrl:"directives/DropDownMenu/DropDownMenuView.html",
            scope :{

                selectedValue:"=",
                listOfValues:"=",
                enableSearchFilter:"=",
                dropdownName:"=",
                onClickFunction:"&"


                // * Text binding ('@' or '@?') *
                // * One-way binding ('<' or '<?') *
                // * Two-way binding ('=' or '=?') *
                // * Function binding ('&' or '&?') *
            },
            link: function(scope, elem, attrs)
            {
                if(  typeof scope.enableSearchOption !== "boolean")
                {
                    scope.enableSearchOption = false;
                }
                if( utilsIsObjectNullOrUndefined(scope.dropdownName) === true)
                {
                    scope.dropdownName = "";
                }
                scope.isSelectedValue = false;
                scope.selectedValue = {
                    name:"",
                    id:""
                };

                scope.onClickValue = function(oSelectedValue){
                    scope.isSelectedValue = true;
                    scope.selectedValue = oSelectedValue;
                   // scope.setDefaultSelectedValue();
                }

                scope.setDefaultSelectedValue = function(){
                    scope.selectedValue = {
                        name:"",
                        id:""
                    };
                }

                scope.setDefaultSelectedValue();
            }
        };
    });
