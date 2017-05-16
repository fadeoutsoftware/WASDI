/**
 * Created by a.corrado on 03/04/2017.
 */


var RadiometricCalibrationController = (function() {

    function RadiometricCalibrationController($scope, oClose,oExtras,oGetParametersOperationService) {
        //MEMBERS
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
                this.m_sFileName_Operation = this.m_oSelectedProduct.name + "_RadiometricCalibration.zip";

            //laod band
            if(utilsIsObjectNullOrUndefined(this.m_oSelectedProduct.bandsGroups.bands) == true)
                var iNumberOfBands = 0;
            else
                var iNumberOfBands = this.m_oSelectedProduct.bandsGroups.bands.length;

            this.m_asSourceBands = [];
            //load bands
            for(var iIndexBand = 0; iIndexBand < iNumberOfBands ;iIndexBand++)
            {
                if( utilsIsObjectNullOrUndefined(this.m_oSelectedProduct.bandsGroups.bands[iIndexBand]) == false )
                    this.m_asSourceBands.push(this.m_oSelectedProduct.bandsGroups.bands[iIndexBand].name);
            }


        }
        // this.m_oTabOpen = "tab1";
        // this.m_asTypeOfData = ["GeoTIFF","NetCDF-BEAM","NetCDF4-CF","NetCDF-CF","CSV","Gamma","Generic Binary","GeoTIFF+XML",
        //     "NetCDF4-BEAM","BEAM-DIMAP","ENVI","PolSARPro","Snaphu","JP2","JPG","PNG","BMP","GIF","BTF","GeoTIFF-BIGTIFF","HDF5"];
        // this.m_asOrbitStateVectors = ["Sentinel Precise(Auto Download)","Sentinel Restituted(Auto Download)","DORIS preliminary POR(ENVISAT)"
        //     ,"DORIS Precise Vor(ENVISAT)(Auto Download)","DELFT Precise(ENVISAT,ERS1&2)(Auto Download)","PRARE Precise(ERS1&2)(Auto Download)"];
        // this.m_sSelectedExtension = this.m_asTypeOfData[0];

        this.m_asAuxiliaryFile = ["Latest Auxiliary File","Product Auxiliary File","External Auxiliary File"];
        this.m_sSelectedAuxiliaryFile = this.m_asAuxiliaryFile[0];
        // this.m_asSourceBands =  ["Band1","Band2","Band3"];

        this.m_asSourceBandsSelected = [];

        this.m_oReturnValue = {
            sourceFileName:"",
            destinationFileName:"",
            // options:{
            //     sourceBandNames:"",
            //     auxFile:"Latest Auxiliary File",
            //     // externalAuxFile:"",
            //     outputImageInComplex:false,
            //     outputImageScaleInDb:false,
            //     createGammaBand:false,
            //     createBetaBand:false,
            //     // selectedPolarisations:"",
            //     // outputSigmaBand:true,
            //     // outputGammaBand:false,
            //     // outputBetaBand:false,
            //
            // }
        };
        //this.m_oOrbit = oExtras;
        //$scope.close = oClose;
        $scope.close = function() {
            oClose("close", 500); // close, but give 500ms for bootstrap to animate
        };

        var oController = this;
        $scope.run = function(oOptions) {
            //TODO CHECK OPTIONS
            var bAreOkOptions = true;

            if( (utilsIsObjectNullOrUndefined(oController.m_oSelectedProduct.fileName) == true) && (utilsIsStrNullOrEmpty(oController.m_oSelectedProduct.fileName) == true) )
                bAreOkOptions = false;
            if( (utilsIsObjectNullOrUndefined(oController.m_sFileName_Operation) == true) && (utilsIsStrNullOrEmpty(oController.m_sFileName_Operation) == true) )
                bAreOkOptions = false;
            // if( (utilsIsObjectNullOrUndefined(oController. m_sSelectedAuxiliaryFile) == true) && (utilsIsStrNullOrEmpty(oController. m_sSelectedAuxiliaryFile) == true) )
            //     bAreOkOptions = false;

            if( (utilsIsObjectNullOrUndefined(oController.m_asSourceBandsSelected) == true) )
                bAreOkOptions = false;
            // if( utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.continueOnFail) == true )
            //     bAreOkOptions = false;
            if( utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.externalAuxFile) == true  && (utilsIsStrNullOrEmpty(oController.m_oReturnValue.options.externalAuxFile) == false) )
                bAreOkOptions = false;
            if( utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.outputImageInComplex) == true )
                bAreOkOptions = false;
            if( utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.outputImageScaleInDb) == true )
                bAreOkOptions = false;

            if( (utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.createGammaBand) == true) )
                bAreOkOptions = false;
            if( utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.createBetaBand) == true )
                bAreOkOptions = false;
            // if( (utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.selectedPolarisations) == true ) && (utilsIsStrNullOrEmpty(oController.m_oReturnValue.options.selectedPolarisations) == false))
            //     bAreOkOptions = false;
            // if( utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.outputSigmaBand) == true )
            //     bAreOkOptions = false;
            // if( utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.outputGammaBand) == true )
            //     bAreOkOptions = false;
            // if( utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.outputBetaBand) == true )
            //     bAreOkOptions = false;

            if(bAreOkOptions != false)
            {
                oController.m_oReturnValue.sourceFileName = oController.m_oSelectedProduct.fileName;
                oController.m_oReturnValue.destinationFileName = oController.m_sFileName_Operation;
                oController.m_oReturnValue.options.sourceBandNames = oController.m_asSourceBandsSelected;
                oController.m_oReturnValue.options.selectedPolarisations = [];
                oController.m_oReturnValue.options.auxFile = oController.m_sSelectedAuxiliaryFile;
                // oController.m_oReturnValue.options.externalAuxFile = "";
            }
            else
            {
                oOptions = null;
            }
            oClose(oOptions, 500); // close, but give 500ms for bootstrap to animate
        };

        this.m_oGetParametersOperationService.getParametersRadiometricCalibration()
            .success(function (data) {
                if(utilsIsObjectNullOrUndefined(data) == false)
                {
                    oController.m_oOptions = utilsProjectConvertJSONFromServerInOptions(data);
                    oController.m_oReturnValue.options = oController.m_oOptions;
                }
                else
                {
                    utilsVexDialogAlertTop("Error in get parameters, there aren't data");
                }
            }).error(function (error) {
            utilsVexDialogAlertTop("Error in get parameters");
        });
    };

    RadiometricCalibrationController.prototype.changeProduct = function(oNewSelectedProductInput)
    {
        if(utilsIsObjectNullOrUndefined(oNewSelectedProductInput) == true)
            return false;
        this.m_oSelectedProduct = oNewSelectedProductInput;
        this.m_sFileName_Operation = this.m_oSelectedProduct.name + "_RadiometricCalibration.zip";

        this.m_oReturnValue = {
            sourceFileName:"",
            destinationFileName:"",
            options:this.m_oOptions

            // options:{
            //     sourceBandNames:"",
            //     auxFile:"Latest Auxiliary File",
            //     // externalAuxFile:"",
            //     outputImageInComplex:false,
            //     outputImageScaleInDb:false,
            //     createGammaBand:false,
            //     createBetaBand:false,
            //     // selectedPolarisations:"",
            //     // outputSigmaBand:true,
            //     // outputGammaBand:false,
            //     // outputBetaBand:false,
            //
            // }
        };


        this.m_asSourceBandsSelected = [];
        //load band
        if(utilsIsObjectNullOrUndefined(this.m_oSelectedProduct.bandsGroups.bands) == true)
            var iNumberOfBands = 0;
        else
            var iNumberOfBands = this.m_oSelectedProduct.bandsGroups.bands.length;

        this.m_asSourceBands = [];
        //load bands
        for(var iIndexBand = 0; iIndexBand < iNumberOfBands ;iIndexBand++)
        {
            if( utilsIsObjectNullOrUndefined(this.m_oSelectedProduct.bandsGroups.bands[iIndexBand]) == false )
                this.m_asSourceBands.push(this.m_oSelectedProduct.bandsGroups.bands[iIndexBand].name);
        }


        return true;
    };

    // RadiometricCalibrationController.prototype.isSelectedExternalAuxiliaryFile = function()
    // {
    //     if(this.m_sSelectedAuxiliaryFile == "External Auxiliary File")
    //         return true;
    //
    //     return false;
    // }

    RadiometricCalibrationController.prototype.selectedProductIsEmpty = function()
    {
        if(utilsIsObjectNullOrUndefined(this.m_oSelectedProduct) == true)
            return true;
        return false;
    };
    RadiometricCalibrationController.$inject = [
        '$scope',
        'close',
        'extras',
        'GetParametersOperationService'
    ];
    return RadiometricCalibrationController;
})();
