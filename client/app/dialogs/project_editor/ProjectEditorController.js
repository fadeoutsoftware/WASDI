ProjectEditorController = (function () {
    function ProjectEditorController(
        $scope,
        oClose,
        oExtras,
        oProjectService,
        oSubscriptionService
    ) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;

        this.m_oProjectService = oProjectService;
        this.m_oSubscriptionService = oSubscriptionService;

        this.m_oEditProject = oExtras.project;
        this.m_bEditMode = oExtras.editMode;

        this.m_aoSubscriptions = [];
        this.m_aoSubscriptionsMap = [];
        this.m_oSubscription = {};
        this.m_bLoadingSubscriptions = true;

        this.getSubscriptionsListByUser();

        /**
         * DefaultProject flag
         * @type {boolean}
         */
        this.m_oDefaultProject = true;

        if (this.m_oEditProject.defaultProject) {
            this.m_oDefaultProject = true;
        } else {
            this.m_oDefaultProject = false;
        }

        $scope.close = function (result) {
            oClose(result, 500);
        }
    }

    ProjectEditorController.prototype.saveProject = function () {
        console.log("ProjectEditorController.saveProject");

        if (utilsIsObjectNullOrUndefined(this.m_oSubscription)) {
            this.m_oEditProject.subscriptionId = "";
        } else {
            this.m_oEditProject.subscriptionId = this.m_oSubscription.subscriptionId;
        }

            this.m_oEditProject.defaultProject = this.m_oDefaultProject;

        this.m_oProjectService.saveProject(this.m_oEditProject).then(function (data) {
            console.log("ProjectEditorController.saveProject | data.data: ", data.data);
            if (utilsIsObjectNullOrUndefined(data.data) === false && data.data.boolValue === true) {
                let oDialog = utilsVexDialogAlertBottomRightCorner("PROJECT SAVED<br>READY");
                utilsVexCloseDialogAfter(2000, oDialog);
            } else {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN SAVING PROJECT");
            }

        }, function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN SAVING PROJECT");
        });

        this.m_oEditProject = {};
        this.m_oSubscription = {};
        this.m_oScope.close();
    }

    ProjectEditorController.prototype.getSubscriptionsListByUser = function () {
        let oController = this;

        console.log("ProjectEditorController.getSubscriptionsListByUser | this.m_oSubscriptionService: ", this.m_oSubscriptionService);

        this.m_oSubscriptionService.getSubscriptionsListByUser().then(
            function (data) {
                if (data.status !== 200) {
                    let oDialog = utilsVexDialogAlertBottomRightCorner(
                        "GURU MEDITATION<br>ERROR GETTING SUBSCRIPTIONS"
                    );
                    utilsVexCloseDialogAfter(4000, oDialog);
                } else {
                    oController.m_aoSubscriptions = data.data;
                    oController.m_aoSubscriptionsMap = oController.m_aoSubscriptions.map(
                        (item) => ({ name: item.name, subscriptionId: item.subscriptionId })
                    );

                    oController.m_aoSubscriptionsMap.forEach((oValue, sKey) => {
                        if (oValue.subscriptionId == oController.m_oEditProject.subscriptionId) {
                            oController.m_oSubscription = oValue;
                        }
                    });
                }

                oController.m_bLoadingSubscriptions = false;
            },
            function (data) {
                var oDialog = utilsVexDialogAlertBottomRightCorner(
                    "GURU MEDITATION<br>ERROR GETTING SUBSCRIPTIONS"
                );
                utilsVexCloseDialogAfter(4000, oDialog);
                oController.m_bLoadingSubscriptions = false;
            }
        );
    }

    ProjectEditorController.$inject = [
        '$scope',
        'close',
        'extras',
        'ProjectService',
        'SubscriptionService'
    ];
    return ProjectEditorController
})();
window.ProjectEditorController = ProjectEditorController