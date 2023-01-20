let EditSubscriptionsController = (function () {
    function EditSubscriptionsController(
        $scope,
        oClose,
        oExtras,
        oSubscriptionService
    ) {
        this.m_oScope = $scope;
        this.m_oExtras = oExtras;

        this.m_oSubscriptionService = oSubscriptionService
    }

    EditSubscriptionsController.$inject = [
        '$scope',
        'close',
        'extras',
        'oSubscriptionService'
    ];
    return EditSubscriptionsController
})();
window.EditSubscriptionsController = EditSubscriptionsController