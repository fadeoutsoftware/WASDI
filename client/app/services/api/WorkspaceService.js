/**
* Created by p.campanella on 25/10/2016.
*/

'use strict';
angular.module('wasdi.WorkspaceService', ['wasdi.WorkspaceService']).
service('WorkspaceService', ['$http',  'ConstantsService', function ($http, oConstantsService) {
    this.APIURL = oConstantsService.getAPIURL();
    this.m_bIgnoreWorkspaceApiUrl = oConstantsService.getIgnoreWorkspaceApiUrl();
    this.m_oHttp = $http;

    this.getWorkspacesInfoListByUser = function() {
        return this.m_oHttp.get(this.APIURL + '/ws/byuser');
    };

    this.getWorkspaceEditorViewModel = function (sWorkspaceId) {
        return this.m_oHttp.get(this.APIURL + '/ws/getws?workspace='+sWorkspaceId);
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

        if (oWorkspace != null) {
            if (oWorkspace.apiUrl != null && !this.m_bIgnoreWorkspaceApiUrl) {
                sUrl = oWorkspace.apiUrl;
            }

            return this.m_oHttp.delete(sUrl + '/ws/delete?workspace=' + oWorkspace.workspaceId + '&deletefile=' + bDeleteFile + '&deletelayer=' + bDeleteLayer);
        }

        return null;
    };

    this.putShareWorkspace = function(sWorkspaceId,sUserId){
        return this.m_oHttp.put(this.APIURL + '/ws/share/add?workspace=' + sWorkspaceId + "&userId=" + sUserId);
    };

    this.getUsersBySharedWorkspace = function(sWorkspaceId){
        return this.m_oHttp.get(this.APIURL + '/ws/share/byworkspace?workspace=' + sWorkspaceId );
    };

    this.deleteUserSharedWorkspace = function(sWorkspaceId,sUserId){
        return this.m_oHttp.delete(this.APIURL + '/ws/share/delete?workspace=' + sWorkspaceId + "&userId=" + sUserId );
    };

}]);
