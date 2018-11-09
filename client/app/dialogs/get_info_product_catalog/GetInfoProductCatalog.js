/**
 * Created by a.corrado on 31/03/2017.
 */


var GetInfoProductCatalogController = (function() {

    function GetInfoProductCatalogController($scope, oClose, oExtras) {
        //MEMBERS
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oProduct = this.m_oExtras.product;
        // this.m_oProductChecked = {};
        this.m_oSelectedProduct = this.m_oExtras.selectedProduct;
        // this.m_oGetParametersOperationService = oGetParametersOperationService;

        $scope.close = function(result) {
            oClose(result, 300); // close, but give 500ms for bootstrap to animate
        };

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
    GetInfoProductCatalogController.prototype.getFileName = function()
    {
        if(utilsIsStrNullOrEmpty(this.m_oProduct.fileName) === true)
        {
            return "Unknown";
        }
        return this.m_oProduct.fileName
    };

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

    GetInfoProductCatalogController.$inject = [
        '$scope',
        'close',
        'extras',
        // 'GetParametersOperationService'
    ];
    return GetInfoProductCatalogController;
})();
