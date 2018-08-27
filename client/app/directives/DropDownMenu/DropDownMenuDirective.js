angular.module('wasdi.DropdownMenuDirective', [])
    .directive('dropdownmenudirective', function () {
        "use strict";
        return{
            restrict : 'EAC',
            templateUrl:"directives/DropDownMenu/DropDownMenuView.html",
            scope :{
                onClickFunction:"&onClickFunction",
                selectedValue:"=selectedValue",
                listOfValues:"=listOfValues"
                // urlImage : '=',
                // body : '=',
                // getDefaultImage:'&',
                // applyEditorPreviewImage:'&',
                // maskManager: '&',
                // filterManager: '&',
                // panScaling: '=',
                // heightCanvas: '=',
                // widthCanvas: '='
                // isLoaded : '='

                // * Text binding ('@' or '@?') *
                // * One-way binding ('<' or '<?') *
                // * Two-way binding ('=' or '=?') *
                // * Function binding ('&' or '&?') *
            },
            link: function(scope, elem, attrs)
            {
                if(utilsIsStrNullOrEmpty(scope.nameDropdown) === true)
                {
                    scope.nameDropdown = "";
                }
                scope.changeSelectedValueOnClick = function(sNewValue){
                    scope.selectedValue = sNewValue;
                }
                // if(utilsIsObjectNullOrUndefined(scope.selectedValue) === true)
                // {
                //     scope.selectedValue = null;
                // }

            }
        };
    });