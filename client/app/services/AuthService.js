/**
 * Created by p.campanella on 21/10/2016.
 */

'use strict';
angular.module('wasdi.AuthService', ['wasdi.ConstantsService']).
service('AuthService', ['$http',  '$state', 'ConstantsService', function ($http, $state, oConstantsService) {
    this.m_oConstantsService = oConstantsService;
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;

    this.login = function(oCredentials) {
        return this.m_oHttp.post(this.APIURL + '/auth/login',oCredentials);
        //return this.m_oHttp.post('http://localhost:8080/wasdiwebserver/rest//auth/login',oCredentials);
    }

    /**
     * logout
     */
    var _this = this;
    this.logout = function() {
        //CLEAN COOKIE
        return this.m_oHttp.get(this.APIURL + '/auth/logout')
    }

    /**
     * signingUser
     * @param oUser
     */
    this.signingUser = function(oUser){
        return this.m_oHttp.post(this.APIURL + '/auth/register',oUser);
    }

    this.checkSession = function(sSession)
    {
        return this.m_oHttp.get(this.APIURL + '/auth/checksession');
    }

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
    //
    // this.ingestFile = function(sSelectedFile,sWorkspace){
    //     return this.m_oHttp.put(this.APIURL + '/auth/upload/ingest?file=' + sSelectedFile + '&workspace=' + sWorkspace);
    // };
    this.recoverPassword = function(sEmail)
    {
        return this.m_oHttp.get(this.APIURL + '/auth/lostPassword?userId=' + sEmail );
    }

}]);
