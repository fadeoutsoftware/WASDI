/**
 * Created by a.corrado on 31/03/2017.
 */


var GetInfoProductCatalogController = (function() {

    function GetInfoProductCatalogController($scope, oClose, oExtras,oWorkspaceService) {
        //MEMBERS
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oWorkspaceService = oWorkspaceService
        this.m_oProduct = this.m_oExtras.product;
        this.m_oSelectedProduct = this.m_oExtras.selectedProduct;
        this.m_aoListOfProductWorkspaces = [];
        $scope.close = function(result) {
            oClose(result, 300); // close, but give 500ms for bootstrap to animate
        };
        this.getWorkspaceListByProduct();
        // this.checkProductInfo();
    };

    // GetInfoProductCatalogController.prototype.checkProductInfo = function()
    // {
    //     if(utilsIsObjectNullOrUndefined(this.m_oProduct) === true )
    //         return false;
    //     var sFilename = this.m_oProduct.fileName;
    //     sFilename = sFilename.trim();
    //     if(utilsIsStrNullOrEmpty(sFilename) === true)
    //     {
    //         this.m_oProductChecked.fileName = "Unknown";
    //     }
    //     else
    //     {
    //         this.m_oProductChecked.fileName = this.m_oProduct.fileName;
    //     }
    //     this.m_oProductChecked.boundingBox = this.m_oProduct.boundingBox;
    //     this.m_oProductChecked.category = this.m_oProduct.category;
    //     this.m_oProductChecked.filePath = this.m_oProduct.filePath;
    // };
    //
    /**
     *
     * @returns {*}
     */
    GetInfoProductCatalogController.prototype.getFileName = function()
    {
        if(utilsIsStrNullOrEmpty(this.m_oProduct.fileName) === true)
        {
            return "Unknown";
        }
        return this.m_oProduct.fileName
    };

    /**
     *
     * @returns {*}
     */
    GetInfoProductCatalogController.prototype.getProductBands = function()
    {
        if( utilsIsObjectNullOrUndefined(this.m_oProduct) === true )
        {
            return [];
        }
        if( utilsIsObjectNullOrUndefined(this.m_oProduct.productViewModel.bandsGroups.bands) === true )
        {
            return [];
        }

        return this.m_oProduct.productViewModel.bandsGroups.bands;
    };

    GetInfoProductCatalogController.prototype.getWorkspaceListByProduct = function()
    {
        var oController = this;
        var sFileName = this.getFileName();
        if( ( utilsIsStrNullOrEmpty(sFileName) === true ) || ( sFileName === "Unknown" ) )
        {
            return false;
        }
        this.m_oWorkspaceService.getWorkspaceListByProductName(sFileName)
            .success(function(data,status){
                if(utilsIsObjectNullOrUndefined(data) === false && status === 200)
                {
                    // workspaceId;
                    // workspaceName;
                    // ownerUserId;
                    oController.m_aoListOfProductWorkspaces = data;
                }
                else
                {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>WAS IMPOSSIBLE GET WORKSPACES LIST");
                }
            })
            .error(function(data,status){
                utilsVexDialogAlertTop("GURU MEDITATION<br>WAS IMPOSSIBLE GET WORKSPACES LIST");
            });
        return true;
    }

    GetInfoProductCatalogController.$inject = [
        '$scope',
        'close',
        'extras',
        'WorkspaceService'
        // 'GetParametersOperationService'
    ];
    return GetInfoProductCatalogController;
})();
