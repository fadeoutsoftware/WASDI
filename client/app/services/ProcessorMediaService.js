/**
 *
 */

'use strict';
angular.module('wasdi.ProcessorMediaService', ['wasdi.ProcessorMediaService']).
service('ProcessorMediaService', ['ConstantsService','$rootScope','$http', function (oConstantsService,$rootScope,$http) {
    this.m_oConstantsService = oConstantsService;
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;
    this.m_sResource = "/processormedia";


    /**
     * Get the list of Application Categories
     * @returns {*}
     */
    this.getCategories = function() {
        return this.m_oHttp.get(this.APIURL + this.m_sResource + '/categories/get');
    };

    /**
     * Get the list of publisher for filtering
     * @returns {*}
     */
    this.getPublishersFilterList = function() {
        return this.m_oHttp.get(this.APIURL + this.m_sResource + '/publisher/getlist');
    };


}]);
