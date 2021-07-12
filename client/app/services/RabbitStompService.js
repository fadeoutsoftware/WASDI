/**
 * Created by a.corrado on 23/01/2017.
 */

angular.module('wasdi.RabbitStompService', ['wasdi.RabbitStompService']).service('RabbitStompService',
    ['$http', 'ConstantsService', '$interval', 'ProcessesLaunchedService', '$q', '$rootScope',
        function ($http, oConstantsService, $interval, oProcessesLaunchedService, $q, $rootScope, $scope) {

            // Reconnection promise to stop the timer if the reconnection succeed or if the user change page
            this.m_oInterval = $interval;
            // Reference to the Constant Service
            this.m_oConstantsService = oConstantsService;
            // Scope
            this.m_oScope = $scope;
            this.m_oRootScope = $rootScope;

            this.m_oWebSocket = null;
            this.m_oReconnectTimerPromise = null;
            this.m_oRabbitReconnectAttemptCount = 0;

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

            this.m_iConnectionState = RabbitConnectionState.Init;

            // Use defer/promise to keep trace when service is ready to perform any operation
            this.m_oServiceReadyDeferred = null;
            this.m_oServiceReadyPromise = null;
            
            this.waitServiceIsReady = function () {
                if (this.m_oServiceReadyPromise == null)
                {
                    this.m_oServiceReadyPromise = this.m_oServiceReadyDeferred.promise;
                }
                return this.m_oServiceReadyPromise;
            }

            this.setMessageCallback = function (fCallback) {
                this.m_fMessageCallBack = fCallback;
            }

            this.setActiveController = function (oController) {
                this.m_oActiveController = oController;
            }

            this.isSubscrbed = function () {
                return !utilsIsStrNullOrEmpty(this.m_sWorkspaceId);
            }

            this.notifyConnectionStateChange = function(connectionState)
            {
                this.m_iConnectionState = connectionState;
                var msgHlp = MessageHelper.getInstance($rootScope);
                msgHlp.notifyRabbitConnectionStateChange(connectionState);
            }

            this.isReadyState = function () {
                return (((utilsIsObjectNullOrUndefined(this.m_oWebSocket) === false) && (this.m_oWebSocket.readyState === WebSocket.OPEN)) ? true : false);
            }

            this.subscribe = function (workspaceId) {

                this.unsubscribe();

                this.m_sWorkspaceId = workspaceId;

                var subscriptionString = "/exchange/amq.topic/" + workspaceId;
                console.log("RabbitStompService: subscribing to " + subscriptionString);
                var oThisService = this;

                try {
                    this.m_oSubscription = this.m_oClient.subscribe(subscriptionString, function (message) {

                        // Check message Body
                        if (message.body)
                        {
                            console.log("RabbitStompService: got message with body " + message.body)

                            // Get The Message View Model
                            var oMessageResult = JSON.parse(message.body);

                            // Check parsed object
                            if (oMessageResult == null)
                            {
                                console.log("RabbitStompService: there was an error parsing result in JSON. Message lost")
                                return;
                            }

                            // Get the Active Workspace Id
                            var sActiveWorkspaceId = "";

                            if (!utilsIsObjectNullOrUndefined(oThisService.m_oConstantsService.getActiveWorkspace()))
                            {
                                sActiveWorkspaceId = oThisService.m_oConstantsService.getActiveWorkspace().workspaceId;
                            }
                            else
                            {
                                console.log("RabbitStompService: Active Workspace is null.")
                            }

                            //Reject the message if it is for another workspace
                            if (oMessageResult.workspaceId != sActiveWorkspaceId) return false;

                            // Check if the callback is hooked
                            if (!utilsIsObjectNullOrUndefined(oThisService.m_fMessageCallBack))
                            {
                                // Call the Message Callback
                                oThisService.m_fMessageCallBack(oMessageResult, oThisService.m_oActiveController);
                            }

                            // Update the process List
                            oThisService.m_oProcessesLaunchedService.loadProcessesFromServer(sActiveWorkspaceId);
                        }
                    });
                }
                catch (e) {
                    console.log("RabbitStompService: Exception subscribing to " + workspaceId);
                }
            };

            this.unsubscribe = function () {
                if (this.m_oSubscription)
                {
                    this.m_sWorkspaceId = "";
                    this.m_oSubscription.unsubscribe();
                }
            };

            this.getConnectionState = function()
            {
                return this.m_iConnectionState;
            }


            this.initWebStomp = function () {
                var _this = this;

                this.m_oServiceReadyDeferred = $q.defer();

                // Web Socket to receive workspace messages
                var oWebSocket = new WebSocket(this.m_oConstantsService.getStompUrl());
                //var oWebSocket = new SockJS(this.m_oConstantsService.getStompUrl());
                this.m_oClient = Stomp.over(oWebSocket);
                this.m_oClient.heartbeat.outgoing = 20000;
                this.m_oClient.heartbeat.incoming = 20000;
                this.m_oClient.debug = null;

                /**
                 * Callback of the Rabbit On Connect
                 */
                var on_connect = function () {
                    console.log('RabbitStompService: Web Stomp connected');

                    _this.notifyConnectionStateChange(RabbitConnectionState.Connected);
                    _this.m_oRabbitReconnectAttemptCount = 0;

                    //CHECK IF the session is valid
                    var oSessionId = _this.m_oConstantsService.getSessionId();

                    if (utilsIsObjectNullOrUndefined(oSessionId))
                    {
                        console.log("RabbitStompService: Error session id Null in on_connect");
                        return false;
                    }

                    _this.m_oServiceReadyDeferred.resolve(true);

                    // Is this a re-connection?
                    if (_this.m_oReconnectTimerPromise != null)
                    {

                        // Yes it is: clear the timer
                        _this.m_oInterval.cancel(_this.m_oReconnectTimerPromise);
                        _this.m_oReconnectTimerPromise = null;

                        if (_this.m_sWorkspaceId !== "")
                        {
                            _this.subscribe(_this.m_sWorkspaceId);
                        }
                    }
                };


                /**
                 * Callback for the Rabbit On Error
                 */
                var on_error = function (sMessage) {

                    console.log('RabbitStompService: WEB STOMP ERROR, message:' + sMessage + ' [' + utilsGetTimeStamp() + ']');

                    if (sMessage == "LOST_CONNECTION" || sMessage.includes("Whoops! Lost connection to"))
                    {
                        console.log('RabbitStompService: Web Socket Connection Lost');

                        _this.notifyConnectionStateChange(RabbitConnectionState.Lost);

                        if (_this.m_oReconnectTimerPromise == null)
                        {
                            // Try to Reconnect
                            _this.m_oRabbitReconnectAttemptCount = 0;
                            _this.m_oReconnectTimerPromise = _this.m_oInterval(_this.m_oRabbitReconnect, 5000);
                        }
                    }
                };

                // Keep local reference to the callbacks to use it in the reconnection callback
                this.m_oOn_Connect = on_connect;
                this.m_oOn_Error = on_error;
                this.m_oWebSocket = oWebSocket;
                // Call back for rabbit reconnection
                var rabbit_reconnect = function () {

                    _this.m_oRabbitReconnectAttemptCount++;
                    console.log('RabbitStompService: Web Stomp Reconnection Attempt (' + _this.m_oRabbitReconnectAttemptCount +')');

                    // Connect again
                    _this.m_oWebSocket = new WebSocket(_this.m_oConstantsService.getStompUrl());
                    //_this.oWebSocket = new SockJS(_this.m_oConstantsService.getStompUrl());
                    _this.m_oClient = Stomp.over(_this.m_oWebSocket);
                    _this.m_oClient.heartbeat.outgoing = 20000;
                    _this.m_oClient.heartbeat.incoming = 20000;
                    _this.m_oClient.debug = null;

                    _this.m_oClient.connect(_this.m_oConstantsService.getRabbitUser(), _this.m_oConstantsService.getRabbitPassword(), _this.m_oOn_Connect, _this.m_oOn_Error, '/');
                };
                
                this.m_oRabbitReconnect = rabbit_reconnect;
                //connect to the queue
                this.m_oClient.connect(_this.m_oConstantsService.getRabbitUser(), _this.m_oConstantsService.getRabbitPassword(), on_connect, on_error, '/');

                return true;
            };

            this.initWebStomp();

        }]);

