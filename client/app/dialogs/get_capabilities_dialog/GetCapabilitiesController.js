/**
 * Created by a.corrado on 22/02/2017.
 */
var GetCapabilitiesController = (function() {

    function GetCapabilitiesController($scope, oClose,$http,oConstantsService) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oConstantsService = oConstantsService;
        this.m_oHttp=$http;
        this.m_aoLayers = [];
        this.m_bIsVisibleLoadIcon = false;
        $scope.close = function(result) {
            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };

    }
    GetCapabilitiesController.prototype.loadLayers = function(sServer)
    {
        //var myGeoserverUrl="http://localhost:8080/geoserver/ows?"
        //var myGeoserverUrl = this.m_oConstantsService.getWmsUrlGeoserver();
        //sServer = myGeoserverUrl+"service=WMS&request=GetCapabilities";
        if(utilsIsObjectNullOrUndefined(sServer))
            return false;
        this.m_bIsVisibleLoadIcon = true;
        sServer = sServer  +"service=WMS&request=GetCapabilities";
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
                       oController.m_aoLayers.push(oResult.Capability.Layer.Layer[iIndexLayer].Name);
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
    GetCapabilitiesController.$inject = [
        '$scope',
        'close',
        '$http',
        'ConstantsService'
    ];

    return GetCapabilitiesController;
})();
