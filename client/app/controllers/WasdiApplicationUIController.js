/**
 * View of the User Interface of a WASDI Application
 * Created by p.campanella on 21/10/2016.
 */

var WasdiApplicationUIController = (function() {

    /**
     * Class constructor
     * @param $scope
     * @param oConstantsService
     * @param oAuthService
     * @param oProcessorService
     * @constructor
     */
    function WasdiApplicationUIController($scope, oConstantsService, oAuthService, oProcessorService, oWorkspaceService, oRabbitStompService, $state) {
        /**
         * Angular Scope
         */
        this.m_oScope = $scope;
        /**
         * Reference to the controller
         * @type {WasdiApplicationUIController}
         */
        this.m_oScope.m_oController = this;
        /**
         * Constant Service
         */
        this.m_oConstantsService = oConstantsService;
        /**
         * Auth Service
         */
        this.m_oAuthService = oAuthService;
        /**
         * Processors Service
         */
        this.m_oProcessorService = oProcessorService;
        /**
         * Workspace Service
         */
        this.m_oWorkspaceService = oWorkspaceService;
        /**
         * Rabbit Service
         */
        this.m_oRabbitStompService = oRabbitStompService;
        /**
         * Angular State Service
         */
        this.m_oState = $state;
        /**
         * Contains one property for each tab. Each Property is an array of the Tab Controls
         * @type {*[]}
         */
        this.m_aoViewElements = [];
        /**
         * Array of the names of the Tabs
         * @type {*[]}
         */
        this.m_asTabs = [];
        /**
         * Actually selected tab
         * @type {string}
         */
        this.m_sSelectedTab = "";


        //KASA FOR TEST
        $scope.appTitle = 'Titolo della applicazione';
        $scope.appDesc = 'Automatic Burned Area Detection using pre-fire and post-fire S2 data selected by user (that should belong to the same S2 tile). if "INCLUDESHRUB" is set to "YES", it means that not only Burned Forest Areas, but also Burned Shrubland Areas are searched for. Otherwise, only Burned Forest Areas are searched for. Reference: L. Pulvirenti et a.l, "An Automatic Processing Chain for Near Real-Time Mapping of Burned Forest Areas Using Sentinel-2 Data", Remote Sens. 2020, 12, 674; doi:10.3390/rs12040674';
        $scope.appPublisher = 'Paolo Campanella';
        $scope.appWebsite = 'http://www.wasdi.net';
        $scope.appMail = 'paolo@fadeout.it';
        $scope.appLastUpdate = '22/5/2020';

        // TODO: Temporary fo test
        //this.m_oConstantsService.setSelectedApplication("edrift_archive_generator");

        this.m_sSelectedApplication = this.m_oConstantsService.getSelectedApplication();

        let oController = this;

        // Check if we have the selected application
        if (utilsIsStrNullOrEmpty(this.m_sSelectedApplication)) {
            // Check if we can get it from the state
            if (!(utilsIsObjectNullOrUndefined(this.m_oState.params.processorName) && utilsIsStrNullOrEmpty(this.m_oState.params.processorName))) {
                // Set the application name
                this.m_sSelectedApplication = this.m_oState.params.processorName;
            } else {
                // No app, go back to the marketplace
                this.m_oState.go("root.marketplace");
            }
        }


        /**
         * Ask the Processor UI to the WASDI server
         */
        this.m_oProcessorService.getProcessorUI(this.m_sSelectedApplication)
            .success(function(data,status){
                // For each Tab
                for (let iTabs=0; iTabs<data.tabs.length; iTabs++) {
                    // Get the tab
                    let oTab = data.tabs[iTabs];
                    // Save the name
                    oController.m_asTabs.push(oTab.name);
                    // Set the first one as default
                    if (iTabs == 0) oController.m_sSelectedTab = oTab.name;
                }

                // Create all the components
                oController.m_aoViewElements = oController.generateViewElements(data);
            })
            .error(function(oError ){
                // TODO: Temperary for debug with an hard coded UI
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR: READING APP UI");
                //let sSampleForm = "{\"tabs\":[{\"name\":\"Basic\",\"controls\":[{\"param\":\"ARCHIVE_START_DATE\",\"type\":\"date\",\"label\":\"Archive Start Date\"},{\"param\":\"ARCHIVE_END_DATE\",\"type\":\"date\",\"label\":\"Archive End Date\"},{\"param\":\"DELETE\",\"type\":\"boolean\",\"label\":\"Delete intermediate images\",\"default\":true},{\"param\":\"SIMULATE\",\"type\":\"boolean\",\"label\":\"Simulate Flood Detection\",\"default\":false},{\"param\":\"BBOX\",\"type\":\"bbox\",\"label\":\"Select Event Area\"}]},{\"name\":\"Advanced\",\"controls\":[{\"param\":\"ORBITS\",\"type\":\"textbox\",\"label\":\"Relative Orbit Numbers (comma separated)\"},{\"param\":\"GRIDSTEP\",\"type\":\"hidden\",\"label\":\"\",\"default\":\"1,1\"},{\"param\":\"PREPROCWORKFLOW\",\"type\":\"textbox\",\"label\":\"Preprocessing Workflow\",\"default\":\"LISTSinglePreproc2\"},{\"param\":\"MOSAICBASENAME\",\"type\":\"textbox\",\"label\":\"Event Code\",\"default\":\"LA\"}]}]}";
                //let oFormToGenerate = JSON.parse(sSampleForm);

                //for (let iTabs=0; iTabs<oFormToGenerate.tabs.length; iTabs++) {
                //    let oTab = oFormToGenerate.tabs[iTabs];
                //    oController.m_asTabs.push(oTab.name);
                //    if (iTabs == 0) oController.m_sSelectedTab = oTab.name;
                //}

                // Create all the components
                //oController.m_aoViewElements = oController.generateViewElements(oFormToGenerate);
            });
    }

    /**
     * Get the list of tabs
     * @returns {*[]} Array of strings, names of the tabs
     */
    WasdiApplicationUIController.prototype.getTabs = function() {
        return this.m_asTabs;
    }

    /**
     * Get the list of controls in a Tab
     * @param sTabName Name of the tab
     * @returns {*}
     */
    WasdiApplicationUIController.prototype.getTabControls = function(sTabName) {
        return this.m_aoViewElements[sTabName];
    }

    /**
     * Generates the view elements of the Processor
     * @param oFormToGenerate JSON Object representing the Processor UI
     * @returns {[]} an object with a property for each tab. Each property is an array of the controls of the tab
     */
    WasdiApplicationUIController.prototype.generateViewElements = function(oFormToGenerate){

        // Output initialization
        let aoViewElements = [];
        // Create the factory
        let oFactory = new ViewElementFactory();

        // For each tab
        for (let iTabs=0; iTabs<oFormToGenerate.tabs.length; iTabs++) {
            // Get the tab
            let oTab = oFormToGenerate.tabs[iTabs];
            // Let the factory create the array of controls to add
            let aoTabControls = oFactory.getTabElements(oTab);
            // Save the tab controls in the relative property
            aoViewElements[oTab.name] = aoTabControls;
        }

        return aoViewElements;
    };

    /**
     * Triggers the execution of Application with Paramteres in a specific Workpsace
     * @param oController Reference to the controller
     * @param sApplicationName Name of the application
     * @param oProcessorInput Processor Input Parameter Object
     * @param oWorkspace Workpsace
     */
    WasdiApplicationUIController.prototype.executeProcessorInWorkspace = function(oController, sApplicationName, oProcessorInput, oWorkspace) {
        try {
            // Subscribe to the asynch notifications
            oController.m_oRabbitStompService.subscribe(oWorkspace.workspaceId);
        }
        catch(error) {
            let oDialog = utilsVexDialogAlertBottomRightCorner('ERROR SUBSCRIBING<br>THE WORKSPACE');
            utilsVexCloseDialogAfter(4000, oDialog);
        }

        // Set the active workspace
        oController.m_oConstantsService.setActiveWorkspace(oWorkspace);

        // Run the processor
        oController.m_oProcessorService.runProcessor(sApplicationName,JSON.stringify(oProcessorInput)).success(function (data, status) {
            if(utilsIsObjectNullOrUndefined(data) == false)
            {
                // Ok, processor scheduled, notify the user
                var oDialog = utilsVexDialogAlertBottomRightCorner("PROCESSOR SCHEDULED<br>READY");
                utilsVexCloseDialogAfter(4000,oDialog);

                // Get the root scope
                let oRootScope = oController.m_oScope.$parent;
                while(oRootScope.$parent != null || oRootScope.$parent != undefined)
                {
                    oRootScope = oRootScope.$parent;
                }

                // send the message to show the processor log dialog
                let oPayload = { processId: data.processingIdentifier };
                oRootScope.$broadcast(RootController.BROADCAST_MSG_OPEN_LOGS_DIALOG_PROCESS_ID, oPayload);
            }
            else
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR RUNNING APPLICATION");
            }
        }).error(function () {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR RUNNING APPLICATION");
        });


    }

    /**
     * Generate the JSON that has to be sent to the Procesor
     */
    WasdiApplicationUIController.prototype.generateParamsAndRun = function() {

        // Output initialization
        let oProcessorInput = {};

        // For each tab
        for (let iTabs = 0; iTabs<this.m_asTabs.length; iTabs++) {
            // Get the name of the tab
            let sTab = this.m_asTabs[iTabs];

            // For all the view elements of the tab
            for (let iControls=0; iControls<this.m_aoViewElements[sTab].length; iControls++) {
                // Take the element
                let oElement = this.m_aoViewElements[sTab][iControls];

                // Debug only log
                //console.log(oElement.paramName + " ["+oElement.type+"]: " + oElement.getValue());

                // Save the value to the output json
                oProcessorInput[oElement.paramName] = oElement.getValue();
            }
        }

        // Log the parameters
        console.log(oProcessorInput);

        // Reference to this controller
        let oController = this;
        // Reference to the application name
        let sApplicationName = this.m_oConstantsService.getSelectedApplication();

        let oToday = new Date();
        let sToday = oToday.toISOString().substring(0, 10);

        let sWorkspaceName = sToday + "_" + sApplicationName;

        // Create a new Workspace
        this.m_oWorkspaceService.createWorkspace(sWorkspaceName).success( function (data,status) {

                // Get the new workpsace Id
                let sWorkspaceId = data.stringValue;

                // Get the view model of this workspace
                oController.m_oWorkspaceService.getWorkspaceEditorViewModel(sWorkspaceId).success(function (data, status) {
                    if (utilsIsObjectNullOrUndefined(data) == false)
                    {
                        // Ok execute
                        oController.executeProcessorInWorkspace(oController, sApplicationName, oProcessorInput, data);
                    }
                }).error(function (data,status) {
                    utilsVexDialogAlertTop('GURU MEDITATION<br>ERROR OPENING THE WORKSPACE');
                });

            }
        ).error( function () {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR CREATING WORKSPACE");
        });
    }


    WasdiApplicationUIController.$inject = [
        '$scope',
        'ConstantsService',
        'AuthService',
        'ProcessorService',
        'WorkspaceService',
        'RabbitStompService',
        '$state'
    ];

    return WasdiApplicationUIController;
}) ();
