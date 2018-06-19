/**
 * Created by a.corrado on 24/05/2017.
 */



var OperaWappController = (function() {

    function OperaWappController($scope, oClose,oExtras,oAuthService,oConstantsService,oCatalogService) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oAuthService = oAuthService;
        this.m_oCatalogService = oCatalogService;
        this.m_oConstantsService = oConstantsService;

        $scope.close = function(result) {
            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };

    }



    OperaWappController.$inject = [
        '$scope',
        'close',
        'extras',
        'AuthService',
        'ConstantsService',
        'CatalogService'
    ];
    return OperaWappController;
})();
