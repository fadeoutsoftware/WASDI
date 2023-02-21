/**
 * Created by a.corrado on 18/11/2016.
 */
var RootController = (function() {

    RootController.BROADCAST_MSG_OPEN_LOGS_DIALOG_PROCESS_ID = "RootController.openLogsDialogProcessId";

    function RootController($rootScope, $scope, oConstantsService, oAuthService, $state, oProcessWorkspaceService, oWorkspaceService,
                            $timeout,oModalService,oRabbitStompService, $window, oTranslate) {
                        
        this.m_oRootScope = $rootScope;
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oConstantsService = oConstantsService;
        this.m_oAuthService = oAuthService;
        this.m_oState=$state;
        this.m_oProcessWorkspaceService = oProcessWorkspaceService;
        this.m_oWorkspaceService = oWorkspaceService;
        this.m_oScope.m_oController=this;
        this.m_aoProcessesRunning=[];
        this.m_aoWorkspaces=[];
        this.m_iNumberOfProcesses = 0;
        this.m_iWaitingProcesses = 0;
        this.m_oLastProcesses = null;
        this.m_bIsOpenNav = false;
        this.m_bIsOpenStatusBar = false; //processes bar
        this.m_oModalService = oModalService;
        this.m_oRabbitStompService = oRabbitStompService;
        this.m_bIsEditModelWorkspaceNameActive = false;
        this.m_isRabbitConnected = true;
        this.m_oSummary = {};
        this.m_oWindow = $window;
        this.m_oTranslate = oTranslate;
        var oController = this;


        this.updateRabbitConnectionState = function(forceNotification)
        {
            if( forceNotification == null || forceNotification == undefined){
                forceNotification = false;
            }
            var connectionState = oRabbitStompService.getConnectionState();
            if( connectionState == 1) {
                oController.m_isRabbitConnected = true;
            }
            else
            {
                oController.m_isRabbitConnected = false;
            }
        };

        // Subscribe to 'rabbit service connection changes'
        var _this = this;

        var msgHlp = MessageHelper.getInstanceWithAnyScope($scope);

        msgHlp.subscribeToRabbitConnectionStateChange(function(event, args) {
            _this.updateRabbitConnectionState();
        });

        // then immediatly check rabbit connection state
        this.updateRabbitConnectionState(true);

        //if user is logged
        if(!utilsIsObjectNullOrUndefined(this.m_oConstantsService.getUser())) {
            try {
                this.m_oUser = this.m_oConstantsService.getUser();
            } catch (oE) {
                console.log("RootController: could not retrieve username: " + oE);
            }
        }
        //FIXME: state "login" not found
        //else this.m_oState.go("login");

        this.m_sWorkSpace = this.m_oConstantsService.getActiveWorkspace();

        if(utilsIsObjectNullOrUndefined(this.m_sWorkSpace) && utilsIsStrNullOrEmpty( this.m_sWorkSpace))
        {
            //if this.m_oState.params.workSpace in empty null or undefined create new workspace
            if(!(utilsIsObjectNullOrUndefined(this.m_oState.params.workSpace) && utilsIsStrNullOrEmpty(this.m_oState.params.workSpace)))
            {
                this.openWorkspace(this.m_oState.params.workSpace);
            }
            else
            {
                //TODO CREATE NEW WORKSPACE OR GO HOME
            }
        }

        this.m_aoProcessesRunning = this.m_oProcessWorkspaceService.getProcesses();

        /*when ProcessLaunchedservice reload the m_aoProcessesRunning rootController reload m_aoProcessesRunning */
        $scope.$on('m_aoProcessesRunning:updated', function(event,data) {
            // you could inspect the data to see
            if(data == true)
            {
                var aoProcessesRunning = $scope.m_oController.m_oProcessWorkspaceService.getProcesses();
                if(utilsIsObjectNullOrUndefined(aoProcessesRunning) == true) return;

                // Set the number of running processes
                $scope.m_oController.getSummary();

                $scope.m_oController.m_oLastProcesses = $scope.m_oController.findLastProcess(aoProcessesRunning);

                $scope.m_oController.m_aoProcessesRunning = $scope.m_oController.initializeTimeCounter(aoProcessesRunning);

            }
        });



        $scope.$on(RootController.BROADCAST_MSG_OPEN_LOGS_DIALOG_PROCESS_ID, function(event,data) {

            let intervalId = setInterval(function(){
                if( $scope.m_oController.openLogsDialogByProcessId(data.processId) == true){
                    clearInterval(intervalId);
                }
            }, 1000);
        });



        /* WATCH  ACTIVE WORKSPACE IN CONSTANT SERVICE
        * every time the workspace change, it clean the log list &&
        * set m_bIsEditModelWorkspaceNameActive = false
        * */
        $scope.$watch('m_oController.m_oConstantsService.m_oActiveWorkspace', function(newValue, oldValue, scope) {
            $scope.m_oController.m_aoProcessesRunning = [];
            $scope.m_oController.m_bIsEditModelWorkspaceNameActive = false;
            if(utilsIsObjectNullOrUndefined(newValue) === false)
            {
                let sWorkspaceName = newValue.workspaceName;

                if (utilsIsStrNullOrEmpty(sWorkspaceName)) {
                    sWorkspaceName = newValue.name;
                }

                if (!utilsIsStrNullOrEmpty(sWorkspaceName) && sWorkspaceName.includes("Untitled Workspace"))
                {
                    $scope.m_oController.getWorkspacesInfo();
                    $scope.m_oController.editModelWorkspaceNameSetTrue();
                }
            }
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
                    if ($scope.m_oController.m_aoProcessesRunning[iIndexProcess].status==="RUNNING" ||
                        $scope.m_oController.m_aoProcessesRunning[iIndexProcess].status==="WAITING" ||
                        $scope.m_oController.m_aoProcessesRunning[iIndexProcess].status==="READY") {
                        $scope.m_oController.m_aoProcessesRunning[iIndexProcess].timeRunning.setSeconds( $scope.m_oController.m_aoProcessesRunning[iIndexProcess].timeRunning.getSeconds() + 1) ;
                    }
                }
            }
            //$scope.m_oController.time++;
            oTimerTimeout = $timeout($scope.onTimeout, 1000)

        };

        this.isProcesssBarOpened = function()
        {
            return this.m_bIsOpenStatusBar;
        };

        var oTimerTimeout = $timeout($scope.onTimeout, 1000)

        this.getWorkspacesInfo();
        this.initTooltips();
        this.getSummary()
    }

    /*********************************** METHODS **************************************/

    RootController.prototype.openPayloadDialog = function (oProcess){

        var oController = this;

        if(utilsIsObjectNullOrUndefined(oProcess) === true)
        {
            return false;
        }
        oController.m_oModalService.showModal({
            templateUrl: "dialogs/payload_dialog/PayloadDialog.html",
            controller: "PayloadDialogController",
            inputs: {
                extras: {
                    process:oProcess,
                }
            }
        }).then(function (modal) {
            modal.element.modal();
            modal.close.then(function(oResult){

            });
        });
    };

    RootController.prototype.initializeTimeCounter = function(aoProcessesRunning)
    {
        if(utilsIsObjectNullOrUndefined(aoProcessesRunning) === true )
        {
            return null;
        }
        var iTotalProcessesNumber = aoProcessesRunning.length;

        for( var  iIndexNewProcess= 0; iIndexNewProcess  < iTotalProcessesNumber; iIndexNewProcess++)
        {
            //time by server
            var oStartTime;
            var oNow;
            var result;
            var seconds;
            var oDate;
            if (aoProcessesRunning[iIndexNewProcess].status === "RUNNING" || aoProcessesRunning[iIndexNewProcess].status === "WAITING" || aoProcessesRunning[iIndexNewProcess].status === "READY")
            {
                if (!utilsIsObjectNullOrUndefined(aoProcessesRunning[iIndexNewProcess].operationStartDate)) {
                    if (!aoProcessesRunning[iIndexNewProcess].operationStartDate.endsWith(" Z")) {
                        aoProcessesRunning[iIndexNewProcess].operationStartDate += " Z";
                    }
                }

                if (!utilsIsObjectNullOrUndefined(aoProcessesRunning[iIndexNewProcess].operationEndDate)) {
                    if (!aoProcessesRunning[iIndexNewProcess].operationEndDate.endsWith(" Z")) {
                        aoProcessesRunning[iIndexNewProcess].operationEndDate += " Z";
                    }
                }

                if (utilsIsObjectNullOrUndefined(aoProcessesRunning[iIndexNewProcess].timeRunning)) {
                    // add start time (useful if the page was reloaded)

                    //time by server
                    oStartTime = new Date(aoProcessesRunning[iIndexNewProcess].operationStartDate);
                    //pick time
                    oNow = new Date();
                    result =  Math.abs(oNow-oStartTime);
                    //approximate result
                    seconds = Math.ceil(result / 1000);

                    if(utilsIsObjectNullOrUndefined(seconds) || seconds < 0 || utilsIsANumber(seconds)=== false)
                    {
                        seconds = 0;
                    }

                    oDate = new Date(1970, 0, 1);
                    oDate.setSeconds(0 + seconds);
                    //add running time
                    aoProcessesRunning[iIndexNewProcess].timeRunning = oDate;
                }
            }
            else {
                if (utilsIsObjectNullOrUndefined(aoProcessesRunning[iIndexNewProcess].timeRunning)) {
                    aoProcessesRunning[iIndexNewProcess].timeRunning = 0;

                    //time by server
                    oStartTime = new Date(aoProcessesRunning[iIndexNewProcess].operationStartDate);
                    var oEndTime = new Date(aoProcessesRunning[iIndexNewProcess].operationEndDate);
                    //pick time
                    result =  Math.abs(oEndTime-oStartTime);
                    //approximate result
                    seconds = Math.ceil(result / 1000);

                    if(utilsIsObjectNullOrUndefined(seconds) || seconds < 0  || utilsIsANumber(seconds)=== false)
                    {
                        seconds = 0;
                    }

                    oDate = new Date(1970, 0, 1);
                    oDate.setSeconds(0 + seconds);
                    //add running time
                    aoProcessesRunning[iIndexNewProcess].timeRunning = oDate;
                }
            }
        }

        return aoProcessesRunning;
    };

    RootController.prototype.findLastProcess = function(aoProcessesRunning)
    {
        if(utilsIsObjectNullOrUndefined(aoProcessesRunning) === true )
        {
            return null;
        }

        var oLastProcessRunning = null;
        var iTotalProcessesNumber = aoProcessesRunning.length;
        // Search the last one that is in running state
        for( var  iIndexNewProcess= 0; iIndexNewProcess < iTotalProcessesNumber; iIndexNewProcess++) {
            if (aoProcessesRunning[iIndexNewProcess].status === "RUNNING"||
                aoProcessesRunning[iIndexNewProcess].status === "WAITING"||
                aoProcessesRunning[iIndexNewProcess].status === "READY") {
                oLastProcessRunning = aoProcessesRunning[iIndexNewProcess];
            }
        }
        return oLastProcessRunning;
    };

    RootController.prototype.getSummary = function()
    {
        var sMessage = this.m_oTranslate.instant("MSG_SUMMARY_ERROR");

        var oController = this;
        this.m_oProcessWorkspaceService.getSummary()
            .then(function (data, status) {
                if(utilsIsObjectNullOrUndefined(data.data) === true || data.data.BoolValue === false)
                {
                    utilsVexDialogAlertTop(sMessage);
                }
                else
                {
                    oController.m_oSummary = data.data;
                    oController.m_iNumberOfProcesses = data.data.userProcessRunning;
                    oController.m_iWaitingProcesses = data.data.userProcessWaiting;
                }

            },(function (data,status) {
                utilsVexDialogAlertTop(sMessage);

            }));
    };

    RootController.prototype.getConnectionStatusForTooltip = function()
    {
        //return Math.random() * 1000;
        if( this.isRabbitConnected() == true){ return "Connected"}
        return "Disconnected";
    }

    RootController.prototype.initTooltips = function()
    {
        var _this = this;
        var tooltipsList = $('[data-toggle="tooltip"]');
        if(tooltipsList.length == 0)
        {
            setTimeout(function () {
                _this.initTooltips();
            }, 100);
            return;
        }

        tooltipsList.tooltip();
    }


    RootController.prototype.openLogoutModal = function()
    {
        $('#logoutModal').modal('show');
    }
    RootController.prototype.closeLogoutModal = function()
    {
        $('#logoutModal').modal('hide');
    }

    RootController.prototype.isRabbitConnected = function()
    {
        return this.m_isRabbitConnected;
    }

    RootController.prototype.onClickProcess = function()
    {
        var oController=this;
        oController.m_bProcessMenuIsVisible = !oController.m_bProcessMenuIsVisible;
    };

    RootController.prototype.onClickLogOut = function()
    {
        var _this = this;

        try {
            _this.m_oConstantsService.setActiveWorkspace(null);

            _this.m_oConstantsService.logOut();
            var oLogOutOptions = { redirectUri : this.m_oConstantsService.BASEURL}
            oKeycloak.logout(oLogOutOptions);
            //_this.m_oState.go("home");
        }catch(e)
        {
        console.log("RootController - Exception " + e);
        }
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

        var sMessage = this.m_oTranslate.instant("MSG_ERROR_READING_WS");

        this.m_oWorkspaceService.getWorkspaceEditorViewModel(sWorkspaceId).then(function (data, status) {
            if (data != null)
            {
                if (data != undefined)
                {
                    oController.m_oConstantsService.setActiveWorkspace(data.data);
                    oController.m_sWorkSpace = oController.m_oConstantsService.getActiveWorkspace();
                }
            }
        },(function (data,status) {
            utilsVexDialogAlertTop(sMessage);
        }));
    };

    /***************************** IS VISIBLE HTML ELEMENT ******************************/

    RootController.prototype.isProcessesBarVisible = function ()
    {
        var sState = this.m_oState.current.name;

        // NOTE: a bug is reported using 'ng-class' in combination with 'ui-view'
        // (https://github.com/angular-ui/ui-router/issues/866)
        // Try to solve with jQuery workaround

        if( sState == "root.workspaces" || sState == "root.marketplace"  || sState == "root.appdetails" || sState == "root.appui")
        {
            $("#main").removeClass("has-processes-bar");
            return false;
        }

        $("#main").addClass("has-processes-bar");
        return true;
    };

    RootController.prototype.isVisibleNavBar=function()
    {
        var sState=this.m_oState.current.name;
        if (sState=="root.workspaces") {
            return false;
        }else{
            return true;
        }

    };

    RootController.prototype.disableEditorButton = function(){
        return (utilsIsObjectNullOrUndefined(this.m_oConstantsService.getActiveWorkspace()));
    };

    RootController.prototype.hideWorkspaceName = function(){
        var sState = this.m_oState.current.name;
        if(sState !== "root.editor") return true;
        return false;
    };


    RootController.prototype.cursorCss = function(){
        var oWorkspace = this.m_oConstantsService.getActiveWorkspace();
        if(utilsIsObjectNullOrUndefined(oWorkspace) === true || utilsIsObjectNullOrUndefined(oWorkspace.workspaceId)=== true )
            return "no-drop";
        else
            return "pointer";
    };

    /*********************************************************************************/
    /* ***************** OPEN LINK *****************/
    RootController.prototype.openEditorPage = function () {
        var oWorkspace = this.m_oConstantsService.getActiveWorkspace();
        if(utilsIsObjectNullOrUndefined(oWorkspace.workspaceId)) return false;
        var oController = this;
        var sWorkSpace = this.m_oConstantsService.getActiveWorkspace();
        //use workSpace when reload editor page
        oController.m_oState.go("root.editor", { workSpace : sWorkSpace.workspaceId });
    };

    RootController.prototype.openSearchorbit = function()
    {
        this.m_oRootScope.title = null;
        this.m_oState.go("root.searchorbit", { });
    };

    RootController.prototype.openMarketPlace = function()
    {
        this.m_oRootScope.title = null;
        this.m_oState.go("root.marketplace", { });
    };

    RootController.prototype.openWorkspaces = function()
    {
        this.m_oRootScope.title = null;
        this.m_oState.go("root.workspaces", { });
    };

    RootController.prototype.openAdminDashboard = function()
    {
        this.m_oRootScope.title = null;
        this.m_oState.go("root.adminDashboard", { });
    };

    RootController.prototype.openImportPage = function () {
        var oController = this;
        this.m_oRootScope.title = null;
        oController.m_oState.go("root.import", { });
    };

    RootController.prototype.openSendFeedbackDialog = function (oWindow) {
        var oController;
        if (utilsIsObjectNullOrUndefined(oWindow) === true) {
            oController = this;
        } else {
            oController = oWindow;
        }
        oController.m_oModalService.showModal({
            templateUrl: "dialogs/send_feedback/SendFeedbackDialog.html",
            controller: "SendFeedbackController",
            inputs: {
                extras: {
                }
            }
        }).then(function (modal) {
            modal.element.modal();
            modal.close.then(function (oResult) {

            });
        });
    };

    RootController.prototype.activePageCss = function(oPage)
    {
        return (oPage == this.m_oState.current.name );
    };

    RootController.prototype.toggleProcessesBar = function()
    {
        this.m_bIsOpenStatusBar = !this.m_bIsOpenStatusBar;
        this.m_bIsOpenNav = !this.m_bIsOpenNav;
    }

    RootController.prototype.openNav = function() {
        //document.getElementById("status-bar").style.height = "500px";
        this.m_bIsOpenStatusBar = !this.m_bIsOpenStatusBar;
        this.m_bIsOpenNav = true;

    };

    RootController.prototype.closeNav = function() {
        //document.getElementById("status-bar").style.height = "4.5%";
        this.m_bIsOpenStatusBar = !this.m_bIsOpenStatusBar;
        this.m_bIsOpenNav = false;

    };


    RootController.prototype.openSnake = function()
    {
        var oController = this;
        this.m_oModalService.showModal({
            templateUrl: "dialogs/snake_dialog/SnakeDialogV2.html",
            controller: "SnakeController"
        }).then(function(modal) {
            modal.element.modal();

            modal.close.then(function(result) {
                oController.m_oScope.Result = result ;
            });
        });

        return true;
    };

    RootController.prototype.deleteProcess = function(oProcessInput)
    {
        this.m_oProcessWorkspaceService.deleteProcess(oProcessInput);
        return true;
    };

    /**
     * openProcessLogsDialog
     * @returns {boolean}
     */
    RootController.prototype.openProcessLogsDialog = function()
    {
        var oController = this;

        this.m_oModalService.showModal({
            templateUrl: "dialogs/workspace_processes_list/WorkspaceProcessesList.html",
            controller: "WorkspaceProcessesList"
        }).then(function(modal) {
            modal.element.modal({
                backdrop: 'static',
                keyboard: false
            });

            modal.close.then(function(result) {
                oController.m_oScope.Result = result ;
            });
        });

        return true;
    };

    RootController.prototype.editModelWorkspaceNameSetTrue = function(){

        var oController = this;
        if  (utilsIsObjectNullOrUndefined(oController.m_oConstantsService.getActiveWorkspace())) return;

        var oCallback = function (value) {

            if (utilsIsObjectNullOrUndefined(value))
            {
                return;
            }

            var oWorkspace = oController.m_oConstantsService.getActiveWorkspace();

            oWorkspace.name = value;

            oController.m_oWorkspaceService.UpdateWorkspace(oWorkspace).then(function (data) {
                oWorkspace.name = data.data.name;
                oController.m_oRootScope.title = data.data.name;
                oController.m_bIsEditModelWorkspaceNameActive = false;
                utilsVexDialogAlertTop("Name successfully changed.")
            },
            function (data) {
                console.log("RootController.editModelWorkspaceNameSetTrue | data: ", data);

                var oDialog = utilsVexDialogAlertBottomRightCorner(
                    "GURU MEDITATION<br>ERROR CHANGING THE WORKSPACE'S NAME"
                );
                utilsVexCloseDialogAfter(4000, oDialog);
            });
        };

        var sMessage = this.m_oTranslate.instant("MSG_INSERT_WS_NAME");

        utilsVexPrompt(sMessage, oController.m_oConstantsService.getActiveWorkspace().name, oCallback);

        this.m_bIsEditModelWorkspaceNameActive = true;
    };

    RootController.prototype.editUserInfo = function(oProcessInput)
    {
        var oController = this;

        this.m_oModalService.showModal({
            templateUrl: "dialogs/edit_user/EditUserDialog.html",
            controller: "EditUserController",
            inputs: {
                extras: {
                    user:oController.m_oUser
                }
            }
        }).then(function(modal) {
            modal.element.modal();
            modal.close.then(function(result) {
                oController.m_oScope.Result = result ;
                oController.m_oUser = oController.m_oConstantsService.getUser();
            });
        });

        return true;
    };

    RootController.prototype.getWorkspacesInfo = function()
    {
        if (this.m_oConstantsService.getUser() != null) {
            if (this.m_oConstantsService.getUser() != undefined) {

                var sMessage = this.m_oTranslate.instant("MSG_ERROR_READING_WS");

                var oController = this;

                this.m_oWorkspaceService.getWorkspacesInfoListByUser().then(function (data, status) {
                    if (utilsIsObjectNullOrUndefined(data) === false)
                    {
                        oController.m_aoWorkspaces = data.data;
                    }

                },(function (data,status) {
                    utilsVexDialogAlertTop(sMessage);

                }));
            }
        }
    }

    RootController.prototype.forcedChangeNameWorkspace = function () {
        //Untitled Workspace
        var sName="Untitled Workspace";

        if(utilsIsObjectNullOrUndefined(this.m_aoWorkspaces) === true)
        {
            return sName;
        }
        var iNumberOfWorkspaces = this.m_aoWorkspaces.length;
        var iIndexReturnValue = 1;
        var sReturnValue = sName + "(" + iIndexReturnValue + ")";
        for(var iIndexWorkspace = 0 ; iIndexWorkspace < iNumberOfWorkspaces ; iIndexWorkspace++ )
        {

            if(this.m_aoWorkspaces[iIndexWorkspace].workspaceName === sReturnValue)
            {
                iIndexReturnValue++;
                sReturnValue = sName + "(" + iIndexReturnValue + ")";
            }
        }

        return sReturnValue;
    };

    RootController.prototype.openProcessorLogsDialog = function(oProcess)
    {

        var oController = this;

        if(utilsIsObjectNullOrUndefined(oProcess) === true)
        {
            return false;
        }
        oController.m_oModalService.showModal({
            templateUrl: "dialogs/processor_logs/ProcessorLogsView.html",
            controller: "ProcessorLogsController",
            inputs: {
                extras: {
                    process:oProcess,
                }
            }
        }).then(function (modal) {
            modal.element.modal();
            modal.close.then(function(oResult){

            });
        });
        return true;
    };

    RootController.prototype.openLogsDialogByProcessId = function(processId)
    {

        var oController = this;

        if(!processId)
        {
            return false;
        }

        let process = null;
        for(let i = 0; i < oController.m_aoProcessesRunning.length; i++)
        {
            if(processId == oController.m_aoProcessesRunning[i].processObjId){
                process = oController.m_aoProcessesRunning[i];
                break;
            }
        }

        if( process != null){
            oController.openProcessorLogsDialog(process);
            return true;
        }else{
        //    console.error("Cannot find process ID " + processId + " in the processes list")
            return false;
        }
    };

    RootController.prototype.openDocumentatonCenter = function () {

        var oAudio = new Audio('assets/audio/R2D2a.wav');
        oAudio.play();

        this.m_oWindow.open('https://wasdi.readthedocs.io/en/latest/index.html', '_blank');
    }

    RootController.prototype.getOperationDescription = function(oOperation) {
        return utilsConvertOperationToDescription(oOperation);
    }

    // Function used to swap language of the interface
    RootController.prototype.swapLanguage = function (sCountryCode){
        this.m_oTranslate.use(sCountryCode);
        this.m_oState.go("root.marketplace");// go workspaces -> go to marketplace
    }


    /*********************************************************************/
    RootController.$inject = [
        '$rootScope',
        '$scope',
        'ConstantsService',
        'AuthService',
        '$state',
        'ProcessWorkspaceService',
        'WorkspaceService',
        '$timeout',
        'ModalService',
        'RabbitStompService',
        '$window',
        '$translate'
    ];

    return RootController;
}) ();
window.RootController = RootController;
