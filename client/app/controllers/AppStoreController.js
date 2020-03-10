/**
 * Created by p.campanella on 21/10/2016.
 */

var AppStoreController = (function() {
    function AppStoreController($scope, oConstantsService, oAuthService) {
        this.m_oScope = $scope;

        this.m_oConstantsService = oConstantsService;
        this.m_oAuthService = oAuthService;

    }


    AppStoreController.$inject = [
        '$scope',
        'ConstantsService',
        'AuthService',
    ];

    return AppStoreController;
}) ();
