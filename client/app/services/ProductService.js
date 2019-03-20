/**
 * Created by p.campanella on 18/11/2016.
 */

'use strict';
angular.module('wasdi.ProductService', ['wasdi.ProductService']).
service('ProductService', ['$http',  'ConstantsService', function ($http, oConstantsService) {
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;

    this.getProductByName = function(sProductName) {
        return this.m_oHttp.get(this.APIURL + '/product/byname?sProductName='+sProductName);
    };

    this.getProductListByWorkspace = function (sWorkspaceId) {
        return this.m_oHttp.get(this.APIURL + '/product/byws?sWorkspaceId='+sWorkspaceId);
    };

    this.addProductToWorkspace = function (sProductName, sWorkspaceId) {
        return this.m_oHttp.get(this.APIURL + '/product/addtows?sProductName='+sProductName+'&sWorkspaceId='+sWorkspaceId);
    };

    this.deleteProductFromWorkspace = function (sProductName, sWorkspaceId, bDeleteFile, bDeleteLayer) {
        //return this.m_oHttp.get('http://localhost:8080/wasdiwebserver/rest/product/delete?sProductName='+sProductName+'&sWorkspaceId='+sWorkspaceId + '&bDeleteFile=' + bDeleteFile);
        return this.m_oHttp.get(this.APIURL + '/product/delete?sProductName='+sProductName+'&sWorkspaceId='+sWorkspaceId + '&bDeleteFile=' + bDeleteFile + '&bDeleteLayer=' + bDeleteLayer);
    };
    
    this.updateProduct = function (oProductViewModel) {
        return this.m_oHttp.post(this.APIURL + '/product/update', oProductViewModel);
    };
    this.getMetadata = function(sProductName){
        return this.m_oHttp.get(this.APIURL+"/product/metadatabyname?sProductName="+sProductName);
    };
    this.getApiMetadata= function(sProductName){
        return this.APIURL+"/product/metadatabyname?sProductName="+sProductName;
    };
    this.uploadFile = function(sWorkspaceInput,oBody,sName)
    {
        var oOptions = {
            transformRequest: angular.identity,
            headers: {'Content-Type': 'multipart/form-data'}
        };
        return this.m_oHttp.post(this.APIURL + '/product/uploadfile?workspace=' + sWorkspaceInput + '&name=' + sName  ,oBody ,oOptions);
    };
}]);

