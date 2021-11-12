/**
 * Created by a.corrado on 09/06/2017.
 */
'use strict';
var WorkspaceProcessesList = (function () {

    function WorkspaceProcessesList($scope, oClose, oProcessWorkspaceService, oConstantsService, oModalService, oProcessorService, $interval) {//,oExtras
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oModalService = oModalService;
        this.m_oProcessorService = oProcessorService;
        this.hasError = false;
        this.m_oProcessWorkspaceService = oProcessWorkspaceService;
        this.m_aoProcessesLogs = [];
        this.m_aoAllProcessesLogs = [];
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

        let oActiveWorkspace = this.m_oConstantsService.getActiveWorkspace();
        if (_.isNil(oActiveWorkspace) == false) {
            this.m_sActiveWorkspaceId = oActiveWorkspace.workspaceId;

            $scope.close = function (result) {
                // stops the update of the inverval
                this.m_oController.stopTick();
                // close, but give 500ms for bootstrap to animate
                oClose(result, 500);
            };

            this.getAllProcessesLogs();
        } else {
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

        // retrieves the last 40 processor Logs considering the current state of the filters
        this.m_oProcessWorkspaceService.getFilteredProcessesFromServer(this.m_sActiveWorkspaceId, 0, 40, this.m_oFilter.m_sStatus, this.m_oFilter.m_sType, this.m_oFilter.m_sDate, this.m_oFilter.m_sName)
            .then(function (data) {
                if (!utilsIsObjectNullOrUndefined(data.data)) {
                    if (data.data.length > 0) {
                        // update only the last 40, instead of reassign all the array
                        data.data.forEach(function callbackFn(element, index) {
                            oController.m_aoProcessesLogs[index] = element;
                        });
                        //oController.m_aoProcessesLogs = data.data;
                    }
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

        this.m_oProcessWorkspaceService.getFilteredProcessesFromServer(this.m_sActiveWorkspaceId, this.m_iFirstProcess, this.m_iLastProcess, this.m_oFilter.m_sStatus, this.m_oFilter.m_sType, this.m_oFilter.m_sDate, this.m_oFilter.m_sName)
            .then(function (data, status) {
                if (!utilsIsObjectNullOrUndefined(data.data)) {
                    if (data.data.length > 0) {
                        oController.m_aoProcessesLogs = oController.m_aoProcessesLogs.concat(data.data);
                        oController.calculateNextListOfProcess();
                    } else {
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
    /**
     * Calculate and retrieve process duration in HH:MM:SS format
     * Bind on ng-binding in the WorkspaceProcessList dialog
     * @param oProcess The process Object (see ProcessWorkspaceViewModel.java)
     * @returns {string} String of duration in HH:MM:SS format
     */
    WorkspaceProcessesList.prototype.getProcessDuration = function (oProcess) {
        // start time by server
        let oStartTime = new Date(oProcess.operationStartDate);
        // still running -> assign "now"
        let oEndTime = new Date();
        // reassign in case the process is already ended
        if(oProcess.operationEndDate != "null Z"){
            oEndTime = new Date(oProcess.operationEndDate);
        }

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
            } else {
                sNumber = String(iNumber);
            }
        }

        return sNumber;
    };

    WorkspaceProcessesList.prototype.downloadProcessesFile = function () {
        var oController = this;

        this.m_oProcessWorkspaceService.getAllProcessesFromServer(this.m_sActiveWorkspaceId,null,null).then(function (data, status) {
            if (data.data != null)
            {
                if (data.data != undefined)
                {
                    oController.m_aoAllProcessesLogs = data.data;

                    let file = oController.generateLogFile();

                    var oLink=document.createElement('a');
                    oLink.href = file;
                    oLink.download = "processes";
                    oLink.click();
                }
            }
        },function (data,status) {
            //alert('error');
            utilsVexDialogAlertTop('GURU MEDITATION<br>ERROR IN DOWNLOADING PROCESSES LIST');
        });

    };

    WorkspaceProcessesList.prototype.generateFile = function (sText) {
        var textFile = null;
        var sType = 'text/csv';
        textFile = utilsMakeFile(sText, textFile, sType);
        return textFile;
    };

    WorkspaceProcessesList.prototype.makeStringLogFile = function () {
        if (utilsIsObjectNullOrUndefined(this.m_aoAllProcessesLogs) === true)
            return null;
        // m_aoAllProcessesLogs
        var iNumberOfProcessesLogs = this.m_aoAllProcessesLogs.length;
        var sText = "";

        sText += "Id,Product Name,Operation Type,User,Status,Progress,Operation date,Operation end date,File size" + "\r\n";

        for (var iIndexProcessLog = 0; iIndexProcessLog < iNumberOfProcessesLogs; iIndexProcessLog++) {
            var sOperationDate = this.m_aoAllProcessesLogs[iIndexProcessLog].operationStartDate;
            var sFileSize = this.m_aoAllProcessesLogs[iIndexProcessLog].fileSize;
            var sOperationEndDate = this.m_aoAllProcessesLogs[iIndexProcessLog].operationEndDate;
            var sOperationType = this.m_aoAllProcessesLogs[iIndexProcessLog].operationType;
            var sPid = this.m_aoAllProcessesLogs[iIndexProcessLog].pid;
            var sProductName = this.m_aoAllProcessesLogs[iIndexProcessLog].productName;
            var sProgressPerc = this.m_aoAllProcessesLogs[iIndexProcessLog].progressPerc;
            var sStatus = this.m_aoAllProcessesLogs[iIndexProcessLog].status;
            var sUserId = this.m_aoAllProcessesLogs[iIndexProcessLog].userId;

            sText += sPid + "," + sProductName + "," + sOperationType +
                "," + sUserId + "," + sStatus + "," + sProgressPerc + "%" +
                "," + sOperationDate + "," + sOperationEndDate + "," + sFileSize + "\r\n";
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
        this.m_oProcessWorkspaceService.deleteProcess(oProcessInput);
        return true;
    };

    WorkspaceProcessesList.prototype.getOperationDescription = function (oOperation) {
        return utilsConvertOperationToDescription(oOperation);
    };


    WorkspaceProcessesList.$inject = [
        '$scope',
        'close',
        'ProcessWorkspaceService',
        'ConstantsService',
        'ModalService',
        'ProcessorService',
        '$interval'
        // 'extras',
    ];
    return WorkspaceProcessesList;
})();
