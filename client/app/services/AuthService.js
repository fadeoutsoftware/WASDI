/**
 * Created by p.campanella on 21/10/2016.
 */

'use strict';
angular.module('wasdi.AuthService', ['wasdi.ConstantsService']).
service('AuthService', ['$http',  '$state', 'ConstantsService', function ($http, $state, oConstantsService) {
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


    //todo replace with appropriate client ID
    var m_sAuthClientId = 'wasdi-client'

    //todo fix endpoints
    var keycloackConfiguration = {
        //'token_endpoint': window.app.url.oidcIssuer + "protocol/openid-connect/token/",
        'token_endpoint': oConstantsService.getAUTHURL() + "auth/realms/wasdi/protocol/openid-connect/token",
        //'end_session_endpoint': window.app.url.oidcIssuer + "protocol/openid-connect/logout/"
        'end_session_endpoint': oConstantsService.getAUTHURL() + "protocol/openid-connect/logout/"
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

    function getUser() {
        var token = getTokenObj()

        if (token) {

            var data = jwt_decode(token['access_token']);

            hats = []
            backendApplicationName = applicationName + '_api'
            if (backendApplicationName in data['resource_access']) {
                var roles = data['resource_access'][backendApplicationName]['roles'];
                console.log(roles)
                roles.forEach(function (role) {
                    hats.push({
                        'id': role,
                        'descr': role,
                        //todo check: is this relevant to WASDI?
                        'domain': { //TODO: define role domain (realm setting?)
                            lone: 6,
                            lonw: 15,
                            lats: 32,
                            latn: 50
                        }
                    })
                })
            }
            acSession.user = {
                'id': data['preferred_username'],
                'name': data['name'],
                'email': data['email'],
                'avatar': '', //TODO: manage avatars (user attribute in keycloack?)
                'hats': hats,
            }

            acSession.authenticated = true

            if (acSession.user.hats.length>0) {
                if (!setLastHat()) {
                    //if not set previously used hat, set the first one
                    setHat(acSession.user.hats[0])
                }
            }
            synch_time();

            console.log(acSession.user)

        } else {
            resetSession();
        }
    }

    this.login = function(oCredentials) {
        /**
         * login
         * @param user
         * @param pass
         */
        //todo check
        // function login(user, pass) {
        //
        //     function loginError(data) {
        //         clearToken();
        //         console.log('AUTH: UNABLE TO OBTAIN TOKEN: ' + data)
        //         resetSession();
        //         raiseSessionChanged();
        //         //todo alert of error in login
        //         // if (typeof vex !== 'undefined') {
        //         //     vex.dialog.alert($translate.instant("MSG_LOGINERROR"));
        //         // } else {
        //         //     alert($translate.instant("MSG_LOGINERROR"))
        //         // }
        //     }
        //
        //     params = 'client_id=' + m_sAuthClientId + '&grant_type=password&username=' + user + '&password=' + pass
        //     $http.post(keycloackConfiguration['token_endpoint'],
        //         params,
        //         {'headers': {'Content-Type': 'application/x-www-form-urlencoded'}}
        //     ).then(
        //         function (data, status, headers, config) {
        //             if (data.status < 200 || data.status >= 300) {
        //                 loginError(data)
        //             } else {
        //                 data = data.data
        //                 console.log('AUTH: token obtained')
        //                 console.log(data)
        //                 // var now = new Date();
        //                 // var validitySeconds = data['expires_in'] - 30
        //                 // data['myexpires'] = new Date(now.getTime() + validitySeconds * 1000)
        //                 // console.log('AUTH: token obtained. Expires at ' + data['myexpires'])
        //                 saveToken(data)
        //                 getUser();
        //                 raiseSessionChanged();
        //             }
        //             //todo return response w/ token
        //         },
        //         loginError
        //     );
        // }

        let sParams = 'client_id=' + m_sAuthClientId + '&grant_type=password&username=' + oCredentials.userId + '&password=' + oCredentials.userPassword
        let sAddress = keycloackConfiguration['token_endpoint'];
        console.log(sAddress)
        return $http.post(sAddress,
            sParams,
            {'headers': {'Content-Type': 'application/x-www-form-urlencoded'}}
        );


        // return this.m_oHttp.post(this.AUTHURL + '/auth/login', oCredentials);
        //return this.m_oHttp.post('http://localhost:8080/wasdiwebserver/rest//auth/login',oCredentials);
    };

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

    /* USER UPLOAD AUTH API */
    this.createAccountUpload = function(sEmailInput)
    {
        // if(utilsIsEmail() === false)
        //     return null;

        return this.m_oHttp.post(this.APIURL + '/auth/upload/createaccount', sEmailInput);//JSON.stringify({"sEmail":sEmailInput})
    };

    this.deleteAccountUpload = function(sIdInput)
    {
        if(utilsIsObjectNullOrUndefined(sIdInput))
            return null;

        return this.m_oHttp.delete(this.APIURL + '/auth/upload/removeaccount',sIdInput);
    };

    this.updatePasswordUpload = function(sEmailInput)
    {
        if(utilsIsEmail(sEmailInput) === false)
            return null;

        return this.m_oHttp.post(this.APIURL + '/auth/upload/updatepassword',sEmailInput);
    };

    this.isCreatedAccountUpload = function()
    {
        return this.m_oHttp.get(this.APIURL + '/auth/upload/existsaccount');
    };

    this.getListFilesUpload = function()
    {
        return this.m_oHttp.get(this.APIURL + '/auth/upload/list');
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
