/**
 * Created by a.corrado on 31/03/2017.
 */


var EdriftCheckImagesTool = (function() {

    function EdriftCheckImagesTool ($scope, oClose,oExtras,oAuthService,oConstantsService, oProcessorService, $interval, oProcessesLaunchedService, oModalService) {
        //MEMBERS
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oAuthService = oAuthService;
        this.m_oConstantsService = oConstantsService;
        this.m_oProcessorService = oProcessorService;
        this.m_oInterval = $interval;
        this.m_oProcessesLaunchedService = oProcessesLaunchedService;
        this.m_oParameters = {};
        this.m_oModalService = oModalService;
        this.m_bIsRunning = false;
        this.m_sProcessRunningId = "";

        this.initializeParameters();

        $scope.close = function(result) {
            oClose(result, 300); // close, but give 500ms for bootstrap to animate
        };

    }


    EdriftCheckImagesTool.prototype.initializeParameters = function()
    {

        var sTodayDate = new Date().toISOString().slice(0,10);

        this.m_oParameters = {
            startdate: sTodayDate,
            enddate: sTodayDate,
            bbox:"29.0,92.0,10.0,100.0",
            orbits:"33,41,62,70,77,99,106,135,143,172"
        };

    };


    EdriftCheckImagesTool.prototype.redirectToWebSite = function(){
        this.m_oWindow.open('http://edrift.cimafoundation.org', '_blank');
    };


    EdriftCheckImagesTool.prototype.plotResults = function(aoResults, oController){

        var asDays = [];
        var asOrbits = oController.m_oParameters.orbits.split(",");

        var aoTraces = [];

        var iOrbits;
        var i;

        var iResults;
        for (iResults= 0; iResults<aoResults.length; iResults ++) {
            asDays.push(aoResults[iResults].date);
        }

        // Per tutte le orbite
        for (iOrbits = 0 ; iOrbits < asOrbits.length; iOrbits ++ ) {

            var sActualOrbit = asOrbits[iOrbits];
            var aiImagesPerOrbit = [];

            // Per tutti i giorni
            for (iResults= 0; iResults<aoResults.length; iResults ++) {

                var iImagePerOrbitPerDay = 0;

                // Se il giorno ha orbita => numero se no zero
                for (i=0; i< aoResults[iResults].orbits.length; i++) {
                    if (aoResults[iResults].orbits[i].orbit == sActualOrbit) {
                        iImagePerOrbitPerDay = aoResults[iResults].orbits[i].images;
                        break;
                    }
                }

                aiImagesPerOrbit.push(iImagePerOrbitPerDay);
            }

            var oTrace = {
                x: asDays,
                y: aiImagesPerOrbit,
                name: sActualOrbit,
                type: 'bar'
            };

            aoTraces.push(oTrace);

        }

        var layout = {barmode: 'group'};

        Plotly.newPlot('CheckImagesBarChart', aoTraces, layout, {}, {showSendToCloud:true});

    };

    EdriftCheckImagesTool.prototype.runProcessor = function(){

        var oController = this;
        var oActiveWorkspace = this.m_oConstantsService.getActiveWorkspace();

        var sJSON=JSON.stringify(this.m_oParameters);

        if(utilsIsObjectNullOrUndefined(oActiveWorkspace) === true)
        {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR: INVALID ACTIVE WORKSPACE ");
            return false;
        }

        this.m_bIsRunning = true;

        this.m_oProcessorService.runProcessor('edriftcheckimages2',sJSON)
            .then(function(data,status){
                if( (utilsIsObjectNullOrUndefined(data.data) === false) && (status === 200))
                {
                    var oDialog =  utilsVexDialogAlertBottomRightCorner("eDRIFT CHECK IMAGES<br>PROCESS HAS BEEN SCHEDULED");
                    utilsVexCloseDialogAfter(4000, oDialog);


                    var sProcessId = data.data.processingIdentifier;

                    oController.m_sProcessRunningId = sProcessId;

                    console.log('Run Processor - Proc ID = ' + sProcessId);
                    oController.m_oInterval(oController.checkProcessResult,1000,1,true,sProcessId,oController);

                }
                else
                {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR: CHECK IMAGES SCHEDULING FAILED");
                }

            })
            .error(function(){
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR: CHECK IMAGES ERROR");
                oController.m_bIsRunning = false;
            });
    };

    EdriftCheckImagesTool.prototype.showLogs = function(){

        var oController = this;

        this.m_oProcessesLaunchedService.getProcessWorkspaceById(oController.m_sProcessRunningId).then(function(data,status){
            if( (utilsIsObjectNullOrUndefined(data.data) === false) && (status === 200))
            {
                oController.m_oModalService.showModal({
                    templateUrl: "dialogs/process_error_logs_dialog/ProcessErrorLogsDialogView.html",
                    controller: "ProcessErrorLogsDialogController",
                    inputs: {
                        extras: {
                            process:data.data,
                        }
                    }
                }).then(function (modal) {
                    modal.element.modal();
                    modal.close.then(function(oResult){

                    });
                });

            }
            else
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR: CHECK IMAGES SCHEDULING FAILED");
            }

        },function(){
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR: CHECK IMAGES ERROR");
                oController.m_bIsRunning = false;
            });


    };


    EdriftCheckImagesTool.prototype.checkProcessResult = function(sProcessId,oController) {
        console.log('---------------------------------Ask Result ' + sProcessId);

        var oLinkToController = oController;

        oLinkToController.m_oProcessesLaunchedService.getProcessWorkspaceById(sProcessId).then(function (data) {
            if(utilsIsObjectNullOrUndefined(data.data) == false)
            {
                if (data.data.status == 'DONE') {
                    console.log('---------------------------------Run Processor - DONE = ' + data.data.payload);
                    oLinkToController.m_bIsRunning = false;

                    var oResult = JSON.parse(data.data.payload);

                    if (oResult != null) {
                        //oLinkToController.m_sResultFromServer = "" + parseInt(oResult.pop) + " People Affected Estimate";
                        //var oDialog = utilsVexDialogAlertBottomRightCorner("eDRIFT RASOR PEOPLE AFFECTED ESTIMATE<br>CALCULATION DONE ["+ parseInt(oResult.pop)+"]");
                        var oDialog = utilsVexDialogAlertBottomRightCorner("eDRIFT CHECK IMAGES AVAILABILITY DONE");
                        utilsVexCloseDialogAfter(4000,oDialog);

                        oLinkToController.plotResults(oResult, oLinkToController);
                    }
                    else {
                        //oLinkToController.m_sResultFromServer = "NA";
                        var oDialog = utilsVexDialogAlertBottomRightCorner("eDRIFT CHECK IMAGES AVAILABILITY DONE ERROR");
                        utilsVexCloseDialogAfter(4000,oDialog);
                    }


                }
                else if (data.data.status == 'STOPPED') {
                    console.log('---------------------------------Run Processor - STOPPED');
                    oLinkToController.m_sResultFromServer = "eDRIFT CHECK IMAGES AVAILABILITY has been Stopped by the User";
                    oLinkToController.m_bIsRunning = false;
                }
                else if (data.data.status == 'ERROR') {
                    console.log('---------------------------------Run Processor - ERROR');
                    oLinkToController.m_sResultFromServer = "There was an Error running eDRIFT CHECK IMAGES AVAILABILITY";
                    oLinkToController.m_bIsRunning = false;
                }
                else if (data.data.status == 'CREATED') {
                    console.log('---------------------------------Run Processor - CREATED');
                    oLinkToController.m_sResultFromServer = "eDRIFT CHECK IMAGES AVAILABILITY is waiting to start";
                    oLinkToController.m_oInterval(oLinkToController.checkProcessResult,1000,1,true,data.data.processObjId, oLinkToController);
                }
                else if (data.data.status == 'RUNNING' || data.data.status == 'WAITING' || data.data.status == 'READY') {
                    console.log('---------------------------------Run Processor - RUNNING');
                    oLinkToController.m_sResultFromServer = "eDRIFT CHECK IMAGES AVAILABILITY is Running";
                    oLinkToController.m_oInterval(oLinkToController.checkProcessResult,1000,1,true,data.data.processObjId, oLinkToController);
                }
                else {
                    console.log('---------------------------------Run Processor - UNKNOWN ' + data.data.status);
                    oLinkToController.m_oInterval(oLinkToController.checkProcessResult,1000,1,true,data.data.processObjId, oLinkToController);
                }
            }
            else
            {
                console.log('---------------------------------Run Processor - DATA NULL');

                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR RUNNING eDRIFT CHECK IMAGES AVAILABILITY");
            }
        },function (error) {
            console.log('---------------------------------Run Processor - ERROR');
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR RUNNING eDRIFT CHECK IMAGES AVAILABILITY");
        });
    };

    EdriftCheckImagesTool.$inject = [
        '$scope',
        'close',
        'extras',
        'AuthService',
        'ConstantsService',
        'ProcessorService',
        '$interval',
        'ProcessesLaunchedService',
        'ModalService'
    ];
    return EdriftCheckImagesTool;
})();
