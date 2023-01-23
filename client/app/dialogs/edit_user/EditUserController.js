/**
 * Created by a.corrado on 31/03/2017.
 */


var EditUserController = (function () {

    function EditUserController($scope, oClose, oExtras, oAuthService, oConstantsService, oProcessWorkspaceService, oOrganizationService, oSubscriptionService, oAdminDashboardService, oModalService) {
        //MEMBERS
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oAuthService = oAuthService;
        this.m_oConstantsService = oConstantsService;
        this.m_oProcessWorkspaceService = oProcessWorkspaceService;
        this.m_oOrganizationService = oOrganizationService;
        this.m_oSubscriptionService = oSubscriptionService;
        this.m_oAdminDashboardService = oAdminDashboardService;
        this.m_oModalService = oModalService;

        this.m_oUser = this.m_oExtras.user;
        this.m_bEditingPassword = false;
        this.m_bEditingUserInfo = false;
        this.m_oEditUser = {};
        this.m_lTotalRuntime = null; 
        this.initializeEditUserInfo();
        this.initializeUserRuntimeInfo();
        this.m_sSelectedTab = "UserInfo";

        this.m_aoOrganizations = [];
        this.m_aoUsersList = [];
        this.m_oEditOrganization = {};
        this.m_oSharingOrganization = {};
        this.m_oShowOrganizationUsersList = false;
        this.m_oShowEditOrganizationForm = false;
        this.m_oShowSharingOrganizationForm = false;
        this.m_sSelectedOrganizationId = null;

        this.m_sUserPartialName = "";
        this.m_aoMatchingUsersList = [];

        this.initializeOrganizationsInfo();


        this.m_asTypes = [];
        this.m_aoTypesMap = [];
        this.m_oType = {};
        this.m_bLoadingTypes = true;

        this.m_aoSubscriptions = [];
        this.m_aoOrganizationsList = [];
        this.m_oEditSubscription = {};
        // this.m_oSharingOrganization = {};
        // this.m_oShowSubscriptionOrganizationsList = false;
        this.m_oShowEditSubscriptionForm = false;
        // this.m_oShowSharingOrganizationForm = false;
        this.m_sSelectedSubscriptionId = null;

        // this.m_sOrganizationPartialName = "";
        // this.m_aoMatchingOrganizationsList = [];

        this.initializeSubscriptionsInfo();


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

                return true;
            }
        );
    }

    EditUserController.prototype.showUsersByOrganization = function(sOrganizationId) {
        console.log("EditUserController.showUsersByOrganization | sOrganizationId: ", sOrganizationId);

        this.m_sSelectedOrganizationId = sOrganizationId;
        console.log("EditUserController.showUsersByOrganization | this.m_sSelectedOrganizationId: ", this.m_sSelectedOrganizationId);
        this.m_oShowOrganizationUsersList = true;
        this.m_oShowEditOrganizationForm = false;
        this.m_oEditOrganization = {};
        this.m_oSharingOrganization = {organizationId: sOrganizationId}
        this.m_aoMatchingUsersList = [];
        this.m_oShowSharingOrganizationForm = false;
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
                        templateUrl: "dialogs/org-users-edit/OrgUsersEditDialog.html",
                        controller: 'OrgUsersEditController',
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

    EditUserController.prototype.showOrganizationSharingForm = function(sOrganizationId) {
        console.log("EditUserController.showOrganizationSharingForm | sOrganizationId: ", sOrganizationId);

        this.m_sSelectedOrganizationId = sOrganizationId;
        this.m_oShowOrganizationUsersList = true;
        this.m_oShowEditOrganizationForm = false;
        this.m_oEditOrganization = {}
        this.m_oSharingOrganization = {}
        this.m_oShowSharingOrganizationForm = true;
        this.m_aoMatchingUsersList = [];

        var oController = this;

        this.m_oOrganizationService.getOrganizationById(sOrganizationId).then(
            function (data) {
                if (utilsIsObjectNullOrUndefined(data.data) === false) {
                    oController.m_oSharingOrganization = data.data;
                } else {
                    utilsVexDialogAlertTop(
                        "GURU MEDITATION<br>ERROR IN GETTING THE ORGANIZATION BY ID"
                    );
                }

                return true;
            }
        );
    }

    // EditUserController.prototype.findUsersByPartialName = function (sUserPartialName) {
    //     this.m_aoMatchingUsersList = [];

    //     if (utilsIsStrNullOrEmpty(sUserPartialName) === true) {
    //         utilsVexDialogAlertTop("GURU MEDITATION<br>AT LEAST THREE CHARACTERS MUST BE PROVIDED");

    //         return false;
    //     }

    //     utilsRemoveSpaces(sUserPartialName);

    //     if (sUserPartialName.length < 3) {
    //         utilsVexDialogAlertTop("GURU MEDITATION<br>AT LEAST THREE CHARACTERS MUST BE PROVIDED");

    //         return false;
    //     }

    //     var oController = this;

    //     this.m_oAdminDashboardService.findUsersByPartialName(sUserPartialName)
    //         .then(
    //             function (data) {
    //                 if (utilsIsObjectNullOrUndefined(data.data) === false) {
    //                     oController.m_aoMatchingUsersList = data.data;
    //                 } else {
    //                     utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN FINDING USERS");
    //                 }

    //                 // oController.clearForm();

    //                 return true;
    //             },
    //             function (error) {
    //                 console.log("EditUserController.findUsersByPartialName | error.data.message: ", error.data.message);

    //                 let errorMessage = oController.m_oTranslate.instant(error.data.message);

    //                 utilsVexDialogAlertTop(errorMessage);
    //             }
    //         );
    // };

    EditUserController.prototype.shareOrganization = function(sOrganizationId, sUserId) {
        console.log("EditUserController.shareOrganization | sOrganizationId: ", sOrganizationId);
        console.log("EditUserController.shareOrganization | sUserId: ", sUserId);

        if( (utilsIsObjectNullOrUndefined(sUserId) === true) || (utilsIsStrNullOrEmpty(sOrganizationId) === true)) {
            return false;
        }

        this.m_sSelectedOrganizationId = sOrganizationId;
        this.m_oShowOrganizationUsersList = true;
        this.m_oShowEditOrganizationForm = false;
        this.m_oEditOrganization = {}
        // this.m_aoMatchingUsersList = [];

        var oController = this;

        this.m_oOrganizationService.addOrganizationSharing(sOrganizationId, sUserId).then(
            function (data) {
                if (utilsIsObjectNullOrUndefined(data.data) === false) {
                    // oController.m_aoUsersList = data.data;
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
        this.m_oShowOrganizationUsersList = true;
        this.m_oShowEditOrganizationForm = false;
        this.m_oEditOrganization = {}
        // this.m_aoMatchingUsersList = [];

        var oController = this;

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

    EditUserController.prototype.hideUsersByOrganization = function(sUserId, sOrganizationId) {
        this.m_aoUsersList = [];
        this.m_oShowOrganizationUsersList = false;
        this.m_oShowEditOrganizationForm = false;
        this.m_sSelectedOrganizationId = null;
        this.m_oEditOrganization = {}
        this.m_oSharingOrganization = {}
        this.m_aoMatchingUsersList = [];
        this.m_oShowSharingOrganizationForm = false;
        this.m_sUserPartialName = "";
    }

    EditUserController.prototype.showOrganizationEditForm = function(sUserId, sOrganizationId) {
        console.log("EditUserController.showOrganizationEditForm | sOrganizationId: ", sOrganizationId);

        this.m_oShowOrganizationUsersList = false;
        this.m_oShowEditOrganizationForm = true;
        this.m_sSelectedOrganizationId = sOrganizationId;
        this.m_oSharingOrganization = {}
        this.m_aoMatchingUsersList = [];
        this.m_oShowSharingOrganizationForm = false;
        this.m_sUserPartialName = "";

        let oController = this;


        this.m_oOrganizationService.getOrganizationById(sOrganizationId).then(
            function (data) {
                if (utilsIsObjectNullOrUndefined(data.data) === false) {
                    oController.m_oEditOrganization = data.data;
                    oController.m_oModalService.showModal({
                        templateUrl: "dialogs/organization-edit/OrganizationEditDialog.html",
                        controller: 'OrganizationEditorController',
                        inputs: {
                            extras: {
                                organization: data.data
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

        this.m_oShowOrganizationUsersList = false;
        this.m_oShowEditOrganizationForm = false;
        this.m_sSelectedOrganizationId = null;
        this.m_aoMatchingUsersList = [];
        this.m_oShowSharingOrganizationForm = false;
        this.m_sUserPartialName = "";

        let sConfirmMsg = "Delete this Organization?"

        var oController = this;


        let oCallbackFunction = function(value) {
            if(value) {
                oController.m_oOrganizationService.deleteOrganization(sOrganizationId).then(function (data) {
                    if(utilsIsObjectNullOrUndefined(data.data) === false && data.data.boolValue === true) {
                        var oDialog = utilsVexDialogAlertBottomRightCorner("ORGANIZATION DELETED<br>READY");
                        utilsVexCloseDialogAfter(4000, oDialog);
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

    EditUserController.prototype.saveOrganization = function() {
        console.log("EditUserController.saveOrganization | m_oEditOrganization: ", this.m_oEditOrganization);

        this.m_oShowOrganizationUsersList = false;
        this.m_oShowEditOrganizationForm = false;
        this.m_sSelectedOrganizationId = null;
        this.m_oSharingOrganization = {}
        this.m_aoMatchingUsersList = [];
        this.m_oShowSharingOrganizationForm = false;
        this.m_sUserPartialName = "";

        var oController = this;
        this.m_oOrganizationService.saveOrganization(this.m_oEditOrganization)
            .then(function (data) {
                if(utilsIsObjectNullOrUndefined(data.data) === false && data.data.boolValue === true) {
                    var oDialog = utilsVexDialogAlertBottomRightCorner("ORGANIZATION SAVED<br>READY");
                    utilsVexCloseDialogAfter(4000, oDialog);
                } else {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN SAVING ORGANIZATION");
                }

                oController.initializeOrganizationsInfo();

            },function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN SAVING ORGANIZATION");
        });
    
        this.m_oEditOrganization = {}
    }

    EditUserController.prototype.cancelEditOrganizationForm = function() {
        console.log("EditUserController.cancelEditOrganizationForm");

        this.m_oShowOrganizationUsersList = false;
        this.m_oShowEditOrganizationForm = false;
        this.m_oEditOrganization = {}
        this.m_sSelectedOrganizationId = null;
        this.m_aoMatchingUsersList = [];
        this.m_oShowSharingOrganizationForm = false;
        this.m_sUserPartialName = "";
    }

    EditUserController.prototype.cancelSharingOrganizationForm = function() {
        console.log("EditUserController.cancelSharingOrganizationForm");

        this.m_oShowOrganizationUsersList = false;
        this.m_oShowEditOrganizationForm = false;
        this.m_oEditOrganization = {}
        this.m_oSharingOrganization = {}
        this.m_sSelectedOrganizationId = null;
        this.m_aoMatchingUsersList = [];
        this.m_oShowSharingOrganizationForm = false;
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

                return true;
            }
        );
    }

    EditUserController.prototype.showSubscriptionEditForm = function(sUserId, sSubscriptionId) {
        console.log("EditUserController.showSubscriptionEditForm | sSubscriptionId: ", sSubscriptionId);

        // this.m_oShowSubscriptionOrganizationsList = false;
        this.m_oShowEditSubscriptionForm = true;
        this.m_sSelectedSubscriptionId = sSubscriptionId;
        // this.m_oSharingSubscription = {}
        // this.m_aoMatchingOrganizationsList = [];
        // this.m_oShowSharingSubscriptionForm = false;
        // this.m_sOrganizationPartialName = "";

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
                                subscription: data.data
                            }
                        }
                    }).then(function (modal) {
                        modal.element.modal({
                            backdrop: 'static'
                        })
                        modal.close.then(function (result) {
                            oController.initializeSubscriptionsInfo();

                        })
                    })


                }
            }
        )

        // this.m_oSubscriptionService.getSubscriptionById(sSubscriptionId).then(
        //     function (data) {
        //         if (utilsIsObjectNullOrUndefined(data.data) === false) {
        //             
        //             oController.m_oSubscriptionService.getSubscriptionTypes().then(
        //                 function (data) {
        //                     if (data.status !== 200) {
        //                         var oDialog = utilsVexDialogAlertBottomRightCorner(
        //                             "GURU MEDITATION<br>ERROR GETTING SUBSCRIPTION TYPES"
        //                         );
        //                         utilsVexCloseDialogAfter(5000, oDialog);
        //                     } else {
        //                         oController.m_asTypes = data.data.map((item) => item.name);
        //                         oController.m_aoTypesMap = oController.m_asTypes.map(
        //                             (name) => ({ name })
        //                         );

        //                         oController.m_aoTypesMap.forEach((oValue, sKey) => {
        //                             if (oValue.name == oController.m_oEditSubscription.type) {
        //                                 oController.m_oType = oValue;
        //                             }
        //                         });
        //                     }

        //                     oController.m_bLoadingTypes = false;
        //                 },
        //                 function (data) {
        //                     var oDialog = utilsVexDialogAlertBottomRightCorner(
        //                         "GURU MEDITATION<br>ERROR GETTING TYPES"
        //                     );
        //                     utilsVexCloseDialogAfter(5000, oDialog);
        //                     oController.m_bLoadingTypes = false;
        //                 }
        //             );

        //         } else {
        //             utilsVexDialogAlertTop(
        //                 "GURU MEDITATION<br>ERROR IN GETTING THE SUBSCRIPTION BY ID"
        //             );
        //         }

        //         return true;
        //     }
        // );
    }

    EditUserController.prototype.deleteSubscription = function(sUserId, sSubscriptionId) {
        console.log("EditUserController.deleteSubscription | sSubscriptionId: ", sSubscriptionId);

        // this.m_oShowSubscriptionOrganizationsList = false;
        this.m_oShowEditSubscriptionForm = false;
        this.m_sSelectedSubscriptionId = null;
        // this.m_aoMatchingOrganizationsList = [];
        // this.m_oShowSharingSubscriptionForm = false;
        // this.m_sOrganizationPartialName = "";

        var oController = this;

        this.m_oSubscriptionService.deleteSubscription(sSubscriptionId)
            .then(function (data) {
                if(utilsIsObjectNullOrUndefined(data.data) === false && data.data.boolValue === true) {
                    var oDialog = utilsVexDialogAlertBottomRightCorner("SUBSCRIPTION DELETED<br>READY");
                    utilsVexCloseDialogAfter(4000, oDialog);
                } else {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN DELETING SUBSCRIPTION");
                }

                oController.initializeSubscriptionsInfo();

            },function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN DELETING SUBSCRIPTION");
        });
    }

    EditUserController.prototype.saveSubscription = function() {
        console.log("EditUserController.saveSubscription | m_oEditSubscription: ", this.m_oEditSubscription);

        // this.m_oShowSubscriptionOrganizationsList = false;
        this.m_oShowEditSubscriptionForm = false;
        this.m_sSelectedSubscriptionId = null;
        // this.m_oSharingSubscription = {}
        // this.m_aoMatchingOrganizationsList = [];
        // this.m_oShowSharingSubscriptionForm = false;
        // this.m_sOrganizationPartialName = "";

        var sType = "";

        if (!utilsIsObjectNullOrUndefined(this.m_oType)) {
            sType = this.m_oType.name;
        }

        this.m_oEditSubscription.type = sType;

        var oController = this;
        this.m_oSubscriptionService.saveSubscription(this.m_oEditSubscription)
            .then(function (data) {
                if(utilsIsObjectNullOrUndefined(data.data) === false && data.data.boolValue === true) {
                    var oDialog = utilsVexDialogAlertBottomRightCorner("SUBSCRIPTION SAVED<br>READY");
                    utilsVexCloseDialogAfter(4000, oDialog);
                } else {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN SAVING SUBSCRIPTION");
                }

                oController.initializeSubscriptionsInfo();

            },function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN SAVING ORGANIZATION");
        });
    
        this.m_oEditSubscription = {}
        this.m_oType = {};
    }

    EditUserController.prototype.cancelEditSubscriptionForm = function() {
        console.log("EditUserController.cancelEditSubscriptionForm");

        // this.m_oShowSubscriptionOrganizationsList = false;
        this.m_oShowEditSubscriptionForm = false;
        this.m_oEditSubscription = {}
        this.m_sSelectedSubscriptionId = null;
        this.m_aoMatchingOrganizationsList = [];
        this.m_oShowSharingSubscriptionForm = false;
        this.m_sOrganizationPartialName = "";
    }

    /*
        EditUserController.prototype.showUsersByOrganization = function(sOrganizationId) {
            console.log("EditUserController.showUsersByOrganization | sOrganizationId: ", sOrganizationId);
    
            this.m_sSelectedOrganizationId = sOrganizationId;
            console.log("EditUserController.showUsersByOrganization | this.m_sSelectedOrganizationId: ", this.m_sSelectedOrganizationId);
            this.m_oShowOrganizationUsersList = true;
            this.m_oShowEditOrganizationForm = false;
            this.m_oEditOrganization = {};
            this.m_oSharingOrganization = {organizationId: sOrganizationId}
            this.m_aoMatchingUsersList = [];
            this.m_oShowSharingOrganizationForm = false;
            this.m_sUserPartialName = "";
    
            if (utilsIsStrNullOrEmpty(sOrganizationId) === true) {
                return false;
            }
    
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
        EditUserController.prototype.hideUsersByOrganization = function(sUserId, sOrganizationId) {
            this.m_aoUsersList = [];
            this.m_oShowOrganizationUsersList = false;
            this.m_oShowEditOrganizationForm = false;
            this.m_sSelectedOrganizationId = null;
            this.m_oEditOrganization = {}
            this.m_oSharingOrganization = {}
            this.m_aoMatchingUsersList = [];
            this.m_oShowSharingOrganizationForm = false;
            this.m_sUserPartialName = "";
        }
    */





    EditUserController.$inject = [
        '$scope',
        'close',
        'extras',
        'AuthService',
        'ConstantsService',
        'ProcessWorkspaceService',
        'OrganizationService',
        'SubscriptionService',
        'AdminDashboardService',
        'ModalService'
    ];
    return EditUserController ;
})();
window.EditUserController= EditUserController;
