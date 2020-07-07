/**
 * Created by p.campanella on 21/10/2016.
 */

var WasdiApplicationUIController = (function() {
    function AppStoreController($scope, oConstantsService, oAuthService, oProcessorService) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oConstantsService = oConstantsService;
        this.m_oAuthService = oAuthService;
        this.m_oProcessorService = oProcessorService;
        this.m_aoViewElements = [];
        this.m_oConstantsService.setSelectedApplication("edrift_archive_generator");
        let oController = this;

        this.m_oProcessorService.getProcessorUI(this.m_oConstantsService.getSelectedApplication())
            .success(function(data,status){
                oController.m_aoViewElements = oController.LoadViewElements(data);
            })
            .error(function(){
                //utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR: READING APP UI");
                let sSampleForm = "{\"tabs\":[{\"name\":\"Basic\",\"controls\":[{\"param\":\"ARCHIVE_START_DATE\",\"type\":\"date\",\"label\":\"Archive Start Date\"},{\"param\":\"ARCHIVE_END_DATE\",\"type\":\"date\",\"label\":\"Archive End Date\"},{\"param\":\"DELETE\",\"type\":\"boolean\",\"label\":\"Delete intermediate images\",\"default\":true},{\"param\":\"SIMULATE\",\"type\":\"boolean\",\"label\":\"Simulate Flood Detection\",\"default\":false},{\"param\":\"BBOX\",\"type\":\"bbox\",\"label\":\"Select Event Area\"}]},{\"name\":\"Advanced\",\"controls\":[{\"param\":\"ORBITS\",\"type\":\"textbox\",\"label\":\"Relative Orbit Numbers (comma separated)\"},{\"param\":\"GRIDSTEP\",\"type\":\"hidden\",\"label\":\"\",\"default\":\"1,1\"},{\"param\":\"PREPROCWORKFLOW\",\"type\":\"textbox\",\"label\":\"Preprocessing Workflow\",\"default\":\"LISTSinglePreproc2\"},{\"param\":\"MOSAICBASENAME\",\"type\":\"textbox\",\"label\":\"Event Code\",\"default\":\"LA\"}]}]}";
                let oFormToGenerate = JSON.parse(sSampleForm);
                oController.m_aoViewElements = oController.LoadViewElements(oFormToGenerate);
            });
    }

    AppStoreController.prototype.LoadViewElements = function(oFormToGenerate){

        let aoViewElements = [];
        let oFactory = new ViewElementFactory();

        for (let iTabs=0; iTabs<oFormToGenerate.tabs.length; iTabs++) {
            let oTab = oFormToGenerate.tabs[iTabs];
            var aoTabControls = oFactory.getTabElements(oTab);
            aoViewElements.push.apply(aoViewElements, aoTabControls);
        }

        return aoViewElements;

        /*
                let tst7 = oFactory.CreateViewElement("searcheoimage");
                tst7.sLabel = "Sono una light search";
                tst7.oStartDate.m_sDate =  moment().subtract(1, 'days').startOf('day');
                tst7.oEndDate.m_sDate = moment();
                tst7.oSelectArea.iHeight = 200;
                tst7.oSelectArea.iWidth = 500;
                tst7.aoProviders.push(providers.ONDA);
                tst7.aoMissionsFilters.push({name:"sentinel-1" },{name:"sentinel-2" });
                tst7.oTableOfProducts.isAvailableSelection = true;
                tst7.oTableOfProducts.isSingleSelection = true;
                */

    };

    AppStoreController.prototype.readValues = function() {

        let oProcessorInput = {};

        for (let iControls=0; iControls<this.m_aoViewElements.length; iControls++) {
            let oElement = this.m_aoViewElements[iControls];
            console.log(oElement.paramName + " ["+oElement.type+"]: " + oElement.getValue());
            oProcessorInput[oElement.paramName] = oElement.getValue();
        }

        console.log(oProcessorInput);
    }


    AppStoreController.$inject = [
        '$scope',
        'ConstantsService',
        'AuthService',
        'ProcessorService'
    ];

    return AppStoreController;
}) ();
