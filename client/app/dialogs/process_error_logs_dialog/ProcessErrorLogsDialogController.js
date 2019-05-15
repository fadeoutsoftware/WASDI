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
        this.m_sSearch="";
        this.m_bSortReverse = true;
        this.m_sSortByColum = "Date";
        this.m_iCurrentPage = 1;
        // this.m_iTotalPages = 0;
        this.m_iNumberOfLogs = 0;
        this.m_iNumberOfLogsPerPage = 10;

        var oController = this;
        $scope.close = function(result) {
            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };
        //this.getAllErrorLogs(this.m_oProcess.processObjId);

        this.getLogs(this.m_oProcess.processObjId,0,this.m_iNumberOfLogsPerPage);
        this.getCountLogs(this.m_oProcess.processObjId);
        this.m_oTick = this.startTick(this.m_oProcess.status);

        $scope.$on('$destroy', function() {
            // Make sure that the interval is destroyed too
            if (angular.isDefined($scope.m_oController.m_oTick)) {
                $interval.cancel($scope.m_oController.m_oTick);
                $scope.m_oController.m_oTick = undefined;
            }
        });
        var oController = this;
        $scope.$watch('m_oController.m_iCurrentPage', function(newValue, oldValue, scope) {
            // $scope.m_oController.m_aoProcessesRunning = [];
            // $scope.m_oController.m_bIsEditModelWorkspaceNameActive = false;
            if(utilsIsObjectNullOrUndefined(newValue) === false && newValue >= 0)
            {
                var iLastRow = oController.m_iNumberOfLogsPerPage * newValue;
                var iFirstRow = oController.m_iNumberOfLogsPerPage * (newValue - 1);
                oController.getLogs(oController.m_oProcess.processObjId,iFirstRow,iLastRow);
                oController.getCountLogs(oController.m_oProcess.processObjId,oController.getCountLogsCallback);
                // if(newValue.name === "Untitled Workspace")
                // {
                //     $scope.m_oController.getWorkspacesInfo();
                //     $scope.m_oController.editModelWorkspaceNameSetTrue();
                // }
            }
        });
    }
    /*************** METHODS ***************/
    ProcessErrorLogsDialogController.prototype.sortBy = function(propertyName) {
        this.m_bSortReverse = (this.m_sSortByColum === propertyName) ? !this.m_bSortReverse : false;
        this.m_sSortByColum = propertyName;
    };

    // ProcessErrorLogsDialogController.prototype.getNumberOfPages = function(iNumberOfLogs){
    //
    //     if( (utilsIsObjectNullOrUndefined(this.m_iNumberOfLogsPerPage) === true)|| ( this.m_iNumberOfLogsPerPage === 0 ))
    //     {
    //         return 0;
    //     }
    //     if( utilsIsObjectNullOrUndefined(iNumberOfLogs) === true )
    //     {
    //         return 0;
    //     }
    //     var iNumberOfPages = iNumberOfLogs/this.m_iNumberOfLogsPerPage;
    //
    //     //Math.ceil(x) returns the value of x rounded up to its nearest integer
    //     iNumberOfPages = Math.ceil(iNumberOfPages);
    //     return iNumberOfPages;
    // };

    ProcessErrorLogsDialogController.prototype.startTick=function(sStatus){
        if( ( utilsIsStrNullOrEmpty(sStatus) === true ) || ( sStatus !== "RUNNING" ) )
        {
            return undefined;
        }
        var oController=this;
        var oTick = this.m_oInterval(function () {
            console.log("Update logs");
            var iLastRow = oController.m_iNumberOfLogsPerPage * oController.m_iCurrentPage;
            var iFirstRow = oController.m_iNumberOfLogsPerPage * (oController.m_iCurrentPage - 1);
            oController.getLogs(oController.m_oProcess.processObjId,iFirstRow,iLastRow);
            oController.getCountLogs(oController.m_oProcess.processObjId,oController.getCountLogsCallback);

        }, 5000);

        return oTick;
    }

    //  ProcessErrorLogsDialogController.prototype.getAllErrorLogs = function(oProcessObjId){
    //     // oProcessObjId = "fb99a0b1-93cb-40ab-9d44-9701a7b11b9b";//TEST
    //     if(utilsIsObjectNullOrUndefined(oProcessObjId) === true)
    //     {
    //         return false;
    //     }
    //     var oController = this;
    //     this.m_oProcessorService.getAllErrorLogs(oProcessObjId).success(function (data, status)
    //     {
    //         if(!utilsIsObjectNullOrUndefined(data))
    //         {
    //             oController.m_aoLogs = data;
    //         }
    //     }).error(function (data,status)
    //     {
    //         utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN PROCESS LOGS DIALOG<br>UNABLE TO LOAD ALL PROCESS LOGS FROM SERVER");
    //     });
    //
    //     return true;
    // }

    ProcessErrorLogsDialogController.prototype.isCaretIconVisible = function(sColumnName,sCaretName)
    {
        if(utilsIsStrNullOrEmpty(sColumnName) === true || utilsIsStrNullOrEmpty(sCaretName) === true)
        {
            return false;
        }

        if(this.m_sSortByColum === sColumnName)
        {
            if(sCaretName === "fa-caret-down"  )
            {
                if(this.m_bSortReverse === false)
                {
                    return false;
                }
                else
                {
                    return true;
                }
            }
            else
            {
                // sCaretName === fa-caret-up
                if(this.m_bSortReverse === false)
                {
                    return true;
                }
                else
                {
                    return false;
                }
            }
        }
        else
        {
            return false;
        }


    }
     ProcessErrorLogsDialogController.prototype.getCountLogsCallback = function(data, status,oController)
     {
         if (data != null)
         {
             if (data != undefined)
             {
                 oController.m_iNumberOfLogs = data;
                 // oController.m_iTotalPages = oController.getNumberOfPages(data);
             }
         }
     }

     // ProcessErrorLogsDialogController.prototype.getCountLogs = function(oProcessObjId, oCallback)
     // {
     //     if(utilsIsObjectNullOrUndefined(oProcessObjId) === true)
     //     {
     //         return false;
     //     }
     //     // oProcessObjId = "fb99a0b1-93cb-40ab-9d44-9701a7b11b9b";
     //
     //     var oController = this;
     //     this.m_oProcessorService.getCountLogs(oProcessObjId).success(function (data, status) {
     //         oCallback(data, status,oController);
     //     }).error(function (data,status) {
     //         //alert('error');
     //         utilsVexDialogAlertTop('GURU MEDITATION<br>ERROR IN GET COUNT LOGS');
     //     });
     //     return true;
     //
     // };

    ProcessErrorLogsDialogController.prototype.getCountLogs = function(oProcessObjId)
     {
         if(utilsIsObjectNullOrUndefined(oProcessObjId) === true)
         {
             return false;
         }
         // oProcessObjId = "fb99a0b1-93cb-40ab-9d44-9701a7b11b9b";

         var oController = this;
         this.m_oProcessorService.getCountLogs(oProcessObjId).success(function (data, status) {
             if (data != null)
             {
                 if (data != undefined)
                 {
                     oController.m_iNumberOfLogs = data;
                     // oController.m_iTotalPages = oController.getNumberOfPages(data);
                 }
             }
         }).error(function (data,status) {
             //alert('error');
             utilsVexDialogAlertTop('GURU MEDITATION<br>ERROR IN GET COUNT LOGS');
         });
         return true;

     };

     ProcessErrorLogsDialogController.prototype.getLogs = function(oProcessObjId,iStartRow,iEndRow)
     {
         if(utilsIsObjectNullOrUndefined(oProcessObjId) === true)
         {
             return false;
         }
         if(utilsIsObjectNullOrUndefined(iStartRow) === true )
         {
             iStartRow ="";
         }
         if(utilsIsObjectNullOrUndefined(iEndRow) === true )
         {
             iEndRow = "";
         }

         // oProcessObjId = "fb99a0b1-93cb-40ab-9d44-9701a7b11b9b";
         var oController = this;
         this.m_oProcessorService.getLogs(oProcessObjId,iStartRow,iEndRow).success(function (data, status) {
             if (data != null)
             {
                 if (data != undefined)
                 {
                     oController.m_aoLogs = data;

                 }
             }
         }).error(function (data,status) {
             //alert('error');
             utilsVexDialogAlertTop('GURU MEDITATION<br>ERROR IN GET LOGS');
         });
         return true;
     };

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
