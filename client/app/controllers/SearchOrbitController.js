/**
 * Created by s.adamo on 23/01/2017.
 */
/**
 * Created by p.campanella on 21/10/2016.
 */

var SearchOrbitController = (function() {
    function SearchOrbitController($scope, $location, oConstantsService, oAuthService,oState, oConfigurationService, oMapService, oSearchOrbitService,oProcessesLaunchedService,oWorkspaceService,oRabbitStompService) {
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
        this.m_oSelectedSensorType = null;
        this.m_oSelectedResolutionType = null;
        this.m_oSelectedSatellite = [];
        this.m_aoOrbits = null;
        this.m_oProcessesLaunchedService=oProcessesLaunchedService;
        this.m_oWorkspaceService = oWorkspaceService;
        this.m_oRabbitStompService = oRabbitStompService;

        this.m_oActiveWorkspace = this.m_oConstantsService.getActiveWorkspace();
        this.m_oUser = this.m_oConstantsService.getUser();
        this.m_bIsVisibleLoadingIcon = false;
        //this.m_oProcessesLaunchedService.updateProcessesBar();
        //if there isn't workspace
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
        this.m_oRabbitStompService.initWebStomp(this.m_oActiveWorkspace,"EditorController",this);
        this.initOrbitSearch = function(){
            //init orbit search
            this.m_oOrbitSearch = new Object();
            this.m_oOrbitSearch.acquisitionStartTime = moment();
            this.m_oOrbitSearch.acquisitionEndTime = moment().add(1, 'd');
        }

        var oController = this;

        this.m_oConfigurationService.getConfiguration().then(function (configuration) {
            if(!utilsIsObjectNullOrUndefined(configuration))
            {
                oController.m_oConfiguration = configuration;
                if(!utilsIsObjectNullOrUndefined(oController.m_oConfiguration.orbitsearch) && !utilsIsObjectNullOrUndefined(oController.m_oConfiguration.orbitsearch.satelliteNames))
                {
                    //check as selected all satellites
                    for(var iIndexSatellite = 0; iIndexSatellite < oController.m_oConfiguration.orbitsearch.satelliteNames.length ; iIndexSatellite++ )
                    {
                        var sOrbit = oController.m_oConfiguration.orbitsearch.satelliteNames[iIndexSatellite];
                        oController.m_oSelectedSatellite.push(sOrbit);
                    }

                }
                    oController.m_oScope.$apply();
            }
            else
            {
                utilsVexDialogAlertTop("Error: impossible get configuration.");
            }
        },function error(data, status, header, config) {
            utilsVexDialogAlertTop("Error: impossible get configuration.");
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

    SearchOrbitController.prototype.searchOrbit = function() {
        var oController = this;
        //if there isn't a selected area throw an error
        if(utilsIsObjectNullOrUndefined(oController.m_oGeoJSON))
        {
            utilsVexDialogAlertTop("You should select an area");
            return false;
        }
        //if there isn't a resolution throw an error
        if(utilsIsObjectNullOrUndefined(oController.m_oSelectedResolutionType))
        {
            utilsVexDialogAlertTop("You should select a resolution");
            return false;
        }
        //if there isn't a sensor type throw an error
        if(utilsIsObjectNullOrUndefined(oController.m_oSelectedSensorType))
        {
            utilsVexDialogAlertTop("You should select a sensor type");
            return false;
        }
        //if there isn't a satellite throw an error
        if(utilsIsObjectNullOrUndefined(oController.m_oSelectedSatellite) || oController.m_oSelectedSatellite.length == 0)
        {
            utilsVexDialogAlertTop("You should select a satellite");
            return false;
        }
        var oOrbitSearch = new Object();
        oOrbitSearch.orbitFilters = new Array();
        this.m_oOrbitSearch.orbitFilters = new Array();
        this.m_oOrbitSearch.satelliteNames = new Array();
        var oOrbitFilter = new Object();

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
        //call search
        this.m_oSearchOrbitService.searchOrbit(oController.m_oOrbitSearch)
            .success(function (data, status, headers, config) {
                if(!utilsIsObjectNullOrUndefined(data))
                {
                    oController.m_aoOrbits = data;
                    oController.m_bIsVisibleLoadingIcon = false;
                }
                else
                {
                    utilsVexDialogAlertTop("Error: search orbits fails.");
                }
        })
            .error(function (data, status, header, config) {
                utilsVexDialogAlertTop("Error: search orbits fails.");
                oController.m_aoOrbits = null;
                oController.m_bIsVisibleLoadingIcon = false;
            });

    };

    SearchOrbitController.prototype.getOrbits = function(){
        return this.m_aoOrbits;
    };

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

            sSwath = sSwath.replace("POLYGON","");
            sSwath = sSwath.replace("((","");
            sSwath = sSwath.replace("))","");
            sSwath = sSwath.split(",");

            for (var iIndexBounds = 0; iIndexBounds < sSwath.length; iIndexBounds++)
            {
                var aBounds = sSwath[iIndexBounds];
                console.log(aBounds);
                var aNewBounds = aBounds.split(" ");

                var oLatLonArray = [];

                oLatLonArray[0] = JSON.parse(aNewBounds[1]); //Lat
                oLatLonArray[1] = JSON.parse(aNewBounds[0]); //Lon

                aasNewContent.push(oLatLonArray);
            }
            var oRectangle = this.m_oMapService.addRectangleOnMap(aasNewContent, null, 0);
            if(utilsIsObjectNullOrUndefined(oRectangle))
            {
                utilsVexDialogAlertTop("Impossible visualize orbit");
                return false;
            }
            this.m_aoOrbits[iIndexOrbitInOrbitsList].FootPrintRectangle = oRectangle;
        }
        return true;

    }

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
                    oController.m_oRabbitStompService.initWebStomp(oController.m_oActiveWorkspace,"SearchOrbitController",oController);
                    oController.m_oProcessesLaunchedService.loadProcessesFromServer(oController.m_oActiveWorkspace.workspaceId);

                }
            }
        }).error(function (data,status) {
            //alert('error');
            utilsVexDialogAlertTop('error Impossible get workspace in editorController.js')
        });
    }

    SearchOrbitController.prototype.showAllSwath = function(oCheckValue){
        if(utilsIsObjectNullOrUndefined(this.m_aoOrbits))
            return false;

        var iNumberOfOrbits = this.m_aoOrbits.length;

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
                var sSwath = oOrbit.SwathFootPrint;
                var aasNewContent = [];

                sSwath = sSwath.replace("POLYGON","");
                sSwath = sSwath.replace("((","");
                sSwath = sSwath.replace("))","");
                sSwath = sSwath.split(",");

                for (var iIndexBounds = 0; iIndexBounds < sSwath.length; iIndexBounds++)
                {
                    var aBounds = sSwath[iIndexBounds];
                    console.log(aBounds);
                    var aNewBounds = aBounds.split(" ");

                    var oLatLonArray = [];

                    oLatLonArray[0] = JSON.parse(aNewBounds[1]); //Lat
                    oLatLonArray[1] = JSON.parse(aNewBounds[0]); //Lon

                    aasNewContent.push(oLatLonArray);
                }
                var oRectangle = this.m_oMapService.addRectangleOnMap(aasNewContent, null, 0);
                if(utilsIsObjectNullOrUndefined(oRectangle))
                {
                    utilsVexDialogAlertTop("Impossible visualize orbit");
                    return false;
                }
                this.m_aoOrbits[iIndexOrbit].FootPrintRectangle = oRectangle;
            }

        }
        else
        {
            //FALSE
            /*remove all rectangles in map*/
            for(var iIndexOrbit = 0; iIndexOrbit < iNumberOfOrbits; iIndexOrbit++)
            {
                var oOrbit = this.m_aoOrbits[iIndexOrbit];
                this.m_oMapService.removeLayerFromMap(oOrbit.FootPrintRectangle);//remove orbit rectangle from map
                oOrbit.FootPrintRectangle = null;
            }
        }


    };

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
        'RabbitStompService'
    ];

    return SearchOrbitController;
}) ();
