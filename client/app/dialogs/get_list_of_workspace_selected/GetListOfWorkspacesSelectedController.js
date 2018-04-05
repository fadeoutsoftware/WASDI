/**
 * Created by a.corrado on 24/05/2017.
 */


var GetListOfWorkspacesController = (function() {

    function GetListOfWorkspacesController($scope, oClose,oWorkspaceService,oExtras) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras
        this.m_sButtonName = oExtras.buttonName;
        this.m_sTitleModal = oExtras.titleModal;
        this.m_bSelectedAllWorkspaces = false;
        this.m_oWorkspaceService = oWorkspaceService;
        this.m_aoWorkspaceList = [];
        this.m_aoWorkspacesSelected = [];
        this.m_bisLoadingWorkspacesList = true;
        this.m_bIsCreatingWorskapce = false;
        this.m_oClose = oClose;
        //$scope.close = oClose;
        $scope.close = function(result) {
            oClose(result, 200); // close, but give 500ms for bootstrap to animate
        };

        this.getWorkspaces();
    }

    /**
     * selecteAllWorkspaces
     */
    GetListOfWorkspacesController.prototype.selectAllWorkspaces = function ()
    {
        this.m_bSelectedAllWorkspaces = true;
        var iNumberOfWorkspaces = this.m_aoWorkspaceList.length;
        for(var iIndexWorkspace = 0 ; iIndexWorkspace < iNumberOfWorkspaces ; iIndexWorkspace++)
        {
            this.selectedWorkspace(this.m_aoWorkspaceList[iIndexWorkspace]);
        }
    }
    /**
     * deselectAllWorkspaces
     */
    GetListOfWorkspacesController.prototype.deselectAllWorkspaces = function ()
    {
        this.m_bSelectedAllWorkspaces = false;
        var iNumberOfWorkspaces = this.m_aoWorkspaceList.length;
        for(var iIndexWorkspace = 0 ; iIndexWorkspace < iNumberOfWorkspaces ; iIndexWorkspace++)
        {
            this.deselectWorkspace(this.m_aoWorkspaceList[iIndexWorkspace]);
        }
    }
    /**
     * getWorkspaces
     */
    GetListOfWorkspacesController.prototype.getWorkspaces = function()
    {
        var oController = this;
        this.m_bisLoadingWorkspacesList = true;
        this.m_oWorkspaceService.getWorkspacesInfoListByUser().success(function (data, status) {
            if (data != null)
            {
                if (data != undefined)
                {
                    oController.m_aoWorkspaceList = data;
                }
            }
            oController.m_bisLoadingWorkspacesList = false;
        }).error(function (data,status) {
            //alert('error');
            oController.m_bisLoadingWorkspacesList = false;
            utilsVexDialogAlertTop('GURU MEDITATION<br>ERROR IN WORKSPACESINFO');
        });
    };

    /**
     * selectedWorkspace
     * @param oWorkspace
     * @returns {boolean}
     */
    GetListOfWorkspacesController.prototype.selectedWorkspace = function(oWorkspace){
        if(utilsIsObjectNullOrUndefined(oWorkspace) === true)
            return false;

        this.m_aoWorkspacesSelected.push(oWorkspace);
        return true;
    };

    /**
     * deselectWorkspace
     * @param oWorkspace
     * @returns {boolean}
     */
    GetListOfWorkspacesController.prototype.deselectWorkspace = function(oWorkspace) {
        if(utilsIsObjectNullOrUndefined(oWorkspace) === true)
            return false;
        utilsRemoveObjectInArray(this.m_aoWorkspacesSelected,oWorkspace);
        return true;
    };

    GetListOfWorkspacesController.prototype.isSelectedWorkspace = function(oWorkspace)
    {
        if(utilsIsObjectNullOrUndefined(oWorkspace) === true)
            return false;
        return utilsIsElementInArray(this.m_aoWorkspacesSelected,oWorkspace);
    };

    /**
     * closeModal
     */
    GetListOfWorkspacesController.prototype.closeModal= function(){

        this.m_oClose(null, 500); // close, but give 500ms for bootstrap to animate

    };

    /**
     * closeModalAndReturnSelectedWorkspaces
     */
    GetListOfWorkspacesController.prototype.closeModalAndReturnSelectedWorkspaces= function(){
        var aoResult = this.m_aoWorkspacesSelected;
        this.m_oClose(aoResult, 500); // close, but give 500ms for bootstrap to animate

    };

    /**
     * createWorkspace
     */
    GetListOfWorkspacesController.prototype.createWorkspace = function () {

        var oController = this;
        this.m_bIsCreatingWorskapce = true;
        this.m_oWorkspaceService.createWorkspace().success(function (data, status) {
            if (data != null)
            {
                if (data != undefined)
                {
                    // var sWorkspaceId = data.stringValue;
                    // oController.openWorkspace(sWorkspaceId);
                    oController.getWorkspaces();
                }
            }
            oController.m_bIsCreatingWorskapce = false;
        }).error(function (data,status) {
            //alert('error');
            utilsVexDialogAlertTop('GURU MEDITATION<br>ERROR IN CREATE WORKSPACE');
            oController.m_bIsCreatingWorskapce = false;
        });
    };
    GetListOfWorkspacesController.$inject = [
        '$scope',
        'close',
        'WorkspaceService',
        'extras'
    ];
    return GetListOfWorkspacesController;
})();
