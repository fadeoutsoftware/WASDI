/**
 * Created by p.campanella on 26/10/2016.
 */

'use strict';
angular.module('wasdi.FileBufferService', ['wasdi.ConstantsService']).
service('FileBufferService', ['$http',  'ConstantsService', function ($http, oConstantsService) {
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;


    this.download = function(sUrl, sWorkspaceId,sBounds,sProvider) {

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

        var sTest = sUrl.substring(iCut, sUrl.length);
        var sEncodedUri = encodeURIComponent(sTest);
        sEncodedUri = sProtocol + sEncodedUri;

        return this.m_oHttp.get(this.APIURL + '/filebuffer/download?fileUrl='+sEncodedUri+"&workspace="+sWorkspaceId+"&bbox="+sBounds+'&provider='+sProvider);
    }
    
    this.publishBand = function(sUrl, sWorkspaceId, sBand) {
        return this.m_oHttp.get(this.APIURL + '/filebuffer/publishband?fileUrl='+encodeURIComponent(sUrl)+"&workspace="+sWorkspaceId+'&band='+sBand);
    }

}]);
