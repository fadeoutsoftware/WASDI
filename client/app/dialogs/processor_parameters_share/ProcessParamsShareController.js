/**
 * Created by bspurgeon08 on 19/09/2022
 */

let ProcessParamsShareController = (function () {
    function ProcessParamsShareController(
        oAdminDashboardService,
        oConstantsService,
        oExtras,
        oTranslate,
        $scope,
        $timeout
    ) {
        this.m_oAdminDashboardService = oAdminDashboardService;
        this.m_oConstantsService = oConstantsService;
        this.m_oExtras = oExtras;

        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oTranslate = oTranslate;
        this.m_oTimeout = $timeout;

        this.m_aoParametersPermissionsList = [];
        this.sResourceType = "PROCESSORPARAMETERSTEMPLATE";

        this.m_sUserEmail = "";
        this.m_sRights = "read";
        this.m_sTemplateId = this.m_oExtras.template.templateId;
        this.m_sOwnerEmail = this.m_oExtras.template.userId;
        
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
        sUserEmail,
        sRights
    ) {

        let oController = this;
        //check that user has provided an email
        if (utilsIsStrNullOrEmpty(sUserEmail) === true) {
            utilsVexDialogAlertTop(
                this.m_oTranslate.instant("DIALOG_PARAMS_SHARE_ADD_ERROR1")
            );
            return false;
        }
        //Check that user has not provided their own email
        if (sUserEmail === oController.m_sOwnerEmail) {
            utilsVexDialogAlertTop(
                this.m_oTranslate.instant("DIALOG_PARAMS_SHARE_ADD_ERROR2")
            );
            return false;
        }

        this.m_oAdminDashboardService
            .addResourcePermission(
                oController.sResourceType,
                sResourceId,
                sUserEmail,
                sRights
            )
            .then(function (data) {
                oController.m_oTimeout(function () {
                    oController.findParametersPermissions(
                        oController.m_sTemplateId
                    );
                }, 500);

                oController.m_sUserEmail = "";
                oController.m_sRights = "read";
            });
    };
    ProcessParamsShareController.prototype.removeParametersPermission =
        function (sResourceId, sUserId) {
            if (utilsIsStrNullOrEmpty(sResourceId) === true) {
                return false;
            }

            let sConfirmMsg1 = this.m_oTranslate.instant("DIALOG_PARAMS_SHARE_REMOVE_CONFIRM");
            let sConfirmMsg2 = "?";
            let oController = this;
            utilsVexDialogConfirm(
                sConfirmMsg1 + sUserId + sConfirmMsg2,
                function (value) {
                    if (value) {
                        oController.m_oAdminDashboardService
                            .removeResourcePermission(
                                oController.sResourceType,
                                sResourceId,
                                sUserId
                            )
                            .then(function (data) {
                                oController.m_oTimeout(function () {
                                    oController.findParametersPermissions(
                                        oController.m_sTemplateId
                                    );
                                }, 500);
                            });
                    }
                }
            );
        };
    ProcessParamsShareController.$inject = [
        "AdminDashboardService",
        "ConstantsService",
        "extras",
        "$translate",
        "$scope",
        "$timeout",
    ];

    return ProcessParamsShareController;
})();
window.ProcessParamsShareController = ProcessParamsShareController;
