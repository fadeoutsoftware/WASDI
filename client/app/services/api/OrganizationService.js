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

}]);
