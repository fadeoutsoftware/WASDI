let SubscriptionProjectsController = (function () {
    function SubscriptionProjectsController($scope, oClose, oExtras, oSubscriptionService, oProjectService, oModalService, oTranslate) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.oExtras = oExtras;
        this.m_oTranslate = oTranslate;

        this.m_oSubscriptionService = oSubscriptionService;
        this.m_oProjectService = oProjectService;
        this.m_oModalService = oModalService; 

        this.m_sSelectedSubscriptionId = this.oExtras.subscriptionId;
        console.log("SubscriptionProjectsController | this.m_sSelectedSubscriptionId: ", this.m_sSelectedSubscriptionId);




        this.m_aoProjects = oExtras.projects;

        this.m_aoProjectsMap = [];
        this.m_oProject = {};

        this.m_bLoadingProjects = true;

        this.initializeProjectsInfo();





        $scope.close = function (result) {
            oClose(result, 500)
        }
    }




    SubscriptionProjectsController.prototype.changeDefaultProject = function(oProject) {
        console.log("SubscriptionProjectsController.changeDefaultProject | oProject: ", oProject);

        var oController = this;

        if (!utilsIsObjectNullOrUndefined(oProject)) {
            this.m_oProjectService.changeDefaultProject(oProject.projectId).then(function (data) {
                console.log("SubscriptionProjectsController.changeDefaultProject | data.data: ", data.data);
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

    SubscriptionProjectsController.prototype.initializeProjectsInfo = function() {
        var oController = this;

        this.m_oProjectService.getProjectsListBySubscription(this.m_sSelectedSubscriptionId).then(
            function (data) {
                if (!utilsIsObjectNullOrUndefined(data.data)) {
                    oController.m_aoProjects = data.data;

                    const oFirstElement = { name: "No Default Project", projectId: null };
                    let aoProjects = [oFirstElement].concat(data.data);

                    oController.m_aoProjectsMap = aoProjects.map(
                        (item) => ({ name: item.name, projectId: item.projectId })
                    );


                    oController.m_oProject = oFirstElement;

                    oController.m_aoProjects.forEach((oValue) => {
                        console.log("SubscriptionProjectsController.initializeProjectsInfo | oValue: ", oValue);
                        if (oValue.defaultProject === true) {
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


    SubscriptionProjectsController.prototype.showProjectEditForm = function(sUserId, sProjectId, sEditMode) {
        console.log("SubscriptionProjectsController.showProjectEditForm | sProjectId: ", sProjectId);

        var oController = this;


        this.m_oProjectService.getProjectById(sProjectId).then(
            function (data) {
                if (utilsIsObjectNullOrUndefined(data.data) === false) {
                    oController.m_oModalService.showModal({
                        templateUrl: "dialogs/project_editor/ProjectEditorDialog.html",
                        controller: "ProjectEditorController",
                        inputs: {
                            extras: {
                                project: data.data,
                                editMode: sEditMode
                            }
                        }
                    }).then(function (modal) {
                        modal.element.modal({
                            backdrop: 'static'
                        })
                        modal.close.then(function () {
                            console.log("SubscriptionProjectsController.showProjectEditForm | calling initializeProjectsInfo() ");
                            oController.initializeProjectsInfo();
                        })
                    })
                } else {
                    utilsVexDialogAlertTop(
                        "GURU MEDITATION<br>ERROR IN GETTING THE PROJECT BY ID"
                    );
                }
            }
        )
    }

    SubscriptionProjectsController.prototype.deleteProject = function(sUserId, sProjectId) {
        console.log("SubscriptionProjectsController.deleteProject | sProjectId: ", sProjectId);

        var oController = this;

        this.m_oProjectService.deleteProject(sProjectId)
            .then(function (data) {
                if(utilsIsObjectNullOrUndefined(data.data) === false && data.data.boolValue === true) {
                    var oDialog = utilsVexDialogAlertBottomRightCorner("PROJECT DELETED<br>READY");
                    utilsVexCloseDialogAfter(4000, oDialog);
                } else if(utilsIsObjectNullOrUndefined(data.data) === false && data.data.boolValue === false) {
                    var oDialog = utilsVexDialogAlertBottomRightCorner(data.data.stringValue);
                    utilsVexCloseDialogAfter(5000, oDialog);
                } else {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN DELETING PROJECT");
                }

                oController.initializeProjectsInfo();

            },function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN DELETING PROJECT");
        });
    }

    SubscriptionProjectsController.prototype.cancelEditProjectForm = function() {
        console.log("SubscriptionProjectsController.cancelEditProjectForm");

        this.m_aoMatchingOrganizationsList = [];
        this.m_sOrganizationPartialName = "";
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