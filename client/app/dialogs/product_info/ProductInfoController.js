/**
 * Created by a.corrado on 15/02/2017.
 */

var ProductInfoController = (function() {

    function ProductInfoController($scope, oClose,oExtras) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oProduct = oExtras;
        //$scope.close = oClose;
        $scope.close = function(result) {
            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };

    }

    ProductInfoController.prototype.getSummaryPropertyNames = function()
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

    ProductInfoController.$inject = [
        '$scope',
        'close',
        'extras',
    ];
    return ProductInfoController;
})();
window.ProductInfoController = ProductInfoController;
