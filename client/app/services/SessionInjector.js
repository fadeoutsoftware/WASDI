/**
 * Created by p.campanella on 22/08/2014.
 */

angular.module('wasdi.sessionInjector', ['wasdi.ConstantsService']).
    factory('sessionInjector', ['ConstantsService', function(oConstantsService) {
    this.m_oConstantservice = oConstantsService;
    var oController = this;
    var sessionInjector = {
        request: function(config) {
            if ( utilsIsSubstring(config.url,oController.m_oConstantservice.getWmsUrlGeoserver()) == true)//config.url == 'http://178.22.66.96:8080/geoserver/ows?service=WMS&request=GetCapabilities'
                return config;
            config.headers['x-session-token'] = oConstantsService.getSessionId();
            return config;
        }
    };
    return sessionInjector;
}]);