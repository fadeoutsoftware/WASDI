

var JRCProcessorController = (function() {

    function JRCProcessorController ($scope, oClose,oExtras,oSnapOperationService,oConstantsService) {
        //MEMBERS
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oSnapOperationService = oSnapOperationService;
        this.m_oConstantsService = oConstantsService;
        this.m_aoProduct = this.m_oExtras.products;
        this.m_sFileName = "";
        this.m_oSelectedReferenceProduct = this.m_aoProduct[0];
        this.m_sEPSG="";
        this.m_sOutputFile="";
        // this.m_asSelectedProducts = [];
        // this.m_asProductsName = utilsProjectGetProductsName(this.m_aoProduct);

        // var oController = this;
        $scope.close = function(result) {
            oClose(null, 300); // close, but give 500ms for bootstrap to animate
        };
        // $scope.add = function(result) {
        //     var aoSavedData = oController.m_oAdvanceFilterOptions.savedData;
        //     oClose(aoSavedData, 300); // close, but give 500ms for bootstrap to animate
        // };

    };

    JRCProcessorController.prototype.redirectToWebSite = function(){
        this.m_oWindow.open('http://www.mydewetra.org', '_blank');
    };

    JRCProcessorController.prototype.runJrcProcessor = function()
    {
        var oJRCJson = {
            inputFile: this.m_oSelectedReferenceProduct.fileName,
            epsg:this.m_sEPSG,
            outputFile:this.m_sOutputFile,
        };
        var oActiveWorkspace = this.m_oConstantsService.getActiveWorkspace();

        this.m_oSnapOperationService.runJRCProcessor(oJRCJson,oActiveWorkspace.workspaceId)
            .success(function(data,status){

            })
            .error(function(){

            });
    }
    JRCProcessorController .$inject = [
        '$scope',
        'close',
        'extras',
        'SnapOperationService',
        'ConstantsService'


    ];
    return JRCProcessorController ;
})();
