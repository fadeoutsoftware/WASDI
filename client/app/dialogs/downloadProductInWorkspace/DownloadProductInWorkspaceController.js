/**
 * Created by a.corrado on 24/05/2017.
 */


var DownloadProductInWorkspaceController = (function() {

    function DownloadProductInWorkspaceController($scope, oClose,oWorkspaceService) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
         // this.m_oExtras = oExtras;
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
        this.m_oWorkspaceService.getWorkspacesInfoListByUser().then(function (data, status) {
            if (data.data != null)
            {
                if (data.data != undefined)
                {
                    oController.m_aoWorkspaceList = data.data;
                }
            }
        },function (data,status) {
            //alert('error');
            utilsVexDialogAlertTop('GURU MEDITATION<br>ERROR READING WORKSPACES');
        });
    };

    DownloadProductInWorkspaceController.prototype.createWorkspace = function () {

        var oController = this;

        this.m_oWorkspaceService.createWorkspace().then(function (data, status) {
            if (data.data != null)
            {
                if (data.data != undefined)
                {
                    //var sWorkspaceId = data.stringValue;
                   // oController.openWorkspace(sWorkspaceId);
                    oController.getWorkspaces();
                }
            }
        },function (data,status) {
            //alert('error');
            utilsVexDialogAlertTop('GURU MEDITATION<br>ERROR CREATING NEW WORKSPACE');
        });
    };
    DownloadProductInWorkspaceController.$inject = [
        '$scope',
        'close',
        // 'extras',
        'WorkspaceService'
    ];
    return DownloadProductInWorkspaceController;
})();
window.DownloadProductInWorkspaceController = DownloadProductInWorkspaceController;
