/**
 * Created by a.corrado on 31/03/2017.
 */


var WappsController = (function() {

    function WappsController($scope, oClose,oExtras,oWorkspaceService,oProductService, oProcessorService) {
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
        this.m_bIsJsonEditModeActive = true;
        this.myJson = {};
        this.m_sMyJsonString = "";
        var oController = this;
        $scope.close = function(result) {
            oClose(result, 300); // close, but give 500ms for bootstrap to animate
        };
        $scope.add = function(result) {
            oClose(result, 300); // close, but give 500ms for bootstrap to animate
        };

        this.getProcessorsList();
    };

    /**
     * getProcessorsList
     */
    WappsController.prototype.getProcessorsList = function() {
        var oController = this;

        this.m_oProcessorService.getProcessorsList().success(function (data) {
            if(utilsIsObjectNullOrUndefined(data) == false)
            {
                oController.m_aoProcessorList = oController.setDefaultImages(data);
            }
            else
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR GETTING WAPPS LIST");
            }
        }).error(function (error) {
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

    WappsController.prototype.runProcessor = function(sProcessor, sJSON) {
        console.log("RUN - " + sProcessor);

        var oController = this;
        //TODO CHECK IF STRING CONVERT AS OBJECT

        if(utilsIsString(sJSON) === true)
        {
            try {
                sJSON = JSON.parse(sJSON);
            }
            catch(err) {
                utilsVexDialogAlertTop("Invalid JSON");
                return;
            }

        }
        this.m_oProcessorService.runProcessor(sProcessor, sJSON).success(function (data) {
            if(utilsIsObjectNullOrUndefined(data) == false)
            {
                console.log('Run ' + data);
            }
            else
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR RUNNING WAPP");
            }
        }).error(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR RUNNING WAPP");
            oController.cleanAllExecuteWorkflowFields();
        });

    };


    WappsController.prototype.getHelpFromProcessor = function(sProcessor) {
        console.log("HELP - " + sProcessor);

        var oController = this;

        this.m_oProcessorService.getHelpFromProcessor(sProcessor).success(function (data) {
            if(utilsIsObjectNullOrUndefined(data) === false)
            {
                var sHelpMessage = data.stringValue;
                if(utilsIsObjectNullOrUndefined(sHelpMessage) === false )
                {
                    try {
                        sHelpMessage = data.stringValue.replace("\\n", "<br>");
                        sHelpMessage = sHelpMessage.replace("\\t","&nbsp&nbsp");
                        oHelp = JSON.parse(sHelpMessage);
                        sHelpMessage = oHelp.help;
                    }
                    catch(err) {
                        sHelpMessage = data.stringValue;
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
                // utilsVexDialogAlertTop(sHelpMessage);
                utilsVexDialogBigAlertTop(sHelpMessage);
            }
            else
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR READING WAPP HELP");
            }
        }).error(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR READING WAPP HELP");
            oController.cleanAllExecuteWorkflowFields();
        });

    };


    WappsController.prototype.collapsePanels = function()
    {
        this.myJson = {};
        this.m_sMyJsonString = "";
        utilsCollapseBootstrapPanels();
    };
    WappsController.$inject = [
        '$scope',
        'close',
        'extras',
        'WorkspaceService',
        'ProductService',
        'ProcessorService'

    ];
    return WappsController;
})();
