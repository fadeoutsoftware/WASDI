
var WorkflowController = (function() {

    function WorkflowController($scope,oExtras,oConstantsService) {

        /**
         * Angular Scope
         */
        this.m_oScope = $scope;
        /**
         * Reference to the controller
         */
        this.m_oScope.m_oController = this;

        this.m_oConstantService = oConstantsService;
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
        this.m_sUserEmail ="";
        /**
         * Field with list of active sharing
         */
        this.m_aoEnabledUsers = [{userId:"me"} , {userId:"myself"} , {userId:"Id"}];
    }
    WorkflowController.prototype.shareWorkflowByUserEmail = function (oUserId){

    }


    WorkflowController.prototype.iAmTheOwner = function (){
        return (this.m_oConstantService.getUser().userId === this.m_oWorkflow.userId) ;
    }
    WorkflowController.$inject = [
        '$scope',
        'extras',
        'ConstantsService'
        ]


    return WorkflowController;
})();
