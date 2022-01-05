angular.module('wasdi.wapSearchEOImage', [])
    .directive('wapsearcheoimage', ['SearchService', 'LightSearchService', 'OpenSearchService', 'ConfigurationService',
        function ($SearchService,
            $LightSearchService,
            oOpenSearchService,
            oConfigurationService) {
            "use strict";
            return {
                restrict: 'E',
                templateUrl: "directives/wasdiApps/wapSearchEOImage/wapSearchEOImage.html",
                scope: {},

                // * Text binding ('@' or '@?') *
                // * One-way binding ('<' or '<?') *
                // * Two-way binding ('=' or '=?') *
                // * Function binding ('&' or '&?') *
                bindToController: {
                    lightSearchObject: '=',
                    tooltip: '='
                    // deleted: '&'
                },
                controller: function () {
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
                    this.backToLightSearch = function () {
                        this.m_bAreVisibleProducts = false;

                    };

                    this.getOpenSearchDate = function () {
                        var oStartDate = $LightSearchService.utcDateConverter(this.lightSearchObject.oStartDate.m_sDate);
                        var oEndDate = $LightSearchService.utcDateConverter(this.lightSearchObject.oEndDate.m_sDate);
                        var oDates = {
                            sensingPeriodFrom: oStartDate,
                            sensingPeriodTo: oEndDate,
                            ingestionFrom: null,
                            ingestionTo: null
                        };
                        return $LightSearchService.getOpenSearchDate(oDates);
                    };

                    this.loadMore = function () {
                        let oThat = this;
                        this.m_oSelectedProvider.currentPage = this.m_oSelectedProvider.currentPage + 1;
                        let oCallback = function (result) {
                            var sResults = result;
                            if (!utilsIsObjectNullOrUndefined(sResults)) {
                                if (!utilsIsObjectNullOrUndefined(sResults.data) && sResults.data != "") {
                                    var aoData = sResults.data;
                                    $LightSearchService.setDefaultPreviews(aoData);
                                    oThat.lightSearchObject.oTableOfProducts.aoProducts = oThat.lightSearchObject.oTableOfProducts.aoProducts.concat(aoData);

                                } else {
                                    utilsVexDialogAlertTop("There are no other products");
                                }
                            }
                            oThat.m_bLoadingData = false;
                        };
                        this.search(oCallback);
                    };

                    this.lightSearch = function () {
                        this.m_bAreVisibleProducts = true;
                        let oThat = this;
                        // clean table
                        this.lightSearchObject.oTableOfProducts.aoProducts = [];

                        let oCallback = function (result) {
                            var sResults = result;
                            if (!utilsIsObjectNullOrUndefined(sResults)) {
                                if (!utilsIsObjectNullOrUndefined(sResults.data) && sResults.data != "") {
                                    var aoData = sResults.data;
                                    $LightSearchService.setDefaultPreviews(aoData);
                                    oThat.lightSearchObject.oTableOfProducts.aoProducts = aoData;

                                }
                            }
                            oThat.m_bLoadingData = false;
                        };

                        this.search(oCallback);
                    };

                    this.isAreaSelected = function () {
                        var sOpenSearchGeoselection = $LightSearchService.getOpenSearchGeoselection(this.lightSearchObject.oSelectArea.oBoundingBox);
                        var bIsSubstring = sOpenSearchGeoselection.includes('undefined');
                        return !bIsSubstring;
                    };

                    this.search = function (oCallback) {
                        //set geoselection

                        var sOpenSearchGeoselection = $LightSearchService.getOpenSearchGeoselection(this.lightSearchObject.oSelectArea.oBoundingBox);

                        var oOpenSearchDates = this.getOpenSearchDate();
                        this.initSelectedProvider();
                        var oProvider = this.m_oSelectedProvider;
                        var oCallbackError = function () {
                            utilsVexDialogAlertTop("It was impossible loading product");
                        };
                        this.initMissionsFilters();
                        // this.m_aoMissions[1].selected = true;//TODO REMOVE LEGACY CODE
                        var aoOpenSearchMissions = $LightSearchService.getOpenSearchMissions(this.m_aoMissions);

                        this.m_bLoadingData = true;

                        $LightSearchService.lightSearch(sOpenSearchGeoselection, oOpenSearchDates, oProvider, aoOpenSearchMissions,
                            oCallback, oCallbackError);
                    };

                    this.loadConfiguration = function () {
                        var oThat = this;
                        oConfigurationService.getConfiguration().then(function (configuration) {

                            oThat.m_oConfiguration = configuration;

                            oThat.m_aoMissions = oThat.m_oConfiguration.missions;

                        });
                    };
                    // initSelectedProvider It's a kid of adapter
                    this.initSelectedProvider = function () {
                        if (this.m_aListOfProvider === null || this.m_aListOfProvider === undefined) {
                            return null;
                        }
                        let iNumberOfProviders = this.m_aListOfProvider.length;

                        for (let iIndexProviders = 0; iIndexProviders < iNumberOfProviders; iIndexProviders++) {
                            let iNumberOfInputSelectedProviders = this.lightSearchObject.aoProviders.length;
                            for (let iIndexInputSelectedProvider = 0; iIndexInputSelectedProvider < iNumberOfInputSelectedProviders; iIndexInputSelectedProvider++) {
                                if (this.m_aListOfProvider[iIndexProviders].name.toLowerCase() ===
                                    this.lightSearchObject.aoProviders[iIndexInputSelectedProvider].toLowerCase()) {
                                    oController.m_oSelectedProvider = this.m_aListOfProvider[iIndexProviders];
                                    break;
                                }
                            }

                        }
                    };

                    // initMissionsFilters It's a kid of adapter
                    this.initMissionsFilters = function () {
                        if (this.m_aoMissions === null || this.m_aoMissions === undefined) {
                            return null;//TODO THROW EXCEPTION?
                        }
                        let iNumberOfMissions = this.m_aoMissions.length;

                        for (let iIndexMission = 0; iIndexMission < iNumberOfMissions; iIndexMission++) {
                            let iNumberOfInputSelectedMissions = this.lightSearchObject.aoMissionsFilters.length;
                            for (let iIndexInputMission = 0; iIndexInputMission < iNumberOfInputSelectedMissions; iIndexInputMission++) {
                                if (this.m_aoMissions[iIndexMission].indexvalue.toLowerCase() ===
                                    this.lightSearchObject.aoMissionsFilters[iIndexInputMission].name.toLowerCase()) {
                                    this.m_aoMissions[iIndexMission].selected = true;
                                }
                            }
                        }
                    };

                    this.selectedProducts = function () {
                        //this.lightSearchObject.oTableOfProducts.oSingleSelectionLayer;
                        //this.lightSearchObject.oTableOfProducts.aoProducts;
                        this.backToLightSearch();
                    }
                    /**************************** BEGIN ****************************/
                    oOpenSearchService.getListOfProvider().then(function (data) {
                        if (utilsIsObjectNullOrUndefined(data.data) === false && data.data.length > 0) {
                            var iLengthData = data.data.length;
                            for (var iIndexProvider = 0; iIndexProvider < iLengthData; iIndexProvider++) {
                                oController.m_aListOfProvider[iIndexProvider] = {
                                    "name": data.data[iIndexProvider].code,
                                    "totalOfProducts": 0,
                                    "totalPages": 1,
                                    "currentPage": 1,
                                    "productsPerPageSelected": 10,
                                    "selected": true,
                                    "isLoaded": false,
                                    "description": data.data[iIndexProvider].description,
                                    "link": data.data[iIndexProvider].link
                                };
                            }

                        }

                    }, function (data) {

                    });

                    this.loadConfiguration();

                    /**************************** END ****************************/

                },
                controllerAs: '$ctrl'
            };
        }]);


