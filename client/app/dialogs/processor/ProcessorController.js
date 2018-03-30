/**
 * Created by a.corrado on 31/03/2017.
 */


var ProcessorController = (function() {

    function ProcessorController($scope, oClose,oExtras,oWorkspaceService,oProductService,oConstantsService,oHttp) {
        //MEMBERS
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_aoProduct = this.m_oExtras.product;
        this.m_oWorkspaceService = oWorkspaceService;
        this.m_oProductService = oProductService;
        this.m_aoWorkspaceList = [];
        this.m_aWorkspacesName = [];
        this.m_aoSelectedWorkspaces = [];
        this.m_sFileName = "";
        this.m_oConstantsService = oConstantsService;
        this.m_oActiveWorkspace = this.m_oConstantsService.getActiveWorkspace();
        this.m_oHttp =  oHttp;
        //file .zip
        this.m_oFile = null;

        var oController = this;
        $scope.close = function() {
            oClose(null, 300); // close, but give 500ms for bootstrap to animate
        };
        $scope.add = function() {
            oController.postWorkFlow(oController, oController.m_oFile[0]);
            oClose(null, 300); // close, but give 500ms for bootstrap to animate
        };

    };


    ProcessorController.prototype.postWorkFlow = function (oController,oSelectedFile)
    {
        if(utilsIsObjectNullOrUndefined(oSelectedFile) === true)
        {
            return false;
        }

        //TODO 19/02/2018 PUT IT INSIDE A SERVICE a.corrado
        var sUrl = oController.m_oConstantsService.getAPIURL();
        sUrl += '/processing/graph?workspace=' + oController.m_oActiveWorkspace.workspaceId + '&source=' + oSelectedFile.fileName + '&destination=' + oController.changeProductName(oSelectedFile.productFriendlyName)//oController.m_sOutputFile;

        var successCallback = function(data, status)
        {
            //utilsVexDialogAlertTop();
            var oDialog = utilsVexDialogAlertBottomRightCorner("WORKFLOW UPLOADED<br>PROCESSING WILL START IN A WHILE");
            utilsVexCloseDialogAfterFewSeconds(4000,oDialog);

        };

        var errorCallback = function (data, status)
        {
            utilsVexDialogAlertTop("GURU MEDITATION<br>THERE WAS AN ERROR UPLOADING WORKFLOW DESCRIPTION FILE");
        };


        var fd = new FormData();
        fd.append('file', this.m_oFile[0]);

        oController.m_oHttp.post(sUrl, fd, {
            transformRequest: angular.identity,
            headers: {'Content-Type': undefined}
        }).then(successCallback, errorCallback);

        return true;
    };

    /**
     *
     * @param sName
     * @returns {*}
     */
    ProcessorController.prototype.changeProductName = function(sName){
        if(utilsIsStrNullOrEmpty(sName) === true)
            return "";

        return sName + "_workflow";
    };

    ProcessorController.$inject = [
        '$scope',
        'close',
        'extras',
        'WorkspaceService',
        'ProductService',
        'ConstantsService',
        '$http'

    ];
    return ProcessorController;
})();
