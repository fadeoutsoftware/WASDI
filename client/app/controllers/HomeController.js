/**
 * Created by p.campanella on 21/10/2016.
 */

var HomeController = (function() {
    function HomeController($scope, $location, oConstantsService, oAuthService) {
        this.m_oScope = $scope;
        this.m_oLocation  = $location;
        this.m_oConstantsService = oConstantsService;
        this.m_oAuthService = oAuthService;

        this.m_sUserName = "";
        this.m_sUserPassword = "";

        this.m_oScope.m_oController = this;
    }

    HomeController.prototype.moveTo = function (sPath) {
        this.m_oLocation.path(sPath);
    }

    HomeController.prototype.login = function () {
        var oLoginInfo = {};
        oLoginInfo.userId = this.m_sUserName;
        oLoginInfo.userPassword = this.m_sUserPassword;

        var oConstantsService = this.m_oConstantsService;

        this.m_oAuthService.login(oLoginInfo).success(function (data, status) {
            if (data != null)
            {
                if (data != undefined)
                {
                    if (data.userId != null)
                    {
                        if (data.userId != "")
                        {
                            oConstantsService.setUser(data);
                        }
                    }
                }
            }
        }).error(function (data,status) {
            alert('error');
        });
    }

    HomeController.prototype.getUserName = function () {
        var oUser = this.m_oConstantsService.getUser();

        if (oUser != null)
        {
            if (oUser != undefined)
            {
                var sName = oUser.name;
                if (sName == null) sName = "";
                if (sName == undefined) sName = "";

                if (oUser.surname != null)
                {
                    if (oUser.surname != undefined)
                    {
                        sName += " " + oUser.surname;

                        return sName;
                    }
                }
            }
        }

        return "";
    }

    HomeController.prototype.isUserLogged = function () {
        return this.m_oConstantsService.isUserLogged();
    }

    HomeController.$inject = [
        '$scope',
        '$location',
        'ConstantsService',
        'AuthService'
    ];

    return HomeController;
}) ();
