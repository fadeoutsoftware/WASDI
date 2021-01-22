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
        this.m_oClickedProduct = this.m_oExtras.selectedProduct;
        this.m_oGetParametersOperationService = oGetParametersOperationService;
        this.m_bSaveSigmaGammaBeta = false;
        if(utilsIsObjectNullOrUndefined(this.m_aoProducts) == true)
        {
            this.m_aoProducts = [];
        }
        //Don't remove it!
        this.m_asProductsName = this.getProductsName();
        this.m_asSelectedProducts = [];

        if(utilsIsObjectNullOrUndefined(this.m_oClickedProduct) == true)
        {
            this.m_oClickedProduct = null;
        }
        else
        {
            this.m_asSelectedProducts.push(this.m_oClickedProduct.name);

            // if( (utilsIsObjectNullOrUndefined(this.m_oSelectedProduct.productFriendlyName)== false) && (utilsIsStrNullOrEmpty(this.m_oSelectedProduct.productFriendlyName)== false))
            //     this.m_sFileName_Operation = this.m_oSelectedProduct.name + "_RangeDopplerTerrainCorrection";
            //
            // if(utilsIsObjectNullOrUndefined(this.m_oSelectedProduct.bandsGroups.bands) == true)
            //     var iNumberOfBands = 0;
            // else
            //     var iNumberOfBands = this.m_oSelectedProduct.bandsGroups.bands.length;
            //
            // this.m_asSourceBands = [];
            // //load bands
            // for(var iIndexBand = 0; iIndexBand < iNumberOfBands ;iIndexBand++)
            // {
            //     if( utilsIsObjectNullOrUndefined(this.m_oSelectedProduct.bandsGroups.bands[iIndexBand]) == false )
            //         this.m_asSourceBands.push(this.m_oSelectedProduct.bandsGroups.bands[iIndexBand].name);
            // }
        }

        this.m_asSourceBandsSelected = [];
        this.m_oReturnValue={
            sourceFileName:"",
            destinationFileName:"",

        };

        $scope.close = function() {
            oClose("close", 500); // close, but give 500ms for bootstrap to animate
        };

        var oController = this;
        $scope.run = function() {
            var iNumberOfSelectedProducts = oController.m_asSelectedProducts.length;
            var aoReturnValue = [];
            // var bAreOkOptions = true;
            for(var iIndexSelectedProduct = 0; iIndexSelectedProduct < iNumberOfSelectedProducts ; iIndexSelectedProduct++)
            {
                var oProduct = oController.getProductByName(oController.m_asSelectedProducts[iIndexSelectedProduct]);

                var bAreOkOptions = true;
                if( (utilsIsObjectNullOrUndefined(oProduct.fileName) == true) && (utilsIsStrNullOrEmpty(oProduct.fileName) == true) )
                    bAreOkOptions = false;
                // if( (utilsIsObjectNullOrUndefined(oController.m_sFileName_Operation) == true) && (utilsIsStrNullOrEmpty(oController.m_sFileName_Operation) == true) )
                //     bAreOkOptions = false;
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

                var oRetValue = null;

                if(bAreOkOptions != false)
                {
                    oRetValue = {
                        options:{}
                    };

                    oRetValue.sourceFileName =  oProduct.fileName;
                    oRetValue.destinationFileName = oProduct.name + "_RangeDopplerTerrainCorrection";
                    // oController.m_oReturnValue.options.sourceBandNames = oController.m_asSourceBandsSelected;
                    oRetValue.options.sourceBandNames = oController.getSelectedBandsByProductName(oProduct.name, oController.m_asSourceBandsSelected);
                    oRetValue.options.demName = oController.m_sDigitalElevationModelSelected;
                    oRetValue.options.demResamplingMethod = oController.m_sDEMResamplingMethodSelected;
                    oRetValue.options.imgResamplingMethod = oController.m_sImageResamplingMethodSelected;
                    oRetValue.options.externalDEMFile =  oController.m_oReturnValue.options.externalDEMFile;
                    oRetValue.options.externalDEMNoDataValue =  oController.m_oReturnValue.options.externalDEMNoDataValue;
                    oRetValue.options.externalDEMApplyEGM =  oController.m_oReturnValue.options.externalDEMApplyEGM;
                    oRetValue.options.pixelSpacingInMeter =  oController.m_oReturnValue.options.pixelSpacingInMeter;
                    oRetValue.options.pixelSpacingInDegree =  oController.m_oReturnValue.options.pixelSpacingInDegree;
                    oRetValue.options.mapProjection =  oController.m_oReturnValue.options.mapProjection;
                    oRetValue.options.alignToStandardGrid =  oController.m_oReturnValue.options.alignToStandardGrid;
                    oRetValue.options.standardGridOriginX =  oController.m_oReturnValue.options.standardGridOriginX;
                    oRetValue.options.standardGridOriginY =  oController.m_oReturnValue.options.standardGridOriginY;
                    oRetValue.options.nodataValueAtSea =  oController.m_oReturnValue.options.nodataValueAtSea;
                    oRetValue.options.saveDEM =  oController.m_oReturnValue.options.saveDEM;
                    oRetValue.options.saveLatLon =  oController.m_oReturnValue.options.saveLatLon;
                    oRetValue.options.saveIncidenceAngleFromEllipsoid =  oController.m_oReturnValue.options.saveIncidenceAngleFromEllipsoid;
                    oRetValue.options.saveLocalIncidenceAngle =  oController.m_oReturnValue.options.saveLocalIncidenceAngle;
                    oRetValue.options.saveProjectedLocalIncidenceAngle =  oController.m_oReturnValue.options.saveProjectedLocalIncidenceAngle;
                    oRetValue.options.saveSelectedSourceBand =  oController.m_oReturnValue.options.saveSelectedSourceBand;
                    oRetValue.options.outputComplex =  oController.m_oReturnValue.options.outputComplex;
                    oRetValue.options.applyRadiometricNormalization =  oController.m_oReturnValue.options.applyRadiometricNormalization;
                    oRetValue.options.saveSigmaNought =  oController.m_oReturnValue.options.saveSigmaNought;
                    oRetValue.options.saveGammaNought =  oController.m_oReturnValue.options.saveGammaNought;
                    oRetValue.options.saveBetaNought =  oController.m_oReturnValue.options.saveBetaNought;
                    oRetValue.options.incidenceAngleForSigma0 =  oController.m_oReturnValue.options.incidenceAngleForSigma0;
                    oRetValue.options.incidenceAngleForGamma0 =  oController.m_oReturnValue.options.incidenceAngleForGamma0;
                    oRetValue.options.auxFile =  oController.m_oReturnValue.options.auxFile;
                    oRetValue.options.externalAuxFile =  oController.m_oReturnValue.options.externalAuxFile;
                }

                if (!utilsIsObjectNullOrUndefined(oRetValue)) {
                    aoReturnValue.push(oRetValue);
                }
            }

            oClose(aoReturnValue, 500); // close, but give 500ms for bootstrap to animate
        };

        this.m_oGetParametersOperationService.getParametersRangeDopplerTerrainCorrection()
            .then(function (data) {
                if(utilsIsObjectNullOrUndefined(data.data) == false)
                {
                    oController.m_oOptions = utilsProjectConvertJSONFromServerInOptions(data.data);
                    oController.m_oReturnValue.options = oController.m_oOptions;

                    oController.m_sLatestAuxiliaryFileSelected = oController.m_oReturnValue.options.auxFile;
                    oController.m_sDigitalElevationModelSelected = oController.m_oReturnValue.options.demName;
                    oController.m_sDEMResamplingMethodSelected = oController.m_oReturnValue.options.demResamplingMethod;
                    oController.m_sImageResamplingMethodSelected = oController.m_oReturnValue.options.imgResamplingMethod;
                    oController.m_sSaveSigmaSelected = oController.m_oReturnValue.options.incidenceAngleForSigma0;
                    oController.m_sSaveGammaSelected =oController.m_oReturnValue.options.incidenceAngleForGamma0;

                     oController.m_asLatestAuxiliaryFile = utilsProjectGetArrayOfValuesForParameterInOperation(data.data,"auxFile");
                     oController.m_asDigitalElevationModel = utilsProjectGetArrayOfValuesForParameterInOperation(data.data,"demName");
                     oController.m_asResamplingMethod = utilsProjectGetArrayOfValuesForParameterInOperation(data.data,"demResamplingMethod");
                     oController.m_asResamplingMethodImg = utilsProjectGetArrayOfValuesForParameterInOperation(data.data,"imgResamplingMethod");
                     oController.m_asSaveBandSigma = utilsProjectGetArrayOfValuesForParameterInOperation(data.data,"incidenceAngleForSigma0");
                     oController.m_asSaveBandGamma = utilsProjectGetArrayOfValuesForParameterInOperation(data.data,"incidenceAngleForGamma0");
                }
                else
                {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN GET PARAMETERS, THERE AREN'T DATA");
                }
            },function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN GET PARAMETERS");
        });

        $scope.$watch('m_oController.m_asSelectedProducts', function (newValue, oldValue, scope)
        {
            $scope.m_oController.m_asSourceBands = $scope.m_oController.getBandsFromSelectedProducts();
        },true);
    }


    RangeDopplerTerrainCorrectionController.prototype.nameIsUsed = function()
    {
        return utilsProjectCheckInDialogIfProductNameIsInUsed( this.m_sFileName_Operation  , this.m_aoProducts );
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
        };
        this.m_asSourceBandsSelected = [];
        var iNumberOfBands;
        if(utilsIsObjectNullOrUndefined(this.m_oSelectedProduct.bandsGroups.bands) == true)
            iNumberOfBands = 0;
        else
            iNumberOfBands = this.m_oSelectedProduct.bandsGroups.bands.length;

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
        if(utilsIsObjectNullOrUndefined(this.m_asSelectedProducts) == true)
            return true;
        return false;
    };


    RangeDopplerTerrainCorrectionController.prototype.getProductsName = function(){

        return utilsProjectGetProductsName(this.m_aoProducts);
    }

    /**
     *
     * @param sName
     * @returns {*}
     */
    RangeDopplerTerrainCorrectionController.prototype.getProductByName = function(sName){
        return utilsProjectGetProductByName(sName,this.m_aoProducts);
    };

    RangeDopplerTerrainCorrectionController.prototype.getBandsFromSelectedProducts = function()
    {
        return utilsProjectGetBandsFromSelectedProducts(this.m_asSelectedProducts,this.m_aoProducts);
    };

    RangeDopplerTerrainCorrectionController.prototype.getSelectedBandsByProductName = function(sProductName, asSelectedBands)
    {
        return utilsProjectGetSelectedBandsByProductName(sProductName, asSelectedBands);
    };
    RangeDopplerTerrainCorrectionController.$inject = [
        '$scope',
        'close',
        'extras',
        'GetParametersOperationService'

    ];
    return RangeDopplerTerrainCorrectionController;
})();
