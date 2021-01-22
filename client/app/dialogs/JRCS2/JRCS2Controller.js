

var JRCS2Controller = (function() {

    function JRCS2Controller ($scope, oClose,oExtras,oSnapOperationService,oConstantsService) {
        //MEMBERS
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oSnapOperationService = oSnapOperationService;
        this.m_oConstantsService = oConstantsService;
        this.m_aoProduct = this.m_oExtras.products;
        this.m_sFileName = "";
        this.m_oSelectedReferenceProduct = this.m_aoProduct[0];
        this.m_sLrnSet="MT_2017.tif";
        this.m_sLrnSetPositive="[3:6]";
        this.m_sLrnSetPositiveNodata="0";
        this.m_sCloudThresh=200;

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

    JRCS2Controller.prototype.getDropdownMenuList = function(aoProduct){

        return utilsProjectGetDropdownMenuListFromProductsList(aoProduct)
    };
    JRCS2Controller.prototype.getSelectedProduct = function(aoProduct,oSelectedProduct){

        return utilsProjectDropdownGetSelectedProduct(aoProduct,oSelectedProduct);
    };

    JRCS2Controller.prototype.redirectToWebSite = function(){
        this.m_oWindow.open('https://ec.europa.eu/jrc/en', '_blank');
    };

    JRCS2Controller.prototype.runJrcProcessor = function()
    {
       var oInputFile = this.getSelectedProduct(this.m_aoProduct,this.m_oReturnValueDropdown);

        var oJRCJson = {
            inputFileName: oInputFile.fileName,
            lrnSet:this.m_sLrnSet,
            lrnSetPositive:this.m_sLrnSetPositive,
            lrnSetNoData:this.m_sLrnSetPositiveNodata,
            cloudThresh:this.m_sCloudThresh
        };


        var oActiveWorkspace = this.m_oConstantsService.getActiveWorkspace();

        this.m_oSnapOperationService.runJRCS2(oJRCJson,oActiveWorkspace.workspaceId)
            .then(function(data,status){

            },function(){

            });
    }
    JRCS2Controller .$inject = [
        '$scope',
        'close',
        'extras',
        'SnapOperationService',
        'ConstantsService'


    ];
    return JRCS2Controller ;
})();
