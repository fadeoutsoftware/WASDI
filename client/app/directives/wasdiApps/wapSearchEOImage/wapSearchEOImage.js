angular.module('wasdi.wapSearchEOImage', [])
    .directive('wapsearcheoimage', ['SearchService','LightSearchService','OpenSearchService','ConfigurationService',
                                                                                                        function ($SearchService,
                                                                                                                  $LightSearchService,
                                                                                                                  oOpenSearchService,
                                                                                                                  oConfigurationService) {
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
                <wapselectarea bounding-box="$ctrl.lightSearchObject.oSelectArea.oBoundingBox"
                                height-map="$ctrl.lightSearchObject.oSelectArea.iHeight"
                                width-map="$ctrl.lightSearchObject.oSelectArea.iWidth"></wapselectarea>

                <wapdatetimepicker date-time="$ctrl.lightSearchObject.oStartDate.oDate"></wapdatetimepicker>

                <wapdatetimepicker date-time="$ctrl.lightSearchObject.oEndDate.oDate"></wapdatetimepicker>

                <button class="btn btn-primary btn-wasdi search-button" ng-click="$ctrl.lightSearch()" ng-disabled = "$ctrl.isAreaSelected() === false">
                    Search
                </button>
            </div>


            <div ng-if="$ctrl.m_bAreVisibleProducts" >
                <button class="btn btn-primary btn-wasdi search-button mb-2" ng-click="$ctrl.backToLightSearch()">
                    <i class="fa fa-arrow-left" aria-hidden="true"></i>
                    back
                </button>

                <wapproductlist height-table = "'400'"
                                          parent-controller = "$ctrl"
                                          products-list= "$ctrl.lightSearchObject.oTableOfProducts.aoProducts"
                                          loading-data = "$ctrl.m_bLoadingData"
                                          is-available-selection = "$ctrl.lightSearchObject.oTableOfProducts.isAvailableSelection"
                                          is-single-selection = "$ctrl.lightSearchObject.oTableOfProducts.isSingleSelection"
                                          single-selection-layer ="$ctrl.lightSearchObject.oTableOfProducts.oSingleSelectionLayer" >

                </wapproductlist>

                <button class="btn btn-primary btn-wasdi search-button mb-2" ng-click="$ctrl.selectedProducts()">
                    <i class="fa fa-floppy-o" aria-hidden="true"></i>
                    save selection
                </button>
            </div>


            `,
            controller: function() {
                //todo check the main object ?
                this.m_aListOfProvider = [];
                this.m_oSelectedProvider = {};
                this.m_bAreVisibleProducts = false;
                this.m_bLoadingData = false;
                this.m_oConfiguration = {};
                this.m_aoMissions = [];
                this.m_oSingleSelectionLayer = {};
                let oController = this;

                /*************************** METHODS ***************************/
                this.backToLightSearch = function() {
                    this.m_bAreVisibleProducts = false;

                };

                this.getOpenSearchDate = function(){
                    var oStartDate = $LightSearchService.utcDateConverter(this.lightSearchObject.oStartDate.m_sDate);
                    var oEndDate = $LightSearchService.utcDateConverter(this.lightSearchObject.oEndDate.m_sDate);
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
                    // clean table
                    this.lightSearchObject.oTableOfProducts.aoProducts = [];

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
                    this.initSelectedProvider();
                    var oProvider = this.m_oSelectedProvider;
                    var oCallbackError = function(){
                        utilsVexDialogAlertTop("It was impossible loading product");
                    };
                    this.initMissionsFilters();
                    // this.m_aoMissions[1].selected = true;//TODO REMOVE LEGACY CODE
                    var aoOpenSearchMissions = $LightSearchService.getOpenSearchMissions(this.m_aoMissions);

                    this.m_bLoadingData = true;

                    $LightSearchService.lightSearch(sOpenSearchGeoselection,oOpenSearchDates,oProvider, aoOpenSearchMissions,
                                                    oCallback, oCallbackError);




                };

                this.loadConfiguration = function(){
                    var oController = this;
                    oConfigurationService.getConfiguration().then(function(configuration){

                        oController.m_oConfiguration = configuration;

                        oController.m_aoMissions = oController.m_oConfiguration.missions;

                    });
                };
                // initSelectedProvider It's a kid of adapter
                this.initSelectedProvider = function(){
                    if(this.m_aListOfProvider === null || this.m_aListOfProvider === undefined){
                        return null;//TODO THROW EXCEPTION?
                    }
                    let iNumberOfProviders = this.m_aListOfProvider.length;

                    for(let iIndexProviders = 0 ; iIndexProviders < iNumberOfProviders;iIndexProviders++){
                        let  iNumberOfInputSelectedProviders = this.lightSearchObject.aoProviders.length;
                        for(let iIndexInputSelectedProvider = 0; iIndexInputSelectedProvider < iNumberOfInputSelectedProviders;iIndexInputSelectedProvider++){
                            if( this.m_aListOfProvider[iIndexProviders].name.toLowerCase() ===
                                this.lightSearchObject.aoProviders[iIndexInputSelectedProvider].toLowerCase()){
                                oController.m_oSelectedProvider = this.m_aListOfProvider[iIndexProviders];
                                break;
                            }
                        }

                    }
                };

                // initMissionsFilters It's a kid of adapter
                this.initMissionsFilters = function(){
                    if(this.m_aoMissions === null || this.m_aoMissions === undefined ) {
                        return null;//TODO THROW EXCEPTION?
                    }
                    let iNumberOfMissions = this.m_aoMissions.length;

                    for(let iIndexMission = 0; iIndexMission < iNumberOfMissions; iIndexMission++){
                        let iNumberOfInputSelectedMissions = this.lightSearchObject.aoMissionsFilters.length;
                        for(let iIndexInputMission = 0 ; iIndexInputMission < iNumberOfInputSelectedMissions; iIndexInputMission ++){
                            if( this.m_aoMissions[iIndexMission].indexvalue.toLowerCase() ===
                                this.lightSearchObject.aoMissionsFilters[iIndexInputMission].name.toLowerCase()){
                                this.m_aoMissions[iIndexMission].selected = true;
                            }
                        }
                    }
                };

                this.selectedProducts = function(){
                    this.lightSearchObject.oTableOfProducts.oSingleSelectionLayer;
                    this.lightSearchObject.oTableOfProducts.aoProducts;
                    this.backToLightSearch();

                }
                /**************************** BEGIN ****************************/
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

                    }

                }).error(function (data) {

                });

                this.loadConfiguration();

                /**************************** END ****************************/

            },
            controllerAs: '$ctrl'
        };
    }]);


