let OrganizationUsersController = (function () {
    function OrganizationUsersController($scope, oClose, oExtras, oOrganizationService, oModalService, oTranslate) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.oExtras = oExtras;
        this.m_oTranslate = oTranslate;

        this.m_oOrganizationService = oOrganizationService;
        this.m_oModalService = oModalService;

        this.m_sUserEmail = "";
        this.m_sRights = "read";

        this.m_sSelectedOrganizationId = this.oExtras.organizationId;

        this.m_bLoadingUsers = true;
        this.showUsersByOrganization();

        $scope.close = function (result) {
            oClose(result, 500)
        }
    }

    OrganizationUsersController.prototype.shareOrganization = function (sUserId, sRights) {
        let oController = this;

        if (utilsIsObjectNullOrUndefined(sUserId)) {
            return false;
        }

        if (oController.m_aoUsersList.some(user => user.userId === sUserId)) {
            utilsVexDialogAlertTop(
                `${sUserId} IS ALREADY PART OF THIS ORGANIZATION`
            );
            return false;
        }

        if (utilsIsStrNullOrEmpty(sRights)) {
            sRights = "read"
        }        

        this.m_oOrganizationService.addOrganizationSharing(this.m_sSelectedOrganizationId, sUserId, sRights).then(
            function (response) {
                if (!utilsIsObjectNullOrUndefined(response.data)) {
                    if (response.data.message === "Done") {
                        let oDialog = utilsVexDialogAlertBottomRightCorner(
                            `ORGANIZATION SUCCESSFULLY SHARED WITH ${sUserId}`
                        );
                        utilsVexCloseDialogAfter(4000, oDialog);

                        oController.m_aoUsersList.push({ userId: sUserId , permissions: sRights});
                        oController.m_sUserEmail = "";
                        oController.m_sRights = "read";
                    } else if (response.data.message === "Already Shared.") {
                        let oDialog = utilsVexDialogAlertBottomRightCorner(
                            `ORGANIZATION ALREADY SHARED WITH ${sUserId}`
                        );
                        utilsVexCloseDialogAfter(4000, oDialog);

                        oController.m_aoUsersList.push({ userId: sUserId });
                        oController.m_sUserEmail = "";
                    }
                }

                return true;
            }, function (error) {
                let sErrorMessage = "GURU MEDITATION<br>ERROR IN SHARING THE ORGANIZATION";

                if (!utilsIsObjectNullOrUndefined(error.data) && !utilsIsStrNullOrEmpty(error.data.message)) {
                    sErrorMessage += "<br><br>" + oController.m_oTranslate.instant(error.data.message);
                }

                utilsVexDialogAlertTop(sErrorMessage);
            }
        )
    }

    OrganizationUsersController.prototype.unshareOrganization = function (sUserId) {
        let oController = this;

        let sConfirmMsg = `Confirm unsharing with ${sUserId}`

        let oUnshareCallback = function (value) {
            if (value) {
                oController.m_oOrganizationService.removeOrganizationSharing(oController.m_sSelectedOrganizationId, sUserId).then(
                    function (response) {
                        if (!utilsIsObjectNullOrUndefined(response)
                                && !utilsIsObjectNullOrUndefined(response.data) && response.status === 200) {
                            let oDialog = utilsVexDialogAlertBottomRightCorner(
                                `ORGANIZATION SUCCESSFULLY UNSHARED WITH ${sUserId}`
                            );
                            utilsVexCloseDialogAfter(4000, oDialog);

                            oController.showUsersByOrganization(oController.m_sSelectedOrganizationId);
                        } else {
                            utilsVexDialogAlertTop(
                                "GURU MEDITATION<br>ERROR IN UNSHARING THE ORGANIZATION"
                            );
                        }
                        return true;
                    }, function (error) {
                        let sErrorMessage = "GURU MEDITATION<br>ERROR IN UNSHARING THE ORGANIZATION";

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

    OrganizationUsersController.prototype.showUsersByOrganization = function () {
        var oController = this;

        this.m_oOrganizationService.getUsersBySharedOrganization(this.m_sSelectedOrganizationId).then(
            function (response) {
                if (!utilsIsObjectNullOrUndefined(response.data)) {
                    oController.m_aoUsersList = response.data;
                } else {
                    utilsVexDialogAlertTop(
                        "GURU MEDITATION<br>ERROR IN GETTING THE LIST OF USERS OF THE ORGANIZATION"
                    );
                }

                oController.m_bLoadingUsers = false;

                return true;
            }, function (error) {
                let sErrorMessage = "GURU MEDITATION<br>ERROR IN GETTING THE LIST OF USERS OF THE ORGANIZATION";

                if (!utilsIsObjectNullOrUndefined(error.data) && !utilsIsStrNullOrEmpty(error.data.message)) {
                    sErrorMessage += "<br><br>" + oController.m_oTranslate.instant(error.data.message);
                }

                utilsVexDialogAlertTop(sErrorMessage);

                oController.m_bLoadingUsers = false;
            }
        );
    }

    OrganizationUsersController.$inject = [
        "$scope",
        "close",
        "extras",
        "OrganizationService", 
        "ModalService",
        '$translate'
    ];
    return OrganizationUsersController;
})();
window.OrganizationUsersController = OrganizationUsersController; 