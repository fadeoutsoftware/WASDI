/**
 * Created by p.campanella on 24/10/2016.
 */
var EditorController = (function () {
    function EditorController($scope, $location, $interval, oConstantsService, oAuthService, oMapService, oFileBufferService, oProductService,$state,oWorkspaceService) {

        // Reference to the needed Services
        this.m_oScope = $scope;
        this.m_oLocation = $location;
        this.m_oInterval = $interval;
        this.m_oConstantsService = oConstantsService;
        this.m_oAuthService = oAuthService;
        this.m_oMapService = oMapService;
        this.m_oFileBufferService = oFileBufferService;
        this.m_oProductService = oProductService;
        this.m_oScope.m_oController = this;
        this.m_bStatusPublishing=false;
        this.m_oState=$state;
        this.m_oWorkspaceService = oWorkspaceService;
        // Reconnection promise to stop the timer if the reconnection succeed or if the user change page
        this.m_oReconnectTimerPromise = null;



        // Here a Workpsace is needed... if it is null create a new one..
        this.m_oActiveWorkspace = this.m_oConstantsService.getActiveWorkspace();

        //if there isn't workspace
        if(utilsIsObjectNullOrUndefined( this.m_oActiveWorkspace) && utilsIsStrNullOrEmpty( this.m_oActiveWorkspace))
        {
            //if this.m_oState.params.workSpace in empty null or undefined create new workspace
            if(!(utilsIsObjectNullOrUndefined(this.m_oState.params.workSpace) && utilsIsStrNullOrEmpty(this.m_oState.params.workSpace)))
            {
                this.openWorkspace(this.m_oState.params.workSpace);
                this.m_oActiveWorkspace = this.m_oConstantsService.getActiveWorkspace();
            }
            else
            {
                //TODO CREATE NEW WORKSPACE OR GO HOME
            }
        }
        this.m_sDownloadFilePath = "";

        // Web Socket to receive workspace messages
        var oWebSocket = new WebSocket(this.m_oConstantsService.getStompUrl());
        this.m_oClient = Stomp.over(oWebSocket);

        // Rabbit subscription
        var m_oSubscription = {};

        // Array of products to show
        this.m_aoProducts = [];


        // Self reference for callbacks
        var oController = this;

        //Set default value tree
        this.m_oTree=null;


        /**
         * Rabbit Callback: receives the Messages
         * @param message
         */
        var oRabbitCallback = function (message) {
            // called when the client receives a STOMP message from the server
            if (message.body) {
                console.log("got message with body " + message.body)

                // Get The Message View Model
                var oMessageResult = JSON.parse(message.body);

                // Route the message
                if (oMessageResult.messageCode=="DOWNLOAD") {
                    oController.receivedDownloadMessage(oMessageResult);
                }
                else if (oMessageResult.messageCode=="PUBLISH") {
                    oController.receivedPublishMessage(oMessageResult);
                }
                else if (oMessageResult.messageCode=="PUBLISHBAND") {
                    this.m_bStatusPublishing=false;
                    oController.receivedPublishBandMessage(oMessageResult);
                }

            } else {
                console.log("got empty message");
            }

            //oController.addTestLayer(message.body);

        }

        /**
         * Callback of the Rabbit On Connect
         */
        var on_connect = function() {
            console.log('Web Stomp connected');
            oController.m_oSubscription = oController.m_oClient.subscribe(oController.m_oActiveWorkspace.workspaceId, oRabbitCallback);

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
        var on_error =  function(sMessage) {
            console.log('Web Stomp Error');
            if (sMessage=="LOST_CONNECTION") {
                console.log('LOST Connection');

                if (oController.m_oReconnectTimerPromise == null) {
                    // Try to Reconnect
                    oController.m_oReconnectTimerPromise = oController.m_oInterval(rabbit_reconnect,5000);
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
            oController.m_oClient.connect('guest', 'guest', oController.m_oOn_Connect, oController.m_oOn_Error, '/');
        };

        //connect to the queue
        this.m_oClient.connect('guest', 'guest', on_connect, on_error, '/');


        // Initialize the map

        oMapService.initMap('wasdiMap');

        // Clean Up when exit!!
        this.m_oScope.$on('$destroy', function () {
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

        /* ---------------- WATCH -----------------*/
        // Read Product List
        // watch when m_oController.m_oConstantsService.m_oActiveWorkspace change (usually after a load/reload of page)
        // reload tree
        $scope.$watch('m_oController.m_oConstantsService.m_oActiveWorkspace', function (newValue, oldValue, scope)
        {
            this.m_oActiveWorkspace = newValue;
            if(!utilsIsObjectNullOrUndefined(this.m_oActiveWorkspace))
            {
                $scope.m_oController.m_oProductService.getProductListByWorkspace(this.m_oActiveWorkspace.workspaceId).success(function (data, status) {
                    if (data != null) {
                        if (data != undefined) {
                            oController.m_aoProducts = data;
                            // i need to make the tree after the products are loaded
                            $scope.m_oController.m_oTree = oController.generateTree();
                            //oController.m_oScope.$apply();
                        }
                    }
                }).error(function (data, status) {
                    console.log('Error reading product list')
                });
            }
        });

    }

    /********************METHODS********************/

    /**
     * Handler of the "download" message
     * @param oMessage Received Message
     */
    EditorController.prototype.receivedDownloadMessage = function (oMessage) {
        if (oMessage == null) return;
        if (oMessage.messageResult=="KO") {
            alert('There was an error in the download');
            return;
        }

        this.m_aoProducts.push(oMessage.payload);
        this.m_oScope.$apply();

        this.m_oProductService.addProductToWorkspace(oMessage.payload.fileName,this.m_oActiveWorkspace.workspaceId).success(function (data, status) {
            console.log('Product added to the ws');
        }).error(function (data,status) {
            console.log('Error adding product to the ws');
        });
    }


    /**
     * Handler of the "publish" message
     * @param oMessage
     */
    EditorController.prototype.receivedPublishMessage = function (oMessage) {
        if (oMessage == null) return;
        if (oMessage.messageResult=="KO") {
            alert('There was an error in the publish');
            return;
        }

    }


    /**
     * Handler of the "PUBLISHBAND" message
     * @param oMessage
     */
    EditorController.prototype.receivedPublishBandMessage = function (oMessage) {
        if (oMessage == null) return;
        if (oMessage.messageResult=="KO") {
            alert('There was an error in the publish band');
            return;
        }

        this.addTestLayer(oMessage.payload.layerId);
    }

    /**
     * Change location to path
     * @param sPath
     */
    EditorController.prototype.moveTo = function (sPath) {
        this.m_oLocation.path(sPath);
    }

    /**
     * Get the user name
     * @returns {*}
     */
    EditorController.prototype.getUserName = function () {
        var oUser = this.m_oConstantsService.getUser();

        if (oUser != null) {
            if (oUser != undefined) {
                var sName = oUser.name;
                if (sName == null) sName = "";
                if (sName == undefined) sName = "";

                if (oUser.surname != null) {
                    if (oUser.surname != undefined) {
                        sName += " " + oUser.surname;

                        return sName;
                    }
                }
            }
        }

        return "";
    }

    /**
     * Check if the user is logged or not
     */
    EditorController.prototype.isUserLogged = function () {
        return this.m_oConstantsService.isUserLogged();
    }

    /**
     * Add test layer
     * @param sLayerId
     */
    EditorController.prototype.addTestLayer = function (sLayerId) {
        //
        var oMap = this.m_oMapService.getMap();

        var wmsLayer = L.tileLayer.wms('http://localhost:8080/geoserver/ows?', {
            layers: 'wasdi:' + sLayerId,
            format: 'image/png',
            transparent: true
        }).addTo(oMap);
    }

    /**
     * Call Download Image Service
     * @param sUrl
     */
    EditorController.prototype.downloadEOImage = function (sUrl) {
        this.m_oFileBufferService.download(sUrl,this.m_oActiveWorkspace.workspaceId).success(function (data, status) {0
            console.log('downloading');
        }).error(function (data, status) {
            console.log('download error');
        });
    }

    /**
     * Call publish service
     * @param sUrl
     */
    EditorController.prototype.publish = function (sUrl) {
        this.m_oFileBufferService.publish(sUrl,this.m_oActiveWorkspace.workspaceId).success(function (data, status) {0
            console.log('publishing');
        }).error(function (data, status) {
            console.log('publish error');
        });
    }

    /**
     * Get a list of product items with only name and index linked to the m_aoProducts ProductViewModel array
     * @returns {Array}
     */
    EditorController.prototype.getProductList = function () {
        var aoProductItems = [];

        var iProductsCount = this.m_aoProducts.length;

        for (var i=0; i<iProductsCount; i++){
            var oProduct = this.m_aoProducts[i];

            var oProductItem = [];
            oProductItem.name = oProduct.name;
            oProductItem.index = i;
            aoProductItems.push(oProductItem);
        }

        return aoProductItems;
    }

    /**
     * Get a list of bands for a product
     * @param oProductItem
     * @returns {Array}
     */
    EditorController.prototype.getBandsForProduct = function (oProductItem) {
        var asBands = [];

        var iProductsCount = this.m_aoProducts.length;

        if (oProductItem.index>=iProductsCount) return asBands;

        var oProduct = this.m_aoProducts[oProductItem.index];

        var aoBands = oProduct.bandsGroups.bands;
        var iBandCount = aoBands.length;

        for (var i=0; i<iBandCount; i++) {
            var oBandItem = {};
            oBandItem.name = aoBands[i].name;
            oBandItem.productName = oProductItem.name;
            oBandItem.productIndex = oProductItem.index;

            asBands.push(oBandItem);
        }

        return asBands;
    }

    EditorController.prototype.openBandImage = function (oBand) {

        var sFileName = this.m_aoProducts[oBand.productIndex].fileName;

        this.m_oFileBufferService.publishBand(sFileName,this.m_oActiveWorkspace.workspaceId, oBand.name).success(function (data, status) {0
            console.log('publishing band ' + oBand.name);
            this.m_bStatusPublishing = true;
        }).error(function (data, status) {
            console.log('publish band error');
        });
    }

    // GENERATE TREE
    // Expected format of the node (there are no required fields)
    //{
    //    id          : "string" // will be autogenerated if omitted
    //    text        : "string" // node text
    //    icon        : "string" // string for custom
    //    state       : {
    //        opened    : boolean  // is the node open
    //        disabled  : boolean  // is the node disabled
    //        selected  : boolean  // is the node selected
    //    },
    //    children    : []  // array of strings or objects
    //    li_attr     : {}  // attributes for the generated LI node
    //    a_attr      : {}  // attributes for the generated A node
    //}
    // Alternative format of the node (id & parent are required)
    //{
    //    id          : "string" // required
    //    parent      : "string" // required
    //    text        : "string" // node text
    //    icon        : "string" // string for custom
    //    state       : {
    //        opened    : boolean  // is the node open
    //        disabled  : boolean  // is the node disabled
    //        selected  : boolean  // is the node selected
    //    },
    //    li_attr     : {}  // attributes for the generated LI node
    //    a_attr      : {}  // attributes for the generated A node
    //}
    EditorController.prototype.generateTree = function ()
    {
        var oController = this;
        var oTree =
        {
            'core': {
                'data': []
                , "check_callback": true
            },
        }



        var productList = this.getProductList();
        //var productList = [{"name":"nomeprodotto1"},{"name":"nomeprodotto2"}];
        for (var iIndexProduct = 0; iIndexProduct < productList.length; iIndexProduct++) {
            var oNode = new Object();
            oNode.text=productList[iIndexProduct].name;//LABEL NODE
            oNode.children=[{"text": "metadata"},{"text":"Bands", "children": []}];//CHILDREN
            oTree.core.data.push(oNode);


            var oaBandsItems = this.getBandsForProduct(productList[iIndexProduct]);
            //var oaBandsItems = [{"name":"band1"},{"name":"band2"}];
            for (var iIndexBandsItems = 0; iIndexBandsItems < oaBandsItems.length; iIndexBandsItems++)
            {
                var oNode=new Object();
                oNode.text = oaBandsItems[iIndexBandsItems].name;//LABEL NODE
                oNode.band = oaBandsItems[iIndexBandsItems];//BAND
                oNode.icon = "assets/icons/File.png";
                oTree.core.data[iIndexProduct].children[1].children.push(oNode);

            }


        }

        return oTree;
    }

    //---------------- OPENWORKSPACE -----------------------
    // ReLOAD workspace when reload page
    EditorController.prototype.openWorkspace = function (sWorkspaceId) {

        var oController = this;

        this.m_oWorkspaceService.getWorkspaceEditorViewModel(sWorkspaceId).success(function (data, status) {
            if (data != null)
            {
                if (data != undefined)
                {
                    oController.m_oConstantsService.setActiveWorkspace(data);
                }
            }
        }).error(function (data,status) {
            alert('error');
        });
    }

    EditorController.$inject = [
        '$scope',
        '$location',
        '$interval',
        'ConstantsService',
        'AuthService',
        'MapService',
        'FileBufferService',
        'ProductService',
        '$state',
        'WorkspaceService'
    ];

    return EditorController;
})();
