/**
 * Created by a.corrado on 06/04/2017.
 */

var RangeDopplerTerrainCorrectionController = (function() {

    function RangeDopplerTerrainCorrectionController($scope, oClose,oExtras,oGetParametersOperationService) {
        //MEMBERS
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_aoProducts = this.m_oExtras.products;
        this.m_oSelectedProduct = this.m_oExtras.selectedProduct;
        this.m_oGetParametersOperationService = oGetParametersOperationService;
        this.m_bSaveSigmaGammaBeta = false;
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
                this.m_sFileName_Operation = this.m_oSelectedProduct.name + "_RangeDopplerTerrainCorrection";

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




        this.m_asSourceBandsSelected = [];

        this.m_oReturnValue={
            sourceFileName:"",
            destinationFileName:"",

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
            if( (utilsIsObjectNullOrUndefined(oController.m_asSourceBandsSelected) == true) )
                bAreOkOptions = false;
            if( (utilsIsObjectNullOrUndefined(oController.m_sDigitalElevationModelSelected) == true) && (utilsIsStrNullOrEmpty(oController.m_sDigitalElevationModelSelected) == true) )
                bAreOkOptions = false;
            if( (utilsIsObjectNullOrUndefined(oController.m_sDEMResamplingMethodSelected) == true) && (utilsIsStrNullOrEmpty(oController.m_sDEMResamplingMethodSelected) == true) )
                bAreOkOptions = false;
            if( (utilsIsObjectNullOrUndefined(oController.m_sImageResamplingMethodSelected) == true) && (utilsIsStrNullOrEmpty(oController.m_sImageResamplingMethodSelected) == true) )
                bAreOkOptions = false;
            if( (utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.pixelSpacingInMeter) == true) && (utilsIsANumber(oController.m_oReturnValue.options.pixelSpacingInMeter) == false) )
                bAreOkOptions = false;
            if( (utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.pixelSpacingInDegree) == true) && (utilsIsANumber(oController.m_oReturnValue.options.pixelSpacingInDegree) == false) )
                bAreOkOptions = false;

            if( utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.nodataValueAtSea) == true )
                bAreOkOptions = false;
            if( utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.saveDEM) == true )
                bAreOkOptions = false;
            if( utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.saveLatLon) == true )
                bAreOkOptions = false;
            if( utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.saveIncidenceAngleFromEllipsoid) == true )
                bAreOkOptions = false;
            if( utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.saveLocalIncidenceAngle) == true )
                bAreOkOptions = false;
            if( utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.saveProjectedLocalIncidenceAngle) == true )
                bAreOkOptions = false;
            if( utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.saveSelectedSourceBand) == true )
                bAreOkOptions = false;
            if( utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.outputComplex) == true )
                bAreOkOptions = false;
            if( utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.applyRadiometricNormalization) == true )
                bAreOkOptions = false;
            if( utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.saveSigmaNought) == true )
                bAreOkOptions = false;
            if( utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.saveGammaNought) == true )
                bAreOkOptions = false;
            if( utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.saveBetaNought) == true )
                bAreOkOptions = false;

            if(bAreOkOptions != false)
            {
                oController.m_oReturnValue.sourceFileName = oController.m_oSelectedProduct.fileName;
                oController.m_oReturnValue.destinationFileName = oController.m_sFileName_Operation;
                oController.m_oReturnValue.options.sourceBandNames = oController.m_asSourceBandsSelected;
                oController.m_oReturnValue.options.demName = oController.m_sDigitalElevationModelSelected;
                oController.m_oReturnValue.options.demResamplingMethod = oController.m_sDEMResamplingMethodSelected;
                oController.m_oReturnValue.options.imgResamplingMethod = oController.m_sImageResamplingMethodSelected;

            }
            else
            {
                oOptions = null;
            }
            oClose(oOptions, 500); // close, but give 500ms for bootstrap to animate
        };

        this.m_oGetParametersOperationService.getParametersRangeDopplerTerrainCorrection()
            .success(function (data) {
                if(utilsIsObjectNullOrUndefined(data) == false)
                {
                    oController.m_oOptions = utilsProjectConvertJSONFromServerInOptions(data);
                    oController.m_oReturnValue.options = oController.m_oOptions;

                    oController.m_sLatestAuxiliaryFileSelected = oController.m_oReturnValue.options.auxFile;
                    oController.m_sDigitalElevationModelSelected = oController.m_oReturnValue.options.demName;
                    oController.m_sDEMResamplingMethodSelected = oController.m_oReturnValue.options.demResamplingMethod;
                    oController.m_sImageResamplingMethodSelected = oController.m_oReturnValue.options.imgResamplingMethod;
                    oController.m_sSaveSigmaSelected = oController.m_oReturnValue.options.incidenceAngleForSigma0;
                    oController.m_sSaveGammaSelected =oController.m_oReturnValue.options.incidenceAngleForGamma0;

                     oController.m_asLatestAuxiliaryFile = utilsProjectGetArrayOfValuesForParameterInOperation(data,"auxFile");
                     oController.m_asDigitalElevationModel = utilsProjectGetArrayOfValuesForParameterInOperation(data,"demName");
                     oController.m_asResamplingMethod = utilsProjectGetArrayOfValuesForParameterInOperation(data,"demResamplingMethod");
                     oController.m_asResamplingMethodImg = utilsProjectGetArrayOfValuesForParameterInOperation(data,"imgResamplingMethod");
                     oController.m_asSaveBandSigma = utilsProjectGetArrayOfValuesForParameterInOperation(data,"incidenceAngleForSigma0");
                     oController.m_asSaveBandGamma = utilsProjectGetArrayOfValuesForParameterInOperation(data,"incidenceAngleForGamma0");
                }
                else
                {
                    utilsVexDialogAlertTop("Error in get parameters, there aren't data");
                }
            }).error(function (error) {
            utilsVexDialogAlertTop("Error in get parameters");
        });
    };

    RangeDopplerTerrainCorrectionController.prototype.changeProduct = function(oNewSelectedProductInput)
    {
        if(utilsIsObjectNullOrUndefined(oNewSelectedProductInput) == true)
            return false;
        this.m_oSelectedProduct = oNewSelectedProductInput;
        this.m_sFileName_Operation = this.m_oSelectedProduct.name + "_RangeDopplerTerrainCorrection.zip";
        this.m_oReturnValue={
            sourceFileName:"",
            destinationFileName:"",
            options:this.m_oOptions
            // options:{
            //     sourceBandNames:"",
            //     demName:this.m_asDigitalElevationModel[6],
            //     // externalDEMFile:"",
            //     // externalDEMNoDataValue:0,
            //     // externalDEMApplyEGM:true,
            //     demResamplingMethod:this.m_asResamplingMethod[0],
            //     imgResamplingMethod:this.m_asResamplingMethod[0],
            //     pixelSpacingInMeter:0,
            //     pixelSpacingInDegree:0,
            //     // mapProjection:"WGS84(DD)",
            //     nodataValueAtSea:true,
            //     saveDEM:false,
            //     saveLatLon:false,
            //     saveIncidenceAngleFromEllipsoid:false,
            //     saveLocalIncidenceAngle:false,
            //     saveProjectedLocalIncidenceAngle:false,
            //     saveSelectedSourceBand:true,
            //     outputComplex:false,
            //
            //     applyRadiometricNormalization:false,
            //     saveSigmaNought:false,
            //     saveGammaNought:false,
            //     saveBetaNought:false,
            //     incidenceAngleForSigma0:"",
            //     incidenceAngleForGamma0:"",
            //     auxFile:"",
            //     externalAuxFile:""
            // }
        };
        this.m_asSourceBandsSelected = [];

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

    RangeDopplerTerrainCorrectionController.prototype.selectedProductIsEmpty = function()
    {
        if(utilsIsObjectNullOrUndefined(this.m_oSelectedProduct) == true)
            return true;
        return false;
    };

    RangeDopplerTerrainCorrectionController.$inject = [
        '$scope',
        'close',
        'extras',
        'GetParametersOperationService'

    ];
    return RangeDopplerTerrainCorrectionController;
})();
