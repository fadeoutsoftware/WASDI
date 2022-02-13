angular.module('wasdi.wapProductList', [])
    .directive('wapproductlist', function () {
        "use strict";
        return {
            restrict: 'E',
            scope: {},

            // * Text binding ('@' or '@?') *
            // * One-way binding ('<' or '<?') *
            // * Two-way binding ('=' or '=?') *
            // * Function binding ('&' or '&?') *
            bindToController: {
                productsList: '=',
                heightTable: '=',
                parentController: '=',
                loadingData: '=',
                isAvailableSelection: '=',
                isSingleSelection : '=',
                singleSelectionLayer: '=',
                tooltip: '='
            },

            templateUrl:"directives/wasdiApps/wapProductList/wapProductList.html",
            controller: function() {
                this.oTableStyle = { height: this.heightTable + 'px' };

            },
            controllerAs: '$ctrl'
        };
    });


