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

        this.m_bLoadingOrganizations = true;
        this.m_bLoadingSubscriptions = true;
        this.m_bLoadingProjects = true;
        this.m_bIsLoading = false;


        this.m_aoSubscriptions = [];
        this.m_bLoadingSubscriptions = true;

        this.initializeSubscriptionsInfo();

        $scope.close = function (result) {
            oClose(result, 500);
        }
    }

    SubscriptionsManageController.prototype.initializeSubscriptionsInfo = function () {
        let oController = this;

        this.m_oSubscriptionService.getSubscriptionsListByUser().then(function (response) {
            if (!utilsIsObjectNullOrUndefined(response)
                && !utilsIsObjectNullOrUndefined(response.data) && response.status === 200) {
                oController.m_aoSubscriptions = response.data;
            } else {
                utilsVexDialogAlertTop(
                    "GURU MEDITATION<br>ERROR IN GETTING THE LIST OF SUBSCRIPTIONS"
                );
            }
            oController.m_bLoadingSubscriptions = false;
            console.log(oController.m_aoSubscriptions)

            return true;
        }, function (error) {
            let sErrorMessage = "GURU MEDITATION<br>ERROR IN GETTING THE LIST OF SUBSCRIPTIONS";

            if (!utilsIsObjectNullOrUndefined(error.data) && !utilsIsStrNullOrEmpty(error.data.message)) {
                sErrorMessage += "<br><br>" + oController.m_oTranslate.instant(error.data.message);
            }

            utilsVexDialogAlertTop(sErrorMessage);
        });
    }

    SubscriptionsManageController.prototype.showUsersBySubscription = function (sSubscriptionId) {

    }

    SubscriptionsManageController.prototype.showProjectsBySubscription = function (sSubscriptionId, sSubscriptionName) {

    }

    SubscriptionsManageController.prototype.showSubscriptionEditForm = function (sSubscriptionId, bIsOwner) {

    }

    SubscriptionsManageController.prototype.deleteSubscription = function (sSubscriptionId) {
        
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