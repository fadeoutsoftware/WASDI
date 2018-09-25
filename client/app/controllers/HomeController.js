/**
 * Created by p.campanella on 21/10/2016.
 */

var HomeController = (function() {
    function HomeController($scope, $location, oConstantsService, oAuthService, oRabbitStompService,oState,oAuthServiceFacebook,
                            oAuthServiceGoogle) {
        this.m_oScope = $scope;
        this.m_oLocation  = $location;
        this.m_oConstantsService = oConstantsService;
        this.m_oAuthService = oAuthService;
        this.m_oRabbitStompService = oRabbitStompService;
        this.m_oState = oState;
        this.m_oAuthServiceFacebook = oAuthServiceFacebook;
        this.m_oAuthServiceGoogle = oAuthServiceGoogle;

        this.m_oScope.m_oController=this;
        this.m_bLoginIsVisible = false;//Login in visible after click on logo
        this.m_sUserName = "";
        this.m_sUserPassword = "";
        this.m_bSuccess = false;
        this.m_bError = false;
        this.m_oRegistrationUser={
            name:"",
            surname:"",
            password:"",
            repeatPassword:"",
            email:"",
            userId:""//userId == email
        }
        this.m_bRegisterIsVisible = false;
        this.m_bBrowserIsIE = utilsUserUseIEBrowser();
        this.m_bVisualizeLink = false;

        if(this.m_oConstantsService.isUserLogged())
            this.m_oState.go("root.workspaces");// go workspaces
        if(this.m_bBrowserIsIE === true)
        {
            this.m_bVisualizeLink = false;
            alert("Wasdi doesn't work on IE/EDGE");// + this.m_bVisualizeLink
        }
        else
        {
            this.m_bVisualizeLink = true;
        }
        var oController = this;
        $scope.$on('event:google-plus-signin-success', function (event,authResult) {

            // Send login to server
            if(utilsIsObjectNullOrUndefined(authResult) === false)
            {
                var oIdToken = {
                    userId: authResult.client_id,
                    googleIdToken : authResult.id_token
                }
                if(utilsIsStrNullOrEmpty(oIdToken.googleIdToken) === false && utilsIsStrNullOrEmpty(oIdToken.userId) === false )
                {
                    oController.m_oAuthServiceGoogle.loginGoogleUser(oIdToken).success(
                        function (data,status)
                        {
                            oController.callbackLogin(data, status,oController)
                        }).error(function (data,status) {
                        //alert('error');
                        utilsVexDialogAlertTop("GURU MEDITATION<br>LOGIN ERROR");

                    });
                }

            }

        });
        $scope.$on('event:google-plus-signin-failure', function (event,authResult) {
            // Auth failure or signout detected
            console.log("event:google-plus-signin-failure");
        });
    }

    HomeController.prototype.changeVisibilityLoginRegister = function(sStatus){

        if(sStatus === "Login")
        {
            this.m_bLoginIsVisible = true;
            this.m_bRegisterIsVisible = false;
            this.cleanSignInForm();

        }

        if(sStatus === "Register")
        {
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
            function (data,status) {
                oController.callbackLogin(data, status,oController)
            }).error(function (data,status) {
            //alert('error');
            utilsVexDialogAlertTop("GURU MEDITATION<br>LOGIN ERROR");

        });
    }

    HomeController.prototype.callbackLogin = function(data, status,oController)
    {
        if (data != null)
        {
            if (data != undefined)
            {
                if (data.userId != null && data.userId != "")
                {
                    //LOGIN OK
                    if(!utilsIsObjectNullOrUndefined(data.sessionId)|| !utilsIsStrNullOrEmpty(data.sessionId))
                    {
                        oController.m_oConstantsService.setUser(data);//set user
                        oController.m_oState.go("root.workspaces");// go workspaces
                    }
                }
                else
                {
                    //LOGIN FAIL
                    utilsVexDialogAlertTop( "GURU MEDITATION<br>WRONG CREDENTIALS, TRY AGAIN");

                }
            }
        }
    }

    HomeController.prototype.getUserName = function () {
        var oUser = this.m_oConstantsService.getUser();

        if (oUser != null)
        {
            if (oUser != undefined)
            {
                var sName = oUser.name;
                if (sName == null) sName = "";
                if (sName == undefined) sName = "";

                if (oUser.surname != null)
                {
                    if (oUser.surname != undefined)
                    {
                        sName += " " + oUser.surname;

                        return sName;
                    }
                }
            }
        }

        return "";
    }

    /**
     * signingUser
     * @returns {boolean}
     */
    HomeController.prototype.signingUser = function(myForm)
    {

        var oUser = {};
        var oController = this;

        if( (utilsIsObjectNullOrUndefined(oController.m_oRegistrationUser) === true) || (this.isValidSigningUser(oController.m_oRegistrationUser) === false) )
        {
            return false;
        }
        oUser.userId = oController.m_oRegistrationUser.userId;
        oUser.password = oController.m_oRegistrationUser.password;
        oUser.name = oController.m_oRegistrationUser.name;
        oUser.surname = oController.m_oRegistrationUser.surname;

        this.m_bSuccess = false;
        this.m_bError = false;
        this.m_oAuthService.signingUser(oUser).success(
            function (data,status) {
                // oController.callbackLogin(data, status,oController);
                if(utilsIsObjectNullOrUndefined(data) !== true)
                {
                    if(data.boolValue === true)
                    {
                        oController.m_bSuccess = true;
                    }
                    else
                    {
                        oController.m_bError = true;
                    }
                }
                else
                {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>SIGNIN ERROR");
                }
            }).error(function (data,status) {
            //alert('error');
            utilsVexDialogAlertTop("GURU MEDITATION<br>SIGNIN ERROR");

        });

        return true;
    }

    HomeController.prototype.isValidSigningUser = function(oRegistrationUser)
    {
        if(utilsIsObjectNullOrUndefined(oRegistrationUser) === true )
        {
            return false;
        }

        if( (utilsIsStrNullOrEmpty(oRegistrationUser.userId) === true) || (utilsIsEmail(oRegistrationUser.userId) === false) ) //
        {
            return false;
        }

        if( (utilsIsStrNullOrEmpty(oRegistrationUser.password) === true) || (oRegistrationUser.password.length < 8) ||
            (utilsIsStrNullOrEmpty(oRegistrationUser.repeatPassword) === true) || (oRegistrationUser.password !== oRegistrationUser.repeatPassword) )
        {
            return false;
        }

        if( utilsIsStrNullOrEmpty(oRegistrationUser.name) === true )
        {
            return false;
        }

        if( utilsIsStrNullOrEmpty(oRegistrationUser.surname) === true )
        {
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

    HomeController.prototype.cleanSignInForm = function()
    {
        this.m_bSuccess = false;
        this.m_bError = false;
        this.m_oScope.signinForm.$setPristine();
        this.m_oRegistrationUser={
            name:"",
            surname:"",
            password:"",
            repeatPassword:"",
            email:"",
            userId:""//userId == email
        }
    }
    // HomeController.prototype.onSignIn = function (googleUser) {
    //     var profile = googleUser.getBasicProfile();
    //     console.log('ID: ' + profile.getId()); // Do not send to your backend! Use an ID token instead.
    //     console.log('Name: ' + profile.getName());
    //     console.log('Image URL: ' + profile.getImageUrl());
    //     console.log('Email: ' + profile.getEmail()); // This is null if the 'email' scope is not present.
    // };


    HomeController.$inject = [
        '$scope',
        '$location',
        'ConstantsService',
        'AuthService',
        'RabbitStompService',
        '$state',
        'AuthServiceFacebook',
        'AuthServiceGoogle'
    ];

    return HomeController;
}) ();
