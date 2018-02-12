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
        this.m_oClickedProduct = this.m_oExtras.selectedProduct;
        this.m_oGetParametersOperationService = oGetParametersOperationService;
        this.m_asSelectedProducts = [];
        this.m_oOptions ={};
        this.m_sSelectedOrbitStateVectors = "";
        this.m_oReturnValue={
            sourceFileName:"",
            destinationFileName:"",
        };

        if(utilsIsObjectNullOrUndefined(this.m_aoProducts) == true)
        {
            this.m_aoProducts = [];
        }
        //Dont't remove it!
        this.m_asProductsName = this.getProductsName();

        if(utilsIsObjectNullOrUndefined(this.m_oClickedProduct) == true)
        {
            this.m_oClickedProduct = null;
        }
        else
        {
            //push the clicked product in the list of selected products
            this.m_asSelectedProducts.push(this.m_oClickedProduct.name);
        }


        $scope.close = function() {
            oClose("close", 500); // close, but give 500ms for bootstrap to animate
        };

        var oController = this;

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

                var oRetValue = null;

                if(bAreOkOptions != false)
                {
                    oRetValue = {
                        options:{}
                    }
                    // oController.m_oReturnValue.sourceFileName = oController.m_oSelectedProduct.fileName;
                    // oController.m_oReturnValue.destinationFileName = oController.m_sFileName_Operation;
                    oRetValue.sourceFileName = oProduct.fileName;
                    oRetValue.destinationFileName = oProduct.name + "_ApplyOrbit";
                    oRetValue.options.orbitType = oController.m_sSelectedOrbitStateVectors;

                }

                if (!utilsIsObjectNullOrUndefined(oRetValue)) {
                    aoReturnValue.push(oRetValue);
                }
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

    // ApplyOrbitController.prototype.changeProduct = function(oNewSelectedProductInput)
    // {
    //     if(utilsIsObjectNullOrUndefined(oNewSelectedProductInput) == true)
    //         return false;
    //     this.m_oSelectedProduct = oNewSelectedProductInput;
    //     this.m_sFileName_Operation = this.m_oSelectedProduct.name + "_ApplyOrbit.zip";
    //     this.m_oReturnValue={
    //         sourceFileName:"",
    //         destinationFileName:"",
    //         options:this.m_oOptions,
    //     };
    //
    //     return true;
    // };

    ApplyOrbitController.prototype.selectedProductIsEmpty = function()
    {
        if(utilsIsObjectNullOrUndefined(this.m_asSelectedProducts))
            return true;

        return false;
    }

    ApplyOrbitController.prototype.getOrbitStateVector = function()
    {
        return this.m_asOrbitStateVectors;
    }


    ApplyOrbitController.prototype.getProductsName = function(){
        return utilsProjectGetProductsName(this.m_aoProducts);
    }

    /**
     *
     * @param sName
     * @returns {*}
     */
    ApplyOrbitController.prototype.getProductByName = function(sName){
        return utilsProjectGetProductByName(sName,this.m_aoProducts);
    };

    ApplyOrbitController.$inject = [
        '$scope',
        'close',
        'extras',
        'GetParametersOperationService'
    ];
    return ApplyOrbitController;
})();
