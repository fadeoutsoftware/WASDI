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
        this.m_sLastGeneratedFile = "";
        this.m_bHasResult = false;

        this.m_oReturnValueDropdown = {};
        this.m_aoProductListDropdown = this.getDropdownMenuList(this.m_aoProducts);

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

    OperaWappController.prototype.getDropdownMenuList = function(aoProduct){

        return utilsProjectGetDropdownMenuListFromProductsList(aoProduct)
    };
    OperaWappController.prototype.getSelectedProduct = function(aoProduct,oSelectedProduct){

        return utilsProjectDropdownGetSelectedProduct(aoProduct,oSelectedProduct);
    }
    OperaWappController.prototype.runSaba = function() {

        this.m_bHasResult = false;

        // var sFile = this.m_oSelectedProduct.fileName;
        var oInputFile = this.getSelectedProduct(this.m_aoProducts,this.m_oReturnValueDropdown);
        var sFile = oInputFile.fileName;

        var sWorkspaceId = this.m_oConstantsService.getActiveWorkspace().workspaceId;

        this.m_sResultFromServer = "Opera Automatic Flooded Area Detection Running";

        var oController = this;
        this.m_bIsRunning = true;

        this.m_oSnapOperationService.runSaba(sFile,sWorkspaceId).success(function (data) {
            oController.m_bIsRunning = false;

            if (data.intValue == 200) {
                var sFile = data.stringValue;
                oController.m_sResultFromServer = "Flooded Area Map Created. File Added to Workspace " + sFile;
                oController.m_sLastGeneratedFile = sFile;
                oController.m_bHasResult = true;
            }
            var oDialog = utilsVexDialogAlertBottomRightCorner("OPERA FLOOD DETECTION<br>DONE");
            utilsVexCloseDialogAfter(4000,oDialog);
        }).error(function (error) {
            oController.m_bIsRunning = false;
            utilsVexDialogAlertTop("GURU MEDITATION<br>THERE WAS AN ERROR RUNNING OPERA");
        });

    };

    OperaWappController.prototype.publish = function() {
        console.log('OPERA OUTPUT FILE ' + this.m_sLastGeneratedFile);

        var sFile = this.m_sLastGeneratedFile;
        var sWorkspaceId = this.m_oConstantsService.getActiveWorkspace().workspaceId;

        this.m_sResultFromServer = "Publishing OPERA Result in Dewetra";
        var oController = this;

        this.m_oSnapOperationService.publishSabaResult(sFile,sWorkspaceId).success(function (data) {
            oController.m_bIsRunning = false;

            if (data.intValue == 200) {
                var sFile = data.stringValue;
                oController.m_sResultFromServer = "File available in Dewetra " + sFile;
                oController.m_sLastGeneratedFile = sFile;
                oController.m_bHasResult = false;
            }
            var oDialog = utilsVexDialogAlertBottomRightCorner("FLOODED AREA<br>PUBLISHED");
            utilsVexCloseDialogAfter(4000,oDialog);
        }).error(function (error) {
            oController.m_bIsRunning = false;
            utilsVexDialogAlertTop("GURU MEDITATION<br>THERE WAS AN ERROR PUBLISHING OPERA");
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
