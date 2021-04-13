/**
 * Created by a.corrado on 16/06/2017.
 */



var ImageEditorController = (function() {

    ImageEditorController.REG_NAME = "ImageEditorController";

    function ImageEditorController($scope, oClose,oExtras,oSnapOperationService,oConstantsService, oHttp) {
        this.m_oScope = $scope;
        this.m_oClose = oClose;
        this.m_oScope.m_oController = this;
        this.m_oScope.$ctrl = this;

        this._selectedTab = TabType.ImageEditorMask;


        this.m_oExtras = oExtras;
        this.m_oSnapOperationService = oSnapOperationService;
        this.m_oFile = null;
        this.m_aoProducts = this.m_oExtras.products;
        this.m_sWorkspaceId = this.m_oExtras.workflowId;
        this.m_asProductsName = utilsProjectGetProductsName(this.m_aoProducts);
        this.m_asSelectedProducts = [];
        this.m_aoWorkflows = [];
        this.m_oSelectedWorkflow = null;
        this.m_oSelectedMultiInputWorkflow = null;

        this.m_oConstantsService = oConstantsService;
        this.m_oWorkflowFileData = {
            workflowName:"",
            workflowDescription:""
        };
        this.isUploadingWorkflow = false;
        if( utilsIsObjectNullOrUndefined(this.m_oExtras.defaultTab) === true)
        {
            this.m_sSelectedWorkflowTab = 'WorkFlowTab1';
        }
        else
        {
            this.m_sSelectedWorkflowTab = this.m_oExtras.defaultTab;
        }
        // this.m_sSelectedWorkflowTab = 'WorkFlowTab1';
        this.m_bIsLoadingWorkflows = false;
        this.m_oHttp =  oHttp;


        $scope.close = function(result) {

            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };

        //Load workflows
        this.getWorkflowsByUser();

    }


    ImageEditorController.prototype.selectedTabMask = function() { this._selectedTab = TabType.ImageEditorMask; }
    ImageEditorController.prototype.selectedTabFilterBand = function() { this._selectedTab = TabType.ImageEditorFilterBand; }
    ImageEditorController.prototype.selectedTabColorManipulation = function() { this._selectedTab = TabType.ImageEditorColorManipulation; }

    ImageEditorController.prototype.isSelectedTabMask = function() { return this._selectedTab == TabType.ImageEditorMask; }
    ImageEditorController.prototype.isSelectedTabFilterBand = function() { return this._selectedTab == TabType.ImageEditorFilterBand; }
    ImageEditorController.prototype.isSelectedTabColorManipulation = function() { return this._selectedTab == TabType.ImageEditorColorManipulation; }




    ImageEditorController.prototype.getWorkflowsByUser = function()
    {
        var oController = this;
        this.m_bIsLoadingWorkflows = true;
        this.m_oSnapOperationService.getWorkflowsByUser().then(function (data) {
            if(utilsIsObjectNullOrUndefined(data.data) == false)
            {
                oController.m_aoWorkflows = data.data;
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
        },function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN GET WORKFLOWS");
            oController.m_bIsLoadingWorkflows = false;
        });
    };

    /**
     * runWorkFlowPerProducts
     */
    ImageEditorController.prototype.runWorkFlowPerProducts = function()
    {
        var iNumberOfProducts = this.m_asSelectedProducts.length;
        if(utilsIsObjectNullOrUndefined(this.m_oSelectedWorkflow) === true)
        {
            return false;
        }
        for(var iIndexSelectedProduct = 0 ; iIndexSelectedProduct < iNumberOfProducts; iIndexSelectedProduct++)
        {
            var oProduct = utilsProjectGetProductByName(this.m_asSelectedProducts[iIndexSelectedProduct],this.m_aoProducts);
            if(utilsIsObjectNullOrUndefined(oProduct))
            {
                return false;
            }
            //TODO REMOVE IT
            // var sDestinationProductName = oProduct.name + "_workflow";
            this.m_oSelectedWorkflow.inputFileNames.push(oProduct.fileName);
            var oSnapWorkflowViewModel = this.getObjectExecuteGraph(this.m_oSelectedWorkflow.workflowId,this.m_oSelectedWorkflow.name,this.m_oSelectedWorkflow.description,
                this.m_oSelectedWorkflow.inputNodeNames, this.m_oSelectedWorkflow.inputFileNames,this.m_oSelectedWorkflow.outputNodeNames,
                this.m_oSelectedWorkflow.outputFileNames);
            if(utilsIsObjectNullOrUndefined(oSnapWorkflowViewModel) === false)
            {
                this.executeGraphFromWorkflowId(this.m_sWorkspaceId,oSnapWorkflowViewModel);
            }
            else
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>YOU HAVE TO INSERT A PRODUCT FOR EACH INPUT NODE.");
            }
        }
        return true;
    };

    ImageEditorController.prototype.runMultiInputWorkFlow=function()
    {
        var oSnapWorkflowViewModel = this.getObjectExecuteGraph(this.m_oSelectedMultiInputWorkflow.workflowId,this.m_oSelectedMultiInputWorkflow.name,this.m_oSelectedMultiInputWorkflow.description,
            this.m_oSelectedMultiInputWorkflow.inputNodeNames, this.m_oSelectedMultiInputWorkflow.inputFileNames,this.m_oSelectedMultiInputWorkflow.outputNodeNames,
            this.m_oSelectedMultiInputWorkflow.outputFileNames);
        if(utilsIsObjectNullOrUndefined(oSnapWorkflowViewModel) === false)
        {
            this.executeGraphFromWorkflowId(this.m_sWorkspaceId,oSnapWorkflowViewModel);
        }
        else
        {
            utilsVexDialogAlertTop("GURU MEDITATION<br>YOU HAVE TO INSERT A PRODUCT FOR EACH INPUT NODE.");
        }

    };

    ImageEditorController.prototype.getObjectExecuteGraph = function(sWorkflowId,sName,sDescription,asInputNodeNames,
                                                                         asInputFileNames,asOutputNodeNames,asOutputFileNames)
    {
        if(this.areOkDataExecuteGraph(sWorkflowId,sName,asInputNodeNames, asInputFileNames) === false)
        {
            return null;
        }
        var oExecuteGraph = this.getEmptyObjectExecuteGraph();
        oExecuteGraph.workflowId = sWorkflowId;
        oExecuteGraph.name = sName;
        oExecuteGraph.description = sDescription;
        oExecuteGraph.inputNodeNames = asInputNodeNames;
        oExecuteGraph.inputFileNames = asInputFileNames;
        oExecuteGraph.outputNodeNames = asOutputNodeNames;
        oExecuteGraph.outputFileNames = asOutputFileNames;

        return oExecuteGraph;
    };

    ImageEditorController.prototype.areOkDataExecuteGraph = function(sWorkflowId,sName,asInputNodeNames,
                                                                         asInputFileNames)
    {
        var bReturnValue = true;
        if(utilsIsStrNullOrEmpty(sWorkflowId) || utilsIsStrNullOrEmpty(sName) || utilsIsObjectNullOrUndefined(asInputNodeNames) ||
            utilsIsObjectNullOrUndefined(asInputFileNames) )
        {
            bReturnValue = false;
        }
        if(asInputNodeNames.length !== asInputFileNames.length)
        {
            bReturnValue = false;
        }

        return bReturnValue;
    };

    ImageEditorController.prototype.getEmptyObjectExecuteGraph = function()
    {
        return {
            workflowId:"",
            name:"",
            description:"",
            inputNodeNames:[],
            inputFileNames:[],
            outputNodeNames:[],
            outputFileNames:[]
        }
    };
    /**
     * executeGraphFromWorkflowId
     * @param sWorkspaceId
     * @param sProductNameSelected
     * @param sDestinationProductName
     * @param sWorkflowId
     * @returns {boolean}
     */
    ImageEditorController.prototype.executeGraphFromWorkflowId = function(sWorkspaceId,oObjectWorkFlow)
    {
        if(utilsIsObjectNullOrUndefined(sWorkspaceId) === true)
        {
            return false;
        }
        if(utilsIsObjectNullOrUndefined(oObjectWorkFlow) === true)
        {
            return false;
        }
        var oController = this;
        this.m_oSnapOperationService.executeGraphFromWorkflowId(sWorkspaceId,oObjectWorkFlow).then(function (data) {
            if(utilsIsObjectNullOrUndefined(data.data) === false && data.data.boolValue === true )
            {
                oController.cleanAllExecuteWorkflowFields();
            }
            else
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN EXECUTE WORKFLOW,");
            }
            oController.closeDialogWithDelay("",500);

        },function (error) {

            oController.cleanAllExecuteWorkflowFields();
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN EXECUTE WORKFLOW");
        });

        return true;
    };
    ImageEditorController.prototype.closeDialogWithDelay = function(result,iDelay) {

        this.m_oClose(result, 700); // close, but give 500ms for bootstrap to animate
    };
    /**
     * deleteWorkflow
     * @param oWorkflow
     * @returns {boolean}
     */
    ImageEditorController.prototype.deleteWorkflow = function(oWorkflow)
    {
        if( utilsIsObjectNullOrUndefined(oWorkflow) === true)
        {
            return false;
        }
        var oController = this;
        this.m_oSnapOperationService.deleteWorkflow(oWorkflow.workflowId).then(function (data)
        {
            if(utilsIsObjectNullOrUndefined(data.data) == false)
            {
                oController.getWorkflowsByUser();
            }
            else
            {
                //TODO ERROR
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN DELETE WORKFLOW");
            }
        },function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN DELETE WORKFLOW");
        });

        return true;
    };

    /**
     * uploadUserGraphOnServer
     */

    ImageEditorController.prototype.uploadUserGraphOnServer = function()
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
    ImageEditorController.prototype.uploadGraph = function(sWorkspaceId,sName,sDescription,oBody)
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
        this.m_oSnapOperationService.uploadGraph(this.m_sWorkspaceId,sName,sDescription,oBody).then(function (data) {
            if(utilsIsObjectNullOrUndefined(data.data) == false)
            {
                //Reload list o workFlows
                oController.getWorkflowsByUser();
                oController.cleanAllUploadWorkflowFields();
                var oDialog = utilsVexDialogAlertBottomRightCorner("SUCCESSFUL UPLOAD");
                utilsVexCloseDialogAfter(4000,oDialog);
            }
            else
            {
                //TODO ERROR
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN UPLOAD WORKFLOW PROCESS");
            }

            oController.isUploadingWorkflow = false;
        },function (error) {
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
    ImageEditorController.prototype.isSelectedProduct = function(){
        return (this.m_asSelectedProducts.length > 0);
    };
    /**
     *
     * @returns {boolean}
     */
    ImageEditorController.prototype.isSelectedWorkFlow = function(){
        return !utilsIsObjectNullOrUndefined(this.m_oSelectedWorkflow);
    };
    /**
     *
     * @returns {boolean}
     */
    ImageEditorController.prototype.isUploadedNewWorkFlow = function (){
        return !utilsIsObjectNullOrUndefined(this.m_oFile);
    };

    /**
     *
     */
    ImageEditorController.prototype.cleanAllUploadWorkflowFields = function (){
        this.m_oWorkflowFileData = {
            workflowName:"",
            workflowDescription:""
        };
        this.m_oFile = null;
    };

    /**
     *
     */
    ImageEditorController.prototype.cleanAllExecuteWorkflowFields = function (){
        this.m_asSelectedProducts = [];
        this.m_oSelectedWorkflow = null;
    };

    ImageEditorController.prototype.isPossibleDoUpload = function()
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

    ImageEditorController.prototype.getSingleInputWorkflow = function()
    {
        var iNumberOfWorkflows = this.m_aoWorkflows.length;
        var aoReturnArray = [];
        for(var iIndexWorkflow = 0 ; iIndexWorkflow < iNumberOfWorkflows ; iIndexWorkflow++)
        {
            if(this.m_aoWorkflows[iIndexWorkflow].inputNodeNames.length < 2 )
            {
                aoReturnArray.push(this.m_aoWorkflows[iIndexWorkflow]);
            }
        }
        return aoReturnArray;
    };
    ImageEditorController.prototype.getMultipleInputWorkflow = function()
    {
        var iNumberOfWorkflows = this.m_aoWorkflows.length;
        var aoReturnArray = [];
        for(var iIndexWorkflow = 0 ; iIndexWorkflow < iNumberOfWorkflows ; iIndexWorkflow++)
        {
            aoReturnArray.push(this.m_aoWorkflows[iIndexWorkflow]);
        }
        return aoReturnArray;
    };

    ImageEditorController.prototype.addProductInputInNode = function(sNode,sProduct)
    {
        if(utilsIsStrNullOrEmpty(sNode) || utilsIsStrNullOrEmpty(sProduct) )
        {
            return false;
        }

        var iIndexOfElement = utilsFindObjectInArray(this.m_oSelectedMultiInputWorkflow.inputNodeNames,sNode);

        if(iIndexOfElement === -1)
        {
            return false;
        }

        // TODO: Refactoring del ciclo
        for(var iProducts = 0; iProducts<this.m_aoProducts.length; iProducts++ ) {
            if (this.m_aoProducts[iProducts].name === sProduct) {
                this.m_oSelectedMultiInputWorkflow.inputFileNames[iIndexOfElement] =  this.m_aoProducts[iProducts].fileName;
                break;
            }
        }




        return true;
    };

    ImageEditorController.$inject = [
        '$scope',
        'close',
        'extras',
        'SnapOperationService',
        'ConstantsService',
        '$http'
    ];
    return ImageEditorController;
})();
window.ImageEditorController = ImageEditorController;
