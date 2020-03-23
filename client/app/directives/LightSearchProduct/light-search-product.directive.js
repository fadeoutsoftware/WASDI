angular.module('wasdi.LightSearchProductDirective', [])
    .directive('lightsearchproductdirective', ['SearchService','LightSearchService','OpenSearchService',function ($SearchService,
                                                                                                                  $LightSearchService,
                                                                                                                  oOpenSearchService) {
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
            <div ng-hide="$ctrl.m_bAreVisibleProducts">
                <map2ddirective bounding-box="$ctrl.lightSearchObject.oSelectArea.oBoundingBox"
                                height-map="$ctrl.lightSearchObject.oSelectArea.iHeight"
                                width-map="$ctrl.lightSearchObject.oSelectArea.iWidth"></map2ddirective>

                <datedirective date-time="$ctrl.lightSearchObject.oStartDate.oDate"></datedirective>

                <datedirective date-time="$ctrl.lightSearchObject.oEndDate.oDate"></datedirective>

                <button class="btn btn-primary btn-wasdi search-button" ng-click="$ctrl.lightSearch()"
                                                                        ng-disabled = "$ctrl.isAreaSelected() === false">
                    Search
                </button>
            </div>


<!--            <button class="btn btn-primary btn-wasdi search-button" ng-click="$ctrl.loadMore()">-->
<!--                Load more-->
<!--            </button>-->
            <div ng-if="$ctrl.m_bAreVisibleProducts" >
                <button class="btn btn-primary btn-wasdi search-button mb-2" ng-click="$ctrl.backToLightSearch()">
                    <i class="fa fa-arrow-left" aria-hidden="true"></i>
                    back
                </button>

                <tableofproductsdirective height-table = "'400'"
                                          parent-controller = "$ctrl"
                                          products-list="$ctrl.lightSearchObject.oTableOfProducts.aoProducts"
                                          loading-data = "$ctrl.m_bLoadingData">

                </tableofproductsdirective>
            </div>


            `,
            controller: function() {
                //todo check the main object ?
                this.m_aListOfProvider = [];
                this.m_oSelectedProvider = {};
                this.m_bAreVisibleProducts = false;
                this.m_bLoadingData = false;
                let oController = this;

                oOpenSearchService.getListOfProvider().success(function (data) {
                    if(utilsIsObjectNullOrUndefined(data) === false && data.length > 0)
                    {
                        var iLengthData = data.length;
                        for(var iIndexProvider = 0; iIndexProvider < iLengthData; iIndexProvider++)
                        {
                            oController.m_aListOfProvider[iIndexProvider] = {
                                "name": data[iIndexProvider].code,
                                "totalOfProducts":0,
                                "totalPages":1,
                                "currentPage":1,
                                "productsPerPageSelected":10,
                                "selected":true,
                                "isLoaded":false,
                                "description": data[iIndexProvider].description,
                                "link": data[iIndexProvider].link
                            };
                        }
                        oController.m_oSelectedProvider = oController.m_aListOfProvider[0];//TODO REMOVE LEGACY CODE
                    }

                }).error(function (data) {

                });

                this.backToLightSearch = function() {
                    this.m_bAreVisibleProducts = false;
                    // clean table
                    this.lightSearchObject.oTableOfProducts.aoProducts = [];
                };

                this.getOpenSearchDate = function(){
                    var oStartDate = $LightSearchService.utcDateConverter(this.lightSearchObject.oStartDate.oDate);
                    var oEndDate = $LightSearchService.utcDateConverter(this.lightSearchObject.oEndDate.oDate);
                    var oDates = {
                        sensingPeriodFrom : oStartDate,
                        sensingPeriodTo: oEndDate,
                        ingestionFrom: null,
                        ingestionTo: null
                    };
                    return $LightSearchService.getOpenSearchDate(oDates);
                };

                this.loadMore = function(){
                    let oController = this;
                    this.m_oSelectedProvider.currentPage = this.m_oSelectedProvider.currentPage + 1;
                    let oCallback = function(result){
                        var sResults = result;
                        if(!utilsIsObjectNullOrUndefined(sResults))
                        {
                            if (!utilsIsObjectNullOrUndefined(sResults.data) && sResults.data != "" ) {
                                var aoData = sResults.data;
                                $LightSearchService.setDefaultPreviews(aoData);
                                oController.lightSearchObject.oTableOfProducts.aoProducts = oController.lightSearchObject.oTableOfProducts.aoProducts.concat(aoData);

                            } else {
                                utilsVexDialogAlertTop("There are no other products");
                            }
                        }
                        oController.m_bLoadingData = false;
                    };
                    this.search(oCallback);
                };

                this.lightSearch = function(){
                    this.m_bAreVisibleProducts = true;
                    let oController = this;
                    let oCallback = function(result){
                        var sResults = result;
                        if(!utilsIsObjectNullOrUndefined(sResults))
                        {
                            if (!utilsIsObjectNullOrUndefined(sResults.data) && sResults.data != "" ) {
                                var aoData = sResults.data;
                                $LightSearchService.setDefaultPreviews(aoData);
                                oController.lightSearchObject.oTableOfProducts.aoProducts = aoData;

                            }
                        }
                        oController.m_bLoadingData = false;
                        debugger;
                    };

                    this.search(oCallback);
                };

                this.isAreaSelected = function(){
                    var sOpenSearchGeoselection = $LightSearchService.getOpenSearchGeoselection(this.lightSearchObject.oSelectArea.oBoundingBox);
                    var bIsSubstring = sOpenSearchGeoselection.includes('undefined');
                    return !bIsSubstring;
                };

                this.search = function(oCallback){
                    //set geoselection

                    var sOpenSearchGeoselection = $LightSearchService.getOpenSearchGeoselection(this.lightSearchObject.oSelectArea.oBoundingBox);

                    var oOpenSearchDates = this.getOpenSearchDate();
                    var oProvider = this.m_oSelectedProvider; //ONDA?
                    var oCallbackError = function(){
                        utilsVexDialogAlertTop("It was impossible loading product");
                    };
                    this.m_bLoadingData = true;

                    $LightSearchService.lightSearch(sOpenSearchGeoselection,oOpenSearchDates,oProvider,oCallback, oCallbackError);

                    // //todo  set default filters ?  this.m_oAdvancedFilterService.setAdvancedFilter + this.m_oSearchService.setMissionFilter



                }

            },
            controllerAs: '$ctrl'
        };
    }]);


