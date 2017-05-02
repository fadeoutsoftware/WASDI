/**
 * Created by a.corrado on 02/05/2017.
 */
angular.module('wasdi.MultiselectDirective', [])
    .directive('multiselect', function () {
        "use strict";
        return{
            restrict:"E",
            scope :{
                optionsDirective:'=options',
                selectedDirective:'=selected'
            },

            templateUrl:"directives/multiselect/MultiselectView.html",
            link: function(scope, elem, attrs) {

                scope.pushBandInSelectedList = function(sBandInput)
                {

                    if(utilsIsStrNullOrEmpty(sBandInput) == true)
                        return false;
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
                scope.isBandSelected = function(sBandInput)
                {
                    if(utilsIsStrNullOrEmpty(sBandInput) == true)
                        return false;

                    var bResult=utilsFindObjectInArray(scope.selectedDirective ,sBandInput);
                    if(utilsIsObjectNullOrUndefined(bResult) == true)
                        return false;
                    if(bResult == -1)
                        return false
                    else
                        return true;
                }
            }
        };
    });