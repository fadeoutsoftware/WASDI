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

        return this.m_oHttp.get(this.APIURL + '/snap/terrain?sSourceProductName=' + sSourceProductName + '&sDestinationProductName=' + sDestinationProductName + '&sWorkspaceId=' + sWorkspaceId);
    }

}]);

