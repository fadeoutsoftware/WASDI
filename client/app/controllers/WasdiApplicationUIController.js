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
    function WasdiApplicationUIController($scope, oConstantsService, oAuthService, oProcessorService, oWorkspaceService, oRabbitStompService, $state, oProductService, $sce) {
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
         * Product Service
         */
        this.m_oProductService = oProductService;
        /**
         * SCE Angular Service
         */
        this.m_oSceService = $sce;
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
        /**
         * Application Data
         * @type {{}}
         */
        this.m_oApplication = {};
        /**
         * List of user's workspaces
         * @type {*[]}
         */
        this.m_aoWorkspaceList = [];

        /**
         * Utility variable for the workspace radio button
         * @type {string}
         */
        $scope.workspaceChose = 'new';

        /**
         * User selected workspace
         * @type {null}
         */
        this.m_oSelectedWorkspace = null;

        /**
         * List of products in the selected workspace
         * @type {*[]}
         */
        this.m_aoProducts = [];

        /**
         * Get the selected application
         * @type {string}
         */
        this.m_sSelectedApplication = this.m_oConstantsService.getSelectedApplication();
        /**
         * Flag to know if all the inputs must be rendered as strings or as objects
         * @type {boolean}
         */
        this.m_bRenderAsStrings = false;

        /**
         * Text of the Help tab
         * @type {string}
         */
        this.m_sHelpHtml = "No Help Avaiable";

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

                if (!utilsIsObjectNullOrUndefined(data.renderAsStrings)) {
                    if (data.renderAsStrings === true) {
                        oController.m_bRenderAsStrings = true;
                    }
                }
            })
            .error(function(oError ){
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR: READING APP UI");
            });

        /**
         * Ask the list of Applications to the WASDI server
         */
        this.m_oProcessorService.getMarketplaceDetail(this.m_sSelectedApplication).success(function (data) {
            if(utilsIsObjectNullOrUndefined(data) == false)
            {
                oController.m_oApplication = data;
            }
            else
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR GETTING APPLICATION DATA");
            }
        }).error(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR GETTING APPLICATION DATA");
        });

        // Retrive also the help
        this.m_oProcessorService.getHelpFromProcessor(this.m_sSelectedApplication).success(function (data) {

            if(utilsIsObjectNullOrUndefined(data) === false)
            {
                var sHelpMessage = data.stringValue;
                if(utilsIsObjectNullOrUndefined(sHelpMessage) === false )
                {
                    try {
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

                var oConverter = new showdown.Converter();

                oController.m_sHelpHtml = oController.m_oSceService.trustAsHtml(oConverter.makeHtml(sHelpMessage));
            }
            else
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR READING APP HELP");
            }
        }).error(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR READING APP HELP");
            oController.cleanAllExecuteWorkflowFields();
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

                // Move to the editor
                oController.m_oState.go("root.editor", { workSpace : oWorkspace.workspaceId });
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
                if (this.m_bRenderAsStrings) {
                    oProcessorInput[oElement.paramName] = oElement.getStringValue();
                }
                else  {
                    oProcessorInput[oElement.paramName] = oElement.getValue();
                }

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

        if (this.m_oSelectedWorkspace == null) {
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
        else {
            this.executeProcessorInWorkspace(this, sApplicationName, oProcessorInput, this.m_oSelectedWorkspace);
        }
    }

    /**
     * Get the name of a category from the id
     * @param sCategoryId
     * @returns {*}
     */
    WasdiApplicationUIController.prototype.formatDate = function(iTimestamp)
    {
        // Create a new JavaScript Date object based on the timestamp
        let oDate = new Date(iTimestamp);
        return oDate.toLocaleDateString();
    };

    /**
     * Click on the new workspace radio: clean existing ones
     */
    WasdiApplicationUIController.prototype.newWorkspaceClicked = function () {
        this.m_oSelectedWorkspace = null;
        this.m_aoWorkspaceList = [];
        this.m_aoProducts = [];

        let asTabs = this.getTabs();

        for (var iTab=0; iTab<asTabs.length; iTab++) {
            let aoControls = this.m_aoViewElements[asTabs[iTab]];

            for (var iControl=0; iControl<aoControls.length; iControl++) {
                if (aoControls[iControl].type=="productscombo") {
                    aoControls[iControl].asListValues = this.m_aoProducts;
                }
            }
        }

    }

    /**
     * Click on the open workspace radio: load the list of workspaces
     */
    WasdiApplicationUIController.prototype.openWorkspaceClicked = function()
    {
        var oController = this;
        this.m_oWorkspaceService.getWorkspacesInfoListByUser().success(function (data,status) {
            if (utilsIsObjectNullOrUndefined(data) == false) {
                oController.m_aoWorkspaceList = data;
            }
            else {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR READING USER WORKSPACES");
            }
        }).error(function () {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR READING USER WORKSPACES");
        })
    };

    /**
     * Method called when the user selects an existing workspace
     * @param oWorkspace
     */
    WasdiApplicationUIController.prototype.selectedWorkspace = function (oWorkspace) {
        this.m_oSelectedWorkspace = oWorkspace;

        let oController = this;

        this.m_oProductService.getProductListByWorkspace(oWorkspace.workspaceId).success(function (data, status) {

            oController.m_aoProducts = [];

            if (utilsIsObjectNullOrUndefined(data) == false) {
                //push all products
                for (var iIndex = 0; iIndex < data.length; iIndex++) {

                    let oProductItem = { value:"", id:""};

                    oProductItem.name = data[iIndex].name;
                    oProductItem.id = data[iIndex].name;

                    // Add the product to the list
                    oController.m_aoProducts.push(oProductItem);
                }
            }

            let asTabs = oController.getTabs();

            for (var iTab=0; iTab<asTabs.length; iTab++) {
                let aoControls = oController.m_aoViewElements[asTabs[iTab]];

                for (var iControl=0; iControl<aoControls.length; iControl++) {
                    if (aoControls[iControl].type=="productscombo") {
                        aoControls[iControl].asListValues = oController.m_aoProducts;
                    }
                }
            }


        }).error(function (data, status) {
            utilsVexDialogAlertTop('GURU MEDITATION<br>ERROR READING PRODUCT LIST');
        });

    }


    WasdiApplicationUIController.$inject = [
        '$scope',
        'ConstantsService',
        'AuthService',
        'ProcessorService',
        'WorkspaceService',
        'RabbitStompService',
        '$state',
        'ProductService',
        '$sce'
    ];

    return WasdiApplicationUIController;
}) ();
