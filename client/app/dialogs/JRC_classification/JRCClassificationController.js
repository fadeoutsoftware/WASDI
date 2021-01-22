

var JRCClassificationController = (function() {
    JRCClassificationController.REG_NAME = "JRCClassificationController";

    function JRCClassificationController ($scope, oClose,oExtras,oSnapOperationService,oConstantsService) {
        //MEMBERS
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oSnapOperationService = oSnapOperationService;
        this.m_oConstantsService = oConstantsService;
        this.m_aoProduct = this.m_oExtras.products;
        this.m_sFileName = "";
        this.m_oSelectedReferenceProduct = this.m_aoProduct[0];
        this.m_sGLC="GLC30.tif";
        this.m_sLANDSATGHSL="LANDSAT_GHSL_BETA_MT.tif";
        this.m_sPreprocess="";
        // this.m_asSelectedProducts = [];
        // this.m_asProductsName = utilsProjectGetProductsName(this.m_aoProduct);
        this.m_oReturnValueDropdown = {};
        this.m_aoProductListDropdown = this.getDropdownMenuList(this.m_aoProduct);
        // var oController = this;
        $scope.close = function(result) {
            oClose(null, 300); // close, but give 500ms for bootstrap to animate
        };
        // $scope.add = function(result) {
        //     var aoSavedData = oController.m_oAdvanceFilterOptions.savedData;
        //     oClose(aoSavedData, 300); // close, but give 500ms for bootstrap to animate
        // };

    }
    JRCClassificationController.prototype.getDropdownMenuList = function(aoProduct){

        return utilsProjectGetDropdownMenuListFromProductsList(aoProduct)
    };
    JRCClassificationController.prototype.getSelectedProduct = function(aoProduct,oSelectedProduct){

        return utilsProjectDropdownGetSelectedProduct(aoProduct,oSelectedProduct);
    }

    JRCClassificationController.prototype.redirectToWebSite = function(){
        this.m_oWindow.open('https://ec.europa.eu/jrc/en', '_blank');
    };

    JRCClassificationController.prototype.runJrcProcessor = function()
    {
        var oInputFile = this.getSelectedProduct(this.m_aoProduct,this.m_oReturnValueDropdown);

        var oJRCJson = {
            inputFileName: oInputFile.fileName,
            glc:this.m_sGLC,
            landsatghsl:this.m_sLANDSATGHSL,
            preprocess:this.m_sPreprocess
        };

        var oActiveWorkspace = this.m_oConstantsService.getActiveWorkspace();
        this.m_oSnapOperationService.runJRCClassification(oJRCJson,oActiveWorkspace.workspaceId)
            .then(function(data,status){

            },function(){

            });
    }
    JRCClassificationController .$inject = [
        '$scope',
        'close',
        'extras',
        'SnapOperationService',
        'ConstantsService'


    ];
    return JRCClassificationController ;
})();
