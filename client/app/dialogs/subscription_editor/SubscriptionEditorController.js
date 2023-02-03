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
        console.log("SubscriptionEditorController | oExtras: ",  oExtras);
        console.log("SubscriptionEditorController | this.m_oExtras: ",  this.m_oExtras);

        this.m_oSubscriptionService = oSubscriptionService;
        this.m_oOrganizationService = oOrganizationService;

        this.m_oEditSubscription = oExtras.subscription;
        this.m_bEditMode = oExtras.editMode;


        this.m_sBuyDate = null;
        this.m_sStartDate = null;
        this.m_sStartDate = null;

        this.initializeDates();

        // this.m_aoTypes = [];
        // this.m_aoTypesMap = [];
        // this.m_oType = {};
        // this.m_bLoadingTypes = true;

        this.m_asOrganizations = [];
        this.m_aoOrganizationsMap = [];
        this.m_oOrganization = {};
        this.m_bLoadingOrganizations = true;

        // this.getSubscriptionTypes();

        this.getOrganizationsListByUser();

        $scope.close = function (result) {
            oClose(result, 500);
        }
    }

    SubscriptionEditorController.prototype.initializeDates = function () {
        console.log("SubscriptionEditorController.initializeDates | this.m_oEditSubscription.startDate: ", this.m_oEditSubscription.startDate);

        if (utilsIsObjectNullOrUndefined(this.m_oEditSubscription.buyDate)) {
            this.m_sBuyDate = null;
        } else {
            this.m_sBuyDate = new Date(this.m_oEditSubscription.buyDate);
        }

        if (utilsIsObjectNullOrUndefined(this.m_oEditSubscription.startDate)) {
            this.m_sStartDate = new Date();
        } else {
            this.m_sStartDate = new Date(this.m_oEditSubscription.startDate);
        }

        if (utilsIsObjectNullOrUndefined(this.m_oEditSubscription.endDate)) {
            this.m_sEndDate = new Date();

            if (this.m_oEditSubscription.typeId.toLowerCase().includes("day")) {
                this.m_sEndDate.setDate(this.m_sStartDate.getDate() + 1);
            } else if (this.m_oEditSubscription.typeId.toLowerCase().includes("week")) {
                this.m_sEndDate.setDate(this.m_sStartDate.getDate() + 7);
            } else if (this.m_oEditSubscription.typeId.toLowerCase().includes("month")) {
                this.m_sEndDate.setMonth(this.m_sStartDate.getMonth() + 1);
            } else if (this.m_oEditSubscription.typeId.toLowerCase().includes("year")) {
                this.m_sEndDate.setFullYear(this.m_sStartDate.getFullYear() + 1);
            }
        } else {
            this.m_sEndDate = new Date(this.m_oEditSubscription.endDate);
        }

        console.log("SubscriptionEditorController.initializeDates | this.m_sStartDate: ", this.m_sStartDate);
    }

    SubscriptionEditorController.prototype.saveSubscription = function (sStartDate) {
        console.log("SubscriptionEditorController.saveSubscription | this.m_oEditSubscription: ", this.m_oEditSubscription);
        console.log("SubscriptionEditorController.saveSubscription | this.m_sStartDate: ", this.m_sStartDate);
        console.log("SubscriptionEditorController.saveSubscription | sStartDate: ", sStartDate);

        // if (utilsIsObjectNullOrUndefined(this.m_oType)) {
        //     this.m_oEditSubscription.typeId = "";
        //     this.m_oEditSubscription.typeName = "";
        // } else {
        //     this.m_oEditSubscription.typeId = this.m_oType.typeId;
        //     this.m_oEditSubscription.typeName = this.m_oType.name;
        // }

        if (utilsIsObjectNullOrUndefined(this.m_oOrganization)) {
            this.m_oEditSubscription.organizationId = "";
        } else {
            this.m_oEditSubscription.organizationId = this.m_oOrganization.organizationId;
        }

        let oController = this;

        this.m_oSubscriptionService.saveSubscription(this.m_oEditSubscription).then(function (data) {
            console.log(" SubscriptionEditorController.saveSubscription | data.data: ", data.data);
            if (!utilsIsObjectNullOrUndefined(data.data) && data.data.boolValue) {
                let oDialog = utilsVexDialogAlertBottomRightCorner("SUBSCRIPTION SAVED<br>READY");
                utilsVexCloseDialogAfter(4000, oDialog);
            } else {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN SAVING SUBSCRIPTION");
            }

            oController.m_oScope.close();

        }, function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN SAVING SUBSCRIPTION");

            oController.m_oScope.close();
        });

        this.m_oEditSubscription = {};
        this.m_oType = {};
        this.m_oOrganization = {};
    }

    /*
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
                    oController.m_aoTypes = data.data;
                    oController.m_aoTypesMap = oController.m_aoTypes.map(
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
    */

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

                oController.m_bLoadingOrganizations = false;
            },
            function (data) {
                var oDialog = utilsVexDialogAlertBottomRightCorner(
                    "GURU MEDITATION<br>ERROR GETTING ORGANIZATIONS"
                );
                utilsVexCloseDialogAfter(4000, oDialog);
                oController.m_bLoadingOrganizations = false;
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