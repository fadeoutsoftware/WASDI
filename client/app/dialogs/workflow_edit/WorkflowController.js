
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

        this.m_sSelectedTab = "Base";

        this.m_oExtras = oExtras;
    }
    WorkflowController.$inject = [
        '$scope',
        'extras'
        ]


    return WorkflowController;
})();
