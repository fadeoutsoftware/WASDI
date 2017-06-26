/**
 * Created by a.corrado on 23/06/2017.
 */
'use strict';

angular.module('wasdi.SatelliteService', ['wasdi.SatelliteService']).
service('SatelliteService', ['$http',  'ConstantsService', function ($http, oConstantsService) {
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;

    this.getTrackSatellite = function(sNameInput){

        return this.m_oHttp.get(this.APIURL + '/satellite/track/' + sNameInput);
    }
}]);

