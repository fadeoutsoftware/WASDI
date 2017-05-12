/**
 * Created by a.corrado on 21/04/2017.
 */
/**
 * Created by s.adamo on 09/02/2017.
 */


'use strict';
angular.module('wasdi.GetParametersOperationService', ['wasdi.GetParametersOperationService']).
service('GetParametersOperationService', ['$http',  'ConstantsService', function ($http, oConstantsService) {
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;
    this.URLGET = "/processing/parameters?sOperation="

    this.getParametersMultilooking = function () {
        //MULTILOOKING
        return this.operation("MULTILOOKING");
    };
    this.getparametersApplyOrbit = function () {

        return this.operation("APPLYORBIT");
    };
    this.getParametersNDVI = function () {

        return this.operation("NDVI");
    };
    this.getParametersRadiometricCalibration = function () {

        return this.operation("CALIBRATE");
    };
    this.getParametersRangeDopplerTerrainCorrection = function () {

        return this.operation("TERRAIN");
    };
    this.operation = function(sOperationInput)
    {
        if(utilsIsObjectNullOrUndefined(sOperationInput) == true || utilsIsStrNullOrEmpty(sOperationInput) == true )
            return null;
        return this.m_oHttp.get(this.APIURL + this.URLGET + sOperationInput);//orbit
    };

}]);

