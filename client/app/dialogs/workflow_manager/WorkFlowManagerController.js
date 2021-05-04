/**
 * Created by a.corrado on 16/06/2017.
 */



var WorkFlowManagerController = (function () {

    function WorkFlowManagerController($scope, oClose, oExtras, oSnapOperationService, oConstantsService, oHttp, oModalService) {
        this.m_oScope = $scope;
        this.m_oClose = oClose;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oSnapOperationService = oSnapOperationService;
        this.m_oModalService = oModalService;
        this.m_oFile = null;
        this.m_aoProducts = this.m_oExtras.products;


        this.m_sWorkspaceId = this.m_oExtras.workflowId;
        this.m_asProductsName = utilsProjectGetProductsName(this.m_aoProducts);
        // this field is used by search enabled directive that requires array of object with
        // a "name" field. The map converts the string array in compatible objects
        this.m_aoProductsClass = this.m_asProductsName.map(name => ({name}));

        /*
        * this.m_aoProductClass ={
        * name : "s1nvonvsi",
        * value : "s1nvonvsi"
        * }
        * */


        this.m_asSelectedProducts = [];
        this.m_aoWorkflows = [];
        this.m_oSelectedWorkflow = null;
        this.m_oSelectedMultiInputWorkflow = null;
        // support var
        this.m_oWAPProduct = null;
        this.m_aoMultiInputSelectedProducts = {};
        // String variable for combobox decoration
        this.m_sWAPPdropdownname = "Select Product";

        this.m_oConstantsService = oConstantsService;
        this.m_oWorkflowFileData = {
            workflowName: "",
            workflowDescription: "",
            isPublic: false
        };
        this.isUploadingWorkflow = false;
        if (utilsIsObjectNullOrUndefined(this.m_oExtras.defaultTab) === true) {
            this.m_sSelectedWorkflowTab = 'WorkFlowTab3';
        } else {
            this.m_sSelectedWorkflowTab = this.m_oExtras.defaultTab;
        }
        // this.m_sSelectedWorkflowTab = 'WorkFlowTab1';
        this.m_bIsLoadingWorkflows = false;
        this.m_oHttp = oHttp;
        //$scope.close = oClose;


        $scope.close = function (result) {

            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };

        //Load workflows
        this.getWorkflowsByUser();

    }

    WorkFlowManagerController.prototype.selectedMultiInputWorkflow = function (oWorkflow) {
        this.m_oSelectedMultiInputWorkflow = oWorkflow;
        // create a dictionary
        this.m_aoMultiInputSelectedProducts = {};
    }

    WorkFlowManagerController.prototype.getWorkflowsByUser = function () {
        var oController = this;
        this.m_bIsLoadingWorkflows = true;
        this.m_oSnapOperationService.getWorkflowsByUser().then(function (data) {
            if (utilsIsObjectNullOrUndefined(data.data) == false) {
                oController.m_aoWorkflows = data.data;
            } else {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN GET WORKFLOWS, DATA NOT AVAILABLE");
            }

            //it changes the default tab, we can't visualize the 'WorkFlowTab1' because there aren't workflows
            if ((utilsIsObjectNullOrUndefined(oController.m_aoWorkflows) === true) || (oController.m_aoWorkflows.length === 0)) {
                oController.m_sSelectedWorkflowTab = 'WorkFlowTab2';
            }
            oController.m_bIsLoadingWorkflows = false;
        },function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR GETTING WORKFLOW LIST");
            oController.m_bIsLoadingWorkflows = false;
        });
    };



    /**
     * runWorkFlowPerProducts
     */
    WorkFlowManagerController.prototype.runWorkFlowPerProducts = function () {
        var iNumberOfProducts = this.m_asSelectedProducts.length;
        if (utilsIsObjectNullOrUndefined(this.m_oSelectedWorkflow) === true) {
            return false;
        }
        for (var iIndexSelectedProduct = 0; iIndexSelectedProduct < iNumberOfProducts; iIndexSelectedProduct++) {
            var oProduct = utilsProjectGetProductByName(this.m_asSelectedProducts[iIndexSelectedProduct], this.m_aoProducts);
            if (utilsIsObjectNullOrUndefined(oProduct)) {
                return false;
            }
            //TODO REMOVE IT
            // var sDestinationProductName = oProduct.name + "_workflow";
            this.m_oSelectedWorkflow.inputFileNames.push(oProduct.fileName);
            var oSnapWorkflowViewModel = this.getObjectExecuteGraph(this.m_oSelectedWorkflow.workflowId, this.m_oSelectedWorkflow.name, this.m_oSelectedWorkflow.description,
                this.m_oSelectedWorkflow.inputNodeNames, this.m_oSelectedWorkflow.inputFileNames, this.m_oSelectedWorkflow.outputNodeNames,
                this.m_oSelectedWorkflow.outputFileNames);
            if (utilsIsObjectNullOrUndefined(oSnapWorkflowViewModel) === false) {
                this.executeGraphFromWorkflowId(this.m_sWorkspaceId, oSnapWorkflowViewModel);
            } else {
                utilsVexDialogAlertTop("GURU MEDITATION<br>YOU HAVE TO INSERT A PRODUCT FOR EACH INPUT NODE.");
            }
        }
        return true;
    };

    WorkFlowManagerController.prototype.runMultiInputWorkFlow = function () {
        for (var sNodeName in this.m_aoMultiInputSelectedProducts) {
            // check if the property/key is defined in the object itself, not in parent
            if (this.m_aoMultiInputSelectedProducts.hasOwnProperty(sNodeName)) {
                this.addProductInputInNode(sNodeName,this.m_aoMultiInputSelectedProducts[sNodeName].name);
            }
        }
        var oSnapWorkflowViewModel = this.getObjectExecuteGraph(this.m_oSelectedMultiInputWorkflow.workflowId, this.m_oSelectedMultiInputWorkflow.name, this.m_oSelectedMultiInputWorkflow.description,
            this.m_oSelectedMultiInputWorkflow.inputNodeNames, this.m_oSelectedMultiInputWorkflow.inputFileNames, this.m_oSelectedMultiInputWorkflow.outputNodeNames,
            this.m_oSelectedMultiInputWorkflow.outputFileNames);
        if (utilsIsObjectNullOrUndefined(oSnapWorkflowViewModel) === false) {
            this.executeGraphFromWorkflowId(this.m_sWorkspaceId, oSnapWorkflowViewModel);
        } else {
            utilsVexDialogAlertTop("GURU MEDITATION<br>YOU HAVE TO INSERT A PRODUCT FOR EACH INPUT NODE.");
        }


    };

    WorkFlowManagerController.prototype.getObjectExecuteGraph = function (sWorkflowId, sName, sDescription, asInputNodeNames,
                                                                          asInputFileNames, asOutputNodeNames, asOutputFileNames) {
        if (this.areOkDataExecuteGraph(sWorkflowId, sName, asInputNodeNames, asInputFileNames) === false) {
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

    WorkFlowManagerController.prototype.areOkDataExecuteGraph = function (sWorkflowId, sName, asInputNodeNames,
                                                                          asInputFileNames) {
        var bReturnValue = true;
        if (utilsIsStrNullOrEmpty(sWorkflowId) || utilsIsStrNullOrEmpty(sName) || utilsIsObjectNullOrUndefined(asInputNodeNames) ||
            utilsIsObjectNullOrUndefined(asInputFileNames)) {
            bReturnValue = false;
        }
        if (asInputNodeNames.length !== asInputFileNames.length) {
            bReturnValue = false;
        }

        return bReturnValue;
    };

    WorkFlowManagerController.prototype.getEmptyObjectExecuteGraph = function () {
        return {
            workflowId: "",
            name: "",
            description: "",
            inputNodeNames: [],
            inputFileNames: [],
            outputNodeNames: [],
            outputFileNames: []
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
    WorkFlowManagerController.prototype.executeGraphFromWorkflowId = function (sWorkspaceId, oObjectWorkFlow) {
        if (utilsIsObjectNullOrUndefined(sWorkspaceId) === true) {
            return false;
        }
        if (utilsIsObjectNullOrUndefined(oObjectWorkFlow) === true) {
            return false;
        }
        var oController = this;
        this.m_oSnapOperationService.executeGraphFromWorkflowId(sWorkspaceId, oObjectWorkFlow).then(function (data) {
            if (utilsIsObjectNullOrUndefined(data.data) === false && data.data.boolValue === true) {
                oController.cleanAllExecuteWorkflowFields();
            } else {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN EXECUTE WORKFLOW,");
            }
            oController.closeDialogWithDelay("", 500);

        },function (error) {

            oController.cleanAllExecuteWorkflowFields();
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN EXECUTE WORKFLOW");
        });

        return true;
    };
    WorkFlowManagerController.prototype.closeDialogWithDelay = function (result, iDelay) {

        this.m_oClose(result, 700); // close, but give 500ms for bootstrap to animate
    };
    /**
     * deleteWorkflow
     * @param oWorkflow
     * @returns {boolean}
     */
    WorkFlowManagerController.prototype.deleteWorkflow = function (oWorkflow) {
        if (utilsIsObjectNullOrUndefined(oWorkflow) === true) {
            return false;
        }
        var oController = this;
        this.m_oSnapOperationService.deleteWorkflow(oWorkflow.workflowId).then(function (data) {
            if (utilsIsObjectNullOrUndefined(data.data) == false) {
                oController.getWorkflowsByUser();
            } else {
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

    WorkFlowManagerController.prototype.uploadUserGraphOnServer = function () {

        if (utilsIsStrNullOrEmpty(this.m_oWorkflowFileData.workflowName) === true) {
            this.m_oWorkflowFileData.workflowName = "workflow";
        }
        var oBody = new FormData();
        oBody.append('file', this.m_oFile[0]);

        this.uploadGraph(this.m_sWorkspaceId, this.m_oWorkflowFileData.workflowName, this.m_oWorkflowFileData.workflowDescription,
            this.m_oWorkflowFileData.isPublic, oBody);
    };

    /**
     * uploadGraph
     * @param sWorkspaceId
     * @param sName
     * @param sDescription
     * @param oBody
     * @returns {boolean}
     */
    WorkFlowManagerController.prototype.uploadGraph = function (sWorkspaceId, sName, sDescription, bIsPublic, oBody) {
        if (utilsIsObjectNullOrUndefined(sWorkspaceId) === true) {
            return false;
        }
        if (utilsIsObjectNullOrUndefined(sName) === true || utilsIsStrNullOrEmpty(sName) === true) {
            return false;
        }
        if (utilsIsObjectNullOrUndefined(sDescription) === true)//|| utilsIsStrNullOrEmpty(sDescription) === true
        {
            return false;
        }
        if (utilsIsObjectNullOrUndefined(oBody) === true) {
            return false;
        }
        this.isUploadingWorkflow = true;
        var oController = this;
        this.m_oSnapOperationService.uploadGraph(this.m_sWorkspaceId, sName, sDescription, oBody, bIsPublic).then(function (data) {
            if (utilsIsObjectNullOrUndefined(data.data) == false) {
                //Reload list o workFlows
                oController.getWorkflowsByUser();
                oController.cleanAllUploadWorkflowFields();
                var oDialog = utilsVexDialogAlertBottomRightCorner("SUCCESSFUL UPLOAD");
                utilsVexCloseDialogAfter(4000, oDialog);
            } else {
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
    WorkFlowManagerController.prototype.isSelectedProduct = function () {
        return (this.m_asSelectedProducts.length > 0);
    };
    /**
     *
     * @returns {boolean}
     */
    WorkFlowManagerController.prototype.isSelectedWorkFlow = function () {
        return !utilsIsObjectNullOrUndefined(this.m_oSelectedWorkflow);
    };
    /**
     *
     * @returns {boolean}
     */
    WorkFlowManagerController.prototype.isUploadedNewWorkFlow = function () {
        return !utilsIsObjectNullOrUndefined(this.m_oFile);
    };

    /**
     *
     */
    WorkFlowManagerController.prototype.cleanAllUploadWorkflowFields = function () {
        this.m_oWorkflowFileData = {
            workflowName: "",
            workflowDescription: "",
            isPublic: false
        };
        this.m_oFile = null;
    };

    /**
     *
     */
    WorkFlowManagerController.prototype.cleanAllExecuteWorkflowFields = function () {
        this.m_asSelectedProducts = [];
        this.m_oSelectedWorkflow = null;
    };

    WorkFlowManagerController.prototype.isPossibleDoUpload = function () {
        // this.m_oWorkflowFileData.workflowName,this.m_oWorkflowFileData.workflowDescription    this.m_oFile[0]
        var bReturnValue = false;
        if ((utilsIsStrNullOrEmpty(this.m_oWorkflowFileData.workflowName) === false) && (utilsIsStrNullOrEmpty(this.m_oWorkflowFileData.workflowDescription) === false)
            && (utilsIsObjectNullOrUndefined(this.m_oFile[0]) === false)) {
            bReturnValue = true;
        }
        return bReturnValue;
    };

    WorkFlowManagerController.prototype.getSingleInputWorkflow = function () {
        var iNumberOfWorkflows = this.m_aoWorkflows.length;
        var aoReturnArray = [];
        for (var iIndexWorkflow = 0; iIndexWorkflow < iNumberOfWorkflows; iIndexWorkflow++) {
            if (this.m_aoWorkflows[iIndexWorkflow].inputNodeNames.length < 2) {
                aoReturnArray.push(this.m_aoWorkflows[iIndexWorkflow]);
            }
        }
        return aoReturnArray;
    };
    WorkFlowManagerController.prototype.getMultipleInputWorkflow = function () {
        var iNumberOfWorkflows = this.m_aoWorkflows.length;
        var aoReturnArray = [];
        for (var iIndexWorkflow = 0; iIndexWorkflow < iNumberOfWorkflows; iIndexWorkflow++) {
            aoReturnArray.push(this.m_aoWorkflows[iIndexWorkflow]);
        }
        return aoReturnArray;
    };

    WorkFlowManagerController.prototype.addProductInputInNode = function (sNode, sProduct) {
        if (utilsIsStrNullOrEmpty(sNode) || utilsIsStrNullOrEmpty(sProduct)) {
            return false;
        }

        var iIndexOfElement = utilsFindObjectInArray(this.m_oSelectedMultiInputWorkflow.inputNodeNames, sNode);

        if (iIndexOfElement === -1) {
            return false;
        }

        // TODO: Refactoring del ciclo
        for (var iProducts = 0; iProducts < this.m_aoProducts.length; iProducts++) {
            if (this.m_aoProducts[iProducts].name === sProduct) {
                this.m_oSelectedMultiInputWorkflow.inputFileNames[iIndexOfElement] = this.m_aoProducts[iProducts].fileName;
                break;
            }
        }

        return true;
    };

    WorkFlowManagerController.prototype.openEditWorkflowDialog = function (oWorkflow) {
        var oController = this;

        oController.m_oModalService.showModal({
            templateUrl: "dialogs/workflow_manager/WorkflowView.html",
            controller: "WorkflowController",
            inputs: {
                extras: {
                    workflow:oWorkflow
                }
            }
        }).then(function (modal) {
            modal.element.modal();
            modal.close.then(function (oResult) {
                //oController.m_oProcessesLaunchedService.loadProcessesFromServer(oController.m_oActiveWorkspace.workspaceId);
            });
        });
    }


    WorkFlowManagerController.prototype.openDeleteWorkflowDialog = function (oWorkflow) {
        if (utilsIsObjectNullOrUndefined(oWorkflow) === true) {
            return false;
        }
        var oController = this;
        var oReturnFunctionValue = function (oValue) {
            if (oValue === true) {
                oController.deleteWorkflow(oWorkflow);
            }

        }
        utilsVexDialogConfirm("Do you want to delete workflow: " + oWorkflow.name + " ?", oReturnFunctionValue);
        return true;
    };

    WorkFlowManagerController.prototype.downloadWorkflow = function (oWorkflow) {
        if (utilsIsObjectNullOrUndefined(oWorkflow) === true) {
            return false;
        }


        this.m_oSnapOperationService.downloadWorkflow(oWorkflow.workflowId);
        return true;
    };

    WorkFlowManagerController.prototype.isTheOwnerOfWorkflow = function (oWorkflow) {
        var oUser = this.m_oConstantsService.getUser();
        if ((utilsIsObjectNullOrUndefined(oWorkflow) === true) || (utilsIsObjectNullOrUndefined(oUser) === true)) {
            return false;
        }
        var sUserIdOwner = oWorkflow.userId;

        if (sUserIdOwner === oUser.userId) {
            return true;
        }
        return false;
    }
    WorkFlowManagerController.prototype.selectBatchWorkflow = function (oWorkflow) {
        if (utilsIsObjectNullOrUndefined(oWorkflow) === true) {
            return false;
        }
        this.m_asSelectedProducts = [];
        this.m_oSelectedWorkflow = oWorkflow;
    };
    WorkFlowManagerController.$inject = [
        '$scope',
        'close',
        'extras',
        'SnapOperationService',
        'ConstantsService',
        '$http',
        'ModalService',

    ];
    return WorkFlowManagerController;
})();
