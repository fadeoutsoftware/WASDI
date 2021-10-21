

'use strict';
angular.module('wasdi.CatalogService', ['wasdi.CatalogService']).
service('CatalogService', ['$http',  'ConstantsService', function ($http, oConstantsService) {
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;

    this.downloadByName = function(sFileName, sWorkspace, sUrl)
    {
        var urlParams = "?" + "token=" + oConstantsService.getSessionId();
        urlParams = urlParams + "&" + "filename=" + sFileName + "&workspace=" + sWorkspace;

        var sAPIUrl = this.APIURL;

        if(typeof sUrl !== "undefined") {
            if ( sUrl !== null) {
                if (sUrl !== "") {
                    sAPIUrl = sUrl;
                }
            }
        }

        window.location.href = sAPIUrl + "/catalog/downloadbyname" + urlParams;
    };

    this.ingestFile = function(sSelectedFile,sWorkspace){
        return this.m_oHttp.put(this.APIURL + '/catalog/upload/ingest?file=' + sSelectedFile + '&workspace=' + sWorkspace);
    };

    this.uploadFTPFile = function(oFtpTransferFile,sWorkspaceId){
        return this.m_oHttp.put(this.APIURL + '/catalog/upload/ftp?workspace='+sWorkspaceId,oFtpTransferFile);
    };
}]);

