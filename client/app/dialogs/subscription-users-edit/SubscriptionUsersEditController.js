let SubscriptionUsersEditController = (function () {
    function SubscriptionUsersEditController($scope, oClose, oExtras, oSubscriptionService, oModalService) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.oExtras = oExtras;

        this.m_oSubscriptionService = oSubscriptionService;
        this.m_oModalService = oModalService; 

        this.m_sSelectedSubscriptionId = this.oExtras.subscriptionId;
        this.m_aoUsersList = oExtras.users;

        $scope.close = function (result) {
            oClose(result, 500)
        }
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

                return true;
            }
        );
    }
    SubscriptionUsersEditController.prototype.openShareUsersModal = function (sSubscriptionId) {
        console.log(sSubscriptionId);

        let oController = this;
        this.m_oSubscriptionService.getSubscriptionById(sSubscriptionId).then(
            function (data) {
                if (utilsIsObjectNullOrUndefined(data.data) === false) {
                    console.log(data.data)
                    oController.m_oModalService.showModal({
                        templateUrl: "dialogs/share-subscription/ShareSubscriptionDialog.html", 
                        controller: "ShareSubscriptionController", 
                        inputs: {
                            extras: {
                                subscription: data.data
                            }
                        }
                    }).then(function (modal) {
                        modal.element.modal({
                            backdrop: 'static'
                        })
                        modal.close.then(function (result) {
                            oController.showUsersBySubscription(result); 
                        })
                    })
                    
                } else {
                    utilsVexDialogAlertTop(
                        "GURU MEDITATION<br>ERROR IN GETTING THE SUBSCRIPTION BY ID"
                    );
                }
                return true;
            }
        )
    }

    SubscriptionUsersEditController.$inject = [
        "$scope",
        "close",
        "extras",
        "SubscriptionService", 
        "ModalService"
    ];
    return SubscriptionUsersEditController;
})();
window.SubscriptionUsersEditController = SubscriptionUsersEditController; 