/**
 * Created by p.campanella on 25/10/2016.
 */

'use strict';
angular.module('wasdi.ConfigurationService', []).
service('ConfigurationService', ['$q', '$http', function ($q, $http) {

    //Configuration search
    this.m_oSearchConfiguration = null;

    var loadConfiguration = function() {
        // the $http API is based on the deferred/promise APIs exposed by the $q service
        // so it returns a promise for us by default
        return $http.get('config/appconfig.json')
            .then(function(response) {
                if (response.data) {
                    return response.data;

                } else {
                    // invalid response
                    return $q.reject(response.data);
                }

            }, function(response) {
                // something went wrong
                return $q.reject(response.data);
            });
    };


    this.getConfiguration = function () {
        var oService = this;
        var oPromise = new Promise(function (resolve, reject) {

            if (oService.m_oConfiguration == null) {
                loadConfiguration().then(function (data) {
                    oService.m_oSearchConfiguration = data;
                    resolve(oService.m_oSearchConfiguration);
                });
            }
            else
                resolve(oService.m_oSearchConfiguration);
        });
        return oPromise;

    };

}]);
