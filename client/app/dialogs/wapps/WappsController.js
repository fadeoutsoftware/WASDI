/**
 * Created by a.corrado on 31/03/2017.
 */


var WappsController = (function() {

    function WappsController($scope, oClose,oExtras,oWorkspaceService,oProductService, oProcessorService) {
        //MEMBERS
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oWorkspaceService = oWorkspaceService;
        this.m_oProductService = oProductService;
        this.m_aoWorkspaceList = [];
        this.m_aWorkspacesName = [];
        this.m_aoSelectedWorkspaces = [];
        this.m_sFileName = "";
        this.m_oProcessorService = oProcessorService;

        var oController = this;
        $scope.close = function(result) {
            oClose(result, 300); // close, but give 500ms for bootstrap to animate
        };
        $scope.add = function(result) {
            oClose(result, 300); // close, but give 500ms for bootstrap to animate
        };

    };


    WappsController.prototype.test = function() {
        var oController = this;

        this.m_oProcessorService.getProcessorsList().success(function (data) {
            if(utilsIsObjectNullOrUndefined(data) == false)
            {
                //TODO OK
                console.log(data);
            }
            else
            {
                //TODO ERROR
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR GETTING WAPPS LIST");
            }
        }).error(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR GETTING WAPPS LIST");
            oController.cleanAllExecuteWorkflowFields();

        });
    };

    WappsController.$inject = [
        '$scope',
        'close',
        'extras',
        'WorkspaceService',
        'ProductService',
        'ProcessorService'

    ];
    return WappsController;
})();
