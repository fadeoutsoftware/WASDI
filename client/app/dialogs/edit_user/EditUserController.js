/**
 * Created by a.corrado on 31/03/2017.
 */


var EditUserController = (function () {

    function EditUserController($scope, oClose, oExtras, oAuthService, oConstantsService, oProcessWorkspaceService, oOrganizationService, oSubscriptionService, oProjectService, oAdminDashboardService, oModalService, oTranslate) {
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

        this.m_oUser = this.m_oExtras.user;
        this.m_bEditingPassword = false;
        this.m_bEditingUserInfo = false;
        this.m_oEditUser = {};
        this.m_lTotalRuntime = null;


        this.m_bLoadingOrganizations = true;
        this.m_bLoadingSubscriptions = true;
        this.m_bLoadingProjects = true;

        this.initializeEditUserInfo();
        this.initializeUserRuntimeInfo();
        this.m_sSelectedTab = "UserInfo";

        this.m_aoOrganizations = [];
        this.m_aoUsersList = [];
        this.m_oEditOrganization = {};
        this.m_oSharingOrganization = {};
        this.m_sSelectedOrganizationId = null;

        this.m_sUserPartialName = "";
        this.m_aoMatchingUsersList = [];


        this.initializeOrganizationsInfo();


        this.m_aoSubscriptions = [];

        this.m_oEditSubscription = {};
        this.m_oSharingSubscription = {};
        this.m_sSelectedSubscriptionId = null;

        this.initializeSubscriptionsInfo();


        this.m_aoProjects = [];

        this.m_aoProjectsMap = [];
        this.m_oProject = {};

        this.initializeProjectsInfo();


        $scope.close = function (result) {
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
                        if (utilsIsObjectNullOrUndefined(data.data) === false) {
                            oController.m_lTotalRuntime = data.data;
                        } else {
                            utilsVexDialogAlertTop(
                                "GURU MEDITATION<br>ERROR IN GETTING THE TOTAL RUNNING TIME"
                            );
                        }

                        return true;
                    }
        );
    };


    EditUserController.prototype.initializeOrganizationsInfo = function() {
        var oController = this;

        this.m_oOrganizationService.getOrganizationsListByUser().then(
            function (data) {
                if (utilsIsObjectNullOrUndefined(data.data) === false) {
                    oController.m_aoOrganizations = data.data;
                } else {
                    utilsVexDialogAlertTop(
                        "GURU MEDITATION<br>ERROR IN GETTING THE LIST OF ORGANIZATIONS"
                    );
                }

                oController.m_bLoadingOrganizations = false;

                return true;
            }
        );
    }

    EditUserController.prototype.showUsersByOrganization = function(sOrganizationId) {
        console.log("EditUserController.showUsersByOrganization | sOrganizationId: ", sOrganizationId);

        this.m_sSelectedOrganizationId = sOrganizationId;
        console.log("EditUserController.showUsersByOrganization | this.m_sSelectedOrganizationId: ", this.m_sSelectedOrganizationId);

        this.m_oEditOrganization = {};
        this.m_oSharingOrganization = {organizationId: sOrganizationId}
        this.m_aoMatchingUsersList = [];
        this.m_sUserPartialName = "";

        if (utilsIsStrNullOrEmpty(sOrganizationId) === true) {
            return false;
        }

        var oController = this;

        this.m_oOrganizationService.getUsersBySharedOrganization(sOrganizationId).then(
            function (data) {
                if (utilsIsObjectNullOrUndefined(data.data) === false) {
                    oController.m_aoUsersList = data.data;
                    oController.m_oModalService.showModal({
                        templateUrl: "dialogs/organization-users/OrganizationUsersDialog.html",
                        controller: 'OrganizationUsersController',
                        inputs: {
                            extras: {
                                users: data.data,
                                organizationId: oController.m_sSelectedOrganizationId
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
                        "GURU MEDITATION<br>ERROR IN GETTING THE LIST OF USERS OF THE ORGANIZATION"
                    );
                }

                return true;
            }
        );
    }

    // EditUserController.prototype.showOrganizationSharingForm = function(sOrganizationId) {
    //     console.log("EditUserController.showOrganizationSharingForm | sOrganizationId: ", sOrganizationId);

    //     this.m_sSelectedOrganizationId = sOrganizationId;
    //     this.m_oEditOrganization = {}
    //     this.m_oSharingOrganization = {}
    //     this.m_aoMatchingUsersList = [];

    //     var oController = this;

    //     this.m_oOrganizationService.getOrganizationById(sOrganizationId).then(
    //         function (data) {
    //             if (utilsIsObjectNullOrUndefined(data.data) === false) {
    //                 oController.m_oSharingOrganization = data.data;
    //             } else {
    //                 utilsVexDialogAlertTop(
    //                     "GURU MEDITATION<br>ERROR IN GETTING THE ORGANIZATION BY ID"
    //                 );
    //             }

    //             return true;
    //         }
    //     );
    // }

    /*
    EditUserController.prototype.shareOrganization = function(sOrganizationId, sUserId) {

        if( (utilsIsObjectNullOrUndefined(sUserId) === true) || (utilsIsStrNullOrEmpty(sOrganizationId) === true)) {
            return false;
        }

        this.m_sSelectedOrganizationId = sOrganizationId;
        this.m_oEditOrganization = {}

        var oController = this;

        this.m_oOrganizationService.addOrganizationSharing(sOrganizationId, sUserId).then(
            function (data) {
                if (utilsIsObjectNullOrUndefined(data.data) === false) {
                    console.log("EditUserController.shareOrganization | data.data: ", data.data);

                    if (data.data.boolValue) {
                        oController.showUsersByOrganization(sOrganizationId);
                    }
                } else {
                    utilsVexDialogAlertTop(
                        "GURU MEDITATION<br>ERROR IN SHARING THE ORGANIZATION"
                    );
                }

                return true;
            }
        );
    }

    EditUserController.prototype.unshareOrganization = function(sOrganizationId, sUserId) {
        console.log("EditUserController.unshareOrganization | sOrganizationId: ", sOrganizationId);
        console.log("EditUserController.unshareOrganization | sUserId: ", sUserId);

        if( (utilsIsObjectNullOrUndefined(sUserId) === true) || (utilsIsStrNullOrEmpty(sOrganizationId) === true)) {
            return false;
        }

        this.m_sSelectedOrganizationId = sOrganizationId;
        this.m_oEditOrganization = {}

        var oController = this;

        this.m_oOrganizationService.removeOrganizationSharing(sOrganizationId, sUserId).then(
            function (data) {
                if (utilsIsObjectNullOrUndefined(data.data) === false) {
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
    */

    EditUserController.prototype.hideUsersByOrganization = function(sUserId, sOrganizationId) {
        this.m_aoUsersList = [];
        this.m_sSelectedOrganizationId = null;
        this.m_oEditOrganization = {}
        this.m_oSharingOrganization = {}
        this.m_aoMatchingUsersList = [];
        this.m_sUserPartialName = "";
    }

    EditUserController.prototype.showOrganizationEditForm = function(sUserId, sOrganizationId, sEditMode) {
        console.log("EditUserController.showOrganizationEditForm | sOrganizationId: ", sOrganizationId);

        this.m_sSelectedOrganizationId = sOrganizationId;
        this.m_oSharingOrganization = {}
        this.m_aoMatchingUsersList = [];
        this.m_sUserPartialName = "";

        let oController = this;


        this.m_oOrganizationService.getOrganizationById(sOrganizationId).then(
            function (data) {
                if (utilsIsObjectNullOrUndefined(data.data) === false) {
                    oController.m_oEditOrganization = data.data;
                    oController.m_oModalService.showModal({
                        templateUrl: "dialogs/organization_editor/OrganizationEditorDialog.html",
                        controller: 'OrganizationEditorController',
                        inputs: {
                            extras: {
                                organization: data.data,
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
            }
        );
    }

    EditUserController.prototype.deleteOrganization = function(sUserId, sOrganizationId) {
        console.log("EditUserController.deleteOrganization | sOrganizationId: ", sOrganizationId);

        this.m_sSelectedOrganizationId = null;
        this.m_aoMatchingUsersList = [];
        this.m_sUserPartialName = "";

        let sConfirmMsg = "Delete this Organization?"

        var oController = this;


        let oCallbackFunction = function(value) {
            if(value) {
                oController.m_oOrganizationService.deleteOrganization(sOrganizationId).then(function (data) {
                    if(utilsIsObjectNullOrUndefined(data.data) === false && data.data.boolValue === true) {
                        var oDialog = utilsVexDialogAlertBottomRightCorner("ORGANIZATION DELETED<br>READY");
                        utilsVexCloseDialogAfter(4000, oDialog);
                    } else if(utilsIsObjectNullOrUndefined(data.data) === false && data.data.boolValue === false) {
                        var oDialog = utilsVexDialogAlertBottomRightCorner(oController.m_oTranslate.instant(data.data.stringValue));
                        utilsVexCloseDialogAfter(5000, oDialog);
                    } else {
                        utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN DELETING ORGANIZATION");
                    }

                    oController.initializeOrganizationsInfo();

                },function (error) {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN DELETING ORGANIZATION");
            })
            }
                
        };

        utilsVexDialogConfirm(sConfirmMsg, oCallbackFunction);
    }

    EditUserController.prototype.cancelEditOrganizationForm = function() {
        console.log("EditUserController.cancelEditOrganizationForm");

        this.m_oEditOrganization = {}
        this.m_sSelectedOrganizationId = null;
        this.m_aoMatchingUsersList = [];
        this.m_sUserPartialName = "";
    }

    EditUserController.prototype.cancelSharingOrganizationForm = function() {
        console.log("EditUserController.cancelSharingOrganizationForm");

        this.m_oEditOrganization = {}
        this.m_oSharingOrganization = {}
        this.m_sSelectedOrganizationId = null;
        this.m_aoMatchingUsersList = [];
        this.m_sUserPartialName = "";
    }

    EditUserController.prototype.initializeSubscriptionsInfo = function() {
        var oController = this;

        this.m_oSubscriptionService.getSubscriptionsListByUser().then(
            function (data) {
                if (utilsIsObjectNullOrUndefined(data.data) === false) {
                    oController.m_aoSubscriptions = data.data;
                } else {
                    utilsVexDialogAlertTop(
                        "GURU MEDITATION<br>ERROR IN GETTING THE LIST OF SUBSCRIPTIONS"
                    );
                }

                oController.m_bLoadingSubscriptions = false;

                return true;
            }
        );
    }

    EditUserController.prototype.showSubscriptionEditForm = function(sUserId, sSubscriptionId, sEditMode) {
        console.log("EditUserController.showSubscriptionEditForm | sSubscriptionId: ", sSubscriptionId);

        this.m_sSelectedSubscriptionId = sSubscriptionId;

        var oController = this;


        this.m_oSubscriptionService.getSubscriptionById(sSubscriptionId).then(
            function (data) {
                if (utilsIsObjectNullOrUndefined(data.data) === false) {
                    oController.m_oEditSubscription = data.data;
                    oController.m_oModalService.showModal({
                        templateUrl: "dialogs/subscription_editor/SubscriptionEditorDialog.html",
                        controller: "SubscriptionEditorController",
                        inputs: {
                            extras: {
                                subscription: data.data,
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
            }
        )
    }

    EditUserController.prototype.deleteSubscription = function(sUserId, sSubscriptionId) {
        console.log("EditUserController.deleteSubscription | sSubscriptionId: ", sSubscriptionId);

        this.m_sSelectedSubscriptionId = null;

        var oController = this;

        this.m_oSubscriptionService.deleteSubscription(sSubscriptionId)
            .then(function (data) {
                if(utilsIsObjectNullOrUndefined(data.data) === false && data.data.boolValue === true) {
                    var oDialog = utilsVexDialogAlertBottomRightCorner("SUBSCRIPTION DELETED<br>READY");
                    utilsVexCloseDialogAfter(4000, oDialog);
                } else if(utilsIsObjectNullOrUndefined(data.data) === false && data.data.boolValue === false) {
                    var oDialog = utilsVexDialogAlertBottomRightCorner(data.data.stringValue);
                    utilsVexCloseDialogAfter(5000, oDialog);
                } else {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN DELETING SUBSCRIPTION");
                }

                oController.initializeSubscriptionsInfo();

            },function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN DELETING SUBSCRIPTION");
        });
    }

    EditUserController.prototype.cancelEditSubscriptionForm = function() {
        console.log("EditUserController.cancelEditSubscriptionForm");

        this.m_oEditSubscription = {}
        this.m_sSelectedSubscriptionId = null;
        this.m_aoMatchingOrganizationsList = [];
        this.m_sOrganizationPartialName = "";
    }

    EditUserController.prototype.showUsersBySubscription = function(sSubscriptionId) {
        console.log("EditUserController.showUsersBySubscription | sSubscriptionId: ", sSubscriptionId);

        this.m_sSelectedSubscriptionId = sSubscriptionId;
        console.log("EditUserController.showUsersBySubscription | this.m_sSelectedSubscriptionId: ", this.m_sSelectedSubscriptionId);
        this.m_oEditSubscription = {};
        this.m_oSharingSubscription = {subscriptionId: sSubscriptionId}
//        this.m_aoMatchingUsersList = [];
//        this.m_sUserPartialName = "";

        if (utilsIsStrNullOrEmpty(sSubscriptionId) === true) {
            return false;
        }

        var oController = this;

        this.m_oSubscriptionService.getUsersBySharedSubscription(sSubscriptionId).then(
            function (data) {
                if (utilsIsObjectNullOrUndefined(data.data) === false) {
                    oController.m_aoUsersList = data.data;
                    oController.m_oModalService.showModal({
                        templateUrl: "dialogs/subscription-users/SubscriptionUsersDialog.html",
                        controller: 'SubscriptionUsersController',
                        inputs: {
                            extras: {
                                users: data.data,
                                subscriptionId: oController.m_sSelectedSubscriptionId
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
                        "GURU MEDITATION<br>ERROR IN GETTING THE LIST OF USERS OF THE SUBSCRIPTION"
                    );
                }

                return true;
            }
        );
    }

    // EditUserController.prototype.showSubscriptionSharingForm = function(sSubscriptionId) {
    //     console.log("EditUserController.showSubscriptionSharingForm | sSubscriptionId: ", sSubscriptionId);

    //     this.m_sSelectedSubscriptionId = sSubscriptionId;
    //     this.m_oEditSubscription = {}
    //     this.m_oSharingSubscription = {}
    //     this.m_aoMatchingUsersList = [];

    //     var oController = this;

    //     this.m_oSubscriptionService.getSubscriptionById(sSubscriptionId).then(
    //         function (data) {
    //             if (utilsIsObjectNullOrUndefined(data.data) === false) {
    //                 oController.m_oSharingSubscription = data.data;
    //             } else {
    //                 utilsVexDialogAlertTop(
    //                     "GURU MEDITATION<br>ERROR IN GETTING THE SUBSCRIPTION BY ID"
    //                 );
    //             }

    //             return true;
    //         }
    //     );
    // }
/*
    EditUserController.prototype.shareSubscription = function(sSubscriptionId, sUserId) {

        if( (utilsIsObjectNullOrUndefined(sUserId) === true) || (utilsIsStrNullOrEmpty(sSubscriptionId) === true)) {
            return false;
        }

        this.m_sSelectedSubscriptionId = sSubscriptionId;
        this.m_oEditSubscription = {}

        var oController = this;

        this.m_oSubscriptionService.addSubscriptionSharing(sSubscriptionId, sUserId).then(
            function (data) {
                if (utilsIsObjectNullOrUndefined(data.data) === false) {
                    console.log("EditUserController.shareSubscription | data.data: ", data.data);

                    if (data.data.boolValue) {
                        oController.showUsersBySubscription(sSubscriptionId);
                    }
                } else {
                    utilsVexDialogAlertTop(
                        "GURU MEDITATION<br>ERROR IN SHARING THE SUBSCRIPTION"
                    );
                }

                return true;
            }
        );
    }

    EditUserController.prototype.unshareSubscription = function(sSubscriptionId, sUserId) {
        console.log("EditUserController.unshareSubscription | sSubscriptionId: ", sSubscriptionId);
        console.log("EditUserController.unshareSubscription | sUserId: ", sUserId);

        if( (utilsIsObjectNullOrUndefined(sUserId) === true) || (utilsIsStrNullOrEmpty(sSubscriptionId) === true)) {
            return false;
        }

        this.m_sSelectedSubscriptionId = sSubscriptionId;
        this.m_oEditSubscription = {}

        var oController = this;

        this.m_oSubscriptionService.removeSubscriptionSharing(sSubscriptionId, sUserId).then(
            function (data) {
                if (utilsIsObjectNullOrUndefined(data.data) === false) {
                    console.log("EditUserController.unshareSubscription | data.data: ", data.data);

                    if (data.data.boolValue) {
                        oController.showUsersBySubscription(sSubscriptionId);
                    }
                } else {
                    utilsVexDialogAlertTop(
                        "GURU MEDITATION<br>ERROR IN UNSHARING THE SUBSCRIPTION"
                    );
                }

                return true;
            }
        );
    }
*/

    EditUserController.prototype.hideUsersBySubscription = function(sUserId, sSubscriptionId) {
        this.m_aoUsersList = [];
        this.m_sSelectedSubscriptionId = null;
        this.m_oEditSubscription = {}
        this.m_oSharingSubscription = {}
        this.m_aoMatchingUsersList = [];
        this.m_sUserPartialName = "";
    }

    EditUserController.prototype.cancelSharingSubscriptionForm = function() {
        console.log("EditUserController.cancelSharingSubscriptionForm");

        this.m_oEditSubscription = {}
        this.m_oSharingSubscription = {}
        this.m_sSelectedSubscriptionId = null;
        this.m_aoMatchingUsersList = [];
        this.m_sUserPartialName = "";
    }




    EditUserController.prototype.changeDefaultProject = function(oProject) {
        console.log("EditUserController.changeDefaultProject | oProject: ", oProject);

        var oController = this;

        if (!utilsIsObjectNullOrUndefined(oProject)) {
            this.m_oProjectService.changeDefaultProject(oProject.projectId).then(function (data) {
                console.log("EditUserController.changeDefaultProject | data.data: ", data.data);
                if (utilsIsObjectNullOrUndefined(data.data) === false && data.data.boolValue === true) {
                    let oDialog = utilsVexDialogAlertBottomRightCorner("ACTIVE PROJECT CHANGED<br>READY");
                    utilsVexCloseDialogAfter(2000, oDialog);

                    oController.initializeProjectsInfo();
                    this.m_oProject = oProject;
                } else {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN CHANGING THE ACTIVE PROJECT");
                }
    
            }, function (error) {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN CHANGING THE ACTIVE PROJECT");
            });
        }
    }

    EditUserController.prototype.initializeProjectsInfo = function() {
        var oController = this;

        this.m_oProjectService.getProjectsListByUser().then(
            function (data) {
                if (utilsIsObjectNullOrUndefined(data.data) === false) {
                    oController.m_aoProjects = data.data;

                    const oFirstElement = { name: "No Default Project", projectId: null };
                    let aoProjects = [oFirstElement].concat(data.data);

                    oController.m_aoProjectsMap = aoProjects.map(
                        (item) => ({ name: item.name, projectId: item.projectId })
                    );


                    oController.m_oProject = oFirstElement;

                    oController.m_aoProjects.forEach((oValue) => {
                        console.log("EditUserController.initializeProjectsInfo | oValue: ", oValue);
                        if (oValue.defaultProject === true) {
                            oController.m_oProject = oValue;
                        }
                    });
                } else {
                    utilsVexDialogAlertTop(
                        "GURU MEDITATION<br>ERROR IN GETTING THE LIST OF PROJECTS"
                    );
                }

                oController.m_bLoadingProjects = false;
                console.log("EditUserController.initializeProjectsInfo | oController.m_oProject: ", oController.m_oProject);

                return true;
            }
        );
    }






    EditUserController.prototype.showProjectsBySubscription = function(sSubscriptionId) {
        console.log("EditUserController.showProjectsBySubscription | sSubscriptionId: ", sSubscriptionId);

        this.m_sSelectedSubscriptionId = sSubscriptionId;
        console.log("EditUserController.showProjectsBySubscription | this.m_sSelectedSubscriptionId: ", this.m_sSelectedSubscriptionId);
        this.m_oEditSubscription = {};
        this.m_oSharingSubscription = {subscriptionId: sSubscriptionId}
        this.m_aoMatchingUsersList = [];

        if (utilsIsStrNullOrEmpty(sSubscriptionId)) {
            return false;
        }

        var oController = this;

        this.m_oProjectService.getProjectsListBySubscription(sSubscriptionId).then(
            function (data) {
                if (utilsIsObjectNullOrUndefined(data.data) === false) {
                    oController.m_aoUsersList = data.data;
                    oController.m_oModalService.showModal({
                        templateUrl: "dialogs/subscription-projects/SubscriptionProjectsDialog.html",
                        controller: 'SubscriptionProjectsController',
                        inputs: {
                            extras: {
                                projects: data.data,
                                subscriptionId: oController.m_sSelectedSubscriptionId
                            }
                        }
                    }).then(function (modal) {
                        modal.element.modal({
                            backdrop: 'static'
                        })
                        modal.close.then(function () {
                            // oController.initializeSubscriptionsInfo();
                        })

                    })
                } else {
                    utilsVexDialogAlertTop(
                        "GURU MEDITATION<br>ERROR IN GETTING THE LIST OF PROJECTS OF THE SUBSCRIPTION"
                    );
                }

                return true;
            }
        );
    }




    EditUserController.prototype.showProjectEditForm = function(sUserId, sProjectId, sEditMode) {
        console.log("EditUserController.showProjectEditForm | sProjectId: ", sProjectId);

        var oController = this;


        this.m_oProjectService.getProjectById(sProjectId).then(
            function (data) {
                if (utilsIsObjectNullOrUndefined(data.data) === false) {
                    oController.m_oModalService.showModal({
                        templateUrl: "dialogs/project_editor/ProjectEditorDialog.html",
                        controller: "ProjectEditorController",
                        inputs: {
                            extras: {
                                project: data.data,
                                editMode: sEditMode
                            }
                        }
                    }).then(function (modal) {
                        modal.element.modal({
                            backdrop: 'static'
                        })
                        modal.close.then(function () {
                            console.log("EditUserController.showProjectEditForm | calling initializeProjectsInfo() ");
                            oController.initializeProjectsInfo();
                        })
                    })
                } else {
                    utilsVexDialogAlertTop(
                        "GURU MEDITATION<br>ERROR IN GETTING THE PROJECT BY ID"
                    );
                }
            }
        )
    }

    EditUserController.prototype.deleteProject = function(sUserId, sProjectId) {
        console.log("EditUserController.deleteProject | sProjectId: ", sProjectId);

        var oController = this;

        this.m_oProjectService.deleteProject(sProjectId)
            .then(function (data) {
                if(utilsIsObjectNullOrUndefined(data.data) === false && data.data.boolValue === true) {
                    var oDialog = utilsVexDialogAlertBottomRightCorner("PROJECT DELETED<br>READY");
                    utilsVexCloseDialogAfter(4000, oDialog);
                } else if(utilsIsObjectNullOrUndefined(data.data) === false && data.data.boolValue === false) {
                    var oDialog = utilsVexDialogAlertBottomRightCorner(data.data.stringValue);
                    utilsVexCloseDialogAfter(5000, oDialog);
                } else {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN DELETING PROJECT");
                }

                oController.initializeProjectsInfo();

            },function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN DELETING PROJECT");
        });
    }

    EditUserController.prototype.cancelEditProjectForm = function() {
        console.log("EditUserController.cancelEditProjectForm");

        this.m_aoMatchingOrganizationsList = [];
        this.m_sOrganizationPartialName = "";
    }





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
        '$translate'
    ];
    return EditUserController ;
})();
window.EditUserController= EditUserController;
