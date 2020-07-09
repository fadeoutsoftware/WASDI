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
    function WasdiApplicationUIController($scope, oConstantsService, oAuthService, oProcessorService) {
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

        // TODO: Temporary fo test
        this.m_oConstantsService.setSelectedApplication("edrift_archive_generator");

        let oController = this;

        /**
         * Ask the Processor UI to the WASDI server
         */
        this.m_oProcessorService.getProcessorUI(this.m_oConstantsService.getSelectedApplication())
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
            .error(function(){
                // TODO: Temperary for debug with an hard coded UI
                //utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR: READING APP UI");
                let sSampleForm = "{\"tabs\":[{\"name\":\"Basic\",\"controls\":[{\"param\":\"ARCHIVE_START_DATE\",\"type\":\"date\",\"label\":\"Archive Start Date\"},{\"param\":\"ARCHIVE_END_DATE\",\"type\":\"date\",\"label\":\"Archive End Date\"},{\"param\":\"DELETE\",\"type\":\"boolean\",\"label\":\"Delete intermediate images\",\"default\":true},{\"param\":\"SIMULATE\",\"type\":\"boolean\",\"label\":\"Simulate Flood Detection\",\"default\":false},{\"param\":\"BBOX\",\"type\":\"bbox\",\"label\":\"Select Event Area\"}]},{\"name\":\"Advanced\",\"controls\":[{\"param\":\"ORBITS\",\"type\":\"textbox\",\"label\":\"Relative Orbit Numbers (comma separated)\"},{\"param\":\"GRIDSTEP\",\"type\":\"hidden\",\"label\":\"\",\"default\":\"1,1\"},{\"param\":\"PREPROCWORKFLOW\",\"type\":\"textbox\",\"label\":\"Preprocessing Workflow\",\"default\":\"LISTSinglePreproc2\"},{\"param\":\"MOSAICBASENAME\",\"type\":\"textbox\",\"label\":\"Event Code\",\"default\":\"LA\"}]}]}";
                let oFormToGenerate = JSON.parse(sSampleForm);

                for (let iTabs=0; iTabs<oFormToGenerate.tabs.length; iTabs++) {
                    let oTab = oFormToGenerate.tabs[iTabs];
                    oController.m_asTabs.push(oTab.name);
                    if (iTabs == 0) oController.m_sSelectedTab = oTab.name;
                }

                // Create all the components
                oController.m_aoViewElements = oController.generateViewElements(oFormToGenerate);
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
     * Generate the JSON that has to be sent to the Procesor
     */
    WasdiApplicationUIController.prototype.getProcessorParams = function() {

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
                //console.log(oElement.paramName + " ["+oElement.type+"]: " + oElement.getValue());

                // Save the value to the output json
                oProcessorInput[oElement.paramName] = oElement.getValue();
            }
        }

        console.log(oProcessorInput);
    }


    WasdiApplicationUIController.$inject = [
        '$scope',
        'ConstantsService',
        'AuthService',
        'ProcessorService'
    ];

    return WasdiApplicationUIController;
}) ();
