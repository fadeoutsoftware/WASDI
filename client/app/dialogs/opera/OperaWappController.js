/**
 * Created by a.corrado on 24/05/2017.
 */



var OperaWappController = (function() {

    function OperaWappController($scope, oClose,oExtras,oAuthService,oConstantsService,oCatalogService,$window, oSnapOperationService) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oAuthService = oAuthService;
        this.m_oCatalogService = oCatalogService;
        this.m_oConstantsService = oConstantsService;
        this.m_oWindow = $window;
        this.m_aoProducts = this.m_oExtras.products;
        this.m_sResultFromServer = "";
        this.m_oSelectedProduct = null;
        this.m_oSnapOperationService = oSnapOperationService;
        this.m_bIsRunning = false;

        if(utilsIsObjectNullOrUndefined(this.m_aoProducts) === false)
        {
            this.m_oSelectedProduct = this.m_aoProducts[0];
        }
        $scope.close = function(result) {
            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };

    }

    OperaWappController.prototype.redirectToOperaWebSite = function(){
        this.m_oWindow.open('http://www.mydewetra.org', '_blank');
    };

    OperaWappController.prototype.runSaba = function() {
        console.log('eccomi');
        var sFile = this.m_oSelectedProduct.fileName;
        var sWorkspaceId = this.m_oConstantsService.getActiveWorkspace().workspaceId;

        var oController = this;
        this.m_bIsRunning = true;

        this.m_oSnapOperationService.runSaba(sFile,sWorkspaceId).success(function (data) {
            oController.m_bIsRunning = false;
            var oDialog = utilsVexDialogAlertBottomRightCorner("OPERA FLOOD DETECTION<br>DONE");
            utilsVexCloseDialogAfterFewSeconds(4000,oDialog);
        }).error(function (error) {
            oController.m_bIsRunning = false;
            utilsVexDialogAlertTop("GURU MEDITATION<br>THERE WAS AN ERROR RUNNING OPERA");
        });

    };


    OperaWappController.$inject = [
        '$scope',
        'close',
        'extras',
        'AuthService',
        'ConstantsService',
        'CatalogService',
        '$window',
        'SnapOperationService'
    ];
    return OperaWappController;
})();
