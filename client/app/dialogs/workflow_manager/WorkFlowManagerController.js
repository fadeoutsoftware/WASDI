/**
 * Created by a.corrado on 16/06/2017.
 */



var WorkFlowManagerController = (function() {

    function WorkFlowManagerController($scope, oClose,oExtras,oSnapOperationService,oConstantsService, oHttp) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oSnapOperationService = oSnapOperationService;
        this.m_oFile = null;
        this.m_aoProducts = this.m_oExtras.products;
        this.m_sWorkspaceId = this.m_oExtras.workflowId;
        this.m_asProductsName = utilsProjectGetProductsName(this.m_aoProducts);
        this.m_asSelectedProducts = [];
        this.m_aoWorkflows = [];
        this.m_oSelectedWorkflow = null;
        this.m_oConstantsService = oConstantsService;
        this.m_oWorkflowFileData = {
            workflowName:"",
            workflowDescription:""
        };
        this.isUploadingWorkflow = false;
        this.m_sSelectedWorkflowTab = 'WorkFlowTab1';
        this.m_bIsLoadingWorkflows = false;
        this.m_oHttp =  oHttp;
        //$scope.close = oClose;
        var oController = this;

        $scope.close = function(result) {

            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };

        //Load workflows
        this.getWorkflowsByUser();
    }

    WorkFlowManagerController.prototype.getWorkflowsByUser = function()
    {
        var oController = this;
        this.m_bIsLoadingWorkflows = true;
        this.m_oSnapOperationService.getWorkflowsByUser().success(function (data) {
            if(utilsIsObjectNullOrUndefined(data) == false)
            {
                oController.m_aoWorkflows = data;
            }
            else
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN GET WORKFLOWS, THERE AREN'T DATA");
            }

            //it changes the default tab, we can't visualize the 'WorkFlowTab1' because there aren't workflows
            if( (utilsIsObjectNullOrUndefined(oController.m_aoWorkflows) === true) || (oController.m_aoWorkflows.length === 0) )
            {
                oController.m_sSelectedWorkflowTab = 'WorkFlowTab2';
            }
            oController.m_bIsLoadingWorkflows = false;
        }).error(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN GET WORKFLOWS");
            oController.m_bIsLoadingWorkflows = false;
        });
    };

    /**
     * runWorkFlowPerProducts
     */
    WorkFlowManagerController.prototype.runWorkFlowPerProducts = function()
    {
        var iNumberOfProducts = this.m_asSelectedProducts.length
        if(utilsIsObjectNullOrUndefined(this.m_oSelectedWorkflow) === true)
        {
            return false;
        }
        for(var iIndexSelectedProduct = 0 ; iIndexSelectedProduct < iNumberOfProducts; iIndexSelectedProduct++)
        {
            //TODO CHECK THIS CODE
            var oProduct = utilsProjectGetProductByName(this.m_asSelectedProducts[iIndexSelectedProduct],this.m_aoProducts);
            if(utilsIsObjectNullOrUndefined(oProduct))
            {
                return false;
            }

            var sDestinationProductName = oProduct.name + "_workflow";
            this.executeGraphFromWorkflowId(this.m_sWorkspaceId,oProduct.name,sDestinationProductName,this.m_oSelectedWorkflow.workflowId);
        }
        return true;
    };

    /**
     * executeGraphFromWorkflowId
     * @param sWorkspaceId
     * @param sProductNameSelected
     * @param sDestinationProductName
     * @param sWorkflowId
     * @returns {boolean}
     */
    WorkFlowManagerController.prototype.executeGraphFromWorkflowId = function(sWorkspaceId,sProductNameSelected,sDestinationProductName,sWorkflowId)
    {
        if(utilsIsObjectNullOrUndefined(sWorkspaceId) === true)
        {
            return false;
        }
        if(utilsIsObjectNullOrUndefined(sWorkflowId) === true)
        {
            return false;
        }
        if(utilsIsObjectNullOrUndefined(sProductNameSelected) === true || utilsIsStrNullOrEmpty(sProductNameSelected) === true)
        {
            return false;
        }
        if(utilsIsObjectNullOrUndefined(sDestinationProductName) === true || utilsIsStrNullOrEmpty(sDestinationProductName) === true)
        {
            return false;
        }
        var oController = this;
        this.m_oSnapOperationService.executeGraphFromWorkflowId(sWorkspaceId,sProductNameSelected,sDestinationProductName,sWorkflowId).success(function (data) {
            if(utilsIsObjectNullOrUndefined(data) == false)
            {
                //TODO OK
                oController.cleanAllExecuteWorkflowFields();
            }
            else
            {
                //TODO ERROR
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN EXECUTE WORKFLOW,");
            }
        }).error(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN EXECUTE WORKFLOW");
            oController.cleanAllExecuteWorkflowFields();

        });

        return true;
    };

    /**
     * deleteWorkflow
     * @param oWorkflow
     * @returns {boolean}
     */
    WorkFlowManagerController.prototype.deleteWorkflow = function(oWorkflow)
    {
        if( utilsIsObjectNullOrUndefined(oWorkflow) === true)
        {
            return false;
        }
        var oController = this;
        this.m_oSnapOperationService.deleteWorkflow(oWorkflow.workflowId).success(function (data)
        {
            if(utilsIsObjectNullOrUndefined(data) == false)
            {
                oController.getWorkflowsByUser();
            }
            else
            {
                //TODO ERROR
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN DELETE WORKFLOW");
            }
        }).error(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN DELETE WORKFLOW");
        });

        return true;
    };

    /**
     * uploadUserGraphOnServer
     */

    WorkFlowManagerController.prototype.uploadUserGraphOnServer = function()
    {

        if(utilsIsStrNullOrEmpty(this.m_oWorkflowFileData.workflowName) === true)
        {
            this.m_oWorkflowFileData.workflowName = "workflow";
        }
        var oBody = new FormData();
        oBody.append('file', this.m_oFile[0]);

        this.uploadGraph(this.m_sWorkspaceId, this.m_oWorkflowFileData.workflowName,this.m_oWorkflowFileData.workflowDescription,oBody);
    };

    /**
     * uploadGraph
     * @param sWorkspaceId
     * @param sName
     * @param sDescription
     * @param oBody
     * @returns {boolean}
     */
    WorkFlowManagerController.prototype.uploadGraph = function(sWorkspaceId,sName,sDescription,oBody)
    {
        if(utilsIsObjectNullOrUndefined(sWorkspaceId) === true)
        {
            return false;
        }
        if(utilsIsObjectNullOrUndefined(sName) === true || utilsIsStrNullOrEmpty(sName) === true)
        {
            return false;
        }
        if(utilsIsObjectNullOrUndefined(sDescription) === true )//|| utilsIsStrNullOrEmpty(sDescription) === true
        {
            return false;
        }
        if(utilsIsObjectNullOrUndefined(oBody) === true )
        {
            return false;
        }
        this.isUploadingWorkflow=true;
        var oController = this;
        this.m_oSnapOperationService.uploadGraph(this.m_sWorkspaceId,sName,sDescription,oBody).success(function (data) {
            if(utilsIsObjectNullOrUndefined(data) == false)
            {
                //Reload list o workFlows
                oController.getWorkflowsByUser();
                oController.cleanAllUploadWorkflowFields();

            }
            else
            {
                //TODO ERROR
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN UPLOAD WORKFLOW PROCESS");
            }

            oController.isUploadingWorkflow = false;
        }).error(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN UPLOAD WORKFLOW PROCESS");
            oController.cleanAllUploadWorkflowFields();
            oController.isUploadingWorkflow = false;
        });

        return true;
    };
    /**
     *
     * @returns {boolean}
     */
    WorkFlowManagerController.prototype.isSelectedProduct = function(){
        return (this.m_asSelectedProducts.length > 0);
    };
    /**
     *
     * @returns {boolean}
     */
    WorkFlowManagerController.prototype.isSelectedWorkFlow = function(){
        return !utilsIsObjectNullOrUndefined(this.m_oSelectedWorkflow);
    };
    /**
     *
     * @returns {boolean}
     */
    WorkFlowManagerController.prototype.isUploadedNewWorkFlow = function (){
        return !utilsIsObjectNullOrUndefined(this.m_oFile);
    };

    /**
     *
     */
    WorkFlowManagerController.prototype.cleanAllUploadWorkflowFields = function (){
        this.m_oWorkflowFileData = {
            workflowName:"",
            workflowDescription:""
        };
        this.m_oFile = null;
    };

    /**
     *
     */
    WorkFlowManagerController.prototype.cleanAllExecuteWorkflowFields = function (){
        this.m_asSelectedProducts = [];
        this.m_oSelectedWorkflow = null;
    };

    WorkFlowManagerController.prototype.isPossibleDoUpload = function()
    {
        // this.m_oWorkflowFileData.workflowName,this.m_oWorkflowFileData.workflowDescription    this.m_oFile[0]
        var bReturnValue = false;
        if( (utilsIsStrNullOrEmpty( this.m_oWorkflowFileData.workflowName) === false) && (utilsIsStrNullOrEmpty(this.m_oWorkflowFileData.workflowDescription) === false)
            && (utilsIsObjectNullOrUndefined(this.m_oFile[0]) === false))
        {
            bReturnValue = true;
        }
        return bReturnValue;
    };
    WorkFlowManagerController.$inject = [
        '$scope',
        'close',
        'extras',
        'SnapOperationService',
        'ConstantsService',
        '$http'
    ];
    return WorkFlowManagerController;
})();
