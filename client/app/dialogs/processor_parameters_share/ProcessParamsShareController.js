/**
 * Created by bspurgeon08 on 19/09/2022
 */

let ProcessParamsShareController = (function () {
    function ProcessParamsShareController(
        oAdminDashboardService,
        oConstantsService,
        oExtras,
        $scope
    ) {
        this.m_oAdminDashboardService = oAdminDashboardService;
        this.m_oConstantsService = oConstantsService;
        this.m_oExtras = oExtras;

        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;

        this.m_aoParametersPermissionsList = [];
        this.sResourceType = "PROCESSORPARAMETERSTEMPLATE";

        this.m_sUserEmail = null;
        this.m_sTemplateId = this.m_oExtras.template.templateId;
        this.m_sOwnerEmail = this.m_oExtras.template.userId;

        console.log(this.m_oExtras.template);
        this.findParametersPermissions(this.m_oExtras.template.templateId);
    }
    //Return list of users with whom the parameters have been shared
    ProcessParamsShareController.prototype.findParametersPermissions =
        function (sResourceId) {
            let oController = this;

            this.m_oAdminDashboardService
                .findResourcePermissions(oController.sResourceType, sResourceId)
                .then(function (data) {
                    if (utilsIsObjectNullOrUndefined(data.data) === false) {
                        oController.m_aoParametersPermissionsList = data.data;
                        console.log(data);
                        console.log(oController.m_aoParametersPermissionsList);
                    } else {
                        utilsVexDialogAlertTop(
                            "GURU MEDITATION<br>ERROR IN FINDING RESOURCE PERMISSIONS"
                        );
                    }
                });
        };
    //Add a user (by email) to list of users with whom the parameters are shared
    ProcessParamsShareController.prototype.addParametersPermission = function (
        sResourceId,
        sUserEmail
    ) {
        console.log(
            "ProcessParamsShareController.findParametersPermissions | sResourceId: " +
                sResourceId
        );
        console.log(
            "ProcessParamsShareController.findParametersPermissions | sUserEmail: " +
                sUserEmail
        );

        let oController = this;
        //check that user has provided an email
        if (utilsIsStrNullOrEmpty(sUserEmail) === true) {
            utilsVexDialogAlertTop(
                "GURU MEDITATION<br>A User Email must be provided"
            );
            return false;
        }
        //Check that user has not provided their own email
        if (sUserEmail === oController.m_sOwnerEmail) {
            utilsVexDialogAlertTop(
                "IT IS NOT POSSIBLE TO SHARE A RESOURCE WITH YOURSELF."
            );
            return false;
        }

        this.m_oAdminDashboardService
            .addResourcePermission(
                oController.sResourceType,
                sResourceId,
                sUserEmail
            )
            .then(function (data) {
                console.log(data);
            });
    };
    ProcessParamsShareController.prototype.removeParametersPermission =
        function (sResourceId, sUserId) {
            if (utilsIsStrNullOrEmpty(sResourceId) === true) {
                return false;
            }
            let oController = this;
            this.m_oAdminDashboardService
                .removeResourcePermission(
                    oController.sResourceType,
                    sResourceId,
                    sUserId
                )
                .then(function (data) {
                    console.log(data);
                });
        };
    ProcessParamsShareController.$inject = [
        "AdminDashboardService",
        "ConstantsService",
        "extras",
        "$scope",
    ];

    return ProcessParamsShareController;
})();
window.ProcessParamsShareController = ProcessParamsShareController;