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

}]);
