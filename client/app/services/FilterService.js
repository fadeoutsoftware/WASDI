

'use strict';
angular.module('wasdi.FilterService', ['wasdi.FilterService']).
service('FilterService', ['$http',  'ConstantsService', function ($http, oConstantsService) {
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;

    this.getFilters = function()
    {
        return this.m_oHttp.get(this.APIURL + "/processing/standardfilters");
    };
    this.applyFilter = function(oFilter)
    {
        return this.m_oHttp.post(this.APIURL + "/processing/applyfilter",oFilter);
    };

    this.getProductBand = function(oBody,sWorkspaceId){
        return this.m_oHttp.post(this.APIURL + '/processing/bandimage?workspace=' + sWorkspaceId, oBody,{responseType: 'arraybuffer'});
    };

}]);

