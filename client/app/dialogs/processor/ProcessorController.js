 /**
 * Created by a.corrado on 31/03/2017.
 */


var ProcessorController = (function() {

    function ProcessorController($scope, oClose,oExtras,oWorkspaceService,oProductService,oConstantsService,oHttp, oProcessorService) {
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
        this.m_sName = "";
        this.m_sDescription = "";
        this.m_sVersion = "";
        this.m_sJSONSample = "";
        this.m_aoProcessorTypes = [{'name':'Python 2.7','id':'ubuntu_python_snap'},{'name':'Python 3.7','id':'ubuntu_python37_snap'},{'name':'IDL 3.7.2','id':'ubuntu_idl372'}];
        this.m_sSelectedType = "";
        this.m_oPublic = true;
        this.m_oProcessorService = oProcessorService;

        var oController = this;
        $scope.close = function() {
            oClose(null, 300); // close, but give 500ms for bootstrap to animate
        };
        $scope.add = function() {
            oController.postProcessor(oController, oController.m_oFile[0]);
            oClose(null, 300); // close, but give 500ms for bootstrap to animate
        };

    };


    ProcessorController.prototype.postProcessor = function (oController,oSelectedFile)
    {
        if(utilsIsObjectNullOrUndefined(oSelectedFile) === true)
        {
            return false;
        }

        var sType = oController.m_sSelectedType.id;
        var sPublic = "1";
        if (oController.m_oPublic === false) sPublic = "0";

        var oBody = new FormData();
        oBody.append('file', this.m_oFile[0]);

        this.m_oProcessorService.uploadProcessor(oController.m_oActiveWorkspace.workspaceId,oController.m_sName,oController.m_sVersion, oController.m_sDescription, sType, oController.m_sJSONSample,sPublic, oBody).success(function (data) {
            var oDialog = utilsVexDialogAlertBottomRightCorner("PROCESSOR UPLOADED<br>IT WILL BE DEPLOYED IN A WHILE");
            utilsVexCloseDialogAfter(4000,oDialog);
        }).error(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>THERE WAS AN ERROR DEPLOYING THE PROCESSOR");
        });

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
        '$http',
        'ProcessorService'

    ];
    return ProcessorController;
})();
