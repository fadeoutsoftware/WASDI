/**
 * Created by a.corrado on 31/03/2017.
 */


var AddProductInCatalogController = (function() {

    function AddProductInCatalogController($scope, oClose,oExtras,oWorkspaceService) {
        //MEMBERS
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_aoProduct = this.m_oExtras.product;
        this.m_oWorkspaceService = oWorkspaceService;
        this.m_aoWorkspaceList = [];
        this.m_aWorkspacesName = [];
        this.m_aoSelectedWorkspaces = [];
        this.m_sFileName = "";
        // this.m_oSelectedProduct = this.m_oExtras.selectedProduct;
        // this.m_oGetParametersOperationService = oGetParametersOperationService;
        $scope.close = function(result) {
            oClose(result, 300); // close, but give 500ms for bootstrap to animate
        };

        this.getWorkspaces();
        this.checkExtras();
    };
    AddProductInCatalogController.prototype.checkExtras = function()
    {
        var sFilename = this.m_aoProduct.fileName;
        sFilename = sFilename.trim();
        if(utilsIsStrNullOrEmpty(sFilename) === true)
        {
            this.m_sFileName = "Unknown";
        }else{
            this.m_sFileName = this.m_aoProduct.fileName;
        }
    }
    AddProductInCatalogController.prototype.getWorkspaces = function()
    {
        var oController = this;
        this.m_oWorkspaceService.getWorkspacesInfoListByUser().success(function (data, status) {
            if (data != null)
            {
                if (data != undefined)
                {
                    oController.m_aoWorkspaceList = data;
                    oController.m_aWorkspacesName = oController.getWorkspacesNameInWorkspaceList();
                }
            }
        }).error(function (data,status) {
            //alert('error');
            utilsVexDialogAlertTop('GURU MEDITATION<br>ERROR IN WORKSPACESINFO');
        });
    };

    AddProductInCatalogController.prototype.getWorkspacesNameInWorkspaceList = function()
    {
        var iIndexNumberOfWorkspaces = this.m_aoWorkspaceList.length;
        var aNames = [];
        for (var iIndex = 0 ; iIndex < iIndexNumberOfWorkspaces; iIndex++)
        {
            aNames.push("Name:" + this.m_aoWorkspaceList[iIndex].workspaceName + " - Id:" + this.m_aoWorkspaceList[iIndex].workspaceId);
        }
        return aNames
    };

    AddProductInCatalogController.$inject = [
        '$scope',
        'close',
        'extras',
        'WorkspaceService'

    ];
    return AddProductInCatalogController;
})();
