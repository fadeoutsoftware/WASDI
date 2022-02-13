angular.module('wasdi.wapSlider', [])
    .directive('wapslider', [ '$rootScope','$timeout' , function ($rootScope, $timeout) {
        "use strict";
        return{
            restrict : 'EAC',
            templateUrl:"directives/wasdiApps/wapSlider/wapSlider.html",
            // * Text binding ('@' or '@?') *
            // * One-way binding ('<' or '<?') *
            // * Two-way binding ('=' or '=?') *
            // * Function binding ('&' or '&?') *
            scope :{
                value:"=",
                min:"=",
                max:"="
            },
            bindToController:{
                tooltip:'='
            },
            controller: function() {},
            controllerAs: '$ctrl',
            link: function(scope, elem, attrs)
            {
                scope.sliderOptions = {
                    showSelectionBar: true,
                    floor: scope.min,
                    ceil: scope.max,
                    showTicks: true
                }

                let oThisScope = scope;
                let oTimeout = $timeout;

                $rootScope.$on("ActiveTabChanged", function (evt, data) {
                    console.log("Active Tab Changed")

                    oTimeout(function () {
                        oThisScope.$$childTail.slider.init();
                    }, 500)
                })


                oTimeout(function () {
                    oThisScope.$$childTail.slider.init();
                }, 500)

/*



                let test = angular.element('wapslider-menu-container');
                /*

                scope.$watch(function() { return angular.element('wapslider-menu-container'); }, function(newValue) {
                    if (newValue.match(/ng-hide/) === null) {
                        // Element is show.

                    }
                });
*/
            },
         
        };
    }]);
