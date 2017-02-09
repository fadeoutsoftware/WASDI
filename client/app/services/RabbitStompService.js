/**
 * Created by a.corrado on 23/01/2017.
 */
'use strict';
angular.module('wasdi.RabbitStompService', ['wasdi.RabbitStompService']).
service('RabbitStompService', ['$http',  'ConstantsService','$interval','ProcessesLaunchedService', function ($http, oConstantsService,$interval,oProcessesLaunchedService,$scope) {

    // Reconnection promise to stop the timer if the reconnection succeed or if the user change page
    this.m_oInterval = $interval;
    this.m_oConstantsService = oConstantsService;
    this.m_oScope = $scope;
    this.m_oActiveWorkspace=null;

    this.m_oReconnectTimerPromise = null;
    this.m_oRabbitReconnect = null;
    this.m_oClient = null;
    this.m_oOn_Connect = null;
    this.m_oOn_Error = null;
    this.m_oRabbitReconnect = null;
    this.m_oProcessesLaunchedService = oProcessesLaunchedService;

    this.m_oSubscription = null;
    this.m_oUser = null;

    /*@Params: WorkspaceID, Name of controller, Controller
    * it need the Controller for call the methods (the methods are inside the active controllers)
    * the methods are call in oRabbitCallback
    * */
    this.initWebStomp = function(oActiveWorkspace,sControllerName,oControllerActive)
    {
        if(utilsIsObjectNullOrUndefined(oActiveWorkspace) || utilsIsObjectNullOrUndefined(oControllerActive) || utilsIsStrNullOrEmpty(sControllerName) )
        {
            console.log("InitWebStomp some value are null");
            return false;
        }
        this.m_oActiveWorkspace=oActiveWorkspace;

        // Web Socket to receive workspace messages
        var oWebSocket = new WebSocket(this.m_oConstantsService.getStompUrl());
        var oController = this;
        this.m_oClient = Stomp.over(oWebSocket);

        /**
         * Rabbit Callback: receives the Messages
         * @param message
         */
        var oRabbitCallback = function (message) {
            // called when the client receives a STOMP message from the server
            if (message.body)
            {
                console.log("got message with body " + message.body)

                // Get The Message View Model
                var oMessageResult = JSON.parse(message.body);

                if (oMessageResult == null) return;
                if (oMessageResult.messageResult == "KO") {
                    //TODO REMOVE ELEMENT IN PROCESS QUEUE
                    utilsVexDialogAlertTop("There was an error in the RabbitCallback");
                    oController.m_oProcessesLaunchedService.loadProcessesFromServer(oController.m_oActiveWorkspace.workspaceId);

                    //alert('There was an error in the RabbitCallback');
                    return;
                }

                //Reject the messages for an other workspace
                if(oMessageResult.workspaceId != oController.m_oActiveWorkspace.workspaceId)
                    return false;
                // Route the message

            switch(oMessageResult.messageCode)
            {
                case "DOWNLOAD":
                    if(sControllerName == "EditorController" || sControllerName == "ImportController")
                    {
                        oControllerActive.receivedDownloadMessage(oMessageResult);
                        oController.m_oProcessesLaunchedService.loadProcessesFromServer(oController.m_oActiveWorkspace.workspaceId);
                        var oDialog = utilsVexDialogAlertBottomRightCorner("The download is ended");
                        utilsVexCloseDialogAfterFewSeconds(3000,oDialog);
                    }
                    break;
                case "PUBLISH":
                    if(sControllerName == "EditorController" )
                    {
                        oControllerActive.receivedPublishMessage(oMessageResult);
                        oController.m_oProcessesLaunchedService.loadProcessesFromServer(oController.m_oActiveWorkspace.workspaceId);
                        var oDialog = utilsVexDialogAlertBottomRightCorner("The publish is ended");
                        utilsVexCloseDialogAfterFewSeconds(3000,oDialog);
                    }
                    break;
                case "PUBLISHBAND":
                    if(sControllerName == "EditorController" )
                    {
                        oControllerActive.receivedPublishBandMessage(oMessageResult.payload);
                        oController.m_oProcessesLaunchedService.loadProcessesFromServer(oController.m_oActiveWorkspace.workspaceId);
                        var oDialog = utilsVexDialogAlertBottomRightCorner("The publish is ended.Product: " + oMessageResult.payload.productName);
                        utilsVexCloseDialogAfterFewSeconds(3000,oDialog);

                    }
                    if( sControllerName == "ImportController")
                    {
                        oController.m_oProcessesLaunchedService.loadProcessesFromServer(oController.m_oActiveWorkspace.workspaceId);
                        var oDialog = utilsVexDialogAlertBottomRightCorner("The publish is ended. Product:" + oMessageResult.payload.fileName);
                        //utilsVexCloseDialogAfterFewSeconds(3000,oDialog);
                    }
                    break;
                case "UPDATEPROCESSES":
                    oController.m_oProcessesLaunchedService.loadProcessesFromServer(oController.m_oActiveWorkspace.workspaceId);
                    break;
                case "TERRAIN":
                    oControllerActive.receivedTerrainMessage(oMessageResult);
                    oController.m_oProcessesLaunchedService.loadProcessesFromServer(oController.m_oActiveWorkspace.workspaceId);
                    var oDialog = utilsVexDialogAlertBottomRightCorner("terrain Operation Completed");
                    utilsVexCloseDialogAfterFewSeconds(3000,oDialog);
                    break;
                default:
                    console.log("got empty message");
            }

        }
    }
            //oController.addTestLayer(message.body);
            /**
             * Callback of the Rabbit On Connect
             */

            var on_connect = function () {
                console.log('Web Stomp connected');

                //CHECK IF sWorkSpaceId is null
                var oSessioId = oController.m_oConstantsService.getSessionId();
                if(utilsIsObjectNullOrUndefined(oSessioId))
                {
                    console.log("Error session id Null in on_connect");
                    return false;
                }
                oController.m_oSubscription = oController.m_oClient.subscribe(oSessioId, oRabbitCallback);

                // Is this a re-connection?
                if (oController.m_oReconnectTimerPromise != null) {
                    // Yes it is: clear the timer
                    oController.m_oInterval.cancel(oController.m_oReconnectTimerPromise);
                    oController.m_oReconnectTimerPromise = null;
                }
            };


            /**
             * Callback for the Rabbit On Error
             */
            var on_error = function (sMessage) {
                console.log('Web Stomp Error');
                if (sMessage == "LOST_CONNECTION") {
                    console.log('LOST Connection');

                    if (oController.m_oReconnectTimerPromise == null) {
                        // Try to Reconnect
                        oController.m_oReconnectTimerPromise = oController.m_oInterval(oController.m_oRabbitReconnect, 5000);
                    }
                }
            };

            // Keep local reference to the callbacks to use it in the reconnection callback
            this.m_oOn_Connect = on_connect;
            this.m_oOn_Error = on_error;

            // Call back for rabbit reconnection
            var rabbit_reconnect = function () {

                console.log('Web Stomp Reconnection Attempt');

                // Connect again
                oController.oWebSocket = new WebSocket(oController.m_oConstantsService.getStompUrl());
                oController.m_oClient = Stomp.over(oController.oWebSocket);
                oController.m_oClient.connect(oController.m_oConstantsService.getRabbitUser(), oController.m_oConstantsService.getRabbitPassword(), oController.m_oOn_Connect, oController.m_oOn_Error, '/');
            };
            this.m_oRabbitReconnect = rabbit_reconnect;
            //connect to the queue
            this.m_oClient.connect(oController.m_oConstantsService.getRabbitUser(), oController.m_oConstantsService.getRabbitPassword(), on_connect, on_error, '/');


            //// Clean Up when exit!!
            oControllerActive.m_oScope.$on('$destroy', function () {
                // Is this a re-connection?
                if (oController.m_oReconnectTimerPromise != null) {
                    // Yes it is: clear the timer
                    oController.m_oInterval.cancel(oController.m_oReconnectTimerPromise);
                    oController.m_oReconnectTimerPromise = null;
                }
                else {
                    if (oController.m_oClient != null) {
                        oController.m_oClient.disconnect();
                    }
                }
            });

        return true;
    }

    //This method remove a message in all queues
    //this.removeMessageInQueues = function(oMessage)
    //{
    //    if(utilsIsObjectNullOrUndefined(oMessage))
    //        return false;
    //    var iIndexMessageInEditoControllerQueue = utilsFindObjectInArray(this.m_aoEditorControllerQueueMessages,oMessage) ;
    //    var iIndexMessageInImportControllerQueue = utilsFindObjectInArray(this.m_aoImportControllerQueueMessages,oMessage) ;
    //    // TODO REMOVE TO CACHE
    //    /*Remove in editor controller*/
    //    if (iIndexMessageInEditoControllerQueue > -1) {
    //        this.m_aoEditorControllerQueueMessages.splice(iIndexMessageInEditoControllerQueue, 1);
    //    }
    //    /*remove in Import Controller*/
    //    if ( iIndexMessageInImportControllerQueue > -1) {
    //        this.m_aoImportControllerQueueMessages.splice( iIndexMessageInImportControllerQueue, 1);
    //    }
    //
    //}



}]);

