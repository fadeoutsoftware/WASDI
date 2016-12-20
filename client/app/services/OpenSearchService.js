/**
 * Created by s.adamo on 15/12/2016.
 */


'use strict';
angular.module('wasdi.OpenSearchService', ['wasdi.OpenSearchService']).
service('OpenSearchService', ['$http',  'ConstantsService', function ($http, oConstantsService) {
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;
    //----------API------------------------
    this.API_GET_PRODUCTS_COUNT = "/search/sentinel/count?sQuery=";
    this.API_GET_PRODUCTS = "/search/sentinel/result?sQuery=";
    //-------------------------------------

    this.getApiProductsCount = function (query) {
        return this.APIURL + this.API_GET_PRODUCTS_COUNT + query;
    }

    this.getApiProducts = function (query) {
        return this.APIURL + this.API_GET_PRODUCTS + query;
    }



}]);

