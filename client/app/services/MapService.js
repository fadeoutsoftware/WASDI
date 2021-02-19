/**
 * Created by p.campanella on 21/10/2016.
 */

'use strict';
angular.module('wasdi.MapService', ['wasdi.ConstantsService']).
service('MapService', ['$http','$rootScope', 'ConstantsService', 'ModalService',  function ($http,$rootScope, oConstantsService, oModalService) {
    // API URL
    this.APIURL = oConstantsService.getAPIURL();

    // Service references
    this.m_oHttp = $http;
    this.m_oConstantsService = oConstantsService;
    //this.m_oRectangleOpenSearch = null;
    this.m_oDrawItems = null;

    this.m_oModalService = oModalService;

    /**
     * Init base layers
     */
    this.initTileLayer= function(){

        this.m_oOSMBasic = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution:
                '&copy; <a href="https://openstreetmap.org">OpenStreetMap</a> contributors',
            maxZoom: 18,
            // this map option disables world wrapping. by default, it is false.
            continuousWorld: false,
            // this option disables loading tiles outside of the world bounds.
            noWrap: true
        });

        this.m_oOpenTopoMap = L.tileLayer('https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png', {
            maxZoom: 17,
            attribution: 'Map data: &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>, <a href="http://viewfinderpanoramas.org">SRTM</a> | Map style: &copy; <a href="https://opentopomap.org">OpenTopoMap</a> (<a href="https://creativecommons.org/licenses/by-sa/3.0/">CC-BY-SA</a>)'
        });

        this.m_oEsriWorldStreetMap = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}', {
            attribution: 'Tiles &copy; Esri &mdash; Source: Esri, DeLorme, NAVTEQ, USGS, Intermap, iPC, NRCAN, Esri Japan, METI, Esri China (Hong Kong), Esri (Thailand), TomTom, 2012'
        });

        this.m_oEsriWorldImagery = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
            attribution: 'Tiles &copy; Esri &mdash; Source: Esri, i-cubed, USDA, USGS, AEX, GeoEye, Getmapping, Aerogrid, IGN, IGP, UPR-EGP, and the GIS User Community'
        });

        this.m_oNASAGIBSViirsEarthAtNight2012 = L.tileLayer('https://map1.vis.earthdata.nasa.gov/wmts-webmerc/VIIRS_CityLights_2012/default/{time}/{tilematrixset}{maxZoom}/{z}/{y}/{x}.{format}', {
            attribution: 'Imagery provided by services from the Global Imagery Browse Services (GIBS), operated by the NASA/GSFC/Earth Science Data and Information System (<a href="https://earthdata.nasa.gov">ESDIS</a>) with funding provided by NASA/HQ.',
            bounds: [[-85.0511287776, -179.999999975], [85.0511287776, 179.999999975]],
            minZoom: 1,
            maxZoom: 8,
            format: 'jpg',
            time: '',
            tilematrixset: 'GoogleMapsCompatible_Level'
        });
    };

    //init tile layer
    this.initTileLayer();

    /**
     * layers control
     */
    this.m_oLayersControl = L.control.layers(
        {
            "Standard": this.m_oOSMBasic,
            "OpenTopoMap":this.m_oOpenTopoMap,
            "EsriWorldStreetMap":this.m_oEsriWorldStreetMap,
            "EsriWorldImagery":this.m_oEsriWorldImagery,
            "NASAGIBSViirsEarthAtNight2012":this.m_oNASAGIBSViirsEarthAtNight2012
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

    this.initWasdiMap = function(sMapDiv)
    {
        if(this.m_oWasdiMap != null)
        {
            this.initTileLayer();
        }
        this.m_oWasdiMap = this.initMap(sMapDiv);
    };
    /**
     * Init the Map
     * @param sMapDiv
     */
    this.initMap = function(sMapDiv) {

        var oMap = L.map(sMapDiv, {
            zoomControl: false,
            //layers: [this.m_oOSMBasic, this.m_oOpenTopoMap, this.m_oEsriWorldStreetMap, this.m_oEsriWorldImagery, this.m_oNASAGIBSViirsEarthAtNight2012],
            layers: [this.m_oOSMBasic],
            keyboard: false
             //maxZoom: 22
        });

        // coordinates in map find this plugin in lib folder
        L.control.mousePosition().addTo(oMap);

        //scale control
        L.control.scale({
            position: "bottomright",
            imperial: false
        }).addTo(oMap);

        //layers control
        this.m_oLayersControl.addTo(oMap);

        // center map
        var southWest = L.latLng(0, 0),
            northEast = L.latLng(0, 0),
            oBoundaries = L.latLngBounds(southWest, northEast);

        oMap.fitBounds(oBoundaries);
        oMap.setZoom(3);

        // var oActiveBaseLayer = this.m_oActiveBaseLayer;

        //add event on base change
        oMap.on('baselayerchange', function(e){
            // console.log(e);
            //e.layer.bringToBack();
            // oActiveBaseLayer = e;
        });

        return oMap;

    };


    this.initMapSingleton = function(sMapDiv) {
        var oOSMBasic = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution:
                '&copy; <a href="https://openstreetmap.org">OpenStreetMap</a> contributors',
            maxZoom: 18,
            // this map option disables world wrapping. by default, it is false.
            continuousWorld: false,
            // this option disables loading tiles outside of the world bounds.
            noWrap: true
        });

        var oMap = L.map(sMapDiv, {
            zoomControl: false,
            //layers: [this.m_oOSMBasic, this.m_oOpenTopoMap, this.m_oEsriWorldStreetMap, this.m_oEsriWorldImagery, this.m_oNASAGIBSViirsEarthAtNight2012],
            layers: [oOSMBasic],
            keyboard: false
            //maxZoom: 22
        });

        // coordinates in map find this plugin in lib folder
        L.control.mousePosition().addTo(oMap);

        //scale control
        L.control.scale({
            position: "bottomright",
            imperial: false
        }).addTo(oMap);

        //layers control
        var oLayersControl = L.control.layers(
            {
                "Standard": this.m_oOSMBasic,
                "OpenTopoMap":this.m_oOpenTopoMap,
                "EsriWorldStreetMap":this.m_oEsriWorldStreetMap,
                "EsriWorldImagery":this.m_oEsriWorldImagery,
                "NASAGIBSViirsEarthAtNight2012":this.m_oNASAGIBSViirsEarthAtNight2012
            },
            {},
            {
                'position' : 'bottomright'
            }
        );
        oLayersControl.addTo(oMap);

        // center map
        var southWest = L.latLng(0, 0),
            northEast = L.latLng(0, 0),
            oBoundaries = L.latLngBounds(southWest, northEast);

        oMap.fitBounds(oBoundaries);
        oMap.setZoom(3);

        // var oActiveBaseLayer = oOSMBasic;

        //add event on base change
        oMap.on('baselayerchange', function(e){
            // console.log(e);
            //e.layer.bringToBack();
            // oActiveBaseLayer = e;
        });

        return oMap;

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
        this.initWasdiMap(sMapDiv);
        //LEAFLET.DRAW LIB

        //add draw.search (opensearch)
        var aoDrawnItems = new L.FeatureGroup();
        this.m_oDrawItems = aoDrawnItems;//save draw items (used in delete shape)
        this.m_oWasdiMap.addLayer(aoDrawnItems);

        var oOptions={
            position:'topright',//position of menu
            draw:{// what kind of shapes are disable/enable
                circle:false,
                circlemarker:false,
                marker:false,
                polyline:false,
                polygon:false
            },

            edit: {
                featureGroup: aoDrawnItems,//draw items are the "voice" of menu
                edit: false,// hide edit button
                remove: false// hide remove button
            }
        };

        var oDrawControl = new L.Control.Draw(oOptions);

        this.m_oWasdiMap.addControl(oDrawControl);

        this.m_oWasdiMap.on(L.Draw.Event.CREATED, function (event)
        {
            var oLayer = event.layer;
            oController.m_oRectangleOpenSearch = oLayer;

            //remove old shape
            if(aoDrawnItems && aoDrawnItems.getLayers().length!==0){
                aoDrawnItems.clearLayers();
            }
            $rootScope.$broadcast('rectangle-created-for-opensearch',{layer:oLayer});//SEND MESSAGE TO IMPORT CONTROLLER
            //save new shape in map
            aoDrawnItems.addLayer(oLayer);
        });

        //REMOVE IT ?
        this.m_oWasdiMap.on(L.Draw.Event.DELETESTOP, function (event) {
           // var layer = event.layers;
        });

        let oModalServiceTemp = this.m_oModalService;

        L.control.custom({
            position: 'topright',
            content : '<div type="button" class="import-insert-bbox-button" title="Insert a bbox by text">'+
                '    <i class="import-insert-bbox-icon fa fa-edit" ></i>'+
                '</div>',
            classes : 'import-insert-bbox-wrapper-button btn-group-vertical btn-group-sm',
            style   :
                {
                },
            events:
                {
                    click: function(data)
                    {
                        oModalServiceTemp.showModal({
                            templateUrl: "dialogs/manual_insert_bbox/ManualInsertBboxView.html",
                            controller: "ManualInsertBboxController",
                            inputs: {
                                extras: {}
                            }
                        }).then(function (modal) {
                            modal.element.modal();
                            modal.close.then(function (oResult) {

                                if (oResult==null) return;

                                let fNorth = parseFloat(oResult.north);
                                let fSouth = parseFloat(oResult.south);
                                let fEast = parseFloat(oResult.east);
                                let fWest = parseFloat(oResult.west);

                                if (isNaN(fNorth) || isNaN(fSouth) || isNaN(fEast) || isNaN(fWest)) {
                                    return;
                                }

                                var aoBounds = [[fNorth, fWest], [fSouth, fEast]];
                                var oLayer = L.rectangle(aoBounds, {color: "#3388ff", weight: 1});
                                oController.m_oRectangleOpenSearch = oLayer;

                                //remove old shape
                                if(aoDrawnItems && aoDrawnItems.getLayers().length!==0){
                                    aoDrawnItems.clearLayers();
                                }
                                $rootScope.$broadcast('rectangle-created-for-opensearch',{layer:oLayer});//SEND MESSAGE TO IMPORT CONTROLLER
                                //save new shape in map
                                aoDrawnItems.addLayer(oLayer);
                            });
                        });
                    }
                }
        }).addTo(this.m_oWasdiMap);

    };

    this.mapDrawEventDeletePolygon = function(oMap,oFunction,oController)
    {
        if(utilsIsObjectNullOrUndefined(oFunction) === true || utilsIsObjectNullOrUndefined(oMap) === true || utilsIsObjectNullOrUndefined(oController) === true)
        {
            return false;
        }

       // var oController = this;
        oMap.on(L.Draw.Event.DELETED,  function (event)
        {
            oFunction(oController);
        });

        return true;
    };

    /**
     * Init map editor
     * @param sMapDiv
     * @returns {boolean}
     */
    this.initMapEditor = function(sMapDiv)
    {
        if(utilsIsObjectNullOrUndefined(sMapDiv)) return false;
        this.initWasdiMap(sMapDiv);

        return true;
    };

    /**
     * Init geo search plugin, the search bar for geographical reference on the map
     * @param opt if present, the search bar is placed on the bottom right corner of the map.
     * @references https://github.com/perliedman/leaflet-control-geocoder
     */
    this.initGeoSearchPluginForOpenStreetMap = function(opt)
    {
        var geocoder = L.Control.Geocoder.nominatim();
        let sPosition = 'topleft';

        if (opt){sPosition='bottomright'}
        L.Control.geocoder({
            geocoder: geocoder,
            position: sPosition
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

                if(this.isAlreadyDrawRectangle(aoProducts[iProduct].bbox) === false){
                    this.addRectangleOnMap(aoProducts[iProduct].bbox, sColor, aoProducts[iProduct].fileName);
                }

            }
        }
        catch (e) {
            console.log(e);
        }

    };

    this.isAlreadyDrawRectangle = function(aoProductBbox) {
        try{
            var aoBounds = this.convertBboxInBoundariesArray(aoProductBbox);
            // var aoLatLngsProduct = [];
            var oRectangle = L.polygon(aoBounds);
            var aoLatLngsProduct = oRectangle.getLatLngs();
            // aoLatLngsProduct.concat(aoLatLngsRectangle);

            // for(var iIndexBound = 0 ; iIndexBound < aoBounds.length; iIndexBound++ ){
            //     var oLatLng = L.latLng(aoBounds[iIndexBound]);
            //     aoLatLngArrayProduct.push(oLatLng);
            // }

            var oMap = this.getMap();
            var isAlreadyDraw = false;
            oMap.eachLayer( function(layer) {
                if(layer instanceof L.Polygon) {
                    var aoLayerLatLng = layer._latlngs;
                    var iLatLngsProductLength = aoLatLngsProduct[0].length;

                    for(let iIndexLatLngProduct = 0 ; iIndexLatLngProduct < iLatLngsProductLength; iIndexLatLngProduct++){

                        var bAreEquals = aoLatLngsProduct[0][iIndexLatLngProduct].equals(aoLayerLatLng[0][iIndexLatLngProduct] , 0.1);
                        if(bAreEquals){
                            isAlreadyDraw = true;
                        }
                    }
                }
            });
        }
        catch(e){
            return false;
        }

        return isAlreadyDraw;
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
            $rootScope.$broadcast('on-mouse-over-rectangle',{rectangle:oRectangle});
            oRectangle.getBounds();


        });
        //mouse out event set default value of style
        oRectangle.on("mouseout", function (event) {//SEND MESSAGE TO IMPORT CONTROLLER
            oRectangle.setStyle({weight:1,fillOpacity:0.2});
            $rootScope.$broadcast('on-mouse-leave-rectangle',{rectangle:oRectangle});
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
        try{
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
                $rootScope.$broadcast('on-mouse-over-rectangle',{rectangle:oRectangle});
                oRectangle.getBounds();
            });

            //mouse out event set default value of style
            oRectangle.on("mouseout", function (event) {
                oRectangle.setStyle({weight:1,fillOpacity:0.2});
                $rootScope.$broadcast('on-mouse-leave-rectangle',{rectangle:oRectangle});
            });

        }catch(e){
            return null;
        }


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
     * flyOnRectangle
     * @param oRectangle
     * @returns {boolean}
     */
    this.flyOnRectangle = function(oRectangle)
    {
        if(utilsIsObjectNullOrUndefined(oRectangle) === true)
        {
            return false;
        }
        if(utilsIsObjectNullOrUndefined(this.m_oWasdiMap) === true)
        {
            return false;
        }
        this.m_oWasdiMap.flyToBounds(oRectangle.getBounds());
        return true;
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
     * @param boundingBox The bounding box declared by product
     * @param geoserverBoundindBox The bounding box declared by the Geo Server when a band has been published
     * @returns {boolean}
     */
    this.isProductGeoreferenced = function(boundingBox, geoserverBoundindBox)
    {
        if( ( utilsIsObjectNullOrUndefined(boundingBox) === true ) || ( utilsIsObjectNullOrUndefined(geoserverBoundindBox) === true) )
        {
            if(utilsIsObjectNullOrUndefined(boundingBox) === true)
            {
                console.debug("Product bounding box is null");
                // Impossible to assume if is correct or not. Assume true
                return true;
            }
            else if(utilsIsObjectNullOrUndefined(geoserverBoundindBox) === true)
            {
                console.debug("Geoserver bounding box is null");
            }

            return false;
        }

        if( ( utilsIsStrNullOrEmpty(boundingBox) === true ) || ( utilsIsStrNullOrEmpty(geoserverBoundindBox) === true) )
        {
            if(utilsIsStrNullOrEmpty(boundingBox) === true)
            {
                console.debug("Product bounding box is null");
                // Impossible to assume if is correct or not. Assume true
                return true;
            }
            else if(utilsIsStrNullOrEmpty(geoserverBoundindBox) === true)
            {
                console.debug("Geoserver bounding box is null");
            }
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
        //TODO FIX IT
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

        if(iArraySplitLength < 10) return null;

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
