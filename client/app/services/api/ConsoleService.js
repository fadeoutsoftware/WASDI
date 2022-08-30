/**
 * Created by p.campanella on 30/08/2022
 */

 'use strict';
 angular.module('wasdi.ConsoleService', ['wasdi.ConsoleService']).
 service('ConsoleService', ['ConstantsService','$rootScope','$http', function (oConstantsService,$rootScope,$http) {
     this.m_oConstantsService = oConstantsService;
     this.APIURL = oConstantsService.getAPIURL();
     this.m_bIgnoreWorkspaceApiUrl = oConstantsService.getIgnoreWorkspaceApiUrl();
     this.m_oHttp = $http;
     this.m_sResource = "/console";
 
     /**
      * Create a Console
      * @param sWorkspaceId
      * @returns {*}
      */
     this.createConsole = function(sWorkspaceId) {

        var oWorkspace = this.m_oConstantsService.getActiveWorkspace();
        var sUrl = this.APIURL;

        if (oWorkspace != null && oWorkspace.apiUrl != null && !this.m_bIgnoreWorkspaceApiUrl) {
            sUrl = oWorkspace.apiUrl;
        }

         return this.m_oHttp.post(sUrl + this.m_sResource + '/create?workspaceId=' + sWorkspaceId);
     }; 

 }]);
 