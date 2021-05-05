
var WorkflowController = (function() {

    function WorkflowController($scope) {

        /**
         * Angular Scope
         */
        this.m_oScope = $scope;
        /**
         * Reference to the controller
         */
        this.m_oScope.m_oController = this;

        this.m_sSelectedTab = "Base";
    }

    return WorkflowController;
})();
