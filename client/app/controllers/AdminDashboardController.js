/**
 * Created by p.petrescu on 31/08/2022.
 */

var AdminDashboardController = (function () {
    function AdminDashboardController(
        $scope,
        $location,
        oConstantsService,
        oTranslate,
        oAuthService,
        oAdminDashboardService,
        oProcessWorkspaceService,
        $state,
        $filter
    ) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oLocation = $location;
        this.m_oAuthService = oAuthService;
        this.m_oAdminDashboardService = oAdminDashboardService;
        this.m_oProcessWorkspaceService = oProcessWorkspaceService;
        this.m_oConstantsService = oConstantsService;

        this.m_sUserPartialName = "";
        this.m_aoUsersList = [];

        this.m_sUserId = "";
        this.m_sDateFrom = "";
        this.m_sDateTo = "";
        this.m_lTotalRunningTimeInMillis = null;

        this.m_sWorkspacePartialName = "";
        this.m_aoWorkspacesList = [];

        this.m_aoResourceTypeList = [
            "NODE",
            "PROCESSORPARAMETERSTEMPLATE",
            "PROCESSOR",
            "STYLE",
            "WORKFLOW",
            "WORKSPACE",
        ];

        this.m_sResourceType_add_remove = "";
        this.m_sResourceId_add_remove = null;
        this.m_sUserEmail_add_remove = null;

        this.m_sResourceType_search = "";
        this.m_sResourceId_search = null;
        this.m_sUserEmail_search = null;

        this.m_aoResourcePermissionsList = [];

        this.m_sNodeCode = "";
        this.m_oMetricsEntry = null;
        this.m_oMetricsEntryFormatted = null;

        this.m_bIsLoading = true;

        this.m_oState = $state;
        this.m_oFilter = $filter;

        this.m_oTranslate = oTranslate;

        this.m_sDateTo = new Date();
        this.m_sDateFrom = new Date(
            this.m_sDateTo.getFullYear(),
            this.m_sDateTo.getMonth(),
            this.m_sDateTo.getDate() - 7
        );

        if (utilsIsObjectNullOrUndefined(oConstantsService.getUser())) {
            this.m_oState.go("home");
        }

        this.isLoadingIconVisible = function () {
            // return false;
            if (this.m_oRabbitStompService.isReadyState() === false) {
                return true;
            }
            return false;
        };
    }

    AdminDashboardController.prototype.findUsersByPartialName = function (
        sUserPartialName
    ) {
        console.log(
            "AdminDashboardController.findUsersByPartialName | sUserPartialName:",
            sUserPartialName
        );

        this.m_aoUsersList = [];

        if (utilsIsStrNullOrEmpty(sUserPartialName) === true) {
            utilsVexDialogAlertTop(
                "GURU MEDITATION<br>AT LEAST THREE CHARACTERS MUST BE PROVIDED"
            );

            return false;
        }

        utilsRemoveSpaces(sUserPartialName);

        if (sUserPartialName.length < 3) {
            utilsVexDialogAlertTop(
                "GURU MEDITATION<br>AT LEAST THREE CHARACTERS MUST BE PROVIDED"
            );

            return false;
        }

        var oController = this;

        this.m_oAdminDashboardService
            .findUsersByPartialName(sUserPartialName)
            .then(
                function (data) {
                    if (utilsIsObjectNullOrUndefined(data.data) === false) {
                        oController.m_aoUsersList = data.data;
                    } else {
                        utilsVexDialogAlertTop(
                            "GURU MEDITATION<br>ERROR IN FINDING USERS"
                        );
                    }

                    // oController.clearForm();

                    return true;
                },
                function (error) {
                    console.log(
                        "AdminDashboardController.findUsersByPartialName | error.data.message: ",
                        error.data.message
                    );

                    let errorMessage = oController.m_oTranslate.instant(
                        error.data.message
                    );

                    utilsVexDialogAlertTop(errorMessage);
                }
            );
    };

    AdminDashboardController.prototype.getProcessWorkspaceTotalRunningTimeByUserAndInterval =
        function (sUserId, sDateFrom, sDateTo) {
            console.log(
                "AdminDashboardController.getProcessWorkspaceTotalRunningTimeByUserAndInterval | userId, dateFrom, dateTo:",
                sUserId,
                sDateFrom,
                sDateTo
            );

            this.m_lTotalRunningTimeInMillis = null;

            if (utilsIsStrNullOrEmpty(sUserId) === true) {
                utilsVexDialogAlertTop(
                    "GURU MEDITATION<br>A VALID USER MUST BE PROVIDED"
                );

                return false;
            }

            utilsRemoveSpaces(sUserId);

            if (utilsIsStrNullOrEmpty(sDateFrom) === true) {
                utilsVexDialogAlertTop(
                    "GURU MEDITATION<br>A VALID START DATE MUST BE PROVIDED"
                );

                return false;
            }

            if (utilsIsStrNullOrEmpty(sDateTo) === true) {
                utilsVexDialogAlertTop(
                    "GURU MEDITATION<br>A VALID END DATE MUST BE PROVIDED"
                );

                return false;
            }

            var oController = this;

            let sDateFromParse = new Date(
                Date.parse(sDateFrom + ":00:00")
            ).toISOString();
            let sDateToParse = new Date(
                Date.parse(sDateTo + ":00:00")
            ).toISOString();

            console.log(sDateToParse);

            this.m_oProcessWorkspaceService
                .getProcessWorkspaceTotalRunningTimeByUserAndInterval(
                    sUserId,
                    sDateFromParse,
                    sDateToParse
                )
                .then(
                    function (data) {
                        console.log(data);
                        if (utilsIsObjectNullOrUndefined(data.data) === false) {
                            oController.m_lTotalRunningTimeInMillis = data.data;
                        } else {
                            utilsVexDialogAlertTop(
                                "GURU MEDITATION<br>ERROR IN GETTING THE TOTAL RUNNING TIME"
                            );
                        }

                        // oController.clearForm();

                        return true;
                    },
                    function (error) {
                        console.log(
                            "AdminDashboardController.getProcessWorkspaceTotalRunningTimeByUserAndInterval | error.data.message: ",
                            error.data.message
                        );

                        let errorMessage = oController.m_oTranslate.instant(
                            error.data.message
                        );

                        utilsVexDialogAlertTop(errorMessage);
                    }
                );
        };

    AdminDashboardController.prototype.convertMsToTime = function (
        lMilliseconds
    ) {
        let lSeconds = Math.floor(lMilliseconds / 1000);
        let lMinutes = Math.floor(lSeconds / 60);
        let lHours = Math.floor(lMinutes / 60);

        lMilliseconds = lMilliseconds % 1000;
        lSeconds = lSeconds % 60;
        lMinutes = lMinutes % 60;
        lHours = lHours % 24;

        return `${this.padTo2Digits(lHours)}:${this.padTo2Digits(
            lMinutes
        )}:${this.padTo2Digits(lSeconds)} ${this.padTo3Digits(lMilliseconds)}`;
    };

    AdminDashboardController.prototype.padTo2Digits = function (lNum) {
        return lNum.toString().padStart(2, "0");
    };

    AdminDashboardController.prototype.padTo3Digits = function (lNum) {
        return lNum.toString().padStart(3, "0");
    };

    AdminDashboardController.prototype.findWorkspacesByPartialName = function (
        sWorkspacePartialName
    ) {
        console.log(
            "AdminDashboardController.findWorkspacesByPartialName | sWorkspacePartialName:",
            sWorkspacePartialName
        );

        this.m_aoWorkspacesList = [];

        if (utilsIsStrNullOrEmpty(sWorkspacePartialName) === true) {
            utilsVexDialogAlertTop(
                "GURU MEDITATION<br>AT LEAST THREE CHARACTERS MUST BE PROVIDED"
            );

            return false;
        }

        utilsRemoveSpaces(sWorkspacePartialName);

        if (sWorkspacePartialName.length < 3) {
            utilsVexDialogAlertTop(
                "GURU MEDITATION<br>AT LEAST THREE CHARACTERS MUST BE PROVIDED"
            );

            return false;
        }

        var oController = this;

        this.m_oAdminDashboardService
            .findWorkspacesByPartialName(sWorkspacePartialName)
            .then(
                function (data) {
                    if (utilsIsObjectNullOrUndefined(data.data) === false) {
                        oController.m_aoWorkspacesList = data.data;
                    } else {
                        utilsVexDialogAlertTop(
                            "GURU MEDITATION<br>ERROR IN FINDING WORKSPACES"
                        );
                    }

                    // oController.clearForm();

                    return true;
                },
                function (error) {
                    console.log(
                        "AdminDashboardController.findWorkspacesByPartialName | error.data.message: ",
                        error.data.message
                    );

                    let errorMessage = oController.m_oTranslate.instant(
                        error.data.message
                    );

                    utilsVexDialogAlertTop(errorMessage);
                }
            );
    };

    AdminDashboardController.prototype.findResourcePermissions = function (
        sResourceType,
        sResourceId,
        sUserEmail
    ) {
        console.log(
            "AdminDashboardController.findResourcePermissions | sResourceType:",
            sResourceType
        );
        console.log(
            "AdminDashboardController.findResourcePermissions | sResourceId:",
            sResourceId
        );
        console.log(
            "AdminDashboardController.findResourcePermissions | sUserEmail:",
            sUserEmail
        );

        this.m_aoResourcePermissionsList = [];

        let iValidSearchParameters = 0;

        if (utilsIsStrNullOrEmpty(sResourceType) === false) {
            iValidSearchParameters++;
        }

        if (utilsIsStrNullOrEmpty(sResourceId) === false) {
            utilsRemoveSpaces(sResourceId);
            iValidSearchParameters++;
        }

        if (utilsIsStrNullOrEmpty(sUserEmail) === false) {
            utilsRemoveSpaces(sUserEmail);
            iValidSearchParameters++;
        }

        if (iValidSearchParameters < 2) {
            utilsVexDialogAlertTop(
                "GURU MEDITATION<br>AT LEAST TWO PARAMETERS MUST BE PROVIDED"
            );

            return false;
        }

        var oController = this;

        this.m_oAdminDashboardService
            .findResourcePermissions(sResourceType, sResourceId, sUserEmail)
            .then(
                function (data) {
                    if (utilsIsObjectNullOrUndefined(data.data) === false) {
                        oController.m_aoResourcePermissionsList = data.data;
                    } else {
                        utilsVexDialogAlertTop(
                            "GURU MEDITATION<br>ERROR IN FINDING RESOURCE PERMISSIONS"
                        );
                    }

                    // oController.clearForm();

                    return true;
                },
                function (error) {
                    console.log(
                        "AdminDashboardController.findResourcePermissions | error.data.message: ",
                        error.data.message
                    );

                    let errorMessage = oController.m_oTranslate.instant(
                        error.data.message
                    );

                    utilsVexDialogAlertTop(errorMessage);
                }
            );
    };

    AdminDashboardController.prototype.getResourceTypeList = function () {
        return this.m_aoResourceTypeList;
    };

    AdminDashboardController.prototype.clearInput = function () {
        this.m_sResourceType_add_remove = "";
        this.m_sResourceType_search = "";
    };

    AdminDashboardController.prototype.clearForm = function () {
        // this.m_sUserPartialName = "";
        // this.m_sWorkspacePartialName = "";

        this.m_sResourceType_add_remove = "";
        this.m_sResourceId_add_remove = "";
        this.m_sUserEmail_add_remove = "";

        this.m_sResourceType_search = "";
        this.m_sResourceId_search = "";
        this.m_sUserEmail_search = "";
    };

    AdminDashboardController.prototype.addResourcePermission = function (
        sResourceType,
        sResourceId,
        sUserEmail
    ) {
        console.log(
            "AdminDashboardController.addResourcePermission | sResourceType:",
            sResourceType
        );
        console.log(
            "AdminDashboardController.addResourcePermission | sResourceId:",
            sResourceId
        );
        console.log(
            "AdminDashboardController.addResourcePermission | sUserEmail:",
            sUserEmail
        );

        if (
            utilsIsStrNullOrEmpty(sResourceType) === true ||
            utilsIsStrNullOrEmpty(sResourceId) === true ||
            utilsIsStrNullOrEmpty(sUserEmail) === true
        ) {
            return false;
        }

        utilsRemoveSpaces(sUserEmail);

        var oController = this;

        this.m_oAdminDashboardService
            .addResourcePermission(sResourceType, sResourceId, sUserEmail)
            .then(
                function (data) {
                    if (utilsIsObjectNullOrUndefined(data.data) === false) {
                        var oDialog = utilsVexDialogAlertBottomRightCorner(
                            oController.m_oTranslate.instant(
                                "MSG_SUCCESS_RESOURCE_PERMISSION_ADDED"
                            )
                        );
                        utilsVexCloseDialogAfter(4000, oDialog);
                    } else {
                        utilsVexDialogAlertTop(
                            "GURU MEDITATION<br>ERROR IN ADDING RESOURCE PERMISSION"
                        );
                    }

                    oController.clearForm();

                    return true;
                },
                function (error) {
                    console.log(
                        "AdminDashboardController.addResourcePermission | error.data.message: ",
                        error.data.message
                    );

                    let errorMessage = oController.m_oTranslate.instant(
                        error.data.message
                    );

                    utilsVexDialogAlertTop(errorMessage);
                }
            );
    };

    AdminDashboardController.prototype.removeResourcePermission = function (
        sResourceType,
        sResourceId,
        sUserEmail
    ) {
        console.log(
            "AdminDashboardController.removeResourcePermission | sResourceType:",
            sResourceType
        );
        console.log(
            "AdminDashboardController.removeResourcePermission | sResourceId:",
            sResourceId
        );
        console.log(
            "AdminDashboardController.removeResourcePermission | sUserEmail:",
            sUserEmail
        );

        if (
            utilsIsStrNullOrEmpty(sResourceType) === true ||
            utilsIsStrNullOrEmpty(sResourceId) === true ||
            utilsIsStrNullOrEmpty(sUserEmail) === true
        ) {
            return false;
        }

        utilsRemoveSpaces(sUserEmail);

        var oController = this;

        this.m_oAdminDashboardService
            .removeResourcePermission(sResourceType, sResourceId, sUserEmail)
            .then(
                function (data) {
                    if (utilsIsObjectNullOrUndefined(data.data) === false) {
                        var oDialog = utilsVexDialogAlertBottomRightCorner(
                            oController.m_oTranslate.instant(
                                "MSG_SUCCESS_RESOURCE_PERMISSION_REMOVED"
                            )
                        );
                        utilsVexCloseDialogAfter(4000, oDialog);
                    } else {
                        utilsVexDialogAlertTop(
                            "GURU MEDITATION<br>ERROR IN REMOVING RESOURCE PERMISSION"
                        );
                    }

                    oController.clearForm();

                    return true;
                },
                function (error) {
                    console.log(
                        "AdminDashboardController.addResourcePermission | error.data.message: ",
                        error.data.message
                    );

                    let errorMessage = oController.m_oTranslate.instant(
                        error.data.message
                    );

                    utilsVexDialogAlertTop(errorMessage);
                }
            );
    };

    AdminDashboardController.prototype.getLatestMetricsEntryByNode = function (
        sNodeCode
    ) {
        console.log(
            "AdminDashboardController.getLatestMetricsEntryByNode | sNodeCode:",
            sNodeCode
        );

        this.m_oMetricsEntry = null;

        if (utilsIsStrNullOrEmpty(sNodeCode) === true) {
            utilsVexDialogAlertTop(
                "GURU MEDITATION<br>A VALID NODE NAME SHOULD BE PROVIDED"
            );

            return false;
        }

        utilsRemoveSpaces(sNodeCode);

        if (sNodeCode.length < 3) {
            utilsVexDialogAlertTop(
                "GURU MEDITATION<br>AT LEAST THREE CHARACTERS MUST BE PROVIDED"
            );

            return false;
        }

        var oController = this;

        this.m_oAdminDashboardService
            .getLatestMetricsEntryByNode(sNodeCode)
            .then(
                function (data) {
                    if (utilsIsObjectNullOrUndefined(data.data) === false) {
                        oController.m_oMetricsEntry = data.data;

                        try {
                            oController.m_oMetricsEntryFormatted =
                                JSON.stringify(
                                    oController.m_oMetricsEntry,
                                    null,
                                    2
                                );
                        } catch (oError) {}
                    } else {
                        utilsVexDialogAlertTop(
                            "GURU MEDITATION<br>ERROR IN FINDING THE LATEST METRICS ENTRY"
                        );
                    }

                    // oController.clearForm();

                    return true;
                },
                function (error) {
                    console.log(
                        "AdminDashboardController.getLatestMetricsEntryByNode | error.data.message: ",
                        error.data.message
                    );

                    let errorMessage = oController.m_oTranslate.instant(
                        error.data.message
                    );

                    utilsVexDialogAlertTop(errorMessage);
                }
            );
    };

    AdminDashboardController.$inject = [
        "$scope",
        "$location",
        "ConstantsService",
        "$translate",
        "AuthService",
        "AdminDashboardService",
        "ProcessWorkspaceService",
        "$state",
        "$filter",
        "ProductService",
        "RabbitStompService",
        "GlobeService",
        "$rootScope",
        "OpportunitySearchService",
        "$interval",
    ];
    return AdminDashboardController;
})();
window.AdminDashboardController = AdminDashboardController;