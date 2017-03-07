/**
 * Created by a.corrado on 18/11/2016.
 */
var RootController = (function() {

    function RootController($scope, oConstantsService, oAuthService, $state, oProcessesLaunchedService, oWorkspaceService,$timeout,oModalService) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oConstantsService = oConstantsService;
        this.m_oAuthService = oAuthService;
        this.m_oState=$state;
        this.m_oProcessesLaunchedService = oProcessesLaunchedService;
        this.m_oWorkspaceService = oWorkspaceService;
        this.m_oScope.m_oController=this;
        this.m_aoProcessesRunning=[];
        this.m_aoLogProcesses = []
        this.m_iNumberOfProcesses = 0;
        this.m_oLastProcesses = null;
        this.m_bIsOpenNav = false;
        this.m_bIsOpenStatusBar = false; //processes bar
        this.m_oModalService = oModalService;
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
            //alert('error in check id session');
            oController.onClickLogOut();
            utilsVexDialogAlertTop('error in check id session');
        });

        //if user is logged
        if(!utilsIsObjectNullOrUndefined(this.m_oConstantsService.getUser()))
            this.m_oUser = this.m_oConstantsService.getUser();
        else
            this.m_oState.go("login");

        //this.m_oUser = this.m_oConstantsService.getUser();

        this.m_sWorkSpace = this.m_oConstantsService.getActiveWorkspace();

        if(utilsIsObjectNullOrUndefined(this.m_sWorkSpace) && utilsIsStrNullOrEmpty( this.m_sWorkSpace))
        {
            //if this.m_oState.params.workSpace in empty null or undefined create new workspace
            if(!(utilsIsObjectNullOrUndefined(this.m_oState.params.workSpace) && utilsIsStrNullOrEmpty(this.m_oState.params.workSpace)))
            {
                this.openWorkspace(this.m_oState.params.workSpace);
                //this.m_oActiveWorkspace = this.m_oConstantsService.getActiveWorkspace();
            }
            else
            {
                //TODO CREATE NEW WORKSPACE OR GO HOME
            }
        }

        this.m_aoProcessesRunning = this.m_oProcessesLaunchedService.getProcesses();

        /*TODO WATCH OR SIMILAR THINGS */
        /*when ProccesLaunchedservice reload the m_aoProcessesRunning rootController reload m_aoProcessesRunning */
        $scope.$on('m_aoProcessesRunning:updated', function(event,data) {
            // you could inspect the data to see
            if(data == true)
            {
                //$scope.m_oController.m_aoProcessesRunning = $scope.m_oController.m_oProcessesLaunchedService.getProcesses();
                //if(utilsIsObjectNullOrUndefined($scope.m_oController.m_aoProcessesRunning) == false)
                //    $scope.m_oController.m_iNumberOfProcesses = $scope.m_oController.m_aoProcessesRunning.length;

                var aoProcessesRunning = $scope.m_oController.m_oProcessesLaunchedService.getProcesses();
                var iNumberOfProcessesRunning = aoProcessesRunning.length;
                var aoOldProcessesRunning = $scope.m_oController.m_aoProcessesRunning;
                var iNumberOfOldProcessesRunning = aoOldProcessesRunning.length;
                //number of processes
                if(utilsIsObjectNullOrUndefined($scope.m_oController.m_aoProcessesRunning) == false)
                    $scope.m_oController.m_iNumberOfProcesses = iNumberOfProcessesRunning;

                //FIND LOG
                for( var  iIndexOldProcess= 0; iIndexOldProcess < iNumberOfOldProcessesRunning; iIndexOldProcess++)
                {
                    for( var iIndexNewProcess = 0; iIndexNewProcess < iNumberOfProcessesRunning; iIndexNewProcess++)
                    {
                        if(aoProcessesRunning[iIndexNewProcess].operationType == aoOldProcessesRunning[iIndexOldProcess].operationType &&
                            aoProcessesRunning[iIndexNewProcess].productName == aoOldProcessesRunning[iIndexOldProcess].productName)
                        {
                            break;
                        }
                    }
                    //if you find a log push it in list
                    if(!(iIndexNewProcess < iNumberOfProcessesRunning))
                    {
                        $scope.m_oController.m_aoLogProcesses.push(aoOldProcessesRunning[iIndexOldProcess]);
                        $scope.m_oController.m_aoProcessesRunning.splice(iIndexOldProcess,1);
                    }

                }
                //FIND LAST PROCESSES
                if(utilsIsObjectNullOrUndefined(aoProcessesRunning) == false)
                    $scope.m_oController.m_oLastProcesses = aoProcessesRunning[iNumberOfProcessesRunning-1];
                else
                    $scope.m_oController.m_oLastProcesses = null;


                //ADD ONLY NEW PROCESS IN m_oController.m_aoProcessesRunning
                for( var  iIndexNewProcess= 0; iIndexNewProcess < iNumberOfProcessesRunning; iIndexNewProcess++)
                {
                    for( var iIndexOldProcess = 0; iIndexOldProcess < iNumberOfOldProcessesRunning; iIndexOldProcess++)
                    {
                        if(aoProcessesRunning[iIndexNewProcess].operationType == aoOldProcessesRunning[iIndexOldProcess].operationType &&
                            aoProcessesRunning[iIndexNewProcess].productName == aoOldProcessesRunning[iIndexOldProcess].productName)
                        {
                            break;
                        }
                    }
                    //if the new processes there isn't in m_oController.m_aoProcessesRunning list add it!
                    if(!(iIndexOldProcess < iNumberOfOldProcessesRunning))
                    {

                        // add start time (useful if the page was reloaded)
                        var sStartTime = new Date(aoProcessesRunning[iIndexNewProcess].operationDate);//time by server
                        var test = new Date();//pick time
                        var result =  Math.abs(test-sStartTime);
                        var seconds = 0
                        seconds = Math.ceil(result / 1000);//approximate result
                        //var seconds = x % 60

                        if(utilsIsObjectNullOrUndefined(seconds) || seconds < 0)
                            seconds = 0;
                        var oDate = new Date(1970, 0, 1);
                        oDate.setSeconds(0 + seconds);
                        //add running time
                        aoProcessesRunning[iIndexNewProcess].timeRunning = oDate;

                        //it convert mb in gb
                        var nSize = aoProcessesRunning[iIndexNewProcess].fileSize / 1024;
                        nSize = Math.round(nSize * 100)/100;
                        aoProcessesRunning[iIndexNewProcess].fileSize = nSize;
                        $scope.m_oController.m_aoProcessesRunning.push(aoProcessesRunning[iIndexNewProcess])
                    }

                }

                //$scope.m_oController.m_aoProcessesRunning = aoProcessesRunning ;

            }

        });

        /* WATCH  ACTIVE WORKSPACE IN CONSTANT SERVICE
        * every time the workspace change, it clean the log list
        * */
        $scope.$watch('m_oController.m_oConstantsService.m_oActiveWorkspace', function(newValue, oldValue, scope) {
            //utilsVexDialogAlertTop("il watch funziona");
            $scope.m_oController.m_aoProcessesRunning = [];
            $scope.m_oController.m_aoLogProcesses = [];
        });
        /*COUNTDOWN METHODS*/

        //this.time = 0;

        $scope.onTimeout = function()
        {
            if(!utilsIsObjectNullOrUndefined($scope.m_oController.m_aoProcessesRunning) && $scope.m_oController.m_aoProcessesRunning.length != 0)
            {
                var iNumberOfProcesses = $scope.m_oController.m_aoProcessesRunning.length;

                for(var iIndexProcess = 0; iIndexProcess < iNumberOfProcesses;iIndexProcess++ )
                {

                    $scope.m_oController.m_aoProcessesRunning[iIndexProcess].timeRunning.setSeconds( $scope.m_oController.m_aoProcessesRunning[iIndexProcess].timeRunning.getSeconds() + 1) ;
                }
            }
            //$scope.m_oController.time++;
            mytimeout = $timeout($scope.onTimeout,1000);
        }

        var mytimeout = $timeout($scope.onTimeout,1000);
        //
        //$scope.stop = function(){
        //    $timeout.cancel(mytimeout);
        //}

    }

    /*********************************** METHODS **************************************/

    RootController.prototype.onClickProcess = function()
    {
        var oController=this;
        oController.m_bProcessMenuIsVisible = !oController.m_bProcessMenuIsVisible;
    };

    RootController.prototype.onClickLogOut = function()
    {
        var oController=this;

        this.m_oAuthService.logout().success(function (data, status) {
            oController.m_oConstantsService.logOut();
            oController.m_oState.go("home");
        }).error(function (data,status) {
            utilsVexDialogAlertTop("Error in Logout");
        });


    };

    RootController.prototype.getWorkspaceName = function()
    {
        var oWorkspace = this.m_oConstantsService.getActiveWorkspace();
        if(utilsIsObjectNullOrUndefined(oWorkspace))
            return "";
        else
        {
            if(utilsIsObjectNullOrUndefined(oWorkspace.name))
                return "";
            else
                return oWorkspace.name;
        }

    };

    RootController.prototype.noActiveLinkInNavBarCSS = function()
    {
        return ".not-active { pointer-events: none; cursor: default;}";
    };

    RootController.prototype.openWorkspace = function (sWorkspaceId) {

        var oController = this;

        this.m_oWorkspaceService.getWorkspaceEditorViewModel(sWorkspaceId).success(function (data, status) {
            if (data != null)
            {
                if (data != undefined)
                {
                    oController.m_oConstantsService.setActiveWorkspace(data);
                    oController.m_sWorkspace = oController.m_oConstantsService.getActiveWorkspace();
                    //oController.m_aoProcessesRunning = oController.m_oProcessesLaunchedService.getProcessesByLocalStorage(oController.m_sWorkspace.workspaceId, oController.m_oUser.userId);
                }
            }
        }).error(function (data,status) {
            utilsVexDialogAlertTop("Error in open WorkSPace by RootController.js");
        });
    };

    /***************************** IS VISIBLE HTML ELEMENT ******************************/
    RootController.prototype.isVisibleProcessesBar = function ()
    {
        var sState=this.m_oState.current.name;
        switch(sState) {
            case "root.workspaces":
                return false;
                break;
            default: return true;
        }

        return true;
    };

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
    };
    /*********************************************************************************/
    /* ***************** OPEN LINK *****************/
    RootController.prototype.openEditorPage = function () {

        var oController = this;
        var sWorkSpace = this.m_oConstantsService.getActiveWorkspace();
        oController.m_oState.go("root.editor", { workSpace : sWorkSpace.workspaceId });//use workSpace when reload editor page
    };


    RootController.prototype.openSearchorbit = function()
    {
        var oController = this;
        // if it publishing a band u can't go in import controller
        //if( this.m_oProcessesLaunchedService.thereAreSomePublishBandProcess() == false )
        //{
            var sWorkSpace = this.m_oConstantsService.getActiveWorkspace();
            oController.m_oState.go("root.searchorbit", { workSpace : sWorkSpace.workspaceId });//use workSpace when reload editor page
        //}


    };

    RootController.prototype.openImportPage = function () {

        var oController = this;
        // if it publishing a band u can't go in import controller
        //if( this.m_oProcessesLaunchedService.thereAreSomePublishBandProcess() == false )
        //{
            var sWorkSpace = this.m_oConstantsService.getActiveWorkspace();

            oController.m_oState.go("root.import", { workSpace : sWorkSpace.workspaceId });//use workSpace when reload editor page
        //}
        //TODO FEEDBACK IF U CAN'T CLICK ON IMPORT
    };

    RootController.prototype.activePageCss = function(oPage)
    {
        switch(oPage) {
            case "root.workspaces":
                if(oPage == this.m_oState.current.name )
                    return true;
                else
                    return false;
                break;
            case "root.editor":
                if(oPage == this.m_oState.current.name )
                    return true;
                else
                    return false;
                break;
            case "root.searchorbit":
                if(oPage == this.m_oState.current.name )
                    return true;
                else
                    return false;
                break;
            case "root.import":
                if(oPage == this.m_oState.current.name )
                    return true;
                else
                    return false;
                break;
            default:
                return false;
        }
        return false;
    };

    RootController.prototype.openNav = function() {
        document.getElementById("status-bar").style.height = "500px";
        this.m_bIsOpenStatusBar = !this.m_bIsOpenStatusBar;
        this.m_bIsOpenNav = true;

    };

    RootController.prototype.closeNav = function() {
        document.getElementById("status-bar").style.height = "4.5%";
        this.m_bIsOpenStatusBar = !this.m_bIsOpenStatusBar;
        this.m_bIsOpenNav = false;

    };

    RootController.prototype.UpdateWorkspace = function($event) {
        if ($event == null || $event.keyCode == 13) {
            var oWorkspace = this.m_oConstantsService.getActiveWorkspace();
            this.m_oWorkspaceService.UpdateWorkspace(oWorkspace).success(function (data) {

            }).error(function (error){

            });
        }

    };

    RootController.prototype.openSnake = function()
    {
        var oController = this
        this.m_oModalService.showModal({
            templateUrl: "dialogs/snake_dialog/SnakeDialog.html",
            controller: "RootController",

        }).then(function(modal) {
            modal.element.modal();
            modal.close.then(function(result) {
                oController.m_oScope.Result = result ;
            });
        });

        return true;
    }

    /*********************************************************************/
    RootController.$inject = [
        '$scope',
        'ConstantsService',
        'AuthService',
        '$state',
        'ProcessesLaunchedService',
        'WorkspaceService',
        '$timeout',
        'ModalService'

    ];

    return RootController;
}) ();
