/**
 * Created by a.corrado on 16/06/2017.
 */



var WorkFlowController = (function() {

    function WorkFlowController($scope, oClose,oExtras,oSnapOperationService,oConstantsService, oHttp) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oSnapOperationService = oSnapOperationService;
        this.m_oFile = null;
        this.m_aoProducts = this.m_oExtras.products;
        this.m_oSelectedFile = null;
        this.m_sOutputFile = "";
        this.m_oConstantsService = oConstantsService;
        this.m_oActiveWorkspace =  this.m_oConstantsService.getActiveWorkspace();
        this.m_oHttp =  oHttp;
        //$scope.close = oClose;
        var oController = this;


        $scope.close = function(result) {
            oController.postWorkFlow(oController);
            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };

        // $scope.$watch('m_oController.m_oFile', function () {
        //     if (!utilsIsObjectNullOrUndefined( $scope.m_oController.m_oFile) ) {
        //         //TODO M_OFILE IS AN ARRAY CHECK IT
        //         $scope.m_oController.upload($scope.m_oController.m_oFile[0]);
        //     }
        // });
    }

    WorkFlowController.prototype.postWorkFlow = function (oController) {

        var sUrl = oController.m_oConstantsService.getAPIURL();
        sUrl += '/processing/graph?workspace=' + oController.m_oActiveWorkspace.workspaceId + '&source=' + oController.m_oSelectedFile.fileName + '&destination=' + oController.m_sOutputFile;

        var successCallback = function(data, status)
        {
            utilsVexDialogAlertTop("Uploaded file");
        };

        var errorCallback = function (data, status)
        {
            utilsVexDialogAlertTop("Error in upload file");
        };


        var fd = new FormData();
        fd.append('file', this.m_oFile[0]);

        oController.m_oHttp.post(sUrl, fd, {
            transformRequest: angular.identity,
            headers: {'Content-Type': undefined}
        }).then(successCallback, errorCallback);

        return true;
    };

    WorkFlowController.prototype.dataForPostWorkFlowAreWellFormed = function () {
        if(utilsIsObjectNullOrUndefined(this.m_oFile) === true) return false;
        if(utilsIsObjectNullOrUndefined(this.m_oActiveWorkspace ) === true ) return false;
        if(utilsIsObjectNullOrUndefined(this.m_oSelectedFile) === true ) return false;
        return true;
    };
    /**
     *
     * @param oProductInput
     */
    WorkFlowController.prototype.selectedProduct = function (oProductInput) {
        this.m_oSelectedFile = oProductInput;
        this.m_sOutputFile = oProductInput.productFriendlyName + "_workflow";

    };

    WorkFlowController.$inject = [
        '$scope',
        'close',
        'extras',
        'SnapOperationService',
        'ConstantsService',
        '$http'
    ];
    return WorkFlowController;
})();
