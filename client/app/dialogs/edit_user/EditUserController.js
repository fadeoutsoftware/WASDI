/**
 * Created by a.corrado on 31/03/2017.
 */


var EditUserController = (function() {

    function EditUserController ($scope, oClose,oExtras,oAuthService,oConstantsService, oProcessWorkspaceService) {
        //MEMBERS
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oAuthService = oAuthService;
        this.m_oConstantsService = oConstantsService;
        this.m_oProcessWorkspaceService = oProcessWorkspaceService;
        this.m_oUser = this.m_oExtras.user;
        this.m_bEditingPassword = false;
        this.m_bEditingUserInfo = false;
        this.m_oEditUser = {};
        this.m_lTotalRuntime = null; 
        this.initializeEditUserInfo();
        this.initializeUserRuntimeInfo();

        $scope.close = function(result) {
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
                            oController.m_lTotalRuntime = new Date(data.data).toISOString().slice(11, 19);
                        } else {
                            utilsVexDialogAlertTop(
                                "GURU MEDITATION<br>ERROR IN GETTING THE TOTAL RUNNING TIME"
                            );
                        }

                        return true;
                    }
        );
    }; 

    EditUserController.$inject = [
        '$scope',
        'close',
        'extras',
        'AuthService',
        'ConstantsService',
        'ProcessWorkspaceService'
    ];
    return EditUserController ;
})();
window.EditUserController= EditUserController;
