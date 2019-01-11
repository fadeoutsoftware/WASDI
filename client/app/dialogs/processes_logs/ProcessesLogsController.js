/**
 * Created by a.corrado on 09/06/2017.
 */

var ProcessesLogsController = (function() {

    function ProcessesLogsController($scope, oClose, oProcessesLaunchedService,oConstantsService) {//,oExtras
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oProcessesLaunchedService = oProcessesLaunchedService;
        this.m_aoProcessesLogs = [];
        this.filterTable = "";this.m_bAreProcessesLoaded = false;
        // this.m_oExtrs= oExtras;
        //$scope.close = oClose;
        this.m_oConstantsService = oConstantsService;
        this.m_sActiveWorkspaceId = this.m_oConstantsService.getActiveWorkspace().workspaceId,
        $scope.close = function(result) {
            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };

        this.getAllProcessesLogs();
        this.m_sHrefLogFile = "";
;    }

    /**
     * getAllProcessesLogs
     * @returns {boolean}
     */
    ProcessesLogsController.prototype.getAllProcessesLogs = function () {
        var oController = this;

        if(utilsIsObjectNullOrUndefined(this.m_sActiveWorkspaceId)=== true)
            return false;

        this.m_oProcessesLaunchedService.getAllProcessesFromServer(this.m_sActiveWorkspaceId).success(function (data, status)
        {
            if(!utilsIsObjectNullOrUndefined(data))
            {
                oController.m_aoProcessesLogs = data;
                oController.m_bAreProcessesLoaded = true;
                oController.m_sHrefLogFile = oController.generateLogFile();
            }
        }).error(function (data,status)
        {

            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN PROCESSES LOGS DIALOG<br>UNABLE TO LOAD ALL PROCESSES LOGS FROM SERVER");
        });

        return true;
    }

    ProcessesLogsController.prototype.getProcessDuration = function (oProcess) {
        //time by server
        var oStartTime = new Date(oProcess.operationDate);
        var oEndTime = new Date(oProcess.operationEndDate);
        //pick time
        var result =  Math.abs(oEndTime-oStartTime);
        //approximate result
        var seconds = Math.ceil(result / 1000);

        if(utilsIsObjectNullOrUndefined(seconds) || seconds < 0) seconds = 0;

        var oDate = new Date(1970, 0, 1);
        oDate.setSeconds(0 + seconds);

        return oDate;
    };

    ProcessesLogsController.prototype.generateFile = function(sText)
    {
        var textFile = null;
        var sType = 'text/plain';
        textFile = utilsMakeFile(sText,textFile,sType);
        return textFile;
    };

    ProcessesLogsController.prototype.makeStringLogFile = function()
    {
        if(utilsIsObjectNullOrUndefined(this.m_aoProcessesLogs) === true)
            return null;
        // m_aoProcessesLogs
        var iNumberOfProcessesLogs = this.m_aoProcessesLogs.length;
        var sText = "";
        for(var iIndexProcessLog = 0; iIndexProcessLog < iNumberOfProcessesLogs; iIndexProcessLog++)
        {
            // sText += this.m_aoProcessesLogs[iIndexProcessLog] + "/n";
            var sOperationDate = this.m_aoProcessesLogs[iIndexProcessLog].operationDate;
            var sFileSize = this.m_aoProcessesLogs[iIndexProcessLog].fileSize;
            var sOperationEndDate = this.m_aoProcessesLogs[iIndexProcessLog].operationEndDate;
            var sOperationType = this.m_aoProcessesLogs[iIndexProcessLog].operationType;
            var sPid = this.m_aoProcessesLogs[iIndexProcessLog].pid;
            // var sProcessObjId = this.m_aoProcessesLogs[iIndexProcessLog].processObjId;
            var sProductName = this.m_aoProcessesLogs[iIndexProcessLog].productName;
            var sProgressPerc = this.m_aoProcessesLogs[iIndexProcessLog].progressPerc;
            var sStatus = this.m_aoProcessesLogs[iIndexProcessLog].status;
            var sUserId = this.m_aoProcessesLogs[iIndexProcessLog].userId;


            sText += iIndexProcessLog +") "+ "Id: "+ sPid +",Product Name: "+sProductName +",Operation Type: " + sOperationType +
                ",User: "+ sUserId + ",Status: " + sStatus + ",Progress: " + sProgressPerc +"%" +
                ",Operation date: "+ sOperationDate +",Operation end date: "+ sOperationEndDate + ",File size: " + sFileSize + "\r\n";
        };

        return sText;
    }

    ProcessesLogsController.prototype.generateLogFile = function()
    {
        var sText = this.makeStringLogFile();
        var oFile = this.generateFile(sText)
        return oFile;
    };

    ProcessesLogsController.$inject = [
        '$scope',
        'close',
        'ProcessesLaunchedService',
        'ConstantsService',
        // 'extras',
    ];
    return ProcessesLogsController;
})();
