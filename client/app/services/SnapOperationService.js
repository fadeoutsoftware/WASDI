/**
 * Created by s.adamo on 09/02/2017.
 */


'use strict';
angular.module('wasdi.SnapOperationService', ['wasdi.SnapOperationService']).service('SnapOperationService', ['$http', 'ConstantsService', function ($http, oConstantsService) {
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;
    this.m_oController = this;
    this.m_oConstantService = oConstantsService;

    /************************************ Workflow **************************************************/
    this.postWorkFlow = function (oFileXmlInput, sWorkspaceInput, sSourceInput, sDestinationInput) {

        return this.m_oHttp.post(this.APIURL + '/processing/graph?workspace=' + sWorkspaceInput + '&source=' + sSourceInput + '&destination=' + sDestinationInput, oFileXmlInput);
    };

    //Run a workflow that has been stored in WASDI.
    this.executeGraphFromWorkflowId = function (sWorkspaceInput, oSnapWorkflowViewModel) {
        return this.m_oHttp.post(this.APIURL + '/processing/graph_id?workspace=' + sWorkspaceInput, oSnapWorkflowViewModel);
    };

    this.uploadGraph = function (sWorkspaceInput, sName, sDescription, oBody, bIsPublic) {
        var oOptions = {
            transformRequest: angular.identity,
            headers: {'Content-Type': undefined}
        };
        return this.m_oHttp.post(this.APIURL + '/processing/uploadgraph?workspace=' + sWorkspaceInput + "&name=" + sName +
            "&description=" + sDescription + "&public=" + bIsPublic, oBody, oOptions);
    };

    this.deleteWorkflow = function (sWorkflowId) {
        return this.m_oHttp.get(this.APIURL + '/processing/deletegraph?workflowId=' + sWorkflowId);
    };

    this.getWorkflowsByUser = function () {
        return this.m_oHttp.get(this.APIURL + '/processing/getgraphsbyusr');
    };

    this.downloadWorkflow = function (sWorkflowId, sUrl = null) {
        var urlParams = "?" + "token=" + this.m_oConstantService.getSessionId();
        urlParams = urlParams + "&" + "workflowId=" + sWorkflowId;

        var sAPIUrl = this.APIURL;

        if (typeof sUrl !== "undefined") {
            if (sUrl !== null) {
                if (sUrl !== "") {
                    sAPIUrl = sUrl;
                }
            }
        }

        window.location.href = sAPIUrl + "/processing/downloadgraph" + urlParams;
    };
    /************************************ Masks ************************************/
    this.getListOfProductMask = function (sFile, sBand, sWorkspaceId) {
        return this.m_oHttp.get(this.APIURL + '/processing/productmasks?file=' + sFile + "&band=" + sBand + "&workspaceId=" + sWorkspaceId);
    };

    /***************************************** Histogram *****************************************/
    this.getProductColorManipulation = function (sFile, sBand, bAccurate, sWorkspaceId) {
        return this.m_oHttp.get(this.APIURL + '/processing/productcolormanipulation?file=' + sFile + "&band=" + sBand + "&workspaceId=" + sWorkspaceId + "&accurate=" + bAccurate);
    };
    /************************************ SHARINGS **************************************************/

    this.getUsersBySharedWorkflow = function (sWorkflowId) {
        return this.m_oHttp.get(this.APIURL + '/processing/share/byworkflow?workflowId='+sWorkflowId);
    }

    this.addWorkflowSharing = function (sWorkflowId, sUserId) {
        return this.m_oHttp.put(this.APIURL + '/processing/share/add?workflowId=' + sWorkflowId + '&userId=' + sUserId);
    }

    this.removeWorkflowSharing = function (sWorkflowId, sUserId) {
        return this.m_oHttp.delete(this.APIURL + '/processing/share/delete?workflowId=' + sWorkflowId + '&userId=' + sUserId);

    }


    /************************************ OTHERS **************************************************/

    this.Operation = function (sOperation, sSourceProductName, sDestinationProductName, sWorkspaceId, oOptionsInput) {
        var sUrl = this.APIURL + '/processing/{sOperation}?sSourceProductName=' + sSourceProductName + '&sDestinationProductName=' + sDestinationProductName + '&sWorkspaceId=' + sWorkspaceId;
        var oConfig = {header: ""};
        sUrl = sUrl.replace("{sOperation}", sOperation);
        return this.m_oHttp.post(sUrl, oOptionsInput, oConfig);
    };

    this.getWPSList = function () {
        return this.m_oHttp.get(this.APIURL + '/processing/WPSlist');
    };

    this.geometricMosaic = function (sWorkspaceId, sDestinationProductName, oMosaic) {
        return this.m_oHttp.post(this.APIURL + '/processing/geometric/mosaic?sWorkspaceId=' + sWorkspaceId
            + "&sDestinationProductName=" + sDestinationProductName, oMosaic);
    }
}]);

