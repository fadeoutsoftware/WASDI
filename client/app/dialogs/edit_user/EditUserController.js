/**
 * Created by a.corrado on 31/03/2017.
 */


var EditUserController = (function() {

    function EditUserController ($scope, oClose,oExtras,oAuthService,oConstantsService) {
        //MEMBERS
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oAuthService = oAuthService;
        this.m_oConstantsService = oConstantsService;
        this.m_oUser = this.m_oExtras.user;
        this.m_bEditingPassword = false;
        this.m_bEditingUserInfo = false;
        this.m_oEditUser = {};
        this.initializeEditUserInfo();

        var oController = this;
        $scope.close = function(result) {
            oClose(result, 300); // close, but give 500ms for bootstrap to animate
        };

    };

    EditUserController.prototype.changePassword = function()
    {

        var oJsonToSend = this.getPasswordsJSON();
        var oController = this;

        this.m_bEditingPassword = true;
        this.m_oAuthService.changePassword(oJsonToSend)
            .success(function (data) {
                if(utilsIsObjectNullOrUndefined(data) === false)
                {
                    if(data.boolValue ===  false)
                    {
                        utilsVexDialogAlertTop("GURU MEDITATION<br>WAS IMPOSSIBLE CHANGE THE PASSWORD");
                    }
                    else
                    {
                        var oVexWindow = utilsVexDialogAlertBottomRightCorner("CHANGED PASSWORD");
                        utilsVexCloseDialogAfterFewSeconds(3000,oVexWindow);

                    }

                }
                else
                {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN CHANGE PASSWORD");
                }
                oController.m_bEditingPassword = false;
                oController.cleanPasswordsInEditUserObject();

            })
            .error(function (error)
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN CHANGE PASSWORD");
                oController.m_bEditingPassword = false;
                oController.cleanPasswordsInEditUserObject();
            });
    }

    EditUserController.prototype.changeUserInfo = function()
    {

        var oJsonToSend = this.getUserInfo();
        var oController = this;

        this.m_bEditingUserInfo = true;
        this.m_oAuthService.changeUserInfo(oJsonToSend)
            .success(function (data) {
                if(utilsIsObjectNullOrUndefined(data) === false ||  data.userId !== "" )
                {
                    if(data.boolValue ===  false)
                    {
                        utilsVexDialogAlertTop("GURU MEDITATION<br>WAS IMPOSSIBLE CHANGE THE USER INFO");
                    }
                    else
                    {
                        var oVexWindow = utilsVexDialogAlertBottomRightCorner("CHANGED USER INFO");
                        utilsVexCloseDialogAfterFewSeconds(3000,oVexWindow);
                        oController.m_oUser = data;
                        oController.m_oConstantsService.setUser(data);//save in coockie
                    }


                }
                else
                {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN CHANGE USER INFO");
                }
                oController.m_bEditingUserInfo = false;
                oController.initializeEditUserInfo();

            })
            .error(function (error)
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN CHANGE USER INFO");
                oController.m_bEditingUserInfo = false;
                oController.initializeEditUserInfo();
            });
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

    EditUserController.$inject = [
        '$scope',
        'close',
        'extras',
        'AuthService',
        'ConstantsService'


    ];
    return EditUserController ;
})();
