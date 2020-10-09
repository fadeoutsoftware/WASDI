/**
 * Created by p.campanella on 22/08/2014.
 */

angular.module('wasdi.sessionInjector', ['wasdi.ConstantsService']).factory('sessionInjector', ['ConstantsService', '$http', function (oConstantsService, $http) {
    this.m_oConstantservice = oConstantsService;
    this.m_oHttp = $http;
    var oController = this;
    var sessionInjector = {
        request: function (config) {
            if (utilsIsSubstring(config.url, oController.m_oConstantservice.getWmsUrlGeoserver()) == true) {//config.url == 'http://178.22.66.96:8080/geoserver/ows?service=WMS&request=GetCapabilities'
                return config;
            } else if (config.url.includes(oController.m_oConstantservice.getAUTHURL())) {
                return config;
            }

            config.headers['x-session-token'] = oConstantsService.getSessionId();

            var asDecodedToken = jwt_decode(oConstantsService.getSessionId());

            if (asDecodedToken === null || !('exp' in asDecodedToken)) {
                console.log('SessionInjector: could not access decoded token :(')
                return config;
            }
            //check if token has expired, but use a two minutes buffer
            if (Date.now() < asDecodedToken['exp'] - 120000) {
                //token is still valid, no need to refresh
                return config;
            }

            //safety checks here
            if (null === oConstantsService.getUser()) {
                console.log('SessionInjector: user is null :(')
                return config;
            }
            if (null == oConstantsService.getUser().refreshToken) {
                console.log('SessionInjector: refresh token is null :(')
                return config;
            }

            let sParams = 'client_id=' + m_sAuthClientId +
                '&grant_type=refresh_token' +
                '&refresh_token=' + oConstantsService.getUser().refreshToken;
            //todo blocking call to refresh
            oController.m_oHttp.post(
                oController.m_oConstantservice.getAUTHURL() + '/protocol/openid-connect/token',
                sParams,
                {'headers': {'Content-Type': 'application/x-www-form-urlencoded'}}
            ).success(function (data) {
                //update access token in constantsService
                window.localStorage.access_token = data['access_token'];
                window.localStorage.refresh_token = data['refresh_token'];


                oController.m_oConstantService.getUser().sessionId = data['access_token'];
                oController.m_oConstantService.getUser().refreshToken = data['refresh_token'];
            }).error(function (err) {
                console.log('SessionInjector: token refresh failed :(')
            });

            config.headers['x-session-token'] = oConstantsService.getSessionId();
            return config;
        }
    };
    return sessionInjector;
}]);
