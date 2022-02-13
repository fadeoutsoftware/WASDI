/**
 * Created by s.adamo on 09/02/2017.
 */


 'use strict';
 angular.module('wasdi.WorkflowService', ['wasdi.WorkflowService']).
 service('WorkflowService', ['ConstantsService','$rootScope','$http', function (oConstantsService,$rootScope,$http) {
     this.APIURL = oConstantsService.getAPIURL();
     this.m_oHttp = $http;
     this.m_oController = this;
     this.m_oConstantService = oConstantsService;

     // header field for post calls
     this.m_oOptions = {
         transformRequest: angular.identity,
         headers: { 'Content-Type': undefined }
     };

     //Run a workflow by Id
     this.executeGraphFromWorkflowId = function (sWorkspaceInput, oSnapWorkflowViewModel) {
         return this.m_oHttp.post(this.APIURL + '/workflows/run?workspace=' + sWorkspaceInput, oSnapWorkflowViewModel);
     };

     // Upload a workflow by file
     this.uploadByFile = function (sWorkspaceInput, sName, sDescription, oBody, bIsPublic) {
         return this.m_oHttp.post(this.APIURL + '/workflows/uploadfile?workspace=' + sWorkspaceInput + "&name=" + sName +
             "&description=" + sDescription + "&public=" + bIsPublic, oBody, this.m_oOptions);
     };

     // Update workflow xml file
     this.updateGraphFile = function (sWorkflowId, oBody) {
         return this.m_oHttp.post(this.APIURL + '/workflows/updatefile?workflowid=' + sWorkflowId, oBody, this.m_oOptions);
     }

     // Update workflow parameters
     this.updateGraphParameters = function (sWorkflowId, sName, sDescription, bIsPublic) {
         return this.m_oHttp.post(this.APIURL + '/workflows/updateparams?workflowid=' + sWorkflowId +
             '&name=' + sName +
             '&description=' + sDescription +
             '&public=' + bIsPublic);
     }

     // Delete workflow
     this.deleteWorkflow = function (sWorkflowId) {
         return this.m_oHttp.get(this.APIURL + '/workflows/delete?workflowId=' + sWorkflowId);
     };

     // Get Workflow list by user
     this.getWorkflowsByUser = function () {
         return this.m_oHttp.get(this.APIURL + '/workflows/getbyuser');
     };

     // Download workflow file
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
 
         window.location.href = sAPIUrl + "/workflows/download" + urlParams;
     };

     /************************************ SHARINGS **************************************************/
 
     // Get list of shared users by workflow id
     this.getUsersBySharedWorkflow = function (sWorkflowId) {
         return this.m_oHttp.get(this.APIURL + '/workflows/share/byworkflow?workflowId=' + sWorkflowId);
     }
 
     // Add sharing
     this.addWorkflowSharing = function (sWorkflowId, sUserId) {
         return this.m_oHttp.put(this.APIURL + '/workflows/share/add?workflowId=' + sWorkflowId + '&userId=' + sUserId);
     }
 
     // Remove sharing
     this.removeWorkflowSharing = function (sWorkflowId, sUserId) {
         return this.m_oHttp.delete(this.APIURL + '/workflows/share/delete?workflowId=' + sWorkflowId + '&userId=' + sUserId);
 
     }
 
     // Get workflow xml
     this.getWorkflowXml= function(sWorkflowId){
         return this.m_oHttp.get(this.APIURL + '/workflows/getxml?workflowId=' + sWorkflowId);
     }
 
     // Update workflow xml
     this.postWorkflowXml= function(sWorkflowId,sWorkflowXml){
         return this.m_oHttp.post(this.APIURL + '/workflows/updatexml?workflowId=' + sWorkflowId, sWorkflowXml,this.m_oOptions);
     }
 }]);
 
 