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
        return this.m_oHttp.get(this.APIURL + '/admin/usersByPartialName?partialName=' + sPartialName );
    };

    this.addResourcePermission = function(sResourceType, sResourceId, sUserId) {
        return this.m_oHttp.post(this.APIURL + '/admin/resourcePermission?resourceType=' + sResourceType + "&resourceId="+ sResourceId + "&userId=" + sUserId);
    };

    this.removeResourcePermission = function(sResourceType, sResourceId, sUserId) {
        return this.m_oHttp.delete(this.APIURL + '/admin/resourcePermission?resourceType=' + sResourceType + "&resourceId="+ sResourceId + "&userId=" + sUserId);
    };

}]);
