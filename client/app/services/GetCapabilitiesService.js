/**
 * Created by a.corrado on 09/03/2017.
 */

'use strict';
angular.module('wasdi.GetCapabilitiesService', ['wasdi.GetCapabilitiesService']).
service('GetCapabilitiesService', ['$http',  'ConstantsService', function ($http, oConstantsService) {
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;

    this.GetCapabilities = function (sServer) {

        return sServer + "service=WMS&request=GetCapabilities";
    }

}]);

