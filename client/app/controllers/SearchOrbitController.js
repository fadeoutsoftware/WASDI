/**
 * Created by s.adamo on 23/01/2017.
 */
/**
 * Created by p.campanella on 21/10/2016.
 */

'use strict';

var SearchOrbitController = (function() {
    function SearchOrbitController($scope, $location, oConstantsService, oAuthService,oState, oConfigurationService,
                                   oMapService, oSearchOrbitService,oProcessesLaunchedService,oWorkspaceService,
                                   oRabbitStompService,oModalService, oProductService,$filter ,oTreeService) {
        this.m_oScope = $scope;
        this.m_oLocation = $location;
        this.m_oConstantsService = oConstantsService;
        this.m_oAuthService = oAuthService;
        this.m_oState = oState;
        this.m_oScope.m_oController = this;
        this.m_oConfigurationService = oConfigurationService;
        this.m_oMapService = oMapService;
        this.m_oSearchOrbitService = oSearchOrbitService;
        this.m_oConfiguration = null;
        this.m_oGeoJSON = null;
        this.m_oSelectedSensorType = [];
        this.m_oSelectedResolutionType = [];
        this.m_oSelectedSatellite = [];
        this.m_aoOrbits = null;
        this.m_oProcessesLaunchedService=oProcessesLaunchedService;
        this.m_oWorkspaceService = oWorkspaceService;
        this.m_oRabbitStompService = oRabbitStompService;
        this.m_oModalService = oModalService;
        this.m_oProductService = oProductService;
        this.m_bMasterCheck = false;
        this.m_bisLoadingAllOrbitInMap = false;
        this.m_oActiveWorkspace = this.m_oConstantsService.getActiveWorkspace();
        this.m_oUser = this.m_oConstantsService.getUser();
        this.m_bIsVisibleLoadingIcon = false;
        this.m_oFilter = $filter;
        this.m_sFilterTable = "";
        this.m_oTreeService = oTreeService;
        this.m_sIdDiv = "#orbitsTree";
        this.m_sIdDivSatelliteResourceTree = "#satelliteResourceTree";
        this.m_oOpportunitiesTree = null;
        // this.m_aoSatelliteResources = [];
        this.m_aoSatelliteResourcesTree = null;
        this.m_aoSatelliteResources = [];
        this.m_isDisabledSearchButton=false;
        //order the table
        this.m_sOrderBy = "Satellite";
        this.m_bReverseOrder = false;
        this.m_sHrefLogFile = "";
        this.downloadKML();//TODO REMOVE IT
        this.getSatellitesResources();

        if(utilsIsObjectNullOrUndefined( this.m_oActiveWorkspace) && utilsIsStrNullOrEmpty( this.m_oActiveWorkspace))
        {
            //if this.m_oState.params.workSpace in empty null or undefined create new workspace
            if(!(utilsIsObjectNullOrUndefined(this.m_oState.params.workSpace) && utilsIsStrNullOrEmpty(this.m_oState.params.workSpace)))
            {
                this.openWorkspace(this.m_oState.params.workSpace);
                //this.m_oActiveWorkspace = this.m_oConstantsService.getActiveWorkspace();
            }
            else
            {
                //TODO CREATE NEW WORKSPACE OR GO HOME
            }
        }
        else
        {
            this.m_oProcessesLaunchedService.loadProcessesFromServer(this.m_oActiveWorkspace.workspaceId);
        }

        /*Hook to Rabbit WebStomp Service*/
        this.m_oRabbitStompService.setMessageCallback(this.receivedRabbitMessage);
        this.m_oRabbitStompService.setActiveController(this);

        this.initOrbitSearch = function(){
            //init orbit search
            this.m_oOrbitSearch = new Object();
            this.m_oOrbitSearch.acquisitionStartTime = moment();
            this.m_oOrbitSearch.acquisitionEndTime = moment().add(7, 'd');
            this.m_oOrbitSearch.lookingType = "LEFT";
            this.m_oOrbitSearch.viewAngle = {
                nearAngle:"",
                farAngle:""
            };
            this.m_oOrbitSearch.swathSize = {
                length:"",
                width:""
            };
        };

        var oController = this;

        this.m_oConfigurationService.getConfiguration().then(function (configuration) {
            if(!utilsIsObjectNullOrUndefined(configuration))
            {
                oController.m_oConfiguration = configuration;
                if(!utilsIsObjectNullOrUndefined(oController.m_oConfiguration.orbitsearch) && !utilsIsObjectNullOrUndefined(oController.m_oConfiguration.orbitsearch.satelliteNames))
                {
                    /*
                    //check as selected all satellites
                    for(var iIndexSatellite = 0; iIndexSatellite < oController.m_oConfiguration.orbitsearch.satelliteNames.length ; iIndexSatellite++ )
                    {
                        var sOrbit = oController.m_oConfiguration.orbitsearch.satelliteNames[iIndexSatellite];
                        oController.m_oSelectedSatellite.push(sOrbit);
                    }
                    */
                }
                if(!utilsIsObjectNullOrUndefined(oController.m_oConfiguration.orbitsearch) && !utilsIsObjectNullOrUndefined(oController.m_oConfiguration.orbitsearch.sensortypes))
                {
                    //check as selected all sensor type
                    for(var iIndexSatellite = 0; iIndexSatellite < oController.m_oConfiguration.orbitsearch.sensortypes.length ; iIndexSatellite++ )
                    {
                        var sSensor = oController.m_oConfiguration.orbitsearch.sensortypes[iIndexSatellite];
                        oController.m_oSelectedSensorType.push(sSensor);
                    }

                }
                if(!utilsIsObjectNullOrUndefined(oController.m_oConfiguration.orbitsearch) && !utilsIsObjectNullOrUndefined(oController.m_oConfiguration.orbitsearch.sensorresolutions))
                {
                    //check as selected all sensor resolution
                    for(var iIndexSatellite = 0; iIndexSatellite < oController.m_oConfiguration.orbitsearch.sensorresolutions.length ; iIndexSatellite++ )
                    {
                        var sSensor = oController.m_oConfiguration.orbitsearch.sensorresolutions[iIndexSatellite];
                        oController.m_oSelectedResolutionType.push(sSensor);
                    }

                }
                    oController.m_oScope.$apply();
            }
            else
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR: IMPOSSIBLE GET CONFIGURATION.");
            }
        },function error(data, status, header, config) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR: IMPOSSIBLE GET CONFIGURATION.");
        });

        this.m_oMapService.initMapWithDrawSearch('orbitMap');


        $scope.$on('rectangle-created-for-opensearch', function(event, args) {

            var oLayer = args.layer;
            //get GeoJSON
            oController.m_oGeoJSON = oLayer.toGeoJSON();

        });

        this.initOrbitSearch();

    }

    // SearchOrbitController.prototype.getDateFromTimestamp = function(timestamp) {
    //   return moment.unix(timestamp / 1000).format("YYYY-MM-DD HH:mm:ss");
    // };

    // SearchOrbitController.prototype.clearMapAndRemoveOrbits = function(){
    //     if(utilsIsObjectNullOrUndefined(this.m_aoOrbits) === true )
    //         return false;
    //     var iOrbitsLength = this.m_aoOrbits.length;
    //     var oRectangle = null;
    //     for(var iOrbitIndex = 0; iOrbitIndex < iOrbitsLength; iOrbitIndex++){
    //         oRectangle = this.m_aoOrbits[iOrbitIndex ].FootPrintRectangle
    //         this.m_oMapService.removeLayerFromMap(oRectangle);
    //     }
    //     this.m_aoOrbits = [];
    //     return true;
    // };

    // SearchOrbitController.prototype.searchOrbit = function()
    // {
    //
    //     var sError = "";
    //
    //     if(utilsIsObjectNullOrUndefined(this.m_oGeoJSON)) sError += "PLEASE SELECT AREA OF INTEREST<br>TOP RIGHT CORNER OF THE MAP<br>";
    //
    //     //if there isn't a resolution throw an error
    //     if(utilsIsObjectNullOrUndefined(this.m_oSelectedResolutionType) || this.m_oSelectedResolutionType.length == 0) sError += "PLEASE SELECT AT LEAST A RESOLUTION<br>";
    //     //if there isn't a sensor type throw an error
    //     if(utilsIsObjectNullOrUndefined(this.m_oSelectedSensorType) || this.m_oSelectedSensorType.length == 0) sError += "PLEASE SELECT AT LEAST A SENSOR TYPE<br>";
    //     //if there isn't a satellite throw an error
    //     if(utilsIsObjectNullOrUndefined(this.m_oSelectedSatellite) || this.m_oSelectedSatellite.length == 0) sError += "PLEASE SELECT AT LEAST A MISSION<br>";
    //
    //     if (!utilsIsStrNullOrEmpty(sError)) {
    //         utilsVexDialogAlertDefault(sError,null);
    //         return;
    //     }
    //
    //     var oController = this;
    //
    //     //clear map and remove orbits and set check as false
    //     this.m_bMasterCheck = false;
    //     this.clearMapAndRemoveOrbits();
    //
    //     var oOrbitSearch = new Object();
    //     oOrbitSearch.orbitFilters = new Array();
    //     this.m_oOrbitSearch.orbitFilters = new Array();
    //     this.m_oOrbitSearch.satelliteNames = new Array();
    //    // var oOrbitFilter = new Object();
    //
    //     if(utilsIsObjectNullOrUndefined(this.m_oSelectedSensorType))
    //         var iLengthSelectedSensorType = 0;
    //     else
    //         var iLengthSelectedSensorType = this.m_oSelectedSensorType.length;
    //
    //     if(utilsIsObjectNullOrUndefined(this.m_oSelectedResolutionType))
    //         var iLengthSelectedResolutionType  = 0;
    //     else
    //         var iLengthSelectedResolutionType  = this.m_oSelectedResolutionType.length;
    //
    //         //add sensor type and resolution
    //     for (var iSensorType = 0; iSensorType < iLengthSelectedSensorType; iSensorType++) {
    //
    //         for (var iResolutionType = 0; iResolutionType < iLengthSelectedResolutionType; iResolutionType++) {
    //             var oOrbitFilter = new Object();
    //             oOrbitFilter.sensorType = this.m_oSelectedSensorType[iSensorType];
    //             oOrbitFilter.sensorResolution = this.m_oSelectedResolutionType[iResolutionType];
    //             oController.m_oOrbitSearch.orbitFilters.push(oOrbitFilter);
    //
    //         }
    //
    //     }
    //
    //     if(utilsIsObjectNullOrUndefined(this.m_oSelectedSatellite))
    //         var iLengthSelectedSatellite = 0;
    //     else
    //         var iLengthSelectedSatellite =this.m_oSelectedSatellite.length;
    //
    //     //satellite names
    //     for (var iSatellite = 0; iSatellite < iLengthSelectedSatellite; iSatellite++) {
    //         oController.m_oOrbitSearch.satelliteNames.push(this.m_oSelectedSatellite[iSatellite]);
    //     }
    //
    //     //
    //
    //     //add polygon area
    //     var sCoordinatesPolygon = "";
    //     if(utilsIsObjectNullOrUndefined(oController.m_oGeoJSON.geometry.coordinates))
    //         var iLengthCoordinates = 0;
    //     else
    //         var iLengthCoordinates = oController.m_oGeoJSON.geometry.coordinates.length;
    //
    //     for (var iLayerCount = 0; iLayerCount < iLengthCoordinates; iLayerCount++) {
    //
    //         var oLayer = oController.m_oGeoJSON.geometry.coordinates[iLayerCount];
    //         for (var iCoordCount = 0; iCoordCount < oLayer.length; iCoordCount++) {
    //             if (oLayer[iCoordCount].length == 2) {
    //                 var x = oLayer[iCoordCount][0];
    //                 var y = oLayer[iCoordCount][1];
    //                 sCoordinatesPolygon += (x + " " + y);
    //
    //                 if (iCoordCount + 1 < oLayer.length)
    //                     sCoordinatesPolygon += ',';
    //             }
    //         }
    //
    //
    //     }
    //
    //     oController.m_oOrbitSearch.polygon = sCoordinatesPolygon;
    //     this.m_bIsVisibleLoadingIcon = true;
    //     this.m_aoOrbits = null;
    //     oController.m_oOrbitSearch.acquisitionStartTime += ":00:00";
    //     oController.m_oOrbitSearch.acquisitionEndTime += ":00:00";
    //     oController.m_oOrbitSearch.viewAngle = oController.convertViewAngleToString();
    //     oController.m_oOrbitSearch.swathSize = oController.convertSwathSizeToString();
    //     //call search
    //     this.m_oSearchOrbitService.searchOrbit(oController.m_oOrbitSearch)
    //         .success(function (data, status, headers, config) {
    //             if(!utilsIsObjectNullOrUndefined(data))
    //             {
    //                 //TODO NEW FEATURE
    //                 //var oDataTree = oController.generateDataTree(data);
    //                 // oController.m_oTreeService.createNewInstanceTree("#orbitsTree",oController.generateDataTree(data));
    //                 if(utilsIsObjectNullOrUndefined(oController.m_oOpportunitiesTree) === true)
    //                 {
    //                     oController.m_oOpportunitiesTree = oController.getResultOpportunitiesTreeJson(data);
    //                     oController.m_oTreeService.createNewInstanceTree(oController.m_sIdDiv,oController.m_oOpportunitiesTree);
    //                 }
    //                 else
    //                 {
    //                     oController.m_oOpportunitiesTree = oController.getResultOpportunitiesTreeJson(data);
    //                     oController.m_oTreeService.loadNewTree(oController.m_sIdDiv,oController.m_oOpportunitiesTree);
    //                 }
    //
    //                 oController.m_aoOrbits = data;
    //                 oController.setOrbitAsUnchecked();
    //                 if(data.length === 0)
    //                 {
    //                     utilsVexDialogAlertTop("GURU MEDITATION<br>NO RESULTS FOR YOUR FILTERS");
    //                 }
    //             }
    //             else
    //             {
    //                 utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR: SEARCH ORBITS FAILS.");
    //             }
    //             oController.initOrbitSearch();
    //             oController.m_bIsVisibleLoadingIcon = false;
    //
    //         })
    //         .error(function (data, status, header, config) {
    //             utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR: SEARCH ORBITS FAILS.");
    //             oController.initOrbitSearch();
    //             oController.m_aoOrbits = null;
    //             oController.m_bIsVisibleLoadingIcon = false;
    //         });
    //
    // };


    // SearchOrbitController.prototype.convertViewAngleToString = function()
    // {
    //     if( (utilsIsObjectNullOrUndefined(this.m_oOrbitSearch) === true)
    //         || (utilsIsObjectNullOrUndefined(this.m_oOrbitSearch.viewAngle) === true)
    //         || (utilsIsObjectNullOrUndefined(this.m_oOrbitSearch.viewAngle.nearAngle) === true)
    //         || (utilsIsObjectNullOrUndefined(this.m_oOrbitSearch.viewAngle.farAngle) === true) )
    //     {
    //         return "";
    //     }
    //     return "(nearAngle:" + this.m_oOrbitSearch.viewAngle.nearAngle + ",farAngle:" + this.m_oOrbitSearch.viewAngle.farAngle +")";
    // };

    // SearchOrbitController.prototype.convertSwathSizeToString = function()
    // {
    //     if( (utilsIsObjectNullOrUndefined(this.m_oOrbitSearch) === true)
    //         || (utilsIsObjectNullOrUndefined(this.m_oOrbitSearch.swathSize) === true)
    //         || (utilsIsObjectNullOrUndefined(this.m_oOrbitSearch.swathSize.length) === true)
    //         || (utilsIsObjectNullOrUndefined(this.m_oOrbitSearch.swathSize.width) === true) )
    //     {
    //         return "";
    //     }
    //     return "(length:" + this.m_oOrbitSearch.swathSize.length + ",width:" + this.m_oOrbitSearch.swathSize.width +")";
    // };

    /**
     *
     * @returns {boolean}
     */
    // SearchOrbitController.prototype.setOrbitAsUnchecked = function(){
    //     if( utilsIsObjectNullOrUndefined(this.m_aoOrbits) === true )
    //     {
    //         return false;
    //     }
    //     var iNumberOfOrbits = this.m_aoOrbits.length;
    //     for(var iOrbitIndex = 0; iOrbitIndex < iNumberOfOrbits; iOrbitIndex++)
    //     {
    //         this.m_aoOrbits[iOrbitIndex].isSelected = false;
    //     }
    //     return true;
    // };
    /**
     *
     * @returns {null|Array|*}
     */
    SearchOrbitController.prototype.getOrbits = function(){
        return this.m_aoOrbits;
    };

    SearchOrbitController.prototype.thereAreOrbits = function(){
        return ( ( utilsIsObjectNullOrUndefined(this.m_aoOrbits) === false ) && (this.m_aoOrbits.length > 0 ) )
    };


    /**
     *
     * @param oOrbit
     * @returns {boolean}
     */
    //TODO REMOVE IT ?
    // SearchOrbitController.prototype.showSwath = function(oOrbit){
    //
    //
    //     if (utilsIsObjectNullOrUndefined(oOrbit))
    //         return false;
    //
    //     if (!oOrbit.hasOwnProperty('SwathFootPrint'))
    //         return false;
    //
    //     /* find orbit in orbit list*/
    //     if(utilsIsObjectNullOrUndefined(this.m_aoOrbits))
    //     {
    //         return false;
    //     }
    //
    //     //Find orbit in Orbits list return index
    //     var iIndexOrbitInOrbitsList = utilsFindObjectInArray(this.m_aoOrbits,oOrbit)
    //     if(iIndexOrbitInOrbitsList == -1)
    //         return false;
    //
    //     //if there is FootPrintRectangle set it null, otherwise create FootPrintRectangle
    //     // it correspond ----> FootPrintRectangle == null == uncheck ||  FootPrintRectangle != null == check
    //     if(!utilsIsObjectNullOrUndefined(this.m_aoOrbits[iIndexOrbitInOrbitsList].FootPrintRectangle))
    //     {
    //         this.m_oMapService.removeLayerFromMap(this.m_aoOrbits[iIndexOrbitInOrbitsList].FootPrintRectangle);//remove orbit rectangle from map
    //         this.m_aoOrbits[iIndexOrbitInOrbitsList].FootPrintRectangle = null;
    //     }
    //     else
    //     {
    //         //create oRectangle
    //         var sSwath = oOrbit.SwathFootPrint;
    //         var aasNewContent = [];
    //         //
    //         // sSwath = sSwath.replace("POLYGON","");
    //         // sSwath = sSwath.replace("((","");
    //         // sSwath = sSwath.replace("))","");
    //         // sSwath = sSwath.split(",");
    //         sSwath = utilsProjectGetPolygonArray(sSwath);
    //
    //         for (var iIndexBounds = 0; iIndexBounds < sSwath.length; iIndexBounds++)
    //         {
    //             var aBounds = sSwath[iIndexBounds];
    //             // console.log(aBounds);
    //             var aNewBounds = aBounds.split(" ");
    //
    //             var oLatLonArray = [];
    //
    //             oLatLonArray[0] = JSON.parse(aNewBounds[1]); //Lat
    //             oLatLonArray[1] = JSON.parse(aNewBounds[0]); //Lon
    //
    //             aasNewContent.push(oLatLonArray);
    //         }
    //         var oRectangle = this.m_oMapService.addRectangleByBoundsArrayOnMap(aasNewContent, null, 0);
    //         if(utilsIsObjectNullOrUndefined(oRectangle))
    //         {
    //             utilsVexDialogAlertTop("IMPOSSIBLE TO VISUALIZE ORBIT");
    //             return false;
    //         }
    //         this.m_aoOrbits[iIndexOrbitInOrbitsList].FootPrintRectangle = oRectangle;
    //     }
    //     return true;
    //
    // };

    /**
     *
     * @param oOrbit
     * @returns {boolean}
     */
    // SearchOrbitController.prototype.showFrame = function(oOrbit){
    //
    //
    //     if (utilsIsObjectNullOrUndefined(oOrbit))
    //         return false;
    //
    //     if (!oOrbit.hasOwnProperty('FrameFootPrint'))
    //         return false;
    //
    //     /* find orbit in orbit list*/
    //     if(utilsIsObjectNullOrUndefined(this.m_aoOrbits))
    //     {
    //         return false;
    //     }
    //
    //     //Find orbit in Orbits list return index
    //     var iIndexOrbitInOrbitsList = utilsFindObjectInArray(this.m_aoOrbits,oOrbit)
    //     if(iIndexOrbitInOrbitsList == -1)
    //         return false;
    //
    //     //if there is FootPrintRectangle set it null, otherwise create FootPrintRectangle
    //     // it correspond ----> FootPrintRectangle == null == uncheck ||  FootPrintRectangle != null == check
    //     if(!utilsIsObjectNullOrUndefined(this.m_aoOrbits[iIndexOrbitInOrbitsList].FootPrintRectangle))
    //     {
    //         this.m_oMapService.removeLayerFromMap(this.m_aoOrbits[iIndexOrbitInOrbitsList].FootPrintRectangle);//remove orbit rectangle from map
    //         this.m_aoOrbits[iIndexOrbitInOrbitsList].FootPrintRectangle = null;
    //     }
    //     else
    //     {
    //         //create oRectangle
    //         var sFrame = oOrbit.FrameFootPrint;
    //         var aasNewContent = [];
    //
    //         // sFrame = sFrame.replace("POLYGON","");
    //         // sFrame = sFrame.replace("((","");
    //         // sFrame = sFrame.replace("))","");
    //         // sFrame = sFrame.split(",");
    //         sFrame = utilsProjectGetPolygonArray(sFrame);
    //
    //         for (var iIndexBounds = 0; iIndexBounds < sFrame.length; iIndexBounds++)
    //         {
    //             var aBounds = sFrame[iIndexBounds];
    //             // console.log(aBounds);
    //             var aNewBounds = aBounds.split(" ");
    //
    //             var oLatLonArray = [];
    //
    //             oLatLonArray[0] = JSON.parse(aNewBounds[1]); //Lat
    //             oLatLonArray[1] = JSON.parse(aNewBounds[0]); //Lon
    //
    //             aasNewContent.push(oLatLonArray);
    //         }
    //         var oRectangle = this.m_oMapService.addRectangleByBoundsArrayOnMap(aasNewContent, null, 0);
    //         if(utilsIsObjectNullOrUndefined(oRectangle))
    //         {
    //             utilsVexDialogAlertTop("IMPOSSIBLE TO VISUALIZE ORBIT");
    //             return false;
    //         }
    //         this.m_aoOrbits[iIndexOrbitInOrbitsList].FootPrintRectangle = oRectangle;
    //     }
    //     return true;
    //
    // };
    /**
     * openWorkspace
     * @param sWorkspaceId
     */
    SearchOrbitController.prototype.openWorkspace = function (sWorkspaceId) {

        var oController = this;

        this.m_oWorkspaceService.getWorkspaceEditorViewModel(sWorkspaceId).success(function (data, status) {
            if (data != null)
            {
                if (data != undefined)
                {
                    oController.m_oConstantsService.setActiveWorkspace(data);
                    oController.m_oActiveWorkspace = oController.m_oConstantsService.getActiveWorkspace();
                    /*Start Rabbit WebStomp*/
                    // oController.m_oRabbitStompService.initWebStomp("SearchOrbitController",oController);
                    oController.m_oProcessesLaunchedService.loadProcessesFromServer(oController.m_oActiveWorkspace.workspaceId);

                }
            }
        }).error(function (data,status) {
            //alert('error');
            utilsVexDialogAlertTop('GURU MEDITATION<br>ERROR IMPOSSIBLE GET WORKSPACE')
        });
    };

    // SearchOrbitController.prototype.showAllFrameFootPrint = function(oCheckValue)
    // {
    //     if(utilsIsObjectNullOrUndefined(this.m_aoOrbits))
    //     {
    //         return false;
    //     }
    //
    //     var aoFilteredOrbits = this.m_oFilter('filter')(this.m_aoOrbits,this.m_sFilterTable);
    //
    //     var iNumberOfOrbits = aoFilteredOrbits.length;
    //     this.m_bisLoadingAllOrbitInMap = true;
    //     if(oCheckValue)
    //     {
    //         //TRUE
    //         /*add all rectangles in map*/
    //         for(var iIndexOrbit = 0; iIndexOrbit < iNumberOfOrbits; iIndexOrbit++)
    //         {
    //             var oOrbit = aoFilteredOrbits[iIndexOrbit];
    //             //if there is saved in orbit the FootPrintRectangle we don't need to create it
    //             if(!utilsIsObjectNullOrUndefined(oOrbit.FootPrintRectangle))
    //             {
    //                 continue;
    //             }
    //             this.showFrame(oOrbit);
    //         }
    //     }
    //     else
    //     {
    //         //FALSE
    //         // /*remove all rectangles in map*/
    //         // for(var iIndexOrbit = 0; iIndexOrbit < iNumberOfOrbits; iIndexOrbit++)
    //         // {
    //         //     var oOrbit = this.m_aoOrbits[iIndexOrbit];
    //         //     this.m_oMapService.removeLayerFromMap(oOrbit.FootPrintRectangle);//remove orbit rectangle from map
    //         //     oOrbit.FootPrintRectangle = null;
    //         // }
    //         this.removeAllRectangle(aoFilteredOrbits);
    //
    //     }
    //     this.m_bisLoadingAllOrbitInMap = false;
    //     this.selectDeselectAllOrbits(this.m_bMasterCheck,aoFilteredOrbits);
    // };

    // TODO REMOVE IT?
    // SearchOrbitController.prototype.showAllSwath = function(oCheckValue){
    //     if(utilsIsObjectNullOrUndefined(this.m_aoOrbits))
    //         return false;
    //     //TODO ADD THIS  -->  var aoFilteredOrbits = this.m_oFilter('filter')(this.m_aoOrbits,this.m_sFilterTable);
    //     //TODO CHANGE m_aoOrbits with  aoFilteredOrbits
    //     var iNumberOfOrbits = this.m_aoOrbits.length;
    //     this.m_bisLoadingAllOrbitInMap = true;
    //     if(oCheckValue)
    //     {
    //         //TRUE
    //         /*add all rectangles in map*/
    //         for(var iIndexOrbit = 0; iIndexOrbit < iNumberOfOrbits; iIndexOrbit++)
    //         {
    //             var oOrbit = this.m_aoOrbits[iIndexOrbit];
    //             //if there is saved in orbit the FootPrintRectangle we don't need to create it
    //             if(!utilsIsObjectNullOrUndefined(oOrbit.FootPrintRectangle))
    //             {
    //                continue;
    //             }
    //             //TODO SUBSTITUTE THIS PART OF CODE WITH showSwath FUNCTION
    //             var sSwath = oOrbit.SwathFootPrint;
    //             var aasNewContent = [];
    //
    //             sSwath = sSwath.replace("POLYGON","");
    //             sSwath = sSwath.replace("((","");
    //             sSwath = sSwath.replace("))","");
    //             sSwath = sSwath.split(",");
    //
    //             for (var iIndexBounds = 0; iIndexBounds < sSwath.length; iIndexBounds++)
    //             {
    //                 var aBounds = sSwath[iIndexBounds];
    //                 //console.log(aBounds);
    //                 var aNewBounds = aBounds.split(" ");
    //
    //                 var oLatLonArray = [];
    //
    //                 oLatLonArray[0] = JSON.parse(aNewBounds[1]); //Lat
    //                 oLatLonArray[1] = JSON.parse(aNewBounds[0]); //Lon
    //
    //                 aasNewContent.push(oLatLonArray);
    //             }
    //             var oRectangle = this.m_oMapService.addRectangleByBoundsArrayOnMap(aasNewContent, null, 0);
    //             if(utilsIsObjectNullOrUndefined(oRectangle))
    //             {
    //                 utilsVexDialogAlertTop("IMPOSSIBLE VISUALIZE ORBIT");
    //                 return false;
    //             }
    //             this.m_aoOrbits[iIndexOrbit].FootPrintRectangle = oRectangle;
    //         }
    //
    //     }
    //     else
    //     {
    //         //FALSE
    //         /*remove all rectangles in map*/
    //         //TODO CHANGE this.m_aoOrbits with aoFiltered orbits
    //         this.removeAllRectangle(this.m_aoOrbits);
    //     }
    //     this.m_bisLoadingAllOrbitInMap = false;
    //     //TODO CHANGE this.m_aoOrbits with aoFiltered orbits
    //     this.selectDeselectAllOrbits(this.m_bMasterCheck,this.m_aoOrbits);
    //
    //     // this.m_oScope.$apply();
    // };

    // SearchOrbitController.prototype.removeAllRectangle = function(aoOrbits)
    // {
    //     if(utilsIsObjectNullOrUndefined(aoOrbits) === true)
    //     {
    //         return false;
    //     }
    //     var iNumberOfOrbits = aoOrbits.length;
    //
    //     for(var iIndexOrbit = 0; iIndexOrbit < iNumberOfOrbits; iIndexOrbit++)
    //     {
    //         var oOrbit = aoOrbits[iIndexOrbit];
    //
    //         if(utilsIsObjectNullOrUndefined(oOrbit.FootPrintRectangle) === false)
    //         {
    //             this.m_oMapService.removeLayerFromMap(oOrbit.FootPrintRectangle);//remove orbit rectangle from map
    //             oOrbit.FootPrintRectangle = null;
    //         }
    //     }
    //     return true;
    // }
    /**
     * selectDeselectAllOrbits
     * @returns {boolean}
     */
    // SearchOrbitController.prototype.selectDeselectAllOrbits = function(bValue,aoOrbits)
    // {
    //     if(utilsIsObjectNullOrUndefined(aoOrbits) === true)
    //     {
    //         return false;
    //     }
    //     var iNumberOfOrbits = aoOrbits.length;
    //
    //     for(var iIndexOrbit = 0 ; iIndexOrbit < iNumberOfOrbits; iIndexOrbit++ )
    //     {
    //         aoOrbits[iIndexOrbit].isSelected = bValue;
    //     }
    //     return true;
    // };

    /**
     * mouseOverOrbitInTable
     * @param oOrbit
     * @returns {boolean}
     */
    // SearchOrbitController.prototype.mouseOverOrbitInTable = function(oOrbit)
    // {
    //     if(utilsIsObjectNullOrUndefined(oOrbit))
    //         return false;
    //     if(utilsIsObjectNullOrUndefined(oOrbit.FootPrintRectangle))
    //         return false;
    //     var oRectangle = oOrbit.FootPrintRectangle;
    //     oRectangle.setStyle({weight:5,fillOpacity:1,color:"Black"});
    //     return true;
    // }
    // SearchOrbitController.prototype.mouseLeaveOrbitInTable = function(oOrbit)
    // {
    //     if(utilsIsObjectNullOrUndefined(oOrbit))
    //         return false;
    //     if(utilsIsObjectNullOrUndefined(oOrbit.FootPrintRectangle))
    //         return false;
    //     var oRectangle = oOrbit.FootPrintRectangle;
    //     oRectangle.setStyle({weight:1,fillOpacity:0.2,color:"#ff7800"});
    //     return true
    // }


    /**
     *
     */
    SearchOrbitController.prototype.openOrbitInfo =function(oOrbit)
    {

        if(utilsIsObjectNullOrUndefined(oOrbit))
            return false;

        var oController = this
        this.m_oModalService.showModal({
            templateUrl: "dialogs/orbit_info/OrbitInfoDialog.html",
            controller: "OrbitInfoController",
            inputs: {
                extras: oOrbit
            }
        }).then(function(modal) {
            modal.element.modal();
            modal.close.then(function(result) {
                oController.m_oScope.Result = result ;
            });
        });
        //var oMessage = "<div>"+oOrbit.Angle+"</div> " +
        //    "<div>"+oOrbit.SatelliteName+"</div>" +
        //    "<div>"+oOrbit.SensorMode+"</div>" +
        //    "<div>"+oOrbit.SensorName+"</div> " +
        //    "<div>"+oOrbit.SensorType+"</div> " +
        //    "<div>"+oOrbit.SwathName+"</div>";
        //var oOptions ={unsafeMessage: oMessage };
        //
        //vex.dialog.alert(oOptions);
        return true;
    };


    /**
     * Rabbit Message Callback
     * @param oMessage
     * @param oController
     */
    SearchOrbitController.prototype.receivedRabbitMessage  = function (oMessage, oController) {

        if (oMessage == null) return;
        // Check the Result
        if (oMessage.messageResult == "KO") {

            var sOperation = "null";
            if (utilsIsStrNullOrEmpty(oMessage.messageCode) === false  )
                sOperation = oMessage.messageCode;
            var oDialog = utilsVexDialogAlertTop('GURU MEDITATION<br>THERE WAS AN ERROR IN THE ' + sOperation + ' PROCESS');
            utilsVexCloseDialogAfter(4000, oDialog);
            this.m_oProcessesLaunchedService.loadProcessesFromServer(this.m_oActiveWorkspace.workspaceId);
            return;
        }
        switch(oMessage.messageCode)
        {
            case "PUBLISH":
            case "PUBLISHBAND":
            case "UPDATEPROCESSES":
                break;
            case "APPLYORBIT":
            case "CALIBRATE":
            case "MULTILOOKING":
            case "NDVI":
            case "TERRAIN":
            case "DOWNLOAD":
            case "GRAPH":
            case "INGEST":
                oController.receivedNewProductMessage(oMessage,oController);
                break;
            default:
                console.log("RABBIT ERROR: got empty message ");
        }

        utilsProjectShowRabbitMessageUserFeedBack(oMessage);

    }
    /**
     *
     * @param oMessage
     * @param oController
     */
    SearchOrbitController.prototype.receivedNewProductMessage = function (oMessage, oController) {
        var sMessage = 'PRODUCT ADDED TO THE WORKSPACE<br>READY';

        // this.test=function(){
        //     this.m_oState.go("root.catalog", { })
        // };
        // if(oMessage.messageCode !== "DOWNLOAD" )
        //     sMessage += " <a href='' ng-click='this.test()'> Link </a>"

        var oDialog = utilsVexDialogAlertBottomRightCorner(sMessage);
        utilsVexCloseDialogAfter(4000, oDialog);
    };

    // SearchOrbitController.prototype.checkedSatellite = function(sSatellite)
    // {
    //     if(utilsIsStrNullOrEmpty(sSatellite) === false)
    //     {
    //         this.m_oSelectedSatellite.push(sSatellite);
    //     }
    // };
    // SearchOrbitController.prototype.uncheckedSatellite = function(sSatellite)
    // {
    //     if(utilsIsStrNullOrEmpty(sSatellite) === false)
    //     {
    //         // var iNumberOfSelectedSatellite = this.m_oSelectedSatellite.length;
    //         utilsRemoveObjectInArray(this.m_oSelectedSatellite,sSatellite);
    //     }
    // };
    /**
     *
     * @param sSatellite
     * @returns {*}
     */
    // SearchOrbitController.prototype.isSatelliteSelected = function(sSatellite)
    // {
    //     if(utilsIsStrNullOrEmpty(sSatellite) === true)
    //     {
    //         return false;
    //     }
    //     return utilsIsElementInArray(this.m_oSelectedSatellite,sSatellite);
    // }

    SearchOrbitController.prototype.clearOrbitsTable = function(){

        this.removeAllLayers();
        this.m_aoOrbits = [];
        this.m_isDisabledSearchButton = false;
        this.m_oTreeService.uncheckAllNodes(this.m_sIdDiv)
        return true;
    };


    SearchOrbitController.prototype.downloadKML = function(oNode)
    {
        if(utilsIsObjectNullOrUndefined(oNode) === true)
        {
            return false;
        }
        var oController = this;
        var sText = oNode.text;
        var sFootPrint = oNode.original.FrameFootPrint;
        console.log(sFootPrint);
        this.m_oSearchOrbitService.getKML(sText,sFootPrint)
            .success(function(data,state){
                if( utilsIsObjectNullOrUndefined(data) === false )
                {
                    var textFile = null;
                    var sType = 'application/xml';
                    var sUrl = utilsMakeFile(data,textFile,sType);
                    utilsSaveFile(sUrl,sText+".kml");

                }
                else
                {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN DOWNLOAD KML");
                }
            })
            .error(function(data,state){
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN  DOWNLOAD KML");
            });

        return true;
    };

    SearchOrbitController.prototype.getResultOpportunitiesTreeJson = function(oDataInput)
    {

        var oData1 = this.generateDataTree(oDataInput);
        var oController = this;
        var oJsonData = {
            core: {
                data: oData1,
                    check_callback: false
            },
            checkbox: {
                three_state : false, // to avoid that fact that checking a node also check others
                    whole_node : false,  // to avoid checking the box just clicking the node
                    tie_selection : false, // for checking without selecting and selecting without checking
            },
            plugins: ['checkbox','contextmenu'],
            "contextmenu": { // my right click menu
                "items":function($node){
                    // var oController = this;
                    var bIsEmptyNodeFootPrint = oController.isEmptyNodeFootPrint($node);

                    var oReturnValue = {
                        "KML": {
                            "label": "Download KML",

                            "action": function (obj) {
                                oController.downloadKML($node);

                            },
                            "_disabled":bIsEmptyNodeFootPrint
                        }
                    }

                    return oReturnValue;
                }

            }
        }

        return oJsonData;
    };


    SearchOrbitController.prototype.generateDataTree = function(aoData)
    {
        if( utilsIsObjectNullOrUndefined(aoData) )
        {
            return null;
        }

        var iNumberOfOpportunities = aoData.length;
        var oReturnValue = [{
            "id": "results",
            "text": "Results",
            "icon":"folder-icon-menu-jstree",
            "state": { "opened": true },
            "children": []
        }];

        var sIdDiv = this.m_sIdDiv;
        var oCheckFunction = this.onCheckEventTreeFunction;
        var oUncheckFunction = this.onUncheckEventTreeFunction;
        var sCheckEvent = this.m_oTreeService.getCheckNodeNameEvent();
        var sUncheckEvent = this.m_oTreeService.getUncheckNodeNameEvent();
        var oController = this;

        //Opportunities = search results = orbits
        for(var iIndexOpportunity = 0 ; iIndexOpportunity < iNumberOfOpportunities; iIndexOpportunity++ )
        {
            var oData = new Date(aoData[iIndexOpportunity].AcquisitionStartTime);

            var sMonth = oData.getUTCMonth() + 1; //months from 1-12
            var sDay = oData.getUTCDate();
            var sYear = oData.getUTCFullYear();
            var sNewDate = sYear + "/" + sMonth + "/" + sDay;

            var sHours = oData.getHours();
            var sMinutes = oData.getMinutes();
            var sSeconds = oData.getSeconds();
            var sTimes = sHours + ":" + sMinutes + ":" + sSeconds;

            var sSensorMode = ( utilsIsObjectNullOrUndefined(aoData[iIndexOpportunity].SensorMode) ? "" : aoData[iIndexOpportunity].SensorMode );
            var sSensorLookDirection = aoData[iIndexOpportunity].SensorLookDirection;
            var sSwathName = aoData[iIndexOpportunity].SwathName;
            var asSplittedSwathName = sSwathName.split("__");
            var sFrameName = asSplittedSwathName[0] + "-" + asSplittedSwathName[1] + "-" + sSensorMode + "-H" + sTimes ;

            //add data node
            var oResultSearchTreeData = this.generateNode(oReturnValue[0],sNewDate);
            oResultSearchTreeData.icon = "calendar1-square-icon-menu-jstree";

            //add sensor look direction node
            var oResultSearchTreeSensorLookDirection = this.generateNode(oResultSearchTreeData,sSensorLookDirection);
            oResultSearchTreeSensorLookDirection.icon = "arrow-mix-icon-menu-jstree";
            //add swath node
            var oResultSearchTreeSwathName = this.generateNode(oResultSearchTreeSensorLookDirection,sSwathName);
            oResultSearchTreeSwathName.icon = "vector-square-icon-menu-jstree";
            oResultSearchTreeSwathName.isSwath = true;
            if( (utilsIsStrNullOrEmpty(aoData[iIndexOpportunity].FrameFootPrint) === true) && (utilsIsStrNullOrEmpty(aoData[iIndexOpportunity].SwathFootPrint) === false) )
            {
                oResultSearchTreeSwathName.FrameFootPrint = aoData[iIndexOpportunity].SwathFootPrint;
                continue;
            }

            //add frame node
            var oFrameNode = this.generateNode(oResultSearchTreeSwathName,sFrameName);
            oFrameNode.icon = "selection-icon-menu-jstree";
            oFrameNode.isFrame = true;

            if( (utilsIsStrNullOrEmpty(aoData[iIndexOpportunity].FrameFootPrint) === false) )
            {
                oFrameNode.FrameFootPrint = aoData[iIndexOpportunity].FrameFootPrint;

            }

        }

        //check event on tree
        this.m_oTreeService.onTreeEvent(sCheckEvent,sIdDiv,oCheckFunction,oController);
        //uncheck event on tree
        this.m_oTreeService.onTreeEvent(sUncheckEvent,sIdDiv,oUncheckFunction,oController);

        return oReturnValue;

    };

    SearchOrbitController.prototype.onCheckEventTreeFunction = function(oController,e,data)
    {
        if(utilsIsObjectNullOrUndefined(data) === true )
        {
            return false;
        }
        var oNode = data.node;
        if(utilsIsObjectNullOrUndefined(oNode) === true )
        {
            return false;
        }


        if(oNode.original.isFrame || oNode.original.isSwath)
        {
            if( utilsIsObjectNullOrUndefined(oNode.original.rectangle) === true)
            {
                var sColor = "orange";
                if (oNode.original.isSwath) sColor = "yellow";

                var oRectangle = oController.drawRectangleInMap(oNode.original,sColor);
                oNode.original.rectangle = oRectangle;
            }
        }

        return true;
    };



    SearchOrbitController.prototype.onUncheckEventTreeFunction = function(oController,e,data)
    {
        if(utilsIsObjectNullOrUndefined(data) === true )
        {
            return false;
        }
        var oNode = data.node;
        if(utilsIsObjectNullOrUndefined(oNode) === true )
        {
            return false;
        }

        if(oNode.original.isFrame || oNode.original.isSwath)
        {
            if(utilsIsObjectNullOrUndefined(oNode.original.rectangle) === false)
            {
                oController.m_oMapService.removeLayerFromMap(oNode.original.rectangle);
                oNode.original.rectangle = null;
            }
        }

        return true;
    };

    SearchOrbitController.prototype.drawAllFrames = function(aoFrames)
    {
        if(utilsIsObjectNullOrUndefined(aoFrames) === false)
        {

            var iNumberOfFrames = aoFrames.length;

            for(var iIndexFrame = 0; iIndexFrame < iNumberOfFrames; iIndexFrame++ )
            {
                if( utilsIsObjectNullOrUndefined(aoFrames[iIndexFrame].original.rectangle) === true)
                {
                    // var sFrameFootPrint = aoFrames[iIndex].original.FrameFootPrint;
                    var oRectangle = this.drawRectangleInMap(aoFrames[iIndexFrame].original);
                    aoFrames[iIndexFrame].original.rectangle = oRectangle;
                }

            }
        }
    };

    SearchOrbitController.prototype.removeAllFrames = function(aoFrames)
    {
        if(utilsIsObjectNullOrUndefined(aoFrames) === false)
        {

            var iNumberOfFrames = aoFrames.length;

            for(var iIndexFrame = 0; iIndexFrame < iNumberOfFrames; iIndexFrame++ )
            {

                if(utilsIsObjectNullOrUndefined(aoFrames[iIndexFrame].original.rectangle) === false)
                {
                    this.m_oMapService.removeLayerFromMap(aoFrames[iIndexFrame].original.rectangle);
                    aoFrames[iIndexFrame].original.rectangle = null;
                }
            }
        }
    };

    SearchOrbitController.prototype.getAllFramesInBranch = function(asNodes,oResult = [])
    {
        for(var i = 0, length = asNodes.length; i < length; i++){
            var oNode = this.m_oTreeService.getNodeById(asNodes[i],this.m_sIdDiv);
            if(!oNode.children || oNode.children.length === 0){
                oResult.push(oNode);
            }else{
                // var oChildren = this.m_oMapService.getNodeById(oNode.children);
                oResult = this.getAllFramesInBranch(oNode.children, oResult);
            }
        }
        return oResult;
    };

    SearchOrbitController.prototype.generateNode = function(oTree,sNewNode)
    {
        var sPropertyIdNodeName = "text";
        var sPropertyChilderNodeName = "children";
        var oResultSearch = utilsSearchTree(oTree,sNewNode,sPropertyIdNodeName,sPropertyChilderNodeName);
        if (utilsIsObjectNullOrUndefined(oResultSearch))
        {
            //new data
            var oNewDataNode={
                // "id": sNewNode,
                "text": sNewNode,
                "children": []
            }

            //add a new data result
            oTree.children.push(oNewDataNode);
            oResultSearch = utilsSearchTree(oTree,sNewNode,sPropertyIdNodeName,sPropertyChilderNodeName);
        }
        return oResultSearch;
    };

    SearchOrbitController.prototype.drawRectangleInMap = function(oOrbit,sColor)
    {
        if(utilsIsStrNullOrEmpty(sColor) === true)
        {
            sColor = null;
        }

        if (utilsIsObjectNullOrUndefined(oOrbit))
        {
            return null;
        }

        if (!oOrbit.hasOwnProperty('FrameFootPrint'))
        {
            return null;
        }

        //create oRectangle
        var sFrame = oOrbit.FrameFootPrint;
        var aasNewContent = [];

        // sFrame = sFrame.replace("POLYGON","");
        // sFrame = sFrame.replace("((","");
        // sFrame = sFrame.replace("))","");
        // sFrame = sFrame.split(",");
        sFrame = utilsProjectGetPolygonArray(sFrame);

        for (var iIndexBounds = 0; iIndexBounds < sFrame.length; iIndexBounds++)
        {
            var aBounds = sFrame[iIndexBounds];
            // console.log(aBounds);
            var aNewBounds = aBounds.split(" ");

            var oLatLonArray = [];

            oLatLonArray[0] = JSON.parse(aNewBounds[1]); //Lat
            oLatLonArray[1] = JSON.parse(aNewBounds[0]); //Lon

            aasNewContent.push(oLatLonArray);
        }
        var oRectangle = this.m_oMapService.addRectangleByBoundsArrayOnMap(aasNewContent, sColor, 0);
        if(utilsIsObjectNullOrUndefined(oRectangle))
        {
            utilsVexDialogAlertTop("THERE ARE PROBLEMS IN ORBIT VISUALIZATION");
            return null;
        }
        return oRectangle;
    };

    SearchOrbitController.prototype.getSatellitesResources = function()
    {
        var oController = this;
        this.m_oSearchOrbitService.getSatellitesResources()
            .success(function(data,status){
                if(utilsIsObjectNullOrUndefined(data) === false || status !== 200)
                {
                    // oController.m_aoSatelliteResources = data;
                    // var oTree = oController.getSatellitesResourcesTreeJson(oController.m_aoSatelliteResources);
                    data = oController.setAllSatelliteSensorsDisable(data);
                    oController.m_aoSatelliteResources = data;
                    if(utilsIsObjectNullOrUndefined(oController.m_aoSatelliteResourcesTree) === true)
                    {
                        oController.m_aoSatelliteResources = data;
                        oController.m_aoSatelliteResourcesTree = oController.getSatellitesResourcesTreeJson(data);
                        oController.m_oTreeService.createNewInstanceTree(oController.m_sIdDivSatelliteResourceTree,oController.m_aoSatelliteResourcesTree);
                    }
                    else
                    {
                        oController.m_aoSatelliteResourcesTree = oController.getSatellitesResourcesTreeJson(data);
                        oController.m_oTreeService.loadNewTree(oController.m_sIdDivSatelliteResourceTree,oController.m_aoSatelliteResourcesTree);
                    }


                }
                else
                {
                    utilsVexDialogAlertTop("THERE ARE PROBLEMS IN SATELLITE RESOURCES");
                }
            })
            .error(function(data,status){
                utilsVexDialogAlertTop("THERE ARE PROBLEMS IN SATELLITE RESOURCES");
            })
    };

    SearchOrbitController.prototype.setAllSatelliteSensorsDisable = function(aoSatelliteResources)
    {
        var iNumberOfSatelliteResources = aoSatelliteResources.length;

        for(var iIndexSatelliteResource = 0 ; iIndexSatelliteResource < iNumberOfSatelliteResources; iIndexSatelliteResource++)
        {
            var oSatelliteResource = aoSatelliteResources[iIndexSatelliteResource]
            var iNumberOfSensors = oSatelliteResource.satelliteSensors.length;
            for(var iSensor = 0; iSensor < iNumberOfSensors;iSensor++)
            {
                oSatelliteResource.satelliteSensors[iSensor].enable = false;
            }
        }
        return aoSatelliteResources;
    };

    SearchOrbitController.prototype.getSatellitesResourcesTreeJson = function(oDataInput)
    {

        var oData1 = this.generateSatellitesResourcesTree(oDataInput);
        var oController = this;
        var oJsonData = {
            core: {
                data: oData1,
                check_callback: false
            },
            checkbox: {
                three_state : false, // to avoid that fact that checking a node also check others
                whole_node : false,  // to avoid checking the box just clicking the node
                tie_selection : false // for checking without selecting and selecting without checking
            },
            plugins: ['checkbox','contextmenu'],
            "contextmenu": { // my right click menu
                "items":function($node){
                    // var oController = this;
                    // var bIsEmptyNodeFootPrint = oController.isEmptyNodeFootPrint($node);
                    var bIsSensorNode = oController.IsSensorNode($node);
                    var oReturnValue = {
                        "Select all sensor modes": {
                            "label": "Select all sensor modes",
                            "action": function (obj) {
                                // oController.downloadKML($node);
                                oController.m_oTreeService.checkChildren(oController.m_sIdDivSatelliteResourceTree,$node)
                            },
                             "_disabled":!bIsSensorNode
                        },
                        "Deselect all sensor modes": {
                            "label": "Deselect all sensor modes",
                            "action": function (obj) {
                                // oController.downloadKML($node);
                                oController.m_oTreeService.uncheckChildren(oController.m_sIdDivSatelliteResourceTree,$node)
                            },
                            "_disabled":!bIsSensorNode
                        }
                    }

                    return oReturnValue;
                }

            }
        }

        return oJsonData;
    };

    // SearchOrbitController.prototype.functionContextMenu = function($node,oController)
    // {
    //     // var oController = this;
    //     var bIsEmptyNodeFootPrint = oController.isEmptyNodeFootPrint($node);
    //
    //     var oReturnValue = {
    //             "KML": {
    //             "label": "Download KML",
    //
    //             "action": function (obj) {
    //                 // console.log("KML");
    //                 oController.downloadKML($node);
    //                 // $node.original
    //                 // if($node.original.isFrame || $node.original.isSwath)
    //                 // {
    //                 //     if( utilsIsObjectNullOrUndefined(oNode.original.rectangle) === true)
    //                 //     {
    //                 //         var sColor = "orange";
    //                 //         if (oNode.original.isSwath) sColor = "yellow";
    //                 //
    //                 //         var oRectangle = oController.drawRectangleInMap(oNode.original,sColor);
    //                 //         oNode.original.rectangle = oRectangle;
    //                 //     }
    //                 // }
    //
    //             },
    //             "_disabled":bIsEmptyNodeFootPrint
    //
    //             // "icon":"radar-icon-context-menu-jstree",
    //         }
    //     }
    //
    //     return oReturnValue;
    // };
    SearchOrbitController.prototype.IsSensorNode = function(oNode)
    {
        if( (utilsIsObjectNullOrUndefined(oNode) === false) &&
            (utilsIsObjectNullOrUndefined(oNode.original.isSensor) === false) &&
            (oNode.original.isSensor === true) )
        {
            return true;
        }
        return false;
    }
    SearchOrbitController.prototype.isEmptyNodeFootPrint = function(oNode)
    {
        if( (oNode.original.isFrame === false) || (oNode.original.isSwath===false) ||
            (oNode.original.hasOwnProperty('FrameFootPrint') === false) ||
            (utilsIsStrNullOrEmpty(oNode.original.FrameFootPrint) === true) )
        {
            return true;
        }

        return false;
    };

    SearchOrbitController.prototype.generateSatellitesResourcesTree = function(oDataInput)
    {
        if( utilsIsObjectNullOrUndefined(oDataInput) )
        {
            return null;
        }

        var iNumberOfSatellites = oDataInput.length;
        var aoSatellites = oDataInput;
        var oReturnValue = [{
            "id": "results",
            "text": "Satellites",
            "icon":"folder-icon-menu-jstree",
            "state": { "opened": true },
            "children": []
        }];

        // var sIdDiv = this.m_sIdDivSatelliteResourceTree;
        // var oController = this;

        //for each satellite
        for(var iSatellite = 0 ; iSatellite < iNumberOfSatellites; iSatellite++)
        {
            var sSatelliteName = aoSatellites[iSatellite].satelliteName;

            //add satellite
            var oSatelliteTree = this.generateNode(oReturnValue[0],sSatelliteName);
            oSatelliteTree.icon = "satelite-icon-context-menu-jstree";

            var oSatellite = aoSatellites[iSatellite];
            var iNumberOfSatelliteSensors = oSatellite.satelliteSensors.length;

            //for each sensor
            for(var iSensor = 0; iSensor < iNumberOfSatelliteSensors; iSensor++)
            {
                var sDescription = oSatellite.satelliteSensors[iSensor].description;

                //add sensor
                var oSatelliteSensorTree = this.generateNode(oSatelliteTree,sDescription);
                oSatelliteSensorTree.icon = "folder-icon-menu-jstree";
                oSatelliteSensorTree.isSensor = true;

                var oSensor = oSatellite.satelliteSensors[iSensor];
                var iNumberOfSensorModes = oSensor.sensorModes.length;
                //for each sensor mode
                for(var iSensorMode = 0; iSensorMode <  iNumberOfSensorModes; iSensorMode++)
                {
                    var sName = oSensor.sensorModes[iSensorMode].name;
                    var oSensorModeNode = this.generateNode(oSatelliteSensorTree,sName);
                    oSensorModeNode.icon = "touch-icon-menu-jstree";
                }

            }

            // var oSatelliteTree = this.generateNode(oSatelliteTree,sSatelliteName);

        }

        return oReturnValue;
    };

    SearchOrbitController.prototype.searchOrbit = function()
    {

        var sError = "";
        if(utilsIsObjectNullOrUndefined(this.m_oGeoJSON))
        {
            sError += "PLEASE SELECT AREA OF INTEREST<br>TOP RIGHT CORNER OF THE MAP<br>";
            utilsVexDialogAlertDefault(sError);
            return false;
        }

        this.setDisabledAllOpportunities(this.m_aoSatelliteResources);

        var aoNodes = this.getAllSelectedNode();
        var aoTempJSON = this.generateArrayJSONSearchOrbit(aoNodes,this.m_aoSatelliteResources);
        var oController = this;
        aoTempJSON = this.removeUselessInfo(aoTempJSON);

        var sAcquisitionStartTime = this.m_oOrbitSearch.acquisitionStartTime += ":00:00";
        var sAcquisitionEndTime = this.m_oOrbitSearch.acquisitionEndTime += ":00:00";

        var oJSON = {
            satelliteFilters:aoTempJSON,
            polygon:this.getPolygon(),
            acquisitionStartTime:sAcquisitionStartTime,
            acquisitionEndTime:sAcquisitionEndTime
        }
        this.m_bIsVisibleLoadingIcon = true;
        // oJSON = JSON.string
        // ify(oJSON);
        this.m_isDisabledSearchButton = true;

        this.m_oSearchOrbitService.searchOrbit(oJSON)
            .success(function (data, status, headers, config) {
                if(!utilsIsObjectNullOrUndefined(data))
                {
                    //var oDataTree = oController.generateDataTree(data);
                    // oController.m_oTreeService.createNewInstanceTree("#orbitsTree",oController.generateDataTree(data));
                    oController.m_aoOrbits = data;

                    if(utilsIsObjectNullOrUndefined(oController.m_oOpportunitiesTree) === true)
                    {
                        oController.m_oOpportunitiesTree = oController.getResultOpportunitiesTreeJson(data);
                        oController.m_oTreeService.createNewInstanceTree(oController.m_sIdDiv,oController.m_oOpportunitiesTree);
                    }
                    else
                    {
                        oController.m_oOpportunitiesTree = oController.getResultOpportunitiesTreeJson(data);
                        oController.m_oTreeService.loadNewTree(oController.m_sIdDiv,oController.m_oOpportunitiesTree);
                    }

                }
                else
                {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR: SEARCH ORBITS FAILS.");
                    oController.m_isDisabledSearchButton = false;
                }
                // oController.initOrbitSearch();
                oController.m_bIsVisibleLoadingIcon = false;
                // oController.m_aoSatelliteResources=[];
            })
            .error(function (data, status, header, config) {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR: SEARCH ORBITS FAILS.");
                // oController.initOrbitSearch();
                oController.m_aoOrbits = null;
                oController.m_bIsVisibleLoadingIcon = false;
                oController.m_isDisabledSearchButton = false;
                // oController.m_aoSatelliteResources=[];
            });

    };
    SearchOrbitController.prototype.setDisabledAllOpportunities = function(aoSatelliteResources)
    {
        var iNumberOfSatellites = aoSatelliteResources.length;
        for(var iIndexSatellite = 0; iIndexSatellite < iNumberOfSatellites; iIndexSatellite++)
        {
            var oSatellite = aoSatelliteResources[iIndexSatellite];
            oSatellite.enable = false;
            // oSatellite.enabled = false;
            var iNumberOfSatelliteSensors = oSatellite.satelliteSensors.length;
            for(var iIndexSensors = 0; iIndexSensors < iNumberOfSatelliteSensors;iIndexSensors++)
            {
                var aoSatelliteSensors = oSatellite.satelliteSensors[iIndexSensors];
                aoSatelliteSensors.enable = false;
                // aoSatelliteSensors.enabled = false;
                var iNumberOfSensorModes = aoSatelliteSensors.sensorModes.length;
                for(var iIndexSensorMode = 0 ; iIndexSensorMode < iNumberOfSensorModes ; iIndexSensorMode++ )
                {
                    var oSensorMode = aoSatelliteSensors.sensorModes[iIndexSensorMode];
                    oSensorMode.enable = false;
                    // oSensorMode.enabled = false;
                }
            }
        }
    }
    SearchOrbitController.prototype.removeUselessInfo = function(aoSatellite){
        var iNumberOfSatellites = aoSatellite.length;
        var oReturnValue = []
        for(var iIndexSatellite = 0; iIndexSatellite < iNumberOfSatellites; iIndexSatellite++)
        {
            var oSatellite = aoSatellite[iIndexSatellite];
            var oTempSatellite = {};
            oTempSatellite.enable = oSatellite.enable;
            oTempSatellite.satelliteName = oSatellite.satelliteName;
            oTempSatellite.satelliteSensors = []
            var iNumberOfSensors = oSatellite.satelliteSensors.length;
            for(var iIndexSensor = 0;iIndexSensor < iNumberOfSensors ; iIndexSensor++)
            {
                var oSensor = oSatellite.satelliteSensors[iIndexSensor];
                if(oSensor.enable)
                {
                    var oTempSensor = {};
                    oTempSensor.description = oSensor.description;
                    oTempSensor.enable = oSensor.enable;
                    oTempSensor.sensorModes = [];
                    var iNumberOfSensorModes = oSensor.sensorModes.length;
                    for(var iIndexSensorMode = 0; iIndexSensorMode < iNumberOfSensorModes; iIndexSensorMode++)
                    {
                        var oSensorMode = oSensor.sensorModes[iIndexSensorMode];
                        if(oSensorMode.enable)
                        {
                            var oTempSensorMode = {};
                            oTempSensorMode.name = oSensorMode.name;
                            oTempSensorMode.enable = oSensorMode.enable;
                            oTempSensor.sensorModes.push(oTempSensorMode);
                        }

                    }
                    oTempSatellite.satelliteSensors.push(oTempSensor);
                }
            }

            oReturnValue.push(oTempSatellite);
        }
        return oReturnValue;
    };

    SearchOrbitController.prototype.getAllSelectedNode = function()
    {
        //todo remove point 1 bookmark call ale cottino
        var asAllCheckedIdNode = this.m_oTreeService.getAllCheckedIDNode(this.m_sIdDivSatelliteResourceTree);
        var iNumberOfCheckedNodes = asAllCheckedIdNode.length;
        var aoNodes = [];

        for(var iCheckedNode = 0 ; iCheckedNode < iNumberOfCheckedNodes; iCheckedNode++)
        {
            var oNode = this.m_oTreeService.getNodeById(asAllCheckedIdNode[iCheckedNode],this.m_sIdDivSatelliteResourceTree );
            aoNodes.push(oNode);
        }
        return aoNodes;

    }

    SearchOrbitController.prototype.generateArrayJSONSearchOrbit = function(aoNodes,aoSatelliteResources)
    {
        if(utilsIsObjectNullOrUndefined(aoNodes) === true || utilsIsObjectNullOrUndefined(aoSatelliteResources) === true)
        {
            return null;
        }

        var iNumberOfNodes = aoNodes.length;
        var aoSelectedFilters = [];

        //take selected filters in tree
        for(var iIndexNode = 0; iIndexNode < iNumberOfNodes; iIndexNode ++)
        {
            var oJsonNode = this.searchNodeInSatelliteResources(aoNodes[iIndexNode]);
            if(utilsIsObjectNullOrUndefined(oJsonNode) === false)
            {
                aoSelectedFilters.push(oJsonNode);
            }

        }

        var iNumberOfSelectedFilters = aoSelectedFilters.length;


        //enable selected filters
        for(var iIndexSelectedFilter = 0; iIndexSelectedFilter < iNumberOfSelectedFilters; iIndexSelectedFilter ++)
        {
            var sSatelliteName = aoSelectedFilters[iIndexSelectedFilter].text;
            var sSatelliteSensorDescription = "";
            var sSatelliteSensorMode = "";
            if(aoSelectedFilters[iIndexSelectedFilter].children.length !== 0)
            {
                sSatelliteSensorDescription = aoSelectedFilters[iIndexSelectedFilter].children[0].text;
                if(aoSelectedFilters[iIndexSelectedFilter].children[0].children.length !== 0)
                {
                    sSatelliteSensorMode = aoSelectedFilters[iIndexSelectedFilter].children[0].children[0].text;
                }
            }
            var oSatellite = this.getSatelliteByName(aoSatelliteResources,sSatelliteName);
            oSatellite.enable = true;
            this.setSatelliteSensorEnable(oSatellite,sSatelliteSensorDescription,sSatelliteSensorMode);
            // aoJSONReturnValue.push(oSatellite);
        }

        //take satellites selected filters (view model)
        var iNumberOfSatelliteResources = aoSatelliteResources.length;
        var aoJSONReturnValue = [];

        for(var iSatelliteResource = 0; iSatelliteResource < iNumberOfSatelliteResources ; iSatelliteResource++)
        {
            if(aoSatelliteResources[iSatelliteResource].enable)
            {
                aoJSONReturnValue.push(aoSatelliteResources[iSatelliteResource]);
            }

        }


        // this.setEnableAllSelectedCheckFilters(aoSelectedFilters,aoSatelliteResources);
        // aoSatelliteResources;
        // aoJSONReturnValue = aoSatelliteResources;
        return aoJSONReturnValue;
    };

    SearchOrbitController.prototype.setSatelliteSensorEnable = function(oSatellite,sSatelliteSensorDescription,sSatelliteSensorMode)
    {
        if(utilsIsStrNullOrEmpty(sSatelliteSensorDescription))
        {
            return ;
        }
        var iNumberOfSatelliteSensors = oSatellite.satelliteSensors.length;
        // var iNumberOfSensorsModes = oSatellite.satelliteSensors.sensorModes.length;
        for(var iIndexSatelliteSensor = 0 ; iIndexSatelliteSensor < iNumberOfSatelliteSensors; iIndexSatelliteSensor ++ )
        {
            var oSatelliteSensor=oSatellite.satelliteSensors[iIndexSatelliteSensor];
            if(sSatelliteSensorDescription === oSatelliteSensor.description)
            {
                oSatelliteSensor.enable = true;
            }
            if(utilsIsStrNullOrEmpty(sSatelliteSensorMode) === false)
            {
                var iNumberOfSensorModes = oSatelliteSensor.sensorModes.length;
                for(var iSensorMode = 0 ; iSensorMode < iNumberOfSensorModes; iSensorMode++ )
                {
                    if( oSatelliteSensor.sensorModes[iSensorMode].name === sSatelliteSensorMode)
                    {
                        oSatelliteSensor.sensorModes[iSensorMode].enable = true;
                    }

                }

            }
        }
    };

    SearchOrbitController.prototype.getSatelliteByName = function(aoSatelliteResources,sSatelliteName)
    {
        var iNumberOfSatellites = aoSatelliteResources.length;
        for(var iIndexSatellite = 0; iIndexSatellite < iNumberOfSatellites; iIndexSatellite++)
        {
            if( aoSatelliteResources[iIndexSatellite].satelliteName === sSatelliteName )
            {
                return aoSatelliteResources[iIndexSatellite];
            }
        }
        return null;
    };


    // SearchOrbitController.prototype.setEnableAllSelectedCheckFilters = function(aoSelectedFilters,aoSatelliteResources)
    // {
    //     var aoAllEnableSatellites = this.getAllSatellitesEnable(aoSelectedFilters,aoSatelliteResources);
    //
    // };

    // SearchOrbitController.prototype.setEnableAllSensorsModes = function(aoSelectedFilters,aoAllEnableSatellites)
    // {
    //     var iNumberOfSatelliteResources  = aoAllEnableSatellites.length;
    //     var iNumberOfSelectedFilters = aoSelectedFilters.length;
    //     var aoReturnValue = [];
    //     for(var iIndexSatellite = 0 ; iIndexSatellite < iNumberOfSatelliteResources ; iIndexSatellite++)
    //     {
    //         var oSatelliteResource = aoAllEnableSatellites[iIndexSatellite];
    //
    //         for(var iSelectedFilter = 0 ; iSelectedFilter < iNumberOfSelectedFilters; iSelectedFilter++)
    //         {
    //             var oSelectedFilter = aoSelectedFilters[iSelectedFilter];
    //             if( oSatelliteResource.satelliteName === oSelectedFilter.text)
    //             {
    //
    //             }
    //         }
    //
    //     }
    // }

    // SearchOrbitController.prototype.getAllSatellitesEnable = function(aoSelectedFilters,aoSatelliteResources)
    // {
    //     var iNumberOfSatelliteResources  = aoSatelliteResources.length;
    //     var iNumberOfSelectedFilters = aoSelectedFilters.length;
    //     var aoReturnValue = [];
    //     for(var iIndexSatellite = 0 ; iIndexSatellite < iNumberOfSatelliteResources ; iIndexSatellite++)
    //     {
    //         var oSatelliteResource = aoSatelliteResources[iIndexSatellite];
    //
    //         for(var iSelectedFilter = 0 ; iSelectedFilter < iNumberOfSelectedFilters; iSelectedFilter++)
    //         {
    //             var oSelectedFilter = aoSelectedFilters[iSelectedFilter];
    //             if( oSatelliteResource.satelliteName === oSelectedFilter.text)
    //             {
    //                 aoReturnValue.push(oSatelliteResource);
    //             }
    //         }
    //
    //     }
    //     return aoReturnValue;
    // };


    SearchOrbitController.prototype.searchNodeInSatelliteResources = function(oNode){
        if(utilsIsObjectNullOrUndefined(oNode))
        {
            return null;
        }
        if(oNode.parent === "results")
        {
            return {
                text : oNode.text,
                enable : true,
                children:[]
            };
        }

        var oTempNode = oNode;
        var oReturnValue = null;
        var oOldNode = [];

        while(oTempNode.text !== "Satellites")
        {
            oReturnValue = {
                text : oTempNode.text,
                enable : true,
                children:[oOldNode],
            };
            oOldNode = oReturnValue;
            oTempNode = this.m_oTreeService.getNodeById(oTempNode.parent,this.m_sIdDivSatelliteResourceTree);
        }


        return oReturnValue;

    }

    SearchOrbitController.prototype.getPolygon = function()
    {
        var sCoordinatesPolygon = "";
        var iLengthCoordinates;
        if(utilsIsObjectNullOrUndefined(this.m_oGeoJSON.geometry) )
        {
            return sCoordinatesPolygon;
        }
        if(utilsIsObjectNullOrUndefined(this.m_oGeoJSON.geometry.coordinates))
        {
            iLengthCoordinates = 0;
        }
        else
        {
            iLengthCoordinates = this.m_oGeoJSON.geometry.coordinates.length;
        }

        for (var iLayerCount = 0; iLayerCount < iLengthCoordinates; iLayerCount++) {

            var oLayer = this.m_oGeoJSON.geometry.coordinates[iLayerCount];
            for (var iCoordCount = 0; iCoordCount < oLayer.length; iCoordCount++) {
                if (oLayer[iCoordCount].length == 2) {
                    var x = oLayer[iCoordCount][0];
                    var y = oLayer[iCoordCount][1];
                    sCoordinatesPolygon += (x + " " + y);

                    if (iCoordCount + 1 < oLayer.length)
                        sCoordinatesPolygon += ',';
                }
            }


        }
        return sCoordinatesPolygon;
    };

    SearchOrbitController.prototype.removeAllLayers = function(){
        var treeInst = $(this.m_sIdDiv).jstree(true);
        var aoNodes = treeInst._model.data;
        this.removeAllLayersInMapByNodes(aoNodes,this.m_sIdDiv)
    }

    SearchOrbitController.prototype.removeAllLayersInMapByNodes = function(aoNodes,sIdDiv)
    {
        if(utilsIsObjectNullOrUndefined(aoNodes) === true)
        {
            return false;
        }

        for (var iIndex in aoNodes)
        {
            if ( (utilsIsObjectNullOrUndefined(aoNodes[iIndex]) === false) )
            {
                // $('orbitMap').jstree("_open_to", aoNodes[iIndex].id);
                var oNode = this.m_oTreeService.getNodeById(aoNodes[iIndex].id,sIdDiv);
                if( utilsIsObjectNullOrUndefined(oNode.original) === false )
                {
                    if(utilsIsObjectNullOrUndefined(oNode.original.rectangle) === false)
                    {
                        // this.m_oMapService.removeRectangle(oNode.original.rectangle);
                        this.m_oMapService.removeLayerFromMap(oNode.original.rectangle);
                    }
                }


            }
        }
        return true;
    };
    SearchOrbitController.$inject = [
        '$scope',
        '$location',
        'ConstantsService',
        'AuthService',
        '$state',
        'ConfigurationService',
        'MapService',
        'SearchOrbitService',
        'ProcessesLaunchedService',
        'WorkspaceService',
        'RabbitStompService',
        'ModalService',
        'ProductService',
        '$filter',
        'TreeService'
    ];

    return SearchOrbitController;
}) ();
