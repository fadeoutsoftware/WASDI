angular.module('wasdi.wapProductsCombo', [])
    .directive('wapproductscombo', function () {
        "use strict";
        return {
            restrict: 'EAC',
            templateUrl: "directives/wasdiApps/wapProductsCombo/wapProductsCombo.html",
            // * Text binding ('@' or '@?') *
            // * One-way binding ('<' or '<?') *
            // * Two-way binding ('=' or '=?') *
            // * Function binding ('&' or '&?') *
            scope: {
                onClickFunction: "&",
                selectedValue: "=",
                listOfValues: "=",
                enableSearchFilter: "=",
                dropdownName: "=",

            },
            bindToController: {
                tooltip: "="
            },

            controller: function() {

            },
            controllerAs: '$ctrl',

            link: function (scope, elem, attrs) {
                if (typeof scope.enableSearchOption !== "boolean") {
                    scope.enableSearchOption = false;
                }
                if (utilsIsObjectNullOrUndefined(scope.dropdownName) === true) {
                    scope.dropdownName = "";
                }

                scope.onClickValue = function (oSelectedValue) {
                    scope.isSelectedValue = true;
                    scope.selectedValue = oSelectedValue;
                }

                scope.setDefaultSelectedValue = function () {
                    scope.selectedValue = {
                        name: "",
                        id: ""
                    };
                }

                if (scope.selectedValue === "") {
                    scope.isSelectedValue = false;
                    scope.setDefaultSelectedValue();
                }
            }
        };
    });
