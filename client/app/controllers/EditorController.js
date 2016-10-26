/**
 * Created by p.campanella on 24/10/2016.
 */


var EditorController = (function () {
    function EditorController($scope, $location, oConstantsService, oAuthService, oMapService, oFileBufferService) {
        this.m_oScope = $scope;
        this.m_oLocation = $location;
        this.m_oConstantsService = oConstantsService;
        this.m_oAuthService = oAuthService;
        this.m_oMapService = oMapService;
        this.m_oFileBufferService = oFileBufferService;

        this.m_sUserName = "";
        this.m_sUserPassword = "";

        this.m_oScope.m_oController = this;

        // TODO: Here a Workpsace is needed... if it is null create a new one..
        this.m_oActiveWorkspace = this.m_oConstantsService.getActiveWorkspace();

        this.m_sDownloadFilePath = "";


        oMapService.initMap('wasdiMap');
    }

    EditorController.prototype.moveTo = function (sPath) {
        this.m_oLocation.path(sPath);
    }

    EditorController.prototype.getUserName = function () {
        var oUser = this.m_oConstantsService.getUser();

        if (oUser != null) {
            if (oUser != undefined) {
                var sName = oUser.name;
                if (sName == null) sName = "";
                if (sName == undefined) sName = "";

                if (oUser.surname != null) {
                    if (oUser.surname != undefined) {
                        sName += " " + oUser.surname;

                        return sName;
                    }
                }
            }
        }

        return "";
    }

    EditorController.prototype.isUserLogged = function () {
        return this.m_oConstantsService.isUserLogged();
    }

    EditorController.prototype.addTestLayer = function () {
        //
        var oMap = this.m_oMapService.getMap();

        var wmsLayer = L.tileLayer.wms('http://localhost:8080/geoserver/ows?', {
            layers: 'wasdi:S1A_IW_GRDH_1SSV_20150213T095824_20150213T095849_004603_005AB7_5539'
        }).addTo(oMap);
    }

    EditorController.prototype.downloadEOImage = function (sUrl) {
        this.m_oFileBufferService.download(sUrl,this.m_oActiveWorkspace.workspaceId).success(function (data, status) {0
            alert('downloading');
        }).error(function (data, status) {
            alert('error');
        });
    }

    EditorController.prototype.publish = function (sUrl) {
        this.m_oFileBufferService.publish(sUrl,this.m_oActiveWorkspace.workspaceId).success(function (data, status) {0
            alert('publishing');
        }).error(function (data, status) {
            alert('error');
        });
    }

    EditorController.$inject = [
        '$scope',
        '$location',
        'ConstantsService',
        'AuthService',
        'MapService',
        'FileBufferService'
    ];

    return EditorController;
})();
