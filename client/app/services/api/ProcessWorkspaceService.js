/**
 * Created by a.corrado on 18/01/2017.
 * This Service points to [host]/wasdiwebserver/rest/process Rest API,
 * which is related to the collection "processworkspace" in mongo DB
 */

'use strict';
angular.module('wasdi.ProcessWorkspaceService', ['wasdi.ProcessWorkspaceService']).
service('ProcessWorkspaceService', ['ConstantsService','$rootScope','$http', 'ModalService',
    function (oConstantsService, $rootScope, $http, oModalService) {

    // this.m_aoProcessesRunning  is a list of objects {processName: "", nodeId: "", typeOfProcess: ""}
    this.m_aoProcessesRunning = [];
    this.m_aoProcessesStopped = [];

    // Days
    this.COOKIE_EXPIRE_TIME_DAYS = 1;
    this.m_oConstantsService = oConstantsService;
    this.m_oModalService = oModalService;
    this.APIURL =  this.m_oConstantsService.getAPIURL();
    this.m_bIgnoreWorkspaceApiUrl = oConstantsService.getIgnoreWorkspaceApiUrl();
    this.m_oHttp = $http;

    /* ATTENTION!!! THE ORDER OF TYPE PROCESS IS IMPORTANT !! */
    this.TYPE_OF_PROCESS=["DOWNLOAD","PUBLISHBAND","PUBLISH","UPDATEPROCESSES"];
    
        /**
         * Load the last 5 processes of a workspace
         * @param sWorkSpaceId
         */
        this.loadProcessesFromServer = function(sWorkSpaceId)
        {
            var oService = this;

            var oWorkspace = this.m_oConstantsService.getActiveWorkspace();
            var sUrl = this.APIURL;

            if (oWorkspace != null && oWorkspace.apiUrl != null && !this.m_bIgnoreWorkspaceApiUrl) {
                sUrl = oWorkspace.apiUrl;
            }

            this.m_oHttp.get(sUrl + '/process/lastbyws?workspace='+sWorkSpaceId).then(function (data, status)
                {
                    if(!utilsIsObjectNullOrUndefined(data.data))
                    {
                        oService.m_aoProcessesRunning = data.data;
                        oService.updateProcessesBar();
                    }
                },function (data,status)
                {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN LOAD PROCESSES");
                });
        };

        /**
         * Get the paginated list of processes of a workspace
         * @param sWorkSpaceId
         * @param iStartIndex
         * @param iEndIndex
         * @returns {*}
         */
        this.getAllProcessesFromServer = function(sWorkSpaceId,iStartIndex,iEndIndex)
        {
            var oWorkspace = this.m_oConstantsService.getActiveWorkspace();
            var sUrl = this.APIURL;

            if (oWorkspace != null && oWorkspace.apiUrl != null && !this.m_bIgnoreWorkspaceApiUrl) {
                sUrl = oWorkspace.apiUrl;
            }

            sUrl += '/process/byws?workspace='+sWorkSpaceId;

            if (utilsIsObjectNullOrUndefined(iStartIndex) === false) {
                sUrl += '&startindex=' + iStartIndex;
            }

            if (utilsIsObjectNullOrUndefined(iEndIndex) === false) {
                sUrl += '&endindex=' + iEndIndex;
            }

            return this.m_oHttp.get(sUrl);
        };

        /**
         * Get the list of process workspace of this user for this application
         * @param sProcessorName Name of the processor to search for
         * @returns {*} List of Process Workpsace View Models
         */
        this.getProcessesByProcessor = function(sProcessorName)
        {
            let sUrl = this.APIURL;
            return this.m_oHttp.get(sUrl + '/process/byapp?processorName='+sProcessorName);
        };


        /**
         * Get the filtered and paginated list of processes of a workspace
         * @param sWorkSpaceId
         * @param iStartIndex
         * @param iEndIndex
         * @param sStatus
         * @param sType
         * @param sDate
         * @param sName
         * @returns {*}
         */
        this.getFilteredProcessesFromServer = function(sWorkSpaceId,iStartIndex,iEndIndex, sStatus, sType, sDate, sName)
        {
            var oWorkspace = this.m_oConstantsService.getActiveWorkspace();
            var sUrl = this.APIURL;

            if (oWorkspace != null && oWorkspace.apiUrl != null && !this.m_bIgnoreWorkspaceApiUrl) {
                sUrl = oWorkspace.apiUrl;
            }

            sUrl = sUrl + '/process/byws?workspace='+sWorkSpaceId +"&startindex=" + iStartIndex + "&endindex=" + iEndIndex;

            if (!utilsIsStrNullOrEmpty(sStatus)) {
                if (sStatus !== "Status...") sUrl += "&status=" +sStatus;
            }
            if (!utilsIsStrNullOrEmpty(sType)) {
                if (sType !== "Type...") sUrl += "&operationType=" +sType;
            }
            if (!utilsIsStrNullOrEmpty(sDate)) {
                sUrl += "&dateFrom=" +sDate + "&dateTo="+sDate;
            }
            if (!utilsIsStrNullOrEmpty(sName)) {
                sUrl += "&namePattern=" +sName;
            }

            return this.m_oHttp.get(sUrl);
        };

        /**
         * Kill (Stop) a running process
         * @param sPidInput
         * @param sWorkSpaceId
         * @param oProcess
         * @returns {boolean}
         */
        this.removeProcessInServer = function(sPidInput,sWorkSpaceId,oProcess)
        {
            if(utilsIsObjectNullOrUndefined(sPidInput)===true) return false;

            var oWorkspace = this.m_oConstantsService.getActiveWorkspace();
            var sUrl = this.APIURL;

            if (oWorkspace != null && oWorkspace.apiUrl != null && !this.m_bIgnoreWorkspaceApiUrl) {
                sUrl = oWorkspace.apiUrl;
            }

            var oService = this;
            this.m_oHttp.get(sUrl + '/process/delete?procws=' + sPidInput).then(function (data, status)
                {
                    if(utilsIsObjectNullOrUndefined(sWorkSpaceId) === false )
                    {
                        oProcess.status = "stopped";
                        oService.m_aoProcessesStopped.push(oProcess);
                        oService.loadProcessesFromServer(sWorkSpaceId);
                    }
                },(function (data,status)
                {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR WHILE KILLING THE PROCESS");
                }));
            return true;
        };

        /**
         * Get the list of running processes
         * @returns {[]}
         */
        this.getProcesses = function()
        {
            return this.m_aoProcessesRunning;
        };

        /**
         * Triggers the update of the WASDI process bar
         */
        this.updateProcessesBar = function()
        {
            //send a message to RootController for update the bar of processes
            $rootScope.$broadcast('m_aoProcessesRunning:updated',true);
        };

        /**
         * Check if a file is under download in this moment
         * @param oLayer
         * @param sTypeOfProcess
         * @returns {boolean}
         */
        this.checkIfFileIsDownloading = function(oLayer,sTypeOfProcess)
        {
            if(utilsIsObjectNullOrUndefined(oLayer)=== true) return false;
            var sProcessName = oLayer.title;
            var sLink = oLayer.link;
            if(utilsIsStrNullOrEmpty(sProcessName)) return false;
            if(utilsIsStrNullOrEmpty(sTypeOfProcess)) return false;

            var sProcess = {"productName":sProcessName,"operationType":sTypeOfProcess, "link":sLink};

            if(utilsIsObjectNullOrUndefined(sProcess)) return false;

            var aoProcesses = this.getProcesses();
            if(utilsIsObjectNullOrUndefined(aoProcesses)) return false;

            var iNumberOfProcesses = aoProcesses.length;

            for(var iIndex = 0; iIndex < iNumberOfProcesses; iIndex++ )
            {
                /*check if the processes are equals*/
                //aoProcesses[iIndex].productName == sProcess.productName
                if( (utilsIsSubstring(aoProcesses[iIndex].productName, sProcess.productName)===true || utilsIsSubstring(aoProcesses[iIndex].productName, sProcess.link)===true)
                    && aoProcesses[iIndex].operationType == sProcess.operationType && aoProcesses[iIndex].status=== "RUNNING")
                {
                    return true;
                }
            }
            return false;

        };

        /**
         * Return the downlod Proc Type
         * @returns {string}
         */
        this.getTypeOfProcessProductDownload = function()
        {
            return this.TYPE_OF_PROCESS[0];
        };

        /**
         * Return the Publish Band Proc Type
         * @returns {string}
         */
        this.getTypeOfProcessPublishingBand = function()
        {
            return this.TYPE_OF_PROCESS[1];
        };

        /**
         * Get a single process workspace object
         * @param sProcessId
         * @returns {*}
         */
        this.getProcessWorkspaceById = function(sProcessId)
        {
            var oWorkspace = this.m_oConstantsService.getActiveWorkspace();
            var sUrl = this.APIURL;

            if (oWorkspace != null && oWorkspace.apiUrl != null && !this.m_bIgnoreWorkspaceApiUrl) {
                sUrl = oWorkspace.apiUrl;
            }

            return this.m_oHttp.get(sUrl + '/process/byid?procws=' + sProcessId);
        };

        /**
         * Get the summary of created and running processes
         * @returns {*}
         */
        this.getSummary  =function()
        {
            var oWorkspace = this.m_oConstantsService.getActiveWorkspace();
            var sUrl = this.APIURL;

            if (oWorkspace != null && oWorkspace.apiUrl != null && !this.m_bIgnoreWorkspaceApiUrl) {
                sUrl = oWorkspace.apiUrl;
            }


            return this.m_oHttp.get(sUrl + '/process/summary');
        };

        /**
         * Delete a process
         * @param oProcessInput
         * @returns {boolean}
         */
        this.deleteProcess = function(oProcessInput)
        {
            var oController = this;
            var oWorkspace = this.m_oConstantsService.getActiveWorkspace();

            this.m_oModalService.showModal({
                templateUrl: "dialogs/delete_process/DeleteProcessDialog.html",
                controller: "DeleteProcessController"
            }).then(function(modal) {
                modal.element.modal();
                modal.close.then(function(result) {
                    if(result === 'delete')
                        oController.removeProcessInServer(oProcessInput.processObjId, oWorkspace.workspaceId, oProcessInput)
                });
            });

            return true;
        };

        this.getProcessorStatistics = function(sProcessorName)
        {
            let sUrl = this.APIURL;
            return this.m_oHttp.get(sUrl + '/process/appstats?processorName='+sProcessorName);
        };

        this.getProcessWorkspaceTotalRunningTimeByUserAndInterval = function(sUserId, sDateFrom, sDateTo) {
            let sUrl = this.APIURL + '/process/runningTime?userId=' + sUserId;

            if (utilsIsStrNullOrEmpty(sDateFrom) === false) {
                sUrl += '&dateFrom=' + sDateFrom;
            }

            if (utilsIsStrNullOrEmpty(sDateTo) === false) {
                sUrl += '&dateTo=' + sDateTo;
            }

            return this.m_oHttp.get(sUrl);
        };

        this.getQueuesStatus = function(sNodeCode, sStatuses) {
            let sUrl = this.APIURL + '/process/queuesStatus?';

            if (utilsIsStrNullOrEmpty(sNodeCode) === false) {
                sUrl += '&nodeCode=' + sNodeCode;
            }

            if (utilsIsStrNullOrEmpty(sStatuses) === false) {
                sUrl += '&statuses=' + sStatuses;
            }

            return this.m_oHttp.get(sUrl);
        };

        this.getAvailableNodesSortedByScore = function() {
            let sUrl = this.APIURL + '/process/nodesByScore?';

            return this.m_oHttp.get(sUrl);
        };

}]);
