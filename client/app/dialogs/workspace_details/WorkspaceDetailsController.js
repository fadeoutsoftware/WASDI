/**
 * Created by m.menapace on 9/02/2021.
 */


var WorkspaceDetailsController = (function () {

    function WorkspaceDetailsController($scope, oExtras, oWorkspaceService, oNodeService) {

        /**
         * Angular Scope
         */
        this.m_oScope = $scope;
        /**
         * import the extras
         */
        this.m_oExtras = oExtras;
        /**
         * Injected Workspace service <-> Used to update the workspace Node
         */
        this.m_oWorkspaceService = oWorkspaceService;
        /**
         * Injected Node service <-> Get the list of nodes
         */
        this.m_oNodeService = oNodeService;


        /**
         * Reference to this controller
         */
        this.m_oScope.m_oController = this;


        /**
         * workspace id
         */
        this.m_workspaceId = this.m_oExtras.WorkSpaceId;

        /**
         * workspace view model <-> WorkspaceEditorViewModel on the server
         */
        this.m_oWorkspaceViewModel = this.m_oExtras.WorkSpaceViewModel;
        /**
         * count of the products in the current workspace
         */
        this.m_oCountProduct = this.m_oExtras.ProductCount;


    } // end constructor

    WorkspaceDetailsController.prototype.getLastTouchDate = function () {

                if (this.m_oWorkspaceViewModel === null) {
            return "";
        } else {
            return new Date(this.m_oWorkspaceViewModel.lastEditDate).toString().replace("\"", "");
        }
    }

    WorkspaceDetailsController.$inject = [
        '$scope',
        'extras',
        'WorkspaceService',
        'NodeService'
    ];
    return WorkspaceDetailsController;
})();
