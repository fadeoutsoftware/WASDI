/**
 * Created by p.campanella on 26/10/2016.
 */

'use strict';
angular.module('wasdi.FileBufferService', ['wasdi.ConstantsService']).
service('FileBufferService', ['$http',  'ConstantsService', function ($http, oConstantsService) {
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;

    this.download = function(sUrl, sWorkspaceId) {
        return this.m_oHttp.get(this.APIURL + '/filebuffer/download?sFileUrl='+sUrl+"&sWorkspaceId="+sWorkspaceId);
    }

    this.publish = function(sUrl, sWorkspaceId) {
        return this.m_oHttp.get(this.APIURL + '/filebuffer/publish?sFileUrl='+sUrl+"&sWorkspaceId="+sWorkspaceId);
    }

    this.publishBand = function(sUrl, sWorkspaceId, sBand) {
        return this.m_oHttp.get(this.APIURL + '/filebuffer/publishband?sFileUrl='+sUrl+"&sWorkspaceId="+sWorkspaceId+'&sBand='+sBand);
    }


}]);