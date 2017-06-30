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
                this.m_sFileName_Operation = this.m_oSelectedProduct.name + "_ApplyOrbit";
        }
        // this.m_oTabOpen = "tab1";
        // this.m_asTypeOfData = ["GeoTIFF","NetCDF-BEAM","NetCDF4-CF","NetCDF-CF","CSV","Gamma","Generic Binary","GeoTIFF+XML",
        //                         "NetCDF4-BEAM","BEAM-DIMAP","ENVI","PolSARPro","Snaphu","JP2","JPG","PNG","BMP","GIF","BTF","GeoTIFF-BIGTIFF","HDF5"];

        // this.m_asOrbitStateVectors = ["Sentinel Precise (Auto Download)","Sentinel Restituted (Auto Download)","DORIS preliminary POR (ENVISAT)"
        //                                 ,"DORIS Precise Vor (ENVISAT) (Auto Download)","DELFT Precise (ENVISAT,ERS1&2) (Auto Download)","PRARE Precise (ERS1&2) (Auto Download)"];

        // this.m_sSelectedExtension = this.m_asTypeOfData[0];
        // this.m_sSelectedOrbitStateVectors = this.m_asOrbitStateVectors[0];
        this.m_sSelectedOrbitStateVectors = "";
        //TODO CHECK IF THERE IS sourceFileName && destinationFileName
        this.m_oReturnValue={
             sourceFileName:"",
             destinationFileName:"",
                // options:{
                //     orbitType:"",
                //     polyDegree:3,
                //     continueOnFail:false
                // }
        };

        this.m_oOptions ={};

        // this.m_oReturnValue = {
        //     sourceFileName:this.m_oSelectedProduct.fileName,
        //     destinationFileName:this.m_sFriendlyName_Operation,
        //     options:{
        //             orbitType:"Sentinel Precise(Auto Download)",
        //             polyDegree:3,
        //             continueOnFail:false}
        // };


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
            if( (utilsIsObjectNullOrUndefined(oController.m_sSelectedOrbitStateVectors) == true) && (utilsIsStrNullOrEmpty(oController.m_sSelectedOrbitStateVectors) == true) )
                bAreOkOptions = false;
            if( (utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.polyDegree) == true) && (utilsIsANumber(oController.m_oReturnValue.options.polyDegree) == false) )
                bAreOkOptions = false;
            if( utilsIsObjectNullOrUndefined(oController.m_oReturnValue.options.continueOnFail) == true )
                bAreOkOptions = false;

            if(bAreOkOptions != false)
            {
                oController.m_oReturnValue.sourceFileName = oController.m_oSelectedProduct.fileName;
                oController.m_oReturnValue.destinationFileName = oController.m_sFileName_Operation;
                oController.m_oReturnValue.options.orbitType = oController.m_sSelectedOrbitStateVectors;
            }
            else
            {
                oOptions = null;
            }
            oClose(oOptions, 500); // close, but give 500ms for bootstrap to animate
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
        // this.m_oReturnValue.sourceFileName = this.m_oSelectedProduct.fileName;
        // this.m_oReturnValue.destinationFileName = this.m_sFriendlyName_Operation;

        return true;
    };

    ApplyOrbitController.prototype.selectedProductIsEmpty = function()
    {
        if(utilsIsObjectNullOrUndefined(this.m_oSelectedProduct) == true)
            return true;
        return false;
    }

    ApplyOrbitController.prototype.getOrbitStateVector = function()
    {
        return this.m_asOrbitStateVectors;
    }

    ApplyOrbitController.$inject = [
        '$scope',
        'close',
        'extras',
        'GetParametersOperationService'
    ];
    return ApplyOrbitController;
})();
