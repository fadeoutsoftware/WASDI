/**
 * Created by s.adamo on 23/01/2017.
 */
/**
 * Created by p.campanella on 18/11/2016.
 */

'use strict';
angular.module('wasdi.SearchOrbitService', ['wasdi.SearchOrbitService']).
service('SearchOrbitService', ['$http',  'ConstantsService', function ($http, oConstantsService) {
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;

    this.searchOrbit = function (oData) {

        return this.m_oHttp.post(this.APIURL + '/searchorbit/search', oData, {
            headers: { 'Content-Type': 'application/json;charset=utf-8'}
        });

    };

    this.getKML = function (sText,sFootPrint) {
        return this.m_oHttp.get(this.APIURL + '/searchorbit/getkmlsearchresults?text=' + sText + "&footPrint=" + sFootPrint);
    }

    this.getSatellitesResources = function (oData) {
        return this.m_oHttp.get(this.APIURL + '/searchorbit/getsatellitesresource');
    }

    this.getTrackSatellite = function(sNameInput){
        return this.m_oHttp.get(this.APIURL + '/searchorbit/track/' + sNameInput);
    }

    this.getUpdatedTrackSatellite = function(sNameInput){
        return this.m_oHttp.get(this.APIURL + '/searchorbit/updatetrack/' + sNameInput);
    }    

}]);

