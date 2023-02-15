SubscriptionEditorController = (function () {
    function SubscriptionEditorController(
        $scope,
        $window,
        oClose,
        oExtras,
        oConstantsService,
        oSubscriptionService,
        oOrganizationService
    ) {
        this.m_oScope = $scope;
        this.m_oWindow = $window;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;

        this.m_oConstantsService = oConstantsService;
        this.m_oSubscriptionService = oSubscriptionService;
        this.m_oOrganizationService = oOrganizationService;

        this.m_oEditSubscription = oExtras.subscription;
        this.m_bEditMode = oExtras.editMode;


        this.m_sBuyDate = null;
        this.m_sStartDate = null;
        this.m_sEndDate = null;
        this.m_lDurationDays = 0;

        this.initializeDates();

        this.m_asOrganizations = [];
        this.m_aoOrganizationsMap = [];
        this.m_oOrganization = {};
        this.m_bLoadingOrganizations = true;

        this.getOrganizationsListByUser();

        $scope.close = function (result) {
            oClose(result, 500);
        }
    }

    SubscriptionEditorController.prototype.initializeDates = function () {
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

        let lDifferenceInTime = this.m_sEndDate.getTime() - this.m_sStartDate.getTime();
        this.m_lDurationDays = lDifferenceInTime / (1000 * 3600 * 24);
        this.m_lDurationDays = Math.round(this.m_lDurationDays);
    }

    SubscriptionEditorController.prototype.selectDate = function () {
        let oStartDate = new Date(this.m_sStartDate);
        this.m_sEndDate = new Date(this.m_sStartDate);

        if (this.m_oEditSubscription.typeId.toLowerCase().includes("day")) {
            this.m_sEndDate.setDate(oStartDate.getDate() + 1);
        } else if (this.m_oEditSubscription.typeId.toLowerCase().includes("week")) {
            this.m_sEndDate.setDate(oStartDate.getDate() + 7);
        } else if (this.m_oEditSubscription.typeId.toLowerCase().includes("month")) {
            this.m_sEndDate.setMonth(oStartDate.getMonth() + 1);
        } else if (this.m_oEditSubscription.typeId.toLowerCase().includes("year")) {
            this.m_sEndDate.setFullYear(oStartDate.getFullYear() + 1);
        }

        let lDifferenceInTime = this.m_sEndDate.getTime() - oStartDate.getTime();
        this.m_lDurationDays = lDifferenceInTime / (1000 * 3600 * 24);
        this.m_lDurationDays = Math.round(this.m_lDurationDays);
    }

    SubscriptionEditorController.prototype.saveSubscription = function (sStartDate) {

        if (utilsIsObjectNullOrUndefined(this.m_oOrganization)) {
            this.m_oEditSubscription.organizationId = "";
        } else {
            this.m_oEditSubscription.organizationId = this.m_oOrganization.organizationId;
        }

        if (utilsIsObjectNullOrUndefined(this.m_oEditSubscription.startDate)) {
            this.m_oEditSubscription.startDate = new Date(this.m_sStartDate);
        }

        if (utilsIsObjectNullOrUndefined(this.m_oEditSubscription.endDate)) {
            this.m_oEditSubscription.endDate = new Date(this.m_sEndDate);
        }

        if (utilsIsObjectNullOrUndefined(this.m_oEditSubscription.durationDays)) {
            this.m_oEditSubscription.durationDays = this.m_lDurationDays;
        }

        let oController = this;

        this.m_oSubscriptionService.saveSubscription(this.m_oEditSubscription).then(function (data) {
            console.log("SubscriptionEditorController.saveSubscription | data.data: ", data.data);
            if (!utilsIsObjectNullOrUndefined(data)
                        && !utilsIsObjectNullOrUndefined(data.data) && data.status === 200) {
                let oDialog = utilsVexDialogAlertBottomRightCorner("SUBSCRIPTION SAVED<br>READY");
                utilsVexCloseDialogAfter(4000, oDialog);

                let oActiveWorkspace = oController.m_oConstantsService.getActiveWorkspace();
                let sActiveWorkspaceId = oActiveWorkspace == null ? null : oActiveWorkspace.workspaceId;

                console.log("SubscriptionEditorController.saveSubscription | oActiveWorkspace: ", oActiveWorkspace);
                console.log("SubscriptionEditorController.saveSubscription | sActiveWorkspaceId: ", sActiveWorkspaceId);

                oController.m_oSubscriptionService.getStripePaymentUrl(data.data.message, sActiveWorkspaceId).then(function (data) {
                    console.log("SubscriptionEditorController.saveSubscription | getStripePaymentUrl | data.data: ", data.data);
                    if (!utilsIsObjectNullOrUndefined(data.data) && data.data.boolValue) {
                        let oDialog = utilsVexDialogAlertBottomRightCorner("PAYMENT URL RECEIVED<br>READY");
                        utilsVexCloseDialogAfter(4000, oDialog);

                        let sUrl = data.data.stringValue;
                        console.log(" SubscriptionEditorController.saveSubscription | getStripePaymentUrl | sUrl: ", sUrl);

                        oController.m_oWindow.open(sUrl, '_blank');
                    }

                    oController.m_oScope.close();
                }, function (error) {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN RETRIEVING THE PAYMENT URL");

                    oController.m_oScope.close();
                });
            } else {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN SAVING SUBSCRIPTION");

                oController.m_oScope.close();
            }

        }, function (error) {
            let sErrorMessage = "GURU MEDITATION<br>ERROR IN SAVING SUBSCRIPTION";

            if (!utilsIsObjectNullOrUndefined(error.data) && !utilsIsStrNullOrEmpty(error.data.message)) {
                sErrorMessage += "<br><br>" + error.data.message;
            }

            utilsVexDialogAlertTop(sErrorMessage);

            oController.m_oScope.close();
        });

        this.m_oEditSubscription = {};
        this.m_oType = {};
        this.m_oOrganization = {};
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
        '$window',
        'close',
        'extras',
        'ConstantsService',
        'SubscriptionService',
        'OrganizationService'
    ];
    return SubscriptionEditorController
})();
window.SubscriptionEditorController = SubscriptionEditorController