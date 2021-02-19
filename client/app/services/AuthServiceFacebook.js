

'use strict';
angular.module('wasdi.AuthServiceFacebook', []).
service('AuthServiceFacebook', ['$rootScope', function ($rootScope) {
    this.m_oUser = {};

    this.LoginFACEBOOK = function () {
        var _self = this;
        //DEBUG BEGIN



        //DEBUG END
        //TODO LEAVE COMMENT
        FB.login(function (response) {

            if (response.status == 'connected')
            {
                _self.getUserInfoFACEBOOK();
            } else {
                console.warn("My warning: impossible do login");
            }

        });

    };

    this.getLoginStatusFACEBOOK = function (oCallback)
    {
        var _self = this;
        FB.getLoginStatus(function (response) {

            if (response.status === 'connected') {
                console.log('Logged in.');
            }
            else {
                _self.LoginFACEBOOK();
            }

        });

    };



    this.getUserInfoFACEBOOK = function () {

        var _self = this;

        FB.api('/me', function (res) {
            console.log("users data");
            _self.setUser(res);
            $rootScope.$apply(function () {
                _self.setUser(res);
                $rootScope.user = res;
            });
        });

    };

    this.getUserImageUrl = function ()
    {
        if ((angular.isUndefined($rootScope.user)) || ($rootScope.user === null))
            return "";

        return 'https://graph.facebook.com/' + $rootScope.user.id + '/picture?type=normal'
    }

    this.logoutFACEBOOK = function () {

        var _self = this;

        FB.logout(function (response) {
            _self.setUser({});
            console.log("Logout");
            $rootScope.$apply(function () {
                _self.setUser({});
                $rootScope.user = {};
            });
        });

    };

    this.setUser = function (oUserInput)
    {
        this.m_oUser = oUserInput;
    };

    this.getUser = function ()
    {
        return this.m_oUser;
    };

    this.isUserLogged = function ()
    {
        if ((angular.isUndefined($rootScope.user)) || ($rootScope.user === null) || (angular.equals($rootScope.user, {})))
            return false;
        return true;
    }
}]);
