 var ProcessorLogsController = (function() {

    function ProcessorLogsController($scope, oClose,oExtras,oAuthService,oConstantsService,oCatalogService,
                                              oProcessorService,$interval, oProcessWorkspaceService) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oAuthService = oAuthService;
        this.m_oCatalogService = oCatalogService;
        this.m_oConstantsService = oConstantsService;
        this.m_oProcess = oExtras.process;
        this.m_oProcessorService = oProcessorService;
        this.m_oProcessWorkspaceService = oProcessWorkspaceService;
        this.m_oInterval = $interval;
        this.m_aoLogs = [];
        this.m_sSearch="";
        this.m_bSortReverse = true;
        this.m_sSortByColum = "Date";
        this.m_iCurrentPage = 1;
        this.m_iNumberOfLogs = 0;
        this.m_iNumberOfLogsPerPage = 10;
        this.m_oController = this;


        var oController = this;

        $scope.close = function(result) {
            console.log("processor_logs: close function")
            // Make sure that the interval is destroyed too
            oController.stopTick(oController);
            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };

        // Get the log count
        this.getLogsCount(this.m_oProcess.processObjId,this.getCountLogsANDLogsCallback);

        // Start the refresh timer
        this.m_oTick = this.startTick(this.m_oProcess.status);

        $scope.$on('$destroy', function() {
            // Make sure that the interval is destroyed too
            oController.stopTick(oController);
        });


        $scope.$watch('m_oController.m_iCurrentPage', function(newValue, oldValue, scope) {

            if(utilsIsObjectNullOrUndefined(newValue) === false && newValue >= 0)
            {
                oController.getLogsCount(oController.m_oProcess.processObjId,oController.getCountLogsANDLogsCallback);
            }
        });
    }
    /*************** METHODS ***************/
    ProcessorLogsController.prototype.sortBy = function(propertyName) {
        this.m_bSortReverse = (this.m_sSortByColum === propertyName) ? !this.m_bSortReverse : false;
        this.m_sSortByColum = propertyName;
    };

    ProcessorLogsController.prototype.getProcessWorkspaceId = function() {
         if (this.m_oProcess.processObjId != null) {
             return this.m_oProcess.processObjId;
         }
         else {
             return "";
         }
     };

     ProcessorLogsController.prototype.getProcessorType = function() {
         if (this.m_oProcess.operationType != null) {
             return this.m_oProcess.operationType;
         }
         else {
             return "";
         }
     };

     ProcessorLogsController.prototype.getProcessorName = function() {
         if (this.m_oProcess.productName != null) {
             return this.m_oProcess.productName;
         }
         else {
             return "";
         }
     };

     ProcessorLogsController.prototype.getProcessorStatus = function() {
         if (this.m_oProcess.status != null) {
             return this.m_oProcess.status;
         }
         else {
             return "";
         }
     };


     ProcessorLogsController.prototype.getPayload = function() {
         if (this.m_oProcess.payload != null) {
             utilsVexDialogBigAlertTop(this.m_oProcess.payload,null);
         }
         else {
             utilsVexDialogBigAlertTop("Payload not available",null);
         }
     };

     ProcessorLogsController.prototype.copyPayload = function() {

         if (this.m_oProcess.payload != null) {
             // Create new element
             var el = document.createElement('textarea');
             // Set value (string to be copied)
             el.value = this.m_oProcess.payload;
             // Set non-editable to avoid focus and move outside of view
             el.setAttribute('readonly', '');
             el.style = {position: 'absolute', left: '-9999px'};
             document.body.appendChild(el);
             // Select text inside element
             el.select();
             // Copy text to clipboard
             document.execCommand('copy');
             // Remove temporary element
             document.body.removeChild(el);

             var oDialog = utilsVexDialogAlertBottomRightCorner("PAYLOAD COPIED<br>READY");
             utilsVexCloseDialogAfter(4000,oDialog);
         }
         else {
             var oDialog = utilsVexDialogAlertBottomRightCorner("NO PAYLOAD TO COPY<br>READY");
             utilsVexCloseDialogAfter(4000,oDialog);
         }
     };

     ProcessorLogsController.prototype.stopTick=function(oController) {
        console.log("processor_logs: stopTick")
         if (angular.isDefined(oController.m_oTick)) {
            console.log("processor_logs: m_oTick is defined")
             oController.m_oInterval.cancel(oController.m_oTick);
             oController.m_oTick = undefined;
         }
     }

     ProcessorLogsController.prototype.startTick=function(sStatus){
        if( ( utilsIsStrNullOrEmpty(sStatus) === true ) || ( sStatus !== "RUNNING" ) )
        {
            return undefined;
        }
        var oController=this;

        var oTick = this.m_oInterval(function () {
            console.log("Update WAPPS logs");
            oController.getLogsCount(oController.m_oProcess.processObjId,oController.getCountLogsANDLogsCallback);

            sStatus = oController.m_oProcess.status;

            if( ( sStatus === "STOPPED" ) || ( sStatus === "ERROR" ) || ( sStatus === "DONE" ))
            {
                oController.stopTick(oController);
            }

        }, 5000);

        return oTick;
    }

    ProcessorLogsController.prototype.isCaretIconVisible = function(sColumnName,sCaretName)
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

                return true;

            }
            else
            {
                // sCaretName === fa-caret-up
                if(this.m_bSortReverse === false)
                {
                    return true;
                }

                return false;

            }
        }
        else
        {
            return false;
        }
    };

    ProcessorLogsController.prototype.getCountLogsANDLogsCallback = function(data, status,oController)
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

                 oController.getPaginatedLogs(oController.m_oProcess.processObjId,iFirstRow,iLastRow);
             }
         }
     }

     ProcessorLogsController.prototype.getLogsCount = function(oProcessObjId, oCallback)
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

         this.m_oProcessorService.getLogsCount(oProcessObjId).then(function (data, status) {
             oCallback(data.data, status,oController);
         },function (data,status) {
             utilsVexDialogAlertTop('GURU MEDITATION<br>ERROR READING PROCESSOR LOGS');
         });

         this.m_oProcessWorkspaceService.getProcessWorkspaceById(oProcessObjId).then(function (data, status) {
            oController.m_oProcess = data.data;
        },function (data,status) {
            utilsVexDialogAlertTop('GURU MEDITATION<br>ERROR REFRESHING PROCESSOR STATUS');
        });

         return true;
     };


     ProcessorLogsController.prototype.getPaginatedLogs = function(oProcessObjId, iStartRow, iEndRow)
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
         this.m_oProcessorService.getPaginatedLogs(oProcessObjId,iStartRow,iEndRow).then(function (data, status) {
             if (data.data != null)
             {
                 if (data.data != undefined)
                 {
                     oController.m_aoLogs = data.data;
                 }
             }
         },function (data,status) {
             //alert('error');
             utilsVexDialogAlertTop('GURU MEDITATION<br>ERROR IN GET LOGS');
         });
         return true;
     };

     ProcessorLogsController.prototype.getOperationDescription = function() {
        return utilsConvertOperationToDescription(this.m_oProcess);
    }

    ProcessorLogsController.$inject = [
        '$scope',
        'close',
        'extras',
        'AuthService',
        'ConstantsService',
        'CatalogService',
        'ProcessorService',
        '$interval',
        'ProcessWorkspaceService'
    ];
    return ProcessorLogsController;
})();
