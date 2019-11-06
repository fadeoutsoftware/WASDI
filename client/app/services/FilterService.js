

'use strict';
angular.module('wasdi.FilterService', ['wasdi.FilterService']).
service('FilterService', ['$http',  'ConstantsService', function ($http, oConstantsService)
{
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;
    /* --------------------------------- TODO REMOVE  FILTERSERVICE AND PUT THE APIs IN SNAPOPERATIONSERVICE (a.corrado 27/02/18)---------------------------------  */
    this.getFilters = function()
    {
        return this.m_oHttp.get(this.APIURL + "/processing/standardfilters");
    };
    // this.applyFilter = function(oFilter)
    // {
    //     return this.m_oHttp.post(this.APIURL + "/processing/applyfilter",oFilter);
    // };
/*
    this.getProductBand = function(oBody,sWorkspaceId){
        return this.m_oHttp.post(this.APIURL + '/processing/bandimage?workspace=' + sWorkspaceId, oBody,{responseType: 'arraybuffer'});
    };
*/
    this.getProductBand = function(oBody,sWorkspaceId, sUrl){

        var sAPIUrl = this.APIURL;

        if(typeof sUrl !== "undefined") {
            if ( sUrl !== null) {
                if (sUrl !== "") {
                    sAPIUrl = sUrl;
                }
            }
        }
        return this.m_oHttp.post(sAPIUrl + '/processing/bandimage?workspace=' + sWorkspaceId, oBody,{responseType: 'arraybuffer'});
    };

}]);

