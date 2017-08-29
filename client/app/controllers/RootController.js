/**
 * Created by a.corrado on 18/11/2016.
 */
var RootController = (function() {

    function RootController($scope, oConstantsService, oAuthService, $state, oProcessesLaunchedService, oWorkspaceService,$timeout,oModalService,oRabbitStompService) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oConstantsService = oConstantsService;
        this.m_oAuthService = oAuthService;
        this.m_oState=$state;
        this.m_oProcessesLaunchedService = oProcessesLaunchedService;
        this.m_oWorkspaceService = oWorkspaceService;
        this.m_oScope.m_oController=this;
        this.m_aoProcessesRunning=[];
        this.m_iNumberOfProcesses = 0;
        this.m_oLastProcesses = null;
        this.m_bIsOpenNav = false;
        this.m_bIsOpenStatusBar = false; //processes bar
        this.m_oModalService = oModalService;
        this.m_oRabbitStompService = oRabbitStompService;
        this.m_bIsEditModelWorkspaceNameActive = false;
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
            utilsVexDialogAlertTop('ERROR IN CHECK ID SESSION');
            // oController.m_oConstantsService.logOut();
            // oController.m_oState.go("home");
            // oController.m_oState.go("home");
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

        /*when ProccesLaunchedservice reload the m_aoProcessesRunning rootController reload m_aoProcessesRunning */
        $scope.$on('m_aoProcessesRunning:updated', function(event,data) {
            // you could inspect the data to see
            if(data == true)
            {
                var aoProcessesRunning = $scope.m_oController.m_oProcessesLaunchedService.getProcesses();
                var iTotalProcessesNumber = aoProcessesRunning.length;

                // get the number of active processes
                if(utilsIsObjectNullOrUndefined(aoProcessesRunning) == false) {

                    var iActiveCount = 0;

                    aoProcessesRunning.forEach(function (oProcess) {
                        if (oProcess.status == "RUNNING" || oProcess.status == "CREATED") {
                            iActiveCount++;
                        }
                    });

                    $scope.m_oController.m_iNumberOfProcesses = iActiveCount;
                }

                $scope.m_oController.m_oLastProcesses = null;

                //FIND LAST PROCESSES
                if(utilsIsObjectNullOrUndefined(aoProcessesRunning) == false) {

                    if (aoProcessesRunning.length>0) {
                        if (aoProcessesRunning[iTotalProcessesNumber-1].status === "CREATED" || aoProcessesRunning[iTotalProcessesNumber-1].status === "RUNNING") {
                            $scope.m_oController.m_oLastProcesses = aoProcessesRunning[iTotalProcessesNumber-1];
                        }
                    }
                }

                // Initialize the time counter for new processes
                for( var  iIndexNewProcess= 0; iIndexNewProcess < iTotalProcessesNumber; iIndexNewProcess++)
                {
                    if (aoProcessesRunning[iIndexNewProcess].status == "CREATED" || aoProcessesRunning[iIndexNewProcess].status == "RUNNING" )
                    {
                        if (utilsIsObjectNullOrUndefined(aoProcessesRunning[iIndexNewProcess].timeRunning)) {
                            // add start time (useful if the page was reloaded)

                            //time by server
                            var oStartTime = new Date(aoProcessesRunning[iIndexNewProcess].operationDate);
                            //pick time
                            var oNow = new Date();
                            var result =  Math.abs(oNow-oStartTime);
                            //approximate result
                            var seconds = Math.ceil(result / 1000);

                            if(utilsIsObjectNullOrUndefined(seconds) || seconds < 0) seconds = 0;

                            var oDate = new Date(1970, 0, 1);
                            oDate.setSeconds(0 + seconds);
                            //add running time
                            aoProcessesRunning[iIndexNewProcess].timeRunning = oDate;
                        }
                    }
                    else {
                        if (utilsIsObjectNullOrUndefined(aoProcessesRunning[iIndexNewProcess].timeRunning)) {
                            aoProcessesRunning[iIndexNewProcess].timeRunning = 0;

                            //time by server
                            var oStartTime = new Date(aoProcessesRunning[iIndexNewProcess].operationDate);
                            var oEndTime = new Date(aoProcessesRunning[iIndexNewProcess].operationEndDate);
                            //pick time
                            var result =  Math.abs(oEndTime-oStartTime);
                            //approximate result
                            var seconds = Math.ceil(result / 1000);

                            if(utilsIsObjectNullOrUndefined(seconds) || seconds < 0) seconds = 0;

                            var oDate = new Date(1970, 0, 1);
                            oDate.setSeconds(0 + seconds);
                            //add running time
                            aoProcessesRunning[iIndexNewProcess].timeRunning = oDate;
                        }
                    }
                }

                $scope.m_oController.m_aoProcessesRunning = aoProcessesRunning;
            }

        });

        /* WATCH  ACTIVE WORKSPACE IN CONSTANT SERVICE
        * every time the workspace change, it clean the log list &&
        * set m_bIsEditModelWorkspaceNameActive = false
        * */
        $scope.$watch('m_oController.m_oConstantsService.m_oActiveWorkspace', function(newValue, oldValue, scope) {
            //utilsVexDialogAlertTop("il watch funziona");
            $scope.m_oController.m_aoProcessesRunning = [];
            $scope.m_oController.m_bIsEditModelWorkspaceNameActive = false;
        });

        /*COUNTDOWN METHOD*/

        //this.time = 0;

        $scope.onTimeout = function()
        {
            if(!utilsIsObjectNullOrUndefined($scope.m_oController.m_aoProcessesRunning) && $scope.m_oController.m_aoProcessesRunning.length != 0)
            {
                var iNumberOfProcesses = $scope.m_oController.m_aoProcessesRunning.length;

                for(var iIndexProcess = 0; iIndexProcess < iNumberOfProcesses;iIndexProcess++ )
                {

                    if ($scope.m_oController.m_aoProcessesRunning[iIndexProcess].status==="RUNNING") {
                        $scope.m_oController.m_aoProcessesRunning[iIndexProcess].timeRunning.setSeconds( $scope.m_oController.m_aoProcessesRunning[iIndexProcess].timeRunning.getSeconds() + 1) ;
                    }

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
            utilsVexDialogAlertTop("ERROR IN LOGOUT");
            // oController.m_oState.go("home");
        });


    };

    RootController.prototype.getWorkspaceName = function()
    {
        var oWorkspace = this.m_oConstantsService.getActiveWorkspace();

        if(utilsIsObjectNullOrUndefined(oWorkspace)) return "";
        else
        {
            if(utilsIsObjectNullOrUndefined(oWorkspace.name)) return "";
            else return oWorkspace.name;
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
                    oController.m_sWorkSpace = oController.m_oConstantsService.getActiveWorkspace();
                    //oController.m_aoProcessesRunning = oController.m_oProcessesLaunchedService.getProcessesByLocalStorage(oController.m_sWorkspace.workspaceId, oController.m_oUser.userId);
                }
            }
        }).error(function (data,status) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN OPEN WORKSPACE");
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

    RootController.prototype.isWorkspacesPageOpen = function(){
        var sState=this.m_oState.current.name;
        if(sState === "root.workspaces")
            return true;
        return false;
    }

    RootController.prototype.cursorCss = function(){
        var sState=this.m_oState.current.name;
        switch(sState) {
            case "root.workspaces":
                return "no-drop";
                break;
            default: return "auto";
        }
    }
    /*********************************************************************************/
    /* ***************** OPEN LINK *****************/
    RootController.prototype.openEditorPage = function () {
        if(this.isWorkspacesPageOpen() === true)
            return false;

        var oController = this;
        var sWorkSpace = this.m_oConstantsService.getActiveWorkspace();
        oController.m_oState.go("root.editor", { workSpace : sWorkSpace.workspaceId });//use workSpace when reload editor page
    };


    RootController.prototype.openSearchorbit = function()
    {
        if(this.isWorkspacesPageOpen() === true) return false;

        //var oController = this;
        //var sWorkSpace = this.m_oConstantsService.getActiveWorkspace();
        //use workSpace when reload editor page
        //oController.m_oState.go("root.searchorbit", { workSpace : sWorkSpace.workspaceId });
        this.m_oState.go("root.searchorbit", { });
    };

    RootController.prototype.openImportPage = function () {
        if(this.isWorkspacesPageOpen() === true)
            return false;

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

    /*
    RootController.prototype.UpdateWorkspace = function($event) {
        if ( ($event == null || $event.keyCode === 13) && this.m_bIsEditModelWorkspaceNameActive === true) {

            //change color of textbox and pencil
            if(utilsIsObjectNullOrUndefined($event )=== true )
                this.editModelWorkspaceName();

            var oWorkspace = this.m_oConstantsService.getActiveWorkspace();
            this.m_oWorkspaceService.UpdateWorkspace(oWorkspace).success(function (data) {

            }).error(function (error){

            });


            if( $event != null && $event.keyCode === 13)
            {
                //disable work space name active
                this.m_bIsEditModelWorkspaceNameActive = false;
            }
        }
        else{
            //change color of textbox and pencil
            if(this.m_bIsEditModelWorkspaceNameActive === false)
                this.editModelWorkspaceName();
        }

    };*/

    RootController.prototype.openSnake = function()
    {
        var oController = this;
        console.log("miao");
        this.m_oModalService.showModal({
            // templateUrl: "dialogs/snake_dialog/SnakeDialog.html",
            templateUrl: "dialogs/snake_dialog/SnakeDialogV2.html",
            controller: "SnakeController"
        }).then(function(modal) {
            modal.element.modal();

            modal.close.then(function(result) {
                oController.m_oScope.Result = result ;
            });
        });

        return true;
    }

    RootController.prototype.deleteProcess = function(oProcessInput)
    {
        var oController = this;
        var oWorkspace = this.m_oConstantsService.getActiveWorkspace();
        this.m_oModalService.showModal({
            templateUrl: "dialogs/delete_process/DeleteProcessDialog.html",
            controller: "DeleteProcessController",


        }).then(function(modal) {
            modal.element.modal();
            modal.close.then(function(result) {
                oController.m_oScope.Result = result ;

                if(result === 'delete')
                    oController.m_oProcessesLaunchedService.removeProcessInServer(oProcessInput.processObjId,oWorkspace.workspaceId,oProcessInput)
            });
        });

        return true;
    };

    /**
     * openProcessLogsDialog
     * @returns {boolean}
     */
    RootController.prototype.openProcessLogsDialog = function()
    {
        var oController = this;
        // var oWorkspace = this.m_oConstantsService.getActiveWorkspace();
        this.m_oModalService.showModal({
            templateUrl: "dialogs/processes_logs/ProcessesLogsDialog.html",
            controller: "ProcessesLogsController",
            // inputs: {
            //     extras: {
            //         workspaceId:
            //     }
            // }

        }).then(function(modal) {
            modal.element.modal();
            modal.close.then(function(result) {
                oController.m_oScope.Result = result ;


            });
        });

        return true;
    };

    RootController.prototype.editModelWorkspaceName = function(){

        this.m_bIsEditModelWorkspaceNameActive = !this.m_bIsEditModelWorkspaceNameActive;
    };

    RootController.prototype.editModelWorkspaceNameSetTrue = function(){

        var oController = this;
        if  (utilsIsObjectNullOrUndefined(oController.m_oConstantsService.getActiveWorkspace())) return;

        var oCallback = function (value) {

            if (utilsIsObjectNullOrUndefined(value)) return;

            var oWorkspace = oController.m_oConstantsService.getActiveWorkspace();
            oWorkspace.name = value;

            oController.m_oWorkspaceService.UpdateWorkspace(oWorkspace).success(function (data) {
                oWorkspace.name = data.name;
                oController.m_bIsEditModelWorkspaceNameActive = false;
            });
        };
        utilsVexPrompt("Insert Workspace Name:<br>", oController.m_oConstantsService.getActiveWorkspace().name, oCallback);

        this.m_bIsEditModelWorkspaceNameActive =true;
    };


    /*********************************************************************/
    RootController.$inject = [
        '$scope',
        'ConstantsService',
        'AuthService',
        '$state',
        'ProcessesLaunchedService',
        'WorkspaceService',
        '$timeout',
        'ModalService',
        'RabbitStompService',


    ];

    return RootController;
}) ();
