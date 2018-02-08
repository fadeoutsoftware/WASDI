/**
 * Created by a.corrado on 04/04/2017.
 */

var MultilookingController = (function() {

    function MultilookingController($scope, oClose,oExtras,oGetParametersOperationService,$q,$timeout) {//,oExtras
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_aoProducts = this.m_oExtras.products;
        this.m_oSelectedProduct = this.m_oExtras.selectedProduct;
        this.m_asSourceBands = [];
        this.m_oGetParametersOperationService = oGetParametersOperationService;
        this.m_iSquarePixel = 10;
        this.m_asSelectedProducts = [];
        this.m_asSourceBandsSelected = [];
        if(utilsIsObjectNullOrUndefined(this.m_aoProducts) == true)
        {
            this.m_aoProducts = [];
        }
        this.m_asProductsName = this.getProductsName();

        if(utilsIsObjectNullOrUndefined(this.m_oSelectedProduct) == true)
        {
            this.m_oSelectedProduct = null;
        }
        else
        {
            // if( (utilsIsObjectNullOrUndefined(this.m_oSelectedProduct.productFriendlyName)== false) && (utilsIsStrNullOrEmpty(this.m_oSelectedProduct.productFriendlyName)== false))
            //     this.m_sFileName_Operation = this.m_oSelectedProduct.name + "_Multilooking";
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




        this.m_oReturnValue={
            sourceFileName:"",
            destinationFileName:"",
            // options:{
            //     sourceBandNames:"",
            //     nRgLooks:1,
            //     nAzLooks:1,
            //     outputIntensity:false,//bool
            //     grSquarePixel:true//bool
            //
            // }
        };
        this.m_oOptions={};

        //this.m_oOrbit = oExtras;
        //$scope.close = oClose;
        $scope.close = function() {

            oClose("close", 500); // close, but give 500ms for bootstrap to animate
        };

        var oController = this;
        // $scope.run = function(oOptions) {
        //     //TODO CHECK OPTIONS
        //     var bAreOkOptions = true;
        //
        //     // check if the name is used by other products
        //     if(oController.nameIsUsed())
        //     {
        //         // oController.m_sMessage = "Error";
        //         return;
        //     }
        //     if( (utilsIsObjectNullOrUndefined(oController.m_asSourceBandsSelected) == true) )
        //         bAreOkOptions = false;
        //     if( (utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.nRgLooks) == true) && (utilsIsANumber(oController.m_oReturnValue.options.nRgLooks) == false) )
        //         bAreOkOptions = false;
        //     if( (utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.nAzLooks) == true) && (utilsIsANumber(oController.m_oReturnValue.options.nAzLooks) == false) )
        //         bAreOkOptions = false;
        //     if( (utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.outputIntensity) == true)  )
        //         bAreOkOptions = false;
        //     if( (utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.grSquarePixel) == true)  )
        //         bAreOkOptions = false;
        //
        //     if(bAreOkOptions != false)
        //     {
        //         oController.m_oReturnValue.sourceFileName = oController.m_oSelectedProduct.fileName;
        //         oController.m_oReturnValue.destinationFileName = oController.m_sFileName_Operation;
        //         oController.m_oReturnValue.options.sourceBandNames = oController.m_asSourceBandsSelected;
        //     }
        //     else
        //     {
        //         oOptions = null;
        //     }
        //     oClose(oOptions, 500); // close, but give 500ms for bootstrap to animate
        // };
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
                    oController.m_oReturnValue.destinationFileName = oProduct.name + "_ApplyOrbit";
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

            // if(!utilsIsObjectNullOrUndefined($scope.m_oController.m_asSelectedProducts))
            // {
                $scope.m_oController.m_asSourceBands = $scope.m_oController.getBandsFromSelectedProducts();
            // }
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
            //     {
            //     sourceBandNames:"",
            //     nRgLooks:1,
            //     nAzLooks:1,
            //     outputIntensity:false,//bool
            //     grSquarePixel:false//bool
            //
            // }
        };

         // angular.element(document).find("#multiselectMultilooking").remove();
        // childScope.$destroy();
        // var myEl = angular.element( document.querySelector( '#divID' ) );
        // myEl.empty();
        // $('#test2').remove();

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

        // this.m_oScope.$apply();
        // angular.element(document).find("#test").append('<multiselect id="multiselectMultilooking" ng-model="m_oController.m_asSourceBandsSelected" options="m_oController.m_asSourceBands" show-select-all="true" show-unselect-all="true" show-search="true"></multiselect>');
        // this.m_oScope.$apply();
         // angular.element(document).find("#test").append('Puppami la fava');
        // $('#test').append('<multiselect id="multiselectMultilooking" ng-model="m_oController.m_asSourceBandsSelected" options="m_oController.m_asSourceBands" show-select-all="true" show-unselect-all="true" show-search="true"></multiselect>');
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
        // if(utilsIsObjectNullOrUndefined(this.m_oSelectedProduct) == true)
        //     return true;
        return false;
    }

    /**
     *
     * @returns {*}
     */
    MultilookingController.prototype.getProductsName = function(){
        if(utilsIsObjectNullOrUndefined(this.m_aoProducts) === true)
            return null;
        var iNumberOfProducts = this.m_aoProducts.length;
        var asProductsName = [];
        for(var iIndexProduct = 0; iIndexProduct < iNumberOfProducts ; iIndexProduct++)
        {
            asProductsName.push(this.m_aoProducts[iIndexProduct].name);
        }
        return asProductsName;
    }

    /**
     *
     * @param sName
     * @returns {*}
     */
    MultilookingController.prototype.getProductByName = function(sName){
        if(utilsIsStrNullOrEmpty(sName) === true)
            return null;
        var iNumberOfProducts = this.m_aoProducts.length;
        ;
        for(var iIndexProduct = 0; iIndexProduct < iNumberOfProducts ; iIndexProduct++)
        {
            if( this.m_aoProducts[iIndexProduct].name === sName)
            {
                return this.m_aoProducts[iIndexProduct];
            }
        }
        return null;
    }

    MultilookingController.prototype.getBandsFromSelectedProducts = function()
    {
        if( utilsIsObjectNullOrUndefined(this.m_asSelectedProducts) === true)
            return null;
        var iNumberOfSelectedProducts = this.m_asSelectedProducts.length;
        var asProductsBands=[];
        for(var iIndexSelectedProduct = 0; iIndexSelectedProduct < iNumberOfSelectedProducts; iIndexSelectedProduct++)
        {
            var oProduct = this.getProductByName(this.m_asSelectedProducts[iIndexSelectedProduct]);
            var iNumberOfBands;

            if(utilsIsObjectNullOrUndefined(oProduct.bandsGroups.bands) === true)
                iNumberOfBands = 0;
            else
                iNumberOfBands = oProduct.bandsGroups.bands.length;

            for(var iIndexBand = 0; iIndexBand < iNumberOfBands; iIndexBand++)
            {
                if( utilsIsObjectNullOrUndefined(oProduct.bandsGroups.bands[iIndexBand]) === false )
                {
                    asProductsBands.push(oProduct.name + "_" + oProduct.bandsGroups.bands[iIndexBand].name);
                }
            }
        }
        return asProductsBands;
        // return ["test","secondo"];
    };

    MultilookingController.prototype.getSelectedBandsByProductName = function(sProductName, asSelectedBands)
    {
        if(utilsIsObjectNullOrUndefined(asSelectedBands) === true)
            return null;

        var iNumberOfSelectedBands = asSelectedBands.length;
        var asReturnBandsName = [];
        for( var iIndexSelectedBand = 0 ; iIndexSelectedBand < iNumberOfSelectedBands; iIndexSelectedBand++ )
        {
            //check if the asSelectedBands[iIndexSelectedBand] is a sProductName band
            if(utilsIsSubstring(sProductName,asSelectedBands[iIndexSelectedBand]))
            {
                var sBandName=  asSelectedBands[iIndexSelectedBand].replace(sProductName);
                asReturnBandsName.push(sBandName);
            }
        }

        asReturnBandsName;
    }

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
