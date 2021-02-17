/**
 * Created by m.menapace on 11/2/2021.
 */

'use strict';
angular.module('wasdi.NodeService', ['wasdi.NodeService']).
service('NodeService', ['$http',  'ConstantsService', function ($http, oConstantsService) {
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;

    this.getNodesList = function () {
        return this.m_oHttp.get(this.APIURL + '/node/allnodes');
    };
}]);
