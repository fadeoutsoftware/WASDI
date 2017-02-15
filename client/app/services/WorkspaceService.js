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
    }

    this.getWorkspaceEditorViewModel = function (sWorkspaceId) {
        return this.m_oHttp.get(this.APIURL + '/ws?sWorkspaceId='+sWorkspaceId);
    }

    this.createWorkspace = function () {
        return this.m_oHttp.get(this.APIURL + '/ws/create');
    }

    this.UpdateWorkspace = function (oWorkspace) {
        return this.m_oHttp.post(this.APIURL + '/ws/update', oWorkspace);
    }

    this.DeleteWorkspace = function (sWorkspaceId, bDeleteFile, bDeleteLayer) {
        return this.m_oHttp.delete(this.APIURL + '/ws/delete?sWorkspaceId=' + sWorkspaceId + '&bDeleteFile=' + bDeleteFile + '&bDeleteLayer=' + bDeleteLayer);
    }

}]);
