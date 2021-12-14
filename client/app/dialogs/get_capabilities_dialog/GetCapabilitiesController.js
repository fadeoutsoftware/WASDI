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
        this.m_sServerLink =sServer;
        sServer = sServer+"service=WMS&request=GetCapabilities";

        if(utilsIsObjectNullOrUndefined(sServer)) return false;

        this.m_bIsVisibleLoadIcon = true;

        var oController = this;

        this.m_oHttp.get(sServer).then(function (data, status) {
            if (!utilsIsObjectNullOrUndefined(data.data) )
            {
                var oResult = new WMSCapabilities().parse(data.data);

                if(!utilsIsObjectNullOrUndefined(oResult.Capability) && !utilsIsObjectNullOrUndefined(oResult.Capability.Layer) && !utilsIsObjectNullOrUndefined(oResult.Capability.Layer.Layer))
                {

                    oController.m_aoLayers = [];
                    var iNumberOfLayers = oResult.Capability.Layer.Layer.length;
                    for(var iIndexLayer = 0; iIndexLayer < iNumberOfLayers; iIndexLayer++)
                    {
                        oController.m_aoLayers.push(oResult.Capability.Layer.Layer[iIndexLayer]);
                    }

                }
            }
            oController.m_bIsVisibleLoadIcon = false;
        },function (data,status)
        {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR: THE GET CAPABILITIES DOESN'T WORK");
            oController.m_bIsVisibleLoadIcon = false;
        });

        return true;
    };

    GetCapabilitiesController.prototype.getLayers = function()
    {
        return this.m_aoLayers;
    };

    GetCapabilitiesController.prototype.isEmptyLayersList = function()
    {
        if(utilsIsObjectNullOrUndefined(this.m_aoLayers))
            return true;

        if( this.m_aoLayers.length == 0) return true;

        return false;
    };


    GetCapabilitiesController.prototype.publishLayer = function (oLayer) {

        if(utilsIsObjectNullOrUndefined(oLayer))
        {
            console.log("Error LayerID is empty...");
            return false;
        }
        //add layer in list
        oLayer.sServerLink = this.m_sServerLink; // add property server link
        
        //if there is a map, add layers to it
        this.m_oEditorController.addLayerMap2DByServer(oLayer.Name,this.m_sServerLink);
        this.m_oEditorController.addLayerMap3DByServer(oLayer.Name,this.m_sServerLink);
        this.m_oEditorController.m_aoExternalLayers.push(oLayer);

        this.m_oMapService.zoomOnExternalLayer(oLayer);
        this.m_oGlobeService.zoomOnExternalLayer(oLayer);
    };


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
