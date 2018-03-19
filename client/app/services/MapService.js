/**
 * Created by p.campanella on 21/10/2016.
 */

'use strict';
angular.module('wasdi.MapService', ['wasdi.ConstantsService']).
service('MapService', ['$http','$rootScope', 'ConstantsService', function ($http,$rootScope, oConstantsService) {
    // API URL
    this.APIURL = oConstantsService.getAPIURL();

    // Service references
    this.m_oHttp = $http;
    this.m_oConstantsService = oConstantsService;
    this.m_oRectangleOpenSearch = null;
    this.m_oDrawItems = null;


    /**
     * Init base layers
     */
    this.initTileLayer= function(){
        this.m_oOSMBasic = L.tileLayer('http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution:
                '&copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors',
            maxZoom: 18,
            // this map option disables world wrapping. by default, it is false.
            continuousWorld: false,
            // this option disables loading tiles outside of the world bounds.
            noWrap: true
        });
        this.m_oOSMMapquest = L.tileLayer('http://otile{s}.mqcdn.com/tiles/1.0.0/osm/{z}/{x}/{y}.png', {
            subdomains: "12",
            attribution:
                '&copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors. Tiles courtesy of <a href="http://www.mapquest.com/" target="_blank">MapQuest</a> <img src="https://developer.mapquest.com/content/osm/mq_logo.png">',
            maxZoom: 18,
            // this map option disables world wrapping. by default, it is false.
            continuousWorld: false,
            // this option disables loading tiles outside of the world bounds.
            noWrap: true
        });
        this.m_oOSMHumanitarian = L.tileLayer('http://{s}.tile.openstreetmap.fr/hot/{z}/{x}/{y}.png', {
            attribution:
                '&copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors. Tiles courtesy of <a href="http://hot.openstreetmap.org/" target="_blank">Humanitarian OpenStreetMap Team</a>',
            maxZoom: 18,
            // this map option disables world wrapping. by default, it is false.
            continuousWorld: false,
            // this option disables loading tiles outside of the world bounds.
            noWrap: true
        });
        this.m_oOCMCycle = L.tileLayer('http://{s}.tile.opencyclemap.org/cycle/{z}/{x}/{y}.png', {
            attribution:
                '&copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors. Tiles courtesy of <a href="http://www.thunderforest.com/" target="_blank">Andy Allan</a>',
            maxZoom: 18,
            // this map option disables world wrapping. by default, it is false.
            continuousWorld: false,
            // this option disables loading tiles outside of the world bounds.
            noWrap: true
        });
        this.m_oOCMTransport = L.tileLayer('http://{s}.tile2.opencyclemap.org/transport/{z}/{x}/{y}.png', {
            attribution:
                '&copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors. Tiles courtesy of <a href="http://www.thunderforest.com/" target="_blank">Andy Allan</a>',
            maxZoom: 18,
            // this map option disables world wrapping. by default, it is false.
            continuousWorld: false,
            // this option disables loading tiles outside of the world bounds.
            noWrap: true
        });

        this.m_oGoogleSatelite = L.tileLayer('http://{s}.google.com/vt/lyrs=s&x={x}&y={y}&z={z}',{
            maxZoom: 18,
            subdomains:['mt0','mt1','mt2','mt3'],
            // this map option disables world wrapping. by default, it is false.
            continuousWorld: false,
            // this option disables loading tiles outside of the world bounds.
            noWrap: true
        });
    };

    //init tile layer
    this.initTileLayer();

    this.m_oGoogleHybrid = new L.gridLayer.googleMutant('hybrid');
    this.m_oGoogleMap = new L.gridLayer.googleMutant('roadmap');
    this.m_oGoogleTerrain = new L.gridLayer.googleMutant('terrain');

    /**
     * layers control
     */
    this.m_oLayersControl = L.control.layers(
        {
            "Standard": this.m_oOSMBasic,
            "Cycle Map": this.m_oOCMCycle,
            "Transport Map": this.m_oOCMTransport,
            "Humanitarian": this.m_oOSMHumanitarian,
            "Google Hybrid": this.m_oGoogleHybrid,
            "Google Map": this.m_oGoogleMap,
            "Google Satellite": this.m_oGoogleSatelite,
            "Google Terrain": this.m_oGoogleTerrain
        },
        {},
        {
            'position' : 'bottomright'
        }
    );

    /**
     * Map Object
     * @type {null}
     */
    this.m_oWasdiMap = null;

    /**
     * Actual base Layer
     */
    this.m_oActiveBaseLayer = this.m_oOSMBasic;

    /**
     * Init the Map
     * @param sMapDiv
     */
    this.initMap = function(sMapDiv) {

        //the map
        if(this.m_oWasdiMap != null)
        {
            this.initTileLayer();
        }

        /*  it need disabled keyboard, there'is a bug :
        *   https://github.com/Leaflet/Leaflet/issues/1228
        *   thw window scroll vertically when i click (only if the browser window are smaller)
        *   alternative solution (hack):
        *   L.Map.addInitHook(function() {
        *   return L.DomEvent.off(this._container, "mousedown", this.keyboard._onMouseDown);
        *   });
        */
        this.m_oWasdiMap = L.map(sMapDiv, {
            zoomControl: false,
            layers: [this.m_oOSMBasic],
            keyboard: false
             //maxZoom: 22
        });

        // coordinates in map find this plugin in lib folder
        L.control.mousePosition().addTo(this.m_oWasdiMap);

        //scale control
        L.control.scale({
            position: "bottomright",
            imperial: false
        }).addTo(this.m_oWasdiMap);

        //layers control
        this.m_oLayersControl.addTo(this.m_oWasdiMap);

        // center map
        var southWest = L.latLng(0, 0),
            northEast = L.latLng(0, 0),
            oBoundaries = L.latLngBounds(southWest, northEast);

        this.m_oWasdiMap.fitBounds(oBoundaries);
        this.m_oWasdiMap.setZoom(3);

        var oActiveBaseLayer = this.m_oActiveBaseLayer;

        //add event on base change
        this.m_oWasdiMap.on('baselayerchange', function(e){
            // console.log(e);
            e.layer.bringToBack();
            oActiveBaseLayer = e;
        });

    };

    /**
     * Get the Map object
     * @returns {null|*}
     */
    this.getMap = function () {
        return this.m_oWasdiMap;
    };


    /**
     *
     * @param sMapDiv
     */
    this.initMapWithDrawSearch = function(sMapDiv)
    {
        var oController=this;

        //Init standard map
        this.initMap(sMapDiv);

        //LEAFLET.DRAW LIB
        //add draw.search (opensearch)
        var drawnItems = new L.FeatureGroup();
        this.m_oDrawItems = drawnItems;//save draw items (used in delete shape)
        this.m_oWasdiMap.addLayer(drawnItems);

        var oOptions={
            position:'topright',//position of menu
            draw:{// what kind of shape is disable/enable
                marker:false,
                polyline:false,
                circle:false,
                polygon:false
            },

            edit: {
                featureGroup: drawnItems,//draw items are the "voice" of menu
            }
        };

        var oDrawControl = new L.Control.Draw(oOptions);

        this.m_oWasdiMap.addControl(oDrawControl);

        //Without this.m_oWasdiMap.on() the shape isn't saved on map
        this.m_oWasdiMap.on(L.Draw.Event.CREATED, function (event)
        {
            var layer = event.layer;
            oController.m_oRectangleOpenSearch = layer;

            //remove old shape
            if(drawnItems && drawnItems.getLayers().length!==0){
                drawnItems.clearLayers();
            }
            $rootScope.$broadcast('rectangle-created-for-opensearch',{layer:layer});//SEND MESSAGE TO IMPORT CONTROLLER
            //save new shape in map
            drawnItems.addLayer(layer);
        });

        //TODO event EDITED
        //this.m_oWasdiMap.on(L.Draw.Event.EDITED, function (event) {
        //    var layer = event.layers;
        //});

    };

    /**
     * Init map editor
     * @param sMapDiv
     * @returns {boolean}
     */
    this.initMapEditor = function(sMapDiv)
    {
        if(utilsIsObjectNullOrUndefined(sMapDiv)) return false;
        this.initMap(sMapDiv);

        return true;
    };

    /**
     * Init geo search plugin
     */
    this.initGeoSearchPluginForOpenStreetMap = function()
    {
        var geocoder = L.Control.Geocoder.mapzen('search-DopSHJw');
        var control = L.Control.geocoder({
            geocoder: geocoder,
            position:'topleft'
        }).addTo(this.m_oWasdiMap);
    };

    /**
     * Clear Map
     */
    this.clearMap = function () {
        if (this.m_oWasdiMap) {
            this.m_oWasdiMap.remove();
            this.m_oWasdiMap = null;
        }
    };

    /**
     *
     */
    this.deleteDrawShapeEditToolbar = function()
    {
        this.m_oDrawItems.clearLayers();
    };

    /**
     *
     * @param oRectangle
     * @returns {boolean}
     */
    this.changeStyleRectangleMouseOver=function(oRectangle)
    {
        if(utilsIsObjectNullOrUndefined(oRectangle))
        {
            //console.log("Error: rectangle is undefined ");
            return false;
        }
        oRectangle.setStyle({weight:3,fillOpacity:0.7});
    };

    /**
     * Change style of rectangle when the mouse is leave the layer (TABLE CASE)
     * @param oRectangle
     * @returns {boolean}
     */
    this.changeStyleRectangleMouseLeave=function(oRectangle)
    {
        if(utilsIsObjectNullOrUndefined(oRectangle)) return false;
        oRectangle.setStyle({weight:1,fillOpacity:0.2});
    };


    /******************************************************LAYER HANDLERS***********************************************/

    /**
     * Set basic map
     * @returns {boolean}
     */
    this.setBasicMap = function()
    {
        if(utilsIsObjectNullOrUndefined(this.m_oOSMBasic)) return false;
        this.m_oWasdiMap.addLayer(this.m_oOSMBasic,true);
        return true;
    };

    /**
     * Remove basic map
     * @returns {boolean}
     */
    this.removeBasicMap = function()
    {
        if(utilsIsObjectNullOrUndefined(this.m_oOSMBasic)) return false;
        this.removeLayerFromMap(this.m_oOSMBasic);
        return true;
    };


    /**
     * Remove a layer from the map
     * @param oLayer
     * @returns {boolean}
     */
    this.removeLayerFromMap = function(oLayer)
    {
        if(utilsIsObjectNullOrUndefined(oLayer))
            return false;
        oLayer.remove();
        return true;
    };

    /**
     * Remove all layers from the map
     */
    this.removeLayersFromMap = function()
    {
        var oController = this;
        oController.m_oWasdiMap.eachLayer(function (layer) {
            oController.m_oWasdiMap.removeLayer(layer);
        });
    };


    /**
     * Convert boundaries
     * @param sBoundaries
     * @returns {Array}
     */
    this.convertBboxInBoundariesArray = function(sBbox)
    {
        var asBoundaries = sBbox.split(",");
        var iNumberOfBoundaries = asBoundaries.length;
        var aoReturnValues = [];
        var iIndexReturnValues = 0;
        for(var iBoundaryIndex = 0 ; iBoundaryIndex < iNumberOfBoundaries; iBoundaryIndex++)
        {
            if(utilsIsOdd(iBoundaryIndex) === false)
            {
                aoReturnValues[iIndexReturnValues] = [asBoundaries[iBoundaryIndex],asBoundaries[iBoundaryIndex+1]];
                iIndexReturnValues++;
            }
        }
        return aoReturnValues;
    };

    /**
     * Add to the 2D Map all the bounding box rectangles of a workspace
     * @param aoProducts
     */
    this.addAllWorkspaceRectanglesOnMap = function (aoProducts, sColor) {
        if (utilsIsObjectNullOrUndefined(aoProducts)) return;
        if(utilsIsStrNullOrEmpty(sColor)) sColor="#ff7800";

        try {

            for (var iProduct = 0; iProduct<aoProducts.length; iProduct++) {
                this.addRectangleOnMap(aoProducts[iProduct].bbox, sColor, aoProducts[iProduct].fileName);
            }
        }
        catch (e) {
            console.log(e);
        }

    };

    /**
     * Add a rectangle shape on the map
     * @param aaBounds
     * @param sColor
     * @param iIndexLayers
     * @returns {null}
     */
    this.addRectangleByBoundsArrayOnMap = function (aaBounds,sColor,iIndexLayers)
    {
        if(utilsIsObjectNullOrUndefined(aaBounds)) return null;

        for(var iIndex = 0; iIndex < aaBounds.length; iIndex++ )
        {
            if(utilsIsObjectNullOrUndefined(aaBounds[iIndex])) return null;
        }

        //default color
        if(utilsIsStrNullOrEmpty(sColor)) sColor="#ff7800";

        // create an colored rectangle
        // weight = line thickness
        var oRectangle = L.polygon(aaBounds, {color: sColor, weight: 1}).addTo(this.m_oWasdiMap);

        if(!utilsIsObjectNullOrUndefined(iIndexLayers))//event on click
            oRectangle.on("click",function (event) {
                $rootScope.$broadcast('on-mouse-click-rectangle',{rectangle:oRectangle});//SEND MESSAGE TO IMPORTCONTROLLER
            });
        //mouse over event change rectangle style
        oRectangle.on("mouseover", function (event) {//SEND MESSAGE TO IMPORT CONTROLLER
            oRectangle.setStyle({weight:3,fillOpacity:0.7});
            $rootScope.$broadcast('on-mouse-over-rectangle',{rectangle:oRectangle});// TODO SEND MASSAGE FOR CHANGE CSS in LAYER LIST TABLE
            var temp = oRectangle.getBounds()


        });
        //mouse out event set default value of style
        oRectangle.on("mouseout", function (event) {//SEND MESSAGE TO IMPORT CONTROLLER
            oRectangle.setStyle({weight:1,fillOpacity:0.2});
            $rootScope.$broadcast('on-mouse-leave-rectangle',{rectangle:oRectangle});// TODO SEND MASSAGE FOR CHANGE CSS in LAYER LIST TABLE
        });

        return oRectangle;
    };

        /**
     * Add a rectangle shape on the map
     * @param aaBounds
     * @param sColor
     * @param sReferenceName
     * @returns {null}
     */
    this.addRectangleOnMap = function (sBbox,sColor,sReferenceName)
    {
        if(utilsIsObjectNullOrUndefined(sBbox)) return null;

        var aoBounds = this.convertBboxInBoundariesArray(sBbox);

        for(var iIndex = 0; iIndex < aoBounds.length; iIndex++ )
        {
            if(utilsIsObjectNullOrUndefined(aoBounds[iIndex])) return null;
        }

        //default color
        if(utilsIsStrNullOrEmpty(sColor)) sColor="#ff7800";

        // create an colored rectangle
        // weight = line thickness
        var oRectangle = L.polygon(aoBounds, {color: sColor, weight: 1}).addTo(this.m_oWasdiMap);

        //event on click
        if(!utilsIsObjectNullOrUndefined(sReferenceName)) {
            oRectangle.on("click",function (event) {
                $rootScope.$broadcast('on-mouse-click-rectangle',{rectangle:oRectangle});
            });
        }

        //mouse over event change rectangle style
        oRectangle.on("mouseover", function (event) {
            oRectangle.setStyle({weight:3,fillOpacity:0.7});
            $rootScope.$broadcast('on-mouse-over-rectangle',{rectangle:oRectangle});// TODO SEND MASSAGE FOR CHANGE CSS in LAYER LIST TABLE
            var temp = oRectangle.getBounds()
        });

        //mouse out event set default value of style
        oRectangle.on("mouseout", function (event) {
            oRectangle.setStyle({weight:1,fillOpacity:0.2});
            $rootScope.$broadcast('on-mouse-leave-rectangle',{rectangle:oRectangle});// TODO SEND MASSAGE FOR CHANGE CSS in LAYER LIST TABLE
        });

        return oRectangle;
    };


    /********************************************ZOOM AND NAVIGATION FUNCTIONS**********************************************/

    /**
     * Center the world
     */
    this.goHome = function()
    {
        this.m_oWasdiMap.fitWorld();
    };


    this.flyToWorkspaceBoundingBox = function (aoProducts) {
        try {
            if(utilsIsObjectNullOrUndefined(aoProducts)) return;
            if( aoProducts.length == 0 ) return;

            var aoBounds = [];

            for (var iProducts = 0; iProducts<aoProducts.length; iProducts++) {
                var oProduct = aoProducts[iProducts];
                var aoProductBounds = this.convertBboxInBoundariesArray(oProduct.bbox);
                aoBounds = aoBounds.concat(aoProductBounds);
            }

            this.m_oWasdiMap.flyToBounds([aoBounds]);
            return true;
        }
        catch (e) {
            console.log(e);
        }
    };

    /**
     * Zoom on bounds
     * @param aBounds
     * @returns {boolean}
     */
    this.zoomOnBounds = function (aBounds)
    {
        try {
            if(utilsIsObjectNullOrUndefined(aBounds)) return false;
            if( aBounds.length == 0 ) return false;

            this.m_oWasdiMap.flyToBounds([aBounds]);
            return true;
        }
        catch (e) {
            console.log(e);
        }
    };


    /**
     * Zoom 2d map based on the bbox string form server
     * @param sBbox
     * @returns {null}
     */
    this.zoomBandImageOnBBOX = function (sBbox) {
        try {
            if(utilsIsObjectNullOrUndefined(sBbox)) return null;

            var aoBounds = this.convertBboxInBoundariesArray(sBbox);

            for(var iIndex = 0; iIndex < aoBounds.length; iIndex++ )
            {
                if(utilsIsObjectNullOrUndefined(aoBounds[iIndex])) return null;
            }

            this.m_oWasdiMap.flyToBounds(aoBounds);
        }
        catch (e) {
            console.log(e);
        }

    };

    /**
     * Zoom 2d Map on a geoserver Bounding box string from server
     * @param geoserverBoundingBox
     */
    this.zoomBandImageOnGeoserverBoundingBox = function (geoserverBoundingBox) {
        try {
            if (utilsIsObjectNullOrUndefined(geoserverBoundingBox)) {
                console.log("MapService.zoomBandImageOnGeoserverBoundingBox: geoserverBoundingBox is null or empty ");
                return;
            }

            geoserverBoundingBox = geoserverBoundingBox.replace(/\n/g,"");
            var oBounds = JSON.parse(geoserverBoundingBox);

            //Zoom on layer
            var corner1 = L.latLng(oBounds.maxy, oBounds.maxx),
                corner2 = L.latLng(oBounds.miny, oBounds.minx),
                bounds = L.latLngBounds(corner1, corner2);

            this.m_oWasdiMap.flyToBounds(bounds);
        }
        catch (e) {
            console.log(e);
        }
    };

    /**
     * Zoom on an external layer
     * @param oLayer
     * @returns {boolean}
     */
    this.zoomOnExternalLayer = function (oLayer) {

        try {
            if (utilsIsObjectNullOrUndefined(oLayer) == true) return false;
            var oBoundingBox = (oLayer.BoundingBox[0].extent);
            if (utilsIsObjectNullOrUndefined(oBoundingBox) == true) return false;

            var corner1 = L.latLng(oBoundingBox[1], oBoundingBox[2]),
                corner2 = L.latLng(oBoundingBox[3], oBoundingBox[0]),
                bounds = L.latLngBounds(corner1, corner2);

            this.getMap().flyToBounds(bounds);
        }
        catch (e) {
            console.log(e);
        }
    };

    /**
     * This method works only for s1 products
     * @param boundingBox
     * @param geoserverBoundindBox
     * @returns {boolean}
     */
    this.isProductGeoreferenced = function(boundingBox, geoserverBoundindBox)
    {
        if( ( utilsIsObjectNullOrUndefined(boundingBox) === true ) || ( utilsIsObjectNullOrUndefined(geoserverBoundindBox) === true) )
        {
            return false;
        }
        var oGeoserverBoundingBox = this.parseGeoserverBoundingBox(geoserverBoundindBox);

        if ( utilsIsObjectNullOrUndefined(oGeoserverBoundingBox)) return false;

        var asBoundingBox = this.fromBboxToRectangleArray(boundingBox);

        if ( utilsIsObjectNullOrUndefined(asBoundingBox)) return false;

        var aoLatLngs = [];

        for (var iPoints = 0; iPoints<asBoundingBox.length-2; iPoints+=2) {
            var oLatLon = [parseFloat(asBoundingBox[iPoints+1]), parseFloat(asBoundingBox[iPoints])];
            aoLatLngs.push(oLatLon);
        }

        var oBBPolygon = L.polygon(aoLatLngs, {color: 'red'});

        var oBBCenter = oBBPolygon.getBounds().getCenter();

        //it takes the center of the bounding box
        var oMidPointGeoserverBoundingBox = utilsGetMidPoint(oGeoserverBoundingBox.maxx,oGeoserverBoundingBox.maxy,oGeoserverBoundingBox.minx,oGeoserverBoundingBox.miny);
        //var oMidPointBoundingBox = utilsGetMidPoint( parseFloat(asBoundingBox[0]), parseFloat(asBoundingBox[1]), parseFloat(asBoundingBox[4]), parseFloat(asBoundingBox[5]));
        var oMidPointBoundingBox = {};
        oMidPointBoundingBox.x = oBBCenter.lng;
        oMidPointBoundingBox.y = oBBCenter.lat;
        //
        var isMidPointGeoserverBoundingBoxInBoundingBox = utilsIsPointInsideSquare(oMidPointGeoserverBoundingBox.x,oMidPointGeoserverBoundingBox.y, oBBPolygon.getBounds().getEast(),oBBPolygon.getBounds().getNorth(),oBBPolygon.getBounds().getWest(), oBBPolygon.getBounds().getSouth());
        var isMidPointBoundingBoxGeoserverBoundingBox = utilsIsPointInsideSquare(oMidPointBoundingBox.x,oMidPointBoundingBox.y,oGeoserverBoundingBox.maxx,oGeoserverBoundingBox.maxy,oGeoserverBoundingBox.minx,oGeoserverBoundingBox.miny);
        if( ( isMidPointBoundingBoxGeoserverBoundingBox === true ) && ( isMidPointGeoserverBoundingBoxInBoundingBox === true ) )
        {
            return true;
        }
        return false;
    };

    /**
     *
     * @param geoserverBoundingBox
     * @returns {null}
     */
    this.parseGeoserverBoundingBox = function(geoserverBoundingBox){
        // Check the input
        if (utilsIsObjectNullOrUndefined(geoserverBoundingBox)) {
            console.log("geoserverBoundingBox: geoserverBoundingBox is null");
            return null;
        }

        // Parse the bounding box
        geoserverBoundingBox = geoserverBoundingBox.replace(/\n/g,"");
        var oBoundingBox = JSON.parse(geoserverBoundingBox);
        if(utilsIsObjectNullOrUndefined(oBoundingBox)) {
            console.log("GlobeService.zoomBandImageOnGeoserverBoundingBox: parsing bouning box is null");
            return null;
        }
        return oBoundingBox;
    };
    /**
     *
     * @param bbox
     * @returns {*}
     */
    this.fromBboxToRectangleArray = function (bbox) {

        // skip if there isn't the product bounding box
        if(utilsIsObjectNullOrUndefined(bbox) === true ) return null;

        var aiInvertedArraySplit = [];
        var  aoArraySplit;

        // Split bbox string
        aoArraySplit = bbox.split(",");

        var iArraySplitLength = aoArraySplit.length;

        if(iArraySplitLength <= 10) return null;

        for(var iIndex = 0; iIndex < iArraySplitLength-1; iIndex = iIndex + 2){
            aiInvertedArraySplit.push(aoArraySplit[iIndex+1]);
            aiInvertedArraySplit.push(aoArraySplit[iIndex]);
        }

        return aiInvertedArraySplit;
    };

    this.addRectangleByGeoserverBoundingBox = function(geoserverBoundingBox,sColor)
    {
        try {
            if (utilsIsObjectNullOrUndefined(geoserverBoundingBox)) {
                console.log("MapService.addRectangleByGeoserverBoundingBox: geoserverBoundingBox is null or empty ");
                return;
            }
            if( (utilsIsObjectNullOrUndefined(sColor) === true) || (utilsIsStrNullOrEmpty(sColor) === true))
            {
                sColor = "#ff7800";
            }
            geoserverBoundingBox = geoserverBoundingBox.replace(/\n/g,"");
            var oBounds = JSON.parse(geoserverBoundingBox);
            //Zoom on layer
            // var corner1 = L.latLng(oBounds.maxy, oBounds.maxx),
            //     corner2 = L.latLng(oBounds.miny, oBounds.minx),
            //     bounds = L.latLngBounds(corner1, corner2);
            var bounds = [ [oBounds.maxy,oBounds.maxx],[oBounds.miny,oBounds.minx] ];
            var oRectangle = L.rectangle(bounds, {color: sColor, weight: 2}).addTo(this.m_oWasdiMap);

            return oRectangle
        }
        catch (e)
        {
            console.log(e);
        }
        return null;
    }
}]);