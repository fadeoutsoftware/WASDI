let SubscriptionUsersEditController = (function () {
    function SubscriptionUsersEditController($scope, oClose, oExtras, oSubscriptionService, oModalService, oTranslate) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.oExtras = oExtras;
        this.m_oTranslate = oTranslate;

        this.m_oSubscriptionService = oSubscriptionService;
        this.m_oModalService = oModalService; 

        this.m_sUserEmail = "";

        this.m_sSelectedSubscriptionId = this.oExtras.subscriptionId;
        this.m_aoUsersList = oExtras.users;

        this.m_bLoadingUsers = true;

        $scope.close = function (result) {
            oClose(result, 500)
        }
    }

    SubscriptionUsersEditController.prototype.shareSubscription = function (sUserId) {
        let sSubscriptionId = this.m_sSelectedSubscriptionId;
        console.log("SubscriptionUsersEditController.shareSubscription | sSubscriptionId: ", sSubscriptionId);
        console.log("SubscriptionUsersEditController.shareSubscription | sUserId: ", sUserId);

        let oController = this;

        if (utilsIsObjectNullOrUndefined(sUserId) === true) {
            return false;
        }

        if (oController.m_aoUsersList.some(user => user.userId === sUserId)) {
            utilsVexDialogAlertTop(
                `THIS SUBSCRIPTION HAS ALREADY BEEN SHARED WITH ${sUserId}`
            );
            return false;
        }

        this.m_oSubscriptionService.addSubscriptionSharing(this.m_sSelectedSubscriptionId, sUserId).then(
            function (data) {
                if (utilsIsObjectNullOrUndefined(data.data) === false) {
                    // oController.m_aoUsersList = data.data;
                    console.log("SubscriptionUsersEditController.shareSubscription | data.data: ", data.data);

                    if (data.data.boolValue) {
                        // oController.showUsersBySubscription(sSubscriptionId);
                        let oDialog = utilsVexDialogAlertBottomRightCorner(
                            `SUBSCRIPTION SUCCESSFULLY SHARED WITH ${sUserId}`
                        );
                        utilsVexCloseDialogAfter(4000, oDialog);

                        oController.m_aoUsersList.push({ userId: sUserId });
                        oController.m_sUserEmail="";
                    } else {
                        var oDialog = utilsVexDialogAlertBottomRightCorner(oController.m_oTranslate.instant(data.data.stringValue));
                        utilsVexCloseDialogAfter(5000, oDialog);
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

    SubscriptionUsersEditController.prototype.unshareSubscription = function (sSubscriptionId, sUserId) {
        let oController = this;
        
        let sConfirmMsg = `Confirm unsharing with ${sUserId}`
        
        let oUnshareReviewCallback = function (value) {
                oController.m_oSubscriptionService.removeSubscriptionSharing(sSubscriptionId, sUserId).then(
                function (data) {
                    if (utilsIsObjectNullOrUndefined(data.data) === false) {
                        // oController.m_aoUsersList = data.data;
                        console.log("EditUserController.unshareSubscription | data.data: ", data.data);
                        if (data.data.boolValue) {
                            oController.showUsersBySubscription(sSubscriptionId);
                        }
                    } else {
                        utilsVexDialogAlertTop(
                            "GURU MEDITATION<br>ERROR IN UNSHARING THE SUBSCRIPTION"
                        );
                    }
                    return true;
                }
            );
        }
        
        utilsVexDialogConfirm(sConfirmMsg, oUnshareReviewCallback); 
    }

    SubscriptionUsersEditController.prototype.showUsersBySubscription = function (sSubscriptionId) {
        var oController = this;

        this.m_oSubscriptionService.getUsersBySharedSubscription(sSubscriptionId).then(
            function (data) {
                if (utilsIsObjectNullOrUndefined(data.data) === false) {
                    oController.m_aoUsersList = data.data;
                } else {
                    utilsVexDialogAlertTop(
                        "GURU MEDITATION<br>ERROR IN GETTING THE LIST OF USERS OF THE SUBSCRIPTION"
                    );
                }

                oController.m_bLoadingUsers = false;

                return true;
            }
        );
    }

    SubscriptionUsersEditController.$inject = [
        "$scope",
        "close",
        "extras",
        "SubscriptionService", 
        "ModalService",
        '$translate'
    ];
    return SubscriptionUsersEditController;
})();
window.SubscriptionUsersEditController = SubscriptionUsersEditController; 