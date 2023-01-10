/**
* Created by p.petrescu on 08/09/2022.
*/

'use strict';
angular.module('wasdi.AdminDashboardService', ['wasdi.AdminDashboardService']).
service('AdminDashboardService', ['$http',  'ConstantsService', function ($http, oConstantsService) {
    this.APIURL = oConstantsService.getAPIURL();
    this.m_bIgnoreWorkspaceApiUrl = oConstantsService.getIgnoreWorkspaceApiUrl();
    this.m_oHttp = $http;

    this.findUsersByPartialName = function(sPartialName) {
        return this.m_oHttp.get(this.APIURL + '/admin/usersByPartialName?partialName=' + sPartialName);
    };

    this.findWorkspacesByPartialName = function(sPartialName) {
        return this.m_oHttp.get(this.APIURL + '/admin/workspacesByPartialName?partialName=' + sPartialName);
    };

    this.findProcessorsByPartialName = function(sPartialName) {
        return this.m_oHttp.get(this.APIURL + '/admin/processorsByPartialName?partialName=' + sPartialName);
    };

    this.findResourcePermissions = function(sResourceType, sResourceId, sUserId) {
        let sUrl = this.APIURL + '/admin/resourcePermissions?';

        if (utilsIsStrNullOrEmpty(sResourceType) === false) {
            sUrl += '&resourceType=' + sResourceType;
        }

        if (utilsIsStrNullOrEmpty(sResourceId) === false) {
            sUrl += '&resourceId=' + sResourceId;
        }

        if (utilsIsStrNullOrEmpty(sUserId) === false) {
            sUrl += '&userId=' + sUserId;
        }

        return this.m_oHttp.get(sUrl);
    };

    this.addResourcePermission = function(sResourceType, sResourceId, sUserId) {
        return this.m_oHttp.post(this.APIURL + '/admin/resourcePermissions?resourceType=' + sResourceType + "&resourceId="+ sResourceId + "&userId=" + sUserId);
    };

    this.removeResourcePermission = function(sResourceType, sResourceId, sUserId) {
        return this.m_oHttp.delete(this.APIURL + '/admin/resourcePermissions?resourceType=' + sResourceType + "&resourceId="+ sResourceId + "&userId=" + sUserId);
    };

    this.getLatestMetricsEntryByNode = function(sNodeCode) {
        return this.m_oHttp.get(this.APIURL + '/admin/metrics/latest?nodeCode=' + sNodeCode);
    };

}]);
