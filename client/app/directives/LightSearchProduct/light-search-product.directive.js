angular.module('wasdi.LightSearchProductDirective', [])
    .directive('lightsearchproductdirective', ['SearchService','LightSearchService',function ($SearchService,$LightSearchService) {
        "use strict";
        return {
            restrict: 'E',
            scope: {},

            // * Text binding ('@' or '@?') *
            // * One-way binding ('<' or '<?') *
            // * Two-way binding ('=' or '=?') *
            // * Function binding ('&' or '&?') *
            bindToController: {
                lightSearchObject: '=',
                // deleted: '&'
            },

            template: `
            <map2ddirective   bounding-box="$ctrl.lightSearchObject.oSelectArea.oBoundingBox"
                              height-map="$ctrl.lightSearchObject.oSelectArea.iHeight"
                              width-map="$ctrl.lightSearchObject.oSelectArea.iWidth"></map2ddirective>

            <datedirective date-time="$ctrl.lightSearchObject.oStartDate.oDate"></datedirective>

            <datedirective date-time="$ctrl.lightSearchObject.oEndDate.oDate"></datedirective>
            <button class="btn btn-primary btn-wasdi search-button" ng-click="$ctrl.lightSearch()">
                Search
            </button>
            <tableofproductsdirective  products-list="$ctrl.lightSearchObject.oTableOfProducts.aoProducts"></tableofproductsdirective>
         `,
            controller: function() {
                //todo check the main object
                this.lightSearch = function(){
                    //set geoselection
                    var sGeoselection = $LightSearchService.getOpenSearchGeoselection(this.lightSearchObject.oSelectArea.oBoundingBox);
                    $SearchService.setGeoselection(sGeoselection);
                    //todo set date start ?
                    //todo set date end ?
                    //todo  set default filters ?  this.m_oAdvancedFilterService.setAdvancedFilter + this.m_oSearchService.setMissionFilter
                    $SearchService.search().then(function(result){
                        var sResults = result;
                        if(!utilsIsObjectNullOrUndefined(sResults))
                        {
                            if (!utilsIsObjectNullOrUndefined(sResults.data) && sResults.data != "" ) {
                                var aoData = sResults.data;
                                // oController.generateLayersList(aoData);
                            }
                        }
                    }, function errorCallback(response) {
                        utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN OPEN SEARCH REQUEST");
                    });
                }

            },
            controllerAs: '$ctrl'
        };
    }]);


