/**
 * Created by s.adamo on 15/12/2016.
 */


'use strict';
angular.module('wasdi.OpenSearchService', ['wasdi.OpenSearchService']).
service('OpenSearchService', ['$http',  'ConstantsService', function ($http, oConstantsService) {
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;
    //----------API------------------------
    this.API_GET_PROVIDERS = "/search/providers";
    this.API_GET_SEARCH = "/search/query?query=";
    this.API_GET_SEARCHLIST = "/search/querylist?";

    //-------------------------------------
    this.getApiProductsWithProviders = function (query) {
        return this.APIURL + this.API_GET_SEARCH + encodeURI(query);
    };

    this.getApiProductsListWithProviders = function (sProvidersInput) {
        return this.APIURL + this.API_GET_SEARCHLIST+"providers="+sProvidersInput;
    };

    this.getApiProductCountWithProviders = function(sQueryInput,sProvidersInput)
    {
        return this.APIURL + '/search/query/count?query=' + encodeURI(sQueryInput) +"&providers="+encodeURI(sProvidersInput);
    };

    this.getApiProductListCountWithProviders = function(sProvidersInput)
    {
        return this.APIURL + '/search/query/countlist?providers='+encodeURI(sProvidersInput);
    };

    this.getListOfProvider = function()
    {
        return this.m_oHttp.get(this.APIURL + this.API_GET_PROVIDERS);
    }


}]);

