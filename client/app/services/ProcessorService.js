/**
 * Created by a.corrado on 18/01/2017.
 */

'use strict';
angular.module('wasdi.ProcessorService', ['wasdi.ProcessorService']).
service('ProcessorService', ['ConstantsService','$rootScope','$http', function (oConstantsService,$rootScope,$http) {
    this.m_oConstantsService = oConstantsService;
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;
    this.m_sResource = "/processors";
    this.getProcessorsList = function() {
        return this.m_oHttp.get(this.APIURL + '/processors/getdeployed');
    };

    this.runProcessor = function (sProcessorName, sJSON) {
        var sEncodedJSON = encodeURI(sJSON);
        var sWorkspaceId = this.m_oConstantsService.getActiveWorkspace();
        if (utilsIsObjectNullOrUndefined(sWorkspaceId) == false) {
            sWorkspaceId = sWorkspaceId.workspaceId;
        }
        else {
            sWorkspaceId = "-";
        }
        return this.m_oHttp.get(this.APIURL + '/processors/run?name='+sProcessorName+'&encodedJson='+ sEncodedJSON+'&workspace='+sWorkspaceId);
    };

    this.getHelpFromProcessor = function (sProcessorName, sJSON) {
        return this.m_oHttp.get(this.APIURL + '/processors/help?name='+sProcessorName);
    };

    this.uploadProcessor = function (sWorkspaceId, sName, sVersion, sDescription, sType, sJsonSample, sPublic, oBody) {

        var oOptions = {
            transformRequest: angular.identity,
            headers: {'Content-Type': undefined}
        };

        return this.m_oHttp.post(this.APIURL + '/processors/uploadprocessor?workspace=' + encodeURI(sWorkspaceId) + '&name=' + encodeURI(sName) + '&version=' + encodeURI(sVersion) +'&description=' + encodeURI(sDescription) + "&type="+encodeURI(sType) + "&paramsSample="+encodeURI(sJsonSample)+"&public="+encodeURI(sPublic),oBody ,oOptions);
    }

    this.getAllErrorLogs = function(oProcessId){

        return this.m_oHttp.get(this.APIURL + '/processors/logs/list?processworkspace='+oProcessId);
    };

    this.getCountLogs = function(oProcessId)
    {
        return this.m_oHttp.get(this.APIURL + this.m_sResource + '/logs/count?processworkspace=' + oProcessId);

    };
    this.getLogs = function(oProcessId,iStartRow,iEndRow)
    {
        return this.m_oHttp.get(this.APIURL + '/processors/logs/list?processworkspace='+oProcessId + '&startrow=' + iStartRow + '&endrow=' + iEndRow);
    };
}]);
