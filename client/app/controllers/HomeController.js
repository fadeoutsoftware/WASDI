/**
 * Created by p.campanella on 21/10/2016.
 */

var HomeController = (function () {
    function HomeController($rootScope, $scope, $location, oConstantsService, oAuthService, oRabbitStompService, oState,
        oWindow, $anchorScroll, oTranslate) {
        this.m_oScope = $scope;
        this.m_oRootScope = $rootScope;
        this.m_oLocation = $location;
        this.m_oConstantsService = oConstantsService;
        this.m_oAuthService = oAuthService;
        this.m_oRabbitStompService = oRabbitStompService;
        this.m_oState = oState;
        this.m_oAnchorService = $anchorScroll;
        this.m_oTranslate = oTranslate;

        this.m_sEmailToRecoverPassword = "";
        this.m_oScope.m_oController = this;
        this.m_bLoginIsVisible = false;//Login in visible after click on logo
        this.m_bIsVisibleRecoveryPassword = false;
        this.m_sUserName = "";
        this.m_sUserPassword = "";
        this.m_bSuccess = false;
        this.m_bError = false;
        this.m_sMessageError = "";

        this.m_bInternalKeycloakFlag = false;

        this.m_oRegistrationUser = {
            name: "",
            surname: "",
            password: "",
            repeatPassword: "",
            email: "",
            userId: ""//userId == email
        };
        this.m_bRegisterIsVisible = false;
        this.m_bBrowserIsIE = utilsUserUseIEBrowser();
        this.m_bVisualizeLink = false;

        this.m_bRegistering = false;

        var oController = this;

        // define in any case the listener
        this.m_oScope.$on('KC_INIT_DONE', function (events, args) {
            oController.checkKeycloakAuthStatus(oController);
        });
        if (bKeyCloakInitialized) {
            this.checkKeycloakAuthStatus(oController);
        }
        else {
            this.waitForKeycloak(oController);
        }


        this.m_bBrowserIsIE = utilsUserUseIEBrowser();

        if (this.m_bBrowserIsIE === true) {
            this.m_bVisualizeLink = false;
            alert("Wasdi doesn't work on IE/EDGE");// + this.m_bVisualizeLink
        } else {
            this.m_bVisualizeLink = true;
        }
    }

    HomeController.prototype.checkKeycloakAuthStatus = function (oController) {

        if (oKeycloak.authenticated) {
            if (oKeycloak.idToken) {
                var aoDataTokens = {
                    'access_token': oKeycloak.idToken,
                    'refresh_token': oKeycloak.refreshToken
                };
            }
            oController.callbackLogin(aoDataTokens, null, oController);
        }
        else {
            console.log("HomeController: not authenticated")
        }
    }

    HomeController.prototype.waitForKeycloak = function (oController) {
        if (bKeyCloakInitialized == false) {
            window.setTimeout(this.waitForKeycloak, 100, oController); /* this checks the flag every 100 milliseconds*/
            return;
        } else {
            oController.m_oRootScope.$broadcast("KC_INIT_DONE");
        }
    }


    HomeController.prototype.changeVisibilityLoginRegister = function (sStatus) {

        if (sStatus === "Login") {
            this.m_bLoginIsVisible = true;
            this.m_bRegisterIsVisible = false;
            this.cleanSignInForm();

        }

        if (sStatus === "Register") {
            this.m_bLoginIsVisible = false;
            this.m_bRegisterIsVisible = true;
        }
    };

    HomeController.prototype.moveTo = function (sPath) {
        this.m_oLocation.path(sPath);
    }

    HomeController.prototype.login = function () {
        var oLoginInfo = {};
        var oController = this;
        oLoginInfo.userId = oController.m_sUserName;
        oLoginInfo.userPassword = oController.m_sUserPassword;
        this.m_oConstantsService.setUser(null);

        var sMessage = this.m_oTranslate.instant("MSG_LOGIN_ERROR");
        this.m_oAuthService.legacyLogin(oLoginInfo).then(
            function (data, status) {
                oController.callbackLogin(data.data, status, oController)
            }, function (data, status) {
                //alert('error');
                utilsVexDialogAlertTop(sMessage);
            });
    }


    HomeController.prototype.callbackLogin = function (data, status, oController) {
        /*two mode callback login:
        * 1- set SessionId directly with response data (Legacy)
        * 2- Decode the token to obtain the fields (KC) <- Implemented down here
        * **/

        if (!oController) oController = this;
        if ((data.hasOwnProperty("sessionId")) && data.sessionId == null) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>LOGIN ERROR");
            return;
        }
        if (data.hasOwnProperty("sessionId")) {
            let oUser = {};
            oUser.userId = data.userId;
            oUser.authProvider = "wasdi";
            oUser.name = data.name;
            oUser.surname = data.surname;
            oUser.sessionId = data.sessionId;
            oUser.role = data.role;
            oUser.type = data.type; 
            oController.m_oConstantsService.setUser(oUser);//set user
            console.log("We are in the sessionId branch")
            oController.m_oState.go("root.marketplace");// go workspaces -> go to marketplace
        }
        else {
            window.localStorage.access_token = data['access_token']
            window.localStorage.refresh_token = data['refresh_token']

            var oDecodedToken = jwt_decode(data['access_token']);

            let oUser = {};
            oUser.userId = oDecodedToken.preferred_username;
            oUser.name = oDecodedToken.given_name;
            oUser.surname = oDecodedToken.family_name;
            oUser.type = data.type; 
            oUser.authProvider = "wasdi";
            oUser.link;
            oUser.description;
            oUser.sessionId = data['access_token'];
            oUser.refreshToken = data['refresh_token'];

            oController.m_oConstantsService.setUser(oUser);//set user
            console.log("We are in the Keycloak branch")
            oController.m_oState.go("root.marketplace");// go workspaces -> go to marketplace
        }

    }

    HomeController.prototype.getUserName = function () {
        var oUser = this.m_oConstantsService.getUser();

        if (oUser != null) {
            if (oUser != undefined) {
                var sName = oUser.name;
                if (sName == null) sName = "";
                if (sName == undefined) sName = "";

                if (oUser.surname != null) {
                    if (oUser.surname != undefined) {
                        sName += " " + oUser.surname;

                        return sName;
                    }
                }
            }
        }

        return "";
    }

    /**
     *
     */

    HomeController.prototype.keycloakLogin = function () {
        oKeycloak.login();
    }
    HomeController.prototype.keycloakRegister = function () {
        oKeycloak.register();
    }

    /**
     * signingUser
     * @returns {boolean}
     */
    HomeController.prototype.signingUser = function (myForm) {
        var oUser = {};
        var oController = this;

        if ((utilsIsObjectNullOrUndefined(oController.m_oRegistrationUser) === true) || (this.isValidSigningUser(oController.m_oRegistrationUser) === false)) {
            return false;
        }

        oController.m_bRegistering = true;

        oUser.userId = oController.m_oRegistrationUser.userId;
        oUser.password = oController.m_oRegistrationUser.password;
        oUser.name = oController.m_oRegistrationUser.name;
        oUser.surname = oController.m_oRegistrationUser.surname;

        if (utilsIsStrNullOrEmpty(oUser.name)) oUser.name = oUser.userId;
        if (utilsIsStrNullOrEmpty(oUser.surname)) oUser.surname = "";

        this.m_bSuccess = false;
        this.m_bError = false;
        this.m_oAuthService.signingUser(oUser).then(

            function (data, status) {
                if (utilsIsObjectNullOrUndefined(data) !== true) {
                    if (data.data.boolValue === true) {
                        oController.m_bSuccess = true;
                    }
                    else {
                        if (utilsIsStrNullOrEmpty(data.data.stringValue) === false) {
                            oController.m_sMessageError = data.data.stringValue;
                        }
                        oController.m_bError = true;

                        utilsVexDialogAlertTop("GURU MEDITATION<br>" + oController.m_sMessageError);
                    }
                } else {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>SIGNIN ERROR");
                }

                oController.m_bRegistering = false;
            }, (function (data, status) {
                utilsVexDialogAlertTop("GURU MEDITATION<br>SIGNIN ERROR");
                oController.m_bRegistering = false;
            }));

        return true;
    }

    HomeController.prototype.isValidSigningUser = function (oRegistrationUser) {
        if (utilsIsObjectNullOrUndefined(oRegistrationUser) === true) {
            return false;
        }

        if ((utilsIsStrNullOrEmpty(oRegistrationUser.userId) === true) || (utilsIsEmail(oRegistrationUser.userId) === false)) //
        {
            return false;
        }

        if ((utilsIsStrNullOrEmpty(oRegistrationUser.password) === true) || (oRegistrationUser.password.length < 8) ||
            (utilsIsStrNullOrEmpty(oRegistrationUser.repeatPassword) === true) || (oRegistrationUser.password !== oRegistrationUser.repeatPassword)) {
            return false;
        }

        if (utilsIsStrNullOrEmpty(oRegistrationUser.name) === true) {
            return false;
        }

        if (utilsIsStrNullOrEmpty(oRegistrationUser.surname) === true) {
            return false;
        }

        return true;
    }
    /**
     * isUserLogged
     */
    HomeController.prototype.isUserLogged = function () {
        return this.m_oConstantsService.isUserLogged();
    };

    HomeController.prototype.cleanSignInForm = function () {
        this.m_bSuccess = false;
        this.m_bError = false;
        this.m_oScope.signinForm.$setPristine();
        this.m_oRegistrationUser = {
            name: "",
            surname: "",
            password: "",
            repeatPassword: "",
            email: "",
            userId: ""//userId == email
        }
    }

    HomeController.prototype.recoverPassword = function (sEmailToRecoverPassword) {
        if (utilsIsStrNullOrEmpty(sEmailToRecoverPassword) === true) {
            return false;
        }
        var oController = this;

        var sMessage1 = this.m_oTranslate.instant("MSG_RECOVER_1");
        var sMessage2 = this.m_oTranslate.instant("MSG_RECOVER_2");
        var sMessage3 = this.m_oTranslate.instant("MSG_LOGIN_ERROR");
        var sMessage4 = "Error recovering account. Please try attemping recover with Keycloak. Press 'ok' to be directed."
        this.m_oAuthService.recoverPassword(sEmailToRecoverPassword).then(
            function (data, status) {
                // oController.callbackLogin(data, status,oController);
                if (utilsIsObjectNullOrUndefined(data) !== true) {
                    if (data.data.boolValue === true) {
                        oController.m_bSuccess = true;
                        utilsVexDialogAlertTop(sMessage1
                            + sEmailToRecoverPassword
                            + sMessage2);
                        // then hide the recovery password dialog
                        oController.m_bIsVisibleRecoveryPassword = !oController.m_bIsVisibleRecoveryPassword;

                    } else if (data.data.boolValue === false){ 
                        utilsVexDialogConfirm(sMessage4, function(value){
                            if(value) {
                               oController.keycloakLogin() 
                            }
                            
                        }) 
                    } else {
                        if (utilsIsStrNullOrEmpty(data.data.stringValue) === false) {
                            oController.m_sMessageError = data.stringValue;
                        }
                        oController.m_bError = true;
                    }   
                } else {
                    utilsVexDialogAlertTop(sMessage3);
                }
            }, (function (data, status) {
                //alert('error');
                utilsVexDialogAlertTop(sMessage3);

            }));

        return true;
    };

    HomeController.prototype.isRecoverPasswordButtonEnable = function (sEmailToRecoverPassword) {
        if (utilsIsStrNullOrEmpty(sEmailToRecoverPassword) === true || sEmailToRecoverPassword === "" || utilsIsEmail(sEmailToRecoverPassword) === false) {
            return false;
        }

        return true;
    }

    // Function used to swap language of the interface
    HomeController.prototype.swapLanguage = function (sCountryCode) {
        this.m_oTranslate.use(sCountryCode);
    }

    HomeController.$inject = [
        '$rootScope',
        '$scope',
        '$location',
        'ConstantsService',
        'AuthService',
        'RabbitStompService',
        '$state',
        '$window',
        '$anchorScroll',
        '$translate'
    ];

    return HomeController;
})();
window.HomeController = HomeController;
