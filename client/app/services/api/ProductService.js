/**
 * Created by p.campanella on 18/11/2016.
 */

'use strict';
angular.module('wasdi.ProductService', ['wasdi.ProductService']).
    service('ProductService', ['$http', 'ConstantsService', function ($http, oConstantsService) {
        this.APIURL = oConstantsService.getAPIURL();
        this.m_oConstantsService = oConstantsService;
        this.m_bIgnoreWorkspaceApiUrl = oConstantsService.getIgnoreWorkspaceApiUrl();
        this.m_oHttp = $http;

        this.getProductListByWorkspace = function (sWorkspaceId) {
            return this.m_oHttp.get(this.APIURL + '/product/byws?workspace=' + sWorkspaceId);
        };

        this.getProductLightListByWorkspace = function (sWorkspaceId) {
            return this.m_oHttp.get(this.APIURL + '/product/bywslight?workspace=' + sWorkspaceId);
        };

        this.deleteProductFromWorkspace = function (sProductName, sWorkspaceId, bDeleteFile, bDeleteLayer) {

            var oWorkspace = this.m_oConstantsService.getActiveWorkspace();
            var sUrl = this.APIURL;

            if (oWorkspace != null && oWorkspace.apiUrl != null && !this.m_bIgnoreWorkspaceApiUrl) {
                sUrl = oWorkspace.apiUrl;
            }

            return this.m_oHttp.get(sUrl + '/product/delete?name=' + sProductName + '&workspace=' + sWorkspaceId + '&deletefile=' + bDeleteFile + '&deletelayer=' + bDeleteLayer);
        };

        this.deleteProductListFromWorkspace = function (sProductNameList, sWorkspaceId, bDeleteFile, bDeleteLayer) {
            if (sProductNameList.length == 0) {
                return 400; // bad parameters
            }
            var oWorkspace = this.m_oConstantsService.getActiveWorkspace();
            var sUrl = this.APIURL;

            if (oWorkspace != null && oWorkspace.apiUrl != null && !this.m_bIgnoreWorkspaceApiUrl) {
                sUrl = oWorkspace.apiUrl;
            }

            // the list is passed in the body request
            return this.m_oHttp.post(sUrl + '/product/deletelist?workspace=' + sWorkspaceId + '&deletefile=' + bDeleteFile + '&deletelayer=' + bDeleteLayer, sProductNameList);
        };

        this.updateProduct = function (oProductViewModel, workspaceId) {
            return this.m_oHttp.post(this.APIURL + '/product/update?workspace=' + workspaceId, oProductViewModel);
        };

        this.getProductMetadata = function (sProductName, sWorkspace) {

            var oWorkspace = this.m_oConstantsService.getActiveWorkspace();
            var sUrl = this.APIURL;

            if (oWorkspace != null && oWorkspace.apiUrl != null && !this.m_bIgnoreWorkspaceApiUrl) {
                sUrl = oWorkspace.apiUrl;
            }
            return sUrl + "/product/metadatabyname?name=" + sProductName + "&workspace=" + sWorkspace;
        };



        this.uploadFile = function (sWorkspaceInput, oBody, sName, sStyle) {

            var oWorkspace = this.m_oConstantsService.getActiveWorkspace();
            var sUrl = this.APIURL;

            if (oWorkspace != null && oWorkspace.apiUrl != null && !this.m_bIgnoreWorkspaceApiUrl) {
                sUrl = oWorkspace.apiUrl;
            }
            var oOptions = {
                transformRequest: angular.identity,
                // headers: {'Content-Type': 'multipart/form-data'}
                headers: { 'Content-Type': undefined }
            };

            sUrl = sUrl + '/product/uploadfile?workspace=' + sWorkspaceInput + '&name=' + sName;
            if (!utilsIsStrNullOrEmpty(sStyle)) {
                sUrl = sUrl + '&style=' + sStyle;
            }
            return this.m_oHttp.post(sUrl, oBody, oOptions);
        };
    }]);

