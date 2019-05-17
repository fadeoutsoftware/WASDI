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
        this.m_iNumberOfLogs = 0;
        this.m_iNumberOfLogsPerPage = 10;


        var oController = this;

        $scope.close = function(result) {
            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };

        // Get the log count
        this.getCountLogs(this.m_oProcess.processObjId,this.getCountLogsANDLogsCallback);

        // Start the refresh timer
        this.m_oTick = this.startTick(this.m_oProcess.status);

        $scope.$on('$destroy', function() {
            // Make sure that the interval is destroyed too
            if (angular.isDefined($scope.m_oController.m_oTick)) {
                $interval.cancel($scope.m_oController.m_oTick);
                $scope.m_oController.m_oTick = undefined;
            }
        });


        $scope.$watch('m_oController.m_iCurrentPage', function(newValue, oldValue, scope) {

            if(utilsIsObjectNullOrUndefined(newValue) === false && newValue >= 0)
            {
                oController.getCountLogs(oController.m_oProcess.processObjId,oController.getCountLogsANDLogsCallback);
            }
        });
    }
    /*************** METHODS ***************/
    ProcessErrorLogsDialogController.prototype.sortBy = function(propertyName) {
        this.m_bSortReverse = (this.m_sSortByColum === propertyName) ? !this.m_bSortReverse : false;
        this.m_sSortByColum = propertyName;
    };

     ProcessErrorLogsDialogController.prototype.getProcessWorkspaceId = function() {
         if (this.m_oProcess.processObjId != null) {
             return this.m_oProcess.processObjId;
         }
         else {
             return "";
         }
     };

     ProcessErrorLogsDialogController.prototype.getProcessorType = function() {
         if (this.m_oProcess.operationType != null) {
             return this.m_oProcess.operationType;
         }
         else {
             return "";
         }
     };

     ProcessErrorLogsDialogController.prototype.getProcessorName = function() {
         if (this.m_oProcess.productName != null) {
             return this.m_oProcess.productName;
         }
         else {
             return "";
         }
     };

     ProcessErrorLogsDialogController.prototype.getProcessorStatus = function() {
         if (this.m_oProcess.status != null) {
             return this.m_oProcess.status;
         }
         else {
             return "";
         }
     };


     ProcessErrorLogsDialogController.prototype.getPayload = function() {
         if (this.m_oProcess.payload != null) {
             utilsVexDialogBigAlertTop(this.m_oProcess.payload,null);
         }
         else {
             utilsVexDialogBigAlertTop("Payload not available",null);
         }
     };

     ProcessErrorLogsDialogController.prototype.startTick=function(sStatus){
        if( ( utilsIsStrNullOrEmpty(sStatus) === true ) || ( sStatus !== "RUNNING" ) )
        {
            return undefined;
        }
        var oController=this;

        var oTick = this.m_oInterval(function () {
            console.log("Update logs");
            oController.getCountLogs(oController.m_oProcess.processObjId,oController.getCountLogsANDLogsCallback);

        }, 5000);

        return oTick;
    }

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


    };

     ProcessErrorLogsDialogController.prototype.getCountLogsANDLogsCallback = function(data, status,oController)
     {
         if (data != null)
         {
             if (data != undefined)
             {
                 oController.m_iNumberOfLogs = data;

                 var iFirstRow = oController.m_iNumberOfLogs - (oController.m_iCurrentPage * oController.m_iNumberOfLogsPerPage);
                 var iLastRow = iFirstRow + oController.m_iNumberOfLogsPerPage;
                 if(iFirstRow < 0)
                 {
                     iFirstRow = 0;
                 }

                 oController.getLogs(oController.m_oProcess.processObjId,iFirstRow,iLastRow);
             }
         }
     }

     ProcessErrorLogsDialogController.prototype.getCountLogs = function(oProcessObjId, oCallback)
     {
         if(utilsIsObjectNullOrUndefined(oProcessObjId) === true)
         {
             return false;
         }
         if(utilsIsObjectNullOrUndefined(oCallback) === true)
         {
             return false;
         }

         var oController = this;

         this.m_oProcessorService.getCountLogs(oProcessObjId).success(function (data, status) {
             oCallback(data, status,oController);
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
