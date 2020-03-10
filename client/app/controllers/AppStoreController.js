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
        let tst2 = oFactory.CreateViewElement("dropdown");
        oViewElements.push(tst1);
        oViewElements.push(tst2);
        return oViewElements;
    };


    AppStoreController.$inject = [
        '$scope',
        'ConstantsService',
        'AuthService',
    ];

    return AppStoreController;
}) ();
