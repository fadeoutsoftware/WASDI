/**
 * Created by p.petrescu on 31/08/2022.
 */

var AdminDashboardController = (function () {
    function AdminDashboardController($scope, $location, oConstantsService, oTranslate, oAuthService, oAdminDashboardService, $state) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oLocation = $location;
        this.m_oAuthService = oAuthService;
        this.m_oAdminDashboardService = oAdminDashboardService;
        this.m_oConstantsService = oConstantsService;

        this.m_sUserPartialName = "";
        this.m_aoUsersList = [];

        this.m_sWorkspacePartialName = "";
        this.m_aoWorkspacesList = [];


        this.m_aoResourceTypeList = ['NODE', 'PROCESSORPARAMETERSTEMPLATE', 'PROCESSOR', 'STYLE', 'WORKFLOW', 'WORKSPACE'];

        this.m_sResourceType = "";
        this.m_sResourceId = null;
        this.m_sUserEmail = null;


        this.m_bIsLoading = true;

        this.m_oState = $state;


        this.m_oTranslate = oTranslate;   

        if(utilsIsObjectNullOrUndefined(oConstantsService.getUser())){
            this.m_oState.go("home");
        }



        this.isLoadingIconVisible = function () {
            // return false;
            if (this.m_oRabbitStompService.isReadyState() === false) {
                return true;
            }
            return false;
        }
    }


    AdminDashboardController.prototype.findUsersByPartialName = function (sUserPartialName) {
        console.log("AdminDashboardController.findUsersByPartialName | sUserPartialName:", sUserPartialName);

        this.m_aoUsersList = [];

        if (utilsIsObjectNullOrUndefined(sUserPartialName) === true) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>AT LEAST THREE CHARACTERS MUST BE PROVIDED");

            return false;
        }

        utilsRemoveSpaces(sUserPartialName);

        if (sUserPartialName.length < 3) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>AT LEAST THREE CHARACTERS MUST BE PROVIDED");

            return false;
        }

        var oController = this;

        this.m_oAdminDashboardService.findUsersByPartialName(sUserPartialName)
            .then(function (data) {
            if (utilsIsObjectNullOrUndefined(data.data) === false) {
                oController.m_aoUsersList = data.data;
            } else {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN FINDING USERS");
            }

            oController.clearForm();

            return true;
        },function (error) {
            console.log("AdminDashboardController.findUsersByPartialName | error.data.message: ", error.data.message);

            let errorMessage = oController.m_oTranslate.instant(error.data.message);

            utilsVexDialogAlertTop(errorMessage);
        });

    }

    AdminDashboardController.prototype.findWorkspacesByPartialName = function (sWorkspacePartialName) {
        console.log("AdminDashboardController.findWorkspacesByPartialName | sWorkspacePartialName:", sWorkspacePartialName);

        this.m_aoWorkspacesList = [];

        if (utilsIsObjectNullOrUndefined(sWorkspacePartialName) === true) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>AT LEAST THREE CHARACTERS MUST BE PROVIDED");

            return false;
        }

        utilsRemoveSpaces(sWorkspacePartialName);

        if (sWorkspacePartialName.length < 3) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>AT LEAST THREE CHARACTERS MUST BE PROVIDED");

            return false;
        }

        var oController = this;

        this.m_oAdminDashboardService.findWorkspacesByPartialName(sWorkspacePartialName)
            .then(function (data) {
            if (utilsIsObjectNullOrUndefined(data.data) === false) {
                oController.m_aoWorkspacesList = data.data;
            } else {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN FINDING WORKSPACES");
            }

            oController.clearForm();

            return true;
        },function (error) {
            console.log("AdminDashboardController.findWorkspacesByPartialName | error.data.message: ", error.data.message);

            let errorMessage = oController.m_oTranslate.instant(error.data.message);

            utilsVexDialogAlertTop(errorMessage);
        });

    }

    AdminDashboardController.prototype.getResourceTypeList = function () {
        return this.m_aoResourceTypeList;
    }

    AdminDashboardController.prototype.clearInput = function () {
        this.m_sResourceType = "";
    }

    AdminDashboardController.prototype.clearForm = function () {
        this.m_sUserPartialName = "";
        this.m_sWorkspacePartialName = "";

        this.m_sResourceType = "";
        this.m_sResourceId = "";
        this.m_sUserEmail = "";
    }


    AdminDashboardController.prototype.addResourcePermission = function (sResourceType, sResourceId, sUserEmail) {
        console.log("AdminDashboardController.addResourcePermission | sResourceType:", sResourceType);
        console.log("AdminDashboardController.addResourcePermission | sResourceId:", sResourceId);
        console.log("AdminDashboardController.addResourcePermission | sUserEmail:", sUserEmail);

        if (utilsIsObjectNullOrUndefined(sResourceType) === true
                || utilsIsStrNullOrEmpty(sResourceId) === true
                || utilsIsStrNullOrEmpty(sUserEmail) === true) {
            return false;
        }

        utilsRemoveSpaces(sUserEmail);

        var oController = this;

        this.m_oAdminDashboardService.addResourcePermission(sResourceType, sResourceId, sUserEmail)
            .then(function (data) {
            if (utilsIsObjectNullOrUndefined(data.data) === false) {
                var oDialog = utilsVexDialogAlertBottomRightCorner(oController.m_oTranslate.instant("MSG_SUCCESS_RESOURCE_PERMISSION_ADDED"));
                utilsVexCloseDialogAfter(4000, oDialog);
            } else {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN ADDING RESOURCE PERMISSION");
            }

            oController.clearForm();
    
            return true;
        },function (error) {
            console.log("AdminDashboardController.addResourcePermission | error.data.message: ", error.data.message);

            let errorMessage = oController.m_oTranslate.instant(error.data.message);

            utilsVexDialogAlertTop(errorMessage);
        });
    };

    AdminDashboardController.prototype.removeResourcePermission = function(sResourceType, sResourceId, sUserEmail) {
        console.log("AdminDashboardController.removeResourcePermission | sResourceType:", sResourceType);
        console.log("AdminDashboardController.removeResourcePermission | sResourceId:", sResourceId);
        console.log("AdminDashboardController.removeResourcePermission | sUserEmail:", sUserEmail);

        if (utilsIsObjectNullOrUndefined(sResourceType) === true
                || utilsIsStrNullOrEmpty(sResourceId) === true
                || utilsIsStrNullOrEmpty(sUserEmail) === true) {
            return false;
        }

        utilsRemoveSpaces(sUserEmail);

        var oController = this;

        this.m_oAdminDashboardService.removeResourcePermission(sResourceType, sResourceId, sUserEmail)
            .then(function (data) {
            if (utilsIsObjectNullOrUndefined(data.data) === false) {
                var oDialog = utilsVexDialogAlertBottomRightCorner(oController.m_oTranslate.instant("MSG_SUCCESS_RESOURCE_PERMISSION_REMOVED"));
                utilsVexCloseDialogAfter(4000, oDialog);
            } else {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN REMOVING RESOURCE PERMISSION");
            }

            oController.clearForm();
    
            return true;
        },function (error) {
            console.log("AdminDashboardController.addResourcePermission | error.data.message: ", error.data.message);

            let errorMessage = oController.m_oTranslate.instant(error.data.message);

            utilsVexDialogAlertTop(errorMessage);
        });
    };




    AdminDashboardController.$inject = [
        '$scope',
        '$location',
        'ConstantsService',
        '$translate',
        'AuthService',
        'AdminDashboardService',
        '$state',
        'ProductService',
        'RabbitStompService',
        'GlobeService',
        '$rootScope',
        'OpportunitySearchService',
        '$interval'
    ];
    return AdminDashboardController;
})();
window.AdminDashboardController = AdminDashboardController;
