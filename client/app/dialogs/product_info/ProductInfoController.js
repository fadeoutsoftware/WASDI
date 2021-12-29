/**
 * Created by a.corrado on 15/02/2017.
 */

var ProductInfoController = (function() {

    function ProductInfoController($scope, oClose,oExtras) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oProduct = oExtras;
        this.m_oPropertiesList = this.getPropertiesList(oExtras);
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

    ProductInfoController.prototype.getPropertiesList = function(oProduct)
    {
        let propertiesList = [];

        if (oProduct && oProduct.properties) {
            if (oProduct.properties.beginposition) {
                propertiesList.push({"label" : "Date", "value": oProduct.properties.beginposition});
            }

            if (oProduct.properties.instrumentshortname) {
                propertiesList.push({"label" : "Instrument", "value": oProduct.properties.instrumentshortname});
            }

            if (oProduct.properties.platformname) {
                propertiesList.push({"label" : "Satellite", "value": oProduct.properties.platformname});
            }

            if (oProduct.properties.sensoroperationalmode) {
                propertiesList.push({"label" : "Mode", "value": oProduct.properties.sensoroperationalmode});
            }

            if (oProduct.properties.relativeorbitnumber) {
                propertiesList.push({"label" : "Relative orbit", "value": oProduct.properties.relativeorbitnumber});
            }

            if (oProduct.properties.size) {
                propertiesList.push({"label" : "Size", "value": oProduct.properties.size});
            }

            if (oProduct.properties.polarisationmode) {
                propertiesList.push({"label" : "Polarisation", "value": oProduct.properties.polarisationmode});
            }

            if (oProduct.properties.dataset) {
                propertiesList.push({"label" : "Dataset", "value": oProduct.properties.dataset});
            }

            if (oProduct.properties.productType) {
                propertiesList.push({"label" : "Product type", "value": oProduct.properties.productType});
            }

            if (oProduct.properties.presureLevels) {
                propertiesList.push({"label" : "Presure levels", "value": oProduct.properties.presureLevels});
            }

            if (oProduct.properties.variables) {
                propertiesList.push({"label" : "Variables", "value": oProduct.properties.variables});
            }

            if (oProduct.properties.format) {
                propertiesList.push({"label" : "File format", "value": oProduct.properties.format});
            }

        }

        return propertiesList;
    }

    ProductInfoController.$inject = [
        '$scope',
        'close',
        'extras',
    ];
    return ProductInfoController;
})();
