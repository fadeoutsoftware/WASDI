SubscriptionPurchaseController = (function () {
    function SubscriptionPurchaseController(
        $scope,
        $window,
        oModalService,
        oConstantsService,
        oSubscriptionService,
        oOrganizationService,
        oTranslate
    ) {
        this.m_oScope = $scope;
        this.m_oWindow = $window;
        this.m_oScope.m_oController = this;
        this.m_oTranslate = oTranslate;

        this.m_oConstantsService = oConstantsService;
        this.m_oSubscriptionService = oSubscriptionService;
        this.m_oOrganizationService = oOrganizationService;

        this.m_oModalService = oModalService;

        this.m_aoTypes = [];
        this.m_aoTypesMap = [];
        this.m_oType = {};

        this.m_aoSelectedSubscriptions = [];

        this.getSubscriptionTypes();
        this.setPrice();

        this.m_iTotalPrice = 0;
    }

    SubscriptionPurchaseController.prototype.addToCart = function (sSubName, iSubPrice) {
        let oSubscriptionInfo = {};
        let oMatchingSub = {};

        this.m_aoTypes.forEach(oSubscription => {
            if (oSubscription.typeId === sSubName) {
                oMatchingSub = oSubscription;
            }
        });
        oSubscriptionInfo = {
            description: oMatchingSub.description,
            name: oMatchingSub.name,
            typeId: oMatchingSub.typeId,
            price: iSubPrice
        }
        this.m_aoSelectedSubscriptions.push(oSubscriptionInfo);

        this.setPrice();
    }

    SubscriptionPurchaseController.prototype.showSubscriptionAddForm = function (oSubscription, price) {
        var oController = this;
        console.log(oSubscription)

        let oNewSubscription = {
            subscriptionId: null,
            typeId: oSubscription.typeId,
            typeName: oSubscription.name,
            buySuccess: false,
            price: price
        };
console.log(oNewSubscription)
        this.m_oModalService.showModal({
            templateUrl: "dialogs/subscription_editor/SubscriptionEditorDialog.html",
            controller: "SubscriptionEditorController",
            inputs: {
                extras: {
                    subscription: oNewSubscription,
                    editMode: true
                }
            }
        }).then(function (modal) {
            modal.element.modal({
                backdrop: 'static'
            })
            modal.close.then(function (oResult) {})
        });
    }

    SubscriptionPurchaseController.prototype.setPrice = function () {
        let iaPrice = [];
        this.m_aoSelectedSubscriptions.forEach(oSubscription => {
            iaPrice.push(oSubscription.price);
        });

        let sum = iaPrice.reduce((a, b) => a + b, 0);

        this.m_iTotalPrice = sum;
    }

    SubscriptionPurchaseController.prototype.getSubscriptionTypes = function () {
        let oController = this;
        oController.m_oSubscriptionService.getSubscriptionTypes().then(function (response) {
            if (!utilsIsObjectNullOrUndefined(response)
                && !utilsIsObjectNullOrUndefined(response.data) && response.status === 200) {
                oController.m_aoTypes = response.data;


            }
        })
    }


    SubscriptionPurchaseController.$inject = [
        '$scope',
        '$window',
        'ModalService',
        'ConstantsService',
        'SubscriptionService',
        'OrganizationService',
        '$translate'
    ];
    return SubscriptionPurchaseController
})();
window.SubscriptionPurchaseController = SubscriptionPurchaseController;