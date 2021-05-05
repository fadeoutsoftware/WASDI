
var WorkflowController = (function() {

    function WorkflowController($scope,oExtras) {

        /**
         * Angular Scope
         */
        this.m_oScope = $scope;
        /**
         * Reference to the controller
         */
        this.m_oScope.m_oController = this;
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
    }
    WorkflowController.$inject = [
        '$scope',
        'extras'
        ]


    return WorkflowController;
})();
