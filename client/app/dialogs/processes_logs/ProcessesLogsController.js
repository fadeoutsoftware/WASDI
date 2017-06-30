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

    }

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
    }

    ProcessesLogsController.$inject = [
        '$scope',
        'close',
        'ProcessesLaunchedService',
        'ConstantsService',
        // 'extras',
    ];
    return ProcessesLogsController;
})();
