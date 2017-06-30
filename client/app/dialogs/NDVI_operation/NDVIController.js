/**
 * Created by a.corrado on 04/04/2017.
 */

var NDVIController = (function() {

    function NDVIController($scope, oClose,oExtras,oGetParametersOperationService) {//,oExtras
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_aoProducts = this.m_oExtras.products;
        this.m_oSelectedProduct = this.m_oExtras.selectedProduct;
        this.m_oGetParametersOperationService = oGetParametersOperationService;
        if(utilsIsObjectNullOrUndefined(this.m_aoProducts) == true)
        {
            this.m_aoProducts = [];
        }
        if(utilsIsObjectNullOrUndefined(this.m_oSelectedProduct) == true)
        {
            this.m_oSelectedProduct = null;
        }
        else
        {
            if( (utilsIsObjectNullOrUndefined(this.m_oSelectedProduct.productFriendlyName)== false) && (utilsIsStrNullOrEmpty(this.m_oSelectedProduct.productFriendlyName)== false))
                this.m_sFileName_Operation = this.m_oSelectedProduct.name + "_NDVI";
            this.m_asSourceBands = [""];
            this.loadBands();
        }
        // this.m_asOrbitStateVectors = ["Sentinel Precise(Auto Download)","Sentinel Restituted(Auto Download)","DORIS preliminary POR(ENVISAT)"
        //     ,"DORIS Precise Vor(ENVISAT)(Auto Download)","DELFT Precise(ENVISAT,ERS1&2)(Auto Download)","PRARE Precise(ERS1&2)(Auto Download)"];
        this.m_sRedSourceBandSelected = "";
        this.m_sNIRSourceBandSelected = "";

        this.m_oReturnValue = {
            sourceFileName:"",
            destinationFileName:"",
            // options:{
            //     redFactor:"1.0",
            //     nirFactor:"1.0",
            //     redSourceBand:"",
            //     nirSourceBand:""
            // }
        };
        this.m_oOptions = {};
        //this.m_oOrbit = oExtras;
        //$scope.close = oClose;
        $scope.close = function() {
            oClose("close", 500); // close, but give 500ms for bootstrap to animate
        };

        var oController = this;
        $scope.run = function(oOptions) {
            //TODO CHECK OPTIONS
            var bAreOkOptions = true;

            if( (utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.nirFactor) == true) && (utilsIsANumber(oController.m_oReturnValue.options.nirFactor) == false) )
                bAreOkOptions = false;
            if( (utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.redFactor) == true) && (utilsIsANumber(oController.m_oReturnValue.options.redFactor) == false) )
                bAreOkOptions = false;

            if( (utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.redSourceBand) == true)  )//&& (utilsIsStrNullOrEmpty(oController.m_oReturnValue.options.redSourceBand) == true)
                bAreOkOptions = false;
            if( (utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.nirSourceBand) == true)  )//&& (utilsIsStrNullOrEmpty(oController.m_oReturnValue.options.nirSourceBand) == true)
                bAreOkOptions = false;
            if(utilsIsObjectNullOrUndefined(oController.m_sRedSourceBandSelected) == true)
                bAreOkOptions = false;
            if(utilsIsObjectNullOrUndefined(oController.m_sNIRSourceBandSelected) == true)
                bAreOkOptions = false;

            if(bAreOkOptions != false)
            {
                oController.m_oReturnValue.sourceFileName = oController.m_oSelectedProduct.fileName;
                oController.m_oReturnValue.destinationFileName = oController.m_sFileName_Operation;
                oController.m_oReturnValue.options.redSourceBand = oController.m_sRedSourceBandSelected;
                oController.m_oReturnValue.options.nirSourceBand = oController.m_sNIRSourceBandSelected;
            }
            else
            {
                oOptions = null;
            }
            oClose(oOptions, 500); // close, but give 500ms for bootstrap to animate
        };

        this.m_oGetParametersOperationService.getParametersNDVI()
            .success(function (data) {
                if(utilsIsObjectNullOrUndefined(data) == false)
                {
                    oController.m_oOptions = utilsProjectConvertJSONFromServerInOptions(data);
                    oController.m_oReturnValue.options = oController.m_oOptions;
                }
                else
                {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN GET PARAMETERS, THERE AREN'T DATA");
                }
            }).error(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN GET PARAMETERS");
        });

    }

    NDVIController.prototype.changeProduct = function(oNewSelectedProductInput)
    {
        if(utilsIsObjectNullOrUndefined(oNewSelectedProductInput) == true)
            return false;
        this.m_oSelectedProduct = oNewSelectedProductInput;
        this.m_sFileName_Operation = this.m_oSelectedProduct.name + "_NDVI.zip";

        this.m_oReturnValue={
            sourceFileName:"",
            destinationFileName:"",
            options: this.m_oOptions
            //     {
            //     redFactor:"1.0",
            //     nirFactor:"1.0",
            //     redSourceBand:"",
            //     nirSourceBand:""
            // }
        };
        this.loadBands();

        return true;
    };

    NDVIController.prototype.loadBands = function()
    {
        if(utilsIsObjectNullOrUndefined(this.m_oSelectedProduct.bandsGroups.bands) == true)
            var iNumberOfBands = 0;
        else
            var iNumberOfBands = this.m_oSelectedProduct.bandsGroups.bands.length;

        this.m_asSourceBands = [""];
        //load bands
        for(var iIndexBand = 0; iIndexBand < iNumberOfBands ;iIndexBand++)
        {
            if( utilsIsObjectNullOrUndefined(this.m_oSelectedProduct.bandsGroups.bands[iIndexBand]) == false )
                this.m_asSourceBands.push(this.m_oSelectedProduct.bandsGroups.bands[iIndexBand].name);
        }

    };

    NDVIController.prototype.selectedProductIsEmpty = function()
    {
        if(utilsIsObjectNullOrUndefined(this.m_oSelectedProduct) == true)
            return true;
        return false;
    };

    NDVIController.$inject = [
        '$scope',
        'close',
        'extras',
        'GetParametersOperationService'
    ];//'extras',
    return NDVIController;
})();
