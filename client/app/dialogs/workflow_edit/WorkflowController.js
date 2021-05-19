
var WorkflowController = (function () {

    function WorkflowController($scope, oExtras, oConstantsService, oSnapOperationService) {

        /**
         * Angular Scope
         */
        this.m_oScope = $scope;
        /**
         * Reference to the controller
         */
        this.m_oScope.m_oController = this;
        this.m_oConstantService = oConstantsService;
        this.m_oSnapOperationService = oSnapOperationService;
        /**
         * First tab visualized
         * @type {string}
         */
        this.m_sSelectedTab = "Base";
        /**
         * Extras injected from modal invoker
         */
        this.m_oExtras = oExtras;
        /**
         * Object with the infos about the current workflow
         */
        this.m_oWorkflow = this.m_oExtras.workflow;
        /**
         * Field to add sharing
         * @type {string}
         */
        this.m_sUserEmail = "";
        /**
         * Field with list of active sharing
         */
        this.m_aoEnabledUsers = [];

        /**
         * Field with the current uploaded file
         */
        this.m_oFile = undefined;

        /* Close this Dialog handler
        $scope.close = function () {
            // close, but give 500ms for bootstrap to animate
            oClose(null, 300);
        };*/


        /**
         * Init the list of users which this workflow is shared with
         */
        this.getListOfEnabledUsers(this.m_oWorkflow.workflowId);



    }
    /**
     * Util to check if the file is uploaded
     * @returns true if the file is uploaded, false instead
     */
    WorkflowController.prototype.isFileLoaded = function () {
        return true; // for debugging reasons
        //return !(this.m_oFile === undefined);
    }



    WorkflowController.prototype.shareWorkflowByUserEmail = function (oUserId) {
        var oController = this;
        this.m_oSnapOperationService.addWorkflowSharing(this.m_oWorkflow.workflowId, oUserId)
            .then(function (data) {
                if (utilsIsObjectNullOrUndefined(data.data) === false && data.data.boolValue === true) {
                    // all done
                    oController.getListOfEnabledUsers(oController.m_oWorkflow.workflowId);
                }
                else {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR SHARING WORKFLOW");
                }
                // reload the sharing list

            }, function (error) {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR SHARING WORKFLOW");
            });
    };

    /**
     * Invokes the API for graph deletion. It handles the request by deleting the
     * workflow if invoked by the Owner, and delete the sharing if invoked by another user
     * @param {*} oUserId the user ID invoking the API
     */
    WorkflowController.prototype.deleteWorkflow = function (oUserId) {
        this.m_oSnapOperationService.deleteWorkflow(this.m_oWorkflow.workflowId, oUserId);
    }

    /**
     * Invokes the deletion of the sharing between the current workflow and the
     * user identified by UserId
     * @param {*} oUserId the identifier of the User
     */
    WorkflowController.prototype.removeUserSharing = function (oUserId) {
        var oController = this;
        this.m_oSnapOperationService.removeWorkflowSharing(this.m_oWorkflow.workflowId, oUserId).then(function (data) {
            if (utilsIsObjectNullOrUndefined(data.data) === false && data.data.boolValue === true) {
                // all done
                oController.getListOfEnabledUsers(oController.m_oWorkflow.workflowId);
            }
            else {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR DELETING SHARING WORKFLOW");
            }
            // reload the sharing list

        }, function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR DELETING SHARING WORKFLOW");
        });
    };

    /**
     * uploadUserGraphOnServer
     */

    WorkflowController.prototype.uploadUserGraphOnServer = function () {

        var oBody = new FormData();
        oBody.append('file', this.m_oFile[0]);
        //this.m_oConstantService.getActiveWorkspace().sWorkspaceId
        this.uploadGraph("idworkspace", // Current Workspace from constant service <-> unused on API
            this.m_oWorkflow.name, this.m_oWorkflow.description, this.m_oWorkflow.public, // name, description and boolean for isPublic
            oBody); // content of the file

    };



    WorkflowController.isUploadedWorkFlow = function () {
        return !utilsIsObjectNullOrUndefined(this.m_oFile);
    }

    /**
 * uploadGraph
 * @param sWorkspaceId
 * @param sName
 * @param sDescription
 * @param oBody
 * @returns {boolean}
 */

    WorkflowController.prototype.uploadGraph = function (sWorkspaceId, sName, sDescription, bIsPublic, oBody) {

        if (utilsIsObjectNullOrUndefined(sName) === true || utilsIsStrNullOrEmpty(sName) === true) {
            return false;
        }
        if (utilsIsObjectNullOrUndefined(sDescription) === true)//|| utilsIsStrNullOrEmpty(sDescription) === true
        {
            return false;
        }
        if (utilsIsObjectNullOrUndefined(oBody) === true) {
            return false;
        }
        this.isUploadingWorkflow = true;
        var oController = this;
        this.m_oSnapOperationService.uploadGraph("workspace", sName, sDescription, oBody, bIsPublic).then(function (data) {
            if (utilsIsObjectNullOrUndefined(data.data) == false) {
                //Reload list o workFlows
                var oDialog = utilsVexDialogAlertBottomRightCorner("SUCCESSFUL UPLOAD, UPDATED WORKFLOW");
                utilsVexCloseDialogAfter(4000, oDialog);

            } else {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN UPLOAD WORKFLOW PROCESS");
            }

            oController.isUploadingWorkflow = false;
        }, function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN UPLOAD WORKFLOW PROCESS");
            oController.cleanAllUploadWorkflowFields();
            oController.isUploadingWorkflow = false;
        });

        return true;
    };


    /**
     * Updates files and parameters of the graph
     * @param {*} sWorkflowId
     * @param {*} sName
     * @param {*} sDescription
     * @param {*} bIsPublic
     * @param {*} oBody
     * @returns
     */
    WorkflowController.prototype.updateGraph = function () {

        this.isUploadingWorkflow = true;
        var oController = this;
        // update name, description, public
        oController.m_oSnapOperationService.updateGraphParameters(this.m_oWorkflow.workflowId,
            oController.m_oWorkflow.name,
            oController.m_oWorkflow.description,
            oController.m_oWorkflow.public).then(function () {
                // update file only if File is uploaded
                if (oController.m_oFile != undefined) {
                    var oBody = new FormData();
                        oBody.append('file', oController.m_oFile[0]);
                        oController.m_oSnapOperationService.updateGraphFile(oController.m_oWorkflow.workflowId, oBody).then(function (data) {
                        if (utilsIsObjectNullOrUndefined(data.data) == false) {
                            var oDialog = utilsVexDialogAlertBottomRightCorner("SUCCESSFULLY UPDATED WORKFLOW");
                            utilsVexCloseDialogAfter(4000, oDialog);
                        } else {
                            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN UPDATE WORKFLOW PROCESS");
                        }

                        oController.isUploadingWorkflow = false;
                    }, function (error) {
                        utilsVexDialogAlertTop("GURU MEDITATION<br>INVALID SNAP GRAPH FILE");
                        oController.cleanAllUploadWorkflowFields();
                        oController.isUploadingWorkflow = false;
                    });
                }
                else {
                    var oDialog = utilsVexDialogAlertBottomRightCorner("SUCCESSFULLY UPDATED WORKFLOW FIELDS");
                    utilsVexCloseDialogAfter(4000, oDialog);
                }
            }, function (error) {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN UPDATE WORKFLOW PROCESS");
            });

        return true;
    };


    WorkflowController.prototype.getListOfEnabledUsers = function (sWorkflowId) {

        if (utilsIsStrNullOrEmpty(sWorkflowId) === true) {
            return false;
        }
        var oController = this;
        this.m_oSnapOperationService.getUsersBySharedWorkflow(sWorkflowId)
            .then(function (data) {
                if (utilsIsObjectNullOrUndefined(data.data) === false) {
                    oController.m_aoEnabledUsers = data.data;
                }
                else {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR GETTING WORKFLOW SHARINGS");
                }

            }, function (error) {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR GETTING WORKFLOW SHARINGS");
            });
        return true;
    };

    WorkflowController.prototype.getWorkflowSharings = function (sWorkflowId) {
        this.m_aoEnabledUsers = this.m_oSnapOperationService.getWorkflowSharing(sWorkflowId);
    }

    WorkflowController.prototype.iAmTheOwner = function () {
        return (this.m_oConstantService.getUser().userId === this.m_oWorkflow.userId);
    }
    WorkflowController.$inject = [
        '$scope',
        'extras',
        'ConstantsService',
        'SnapOperationService'
    ]


    return WorkflowController;
})();
