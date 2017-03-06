/**
 * Created by p.campanella on 21/10/2016.
 */

'use strict';
angular.module('wasdi.MapService', ['wasdi.ConstantsService']).
service('MapService', ['$http','$rootScope', 'ConstantsService', function ($http,$rootScope, oConstantsService) {
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;
    this.m_oRectangleOpenSearch = null;
    this.m_oDrawItems = null;
    this.m_oConstantsService = oConstantsService;
    /**
     * base layers
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
    }

    //init tile layer
    this.initTileLayer();

    this.m_oGoogleHybrid = new L.gridLayer.googleMutant('hybrid');
    this.m_oGoogleMap = new L.gridLayer.googleMutant('roadmap');
    this.m_oGoogleTerrain = new L.gridLayer.googleMutant('terrain');
    //this.m_oGoogleSatelite = new L.gridLayer.googleMutant('satellite');// it doesn't work look at m_oGoogleSatelite in init layer

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

    //************** MAP *********************
    this.m_oWasdiMap = null;


    this.m_oActiveBaseLayer = this.m_oOSMBasic;

    this.clearMap = function () {
        if (this.m_oWasdiMap) {
            this.m_oWasdiMap.remove();
            this.m_oWasdiMap = null;
        }
    }


    this.initMap = function(sMapDiv) {


        /**
         * the map
         */
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
            keyboard: false,
             //maxZoom: 22
        });

        //this.removeLayersFromMap();

        /* coordinates in map
        * find this plugin in lib folder
        * */
        L.control.mousePosition().addTo(this.m_oWasdiMap);

        /**
         * scale control
         */
        L.control.scale({
            position: "bottomright",
            imperial: false
        }).addTo(this.m_oWasdiMap);


        /**
         * layers control
         */
        this.m_oLayersControl.addTo(this.m_oWasdiMap);

        ///**
        // * fitBounds
        //
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
            e.layer.bringToBack()
            oActiveBaseLayer = e;
        });


        //add event on base change
        //this.m_oWasdiMap.on('load', function(e){
        //    oWasdiMap.invalidateSize();
        //});
    }

    //remove layer layer.remove()
    this.removeLayerFromMap = function(oLayer)
    {
        if(utilsIsObjectNullOrUndefined(oLayer))
            return false;
        oLayer.remove();
        return true;
    }
    /*remove layerS */
    this.removeLayersFromMap = function()
    {
        var oController = this;
        oController.m_oWasdiMap.eachLayer(function (layer) {
            oController.m_oWasdiMap.removeLayer(layer);
        });
    }



    //Add rectangle shape
    this.addRectangleOnMap = function (aaBounds,sColor,iIndexLayers)
    {
        if(utilsIsObjectNullOrUndefined(aaBounds))
            return null;

        for(var iIndex = 0; iIndex < aaBounds.length; iIndex++ )
        {
            if(utilsIsObjectNullOrUndefined(aaBounds[iIndex]))
                return null;

            ///* if the LatLng coordinates are "outside the map" return the right coordinates */
            //var adLatLng = L.latLng(aaBounds[iIndex]);
            //aaBounds[iIndex] = this.m_oWasdiMap.wrapLatLng(adLatLng);

        }


        if(utilsIsStrNullOrEmpty(sColor))
            sColor="#ff7800";//default color


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

        ////TODO REMOVE IT USED ONLY FOR TEST
        //this.m_oWasdiMap.fitBounds(aaBounds);//zoom on rectangle

        return oRectangle;
    }

    // ZOOM
    this.zoomOnBounds = function (aBounds)
    {
        if(utilsIsObjectNullOrUndefined(aBounds))
            return false;
        ////check if there are 2 points
        //if(aaBounds.length != 2)
        //    return false;
        //check if they are points [ax,ay],[bx,by] == good
        // [ax,ay,az,....],[bx,by,bz,....] == bad
        //if(aaBounds[0].length != 2 || aaBounds[1].length != 2)
        //    return false;
        if( aBounds.length == 0 )
            return false;

        this.m_oWasdiMap.fitBounds([aBounds]);
        return true;
    }



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

    }

    this.getMap = function () {
        return this.m_oWasdiMap;
    }
    this.deleteDrawShapeEditToolbar = function()
    {
        this.m_oDrawItems.clearLayers();
    }

    this.getHome = function()
    {
        //var oCenter = this.m_oWasdiMap.getCenter();
        //this.m_oWasdiMap.setView(oCenter,5,maxZoom);
        this.m_oWasdiMap.fitWorld();
    }

    this.setBasicMap = function()
    {
        if(utilsIsObjectNullOrUndefined(this.m_oOSMBasic))
            return false
        this.m_oWasdiMap.addLayer(this.m_oOSMBasic,true);
        return true;
    }

    this.removeBasicMap = function()
    {
        if(utilsIsObjectNullOrUndefined(this.m_oOSMBasic))
            return false
        this.removeLayerFromMap(this.m_oOSMBasic);
        return true;
    }

    this.initMapEditor = function(sMapDiv)
    {
        if(utilsIsObjectNullOrUndefined(sMapDiv))
            return false;
        this.initMap(sMapDiv);

        //this.m_oWasdiMap.on('click', function(e){
        //    //// console.log(e);
        //    //e.layer.bringToBack();
        //});
        return true;
    }
}]);