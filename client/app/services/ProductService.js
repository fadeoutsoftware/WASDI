/**
 * Created by p.campanella on 18/11/2016.
 */

'use strict';
angular.module('wasdi.ProductService', ['wasdi.ProductService']).
service('ProductService', ['$http',  'ConstantsService', function ($http, oConstantsService) {
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;

    this.getProductByName = function(sProductName) {
        return this.m_oHttp.get(this.APIURL + '/product/byname?sProductName='+sProductName+'&workspace='+oConstantsService.getActiveWorkspace().workspaceId);
    };

    this.getProductListByWorkspace = function (sWorkspaceId) {
        return this.m_oHttp.get(this.APIURL + '/product/byws?sWorkspaceId='+sWorkspaceId);
    };

    this.getProductLightListByWorkspace = function (sWorkspaceId) {
        return this.m_oHttp.get(this.APIURL + '/product/bywslight?sWorkspaceId='+sWorkspaceId);
    };

    this.addProductToWorkspace = function (sProductName, sWorkspaceId) {
        return this.m_oHttp.get(this.APIURL + '/product/addtows?sProductName='+sProductName+'&sWorkspaceId='+sWorkspaceId);
    };

    this.deleteProductFromWorkspace = function (sProductName, sWorkspaceId, bDeleteFile, bDeleteLayer) {
        //return this.m_oHttp.get('http://localhost:8080/wasdiwebserver/rest/product/delete?sProductName='+sProductName+'&sWorkspaceId='+sWorkspaceId + '&bDeleteFile=' + bDeleteFile);
        return this.m_oHttp.get(this.APIURL + '/product/delete?sProductName='+sProductName+'&sWorkspaceId='+sWorkspaceId + '&bDeleteFile=' + bDeleteFile + '&bDeleteLayer=' + bDeleteLayer);
    };

    this.updateProduct = function (oProductViewModel,workspaceId) {
        return this.m_oHttp.post(this.APIURL + '/product/update?workspace=' + workspaceId, oProductViewModel);
    };

    this.getApiMetadata= function(sProductName, sWorkspace){
        return this.APIURL+"/product/metadatabyname?sProductName="+sProductName+"&workspace="+sWorkspace;
    };



    this.uploadFile = function(sWorkspaceInput,oBody,sName)
    {
        var oOptions = {
            transformRequest: angular.identity,
            // headers: {'Content-Type': 'multipart/form-data'}
            headers: {'Content-Type': undefined}
        };
        return this.m_oHttp.post(this.APIURL + '/product/uploadfile?workspace=' + sWorkspaceInput + '&name=' + sName  ,oBody ,oOptions);
    };
}]);

