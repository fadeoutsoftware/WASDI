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
        // this.m_oActiveWorkspace =  this.m_oConstantsService.getActiveWorkspace();
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
        this.m_oSnapOperationService.getWorkflowsByUser().success(function (data) {
            if(utilsIsObjectNullOrUndefined(data) == false)
            {
                //TODO CHECK IT !
                oController.m_aoWorkflows = data;
            }
            else
            {
                //TODO ERROR
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN GET WORKFLOWS, THERE AREN'T DATA");
            }
        }).error(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN GET WORKFLOWS");
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
            var sDestinationProductName = this.m_asSelectedProducts.fileName + "_workflow";
            this.executeGraphFromWorkflowId(this.m_sWorkspaceId,this.m_asSelectedProducts.fileName,sDestinationProductName,this.m_oSelectedWorkflow.id);
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

        this.m_oSnapOperationService.executeGraphFromWorkflowId(sWorkspaceId,sProductNameSelected,sDestinationProductName,sWorkflowId).success(function (data) {
            if(utilsIsObjectNullOrUndefined(data) == false)
            {
                //TODO OK
            }
            else
            {
                //TODO ERROR
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN EXECUTE WORKFLOW,");
            }
        }).error(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN EXECUTE WORKFLOW");
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

        this.m_oSnapOperationService.deleteWorkflow(oWorkflow.id).success(function (data)
        {
            if(utilsIsObjectNullOrUndefined(data) == false)
            {
                //TODO OK
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
        var sFileName = this.m_oFile[0].name;
        if(utilsIsStrNullOrEmpty(sFileName) === true)
        {
            sFileName = "workflow";
        }
        var oBody = new FormData();
        oBody.append('file', this.m_oFile[0]);
        this.uploadGraph(this.m_sWorkspaceId, "TestFile","Description",oBody);
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
        if(utilsIsObjectNullOrUndefined(sDescription) === true || utilsIsStrNullOrEmpty(sDescription) === true)
        {
            return false;
        }
        if(utilsIsObjectNullOrUndefined(oBody) === true )
        {
            return false;
        }

        var oController = this;
        this.m_oSnapOperationService.uploadGraph(this.m_sWorkspaceId,sName,sDescription,oBody).success(function (data) {
            if(utilsIsObjectNullOrUndefined(data) == false)
            {
                //Reload list o workFlows
                oController.getWorkflowsByUser()
            }
            else
            {
                //TODO ERROR
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN UPLOAD WORKFLOW PROCESS");
            }
        }).error(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN UPLOAD WORKFLOW PROCESS");
        });

        return true;
    }

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
