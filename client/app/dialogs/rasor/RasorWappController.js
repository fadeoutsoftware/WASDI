/**
 * Created by a.corrado on 24/05/2017.
 */



var RasorWappController = (function() {

    function RasorWappController($scope, oClose,oExtras,oAuthService,oConstantsService,oCatalogService,$window, oProcessorService, $interval, oProcessesLaunchedService) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oAuthService = oAuthService;
        this.m_oCatalogService = oCatalogService;
        this.m_oConstantsService = oConstantsService;
        this.m_oWindow = $window;
        this.m_aoProducts = this.m_oExtras.products;
        this.m_sResultFromServer = "";
        this.m_oSelectedProduct = null;
        this.m_oProcessorService = oProcessorService;
        this.m_oInterval = $interval;
        this.m_oProcessesLaunchedService = oProcessesLaunchedService;
        this.m_bIsRunning = false;

        if(utilsIsObjectNullOrUndefined(this.m_aoProducts) === false)
        {
            this.m_oSelectedProduct = this.m_aoProducts[0];
        }

        $scope.close = function(result) {
            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };

    }

    RasorWappController.prototype.redirectToRasorWebSite = function(){
        this.m_oWindow.open('http://www.rasor.eu/rasor/', '_blank');
    };

    RasorWappController.prototype.checkProcessResult = function(sRasorProcessId,oController) {
        console.log('---------------------------------CHIEDI RISULTATO ' + sRasorProcessId);

        var oLinkToController = oController;

        oLinkToController.m_oProcessesLaunchedService.getProcessWorkspaceById(sRasorProcessId).success(function (data) {
            if(utilsIsObjectNullOrUndefined(data) == false)
            {
                if (data.status == 'DONE') {
                    console.log('---------------------------------Run Rasor - DONE = ' + data.payload);
                    oLinkToController.m_bIsRunning = false;

                    var oResult = JSON.parse(data.payload);
                    oLinkToController.m_sResultFromServer = "" + parseInt(oResult.pop) + " Persone";

                    var oDialog = utilsVexDialogAlertBottomRightCorner("RASOR HUMAN IMPACT<br>CALCULATION DONE ["+ parseInt(oResult.pop)+"]");
                    utilsVexCloseDialogAfter(4000,oDialog);
                }
                else if (data.status == 'STOPPED') {
                    console.log('---------------------------------Run Rasor - STOPPED');
                    oLinkToController.m_sResultFromServer = "RASOR has been Stopped by the User";
                    oLinkToController.m_bIsRunning = false;
                }
                else if (data.status == 'ERROR') {
                    console.log('---------------------------------Run Rasor - ERROR');
                    oLinkToController.m_sResultFromServer = "There was an Error running RASOR";
                    oLinkToController.m_bIsRunning = false;
                }
                else if (data.status == 'WAITING') {
                    console.log('---------------------------------Run Rasor - WAITING');
                    oLinkToController.m_sResultFromServer = "RASOR App is waiting to start";
                    oLinkToController.m_oInterval(oLinkToController.checkProcessResult,1000,1,true,data.processObjId, oLinkToController);
                }
                else if (data.status == 'RUNNING') {
                    console.log('---------------------------------Run Rasor - RUNNING');
                    oLinkToController.m_sResultFromServer = "RASOR App is Running";
                    oLinkToController.m_oInterval(oLinkToController.checkProcessResult,1000,1,true,data.processObjId, oLinkToController);
                }
                else if (data.status == 'CREATED') {
                    console.log('---------------------------------Run Rasor - CREATED');
                    oLinkToController.m_sResultFromServer = "RASOR App Created";
                    oLinkToController.m_oInterval(oLinkToController.checkProcessResult,1000,1,true,data.processObjId, oLinkToController);
                }
                else {
                    console.log('---------------------------------Run Rasor - UNKNOWN ' + data.status);
                    oLinkToController.m_oInterval(oLinkToController.checkProcessResult,1000,1,true,data.processObjId, oLinkToController);
                }
            }
            else
            {
                console.log('---------------------------------Run Rasor - DATA NuLL');

                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR RUNNING RASOR WAPP");
            }
        }).error(function (error) {
            console.log('---------------------------------Run Rasor - ERROR');
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR RUNNING RASOR WAPP");
            oController.cleanAllExecuteWorkflowFields();
        });
    };

    RasorWappController.prototype.runRasor = function() {
        console.log("RUN  RASOR WAPP" );

        var oController = this;

        var sWorkspaceId = this.m_oConstantsService.getActiveWorkspace().name;
        var sFile = this.m_oSelectedProduct.fileName;
        var sJSON = '{"file":"'+ sFile+'","workspace":"'+sWorkspaceId + '"}';

        this.m_sResultFromServer = "RASOR App is waiting to start";
        this.m_bIsRunning = true;

        this.m_oProcessorService.runProcessor('rasor2', sJSON).success(function (data) {
            if(utilsIsObjectNullOrUndefined(data) == false)
            {
                var sRasorProcessId = data.processingIdentifier;
                console.log('Run Rasor - Proc ID = ' + sRasorProcessId);
                oController.m_oInterval(oController.checkProcessResult,1000,1,true,sRasorProcessId,oController);
            }
            else
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR RUNNING WAPP");
            }
        }).error(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR RUNNING WAPP");
            oController.cleanAllExecuteWorkflowFields();
            //oController.m_sResultFromServer = "RASOR App is waiting to start";
            oController.m_bIsRunning = false;
        });

    };

    RasorWappController.$inject = [
        '$scope',
        'close',
        'extras',
        'AuthService',
        'ConstantsService',
        'CatalogService',
        '$window',
        'ProcessorService',
        '$interval',
        'ProcessesLaunchedService'
    ];

    return RasorWappController;
})();
