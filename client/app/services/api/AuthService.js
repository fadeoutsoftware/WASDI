/**
 * Created by p.campanella on 21/10/2016.
 */

'use strict';
angular.module('wasdi.AuthService', ['wasdi.ConstantsService']).
service('AuthService', ['$http',  '$state', 'ConstantsService', function ($http, $state, oConstantsService) {
    this.m_oConstantsService = oConstantsService;
    this.APIURL = oConstantsService.getAPIURL();
    this.AUTHURL = oConstantsService.getAUTHURL();
    this.m_oHttp = $http;

    var acSessionChangedEvent = 'ac-session-changed';

    /**
     * acSession data
     * - authenticated flag
     * - user data
     * - available hats
     * - current hat
     */
    //TODO check
    var acSession = {
        authenticated: false,
        user: {},
        hat: {}
    }


    var m_sAuthClientId = 'wasdi_client'

    //todo fix endpoints
    var keycloakConfiguration = {
        //'token_endpoint': window.app.url.oidcIssuer + "protocol/openid-connect/token/",
        'token_endpoint': oConstantsService.getAUTHURL() + "/protocol/openid-connect/token",
        //'end_session_endpoint': window.app.url.oidcIssuer + "protocol/openid-connect/logout/"
        'end_session_endpoint': oConstantsService.getAUTHURL() + "/protocol/openid-connect/logout"
    }

    //TODO check
    function raiseSessionChanged() {
        $rootScope.acSession = acSession
        $rootScope.$broadcast(acSessionChangedEvent, acSession)
    }

    //TODO check
    function resetSession() {
        acSession = {
            authenticated: false,
            user: {},
            hat: {}
        };
        raiseSessionChanged();
    };

    //TODO check
    function clearToken() {
        // delete window.localStorage.access_token
        // delete window.localStorage.refresh_token
        resetSession()
    }

    function getTokenObj() {
        if (window.localStorage.access_token && window.localStorage.refresh_token)
            return {'access_token': window.localStorage.access_token, 'refresh_token':window.localStorage.refresh_token}
        delete window.localStorage.access_token
        delete window.localStorage.refresh_token
        return null;
    }

    function saveToken(token) {
        window.localStorage.access_token = token['access_token']
        window.localStorage.refresh_token = token['refresh_token']
    }

    this.login = function(oCredentials) {

        let sParams = 'client_id=' + m_sAuthClientId + '&grant_type=password&username=' + oCredentials.userId + '&password=' + oCredentials.userPassword
        let sAddress = keycloakConfiguration['token_endpoint'];
        console.log(sAddress)
        return $http.post(sAddress,
            sParams,
            {'headers': {'Content-Type': 'application/x-www-form-urlencoded'}}
        );


        // return this.m_oHttp.post(this.AUTHURL + '/auth/login', oCredentials);
        //return this.m_oHttp.post('http://localhost:8080/wasdiwebserver/rest//auth/login',oCredentials);
    };

    this.legacyLogin = function(oCredentials) {
        return this.m_oHttp.post(this.APIURL + '/auth/login', oCredentials);
    };


    /**
     * logout
     */

    this.logout = function() {
        //CLEAN COOKIE
        return this.m_oHttp.get(this.APIURL + '/auth/logout')
    };

    /**
     * signingUser
     * @param oUser
     */
    this.signingUser = function(oUser){
        return this.m_oHttp.post(this.APIURL + '/auth/register',oUser);
    };

    /**
     * Create sftp account on node
     * @param sEmailInput
     * @returns {*}
     */
    this.createAccountUpload = function(sEmailInput)
    {
        var oWorkspace = this.m_oConstantsService.getActiveWorkspace();
        var sUrl = this.APIURL;
        if (oWorkspace != null) {
            if (oWorkspace.apiUrl != null) {
                sUrl = oWorkspace.apiUrl;
            }
        }

        return this.m_oHttp.post(sUrl + '/auth/upload/createaccount', sEmailInput);//JSON.stringify({"sEmail":sEmailInput})
    };

    /**
     * Delete sftp account
     * @param sIdInput
     * @returns {null|*}
     */
    this.deleteAccountUpload = function(sIdInput)
    {
        if(utilsIsObjectNullOrUndefined(sIdInput))
            return null;

        var oWorkspace = this.m_oConstantsService.getActiveWorkspace();
        var sUrl = this.APIURL;
        if (oWorkspace != null) {
            if (oWorkspace.apiUrl != null) {
                sUrl = oWorkspace.apiUrl;
            }
        }

        return this.m_oHttp.delete(sUrl + '/auth/upload/removeaccount',sIdInput);
    };

    /**
     * Update SFTP Account Password
     * @param sEmailInput
     * @returns {null|*}
     */
    this.updatePasswordUpload = function(sEmailInput)
    {
        if(utilsIsEmail(sEmailInput) === false)
            return null;

        var oWorkspace = this.m_oConstantsService.getActiveWorkspace();
        var sUrl = this.APIURL;
        if (oWorkspace != null) {
            if (oWorkspace.apiUrl != null) {
                sUrl = oWorkspace.apiUrl;
            }
        }

        return this.m_oHttp.post(sUrl + '/auth/upload/updatepassword',sEmailInput);
    };

    /**
     * Test if the sftp account exists
     * @returns {*}
     */
    this.isCreatedAccountUpload = function()
    {
        var oWorkspace = this.m_oConstantsService.getActiveWorkspace();
        var sUrl = this.APIURL;
        if (oWorkspace != null) {
            if (oWorkspace.apiUrl != null) {
                sUrl = oWorkspace.apiUrl;
            }
        }

        return this.m_oHttp.get(sUrl + '/auth/upload/existsaccount');
    };

    /**
     * Get the list of sftp files in the node
     * @returns {*}
     */
    this.getListFilesUpload = function()
    {
        var oWorkspace = this.m_oConstantsService.getActiveWorkspace();
        var sUrl = this.APIURL;
        if (oWorkspace != null) {
            if (oWorkspace.apiUrl != null) {
                sUrl = oWorkspace.apiUrl;
            }
        }

        return this.m_oHttp.get(sUrl + '/auth/upload/list');
    };

    this.changePassword = function(oPasswords)
    {
        return this.m_oHttp.post(this.APIURL + '/auth/changePassword',oPasswords);
    };

    this.changeUserInfo = function(oUserInfo)
    {
        return this.m_oHttp.post(this.APIURL + '/auth/editUserDetails',oUserInfo);
    };

    this.validateUser = function(sEmail,sValidationCode)
    {
        return this.m_oHttp.get(this.APIURL + '/auth/validateNewUser?email=' + sEmail + '&validationCode=' + sValidationCode);
    }
    
    this.recoverPassword = function(sEmail)
    {
        return this.m_oHttp.get(this.APIURL + '/auth/lostPassword?userId=' + sEmail );
    }

}]);
