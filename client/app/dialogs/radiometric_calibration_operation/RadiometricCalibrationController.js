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
        this.m_oClickedProduct = this.m_oExtras.selectedProduct;
        this.m_oGetParametersOperationService = oGetParametersOperationService;
        if(utilsIsObjectNullOrUndefined(this.m_aoProducts) == true)
        {
            this.m_aoProducts = [];
        }
        // Don't remove it!
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
            //     this.m_sFileName_Operation = this.m_oSelectedProduct.name + "_RadiometricCalibration";
            //
            // //laod band
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

        this.m_asAuxiliaryFile = ["Latest Auxiliary File","Product Auxiliary File","External Auxiliary File"];
        this.m_sSelectedAuxiliaryFile = this.m_asAuxiliaryFile[0];
        this.m_asSourceBandsSelected = [];
        this.m_oReturnValue = {
            sourceFileName:"",
            destinationFileName:"",

        };

        $scope.close = function() {
            oClose("close", 500); // close, but give 500ms for bootstrap to animate
        };

        var oController = this;

        $scope.run = function() {
            //TODO CHECK OPTIONS
            var iNumberOfSelectedProducts = oController.m_asSelectedProducts.length;
            var aoReturnValue = [];
            var bAreOkOptions = true;
            for(var iIndexSelectedProduct = 0; iIndexSelectedProduct < iNumberOfSelectedProducts ; iIndexSelectedProduct++)
            {
                var oProduct = oController.getProductByName(oController.m_asSelectedProducts[iIndexSelectedProduct]);

                if( (utilsIsObjectNullOrUndefined(oProduct.fileName) == true) && (utilsIsStrNullOrEmpty(oProduct.fileName) == true) )
                    bAreOkOptions = false;
                // if( (utilsIsObjectNullOrUndefined(oController.m_sFileName_Operation) == true) && (utilsIsStrNullOrEmpty(oController.m_sFileName_Operation) == true) )
                //     bAreOkOptions = false;
                // if( (utilsIsObjectNullOrUndefined(oController. m_sSelectedAuxiliaryFile) == true) && (utilsIsStrNullOrEmpty(oController. m_sSelectedAuxiliaryFile) == true) )
                //     bAreOkOptions = false;

                // if( (utilsIsObjectNullOrUndefined(oController.m_asSourceBandsSelected) == true) )
                //     bAreOkOptions = false;
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

                var oRetValue = null;

                if(bAreOkOptions != false)
                {
                    oRetValue = {
                        options:{}
                    };

                    oRetValue.sourceFileName = oProduct.fileName;
                    oRetValue.destinationFileName =  oProduct.name + "_RadiometricCalibration";
                    // oController.m_oReturnValue.options.sourceBandNames = oController.m_asSourceBandsSelected;
                    oRetValue.options.sourceBandNames = oController.getSelectedBandsByProductName(oProduct.name, oController.m_asSourceBandsSelected);
                    oRetValue.options.selectedPolarisations = [];
                    oRetValue.options.auxFile = oController.m_sSelectedAuxiliaryFile;
                    oRetValue.options.createBetaBand = oController.m_oReturnValue.options.createBetaBand;
                    oRetValue.options.createGammaBand = oController.m_oReturnValue.options.createGammaBand;
                    oRetValue.options.externalAuxFile = oController.m_oReturnValue.options.externalAuxFile;
                    oRetValue.options.outputBetaBand = oController.m_oReturnValue.options.outputBetaBand;
                    oRetValue.options.outputGammaBand = oController.m_oReturnValue.options.outputGammaBand;
                    oRetValue.options.outputImageInComplex = oController.m_oReturnValue.options.outputImageInComplex;
                    oRetValue.options.outputImageScaleInDb = oController.m_oReturnValue.options.outputImageScaleInDb;
                    oRetValue.options.outputSigmaBand = oController.m_oReturnValue.options.outputSigmaBand;
                    // oController.m_oReturnValue.options.externalAuxFile = "";
                }

                if (!utilsIsObjectNullOrUndefined(oRetValue)) {
                    aoReturnValue.push(oRetValue);
                }
            }


            oClose(aoReturnValue, 500); // close, but give 500ms for bootstrap to animate
        };

        this.m_oGetParametersOperationService.getParametersRadiometricCalibration()
            .then(function (data) {
                if(utilsIsObjectNullOrUndefined(data.data) == false)
                {
                    oController.m_oOptions = utilsProjectConvertJSONFromServerInOptions(data.data);
                    oController.m_oReturnValue.options = oController.m_oOptions;
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


    RadiometricCalibrationController.prototype.nameIsUsed = function()
    {
        return utilsProjectCheckInDialogIfProductNameIsInUsed( this.m_sFileName_Operation  , this.m_aoProducts );
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

        };


        this.m_asSourceBandsSelected = [];
        //load band
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


    /**
     *
     * @returns {boolean}
     */
    RadiometricCalibrationController.prototype.selectedProductIsEmpty = function()
    {
        if(utilsIsObjectNullOrUndefined(this.m_asSelectedProducts) == true)
            return true;
        return false;
    };

    /**
     *
     * @returns {*}
     */
    RadiometricCalibrationController.prototype.getProductsName = function(){

        return utilsProjectGetProductsName(this.m_aoProducts);
    }

    /**
     *
     * @param sName
     * @returns {*}
     */
    RadiometricCalibrationController.prototype.getProductByName = function(sName){
        return utilsProjectGetProductByName(sName,this.m_aoProducts);
    };

    RadiometricCalibrationController.prototype.getBandsFromSelectedProducts = function()
    {
        return utilsProjectGetBandsFromSelectedProducts(this.m_asSelectedProducts,this.m_aoProducts);
    };

    RadiometricCalibrationController.prototype.getSelectedBandsByProductName = function(sProductName, asSelectedBands)
    {
        return utilsProjectGetSelectedBandsByProductName(sProductName, asSelectedBands);
    };

    RadiometricCalibrationController.$inject = [
        '$scope',
        'close',
        'extras',
        'GetParametersOperationService'
    ];
    return RadiometricCalibrationController;
})();
