/**
 * Created by a.corrado on 31/03/2017.
 */


var WappsController = (function() {

    function WappsController($scope, oClose,oExtras,oWorkspaceService,oProductService, oProcessorService,oConstantsService, oModalService, $timeout, oRabbitStompService) {
        //MEMBERS
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oWorkspaceService = oWorkspaceService;
        this.m_oProductService = oProductService;
        this.m_aoWorkspaceList = [];
        this.m_aWorkspacesName = [];
        this.m_aoSelectedWorkspaces = [];
        this.m_sFileName = "";
        this.m_oProcessorService = oProcessorService;
        this.m_aoProcessorList = [];
        this.m_bIsLoadingProcessorList = false;
        this.m_bIsJsonEditModeActive = false;
        this.m_sJson = {};
        this.m_sMyJsonString = "";
        this.m_oModalService = oModalService;
        this.m_oConstantsService = oConstantsService;
        this.m_oTimeout = $timeout; 
        this.m_oRabbitStompService = oRabbitStompService; 

        // this.m_sSearchTextApp = "";

        /* 
        RabbitStomp Service call
        */
        this.m_iHookIndex = this.m_oRabbitStompService.addMessageHook(
            "DELETEPROCESSOR",
            this,
            this.rabbitMessageHook
        );

        let oController = this; 

        $scope.close = function(result) {

            oController.m_oRabbitStompService.removeMessageHook(oController.m_iHookIndex);
            oClose(result, 300); // close, but give 500ms for bootstrap to animate
        };
        $scope.add = function(result) {
            oController.m_oRabbitStompService.removeMessageHook(oController.m_iHookIndex);
            oClose(result, 300); // close, but give 500ms for bootstrap to animate
        };

        this.getProcessorsList();
    };

    /**
     * getProcessorsList
     */
    WappsController.prototype.getProcessorsList = function() {
        var oController = this;
        // swap to true after loading icon is properly rendered
        oController.m_bIsLoadingProcessorList = true;
        this.m_oProcessorService.getProcessorsList().then(function (data) {
            if(utilsIsObjectNullOrUndefined(data.data) == false)
            {
                oController.m_aoProcessorList = oController.setDefaultImages(data.data);
                oController.m_bIsLoadingProcessorList = false;
            }
            else
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR GETTING WAPPS LIST");
            }
        },function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR GETTING WAPPS LIST");
            oController.cleanAllExecuteWorkflowFields();

        });
    };

    WappsController.prototype.setDefaultImages = function(aoProcessorList)
    {
        if(utilsIsObjectNullOrUndefined(aoProcessorList) === true)
        {
            return aoProcessorList;
        }
        var sDefaultImage = "assets/icons/ImageNotFound.svg";
        var iNumberOfProcessors = aoProcessorList.length;
        for(var iIndexProcessor = 0; iIndexProcessor < iNumberOfProcessors; iIndexProcessor++)
        {
            if(utilsIsObjectNullOrUndefined(aoProcessorList.imgLink))
            {
                aoProcessorList[iIndexProcessor].imgLink = sDefaultImage;
            }
        }
        return aoProcessorList;
    };

    WappsController.prototype.selectProcessor = function(processor)
    {
        this._selectedProcessor = processor;
        this.m_sJson = {};

        if (!utilsIsStrNullOrEmpty(processor.paramsSample)) {
            this.m_sMyJsonString = decodeURIComponent(processor.paramsSample);

            try {
                var oParsed = JSON.parse(this.m_sMyJsonString);
                sPrettyPrint = JSON.stringify(oParsed, null, 2);
                this.m_sMyJsonString = sPrettyPrint;
            }
            catch (oError) {

            }
        }
        else {
            this.m_sMyJsonString = "";
        }
    }

    WappsController.prototype.tryParseJSON =function(sJsonString){
        try {
            var oJsonParsedObject = JSON.parse(sJsonString);

            if (oJsonParsedObject && typeof oJsonParsedObject === "object") {
                return oJsonParsedObject;
            }
        }
        catch (e) { }

        return false;
    };

    WappsController.prototype.runProcessor = function()
    {
        console.log("RUN - " + this._selectedProcessor.processorName);

        var oController = this;

        let sJSON = null;
        if(this.m_bIsJsonEditModeActive == true){
            sJSON = this.m_sJson;
        }else{
            sJSON = this.m_sMyJsonString;
        }

        let sStringJSON = "";

        if(utilsIsString(sJSON) === false)  {
            sStringJSON = JSON.stringify(sJSON);
        }
        else {
            sStringJSON = sJSON;
        }

        try {
            JSON.parse(sStringJSON);
        } catch (e) {

            let sErrorMessage = "INVALID JSON INPUT PARAMETERS<br>" + e.toString();
            utilsVexDialogBigAlertTop(sErrorMessage);

            return;
        }


        this.m_oProcessorService.runProcessor(this._selectedProcessor.processorName, sStringJSON)
            .then(function (data) {
                if(utilsIsObjectNullOrUndefined(data.data) == false)
                {
                    var oDialog = utilsVexDialogAlertBottomRightCorner("PROCESSOR SCHEDULED<br>READY");
                    utilsVexCloseDialogAfter(4000,oDialog);

                    let rootscope = oController.m_oScope.$parent;
                    while(rootscope.$parent != null || rootscope.$parent != undefined)
                    {
                        rootscope = rootscope.$parent;
                    }

                    let payload = { processId: data.data.processingIdentifier };
                    rootscope.$broadcast(RootController.BROADCAST_MSG_OPEN_LOGS_DIALOG_PROCESS_ID, payload);


                    $('#wappsDialog').modal('hide');

                }
                else
                {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR RUNNING WAPP");
                }
            },function (error) {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR RUNNING WAPP");
            });

    };

    WappsController.prototype.downloadClick = function(oProcessor) {
        if(utilsIsObjectNullOrUndefined(oProcessor) === true)
        {
            return false;
        }

        this.m_oProcessorService.downloadProcessor(oProcessor.processorId);

    };

    WappsController.prototype.deleteClick= function(oProcessor) {
        
        if(utilsIsObjectNullOrUndefined(oProcessor) === true)
        {
            return false;
        }
        var oController = this
        var oReturnFunctionValue = function(oValue){
            this.m_bIsLoadingProcessorList = true
            if (oValue === true)
            {
                oController.m_oProcessorService.deleteProcessor(oProcessor.processorId).then(function (data) {
                    oController.m_bIsLoadingProcessorList = true;
                
                }
                    
                ); 
            }
            oController.m_bIsLoadingProcessorList = false;
        }

        utilsVexDialogConfirm("Are you SURE you want to delete the Processor: " + oProcessor.processorName + " ?", oReturnFunctionValue);
    };

    WappsController.prototype.editClick= function(oProcessor) {
        var oController = this;

        oController.m_oModalService.showModal({
            templateUrl: "dialogs/processor/ProcessorView.html",
            controller: "ProcessorController",
            inputs: {
                extras: {
                    processor:oProcessor
                }
            }
        }).then(function (modal) {
            modal.element.modal();
            modal.close.then(function (oResult) {
                //console.alert("Closed modal");
            });
        });

    }

    WappsController.prototype.paramsClick = function(oProcessor) {
        var oController = this;

        this.m_sJson = "m_sJson";

        this.m_sMyJsonString = "m_sMyJsonString";

        oController.m_oModalService.showModal({
            templateUrl: "dialogs/processor_parameters_template/ProcessorParametersTemplateView.html",
            controller: "ProcessorParametersTemplateController",
            inputs: {
                extras: {
                    processor:oProcessor
                }
            }
        }).then(function (modal) {
            modal.element.modal();
            modal.close.then(function (oResult) {
                if (utilsIsObjectNullOrUndefined(oResult) === false) {

                    let sJson = decodeURIComponent(oResult);

                    try {
                        var oParsed = JSON.parse(sJson);
                        sPrettyPrint = JSON.stringify(oParsed, null, 2);

                        oController.m_sMyJsonString = sPrettyPrint;
                    }
                    catch (oError) {

                    }

                }
            });
        });

    }

    WappsController.prototype.getProcessorNameAsTitle = function(){
        if( this._selectedProcessor ){
            return this._selectedProcessor.processorName;
        }
        return "";
    }

    WappsController.prototype.getHelpFromProcessor = function() {
        //console.log("HELP - " + this._selectedProcessor.processorName);

        var oController = this;

        this.m_oProcessorService.getHelpFromProcessor(this._selectedProcessor.processorName).then(function (data) {
            if(utilsIsObjectNullOrUndefined(data.data) === false)
            {
                var sHelpMessage = data.data.stringValue;
                if(utilsIsObjectNullOrUndefined(sHelpMessage) === false )
                {
                    try {
                        //sHelpMessage = data.data.stringValue.replace("\\n", "<br>");
                        //sHelpMessage = sHelpMessage.replace("\\t","&nbsp&nbsp");
                        var oHelp = JSON.parse(sHelpMessage);
                        sHelpMessage = oHelp.help;
                    }
                    catch(err) {
                        sHelpMessage = data.data.stringValue;
                    }

                }
                else
                {
                    sHelpMessage = "";
                }
                //If the message is empty from the server or is null
                if(sHelpMessage === "")
                {
                    sHelpMessage = "There isn't any help message."
                }

                var oConverter = new showdown.Converter({tables: true});

                // utilsVexDialogAlertTop(sHelpMessage);
                utilsVexDialogBigAlertTop(oConverter.makeHtml(sHelpMessage));
            }
            else
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR READING WAPP HELP");
            }
        },function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR READING WAPP HELP");
            oController.cleanAllExecuteWorkflowFields();
        });

    };

    WappsController.prototype.collapsePanels = function()
    {
        this.m_sJson = {};
        this.m_sMyJsonString = "";
        utilsCollapseBootstrapPanels();
    };
    WappsController.prototype.formatJson = function() {

        this.m_sMyJsonString = JSON.stringify(JSON.parse(this.m_sMyJsonString.replaceAll("'", '"')), null, 2);
    }
    WappsController.prototype.rabbitMessageHook = function (oRabbitMessage, oController) {
        oController.getProcessorsList();
        oController.m_bIsLoadingProcessorList = false;
    }


    WappsController.$inject = [
        '$scope',
        'close',
        'extras',
        'WorkspaceService',
        'ProductService',
        'ProcessorService',
        'ConstantsService',
        'ModalService',
        '$timeout',
        'RabbitStompService'
    ];
    return WappsController;
})();
window.WappsController =  WappsController;
