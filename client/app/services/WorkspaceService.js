/**
* Created by p.campanella on 25/10/2016.
*/

'use strict';
angular.module('wasdi.WorkspaceService', ['wasdi.WorkspaceService']).
service('WorkspaceService', ['$http',  'ConstantsService', function ($http, oConstantsService) {
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;

    this.getWorkspacesInfoListByUser = function() {
        return this.m_oHttp.get(this.APIURL + '/ws/byuser');
    };

    this.getWorkspaceEditorViewModel = function (sWorkspaceId) {
        return this.m_oHttp.get(this.APIURL + '/ws?sWorkspaceId='+sWorkspaceId);
    };



    this.createWorkspace = function (sName = null) {

        let sRestPath = '/ws/create';

        if (sName != null) {
            sRestPath = sRestPath + "?name=" + sName;
        }

        return this.m_oHttp.get(this.APIURL + sRestPath);
    };

    this.UpdateWorkspace = function (oWorkspace) {
        return this.m_oHttp.post(this.APIURL + '/ws/update', oWorkspace);
    };

    this.DeleteWorkspace = function (oWorkspace, bDeleteFile, bDeleteLayer) {

        var sUrl = this.APIURL;

        if (oWorkspace.apiUrl != null) sUrl = oWorkspace.apiUrl;

        return this.m_oHttp.delete(sUrl + '/ws/delete?sWorkspaceId=' + oWorkspace.workspaceId + '&bDeleteFile=' + bDeleteFile + '&bDeleteLayer=' + bDeleteLayer);
    };

    this.getWorkspaceListByProductName = function(sProductName){
        return this.m_oHttp.get(this.APIURL + '/ws/workspacelistbyproductname?productname=' + sProductName);
    };

    this.putShareWorkspace = function(sWorkspaceId,sUserId){
        return this.m_oHttp.put(this.APIURL + '/ws/share?sWorkspaceId=' + sWorkspaceId + "&sUserId=" + sUserId);
    };

    this.getUsersBySharedWorkspace = function(sWorkspaceId){
        return this.m_oHttp.get(this.APIURL + '/ws/enableusersworkspace?sWorkspaceId=' + sWorkspaceId );
    };
    this.deleteUserSharedWorkspace = function(sWorkspaceId,sUserId){
        return this.m_oHttp.delete(this.APIURL + '/ws/deleteworkspacesharing?sWorkspaceId=' + sWorkspaceId + "&sUserId=" + sUserId );
    };



}]);
