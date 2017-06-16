/**
 * Created by a.corrado on 16/06/2017.
 */



var WorkFlowController = (function() {

    function WorkFlowController($scope, oClose,oExtras,oSnapOperationService,oConstantsService) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oSnapOperationService = oSnapOperationService;
        this.m_oFile = null;
        this.m_aoProducts = this.m_oExtras.products;
        this.m_oSelectedFile = null;
        this.m_oConstantsService = oConstantsService;
        this.m_oActiveWorkspace =  this.m_oConstantsService.getActiveWorkspace();
        //$scope.close = oClose;
        var that = this;
        $scope.close = function(result) {
            that.postWorkFlow();
            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };
        // $scope.$watch('m_oController.m_oFile', function () {
        //     if (!utilsIsObjectNullOrUndefined( $scope.m_oController.m_oFile) ) {
        //         //TODO M_OFILE IS AN ARRAY CHECK IT
        //         $scope.m_oController.upload($scope.m_oController.m_oFile[0]);
        //     }
        // });
    }
    WorkFlowController.prototype.postWorkFlow = function () {

        this.m_oSnapOperationService.postWorkFlow(this.m_oFile[0],this.m_oActiveWorkspace.workspaceId,this.m_oSelectedFile.fileName, this.m_oSelectedFile.productFriendlyName ).success(function (data, status,headers)
        {
            if (!utilsIsObjectNullOrUndefined(data) )
            {

            }
        }).error(function (data,status)
        {
            utilsVexDialogAlertTop("Error: was impossible send Workflow to the server");
        });

        return true;
    };

    WorkFlowController.prototype.dataForPostWorkFlowAreWellFormed = function () {
        if(utilsIsObjectNullOrUndefined(this.m_oFile) === true)
            return false;
        if(utilsIsObjectNullOrUndefined(this.m_oActiveWorkspace ) === true )
            return false;
        if(utilsIsObjectNullOrUndefined(this.m_oSelectedFile) === true )
            return false;
        return true;
    };

    WorkFlowController.$inject = [
        '$scope',
        'close',
        'extras',
        'SnapOperationService',
        'ConstantsService'
    ];
    return WorkFlowController;
})();
