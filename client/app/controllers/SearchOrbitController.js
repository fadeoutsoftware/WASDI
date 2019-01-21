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
                                   oRabbitStompService,oModalService, oProductService,$filter) {
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
        //order the table
        this.m_sOrderBy = "Satellite";
        this.m_bReverseOrder = false;

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

    SearchOrbitController.prototype.getDateFromTimestamp = function(timestamp) {
      return moment.unix(timestamp / 1000).format("YYYY-MM-DD HH:mm:ss");
    };

    SearchOrbitController.prototype.clearMapAndRemoveOrbits = function(){
        if(utilsIsObjectNullOrUndefined(this.m_aoOrbits) === true )
            return false;
        var iOrbitsLength = this.m_aoOrbits.length;
        var oRectangle = null;
        for(var iOrbitIndex = 0; iOrbitIndex < iOrbitsLength; iOrbitIndex++){
            oRectangle = this.m_aoOrbits[iOrbitIndex ].FootPrintRectangle
            this.m_oMapService.removeLayerFromMap(oRectangle);
        }
        this.m_aoOrbits = [];
        return true;
    };

    SearchOrbitController.prototype.searchOrbit = function()
    {

        var sError = "";

        if(utilsIsObjectNullOrUndefined(this.m_oGeoJSON)) sError += "PLEASE SELECT AREA OF INTEREST<br>TOP RIGHT CORNER OF THE MAP<br>";

        //if there isn't a resolution throw an error
        if(utilsIsObjectNullOrUndefined(this.m_oSelectedResolutionType) || this.m_oSelectedResolutionType.length == 0) sError += "PLEASE SELECT AT LEAST A RESOLUTION<br>";
        //if there isn't a sensor type throw an error
        if(utilsIsObjectNullOrUndefined(this.m_oSelectedSensorType) || this.m_oSelectedSensorType.length == 0) sError += "PLEASE SELECT AT LEAST A SENSOR TYPE<br>";
        //if there isn't a satellite throw an error
        if(utilsIsObjectNullOrUndefined(this.m_oSelectedSatellite) || this.m_oSelectedSatellite.length == 0) sError += "PLEASE SELECT AT LEAST A MISSION<br>";

        if (!utilsIsStrNullOrEmpty(sError)) {
            utilsVexDialogAlertDefault(sError,null);
            return;
        }

        var oController = this;

        //clear map and remove orbits and set check as false
        this.m_bMasterCheck = false;
        this.clearMapAndRemoveOrbits();

        var oOrbitSearch = new Object();
        oOrbitSearch.orbitFilters = new Array();
        this.m_oOrbitSearch.orbitFilters = new Array();
        this.m_oOrbitSearch.satelliteNames = new Array();
       // var oOrbitFilter = new Object();

        if(utilsIsObjectNullOrUndefined(this.m_oSelectedSensorType))
            var iLengthSelectedSensorType = 0;
        else
            var iLengthSelectedSensorType = this.m_oSelectedSensorType.length;

        if(utilsIsObjectNullOrUndefined(this.m_oSelectedResolutionType))
            var iLengthSelectedResolutionType  = 0;
        else
            var iLengthSelectedResolutionType  = this.m_oSelectedResolutionType.length;

            //add sensor type and resolution
        for (var iSensorType = 0; iSensorType < iLengthSelectedSensorType; iSensorType++) {

            for (var iResolutionType = 0; iResolutionType < iLengthSelectedResolutionType; iResolutionType++) {
                var oOrbitFilter = new Object();
                oOrbitFilter.sensorType = this.m_oSelectedSensorType[iSensorType];
                oOrbitFilter.sensorResolution = this.m_oSelectedResolutionType[iResolutionType];
                oController.m_oOrbitSearch.orbitFilters.push(oOrbitFilter);

            }

        }

        if(utilsIsObjectNullOrUndefined(this.m_oSelectedSatellite))
            var iLengthSelectedSatellite = 0;
        else
            var iLengthSelectedSatellite =this.m_oSelectedSatellite.length;

        //satellite names
        for (var iSatellite = 0; iSatellite < iLengthSelectedSatellite; iSatellite++) {
            oController.m_oOrbitSearch.satelliteNames.push(this.m_oSelectedSatellite[iSatellite]);
        }

        //

        //add polygon area
        var sCoordinatesPolygon = "";
        if(utilsIsObjectNullOrUndefined(oController.m_oGeoJSON.geometry.coordinates))
            var iLengthCoordinates = 0;
        else
            var iLengthCoordinates = oController.m_oGeoJSON.geometry.coordinates.length;

        for (var iLayerCount = 0; iLayerCount < iLengthCoordinates; iLayerCount++) {

            var oLayer = oController.m_oGeoJSON.geometry.coordinates[iLayerCount];
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

        oController.m_oOrbitSearch.polygon = sCoordinatesPolygon;
        this.m_bIsVisibleLoadingIcon = true;
        this.m_aoOrbits = null;
        oController.m_oOrbitSearch.acquisitionStartTime += ":00:00";
        oController.m_oOrbitSearch.acquisitionEndTime += ":00:00";
        oController.m_oOrbitSearch.viewAngle = oController.convertViewAngleToString();
        oController.m_oOrbitSearch.swathSize = oController.convertSwathSizeToString();
        //call search
        this.m_oSearchOrbitService.searchOrbit(oController.m_oOrbitSearch)
            .success(function (data, status, headers, config) {
                if(!utilsIsObjectNullOrUndefined(data))
                {
                    oController.m_aoOrbits = data;
                    oController.setOrbitAsUnchecked();
                    if(data.length === 0)
                    {
                        utilsVexDialogAlertTop("GURU MEDITATION<br>NO RESULTS FOR YOUR FILTERS");
                    }
                }
                else
                {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR: SEARCH ORBITS FAILS.");
                }
                oController.initOrbitSearch();
                oController.m_bIsVisibleLoadingIcon = false;

            })
            .error(function (data, status, header, config) {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR: SEARCH ORBITS FAILS.");
                oController.initOrbitSearch();
                oController.m_aoOrbits = null;
                oController.m_bIsVisibleLoadingIcon = false;
            });

    };


    SearchOrbitController.prototype.convertViewAngleToString = function()
    {
        if( (utilsIsObjectNullOrUndefined(this.m_oOrbitSearch) === true)
            || (utilsIsObjectNullOrUndefined(this.m_oOrbitSearch.viewAngle) === true)
            || (utilsIsObjectNullOrUndefined(this.m_oOrbitSearch.viewAngle.nearAngle) === true)
            || (utilsIsObjectNullOrUndefined(this.m_oOrbitSearch.viewAngle.farAngle) === true) )
        {
            return "";
        }
        return "(nearAngle:" + this.m_oOrbitSearch.viewAngle.nearAngle + ",farAngle:" + this.m_oOrbitSearch.viewAngle.farAngle +")";
    };

    SearchOrbitController.prototype.convertSwathSizeToString = function()
    {
        if( (utilsIsObjectNullOrUndefined(this.m_oOrbitSearch) === true)
            || (utilsIsObjectNullOrUndefined(this.m_oOrbitSearch.swathSize) === true)
            || (utilsIsObjectNullOrUndefined(this.m_oOrbitSearch.swathSize.length) === true)
            || (utilsIsObjectNullOrUndefined(this.m_oOrbitSearch.swathSize.width) === true) )
        {
            return "";
        }
        return "(length:" + this.m_oOrbitSearch.swathSize.length + ",width:" + this.m_oOrbitSearch.swathSize.width +")";
    };

    /**
     *
     * @returns {boolean}
     */
    SearchOrbitController.prototype.setOrbitAsUnchecked = function(){
        if( utilsIsObjectNullOrUndefined(this.m_aoOrbits) === true )
        {
            return false;
        }
        var iNumberOfOrbits = this.m_aoOrbits.length;
        for(var iOrbitIndex = 0; iOrbitIndex < iNumberOfOrbits; iOrbitIndex++)
        {
            this.m_aoOrbits[iOrbitIndex].isSelected = false;
        }
        return true;
    };
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
    SearchOrbitController.prototype.showSwath = function(oOrbit){


        if (utilsIsObjectNullOrUndefined(oOrbit))
            return false;

        if (!oOrbit.hasOwnProperty('SwathFootPrint'))
            return false;

        /* find orbit in orbit list*/
        if(utilsIsObjectNullOrUndefined(this.m_aoOrbits))
        {
            return false;
        }

        //Find orbit in Orbits list return index
        var iIndexOrbitInOrbitsList = utilsFindObjectInArray(this.m_aoOrbits,oOrbit)
        if(iIndexOrbitInOrbitsList == -1)
            return false;

        //if there is FootPrintRectangle set it null, otherwise create FootPrintRectangle
        // it correspond ----> FootPrintRectangle == null == uncheck ||  FootPrintRectangle != null == check
        if(!utilsIsObjectNullOrUndefined(this.m_aoOrbits[iIndexOrbitInOrbitsList].FootPrintRectangle))
        {
            this.m_oMapService.removeLayerFromMap(this.m_aoOrbits[iIndexOrbitInOrbitsList].FootPrintRectangle);//remove orbit rectangle from map
            this.m_aoOrbits[iIndexOrbitInOrbitsList].FootPrintRectangle = null;
        }
        else
        {
            //create oRectangle
            var sSwath = oOrbit.SwathFootPrint;
            var aasNewContent = [];
            //
            // sSwath = sSwath.replace("POLYGON","");
            // sSwath = sSwath.replace("((","");
            // sSwath = sSwath.replace("))","");
            // sSwath = sSwath.split(",");
            sSwath = utilsProjectGetPolygonArray(sSwath);

            for (var iIndexBounds = 0; iIndexBounds < sSwath.length; iIndexBounds++)
            {
                var aBounds = sSwath[iIndexBounds];
                // console.log(aBounds);
                var aNewBounds = aBounds.split(" ");

                var oLatLonArray = [];

                oLatLonArray[0] = JSON.parse(aNewBounds[1]); //Lat
                oLatLonArray[1] = JSON.parse(aNewBounds[0]); //Lon

                aasNewContent.push(oLatLonArray);
            }
            var oRectangle = this.m_oMapService.addRectangleByBoundsArrayOnMap(aasNewContent, null, 0);
            if(utilsIsObjectNullOrUndefined(oRectangle))
            {
                utilsVexDialogAlertTop("IMPOSSIBLE TO VISUALIZE ORBIT");
                return false;
            }
            this.m_aoOrbits[iIndexOrbitInOrbitsList].FootPrintRectangle = oRectangle;
        }
        return true;

    };

    /**
     *
     * @param oOrbit
     * @returns {boolean}
     */
    SearchOrbitController.prototype.showFrame = function(oOrbit){


        if (utilsIsObjectNullOrUndefined(oOrbit))
            return false;

        if (!oOrbit.hasOwnProperty('FrameFootPrint'))
            return false;

        /* find orbit in orbit list*/
        if(utilsIsObjectNullOrUndefined(this.m_aoOrbits))
        {
            return false;
        }

        //Find orbit in Orbits list return index
        var iIndexOrbitInOrbitsList = utilsFindObjectInArray(this.m_aoOrbits,oOrbit)
        if(iIndexOrbitInOrbitsList == -1)
            return false;

        //if there is FootPrintRectangle set it null, otherwise create FootPrintRectangle
        // it correspond ----> FootPrintRectangle == null == uncheck ||  FootPrintRectangle != null == check
        if(!utilsIsObjectNullOrUndefined(this.m_aoOrbits[iIndexOrbitInOrbitsList].FootPrintRectangle))
        {
            this.m_oMapService.removeLayerFromMap(this.m_aoOrbits[iIndexOrbitInOrbitsList].FootPrintRectangle);//remove orbit rectangle from map
            this.m_aoOrbits[iIndexOrbitInOrbitsList].FootPrintRectangle = null;
        }
        else
        {
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
            var oRectangle = this.m_oMapService.addRectangleByBoundsArrayOnMap(aasNewContent, null, 0);
            if(utilsIsObjectNullOrUndefined(oRectangle))
            {
                utilsVexDialogAlertTop("IMPOSSIBLE TO VISUALIZE ORBIT");
                return false;
            }
            this.m_aoOrbits[iIndexOrbitInOrbitsList].FootPrintRectangle = oRectangle;
        }
        return true;

    };
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

    SearchOrbitController.prototype.showAllFrameFootPrint = function(oCheckValue)
    {
        if(utilsIsObjectNullOrUndefined(this.m_aoOrbits))
        {
            return false;
        }

        var aoFilteredOrbits = this.m_oFilter('filter')(this.m_aoOrbits,this.m_sFilterTable);

        var iNumberOfOrbits = aoFilteredOrbits.length;
        this.m_bisLoadingAllOrbitInMap = true;
        if(oCheckValue)
        {
            //TRUE
            /*add all rectangles in map*/
            for(var iIndexOrbit = 0; iIndexOrbit < iNumberOfOrbits; iIndexOrbit++)
            {
                var oOrbit = aoFilteredOrbits[iIndexOrbit];
                //if there is saved in orbit the FootPrintRectangle we don't need to create it
                if(!utilsIsObjectNullOrUndefined(oOrbit.FootPrintRectangle))
                {
                    continue;
                }
                this.showFrame(oOrbit);
            }
        }
        else
        {
            //FALSE
            // /*remove all rectangles in map*/
            // for(var iIndexOrbit = 0; iIndexOrbit < iNumberOfOrbits; iIndexOrbit++)
            // {
            //     var oOrbit = this.m_aoOrbits[iIndexOrbit];
            //     this.m_oMapService.removeLayerFromMap(oOrbit.FootPrintRectangle);//remove orbit rectangle from map
            //     oOrbit.FootPrintRectangle = null;
            // }
            this.removeAllRectangle(aoFilteredOrbits);

        }
        this.m_bisLoadingAllOrbitInMap = false;
        this.selectDeselectAllOrbits(this.m_bMasterCheck,aoFilteredOrbits);
    };

    // TODO REMOVE IT?
    SearchOrbitController.prototype.showAllSwath = function(oCheckValue){
        if(utilsIsObjectNullOrUndefined(this.m_aoOrbits))
            return false;
        //TODO ADD THIS  -->  var aoFilteredOrbits = this.m_oFilter('filter')(this.m_aoOrbits,this.m_sFilterTable);
        //TODO CHANGE m_aoOrbits with  aoFilteredOrbits
        var iNumberOfOrbits = this.m_aoOrbits.length;
        this.m_bisLoadingAllOrbitInMap = true;
        if(oCheckValue)
        {
            //TRUE
            /*add all rectangles in map*/
            for(var iIndexOrbit = 0; iIndexOrbit < iNumberOfOrbits; iIndexOrbit++)
            {
                var oOrbit = this.m_aoOrbits[iIndexOrbit];
                //if there is saved in orbit the FootPrintRectangle we don't need to create it
                if(!utilsIsObjectNullOrUndefined(oOrbit.FootPrintRectangle))
                {
                   continue;
                }
                //TODO SUBSTITUTE THIS PART OF CODE WITH showSwath FUNCTION
                var sSwath = oOrbit.SwathFootPrint;
                var aasNewContent = [];

                sSwath = sSwath.replace("POLYGON","");
                sSwath = sSwath.replace("((","");
                sSwath = sSwath.replace("))","");
                sSwath = sSwath.split(",");

                for (var iIndexBounds = 0; iIndexBounds < sSwath.length; iIndexBounds++)
                {
                    var aBounds = sSwath[iIndexBounds];
                    //console.log(aBounds);
                    var aNewBounds = aBounds.split(" ");

                    var oLatLonArray = [];

                    oLatLonArray[0] = JSON.parse(aNewBounds[1]); //Lat
                    oLatLonArray[1] = JSON.parse(aNewBounds[0]); //Lon

                    aasNewContent.push(oLatLonArray);
                }
                var oRectangle = this.m_oMapService.addRectangleByBoundsArrayOnMap(aasNewContent, null, 0);
                if(utilsIsObjectNullOrUndefined(oRectangle))
                {
                    utilsVexDialogAlertTop("IMPOSSIBLE VISUALIZE ORBIT");
                    return false;
                }
                this.m_aoOrbits[iIndexOrbit].FootPrintRectangle = oRectangle;
            }

        }
        else
        {
            //FALSE
            /*remove all rectangles in map*/
            //TODO CHANGE this.m_aoOrbits with aoFiltered orbits
            this.removeAllRectangle(this.m_aoOrbits);
        }
        this.m_bisLoadingAllOrbitInMap = false;
        //TODO CHANGE this.m_aoOrbits with aoFiltered orbits
        this.selectDeselectAllOrbits(this.m_bMasterCheck,this.m_aoOrbits);

        // this.m_oScope.$apply();
    };

    SearchOrbitController.prototype.removeAllRectangle = function(aoOrbits)
    {
        if(utilsIsObjectNullOrUndefined(aoOrbits) === true)
        {
            return false;
        }
        var iNumberOfOrbits = aoOrbits.length;

        for(var iIndexOrbit = 0; iIndexOrbit < iNumberOfOrbits; iIndexOrbit++)
        {
            var oOrbit = aoOrbits[iIndexOrbit];

            if(utilsIsObjectNullOrUndefined(oOrbit.FootPrintRectangle) === false)
            {
                this.m_oMapService.removeLayerFromMap(oOrbit.FootPrintRectangle);//remove orbit rectangle from map
                oOrbit.FootPrintRectangle = null;
            }
        }
        return true;
    }
    /**
     * selectDeselectAllOrbits
     * @returns {boolean}
     */
    SearchOrbitController.prototype.selectDeselectAllOrbits = function(bValue,aoOrbits)
    {
        if(utilsIsObjectNullOrUndefined(aoOrbits) === true)
        {
            return false;
        }
        var iNumberOfOrbits = aoOrbits.length;

        for(var iIndexOrbit = 0 ; iIndexOrbit < iNumberOfOrbits; iIndexOrbit++ )
        {
            aoOrbits[iIndexOrbit].isSelected = bValue;
        }
        return true;
    };

    /**
     * mouseOverOrbitInTable
     * @param oOrbit
     * @returns {boolean}
     */
    SearchOrbitController.prototype.mouseOverOrbitInTable = function(oOrbit)
    {
        if(utilsIsObjectNullOrUndefined(oOrbit))
            return false;
        if(utilsIsObjectNullOrUndefined(oOrbit.FootPrintRectangle))
            return false;
        var oRectangle = oOrbit.FootPrintRectangle;
        oRectangle.setStyle({weight:5,fillOpacity:1,color:"Black"});
        return true;
    }
    SearchOrbitController.prototype.mouseLeaveOrbitInTable = function(oOrbit)
    {
        if(utilsIsObjectNullOrUndefined(oOrbit))
            return false;
        if(utilsIsObjectNullOrUndefined(oOrbit.FootPrintRectangle))
            return false;
        var oRectangle = oOrbit.FootPrintRectangle;
        oRectangle.setStyle({weight:1,fillOpacity:0.2,color:"#ff7800"});
        return true
    }


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

    SearchOrbitController.prototype.checkedSatellite = function(sSatellite)
    {
        if(utilsIsStrNullOrEmpty(sSatellite) === false)
        {
            this.m_oSelectedSatellite.push(sSatellite);
        }
    };
    SearchOrbitController.prototype.uncheckedSatellite = function(sSatellite)
    {
        if(utilsIsStrNullOrEmpty(sSatellite) === false)
        {
            // var iNumberOfSelectedSatellite = this.m_oSelectedSatellite.length;
            utilsRemoveObjectInArray(this.m_oSelectedSatellite,sSatellite);
        }
    };
    /**
     *
     * @param sSatellite
     * @returns {*}
     */
    SearchOrbitController.prototype.isSatelliteSelected = function(sSatellite)
    {
        if(utilsIsStrNullOrEmpty(sSatellite) === true)
        {
            return false;
        }
        return utilsIsElementInArray(this.m_oSelectedSatellite,sSatellite);
    }

    SearchOrbitController.prototype.clearOrbitsTable = function(){
        if(utilsIsObjectNullOrUndefined(this.m_aoOrbits) === true)
            return false;
        var iNumberOfOrbits = this.m_aoOrbits.length;

        for(var iIndexOrbit = 0; iIndexOrbit < iNumberOfOrbits; iIndexOrbit++)
        {
            var oOrbit = this.m_aoOrbits[iIndexOrbit];
            this.m_oMapService.removeLayerFromMap(oOrbit.FootPrintRectangle);//remove orbit rectangle from map
            oOrbit.FootPrintRectangle = null;
        }
        this.m_aoOrbits = [];
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
        '$filter'
    ];

    return SearchOrbitController;
}) ();
