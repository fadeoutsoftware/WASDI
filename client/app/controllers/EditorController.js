/**
 * Created by p.campanella on 24/10/2016.
 */
var EditorController = (function () {
    function EditorController($scope, $location, $interval, oConstantsService, oAuthService, oMapService, oFileBufferService,
                              oProductService, $state, oWorkspaceService, oGlobeService, oProcessesLaunchedService, oRabbitStompService, oSnapOperationService, oModalService) {

        // Reference to the needed Services
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oLocation = $location;
        this.m_oInterval = $interval;
        this.m_oConstantsService = oConstantsService;
        this.m_oAuthService = oAuthService;
        this.m_oMapService = oMapService;
        this.m_oFileBufferService = oFileBufferService;
        this.m_oProductService = oProductService;
        this.m_oSnapOperationService = oSnapOperationService;
        this.m_oGlobeService = oGlobeService;
        this.m_oState = $state;
        this.m_oProcessesLaunchedService = oProcessesLaunchedService;
        this.m_oWorkspaceService = oWorkspaceService;
        this.m_oRabbitStompServive = oRabbitStompService;
        this.m_b2DMapModeOn = true;
        this.m_b3DMapModeOn = false;
        this.m_bIsVisibleMapOfLeaflet = false;
        this.m_oModalService = oModalService;

        this.m_oAreHideBars = {
            mainBar:false,
            radarBar:true,
            opticalBar:true

        }

        /*Last file donloaded*/
        this.m_oLastDownloadedProduct = null;
        //Pixel Info
        this.m_bIsVisiblePixelInfo = false;
        //this.hideOrShowPixelInfo()//set css

        //layer list
        this.m_aoLayersList = [];//only id
        //this.m_aoProcessesRunning=[];
        // Array of products to show
        this.m_aoProducts = [];

        // Here a Workpsace is needed... if it is null create a new one..
        this.m_oActiveWorkspace = this.m_oConstantsService.getActiveWorkspace();
        this.m_oUser = this.m_oConstantsService.getUser();

        //if there isn't workspace
        if (utilsIsObjectNullOrUndefined(this.m_oActiveWorkspace) && utilsIsStrNullOrEmpty(this.m_oActiveWorkspace)) {
            //if this.m_oState.params.workSpace in empty null or undefined create new workspace
            if (!(utilsIsObjectNullOrUndefined(this.m_oState.params.workSpace) && utilsIsStrNullOrEmpty(this.m_oState.params.workSpace))) {
                this.openWorkspace(this.m_oState.params.workSpace);
                //this.m_oActiveWorkspace = this.m_oConstantsService.getActiveWorkspace();
            }
            else {

                //TODO CREATE NEW WORKSPACE OR GO HOME
            }
        }
        else {
            this.m_oProcessesLaunchedService.loadProcessesFromServer(this.m_oActiveWorkspace.workspaceId);
            this.getProductListByWorkspace();
        }
        this.m_sDownloadFilePath = "";

        // Rabbit subscription
        //var m_oSubscription = {};


        // Self reference for callbacks
        var oController = this;

        //Set default value tree
        this.m_oTree = null;//IMPORTANT NOTE: there's a 'WATCH' for this.m_oTree in TREE DIRECTIVE

        /*Start Rabbit WebStomp*/
        this.m_oRabbitStompServive.initWebStomp(this.m_oActiveWorkspace, "EditorController", this);


        // Initialize the map

        //oMapService.initMap('wasdiMap');
        oMapService.initMapEditor('wasdiMap');
        oMapService.removeLayersFromMap();
        //oMapService.removeBasicMap();
        this.m_oGlobeService.initGlobe('cesiumContainer2');


    }

    /********************METHODS********************/

    EditorController.prototype.openPublishedBandsInTree = function () {

        var treeInst = $('#jstree').jstree(true);
        var m = treeInst._model.data;
        for (var i in m) {
            if (!utilsIsObjectNullOrUndefined(m[i].original) && !utilsIsObjectNullOrUndefined(m[i].original.band) && m[i].original.bPubblish == true) {
                $("#jstree").jstree("_open_to", m[i].id);
            }
        }
    };

    EditorController.prototype.selectNodeByFileNameInTree = function (sFileName) {
        if(utilsIsObjectNullOrUndefined(sFileName) == true)
            return false;
        var treeInst = $('#jstree').jstree(true);
        var m = treeInst._model.data;
        for (var i in m) {
            if (!utilsIsObjectNullOrUndefined(m[i].original)  && m[i].original.fileName == sFileName) {//&& !utilsIsObjectNullOrUndefined(m[i].original.band)
                $("#jstree").jstree(true).deselect_all();
                $("#jstree").jstree(true).select_node(m[i].id,true);
                $("#jstree").jstree(true).open_node(m[i].id,true);
                break;
           }
        }
        return true;
    };

    EditorController.prototype.renameNodeInTree = function (sFileName,sNewNameInput) {
        if((utilsIsObjectNullOrUndefined(sNewNameInput) == true) || (utilsIsStrNullOrEmpty(sNewNameInput) == true))
            return false;

        var treeInst = $('#jstree').jstree(true);
        var m = treeInst._model.data;
        for (var i in m) {
            if (!utilsIsObjectNullOrUndefined(m[i].original) && m[i].original.fileName == sFileName) {
                $("#jstree").jstree(true).rename_node(m[i].id,sNewNameInput);
                break;
            }
        }
        return true;
    };

    /**
     * Handler of the "download" message
     * @param oMessage Received Message
     */
    /* THIS FUNCTION ARE CALLED IN RABBIT SERVICE */
    EditorController.prototype.receivedDownloadMessage = function (oMessage) {
        if (oMessage == null) return;
        if (oMessage.messageResult == "KO") {
            //alert('There was an error in the download');
            utilsVexDialogAlertTop('There was an error in the download');
            return;
        }
        var oController = this;
        this.m_oProductService.addProductToWorkspace(oMessage.payload.fileName, this.m_oActiveWorkspace.workspaceId).success(function (data, status) {
            if (data.boolValue == true) {
                //console.log('Product added to the ws');
                utilsVexDialogAlertBottomRightCorner('Product added to the ws');
                oController.getProductListByWorkspace();//oMessage.payload.name
                oController.m_oLastDownloadedProduct = oMessage.payload.fileName; //the m_oLastDownloadedProduct will be select & open in jstree

                //oController.selectNodeByFileNameInTree(oMessage.payload.fileName);
                //TODO SELECT NEW NODE

                // $("#jstree").on('loaded.jstree', function(e, data) {
                //     //$treeview.jstree(true).select_all();
                //     oController.selectNodeByFileNameInTree(oMessage.payload.fileName);
                //
                // });

                // $('#jstree').jstree(true).select_all();
                // $('#jstree').jstree(true).select_node(oMessage.payload.fileName);
                // $('#jstree').jstree(true).select_node(oMessage.payload.name);
                // var oNode = $('#jstree').jstree(true).get_node(oMessage.payload.name);
                //  $('#jstree').jstree(true).select_node('mn1');
            }
            else {
                utilsVexDialogAlertTop("Error in add product to workspace");
            }

        }).error(function (data, status) {
            utilsVexDialogAlertTop('Error adding product to the ws')
        });

    }

    EditorController.prototype.receivedTerrainMessage = function (oMessage) {
        this.receivedDownloadMessage(oMessage);
    };


    /**
     * Handler of the "publish" message
     * @param oMessage
     */
    /* THIS FUNCTION ARE CALLED IN RABBIT SERVICE */
    EditorController.prototype.receivedPublishMessage = function (oMessage) {
        if (oMessage == null) return;
        if (oMessage.messageResult == "KO") {
            //alert('There was an error in the publish');
            utilsVexDialogAlertTop('There was an error in the publish');
            return;
        }

    }


    /**
     * Handler of the "PUBLISHBAND" message
     * @param oMessage
     */
    /* THIS FUNCTION ARE CALLED IN RABBIT SERVICE */
    EditorController.prototype.receivedPublishBandMessage = function (oLayer) {

        if (utilsIsObjectNullOrUndefined(oLayer)) {
            console.log("Error LayerID is empty...");
            return false;
        }

        //add layer in list


        // check if the background is grey or there is a map
        if (this.m_bIsVisibleMapOfLeaflet == true) {
            //if there is a map, add layers to it
            this.addLayerMap2D(oLayer.layerId);
            this.addLayerMap3D(oLayer.layerId);
            this.m_aoLayersList.push(oLayer);

            if (!utilsIsStrNullOrEmpty(oLayer.geoserverBoundingBox))//if there isn't boundiBox is impossible do zoom
            {
                var oBounds = JSON.parse(oLayer.geoserverBoundingBox);
                this.m_oGlobeService.zoomOnLayerBoundingBox([oBounds.minx, oBounds.miny, oBounds.maxx, oBounds.maxy]);

                //this.m_oMapService.zoomOnBounds([oBounds.minx,oBounds.miny,oBounds.maxx,oBounds.maxy]);
                /*Zoom on layer*/
                var oMap = this.m_oMapService.getMap();
                var corner1 = L.latLng(oBounds.maxy, oBounds.maxx),
                    corner2 = L.latLng(oBounds.miny, oBounds.minx),
                    bounds = L.latLngBounds(corner1, corner2);
                oMap.fitBounds(bounds);
            }

        }
        else {
            //if the backgrounds is grey
            // remove all others bands in tree - map
            var iNumberOfLayers = this.m_aoLayersList.length;
            for (var iIndexLayer = 0; iIndexLayer < iNumberOfLayers; iIndexLayer++) {
                //check if there is layerId if there isn't the layer was added by get capabilities
                if (!utilsIsObjectNullOrUndefined(this.m_aoLayersList[iIndexLayer].layerId)) {
                    var oNode = $('#jstree').jstree(true).get_node(this.m_aoLayersList[iIndexLayer].layerId);
                    oNode.original.bPubblish = false;
                    $('#jstree').jstree(true).set_icon(this.m_aoLayersList[iIndexLayer].layerId, 'assets/icons/uncheck_20x20.png');
                }
            }
            this.m_aoLayersList = [];
            this.m_oMapService.removeLayersFromMap();
            this.m_oGlobeService.clearGlobe();
            this.m_oGlobeService.initGlobe('cesiumContainer2');

            // so add the new bands
            // and the bounding box in cesium
            var aBounds = JSON.parse("[" + oLayer.boundingBox + "]");

            for (var iIndex = 0; iIndex < aBounds.length - 1; iIndex = iIndex + 2) {
                var iSwap;
                iSwap = aBounds[iIndex];
                aBounds[iIndex] = aBounds[iIndex + 1];
                aBounds[iIndex + 1] = iSwap;
            }

            //this.m_oGlobeService.addRectangleOnGlobeBoundingBox(aBounds);

            if(aBounds.length  > 1)
                this.m_oGlobeService.addRectangleOnGlobeParamArray(aBounds);
            this.addLayerMap2D(oLayer.layerId);

            this.m_aoLayersList.push(oLayer);
            this.zoomOnLayer2DMap(oLayer.layerId);
            this.zoomOnLayer3DGlobe(oLayer.layerId);//TODO RESOLVE PROBLeM WITH 3D zoom
        }


        /*CHANGE TREE */
        var oNode = $('#jstree').jstree(true).get_node(oLayer.layerId);
        oNode.original.bPubblish = true;
        $('#jstree').jstree(true).set_icon(oLayer.layerId, 'assets/icons/check_20x20.png');
        // var oNodet = $('#jstree').jstree(true).get_node(oLayer.layerId);
        //$('#jstree').jstree(true).set_icon(oLayer.layerId, 'assets/icons/check.png');
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
    EditorController.prototype.addLayerMap2D = function (sLayerId) {
        //
        var oMap = this.m_oMapService.getMap();
        var sUrl = this.m_oConstantsService.getWmsUrlGeoserver();//'http://localhost:8080/geoserver/ows?'

        //var wmsLayer = L.tileLayer.wms(sUrl, {
        //    layers: 'wasdi:' + sLayerId,
        //    format: 'image/png',
        //    transparent: true,
        //    noWrap:true
        //});

        var wmsLayer = L.tileLayer.betterWms(sUrl, {
            layers: 'wasdi:' + sLayerId,
            format: 'image/png',
            transparent: true,
            noWrap: true
        });
        wmsLayer.setZIndex(1000);//it set the zindex of layer in map
        wmsLayer.addTo(oMap);


        //
        //var source = L.WMS.source(sUrl, {
        //    layers: 'wasdi:' + sLayerId,
        //    format: 'image/png',
        //    transparent: true,
        //    noWrap:true
        //});
        //source.getLayer('wasdi:' + sLayerId).addTo(oMap);


        //var MySource = L.WMS.Source.extend({
        //    'ajax': function(url, callback) {
        //        $.ajax(url, {
        //            'context': this,
        //            'success': function(result) {
        //                callback.call(this, result);
        //            }
        //        });
        //    },
        //    'showFeatureInfo': function(latlng, info) {
        //        $('.output').html(info);
        //    }
        //});

    }

    ///**
    // * Add layer for Cesium Globe
    // * @param sLayerId
    // */
    EditorController.prototype.addLayerMap3D = function (sLayerId) {
        var oGlobeLayers = this.m_oGlobeService.getGlobeLayers();
        var sUrlGeoserver = this.m_oConstantsService.getWmsUrlGeoserver();
        var oWMSOptions = { // wms options
            transparent: true,
            format: 'image/png',
            //crossOriginKeyword: null
        };//crossOriginKeyword: null

        // WMS get GEOSERVER
        var oProvider = new Cesium.WebMapServiceImageryProvider({
            url: sUrlGeoserver,
            layers: 'wasdi:' + sLayerId,
            parameters: oWMSOptions,

        });
        oGlobeLayers.addImageryProvider(oProvider);
        //this.test=oGlobeLayers.addImageryProvider(oProvider);
    }

    /**
     *
     * @param sLayerId
     */
    EditorController.prototype.addLayerMap3DByServer = function (sLayerId,sServer) {

        if(sServer == null)
            return false;

        var oGlobeLayers=this.m_oGlobeService.getGlobeLayers();
        var sUrlGeoserver = sServer;
        var oWMSOptions= { // wms options
            transparent: true,
            format: 'image/png',
            //crossOriginKeyword: null
        };//crossOriginKeyword: null

        // WMS get GEOSERVER
        var oProvider = new Cesium.WebMapServiceImageryProvider({
            url : sUrlGeoserver,
            layers:'wasdi:' + sLayerId,
            parameters : oWMSOptions,

        });
        oGlobeLayers.addImageryProvider(oProvider);
        //this.test=oGlobeLayers.addImageryProvider(oProvider);
    }

    /**
     *
     * @param sLayerId
     */
    EditorController.prototype.addLayerMap2DByServer = function (sLayerId,sServer) {
        //
        if(sServer == null)
            return false;
        var oMap = this.m_oMapService.getMap();
        var sUrl = sServer//'http://localhost:8080/geoserver/ows?'


        var wmsLayer = L.tileLayer.betterWms(sUrl, {
            layers: 'wasdi:' + sLayerId,
            format: 'image/png',
            transparent: true,
            noWrap:true
        });
        wmsLayer.setZIndex(1000);//it set the zindex of layer in map
        wmsLayer.addTo(oMap);
        return true;
    }
    /**
     * Call Download Image Service
     * @param sUrl
     */
    EditorController.prototype.downloadEOImage = function (sUrl) {
        this.m_oFileBufferService.download(sUrl, this.m_oActiveWorkspace.workspaceId).success(function (data, status) {
            utilsVexDialogAlertBottomRightCorner('downloading');
            //console.log('downloading');
        }).error(function (data, status) {
            utilsVexDialogAlertTop('download error');
            //console.log('download error');
        });
    }

    /**
     * Call publish service
     * @param sUrl
     */
    EditorController.prototype.publish = function (sUrl) {
        this.m_oFileBufferService.publish(sUrl, this.m_oActiveWorkspace.workspaceId).success(function (data, status) {
            utilsVexDialogAlertBottomRightCorner('publishing');
            //console.log('publishing');
        }).error(function (data, status) {
            utilsVexDialogAlertTop('publish error');
            //console.log('publish error');
        });
    }

    /**
     * Get a list of product items with only name and index linked to the m_aoProducts ProductViewModel array
     * @returns {Array}
     */
    EditorController.prototype.getProductList = function () {
        var aoProductItems = [];

        var iProductsCount = this.m_aoProducts.length;

        for (var i = 0; i < iProductsCount; i++) {
            var oProduct = this.m_aoProducts[i];

            var oProductItem = [];
            oProductItem.name = oProduct.name;
            oProductItem.productFriendlyName = oProduct.productFriendlyName;
            oProductItem.fileName = oProduct.fileName;
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

        if (oProductItem.index >= iProductsCount) return asBands;

        var oProduct = this.m_aoProducts[oProductItem.index];

        var aoBands = oProduct.bandsGroups.bands;
        if (!utilsIsObjectNullOrUndefined(aoBands))
            var iBandCount = aoBands.length;
        else
            var iBandCount = 0;

        for (var i = 0; i < iBandCount; i++) {
            var oBandItem = {};
            oBandItem.name = aoBands[i].name;
            oBandItem.productName = oProductItem.name;
            oBandItem.productIndex = oProductItem.index;

            asBands.push(oBandItem);
        }

        return asBands;
    }

    // OPEN BAND
    // oIdBandNodeInTree is the node id in tree, in some case when the page is reload
    // we need know the node id for change the icon
    EditorController.prototype.openBandImage = function (oBand, oIdBandNodeInTree) {
        var oController = this;
        var sFileName = this.m_aoProducts[oBand.productIndex].fileName;

        this.m_oFileBufferService.publishBand(sFileName, this.m_oActiveWorkspace.workspaceId, oBand.name).success(function (data, status) {
            var oDialog = utilsVexDialogAlertBottomRightCorner('publishing band ' + oBand.name);
            utilsVexCloseDialogAfterFewSeconds(3000, oDialog);
            //console.log('publishing band ' + oBand.name);
            if (!utilsIsObjectNullOrUndefined(data) && data.messageResult != "KO" && utilsIsObjectNullOrUndefined(data.messageResult)) {
                /*if the band was published*/
                if (data.messageCode == "PUBLISHBAND")
                    oController.receivedPublishBandMessage(data.payload);
                //else
                //    oController.m_oProcessesLaunchedService.loadProcessesFromServer(oController.m_oActiveWorkspace.workspaceId);
                //oController.m_oProcessesLaunchedService.addProcessesByLocalStorage(oBand.productName + "_" + oBand.name,
                //                                                    oIdBandNodeInTree,
                //                                                    oController.m_oProcessesLaunchedService.getTypeOfProcessPublishingBand(),
                //                                                    oController.m_oActiveWorkspace.workspaceId,
                //                                                    oController.m_oUser.userId);

                /*{processName:oBand.productName + "_" + oBand.name,
                 nodeId:oIdBandNodeInTree,
                 typeOfProcess:oController.m_oProcessesLaunchedService.getTypeOfProcessPublishingBand()}
                 *
                 * */
                //else
                //    oController.pushProcessInListOfRunningProcesses(oBand.productName + "_" + oBand.name,oIdBandNodeInTree);
                //TODO PUSH PROCESS WITH SERVICE
            }
            else {
                //TODO ERROR
                utilsVexDialogAlertTop("Error in publish band");
                //console.log("Error in publish band");
            }

        }).error(function (data, status) {
            console.log('publish band error');
            utilsVexDialogAlertTop("Error in publish band");
            //TODO ERROR
        });
    }

    //REMOVE BAND
    EditorController.prototype.removeBandImage = function (oBand) {
        if (utilsIsObjectNullOrUndefined(oBand) == true) {
            console.log("Error in removeBandImage")
            return false;
        }

        var oController = this;
        var sLayerId = "wasdi:" + oBand.productName + "_" + oBand.name;

        var oMap2D = oController.m_oMapService.getMap();
        var oGlobeLayers = oController.m_oGlobeService.getGlobeLayers();


        //remove layer in 2D map
        oMap2D.eachLayer(function (layer) {
            if (utilsIsStrNullOrEmpty(sLayerId) == false && layer.options.layers == sLayerId) {
                //oController.m_oMapService.removeLayerFromMap(layer)
                oMap2D.removeLayer(layer);

            }
        });

        //remove layer in 3D globe
        var oLayer = null;
        var bCondition = true;
        var iIndexLayer = 0;


        if (this.m_bIsVisibleMapOfLeaflet == true) {
            while (bCondition) {
                oLayer = oGlobeLayers.get(iIndexLayer);

                if (utilsIsStrNullOrEmpty(sLayerId) == false && utilsIsObjectNullOrUndefined(oLayer) == false
                    && oLayer.imageryProvider.layers == sLayerId) {
                    bCondition = false;
                    oLayer = oGlobeLayers.remove(oLayer);
                }
                iIndexLayer++;
            }

        }
        else {
            this.m_oGlobeService.clearGlobe();
            this.m_oGlobeService.initGlobe('cesiumContainer2');
        }

        //Remove layer from layers list
        var iLenghtLayersList;
        if (utilsIsObjectNullOrUndefined(oController.m_aoLayersList))
            iLenghtLayersList = 0;
        else
            iLenghtLayersList = oController.m_aoLayersList.length;

        for (var iIndex = 0; iIndex < iLenghtLayersList; ) {
            if (utilsIsStrNullOrEmpty(sLayerId) == false && utilsIsSubstring(sLayerId, oController.m_aoLayersList[iIndex].layerId)) {
                oController.m_aoLayersList.splice(iIndex,1);
                iLenghtLayersList--;
            }
            else
            {
                iIndex++;
            }

        }
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
    EditorController.prototype.generateTree = function (sSelectedNodeInput) {//sSelectedNodeInput
        var oController = this;
        var oTree =
            {
                'core': {'data': [], "check_callback": true},
                "state" : { "key" : "state_tree" },
                "plugins": ["contextmenu","state"],  // all plugin i use
                "contextmenu": { // my right click menu
                    "items": function ($node) {

                        //only the band has property $node.original.band
                        var oReturnValue = null;
                        if (utilsIsObjectNullOrUndefined($node.original.band) == false && $node.original.bPubblish == true) {
                            //******************************** BAND *************************************
                            var oBand = $node.original.band;

                            oReturnValue =
                                {
                                    "Zoom2D": {
                                        "label": "Zoom Band 2D Map",
                                        "action": function (obj) {
                                            if (utilsIsObjectNullOrUndefined(oBand) == false)
                                                oController.zoomOnLayer2DMap(oBand.productName + "_" + oBand.name);
                                        }
                                    },//TODO
                                    //"Zoom3D" : {
                                    //    "label" : "Zoom Band 3D Map",
                                    //    "action" : function (obj) {
                                    //        if(utilsIsObjectNullOrUndefined(oBand) == false)
                                    //            oController.zoomOnLayer3DGlobe(oBand.productName+"_"+oBand.name);
                                    //    }
                                    //}

                                };
                        }

                        //only products has $node.original.fileName
                        if (utilsIsObjectNullOrUndefined($node.original.fileName) == false) {
                            //***************************** PRODUCT ********************************************
                            oReturnValue =
                                {

                                    "DeleteProduct": {
                                        "label": "Delete Product",
                                        "action": function (obj) {

                                            utilsVexDialogConfirmWithCheckBox("Deleting product. Are you sure?", function (value) {
                                                var bDeleteFile = false;
                                                var bDeleteLayer = false;
                                                if (value) {
                                                    if (value.files == 'on')
                                                        bDeleteFile = true;
                                                    if (value.geoserver == 'on')
                                                        bDeleteLayer = true;

                                                    oController.m_oProductService.deleteProductFromWorkspace($node.original.fileName, oController.m_oActiveWorkspace.workspaceId, bDeleteFile, bDeleteLayer)
                                                        .success(function (data) {
                                                            //reload product list
                                                            oController.getProductListByWorkspace();

                                                        }).error(function (error) {

                                                    });
                                                }

                                            });
                                        }
                                    },

                                    "Merge": {
                                        "label": "Merge ",
                                        "action": function (obj) {
                                            //$node.original.fileName;
                                            oController.openMergeDialog($node.original.fileName);
                                        }
                                    },
                                    "Properties": {
                                        "label": "Properties ",
                                        "icon":"fa fa-info",
                                        "action": function (obj) {
                                            //$node.original.fileName;
                                            if( (utilsIsObjectNullOrUndefined($node.original.fileName) == false) && (utilsIsStrNullOrEmpty($node.original.fileName) == false) )
                                            {
                                                var iNumberOfProdcuts = oController.m_aoProducts.length;
                                                for(var iIndexProduct = 0; iIndexProduct < iNumberOfProdcuts ; iIndexProduct++)
                                                {
                                                    if( oController.m_aoProducts[iIndexProduct].fileName == $node.original.fileName)
                                                    {
                                                        oController.openProductInfoDialog(oController.m_aoProducts[iIndexProduct]);
                                                        break;
                                                    }

                                                }

                                            }
                                        }
                                    },
                                    "Rename": {
                                        "label": "Rename",
                                        "action": function (obj) {
                                            //$node.original.fileName;
                                            var oCallback = function(value){
                                                if((utilsIsObjectNullOrUndefined(value.renameProduct) == true) ||(utilsIsStrNullOrEmpty(value.renameProduct) == true))
                                                    return false;
                                                var bResult = oController.renameNodeInTree($node.original.fileName,value.renameProduct);
                                                if(bResult == false)
                                                    console.log("Error: it's impossible rename the product");
                                            }

                                            utilsVexDialogChangeNameInTree("Insert new name",oCallback,$node.original.fileName);
                                            // var bResult = oController.renameNodeInTree($node.original.fileName,"test");
                                            // if(bResult == false)
                                            //     console.log("Error: it's impossible rename the product");
                                        }
                                    },
                                    //SUBMENU RADAR
                                    "Radar": {
                                        "label": "Radar",
                                        "action": false,
                                        "separator_before":true,
                                        "submenu":
                                            {
                                                //APPLY ORBIT
                                                "ApplyOrbit": {
                                                    "label": "Apply Orbit",
                                                    "action": function (obj) {
                                                        var sSourceFileName = $node.original.fileName;
                                                        var oFindedProduct = oController.findProductByFileName(sSourceFileName);
                                                        var sDestinationFileName = '';

                                                        if(utilsIsObjectNullOrUndefined(oFindedProduct) == false)
                                                            oController.openApplyOrbitDialog(oFindedProduct);

                                                        //oController.m_oSnapOperationService.ApplyOrbit(sSourceFileName, sDestinationFileName, oController.m_oActiveWorkspace.workspaceId)
                                                        //    .success(function (data) {
                                                        //
                                                        //    }).error(function (error) {
                                                        //
                                                        //});
                                                    }
                                                },
                                                "Multilooking": {
                                                    "label": "Multilooking",
                                                    "action": function (obj) {
                                                        var sSourceFileName = $node.original.fileName;
                                                        var sDestinationFileName = '';
                                                        var oFindedProduct = oController.findProductByFileName(sSourceFileName);

                                                        if(utilsIsObjectNullOrUndefined(oFindedProduct) == false)
                                                            oController.openMultilookingDialog(oFindedProduct);
                                                        // oController.m_oSnapOperationService.Multilooking(sSourceFileName, sDestinationFileName, oController.m_oActiveWorkspace.workspaceId)
                                                        //     .success(function (data) {
                                                        //
                                                        //     }).error(function (error) {
                                                        //
                                                        // });
                                                    }
                                                },
                                                //SUB-SUBMENU RADIOMETRIC
                                                "Radiometric": {
                                                    "label": "radiometric",
                                                    "action": false,
                                                    "separator_before":true,
                                                    "submenu":
                                                        {
                                                            "Calibrate": {
                                                                "label": "Calibrate",
                                                                "action": function (obj) {
                                                                    var sSourceFileName = $node.original.fileName;
                                                                    var sDestinationFileName = '';
                                                                    var oFindedProduct = oController.findProductByFileName(sSourceFileName);

                                                                    if(utilsIsObjectNullOrUndefined(oFindedProduct) == false)
                                                                        oController.openRadiometricCalibrationDialog(oFindedProduct);

                                                                    // oController.m_oSnapOperationService.Calibrate(sSourceFileName, sDestinationFileName, oController.m_oActiveWorkspace.workspaceId)
                                                                    //     .success(function (data) {
                                                                    //
                                                                    //     }).error(function (error) {
                                                                    //
                                                                    // });
                                                                }
                                                            },
                                                        }
                                                },
                                                //SUB-SUBMENU GEOMETRIC
                                                "Geometric": {
                                                    "label": "Geometric",
                                                    "action": false,
                                                    "submenu":
                                                        {
                                                            //SUB-SUB-SUBMENU GEOMETRIC
                                                            "Terrain correction": {
                                                                "label": "Terrain correction",
                                                                "action": false,
                                                                "submenu":
                                                                    {
                                                                        "Range Doppler Terrain Correction": {
                                                                            "label": "Range Doppler Terrain Correction",
                                                                            "action": function (obj) {
                                                                                var sSourceFileName = $node.original.fileName;
                                                                                var sDestinationFileName = '';
                                                                                var oFindedProduct = oController.findProductByFileName(sSourceFileName);

                                                                                if(utilsIsObjectNullOrUndefined(oFindedProduct) == false)
                                                                                    oController.rangeDopplerTerrainCorrectionDialog(oFindedProduct);
                                                                                // oController.m_oSnapOperationService.TerrainCorrection(sSourceFileName, sDestinationFileName, oController.m_oActiveWorkspace.workspaceId)
                                                                                //     .success(function (data) {
                                                                                //
                                                                                //     }).error(function (error) {
                                                                                //
                                                                                // });
                                                                            }
                                                                        },
                                                                    }
                                                            },

                                                        }
                                                },

                                            }
                                    },

                                    //SUBMENU OPTICAL
                                    "Optical": {
                                        "label": "Optical",
                                        "action": false,
                                        "submenu":
                                            {
                                                "NDVI": {
                                                    "label": "NDVI",
                                                    "action": function (obj) {
                                                        var sSourceFileName = $node.original.fileName;
                                                        var sDestinationFileName = '';
                                                        var oFindedProduct = oController.findProductByFileName(sSourceFileName);

                                                        if(utilsIsObjectNullOrUndefined(oFindedProduct) == false)
                                                            oController.openNDVIDialog(oFindedProduct);
                                                        // oController.m_oSnapOperationService.NDVI(sSourceFileName, sDestinationFileName, oController.m_oActiveWorkspace.workspaceId)
                                                        //     .success(function (data) {
                                                        //
                                                        //     }).error(function (error) {
                                                        //
                                                        // });
                                                    }
                                                },

                                            }
                                    },

                                };
                        }

                        return oReturnValue;
                    }
                }
            }


        var productList = this.getProductList();
      //  var productList = this.m_aoProducts;
        //for each product i generate sub-node
        for (var iIndexProduct = 0; iIndexProduct < productList.length; iIndexProduct++) {

            //product node
            var oNode = new Object();
            if(utilsIsObjectNullOrUndefined( productList[iIndexProduct].productFriendlyName) == false)
                oNode.text = productList[iIndexProduct].productFriendlyName;//LABEL NODE
            else
                oNode.text = productList[iIndexProduct].name;//LABEL NODE

            //usually the selected node is the node just download
            // if( (utilsIsObjectNullOrUndefined(sSelectedNodeInput)==false) && (utilsIsStrNullOrEmpty(sSelectedNodeInput)==false) && productList[iIndexProduct].name == sSelectedNodeInput)
            //     oNode.state = {"opened" :true, "disabled":false, "selected" :true };//'opened' :true, 'disabled':false,
            // else
            //     oNode.state = {"opened" :false, "disabled":false, "selected" :true };//default state

            // oNode.state = { "opened" :false,"disabled":false,"selected " :true };//

            oNode.fileName = productList[iIndexProduct].fileName;
            oNode.children = [{"text": "metadata", "icon": "assets/icons/folder_20x20.png"}, {
                "text": "Bands",
                "icon": "assets/icons/folder_20x20.png",
                "children": []
            }];//CHILDREN
            oNode.icon = "assets/icons/product_20x20.png";
            oTree.core.data.push(oNode);

            var oaBandsItems = this.getBandsForProduct(productList[iIndexProduct]);

            for (var iIndexBandsItems = 0; iIndexBandsItems < oaBandsItems.length; iIndexBandsItems++) {
                var oNode = new Object();
                oNode.text = oaBandsItems[iIndexBandsItems].name;//LABEL NODE
                oNode.band = oaBandsItems[iIndexBandsItems];//BAND
                oNode.icon = "assets/icons/uncheck_20x20.png";

                //generate id for bands => ProductName+BandName
                oNode.id = oTree.core.data[iIndexProduct].text + "_" + oaBandsItems[iIndexBandsItems].name;
                oNode.bPubblish = false;
                oTree.core.data[iIndexProduct].children[1].children.push(oNode);
            }

        }

        return oTree;
    }


    /**
     *
     * @param sName == selected node (it's the name node with blue background)
     */


    EditorController.prototype.getProductListByWorkspace = function () {//sName
        var oController = this;
        oController.m_aoProducts = [];

        this.m_oProductService.getProductListByWorkspace(oController.m_oActiveWorkspace.workspaceId).success(function (data, status) {
            if (data != null) {
                if (data != undefined) {
                    //push all products
                    for (var iIndex = 0; iIndex < data.length; iIndex++) {
                        //check if friendly file name isn't null
                        if(utilsIsObjectNullOrUndefined(data[iIndex].productFriendlyName) == true)
                        {
                            data[iIndex].productFriendlyName =  data[iIndex].name;
                        }
                        oController.m_aoProducts.push(data[iIndex]);
                    }

                    // if( (utilsIsObjectNullOrUndefined(sName) == false) || (utilsIsStrNullOrEmpty(sName) == false) )
                    //     oController.m_oTree = oController.generateTree(sName); // i need to make the tree after the products are loaded
                    // else
                        oController.m_oTree = oController.generateTree();   // i need to make the tree after the products are loaded


                    //oController.m_oScope.$apply();
                }
            }
        }).error(function (data, status) {
            utilsVexDialogAlertTop('Error reading product list');
            //console.log('Error reading product list');
        });
    }
    /* Search element in tree
     * */

    //---------------- OPENWORKSPACE -----------------------
    // ReLOAD workspace when reload page
    EditorController.prototype.openWorkspace = function (sWorkspaceId) {

        var oController = this;

        this.m_oWorkspaceService.getWorkspaceEditorViewModel(sWorkspaceId).success(function (data, status) {
            if (data != null) {
                if (data != undefined) {
                    oController.m_oConstantsService.setActiveWorkspace(data);
                    oController.m_oActiveWorkspace = oController.m_oConstantsService.getActiveWorkspace();
                    /*Start Rabbit WebStomp*/
                    oController.m_oRabbitStompServive.initWebStomp(oController.m_oActiveWorkspace, "EditorController", oController);
                    oController.getProductListByWorkspace();
                    oController.m_oProcessesLaunchedService.loadProcessesFromServer(oController.m_oActiveWorkspace.workspaceId);
                }
            }
        }).error(function (data, status) {
            //alert('error');
            utilsVexDialogAlertTop('error Impossible get workspace in editorController.js')
        });
    }

    /**************** MAP 3D/2D MODE ON/OFF  (SWITCH)*************************/
    EditorController.prototype.onClickChangeMap = function () {
        var oController = this;

        oController.m_b2DMapModeOn = !oController.m_b2DMapModeOn;
        oController.m_b3DMapModeOn = !oController.m_b3DMapModeOn;

        //3D MAP
        if (oController.m_b2DMapModeOn == false && oController.m_b3DMapModeOn == true) {
            oController.m_oMapService.clearMap();
            oController.m_oGlobeService.clearGlobe();
            oController.m_oGlobeService.initGlobe('cesiumContainer');
            oController.m_oMapService.initMap('wasdiMap2');

            //setTimeout(function(){ oController.m_oMapService.getMap().invalidateSize()}, 400);
            oController.delayInLoadMaps();
            // Load Layers
            oController.loadLayersMap2D();
            oController.loadLayersMap3D();
        }//2D MAP
        else if (oController.m_b2DMapModeOn == true && oController.m_b3DMapModeOn == false) {
            oController.m_oMapService.clearMap();
            oController.m_oGlobeService.clearGlobe();
            oController.m_oMapService.initMap('wasdiMap');
            oController.m_oGlobeService.initGlobe('cesiumContainer2');
            //setTimeout(function(){ oController.m_oMapService.getMap().invalidateSize()}, 400);
            oController.delayInLoadMaps();
            // Load Layers
            oController.loadLayersMap2D();
            oController.loadLayersMap3D();
        }

    }

    EditorController.prototype.delayInLoadMaps = function () {
        var oController = this;
        setTimeout(function () {
            oController.m_oMapService.getMap().invalidateSize()
        }, 400);
    }

    //Use it when switch map 2d/3d
    EditorController.prototype.loadLayersMap2D = function () {
        var oController = this;
        if (utilsIsObjectNullOrUndefined(oController.m_aoLayersList)) {
            console.log('Error in layers list');
            return false;
        }

        for (var iIndexLayers = 0; iIndexLayers < oController.m_aoLayersList.length; iIndexLayers++) {
            if (!utilsIsObjectNullOrUndefined(oController.m_aoLayersList[iIndexLayers].layerId))//check if layer was downloaded from geoserver(get capabilities)
                oController.addLayerMap2D(oController.m_aoLayersList[iIndexLayers].layerId);
            else
                oController.addLayerMap2DByServer(oController.m_aoLayersList[iIndexLayers].Title,oController.m_aoLayersList[iIndexLayers].sServerLink);
        }
    }

    //Use it when switch map 2d/3d
    EditorController.prototype.loadLayersMap3D = function () {
        var oController = this;
        if (utilsIsObjectNullOrUndefined(oController.m_aoLayersList)) {
            console.log('Error in layers list');
            return false;
        }

        for (var iIndexLayers = 0; iIndexLayers < oController.m_aoLayersList.length; iIndexLayers++) {
            if (!utilsIsObjectNullOrUndefined(oController.m_aoLayersList[iIndexLayers].layerId))//check if layer was downloaded from geoserver(get capabilities)
                oController.addLayerMap3D(oController.m_aoLayersList[iIndexLayers].layerId);
            else
                oController.addLayerMap3DByServer(oController.m_aoLayersList[iIndexLayers].Title,oController.m_aoLayersList[iIndexLayers].sServerLink);
        }
    }

    /*
     synchronize the 3D Map and 2D map
     */
    EditorController.prototype.synchronize3DMap = function () {

        var oMap = this.m_oMapService.getMap();
        var oBoundsMap = oMap.getBounds();
        /* it take the edge of 2d map*/

        var oGlobe = this.m_oGlobeService.getGlobe();
        /* set view of globe*/
        oGlobe.camera.setView({
            destination: Cesium.Rectangle.fromDegrees(oBoundsMap.getWest(), oBoundsMap.getSouth(), oBoundsMap.getEast(), oBoundsMap.getNorth()),
            orientation: {
                heading: 0.0,
                pitch: -Cesium.Math.PI_OVER_TWO,
                roll: 0.0
            }

        });

    }
    /*
     synchronize the 2D Map and 3D map
     */
    EditorController.prototype.synchronize2DMap = function () {

        var oMap = this.m_oMapService.getMap();
        //var oGlobe = this.m_oGlobeService.getGlobe();
        var aCenter = this.m_oGlobeService.getMapCenter();
        if (utilsIsObjectNullOrUndefined(aCenter))
            return false;
        oMap.flyTo(aCenter);
        //var oRectangle = oGlobe.scene.camera.computeViewRectangle(oGlobe.scene.globe.ellipsoid);
        //if(utilsIsObjectNullOrUndefined(oRectangle))
        //    return false;
        //// center map
        //var oBoundaries = L.latLngBounds(oRectangle.south,oRectangle.west,oRectangle.north,oRectangle.east);
        //oMap.fitBounds(oBoundaries);
        return true;
    }

    EditorController.prototype.zoomOnLayer3DGlobe = function (oLayerId) {
        if (utilsIsObjectNullOrUndefined(oLayerId))
            return false;
        if (utilsIsObjectNullOrUndefined(this.m_aoLayersList))
            return false;

        var iNumberOfLayers = this.m_aoLayersList.length;

        for (var iIndexLayer = 0; iIndexLayer < iNumberOfLayers; iIndexLayer++) {
            if (this.m_aoLayersList[iIndexLayer].layerId == oLayerId)
                break;
        }

        if (!(iIndexLayer < iNumberOfLayers))//there isn't layer in layerList
            return false;

        if (utilsIsStrNullOrEmpty(this.m_aoLayersList[iIndexLayer].boundingBox))
            return false;
        var aBoundingBox = JSON.parse("[" + this.m_aoLayersList[iIndexLayer].boundingBox + "]");

        if (utilsIsObjectNullOrUndefined(aBoundingBox) == true)
            return false;

        //var aaBounds = [];
        //for (var iIndex = 0; iIndex < aBoundingBox.length - 1; iIndex = iIndex + 2) {
        //    aaBounds.push([aBoundingBox[iIndex + 1], aBoundingBox[iIndex]]);
        //
        //}

        /* set view of globe*/
        //this.m_oGlobeService.zoomOnLayerParamArray(aBoundingBox);

        var aaBounds = [];
        for (var iIndex = 0; iIndex < aBoundingBox.length - 1; iIndex = iIndex + 2) {
            aaBounds.push(new Cesium.Cartographic.fromDegrees(aBoundingBox[iIndex + 1], aBoundingBox[iIndex ]));

        }

        var oGlobe = this.m_oGlobeService.getGlobe();
        var zoom = Cesium.Rectangle.fromCartographicArray(aaBounds);
        oGlobe.camera.setView({
            destination: zoom,
            orientation: {
                heading: 0.0,
                pitch: -Cesium.Math.PI_OVER_TWO,
                roll: 0.0
            }
        });



        //oGlobe.camera.setView({
        //    destination:  Cesium.Rectangle.fromDegrees( oBoundingBox.minx , oBoundingBox.miny , oBoundingBox.maxx,oBoundingBox.maxy),
        //    orientation: {
        //        heading: 0.0,
        //        pitch: -Cesium.Math.PI_OVER_TWO,
        //        roll: 0.0
        //    }
        //
        //});

        //oBoundingBox = [[107.144188,77.391487],[90.454704,78.801079],[93.399925,80.717804],[112.501625,79.084023],[107.144188, 77.391487]];
        //var oCartographic =  [new Cesium.Cartographic(77.391487,107.144188),new Cesium.Cartographic(78.801079,90.454704),new Cesium.Cartographic(80.717804,93.399925),new Cesium.Cartographic(79.084023,112.501625),new Cesium.Cartographic(77.391487,107.144188)]
        //var prova = Cesium.Rectangle.fromCartographicArray(oCartographic);
        //oGlobe.camera.setView({
        //    destination: prova,
        //    orientation: {
        //        heading: 0.0,
        //        pitch: -Cesium.Math.PI_OVER_TWO,
        //        roll: 0.0
        //    }
        //
        //});
        return true;
    }

    EditorController.prototype.zoomOnLayer2DMap = function (oLayerId) {
        if (utilsIsObjectNullOrUndefined(oLayerId))
            return false;
        if (utilsIsObjectNullOrUndefined(this.m_aoLayersList))
            return false
        var iNumberOfLayers = this.m_aoLayersList.length;

        for (var iIndexLayer = 0; iIndexLayer < iNumberOfLayers; iIndexLayer++) {
            if (this.m_aoLayersList[iIndexLayer].layerId == oLayerId)
                break;
        }

        if (!(iIndexLayer < iNumberOfLayers))//there isn't layer in layerList
            return false;

        if (utilsIsStrNullOrEmpty(this.m_aoLayersList[iIndexLayer].geoserverBoundingBox))
            return false;

        var oBoundingBox = JSON.parse(this.m_aoLayersList[iIndexLayer].geoserverBoundingBox);

        if (utilsIsObjectNullOrUndefined(oBoundingBox) == true)
            return false;

        //var aaBounds = [];
        //for( var iIndex = 0; iIndex < aBoundingBox.length-1 ;iIndex = iIndex + 2 )
        //{
        //    aaBounds.push([aBoundingBox[iIndex],aBoundingBox[iIndex+1]]);
        //
        //}
        //this.m_oMapService.zoomOnBounds(oBoundingBox);
        var oMap = this.m_oMapService.getMap();
        var corner1 = L.latLng(oBoundingBox.maxy, oBoundingBox.maxx),
            corner2 = L.latLng(oBoundingBox.miny, oBoundingBox.minx),
            bounds = L.latLngBounds(corner1, corner2);
        oMap.fitBounds(bounds);

        return true;
    }

    EditorController.prototype.addOrRemoveMapLayer = function () {
        this.m_bIsVisibleMapOfLeaflet = !this.m_bIsVisibleMapOfLeaflet;

        //If there is the map or grey background
        if (this.m_bIsVisibleMapOfLeaflet == true) {
            //if there is the map
            //reinitialize the globe
            this.m_oGlobeService.clearGlobe();
            this.m_oGlobeService.initGlobe('cesiumContainer2');

            //add layers in 3d map
            var iNumberOfLayers = this.m_aoLayersList.length;
            for (var iIndexLayer = 0; iIndexLayer < iNumberOfLayers; iIndexLayer++) {
                if (!utilsIsObjectNullOrUndefined(this.m_aoLayersList[iIndexLayer].layerId))//check if the layer was took with get capabilities
                {
                    this.addLayerMap3D(this.m_aoLayersList[iIndexLayer].layerId);//import layer

                    if (!utilsIsStrNullOrEmpty(this.m_aoLayersList[iIndexLayer].geoserverBoundingBox))//it dosen't do the zoom if there isn't bounding box
                    {
                        var oBounds = JSON.parse(this.m_aoLayersList[iIndexLayer].geoserverBoundingBox);
                        //var aBounds = JSON.parse("["+this.m_aoLayersList[iIndexLayer].boundingBox+"]");
                        //
                        //for(var iIndex = 0; iIndex < aBounds.length-1 ;iIndex=iIndex+2)
                        //{
                        //    var iSwap;
                        //    iSwap = aBounds[iIndex];
                        //    aBounds[iIndex] = aBounds[iIndex+1];
                        //    aBounds[iIndex+1] = iSwap;
                        //}

                        //this.m_oGlobeService.zoomOnLayerParamArray(aBounds);

                        this.m_oGlobeService.zoomOnLayerBoundingBox([oBounds.minx, oBounds.miny, oBounds.maxx, oBounds.maxy]);
                    }
                }
                else {
                    this.addLayerMap3D(this.m_aoLayersList[iIndexLayer].Title);//get capabilities layer
                    var oBounds = (this.m_aoLayersList[0].BoundingBox[0].extent);
                    if (!utilsIsObjectNullOrUndefined(oBounds))//check if possible do zoom
                    {
                        this.m_oGlobeService.zoomOnLayerBoundingBox([oBounds[0], oBounds[1], oBounds[2], oBounds[3]]);
                    }
                }
            }

            this.m_oMapService.setBasicMap();
        }
        else {
            //if there'isnt the map (grey background)

            var iNumberOfLayers = this.m_aoLayersList.length;
            if (iNumberOfLayers == 1)//if there is only one layer
            {
                //save layer and remove background map
                this.m_oMapService.removeBasicMap();
                //reinitialize the globe
                this.m_oGlobeService.clearGlobe();
                this.m_oGlobeService.initGlobe('cesiumContainer2');

                //insert in globe rectangle

                if (!utilsIsObjectNullOrUndefined(this.m_aoLayersList[0].boundingBox)) //if there is .boundingBox property, the layer was downloaded by copernicus server
                {

                    //var oBounds = JSON.parse(this.m_aoLayersList[0].boundingBox);
                    //this.m_oGlobeService.addRectangleOnGlobe([oBounds.minx,oBounds.miny,oBounds.maxx,oBounds.maxy]);
                    //this.m_oGlobeService.zoomOnLayer([oBounds.minx,oBounds.miny,oBounds.maxx,oBounds.maxy]);


                    //var oBoundingBox = [[1.870018854971,1.35073626116],[1.57873240871,1.37533828267],[1.63013621236,1.40879144478],[1.963523770089,1.38027658707],[1.870018854971, 1.35073626116]];
                    //var aoCartographic =  [new Cesium.Cartographic(107.144188,77.391487),new Cesium.Cartographic(90.454704,78.801079),new Cesium.Cartographic(93.399925,80.717804),new Cesium.Cartographic(112.501625,79.084023),new Cesium.Cartographic(107.144188,77.391487)]

                    //var aoCartographic =  [new Cesium.Cartographic(1.35073626116,1.870018854971),new Cesium.Cartographic(1.37533828267,1.57873240871),new Cesium.Cartographic(1.40879144478,1.63013621236),new Cesium.Cartographic(1.38027658707,1.963523770089),new Cesium.Cartographic( 1.35073626116,1.870018854971)]
                    //var aoCartographic =  [new Cesium.Cartographic.fromDegrees(107.144188,77.391487),new Cesium.Cartographic.fromDegrees(90.454704,78.801079),new Cesium.Cartographic.fromDegrees(93.399925,80.717804),new Cesium.Cartographic.fromDegrees(112.501625,79.084023),new Cesium.Cartographic.fromDegrees(107.144188,77.391487)]

                    //var aoCartographic =  [new Cesium.Cartographic.fromDegrees(13.643888,43.863701),new Cesium.Cartographic.fromDegrees(10.388990,44.271336),new Cesium.Cartographic.fromDegrees(10.717404,45.770004),new Cesium.Cartographic.fromDegrees(14.057657,45.362556),new Cesium.Cartographic.fromDegrees(13.643888,43.863701)]
                    var aBounds = JSON.parse("[" + this.m_aoLayersList[0].boundingBox + "]");
                    if(aBounds.length > 1)
                    {
                        for (var iIndex = 0; iIndex < aBounds.length - 1; iIndex = iIndex + 2)
                        {
                            var iSwap;
                            iSwap = aBounds[iIndex];
                            aBounds[iIndex] = aBounds[iIndex + 1];
                            aBounds[iIndex + 1] = iSwap;
                        }
                        this.m_oGlobeService.addRectangleOnGlobeParamArray(aBounds);
                        //this.m_oGlobeService.zoomOnLayerParamArray(aBounds);//TODO RESOLVE PROBELM WITH 3D zoom
                    }
                }

                if (!utilsIsObjectNullOrUndefined(this.m_aoLayersList[0].BoundingBox)) //if there is BoundingBox.extent property, the layer was downloaded by external server
                {
                    //TODO CHECK IF .BoundingBox[0] it's right
                    var oBounds = (this.m_aoLayersList[0].BoundingBox[0].extent);
                    this.m_oGlobeService.addRectangleOnGlobeBoundingBox([oBounds[0], oBounds[1], oBounds[2], oBounds[3]]);
                    this.m_oGlobeService.zoomOnLayerBoundingBox([oBounds[0], oBounds[1], oBounds[2], oBounds[3]]);//TODO
                }

            }

            if (iNumberOfLayers > 1)//if there is 2 or plus layers remove all layer
            {
                //remove all layers
                var oController = this;

                var oCallback = function (value) {
                    if (value) {
                        //clear 2d map
                        oController.m_oMapService.removeLayersFromMap();
                        //reinitialize the globe
                        oController.m_oGlobeService.clearGlobe();
                        oController.m_oGlobeService.initGlobe('cesiumContainer2');
                        //for each layer update the node in tree

                        for (var iIndexLayer = 0; iIndexLayer < iNumberOfLayers; iIndexLayer++) {
                            if (!utilsIsObjectNullOrUndefined(oController.m_aoLayersList[iIndexLayer].layerId)) {
                                var oNode = $('#jstree').jstree(true).get_node(oController.m_aoLayersList[iIndexLayer].layerId);
                                oNode.original.bPubblish = false;
                                $('#jstree').jstree(true).set_icon(oController.m_aoLayersList[iIndexLayer].layerId, 'assets/icons/uncheck_20x20.png');
                            }
                        }
                        oController.m_aoLayersList = [];//empty layer list
                        return true;
                    }
                    else {
                        oController.m_bIsVisibleMapOfLeaflet = !oController.m_bIsVisibleMapOfLeaflet;//revert status
                        return false;
                    }
                }

                var bResponse = false;
                if (iNumberOfLayers != 0)
                    bResponse = utilsVexDialogConfirm("Going in Image-Mode: open layers will be closed. Are you sure?", oCallback);//ask user if he want delete layers

            }

            if (iNumberOfLayers == 0)//if there isn't layers
            {
                //clear 2d map
                this.m_oMapService.removeLayersFromMap();
                //reinitialize the globe
                this.m_oGlobeService.clearGlobe();
                this.m_oGlobeService.initGlobe('cesiumContainer2');
            }


        }
    }
    // SHOW MODAL
    EditorController.prototype.openGetCapabilitiesDialog = function () {
        var oController = this
        this.m_oModalService.showModal({
            templateUrl: "dialogs/get_capabilities_dialog/GetCapabilitiesDialog.html",
            controller: "GetCapabilitiesController",
            inputs: {
                extras: oController
            }
        }).then(function (modal) {
            modal.element.modal();
            modal.close.then(function (result) {
                oController.m_oScope.Result = result;
            });
        });

        return true;
    }

    EditorController.prototype.hideOrShowPixelInfo = function () {
        this.m_bIsVisiblePixelInfo = !this.m_bIsVisiblePixelInfo;

        if (this.m_bIsVisiblePixelInfo == true) {
            //$('.leaflet-popup-pane').css({"display":""});
            $('.leaflet-popup-pane').css({"visibility": "visible"});
            /*.leaflet-fade-anim .leaflet-map-pane .leaflet-popup*/
        }
        else {
            //$('.leaflet-popup-pane').css({"display":"none"});
            $('.leaflet-popup-pane').css({"visibility": "hidden"});

        }
    }

    // SHOW MODAL
    EditorController.prototype.openMergeDialog = function (oSelectedProduct) {

        var oController = this;

        this.m_oModalService.showModal({
            templateUrl: "dialogs/merge_products_dialog/MergeProductsDialog.html",
            controller: "MergeProductsController",
            inputs: {
                extras: {
                    SelectedProduct: oSelectedProduct,
                    ListOfProducts: oController.m_aoProducts,
                    WorkSpaceId: oController.m_oActiveWorkspace
                }
            }
        }).then(function (modal) {
            modal.element.modal();
            modal.close.then(function (result) {
                oController.m_oScope.Result = result;
            });
        });

        return true;
    };

    EditorController.prototype.findProductByFileName = function(sFileNameInput)
    {
        if( (utilsIsObjectNullOrUndefined(sFileNameInput) == true) && (utilsIsStrNullOrEmpty(sFileNameInput) == true))
            return null;
        if(utilsIsObjectNullOrUndefined(this.m_aoProducts) == true)
            return null;

        var iNumberOfProducts = this.m_aoProducts.length;
        if(this.m_aoProducts.length == 0)
            return null;

        for(var iIndexProduct = 0; iIndexProduct < iNumberOfProducts; iIndexProduct++)
        {
            if( this.m_aoProducts[iIndexProduct].fileName == sFileNameInput )
            {
                return this.m_aoProducts[iIndexProduct];
            }
        }
        return null;
    }
    //---------------------------------- SHOW MODALS ------------------------------------
    EditorController.prototype.openApplyOrbitDialog = function (oSelectedProduct) {
        var oController = this;
        this.m_oModalService.showModal({
            templateUrl: "dialogs/apply_orbit_operation/ApplyOrbitDialog.html",
            controller: "ApplyOrbitController",
            inputs: {
                extras: {
                    products:oController.m_aoProducts,
                    selectedProduct:oSelectedProduct
                }
            }
        }).then(function (modal) {
            modal.element.modal();
            modal.close.then(function (result) {
                oController.m_oScope.Result = result;
            });
        });

        return true;
    };

    EditorController.prototype.openRadiometricCalibrationDialog = function (oSelectedProduct) {
        var oController = this;
        this.m_oModalService.showModal({
            templateUrl: "dialogs/radiometric_calibration_operation/RadiometricCalibrationDialog.html",
            controller: "RadiometricCalibrationController",
            inputs: {
                extras: {
                    products:oController.m_aoProducts,
                    selectedProduct:oSelectedProduct
                }
            }
        }).then(function (modal) {
            modal.element.modal();
            modal.close.then(function (result) {
                oController.m_oScope.Result = result;
            });
        });

        return true;
    }
    EditorController.prototype.openMultilookingDialog = function (oSelectedProduct) {
        var oController = this;
        this.m_oModalService.showModal({
            templateUrl: "dialogs/multilooking_operation/MultilookingDialog.html",
            controller: "MultilookingController",
            inputs: {
                extras: {
                    products:oController.m_aoProducts,
                    selectedProduct:oSelectedProduct
                }
            }
        }).then(function (modal) {
            modal.element.modal();
            modal.close.then(function (result) {
                oController.m_oScope.Result = result;
            });
        });

        return true;
    }
    EditorController.prototype.openNDVIDialog = function (oSelectedProduct) {
        var oController = this;
        this.m_oModalService.showModal({
            templateUrl: "dialogs/NDVI_operation/NDVIDialog.html",
            controller: "NDVIController",
            inputs: {
                extras: {
                    products:oController.m_aoProducts,
                    selectedProduct:oSelectedProduct
                }
            }
        }).then(function (modal) {
            modal.element.modal();
            modal.close.then(function (result) {
                oController.m_oScope.Result = result;
            });
        });

        return true;
    }
    EditorController.prototype.openProductInfoDialog = function (oProductInput)
    {
        var oController = this;
        this.m_oModalService.showModal({
            templateUrl: "dialogs/product_editor_info/ProductEditorInfoDialog.html",
            controller: "ProductEditorInfoController",
            inputs: {
                extras: {
                    product:oProductInput
                }
            }
        }).then(function (modal) {
            modal.element.modal();
            modal.close.then(function (result) {
                oController.m_oScope.Result = result;
            });
        });

        return true;
    }

    EditorController.prototype.rangeDopplerTerrainCorrectionDialog = function (oSelectedProduct)
    {
        var oController = this;
        this.m_oModalService.showModal({
            templateUrl: "dialogs/range_doppler_terrain_correction_operation/RangeDopplerTerrainCorrectionDialog.html",
            controller: "RangeDopplerTerrainCorrectionController",
            inputs: {
                extras: {
                    products:oController.m_aoProducts,
                    selectedProduct:oSelectedProduct
                }
            }
        }).then(function (modal) {
            modal.element.modal();
            modal.close.then(function (result) {
                oController.m_oScope.Result = result;
            });
        });

        return true;
    }
    /*OPERATION MENU*/
    EditorController.prototype.hideOperationMainBar= function()
    {
        this.m_oAreHideBars.mainBar = true;
    }
    EditorController.prototype.hideOperationRadarBar = function()
    {
        this.m_oAreHideBars.radarBar = true;
    }
    EditorController.prototype.hideOperationOpticalBar= function()
    {
        this.m_oAreHideBars.opticalBar = true;
    }
    EditorController.prototype.showOperationMainBar= function()
    {
        this.m_oAreHideBars.mainBar = false;
    }
    EditorController.prototype.showOperationRadarBar = function()
    {
        this.m_oAreHideBars.radarBar = false;
    }
    EditorController.prototype.showOperationOpticalBar= function()
    {
        this.m_oAreHideBars.opticalBar = false;
    }

    EditorController.prototype.isHiddenOperationMainBar= function()
    {
        return this.m_oAreHideBars.mainBar;
    }
    EditorController.prototype.isHiddenOperationRadarBar = function()
    {
        return this.m_oAreHideBars.radarBar;
    }
    EditorController.prototype.isHiddenOperationOpticalBar= function()
    {
        return this.m_oAreHideBars.opticalBar;
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
        'WorkspaceService',
        'GlobeService',
        'ProcessesLaunchedService',
        'RabbitStompService',
        'SnapOperationService',
        'ModalService',

    ];

    return EditorController;
})();
