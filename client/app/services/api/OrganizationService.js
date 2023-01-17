/**
 * Created by p.petrescu on 17/01/2023.
 */

'use strict';
angular.module('wasdi.OrganizationService', ['wasdi.OrganizationService']).
service('OrganizationService', ['$http',  'ConstantsService', function ($http, oConstantsService) {
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;

    this.getOrganizationListByUser = function () {
        return this.m_oHttp.get(this.APIURL + '/organizations/byuser');
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

}]);
