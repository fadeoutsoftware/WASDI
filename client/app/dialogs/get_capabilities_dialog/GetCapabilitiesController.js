/**
 * Created by a.corrado on 22/02/2017.
 */
var GetCapabilitiesController = (function() {

    function GetCapabilitiesController($scope, oClose,$http,oConstantsService,oExtras,oMapService,oGlobeService ) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oConstantsService = oConstantsService;
        this.m_oHttp=$http;
        this.m_aoLayers = [];
        this.m_bIsVisibleLoadIcon = false;
        this.m_oEditorController = oExtras;
        this.m_sServerLink = null;
        this.m_oMapService = oMapService;
        this.m_oGlobeService = oGlobeService;
        $scope.close = function(result) {
            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };

    }
    GetCapabilitiesController.prototype.loadLayers = function(sServer)
    {
        //var myGeoserverUrl = this.m_oConstantsService.getWmsUrlGeoserver();
        this.m_sServerLink =sServer;// myGeoserverUrl;
        sServer = sServer+"service=WMS&request=GetCapabilities";

        if(utilsIsObjectNullOrUndefined(sServer))
            return false;

        this.m_bIsVisibleLoadIcon = true;
        //this.m_sServerLink = sServer;
        //sServer = sServer  +"service=WMS&request=GetCapabilities";

        var oController = this;
        this.m_oHttp.get(sServer).success(function (data, status) {
            if (!utilsIsObjectNullOrUndefined(data) )
            {
                var oResult = new WMSCapabilities().parse(data);
                if(!utilsIsObjectNullOrUndefined(oResult.Capability) && !utilsIsObjectNullOrUndefined(oResult.Capability.Layer) && !utilsIsObjectNullOrUndefined(oResult.Capability.Layer.Layer))
                {
                    var iNumberOfLayers = oResult.Capability.Layer.Layer.length;
                   for(var iIndexLayer = 0; iIndexLayer < iNumberOfLayers; iIndexLayer++)
                   {
                       oController.m_aoLayers.push(oResult.Capability.Layer.Layer[iIndexLayer]);
                   }

                }
            }
            oController.m_bIsVisibleLoadIcon = false;
        }).error(function (data,status)
        {
            utilsVexDialogAlertTop("Error: the get capabilities doesn't work");
            oController.m_bIsVisibleLoadIcon = false;
        });

        //$.get(sServer, function(data, status){
        //    //alert("Data: " + data + "\nStatus: " + status);
        //
        //});

        return true;
    }
    GetCapabilitiesController.prototype.getLayers = function()
    {
        return this.m_aoLayers;
    }
    GetCapabilitiesController.prototype.isEmptyLayersList = function()
    {
        if(utilsIsObjectNullOrUndefined(this.m_aoLayers))
            return true;

        if( this.m_aoLayers.length == 0)
            return true;

        return false;
    }
    GetCapabilitiesController.prototype.addLayerToMap = function(sLayerId)
    {
        this.addLayerMap2D(sLayerId,this.m_sServerLink)
    }

    GetCapabilitiesController.prototype.addLayerMap2D = function (sLayerId,sUrlGeoserver) {
        var oMap = this.m_oMapService.getMap();

        var wmsLayer = L.tileLayer.betterWms(sUrlGeoserver, {
            layers:  sLayerId,
            format: 'image/png',
            transparent: true,
            noWrap:true
        });
        wmsLayer.setZIndex(1000);
        wmsLayer.addTo(oMap);

    }

    GetCapabilitiesController.prototype.addLayerMap3D = function (sLayerId,sUrlGeoserver) {
        var oGlobeLayers=this.m_oGlobeService.getGlobeLayers();

        var oWMSOptions= { // wms options
            transparent: true,
            format: 'image/png',
            //crossOriginKeyword: null
        };//crossOriginKeyword: null

        // WMS get GEOSERVER
        var oProvider = new Cesium.WebMapServiceImageryProvider({
            url : sUrlGeoserver,
            layers: sLayerId,
            parameters : oWMSOptions,

        });
        oGlobeLayers.addImageryProvider(oProvider);
        //this.test=oGlobeLayers.addImageryProvider(oProvider);
    }

    GetCapabilitiesController.prototype.publishLayer = function (oLayer) {

        if(utilsIsObjectNullOrUndefined(oLayer))
        {
            console.log("Error LayerID is empty...");
            return false;
        }
        //add layer in list
        oLayer.sServerLink = this.m_sServerLink; // add property server link

        // check if the background is grey or there is a map
        if(this.m_oEditorController.m_bIsVisibleMapOfLeaflet == true)
        {
            //if there is a map, add layers to it
            this.addLayerMap2D(oLayer.Name,this.m_sServerLink);
            this.addLayerMap3D(oLayer.Name,this.m_sServerLink);
            this.m_oEditorController.m_aoLayersList.push(oLayer);
        }
        else
        {
            //if the backgrounds is grey
            // remove all others bands in tree - map
            var iNumberOfLayers = this.m_oEditorController.m_aoLayersList.length;
            for(var iIndexLayer = 0; iIndexLayer < iNumberOfLayers; iIndexLayer++)
            {
                //check if there is layerId if there isn't the layer was added by get capabilities
                if(!utilsIsObjectNullOrUndefined(this.m_oEditorController.m_aoLayersList[iIndexLayer].layerId))
                {
                    var oNode = $('#jstree').jstree(true).get_node(this.m_oEditorController.m_aoLayersList[iIndexLayer].layerId);
                    oNode.original.bPubblish = false;
                    $('#jstree').jstree(true).set_icon(this.m_oEditorController.m_aoLayersList[iIndexLayer].layerId, 'assets/icons/uncheck_20x20.png');
                }
            }
            this.m_oEditorController.m_aoLayersList = [];
            this.m_oMapService.removeLayersFromMap();
            this.m_oGlobeService.clearGlobe();
            this.m_oGlobeService.initGlobe('cesiumContainer2');

            // so add the new bands
            // and the bounding box in cesium
            //TODO CHECK IF .BoundingBox[0] it's right
            var oBounds = (oLayer.BoundingBox[0].extent);
            this.m_oGlobeService.addRectangleOnGlobeBoundingBox([oBounds[0],oBounds[1],oBounds[2],oBounds[3]]);
            this.addLayerMap2D(oLayer.Name,this.m_sServerLink);
            this.m_oEditorController.m_aoLayersList.push(oLayer);

        }
        var oBounds = (oLayer.BoundingBox[0].extent);
        //zoom on layer
        this.zoomOnLayer2DMap([oBounds[1],oBounds[2],oBounds[3],oBounds[0]]);
        this.zoomOnLayer3DGlobe([oBounds[0],oBounds[1],oBounds[2],oBounds[3]]);
    }

    GetCapabilitiesController.prototype.zoomOnLayer2DMap = function(oBoundingBox)
    {
        if (utilsIsObjectNullOrUndefined(oBoundingBox) == true)
            return false;
        var oMap = this.m_oMapService.getMap();
        var corner1 = L.latLng(oBoundingBox[0], oBoundingBox[1]),
            corner2 = L.latLng(oBoundingBox[2], oBoundingBox[3]),
            bounds = L.latLngBounds(corner1, corner2);
        oMap.fitBounds(bounds);
    }



    GetCapabilitiesController.prototype.zoomOnLayer3DGlobe = function(oBoundingBox)
    {
        if(utilsIsObjectNullOrUndefined(oBoundingBox)== true)
            return false;
        var oGlobe = this.m_oGlobeService.getGlobe();
        /* set view of globe*/
        oGlobe.camera.setView({
            destination:  Cesium.Rectangle.fromDegrees(oBoundingBox[0], oBoundingBox[1], oBoundingBox[2], oBoundingBox[3]),
            orientation: {
                heading: 0.0,
                pitch: -Cesium.Math.PI_OVER_TWO,
                roll: 0.0
            }
        });
    }

    GetCapabilitiesController.$inject = [
        '$scope',
        'close',
        '$http',
        'ConstantsService',
        'extras',
        'MapService',
        'GlobeService'

    ];

    return GetCapabilitiesController;
})();
