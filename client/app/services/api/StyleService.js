/**
 * Created by PetruPetrescu on 25/02/2022.
 */

 'use strict';
 angular.module('wasdi.StyleService', ['wasdi.StyleService']).
 service('StyleService', ['ConstantsService','$rootScope','$http', function (oConstantsService,$rootScope,$http) {
     this.APIURL = oConstantsService.getAPIURL();
     this.m_oHttp = $http;
     this.m_oController = this;
     this.m_oConstantService = oConstantsService;

     // header field for post calls
     this.m_oOptions = {
         transformRequest: angular.identity,
         headers: { 'Content-Type': undefined }
     };

     // Upload a style by file
     this.uploadFile = function (sName, sDescription, oBody, bIsPublic) {
         return this.m_oHttp.post(this.APIURL + '/styles/uploadfile?' + "name=" + sName +
             "&description=" + sDescription + "&public=" + bIsPublic, oBody, this.m_oOptions);
     };

     // Update style xml file
     this.updateStyleFile = function (sStyleId, oBody) {
         return this.m_oHttp.post(this.APIURL + '/styles/updatefile?styleId=' + sStyleId, oBody, this.m_oOptions);
     }

     // Update style parameters
     this.updateStyleParameters = function (sStyleId, sDescription, bIsPublic) {
         return this.m_oHttp.post(this.APIURL + '/styles/updateparams?styleId=' + sStyleId +
             '&description=' + sDescription +
             '&public=' + bIsPublic);
     }

     // Delete style
     this.deleteStyle = function (sStyleId) {
         return this.m_oHttp.delete(this.APIURL + '/styles/delete?styleId=' + sStyleId);
     };

     // Get Style list by user
     this.getStylesByUser = function () {
         return this.m_oHttp.get(this.APIURL + '/styles/getbyuser');
     };

     // Download style file
     this.downloadStyle = function (sStyleId, sUrl = null) {
         var urlParams = "?" + "token=" + this.m_oConstantService.getSessionId();
         urlParams = urlParams + "&" + "styleId=" + sStyleId;
 
         var sAPIUrl = this.APIURL;
 
         if (typeof sUrl !== "undefined") {
             if (sUrl !== null) {
                 if (sUrl !== "") {
                     sAPIUrl = sUrl;
                 }
             }
         }
 
         window.location.href = sAPIUrl + "/styles/download" + urlParams;
     };

     /************************************ SHARINGS **************************************************/
 
     // Get list of shared users by style id
     this.getUsersBySharedStyle = function (sStyleId) {
         return this.m_oHttp.get(this.APIURL + '/styles/share/bystyle?styleId=' + sStyleId);
     }
 
     // Add sharing
     this.addStyleSharing = function (sStyleId, sUserId, sRights) {
         return this.m_oHttp.put(this.APIURL + '/styles/share/add?styleId=' + sStyleId + '&userId=' + sUserId+"&rights="+sRights);
     }
 
     // Remove sharing
     this.removeStyleSharing = function (sStyleId, sUserId) {
         return this.m_oHttp.delete(this.APIURL + '/styles/share/delete?styleId=' + sStyleId + '&userId=' + sUserId);
 
     }
 
     // Get style xml
     this.getStyleXml= function(sStyleId){
         return this.m_oHttp.get(this.APIURL + '/styles/getxml?styleId=' + sStyleId);
     }
 
     // Update style xml
     this.postStyleXml= function(sStyleId,sStyleXml){
         return this.m_oHttp.post(this.APIURL + '/styles/updatexml?styleId=' + sStyleId, sStyleXml,this.m_oOptions);
     }
 }]);
 
 