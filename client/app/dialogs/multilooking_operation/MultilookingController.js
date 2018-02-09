/**
 * Created by a.corrado on 04/04/2017.
 */

var MultilookingController = (function() {

    function MultilookingController($scope, oClose,oExtras,oGetParametersOperationService,$q,$timeout) {//,oExtras
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_aoProducts = this.m_oExtras.products;
        this.m_oClickedProduct = this.m_oExtras.selectedProduct;
        this.m_asSourceBands = [];
        this.m_oGetParametersOperationService = oGetParametersOperationService;
        this.m_iSquarePixel = 10;
        this.m_asSelectedProducts = [];
        this.m_asSourceBandsSelected = [];
        this.m_oReturnValue={
            sourceFileName:"",
            destinationFileName:"",
        };
        this.m_oOptions={};

        if(utilsIsObjectNullOrUndefined(this.m_aoProducts) == true)
        {
            this.m_aoProducts = [];
        }
        // Don't remove it!
        this.m_asProductsName = this.getProductsName();

        if(utilsIsObjectNullOrUndefined(this.m_oClickedProduct) == true)
        {
            this.m_oClickedProduct = null;
        }
        else
        {
            this.m_asSelectedProducts.push(this.m_oClickedProduct.name);
        }


        $scope.close = function() {
            // close, but give 500ms for bootstrap to animate
            oClose("close", 500);
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

                if( (utilsIsObjectNullOrUndefined(oProduct) == true) )
                    bAreOkOptions = false;
                if( (utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.nRgLooks) == true) && (utilsIsANumber(oController.m_oReturnValue.options.nRgLooks) == false) )
                    bAreOkOptions = false;
                if( (utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.nAzLooks) == true) && (utilsIsANumber(oController.m_oReturnValue.options.nAzLooks) == false) )
                    bAreOkOptions = false;
                if( (utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.outputIntensity) == true)  )
                    bAreOkOptions = false;
                if( (utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.grSquarePixel) == true)  )
                    bAreOkOptions = false;

                if(bAreOkOptions != false)
                {
                    oController.m_oReturnValue.sourceFileName = oProduct.fileName;
                    oController.m_oReturnValue.destinationFileName = oProduct.name + "_Multilooking";
                    // oController.m_oReturnValue.options.sourceBandNames = oController.m_asSourceBandsSelected;
                    oController.m_oReturnValue.options.sourceBandNames = oController.getSelectedBandsByProductName(oProduct.name, oController.m_asSourceBandsSelected);
                }
                else
                {
                    oController.m_oReturnValue = null;
                }

                aoReturnValue.push(oController.m_oReturnValue);

            }

            oClose(aoReturnValue, 500); // close, but give 500ms for bootstrap to animate
        };

        this.m_oGetParametersOperationService.getParametersMultilooking()
            .success(function (data) {
                if(utilsIsObjectNullOrUndefined(data) == false)
                {
                    oController.m_oOptions = utilsProjectConvertJSONFromServerInOptions(data);
                    oController.m_oReturnValue.options = oController.m_oOptions;
                    // oController.m_oScope.$apply();
                }
                else
                {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN GET PARAMETERS, THERE AREN'T DATA");
                }
            }).error(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN GET PARAMETERS");
        });

        $scope.$watch('m_oController.m_asSelectedProducts', function (newValue, oldValue, scope)
        {
            $scope.m_oController.m_asSourceBands = $scope.m_oController.getBandsFromSelectedProducts();
        },true);
    };

    MultilookingController.prototype.nameIsUsed = function()
    {
        return utilsProjectCheckInDialogIfProductNameIsInUsed( this.m_sFileName_Operation  , this.m_aoProducts );
    };

    MultilookingController.prototype.changeProduct = function(oNewSelectedProductInput)
    {
        if(utilsIsObjectNullOrUndefined(oNewSelectedProductInput) == true)
            return false;
        this.m_oSelectedProduct = oNewSelectedProductInput;
        this.m_sFileName_Operation = this.m_oSelectedProduct.name + "_Multilooking.zip";

        this.m_oReturnValue={
            sourceFileName:"",
            destinationFileName:"",
            options:this.m_oOptions
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

    MultilookingController.prototype.changeInputAutomatically = function()
    {
        if( this.m_oReturnValue.options.grSquarePixel == true )
        {
            this.m_oReturnValue.options.nAzLooks = this.m_oReturnValue.options.nRgLooks;
            this.m_iSquarePixel =  this.m_oReturnValue.options.nRgLooks * 10;
        }
        else
        {
            this.m_iSquarePixel = "";
        }
    };

    MultilookingController.prototype.clickOnCheckBoxGrSquarePixel = function()
    {
        if(this.m_oReturnValue.options.grSquarePixel == true)
        {
            this.m_oReturnValue.options.outputIntensity = false;
            this.changeInputAutomatically();
            // this.m_iSquarePixel =  this.m_oReturnValue.options.nRgLooks * 10;
        }

        else
            this.m_oReturnValue.options.outputIntensity = true;
    }
    MultilookingController.prototype.clickOnCheckBoxOutputIntensity = function()
    {
        if(this.m_oReturnValue.options.outputIntensity == true)
        {
            this.m_oReturnValue.options.grSquarePixel = false;
            this.m_iSquarePixel = '';
        }
        else
        {
            this.m_oReturnValue.options.grSquarePixel = true;
            this.changeInputAutomatically();
            // this.m_iSquarePixel =  this.m_oReturnValue.options.nRgLooks * 10;
        }

    }

    MultilookingController.prototype.selectedProductIsEmpty = function()
    {
        if(utilsIsObjectNullOrUndefined(this.m_asSelectedProducts) == true)
            return true;
        return false;
    }

    /**
     *
     * @returns {*}
     */

    MultilookingController.prototype.getProductsName = function(){

        return utilsProjectGetProductsName(this.m_aoProducts);
    }

    /**
     *
     * @param sName
     * @returns {*}
     */
    MultilookingController.prototype.getProductByName = function(sName){
        return utilsProjectGetProductByName(sName,this.m_aoProducts);
    };

    MultilookingController.prototype.getBandsFromSelectedProducts = function()
    {
        return utilsProjectGetBandsFromSelectedProducts(this.m_asSelectedProducts,this.m_aoProducts);
    };

    MultilookingController.prototype.getSelectedBandsByProductName = function(sProductName, asSelectedBands)
    {
        return utilsProjectGetSelectedBandsByProductName(sProductName, asSelectedBands);
    };

    MultilookingController.$inject = [
        '$scope',
        'close',
        'extras',
        'GetParametersOperationService',
        '$q',
        '$timeout'

    ];//'extras',
    return MultilookingController;
})();
