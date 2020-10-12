/**
 * Created by p.campanella on 22/08/2014.
 */

angular.module('wasdi.sessionInjector', ['wasdi.ConstantsService']).factory('sessionInjector', ['ConstantsService', function (oConstantsService) {
    this.m_oConstantservice = oConstantsService;
    //this.m_oHttp = $http;
    var oController = this;
    var sessionInjector = {
        request: function (config) {
            if (utilsIsSubstring(config.url, oController.m_oConstantservice.getWmsUrlGeoserver()) == true) {//config.url == 'http://178.22.66.96:8080/geoserver/ows?service=WMS&request=GetCapabilities'
                return config;
            } else if (config.url.includes(oController.m_oConstantservice.getAUTHURL())) {
                return config;
            }

            let sSessionId = oConstantsService.getSessionId()

            config.headers['x-session-token'] = sSessionId;

            var asDecodedToken = null;

            try {
                asDecodedToken =jwt_decode(oConstantsService.getSessionId());

                if (asDecodedToken === null || !('exp' in asDecodedToken)) {
                    console.log('SessionInjector: could not access decoded token :(')
                    return config;
                }

                let iNow = Date.now()/1000
                let iExp = asDecodedToken['exp']
                let iSecondsRemaining = iExp - iNow

                    //check if token has expired, but use a two minutes buffer
                if (iSecondsRemaining > 10) {
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

                let sParams = 'client_id=wasdi_client' +
                    '&grant_type=refresh_token' +
                    '&refresh_token=' + oConstantsService.getUser().refreshToken;
                // blocking call to refresh
                var bAsync = false;
                var oRequest = new XMLHttpRequest();

                var oConstantServiceReference = oController.m_oConstantservice;

                oRequest.onload = function () {
                    var iStatus = oRequest.status; // HTTP response status, e.g., 200 for "200 OK"
                    var oData = JSON.parse(oRequest.responseText); // Returned data, e.g., an HTML document.

                    if(200===iStatus){
                        window.localStorage.access_token = oData['access_token'];
                        window.localStorage.refresh_token = oData['refresh_token'];
                        oConstantServiceReference.getUser().sessionId = oData['access_token'];
                        oConstantServiceReference.getUser().refreshToken = oData['refresh_token'];
                    } else {
                        console.log('SessionInjector: token refresh failed :(')
                    }
                };

                oRequest.open("POST", oController.m_oConstantservice.getAUTHURL() + '/protocol/openid-connect/token', bAsync);
                oRequest.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
                oRequest.send(sParams);

                // oController.m_oHttp.post(
                //     oController.m_oConstantservice.getAUTHURL() + '/protocol/openid-connect/token',
                //     sParams,
                //     {'headers': {'Content-Type': 'application/x-www-form-urlencoded'}}
                // ).success(function (data) {
                //     //update access token in constantsService
                //     window.localStorage.access_token = data['access_token'];
                //     window.localStorage.refresh_token = data['refresh_token'];
                //
                //
                //     oController.m_oConstantService.getUser().sessionId = data['access_token'];
                //     oController.m_oConstantService.getUser().refreshToken = data['refresh_token'];
                // }).error(function (err) {
                //     console.log('SessionInjector: token refresh failed :(')
                // });

                config.headers['x-session-token'] = oConstantsService.getSessionId();
                return config;

            }
            catch (oEx) {
                return  config;
            }
        }
    };
    return sessionInjector;
}]);
