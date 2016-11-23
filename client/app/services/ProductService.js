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
    }

    this.getProductListByWorkspace = function (sWorkspaceId) {
        return this.m_oHttp.get(this.APIURL + '/product/byws?sWorkspaceId='+sWorkspaceId);
    }

    this.addProductToWorkspace = function (sProductName, sWorkspaceId) {
        return this.m_oHttp.get(this.APIURL + '/product/addtows?sProductName='+sProductName+'&sWorkspaceId='+sWorkspaceId);
    }
}]);

