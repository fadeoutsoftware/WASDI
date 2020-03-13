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
        let oViewElements = [];
        let oFactory = new ViewElementFactory();

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
        tst7.oSelectArea.iHeight = 200;
        tst7.oSelectArea.iWidth = 500;
        // oViewElements.push(tst1);
        // oViewElements.push(tst2);
        // oViewElements.push(tst3);
        // oViewElements.push(tst4);
        // oViewElements.push(tst5);
        // oViewElements.push(tst6);
        oViewElements.push(tst7);
        return oViewElements;
    };


    AppStoreController.$inject = [
        '$scope',
        'ConstantsService',
        'AuthService',
    ];

    return AppStoreController;
}) ();
