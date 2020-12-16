/**
 * Created by p.campanella on 21/10/2016.
 */
//'use strict';

var HomeController = (function () {
    function HomeController($scope, $location, oConstantsService, oAuthService, oRabbitStompService, oState, oAuthServiceFacebook,
                            //oAuthServiceGoogle,
                            oWindow, $anchorScroll) {
        this.m_oScope = $scope;
        this.m_oLocation = $location;
        this.m_oConstantsService = oConstantsService;
        this.m_oAuthService = oAuthService;
        this.m_oRabbitStompService = oRabbitStompService;
        this.m_oState = oState;
        this.m_oAuthServiceFacebook = oAuthServiceFacebook;
        //this.m_oAuthServiceGoogle = oAuthServiceGoogle;
        this.m_oAnchorService = $anchorScroll;

        this.m_sEmailToRecoverPassword = "";
        this.m_oScope.m_oController = this;
        this.m_bLoginIsVisible = false;//Login in visible after click on logo
        this.m_bIsVisibleRecoveryPassword = false;
        this.m_sUserName = "";
        this.m_sUserPassword = "";
        this.m_bSuccess = false;
        this.m_bError = false;
        this.m_sMessageError = "";
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

        // Instead of oAuthSuccess if on loading of this js, the
        // okeycloak variable is setted and authenticated, do the callbacklogin
        // and state.go -> marketplace
        if (oKeycloak.authenticated){
            if (oKeycloak.idToken) {
                data = {
                    'access_token': oKeycloak.idToken,
                    'refresh_token': oKeycloak.refresh_token
                };
            }
            oController.callbackLogin(data, null, oController);
        }


/*        if (oKeycloak.onAuthSuccess == undefined) {
            console.log("Home controller - Keycloak Adapter - ON AUTH SUCCESS UNDEFINED");
            oKeycloak.onAuthSuccess = function () {
                console.log("Home controller - Keycloak Adapter - EVENT AUTH SUCCESS TRIGGERED");
                if (oKeycloak.idToken) {
                    data = {
                        'access_token': oKeycloak.idToken,
                        'refresh_token': ""
                    };
                }
                oController.callbackLogin(data, null, oController);
            };
        }*/




        //if (this.m_oConstantsService.isUserLogged())
           // this.m_oState.go("root.marketplace");// go workspaces
        if (this.m_bBrowserIsIE === true) {
            this.m_bVisualizeLink = false;
            alert("Wasdi doesn't work on IE/EDGE");// + this.m_bVisualizeLink
        } else {
            this.m_bVisualizeLink = true;
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
        // var oConstantsService = oController.m_oConstantsService;
        this.m_oConstantsService.setUser(null);
        this.m_oAuthService.login(oLoginInfo).success(
            function (data, status) {
                oController.callbackLogin(data, status, oController)
            }).error(function (error) {
            //alert('error');
            utilsVexDialogAlertTop("GURU MEDITATION<br>LOGIN ERROR");

        });
    }


    HomeController.prototype.callbackLogin = function (data, status, oController) {

        console.log('AUTH: token obtained')
        console.log(data)

        if (!oController) oController = this;
        // var now = new Date();
        // var validitySeconds = data['expires_in'] - 30
        // data['myexpires'] = new Date(now.getTime() + validitySeconds * 1000)
        // console.log('AUTH: token obtained. Expires at ' + data['myexpires'])

        window.localStorage.access_token = data['access_token']
        window.localStorage.refresh_token = data['refresh_token']

        var oDecodedToken = jwt_decode(data['access_token']);

        console.log(oDecodedToken)

        let oUser = {}
        oUser.userId = oDecodedToken.preferred_username;
        oUser.name = oDecodedToken.given_name;
        oUser.surname = oDecodedToken.family_name;
        oUser.authProvider = "wasdi";
        oUser.link;
        oUser.description;
        oUser.sessionId = data['access_token'];
        oUser.refreshToken = data['refresh_token'];

        oController.m_oConstantsService.setUser(oUser);//set user
        oController.m_oState.go("root.marketplace");// go workspaces -> go to marketplace

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

    HomeController.prototype.keycloakLogin = function (){
    console.log("Home Controller - OKEYCLOAK login invoked");
    oKeycloak.login();
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
        this.m_oAuthService.signingUser(oUser).success(
            function (data, status) {
                if (utilsIsObjectNullOrUndefined(data) !== true) {
                    if (data.boolValue === true) {
                        oController.m_bSuccess = true;
                    } else {
                        if (utilsIsStrNullOrEmpty(data.stringValue) === false) {
                            oController.m_sMessageError = data.stringValue;
                        }
                        oController.m_bError = true;

                        utilsVexDialogAlertTop("GURU MEDITATION<br>" + oController.m_sMessageError);
                    }
                } else {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>SIGNIN ERROR");
                }

                oController.m_bRegistering = false;
            }).error(function (data, status) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>SIGNIN ERROR");
            oController.m_bRegistering = false;
        });

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
        this.m_oAuthService.recoverPassword(sEmailToRecoverPassword).success(
            function (data, status) {
                // oController.callbackLogin(data, status,oController);
                if (utilsIsObjectNullOrUndefined(data) !== true) {
                    if (data.boolValue === true) {
                        oController.m_bSuccess = true;
                    } else {
                        if (utilsIsStrNullOrEmpty(data.stringValue) === false) {
                            oController.m_sMessageError = data.stringValue;
                        }
                        oController.m_bError = true;
                    }
                } else {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>SIGNIN ERROR");
                }
            }).error(function (data, status) {
            //alert('error');
            utilsVexDialogAlertTop("GURU MEDITATION<br>SIGNIN ERROR");

        });

        return true;
    };

    HomeController.prototype.isRecoverPasswordButtonEnable = function (sEmailToRecoverPassword) {
        if (utilsIsStrNullOrEmpty(sEmailToRecoverPassword) === true || sEmailToRecoverPassword === "" || utilsIsEmail(sEmailToRecoverPassword) === false) {
            return false;
        }

        return true;
    }
    HomeController.$inject = [
        '$scope',
        '$location',
        'ConstantsService',
        'AuthService',
        'RabbitStompService',
        '$state',
        'AuthServiceFacebook',
        //       'AuthServiceGoogle',
        '$window',
        '$anchorScroll'
    ];

    return HomeController;
})();
