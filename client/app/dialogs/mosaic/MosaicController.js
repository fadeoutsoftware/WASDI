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


        $scope.close = function(result) {

            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };

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
