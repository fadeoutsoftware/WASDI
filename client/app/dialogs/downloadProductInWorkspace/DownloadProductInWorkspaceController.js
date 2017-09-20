/**
 * Created by a.corrado on 24/05/2017.
 */


var DownloadProductInWorkspaceController = (function() {

    function DownloadProductInWorkspaceController($scope, oClose,oExtras,oWorkspaceService) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oWorkspaceService = oWorkspaceService;
        this.m_aoWorkspaceList = [];
        this.m_oClose = oClose;
        //$scope.close = oClose;
        $scope.close = function(result) {
            oClose(result, 200); // close, but give 500ms for bootstrap to animate
        };
        this.getWorkspaces();
    }
    DownloadProductInWorkspaceController.prototype.closeAndDonwloadProduct= function(result){

            this.m_oClose(result, 500); // close, but give 500ms for bootstrap to animate

    }
    DownloadProductInWorkspaceController.prototype.getWorkspaces = function()
    {
        var oController = this;
        this.m_oWorkspaceService.getWorkspacesInfoListByUser().success(function (data, status) {
            if (data != null)
            {
                if (data != undefined)
                {
                    oController.m_aoWorkspaceList = data;
                }
            }
        }).error(function (data,status) {
            //alert('error');
            utilsVexDialogAlertTop('GURU MEDITATION<br>ERROR IN WORKSPACESINFO');
        });
    };

    DownloadProductInWorkspaceController.$inject = [
        '$scope',
        'close',
        'extras',
        'WorkspaceService'
    ];
    return DownloadProductInWorkspaceController;
})();
