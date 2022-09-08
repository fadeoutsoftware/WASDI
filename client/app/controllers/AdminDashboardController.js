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

        this.m_sPartialName = "";
        this.m_aoUserList = [];


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


    AdminDashboardController.prototype.findUsersByPartialName = function () {
        console.log("AdminDashboardController.findUsersByPartialName | this.m_sPartialName:", this.m_sPartialName);

        this.m_aoUserList = [];

        if (utilsIsObjectNullOrUndefined(this.m_sPartialName) === true) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>AT LEAST THREE CHARACTERS MUST BE PROVIDED");

            return false;
        }

        utilsRemoveSpaces(this.m_sPartialName);

        if (this.m_sPartialName.length < 3) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>AT LEAST THREE CHARACTERS MUST BE PROVIDED");

            return false;
        }

        var oController = this;

        this.m_oAdminDashboardService.findUsersByPartialName(this.m_sPartialName)
            .then(function (data) {
            if (utilsIsObjectNullOrUndefined(data.data) === false) {
                oController.m_aoUserList = data.data;
            } else {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN ADDING RESOURCE PERMISSION");
            }

            oController.clearForm();

            return true;
        },function (error) {
            console.log("AdminDashboardController.findUsersByPartialName | error.data.message: ", error.data.message);

            let errorMessage = oController.m_oTranslate.instant(error.data.message);

            utilsVexDialogAlertTop(errorMessage);
        });

    }

    AdminDashboardController.prototype.getResourceTypeList = function () {
        // console.log("AdminDashboardController.getResourceTypeList | this.m_aoResourceTypeList: ", this.m_aoResourceTypeList);
        return this.m_aoResourceTypeList;
    }

    AdminDashboardController.prototype.clearInput = function () {
        this.m_sResourceType = "";
    }

    AdminDashboardController.prototype.clearForm = function () {
        this.m_sPartialName = "";

        this.m_sResourceType = "";
        this.m_sResourceId = "";
        this.m_sUserEmail = "";
    }


    AdminDashboardController.prototype.addResourcePermission = function () {
        console.log("AdminDashboardController.addResourcePermission | this.m_sResourceType:", this.m_sResourceType);
        console.log("AdminDashboardController.addResourcePermission | this.m_sResourceId:", this.m_sResourceId);
        console.log("AdminDashboardController.addResourcePermission | this.m_sUserEmail:", this.m_sUserEmail);

        if (utilsIsObjectNullOrUndefined(this.m_sResourceType) === true
                || utilsIsStrNullOrEmpty(this.m_sResourceId) === true
                || utilsIsStrNullOrEmpty(this.m_sUserEmail) === true) {
            return false;
        }

        utilsRemoveSpaces(this.m_sUserEmail);

        var oController = this;

        this.m_oAdminDashboardService.addResourcePermission(this.m_sResourceType, this.m_sResourceId, this.m_sUserEmail)
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

    AdminDashboardController.prototype.removeResourcePermission = function() {
        console.log("AdminDashboardController.removeResourcePermission | this.m_sResourceType:", this.m_sResourceType);
        console.log("AdminDashboardController.removeResourcePermission | this.m_sResourceId:", this.m_sResourceId);
        console.log("AdminDashboardController.removeResourcePermission | this.m_sUserEmail:", this.m_sUserEmail);

        if (utilsIsObjectNullOrUndefined(this.m_sResourceType) === true
                || utilsIsStrNullOrEmpty(this.m_sResourceId) === true
                || utilsIsStrNullOrEmpty(this.m_sUserEmail) === true) {
            return false;
        }

        utilsRemoveSpaces(this.m_sUserEmail);

        var oController = this;

        this.m_oAdminDashboardService.removeResourcePermission(this.m_sResourceType, this.m_sResourceId, this.m_sUserEmail)
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
