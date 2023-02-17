/**
 * Created by a.corrado on 31/03/2017.
 */


var EditUserController = (function () {

    function EditUserController($scope, oClose, oExtras, oAuthService, oConstantsService, oProcessWorkspaceService, oOrganizationService, oSubscriptionService, oProjectService, oAdminDashboardService, oModalService, oTranslate, oRabbitStompService) {
        //MEMBERS
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oAuthService = oAuthService;
        this.m_oConstantsService = oConstantsService;
        this.m_oProcessWorkspaceService = oProcessWorkspaceService;
        this.m_oOrganizationService = oOrganizationService;
        this.m_oSubscriptionService = oSubscriptionService;
        this.m_oProjectService = oProjectService;
        this.m_oAdminDashboardService = oAdminDashboardService;
        this.m_oModalService = oModalService;
        this.m_oTranslate = oTranslate;
        this.m_oRabbitStompService = oRabbitStompService;

        this.m_oUser = this.m_oExtras.user;
        this.m_bEditingPassword = false;
        this.m_bEditingUserInfo = false;
        this.m_oEditUser = {};
        this.m_lTotalRuntime = null;


        this.m_bLoadingTypes = true;
        this.m_bLoadingOrganizations = true;
        this.m_bLoadingSubscriptions = true;
        this.m_bLoadingProjects = true;
        this.m_bIsLoading = false;

        this.initializeEditUserInfo();
        this.initializeUserRuntimeInfo();
        this.m_sSelectedTab = "UserInfo";


        this.m_aoTypes = [];
        this.m_aoTypesMap = [];
        this.m_oType = {};

        this.getSubscriptionTypes();


        this.m_aoOrganizations = [];
        this.m_aoUsersList = [];
        this.m_oEditOrganization = {};

        this.initializeOrganizationsInfo();


        this.m_aoSubscriptions = [];

        this.m_oEditSubscription = {};

        this.initializeSubscriptionsInfo();


        this.m_aoProjects = [];

        this.m_aoProjectsMap = [];
        this.m_oProject = {};

        this.initializeProjectsInfo();

        
        /* 
        RabbitStomp Service call
        */
        this.m_iHookIndex = this.m_oRabbitStompService.addMessageHook(
            "SUBSCRIPTION",
            this,
            this.rabbitMessageHook
        );

        var oController = this;


        $scope.close = function (result) {
            oController.m_oRabbitStompService.removeMessageHook(oController.m_iHookIndex);

            oClose(result, 300); // close, but give 500ms for bootstrap to animate
        };

        $scope.add = function (result) {
            oController.m_oRabbitStompService.removeMessageHook(oController.m_iHookIndex);
            oClose(result, 300); // close, but give 500ms for bootstrap to animate
        };
    }

    /*
        getUserAuthProvider
     */
    EditUserController.prototype.getUserAuthProvider = function()
    {
        return this.m_oUser.authProvider;
    };
    EditUserController.prototype.isUserLoggedWithGoogle = function()
    {
        return ((this.m_oUser.authProvider === "google") ? true: false);
    };

    /*
        changePassword
     */
    EditUserController.prototype.changePassword = function()
    {

        var oJsonToSend = this.getPasswordsJSON();
        var oController = this;

        this.m_bEditingPassword = true;
        this.m_oAuthService.changePassword(oJsonToSend)
            .then(function (data) {
                if(utilsIsObjectNullOrUndefined(data) === false)
                {
                    if(data.data.boolValue ===  false)
                    {
                        utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR PASSWORD NOT CHANGED");
                    }
                    else
                    {
                        var oVexWindow = utilsVexDialogAlertBottomRightCorner("PASSWORD CHANGED");
                        utilsVexCloseDialogAfter(3000,oVexWindow);

                    }

                }
                else
                {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR PASSWORD NOT CHANGED");
                }
                oController.m_bEditingPassword = false;
                oController.cleanPasswordsInEditUserObject();

            },(function (error)
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR PASSWORD NOT CHANGED");
                oController.m_bEditingPassword = false;
                oController.cleanPasswordsInEditUserObject();
            }));
    }

    EditUserController.prototype.changeUserInfo = function()
    {

        var oJsonToSend = this.getUserInfo();
        var oController = this;

        this.m_bEditingUserInfo = true;
        this.m_oAuthService.changeUserInfo(oJsonToSend)
            .then(function (data) {
                if(utilsIsObjectNullOrUndefined(data.data) === false ||  data.data.userId !== "" )
                {
                    if(data.data.boolValue ===  false)
                    {
                        utilsVexDialogAlertTop("GURU MEDITATION<br>IMPOSSIBLE TO CHANGE USER INFO");
                    }
                    else
                    {
                        var oVexWindow = utilsVexDialogAlertBottomRightCorner("CHANGED USER INFO");
                        utilsVexCloseDialogAfter(3000,oVexWindow);
                        oController.m_oUser = data.data;
                        oController.m_oConstantsService.setUser(data.data);//save in cookie
                    }


                }
                else
                {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN CHANGE USER INFO");
                }
                oController.m_bEditingUserInfo = false;
                oController.initializeEditUserInfo();

            },(function (error)
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN CHANGE USER INFO");
                oController.m_bEditingUserInfo = false;
                oController.initializeEditUserInfo();
            }));
    }

    EditUserController.prototype.getPasswordsJSON = function()
    {
        return {
            newPassword: this.m_oEditUser.newPassword,
            currentPassword: this.m_oEditUser.currentPassword,
        }
    }
    EditUserController.prototype.getUserInfo = function()
    {
        return {
            name: this.m_oEditUser.name,
            surname: this.m_oEditUser.surname,
        }
    };

    EditUserController.prototype.cleanPasswordsInEditUserObject= function()
    {
        this.m_oEditUser.newPassword = "";
        this.m_oEditUser.repeatNewPassword = "";
        this.m_oEditUser.currentPassword = "";
    };

    EditUserController.prototype.cleanInfoInEditUserObject= function()
    {
        this.m_oEditUser.name = "";
        this.m_oEditUser.surname = "";

    };

    EditUserController.prototype.initializeEditUserInfo = function()
    {
        this.m_oEditUser = {
            name:"",
            surname:"",
            newPassword:"",
            repeatNewPassword:"",
            currentPassword:"",
        }

        if( utilsIsObjectNullOrUndefined(this.m_oUser) === false )
        {
            this.m_oEditUser.name = this.m_oUser.name;
            this.m_oEditUser.surname = this.m_oUser.surname;
        }
    };

    EditUserController.prototype.initializeUserRuntimeInfo = function() {
        let oController = this;
        if (utilsIsStrNullOrEmpty(this.m_oUser.userId) === true) {
            utilsVexDialogAlertTop(
                "GURU MEDITATION<br>A VALID USER MUST BE PROVIDED"
            );

            return false;
        }

        
        this.m_oProcessWorkspaceService.getProcessWorkspaceTotalRunningTimeByUserAndInterval(this.m_oUser.userId).then(
            function (data) {
                if (!utilsIsObjectNullOrUndefined(data)
                        && !utilsIsObjectNullOrUndefined(data.data) && data.status === 200) {
                    oController.m_lTotalRuntime = data.data;
                } else {
                    utilsVexDialogAlertTop(
                        "GURU MEDITATION<br>ERROR IN GETTING THE TOTAL RUNNING TIME"
                    );
                }

                return true;
            }, function (error) {
                let sErrorMessage =  "GURU MEDITATION<br>ERROR IN GETTING THE TOTAL RUNNING TIME";

                utilsVexDialogAlertTop(sErrorMessage);
            }
        );
    };


    EditUserController.prototype.initializeOrganizationsInfo = function() {
        var oController = this;

        this.m_oOrganizationService.getOrganizationsListByUser().then(
            function (response) {
                if (!utilsIsObjectNullOrUndefined(response)
                        && !utilsIsObjectNullOrUndefined(response.data) && response.status === 200) {
                    oController.m_aoOrganizations = response.data;
                } else {
                    utilsVexDialogAlertTop(
                        "GURU MEDITATION<br>ERROR IN GETTING THE LIST OF ORGANIZATIONS"
                    );
                }

                oController.m_bLoadingOrganizations = false;

                return true;
            }, function (error) {
                let sErrorMessage = "GURU MEDITATION<br>ERROR IN GETTING THE LIST OF ORGANIZATIONS";

                if (!utilsIsObjectNullOrUndefined(error.data) && !utilsIsStrNullOrEmpty(error.data.message)) {
                    sErrorMessage += "<br><br>" + oController.m_oTranslate.instant(error.data.message);
                }

                utilsVexDialogAlertTop(sErrorMessage);
            }
        );
    }

    EditUserController.prototype.showUsersByOrganization = function(sOrganizationId) {
        if (utilsIsStrNullOrEmpty(sOrganizationId)) {
            return false;
        }

        this.m_oModalService.showModal({
            templateUrl: "dialogs/organization-users/OrganizationUsersDialog.html",
            controller: 'OrganizationUsersController',
            inputs: {
                extras: {
                    organizationId: sOrganizationId
                }
            }
        }).then(function (modal) {
            modal.element.modal({
                backdrop: 'static'
            })
            modal.close.then(function () {
            })

        })
    }

    EditUserController.prototype.showOrganizationEditForm = function(sOrganizationId, sEditMode) {
        let oController = this;

        this.m_oOrganizationService.getOrganizationById(sOrganizationId).then(
            function (response) {
                if (!utilsIsObjectNullOrUndefined(response)
                        && !utilsIsObjectNullOrUndefined(response.data) && response.status === 200) {
                    oController.m_oEditOrganization = response.data;
                    oController.m_oModalService.showModal({
                        templateUrl: "dialogs/organization_editor/OrganizationEditorDialog.html",
                        controller: 'OrganizationEditorController',
                        inputs: {
                            extras: {
                                organization: response.data,
                                editMode: sEditMode
                            }
                        }
                    }).then(function (modal) {
                        modal.element.modal({
                            backdrop: 'static'
                        })
                        modal.close.then(function () {
                            oController.initializeOrganizationsInfo();
                        })

                    })
                } else {
                    utilsVexDialogAlertTop(
                        "GURU MEDITATION<br>ERROR IN GETTING THE ORGANIZATION BY ID"
                    );
                }

                return true;
            }, function (error) {
                let sErrorMessage = "GURU MEDITATION<br>ERROR IN FETCHING THE ORGANIZATION";

                if (!utilsIsObjectNullOrUndefined(error.data) && !utilsIsStrNullOrEmpty(error.data.message)) {
                    sErrorMessage += "<br><br>" + oController.m_oTranslate.instant(error.data.message);
                }

                utilsVexDialogAlertTop(sErrorMessage);
            }
        );
    }

    EditUserController.prototype.showOrganizationAddForm = function() {
        var oController = this;

        let oNewOrganization = {};

        this.m_oModalService.showModal({
            templateUrl: "dialogs/organization_editor/OrganizationEditorDialog.html",
            controller: "OrganizationEditorController",
            inputs: {
                extras: {
                    organization: oNewOrganization,
                    editMode: true
                }
            }
        }).then(function (modal) {
            modal.element.modal({
                backdrop: 'static'
            })
            modal.close.then(function () {
                oController.initializeOrganizationsInfo();
            })
        });
    }

    EditUserController.prototype.deleteOrganization = function(sOrganizationId) {
        let sConfirmMsg = "Delete this Organization?"

        var oController = this;

        let oCallbackFunction = function(value) {
            if(value) {
                oController.m_oOrganizationService.deleteOrganization(sOrganizationId)
                .then(function (response) {
                    if (!utilsIsObjectNullOrUndefined(response) && response.status === 200) {
                        var oDialog = utilsVexDialogAlertBottomRightCorner("ORGANIZATION DELETED<br>READY");
                        utilsVexCloseDialogAfter(4000, oDialog);
                    } else {
                        utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN DELETING ORGANIZATION");
                    }

                    oController.initializeOrganizationsInfo();
                }, function (error) {
                    let sErrorMessage = "GURU MEDITATION<br>ERROR IN DELETING ORGANIZATION";

                    if (!utilsIsObjectNullOrUndefined(error.data) && !utilsIsStrNullOrEmpty(error.data.message)) {
                        sErrorMessage += "<br><br>" + oController.m_oTranslate.instant(error.data.message);
                    }

                    utilsVexDialogAlertTop(sErrorMessage);
                })
            }
        };

        utilsVexDialogConfirm(sConfirmMsg, oCallbackFunction);
    }

    EditUserController.prototype.cancelEditOrganizationForm = function() {
        this.m_oEditOrganization = {}
    }

    EditUserController.prototype.cancelSharingOrganizationForm = function() {
        this.m_oEditOrganization = {}
    }

    EditUserController.prototype.initializeSubscriptionsInfo = function() {
        var oController = this;

        this.m_oSubscriptionService.getSubscriptionsListByUser().then(
            function (response) {
                if (!utilsIsObjectNullOrUndefined(response)
                        && !utilsIsObjectNullOrUndefined(response.data) && response.status === 200) {
                    oController.m_aoSubscriptions = response.data;
                } else {
                    utilsVexDialogAlertTop(
                        "GURU MEDITATION<br>ERROR IN GETTING THE LIST OF SUBSCRIPTIONS"
                    );
                }

                oController.m_bLoadingSubscriptions = false;

                return true;
            }, function (error) {
                let sErrorMessage = "GURU MEDITATION<br>ERROR IN GETTING THE LIST OF SUBSCRIPTIONS";

                if (!utilsIsObjectNullOrUndefined(error.data) && !utilsIsStrNullOrEmpty(error.data.message)) {
                    sErrorMessage += "<br><br>" + oController.m_oTranslate.instant(error.data.message);
                }

                utilsVexDialogAlertTop(sErrorMessage);
            }
        );
    }

    EditUserController.prototype.showSubscriptionEditForm = function(sSubscriptionId, sEditMode) {
        var oController = this;

        this.m_oSubscriptionService.getSubscriptionById(sSubscriptionId).then(
            function (response) {
                if (!utilsIsObjectNullOrUndefined(response)
                        && !utilsIsObjectNullOrUndefined(response.data) && response.status === 200) {
                    oController.m_oEditSubscription = response.data;
                    oController.m_oModalService.showModal({
                        templateUrl: "dialogs/subscription_editor/SubscriptionEditorDialog.html",
                        controller: "SubscriptionEditorController",
                        inputs: {
                            extras: {
                                subscription: response.data,
                                editMode: sEditMode
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
                } else {
                    utilsVexDialogAlertTop(
                        "GURU MEDITATION<br>ERROR IN GETTING THE SUBSCRIPTION BY ID"
                    );
                }
            }, function (error) {
                let sErrorMessage = "GURU MEDITATION<br>ERROR IN FETCHING THE SUBSCRIPTION";

                if (!utilsIsObjectNullOrUndefined(error.data) && !utilsIsStrNullOrEmpty(error.data.message)) {
                    sErrorMessage += "<br><br>" + oController.m_oTranslate.instant(error.data.message);
                }

                utilsVexDialogAlertTop(sErrorMessage);
            }
        )
    }

    EditUserController.prototype.showSubscriptionAddForm = function(typeId, typeName) {
        var oController = this;

        let oNewSubscription = {
            typeId: typeId,
            typeName: typeName
        };

        this.m_oModalService.showModal({
            templateUrl: "dialogs/subscription_editor/SubscriptionEditorDialog.html",
            controller: "SubscriptionEditorController",
            inputs: {
                extras: {
                    subscription: oNewSubscription,
                    editMode: true
                }
            }
        }).then(function (modal) {
            modal.element.modal({
                backdrop: 'static'
            })
            modal.close.then(function () {
                oController.initializeSubscriptionsInfo();
            })
        });
    }

    EditUserController.prototype.deleteSubscription = function(sSubscriptionId) {

        let sConfirmMsg = "Delete this Subscription?"

        var oController = this;
        
        let oCallbackFunction = function(value) {
            if (value) {
                oController.m_oSubscriptionService.deleteSubscription(sSubscriptionId)
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
                    });
            }
        };

        utilsVexDialogConfirm(sConfirmMsg, oCallbackFunction);
    }

    EditUserController.prototype.cancelEditSubscriptionForm = function() {
        this.m_oEditSubscription = {}
    }

    EditUserController.prototype.showUsersBySubscription = function(sSubscriptionId) {
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
            })

        })
    }

    EditUserController.prototype.cancelSharingSubscriptionForm = function() {
        this.m_oEditSubscription = {}
    }

    EditUserController.prototype.changeActiveProjectWithButton = function(oProject) {
        var oController = this;

        if (!utilsIsObjectNullOrUndefined(oProject)) {
            this.m_oProjectService.changeActiveProject(oProject.projectId).then(function (response) {

                if (!utilsIsObjectNullOrUndefined(response.data)) {
                    let oDialog = utilsVexDialogAlertBottomRightCorner("ACTIVE PROJECT CHANGED<br>READY");
                    utilsVexCloseDialogAfter(2000, oDialog);

                    this.m_oProject = oProject;
                } else {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN CHANGING THE ACTIVE PROJECT");
                }
    
            }, function (error) {
                let sErrorMessage = "GURU MEDITATION<br>ERROR IN CHANGING THE ACTIVE PROJECT";

                if (!utilsIsObjectNullOrUndefined(error.data) && !utilsIsStrNullOrEmpty(error.data.message)) {
                    sErrorMessage += "<br><br>" + oController.m_oTranslate.instant(error.data.message);
                }

                utilsVexDialogAlertTop(sErrorMessage);
            });
        }
    }

    EditUserController.prototype.initializeProjectsInfo = function() {
        var oController = this;

        this.m_oProjectService.getProjectsListByUser().then(
            function (response) {
                if (!utilsIsObjectNullOrUndefined(response)
                        && !utilsIsObjectNullOrUndefined(response.data) && response.status === 200) {
                    oController.m_aoProjects = response.data;

                    const oFirstElement = { name: "No Active Project", projectId: null };
                    let aoProjects = [oFirstElement].concat(response.data);

                    oController.m_aoProjectsMap = aoProjects.map(
                        (item) => ({ name: item.name, projectId: item.projectId })
                    );


                    oController.m_oProject = oFirstElement;

                    oController.m_aoProjects.forEach((oValue) => {
                        if (oValue.activeProject) {
                            oController.m_oProject = oValue;
                        }
                    });
                } else {
                    utilsVexDialogAlertTop(
                        "GURU MEDITATION<br>ERROR IN GETTING THE LIST OF PROJECTS"
                    );
                }

                oController.m_bLoadingProjects = false;

                return true;
            }, function (error) {
                let sErrorMessage = "GURU MEDITATION<br>ERROR IN GETTING THE LIST OF PROJECTS";

                if (!utilsIsObjectNullOrUndefined(error.data) && !utilsIsStrNullOrEmpty(error.data.message)) {
                    sErrorMessage += "<br><br>" + oController.m_oTranslate.instant(error.data.message);
                }

                utilsVexDialogAlertTop(sErrorMessage);

                oController.m_bLoadingProjects = false;
            }
        );
    }


    EditUserController.prototype.showProjectsBySubscription = function(sSubscriptionId, sSubscriptionName) {
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

    EditUserController.prototype.cancelEditProjectForm = function() {
    }

    EditUserController.prototype.getSubscriptionTypes = function () {
        let oController = this;

        oController.m_oSubscriptionService.getSubscriptionTypes().then(
            function (response) {
                if (!utilsIsObjectNullOrUndefined(response)
                        && !utilsIsObjectNullOrUndefined(response.data) && response.status === 200) {
                    oController.m_aoTypes = response.data;
                    oController.m_aoTypesMap = oController.m_aoTypes.map(
                        (item) => ({ name: item.name, typeId: item.typeId })
                    );

                    oController.m_aoTypesMap.forEach((oValue, sKey) => {
                        if (oValue.typeId == oController.m_oEditSubscription.typeId) {
                            oController.m_oType = oValue;
                        }
                    });                
                } else {
                    var oDialog = utilsVexDialogAlertBottomRightCorner(
                        "GURU MEDITATION<br>ERROR GETTING SUBSCRIPTION TYPES"
                    );
                    utilsVexCloseDialogAfter(4000, oDialog);
                }

                oController.m_bLoadingTypes = false;
            }, function (error) {
                var oDialog = utilsVexDialogAlertBottomRightCorner(
                    "GURU MEDITATION<br>ERROR GETTING SUBSCRIPTION TYPES"
                );
                utilsVexCloseDialogAfter(4000, oDialog);

                oController.m_bLoadingTypes = false;
            }
        );
    }

    EditUserController.prototype.rabbitMessageHook = function (oRabbitMessage, oController) {
        console.log("EditUserController.rabbitMessageHook | oRabbitMessage:", oRabbitMessage);
        oController.initializeSubscriptionsInfo();
        oController.m_bIsLoading = false;

        if (!utilsIsObjectNullOrUndefined(oRabbitMessage)) {
            let sRabbitMessage = JSON.stringify(oRabbitMessage);

            var oVexWindow = utilsVexDialogAlertBottomRightCorner(sRabbitMessage);
            utilsVexCloseDialogAfter(5000, oVexWindow);
        }
    };

    EditUserController.$inject = [
        '$scope',
        'close',
        'extras',
        'AuthService',
        'ConstantsService',
        'ProcessWorkspaceService',
        'OrganizationService',
        'SubscriptionService',
        'ProjectService',
        'AdminDashboardService',
        'ModalService',
        '$translate',
        "RabbitStompService"
    ];
    return EditUserController ;
})();
window.EditUserController= EditUserController;
