let ShareSubscriptionController = (function () {
    function ShareSubscriptionController(
        $scope,
        oClose,
        oExtras,
        oAdminDashboardService,
        oSubscriptionService
    ) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;

        this.m_oAdminDashboardService = oAdminDashboardService;
        this.m_oSubscriptionService = oSubscriptionService;
        this.m_sSelectedSubscriptionId = oExtras.subscription.subscriptionId;

        //Input Model for search
        this.m_sUserPartialName = "";
        this.m_aoMatchingUsersList = [];

        console.log(this.m_sSelectedSubscriptionId);

        $scope.close = function (result) {
            console.log()
            oClose(result, 500)
        }
    }

    ShareSubscriptionController.prototype.findUsersByPartialName = function (sUserPartialName) {
        if (utilsIsStrNullOrEmpty(sUserPartialName) === true) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>AT LEAST THREE CHARACTERS MUST BE PROVIDED");

            return false;
        }

        utilsRemoveSpaces(sUserPartialName);

        if (sUserPartialName.length < 3) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>AT LEAST THREE CHARACTERS MUST BE PROVIDED");

            return false;
        }

        let oController = this;
        this.m_oAdminDashboardService.findUsersByPartialName(sUserPartialName).then(
            function (data) {
                if (utilsIsObjectNullOrUndefined(data.data) === false) {
                    oController.m_aoMatchingUsersList = data.data;
                } else {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN FINDING USERS");
                }
                // oController.clearForm();
                return true;
            },
            function (error) {
                console.log("EditUserController.findUsersByPartialName | error.data.message: ", error.data.message);

                let errorMessage = oController.m_oTranslate.instant(error.data.message);

                utilsVexDialogAlertTop(errorMessage);
            }
        )
    }

    ShareSubscriptionController.prototype.shareSubscription = function (sUserId) {
        let sSubscriptionId = this.m_sSelectedSubscriptionId;
        console.log("EditUserController.shareSubscription | sSubscriptionId: ", sSubscriptionId);
        console.log("EditUserController.shareSubscription | sUserId: ", sUserId);

        if (utilsIsObjectNullOrUndefined(sUserId) === true) {
            return false;
        }

        let oController = this;

        this.m_oSubscriptionService.addSubscriptionSharing(this.m_sSelectedSubscriptionId, sUserId).then(
            function (data) {
                if (utilsIsObjectNullOrUndefined(data.data) === false) {
                    // oController.m_aoUsersList = data.data;
                    console.log("EditUserController.shareSubscription | data.data: ", data.data);

                    if (data.data.boolValue) {
                        // oController.showUsersBySubscription(sSubscriptionId);
                        console.log('User added')
                    }
                } else {
                    utilsVexDialogAlertTop(
                        "GURU MEDITATION<br>ERROR IN SHARING THE SUBSCRIPTION"
                    );
                }
                return true;
            }
        )

    }

    ShareSubscriptionController.$inject = [
        "$scope",
        "close",
        "extras",
        "AdminDashboardService",
        "SubscriptionService"
    ];
    return ShareSubscriptionController;
})();
window.ShareSubscriptionController = ShareSubscriptionController; 