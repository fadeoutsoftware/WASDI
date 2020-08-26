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

    /**
     * Upload or Update Processor logo
     * @param sWorkspaceId
     * @param sProcessorId
     * @param oBody
     * @returns {*}
     */
    this.uploadProcessorLogo = function (sProcessorId, oBody) {

        var oOptions = {
            transformRequest: angular.identity,
            headers: {'Content-Type': undefined}
        };

        return this.m_oHttp.post(this.APIURL + this.m_sResource + '/logo/upload?processorId=' + encodeURI(sProcessorId), oBody ,oOptions);
    };

    /**
     * Upload Processor Image
     * @param sProcessorId
     * @param oBody
     * @returns {*}
     */
    this.uploadProcessorImage = function (sProcessorId, oBody) {

        var oOptions = {
            transformRequest: angular.identity,
            headers: {'Content-Type': undefined}
        };

        return this.m_oHttp.post(this.APIURL + this.m_sResource + '/images/upload?processorId=' + encodeURI(sProcessorId), oBody ,oOptions);
    };


    this.removeProcessorImage = function (sProcessorId, sImage) {
        return this.m_oHttp.delete(this.APIURL + this.m_sResource + '/images/delete?processorId=' + encodeURI(sProcessorId) + "&imageName=" + sImage);
    };

}]);
