/**
 * Created by a.corrado on 18/11/2016.
 */
var RootController = (function() {

    function RootController($scope, oConstantsService, oAuthService, $state) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oConstantsService = oConstantsService;
        this.m_oAuthService = oAuthService;
        this.m_oState=$state;
        this.m_oScope.m_oController=this;
        var oController = this;
        this.m_oAuthService.checkSession().success(function (data, status) {
            if (data == null || data == undefined || data == '')
            {
                oController.m_oConstantsService.logOut();
                oController.m_oState.go("home");
            }
            else
            {
                this.m_oUser = this.m_oConstantsService.getUser();
            }
        }).error(function (data,status) {
            //TODO use vex for error message
            alert('error in check id session');
        });

        //if user is logged
        //if(!utilsIsObjectNullOrUndefined(this.m_oConstantsService.getUser()))
        //    this.m_oUser = this.m_oConstantsService.getUser();
        //else
        //    this.m_oState.go("login");


    }

    RootController.prototype.isVisibleNavBar=function()
    {
        var sState=this.m_oState.current.name;
        switch(sState) {
            case "root.workspaces":
                return false;
                break;
            default: return true;
        }

        return true;
    }

    RootController.prototype.onClickProcess = function()
    {
        var oController=this;
        oController.m_bProcessMenuIsVisible = !oController.m_bProcessMenuIsVisible;
    }
    RootController.prototype.onClickLogOut = function()
    {
        var oController=this;
        oController.m_oConstantsService.logOut();
        oController.m_oState.go("home");
    }
    RootController.prototype.getActiveWorkspace = function()
    {
        var sWorkspace = this.m_oConstantsService.getActiveWorkspace();
        if(utilsIsObjectNullOrUndefined(sWorkspace))
            return "";
        else
        {
            if(utilsIsObjectNullOrUndefined(sWorkspace.workspaceId))
                return ""
            else
                return sWorkspace.workspaceId;
        }

    }
    RootController.$inject = [
        '$scope',
        'ConstantsService',
        'AuthService',
        '$state'

    ];

    return RootController;
}) ();
