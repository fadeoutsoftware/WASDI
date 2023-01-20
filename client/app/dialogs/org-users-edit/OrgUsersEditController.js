let OrgUsersEditController = (function () {
    function OrgUsersEditController($scope, oClose, oExtras, oOrganizationService) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.oExtras = oExtras;

        this.m_oOrganizationService = oOrganizationService;

        this.m_sSelectedOrganizationId = this.oExtras.organizationId; 
        this.m_aoUsersList = oExtras.users;

        $scope.close = function (result) {
            oClose(result, 500)
        }
    }
    OrgUsersEditController.prototype.unshareOrganization = function (sOrganizationId, sUserId) {
        let oController = this;
        
        this.m_oOrganizationService.removeOrganizationSharing(sOrganizationId, sUserId).then(
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

                return true;
            }
        );
    }


    OrgUsersEditController.$inject = [
        "$scope",
        "close",
        "extras",
        "OrganizationService"
    ];
    return OrgUsersEditController;
})();
window.OrgUsersEditController = OrgUsersEditController; 