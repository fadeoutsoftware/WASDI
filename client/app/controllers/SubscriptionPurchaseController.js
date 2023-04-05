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

        this.m_aoSubscriptionTypes = []; 

        this.getSubscriptionTypes();
    }


    SubscriptionPurchaseController.prototype.showSubscriptionAddForm = function (typeId, typeName) {
        var oController = this;

        let oNewSubscription = {
            subscriptionId: null,
            typeId: typeId,
            typeName: typeName,
            buySuccess: false
        };

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
            modal.close.then(function () {
                //oController.initializeSubscriptionsInfo();
            })
        });
    }

    SubscriptionPurchaseController.prototype.getSubscriptionTypes = function () {
        this.m_oSubscriptionService.getSubscriptionTypes().then(function (response) {
           if(!utilsIsObjectNullOrUndefined(response)) {
            this.m_aoSubscriptionTypes = response.data; 
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