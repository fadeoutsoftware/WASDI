/**
 * Created by s.adamo on 09/02/2017.
 */


'use strict';
angular.module('wasdi.SnapOperationService', ['wasdi.SnapOperationService']).
service('SnapOperationService', ['$http',  'ConstantsService', function ($http, oConstantsService) {
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;
    this.m_oController = this;
    /************************************ RADAR OPERATIONS ************************************/
    this.ApplyOrbit = function (sSourceProductName, sDestinationProductName, sWorkspaceId,oOptionsInput) {
        return this.Operation("radar/applyOrbit", sSourceProductName, sDestinationProductName, sWorkspaceId, oOptionsInput);//orbit
    };
    // this.ApplyOrbit = function (sSourceProductName, sDestinationProductName, sWorkspaceId,oOptionsInput) {
    //     // return this.Operation("radar/applyOrbit", sSourceProductName, sDestinationProductName, sWorkspaceId, oOptionsInput);//orbit
    //     var sUrl = this.APIURL + '/processing/radar/applyOrbit?sSourceProductName=' + sSourceProductName + '&sDestinationProductName=' + sDestinationProductName + '&sWorkspaceId=' + sWorkspaceId;
    //     var oConfig = {header:""};
    //     return this.m_oHttp.post(sUrl,oOptionsInput,oConfig);
    // };

    this.Calibrate = function (sSourceProductName, sDestinationProductName, sWorkspaceId,oOptionsInput) {

        return this.Operation("radar/radiometricCalibration", sSourceProductName, sDestinationProductName, sWorkspaceId,oOptionsInput);//calibrate
    };
    this.Multilooking = function (sSourceProductName, sDestinationProductName, sWorkspaceId,oOptionsInput) {

        return this.Operation("radar/multilooking", sSourceProductName, sDestinationProductName, sWorkspaceId,oOptionsInput);//multilooking
    };

    /************************************ GEOMETRIC OPERATIONS ************************************/
    this.RangeDopplerTerrainCorrection = function (sSourceProductName, sDestinationProductName, sWorkspaceId,oOptionsInput) {

        return this.Operation("geometric/rangeDopplerTerrainCorrection", sSourceProductName, sDestinationProductName, sWorkspaceId,oOptionsInput);//terrain
    };

    /************************************ NDVI OPERATIONS ************************************/
    this.NDVI = function (sSourceProductName, sDestinationProductName, sWorkspaceId,oOptionsInput) {

        return this.Operation("optical/ndvi", sSourceProductName, sDestinationProductName, sWorkspaceId,oOptionsInput);//ndvi
    };

    /************************************ Workflow **************************************************/
    this.postWorkFlow =  function(oFileXmlInput,sWorkspaceInput,sSourceInput,sDestinationInput){

        return this.m_oHttp.post(this.APIURL + '/processing/graph?workspace=' + sWorkspaceInput + '&source=' + sSourceInput + '&destination=' + sDestinationInput, oFileXmlInput);
    };

    //Run a workflow that has been stored in WASDI.
    this.executeGraphFromWorkflowId = function(sWorkspaceInput,oSnapWorkflowViewModel)
    {
        return this.m_oHttp.post(this.APIURL + '/processing/graph_id?workspace=' + sWorkspaceInput,oSnapWorkflowViewModel);
    };

    this.uploadGraph = function(sWorkspaceInput,sName,sDescription,oBody,bIsPublic)
    {
        var oOptions = {
            transformRequest: angular.identity,
            headers: {'Content-Type': undefined}
        };
        return this.m_oHttp.post(this.APIURL + '/processing/uploadgraph?workspace=' + sWorkspaceInput + "&name=" + sName +
                                    "&description=" + sDescription + "&public=" + bIsPublic,oBody ,oOptions);
    };

    this.deleteWorkflow = function(sWorkflowId)
    {
        return this.m_oHttp.get(this.APIURL + '/processing/deletegraph?workflowId=' + sWorkflowId );
    };

    this.getWorkflowsByUser = function()
    {
        return this.m_oHttp.get(this.APIURL + '/processing/getgraphsbyusr');
    };
    /************************************ Masks ************************************/
    this.getListOfProductMask = function(sFile,sBand, sWorkspaceId)
    {
        return this.m_oHttp.get(this.APIURL + '/processing/productmasks?file=' + sFile + "&band=" + sBand + "&workspaceId=" + sWorkspaceId);
    };

    /***************************************** Histogram *****************************************/
    this.getProductColorManipulation = function(sFile,sBand,bAccurate,sWorkspaceId)
    {
        return this.m_oHttp.get(this.APIURL + '/processing/productcolormanipulation?file=' + sFile + "&band=" + sBand + "&workspaceId=" + sWorkspaceId + "&accurate=" + bAccurate);
    };
    /************************************ OTHERS **************************************************/

    this.Operation = function(sOperation, sSourceProductName, sDestinationProductName, sWorkspaceId, oOptionsInput)
    {
        var sUrl = this.APIURL + '/processing/{sOperation}?sSourceProductName=' + sSourceProductName + '&sDestinationProductName=' + sDestinationProductName + '&sWorkspaceId=' + sWorkspaceId;
        var oConfig = {header:""};
        sUrl = sUrl.replace("{sOperation}", sOperation);
        return this.m_oHttp.post(sUrl,oOptionsInput,oConfig);
    };


    this.runSaba = function (sFile, sWorkspaceId) {
        return this.m_oHttp.get(this.APIURL + '/processing/saba?file=' + sFile + "&workspaceId=" + sWorkspaceId);
    }

    this.publishSabaResult = function (sFile, sWorkspaceId) {
        return this.m_oHttp.get(this.APIURL + '/processing/ddspublishsaba?file=' + sFile + "&workspaceId=" + sWorkspaceId);
    }

    this.getWPSList = function () {
        return this.m_oHttp.get(this.APIURL + '/processing/WPSlist');
    };

    this.runListFlood = function (oListFlood,sWorkspaceId) {
        return this.m_oHttp.post(this.APIURL + '/processing/asynchlistflood?workspaceId='+sWorkspaceId ,oListFlood);
    };

    this.runJRCWorkflow = function (oJRC,sWorkspaceId) {
        return this.m_oHttp.post(this.APIURL + '/processing/asynchjrctest?workspaceId='+sWorkspaceId ,oJRC);
    };
    this.runJRCS2 = function (oJRC,sWorkspaceId) {
        // return this.m_oHttp.post(this.APIURL + '/processing/asynchjrctest?workspaceId='+sWorkspaceId ,oJRC);
    };
    this.runJRCClassification = function (oJRC,sWorkspaceId) {
        return this.m_oHttp.post(this.APIURL + '/processing/asynchjrctest2?workspaceId='+sWorkspaceId ,oJRC);
    };
    this.geometricMosaic = function(sWorkspaceId,oMosaic){
        return this.m_oHttp.post(this.APIURL + '/processing/geometric/mosaic?workspaceId='+sWorkspaceId ,oMosaic);
    }
}]);

