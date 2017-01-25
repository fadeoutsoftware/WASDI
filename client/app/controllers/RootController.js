/**
 * Created by a.corrado on 18/11/2016.
 */
var RootController = (function() {

    function RootController($scope, oConstantsService, oAuthService, $state, oProcessesLaunchedService) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oConstantsService = oConstantsService;
        this.m_oAuthService = oAuthService;
        this.m_oState=$state;
        this.m_oProcessesLaunchedService = oProcessesLaunchedService;
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
                oController.m_oUser = oController.m_oConstantsService.getUser();
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


        this.m_sWorkSpace = this.m_oConstantsService.getActiveWorkspace();
        this.m_oUser = this.m_oConstantsService.getUser();
        if(!utilsIsObjectNullOrUndefined(this.m_sWorkSpace) && !utilsIsObjectNullOrUndefined(this.m_oUser))
            this.m_aoProcessesRunning = this.m_oProcessesLaunchedService.getProcessesByLocalStorage(this.m_sWorkSpace.workspaceId,this.m_oUser.userId);

        /*TODO WATCH OR SIMILAR THINGS */
        /*when ProccesLaunchedservice reload the m_aoProcessesRunning rootController reload m_aoProcessesRunning */
        $scope.$on('m_aoProcessesRunning:updated', function(event,data) {
            // you could inspect the data to see
            if(data == true)
            {
                $scope.m_oController.m_sWorkSpace = $scope.m_oController.m_oConstantsService.getActiveWorkspace();
                $scope.m_oController.m_oUser = $scope.m_oController.m_oConstantsService.getUser();
                if(!utilsIsObjectNullOrUndefined( $scope.m_oController.m_sWorkSpace) && !utilsIsObjectNullOrUndefined( $scope.m_oController.m_oUser))
                {
                    $scope.m_oController.m_aoProcessesRunning = $scope.m_oController.m_oProcessesLaunchedService.getProcessesByLocalStorage(
                        $scope.m_oController.m_sWorkSpace.workspaceId, $scope.m_oController.m_oUser.userId);
                }
                //$scope.m_oController.m_aoProcessesRunning = $scope.m_oController.m_oProcessesLaunchedService.getProcessesByLocalStorage();
            }
        });

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
            if(utilsIsObjectNullOrUndefined(sWorkspace.name))
                return ""
            else
                return sWorkspace.name;
        }

    }
    RootController.prototype.openImportPage = function () {

        var oController = this;
        // if it publishing a band u can't go in import controller
        if( this.m_oProcessesLaunchedService.thereAreSomePublishBandProcess() == false )
        {
            var sWorkSpace = this.m_oConstantsService.getActiveWorkspace();
            oController.m_oState.go("root.import", { workSpace : sWorkSpace.workspaceId });//use workSpace when reload editor page
        }
        //TODO FEEDBACK IF U CAN'T CLICK ON IMPORT
    }

    RootController.prototype.noActiveLinkInNavBarCSS = function()
    {
        return ".not-active { pointer-events: none; cursor: default;}";
    }
    RootController.prototype.openEditorPage = function () {

        var oController = this;
        var sWorkSpace = this.m_oConstantsService.getActiveWorkspace();
        oController.m_oState.go("root.editor", { workSpace : sWorkSpace.workspaceId });//use workSpace when reload editor page
    }
    RootController.$inject = [
        '$scope',
        'ConstantsService',
        'AuthService',
        '$state',
        'ProcessesLaunchedService'

    ];

    return RootController;
}) ();
