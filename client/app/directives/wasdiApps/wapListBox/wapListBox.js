/**
 * Created by a.corrado on 02/05/2017.
 */
angular.module('wasdi.wapListBox', [])
    .directive('waplistbox', function () {
        "use strict";
        return{
            restrict:"E",
            scope :{
                 optionsDirective:'=options',
                //options:'=',
                selectedDirective:'=selected'
                // * Text binding ('@' or '@?') *
                // * One-way binding ('<' or '<?') *
                // * Two-way binding ('=' or '=?') *
                // * Function binding ('&' or '&?') *
            },

            templateUrl:"directives/wasdiApps/wapListBox/wapListBox.html",
            link: function(scope, elem, attrs) {

                scope.pushOptionInSelectedList = function(sBandInput)
                {

                    if(utilsIsStrNullOrEmpty(sBandInput) == true) return false;

                    var iNumberOfSelectedBand = scope.selectedDirective.length;
                    var bFinded = false;
                    for(var iIndexBand = 0; iIndexBand < iNumberOfSelectedBand; iIndexBand++)
                    {
                        if(scope.selectedDirective[iIndexBand] == sBandInput)
                        {
                            scope.selectedDirective.splice(iIndexBand,1);
                            bFinded=true;
                            break;
                        }
                    }

                    if(bFinded == false)
                    {
                        scope.selectedDirective.push(sBandInput);
                    }
                    return true;
                };
                
                scope.isOptionSelected = function(sBandInput)
                {
                    if(utilsIsStrNullOrEmpty(sBandInput) == true) return false;

                    var bResult=utilsFindObjectInArray(scope.selectedDirective ,sBandInput);
                    if(utilsIsObjectNullOrUndefined(bResult) == true) return false;

                    if(bResult == -1)
                    {
                        return false;
                    }

                    return true;
                }
            }
        };
    });
