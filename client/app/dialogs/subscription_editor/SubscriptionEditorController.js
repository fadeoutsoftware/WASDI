SubscriptionEditorController = (function () {
    function SubscriptionEditorController(
        $scope,
        oClose,
        oExtras,
        oSubscriptionService
    ) {
        this.m_oScope = $scope;
        this.m_oExtras = oExtras;
        this.m_oScope.m_oController = this;

        this.m_oSubscriptionService = oSubscriptionService
        this.m_bEditSubscription = false;
        this.m_oEditSubscription = oExtras.subscription;

        this.m_asTypes = [];
        this.m_aoTypesMap = [];
        this.m_oType = {};
        this.m_bLoadingTypes = true;

        console.log(this.m_oExtras)
        this.getSubscriptionTypes();


        $scope.close = function (result) {
            oClose(result, 500)
        }

    }
    SubscriptionEditorController.prototype.getSubscriptionById = function (sSubscriptionId) {

    }
    SubscriptionEditorController.prototype.saveSubscription = function () {
        console.log("EditUserController.saveSubscription | m_oEditSubscription: ", this.m_oEditSubscription);

        let sType = "";

        if (!utilsIsObjectNullOrUndefined(this.m_oType)) {
            sType = this.m_oType.name;
        }

        this.m_oEditSubscription.type = sType;

        let oController = this;
        this.m_oSubscriptionService.saveSubscription(this.m_oEditSubscription).then(function (data) {
            if (utilsIsObjectNullOrUndefined(data.data) === false && data.data.boolValue === true) {
                var oDialog = utilsVexDialogAlertBottomRightCorner("SUBSCRIPTION SAVED<br>READY");
                utilsVexCloseDialogAfter(4000, oDialog);
            } else {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN SAVING SUBSCRIPTION");
            }

        }, function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN SAVING ORGANIZATION");
        });

        this.m_oEditSubscription = {};
        this.m_oType = {};
        this.m_oScope.close();
    }

    SubscriptionEditorController.prototype.getSubscriptionTypes = function () {
        let oController = this;
        oController.m_oSubscriptionService.getSubscriptionTypes().then(
            function (data) {
                if (data.status !== 200) {
                    var oDialog = utilsVexDialogAlertBottomRightCorner(
                        "GURU MEDITATION<br>ERROR GETTING SUBSCRIPTION TYPES"
                    );
                    utilsVexCloseDialogAfter(5000, oDialog);
                } else {
                    oController.m_asTypes = data.data.map((item) => item.name);
                    oController.m_aoTypesMap = oController.m_asTypes.map(
                        (name) => ({ name })
                    );

                    oController.m_aoTypesMap.forEach((oValue, sKey) => {
                        if (oValue.name == oController.m_oEditSubscription.type) {
                            oController.m_oType = oValue;
                        }
                    });
                }

                oController.m_bLoadingTypes = false;
            }, function (data) {
                var oDialog = utilsVexDialogAlertBottomRightCorner(
                    "GURU MEDITATION<br>ERROR GETTING TYPES"
                );
                utilsVexCloseDialogAfter(5000, oDialog);
                oController.m_bLoadingTypes = false;
            }
        );
    }

    SubscriptionEditorController.$inject = [
        '$scope',
        'close',
        'extras',
        'SubscriptionService'
    ];
    return SubscriptionEditorController
})();
window.SubscriptionEditorController = SubscriptionEditorController