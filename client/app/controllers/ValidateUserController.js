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
            console.log("test");
            oController.m_oState.go("home");// go workspaces
        }, 5000 );
    }

    ValidateUserController.prototype.validateUser = function(sEmail,sValidationCode)
    {
        var oController = this;
        this.m_oAuthService.validateUser(sEmail,sValidationCode).success(
        function (data,status) {
            if(utilsIsObjectNullOrUndefined(data) === false)
            {
                if(data.boolValue === true)
                {
                    oController.m_sMessage = "The user is validate.";
                }
                else
                {
                    oController.m_sMessage = "Server error is impossible validate the user or something goes wrong.";
                }
                oController.m_sMessage = oController.m_sMessage + " Automatically redirect...";
                oController.timeoutRedirect();
            }
        }).error(function (data,status){
            utilsVexDialogAlertTop("GURU MEDITATION<br>VALIDATE USER");
            oController.timeoutRedirect();

        });
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
