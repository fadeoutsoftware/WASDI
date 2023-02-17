let SubscriptionUsersController = (function () {
    function SubscriptionUsersController($scope, oClose, oExtras, oSubscriptionService, oModalService, oTranslate) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.oExtras = oExtras;
        this.m_oTranslate = oTranslate;

        this.m_oSubscriptionService = oSubscriptionService;
        this.m_oModalService = oModalService; 

        this.m_sUserEmail = "";

        this.m_sSelectedSubscriptionId = this.oExtras.subscriptionId;

        this.m_bLoadingUsers = true;
        this.showUsersBySubscription();

        $scope.close = function (result) {
            oClose(result, 500)
        }
    }

    SubscriptionUsersController.prototype.shareSubscription = function (sUserId) {
        let oController = this;

        if (utilsIsObjectNullOrUndefined(sUserId)) {
            return false;
        }

        if (oController.m_aoUsersList.some(user => user.userId === sUserId)) {
            utilsVexDialogAlertTop(
                `THIS SUBSCRIPTION HAS ALREADY BEEN SHARED WITH ${sUserId}`
            );
            return false;
        }

        this.m_oSubscriptionService.addSubscriptionSharing(this.m_sSelectedSubscriptionId, sUserId).then(
            function (response) {
                if (!utilsIsObjectNullOrUndefined(response.data)) {
                    if (response.data.message === "Done") {
                        let oDialog = utilsVexDialogAlertBottomRightCorner(
                            `SUBSCRIPTION SUCCESSFULLY SHARED WITH ${sUserId}`
                        );
                        utilsVexCloseDialogAfter(4000, oDialog);

                        oController.m_aoUsersList.push({ userId: sUserId });
                        oController.m_sUserEmail = "";
                    } else if (response.data.message === "Already Shared.") {
                        let oDialog = utilsVexDialogAlertBottomRightCorner(
                            `SUBSCRIPTION ALREADY SHARED WITH ${sUserId}`
                        );
                        utilsVexCloseDialogAfter(4000, oDialog);

                        oController.m_aoUsersList.push({ userId: sUserId });
                        oController.m_sUserEmail = "";
                    }
                }

                return true;
            }, function (error) {
                let sErrorMessage = "GURU MEDITATION<br>ERROR IN SHARING THE SUBSCRIPTION";

                if (!utilsIsObjectNullOrUndefined(error.data) && !utilsIsStrNullOrEmpty(error.data.message)) {
                    sErrorMessage += "<br><br>" + oController.m_oTranslate.instant(error.data.message);
                }

                utilsVexDialogAlertTop(sErrorMessage);
            }
        )
    }

    SubscriptionUsersController.prototype.unshareSubscription = function (sUserId) {
        let oController = this;
        
        let sConfirmMsg = `Confirm unsharing with ${sUserId}`
        
        let oUnshareCallback = function (value) {
            if (value) {
                oController.m_oSubscriptionService.removeSubscriptionSharing(oController.m_sSelectedSubscriptionId, sUserId).then(
                    function (response) {
                        if (!utilsIsObjectNullOrUndefined(response)
                                && !utilsIsObjectNullOrUndefined(response.data) && response.status === 200) {
                            let oDialog = utilsVexDialogAlertBottomRightCorner(
                                `SUBSCRIPTION SUCCESSFULLY UNSHARED WITH ${sUserId}`
                            );
                            utilsVexCloseDialogAfter(4000, oDialog);

                            oController.showUsersBySubscription();
                        } else {
                            utilsVexDialogAlertTop(
                                "GURU MEDITATION<br>ERROR IN UNSHARING THE SUBSCRIPTION"
                            );
                        }
                        return true;
                    }, function (error) {
                        let sErrorMessage = "GURU MEDITATION<br>ERROR IN UNSHARING THE SUBSCRIPTION";

                        if (!utilsIsObjectNullOrUndefined(error.data) && !utilsIsStrNullOrEmpty(error.data.message)) {
                            sErrorMessage += "<br><br>" + oController.m_oTranslate.instant(error.data.message);
                        }

                        utilsVexDialogAlertTop(sErrorMessage);
                    }
                );
            }
        }
        
        utilsVexDialogConfirm(sConfirmMsg, oUnshareCallback); 
    }

    SubscriptionUsersController.prototype.showUsersBySubscription = function () {
        var oController = this;

        this.m_oSubscriptionService.getUsersBySharedSubscription(this.m_sSelectedSubscriptionId).then(
            function (response) {
                if (!utilsIsObjectNullOrUndefined(response.data)) {
                    oController.m_aoUsersList = response.data;
                } else {
                    utilsVexDialogAlertTop(
                        "GURU MEDITATION<br>ERROR IN GETTING THE LIST OF USERS OF THE SUBSCRIPTION"
                    );
                }

                oController.m_bLoadingUsers = false;

                return true;
            }, function (error) {
                let sErrorMessage = "GURU MEDITATION<br>ERROR IN GETTING THE LIST OF USERS OF THE SUBSCRIPTION";

                if (!utilsIsObjectNullOrUndefined(error.data) && !utilsIsStrNullOrEmpty(error.data.message)) {
                    sErrorMessage += "<br><br>" + oController.m_oTranslate.instant(error.data.message);
                }

                utilsVexDialogAlertTop(sErrorMessage);

                oController.m_bLoadingUsers = false;
            }
        );
    }

    SubscriptionUsersController.$inject = [
        "$scope",
        "close",
        "extras",
        "SubscriptionService", 
        "ModalService",
        '$translate'
    ];
    return SubscriptionUsersController;
})();
window.SubscriptionUsersController = SubscriptionUsersController; 