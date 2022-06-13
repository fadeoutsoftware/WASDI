/**
 * Created by p.campanella on 21/10/2016.
 */

var ValidateUserController  = (function() {
    function ValidateUserController($scope, $location, oConstantsService, oAuthService, oState, oTimeout, oTranslate) {
        this.m_oScope = $scope;
        this.m_oLocation  = $location;
        this.m_oConstantsService = oConstantsService;
        this.m_oState = oState;
        this.m_oAuthService = oAuthService;
        this.m_sMessage = "Waiting...";
        this.m_oTimeout = oTimeout;
        this.m_oTranslate = oTranslate;
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

        var sOkMsg = this.m_oTranslate.instant("MSG_USER_VALIDATED");
        var sKoMsg = this.m_oTranslate.instant("MSG_USER_NOT_VALIDATED");
        var sRedirectMsg = this.m_oTranslate.instant("MSG_USER_REDIRECT");
        var sErrorMsg = this.m_oTranslate.instant("MSG_USER_VALIDATE_ERROR");


        this.m_oAuthService.validateUser(sEmail,sValidationCode).then(
        function (data,status) {
            if(utilsIsObjectNullOrUndefined(data.data) === false)
            {
                if(data.data.boolValue === true)
                {
                    oController.m_sMessage = sOkMsg;
                }
                else
                {
                    oController.m_sMessage = sKoMsg;
                }
                oController.m_sMessage = oController.m_sMessage + sRedirectMsg;
                oController.timeoutRedirect();
            }
        },(function (data,status){
            utilsVexDialogAlertTop(sErrorMsg);
            oController.timeoutRedirect();

        }));
    };

    ValidateUserController.$inject = [
        '$scope',
        '$location',
        'ConstantsService',
        'AuthService',
        '$state',
        '$timeout',
        "$translate"
    ];

    return ValidateUserController;
})();
window.ValidateUserController = ValidateUserController;
