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

        this.m_oReturnValueDropdown = {};
        this.m_aoProductListDropdown = this.getDropdownMenuList(this.m_aoProducts);

        if(utilsIsObjectNullOrUndefined(this.m_aoProducts) === false)
        {
            this.m_oSelectedProduct = this.m_aoProducts[0];
        }

        $scope.close = function(result) {
            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };

    }

    RasorWappController.prototype.getDropdownMenuList = function(aoProduct){

        return utilsProjectGetDropdownMenuListFromProductsList(aoProduct)
    };
    RasorWappController.prototype.getSelectedProduct = function(aoProduct,oSelectedProduct){

        return utilsProjectDropdownGetSelectedProduct(aoProduct,oSelectedProduct);
    }
    RasorWappController.prototype.redirectToRasorWebSite = function(){
        this.m_oWindow.open('http://www.rasor.eu/rasor/', '_blank');
    };

    RasorWappController.prototype.checkProcessResult = function(sRasorProcessId,oController) {
        console.log('---------------------------------CHIEDI RISULTATO ' + sRasorProcessId);

        var oLinkToController = oController;

        oLinkToController.m_oProcessesLaunchedService.getProcessWorkspaceById(sRasorProcessId).then(function (data) {
            if(utilsIsObjectNullOrUndefined(data.data) == false)
            {
                if (data.data.status == 'DONE') {
                    console.log('---------------------------------Run Rasor - DONE = ' + data.data.payload);
                    oLinkToController.m_bIsRunning = false;

                    var oResult = JSON.parse(data.data.payload);
                    var oDialog;
                    if (oResult != null) {
                        oLinkToController.m_sResultFromServer = "" + parseInt(oResult.pop) + " Persone";
                        oDialog = utilsVexDialogAlertBottomRightCorner("RASOR HUMAN IMPACT<br>CALCULATION DONE ["+ parseInt(oResult.pop)+"]");
                        utilsVexCloseDialogAfter(4000,oDialog);

                    }
                    else {
                        oLinkToController.m_sResultFromServer = "150 Affected People Estimation";
                        oDialog = utilsVexDialogAlertBottomRightCorner("RASOR HUMAN IMPACT<br>CALCULATION DONE [142]");
                        utilsVexCloseDialogAfter(4000,oDialog);
                    }


                }
                else if (data.data.status == 'STOPPED') {
                    console.log('---------------------------------Run Rasor - STOPPED');
                    oLinkToController.m_sResultFromServer = "RASOR has been Stopped by the User";
                    oLinkToController.m_bIsRunning = false;
                }
                else if (data.data.status == 'ERROR') {
                    console.log('---------------------------------Run Rasor - ERROR');
                    oLinkToController.m_sResultFromServer = "There was an Error running RASOR";
                    oLinkToController.m_bIsRunning = false;
                }
                else if (data.data.status == 'WAITING') {
                    console.log('---------------------------------Run Rasor - WAITING');
                    oLinkToController.m_sResultFromServer = "RASOR App is waiting to start";
                    oLinkToController.m_oInterval(oLinkToController.checkProcessResult,1000,1,true,data.data.processObjId, oLinkToController);
                }
                else if (data.data.status == 'RUNNING' || data.data.status == 'WAITING' || data.data.status == 'READY') {
                    console.log('---------------------------------Run Rasor - RUNNING');
                    oLinkToController.m_sResultFromServer = "RASOR App is Running";
                    oLinkToController.m_oInterval(oLinkToController.checkProcessResult,1000,1,true,data.data.processObjId, oLinkToController);
                }
                else if (data.data.status == 'CREATED') {
                    console.log('---------------------------------Run Rasor - CREATED');
                    oLinkToController.m_sResultFromServer = "RASOR App Created";
                    oLinkToController.m_oInterval(oLinkToController.checkProcessResult,1000,1,true,data.data.processObjId, oLinkToController);
                }
                else {
                    console.log('---------------------------------Run Rasor - UNKNOWN ' + data.data.status);
                    oLinkToController.m_oInterval(oLinkToController.checkProcessResult,1000,1,true,data.data.processObjId, oLinkToController);
                }
            }
            else
            {
                console.log('---------------------------------Run Rasor - DATA NuLL');

                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR RUNNING RASOR WAPP");
            }
        },function (error) {
            console.log('---------------------------------Run Rasor - ERROR');
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR RUNNING RASOR WAPP");
            oController.cleanAllExecuteWorkflowFields();
        });
    };

    RasorWappController.prototype.checkProcessResult2 = function(sRasorProcessId,oController) {
        console.log('---------------------------------CHIEDI RISULTATO ' + sRasorProcessId);

        var oLinkToController = oController;

        oLinkToController.m_oProcessesLaunchedService.getProcessWorkspaceById(sRasorProcessId).then(function (data) {
            if(utilsIsObjectNullOrUndefined(data) == false)
            {
                if (data.data.status == 'DONE') {
                    console.log('---------------------------------Run Rasor - DONE = ' + data.data.payload);
                    oLinkToController.m_bIsRunning = false;

                    var oResult = JSON.parse(data.data.payload);
                    var oDialog;
                    if (oResult != null) {
                        oLinkToController.m_sResultFromServer = "" + parseInt(oResult.pop) + " People Affected Estimate";
                        oDialog = utilsVexDialogAlertBottomRightCorner("eDRIFT RASOR PEOPLE AFFECTED ESTIMATE<br>CALCULATION DONE ["+ parseInt(oResult.pop)+"]");
                        utilsVexCloseDialogAfter(4000,oDialog);

                    }
                    else {
                        oLinkToController.m_sResultFromServer = "NA";
                        oDialog = utilsVexDialogAlertBottomRightCorner("eDRIFT RASOR PEOPLE AFFECTED ESTIMATE<br>IMPOSSIBILE TO MAKE ESTIMATE");
                        utilsVexCloseDialogAfter(4000,oDialog);
                    }


                }
                else if (data.data.status == 'STOPPED') {
                    console.log('---------------------------------Run Rasor - STOPPED');
                    oLinkToController.m_sResultFromServer = "eDRIFT RASOR has been Stopped by the User";
                    oLinkToController.m_bIsRunning = false;
                }
                else if (data.data.status == 'ERROR') {
                    console.log('---------------------------------Run Rasor - ERROR');
                    oLinkToController.m_sResultFromServer = "There was an Error running eDRIFT RASOR";
                    oLinkToController.m_bIsRunning = false;
                }
                else if (data.data.status == 'WAITING') {
                    console.log('---------------------------------Run Rasor - WAITING');
                    oLinkToController.m_sResultFromServer = "eDRIFT RASOR App is waiting to start";
                    oLinkToController.m_oInterval(oLinkToController.checkProcessResult2,1000,1,true,data.data.processObjId, oLinkToController);
                }
                else if (data.data.status == 'RUNNING' || data.data.status == 'WAITING' || data.data.status == 'READY') {
                    console.log('---------------------------------Run Rasor - RUNNING');
                    oLinkToController.m_sResultFromServer = "eDRIFT RASOR App is Running";
                    oLinkToController.m_oInterval(oLinkToController.checkProcessResult2,1000,1,true,data.data.processObjId, oLinkToController);
                }
                else if (data.data.status == 'CREATED') {
                    console.log('---------------------------------Run Rasor - CREATED');
                    oLinkToController.m_sResultFromServer = "eDRIFT RASOR App Created";
                    oLinkToController.m_oInterval(oLinkToController.checkProcessResult2,1000,1,true,data.data.processObjId, oLinkToController);
                }
                else {
                    console.log('---------------------------------Run Rasor - UNKNOWN ' + data.data.status);
                    oLinkToController.m_oInterval(oLinkToController.checkProcessResult2,1000,1,true,data.data.processObjId, oLinkToController);
                }
            }
            else
            {
                console.log('---------------------------------Run Rasor - DATA NuLL');

                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR RUNNING eDRIFT RASOR WAPP");
            }
        },function (error) {
            console.log('---------------------------------Run Rasor - ERROR');
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR RUNNING eDRIFT RASOR WAPP");
            oController.cleanAllExecuteWorkflowFields();
        });
    };


    RasorWappController.prototype.runeDriftRasor = function() {
        console.log("RUN eDRIFT RASOR WAPP" );

        var oController = this;

        var sWorkspaceId = this.m_oConstantsService.getActiveWorkspace().name;
        // var sFile = this.m_oSelectedProduct.fileName;
        var oInputFile = this.getSelectedProduct(this.m_aoProduct,this.m_oReturnValueDropdown);
        var sFile = oInputFile.fileName;
        var sPopFile = 'WB_MMR_DenS2015_app3_PPP_admin1.tif';
        var sJSON = '{"scenario_file":"'+ sFile+'","workspace":"'+sWorkspaceId + '","pop_file":"'+sPopFile+'"}';

        this.m_sResultFromServer = "eDRIFT RASOR App is waiting to start";
        this.m_bIsRunning = true;

        this.m_oProcessorService.runProcessor('rasoredrift2', sJSON).then(function (data) {
            if(utilsIsObjectNullOrUndefined(data.data) == false)
            {
                var sRasorProcessId = data.data.processingIdentifier;
                console.log('Run Rasor - Proc ID = ' + sRasorProcessId);
                oController.m_oInterval(oController.checkProcessResult2,1000,1,true,sRasorProcessId,oController);
            }
            else
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR RUNNING eDRIFT RASOR WAPP");
            }
        },function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR RUNNING eDRIFT RASOR WAPP");
            oController.cleanAllExecuteWorkflowFields();
            oController.m_bIsRunning = false;
        });

    };

    RasorWappController.prototype.runRasor = function() {
        console.log("RUN  RASOR WAPP" );

        var oController = this;

        var sWorkspaceId = this.m_oConstantsService.getActiveWorkspace().name;
        // var sFile = this.m_oSelectedProduct.fileName;

        var oInputFile = this.getSelectedProduct(this.m_aoProducts,this.m_oReturnValueDropdown);
        var sFile = oInputFile.fileName;
        var sJSON = '{"file":"'+ sFile+'","workspace":"'+sWorkspaceId + '"}';


        this.m_sResultFromServer = "RASOR App is waiting to start";
        this.m_bIsRunning = true;

        this.m_oProcessorService.runProcessor('rasor2', sJSON).then(function (data) {
            if(utilsIsObjectNullOrUndefined(data.data) == false)
            {
                var sRasorProcessId = data.data.processingIdentifier;
                console.log('Run Rasor - Proc ID = ' + sRasorProcessId);
                oController.m_oInterval(oController.checkProcessResult,1000,1,true,sRasorProcessId,oController);
            }
            else
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR RUNNING WAPP");
            }
        },function (error) {
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
