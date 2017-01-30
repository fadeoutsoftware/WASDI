/**
 * Created by s.adamo on 23/01/2017.
 */
/**
 * Created by p.campanella on 21/10/2016.
 */

var SearchOrbitController = (function() {
    function SearchOrbitController($scope, $location, oConstantsService, oAuthService,oState, oConfigurationService, oMapService, oSearchOrbitService) {
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
        this.m_oSelectedSatellite = null;
        this.m_aoOrbits = null;

        this.initOrbitSearch = function(){
            //init orbit search
            this.m_oOrbitSearch = new Object();
            this.m_oOrbitSearch.acquisitionStartTime = moment();
            this.m_oOrbitSearch.acquisitionEndTime = moment().add(1, 'd');
        }

        var oController = this;

        this.m_oConfigurationService.getConfiguration().then(function (configuration) {
            oController.m_oConfiguration = configuration;
            oController.m_oScope.$apply();
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

        if(utilsIsObjectNullOrUndefined(oController.m_oGeoJSON))
        {
            utilsVexDialogAlertTop("You should select an area");
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

        //call search
        this.m_oSearchOrbitService.searchOrbit(oController.m_oOrbitSearch)
            .success(function (data, status, headers, config) {
                oController.m_aoOrbits = data;

        })
            .error(function (data, status, header, config) {
                oController.m_aoOrbits = null;
            });

    };

    SearchOrbitController.prototype.getOrbits = function(){
        return this.m_aoOrbits;
    };

    SearchOrbitController.prototype.showSwath = function(oOrbit){
        if (utilsIsObjectNullOrUndefined(oOrbit))
            return;

        if (!oOrbit.hasOwnProperty('SwathFootPrint'))
            return;

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

        this.m_oMapService.addRectangleOnMap(aasNewContent, null, 0);
    };





    SearchOrbitController.$inject = [
        '$scope',
        '$location',
        'ConstantsService',
        'AuthService',
        '$state',
        'ConfigurationService',
        'MapService',
        'SearchOrbitService'
    ];

    return SearchOrbitController;
}) ();
