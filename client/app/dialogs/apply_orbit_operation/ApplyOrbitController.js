/**
 * Created by a.corrado on 31/03/2017.
 */


var ApplyOrbitController = (function() {

    function ApplyOrbitController($scope, oClose,oExtras) {
        //MEMBERS
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_aoProducts = this.m_oExtras.products;
        this.m_oSelectedProduct = this.m_oExtras.selectedProduct;

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
                this.m_sFriendlyName_Operation = this.m_oSelectedProduct.productFriendlyName + "_ApplyOrbit";
        }
        // this.m_oTabOpen = "tab1";
        this.m_asTypeOfData = ["GeoTIFF","NetCDF-BEAM","NetCDF4-CF","NetCDF-CF","CSV","Gamma","Generic Binary","GeoTIFF+XML",
                                "NetCDF4-BEAM","BEAM-DIMAP","ENVI","PolSARPro","Snaphu","JP2","JPG","PNG","BMP","GIF","BTF","GeoTIFF-BIGTIFF","HDF5"];
        this.m_asOrbitStateVectors = ["Sentinel Precise(Auto Download)","Sentinel Restituted(Auto Download)","DORIS preliminary POR(ENVISAT)"
                                        ,"DORIS Precise Vor(ENVISAT)(Auto Download)","DELFT Precise(ENVISAT,ERS1&2)(Auto Download)","PRARE Precise(ERS1&2)(Auto Download)"];

        this.m_sSelectedExtension = this.m_asTypeOfData[0];
        this.m_sSelectedOrbitStateVectors = this.m_asOrbitStateVectors[0];
        //TODO CHECK IF THERE IS sourceFileName && destinationFileName
        this.m_oReturnValue = {
            sourceFileName:this.m_oSelectedProduct.filename,
            destinationFileName:this.m_sFriendlyName_Operation,
            options:{}
        };


        //this.m_oOrbit = oExtras;
        //$scope.close = oClose;
        $scope.close = function() {
            oClose("close", 500); // close, but give 500ms for bootstrap to animate
        };
        $scope.run = function(oOptions) {
            //TODO CHECK OPTIONS
            oClose(oOptions, 500); // close, but give 500ms for bootstrap to animate
        };
    };
    // ApplyOrbitController.prototype.tabOpen = function(sTabInput)
    // {
    //     if(utilsIsStrNullOrEmpty(sTabInput))
    //         return false;
    //     this.m_oTabOpen = sTabInput;
    //     return true;
    // };
    // ApplyOrbitController.prototype.fir   // {
    //     if( this.m_oTabOpen == "tab1")
    //         return true;
    //     else
    //         return false;
    // };
    // ApplyOrbitController.prototype.secondTabIsOpen = function()
    // {
    //     if( this.m_oTabOpen == "tab2")
    //         return true;
    //     else
    //         return false;
    // };stTabIsO
    ApplyOrbitController.prototype.changeProduct = function(oNewSelectedProductInput)
    {
        if(utilsIsObjectNullOrUndefined(oNewSelectedProductInput) == true)
            return false;
        this.m_oSelectedProduct = oNewSelectedProductInput;
        this.m_sFriendlyName_Operation = this.m_oSelectedProduct.productFriendlyName + "_ApplyOrbit";

        this.m_oReturnValue.sourceFileName = this.m_oSelectedProduct.filename;
        this.m_oReturnValue.destinationFileName = this.m_sFriendlyName_Operation;

        return true;
    };


    ApplyOrbitController.$inject = [
        '$scope',
        'close',
        'extras'
    ];
    return ApplyOrbitController;
})();
