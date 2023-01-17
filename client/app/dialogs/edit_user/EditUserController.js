/**
 * Created by a.corrado on 31/03/2017.
 */


var EditUserController = (function () {

    function EditUserController($scope, oClose, oExtras, oAuthService, oConstantsService, oProcessWorkspaceService, oOrganizationService) {
        //MEMBERS
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oAuthService = oAuthService;
        this.m_oConstantsService = oConstantsService;
        this.m_oProcessWorkspaceService = oProcessWorkspaceService;
        this.m_oOrganizationService = oOrganizationService;
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
        this.m_oShowOrganizationUsersList = false;
        this.m_oShowEditOrganizationForm = false;
        this.m_sSelectedOrganizationId = null;

        this.initializeOrganizationsInfo();

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
        // this.m_aoOrganizations = [
        //     {organizationId: "org-1", name: "WASDI", ownerUserId: "p.campanella@fadeout.it"},
        //     {organizationId: "org-2", name: "Fadeout Software", ownerUserId: "p.campanella@fadeout.it"}
        // ];

        var oController = this;

        this.m_oOrganizationService.getOrganizationListByUser().then(
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

    EditUserController.prototype.showUsersByOrganization = function(sUserId, sOrganizationId) {
        if( (utilsIsObjectNullOrUndefined(sUserId) === true) || (utilsIsStrNullOrEmpty(sOrganizationId) === true)) {
            //TODO THROW ERROR ?
            return false;
        }

        this.m_sSelectedOrganizationId = sOrganizationId;

        if (sOrganizationId === "org-1") {
            this.m_aoUsersList = [
                {"name":"Petru","role":"Developer","sessionId":null,"surname":"Petrescu","userId":"petru.petrescu@wasdi.cloud"},
                {"name":"Betty","role":"Developer","sessionId":null,"surname":"Spurgeon","userId":"betty.spurgeon@wasdi.cloud"}
            ];
        } else if (sOrganizationId === "org-2") {
            this.m_aoUsersList = [
                {"name":"Marco","role":"Developer","sessionId":null,"surname":"Menapace","userId":"m.menapace@fadeout.it"},
                {"name":"Cristiano","role":"Developer","sessionId":null,"surname":"Nattero","userId":"c.nattero@fadeout.it"}
            ];
        }

        this.m_oShowOrganizationUsersList = true;
        this.m_oShowEditOrganizationForm = false;
        this.m_oEditOrganization = {}

        var oController = this;


    }

    EditUserController.prototype.hideUsersByOrganization = function(sUserId, sOrganizationId) {
        this.m_aoUsersList = [];
        this.m_oShowOrganizationUsersList = false;
        this.m_oShowEditOrganizationForm = false;
        this.m_sSelectedOrganizationId = null;
        this.m_oEditOrganization = {}
    }

    EditUserController.prototype.showOrganizationEditForm = function(sUserId, sOrganizationId) {
        console.log("EditUserController.showOrganizationEditForm | sOrganizationId: ", sOrganizationId);
        
        if (utilsIsStrNullOrEmpty(sOrganizationId) === true) {
            this.m_oEditOrganization = {};
        } else if (sOrganizationId === "org-1") {
            this.m_aoUsersList = [
                {"name":"Petru","role":"Developer","sessionId":null,"surname":"Petrescu","userId":"petru.petrescu@wasdi.cloud"},
                {"name":"Betty","role":"Developer","sessionId":null,"surname":"Spurgeon","userId":"betty.spurgeon@wasdi.cloud"}
            ];

            this.m_oEditOrganization = {organizationId: "org-1", name: "WASDI", ownerUserId: "p.campanella@fadeout.it", description: "A Luxembourgish company", address: "Luxembourg", email: "info@wasdi.cloud", url: "wasdi.net"};
        } else if (sOrganizationId === "org-2") {
            this.m_aoUsersList = [
                {"name":"Marco","role":"Developer","sessionId":null,"surname":"Menapace","userId":"m.menapace@fadeout.it"},
                {"name":"Cristiano","role":"Developer","sessionId":null,"surname":"Nattero","userId":"c.nattero@fadeout.it"}
            ];

            this.m_oEditOrganization = {organizationId: "org-2", name: "Fadeout Software", ownerUserId: "p.campanella@fadeout.it", description: "An Italian company", address: "Genoa", email: "info@fadeout.it", url: "fadeout.it"};
        }

        this.m_oShowOrganizationUsersList = false;
        this.m_oShowEditOrganizationForm = true;
        this.m_sSelectedOrganizationId = sOrganizationId;
    }

    EditUserController.prototype.deleteOrganization = function(sUserId, sOrganizationId) {
        console.log("EditUserController.deleteOrganization | sOrganizationId: ", sOrganizationId);

        this.m_oShowOrganizationUsersList = false;
        this.m_oShowEditOrganizationForm = false;
        this.m_sSelectedOrganizationId = null;

        // if (sOrganizationId === "org-1") {
        //     this.m_aoOrganizations = [
        //         {organizationId: "org-2", name: "Fadeout Software", ownerUserId: "p.campanella@fadeout.it"}
        //     ];
        // } else if (sOrganizationId === "org-2") {
        //     this.m_aoOrganizations = [
        //         {organizationId: "org-1", name: "WASDI", ownerUserId: "p.campanella@fadeout.it"}
        //     ];
        // }

        var oController = this;
        this.m_oOrganizationService.deleteOrganization(sOrganizationId)
            .then(function (data) {
                if(utilsIsObjectNullOrUndefined(data.data) === false && data.data.boolValue === true) {
                    //TODO Organization Deleted
                } else {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN DELETING ORGANIZATION");
                }

                oController.initializeOrganizationsInfo();

            },function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN DELETING ORGANIZATION");
        });

    }

    EditUserController.prototype.saveOrganization = function() {
        console.log("EditUserController.saveOrganization | m_oEditOrganization: ", this.m_oEditOrganization);

        this.m_oShowOrganizationUsersList = false;
        this.m_oShowEditOrganizationForm = false;
        this.m_sSelectedOrganizationId = null;

        // var oJsonToSend = this.getUserInfo();
        // var oController = this;

        // this.m_bEditingUserInfo = true;
        // this.m_oAuthService.changeUserInfo(oJsonToSend)
        //     .then(function (data) {
        //         if(utilsIsObjectNullOrUndefined(data.data) === false ||  data.data.userId !== "" )
        //         {
        //             if(data.data.boolValue ===  false)
        //             {
        //                 utilsVexDialogAlertTop("GURU MEDITATION<br>IMPOSSIBLE TO CHANGE USER INFO");
        //             }
        //             else
        //             {
        //                 var oVexWindow = utilsVexDialogAlertBottomRightCorner("CHANGED USER INFO");
        //                 utilsVexCloseDialogAfter(3000,oVexWindow);
        //                 oController.m_oUser = data.data;
        //                 oController.m_oConstantsService.setUser(data.data);//save in cookie
        //             }


        //         }
        //         else
        //         {
        //             utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN CHANGE USER INFO");
        //         }
        //         oController.m_bEditingUserInfo = false;
        //         oController.initializeEditUserInfo();

        //     },(function (error)
        //     {
        //         utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN CHANGE USER INFO");
        //         oController.m_bEditingUserInfo = false;
        //         oController.initializeEditUserInfo();
        //     }));
    }

    EditUserController.prototype.cancelEditOrganizationForm = function() {
        console.log("EditUserController.cancelEditOrganizationForm");

        this.m_oShowOrganizationUsersList = false;
        this.m_oShowEditOrganizationForm = false;
        this.m_oEditOrganization = {}
        this.m_sSelectedOrganizationId = null;
    }

    EditUserController.$inject = [
        '$scope',
        'close',
        'extras',
        'AuthService',
        'ConstantsService',
        'ProcessWorkspaceService',
        'OrganizationService'
    ];
    return EditUserController ;
})();
window.EditUserController= EditUserController;
