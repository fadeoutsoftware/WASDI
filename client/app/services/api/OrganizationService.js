/**
 * Created by p.petrescu on 17/01/2023.
 */

'use strict';
angular.module('wasdi.OrganizationService', ['wasdi.OrganizationService']).
service('OrganizationService', ['$http',  'ConstantsService', function ($http, oConstantsService) {
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;

    this.getOrganizationsListByUser = function () {
        return this.m_oHttp.get(this.APIURL + '/organizations/byuser');
    };

    this.getOrganizationById = function (sOrganizationId) {
        return this.m_oHttp.get(this.APIURL + '/organizations/byId?organization=' + sOrganizationId);
    };

    this.saveOrganization = function (oOrganization) {
        if (utilsIsStrNullOrEmpty(oOrganization.organizationId)) {
            return this.createOrganization(oOrganization);
        } else {
            return this.updateOrganization(oOrganization);
        }
    };

    this.createOrganization = function (oOrganization) {
        return this.m_oHttp.post(this.APIURL + '/organizations/add', oOrganization);
    };

    this.updateOrganization = function (oOrganization) {
        return this.m_oHttp.put(this.APIURL + '/organizations/update', oOrganization);
    };

    this.deleteOrganization = function (sOrganizationId) {

        return this.m_oHttp.delete(this.APIURL + '/organizations/delete?organization=' + sOrganizationId);
    };

    // Get list of shared users by organization id
    this.getUsersBySharedOrganization = function (sOrganizationId) {
        return this.m_oHttp.get(this.APIURL + '/organizations/share/byorganization?organization=' + sOrganizationId);
    }

    // Add sharing
    this.addOrganizationSharing = function (sOrganizationId, sUserId) {
        return this.m_oHttp.post(this.APIURL + '/organizations/share/add?organization=' + sOrganizationId + '&userId=' + sUserId);
    }

    // Remove sharing
    this.removeOrganizationSharing = function (sOrganizationId, sUserId) {
        return this.m_oHttp.delete(this.APIURL + '/organizations/share/delete?organization=' + sOrganizationId + '&userId=' + sUserId);

    }

}]);
