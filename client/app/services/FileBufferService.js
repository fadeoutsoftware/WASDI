/**
 * Created by p.campanella on 26/10/2016.
 */

'use strict';
angular.module('wasdi.FileBufferService', ['wasdi.ConstantsService']).
service('FileBufferService', ['$http',  'ConstantsService', function ($http, oConstantsService) {
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;


    this.download = function(sUrl, sWorkspaceId,sBounds,sProvider) {
        //sUrl="https://scihub.copernicus.eu/dhus/odata/v1/Products('ed9c834d-0d8c-47d2-8337-3036bd14d0f3')/$value";

        var iCut = 4;
        var sProtocol = 'http';

        if (sUrl.startsWith("https:")) {
            iCut = 5;
            sProtocol = 'https';
        }
        if (sUrl.startsWith("file")) {
            iCut = 4;
            sProtocol = 'file';
        }

        if (utilsIsStrNullOrEmpty(sBounds) == false) {
            if (sBounds.length > 1000) {
                sBounds =""
            }
        }

        var sTest = sUrl.substring(iCut, sUrl.length);
        var sEncodedUri = encodeURIComponent(sTest);
        sEncodedUri = sProtocol + sEncodedUri;

        return this.m_oHttp.get(this.APIURL + '/filebuffer/download?sFileUrl='+sEncodedUri+"&sWorkspaceId="+sWorkspaceId+"&sBoundingBox="+sBounds+'&sProvider='+sProvider);
    }
    
    this.publish = function(sUrl, sWorkspaceId) {
        return this.m_oHttp.get(this.APIURL + '/filebuffer/publish?sFileUrl='+encodeURIComponent(sUrl)+"&sWorkspaceId="+sWorkspaceId);
    }

    this.publishBand = function(sUrl, sWorkspaceId, sBand) {
        return this.m_oHttp.get(this.APIURL + '/filebuffer/publishband?sFileUrl='+encodeURIComponent(sUrl)+"&sWorkspaceId="+sWorkspaceId+'&sBand='+sBand);
    }

    this.getBandLayerId = function (sUrl, sWorkspaceId, sBand) {
        return this.m_oHttp.get(this.APIURL + '/filebuffer/getbandlayerid?sFileUrl='+encodeURIComponent(sUrl)+"&sWorkspaceId="+sWorkspaceId+'&sBand='+sBand);
    }

}]);
