/**
 * Created by s.adamo on 09/02/2017.
 */


'use strict';
angular.module('wasdi.ProcessingService', ['wasdi.ProcessingService']).
service('ProcessingService', ['$http', 'ConstantsService', function ($http, oConstantsService) {
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;
    this.m_oController = this;
    this.m_oConstantService = oConstantsService;
    // header field for post calls
    this.m_oOptions = {
        transformRequest: angular.identity,
        headers: { 'Content-Type': undefined }
    };

    this.geometricMosaic = function (sWorkspaceId, sDestinationProductName, oMosaic) {
        return this.m_oHttp.post(this.APIURL + '/processing/mosaic?workspace=' + sWorkspaceId
            + "&name=" + sDestinationProductName, oMosaic);
    }
}]);

