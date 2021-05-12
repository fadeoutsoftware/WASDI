
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

        // Close this Dialog handler
        $scope.close = function () {
            // close, but give 500ms for bootstrap to animate
            oClose(null, 300);
        };
        /**
         * Init the list of users which this workflow is shared with
         */
        this.getListOfEnableUsers(this.m_oWorkflow.workflowId);


    }
    WorkflowController.prototype.shareWorkflowByUserEmail = function (oUserId) {
        this.m_oSnapOperationService.addWorkflowSharing(this.m_oWorkflow.workflowId, oUserId);
    }

    /**
     * Invokes the API for graph deletion. It handles the request by deleting the 
     * workflow if invoked by the Owner, and delete the sharing if invoked by another user
     * @param {*} oUserId the user ID invoking the API
     */
    WorkflowController.prototype.deleteWorkflow = function (oUserId) {
        this.m_oSnapOperationService.deleteWorkflow(this.m_oWorkflow.workflowId, oUserId);
    }


    WorkflowController.prototype.removeUserSharing = function (oUserId) {
        this.m_oSnapOperationService.removeWorkflowSharing(this.m_oWorkflow.workflowId, oUserId);
    }

    WorkflowController.prototype.getListOfEnableUsers = function (sWorkflowId) {

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
