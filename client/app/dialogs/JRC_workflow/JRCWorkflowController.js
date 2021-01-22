

var JRCWorkflowController = (function() {

    function JRCWorkflowController ($scope, oClose,oExtras,oSnapOperationService,oConstantsService) {
        //MEMBERS
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oSnapOperationService = oSnapOperationService;
        this.m_oConstantsService = oConstantsService;
        this.m_aoProduct = this.m_oExtras.products;
        this.m_sFileName = "";
        this.m_oSelectedReferenceProduct = this.m_aoProduct[0];
        this.m_sEPSG="3857";
        this.m_sOutputFile="test_output";
        this.m_sPreprocess="SentinelToGeoTiff";
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
    JRCWorkflowController.prototype.getDropdownMenuList = function(aoProduct){

        return utilsProjectGetDropdownMenuListFromProductsList(aoProduct)
    };
    JRCWorkflowController.prototype.getSelectedProduct = function(aoProduct,oSelectedProduct){

        return utilsProjectDropdownGetSelectedProduct(aoProduct,oSelectedProduct);
    };

    JRCWorkflowController.prototype.redirectToWebSite = function(){
        this.m_oWindow.open('https://ec.europa.eu/jrc/en', '_blank');
    };

    JRCWorkflowController.prototype.runJrcProcessor = function()
    {
        var oInputFile = this.getSelectedProduct(this.m_aoProduct,this.m_oReturnValueDropdown);

        var oJRCJson = {
            inputFileName: oInputFile.fileName,
            epsg:this.m_sEPSG,
            outputFileName:this.m_sOutputFile,
            preprocess:this.m_sPreprocess
        };

        var oActiveWorkspace = this.m_oConstantsService.getActiveWorkspace();

        this.m_oSnapOperationService.runJRCWorkflow(oJRCJson,oActiveWorkspace.workspaceId)
            .then(function(data,status){

            },function(){

            });
    }
    JRCWorkflowController .$inject = [
        '$scope',
        'close',
        'extras',
        'SnapOperationService',
        'ConstantsService'


    ];
    return JRCWorkflowController ;
})();
