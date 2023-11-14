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
      * Create a Console for the actual user in the specified workspace
      * @param sWorkspaceId Id of the workspace
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
     
     /**
      * Verify if there is a console (JupyterLab) for this user in this workspace
      * @param {String} sWorkspaceId Id of the workspace
      * @returns http promise
      */
     this.isConsoleReady = function(sWorkspaceId) {
        var oWorkspace = this.m_oConstantsService.getActiveWorkspace();
        var sUrl = this.APIURL;

        if (oWorkspace != null && oWorkspace.apiUrl != null && !this.m_bIgnoreWorkspaceApiUrl) {
            sUrl = oWorkspace.apiUrl;
        }

         return this.m_oHttp.get(sUrl + this.m_sResource + '/ready?workspaceId=' + sWorkspaceId);
     }

 }]);
 