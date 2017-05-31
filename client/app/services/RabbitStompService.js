/**
 * Created by a.corrado on 23/01/2017.
 */
'use strict';
angular.module('wasdi.RabbitStompService', ['wasdi.RabbitStompService']).
service('RabbitStompService', ['$http',  'ConstantsService','$interval','ProcessesLaunchedService', function ($http, oConstantsService,$interval,oProcessesLaunchedService,$scope) {

    // Reconnection promise to stop the timer if the reconnection succeed or if the user change page
    this.m_oInterval = $interval;
    // Reference to the Constant Service
    this.m_oConstantsService = oConstantsService;
    // Scope
    this.m_oScope = $scope;

    this.m_oReconnectTimerPromise = null;
    this.m_oRabbitReconnect = null;

    // STOMP Client
    this.m_oClient = null;
    // Rabbit Connect Callback
    this.m_oOn_Connect = null;
    // Rabbit Error Callback
    this.m_oOn_Error = null;
    // Rabbit Reconnect Callback
    this.m_oRabbitReconnect = null;

    // Reference to the ProcessLaunched Service
    this.m_oProcessesLaunchedService = oProcessesLaunchedService;

    this.m_oSubscription = null;
    this.m_oUser = null;

    this.m_aoErrorsList = [];

    this.m_fMessageCallBack = null;

    this.m_oActiveController = null;

    this.m_sWorkspaceId = "";

    this.setMessageCallback = function (fCallback) {
        this.m_fMessageCallBack = fCallback;
    }

    this.setActiveController = function (oController) {
        this.m_oActiveController = oController;
    }

    this.isSubscrbed = function() {
        return !utilsIsStrNullOrEmpty(this.m_sWorkspaceId);
    }


    this.subscribe = function (workspaceId) {
        this.unsubscribe();

        this.m_sWorkspaceId = workspaceId;

        var subscriptionString = "/exchange/amq.topic/" + workspaceId;
        console.log("RabbitStompService: subscribing to " + subscriptionString);
        var oThisService = this;


        this.m_oSubscription = this.m_oClient.subscribe(subscriptionString, function (message) {

            // Check message Body
            if (message.body)
            {
                console.log("RabbitStompService: got message with body " + message.body)

                // Get The Message View Model
                var oMessageResult = JSON.parse(message.body);

                // Check parsed object
                if (oMessageResult == null) {
                    console.log("RabbitStompService: there was an error parsing result in JSON. Message lost")
                    return;
                }

                // Get the Active Workspace Id
                var sActiveWorkspaceId = "";

                if (!utilsIsObjectNullOrUndefined(oThisService.m_oConstantsService.getActiveWorkspace())) {
                    sActiveWorkspaceId = oThisService.m_oConstantsService.getActiveWorkspace().workspaceId;
                }
                else {
                    console.log("RabbitStompService: Active Workspace is null.")
                }

                if (oMessageResult.messageResult == "KO") {

                    // Get the operation NAme
                    var sOperation = "null";
                    if (utilsIsStrNullOrEmpty(oMessageResult.messageCode) == false  ) sOperation = oMessageResult.messageCode;

                    // Add an error Message
                    oThisService.m_aoErrorsList.push({text:"There was an error in the " + sOperation + " operation"});
                    // Update Process Messages
                    oThisService.m_oProcessesLaunchedService.loadProcessesFromServer(sActiveWorkspaceId);

                    return;
                }

                //Reject the message if it is for another workspace
                if(oMessageResult.workspaceId != sActiveWorkspaceId) return false;


                // Check if the callback is hooked
                if(!utilsIsObjectNullOrUndefined(oThisService.m_fMessageCallBack)) {
                    // Call the Message Callback
                    oThisService.m_fMessageCallBack(oMessageResult, oThisService.m_oActiveController);
                }


                // Update the process List
                oThisService.m_oProcessesLaunchedService.loadProcessesFromServer(sActiveWorkspaceId);

            }
        });
    }

    this.unsubscribe = function () {
        if (this.m_oSubscription) {
            this.m_sWorkspaceId = "";
            this.m_oSubscription.unsubscribe();
        }
    }

    /*@Params: WorkspaceID, Name of controller, Controller
    * it need the Controller for call the methods (the methods are inside the active controllers)
    * the methods are call in oRabbitCallback
    * */
    this.initWebStomp = function()
    {
        // Web Socket to receive workspace messages
        //var oWebSocket = new WebSocket(this.m_oConstantsService.getStompUrl());
        var oWebSocket = new SockJS(this.m_oConstantsService.getStompUrl());
        var oThisService = this;
        this.m_oClient = Stomp.over(oWebSocket);
        this.m_oClient.heartbeat.outgoing = 0;
        this.m_oClient.heartbeat.incoming = 0;
        this.m_oClient.debug = null;

        /**
         * Callback of the Rabbit On Connect
         */
        var on_connect = function () {
            console.log('RabbitStompService: Web Stomp connected');

            //CHECK IF the session is valid
            var oSessionId = oThisService.m_oConstantsService.getSessionId();
            if(utilsIsObjectNullOrUndefined(oSessionId))
            {
                console.log("RabbitStompService: Error session id Null in on_connect");
                return false;
            }

            // Is this a re-connection?
            if (oThisService.m_oReconnectTimerPromise != null) {

                // Yes it is: clear the timer
                oThisService.m_oInterval.cancel(oThisService.m_oReconnectTimerPromise);
                oThisService.m_oReconnectTimerPromise = null;

                if (oThisService.m_sWorkspaceId !== "") {
                    oThisService.subscribe(oThisService.m_sWorkspaceId);
                }
            }
        };


        /**
         * Callback for the Rabbit On Error
         */
        var on_error = function (sMessage) {

            console.log('RabbitStompService: WEB STOMP ERROR, message:' + sMessage + ' [' +utilsGetTimeStamp() + ']');


            if (sMessage == "LOST_CONNECTION" || sMessage == "Whoops! Lost connection to undefined") {
                console.log('RabbitStompService: Web Socket Connection Lost');

                if (oThisService.m_oReconnectTimerPromise == null) {
                    // Try to Reconnect
                    oThisService.m_oReconnectTimerPromise = oThisService.m_oInterval(oThisService.m_oRabbitReconnect, 5000);
                }
            }
        };

        // Keep local reference to the callbacks to use it in the reconnection callback
        this.m_oOn_Connect = on_connect;
        this.m_oOn_Error = on_error;

        // Call back for rabbit reconnection
        var rabbit_reconnect = function () {

            console.log('RabbitStompService: Web Stomp Reconnection Attempt');

            // Connect again
            //oThisService.oWebSocket = new WebSocket(oThisService.m_oConstantsService.getStompUrl());
            oThisService.oWebSocket = new SockJS(oThisService.m_oConstantsService.getStompUrl());
            oThisService.m_oClient = Stomp.over(oThisService.oWebSocket);
            oThisService.m_oClient.heartbeat.outgoing = 0;
            oThisService.m_oClient.heartbeat.incoming = 0;
            oThisService.m_oClient.debug = null;

            oThisService.m_oClient.connect(oThisService.m_oConstantsService.getRabbitUser(), oThisService.m_oConstantsService.getRabbitPassword(), oThisService.m_oOn_Connect, oThisService.m_oOn_Error, '/');
        };


        this.m_oRabbitReconnect = rabbit_reconnect;
        //connect to the queue
        this.m_oClient.connect(oThisService.m_oConstantsService.getRabbitUser(), oThisService.m_oConstantsService.getRabbitPassword(), on_connect, on_error, '/');


        return true;
    }

    this.initWebStomp();

}]);

