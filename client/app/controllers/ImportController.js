/**
 * Created by a.corrado on 30/11/2016.
 */

var ImportController = (function() {
    //**************************************************************************
    /*IMPORTANT NOTE  THE LAYER CORRESPOND TO PRODUCTS*/
    //**************************************************************************
    function ImportController($scope, oConstantsService, oAuthService,$state,oMapService, oSearchService, oAdvancedFilterService,
                              oAdvancedSearchService, oConfigurationService, oFileBufferService, oRabbitStompService, oProductService,
                              oProcessWorkspaceService,oWorkspaceService,oResultsOfSearchService,oModalService,oOpenSearchService,oPageservice, oTranslate ) {
        // Service references
        this.m_oScope = $scope;
        this.m_oConstantsService = oConstantsService;
        this.m_oAuthService = oAuthService;
        this.m_oState = $state;
        this.m_oMapService = oMapService;
        this.m_oSearchService = oSearchService;
        this.m_oAdvancedFilterService = oAdvancedFilterService;
        this.m_oAdvancedSearchService = oAdvancedSearchService;
        this.m_oConfigurationService = oConfigurationService;
        this.m_oFileBufferService = oFileBufferService;
        this.m_oRabbitStompService = oRabbitStompService;
        this.m_oProductService = oProductService;
        this.m_oProcessWorkspaceService = oProcessWorkspaceService;
        this.m_oWorkspaceService=oWorkspaceService;
        this.m_oResultsOfSearchService = oResultsOfSearchService;
        this.m_oModalService = oModalService;
        this.m_oOpenSearchService = oOpenSearchService;
        this.m_oPageService = oPageservice;
        this.m_oTranslate = oTranslate;

        // Self link for the scope
        this.m_oScope.m_oController = this;
        this.m_oScope.$ctrl = this;

        this.m_bShowsensingfilter = true;

        //this.m_sTypeOfFilterSelected = "Time period";
        this.setFilterTypeAsTimePeriod();
        this.m_oAdvanceFilter = {
            filterActive:"Seasons",//Seasons,Range,Months
            savedData:[],
            selectedSeasonYears:[],
            selectedSeason:"",
            listOfYears:[],
            listOfMonths:[],
            // listOfDays:[],
            selectedYears:[],
            selectedDayFrom:"",
            selectedDayTo:"",
            selectedMonthFrom:"",
            selectedMonthTo:"",
            selectedYearsSearchForMonths:[],
            selectedMonthsSearchForMonths:[]
        };

        this.initDefaultYears();
        this.initDefaultMonths();
        this.datePickerYears={
            datepickerMode: "year"
        };
        this.m_aoSeason = [
            {name:"Spring"},
            {name:"Winter"},
            {name:"Summer"},
            {name:"Autumn"}
        ];


        //set Page service
        this.m_oPageService.setFunction(this.search);

        //tab index
        this.m_activeMissionTab = 0;
        this.m_iActiveProvidersTab = 0;

        this.m_oDetails = {};
        this.m_oDetails.productIds = [];
        this.m_oScope.selectedAll = false;
        this.m_oConfiguration = null;
        this.m_bisVisibleLocalStorageInputs = false;
        this.m_oStatus = {
            opened: false
        };
        this.m_bIsOpen=true;
        // Flag to know if we are in the result page (true) or in the filter page (false)
        this.m_bIsVisibleListOfLayers = false;
        // Flag to know if we are in a Paginated Search (true) or full list search (false)
        this.m_bIsPaginatedList = true;

        this.m_oMapService.initMapWithDrawSearch('wasdiMapImport');
        this.m_oMapService.mapDrawEventDeletePolygon(this.m_oMapService.getMap(),this.clearModelGeoselection,this);
        this.m_oMapService.initGeoSearchPluginForOpenStreetMap();

        // layers list == products list
        this.m_aoProductsList = [];
        // List of missions
        this.m_aoMissions = [];
        // list of providers
        this.m_aListOfProvider = [];

        /* number of possible products per pages and number of products per pages selected */
        this.m_iProductsPerPageSelected = 10;//default value
        this.m_iProductsPerPage=[10,15,20,25,50];
        //Page
        this.m_iCurrentPage = 1;
        this.m_iTotalPages = 1;
        this.m_iTotalOfProducts = 0;

        this.m_bClearFiltersEnabled = true;

        // var dateObj = new Date();
        this.m_oModel = {
            textQuery:'',
            list: '',
            geoselection: '',
            offset: 0,
            pagesize: 25,
            advancedFilter: '',
            missionFilter: '',
            doneRequest: '',
            sensingPeriodFrom: '',
            sensingPeriodTo:'' ,
            ingestionFrom: '',
            ingestionTo: ''
        };

        this.m_fUtcDateConverter = function(date){
            var result = date;
            if(date != undefined) {
                var day =  moment(date).get('date');
                var month = moment(date).get('month');
                var year = moment(date).get('year');
                var utcDate = moment(year+ "-" + (parseInt(month)+1) +"-" +day+ " 00:00 +0000", "YYYY-MM-DD HH:mm Z"); // parsed as 4:30 UTC
                result =  utcDate;
            }

            return result;
        };

        this.m_oMergeSearch =
        {
            "statusIsOpen":false,
            period:''
        };

        /* Active Workspace */
        this.m_oActiveWorkspace = this.m_oConstantsService.getActiveWorkspace();
        this.m_oUser = this.m_oConstantsService.getUser();

        //if there isn't workspace
        if(utilsIsObjectNullOrUndefined(this.m_oActiveWorkspace) && utilsIsStrNullOrEmpty( this.m_oActiveWorkspace))
        {
            //if this.m_oState.params.workSpace in empty null or undefined create new workspace
            if(!(utilsIsObjectNullOrUndefined(this.m_oState.params.workSpace) && utilsIsStrNullOrEmpty(this.m_oState.params.workSpace)))
            {
                this.openWorkspace(this.m_oState.params.workSpace);
                this.m_oActiveWorkspace = this.m_oConstantsService.getActiveWorkspace();
            }

        }
        else
        {
            /*Load elements by Service if there was a previous search i load*/
            if (!utilsIsObjectNullOrUndefined(this.m_oActiveWorkspace)) this.m_oProcessWorkspaceService.loadProcessesFromServer(this.m_oActiveWorkspace.workspaceId);

            var oWorkspaceByResultService = this.m_oResultsOfSearchService.getActiveWorkspace();
            //if the workspace id saved in ResultService but the id it's differet to actual workspace id clean ResultService
            if(utilsIsObjectNullOrUndefined(oWorkspaceByResultService) || (oWorkspaceByResultService.workspaceId != this.m_oActiveWorkspace.workspaceId)) {
                this.m_oResultsOfSearchService.setDefaults();
            }

            this.loadOpenSearchParamsByResultsOfSearchServices(this);
        }

        /*Hook to Rabbit WebStomp Service*/
        this.m_oRabbitStompService.setMessageCallback(this.receivedRabbitMessage);
        this.m_oRabbitStompService.setActiveController(this);

        var oController = this;

        //get configuration
        this.m_oConfigurationService.getConfiguration().then(function(configuration){

            oController.m_oConfiguration = configuration;
            var oMissions = oController.m_oResultsOfSearchService.getMissions();

            if(!utilsIsObjectNullOrUndefined(oMissions) && oMissions.length != 0)
            {
                oController.m_aoMissions = oMissions;
            }
            else
            {
                oController.m_aoMissions = oController.m_oConfiguration.missions;
            }

            oController.m_bShowsensingfilter = true;
            oController.m_oScope.$apply();
            // selects the first mission (S1)
            oController.updateMissionSelection(0);
        });


        this.m_DatePickerPosition = function($event){

        };



        $scope.$on('rectangle-created-for-opensearch', function(event, args) {

            var oLayer = args.layer;
            var sFilter = '( footprint:"intersects(POLYGON((';
            if(!utilsIsObjectNullOrUndefined(oLayer))
            {
                var iNumberOfPoints = oLayer._latlngs[0].length;
                var aaLatLngs = oLayer._latlngs[0];
                /*open search want the first point as end point */
                var iLastlat=aaLatLngs[0].lat;
                var iLastlng=aaLatLngs[0].lng;
                for(var iIndexBounds = 0; iIndexBounds < iNumberOfPoints; iIndexBounds++)
                {

                    sFilter = sFilter + aaLatLngs[iIndexBounds].lng + " " +aaLatLngs[iIndexBounds].lat+",";
                    //if(iIndexBounds != (iNumberOfPoints-1))
                    //    sFilter = sFilter + ",";
                }
                sFilter = sFilter + iLastlng + " " + iLastlat + ')))" )';
            }
            //(%20footprint:%22Intersects(POLYGON((5.972671999999995%2036.232811331264955,20.123062624999992%2036.232811331264955,20.123062624999992%2048.3321995971576,5.972671999999995%2048.3321995971576,5.972671999999995%2036.232811331264955)))%22%20)
            //set filter
            $scope.m_oController.m_oModel.geoselection = sFilter ;
        });

        /*When mouse on rectangle layer (change css)*/
        $scope.$on('on-mouse-over-rectangle', function(event, args) {

            var oRectangle = args.rectangle;
            if(!utilsIsObjectNullOrUndefined(oRectangle))
            {
                var iLengthLayersList;
                if(utilsIsObjectNullOrUndefined($scope.m_oController.m_aoProductsList.length))
                    iLengthLayersList = 0;
                else
                    iLengthLayersList = $scope.m_oController.m_aoProductsList.length;

                for(var iIndex = 0; iIndex < iLengthLayersList; iIndex++)
                {

                    if($scope.m_oController.m_aoProductsList[iIndex].rectangle == oRectangle)
                    {
                        var sId = "layer"+$scope.m_oController.m_aoProductsList[iIndex].id;
                        //change css of table
                        jQuery("#"+sId).css({"border-top": "2px solid green", "border-bottom": "2px solid green","background-color": "#bfbfbf"});


                    }
                }

            }
        });

        /*When mouse leaves rectangle layer (change css)*/
        $scope.$on('on-mouse-leave-rectangle', function(event, args) {

            var oRectangle = args.rectangle;
            if(!utilsIsObjectNullOrUndefined(oRectangle))
            {
                var iLengthLayersList;
                if(utilsIsObjectNullOrUndefined($scope.m_oController.m_aoProductsList.length))
                    iLengthLayersList = 0;
                else
                    iLengthLayersList = $scope.m_oController.m_aoProductsList.length;

                for(var iIndex = 0; iIndex < iLengthLayersList; iIndex++)
                {

                    if($scope.m_oController.m_aoProductsList[iIndex].rectangle == oRectangle)
                    {
                        var sId = "layer"+$scope.m_oController.m_aoProductsList[iIndex].id;
                        //return default css of table
                        jQuery("#"+sId).css({"border-top": "", "border-bottom": "", "background-color": "white"});

                    }
                }

            }

        });

        /*When rectangle was clicked (change focus on table)*/
        $scope.$on('on-mouse-click-rectangle', function(event, args) {

            var oRectangle = args.rectangle;

            if(!utilsIsObjectNullOrUndefined(oRectangle))
            {
                if(utilsIsObjectNullOrUndefined($scope.m_oController.m_aoProductsList))
                    var iLengthLayersList = 0;
                else
                    var iLengthLayersList = $scope.m_oController.m_aoProductsList.length;

                for(var iIndex = 0; iIndex < iLengthLayersList; iIndex++)
                {

                    if($scope.m_oController.m_aoProductsList[iIndex].rectangle == oRectangle)
                    {
                        var sId = "layer"+$scope.m_oController.m_aoProductsList[iIndex].id;

                        /* change view on table , when the pointer of mouse is on a rectangle the table scroll, the row will become visible(if it isn't) */
                        var container = $('#div-container-table'), scrollTo = $('#'+sId);

                        //http://stackoverflow.com/questions/2905867/how-to-scroll-to-specific-item-using-jquery
                        container.animate({
                            scrollTop: scrollTo.offset().top - container.offset().top + container.scrollTop()
                        });

                    }
                }

            }
        });

        // Set search default values:
        this.m_aListOfProvider = this.m_oPageService.getProviders();
        this.setDefaultData();
        this.updateAdvancedSearch();
    }

    /***************** METHODS ***************/


    ImportController.prototype.setFilterTypeAsTimePeriod = function(){ this.m_sTypeOfFilterSelected = "Time period"; }
    ImportController.prototype.setFilterTypeAsTimeSeries = function(){ this.m_sTypeOfFilterSelected = "Time series"; }
    ImportController.prototype.updateAdvancedSavedFiltersUi = function()
    {
        if(this.m_oAdvanceFilter.savedData.length == 0)
        {
            this.setFilterTypeAsTimePeriod();
        }
    }


    ImportController.prototype.toggleMissionSelection= function(mission, index, event)
    {

        // Beforehand deselect all the missions -> the migrate this behaviour to tab selection (on active tabs)
        let curMission = null;
        for (var i = 0;i< this.m_aoMissions.length;i++){ 
            curMission = this.m_aoMissions[i];
            curMission.selected= false;
        }
        mission.selected = !mission.selected;
        this.updateMissionSelection(index);
        // also do the selection of the tab 
        this.m_activeMissionTab = index;
        

        // prevent tab selection when user click on the checkbox
        event.stopPropagation();
    }

    ImportController.prototype.selectTabMission= function(mission, index, event)
    {
        this.m_activeMissionTab = index;
    }
    ImportController.prototype.isMissionTabOpen= function(index)
    {
        return this.m_activeMissionTab === index;
    }

    /**
     * Get the list of available missions
     * @returns {Array|*}
     */
    ImportController.prototype.getMissions= function() {
        return this.m_aoMissions;
    };

    /**
     * Open Navigation Bar
     */
    ImportController.prototype.openNav= function() {
        document.getElementById("mySidenav").style.width = "30%";
    };

    /**
     * Close Navigation Bar
     */
    ImportController.prototype.closeNav=function() {
        document.getElementById("mySidenav").style.width = "0";
    };

    //ALTERNATIVE METHODS
    ImportController.prototype.openCloseNav=function()
    {
        var oController=this;
        if(oController.m_bIsOpen == true)
        {
            oController.closeNav();
            oController.m_bIsOpen = false;
        }
        else{
            if(oController.m_bIsOpen == false)
            {
                oController.openNav();
                oController.m_bIsOpen=true;
            }
        }
    };

    ImportController.prototype.updateFilter = function(parentIndex){

        for(var i=0; i<this.m_aoMissions[parentIndex].filters.length; i++) {
            if(this.m_aoMissions[parentIndex].filters[i].indexvalue &&
                this.m_aoMissions[parentIndex].filters[i].indexvalue.trim() != '') {
                // selected = true;
                this.m_aoMissions[parentIndex].selected = true

                break;
            }
        }

        this.setFilter();
    };


    ImportController.prototype.getAdvancedDateFilterQuery = function(searchFilter){

        var sFilter='';

        if (utilsIsObjectNullOrUndefined(searchFilter)) return sFilter;

        if(!utilsIsObjectNullOrUndefined(searchFilter.sensingPeriodFrom) && !utilsIsObjectNullOrUndefined(searchFilter.sensingPeriodTo))
        {
            sFilter += '( beginPosition:['+ this.m_oAdvancedSearchService.formatDateFrom_(searchFilter.sensingPeriodFrom) +
                ' TO ' + this.m_oAdvancedSearchService.formatToDate(searchFilter.sensingPeriodTo) + '] AND endPosition:[' +
                this.m_oAdvancedSearchService.formatDateFrom_(searchFilter.sensingPeriodFrom) + ' TO ' + this.m_oAdvancedSearchService.formatDateTo_(searchFilter.sensingPeriodTo) + '] )';
        }
        else if (!utilsIsObjectNullOrUndefined(searchFilter.sensingPeriodFrom))
        {
            sFilter += '( beginPosition:['+ this.m_oAdvancedSearchService.formatDateFrom_(searchFilter.sensingPeriodFrom) +
                ' TO NOW] AND endPosition:[' + this.m_oAdvancedSearchService.formatDateFrom_(searchFilter.sensingPeriodFrom) + ' TO NOW] )';
        }
        else if(!utilsIsObjectNullOrUndefined(searchFilter.sensingPeriodTo))
        {
            sFilter += '( beginPosition:[ * TO ' + this.m_oAdvancedSearchService.formatDateTo_(searchFilter.sensingPeriodTo) + '] AND endPosition:[* TO ' + this.m_oAdvancedSearchService.formatToDate(searchfilter.sensingPeriodTo) + ' ] )';
        }
        if(!utilsIsObjectNullOrUndefined(searchFilter.ingestionFrom) && !utilsIsObjectNullOrUndefined(searchFilter.ingestionTo))
        {
            sFilter += ((sFilter)?' AND':'') + '( ingestionDate:['+ this.m_oAdvancedSearchService.formatDateFrom_(searchFilter.ingestionFrom) +
                ' TO ' + this.m_oAdvancedSearchService.formatDateTo_(searchFilter.ingestionTo) + ' ] )';
        }
        else if (!utilsIsObjectNullOrUndefined(searchFilter.ingestionFrom))
        {
            sFilter += ((sFilter)?' AND':'') + '( ingestionDate:['+ this.m_oAdvancedSearchService.formatDateFrom_(searchFilter.ingestionFrom) +' TO NOW] )';
        }
        else if(!utilsIsObjectNullOrUndefined(searchFilter.ingestionTo))
        {
            sFilter += ((sFilter)?' AND':'') + '( ingestionDate:[ * TO ' + this.m_oAdvancedSearchService.formatDateTo_(searchFilter.ingestionTo) + ' ] )';
        }

        return sFilter;
    };


    ImportController.prototype.updateAdvancedSearch = function(){

        var oAdvancedSensingFrom = null;
        if (!utilsIsObjectNullOrUndefined(this.m_oModel.sensingPeriodFrom) && this.m_oModel.sensingPeriodFrom!=="") {
            oAdvancedSensingFrom = this.m_fUtcDateConverter(this.m_oModel.sensingPeriodFrom);
        }
        var oAdvancedSensingTo = null;
        if (!utilsIsObjectNullOrUndefined(this.m_oModel.sensingPeriodTo) && this.m_oModel.sensingPeriodTo !=="") {
            oAdvancedSensingTo = this.m_fUtcDateConverter(this.m_oModel.sensingPeriodTo);
        }
        var oAdvancedIngestionFrom = null;
        if (!utilsIsObjectNullOrUndefined(this.m_oModel.ingestionFrom) && this.m_oModel.ingestionFrom !== "") {
            oAdvancedIngestionFrom = this.m_fUtcDateConverter(this.m_oModel.ingestionFrom);
        }
        var oAdvancedIngestionTo = null;
        if (!utilsIsObjectNullOrUndefined(this.m_oModel.ingestionTo) && this.m_oModel.ingestionTo !== "") {
            oAdvancedIngestionTo = this.m_fUtcDateConverter(this.m_oModel.ingestionTo);
        }

        var advancedFilter = {
            sensingPeriodFrom : oAdvancedSensingFrom,
            sensingPeriodTo: oAdvancedSensingTo,
            ingestionFrom: oAdvancedIngestionFrom,
            ingestionTo: oAdvancedIngestionTo
        };

        var sFilterQuery = this.getAdvancedDateFilterQuery(advancedFilter);

        // update advanced filter for save search
        //this.m_oAdvancedSearchService.setAdvancedSearchFilter(advancedFilter, this.m_oModel);
        this.m_oSearchService.setAdvancedFilter(sFilterQuery); //this.m_oAdvancedSearchService.getAdvancedSearchFilter()

    };

    ImportController.prototype.updateMissionSelection = function(index){

        if(!this.m_aoMissions[index].selected) {
            for(var i=0; i<this.m_aoMissions[index].filters.length; i++) {
                this.m_aoMissions[index].filters[i].indexvalue = '';
            }
        }
        this.setFilter();

    };

    ImportController.prototype.setFilter = function() {

        this.m_oAdvancedFilterService.setAdvancedFilter(this.m_aoMissions);
        this.m_oModel.missionFilter = this.m_oAdvancedFilterService.getAdvancedFilter();
        this.m_oSearchService.setMissionFilter(this.m_oModel.missionFilter);
    };

    ImportController.prototype.clearInput = function(parentIndex, index){
        if(this.m_aoMissions[parentIndex] && this.m_aoMissions[parentIndex].filters[index]) {

            let visibleFilters = [];

            for (let filter of this.m_aoMissions[parentIndex].filters) {
                let isFilterVisible = true;

                if (filter.visibilityConditions) {

                    let visibilityConditionsArray = filter.visibilityConditions.split("&");

                    for (let visibilityCondition of visibilityConditionsArray) {

                        let innerVisibilityConditions;
                        if (visibilityCondition.startsWith("(") && visibilityCondition.endsWith(")")) {
                            innerVisibilityConditions = visibilityCondition.substring(1, visibilityCondition.length - 1);
                        } else {
                            innerVisibilityConditions = visibilityCondition;
                        }

                        if (innerVisibilityConditions.includes("|")) {
                            let innerVisibilityConditionsArray = innerVisibilityConditions.split("|");

                            let innerFilterVisibleFlag = false;

                            for (let innerVisibilityCondition of innerVisibilityConditionsArray) {
                                if (this.m_oModel.missionFilter.includes(innerVisibilityCondition)) {
                                    innerFilterVisibleFlag = true;
                                    break;
                                }
                            }

                            if (!innerFilterVisibleFlag) {
                                isFilterVisible = false;
                                break;
                            }
                        } else {
                            if (!this.m_oModel.missionFilter.includes(visibilityCondition)) {
                                isFilterVisible = false;
                                break;
                            }
                        }
                    }

                }

                if (isFilterVisible) {
                    visibleFilters.push(filter);
                }
            }

            visibleFilters[index].indexvalue = "";
        }

    };

    ImportController.prototype.searchAllSelectedProviders = function()
    {
        if( (this.thereIsAtLeastOneProvider() === false) || (this.m_bIsVisibleListOfLayers || this.m_bisVisibleLocalStorageInputs)) return false;
        var iNumberOfProviders = this.m_aListOfProvider.length;

        for(var iIndexProvider = 0 ; iIndexProvider < iNumberOfProviders; iIndexProvider++)
        {
            if(this.m_aListOfProvider[iIndexProvider].selected === true)
            {

                this.searchAndCount(this.m_aListOfProvider[iIndexProvider]);
            }
        }
        return true;
    };

    ImportController.prototype.searchAndCount = function(oProvider)
    {
        var oController = this;

        if(oController.thereIsAtLeastOneProvider() === false) return false;
        if(utilsIsObjectNullOrUndefined(oProvider) === true) return false;

        oController.m_bClearFiltersEnabled = false;
        //delete layers and relatives rectangles in map
        oController.deleteProducts(oProvider.name);
        //hide previous results
        oController.m_bIsVisibleListOfLayers = true;
        oController.m_bIsPaginatedList = true;

        if(oController.m_oModel.textQuery.endsWith('*')){
            oController.m_oSearchService.setTextQuery("*" + oController.m_oModel.textQuery + "*");
        }
        oController.m_oSearchService.setTextQuery(oController.m_oModel.textQuery);
        oController.m_oSearchService.setGeoselection(oController.m_oModel.geoselection);
        var aoProviders = [];
        aoProviders.push(oProvider);
        oController.m_oSearchService.setProviders(aoProviders);

        var oProvider = oController.m_oPageService.getProviderObject(oProvider.name);
        var iOffset = oController.m_oPageService.calcOffset(oProvider.name);
        oController.m_oSearchService.setOffset(iOffset);//default 0 (index page)
        oController.m_oSearchService.setLimit(oProvider.productsPerPageSelected);// default 10 (total of element per page)
        oProvider.isLoaded = false;
        oProvider.totalOfProducts = 0;

        oController.m_oSearchService.getProductsCount().then(
            function(result)
            {
                if(result)
                {
                    if(utilsIsObjectNullOrUndefined(result.data) === false )
                    {
                        oProvider.totalOfProducts = result.data;
                        //calc number of pages
                        var remainder = oProvider.totalOfProducts % oProvider.productsPerPageSelected;
                        oProvider.totalPages =  Math.floor(oProvider.totalOfProducts / oProvider.productsPerPageSelected);
                        if(remainder !== 0) oProvider.totalPages += 1;
                    }
                }
            }, function errorCallback(response) {
                console.log("Impossible get products number");
            });

        oController.m_oSearchService.search().then(function(result){
            var sResults = result;

            if(!utilsIsObjectNullOrUndefined(sResults))
            {
                if (!utilsIsObjectNullOrUndefined(sResults.data) && sResults.data != "" ) {
                    var aoData = sResults.data;
                    oController.generateLayersList(aoData);
                }

                oProvider.isLoaded = true;
            }
        }, function errorCallback(response) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN OPEN SEARCH REQUEST");
            oProvider.isLoaded = true;
            // oController.m_bIsVisibleListOfLayers = false;//visualize filter list
            // oController.m_oResultsOfSearchService.setIsVisibleListOfProducts(oController.m_bIsVisibleListOfLayers );
        });


    };

    ImportController.prototype.search = function(oProvider, oThat)
    {
        var oController = this;
        if(utilsIsObjectNullOrUndefined(oThat) === false) oController = oThat;

        if(oController.thereIsAtLeastOneProvider() === false) return false;
        if(utilsIsObjectNullOrUndefined(oProvider) === true) return false;

        oController.m_bClearFiltersEnabled = false;
        //delete layers and relatives rectangles in map
        oController.deleteProducts(oProvider.name);
        //hide previous results
        oController.m_bIsVisibleListOfLayers = true;
        oController.m_bIsPaginatedList = true;
        //TODO
        // "*" + oController.m_oModel.textQuery + "*" fix
        // oController.m_oSearchService.setTextQuery("*" + oController.m_oModel.textQuery + "*");
        oController.m_oSearchService.setTextQuery(oController.m_oModel.textQuery);
        oController.m_oSearchService.setGeoselection(oController.m_oModel.geoselection);
        var aoProviders = [];
        aoProviders.push(oProvider);
        oController.m_oSearchService.setProviders(aoProviders);

        var oProvider = oController.m_oPageService.getProviderObject(oProvider.name);
        var iOffset = oController.m_oPageService.calcOffset(oProvider.name);
        oController.m_oSearchService.setOffset(iOffset);//default 0 (index page)
        oController.m_oSearchService.setLimit(oProvider.productsPerPageSelected);// default 10 (total of element per page)
        oProvider.isLoaded = false;

        var sMessage = oController.m_oTranslate.instant("MSG_SEARCH_ERROR");

        oController.m_oSearchService.search().then(function(result){
                var sResults = result;

                if(!utilsIsObjectNullOrUndefined(sResults))
                {
                    if (!utilsIsObjectNullOrUndefined(sResults.data) && sResults.data != "" ) {
                        var aoData = sResults.data;
                        oController.generateLayersList(aoData);
                    }

                    oProvider.isLoaded = true;
                }
            }, function errorCallback(response) {
                utilsVexDialogAlertTop(sMessage);
                oProvider.isLoaded = true;
            });


    };



    ImportController.prototype.searchListAllSelectedProviders = function()
    {
        if((this.m_bIsVisibleListOfLayers || this.m_bisVisibleLocalStorageInputs)) return false;

        // Check input data
        if(this.thereIsAtLeastOneProvider() === false) {
            var sError= this.m_oTranslate.instant("MSG_SEARCH_SELECT_PROVIDER");
            utilsVexDialogAlertDefault(sError,null);
            return false;
        }

        if (utilsIsStrNullOrEmpty(this.m_oModel.geoselection)) {
            var sError= this.m_oTranslate.instant("MSG_SEARCH_ERROR_BBOX");
            utilsVexDialogAlertDefault(sError,null);
            return false;
        }

        var iNumberOfProviders = this.m_aListOfProvider.length;

        for(var iIndexProvider = 0 ; iIndexProvider < iNumberOfProviders; iIndexProvider++)
        {
            if(this.m_aListOfProvider[iIndexProvider].selected === true)
            {
                this.searchList(this.m_aListOfProvider[iIndexProvider]);
            }
        }
        return true;
    };

    /**
     * Executes a not-paginated query for a multi period search
     * @param oProvider
     * @param oThat
     * @returns {boolean}
     */
    ImportController.prototype.searchList = function(oProvider, oThat)
    {
        // Take reference to the controller
        var oController = this;

        if(utilsIsObjectNullOrUndefined(oThat) === false)
        {
            oController = oThat;
        }

        // Check input data
        if(oController.thereIsAtLeastOneProvider() === false || utilsIsObjectNullOrUndefined(oProvider) === true) {
            return false;
        }

        if (utilsIsStrNullOrEmpty(oController.m_oModel.geoselection)) {
            return false;
        }

        oController.m_bClearFiltersEnabled = false;
        //delete layers and relatives rectangles in map
        oController.deleteProducts(oProvider.name);
        //hide previous results
        oController.m_bIsVisibleListOfLayers = true;
        oController.m_bIsPaginatedList = false;
        oController.m_oSearchService.setTextQuery(oController.m_oModel.textQuery);
        oController.m_oSearchService.setGeoselection(oController.m_oModel.geoselection);
        var aoProviders = [];
        aoProviders.push(oProvider);
        oController.m_oSearchService.setProviders(aoProviders);

        // Pagination Info: should be refactored, not needed in the list version
        var oProvider = oController.m_oPageService.getProviderObject(oProvider.name);
        var iOffset = oController.m_oPageService.calcOffset(oProvider.name);
        oController.m_oSearchService.setOffset(iOffset);//default 0 (index page)
        oController.m_oSearchService.setLimit(oProvider.productsPerPageSelected);// default 10 (total of element per page)
        oProvider.isLoaded = false;
        oProvider.totalOfProducts = 0;

        // Generation of different time filters
        var asTimePeriodsFilters = [];

        // For each saved period
        for (var iPeriods = 0; iPeriods<this.m_oAdvanceFilter.savedData.length; iPeriods++) {

            // Prepare input data for date conversion
            var oAdvancedSensingFrom = null;
            if (!utilsIsObjectNullOrUndefined(this.m_oAdvanceFilter.savedData[iPeriods].data.dateSensingPeriodFrom) && this.m_oAdvanceFilter.savedData[iPeriods].data.dateSensingPeriodFrom!=="") {
                oAdvancedSensingFrom = this.m_fUtcDateConverter(this.m_oAdvanceFilter.savedData[iPeriods].data.dateSensingPeriodFrom);
            }
            var oAdvancedSensingTo = null;
            if (!utilsIsObjectNullOrUndefined(this.m_oAdvanceFilter.savedData[iPeriods].data.dateSensingPeriodTo) && this.m_oAdvanceFilter.savedData[iPeriods].data.dateSensingPeriodTo !=="") {
                oAdvancedSensingTo = this.m_fUtcDateConverter(this.m_oAdvanceFilter.savedData[iPeriods].data.dateSensingPeriodTo);
            }
            var oAdvancedIngestionFrom = null;
            var oAdvancedIngestionTo = null;

            var advancedFilter = {
                sensingPeriodFrom : oAdvancedSensingFrom,
                sensingPeriodTo: oAdvancedSensingTo,
                ingestionFrom: oAdvancedIngestionFrom,
                ingestionTo: oAdvancedIngestionTo
            };

            // Get the time filter object
            var sTimeFilter = this.getAdvancedDateFilterQuery(advancedFilter);
            // Push it to the queries list
            asTimePeriodsFilters.push(sTimeFilter);
        }

        // Call the complete Get Product Count for all the queries of this provider
        oController.m_oSearchService.getProductsListCount(asTimePeriodsFilters).then(
            function(result)
            {
                if(result)
                {
                    if(utilsIsObjectNullOrUndefined(result.data) === false )
                    {
                        oProvider.totalOfProducts = result.data;
                        //calc number of pages
                        var remainder = oProvider.totalOfProducts % oProvider.productsPerPageSelected;
                        oProvider.totalPages =  Math.floor(oProvider.totalOfProducts / oProvider.productsPerPageSelected);
                        if(remainder !== 0) oProvider.totalPages += 1;
                    }
                }
            }, function errorCallback(response) {
                console.log("Impossible get products number");
            });

        var sMessage = oController.m_oTranslate.instant("MSG_SEARCH_ERROR");

        // Call the complete Search for all the queries of this provider
        oController.m_oSearchService.searchList(asTimePeriodsFilters).then(function(result){
            var sResults = result;

            if(!utilsIsObjectNullOrUndefined(sResults))
            {
                if (!utilsIsObjectNullOrUndefined(sResults.data) && sResults.data != "" ) {
                    var aoData = sResults.data;
                    oController.generateLayersList(aoData)//.feed;
                }

                oProvider.isLoaded = true;
            }
        }, function errorCallback(response) {
            var oDialog = utilsVexDialogAlertBottomRightCorner(sMessage);
            utilsVexCloseDialogAfter(4000, oDialog);

            oController.m_bIsVisibleListOfLayers = false;//visualize filter list
            oController.m_oResultsOfSearchService.setIsVisibleListOfProducts(oController.m_bIsVisibleListOfLayers );
        });


    };

        /**
         * Download a product
         * @param sUrl
         * @param sWorkspaceId
         * @param sBounds
         * @param sProvider
         * @param oCallback
         * @param oError
         */
    ImportController.prototype.downloadProduct = function(sUrl, sFileName, sWorkspaceId,sBounds,sProvider,oCallback,oError)
    {
        if(utilsIsObjectNullOrUndefined(oCallback) === true)
        {
            var sMessage = this.m_oTranslate.instant("MSG_IMPORTING");
            oCallback = function (data, status) {
                var oDialog = utilsVexDialogAlertBottomRightCorner(sMessage);
                utilsVexCloseDialogAfter("3000",oDialog);
            }
        }
        if(utilsIsObjectNullOrUndefined(oError) === true)
        {
            var sMessage = this.m_oTranslate.instant("MSG_ERROR_IMPORTING");
            oError = function (data,status) {
                utilsVexDialogAlertTop(sMessage);
                // oProduct.isDisabledToDoDownload = false;
            };
        }
        this.m_oFileBufferService.download(sUrl,sFileName, sWorkspaceId,sBounds,sProvider).then(oCallback, oError);
    };

    ImportController.prototype.openSelectWorkspaceDialog = function(oCallback){
        // var oThat = this;
        this.m_oModalService.showModal({
            templateUrl: "dialogs/downloadProductInWorkspace/DownloadProductInWorkspaceView.html",
            controller: "DownloadProductInWorkspaceController",
        }).then(function(modal){
            modal.element.modal();
            modal.close.then(oCallback);
        });

        return true;
    };


    ImportController.prototype.getFilterPlaceholder = function(filter)
    {
        if(filter["indexhint"] != undefined){
            return filter["indexhint"];
        }
        return "";

    }



    /**
     * downloadProduct
     * @param oLayer
     * @returns {boolean}
     */
    /* us server Download the product in geoserver, the parameter oLayer = product*/
    ImportController.prototype.downloadProductInWorkspace = function(oProduct)
    {
        if(utilsIsObjectNullOrUndefined(oProduct))
            return false;
        var oThat = this;
        var oDialogCallback = function(result) {

            if(utilsIsObjectNullOrUndefined(result))
                return false;
            oProduct.isDisabledToDoDownload = true;
            var oWorkSpace = result;

            if(utilsIsObjectNullOrUndefined(oWorkSpace) || utilsIsObjectNullOrUndefined(oProduct))
            {
                console.log("Error there isn't workspaceID or layer");
                return false;
            }

            var url = oProduct.link;
            if(utilsIsObjectNullOrUndefined(url))
            {
                console.log("Error there isn't workspaceID or layer")
                return false;
            }
            var sMessage = this.m_oTranslate.instant("MSG_ERROR_IMPORTING");

            var oError = function (data,status) {
                utilsVexDialogAlertTop(sMessage);
                oProduct.isDisabledToDoDownload = false;
            }

            oThat.downloadProduct(url,oProduct.title,oWorkSpace.workspaceId,oProduct.bounds.toString(),oProduct.provider,null,oError);

            return true;
        };

        this.openSelectWorkspaceDialog(oDialogCallback);
    };

    ImportController.prototype.downloadProductsListInWorkspace = function(aoProducts)
    {
        if(utilsIsObjectNullOrUndefined(aoProducts))
            return false;
        var oThat = this;

        var sMessage = this.m_oTranslate.instant("MSG_ERROR_IMPORTING");

        var oDialogCallback = function(result) {

            if(utilsIsObjectNullOrUndefined(result))
                return false;
            var oWorkSpace = result;
            var iNumberOfProducts = aoProducts.length;
            if(utilsIsObjectNullOrUndefined(oWorkSpace) )
            {
                console.log("Error there isn't workspaceID");
                return false;
            }

            for(var iIndexProduct = 0 ; iIndexProduct < iNumberOfProducts; iIndexProduct++)
            {
                aoProducts[iIndexProduct].isDisabledToDoDownload = true;
                var url = aoProducts[iIndexProduct].link;
                var oError = function (data,status) {
                    utilsVexDialogAlertTop(sMessage);
                    aoProducts[iIndexProduct].isDisabledToDoDownload = false;
                }

                oThat.downloadProduct(url,aoProducts[iIndexProduct].title, oWorkSpace.workspaceId,aoProducts[iIndexProduct].bounds.toString(),aoProducts[iIndexProduct].provider,null,oError);
            }
            return true;
        };

        this.openSelectWorkspaceDialog(oDialogCallback);
    }

    ImportController.prototype.clearFilter = function() {
        for(var i=0; i < this.m_aoMissions.length; i++)
        {
            this.m_aoMissions[i].selected = false;
            this.updateMissionSelection(i);
        }
        this.setFilter();

        /* clear date */
        this.updateAdvancedSearch();
        this.setDefaultData();


        /* remove rectangle in map */
        if(!utilsIsObjectNullOrUndefined(this.m_aoProductsList))
        {
            var iNumberOfProducts = this.m_aoProductsList.length;
            var oRectangle;
            for(var iIndex = 0; iIndex < iNumberOfProducts; iIndex++)
            {
                oRectangle = this.m_aoProductsList[iIndex].rectangle;
                this.m_oMapService.removeLayerFromMap(oRectangle);
            }
            this.m_aoProductsList = [];
        }

        this.m_oMapService.deleteDrawShapeEditToolbar();

        /* go back */
        this.setPaginationVariables();
    }

    ImportController.prototype.openSensingPeriodFrom = function($event) {
        this.m_oStatus.openedSensingPeriodFrom = true;
        this.m_DatePickerPosition($event);
    };
    ImportController.prototype.openSensingPeriodTo = function($event) {
        this.m_oStatus.openedSensingPeriodTo = true;
        this.m_DatePickerPosition($event);
    };
    ImportController.prototype.openIngestionFrom = function($event) {
        this.m_oStatus.openedIngestionFrom = true;
        this.m_DatePickerPosition($event);
    };
    ImportController.prototype.openIngestionTo = function($event) {
        this.m_oStatus.openedIngestionTo = true;
        this.m_DatePickerPosition($event);
    };

    ImportController.prototype.openPeriodForMergeSearch = function($event)
    {
        this.m_oMergeSearch.statusIsOpen = true;
        this.m_DatePickerPosition($event);
    };

    ImportController.prototype.disabled = function(date, mode) {
        return false;
    };

    ImportController.prototype.changeProductsPerPage = function()
    {

    };


    /**
     * updateLayerListForActiveTab
     * @param iActiveTab
     */
    ImportController.prototype.updateLayerListForActiveTab = function(sProvider) {
        var oController = this;

        var aaoAllBounds = [];

        oController.deleteLayers();

        for(var iIndexData = 0; iIndexData < oController.m_aoProductsList.length; iIndexData++)
        {
            if (oController.m_aoProductsList[iIndexData].provider !== sProvider) continue;

            var oRectangle = oController.m_oMapService.addRectangleByBoundsArrayOnMap(oController.m_aoProductsList[iIndexData].bounds ,null,iIndexData);
            if(utilsIsObjectNullOrUndefined(oRectangle) === false)
            {
                oController.m_aoProductsList[iIndexData].rectangle = oRectangle
            }
            aaoAllBounds.push(oController.m_aoProductsList[iIndexData].bounds);
        }

        if (aaoAllBounds.length > 0 && aaoAllBounds[0] && aaoAllBounds[0].length) {
            oController.m_oMapService.zoomOnBounds(aaoAllBounds);
        }
    };

    /*
    * Generate layers list
    * */
    ImportController.prototype.generateLayersList = function(aData)
    {
        var oController = this;
        if(utilsIsObjectNullOrUndefined(aData) === true) return false;

        // var aaoAllBounds = [];

        var iDataLength = aData.length;
        for(var iIndexData = 0; iIndexData < iDataLength; iIndexData++)
        {
            var oSummary =  this.stringToObjectSummary(aData[iIndexData].summary);//change summary string to array
            aData[iIndexData].summary = oSummary;

            if(utilsIsObjectNullOrUndefined( aData[iIndexData].preview) || utilsIsStrNullOrEmpty( aData[iIndexData].preview))
                aData[iIndexData].preview = "assets/icons/ImageNotFound.svg";//default value ( set it if there isn't the image)

            if (utilsIsObjectNullOrUndefined(aData[iIndexData].footprint)==false) {
                //get bounds
                var aoBounds = oController.polygonToBounds( aData[iIndexData].footprint);
                aData[iIndexData].bounds = aoBounds;
                // aaoAllBounds.push(aoBounds);
            }

            aData[iIndexData].rectangle = null;
            aData[iIndexData].checked = false;

            oController.m_aoProductsList.push(aData[iIndexData]);
        }

        var iActive = this.m_iActiveProvidersTab;

        for (var i=0; i<oController.m_aListOfProvider.length; i++) {
            if (oController.m_aListOfProvider[i].selected) break;

            if (i>=iActive) iActive++;
        }


        var sProvider = oController.m_aListOfProvider[iActive].name;
        oController.updateLayerListForActiveTab(sProvider);
        // oController.m_oMapService.zoomOnBounds(aaoAllBounds);
    };

    /*GetRelativeOrbit*/
    ImportController.prototype.getRelativeOrbit = function (aArrayInput) {

        if(utilsIsObjectNullOrUndefined(aArrayInput) === true) return null;
        var iLengthArray = aArrayInput.length;

        for(var iIndexArray = 0 ; iIndexArray < iLengthArray; iIndexArray++)
        {
            //if there are property name & name == relativeorbitnumber
            if( (utilsIsObjectNullOrUndefined(aArrayInput[iIndexArray].name)!== true ) && ( aArrayInput[iIndexArray].name ==="relativeorbitnumber"))
            {
                return aArrayInput[iIndexArray].content;//return relative orbit
            }

        }
        return null;
    };

    /*Get Download link */
    ImportController.prototype.getDownloadLink = function(oLayer)
    {
        if(utilsIsObjectNullOrUndefined(oLayer))
            return null;
        if(utilsIsObjectNullOrUndefined(oLayer.link))
            return null;
        var iLengthLink = oLayer.link.length;

        for(var iIndex = 0 ; iIndex < iLengthLink; iIndex++)
        {

            if(utilsIsObjectNullOrUndefined(oLayer.link[iIndex].rel))
            {
                return oLayer.link[iIndex].href;
            }
        }
            return null;
    };

    /*
    * Get preview in layer
    * If method return null somethings doesn't works
    * N.B. the method can return empty image
    * */
    ImportController.prototype.getPreviewLayer = function(oLayer)
    {
        if(utilsIsObjectNullOrUndefined(oLayer)) return null;
        if(utilsIsObjectNullOrUndefined(oLayer.link)) return null;
        var iLinkLength = oLayer.link.length;

        for(var iIndex = 0; iIndex < iLinkLength; iIndex++)
        {
            if(oLayer.link[iIndex].rel == "icon"){
                if( (!utilsIsObjectNullOrUndefined(oLayer.link[iIndex].image) ) && ( !utilsIsObjectNullOrUndefined(oLayer.link[iIndex].image.content) ) ){
                    return oLayer.link[iIndex].image.content;
                }
            }
        }
        return null;
    }

    /* CONVERT POLYGON FORMAT TO BOUND FORMAT */
    ImportController.prototype.polygonToBounds=function (sContent) {
        // sContent="POLYGON ((-180.0 -64.991,-120.001495 -64.991,-60.003 -64.991,-0.004501343 -64.991,59.994003 -64.991,119.99249 -64.991,179.991 -64.991,179.991 -32.490498,179.991 0.010002136,119.99249 0.01,59.993996 0.01,-0.004501343 0.01,-60.003006 0.01,-120.001495 0.01,-180.0 0.01,-180.0 -32.4905,-180.0 -64.991,-180.0 -0.01,-120.001495 -0.01,-60.003 -0.01,-0.004501343 -0.01,59.994003 -0.01,119.99249 -0.01,179.991 -0.01,179.991 37.495003,179.991 75.0,119.99249 75.0,59.993996 75.0,-0.004501343 75.0,-60.003006 75.0,-120.001495 75.0,-180.0 75.0,-180.0 37.495,-180.0 -0.010002136))";
        // sContent = "POLYGON ((-180.0 -64.991,-120.001495 -64.991,-60.003 -64.991,-0.004501343 -64.991,59.994003 -64.991,119.99249 -64.991,179.991 -64.991,179.991 -32.490498,179.991 0.010002136,119.99249 0.01,59.993996 0.01,-0.004501343 0.01,-60.003006 0.01,-120.001495 0.01,-180.0 0.01,-180.0 -32.4905,-180.0 -64.991,-180.0 -0.01,-120.001495 -0.01,-60.003 -0.01,-0.004501343 -0.01,59.994003 -0.01,119.99249 -0.01,179.991 -0.01,179.991 37.495003,179.991 75.0,119.99249 75.0,59.993996 75.0,-0.004501343 75.0,-60.003006 75.0,-120.001495 75.0,-180.0 75.0,-180.0 37.495,-180.0 -0.010002136))";
        // sContent = "POLYGON ((-180.0 -54.991,-120.001495 -54.991,-60.003 -54.991,-0.004501343 -54.991,59.994003 -54.991,119.99249 -54.991,179.991 -54.991,179.991 -0.0099983215,119.99249 -0.01,59.993996 -0.01,-0.004501343 -0.01,-60.003006 -0.01,-120.001495 -0.01,-180.0 -0.01,-180.0 -54.991,-180.0 0.01,-120.001495 0.01,-60.003 0.01,-0.004501343 0.01,59.994003 0.01,119.99249 0.01,179.991 0.01,179.991 37.504997,179.991 75.0,119.99249 75.0,59.993996 75.0,-0.004501343 75.0,-60.003006 75.0,-120.001495 75.0,-180.0 75.0,-180.0 37.505,-180.0 0.010002136))";
        sContent = sContent.replace("MULTIPOLYGON ","");
        sContent = sContent.replace("MULTIPOLYGON","");
        sContent = sContent.replace("POLYGON ","");
        sContent = sContent.replace("POLYGON","");
        sContent = sContent.replace("(((","");
        sContent = sContent.replace(")))","");
        sContent = sContent.replace("((","");
        sContent = sContent.replace("))","");
        sContent = sContent.replace(/, /g,",");

        // sContent= "-13.554 158.521,-13.936 158.45,-14.493 158.348,-14.394 157.898,-14.351 157.707,-14.085 156.505,-13.983 156.05,-13.95 155.906,-13.919 155.764,-13.887 155.626,-13.857 155.49,-13.768 155.101,-13.685 154.733,-13.657 154.615,-13.605 154.386,-13.529 154.057,-13.319 153.161,-13.212 152.713,-13.192 152.626,-13.111 152.293,-13.073 152.132,-13.053 152.053,-12.872 151.31,-12.706 150.638,-12.556 150.025,-12.017 150.145,-12.011 150.107,-11.992 150.0,-8.376 150.8,-8.263 150.824,-6.116 151.299,-6.003 151.323,-2.726 152.048,-2.612 152.074,-1.595 152.299,-1.729 152.964,-1.779 153.203,-1.833 153.451,-1.888 153.709,-1.946 153.976,-1.972 154.093,-1.876 154.114,-1.935 154.397,-2.027 154.847,-2.06 155.005,-2.092 155.166,-2.126 155.331,-2.306 156.228,-2.384 156.625,-2.426 156.834,-2.539 156.809,-2.577 156.998,-2.623 157.22,-2.669 157.451,-2.718 157.69,-2.82 158.198,-2.932 158.749,-2.991 159.043,-3.189 160.008,-3.263 160.362,-3.426 161.126,-3.76 161.055,-3.871 161.032,-4.093 160.984,-4.204 160.961,-4.315 160.937,-4.538 160.89,-4.76 160.844,-4.871 160.82,-6.427 160.498,-6.761 160.431,-6.983 160.385,-7.205 160.341,-7.539 160.273,-7.873 160.207,-7.984 160.184,-8.095 160.162,-9.207 159.942,-9.318 159.921,-9.541 159.877,-10.097 159.77,-10.43 159.705,-10.542 159.684,-10.653 159.662,-11.098 159.578,-11.209 159.556,-11.876 159.43,-11.987 159.41,-12.321 159.347,-12.432 159.327,-12.766 159.264,-12.877 159.244,-12.988 159.223,-13.1 159.203,-13.433 159.141,-13.545 159.121,-13.656 159.1,-13.584 158.694,-13.554 158.521";
        sContent = sContent.split(",");
        var aasNewContent = [];
        for (var iIndexBounds = 0; iIndexBounds < sContent.length; iIndexBounds++)
        {
            var aBounds = sContent[iIndexBounds];
            var aNewBounds = aBounds.split(" ");

            //var aoOutputPoint = proj4(sSourceProjection,sDestinationProjection,aNewBounds);

            //var aBounds = sContent[iIndexBounds];
            //var aNewBounds = aBounds.split(" ");

            var oLatLonArray = [];

            try{
                oLatLonArray[0] = JSON.parse(aNewBounds[1]); //Lat
                oLatLonArray[1] = JSON.parse(aNewBounds[0]); //Lon
            }catch(err){
                console.log("Function polygonToBounds: Error in parse operation");
                return [];
            }

            aasNewContent.push(oLatLonArray);
        }
        return aasNewContent;
    };

    /*
        Usually the summary format is a string = "date:...,instrument:...,mode:...,satellite:...,size:...";
     */
    ImportController.prototype.stringToObjectSummary = function(sSummary)
    {
        if(utilsIsObjectNullOrUndefined(sSummary)) return null;
        if(utilsIsStrNullOrEmpty(sSummary)) return null;

        //split summary
        var aSplit = sSummary.split(",");
        //make object summary
        var oNewSummary = {Date:"",Instrument:"",Mode:"",Satellite:"",Size:""};
        var asSummary = ["Date","Instrument","Mode","Satellite","Size"];
        var iSplitLength=aSplit.length;
        var iSummaryLength = asSummary.length;

        /* it dosen't know if date,instrument,mode...are the first element,second element,... of aSplit array
        * we fix it with this code
        * */
        for(var iIndex = 0; iIndex < iSplitLength; iIndex++ )
        {
            for(var jIndex = 0; jIndex < iSummaryLength;jIndex++ )
            {
                if(utilsStrContainsCaseInsensitive(aSplit[iIndex],asSummary[jIndex]))
                {
                    var oData = aSplit[iIndex].replace(asSummary[jIndex]+":","");
                    oData = oData.replace(" ","");//remove spaces from data
                    oNewSummary[asSummary[jIndex].replace(":","")] = oData ;
                    break;
                }
            }
        }

        return oNewSummary;
    };

    /*********************** CHANGE CSS RECTANGLE (LEAFLET MAP) ****************************/
    /*
    *   Change style of rectangle when the mouse is over the layer (TABLE CASE)
    * */
    ImportController.prototype.changeStyleRectangleMouseOver=function(oRectangle)
    {
        if(utilsIsObjectNullOrUndefined(oRectangle))
        {
            console.log("Error: rectangle is undefined ");
            return false;
        }
        if(utilsIsObjectNullOrUndefined(oRectangle._rawPxBounds))
        {
            return false;
        }
        oRectangle.setStyle({weight:3,fillOpacity:0.7});
    };

    /*
     *   Change style of rectangle when the mouse is leave the layer (TABLE CASE)
     * */
    ImportController.prototype.changeStyleRectangleMouseLeave=function(oRectangle)
    {
        if(utilsIsObjectNullOrUndefined(oRectangle))
        {
            console.log("Error: rectangle is undefined ");
            return false;
        }
        if(utilsIsObjectNullOrUndefined(oRectangle._rawPxBounds))
        {
            return false;
        }
        oRectangle.setStyle({weight:1,fillOpacity:0.2});
    };

    /************************************************************/

    /* the method take in input a rectangle, return the layer index Bound with rectangle
    *  parameters: rectangle
    * */
    ImportController.prototype.indexOfRectangleInLayersList=function(oRectangle) {
        var oController = this;
        if (utilsIsObjectNullOrUndefined(oController.m_aoProductsList)) {
            console.log("Error LayerList is empty");
            return false;
        }
        var iLengthLayersList = oController.m_aoProductsList.length;

        for (var iIndex = 0; iIndex < iLengthLayersList; iIndex++)
        {
            if(oController.m_aoProductsList[iIndex].rectangle == oRectangle)
                return iIndex; // I FIND IT !!
        }
        return -1;//the method doesn't find the Rectangle in LayersList.
    };

    // Set bounds then call m_oMapService.zoomOnBounds(aaBounds)
    ImportController.prototype.zoomOnBounds = function(oRectangle)
    {
        var oBounds = oRectangle.getBounds();
        var oNorthEast = oBounds.getNorthEast();
        var oSouthWest = oBounds.getSouthWest();

        if(utilsIsObjectNullOrUndefined(oNorthEast) || utilsIsObjectNullOrUndefined(oSouthWest))
        {
            console.log("Error in zoom on bounds");
        }
        else
        {
            var aaBounds = [[oNorthEast.lat,oNorthEast.lng],[oSouthWest.lat,oSouthWest.lng]];
            var oController = this;
            if(oController.m_oMapService.zoomOnBounds(aaBounds) == false) console.log("Error in zoom on bounds");
        }
    };

    ImportController.prototype.isEmptyProductsList = function(){
        if(utilsIsObjectNullOrUndefined(this.m_aoProductsList) ) return true;
        if( this.m_aoProductsList.length == 0) return true;

        return false;
    };


    ImportController.prototype.deleteLayers = function()
    {
        if(this.isEmptyProductsList()) return false;
        var iLengthProductsList = this.m_aoProductsList.length;
        var oMap = this.m_oMapService.getMap();
        for(var iIndexProductsList = 0; iIndexProductsList < iLengthProductsList; iIndexProductsList++)
        {
            var oRectangle = this.m_aoProductsList[iIndexProductsList].rectangle;
            if(!utilsIsObjectNullOrUndefined(oRectangle))
                oRectangle.removeFrom(oMap);
        }
    };

    /**
     * deleteProducts
     * @param sProvider
     * @returns {boolean}
     */
    ImportController.prototype.deleteProducts = function(sProvider)
    {
        //check if layers list is empty
        if(this.isEmptyProductsList()) return false;
        var iLengthProductsList = this.m_aoProductsList.length;
        var oMap = this.m_oMapService.getMap();
        /* remove rectangle in map*/
        for(var iIndexProductsList = 0; iIndexProductsList < iLengthProductsList; iIndexProductsList++)
        {
            if( (utilsIsObjectNullOrUndefined(this.m_aoProductsList[iIndexProductsList].provider)=== false) && (this.m_aoProductsList[iIndexProductsList].provider === sProvider))
            {
                var oRectangle = this.m_aoProductsList[iIndexProductsList].rectangle;
                if(!utilsIsObjectNullOrUndefined(oRectangle))
                    oRectangle.removeFrom(oMap);
                if (iIndexProductsList > -1) {
                    this.m_aoProductsList.splice(iIndexProductsList, 1);
                    iLengthProductsList--;
                    iIndexProductsList--;
                }
            }


        }
        //delete layers list
        //this.m_aoProductsList = [];
        return true;
    };


    ImportController.prototype.infoLayer=function(oProduct)
    {
        if(utilsIsObjectNullOrUndefined(oProduct)) return false;

        var oController = this;
        this.m_oModalService.showModal({
            templateUrl: "dialogs/product_info/ProductInfoDialog.html",
            controller: "ProductInfoController",
            inputs: {
                extras: oProduct
            }
        }).then(function(modal) {
            modal.element.modal();
            modal.close.then(function(result) {
                oController.m_oScope.Result = result ;
            });
        });

        return true;
    };
    /**
     *
     * @param oProduct
     * @returns {boolean}
     */
    ImportController.prototype.openModalDownloadProductInSelectedWorkspaces = function(oProduct)
    {
        if(utilsIsObjectNullOrUndefined(oProduct) === true)
        {
            return false;
        }

        var sMessage = this.m_oTranslate.instant("MSG_ADD_TO_WS");
        var sErrorMessage = this.m_oTranslate.instant("MSG_ERROR_IMPORTING");

        var oOptions = {
            titleModal: sMessage,
            buttonName: sMessage
        };

        var oThat = this;
        var oCallback = function(result)
        {
            if(utilsIsObjectNullOrUndefined(result) === true)
            {
                return false;
            }

            var aoWorkSpaces = result;
            var iNumberOfWorkspaces = aoWorkSpaces.length;
            if(utilsIsObjectNullOrUndefined(aoWorkSpaces) )
            {
                console.log("Error there aren't Workspaces");
                return false;
            }
            // download product in all workspaces
            for(var iIndexWorkspace = 0 ; iIndexWorkspace < iNumberOfWorkspaces; iIndexWorkspace++)
            {
                oProduct.isDisabledToDoDownload = true;
                var sUrl = oProduct.link;
                var oError = function (data,status) {
                            utilsVexDialogAlertTop(sErrorMessage);
                            oProduct.isDisabledToDoDownload = false;
                        };

                var sBound = "";

                if (utilsIsObjectNullOrUndefined(oProduct.bounds) == false) {
                    sBound = oProduct.bounds.toString();
                }
                oThat.downloadProduct(sUrl,oProduct.title, aoWorkSpaces[iIndexWorkspace].workspaceId,sBound,oProduct.provider,null,oError);

            }

            oThat.deselectAllProducts();

            return true;
        };

        utilsProjectOpenGetListOfWorkspacesSelectedModal(oCallback,oOptions,this.m_oModalService);
    };

    ImportController.prototype.openModalDownloadSelectedProductsInSelectedWorkspaces = function()
    {
        var aoListOfSelectedProducts = this.getListOfSelectedProducts();
        let oController = this;
        if(utilsIsObjectNullOrUndefined(aoListOfSelectedProducts) === true)
        {
            return false;
        }

        let oActiveProject = oController.m_oConstantsService.getActiveProject();
        let asActiveSubscriptions = oController.m_oConstantsService.getActiveSubscriptions(); 
        if(asActiveSubscriptions.length === 0) {
            let sMessage = "You do not have an Active Subscription right now.<br>Click 'OK' to visit our purchase page";
            utilsVexDialogConfirm(sMessage, function(value) {
                if(value) {
                    oController.m_oState.go("root.subscriptions", {});
                }
            }); 
            return false;
        }

        if(utilsIsObjectNullOrUndefined(oActiveProject)) {
            utilsVexDialogAlertTop("You do not have an active project right now.<br>Please make a selection.")
            return false;
        }

        var sMessage = this.m_oTranslate.instant("MSG_ADD_TO_WS");
        var sErrorMessage = this.m_oTranslate.instant("MSG_ERROR_IMPORTING");

        var oOptions = {
            titleModal: sMessage,
            buttonName: sMessage
        };
        var oThat = this;
        var oCallback= function(result)
        {
            if(utilsIsObjectNullOrUndefined(result) === true)
            {
                return false;
            }
            var aoWorkSpaces = result;
            var iNumberOfWorkspaces = aoWorkSpaces.length;
            var iNumberOfProducts = aoListOfSelectedProducts.length;
            if(utilsIsObjectNullOrUndefined(aoWorkSpaces) )
            {
                console.log("Error there aren't Workspaces");
                return false;
            }

            // download selected products in all workspaces
            //fetch all workspaces
            for(var iIndexWorkspace = 0 ; iIndexWorkspace < iNumberOfWorkspaces; iIndexWorkspace++)
            {
                //fetch all products
                for(var iIndexProduct = 0 ; iIndexProduct < iNumberOfProducts; iIndexProduct++)
                {
                    aoListOfSelectedProducts[iIndexProduct].isDisabledToDoDownload = true;
                    var url = aoListOfSelectedProducts[iIndexProduct].link;
                    var oError = function (data,status) {
                        utilsVexDialogAlertTop(sErrorMessage);
                        aoListOfSelectedProducts[iIndexProduct].isDisabledToDoDownload = false;
                    }

                    oThat.downloadProduct(url, aoListOfSelectedProducts[iIndexProduct].title, aoWorkSpaces[iIndexWorkspace].workspaceId,aoListOfSelectedProducts[iIndexProduct].bounds.toString(),aoListOfSelectedProducts[iIndexProduct].provider,null,oError);

                }
            }
            oThat.deselectAllProducts();

            return true;
        };

        utilsProjectOpenGetListOfWorkspacesSelectedModal(oCallback,oOptions,this.m_oModalService);
    };

    /**
     *
     * @param oMessage
     * @param oController
     */
    ImportController.prototype.receivedRabbitMessage  = function (oMessage, oController) {
        // Check if the message is valid
        if (oMessage == null) return;

        // Check the Result
        if (oMessage.messageResult == "KO") {

            var sOperation = "null";
            if (utilsIsStrNullOrEmpty(oMessage.messageCode) === false  ) sOperation = oMessage.messageCode;

            var sErrorDescription = "";

            if (utilsIsStrNullOrEmpty(oMessage.payload) === false) sErrorDescription = oMessage.payload;
            if (utilsIsStrNullOrEmpty(sErrorDescription) === false) sErrorDescription = "<br>"+sErrorDescription;

            var oDialog = utilsVexDialogAlertTop(oController.m_oTranslate.instant("MSG_ERROR_IN_OPERATION_1") + sOperation +  oController.m_oTranslate.instant("MSG_ERROR_IN_OPERATION_2")+ sErrorDescription);
            utilsVexCloseDialogAfter(10000, oDialog);

            return;
        }

        // Switch the Code
        switch(oMessage.messageCode) {
            case "DOWNLOAD":
            case "GRAPH":
            case "INGEST":
            case "MOSAIC":
            case "SUBSET":
            case "MULTISUBSET":
            case "RASTERGEOMETRICRESAMPLE":
            case "REGRID":
                oController.receivedNewProductMessage(oMessage);
                break;
        }

        utilsProjectShowRabbitMessageUserFeedBack(oMessage, oController.m_oTranslate);
    };

    ImportController.prototype.receivedNewProductMessage = function (oMessage, oController) {

        var sMessage = this.m_oTranslate.instant("MSG_EDIT_PRODUCT_ADDED");

        var oDialog = utilsVexDialogAlertBottomRightCorner(sMessage);
        utilsVexCloseDialogAfter(3000, oDialog);
    };

    ImportController.prototype.openWorkspace = function (sWorkspaceId) {

        var oController = this;

        var sErrorMessage = this.m_oTranslate.instant("MSG_ERROR_READING_WS");

        this.m_oWorkspaceService.getWorkspaceEditorViewModel(sWorkspaceId).then(function (data, status) {
            if (data.data != null)
            {
                if (data.data != undefined)
                {
                    oController.m_oConstantsService.setActiveWorkspace(data.data);
                    oController.m_oActiveWorkspace = oController.m_oConstantsService.getActiveWorkspace();

                    /*Start Rabbit WebStomp*/
                    // oController.m_oRabbitStompService.initWebStomp("ImportController",oController);
                    oController.loadOpenSearchParamsByResultsOfSearchServices(oController);
                    if (!utilsIsObjectNullOrUndefined(oController.m_oActiveWorkspace)) oController.m_oProcessWorkspaceService.loadProcessesFromServer(oController.m_oActiveWorkspace.workspaceId);
                }
            }
        },function (data,status) {
            utilsVexDialogAlertTop(sErrorMessage);
        });
    };

    ImportController.prototype.loadOpenSearchParamsByResultsOfSearchServices = function(oController)
    {
        if(utilsIsObjectNullOrUndefined(oController)) return false;

        /*Load elements by Service if there was a previous search i load*/
        oController.m_oModel.textQuery = oController.m_oResultsOfSearchService.getTextQuery();
        oController.m_oModel.geoselection = oController.m_oResultsOfSearchService.getGeoSelection();
        oController.m_iCurrentPage = oController.m_oResultsOfSearchService.getCurrentPage();
        oController.m_iProductsPerPageSelected = oController.m_oResultsOfSearchService.getProductsPerPageSelected();
        oController.m_aoProductsList = oController.m_oResultsOfSearchService.getProductList();
        oController.m_iTotalPages = oController.m_oResultsOfSearchService.getTotalPages();
        oController.m_bIsVisibleListOfLayers = oController.m_oResultsOfSearchService.getIsVisibleListOfProducts();
        oController.m_iTotalOfProducts = oController.m_oResultsOfSearchService.getTotalOfProducts();
        oController.m_oModel.sensingPeriodFrom = oController.m_oResultsOfSearchService.getSensingPeriodFrom();
        oController.m_oModel.sensingPeriodTo = oController.m_oResultsOfSearchService.getSensingPeriodTo();
        oController.m_oModel.ingestionFrom = oController.m_oResultsOfSearchService.getIngestionPeriodFrom();
        oController.m_oModel.ingestionTo = oController.m_oResultsOfSearchService.getIngestionPeriodTo();
        // oController.m_aListOfProvider = oController.m_oResultsOfSearchService.getProviders();

        /* add rectangle in maps */

        if(utilsIsObjectNullOrUndefined(oController.m_aoProductsList)) return true;

        var iNumberOfProducts = oController.m_aoProductsList.length;

        for(var iIndexProduct = 0; iIndexProduct < iNumberOfProducts ; iIndexProduct++) {
            if (!utilsIsObjectNullOrUndefined(oController.m_aoProductsList[iIndexProduct].rectangle))
            {
                var oMap = oController.m_oMapService.getMap();
                oController.m_aoProductsList[iIndexProduct].rectangle.addTo(oMap);

            }
        }

        return true;
    };

    ImportController.prototype.setPaginationVariables = function()
    {
        this.m_bClearFiltersEnabled = true;

        this.m_bIsVisibleListOfLayers = false;
        //set default pages
        this.m_oPageService.setDefaultPaginationValuesForProvider();

        this.m_oResultsOfSearchService.setIsVisibleListOfProducts(false);
    };

    ImportController.prototype.isPossibleDoDownload = function(oLayer)
    {
        var bReturnValue = false;
        if(utilsIsObjectNullOrUndefined(oLayer))
            return false;
        if(this.m_oProcessWorkspaceService.checkIfFileIsDownloading(oLayer,this.m_oProcessWorkspaceService.getTypeOfProcessProductDownload()) === true)
        {
            bReturnValue = true;
            oLayer.isDisabledToDoDownload = false;
        }

        return bReturnValue;
    };

    ImportController.prototype.visualizeLocalStorageInputs = function(bIsVisible)
    {
        this.m_bisVisibleLocalStorageInputs = !this.m_bisVisibleLocalStorageInputs;
    };

    ImportController.prototype.loadProductsInLocalStorage = function()
    {
        if(utilsIsObjectNullOrUndefined(this.m_oMergeSearch.period)) return false;
        if(utilsIsString(this.m_oMergeSearch.period)) return false;
        if(utilsIsObjectNullOrUndefined(this.m_oMergeSearch.period)) return false;

        return true;
    };

    ImportController.prototype.thereIsAtLeastOneProvider = function()
    {
        var iLengthListOfProvider = this.m_aListOfProvider.length;
        for(var iIndexProvider = 0; iIndexProvider < iLengthListOfProvider; iIndexProvider++)
        {
            if(this.m_aListOfProvider[iIndexProvider].selected === true )
                return true;

        }
        return false;
    };


    ImportController.prototype.isSearchBtnEnabled = function()
    {
        // Disabled if
        // m_oController.thereIsAtLeastOneProvider() === false || (m_oController.m_bIsVisibleListOfLayers || m_oController.m_bisVisibleLocalStorageInputs)
        if( this.thereIsAtLeastOneProvider() === false ){ return false; }
        if( this.m_bIsVisibleListOfLayers || this.m_bisVisibleLocalStorageInputs){ return false}
        return true
    }


    /**
     * setDefaultData
     */
    ImportController.prototype.setDefaultData = function(){

        // Set Till today
        var oToDate = new Date();
        this.m_oResultsOfSearchService.setSensingPeriodTo(oToDate);

        // From last week
        var oFromDate = new Date();
        var dayOfMonth = oFromDate.getDate();
        oFromDate.setDate(dayOfMonth - 7);

        this.m_oResultsOfSearchService.setSensingPeriodFrom(oFromDate);


        this.m_oModel.sensingPeriodFrom = this.m_oResultsOfSearchService.getSensingPeriodFrom();
        this.m_oModel.sensingPeriodTo = this.m_oResultsOfSearchService.getSensingPeriodTo();
    };


    ImportController.prototype.isEmptyProviderLayerList = function(sProvider)
    {
        var iNumberOfProduct = this.m_aoProductsList.length;
        var bIsEmpty = true;

        for(var iIndexProduct = 0 ; iIndexProduct < iNumberOfProduct; iIndexProduct++)
        {
            if(this.m_aoProductsList[iIndexProduct].provider === sProvider){
                bIsEmpty = false;
                return bIsEmpty;
            }
        }
        return bIsEmpty;
    };


    ImportController.prototype.getPeriod = function(iMonthFrom,iDayFrom,iMonthTo,iDayTo){
        if( (utilsIsObjectNullOrUndefined(iMonthFrom) === true) || (utilsIsObjectNullOrUndefined(iDayFrom) === true) ||
            (utilsIsObjectNullOrUndefined(iMonthTo) === true) || (utilsIsObjectNullOrUndefined(iDayTo) === true) )
            return null;

        var dateSensingPeriodFrom = new Date();
        var dateSensingPeriodTo = new Date();
        dateSensingPeriodFrom.setMonth(iMonthFrom);
        dateSensingPeriodFrom.setDate(iDayFrom);
        dateSensingPeriodTo.setMonth(iMonthTo);
        dateSensingPeriodTo.setDate(iDayTo);
        return{
            dateSensingPeriodFrom:dateSensingPeriodFrom,
            dateSensingPeriodTo:dateSensingPeriodTo
        }
    };

    ImportController.prototype.getPeriodSpring = function()
    {
        return this.getPeriod(2,21,5,20)
    };
    ImportController.prototype.getPeriodSummer = function()
    {
        return this.getPeriod(5,21,8,22);
    };
    ImportController.prototype.getPeriodWinter = function()
    {
        return this.getPeriod(11,21,2,20);
    };
    ImportController.prototype.getPeriodAutumn = function()
    {
        return this.getPeriod(8,23,11,20);
    };

    ImportController.prototype.addSeason = function()
    {
        var sSeason = this.m_oAdvanceFilter.selectedSeason;

        switch(sSeason) {
            case "spring":
                if(utilsIsObjectNullOrUndefined(this.m_oAdvanceFilter.selectedSeasonYears) === false)
                {
                    for(var iIndexYear = 0 ; iIndexYear < this.m_oAdvanceFilter.selectedSeasonYears.length; iIndexYear++ )
                    {
                        var oDataPeriod = this.getPeriodSpring();
                        oDataPeriod.dateSensingPeriodFrom.setYear(this.m_oAdvanceFilter.selectedSeasonYears[iIndexYear]);
                        oDataPeriod.dateSensingPeriodTo.setYear(this.m_oAdvanceFilter.selectedSeasonYears[iIndexYear]);
                        var sName = this.m_oAdvanceFilter.selectedSeasonYears[iIndexYear] + " Spring";
                        this.saveDataInAdvanceFilter(sName, oDataPeriod);
                    }

                }

                break;
            case "summer":

                if(utilsIsObjectNullOrUndefined(this.m_oAdvanceFilter.selectedSeasonYears) === false)
                {
                    for(var iIndexYear = 0 ; iIndexYear < this.m_oAdvanceFilter.selectedSeasonYears.length; iIndexYear++ )
                    {
                        var oDataPeriod = this.getPeriodSummer();
                        oDataPeriod.dateSensingPeriodFrom.setYear(this.m_oAdvanceFilter.selectedSeasonYears[iIndexYear]);
                        oDataPeriod.dateSensingPeriodTo.setYear(this.m_oAdvanceFilter.selectedSeasonYears[iIndexYear]);
                        var sName = this.m_oAdvanceFilter.selectedSeasonYears[iIndexYear] + " Summer";
                        this.saveDataInAdvanceFilter(sName, oDataPeriod);
                    }
                }
                break;
            case "autumn":
                if(utilsIsObjectNullOrUndefined(this.m_oAdvanceFilter.selectedSeasonYears) === false)
                {
                    for(var iIndexYear = 0 ; iIndexYear < this.m_oAdvanceFilter.selectedSeasonYears.length; iIndexYear++ )
                    {
                        var oDataPeriod = this.getPeriodAutumn();
                        oDataPeriod.dateSensingPeriodFrom.setYear(this.m_oAdvanceFilter.selectedSeasonYears[iIndexYear]);
                        oDataPeriod.dateSensingPeriodTo.setYear(this.m_oAdvanceFilter.selectedSeasonYears[iIndexYear]);
                        var sName = this.m_oAdvanceFilter.selectedSeasonYears[iIndexYear] + " Autumn";
                        this.saveDataInAdvanceFilter(sName, oDataPeriod);
                    }
                }
                break;
            case "winter":
                if(utilsIsObjectNullOrUndefined(this.m_oAdvanceFilter.selectedSeasonYears) === false)
                {
                    for(var iIndexYear = 0 ; iIndexYear < this.m_oAdvanceFilter.selectedSeasonYears.length; iIndexYear++ )
                    {
                        var oDataPeriod = this.getPeriodWinter();
                        // P.Campanella 10/02/2018: the winter start in yyyy and ends in yyyy+1. Or viceversa yyyy-1 to yyyyy
                        oDataPeriod.dateSensingPeriodFrom.setYear(this.m_oAdvanceFilter.selectedSeasonYears[iIndexYear]-1);
                        oDataPeriod.dateSensingPeriodTo.setYear(this.m_oAdvanceFilter.selectedSeasonYears[iIndexYear]);
                        var sName = this.m_oAdvanceFilter.selectedSeasonYears[iIndexYear] + " Winter";
                        this.saveDataInAdvanceFilter(sName, oDataPeriod);
                    }
                }
                break;
        }
    };

    ImportController.prototype.saveDataInAdvanceFilter = function(sName,oData)
    {
        if(utilsIsObjectNullOrUndefined(oData) === true || utilsIsStrNullOrEmpty(sName) === true)
            return false;

        var oSaveData = {
            name:sName,
            data:oData
        };

        var iNumberOfSaveData = this.m_oAdvanceFilter.savedData.length;
        for(var iIndexSaveData = 0; iIndexSaveData < iNumberOfSaveData; iIndexSaveData++)
        {
            if(this.m_oAdvanceFilter.savedData[iIndexSaveData].name === oSaveData.name)
            {
                return false;
            }
        }
        this.m_oAdvanceFilter.savedData.push(oSaveData);

        return true;
    };

    ImportController.prototype.removeSaveDataChips = function(oData)
    {
        if(utilsIsObjectNullOrUndefined(oData) === true)
            return false;
        if(utilsIsObjectNullOrUndefined(this.m_oAdvanceFilter.savedData) === true)
            return false;
        var iNumberOfSaveData = this.m_oAdvanceFilter.savedData.length;

        for(var iIndexNumberOfSaveData = 0; iIndexNumberOfSaveData < iNumberOfSaveData; iIndexNumberOfSaveData++)
        {
            if(this.m_oAdvanceFilter.savedData[iIndexNumberOfSaveData] === oData)
            {
                this.m_oAdvanceFilter.savedData.splice(iIndexNumberOfSaveData, 1);

                this.updateAdvancedSavedFiltersUi();

                break;
            }
        }

        return true;
    };

    ImportController.prototype.initDefaultYears = function()
    {
        var oActualDate = new Date();
        var iYears = oActualDate.getFullYear();
        for(var iIndex = 0 ; iIndex < 20; iIndex++)
        {
            this.m_oAdvanceFilter.listOfYears.push(iYears.toString());
            iYears--;
        }

    };

    ImportController.prototype.initDefaultMonths = function()
    {
        /*
            January - 31 days
            February - 28 days in a common year and 29 days in leap years
            March - 31 days
            April - 30 days
            May - 31 days
            June - 30 days
            July - 31 days
            August - 31 days
            September - 30 days
            October - 31 days
            November - 30 days
            December - 31 days
        * */
        var asMonths = ["January","February","March","April","May","June","July","August","September","October","November","December"];
        for(var iIndex = 0 ; iIndex < asMonths.length; iIndex++)
        {
            this.m_oAdvanceFilter.listOfMonths.push(asMonths[iIndex]);
        }

    };

    ImportController.prototype.getMonthDays = function(sMonth, sYear)
    {
        var sMonthLowerCase = sMonth.toLocaleLowerCase();
        switch(sMonthLowerCase) {
            case "january":
                return 31;
            case "february":

                if(utilsLeapYear(sYear))
                {
                    return 29;
                }
                else
                {
                    return 28;
                }
            case "march":
                return 31;
            case "april":
                return 30;
            case "may":
                return 31;
            case "june":
                return 30;
            case "july":
                return 31;
            case "august":
                return 31;
            case "september":
                return 30;
            case "october":
                return 31;
            case "november":
                return 30;
            case "december":
                return 31;

        }
    };

    ImportController.prototype.getMonthDaysFromRangeOfMonths = function(sMonth, asYears)
    {
        if(utilsIsObjectNullOrUndefined(asYears) === true)
            return [];
        if(asYears.length < 1)
            return [];
        var iNumberOfYears = asYears.length;
        var sMonthLowerCase = sMonth.toLocaleLowerCase();
        var iReturnValue = 0;
        if( (sMonthLowerCase === "february") )
        {
            for(var iIndexYear = 0 ; iIndexYear < iNumberOfYears; iIndexYear++)
            {
                var iTemp = this.getMonthDays(sMonth,asYears[iIndexYear])
                //if true it takes the new value, in the case of leap years it takes 29 days
                if(iTemp > iReturnValue)
                    iReturnValue = iTemp;
            }
        }
        else
        {
            iReturnValue = this.getMonthDays(sMonth,asYears[0]);
        }
        return utilsGenerateArrayWithFirstNIntValue(1,iReturnValue);
        //check leap year
    };
    ImportController.prototype.getMonthDaysFrom = function(){
        return this.getMonthDaysFromRangeOfMonths(this.m_oAdvanceFilter.selectedMonthFrom, this.m_oAdvanceFilter.selectedYears);
    };
    ImportController.prototype.getMonthDaysTo = function(){
        return this.getMonthDaysFromRangeOfMonths(this.m_oAdvanceFilter.selectedMonthTo, this.m_oAdvanceFilter.selectedYears);
    };

    ImportController.prototype.areSelectedMonthAndYearFrom = function(){
        return (this.m_oAdvanceFilter.selectedYears.length !== 0 && this.m_oAdvanceFilter.selectedMonthFrom !== "");
    };
    ImportController.prototype.areSelectedMonthAndYearTo = function(){
       return (this.m_oAdvanceFilter.selectedYears.length !== 0 && this.m_oAdvanceFilter.selectedMonthTo !== "");
    };

    ImportController.prototype.addFilterDataFromTo = function(){
        if(utilsIsObjectNullOrUndefined(this.m_oAdvanceFilter.selectedYears) === true ) return false;
        var iNumberOfSelectedYears = this.m_oAdvanceFilter.selectedYears.length;
        for(var iIndexYear = 0; iIndexYear < iNumberOfSelectedYears; iIndexYear++ )
        {
            var sName="";
            sName +=   this.m_oAdvanceFilter.selectedDayFrom.toString() + "/" + this.m_oAdvanceFilter.selectedMonthFrom.toString();
            sName +=  " - " +this.m_oAdvanceFilter.selectedDayTo.toString() + "/" + this.m_oAdvanceFilter.selectedMonthTo.toString();
            sName += " " + this.m_oAdvanceFilter.selectedYears[iIndexYear].toString();

            var dateSensingPeriodFrom = new Date();
            var dateSensingPeriodTo = new Date();
            dateSensingPeriodFrom.setYear(this.m_oAdvanceFilter.selectedYears[iIndexYear]);
            dateSensingPeriodFrom.setMonth(this.convertNameMonthInNumber(this.m_oAdvanceFilter.selectedMonthFrom));
            // TODO CHECK LEAP YEAS (29 DAYS FEBRUARY)
            dateSensingPeriodFrom.setDate( this.m_oAdvanceFilter.selectedDayFrom);
            dateSensingPeriodTo.setYear(this.m_oAdvanceFilter.selectedYears[iIndexYear]);
            dateSensingPeriodTo.setMonth(this.convertNameMonthInNumber(this.m_oAdvanceFilter.selectedMonthTo));
            dateSensingPeriodTo.setDate(this.m_oAdvanceFilter.selectedDayTo);
            var oData={
                dateSensingPeriodFrom:dateSensingPeriodFrom,
                dateSensingPeriodTo:dateSensingPeriodTo
            };
            this.saveDataInAdvanceFilter(sName,oData);
        }

    };

    ImportController.prototype.convertNameMonthInNumber = function(sName){
        if(utilsIsStrNullOrEmpty(sName) === true)
            return -1;

        var sMonthLowerCase = sName.toLocaleLowerCase();
        switch(sMonthLowerCase) {
            case "january":
                return 0;
            case "february":
                return 1;
            case "march":
                return 2;
            case "april":
                return 3;
            case "may":
                return 4;
            case "june":
                return 5;
            case "july":
                return 6;
            case "august":
                return 7;
            case "september":
                return 8;
            case "october":
                return 9;
            case "november":
                return 10;
            case "december":
                return 11;

        }
        return -1;
    };

    /**
     *
     * @returns {boolean}
     */
    ImportController.prototype.addFilterMonths = function()
    {
        if( (utilsIsObjectNullOrUndefined(this.m_oAdvanceFilter.selectedYearsSearchForMonths) === true) || (utilsIsObjectNullOrUndefined(this.m_oAdvanceFilter.selectedMonthsSearchForMonths) === true) )
        {
            return false;
        }
        var iNumberOfSelectedYears = this.m_oAdvanceFilter.selectedYearsSearchForMonths.length;
        var iNumberOfSelectedMonths = this.m_oAdvanceFilter.selectedMonthsSearchForMonths.length;

        for(var iIndexYear = 0; iIndexYear < iNumberOfSelectedYears; iIndexYear++)
        {
            for(var iIndexMonth = 0; iIndexMonth < iNumberOfSelectedMonths; iIndexMonth++)
            {
                var sName = this.m_oAdvanceFilter.selectedYearsSearchForMonths[iIndexYear].toString() +" "+  this.m_oAdvanceFilter.selectedMonthsSearchForMonths[iIndexMonth];
                var dateSensingPeriodFrom = new Date();
                var dateSensingPeriodTo = new Date();
                dateSensingPeriodFrom.setYear(this.m_oAdvanceFilter.selectedYearsSearchForMonths[iIndexYear]);
                dateSensingPeriodFrom.setMonth(this.convertNameMonthInNumber(this.m_oAdvanceFilter.selectedMonthsSearchForMonths[iIndexMonth]));
                dateSensingPeriodFrom.setDate(1);
                dateSensingPeriodTo.setYear(this.m_oAdvanceFilter.selectedYearsSearchForMonths[iIndexYear]);
                dateSensingPeriodTo.setMonth(this.convertNameMonthInNumber(this.m_oAdvanceFilter.selectedMonthsSearchForMonths[iIndexMonth]));
                dateSensingPeriodTo.setDate(this.getMonthDays(this.m_oAdvanceFilter.selectedMonthsSearchForMonths[iIndexMonth],this.m_oAdvanceFilter.selectedYearsSearchForMonths[iIndexYear]));
                var oData={
                    dateSensingPeriodFrom:dateSensingPeriodFrom,
                    dateSensingPeriodTo:dateSensingPeriodTo
                };
                this.saveDataInAdvanceFilter(sName,oData);
            }
        }
        return true;
    };

    ImportController.prototype.getNameOfSelectedResultsTab = function(){

        if( utilsIsObjectNullOrUndefined(this.m_aListOfProvider) === true )
        {
            return "";
        }

        var iNumberOfProviders = this.m_aListOfProvider.length;
        var iIndexOfSelectedTabs = 0;
        for(var iIndexProvider = 0; iIndexProvider < iNumberOfProviders; iIndexProvider++)
        {
            if( this.m_aListOfProvider[iIndexProvider].selected === true )
            {
                if( iIndexOfSelectedTabs === this.m_iActiveProvidersTab)
                {
                    return this.m_aListOfProvider[iIndexProvider].name
                }
                iIndexOfSelectedTabs++;
            }

        }
        return "";

    }
    /**
     * selectAllProducts
     * @returns {boolean}
     */
    ImportController.prototype.selectAllProducts = function(){

        if(utilsIsObjectNullOrUndefined(this.m_aoProductsList))
        {
            return false;
        }
        var iNumberOfProducts = this.m_aoProductsList.length ;
        var sProviderTabResults = this.getNameOfSelectedResultsTab()
        for(var iIndexProduct = 0 ; iIndexProduct < iNumberOfProducts; iIndexProduct++)
        {
            if( this.m_aoProductsList[iIndexProduct].provider === sProviderTabResults )
            {
                this.m_aoProductsList[iIndexProduct].checked = true;
            }
        }
        return true;
    }

    /**
     * deselectAllProducts
     * @returns {boolean}
     */
    ImportController.prototype.deselectAllProducts = function(){
        if(utilsIsObjectNullOrUndefined(this.m_aoProductsList))
        {
            return false;
        }
        var iNumberOfProducts = this.m_aoProductsList.length ;
        var sProviderTabResults = this.getNameOfSelectedResultsTab()
        for(var iIndexProduct = 0 ; iIndexProduct < iNumberOfProducts; iIndexProduct++)
        {
            if( this.m_aoProductsList[iIndexProduct].provider === sProviderTabResults )
            {
                this.m_aoProductsList[iIndexProduct].checked = false;
            }
        }
        return true;
    }
    ImportController.prototype.getListOfSelectedProducts = function()
    {
        if(utilsIsObjectNullOrUndefined(this.m_aoProductsList))
        {
            return [];
        }
        var iNumberOfProducts = this.m_aoProductsList.length ;
        var aoLayerSelected = [];
        for(var iIndexProduct = 0 ; iIndexProduct < iNumberOfProducts; iIndexProduct++)
        {
            if(this.m_aoProductsList[iIndexProduct].checked === true)
            {
                aoLayerSelected.push(this.m_aoProductsList[iIndexProduct])
            }

        }
        return aoLayerSelected;
    };

    ImportController.prototype.downloadAllSelectedProducts = function()
    {
        var aoListOfSelectedProducts = this.getListOfSelectedProducts();
        this.downloadProductsListInWorkspace(aoListOfSelectedProducts);
        return true;
    };
    /**
     * cleanAdvanceFilters
     */
    ImportController.prototype.cleanAdvanceFilters = function()
    {

        this.m_oAdvanceFilter.selectedSeasonYears = [];
        this.m_oAdvanceFilter.selectedYears = [];
        this.m_oAdvanceFilter.selectedDayFrom = "";
        this.m_oAdvanceFilter.selectedDayTo = "";
        this.m_oAdvanceFilter.selectedMonthFrom = "";
        this.m_oAdvanceFilter.selectedMonthTo = "";
        this.m_oAdvanceFilter.selectedYearsSearchForMonths = [];
        this.m_oAdvanceFilter.selectedMonthsSearchForMonths = [];
    };

    ImportController.prototype.removeAllAdvanceSavedFilters = function()
    {
        this.m_oAdvanceFilter.savedData = [];
        this.updateAdvancedSavedFiltersUi();
    };

    ImportController.prototype.openAdvanceFiltersDialog = function(){
        var oController = this;
        this.m_oModalService.showModal({
            templateUrl: "dialogs/import_advance_filters/ImportAdvanceFiltersView.html",
            controller: "ImportAdvanceFiltersController",
            inputs: {
                extras: ""
            }

        }).then(function(modal){
            modal.element.modal({
                backdrop: 'static',
                keyboard: false
            });
            modal.close.then(function(result) {
                if(utilsIsObjectNullOrUndefined(result) === true || result.length === 0)
                {
                    //oController.m_sTypeOfFilterSelected = 'Time period';
                    oController.setFilterTypeAsTimePeriod();
                    return false;
                }

                oController.m_oAdvanceFilter.savedData = result;
                return true;
            })
        });

        return true;
    };

    ImportController.prototype.getSelectedValueInMission = function(aoMission,sIndexNameDropdownMenu){
        if(utilsIsObjectNullOrUndefined(aoMission) === true || utilsIsStrNullOrEmpty(sIndexNameDropdownMenu) === true)
            return "";
        var aoFilters = aoMission.filters;
        var iNumberOfFilters = aoFilters.length;
        for(var iIndexFilter = 0 ; iIndexFilter < iNumberOfFilters; iIndexFilter++)
        {
            if(aoFilters[iIndexFilter].indexname === sIndexNameDropdownMenu)
            {
                if(utilsIsObjectNullOrUndefined(aoFilters[iIndexFilter].indexvalue) === false)
                {
                    return aoFilters[iIndexFilter].indexvalue;
                }

            }
        }
        return "";
    };

    ImportController.prototype.isVisibleFilter = function(sCollection,sFilterName,sMissionName)
    {
        var bIsVisible = true;//all filters are visible as default

        if(utilsIsStrNullOrEmpty(sFilterName) === false )
        {
            // Proba-V filters
            if(sMissionName === "Mission: Proba-V" )
            {
                bIsVisible = false;
                bIsVisible = bIsVisible || this.probaVVisibleFilterProductID(sCollection,sFilterName);
                bIsVisible = bIsVisible || this.probaVVisibleFilterIsCollection(sFilterName);
                bIsVisible = bIsVisible || this.probaVVisibleFilterCloudCoverage(sCollection,sFilterName);
                bIsVisible = bIsVisible || this.probaVVisibleFilterSnowCoverage(sCollection,sFilterName);
                bIsVisible = bIsVisible || this.probaVVisibleFilterProductref(sCollection,sFilterName);
                bIsVisible = bIsVisible || this.probaVVisibleFilterCameraId(sCollection,sFilterName);
            }

        }
        return bIsVisible;
    }
    ImportController.prototype.probaVVisibleFilterIsCollection = function(sFilterName)
    {
        if( sFilterName === "collection" )
            return true;

        return false;
    }
    ImportController.prototype.probaVVisibleFilterProductID = function(sCollection,sFilterName)
    {

        if( sFilterName !== "ProductID" )
            return false;

        switch(sCollection){
            case "urn:ogc:def:EOP:VITO:PROBAV_S1-TOA_1KM_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S1-TOC_1KM_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S10-TOC_1KM_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S1-TOA_333M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S1-TOC_333M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S10-TOC_333M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S10-TOC-NDVI_333M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_L2A_333M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S1-TOA_100M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S1-TOC_100M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S1-TOC-NDVI_100M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S5-TOA_100M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S5-TOC_100M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S5-TOC-NDVI_100M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_L2A_100M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S10-TOC-NDVI_1KM_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_P_V001":
                return true;

            case "urn:ogc:def:EOP:VITO:PROBAV_L2A_1KM_V001":
                return false;

            default: return false;

        }

    };
    ImportController.prototype.probaVVisibleFilterCloudCoverage = function(sCollection,sFilterName)
    {

        if( sFilterName !== "cloudcoverpercentage" )
            return false;

        switch(sCollection){
            case "urn:ogc:def:EOP:VITO:PROBAV_S1-TOA_1KM_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S1-TOC_1KM_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S10-TOC_1KM_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S1-TOA_333M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S1-TOC_333M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S10-TOC_333M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S10-TOC-NDVI_333M_V001":

            case "urn:ogc:def:EOP:VITO:PROBAV_S1-TOA_100M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S1-TOC_100M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S1-TOC-NDVI_100M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S5-TOA_100M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S5-TOC_100M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S5-TOC-NDVI_100M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S10-TOC-NDVI_1KM_V001":

                return true;

            case "urn:ogc:def:EOP:VITO:PROBAV_L2A_1KM_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_P_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_L2A_333M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_L2A_100M_V001":
                return false;

            default: return false;

        }

    };
    ImportController.prototype.probaVVisibleFilterSnowCoverage = function(sCollection,sFilterName)
    {

        if( sFilterName !== "snowcoverpercentage" )
            return false;

        switch(sCollection){
            case "urn:ogc:def:EOP:VITO:PROBAV_S1-TOA_1KM_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S1-TOC_1KM_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S10-TOC_1KM_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S1-TOA_333M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S1-TOC_333M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S10-TOC_333M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S10-TOC-NDVI_333M_V001":

            case "urn:ogc:def:EOP:VITO:PROBAV_S1-TOA_100M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S1-TOC_100M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S1-TOC-NDVI_100M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S5-TOA_100M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S5-TOC_100M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S5-TOC-NDVI_100M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S10-TOC-NDVI_1KM_V001":

                return true;


            case "urn:ogc:def:EOP:VITO:PROBAV_L2A_1KM_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_P_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_L2A_333M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_L2A_100M_V001":
                return false;


            default: return false;


        }

    };

    ImportController.prototype.probaVVisibleFilterProductref= function(sCollection,sFilterName)
    {

        if( sFilterName !== "productref" )
            return false;

        switch(sCollection){
            case "urn:ogc:def:EOP:VITO:PROBAV_L2A_1KM_V001":
                return true;


            case "urn:ogc:def:EOP:VITO:PROBAV_L2A_333M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_L2A_100M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S1-TOA_1KM_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S1-TOC_1KM_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S10-TOC_1KM_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S1-TOA_333M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S1-TOC_333M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S10-TOC_333M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S10-TOC-NDVI_333M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S1-TOA_100M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S1-TOC_100M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S1-TOC-NDVI_100M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S5-TOA_100M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S5-TOC_100M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S5-TOC-NDVI_100M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S10-TOC-NDVI_1KM_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_P_V001":
                return false;


            default: return false;


        }

    };
    ImportController.prototype.probaVVisibleFilterCameraId= function(sCollection,sFilterName)
    {

        if( sFilterName !== "cameraId" )
            return false;

        switch(sCollection){
            case "urn:ogc:def:EOP:VITO:PROBAV_L2A_1KM_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_L2A_333M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_L2A_100M_V001":
                return true;



            case "urn:ogc:def:EOP:VITO:PROBAV_S1-TOA_1KM_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S1-TOC_1KM_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S10-TOC_1KM_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S1-TOA_333M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S1-TOC_333M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S10-TOC_333M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S10-TOC-NDVI_333M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S1-TOA_100M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S1-TOC_100M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S1-TOC-NDVI_100M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S5-TOA_100M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S5-TOC_100M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S5-TOC-NDVI_100M_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_S10-TOC-NDVI_1KM_V001":
            case "urn:ogc:def:EOP:VITO:PROBAV_P_V001":
                return false;


            default: return false;

        }

    }
    ImportController.prototype.clearModelGeoselection = function(oController)
    {
        oController.m_oModel.geoselection = "";
    }

    ImportController.$inject = [
        '$scope',
        'ConstantsService',
        'AuthService',
        '$state',
        'MapService',
        'SearchService',
        'AdvancedFilterService',
        'AdvancedSearchService',
        'ConfigurationService',
        'FileBufferService',
        'RabbitStompService',
        'ProductService',
        'ProcessWorkspaceService',
        'WorkspaceService',
        'ResultsOfSearchService',
        'ModalService',
        'OpenSearchService',
        'PagesService',
        '$translate'
    ];

    return ImportController;
}) ();
window.ImportController = ImportController;
