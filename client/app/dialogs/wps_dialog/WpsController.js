var WpsController = (function() {

    function WpsController($scope, oClose,oExtras,oConstantsService,oSnapOperationService) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oSnapOperationService = oSnapOperationService;
        // this.m_aoAttributes = this.m_oExtras.metadataAttributes;
        this.m_sNameNode = this.m_oExtras.nameNode;
        this.m_oConstantService = oConstantsService;
        this.m_sUrlWasdiGeoserverWPS = this.m_oConstantService.getWasdiGeoserverWPS();
        this.m_aoProcesses = [];
        this.m_oWPSService = null;
        this.m_sSelectedWps = "";
        this.m_asWPSListURL = [];
        // this.m_sSelectedProcess = "";
        /*metadataAttributes:node.original.attributes*/
        //$scope.close = oClose;
        $scope.close = function(result) {
            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };

        this.setUpProxy();
        this.getWPSList();
        // this.wpsInitialization();
    }
    WpsController.prototype.wpsInitialization = function(){

        var wpsService = new WpsService({
            url:  this.m_sUrlWasdiGeoserverWPS,
            // url:  "http://geoprocessing.demo.52north.org:8080/latest-wps/WebProcessingService",
            version: "1.0.0"
        });

        var oController = this;
        var capabilitiesCallback = function(response) {

            // extract processes, add them to process-list
            //array of processes
            oController.m_aoProcesses = response.capabilities.processes;

            //made a list of processes
            // var _select = $('<select>');
            // $.each(oController.m_aoProcesses, function(index, process) {
            //     _select.append(
            //         $('<option></option>').val(process.identifier).html(process.identifier)
            //     );
            // });
            // $('#processes').append(_select.html());
            // $('#processes_execute').append(_select.html());


            //made a list on info
            // $("textarea#capabilitiesText").val((new XMLSerializer()).serializeToString(capabilitiesDocument));
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
        var oController = this;
        var describeProcessCallback = function(response) {

            var processDescription = response;


            // $("textarea#processDescriptionText").val((new XMLSerializer()).serializeToString(processDocument));
            oController.generateInput(processDescription.processOffering.process.inputs);
        };

        this.m_oWPSService.describeProcess_GET(describeProcessCallback, sIdentifier);

        return true;
    };

    WpsController.prototype.generateInput = function(aoInputs){
        if(utilsIsObjectNullOrUndefined(aoInputs) === true)
        {
            return false;
        }
        var iNumberOfInputs = aoInputs.length;
        for( var iIndexInputs = 0 ; iIndexInputs < iNumberOfInputs; iIndexInputs++ )
        {
            if(utilsIsObjectNullOrUndefined(aoInputs[iIndexInputs].literalData) === false)
            {
                // this.literalDataInput(sIdentifier,dataType, uom, value)
            }
            if(utilsIsObjectNullOrUndefined(aoInputs[iIndexInputs].complexData) === false)
            {
                // this.complexDataInput(aoInputs[iIndexInputs].identifier, mimeType, schema, encoding,  asReference, complexPayload)
                var iNumberOfFormats = aoInputs[iIndexInputs].complexData.formats.length;
                for( var iIndexFormat = 0; iIndexFormat < iNumberOfFormats; iIndexFormat++)
                {
                    var oFormat = aoInputs[iIndexInputs].complexData.formats[iIndexFormat];
                    this.complexDataInput(aoInputs[iIndexInputs].identifier, oFormat.mimeType,oFormat.schema, oFormat.encoding,  true, undefined)
                }
            }
            if(utilsIsObjectNullOrUndefined(aoInputs[iIndexInputs].boundingBoxData) === false)
            {
                // this.boundingBoxDataInput(sIdentifier, crs, dimension, lowerCorner, upperCorner)
            }

        }

    };

    WpsController.prototype.literalDataInput = function(sIdentifier,dataType, uom, value){
        var inputGenerator = new InputGenerator();
        inputGenerator.createLiteralDataInput_wps_1_0_and_2_0(sIdentifier, dataType, uom, value);
    };

    WpsController.prototype.complexDataInput = function(sIdentifier, mimeType, schema,
                                                             encoding,  asReference, complexPayload){
        var inputGenerator = new InputGenerator();
        inputGenerator.createComplexDataInput_wps_1_0_and_2_0(sIdentifier, mimeType, schema, encoding, asReference, complexPayload);
    };

    WpsController.prototype.boundingBoxDataInput = function(sIdentifier, crs, dimension, lowerCorner, upperCorner){
        var inputGenerator = new InputGenerator();
        inputGenerator.createBboxDataInput_wps_1_0_and_2_0(sIdentifier, crs, dimension, lowerCorner, upperCorner);
    };

    WpsController.prototype.literalDataOutputWPS1 = function(sIdentifier, bReference){
        var outputGenerator = new OutputGenerator();
        outputGenerator.createLiteralOutput_WPS_1_0(sIdentifier, bReference);
    };
    WpsController.prototype.complexDataOutputWPS1 = function(sIdentifier, bReferenceidentifier, mimeType, schema,
                                                         encoding, uom, asReference, title, abstractValue){
        var outputGenerator = new OutputGenerator();
        outputGenerator.createComplexOutput_WPS_1_0(sIdentifier, bReferenceidentifier, mimeType, schema,
            encoding, uom, asReference, title, abstractValue);
    };

    // /**
    //  * clickOnShowCapabilities
    //  */
    WpsController.prototype.clickOnShowCapabilities = function()
    {
        // var sel = document.getElementById("wps");

        // $('#capabilitiesByClick').wpsCall({
        //     url : sel.options[sel.selectedIndex].text,
        //     requestType : GET_CAPABILITIES_TYPE
        // });
        $('#capabilitiesByClick').wpsCall({
            url : this.m_sSelectedWps,
            requestType : GET_CAPABILITIES_TYPE
        });
    };

    /**
     * onChangeWPS
     */

    WpsController.prototype.onChangeWPS = function()
    {
        // var sel = document.getElementById("wps");
        // getCapabilities(sel.options[sel.selectedIndex].text);
        this.m_sSelectedWps = this.m_sSelectedWps.trim();
        this.m_sSelectedWps = this.m_sSelectedWps.replace("http://","");
        // getCapabilities(this.m_oConstantService.getWPSPROXY() + this.m_sSelectedWps);
        getCapabilities(this.m_sSelectedWps);
        this.clickOnShowCapabilities();
    };

    /**
     * setUpProxy
     */
    WpsController.prototype.setUpProxy = function()
    {
        var oSetupProxy= {
            proxy : {
                url : 'http://178.22.66.96/corsproxy/',
                type: 'parameter',
            }
        }
        jQuery.wpsSetup(oSetupProxy);
    };

    WpsController.prototype.getWPSList = function()
    {
        var oController = this;
        this.m_oSnapOperationService.getWPSList()
            .then(function(data,status)
            {
                oController.callbackWPSList(oController,data.data,status)
                // if( (utilsIsObjectNullOrUndefined(data) === false) && (status === 200))
                // {
                //     oController.m_asWPSListURL = data;
                // }
                // else
                // {
                //     utilsVexDialogAlertTop("GURU MEDITATION<br>WAS IMPOSSIBLE GET WPS LIST.");
                //
                // }
            },function(data,status)
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>WAS IMPOSSIBLE GET WPS LIST.");
            });
    };

    WpsController.prototype.callbackWPSList = function(oController,data,status)
    {
        if( (utilsIsObjectNullOrUndefined(data) === false) && (status === 200))
        {
            var iNumberOfWPSUrl = data.length;
            for(var iIndexWPSUrl = 0 ; iIndexWPSUrl < iNumberOfWPSUrl; iIndexWPSUrl++)
            {
                oController.m_asWPSListURL.push(data[iIndexWPSUrl].address)
            }
            // oController.m_asWPSListURL = data;
        }
        else
        {
            utilsVexDialogAlertTop("GURU MEDITATION<br>WAS IMPOSSIBLE GET WPS LIST.");

        }
    }

    WpsController.$inject = [
        '$scope',
        'close',
        'extras',
        'ConstantsService',
        'SnapOperationService',

    ];
    return WpsController;
})();
window.WpsController = WpsController;
