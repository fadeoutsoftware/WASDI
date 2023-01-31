let OrgUsersEditController = (function () {
    function OrgUsersEditController($scope, oClose, oExtras, oOrganizationService, oModalService, oTranslate) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.oExtras = oExtras;
        this.m_oTranslate = oTranslate;

        this.m_oOrganizationService = oOrganizationService;
        this.m_oModalService = oModalService;

        this.m_sUserEmail = "";

        this.m_sSelectedOrganizationId = this.oExtras.organizationId;
        this.m_aoUsersList = oExtras.users;
        this.m_bLoadingUsers = true;

        $scope.close = function (result) {
            oClose(result, 500)
        }
    }

    OrgUsersEditController.prototype.shareOrganization = function (sUserId) {
        let sOrganizationId = this.m_sSelectedOrganizationId;
        console.log("OrgUsersEditController.shareOrganization | sOrganizationId: ", sOrganizationId);
        console.log("OrgUsersEditController.shareOrganization | sUserId: ", sUserId);

        let oController = this;
        if (utilsIsObjectNullOrUndefined(sUserId) === true) {
            return false;
        }

        if (oController.m_aoUsersList.some(user => user.userId === sUserId)) {
            utilsVexDialogAlertTop(
                `${sUserId} IS ALREADY PART OF THIS ORGANIZATION`
            );
            return false;
        }

        this.m_oOrganizationService.addOrganizationSharing(this.m_sSelectedOrganizationId, sUserId).then(
            function (data) {
                if (utilsIsObjectNullOrUndefined(data.data) === false) {

                    console.log("OrgUsersEditController.shareOrganization | data.data: ", data.data);

                    if (data.data.boolValue) {
                        console.log(data.data)
                        let oDialog = utilsVexDialogAlertBottomRightCorner(
                            `${sUserId} WAS SUCCESSFULLY ADDED`
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
                        "GURU MEDITATION<br>ERROR IN SHARING THE ORGANIZATION"
                    );
                }
                return true;
            }
        )
    }

    OrgUsersEditController.prototype.unshareOrganization = function (sOrganizationId, sUserId) {
        let oController = this;
        
        let sConfirmMsg = `Confirm unsharing with ${sUserId}`
        
        let oUnshareReviewCallback = function (value) {
                oController.m_oOrganizationService.removeOrganizationSharing(sOrganizationId, sUserId).then(
                    function (data) {
                        if (utilsIsObjectNullOrUndefined(data.data) === false) {
                            // oController.m_aoUsersList = data.data;
                            console.log("EditUserController.unshareOrganization | data.data: ", data.data);
                            if (data.data.boolValue) {
                                oController.showUsersByOrganization(sOrganizationId);
                            }
                        } else {
                            utilsVexDialogAlertTop(
                                "GURU MEDITATION<br>ERROR IN UNSHARING THE ORGANIZATION"
                            );
                        }
                        return true;
                    }
            );
        }
        
        utilsVexDialogConfirm(sConfirmMsg, oUnshareReviewCallback); 
    }

    OrgUsersEditController.prototype.showUsersByOrganization = function (sOrganizationId) {
        var oController = this;

        this.m_oOrganizationService.getUsersBySharedOrganization(sOrganizationId).then(
            function (data) {
                if (utilsIsObjectNullOrUndefined(data.data) === false) {
                    oController.m_aoUsersList = data.data;
                } else {
                    utilsVexDialogAlertTop(
                        "GURU MEDITATION<br>ERROR IN GETTING THE LIST OF USERS OF THE ORGANIZATION"
                    );
                }

                oController.m_bLoadingUsers = false;

                return true;
            }
        );
    }

    OrgUsersEditController.$inject = [
        "$scope",
        "close",
        "extras",
        "OrganizationService", 
        "ModalService",
        '$translate'
    ];
    return OrgUsersEditController;
})();
window.OrgUsersEditController = OrgUsersEditController; 