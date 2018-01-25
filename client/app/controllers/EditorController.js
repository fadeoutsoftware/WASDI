/**
 * Created by p.campanella on 24/10/2016.
 */
var EditorController = (function () {
    function EditorController($scope, $location, $interval, oConstantsService, oAuthService, oMapService, oFileBufferService,
                              oProductService, $state, oWorkspaceService, oGlobeService, oProcessesLaunchedService, oRabbitStompService,
                              oSnapOperationService, oModalService, oFilterService) {
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
        this.m_oRabbitStompService = oRabbitStompService;
        this.m_oFilterService = oFilterService;
        this.m_b2DMapModeOn = true;
        this.m_b3DMapModeOn = false;
        this.m_bPreviewBandNotGeoreferenced = true;
        this.m_bIsVisibleMapOfLeaflet = false;
        this.m_oModalService = oModalService;
        this.m_bIsLoadingTree = true;
        this.m_sToolTipBtnSwitchGeographic = "EDITOR_TOOLTIP_TO_GEO";
        this.m_sClassBtnSwitchGeographic = "btn-switch-not-geographic";

        /******* band without georeference members: ********/
        // this.m_sPreviewUrlSelectedBand = "assets/img/test_image.jpg";
        // this.m_sViewUrlSelectedBand = "assets/img/test_image.jpg";
        this.m_sPreviewUrlSelectedBand = "";
        this.m_sViewUrlSelectedBand = "";
        this.m_oBodyMapContainer = {
            "productFileName": "",
            "bandName": "",
            "filterVM": "",
            "vp_x": 0,
            "vp_y": 0,
            "vp_w": 10363,
            "vp_h": 10861,
            "img_w": 500,
            "img_h": 200
        };

        /****************/

        // this.m_sUrlSelectedBand = "";
        this.m_oAreHideBars = {
            mainBar:false,
            radarBar:true,
            opticalBar:true,
            processorBar:true
        }
        this.m_iActiveMapPanelTab = 0;
        this.GLOBE_DEFAULT_ZOOM = 2000000;

        //Last file downloaded
        this.m_oLastDownloadedProduct = null;
        //Pixel Info
        this.m_bIsVisiblePixelInfo = false;
        //layer list
        this.m_aoLayersList = [];//only id
        //this.m_aoProcessesRunning=[];
        // Array of products to show
        this.m_aoProducts = [];
        // Flag to know if we are in Info mode on 2d map
        this.m_bIsModeOnPixelInfo = false;
        // Here a Workpsace is needed... if it is null create a new one..
        this.m_oActiveWorkspace = this.m_oConstantsService.getActiveWorkspace();
        this.m_oUser = this.m_oConstantsService.getUser();

        // Initialize the map
        oMapService.initMapEditor('wasdiMap');
        oMapService.removeLayersFromMap();

        this.m_oGlobeService.initGlobe('cesiumContainer2');


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
            if (this.m_oRabbitStompService.isSubscrbed() == false) {
                this.m_oRabbitStompService.subscribe(this.m_oActiveWorkspace.workspaceId);
            }

        }

        //Set default value tree
        this.m_oTree = null;//IMPORTANT NOTE: there's a 'WATCH' for this.m_oTree in TREE DIRECTIVE

        /*Hook to Rabbit WebStomp Service*/
        this.m_oRabbitStompService.setMessageCallback(this.receivedRabbitMessage);
        this.m_oRabbitStompService.setActiveController(this);
        this.drawColourManipulationHistogram("colourManipulationDiv")
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
                // CARE WE CAN'T DO OPEN_NODE AND SELECET_NODE AT THE SAME TIME
                $("#jstree").jstree(true).select_node(m[i].id,true);
                // $("#jstree").jstree(true).open_node(m[i].id,true);
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
     * Handler of the Rabbit message
     * @param oMessage Received Message
     */
    EditorController.prototype.receivedRabbitMessage = function (oMessage, oController) {

        // Check if the message is valid
       if (oMessage == null) return;

        // Check the Result
        if (oMessage.messageResult == "KO") {

            var sOperation = "null";
            if (utilsIsStrNullOrEmpty(oMessage.messageCode) === false  ) sOperation = oMessage.messageCode;

            var oDialog = utilsVexDialogAlertTop('GURU MEDITATION<br>THERE WAS AN ERROR IN THE ' + sOperation + ' PROCESS');
            utilsVexCloseDialogAfterFewSeconds(3000, oDialog);
            this.m_oProcessesLaunchedService.loadProcessesFromServer(this.m_oActiveWorkspace);

            if (oMessage.messageCode =="PUBLISHBAND") {
                if (utilsIsObjectNullOrUndefined(oMessage.payload)==false) {
                    if (utilsIsObjectNullOrUndefined(oMessage.payload.productName) == false && utilsIsObjectNullOrUndefined(oMessage.payload.bandName) == false) {
                        var sNodeName = oMessage.payload.productName + "_" + oMessage.payload.bandName;

                        $("#jstree").jstree().enable_node(sNodeName);
                        $('#jstree').jstree(true).set_icon(sNodeName,'assets/icons/uncheck_20x20.png');

                    }
                }
            }

            return;
        }

        // Switch the Code
        switch(oMessage.messageCode) {
            case "PUBLISH":
                // oController.receivedPublishBandMessage(oMessage);
                oController.receivedPublishMessage(oMessage);
                break;
            case "PUBLISHBAND":
                oController.receivedPublishBandMessage(oMessage);
                break;
            case "UPDATEPROCESSES":
                //oController.m_oProcessesLaunchedService.loadProcessesFromServer(oController.m_oActiveWorkspace.workspaceId);
                break;
            case "DOWNLOAD":
            case "APPLYORBIT":
            case "CALIBRATE":
            case "MULTILOOKING":
            case "NDVI":
            case "TERRAIN":
            case "GRAPH":
            case "INGEST":
                oController.receivedNewProductMessage(oMessage);
                break;
        }

        utilsProjectShowRabbitMessageUserFeedBack(oMessage);
    }

    /**
     * Callback for messages that adds a new product to the Workspace
     * @param oMessage
     */
    EditorController.prototype.receivedNewProductMessage = function (oMessage) {

        // P.Campanella 29/05/2017: Moved Add Product To WS in the launcher server side: TEST this

        var oDialog = utilsVexDialogAlertBottomRightCorner('PRODUCT ADDED TO THE WS<br>READY');
        utilsVexCloseDialogAfterFewSeconds(3000, oDialog);
        this.getProductListByWorkspace();

        //the m_oLastDownloadedProduct will be select & open in jstree
        this.m_oLastDownloadedProduct = oMessage.payload.fileName;

    }


    /**
     * Handler of the "publish" message
     * @param oMessage
     */
    /* THIS FUNCTION ARE CALLED IN RABBIT SERVICE */
    EditorController.prototype.receivedPublishMessage = function (oMessage) {
        if (oMessage == null) return;
        if (oMessage.messageResult == "KO") {
            //alert('There was an error in the publish');
            utilsVexDialogAlertTop('GURU MEDITATION<br>THERE WAS AN ERROR IN THE PUBLISH');
            return;
        }

    }


    /**
     * Handler of the "PUBLISHBAND" message
     * @param oMessage
     */
    EditorController.prototype.receivedPublishBandMessage = function (oMessage) {
        var oLayer = oMessage.payload;

         if (utilsIsObjectNullOrUndefined(oLayer)) {
            console.log("Error LayerID is empty...");
            return false;
        }

        var sLabelText = "";

        oLayer.isVisibleInMap = true;

        //add layer in list

        // check if the background is grey or there is a map
        if (this.m_bIsVisibleMapOfLeaflet == true) {
            //if there is a map, add layers to it
            this.addLayerMap2D(oLayer.layerId);
            this.addLayerMap3D(oLayer.layerId);
            this.m_aoLayersList.push(oLayer);

            //if there isn't Bounding Box is impossible do zoom
            if (!utilsIsStrNullOrEmpty(oLayer.geoserverBoundingBox))
            {
                var oBounds = JSON.parse(oLayer.geoserverBoundingBox);

                this.m_oGlobeService.zoomOnLayerBoundingBox([oBounds.minx, oBounds.miny, oBounds.maxx, oBounds.maxy]);

                //Zoom on layer
                var oMap = this.m_oMapService.getMap();
                var corner1 = L.latLng(oBounds.maxy, oBounds.maxx),
                    corner2 = L.latLng(oBounds.miny, oBounds.minx),
                    bounds = L.latLngBounds(corner1, corner2);

                oMap.fitBounds(bounds);
            }
        }
        else {
            //if the backgrounds is grey remove all others bands in tree - map
            var iNumberOfLayers = this.m_aoLayersList.length;

            // CLOSE ALL THE NODES
            for (var iIndexLayer = 0; iIndexLayer < iNumberOfLayers; iIndexLayer++) {

                //check if there is layerId if there isn't the layer was added by get capabilities
                if (!utilsIsObjectNullOrUndefined(this.m_aoLayersList[iIndexLayer].layerId)) {

                    var sNodeId = this.m_aoLayersList[iIndexLayer].productName + "_" + this.m_aoLayersList[iIndexLayer].bandName;

                    var oNode = $('#jstree').jstree(true).get_node(sNodeId);

                    if (utilsIsObjectNullOrUndefined(oNode.original)==false) {

                        oNode.original.bPubblish = false;
                        $('#jstree').jstree(true).set_icon(sNodeId, 'assets/icons/uncheck_20x20.png');

                    }
                    else {
                        console.log("Editor Controller: ERROR oNode.original not defined");
                    }

                }
            }

            this.m_aoLayersList = [];
            this.m_oMapService.removeLayersFromMap();
            this.m_oGlobeService.removeAllEntities();

            // so add the new bands and the bounding box in cesium
            var aBounds = JSON.parse("[" + oLayer.boundingBox + "]");

            for (var iIndex = 0; iIndex < aBounds.length - 1; iIndex = iIndex + 2) {
                var iSwap;
                iSwap = aBounds[iIndex];
                aBounds[iIndex] = aBounds[iIndex + 1];
                aBounds[iIndex + 1] = iSwap;
            }

            if(aBounds.length  > 1) this.m_oGlobeService.addRectangleOnGlobeParamArray(aBounds);

            this.addLayerMap2D(oLayer.layerId);
            this.m_aoLayersList.push(oLayer);
            this.zoomOnLayer2DMap(oLayer.layerId);
            this.zoomOnLayer3DGlobe(oLayer.layerId);
        }


        var sNodeID = oLayer.productName + "_" + oLayer.bandName;

        /*CHANGE TREE */
        var oNode = $('#jstree').jstree(true).get_node(sNodeID); //oLayer.layerId

        if (utilsIsObjectNullOrUndefined(oNode.original) == false) {

            oNode.original.bPubblish = true;
            oNode.original.band.layerId = oLayer.layerId;
            oNode.original.band.published=true;

            $('#jstree').jstree(true).set_icon(sNodeID, 'assets/icons/check_20x20.png');
            $("#jstree").jstree().enable_node(sNodeID);
            sLabelText = $("#jstree").jstree().get_text(sNodeID);
            sLabelText = sLabelText.replace("band-not-published-label", "band-published-label");

            utilsJstreeUpdateLabelNode(sNodeID,sLabelText);
        }
        else {
            console.log ("EditorController: ERROR NODE " + oLayer.layerId + " does not exists in the Tree!!")
        }
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

        var oMap = this.m_oMapService.getMap();
        var sUrl = this.m_oConstantsService.getWmsUrlGeoserver();//'http://localhost:8080/geoserver/ows?'

        var wmsLayer = L.tileLayer.betterWms(sUrl, {
            layers: 'wasdi:' + sLayerId,
            format: 'image/png',
            transparent: true,
            noWrap: true
        });
        wmsLayer.setZIndex(1000);//it set the zindex of layer in map
        wmsLayer.addTo(oMap);


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
            utilsVexDialogAlertBottomRightCorner('DOWNLOADING');
            //console.log('downloading');
        }).error(function (data, status) {
            utilsVexDialogAlertTop('GURU MEDITATION<br>DOWNLOAD ERROR');
            //console.log('download error');
        });
    }

    /**
     * Call publish service
     * @param sUrl
     */
    EditorController.prototype.publish = function (sUrl) {
        this.m_oFileBufferService.publish(sUrl, this.m_oActiveWorkspace.workspaceId).success(function (data, status) {
            utilsVexDialogAlertBottomRightCorner('PUBLISHING');
            //console.log('publishing');
        }).error(function (data, status) {
            utilsVexDialogAlertTop('GURU MEDITATION<br>PUBLISH ERROR');
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
5
        if (oProductItem.index >= iProductsCount) return asBands;

        var oProduct = this.m_aoProducts[oProductItem.index];

        var aoBands = oProduct.bandsGroups.bands;

        var iBandCount = 0;

        if (!utilsIsObjectNullOrUndefined(aoBands)) {
            iBandCount = aoBands.length;
        }

        for (var i = 0; i < iBandCount; i++) {
            var oBandItem = {};
            oBandItem.name = aoBands[i].name;
            oBandItem.productName = oProductItem.name;
            oBandItem.productIndex = oProductItem.index;
            oBandItem.published = false;

            if (!utilsIsObjectNullOrUndefined(aoBands[i].published)) {
                oBandItem.published = aoBands[i].published;
            }


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
        var bAlreadyPublished = oBand.published;

        this.m_oFileBufferService.publishBand(sFileName, this.m_oActiveWorkspace.workspaceId, oBand.name).success(function (data, status) {
            if (!bAlreadyPublished) {
                var oDialog = utilsVexDialogAlertBottomRightCorner('PUBLISHING BAND ' + oBand.name);
                utilsVexCloseDialogAfterFewSeconds(3000, oDialog);
            }
            //console.log('publishing band ' + oBand.name);
            if (!utilsIsObjectNullOrUndefined(data) && data.messageResult != "KO" && utilsIsObjectNullOrUndefined(data.messageResult)) {
                /*if the band was published*/

                if (data.messageCode === "PUBLISHBAND")
                {
                    oController.receivedPublishBandMessage(data);

                    var elementMapContainer = angular.element(document.querySelector('#mapcontainer'));
                    var heightMapContainer = elementMapContainer[0].offsetHeight;
                    var widthMapContainer = elementMapContainer[0].offsetWidth;

                    var elementImagePreview = angular.element(document.querySelector('#imagepreviewcanvas'));
                    var heightImagePreview = elementImagePreview[0].offsetHeight;
                    var widthImagePreview = elementImagePreview[0].offsetWidth;

                    oController.m_oBodyMapContainer = {
                        "productFileName": sFileName,
                        "bandName": oBand.name,
                        // "filterVM": "",
                            "vp_x": 0,
                            "vp_y": 0,
                            "vp_w": 10363,
                            "vp_h": 10861,
                            "img_w": widthMapContainer,
                            "img_h": heightMapContainer
                    };
                    var oBodyImagePreview = {
                        "productFileName": sFileName,
                        "bandName": oBand.name,
                        // "filterVM": "",
                            "vp_x": 0,
                            "vp_y": 0,
                            "vp_w": 10363,
                            "vp_h": 10861,
                            "img_w": widthImagePreview,
                            "img_h": heightImagePreview
                    };

                    oController.processingViewBandImage(oController.m_oActiveWorkspace.workspaceId);
                    oController.processingPreviewBandImage(oBodyImagePreview,oController.m_oActiveWorkspace.workspaceId);
                }
                else
                {
                    if (data.messageCode !== "WAITFORRABBIT")//"PUBLISHBAND"
                    {
                        // oController.receivedPublishBandMessage(data.payload);
                        $("#jstree").jstree().enable_node(oBand.productName+"_"+oBand.name);
                        $('#jstree').jstree(true).set_icon(oBand.productName+"_"+oBand.name,'assets/icons/uncheck_20x20.png');
                    }
                }

            }
            else {

                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN PUBLISH BAND");
                $("#jstree").jstree().enable_node(oBand.productName+"_"+oBand.name);
                $('#jstree').jstree(true).set_icon(oBand.productName+"_"+oBand.name,'assets/icons/uncheck_20x20.png');
            }

        }).error(function (data, status) {
            console.log('publish band error');
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN PUBLISH BAND");
            $("#jstree").jstree().enable_node(oBand.productName+"_"+oBand.name);
            $('#jstree').jstree(true).set_icon(oBand.productName+"_"+oBand.name,'assets/icons/uncheck_20x20.png');
          });
    }

    //REMOVE BAND
    EditorController.prototype.removeBandImage = function (oBand) {

        if (utilsIsObjectNullOrUndefined(oBand) == true) {
            console.log("Error in removeBandImage")
            return false;
        }

        var sLayerId = "wasdi:" + oBand.layerId;
        var oMap2D = this.m_oMapService.getMap();
        var aoGlobeLayers = this.m_oGlobeService.getGlobeLayers();


        //remove layer in 2D map
        oMap2D.eachLayer(function (layer) {
            if (utilsIsStrNullOrEmpty(sLayerId) == false && layer.options.layers == sLayerId) {
                oMap2D.removeLayer(layer);
            }
        });

        //remove layer in 3D globe
        var oLayer = null;

        if (this.m_bIsVisibleMapOfLeaflet == true) {

            for (var iIndexLayer=0; iIndexLayer<aoGlobeLayers.length; iIndexLayer++) {
                oLayer = aoGlobeLayers.get(iIndexLayer);

                if (utilsIsStrNullOrEmpty(sLayerId) == false && utilsIsObjectNullOrUndefined(oLayer) == false && oLayer.imageryProvider.layers == sLayerId) {
                    oLayer = aoGlobeLayers.remove(oLayer);
                    break;
                }
            }
        }
        else {
            this.m_oGlobeService.removeAllEntities();
            this.m_oGlobeService.flyToWorkspaceBoundingBox(this.m_aoProducts);
        }

        //Remove layer from layers list
        var iLenghtLayersList = 0;
        if (utilsIsObjectNullOrUndefined(this.m_aoLayersList) == false) iLenghtLayersList = this.m_aoLayersList.length;

        if (utilsIsStrNullOrEmpty(sLayerId) == false) {
            for (var iIndex = 0; iIndex < iLenghtLayersList; ) {
                if (utilsIsSubstring(sLayerId, this.m_aoLayersList[iIndex].layerId)) {
                    this.m_aoLayersList.splice(iIndex,1);
                    iLenghtLayersList--;
                }
                else iIndex++;
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
                                    },
                                    "Zoom3D" : {
                                       "label" : "Zoom Band 3D Map",
                                       "action" : function (obj) {
                                           if(utilsIsObjectNullOrUndefined(oBand) == false)
                                               oController.zoomOnLayer3DGlobe(oBand.productName+"_"+oBand.name);
                                       }
                                    },
                                    "Filter Band":{
                                        "label": "Filter Band",
                                        "action" : function(pbj){
                                            if(utilsIsObjectNullOrUndefined(oBand) == false)
                                                oController.filterBandDialog();
                                        }
                                    }
                                };
                        }

                        //only products has $node.original.fileName
                        if (utilsIsObjectNullOrUndefined($node.original.fileName) == false) {
                            //***************************** PRODUCT ********************************************
                            oReturnValue =
                                {
                                    "Radar": {
                                        "label": "Radar",
                                        "action": false,
                                        "icon":"radar-icon-context-menu-jstree",
                                        "submenu":
                                            {
                                                //APPLY ORBIT
                                                "ApplyOrbit": {
                                                    "label": "Apply Orbit",
                                                    "action": function (obj) {
                                                        var sSourceFileName = $node.original.fileName;
                                                        var oFindedProduct = oController.findProductByFileName(sSourceFileName);
                                                        var sDestinationFileName = '';

                                                        if(utilsIsObjectNullOrUndefined(oFindedProduct) == false)  oController.openApplyOrbitDialog(oFindedProduct);

                                                    }
                                                },
                                                "Multilooking": {
                                                    "label": "Multilooking",
                                                    "action": function (obj) {
                                                        var sSourceFileName = $node.original.fileName;
                                                        var sDestinationFileName = '';
                                                        var oFindedProduct = oController.findProductByFileName(sSourceFileName);

                                                        if(utilsIsObjectNullOrUndefined(oFindedProduct) == false)  oController.openMultilookingDialog(oFindedProduct);
                                                    }
                                                },

                                                //         }
                                                // },
                                                //SUB-SUBMENU GEOMETRIC
                                                // "Geometric": {
                                                //     "label": "Geometric",
                                                //     "action": false,
                                                //     "submenu":
                                                //         {
                                                //SUB-SUB-SUBMENU GEOMETRIC
                                                // "Terrain correction": {
                                                //     "label": "Terrain correction",
                                                //     "action": false,
                                                //     "submenu":
                                                //         {

                                                //         }
                                                // },

                                                //         }
                                                // },
                                                "Range Doppler Terrain Correction": {
                                                    "label": "Range Doppler Terrain Correction",
                                                    "action": function (obj) {
                                                        var sSourceFileName = $node.original.fileName;
                                                        var sDestinationFileName = '';
                                                        var oFindedProduct = oController.findProductByFileName(sSourceFileName);

                                                        if(utilsIsObjectNullOrUndefined(oFindedProduct) == false) oController.rangeDopplerTerrainCorrectionDialog(oFindedProduct);
                                                    }
                                                },
                                                //SUB-SUBMENU RADIOMETRIC
                                                // "Radiometric": {
                                                //     "label": "Radiometric",
                                                //     "action": false,
                                                //     "separator_before":true,
                                                //     "submenu":
                                                //         {

                                                "Calibrate": {
                                                    "label": "Calibrate",
                                                    "action": function (obj) {
                                                        var sSourceFileName = $node.original.fileName;
                                                        var sDestinationFileName = '';
                                                        var oFindedProduct = oController.findProductByFileName(sSourceFileName);

                                                        if(utilsIsObjectNullOrUndefined(oFindedProduct) == false) oController.openRadiometricCalibrationDialog(oFindedProduct);

                                                    }
                                                },

                                            }
                                    },
                                    "Optical": {
                                        "label": "Optical",
                                        "action": false,
                                        "icon":"optical-icon-context-menu-jstree",
                                        "submenu":
                                            {
                                                "NDVI": {
                                                    "label": "NDVI",
                                                    "action": function (obj) {
                                                        var sSourceFileName = $node.original.fileName;
                                                        var sDestinationFileName = '';
                                                        var oFindedProduct = oController.findProductByFileName(sSourceFileName);

                                                        if(utilsIsObjectNullOrUndefined(oFindedProduct) == false) oController.openNDVIDialog(oFindedProduct);
                                                    }
                                                },

                                            }
                                    },
                                    "Properties": {
                                        "label": "Properties ",
                                        "icon":"info-icon-context-menu-jstree",
                                        "separator_before":true,
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
                                    "DeleteProduct": {
                                        "label": "Delete Product",
                                        "icon":"delete-icon-context-menu-jstree",

                                        "action": function (obj) {

                                            utilsVexDialogConfirmWithCheckBox("DELETING PRODUCT.<br>ARE YOU SURE?", function (value) {
                                                var bDeleteFile = false;
                                                var bDeleteLayer = false;
                                                if (value) {
                                                    if (value.files == 'on')
                                                        bDeleteFile = true;
                                                    if (value.geoserver == 'on')
                                                        bDeleteLayer = true;
                                                    this.temp = $node;
                                                    var that = this;
                                                    oController.m_oProductService.deleteProductFromWorkspace($node.original.fileName, oController.m_oActiveWorkspace.workspaceId, bDeleteFile, bDeleteLayer)
                                                        .success(function (data) {
                                                            var iLengthLayer;
                                                            var iLengthChildren_d = that.temp.children_d.length;

                                                            for(var iIndexChildren = 0; iIndexChildren < iLengthChildren_d; iIndexChildren++)
                                                            {
                                                                iLengthLayer = oController.m_aoLayersList.length;
                                                                for(var iIndexLayer = 0; iIndexLayer < iLengthLayer; iIndexLayer++)
                                                                {
                                                                    if( that.temp.children_d[iIndexChildren] ===  oController.m_aoLayersList[iIndexLayer].layerId)
                                                                    {
                                                                        oController.removeBandImage(oController.m_aoLayersList[iIndexChildren]);
                                                                        break;
                                                                    }

                                                                }

                                                            }

                                                            //reload product list
                                                            oController.getProductListByWorkspace();


                                                        }).error(function (error) {
                                                            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN DELETE PRODUCT");
                                                    });
                                                }

                                            });
                                        }
                                    },

                                };
                        }

                        return oReturnValue;
                    }
                }
            }


        var productList = this.getProductList();
        // var productList = this.m_aoProducts;
        // for each product i generate sub-node
        for (var iIndexProduct = 0; iIndexProduct < productList.length; iIndexProduct++) {

            //product node
            var oNode = new Object();
            if(utilsIsObjectNullOrUndefined( productList[iIndexProduct].productFriendlyName) == false)
                oNode.text = productList[iIndexProduct].productFriendlyName;//LABEL NODE
            else
                oNode.text = productList[iIndexProduct].name;//LABEL NODE

            oNode.fileName = productList[iIndexProduct].fileName;
            oNode.id = productList[iIndexProduct].fileName;

            var oController=this;


                oNode.children = [
                    {"text":"Metadata",
                     "icon": "assets/icons/folder_20x20.png",
                     "children": [],
                     "clicked":false,//semaphore
                        "url" : oController.m_oProductService.getApiMetadata(oNode.fileName),
                }, {
                    "text": "Bands",
                    "icon": "assets/icons/folder_20x20.png",
                    "children": []
                },

                ];
            oNode.icon = "assets/icons/product_20x20.png";
            oTree.core.data.push(oNode);

            var oaBandsItems = this.getBandsForProduct(productList[iIndexProduct]);

            for (var iIndexBandsItems = 0; iIndexBandsItems < oaBandsItems.length; iIndexBandsItems++) {
                var oNode = new Object();

                //LABEL NODE
                if (oaBandsItems[iIndexBandsItems].published) {
                    oNode.text = "<span class='band-published-label'>" + oaBandsItems[iIndexBandsItems].name + "</span>";
                }
                else {
                    oNode.text = "<span class='band-not-published-label'>" +  oaBandsItems[iIndexBandsItems].name + "</span>";
                }

                oNode.band = oaBandsItems[iIndexBandsItems];//BAND
                oNode.icon = "assets/icons/uncheck_20x20.png";

                //generate id for bands => fileName+BandName
                // var fileNameWithoutExtension = oTree.core.data[iIndexProduct].fileName;
                // fileNameWithoutExtension = fileNameWithoutExtension.match(/(.*)\.[^.]+$/);

                oNode.id = productList[iIndexProduct].name + "_" + oaBandsItems[iIndexBandsItems].name;
                oNode.bPubblish = false;
                oTree.core.data[iIndexProduct].children[1].children.push(oNode);
            }

        }

        return oTree;
    }


    /**
     *
     */
    EditorController.prototype.getProductListByWorkspace = function () {
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

                        // Add the product to the list
                        oController.m_aoProducts.push(data[iIndex]);
                    }

                    // i need to make the tree after the products are loaded
                    oController.m_oTree = oController.generateTree();
                    oController.m_bIsLoadingTree = false;
                    oController.m_oGlobeService.flyToWorkspaceBoundingBox(oController.m_aoProducts);

                }
            }
        }).error(function (data, status) {
            utilsVexDialogAlertTop('GURU MEDITATION<br>ERROR READING PRODUCT LIST');
        });
    }


    //---------------- OPENWORKSPACE -----------------------
    // ReLOAD workspace when reload page
    EditorController.prototype.openWorkspace = function (sWorkspaceId) {

        var oController = this;

        this.m_oWorkspaceService.getWorkspaceEditorViewModel(sWorkspaceId).success(function (data, status) {
            if (data != null) {
                if (data != undefined) {
                    oController.m_oConstantsService.setActiveWorkspace(data);
                    oController.m_oActiveWorkspace = oController.m_oConstantsService.getActiveWorkspace();

                    oController.getProductListByWorkspace();
                    oController.m_oProcessesLaunchedService.loadProcessesFromServer(oController.m_oActiveWorkspace.workspaceId);
                    /*Subscribe Rabbit WebStomp*/
                    //TODO CHECK THE SUBSCRIBE ERROR
                    if (oController.m_oRabbitStompService.isSubscrbed() == false) {
                        oController.m_oRabbitStompService.subscribe(oController.m_oActiveWorkspace.workspaceId);
                    }

                }
            }
        }).error(function (data, status) {
            //alert('error');
            utilsVexDialogAlertTop('GURU MEDITATION<br>ERROR IMPOSSIBLE GET WORKSPACE IN EDITORCONTROLLER')
        });
    }

    /********************** PROCESSING BAND **********************/
    /*
    *
    {
        "productFileName": "S1B_IW_GRDH_1SDV_20170621T052711_20170621T052736_006144_00ACB6_75AA.zip",
        "bandName": "Amplitude_VH",
        "filterVM": <filter>,
        "vp_x": 6000,
        "vp_y": 6000,
        "vp_w": 2000,
        "vp_h": 2000,
        "img_w": 200,
        "img_h": 200
    }
        filterVM è l'oggetto che rappresenta l'eventuale filtro da applicare (può anche non essere passato) ed è lo stesso oggetto ritornato dall'API "standardfilters"

        vp_x, vp_y, vp_w, vp_h rappresentano in viewport (in pixel) di ritaglio dell'immagine

        img_w, img_h rappresentano la dimensione dell'immagine jpg ritornata
    * */
    EditorController.prototype.processingPreviewBandImage = function(oBody,workspaceId)
    {

        if(utilsIsObjectNullOrUndefined(oBody) === true)
            return false;
        if(utilsIsStrNullOrEmpty(workspaceId) === true)
            return false;

        var oController = this;
        this.m_oFilterService.getProductBand(oBody,workspaceId).success(function (data, status) {
            if (data != null)
            {
                if (data != undefined)
                {
                    var blob = new Blob([data], {type: "octet/stream"});
                    var objectUrl = URL.createObjectURL(blob);
                    // document.querySelector("#previewimage").src = objectUrl;
                    oController.m_sPreviewUrlSelectedBand = objectUrl;
                    // window.open(objectUrl,'_self');
                }
            }
        }).error(function (data, status) {
            //alert('error');
            utilsVexDialogAlertTop('GURU MEDITATION<br>ERROR IMPOSSIBLE PROCESSING BAND IMAGE ')
        });

        return true;
    };

    EditorController.prototype.processingViewBandImage = function(workspaceId)
    {

        // if(utilsIsObjectNullOrUndefined(oBody) === true)
        //     return false;
        if(utilsIsStrNullOrEmpty(workspaceId) === true)
            return false;

        var oController = this;
        this.m_oFilterService.getProductBand(this.m_oBodyMapContainer,workspaceId).success(function (data, status) {
            if (data != null)
            {
                if (data != undefined)
                {
                    var blob = new Blob([data], {type: "octet/stream"});
                    var objectUrl = URL.createObjectURL(blob);
                    // document.querySelector("#previewimage").src = objectUrl;
                    oController.m_sViewUrlSelectedBand = objectUrl;
                    // window.open(objectUrl,'_self');
                }
            }
        }).error(function (data, status) {
            //alert('error');
            utilsVexDialogAlertTop('GURU MEDITATION<br>ERROR IMPOSSIBLE PROCESSING BAND IMAGE ')
        });

        return true;
    };

    EditorController.prototype.createBodyForprocessingBandImage = function(){

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
        oGlobe.camera.flyTo({
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
        if (utilsIsObjectNullOrUndefined(oLayerId)) return false;
        if (utilsIsObjectNullOrUndefined(this.m_aoLayersList)) return false;

        var iNumberOfLayers = this.m_aoLayersList.length;

        for (var iIndexLayer = 0; iIndexLayer < iNumberOfLayers; iIndexLayer++) {
            if (this.m_aoLayersList[iIndexLayer].layerId == oLayerId) break;
        }

        //there isn't layer in layerList
        if (!(iIndexLayer < iNumberOfLayers)) return false;

        if (utilsIsStrNullOrEmpty(this.m_aoLayersList[iIndexLayer].boundingBox)) return false;

        var aBoundingBox = JSON.parse("[" + this.m_aoLayersList[iIndexLayer].boundingBox + "]");

        if (utilsIsObjectNullOrUndefined(aBoundingBox) == true) return false;

        var aaBounds = [];
        for (var iIndex = 0; iIndex < aBoundingBox.length - 1; iIndex = iIndex + 2) {
            aaBounds.push(new Cesium.Cartographic.fromDegrees(aBoundingBox[iIndex + 1], aBoundingBox[iIndex ]));
        }

        var oGlobe = this.m_oGlobeService.getGlobe();
        var zoom = Cesium.Rectangle.fromCartographicArray(aaBounds);
        var oCenter = Cesium.Rectangle.center(zoom);

        //oGlobe.camera.setView({
        oGlobe.camera.flyTo({
            destination : Cesium.Cartesian3.fromRadians(oCenter.longitude, oCenter.latitude, this.GLOBE_DEFAULT_ZOOM),
            orientation: {
                heading: 0.0,
                pitch: -Cesium.Math.PI_OVER_TWO,
                roll: 0.0
            }
        });

        return true;
    }

    EditorController.prototype.zoomOnLayer2DMap = function (oLayerId) {
        if (utilsIsObjectNullOrUndefined(oLayerId)) return false;
        if (utilsIsObjectNullOrUndefined(this.m_aoLayersList)) return false

        var iNumberOfLayers = this.m_aoLayersList.length;

        for (var iIndexLayer = 0; iIndexLayer < iNumberOfLayers; iIndexLayer++) {
            if (this.m_aoLayersList[iIndexLayer].layerId == oLayerId) break;
        }

        //there isn't layer in layerList
        if (!(iIndexLayer < iNumberOfLayers)) return false;

        if (utilsIsStrNullOrEmpty(this.m_aoLayersList[iIndexLayer].geoserverBoundingBox)) return false;

        var oBoundingBox = null;
        try {
            oBoundingBox = JSON.parse(this.m_aoLayersList[iIndexLayer].geoserverBoundingBox);
        } catch (e) {
            console.log(e);
        }


        if (utilsIsObjectNullOrUndefined(oBoundingBox) == true) return false;

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

    /*
        this method check if there is a band, if there is the band
        is removed else the band is add
     */
    EditorController.prototype.addOrRemoveMapLayer = function () {
        this.m_bIsVisibleMapOfLeaflet = !this.m_bIsVisibleMapOfLeaflet;

        //switch leaflet map mode and img preview mode
        this.m_bPreviewBandNotGeoreferenced = !this.m_bPreviewBandNotGeoreferenced;

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
                        try {
                            var oBounds = JSON.parse(this.m_aoLayersList[iIndexLayer].geoserverBoundingBox);

                            this.m_oGlobeService.zoomOnLayerBoundingBox([oBounds.minx, oBounds.miny, oBounds.maxx, oBounds.maxy]);
                        }
                        catch (e) {
                            console.log(e);
                        }
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
                    bResponse = utilsVexDialogConfirm("GOING IN IMAGE-MODE:<br>GEO MAP WILL BE CLOSED<br>ARE YOU SURE?", oCallback);//ask user if he want delete layers

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
            $('.leaflet-container').css('cursor','crosshair');
            // $("#wasdiMap").hover( function(){
            //     $(this).css({"pointer":"crosshair"});//set pointer with cross
            // });
            /*.leaflet-fade-anim .leaflet-map-pane .leaflet-popup*/
        }
        else {
            //$('.leaflet-popup-pane').css({"display":"none"});
            $('.leaflet-popup-pane').css({"visibility": "hidden"});
            $('.leaflet-container').css('cursor','');

            // $("#wasdiMap").hover( function(){
            //     $(this).css({"pointer":"auto"});//set pointer withe default pointer
            // });
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
    };

    //---------------------------------- SHOW MODALS ------------------------------------

    EditorController.prototype.openWorkflowDialog = function(){
        var oController = this;
        this.m_oModalService.showModal({
            templateUrl: "dialogs/workflow_operation/WorkFlowView.html",
            controller: "WorkFlowController",
            inputs: {
                extras: {
                    products:oController.m_aoProducts,
                }
            }
        }).then(function (modal) {
            modal.element.modal();
            modal.close.then(function (oResult) {


            });
        });

        return true;
    };

    EditorController.prototype.openMaskManager = function(){
        var oController = this;
        this.m_oModalService.showModal({
            templateUrl: "dialogs/mask_manager/MaskManagerView.html",
            controller: "MaskManagerController",
            inputs: {
                extras: {
                    //products:oController.m_aoProducts,
                }
            }
        }).then(function (modal) {
            modal.element.modal();
            modal.close.then(function (oResult) {


            });
        });

        return true;
    };

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
            modal.close.then(function (oResult) {


                if(utilsIsObjectNullOrUndefined(oResult) == true)
                {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR THE APPLY ORBIT OPTIONS ARE WRONG OR EMPTY!");
                    return false;
                }
                if(oResult == "close")
                    return false;

                // oController.m_oScope.Result = oResult;
                oController.m_oSnapOperationService.ApplyOrbit(oResult.sourceFileName, oResult.destinationFileName, oController.m_oActiveWorkspace.workspaceId,oResult.options)
                    .success(function (data) {

                    }).error(function (error) {
                        utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR THE OPERATION APPLY ORBIT DOSEN'T WORK");
                });
                return true;
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
            modal.close.then(function (oResult) {

                if(utilsIsObjectNullOrUndefined(oResult) == true)
                {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR THE NDVI OPTIONS ARE WRONG OR EMPTY!");
                    return false;
                }
                if(oResult == "close")
                    return false;

                // oController.m_oScope.Result = oResult;
                oController.m_oSnapOperationService.Calibrate(oResult.sourceFileName, oResult.destinationFileName, oController.m_oActiveWorkspace.workspaceId,oResult.options)
                    .success(function (data) {

                    }).error(function (error) {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR THE OPERATION RADIOMETRIC CALIBRATION DOESN'T WORK");
                });
                return true;
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
            modal.close.then(function (oResult) {

                if(utilsIsObjectNullOrUndefined(oResult) == true)
                {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR THE MULTILOOKING OPTIONS ARE WRONG OR EMPTY!");
                    return false;
                }
                if(oResult == "close")
                    return false;

                // oController.m_oScope.Result = oResult;
                oController.m_oSnapOperationService.Multilooking(oResult.sourceFileName, oResult.destinationFileName, oController.m_oActiveWorkspace.workspaceId,oResult.options)
                    .success(function (data) {

                    }).error(function (error) {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR THE OPERATION MULTILOOKING DOSEN'T WORK");
                });
                return true;

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
            modal.close.then(function (oResult) {

                if(utilsIsObjectNullOrUndefined(oResult) == true)
                {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR THE NDVI OPTIONS ARE WRONG OR EMPTY!");
                    return false;
                }
                if(oResult == "close")
                    return false;

                // oController.m_oScope.Result = oResult;
                oController.m_oSnapOperationService.NDVI(oResult.sourceFileName, oResult.destinationFileName, oController.m_oActiveWorkspace.workspaceId,oResult.options)
                    .success(function (data) {

                    }).error(function (error) {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR THE OPERATION NDVI DOESN'T WORK");
                });
                return true;
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
                if(utilsIsObjectNullOrUndefined(result)===true)
                    return false;
                //TODO ADD FILENAME = RESULT
                // oController.m_oScope.Result = result;
            });
        });

        return true;
    };
    EditorController.prototype.openSFTPDialog = function (oProductInput)
    {
        var oController = this;
        this.m_oModalService.showModal({
            templateUrl: "dialogs/sftp_upload/SftpUploadDialog.html",
            controller: "SftpUploadController",
            inputs: {
                extras: {
                    product:oProductInput
                }
            }
        }).then(function (modal) {
            modal.element.modal();
            modal.close.then(function (result) {
                if(utilsIsObjectNullOrUndefined(result)===true)
                    return false;
                //TODO ADD FILENAME = RESULT
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
            modal.close.then(function (oResult) {

                if(utilsIsObjectNullOrUndefined(oResult) == true)
                {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR THE RANGE DOPPLER TERRAIN CORRECTION OPTIONS ARE WRONG OR EMPTY!");
                    return false;
                }
                if(oResult == "close")
                    return false;

                // oController.m_oScope.Result = oResult;
                oController.m_oSnapOperationService.RangeDopplerTerrainCorrection(oResult.sourceFileName, oResult.destinationFileName, oController.m_oActiveWorkspace.workspaceId,oResult.options)
                    .success(function (data) {

                    }).error(function (error) {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR THE OPERATION RANGE DOPPLER TERRATIN CORRECTION DOESN'T WORK");
                });
                return true;
            });
        });

        return true;
    }
    EditorController.prototype.filterBandDialog = function (oSelectedProduct)
    {
        var oController = this;
        this.m_oModalService.showModal({
            templateUrl: "dialogs/filter_band_operation/FilterBandDialog.html",
            controller: "FilterBandController",
            inputs: {
                extras: null,
            }
        }).then(function (modal) {
            modal.element.modal();
            modal.close.then(function (oResult) {

            });
        });

        return true;
    }

    /************************** OPERATION MENU *************************/
    EditorController.prototype.hideOperationMainBar= function()
    {
        this.m_oAreHideBars.mainBar = true;
    };
    EditorController.prototype.hideOperationRadarBar = function()
    {
        this.m_oAreHideBars.radarBar = true;
    };
    EditorController.prototype.hideProcessorBar = function()
    {
        this.m_oAreHideBars.processorBar = true;
    };
    EditorController.prototype.hideOperationOpticalBar= function()
    {
        this.m_oAreHideBars.opticalBar = true;
    };
    EditorController.prototype.showOperationMainBar= function()
    {
        this.m_oAreHideBars.mainBar = false;
    };
    EditorController.prototype.showOperationRadarBar = function()
    {
        this.m_oAreHideBars.radarBar = false;
    };
    EditorController.prototype.showOperationOpticalBar= function()
    {
        this.m_oAreHideBars.opticalBar = false;
    };
    EditorController.prototype.showOperationProcessor= function()
    {
        this.m_oAreHideBars.processorBar = false;
    };

    EditorController.prototype.isHiddenOperationMainBar= function()
    {
        return this.m_oAreHideBars.mainBar;
    };
    EditorController.prototype.isHiddenOperationRadarBar = function()
    {
        return this.m_oAreHideBars.radarBar;
    };
    EditorController.prototype.isHiddenProcessorBar = function()
    {
        return this.m_oAreHideBars.processorBar;
    };

    EditorController.prototype.isHiddenOperationOpticalBar= function()
    {
        return this.m_oAreHideBars.opticalBar;
    };

    /***************** CSS CHANGE ****************/
    EditorController.prototype.changeClassBtnSwitchGeographic = function()
    {
        if(this.m_sClassBtnSwitchGeographic === "btn-switch-not-geographic"  )
        {
            this.m_sClassBtnSwitchGeographic = "btn-switch-geographic";
            this.m_sToolTipBtnSwitchGeographic = "EDITOR_TOOLTIP_TO_EDITOR";
        }

        else
        {
            this.m_sClassBtnSwitchGeographic = "btn-switch-not-geographic";
            this.m_sToolTipBtnSwitchGeographic = "EDITOR_TOOLTIP_TO_GEO";

        }
    }

    /* Generate tree for metadata */

    EditorController.prototype.generateMetadatadTree = function(oElement,oNewTree,iIndexNewTreeAttribute)
    {


        if (typeof oElement != "undefined" && oElement != null)
        {
            /* i generate new object
             {
             *       'text':'name'
             *       'Children':[]
             * }
             * */

            var oNode = new Object();
            oNode.text=oElement.name;
            oNode.children= [];
            oNewTree.push(oNode);

            if(oElement.elements != null)// if is a leaf
            {
                // i call the algorithm for all child
                for (var iIndexNumberElements = 0; iIndexNumberElements < (oElement.elements.length); iIndexNumberElements++)
                {
                    this.generateMetadatadTree(oElement.elements[iIndexNumberElements] ,oNewTree[iIndexNewTreeAttribute].children, iIndexNumberElements);
                }
            }

            /*
             if(oElement.bands != null)// if is a leaf
             {
             // i call the algorithm for all child
             for (var iIndexNumberElements = 0; iIndexNumberElements < (oElement.elements.length); iIndexNumberElements++)
             {
             this.generateWellFormedTree(oElement.bands[iIndexNumberElements] ,oNewTree[iIndexNewTreeAttribute].children, iIndexNumberElements);
             }
             }*/
        }

    };

    EditorController.prototype.changeModeOnOffPixelInfo = function()
    {
        this.m_bIsModeOnPixelInfo = !this.m_bIsModeOnPixelInfo;

    };

    EditorController.prototype.getClassPixelInfo = function()
    {
        if( this.m_bIsModeOnPixelInfo )
            return "#009036";//green
        else
            return "#43516A";//white

    };

    //hide band in layer list
    EditorController.prototype.hideBandInLayerList = function (oBand) {
        if (utilsIsObjectNullOrUndefined(oBand) == true) {
            console.log("Error in removeBandImage")
            return false;
        }

        var oController = this;
        if(utilsIsObjectNullOrUndefined(oBand.name) === false)
            var sLayerId = "wasdi:" + oBand.productName + "_" + oBand.name;// band removed
        else
            var sLayerId = "wasdi:" + oBand.layerId;  // + "_" + oBand.bandName;//remove bands after a product was deleted

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

        //TODO CHECK EVERY TIME WHILE(TRUE)
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
        oBand.isVisibleInMap = false;
        //Remove layer from layers list
        // var iLenghtLayersList;
        // if (utilsIsObjectNullOrUndefined(oController.m_aoLayersList))
        //     iLenghtLayersList = 0;
        // else
        //     iLenghtLayersList = oController.m_aoLayersList.length;

        // for (var iIndex = 0; iIndex < iLenghtLayersList; ) {
        //     if (utilsIsStrNullOrEmpty(sLayerId) == false && utilsIsSubstring(sLayerId, oController.m_aoLayersList[iIndex].layerId)) {
        //         oController.m_aoLayersList.splice(iIndex,1);
        //         iLenghtLayersList--;
        //     }
        //     else
        //     {
        //         iIndex++;
        //     }
        //
        // }
    };

    EditorController.prototype.isHideTree = function()
    {
        return ( (this.m_oTree === null) || (this.m_oTree.core.data.length === 0) );
    };

    EditorController.prototype.goSearch = function()
    {
        this.m_oState.go("root.import", { });
    };
    /************************ COLOUR MANIPOLATION ****************************/
    EditorController.prototype.drawColourManipulationHistogram = function(sNameDiv)
    {
        if(utilsIsStrNullOrEmpty(sNameDiv) === true)
            return false;

        var x = [];
        for (var i = 0; i < 500; i ++) {
            x[i] = Math.random();
        }

        var trace = {
            x: x,
            type: 'histogram',
        };
        var data = [trace];
        var layout = {
            //title: "Colour Manipolation",
            showlegend: false,
            height:200,
            margin: {
                l: 5,
                r: 5,
                b: 5,
                t: 5,
                pad: 4
            },
        };
        Plotly.newPlot(sNameDiv, data, layout,{staticPlot: true});

        return true;
    };
    EditorController.prototype.adjust95percentageColourManipulation = function()
    {
        //TODO NEW BAND
    };
    EditorController.prototype.adjust100percentageColourManipulation = function()
    {
        //TODO NEW BAND
    };
    EditorController.prototype.resetColourManipulation = function()
    {
        //TODO REMOVE NEW BAND
        this.removeBandImage(oBand)
        //TODO ADD OLD BAND
        this.addLayerMap2D(sLayerId);
    };
    EditorController.prototype.modifyColourManipulation = function(iMin,iMax,iAverage,sLayerId,oBand)
    {
        //TODO REQUEST
        //send iMin iMax iAverage to the server
        //TODO REMOVE OLD BAND
        this.removeBandImage(oBand)
        //TODO ADD NEW BAND
        this.addLayerMap2D(sLayerId);
    };
    EditorController.prototype.minSliderColourManipulation = function(iMin,iMax,iAverage)
    {
        this.modifyColourManipulation(iMin,iMax,iAverage,sLayerId,oBand);
    };
    EditorController.prototype.maxSliderColourManipulation = function(iMin,iMax,iAverage)
    {
        this.modifyColourManipulation(iMin,iMax,iAverage,sLayerId,oBand);
    };
    EditorController.prototype.averageSliderColourManipulation = function(iMin,iMax,iAverage)
    {
        this.modifyColourManipulation(iMin,iMax,iAverage,sLayerId,oBand);
    };
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
        'FilterService'

    ];

    return EditorController;
})();
