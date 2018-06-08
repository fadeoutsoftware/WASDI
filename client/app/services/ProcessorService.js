/**
 * Created by a.corrado on 18/01/2017.
 */

'use strict';
angular.module('wasdi.ProcessorService', ['wasdi.ProcessorService']).
service('ProcessorService', ['ConstantsService','$rootScope','$http', function (oConstantsService,$rootScope,$http) {
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;

    this.getProcessorsList = function() {
        return this.m_oHttp.get(this.APIURL + '/processors/getdeployed');
    }

    this.runProcessor = function (sProcessorName, sJSON) {
        var sEncodedJSON = encodeURI(sJSON);
        return this.m_oHttp.get(this.APIURL + '/processors/run?name='+sProcessorName+'&encodedJson='+ sEncodedJSON);
    }


}]);
