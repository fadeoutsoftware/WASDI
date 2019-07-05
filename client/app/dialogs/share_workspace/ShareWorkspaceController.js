/**
 * Created by a.corrado on 16/06/2017.
 */



var ShareWorkspaceController = (function() {

    function ShareWorkspaceController($scope, oClose,oExtras,oSnapOperationService,oConstantsService) {
        this.m_oScope = $scope;
        this.m_oClose = oClose;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oSnapOperationService = oSnapOperationService;
        this.m_oFile = null;
        this.m_sWorkspace = this.m_oExtras.workspace;
        this.m_asSelectedProducts = [];
        this.m_aoWorkflows = [];
        this.m_oSelectedWorkflow = null;
        this.m_oSelectedMultiInputWorkflow = null;

        this.m_oConstantsService = oConstantsService;

        if(utilsIsObjectNullOrUndefined(this.m_sWorkspace) === true){
            this.m_sWorkspace = this.m_oConstantsService.getActiveWorkspace()
        }
        this.m_aoEnableUsers = this.getListOfEnableUsers();

        var oController = this;

        $scope.close = function(result) {

            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };


    }

    ShareWorkspaceController.prototype.getListOfEnableUsers = function(oWorkspace){
        return ["email@fuffa.it","email@test.it","email@nonva.it"]
        // return oWorkspace.sharedUsers;
    };

    ShareWorkspaceController.prototype.shareWorkspaceByUserEmail = function(oWorkspace,sEmail){

    };

    ShareWorkspaceController.prototype.disablePermisionsUsersByWorkspace = function(oWorkspace,sEmail){

    };


    ShareWorkspaceController.$inject = [
        '$scope',
        'close',
        'extras',
        'SnapOperationService',
        'ConstantsService',

    ];
    return ShareWorkspaceController;
})();
