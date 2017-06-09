/**
 * Created by a.corrado on 15/02/2017.
 */

var ProductEditorInfoController = (function() {

    function ProductEditorInfoController($scope, oClose,oExtras,oProductService) {//,
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oProduct = oExtras.product;
        this.m_oProductService = oProductService;
        this.m_oReturnProduct = oExtras.product;
        this.m_oOldFriendlyName = oExtras.product.productFriendlyName;
        //$scope.close = oClose;

        var oController=this;
        $scope.close = function(result) {
            oController.updateProduct(oController.m_oProduct);

            oClose(oController.m_oReturnProduct, 500); // close, but give 500ms for bootstrap to animate
        };

    }

    ProductEditorInfoController.prototype.getSummaryPropertyNames = function()
    {
        //var group = this.m_oProduct.summary;

        var allPropertyNames = Object.keys(this.m_oProduct.summary);
        return allPropertyNames;
        //for (var j=0; j<allPropertyNames.length; j++) {
        //    var name = allPropertyNames[j];
        //    var value = group[name];
        //    // Do something
        //}
    }
    ProductEditorInfoController.prototype.updateProduct = function()
    {

        if(utilsIsObjectNullOrUndefined(this.m_oProduct) === true)
            return false;
        var oController=this;

        var oOldMetadata = this.m_oProduct.metadata;
        this.m_oProduct.metadata = null;

        if(this.m_oProduct.productFriendlyName === this.m_oOldFriendlyName)
            return false;
        this.m_oProductService.updateProduct(this.m_oProduct).success(function (data, status)
        {
            if(data === "") {
                oController.m_oProduct.metadata = oOldMetadata;
                oController.m_oReturnProduct = oController.m_oProduct;
                utilsJstreeUpdateLabelNode(oController.m_oReturnProduct.fileName, oController.m_oReturnProduct.productFriendlyName);
                console.log("Product Updated");
            }
            else
                console.log("Error update product: there was an error to the server");

        }).error(function (data,status) {
            //alert('error');
            utilsVexDialogAlertTop("Error: impossible to update the product");

        });



        return true;
    }
    ProductEditorInfoController.$inject = [
        '$scope',
        'close',
        'extras',
        'ProductService'
    ];
    return ProductEditorInfoController;
})();
