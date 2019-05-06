 var ProcessErrorLogsDialogController = (function() {

    function ProcessErrorLogsDialogController($scope, oClose,oExtras,oAuthService,oConstantsService,oCatalogService,
                                              oProcessorService,$interval) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oAuthService = oAuthService;
        this.m_oCatalogService = oCatalogService;
        this.m_oConstantsService = oConstantsService;
        this.m_oProcess = oExtras.process;
        this.m_oProcessorService = oProcessorService;
        this.m_oInterval = $interval;
        this.m_aoLogs = [];
        var oController = this;
        $scope.close = function(result) {
            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };
        this.getAllErrorLogs(this.m_oProcess.processObjId);


        this.m_oTick = this.startTick(this.m_oProcess.status);

        $scope.$on('$destroy', function() {
            // Make sure that the interval is destroyed too
            if (angular.isDefined($scope.m_oController.m_oTick)) {
                $interval.cancel($scope.m_oController.m_oTick);
                $scope.m_oController.m_oTick = undefined;
            }
        });
    }
    /*************** METHODS ***************/
    ProcessErrorLogsDialogController.prototype.startTick=function(sStatus){
        if( ( utilsIsStrNullOrEmpty(sStatus) === true ) || ( sStatus !== "RUNNING" ) )
        {
            return undefined;
        }
        var oController=this;
        var oTick = this.m_oInterval(function () {
            console.log("Update logs");
            oController.getAllErrorLogs(oController.m_oProcess.processObjId);
            // $scope.m_oController.updatePositionsSatellites();
        }, 5000);

        return oTick;
    }

     ProcessErrorLogsDialogController.prototype.getAllErrorLogs = function(oProcessObjId){
         //oProcessObjId = "fb99a0b1-93cb-40ab-9d44-9701a7b11b9b";//TEST
        if(utilsIsObjectNullOrUndefined(oProcessObjId) === true)
        {
            return false;
        }
        var oController = this;
        this.m_oProcessorService.getAllErrorLogs(oProcessObjId).success(function (data, status)
        {
            if(!utilsIsObjectNullOrUndefined(data))
            {
                oController.m_aoLogs = data;
            }
        }).error(function (data,status)
        {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN PROCESS LOGS DIALOG<br>UNABLE TO LOAD ALL PROCESS LOGS FROM SERVER");
        });

        return true;
    }

    ProcessErrorLogsDialogController.$inject = [
        '$scope',
        'close',
        'extras',
        'AuthService',
        'ConstantsService',
        'CatalogService',
        'ProcessorService',
        '$interval'
    ];
    return ProcessErrorLogsDialogController;
})();
