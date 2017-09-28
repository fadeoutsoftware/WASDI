

'use strict';
angular.module('wasdi.CatalogService', ['wasdi.CatalogService']).
service('CatalogService', ['$http',  'ConstantsService', function ($http, oConstantsService) {
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;

    this.getCategories = function()
    {
        return this.m_oHttp.get(this.APIURL + "/catalog/categories");
    };
    this.getEntries = function(sFrom,sTo,sFreeText,sCategory)
    {
          return this.m_oHttp.get(this.APIURL + "/catalog/entries?from=" + sFrom + "&to=" + sTo + "&freetext=" + sFreeText + "&category=" + sCategory);
         // return this.m_oHttp.get(this.APIURL + "/catalog/entries?from=" + sFrom + "&to=" + sTo + "&category=" + sCategory);
         // return this.m_oHttp.get(this.APIURL + "/catalog/entries");
    };
}]);

