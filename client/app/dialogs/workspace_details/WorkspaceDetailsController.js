 /**
 * Created by m.menapace on 9/02/2021.
 */


var WorkspaceDetailsController = (function() {

    function WorkspaceDetailsController($scope) {

        /**
         * Angular Scope
         */
        this.m_oScope = $scope;
        /**
         * Reference to this controller
         */
        this.m_oScope.m_oController = this;
    }
     WorkspaceDetailsController.$inject = [
        '$scope',
    ];
    return WorkspaceDetailsController;
})();
