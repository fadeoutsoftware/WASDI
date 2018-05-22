/**
 * Created by a.corrado on 23/05/2017.
 */
var GenerateAutomaticOperationDialogController = (function() {

    function GenerateAutomaticOperationDialogController($scope, oClose,oExtras) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oGetParameters = this.m_oExtras.getParameters;
        this.m_sNameDialog = this.m_oExtras.nameDialog;
        this.m_oParameters=[];
        this.m_asSelectedProducts = [];
        this.m_asSourceBands = [];
        this.m_asSourceBandsSelected = [];
        this.m_aoProducts = this.m_oExtras.products;
        if(utilsIsObjectNullOrUndefined(this.m_aoProducts) == true)
        {
            this.m_aoProducts = [];
        }
        //Dont't remove it!
        this.m_asProductsName = this.getProductsName();
        var oController = this;

        $scope.close = function() {
            oClose("close", 500); // close, but give 500ms for bootstrap to animate
        };
        $scope.run = function() {
            var oResult = oController.getOperationArrayForSelectedProducts();
            oClose(oResult, 500); // close, but give 500ms for bootstrap to animate
        };
        this.getParameters();

        /*************************************** WATCH ***************************************/
        $scope.$watch('m_oController.m_asSelectedProducts', function (newValue, oldValue, scope)
        {
            $scope.m_oController.m_asSourceBands = $scope.m_oController.getBandsFromSelectedProducts();
        },true);
    }

    /**
     *
     * @returns {*}
     */
    GenerateAutomaticOperationDialogController.prototype.getBandsFromSelectedProducts = function()
    {
        return utilsProjectGetBandsFromSelectedProducts(this.m_asSelectedProducts,this.m_aoProducts);
    };
    /**
     * getProductsName
     * @returns {*}
     */
    GenerateAutomaticOperationDialogController.prototype.getProductsName = function(){
        return utilsProjectGetProductsName(this.m_aoProducts);
    }
    /**
     *
     * @param sName
     * @returns {*}
     */
    GenerateAutomaticOperationDialogController.prototype.getProductByName = function(sName){
        return utilsProjectGetProductByName(sName,this.m_aoProducts);
    };
    /**
     * getParameters
     */
    GenerateAutomaticOperationDialogController.prototype.getParameters = function()
    {
        var oController = this;
        this.m_oGetParameters.success(function (data) {
            if(utilsIsObjectNullOrUndefined(data) == false)
            {
                // oController.m_oOptions = utilsProjectConvertJSONFromServerInOptions(data);
                oController.m_oParameters = data;
                // oController.m_oReturnValue.options = oController.m_oOptions;
                // oController.m_oScope.$apply();
            }
            else
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN GET PARAMETERS, THERE AREN'T DATA");
            }
        }).error(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN GET PARAMETERS");
        });

    }
    /**
     * getTypeOfParameter
     * @param oParameter
     * @returns {*}
     */
    GenerateAutomaticOperationDialogController.prototype.getTypeOfParameter = function(oParameter)
    {
        if(utilsIsObjectNullOrUndefined(oParameter) === true)
        {
            return null;
        }
        //ATTENTION don't change the order of if
        //if source bands
        if(oParameter.alias === "sourceBands")
        {
            return "sourceBands";
        }
        //if boolean
        if( (oParameter.defaultValue === true) || (oParameter.defaultValue === false) || (oParameter.defaultValue === "true") || (oParameter.defaultValue === "false") )
        {
            //checkbox case
            return "checkbox";
        }
        //if there is a list of values
        if(oParameter.valueSet.length > 0)
        {
            // drop-down list case
            return "dropdown";
        }
        else
        {
            //input text case
            return "text";
        }

        return null;
    };

    /**
     * getParametersObject
     * @returns {*}
     * return all parameters selected by user
     */
    GenerateAutomaticOperationDialogController.prototype.getParametersObject = function()
    {
        if( utilsIsObjectNullOrUndefined( this.m_oParameters ) === true )
        {
            return null;
        }
        var iNumberOfParameters = this.m_oParameters.length;
        var oOperationObject = {};
        for(var iIndexParameter = 0; iIndexParameter < iNumberOfParameters; iIndexParameter++)
        {
            var sObjectProperty = this.m_oParameters[iIndexParameter].field;
            if(utilsIsStrNullOrEmpty(sObjectProperty) === false)
            {
                var sValue = this.m_oParameters[iIndexParameter].defaultValue;
                if(utilsIsObjectNullOrUndefined(sValue) === false)
                {
                    oOperationObject[sObjectProperty] = sValue;
                }
            }
        }
        return oOperationObject;

    };
    /**
     * getOperationObject
     * @param oProduct
     * @returns {*}
     * return sourceFileName, destinationFileName
     */
    GenerateAutomaticOperationDialogController.prototype.getProductObject = function(oProduct)
    {
        if(utilsIsObjectNullOrUndefined(oProduct) === true)
        {
            return null;
        }
        var sOperationName = "_OperationDefaultName";
        //get operation name
        if(utilsIsStrNullOrEmpty(this.m_sNameDialog) === false )
        {
            //remove spaces
            sOperationName = "_" + this.m_sNameDialog.replace(/ /g,'');
        }
        var oOperationObject = {};
        //product info
        oOperationObject.sourceFileName = oProduct.fileName;
        oOperationObject.destinationFileName = oProduct.name + sOperationName;
        oOperationObject.options = {};
        return oOperationObject;
    };
    /**
     * getOperationArrayForSelectedProducts
     * @returns {*}
     * return object, send it to the server
     */
    GenerateAutomaticOperationDialogController.prototype.getOperationArrayForSelectedProducts = function()
    {
        if(utilsIsObjectNullOrUndefined(this.m_asSelectedProducts))
        {
            return null;
        }
        var oParameters = this.getParametersObject();
        if(utilsIsObjectNullOrUndefined(oParameters) === true)
        {
            return null;
        }
        var iNumberOfProducts = this.m_asSelectedProducts.length;
        var aoArrayObject = [];
        //generate array list with the objects created by product info + parameters
        for(var iIndexProduct = 0; iIndexProduct < iNumberOfProducts; iIndexProduct++)
        {
            var oProduct = this.getProductByName(this.m_asSelectedProducts[iIndexProduct]);
            var oOperationObject = this.getProductObject(oProduct);
            if(utilsIsObjectNullOrUndefined(oOperationObject) === false)
            {
                // put parameters in product object
                oOperationObject.options = oParameters;
                //get selected band per products
                oOperationObject.options.sourceBandNames = this.getSelectedBandsByProductName(oProduct.name, this.m_asSourceBandsSelected);

                aoArrayObject.push(oOperationObject);
            }
        }
        return aoArrayObject;
    };

    /**
     * getSelectedBandsByProductName
     * @param sProductName
     * @param asSelectedBands
     * @returns {*}
     */
    GenerateAutomaticOperationDialogController.prototype.getSelectedBandsByProductName = function(sProductName, asSelectedBands)
    {
        return utilsProjectGetSelectedBandsByProductName(sProductName, asSelectedBands);
    };
    GenerateAutomaticOperationDialogController.$inject = [
        '$scope',
        'close',
        'extras',
    ];
    return GenerateAutomaticOperationDialogController;
})();
