let ShareOrganizationController = (function () {
    function ShareOrganizationController(
        $scope,
        oClose,
        oExtras,
        oAdminDashboardService,
        oOrganizationService
    ) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;

        this.m_oAdminDashboardService = oAdminDashboardService;
        this.m_oOrganizationService = oOrganizationService;
        this.m_sSelectedOrganizationId = oExtras.organization.organizationId;
        this.m_aoUsersList = oExtras.usersList

        //Input Model for search
        this.m_sUserPartialName = "";
        this.m_aoMatchingUsersList = [];

        console.log(this.m_sSelectedOrganizationId);

        $scope.close = function (result) {
            console.log()
            oClose(result, 500)
        }
    }

    ShareOrganizationController.prototype.findUsersByPartialName = function (sUserPartialName) {
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

    ShareOrganizationController.prototype.shareOrganization = function (sUserId) {
        let sOrganizationId = this.m_sSelectedOrganizationId;
        console.log("EditUserController.shareOrganization | sOrganizationId: ", sOrganizationId);
        console.log("EditUserController.shareOrganization | sUserId: ", sUserId);

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

                    console.log("EditUserController.shareOrganization | data.data: ", data.data);

                    if (data.data.boolValue) {
                        console.log(data.data)
                        let oDialog = utilsVexDialogAlertBottomRightCorner(
                            `${sUserId} WAS SUCCESSFULLY ADDED`
                        );
                        utilsVexCloseDialogAfter(4000, oDialog);

                        oController.m_aoUsersList.push({userId: sUserId})
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

    ShareOrganizationController.$inject = [
        "$scope",
        "close",
        "extras",
        "AdminDashboardService",
        "OrganizationService"
    ];
    return ShareOrganizationController;
})();
window.ShareOrganizationController = ShareOrganizationController; 