// angular.module('wasdi.wapNumericBox', [])
//     .directive('wapnumericbox', function () {
//         "use strict";
//         return {
//             require:'ngModel',
//             restrict: 'E',
//             scope: {},
//             bindToController: {
//                 inputText: '='
//             },
//             link: function (scope, element, attr, ctrl) {
//                 function inputValue(val) {
//                   if (val) {
//                     var digits = val.replace(/[^0-9]/g, '');
        
//                     if (digits !== val) {
//                       ctrl.$setViewValue(digits);
//                       ctrl.$render();
//                     }
//                     return parseInt(digits,10);
//                   }
//                   return undefined;
//                 }            
//                 ctrl.$parsers.push(inputValue);
//               },
            
//             template: `<input type="text" class="form-control"  ng-model="$ctrl.inputText">`,
//             controller: function() {

//             },
//             controllerAs: '$ctrl'
//         };
//     });
angular.module('wasdi.wapNumericBox', [])
    .directive('wapnumericbox', function () {
        "use strict";
        return {
            restrict: 'E',
            scope: {},
            bindToController: {
                inputText: '='
            },
            template: `<input type="number" class="form-control"  ng-model="$ctrl.inputText">`,
            controller: function() {

            },
            controllerAs: '$ctrl'
        };
    });

