SubscriptionEditorController = (function () {
    function SubscriptionEditorController(
        $scope,
        oClose,
        oExtras,
        oSubscriptionService,
        oOrganizationService
    ) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;

        this.m_oSubscriptionService = oSubscriptionService;
        this.m_oOrganizationService = oOrganizationService;

        this.m_oEditSubscription = oExtras.subscription;
        this.m_bEditMode = oExtras.editMode;

        this.m_asTypes = [];
        this.m_aoTypesMap = [];
        this.m_oType = {};
        this.m_bLoadingTypes = true;

        this.m_asOrganizations = [];
        this.m_aoOrganizationsMap = [];
        this.m_oOrganization = {};
        this.m_bLoadingOrganization = true;

        this.getSubscriptionTypes();

        this.getOrganizationsListByUser();

        $scope.close = function (result) {
            oClose(result, 500);
        }
    }

    SubscriptionEditorController.prototype.saveSubscription = function () {
        console.log("SubscriptionEditorController.saveSubscription");

        if (utilsIsObjectNullOrUndefined(this.m_oType)) {
            this.m_oEditSubscription.typeId = "";
            this.m_oEditSubscription.typeName = "";
        } else {
            this.m_oEditSubscription.typeId = this.m_oType.typeId;
            this.m_oEditSubscription.typeName = this.m_oType.name;
        }

        if (utilsIsObjectNullOrUndefined(this.m_oOrganization)) {
            this.m_oEditSubscription.organizationId = "";
        } else {
            this.m_oEditSubscription.organizationId = this.m_oOrganization.organizationId;
        }

        this.m_oSubscriptionService.saveSubscription(this.m_oEditSubscription).then(function (data) {
            console.log(" SubscriptionEditorController.saveSubscription | data.data: ", data.data);
            if (utilsIsObjectNullOrUndefined(data.data) === false && data.data.boolValue === true) {
                let oDialog = utilsVexDialogAlertBottomRightCorner("SUBSCRIPTION SAVED<br>READY");
                utilsVexCloseDialogAfter(4000, oDialog);
            } else {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN SAVING SUBSCRIPTION");
            }

        }, function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN SAVING SUBSCRIPTION");
        });

        this.m_oEditSubscription = {};
        this.m_oType = {};
        this.m_oOrganization = {};
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
                    utilsVexCloseDialogAfter(4000, oDialog);
                } else {
                    oController.m_asTypes = data.data;
                    oController.m_aoTypesMap = oController.m_asTypes.map(
                        (item) => ({ name: item.name, typeId: item.typeId })
                    );

                    oController.m_aoTypesMap.forEach((oValue, sKey) => {
                        if (oValue.typeId == oController.m_oEditSubscription.typeId) {
                            oController.m_oType = oValue;
                        }
                    });
                }

                oController.m_bLoadingTypes = false;
            }, function (data) {
                var oDialog = utilsVexDialogAlertBottomRightCorner(
                    "GURU MEDITATION<br>ERROR GETTING TYPES"
                );
                utilsVexCloseDialogAfter(4000, oDialog);
                oController.m_bLoadingTypes = false;
            }
        );
    }

    SubscriptionEditorController.prototype.getOrganizationsListByUser = function () {
        let oController = this;

        this.m_oOrganizationService.getOrganizationsListByUser().then(
            function (data) {
                if (data.status !== 200) {
                    let oDialog = utilsVexDialogAlertBottomRightCorner(
                        "GURU MEDITATION<br>ERROR GETTING ORGANIZATIONS"
                    );
                    utilsVexCloseDialogAfter(4000, oDialog);
                } else {
                    const oFirstElement = { name: "No Organization", organizationId: null };
                    oController.m_asOrganizations = [oFirstElement].concat(data.data);
                    oController.m_aoOrganizationsMap = oController.m_asOrganizations.map(
                        (item) => ({ name: item.name, organizationId: item.organizationId })
                    );

                    oController.m_aoOrganizationsMap.forEach((oValue, sKey) => {
                        if (oValue.organizationId == oController.m_oEditSubscription.organizationId) {
                            oController.m_oOrganization = oValue;
                        }
                    });
                }

                oController.m_bLoadingOrganization = false;
            },
            function (data) {
                var oDialog = utilsVexDialogAlertBottomRightCorner(
                    "GURU MEDITATION<br>ERROR GETTING ORGANIZATIONS"
                );
                utilsVexCloseDialogAfter(4000, oDialog);
                oController.m_bLoadingOrganization = false;
            }
        );
    }

    SubscriptionEditorController.$inject = [
        '$scope',
        'close',
        'extras',
        'SubscriptionService',
        'OrganizationService'
    ];
    return SubscriptionEditorController
})();
window.SubscriptionEditorController = SubscriptionEditorController