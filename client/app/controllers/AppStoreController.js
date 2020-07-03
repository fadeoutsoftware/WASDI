/**
 * Created by p.campanella on 21/10/2016.
 */

var AppStoreController = (function() {
    function AppStoreController($scope, oConstantsService, oAuthService) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oConstantsService = oConstantsService;
        this.m_oAuthService = oAuthService;
        this.m_aoViewElements = this.LoadViewElements();
        this.test = "miao";
    }

    AppStoreController.prototype.LoadViewElements = function(){

        let sSampleForm = "{\"tabs\":[{\"name\":\"Basic\",\"controls\":[{\"param\":\"ARCHIVE_START_DATE\",\"type\":\"date\",\"label\":\"Archive Start Date\"},{\"param\":\"ARCHIVE_END_DATE\",\"type\":\"date\",\"label\":\"Archive End Date\"},{\"param\":\"DELETE\",\"type\":\"boolean\",\"label\":\"Delete intermediate images\",\"default\":true},{\"param\":\"SIMULATE\",\"type\":\"boolean\",\"label\":\"Simulate Flood Detection\",\"default\":false},{\"param\":\"BBOX\",\"type\":\"bbox\",\"label\":\"Select Event Area\"}]},{\"name\":\"Advanced\",\"controls\":[{\"param\":\"ORBITS\",\"type\":\"textbox\",\"label\":\"Relative Orbit Numbers (comma separated)\"},{\"param\":\"GRIDSTEP\",\"type\":\"hidden\",\"label\":\"\",\"default\":\"1,1\"},{\"param\":\"PREPROCWORKFLOW\",\"type\":\"textbox\",\"label\":\"Preprocessing Workflow\",\"default\":\"LISTSinglePreproc2\"},{\"param\":\"MOSAICBASENAME\",\"type\":\"textbox\",\"label\":\"Event Code\",\"default\":\"LA\"}]}]}";
        let oFormToGenerate = JSON.parse(sSampleForm);

        let aoViewElements = [];
        let oFactory = new ViewElementFactory();

        for (let iTabs=0; iTabs<oFormToGenerate.tabs.length; iTabs++) {
            let oTab = oFormToGenerate.tabs[iTabs];
            var aoTabControls = oFactory.getTabElements(oTab);
            aoViewElements.push.apply(aoViewElements, aoTabControls);
        }

        let tst1 = oFactory.CreateViewElement("textbox");
        tst1.sLabel = "Sono una texbox";

        let tst2 = oFactory.CreateViewElement("dropdown");
        tst2.sLabel = "Sono una dropdown";

        let tst3 = oFactory.CreateViewElement("selectarea");
        tst3.sLabel = "Sono una selectarea";

        let tst4 = oFactory.CreateViewElement("selectarea");
        tst4.sLabel = "Sono una selectarea";

        let tst5 = oFactory.CreateViewElement("date");
        tst5.sLabel = "Sono una data";

        let tst6 = oFactory.CreateViewElement("tableofproducts");
        tst6.sLabel = "Sono una table";

        let tst7 = oFactory.CreateViewElement("lighserachproduct");
        tst7.sLabel = "Sono una light search";
        tst7.oStartDate.oDate =  moment().subtract(1, 'days').startOf('day');
        tst7.oEndDate.oDate = moment();
        tst7.oSelectArea.iHeight = 200;
        tst7.oSelectArea.iWidth = 500;
        tst7.aoProviders.push(providers.ONDA);
        tst7.aoMissionsFilters.push({name:"sentinel-1" },{name:"sentinel-2" });
        tst7.oTableOfProducts.isAvailableSelection = true;
        tst7.oTableOfProducts.isSingleSelection = true;

        /*oViewElements.push(tst1);
        aoViewElements.push(tst2);
        aoViewElements.push(tst3);
        aoViewElements.push(tst4);
        aoViewElements.push(tst5);
        aoViewElements.push(tst6);
        aoViewElements.push(tst7);*/
        return aoViewElements;
    };

    AppStoreController.prototype.readValues = function() {

        for (var i=0;i<this.m_aoViewElements.length;i++) {
            let oElement = this.m_aoViewElements[i];
            console.log(oElement.type);
        }
    }


    AppStoreController.$inject = [
        '$scope',
        'ConstantsService',
        'AuthService',
    ];

    return AppStoreController;
}) ();
