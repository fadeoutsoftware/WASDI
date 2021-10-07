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

    this.Operation = function (sOperation, sSourceProductName, sDestinationProductName, sWorkspaceId, oOptionsInput) {
        var sUrl = this.APIURL + '/processing/{sOperation}?sSourceProductName=' + sSourceProductName + '&sDestinationProductName=' + sDestinationProductName + '&sWorkspaceId=' + sWorkspaceId;
        var oConfig = { header: "" };
        sUrl = sUrl.replace("{sOperation}", sOperation);
        return this.m_oHttp.post(sUrl, oOptionsInput, oConfig);
    };

    this.getWPSList = function () {
        return this.m_oHttp.get(this.APIURL + '/processing/WPSlist');
    };

    this.geometricMosaic = function (sWorkspaceId, sDestinationProductName, oMosaic) {
        return this.m_oHttp.post(this.APIURL + '/processing/geometric/mosaic?sWorkspaceId=' + sWorkspaceId
            + "&sDestinationProductName=" + sDestinationProductName, oMosaic);
    }
}]);

