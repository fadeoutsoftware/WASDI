/**
 * Created by a.corrado on 31/03/2017.
 */


var ApplyOrbitController = (function() {

    function ApplyOrbitController($scope, oClose,oExtras,oGetParametersOperationService) {
        //MEMBERS
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_aoProducts = this.m_oExtras.products;
        this.m_oSelectedProduct = this.m_oExtras.selectedProduct;
        this.m_oGetParametersOperationService = oGetParametersOperationService;
        this.m_asSelectedProducts = [];
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
            if( (utilsIsObjectNullOrUndefined(this.m_oSelectedProduct.productFriendlyName)== false) && (utilsIsStrNullOrEmpty(this.m_oSelectedProduct.productFriendlyName)== false))
                this.m_sFileName_Operation = this.m_oSelectedProduct.name + "_ApplyOrbit";
        }

        this.m_sSelectedOrbitStateVectors = "";
        this.m_oReturnValue={
             sourceFileName:"",
             destinationFileName:"",

        };

        this.m_oOptions ={};

        $scope.close = function() {
            oClose("close", 500); // close, but give 500ms for bootstrap to animate
        };

        var oController = this;
        // $scope.run = function(oOptions) {
        //
        //     // CHECK OPTIONS
        //     var bAreOkOptions = true;
        //     if( (utilsIsObjectNullOrUndefined(oController.m_oSelectedProduct.fileName) == true) && (utilsIsStrNullOrEmpty(oController.m_oSelectedProduct.fileName) == true) )
        //         bAreOkOptions = false;
        //     if( (utilsIsObjectNullOrUndefined(oController.m_sFileName_Operation) == true) && (utilsIsStrNullOrEmpty(oController.m_sFileName_Operation) == true) )
        //         bAreOkOptions = false;
        //     if( (utilsIsObjectNullOrUndefined(oController.m_sSelectedOrbitStateVectors) == true) && (utilsIsStrNullOrEmpty(oController.m_sSelectedOrbitStateVectors) == true) )
        //         bAreOkOptions = false;
        //     if( (utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.polyDegree) == true) && (utilsIsANumber(oController.m_oReturnValue.options.polyDegree) == false) )
        //         bAreOkOptions = false;
        //     if( utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.continueOnFail) == true )
        //         bAreOkOptions = false;
        //
        //     if(bAreOkOptions != false)
        //     {
        //         //
        //         oController.m_oReturnValue.sourceFileName = oController.m_oSelectedProduct.fileName;
        //         oController.m_oReturnValue.destinationFileName = oController.m_sFileName_Operation;
        //         oController.m_oReturnValue.options.orbitType = oController.m_sSelectedOrbitStateVectors;
        //     }
        //     else
        //     {
        //         oOptions = null;
        //     }
        //     oClose(oOptions, 500); // close, but give 500ms for bootstrap to animate
        // };
        $scope.run = function() {

            // CHECK OPTIONS
            var iNumberOfSelectedProducts = oController.m_asSelectedProducts.length;
            var aoReturnValue = [];
            var bAreOkOptions = true;
            for(var iIndexSelectedProduct = 0; iIndexSelectedProduct < iNumberOfSelectedProducts ; iIndexSelectedProduct++)
            {
                var oProduct = oController.getProductByName(oController.m_asSelectedProducts[iIndexSelectedProduct]);

                if( (utilsIsObjectNullOrUndefined(oProduct.fileName) == true) && (utilsIsStrNullOrEmpty(oProduct.fileName) == true) )
                    bAreOkOptions = false;
                //TODO CHECK IT
                // if( (utilsIsObjectNullOrUndefined(oController.m_sFileName_Operation) == true) && (utilsIsStrNullOrEmpty(oController.m_sFileName_Operation) == true) )
                //     bAreOkOptions = false;
                if( (utilsIsObjectNullOrUndefined(oController.m_sSelectedOrbitStateVectors) == true) && (utilsIsStrNullOrEmpty(oController.m_sSelectedOrbitStateVectors) == true) )
                    bAreOkOptions = false;
                if( (utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.polyDegree) == true) && (utilsIsANumber(oController.m_oReturnValue.options.polyDegree) == false) )
                    bAreOkOptions = false;
                if( utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.continueOnFail) == true )
                    bAreOkOptions = false;
                if(bAreOkOptions != false)
                {
                    // oController.m_oReturnValue.sourceFileName = oController.m_oSelectedProduct.fileName;
                    // oController.m_oReturnValue.destinationFileName = oController.m_sFileName_Operation;
                    oController.m_oReturnValue.sourceFileName = oProduct.fileName;
                    oController.m_oReturnValue.destinationFileName = oProduct.name + "_ApplyOrbit";
                    oController.m_oReturnValue.options.orbitType = oController.m_sSelectedOrbitStateVectors;

                }
                else
                {
                    oController.m_oReturnValue = null;
                }

                aoReturnValue.push(oController.m_oReturnValue);
            }



            oClose(aoReturnValue, 500); // close, but give 500ms for bootstrap to animate
        };

        this.m_oGetParametersOperationService.getparametersApplyOrbit()
            .success(function (data) {
                if(utilsIsObjectNullOrUndefined(data) == false)
                {

                    oController.m_oOptions = utilsProjectConvertJSONFromServerInOptions(data);
                    oController.m_oReturnValue.options = oController.m_oOptions;

                    //set selected value(default value) orbit state
                    oController.m_sSelectedOrbitStateVectors = oController.m_oReturnValue.options.orbitType;
                    //array of value orbit state
                    oController.m_asOrbitStateVectors = utilsProjectGetArrayOfValuesForParameterInOperation(data,"orbitType");
                }
                else
                {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN GET PARAMETERS, THERE AREN'T DATA");
                }

            }).error(function (error) {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN GET PARAMETERS");
            });

    };

    ApplyOrbitController.prototype.nameIsUsed = function()
    {
        return utilsProjectCheckInDialogIfProductNameIsInUsed( this.m_sFileName_Operation  , this.m_aoProducts );
    };

    ApplyOrbitController.prototype.changeProduct = function(oNewSelectedProductInput)
    {
        if(utilsIsObjectNullOrUndefined(oNewSelectedProductInput) == true)
            return false;
        this.m_oSelectedProduct = oNewSelectedProductInput;
        this.m_sFileName_Operation = this.m_oSelectedProduct.name + "_ApplyOrbit.zip";
        this.m_oReturnValue={
            sourceFileName:"",
            destinationFileName:"",
            options:this.m_oOptions,
        };

        return true;
    };

    ApplyOrbitController.prototype.selectedProductIsEmpty = function()
    {
        if(utilsIsObjectNullOrUndefined(this.m_asSelectedProducts))
            return true;
        // if(utilsIsObjectNullOrUndefined(this.m_oSelectedProduct) == true)
        //     return true;
        return false;
    }

    ApplyOrbitController.prototype.getOrbitStateVector = function()
    {
        return this.m_asOrbitStateVectors;
    }

    ApplyOrbitController.prototype.getProductsName = function(){
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

    ApplyOrbitController.prototype.getProductByName = function(sName){
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

    ApplyOrbitController.$inject = [
        '$scope',
        'close',
        'extras',
        'GetParametersOperationService'
    ];
    return ApplyOrbitController;
})();
