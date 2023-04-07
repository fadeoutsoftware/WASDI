SubscriptionsManageController = (function () {
    function SubscriptionsManageController(
        $scope,
        $window,
        oClose,
        oConstantsService,
        oSubscriptionService,
        oOrganizationService,
        oTranslate
    ) {
        this.m_oScope = $scope;
        this.m_oWindow = $window;
        this.m_oScope.m_oController = this;
        this.m_oClose = oClose;
        this.m_oTranslate = oTranslate;
        this.m_oConstantsService = oConstantsService;
        this.m_oSubscriptionService = oSubscriptionService;
        this.m_oOrganizationService = oOrganizationService;

        $scope.close = function (result) {
            oClose(result, 500);
        }
    }

    SubscriptionsManageController.$inject = [
        '$scope',
        '$window',
        'close',
        'ConstantsService',
        'SubscriptionService',
        'OrganizationService',
        '$translate'
    ]; 
    return SubscriptionsManageController
})(); 
window.SubscriptionsManageController = SubscriptionsManageController;