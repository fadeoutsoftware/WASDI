/**
 * Created by a.corrado on 15/02/2017.
 */

var ProductEditorInfoController = (function () {
    function ProductEditorInfoController(
        $scope,
        oClose,
        oExtras,
        oProductService,
        oConstantsService,
        oStyleService
    ) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oOriginalProduct = oExtras.product;
        this.m_oProductService = oProductService;
        this.m_oStyleService = oStyleService;
        this.m_oReturnProduct = oExtras.product;
        this.m_oOldFriendlyName = oExtras.product.productFriendlyName;
        this.m_oDescription = oExtras.product.description;
        this.workspaceId = oConstantsService.getActiveWorkspace().workspaceId;
        this.m_oProduct = {
            ...oExtras.product
        }; 
        this.m_asStyles = [];
        this.m_aoStylesMap = [];
        this.m_oStyle = {};
        this.m_bLoadingStyle = true;
        this.m_bProductChanged = false;

        var oController = this;

        $scope.close = function (result) {
            //Check if product changed
            if (!_.isEqual(oController.m_oProduct, oController.m_oOriginalProduct)) {
                oController.m_bProductChanged = true;
                oController.updateProduct(
                    oController.m_oProduct,
                    oController.workspaceId
                );
            }
            

            oClose(oController.m_bProductChanged, 500); // close, but give 500ms for bootstrap to animate
        };

        this.m_oStyleService.getStylesByUser().then(
            function (data) {
                if (data.status !== 200) {
                    var oDialog = utilsVexDialogAlertBottomRightCorner(
                        "GURU MEDITATION<br>ERROR GETTING STYLES"
                    );
                    utilsVexCloseDialogAfter(5000, oDialog);
                } else {
                    oController.m_asStyles = data.data.map((item) => item.name);
                    oController.m_aoStylesMap = oController.m_asStyles.map(
                        (name) => ({ name })
                    );

                    oController.m_aoStylesMap.forEach((oValue, sKey) => {
                        if (oValue.name == oController.m_oProduct.style) {
                            oController.m_oStyle = oValue;
                        }
                    });
                }

                oController.m_bLoadingStyle = false;
            },
            function (data) {
                var oDialog = utilsVexDialogAlertBottomRightCorner(
                    "GURU MEDITATION<br>ERROR GETTING STYLES"
                );
                utilsVexCloseDialogAfter(5000, oDialog);
                oController.m_bLoadingStyle = false;
            }
        );
    }

    ProductEditorInfoController.prototype.updateProduct = function () {
        if (utilsIsObjectNullOrUndefined(this.m_oProduct) === true)
            return false;
        var _this = this;

        var oOldMetadata = this.m_oProduct.metadata;
        this.m_oProduct.metadata = null;

        var sStyle = "";

        if (!utilsIsObjectNullOrUndefined(this.m_oStyle)) {
            sStyle = this.m_oStyle.name;
        }

        this.m_oProduct.style = sStyle;

        var oUpdatedViewModel = {};
        
        oUpdatedViewModel["fileName"] = this.m_oProduct.fileName;
        oUpdatedViewModel["productFriendlyName"] =
            this.m_oProduct.productFriendlyName;
        oUpdatedViewModel["style"] = this.m_oProduct.style;
        oUpdatedViewModel["description"] = this.m_oProduct.description;

        this.m_oProductService
            .updateProduct(oUpdatedViewModel, this.workspaceId)
            .then(
                function (data) {
                    if (data.data === "") {
                        _this.m_oProduct.metadata = oOldMetadata;
                        _this.m_oReturnProduct = _this.m_oProduct;
                        utilsJstreeUpdateLabelNode(
                            _this.m_oReturnProduct.fileName,
                            _this.m_oReturnProduct.productFriendlyName,
                            _this.m_oReturnProduct.description
                        );
                    } else {
                        console.log(
                            "Error update product: there was an error to the server"
                        );
                    }     
                },
                function (error) {
                    var oDialog = utilsVexDialogAlertBottomRightCorner(
                        "GURU MEDITATION<br>ERROR: IMPOSSIBLE TO UPDATE THE PRODUCT"
                    );
                    utilsVexCloseDialogAfter(5000, oDialog);

                    // restore product friendly name due to update failed
                    _this.m_oProduct.productFriendlyName =
                        _this.m_oOldFriendlyName;
                }
            );

        return true;
    };
    ProductEditorInfoController.$inject = [
        "$scope",
        "close",
        "extras",
        "ProductService",
        "ConstantsService",
        "StyleService",
    ];
    return ProductEditorInfoController;
})();
window.ProductEditorInfoController = ProductEditorInfoController;
