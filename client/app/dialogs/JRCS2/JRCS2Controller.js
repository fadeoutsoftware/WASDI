

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


        // var oController = this;
        $scope.close = function(result) {
            oClose(null, 300); // close, but give 500ms for bootstrap to animate
        };
        // $scope.add = function(result) {
        //     var aoSavedData = oController.m_oAdvanceFilterOptions.savedData;
        //     oClose(aoSavedData, 300); // close, but give 500ms for bootstrap to animate
        // };

    };

    JRCS2Controller.prototype.redirectToWebSite = function(){
        this.m_oWindow.open('https://ec.europa.eu/jrc/en', '_blank');
    };

    JRCS2Controller.prototype.runJrcProcessor = function()
    {
        var oJRCJson = {
            inputFileName: this.m_oSelectedReferenceProduct.fileName,
            LrnSet:this.m_sLrnSet,
            LrnSetPositive:this.m_sLrnSetPositive,
            LrnSetPositiveNodata:this.m_sLrnSetPositiveNodata,
            CloudThresh:this.m_sCloudThresh
        };

        var oActiveWorkspace = this.m_oConstantsService.getActiveWorkspace();

        this.m_oSnapOperationService.runJRCS2(oJRCJson,oActiveWorkspace.workspaceId)
            .success(function(data,status){

            })
            .error(function(){

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
