/**
 * Created by m.menapace on 9/02/2021.
 */


var WorkspaceDetailsController = (function () {

    function WorkspaceDetailsController($scope, oExtras, oWorkspaceService, oNodeService, oConstantService) {

        /**
         * Angular Scope
         */
        this.m_oScope = $scope;
        /**
         * import the extras
         */
        this.m_oExtras = oExtras;
        /**
         * Injected Workspace service <-> Used to update the workspace Node on opening of the dialog
         */
        this.m_oWorkspaceService = oWorkspaceService;
        /**
         * Injected Node service <-> Get the list of nodes on opening of the dialog
         */
        this.m_oNodeService = oNodeService;
        /**
         * Link to the costants service
         */
        this.m_oConstantService = oConstantService;
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
        /**
         * Computational Node list
         */
        this.m_aoNodesList = this.m_oExtras.NodeList;
        /**
         * Current node code
         */
        this.m_sCurrentNode = this.m_oExtras.WorkSpaceViewModel.nodeCode;


        /**
         * Extract an array of strings from the node list
         * @returns {*}
         */
        //this.m_asNodeCode = (this.m_aoNodesList.map(function (item) {return item['nodeCode']}).map(name =>({name})));
        this.m_asNodeCode = this.m_aoNodesList.map(function (item) {
            return item['nodeCode']
        });
        this.m_asNodeCode.push("wasdi");

        /**
         * Extract an array of strings from the node list
         * @returns {*}
         */
        this.m_asCloudProvider = this.m_aoNodesList.map(function (item) {
            return item['cloudProvider']
        }).push("wasdi");


    } // end constructor

    WorkspaceDetailsController.prototype.getLastTouchDate = function () {

        if (this.m_oWorkspaceViewModel === null) {
            return "";
        } else {
            return new Date(this.m_oWorkspaceViewModel.lastEditDate).toString().replace("\"", "");
        }
    }

    WorkspaceDetailsController.prototype.setNodeCode = function (node) {
        this.m_sCurrentNode = node;
    }
    WorkspaceDetailsController.prototype.saveNodeCode = function () {
        this.m_oWorkspaceViewModel.nodeCode = this.m_sCurrentNode;
        let oWorkspaceViewModel;
        let oController = this;
        this.m_oWorkspaceService.UpdateWorkspace(this.m_oWorkspaceViewModel).then(function(data){
            oWorkspaceViewModel=data.data;
            if (null != oWorkspaceViewModel) {
                // update on the current active workspaces
                oController.m_oConstantService.getActiveWorkspace().nodeCode = this.m_sCurrentNode;
               // oController.m_oConstantService.getActiveWorkspace().apiUrl = oWorkspaceViewModel.apiUrl;
                var oDialog = utilsVexDialogAlertBottomRightCorner('WORKSPACE NODE UPDATED<br>READY');
                utilsVexCloseDialogAfter(4000, oDialog);
            }
            else{
                var oDialog = utilsVexDialogAlertTop('GURU MEDITATION<br>THERE WAS AN ERROR UPDATING WORKSPACE NODE');
                utilsVexCloseDialogAfter(10000, oDialog);
            }
        });

    }

    /**
     * Get the Cloud provider SLA Link if available
     * @returns 
     */
    WorkspaceDetailsController.prototype.getSLALink = function () {

        if (this.m_oWorkspaceViewModel === null) {
            return "";
        } else {
            if (utilsIsObjectNullOrUndefined(this.m_oWorkspaceViewModel.slaLink)) {
                return "";
            }
            else {
                return this.m_oWorkspaceViewModel.slaLink;
            }
        }
    }

    /**
     * Methods that sets the node and post the updated ViewModel
     */

    WorkspaceDetailsController.$inject = [
        '$scope',
        'extras',
        'WorkspaceService',
        'NodeService',
        'ConstantsService'
    ];
    return WorkspaceDetailsController;
})();
