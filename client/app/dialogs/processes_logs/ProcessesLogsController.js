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

            utilsVexDialogAlertTop("Error in processes logs dialog: it's impossible load all processes logs from server");
        });

        return true;
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
