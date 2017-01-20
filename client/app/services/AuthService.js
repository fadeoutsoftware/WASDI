/**
 * Created by p.campanella on 21/10/2016.
 */

'use strict';
angular.module('wasdi.AuthService', ['wasdi.ConstantsService']).
service('AuthService', ['$http',  'ConstantsService', function ($http, oConstantsService) {
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;

    this.login = function(oCredentials) {
        return this.m_oHttp.post(this.APIURL + '/auth/login',oCredentials);
    }

    this.logout = function() {
        //CLEAN COOKIE
        return this.m_oHttp.get(this.APIURL + '/auth/logout');
    }

    this.checkSession = function(sSession)
    {
        return this.m_oHttp.get(this.APIURL + '/auth/checksession');
    }

}]);