/**
 * Created by p.campanella on 21/10/2016.
 */

var ValidateUserController  = (function() {
    function ValidateUserController($scope, $location, oConstantsService, oAuthService, oState, oTimeout) {
        this.m_oScope = $scope;
        this.m_oLocation  = $location;
        this.m_oConstantsService = oConstantsService;
        this.m_oState = oState;
        this.m_oAuthService = oAuthService;
        this.m_sMessage = "Waiting...";
        this.m_oTimeout = oTimeout;
        this.m_oScope.m_oController = this;
        var sEmail = this.m_oLocation.$$search.email;
        var sValidationCode = this.m_oLocation.$$search.validationCode;

        if( (utilsIsStrNullOrEmpty(sEmail) === false) && (utilsIsStrNullOrEmpty(sValidationCode) === false) )
        {
            this.validateUser(sEmail,sValidationCode);
        }

    }

    ValidateUserController.prototype.timeoutRedirect = function(){
        var oController = this;
        this.m_oTimeout( function(){
            oController.m_oState.go("home");// go workspaces
        }, 2000 );
    }

    ValidateUserController.prototype.validateUser = function(sEmail,sValidationCode)
    {
        var oController = this;
        this.m_oAuthService.validateUser(sEmail,sValidationCode).then(
        function (data,status) {
            if(utilsIsObjectNullOrUndefined(data.data) === false)
            {
                if(data.data.boolValue === true)
                {
                    oController.m_sMessage = "User validated";
                }
                else
                {
                    oController.m_sMessage = "Server error: impossible to validate the user";
                }
                oController.m_sMessage = oController.m_sMessage + " Redirecting...";
                oController.timeoutRedirect();
            }
        },(function (data,status){
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR VALIDATING USER");
            oController.timeoutRedirect();

        }));
    };

    ValidateUserController.$inject = [
        '$scope',
        '$location',
        'ConstantsService',
        'AuthService',
        '$state',
        '$timeout'
    ];

    return ValidateUserController;
})();
window.ValidateUserController = ValidateUserController;
