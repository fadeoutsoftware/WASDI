/**
 * Created by a.corrado on 16/06/2017.
 */



var MosaicController = (function() {

    function MosaicController($scope, oClose,oExtras,oSnapOperationService,oConstantsService) {
        this.m_oScope = $scope;
        this.m_oClose = oClose;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_aoProducts = this.m_oExtras.products;
        this.m_oSnapOperationService = oSnapOperationService;
        this.m_oConstantsService = oConstantsService;
        this.m_asProductsName = utilsProjectGetProductsName(this.m_aoProducts);
        this.m_asSelectedProducts = [];
        this.m_bAutoFindBoundingBox = true;
        this.m_bAutoSelectBands = true;
        this.m_oMosaicViewModel = {
            autoFindBoundingBox:true,
            autoSelectBands:true,
            southBound:-1,
            eastBound:-1,
            westBound :-1,
            northBound :-1,
            pixelSizeX:0.005,
            pixelSizeY:0.005,
            outputFile:"",
            bands:""
        }

        var oController = this;
        $scope.close = function(result) {
            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };
        $scope.run = function(result) {
            oController.runMosaic();
            // close(""); // close, but give 500ms for bootstrap to animate
        };

    }

    MosaicController.prototype.isDisabledRunButton = function(){
        if(utilsIsObjectNullOrUndefined(this.m_asSelectedProducts))
        {
            return true;
        }
        var iNumberOfSelectedProducts = this.m_asSelectedProducts.length;

        if( iNumberOfSelectedProducts < 2)
        {
            return true;
        }
        return false;
    }
    MosaicController.prototype.runMosaic = function(){

        var oActiveWorkspace = this.m_oConstantsService.getActiveWorkspace();

        this.m_oSnapOperationService.geometricMosaic(oActiveWorkspace.workspaceId,this.m_oMosaicViewModel.outputFile, this.m_oMosaicViewModel).success(function (data) {

        }).error(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN GET PARAMETERS");
        });
    }
    MosaicController.$inject = [
        '$scope',
        'close',
        'extras',
        'SnapOperationService',
        'ConstantsService',
    ];
    return MosaicController;
})();
