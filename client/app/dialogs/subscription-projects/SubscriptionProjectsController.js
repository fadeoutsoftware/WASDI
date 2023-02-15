let SubscriptionProjectsController = (function () {
    function SubscriptionProjectsController($scope, oClose, oExtras, oSubscriptionService, oProjectService, oModalService, oTranslate) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oTranslate = oTranslate;


        this.m_oSubscriptionService = oSubscriptionService;
        this.m_oProjectService = oProjectService;
        this.m_oModalService = oModalService;

        this.m_sSelectedSubscriptionId = this.m_oExtras.subscriptionId;
        this.m_sSelectedSubscriptionName = this.m_oExtras.subscriptionName;



        this.m_aoProjects = this.m_oExtras.projects;

        this.m_aoProjectsMap = [];
        this.m_oProject = {};

        this.m_bLoadingProjects = true;

        this.initializeProjectsInfo();





        $scope.close = function (result) {
            oClose(result, 500)
        }
    }



    /*
    SubscriptionProjectsController.prototype.changeActiveProject = function(oProject) {
        console.log("SubscriptionProjectsController.changeActiveProject | oProject: ", oProject);

        var oController = this;

        if (!utilsIsObjectNullOrUndefined(oProject)) {
            this.m_oProjectService.changeActiveProject(oProject.projectId).then(function (data) {
                console.log("SubscriptionProjectsController.changeActiveProject | data.data: ", data.data);
                if (utilsIsObjectNullOrUndefined(data.data) === false && data.data.boolValue === true) {
                    let oDialog = utilsVexDialogAlertBottomRightCorner("ACTIVE PROJECT CHANGED<br>READY");
                    utilsVexCloseDialogAfter(2000, oDialog);

                    oController.initializeProjectsInfo();
                    this.m_oProject = oProject;
                } else {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN CHANGING THE ACTIVE PROJECT");
                }
    
            }, function (error) {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN CHANGING THE ACTIVE PROJECT");
            });
        }
    }
    */

    SubscriptionProjectsController.prototype.initializeProjectsInfo = function() {
        var oController = this;

        this.m_oProjectService.getProjectsListBySubscription(this.m_sSelectedSubscriptionId).then(
            function (data) {
                if (!utilsIsObjectNullOrUndefined(data.data)) {
                    oController.m_aoProjects = data.data;

                    const oFirstElement = { name: "No Active Project", projectId: null };
                    let aoProjects = [oFirstElement].concat(data.data);

                    oController.m_aoProjectsMap = aoProjects.map(
                        (item) => ({ name: item.name, projectId: item.projectId })
                    );


                    oController.m_oProject = oFirstElement;

                    oController.m_aoProjects.forEach((oValue) => {
                        // console.log("SubscriptionProjectsController.initializeProjectsInfo | oValue: ", oValue);
                        if (oValue.activeProject) {
                            oController.m_oProject = oValue;
                        }
                    });
                } else {
                    utilsVexDialogAlertTop(
                        "GURU MEDITATION<br>ERROR IN GETTING THE LIST OF PROJECTS"
                    );
                }

                oController.m_bLoadingProjects = false;
                console.log("SubscriptionProjectsController.initializeProjectsInfo | oController.m_oProject: ", oController.m_oProject);

                return true;
            }
        );
    }

    SubscriptionProjectsController.prototype.showProjectEditForm = function(sProjectId) {
        var oController = this;

        this.m_oProjectService.getProjectById(sProjectId).then(
            function (data) {
                if (!utilsIsObjectNullOrUndefined(data)
                        && !utilsIsObjectNullOrUndefined(data.data) && data.status === 200) {
                    oController.m_oModalService.showModal({
                        templateUrl: "dialogs/project_editor/ProjectEditorDialog.html",
                        controller: "ProjectEditorController",
                        inputs: {
                            extras: {
                                project: data.data,
                                subscriptionId: oController.m_sSelectedSubscriptionId,
                                subscriptionName: oController.m_sSelectedSubscriptionName,
                                editMode: true
                            }
                        }
                    }).then(function (modal) {
                        modal.element.modal({
                            backdrop: 'static'
                        })
                        modal.close.then(function () {
                            oController.initializeProjectsInfo();
                        })
                    })
                } else {
                    utilsVexDialogAlertTop(
                        "GURU MEDITATION<br>ERROR IN GETTING THE PROJECT BY ID"
                    );
                }
            }, function (error) {
                let sErrorMessage = "GURU MEDITATION<br>ERROR IN FETCHING THE PROJECT";

                if (!utilsIsObjectNullOrUndefined(error.data) && !utilsIsStrNullOrEmpty(error.data.message)) {
                    sErrorMessage += "<br><br>" + error.data.message;
                }

                utilsVexDialogAlertTop(sErrorMessage);
            }
        )
    }

    SubscriptionProjectsController.prototype.showProjectAddForm = function() {
        var oController = this;

        let oNewProject = {};

        this.m_oModalService.showModal({
            templateUrl: "dialogs/project_editor/ProjectEditorDialog.html",
            controller: "ProjectEditorController",
            inputs: {
                extras: {
                    project: oNewProject,
                    subscriptionId: oController.m_sSelectedSubscriptionId,
                    subscriptionName: oController.m_sSelectedSubscriptionName,
                    editMode: true
                }
            }
        }).then(function (modal) {
            modal.element.modal({
                backdrop: 'static'
            })
            modal.close.then(function () {
                oController.initializeProjectsInfo();
            })
        });
    }

    SubscriptionProjectsController.prototype.deleteProject = function(sProjectId) {

        let sConfirmMsg = "Delete this Project?"

        var oController = this;
        
        let oCallbackFunction = function(value) {
            if (value) {
                oController.m_oProjectService.deleteProject(sProjectId)
                    .then(function (data) {

                        if (!utilsIsObjectNullOrUndefined(data) && data.status === 200) {
                            var oDialog = utilsVexDialogAlertBottomRightCorner("PROJECT DELETED<br>READY");
                            utilsVexCloseDialogAfter(4000, oDialog);
                        } else {
                            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN DELETING PROJECT");
                        }

                        oController.initializeProjectsInfo();
                    }, function (error) {
                        let sErrorMessage = "GURU MEDITATION<br>ERROR IN DELETING PROJECT";

                        if (!utilsIsObjectNullOrUndefined(error.data) && !utilsIsStrNullOrEmpty(error.data.message)) {
                            sErrorMessage += "<br><br>" + error.data.message;
                        }

                        utilsVexDialogAlertTop(sErrorMessage);
                    });

            }
        };

        utilsVexDialogConfirm(sConfirmMsg, oCallbackFunction);
    }

    SubscriptionProjectsController.prototype.cancelEditProjectForm = function() {
        console.log("SubscriptionProjectsController.cancelEditProjectForm");
    }



    SubscriptionProjectsController.$inject = [
        "$scope",
        "close",
        "extras",
        "SubscriptionService", 
        "ProjectService", 
        "ModalService",
        '$translate'
    ];
    return SubscriptionProjectsController;
})();
window.SubscriptionProjectsController = SubscriptionProjectsController; 