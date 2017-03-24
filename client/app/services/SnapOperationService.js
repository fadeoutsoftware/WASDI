/**
 * Created by s.adamo on 09/02/2017.
 */
/**
 * Created by s.adamo on 23/01/2017.
 */
/**
 * Created by p.campanella on 18/11/2016.
 */

'use strict';
angular.module('wasdi.SnapOperationService', ['wasdi.SnapOperationService']).
service('SnapOperationService', ['$http',  'ConstantsService', function ($http, oConstantsService) {
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;

    this.TerrainCorrection = function (sSourceProductName, sDestinationProductName, sWorkspaceId) {

        return this.Operation("terrain", sSourceProductName, sDestinationProductName, sWorkspaceId);
    };

    this.ApplyOrbit = function (sSourceProductName, sDestinationProductName, sWorkspaceId) {

        return this.Operation("orbit", sSourceProductName, sDestinationProductName, sWorkspaceId);
    };
    this.Calibrate = function (sSourceProductName, sDestinationProductName, sWorkspaceId) {

        return this.Operation("calibrate", sSourceProductName, sDestinationProductName, sWorkspaceId);
    };
    this.Multilooking = function (sSourceProductName, sDestinationProductName, sWorkspaceId) {

        return this.Operation("multilooking", sSourceProductName, sDestinationProductName, sWorkspaceId);
    };
    this.NDVI = function (sSourceProductName, sDestinationProductName, sWorkspaceId) {

        return this.Operation("ndvi", sSourceProductName, sDestinationProductName, sWorkspaceId);
    };

    this.Operation = function(sOperation, sSourceProductName, sDestinationProductName, sWorkspaceId)
    {
        var sUrl = this.APIURL + '/snap/{sOperation}?sSourceProductName=' + sSourceProductName + '&sDestinationProductName=' + sDestinationProductName + '&sWorkspaceId=' + sWorkspaceId;
        sUrl = sUrl.replace("{sOperation}", sOperation);
        return this.m_oHttp.get(sUrl);
    }


}]);

