/**
 * Created by a.corrado on 24/05/2017.
 */



var OperaWappController = (function() {

    function OperaWappController($scope, oClose,oExtras,oAuthService,oConstantsService,oCatalogService,$window) {
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
        if(utilsIsObjectNullOrUndefined(this.m_aoProducts) === false)
        {
            this.m_oSelectedProduct = this.m_aoProducts[0];
        }
        $scope.close = function(result) {
            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };

    }

    OperaWappController.prototype.redirectToOperaWebSite = function(){
        // this.m_oWindow.open('http://www.rasor.eu/rasor/', '_blank');
    }


    OperaWappController.$inject = [
        '$scope',
        'close',
        'extras',
        'AuthService',
        'ConstantsService',
        'CatalogService',
        '$window'
    ];
    return OperaWappController;
})();
