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
    this.API_GET_PROVIDERS = "/search/providers";
    this.API_GET_SEARCH = "/search/query?sQuery=";
    //-------------------------------------

    this.getApiProductsCount = function (query) {
        return this.APIURL + this.API_GET_PRODUCTS_COUNT + query;
        //return "fake-data/products_count.json";
    }

    this.getApiProducts = function (query) {
        return this.APIURL + this.API_GET_PRODUCTS + query;
        //return "http://localhost:8080/wasdiwebserver/rest/" + this.API_GET_PRODUCTS + query;
        //return "fake-data/feed.json";
        //return "fake-data/opensearch_fakeData.json";
    }

    this.getApiProductsWithProviders = function (query) {
        return this.APIURL + this.API_GET_SEARCH + query;
        //return "http://localhost:8080/wasdiwebserver/rest/" + this.API_GET_PRODUCTS + query;
        //return "fake-data/feed.json";
        //return "fake-data/opensearch_fakeData.json";
    }

    this.getListOfProvider = function()
    {
        return this.m_oHttp.get(this.APIURL + this.API_GET_PROVIDERS);
    }


}]);

