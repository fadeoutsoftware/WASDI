SubscriptionEditorController = (function () {
    function SubscriptionEditorController(
        $scope,
        $window,
        oClose,
        oExtras,
        oModalService,
        oConstantsService,
        oSubscriptionService,
        oOrganizationService,
        oTranslate
    ) {
        this.m_oScope = $scope;
        this.m_oWindow = $window;
        this.m_oScope.m_oController = this;
        this.m_oClose = oClose;
        this.m_oTranslate = oTranslate;
        this.m_oConstantsService = oConstantsService;
        this.m_oSubscriptionService = oSubscriptionService;
        this.m_oOrganizationService = oOrganizationService;

        this.m_oModalService = oModalService;

        this.m_oEditSubscription = oExtras.subscription;
        this.m_bEditMode = oExtras.editMode;
        this.m_iIndex = oExtras.index

        this.m_sBuyDate = null;
        this.m_sStartDate = null;
        this.m_sEndDate = null;
        this.m_lDurationDays = 0;

        this.m_bIsPaid = false;

        this.m_asOrganizations = [];
        this.m_aoOrganizationsMap = [];
        this.m_oOrganization = {};
        this.m_bLoadingOrganizations = true;

        this.initializeSubscriptionInfo();

        //close function 
        $scope.close = function (result, index) {
            oClose(result, index, 500);
        }
    }


    SubscriptionEditorController.prototype.createSubscriptionObject = function () {
        if (!this.m_oEditSubscription.name) {
            this.m_oEditSubscription.name = `${this.m_oEditSubscription.typeName} ${new Date().toISOString()}`
        }
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
    }


    SubscriptionEditorController.prototype.initializeSubscriptionInfo = function () {
        if (utilsIsStrNullOrEmpty(this.m_oEditSubscription.subscriptionId)) {
            this.m_bIsPaid = this.m_oEditSubscription.buySuccess;
            this.initializeDates();
            this.getOrganizationsListByUser();

        } else {
            var oController = this;

            this.m_oSubscriptionService.getSubscriptionById(this.m_oEditSubscription.subscriptionId).then(
                function (response) {
                    if (!utilsIsObjectNullOrUndefined(response)
                        && !utilsIsObjectNullOrUndefined(response.data) && response.status === 200) {
                        oController.m_oEditSubscription = response.data;
                        oController.m_bIsPaid = oController.m_oEditSubscription.buySuccess;
                        oController.initializeDates();
                        oController.getOrganizationsListByUser();
                    } else {
                        utilsVexDialogAlertTop(
                            "GURU MEDITATION<br>ERROR IN GETTING THE SUBSCRIPTION BY ID"
                        );
                    }
                }, function (error) {
                    let sErrorMessage = "GURU MEDITATION<br>ERROR IN FETCHING THE SUBSCRIPTION";

                    if (!utilsIsObjectNullOrUndefined(error.data) && !utilsIsStrNullOrEmpty(error.data.message)) {
                        sErrorMessage += "<br><br>" + oController.m_oTranslate.instant(error.data.message);
                    }

                    utilsVexDialogAlertTop(sErrorMessage);
                }
            )
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

    SubscriptionEditorController.prototype.saveSubscription = function () {
        this.createSubscriptionObject();
        console.log(this.m_oEditSubscription);
        let oController = this;

        this.m_oSubscriptionService.saveSubscription(this.m_oEditSubscription).then(function (response) {
            if (!utilsIsObjectNullOrUndefined(response)
                && !utilsIsObjectNullOrUndefined(response.data) && response.status === 200) {

                oController.m_oEditSubscription.subscriptionId = response.data.message;

                oController.initializeSubscriptionInfo();
                oController.initializeDates();

                let oDialog = utilsVexDialogAlertBottomRightCorner("SUBSCRIPTION SAVED<br>READY");
                utilsVexCloseDialogAfter(4000, oDialog);
                return true;
            } else {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN SAVING SUBSCRIPTION");
            }

            if (oController.m_bIsPaid) {
                oController.m_oScope.close();
            }
        }, function (error) {
            let sErrorMessage = "GURU MEDITATION<br>ERROR IN SAVING SUBSCRIPTION";

            if (!utilsIsObjectNullOrUndefined(error.data) && !utilsIsStrNullOrEmpty(error.data.message)) {
                sErrorMessage += "<br><br>" + oController.m_oTranslate.instant(error.data.message);
            }

            utilsVexDialogAlertTop(sErrorMessage);

            if (oController.m_bIsPaid) {
                oController.m_oScope.close();
            }
        });

        // if (this.m_bIsPaid) {
        //     this.m_oEditSubscription = {};
        //     this.m_oType = {};
        //     this.m_oOrganization = {};
        // }
    }
    SubscriptionEditorController.prototype.getStripePaymentUrl = function () {
        let oController = this;

        let oActiveWorkspace = this.m_oConstantsService.getActiveWorkspace();
        let sActiveWorkspaceId = oActiveWorkspace == null ? null : oActiveWorkspace.workspaceId;

        this.m_oSubscriptionService.getStripePaymentUrl(this.m_oEditSubscription.subscriptionId, sActiveWorkspaceId).then(function (response) {
            if (!utilsIsObjectNullOrUndefined(response.data) && response.data.message) {
                let oDialog = utilsVexDialogAlertBottomRightCorner("PAYMENT URL RECEIVED<br>READY");
                utilsVexCloseDialogAfter(4000, oDialog);

                let sUrl = response.data.message;

                oController.m_oWindow.open(sUrl, '_blank');
            }


        }, function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN RETRIEVING THE PAYMENT URL");

            oController.m_oScope.close();
        });
    }

    SubscriptionEditorController.prototype.checkout = function () {

        if (!this.m_oEditSubscription.subscriptionId) {
            this.saveSubscription();
        //     if (this.saveSubscription()) {
        //         this.getStripePaymentUrl();
        //     }


        }

        //this.getStripePaymentUrl();
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
        'ModalService',
        'ConstantsService',
        'SubscriptionService',
        'OrganizationService',
        '$translate'
    ];
    return SubscriptionEditorController
})();
window.SubscriptionEditorController = SubscriptionEditorController