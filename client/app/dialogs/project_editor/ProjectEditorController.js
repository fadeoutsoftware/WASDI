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

        this.m_oEditProject = this.m_oExtras.project;

        this.m_bEditMode = this.m_oExtras.editMode;

        this.m_oSubscription = {subscriptionId: this.m_oExtras.subscriptionId, name: this.m_oExtras.subscriptionName};

        this.initializeProjectInfo();

        /**
         * ActiveProject flag
         * @type {boolean}
         */
        this.m_oActiveProject = true;

        if (this.m_oEditProject.activeProject) {
            this.m_oActiveProject = true;
        } else {
            this.m_oActiveProject = false;
        }

        $scope.close = function (result) {
            oClose(result, 500);
        }
    }
    
    ProjectEditorController.prototype.initializeProjectInfo = function() {
        if (utilsIsStrNullOrEmpty(this.m_oEditProject.projectId)) {
            this.m_oEditProject.subscriptionId = this.m_oSubscription.subscriptionId;
        }

        this.m_oEditProject.subscriptionName = this.m_oSubscription.name;
    }

    ProjectEditorController.prototype.saveProject = function () {
        if (utilsIsObjectNullOrUndefined(this.m_oSubscription)) {
            this.m_oEditProject.subscriptionId = "";
        } else {
            this.m_oEditProject.subscriptionId = this.m_oSubscription.subscriptionId;
        }

        let oController = this;

        this.m_oEditProject.activeProject = this.m_oActiveProject;

        this.m_oProjectService.saveProject(this.m_oEditProject).then(function (data) {
            if (!utilsIsObjectNullOrUndefined(data)
                        && !utilsIsObjectNullOrUndefined(data.data) && data.status === 200) {
                let oDialog = utilsVexDialogAlertBottomRightCorner("PROJECT SAVED<br>READY");
                utilsVexCloseDialogAfter(2000, oDialog);
            } else {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN SAVING PROJECT");
            }

            oController.m_oScope.close();

        }, function (error) {
            let sErrorMessage = "GURU MEDITATION<br>ERROR IN SAVING PROJECT";

            if (!utilsIsObjectNullOrUndefined(error.data) && !utilsIsStrNullOrEmpty(error.data.message)) {
                sErrorMessage += "<br><br>" + error.data.message;
            }

            utilsVexDialogAlertTop(sErrorMessage);

            oController.m_oScope.close();
        });

        this.m_oEditProject = {};
        this.m_oSubscription = {};
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