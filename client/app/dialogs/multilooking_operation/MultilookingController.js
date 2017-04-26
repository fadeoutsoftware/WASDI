/**
 * Created by a.corrado on 04/04/2017.
 */

var MultilookingController = (function() {

    function MultilookingController($scope, oClose,oExtras,oGetParametersOperationService) {//,oExtras
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_aoProducts = this.m_oExtras.products;
        this.m_oSelectedProduct = this.m_oExtras.selectedProduct;
        this.m_asSourceBands = [];
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
                this.m_sFileName_Operation = this.m_oSelectedProduct.name + "_Multilooking.zip";

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
        // this.m_asOrbitStateVectors = ["Sentinel Precise(Auto Download)","Sentinel Restituted(Auto Download)","DORIS preliminary POR(ENVISAT)"
        //     ,"DORIS Precise Vor(ENVISAT)(Auto Download)","DELFT Precise(ENVISAT,ERS1&2)(Auto Download)","PRARE Precise(ERS1&2)(Auto Download)"];


        // this.m_asSourceBands =  ["Band1","Band2","Band3"];
        this.m_asSourceBandsSelected = [];

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
        $scope.run = function(oOptions) {
            //TODO CHECK OPTIONS
            var bAreOkOptions = true;

            if( (utilsIsObjectNullOrUndefined(oController.m_asSourceBandsSelected) == true) )
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
                oController.m_oReturnValue.sourceFileName = oController.m_oSelectedProduct.fileName;
                oController.m_oReturnValue.destinationFileName = oController.m_sFileName_Operation;
                oController.m_oReturnValue.options.sourceBandNames = oController.m_asSourceBandsSelected;
            }
            else
            {
                oOptions = null;
            }
            oClose(oOptions, 500); // close, but give 500ms for bootstrap to animate
        };

        this.m_oGetParametersOperationService.getParametersMultilooking()
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
    }

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

        // angular.element("multiselectMultilooking").remove();

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
        // angular.element("multiselectMultilooking").append(' <multiselect id="multiselectMultilooking" ng-model="m_oController.m_asSourceBandsSelected" options="m_oController.m_asSourceBands" show-select-all="true" show-unselect-all="true" show-search="true"></multiselect>');
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
        if(utilsIsObjectNullOrUndefined(this.m_oSelectedProduct) == true)
            return true;
        return false;
    }

    MultilookingController.$inject = [
        '$scope',
        'close',
        'extras',
        'GetParametersOperationService'

    ];//'extras',
    return MultilookingController;
})();
