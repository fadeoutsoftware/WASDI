SubscriptionsManageController = (function () {
    function SubscriptionsManageController(
        $scope,
        $window,
        oClose,
        oConstantsService,
        oSubscriptionService,
        oOrganizationService,
        oTranslate,
        oProjectService,
        oModalService
    ) {
        this.m_oScope = $scope;
        this.m_oWindow = $window;
        this.m_oScope.m_oController = this;
        this.m_oClose = oClose;
        this.m_oTranslate = oTranslate;
        this.m_oConstantsService = oConstantsService;
        this.m_oSubscriptionService = oSubscriptionService;
        this.m_oOrganizationService = oOrganizationService;
        this.m_oProjectService = oProjectService;
        this.m_oModalService = oModalService;

        this.m_bLoadingOrganizations = true;
        this.m_bLoadingSubscriptions = true;
        this.m_bLoadingProjects = true;
        this.m_bIsLoading = false;

        this.m_aoSubscriptionProjects = [];

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
        if (utilsIsStrNullOrEmpty(sSubscriptionId)) {
            return false;
        }

        this.m_oModalService.showModal({
            templateUrl: "dialogs/subscription-users/SubscriptionUsersDialog.html",
            controller: 'SubscriptionUsersController',
            inputs: {
                extras: {
                    subscriptionId: sSubscriptionId
                }
            }
        }).then(function (modal) {
            modal.element.modal({
                backdrop: 'static'
            })
            modal.close.then(function () {
            });
        });
    }

    SubscriptionsManageController.prototype.showProjectsBySubscription = function (sSubscriptionId, sSubscriptionName) {
        this.m_oEditSubscription = {};

        if (utilsIsStrNullOrEmpty(sSubscriptionId)) {
            return false;
        }

        var oController = this;

        this.m_oModalService.showModal({
            templateUrl: "dialogs/subscription-projects/SubscriptionProjectsDialog.html",
            controller: 'SubscriptionProjectsController',
            inputs: {
                extras: {
                    subscriptionId: sSubscriptionId,
                    subscriptionName: sSubscriptionName
                }
            }
        }).then(function (modal) {
            modal.element.modal({
                backdrop: 'static'
            })
            modal.close.then(function () {
                oController.initializeProjectsInfo();
            })

        })
    }

    SubscriptionsManageController.prototype.showSubscriptionEditForm = function (sSubscriptionId, bEditMode) {
        var oController = this;

        let oOldSubscription = {
            subscriptionId: sSubscriptionId
        };

        oController.m_oModalService.showModal({
            templateUrl: "dialogs/subscription_editor/SubscriptionEditorDialog.html",
            controller: "SubscriptionEditorController",
            inputs: {
                extras: {
                    subscription: oOldSubscription,
                    editMode: bEditMode
                }
            }
        }).then(function (modal) {
            modal.element.modal({
                backdrop: 'static'
            })
            modal.close.then(function () {
                oController.initializeSubscriptionsInfo();
            })
        })
    }



    SubscriptionsManageController.prototype.deleteSubscription = function (oSubscription) {
        let sConfirmMsgOwner = "Are you sure you want to delete this subscription?";
        let sConfirmMsgShare = "Are you sure you want to remove your permissions for this subscription?";
        let oController = this;

        let oCallbackFunction = function (value) {
            if (value) {
                oController.m_oSubscriptionService.deleteSubscription(oSubscription.subscriptionId)
                    .then(function (response) {
                        if (!utilsIsObjectNullOrUndefined(response) && response.status === 200) {
                            let sMessage = "SUBSCRIPTION DELETED<br>READY";

                            if (!utilsIsObjectNullOrUndefined(response.data) && !utilsIsStrNullOrEmpty(response.data.message)) {
                                if (response.data.message !== "Done") {
                                    sMessage += "<br><br>" + oController.m_oTranslate.instant(response.data.message);
                                }
                            }
                            var oDialog = utilsVexDialogAlertBottomRightCorner(sMessage);
                            utilsVexCloseDialogAfter(4000, oDialog);
                        } else {
                            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN DELETING SUBSCRIPTION");
                        }

                        oController.initializeSubscriptionsInfo();
                    }, function (error) {
                        let sErrorMessage = "GURU MEDITATION<br>ERROR IN DELETING SUBSCRIPTION";

                        if (!utilsIsObjectNullOrUndefined(error.data) && !utilsIsStrNullOrEmpty(error.data.message)) {
                            sErrorMessage += "<br><br>" + oController.m_oTranslate.instant(error.data.message);
                        }
                        utilsVexDialogAlertTop(sErrorMessage);
                    })
            }

        }
        if (oSubscription.adminRole === true) {
            console.log(sConfirmMsgOwner)
            utilsVexDialogConfirm(sConfirmMsgOwner, oCallbackFunction);
        } else {
            console.log(sConfirmMsgShare)
            utilsVexDialogConfirm(sConfirmMsgShare, oCallbackFunction);
        }

    }

    SubscriptionsManageController.$inject = [
        '$scope',
        '$window',
        'close',
        'ConstantsService',
        'SubscriptionService',
        'OrganizationService',
        '$translate',
        'ProjectService',
        'ModalService'
    ];
    return SubscriptionsManageController
})();
window.SubscriptionsManageController = SubscriptionsManageController;