var WpsController = (function() {

    function WpsController($scope, oClose,oExtras,oConstantsService) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        // this.m_aoAttributes = this.m_oExtras.metadataAttributes;
        this.m_sNameNode = this.m_oExtras.nameNode;
        this.m_oConstantService = oConstantsService;
        this.m_sUrlWasdiGeoserverWPS = this.m_oConstantService.getWasdiGeoserverWPS();
        this.m_aoProcesses = [];
        this.m_oWPSService = null;
        /*metadataAttributes:node.original.attributes*/
        //$scope.close = oClose;
        $scope.close = function(result) {
            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };

        this.wpsInitialization();
    }
    WpsController.prototype.wpsInitialization = function(){

        var wpsService = new WpsService({
            url:  this.m_sUrlWasdiGeoserverWPS,
            version: "1.0.0"
        });

        var oController = this;
        var capabilitiesCallback = function(response) {

            capabilities = response;
            // extract processes, add them to process-list
            //array of processes
            oController.m_aoProcesses = response.capabilities.processes;
            var capabilitiesDocument = capabilities.responseDocument;

            $("textarea#capabilitiesText").val((new XMLSerializer()).serializeToString(capabilitiesDocument));
        };

        wpsService.getCapabilities_GET(capabilitiesCallback);

        this.m_oWPSService = wpsService;

    };

    WpsController.prototype.getDescriptionSelectedProcess = function(sIdentifier)
    {
        if(utilsIsStrNullOrEmpty(sIdentifier) === true)
        {
            return false;
        }

        var describeProcessCallback = function(response) {

            processDescription = response;

            //set value of textarea
            var processDocument = processDescription.responseDocument;

            $("textarea#processDescriptionText").val((new XMLSerializer()).serializeToString(processDocument));

        };

        this.m_oWPSService.describeProcess_GET(describeProcessCallback, sIdentifier);

        return true;
    };
    WpsController.prototype.executeCallback = function(response) {

        var doc = response.getRawResponseDocument();
        try {
            $("textarea#xmlExecute").val((new XMLSerializer()).serializeToString(doc));
        } catch (e){
            $("textarea#xmlExecute").val(doc);
        }

    };
    WpsController.prototype.testExecuteRequest_wps_1_0 = function()
    {
        var inputGenerator = new InputGenerator();
        var inputs,outputs;
        this.m_oWPSService.execute(this.executeCallback(), "JTS:area", "document", "async", false, inputs, outputs);

    };

    WpsController.$inject = [
        '$scope',
        'close',
        'extras',
        'ConstantsService'
    ];
    return WpsController;
})();
