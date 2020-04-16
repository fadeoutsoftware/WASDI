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

    this.deleteProcessor = function (sProcessorId) {

        var sWorkspaceId = this.m_oConstantsService.getActiveWorkspace();

        if (utilsIsObjectNullOrUndefined(sWorkspaceId) == false) {
            sWorkspaceId = sWorkspaceId.workspaceId;
        }
        else {
            sWorkspaceId = "-";
        }
        return this.m_oHttp.get(this.APIURL + '/processors/delete?processorId='+sProcessorId+'&workspaceId='+sWorkspaceId);
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
    };

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

    this.updateProcessor = function (sWorkspaceId, sProcessorId, oBody) {

        return this.m_oHttp.post(this.APIURL + '/processors/update?workspace=' + encodeURI(sWorkspaceId) + '&processorId=' + encodeURI(sProcessorId),oBody);
    };

    this.updateProcessorFiles = function (sWorkspaceId, sProcessorId, oBody) {

        var oOptions = {
            transformRequest: angular.identity,
            headers: {'Content-Type': undefined}
        };

        return this.m_oHttp.post(this.APIURL + '/processors/updatefiles?workspace=' + encodeURI(sWorkspaceId) + '&processorId=' + encodeURI(sProcessorId),oBody ,oOptions);
    };

    this.readGDACS = function () {
        return this.m_oHttp.get("https://www.gdacs.org/gdacsapi/api/events/geteventlist/MAP?eventtypes=FL");
    };

    this.putShareProcessor = function(sProcessorId,sUserId){
        return this.m_oHttp.put(this.APIURL + '/processors/share/add?processorId=' + sProcessorId + "&userId=" + sUserId);
    };

    this.getUsersBySharedProcessor = function(sProcessorId){
        return this.m_oHttp.get(this.APIURL + '/processors/share/byprocessor?processorId=' + sProcessorId );
    };

    this.deleteUserSharedProcessor = function(sProcessorId,sUserId){
        return this.m_oHttp.delete(this.APIURL + '/processors/share/delete?processorId=' + sProcessorId + "&userId=" + sUserId );
    };

    this.downloadProcessor = function (sProcessorId, sUrl=null) {
        var urlParams = "?" + "token=" + this.m_oConstantsService.getSessionId();
        urlParams = urlParams + "&" + "processorId=" + sProcessorId;

        var sAPIUrl = this.APIURL;

        if(typeof sUrl !== "undefined") {
            if ( sUrl !== null) {
                if (sUrl !== "") {
                    sAPIUrl = sUrl;
                }
            }
        }

        window.location.href = sAPIUrl + "/processors/downloadprocessor" + urlParams;
    };
}]);
