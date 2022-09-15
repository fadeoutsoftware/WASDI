/**
 * Created by a.corrado on 18/01/2017.
 */

'use strict';
angular.module('wasdi.ProcessorService', ['wasdi.ProcessorService']).
service('ProcessorService', ['ConstantsService','$rootScope','$http', function (oConstantsService,$rootScope,$http) {
    this.m_oConstantsService = oConstantsService;
    this.APIURL = oConstantsService.getAPIURL();
    this.m_bIgnoreWorkspaceApiUrl = oConstantsService.getIgnoreWorkspaceApiUrl();
    this.m_oHttp = $http;
    this.m_sResource = "/processors";

    /**
     * Get the full light list of deployed processors
     * @returns {*}
     */
    this.getProcessorsList = function() {
        return this.m_oHttp.get(this.APIURL + '/processors/getdeployed');
    };

    /**
     * Get the base info of a single processor
     * (same view model as returned by getProcessorsList)
     * @param sProcessorId
     * @returns {*}
     */
    this.getDeployedProcessor = function (sProcessorId) {
        return this.m_oHttp.get(this.APIURL + '/processors/getprocessor?processorId=' + sProcessorId);
    }


    /**
     * Get the list of applications to be shown in the marketplace
     * @param oFilter
     * @returns {*}
     */
    this.getMarketplaceList = function(oFilter) {
        return this.m_oHttp.post(this.APIURL + '/processors/getmarketlist',oFilter);
    };

    /**
     * Get the details of an application for the marketplace
     * @param sApplication
     * @returns {*}
     */
    this.getMarketplaceDetail = function(sApplication) {
        return this.m_oHttp.get(this.APIURL + '/processors/getmarketdetail?processorname=' + sApplication);
    };

    /**
     * Run a user processor
     * @param sProcessorName
     * @param sJSON
     * @returns {*}
     */
    this.runProcessor = function (sProcessorName, sJSON) {
        var sEncodedJSON = encodeURI(sJSON);
        var sWorkspaceId = this.m_oConstantsService.getActiveWorkspace();
        if (utilsIsObjectNullOrUndefined(sWorkspaceId) == false) {
            sWorkspaceId = sWorkspaceId.workspaceId;
        }
        else {
            sWorkspaceId = "-";
        }
        return this.m_oHttp.post(this.APIURL + '/processors/run?name='+sProcessorName+'&workspace='+sWorkspaceId, sEncodedJSON);
    };

    /**
     * Delete a user processor
     * @param sProcessorId
     * @returns {*}
     */
    this.deleteProcessor = function (sProcessorId) {

        var sWorkspaceId = this.m_oConstantsService.getActiveWorkspace();

        if (utilsIsObjectNullOrUndefined(sWorkspaceId) == false) {
            sWorkspaceId = sWorkspaceId.workspaceId;
        }
        else {
            sWorkspaceId = "-";
        }
        return this.m_oHttp.get(this.APIURL + '/processors/delete?processorId='+sProcessorId+'&workspace='+sWorkspaceId);
    };

    /**
     * Get help of a porcessor
     * @param sProcessorName
     * @param sJSON
     * @returns {*}
     */
    this.getHelpFromProcessor = function (sProcessorName, sJSON) {
        return this.m_oHttp.get(this.APIURL + '/processors/help?name='+sProcessorName);
    };

    /**
     * Upload processor data
     * @param sWorkspaceId
     * @param sName
     * @param sVersion
     * @param sDescription
     * @param sType
     * @param sJsonSample
     * @param sPublic
     * @param oBody
     * @returns {*}
     */
    this.uploadProcessor = function (sWorkspaceId, sName, sVersion, sDescription, sType, sJsonSample, sPublic, oBody) {

        var oOptions = {
            transformRequest: angular.identity,
            headers: {'Content-Type': undefined}
        };

        return this.m_oHttp.post(this.APIURL + '/processors/uploadprocessor?workspace=' + encodeURI(sWorkspaceId) + '&name=' + encodeURI(sName) + '&version=' + encodeURI(sVersion) +'&description=' + encodeURI(sDescription) + "&type="+encodeURI(sType) + "&paramsSample="+encodeURI(sJsonSample)+"&public="+encodeURI(sPublic),oBody ,oOptions);
    };

    /**
     * Get processors logs
     * @param oProcessId
     * @returns {*}
     */
    this.getProcessorLogs = function(oProcessId){
        var oWorkspace = this.m_oConstantsService.getActiveWorkspace();
        var sUrl = this.APIURL;

        if (oWorkspace != null && oWorkspace.apiUrl != null && !this.m_bIgnoreWorkspaceApiUrl) {
            sUrl = oWorkspace.apiUrl;
        }

        return this.m_oHttp.get(sUrl + '/processors/logs/list?processworkspace='+oProcessId);
    };

    /**
     * Get count a processor's logs
     * @param oProcessId
     * @returns {*}
     */
    this.getLogsCount = function(oProcessId)
    {
        var oWorkspace = this.m_oConstantsService.getActiveWorkspace();
        var sUrl = this.APIURL;

        if (oWorkspace != null && oWorkspace.apiUrl != null && !this.m_bIgnoreWorkspaceApiUrl) {
            sUrl = oWorkspace.apiUrl;
        }

        return this.m_oHttp.get(sUrl + this.m_sResource + '/logs/count?processworkspace=' + oProcessId);
    };

    /**
     * Get a page of processor's logs
     * @param oProcessId
     * @param iStartRow
     * @param iEndRow
     * @returns {*}
     */
    this.getPaginatedLogs = function(oProcessId, iStartRow, iEndRow)
    {
        var oWorkspace = this.m_oConstantsService.getActiveWorkspace();
        var sUrl = this.APIURL;

        if (oWorkspace != null && oWorkspace.apiUrl != null && !this.m_bIgnoreWorkspaceApiUrl) {
            sUrl = oWorkspace.apiUrl;
        }

        sUrl += '/processors/logs/list?processworkspace='+oProcessId;

        if (utilsIsObjectNullOrUndefined(iStartRow) === false) {
            sUrl += '&startrow=' + iStartRow;
        }

        if (utilsIsObjectNullOrUndefined(iEndRow) === false) {
            sUrl += '&endrow=' + iEndRow;
        }

        return this.m_oHttp.get(sUrl);
    };

    /**
     * Update a processor
     * @param sWorkspaceId
     * @param sProcessorId
     * @param oBody
     * @returns {*}
     */
    this.updateProcessor = function (sProcessorId, oBody) {

        return this.m_oHttp.post(this.APIURL + '/processors/update?processorId=' + encodeURI(sProcessorId),oBody);
    };

    /**
     * Update the details of a processor
     * @param sWorkspaceId
     * @param sProcessorId
     * @param oBody
     * @returns {*}
     */
    this.updateProcessorDetails = function (sProcessorId, oBody) {
        return this.m_oHttp.post(this.APIURL + '/processors/updatedetails?processorId=' + encodeURI(sProcessorId),oBody);
    };

    /**
     * Update Processor files
     * @param sWorkspaceId
     * @param sProcessorId
     * @param oBody
     * @returns {*}
     */
    this.updateProcessorFiles = function (sFileName, sProcessorId, oBody) {

        var oOptions = {
            transformRequest: angular.identity,
            headers: {'Content-Type': undefined}
        };

        var sWorkspaceId = this.m_oConstantsService.getActiveWorkspace();

        if (utilsIsObjectNullOrUndefined(sWorkspaceId) == false) {
            sWorkspaceId = sWorkspaceId.workspaceId;
        }
        else {
            sWorkspaceId = "-";
        }        

        return this.m_oHttp.post(this.APIURL + '/processors/updatefiles?workspace=' + encodeURI(sWorkspaceId) + '&processorId=' + encodeURI(sProcessorId) + '&file='+encodeURI(sFileName),oBody ,oOptions);
    };

    /**
     * Create a Console
     * @param sWorkspaceId
     * @returns {*}
     */
    this.createConsole = function(sWorkspaceId) {
        return this.m_oHttp.post(this.APIURL + '/console/create?workspaceId=' + sWorkspaceId);
    };

    /**
     * Share a processor
     * @param sProcessorId
     * @param sUserId
     * @returns {*}
     */
    this.putShareProcessor = function(sProcessorId,sUserId){
        return this.m_oHttp.put(this.APIURL + '/processors/share/add?processorId=' + sProcessorId + "&userId=" + sUserId);
    };

    /**
     * Get users that has access to a processor
     * @param sProcessorId
     * @returns {*}
     */
    this.getUsersBySharedProcessor = function(sProcessorId){
        return this.m_oHttp.get(this.APIURL + '/processors/share/byprocessor?processorId=' + sProcessorId );
    };

    /**
     * Delete a processor sharing
     * @param sProcessorId
     * @param sUserId
     * @returns {*}
     */
    this.deleteUserSharedProcessor = function(sProcessorId,sUserId){
        return this.m_oHttp.delete(this.APIURL + '/processors/share/delete?processorId=' + sProcessorId + "&userId=" + sUserId );
    };

    /**
     * Force processor Refresh
     * @param sProcessorId
     * @returns {*}
     */
    this.redeployProcessor = function(sProcessorId){

        var oWorkspace = this.m_oConstantsService.getActiveWorkspace();
        var sWorkspaceId = "-";

        if (utilsIsObjectNullOrUndefined(oWorkspace) == false) {
            sWorkspaceId = oWorkspace.workspaceId;
        }

        return this.m_oHttp.get(this.APIURL + '/processors/redeploy?processorId=' + sProcessorId + "&workspace=" + sWorkspaceId);
    };

    /**
     * Force library Update
     * @param sProcessorId
     * @returns {*}
     */
         this.forceLibUpdate = function(sProcessorId){

            var oWorkspace = this.m_oConstantsService.getActiveWorkspace();
            var sWorkspaceId = "-";
    
            if (utilsIsObjectNullOrUndefined(oWorkspace) == false) {
                sWorkspaceId = oWorkspace.workspaceId;
            }
    
            return this.m_oHttp.get(this.APIURL + '/processors/libupdate?processorId=' + sProcessorId + "&workspace=" + sWorkspaceId);
        };

        /**
         * Force environment Update
         * @param sProcessorId
         * @param sEnvUpdCommand
         * @returns {*}
         */
        this.forceEnvUpdate = function(sProcessorId, sEnvUpdCommand) {

                var oWorkspace = this.m_oConstantsService.getActiveWorkspace();
                var sWorkspaceId = "-";

                if (utilsIsObjectNullOrUndefined(oWorkspace) == false) {
                    sWorkspaceId = oWorkspace.workspaceId;
                }

                return this.m_oHttp.get(this.APIURL + '/processors/environmentupdate?processorId=' + sProcessorId + "&workspace=" + sWorkspaceId + "&updateCommand=" + sEnvUpdCommand);
            };

    /**
     * Download a processor
     * @param sProcessorId
     * @param sUrl
     */
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

    /**
     * Get the representation of the Processor UI
     * @param sProcessorName
     * @returns {*}
     */
    this.getProcessorUI = function (sProcessorName) {
        return this.m_oHttp.get(this.APIURL + '/processors/ui?name='+sProcessorName);
    }

    /**
     * Save the Processor UI JSON Definition
     * @param sProcessorName name of the processor
     * @param sProcessorUI string with the json
     * @returns {*}
     */
    this.saveProcessorUI = function (sProcessorName, sProcessorUI) {
        return this.m_oHttp.post(this.APIURL + '/processors/saveui?name='+sProcessorName, sProcessorUI);
    }
}]);
