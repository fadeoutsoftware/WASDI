/**
 * Created by a.corrado on 09/06/2017.
 */
'use strict';
var WorkspaceProcessesList = (function () {

    function WorkspaceProcessesList($scope, oClose, oProcessesLaunchedService, oConstantsService, oModalService, oProcessorService, $interval) {//,oExtras
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oModalService = oModalService;
        this.m_oProcessorService = oProcessorService;
        this.hasError = false;
        this.m_oProcessesLaunchedService = oProcessesLaunchedService;
        this.m_aoProcessesLogs = [];
        this.filterTable = "";
        this.m_bAreProcessesLoaded = false;
        this.m_oFilter = {};

        this.m_oFilter.m_sStatus = "Status...";
        this.m_oFilter.m_sType = "Type...";
        this.m_oFilter.m_sDate = "";
        this.m_oFilter.m_sName = "";

        this.m_oInterval = $interval;

        this.m_iNumberOfProcessForRequest = 40;
        this.m_iFirstProcess = 0;
        this.m_iLastProcess = this.m_iNumberOfProcessForRequest;
        // this.m_oExtrs= oExtras;
        //$scope.close = oClose;
        this.m_oConstantsService = oConstantsService;
        this.isLoadMoreButtonClickable = true;

        this.m_oTick;

        if (_.isNil(this.m_oConstantsService.getActiveWorkspace()) == false) {
            this.m_sActiveWorkspaceId = this.m_oConstantsService.getActiveWorkspace().workspaceId;

            $scope.close = function (result) {
                // stops the update of the inverval
                this.m_oController.stopTick();
                // close, but give 500ms for bootstrap to animate
                oClose(result, 500);
            };

            this.getAllProcessesLogs();
            this.m_sHrefLogFile = "";
        }
        else {
            this.hasError = true;
            this.m_sActiveWorkspaceId = null;
        }
        // invoke the
        this.IntervalUpdate();

    }

    // function that stops the interval update
    WorkspaceProcessesList.prototype.stopTick = function () {
        let oController = this;
        if (angular.isDefined(oController.m_oTick)) {
            oController.m_oInterval.cancel(oController.m_oTick);
            oController.m_oTick = undefined;
        }
    }

    /**
     * Function invoked in the constructor to update the status of the
     * current modal, the current version stops after 10 iterations
     * @constructor
     */
    WorkspaceProcessesList.prototype.IntervalUpdate = function () {
        let oController = this;
        let iIntervalmS = 5000; // interval of the periodical update in mS
        // Check the status of The windows in the update loop !
        oController.m_oTick = oController.m_oInterval(function () {
            // suspend binding on angular 
            oController.getLastProcessesLogs();
        }, iIntervalmS);
    }

    WorkspaceProcessesList.prototype.comboStatusClick = function (sStatus) {

        if (sStatus == "None") sStatus = "Status...";
        this.m_oFilter.m_sStatus = sStatus;
    };

    WorkspaceProcessesList.prototype

        .comboTypeClick = function (sStatus) {

            if (sStatus == "None") sStatus = "Type...";
            this.m_oFilter.m_sType = sStatus;
        };

    WorkspaceProcessesList.prototype.applyFilters = function () {
        this.resetCounters();
        this.m_aoProcessesLogs = [];
        this.getAllProcessesLogs();
    };

    WorkspaceProcessesList.prototype.resetFilters = function () {
        this.resetCounters();
        this.m_aoProcessesLogs = [];
        this.m_oFilter.m_sType = "Type...";
        this.m_oFilter.m_sStatus = "Status...";
        this.m_oFilter.m_sName = "";
        this.m_oFilter.m_sDate = "";

        this.getAllProcessesLogs();
    };


    /**
     * Get the Last 40 ProcessesLogs
     * Note: the processes Log are retrieved considering the status of the filters
     * @returns {boolean}
     */
    WorkspaceProcessesList.prototype.getLastProcessesLogs = function () {
        var oController = this;

        if (utilsIsObjectNullOrUndefined(this.m_sActiveWorkspaceId) === true) {
            return false;
        }

        //this.m_bAreProcessesLoaded = false;
        // retrieves the last 40 processor Logs considering the current state of the filters
        this.m_oProcessesLaunchedService.getFilteredProcessesFromServer(this.m_sActiveWorkspaceId, 0, 40,this.m_oFilter.m_sStatus, this.m_oFilter.m_sType, this.m_oFilter.m_sDate, this.m_oFilter.m_sName)
            .then(function (data) {
                if (!utilsIsObjectNullOrUndefined(data.data)) {
                    if (data.data.length > 0) {
                        oController.m_aoProcessesLogs = data.data;
                        oController.m_sHrefLogFile = oController.generateLogFile();
                    }
                    else {
                        oController.isLoadMoreButtonClickable = false;
                    }

                    if (data.data.length < oController.m_iNumberOfProcessForRequest) {
                        //there aren't enough processes for other requests so you can't load more processes
                        oController.isLoadMoreButtonClickable = false;

                    }
                   // oController.m_bAreProcessesLoaded = true;
                }
            }, function (data, status) {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN PROCESSES LOGS DIALOG<br>UNABLE TO LOAD ALL PROCESSES LOGS FROM SERVER");
                oController.m_bAreProcessesLoaded = true;
            });

        return true;
    };

    /**
     * getAllProcessesLogs
     * @returns {boolean}
     */
    WorkspaceProcessesList.prototype.getAllProcessesLogs = function () {
        var oController = this;

        if (utilsIsObjectNullOrUndefined(this.m_sActiveWorkspaceId) === true) {
            return false;
        }

        this.m_bAreProcessesLoaded = false;

        //this.m_oProcessesLaunchedService.getAllProcessesFromServer(this.m_sActiveWorkspaceId,this.m_iFirstProcess,this.m_iLastProcess).success(function (data, status)
        this.m_oProcessesLaunchedService.getFilteredProcessesFromServer(this.m_sActiveWorkspaceId, this.m_iFirstProcess, this.m_iLastProcess,this.m_oFilter.m_sStatus, this.m_oFilter.m_sType, this.m_oFilter.m_sDate, this.m_oFilter.m_sName)
            .then(function (data, status) {
                if (!utilsIsObjectNullOrUndefined(data.data)) {
                    if (data.data.length > 0) {
                        oController.m_aoProcessesLogs = oController.m_aoProcessesLogs.concat(data.data);
                        oController.m_sHrefLogFile = oController.generateLogFile();
                        oController.calculateNextListOfProcess();
                    }
                    else {
                        oController.isLoadMoreButtonClickable = false;
                    }

                    if (data.data.length < oController.m_iNumberOfProcessForRequest) {
                        //there aren't enough processes for other requests so you can't load more processes
                        oController.isLoadMoreButtonClickable = false;

                    }
                    oController.m_bAreProcessesLoaded = true;
                }
            }, function (data, status) {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN PROCESSES LOGS DIALOG<br>UNABLE TO LOAD ALL PROCESSES LOGS FROM SERVER");
                oController.m_bAreProcessesLoaded = true;
            });

        return true;
    };

    WorkspaceProcessesList.prototype.calculateNextListOfProcess = function () {
        this.m_iFirstProcess = this.m_iFirstProcess + this.m_iNumberOfProcessForRequest;
        this.m_iLastProcess = this.m_iLastProcess + this.m_iNumberOfProcessForRequest;
    };

    WorkspaceProcessesList.prototype.resetCounters = function () {
        this.m_iNumberOfProcessForRequest = 40;
        this.m_iFirstProcess = 0;
    };

    WorkspaceProcessesList.prototype.getProcessDuration = function (oProcess) {
        //time by server
        let oStartTime = new Date(oProcess.operationStartDate);
        let oEndTime = new Date(oProcess.operationEndDate);

        if (utilsIsValidDate(oEndTime) === false) {
            oEndTime = new Date(oProcess.lastChangeDate);
        }

        if (utilsIsValidDate(oEndTime) === false) {
            oEndTime = new Date();
        }

        //pick time
        let iMilliseconds = Math.abs(oEndTime - oStartTime);
        //approximate result
        let iSecondsTimeSpan = Math.ceil(iMilliseconds / 1000);

        if (utilsIsObjectNullOrUndefined(iSecondsTimeSpan) || iSecondsTimeSpan < 0) iSecondsTimeSpan = 0;

        // Calculate number of hours
        let iHours = Math.trunc(iSecondsTimeSpan / (3600));

        let iMinutesReminder = iSecondsTimeSpan - (iHours * 3600);
        let iMinutes = Math.trunc(iMinutesReminder / 60);
        let iSeconds = iMinutesReminder - (iMinutes * 60);

        let sTimeSpan = this.renderTwoDigitNumber(iHours) + ":" + this.renderTwoDigitNumber(iMinutes) + ":" + this.renderTwoDigitNumber(iSeconds);


        //var oDate = new Date(1970, 0, 1);
        //oDate.setSeconds(0 + iSecondsTimeSpan);

        //return oDate;
        return sTimeSpan;
    };

    WorkspaceProcessesList.prototype.renderTwoDigitNumber = function (iNumber) {
        // Render the number
        let sNumber = "00";

        if (iNumber > 0) {
            if (iNumber < 10) {
                sNumber = "0" + String(iNumber);
            }
            else {
                sNumber = String(iNumber);
            }
        }

        return sNumber;
    };

    WorkspaceProcessesList.prototype.generateFile = function (sText) {
        var textFile = null;
        var sType = 'text/plain';
        textFile = utilsMakeFile(sText, textFile, sType);
        return textFile;
    };

    WorkspaceProcessesList.prototype.makeStringLogFile = function () {
        if (utilsIsObjectNullOrUndefined(this.m_aoProcessesLogs) === true)
            return null;
        // m_aoProcessesLogs
        var iNumberOfProcessesLogs = this.m_aoProcessesLogs.length;
        var sText = "";
        for (var iIndexProcessLog = 0; iIndexProcessLog < iNumberOfProcessesLogs; iIndexProcessLog++) {
            // sText += this.m_aoProcessesLogs[iIndexProcessLog] + "/n";
            var sOperationDate = this.m_aoProcessesLogs[iIndexProcessLog].operationStartDate;
            var sFileSize = this.m_aoProcessesLogs[iIndexProcessLog].fileSize;
            var sOperationEndDate = this.m_aoProcessesLogs[iIndexProcessLog].operationEndDate;
            var sOperationType = this.m_aoProcessesLogs[iIndexProcessLog].operationType;
            var sPid = this.m_aoProcessesLogs[iIndexProcessLog].pid;
            // var sProcessObjId = this.m_aoProcessesLogs[iIndexProcessLog].processObjId;
            var sProductName = this.m_aoProcessesLogs[iIndexProcessLog].productName;
            var sProgressPerc = this.m_aoProcessesLogs[iIndexProcessLog].progressPerc;
            var sStatus = this.m_aoProcessesLogs[iIndexProcessLog].status;
            var sUserId = this.m_aoProcessesLogs[iIndexProcessLog].userId;


            sText += iIndexProcessLog + ") " + "Id: " + sPid + ",Product Name: " + sProductName + ",Operation Type: " + sOperationType +
                ",User: " + sUserId + ",Status: " + sStatus + ",Progress: " + sProgressPerc + "%" +
                ",Operation date: " + sOperationDate + ",Operation end date: " + sOperationEndDate + ",File size: " + sFileSize + "\r\n";
        }

        return sText;
    };
    /**
     *
     */
    WorkspaceProcessesList.prototype.generateLogFile = function () {
        var sText = this.makeStringLogFile();
        var oFile = this.generateFile(sText);
        return oFile;
    };

    WorkspaceProcessesList.prototype.openProcessorLogsDialog = function (oProcess) {

        var oController = this;

        if (utilsIsObjectNullOrUndefined(oProcess) === true) {
            return false;
        }
        oController.m_oModalService.showModal({
            templateUrl: "dialogs/processor_logs/ProcessorLogsView.html",
            controller: "ProcessorLogsController",
            inputs: {
                extras: {
                    process: oProcess,
                }
            }
        }).then(function (modal) {
            //modal.element.modal();
            modal.element.modal({
                backdrop: 'static',
                keyboard: false
            });

            modal.close.then(function (oResult) {

            });
        });
        return true;
    };

    WorkspaceProcessesList.prototype.openPayloadDialog = function (oProcess) {

        var oController = this;

        if (utilsIsObjectNullOrUndefined(oProcess) === true) {
            return false;
        }
        oController.m_oModalService.showModal({
            templateUrl: "dialogs/payload_dialog/PayloadDialog.html",
            controller: "PayloadDialogController",
            inputs: {
                extras: {
                    process: oProcess,
                }
            }
        }).then(function (modal) {
            modal.element.modal();
            modal.close.then(function (oResult) {

            });
        });
    };



    WorkspaceProcessesList.prototype.deleteProcess = function (oProcessInput) {
        this.m_oProcessesLaunchedService.deleteProcess(oProcessInput);
        return true;
    };

    WorkspaceProcessesList.prototype.getOperationDescription = function (oOperation) {
        return utilsConvertOperationToDescription(oOperation);
    };




    WorkspaceProcessesList.$inject = [
        '$scope',
        'close',
        'ProcessesLaunchedService',
        'ConstantsService',
        'ModalService',
        'ProcessorService',
        '$interval'
        // 'extras',
    ];
    return WorkspaceProcessesList;
})();
