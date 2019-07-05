/**
 * Created by a.corrado on 09/06/2017.
 */

var ProcessesLogsController = (function() {

    function ProcessesLogsController($scope, oClose, oProcessesLaunchedService,oConstantsService,oModalService,oProcessorService) {//,oExtras
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oModalService = oModalService;
        this.m_oProcessorService = oProcessorService;
        this.hasError = false;
        this.m_oProcessesLaunchedService = oProcessesLaunchedService;
        this.m_aoProcessesLogs = [];
        this.filterTable = "";
        this.m_bAreProcessesLoaded = false;


        this.m_iNumberOfProcessForRequest = 40;
        this.m_iFirstProcess = 0;
        this.m_iLastProcess = this.m_iNumberOfProcessForRequest;
        // this.m_oExtrs= oExtras;
        //$scope.close = oClose;
        this.m_oConstantsService = oConstantsService;
        this.isLoadMoreButtonClickable = true;

        if(_.isNil(this.m_oConstantsService.getActiveWorkspace()) == false)
        {
            this.m_sActiveWorkspaceId = this.m_oConstantsService.getActiveWorkspace().workspaceId;

            $scope.close = function(result) {
                oClose(result, 500); // close, but give 500ms for bootstrap to animate
            };

            this.getAllProcessesLogs();
            this.m_sHrefLogFile = "";
        }
        else{
            this.hasError = true;
            this.m_sActiveWorkspaceId = null;
        }


;    }

    /**
     * getAllProcessesLogs
     * @returns {boolean}
     */
    ProcessesLogsController.prototype.getAllProcessesLogs = function () {
        var oController = this;

        if(utilsIsObjectNullOrUndefined(this.m_sActiveWorkspaceId)=== true)
        {
            return false;
        }

        this.m_bAreProcessesLoaded = false;
        this.m_oProcessesLaunchedService.getAllProcessesFromServer(this.m_sActiveWorkspaceId,this.m_iFirstProcess,this.m_iLastProcess).success(function (data, status)
        {
            if(!utilsIsObjectNullOrUndefined(data))
            {
                if(data.length > 0){
                    oController.m_aoProcessesLogs = oController.m_aoProcessesLogs.concat(data);
                    oController.m_sHrefLogFile = oController.generateLogFile();
                    oController.calculateNextListOfProcess();
                }
                else
                {
                    oController.isLoadMoreButtonClickable = false;
                }

                if(data.length < oController.m_iNumberOfProcessForRequest )
                {
                    //there aren't enough processes for other requests so you can't load more processes
                    oController.isLoadMoreButtonClickable = false;

                }
                oController.m_bAreProcessesLoaded = true;
            }
        }).error(function (data,status)
        {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN PROCESSES LOGS DIALOG<br>UNABLE TO LOAD ALL PROCESSES LOGS FROM SERVER");
            oController.m_bAreProcessesLoaded = true;
        });

        return true;
    }

    ProcessesLogsController.prototype.calculateNextListOfProcess = function(){
        this.m_iFirstProcess = this.m_iFirstProcess + this.m_iNumberOfProcessForRequest;
        this.m_iLastProcess = this.m_iLastProcess + this.m_iNumberOfProcessForRequest;
    };

    ProcessesLogsController.prototype.getProcessDuration = function (oProcess) {
        //time by server
        var oStartTime = new Date(oProcess.operationStartDate);

        var oEndTime = new Date(oProcess.operationEndDate);
        if( utilsIsValidDate(oEndTime) === false )
        {
            oEndTime = new Date();
        }
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


            sText += iIndexProcessLog +") "+ "Id: "+ sPid +",Product Name: "+sProductName +",Operation Type: " + sOperationType +
                ",User: "+ sUserId + ",Status: " + sStatus + ",Progress: " + sProgressPerc +"%" +
                ",Operation date: "+ sOperationDate +",Operation end date: "+ sOperationEndDate + ",File size: " + sFileSize + "\r\n";
        };

        return sText;
    }
    /**
     *
     */
    ProcessesLogsController.prototype.generateLogFile = function()
    {
        var sText = this.makeStringLogFile();
        var oFile = this.generateFile(sText)
        return oFile;
    };

    ProcessesLogsController.prototype.openErrorLogsDialog = function(oProcess)
    {

        var oController = this;

       if(utilsIsObjectNullOrUndefined(oProcess) === true)
       {
           return false;
       }
        oController.m_oModalService.showModal({
            templateUrl: "dialogs/process_error_logs_dialog/ProcessErrorLogsDialogView.html",
            controller: "ProcessErrorLogsDialogController",
            inputs: {
                extras: {
                     process:oProcess,
                }
            }
        }).then(function (modal) {
            modal.element.modal();
            modal.close.then(function(oResult){

            });
        });
       return true;
    };



    ProcessesLogsController.$inject = [
        '$scope',
        'close',
        'ProcessesLaunchedService',
        'ConstantsService',
        'ModalService',
        'ProcessorService'
        // 'extras',
    ];
    return ProcessesLogsController;
})();
