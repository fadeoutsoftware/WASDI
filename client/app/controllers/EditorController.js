/**
 * Created by p.campanella on 24/10/2016.
 */
 var EditorController = (function () {
    function EditorController($rootScope, $scope, $location, $interval, oConstantsService, oAuthService, oMapService, oFileBufferService,
        oProductService, $state, oWorkspaceService, oNodeService, oGlobeService, oProcessWorkspaceService, oRabbitStompService,
        oModalService, oTranslate, oCatalogService, oProcessorService, oConsoleService, 
        $window) {
        // Reference to the needed Services
        this.m_oWindow = $window;
        this.m_oRootScope = $rootScope;
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oLocation = $location;
        this.m_oInterval = $interval;
        this.m_oConstantsService = oConstantsService;
        this.m_oAuthService = oAuthService;
        this.m_oMapService = oMapService;
        this.m_oFileBufferService = oFileBufferService;
        this.m_oProductService = oProductService;
        this.m_oGlobeService = oGlobeService;
        this.m_oState = $state;
        this.m_oProcessWorkspaceService = oProcessWorkspaceService;
        this.m_oWorkspaceService = oWorkspaceService;
        this.m_oNodeService = oNodeService;
        this.m_oRabbitStompService = oRabbitStompService;
        this.m_oModalService = oModalService;

        if (this.m_oConstantsService.getActiveWorkspace()) {
            this.m_oRootScope.title = this.m_oConstantsService.getActiveWorkspace().name;
        }
        
        this.m_oTranslate = oTranslate;
        this.m_oCatalogService = oCatalogService;
        
        /**
         * Processors Service
         */
        this.m_oProcessorService = oProcessorService;

        /**
         * Console Service
         */
        this.m_oConsoleService = oConsoleService;

        // Flag to know if in the big map is 2d (true) or 3d (false)
        this.m_b2DMapModeOn = true;
        // Flag to know if the first zoom on band has been done
        this.m_bFirstZoomOnBandDone = false;

        //filter query text in tree
        this.m_sTextQueryFilterInTree = "";
        this.m_bIsFilteredTree = false;

        this.m_bIsLoadingTree = true;

        this.m_oMapContainerSize = utilsProjectGetMapContainerSize();
        // Field used to control the opacity of the base layer in 2D map mode
        this.oBaseBand = {
            "opacity": 100
        }
        // support variable to handle select all/ de select all in tree
        this.m_bAllSelected = false;

        // Reference to the actual active Band
        this.m_oActiveBand = null;
        //
        this.m_aoNavBarMenu = [];

        this.m_oAreHideBars = {
            mainBar: false,
            radarBar: true,
            opticalBar: true,
            processorBar: true
        };

        // Index of the actual Active Tab
        this.m_iActiveMapPanelTab = 0;
        // Default globe zoom
        this.GLOBE_DEFAULT_ZOOM = 2000000;

        //Last file downloaded
        this.m_oLastDownloadedProduct = null;
        //Pixel Info
        this.m_bIsVisiblePixelInfo = false;
        // List of the Band Items that are now visible
        this.m_aoVisibleBands = [];
        // List of external layers added
        this.m_aoExternalLayers = [];
        // Array of products of this workspace
        this.m_aoProducts = [];
        // List of computational nodes
        this.m_aoNodesList = [];
        // Flag to know if we are in Info mode on 2d map
        this.m_bIsModeOnPixelInfo = false;
        // Here a Workspace is needed... if it is null create a new one..
        this.m_oActiveWorkspace = this.m_oConstantsService.getActiveWorkspace();
        // Actual User
        this.m_oUser = this.m_oConstantsService.getUser();
        //{}
        this.m_aoProductsLayersIn3DMapArentGeoreferenced = [];
        //default sort by value
        this.sSortType = 'default'; 

        // Initialize the map
        oMapService.initMapEditor('wasdiMap');
        // add the GeoSearch plugin bar
        oMapService.initGeoSearchPluginForOpenStreetMap({ "position": 'bottomRight' });
        oMapService.removeLayersFromMap();

        //RabbitStomp Service Call
        this.m_iHookIndex = this.m_oRabbitStompService.addMessageHook(
            "LAUNCHJUPYTERNOTEBOOK",
            this,
            this.rabbitMessageHook
        )
        
        

        // Initialize the globe
        this.m_oGlobeService.initGlobe('cesiumContainer2');

        //if there isn't workspace
        if (utilsIsObjectNullOrUndefined(this.m_oActiveWorkspace) && utilsIsStrNullOrEmpty(this.m_oActiveWorkspace)) {
            //if this.m_oState.params.workSpace in empty null or undefined create new workspace
            if (!(utilsIsObjectNullOrUndefined(this.m_oState.params.workSpace) && utilsIsStrNullOrEmpty(this.m_oState.params.workSpace))) {
                // Open workspace
                this.openWorkspace(this.m_oState.params.workSpace);
            } else {
                // go to workspaces section
                this.m_oState.go("root.workspaces");
            }
        } else {
            // Load Processes
            this.m_oProcessWorkspaceService.loadProcessesFromServer(this.m_oActiveWorkspace.workspaceId);
        }

        // Load products
        this.getProductListByWorkspace();

        // Subscribe Rabbit
        this._subscribeToRabbit = function () {
            if (this.m_oRabbitStompService.isSubscrbed() == false && !utilsIsObjectNullOrUndefined(this.m_oActiveWorkspace)) {
                var _this = this;
                this.m_oRabbitStompService.waitServiceIsReady()
                    .then(function () {
                        console.log('EditorController: Web Stomp is ready --> subscribe');
                        _this.m_oRabbitStompService.subscribe(_this.m_oActiveWorkspace.workspaceId);
                    })

            }
        }

        this._subscribeToRabbit();

        //Set default value tree
        //IMPORTANT NOTE: there's a 'WATCH' for this.m_oTree in TREE DIRECTIVE
        this.m_oTree = null;

        // Hook to Rabbit WebStomp Service
        this.m_oRabbitStompService.setMessageCallback(this.receivedRabbitMessage);
        this.m_oRabbitStompService.setActiveController(this);
        
        this.m_bNotebookIsReady = false;
       

        // Go in geographic mode
        this.switchToGeographicMode();

        var oThat = this;

        angular.element($window).bind('resize', function () {

            $scope.width = $window.innerWidth;
            oThat.m_oMapContainerSize = utilsProjectGetMapContainerSize();

            // manuall $digest required as resize event
            // is outside of angular
            $scope.$digest();
        });


        if (!utilsIsObjectNullOrUndefined(this.m_oActiveWorkspace)) {
           
            this.m_oConsoleService.isConsoleReady(this.m_oActiveWorkspace.workspaceId).then(function (data, status) {
               
                if (utilsIsObjectNullOrUndefined(data.data) == false) {
                    oThat.m_bNotebookIsReady = data.data.boolValue;
                }
                 
                oThat.generateDefaultNavBarMenu();
            }, (function (data, status) {
                var sMessage = oThat.m_oTranslate.instant("MSG_PRODUCT_LIST_ERROR")
                utilsVexDialogAlertBottomRightCorner(sMessage);
            }));        
        }
        //set default navbar menu
        this.generateDefaultNavBarMenu();
        
    }

    /********************************************************* TRANSLATE SERVICE ********************************************************/
    EditorController.prototype.generateDefaultNavBarMenu = function () {
        this.m_aoNavBarMenu = [
            {
                name: "", //WAPPS
                caption_i18n: "EDITOR_OPERATION_TITLE_WAPPS",
                subMenu: [],
                onClick: this.openWappsDialog,
                icon: "fa fa-lg fa-rocket",
            },
            // --- Processor ---
            {
                name: "", // New Processor
                caption_i18n: "EDITOR_OPERATION_TITLE_NEW_PROCESSOR",
                subMenu: [],
                onClick: this.openProcessorDialog,
                icon: "fa fa-lg fa-plus-square",
            },
            // --- Workflow ---
            {
                name: "",
                icon: "fa fa-cogs",
                caption_i18n: "EDITOR_OPERATION_TITLE_WORKFLOW",
                subMenu: [],
                onClick: this.openWorkflowManagerDialog,
            },
            // --- Import ---
            {
                name: "",
                icon: "fa fa-cloud-upload-alt",
                caption_i18n: "EDITOR_OPERATION_TITLE_IMPORT",
                subMenu: [],
                onClick: this.openImportsDialog,
            },

            // --- Jupyter Notebook ---
            {
                name: "", // Jupyter Notebook
                caption_i18n: "EDITOR_OPERATION_TITLE_JUPYTER_NOTEBOOK_CREATE",
                subMenu: [],
                onClick: this.openJupyterNotebookPage,
                icon: "fa fa-laptop",
            },
            // --- Style ---
            {
                name: "",
                icon: "fa fa-paint-brush",
                caption_i18n: "EDITOR_OPERATION_TITLE_STYLE",
                subMenu: [],
                onClick: this.openStyleManagerDialog,
            },
            {
                name: "", //Share
                caption_i18n: "EDITOR_OPERATION_TITLE_SHARE",
                subMenu: [],
                onClick: this.openShareDialog,
                icon: "fa fa-share-alt fa-lg",
            },
        ];
        
        let oJupyterButton =  this.m_aoNavBarMenu.find(iIndex => iIndex.caption_i18n ==="EDITOR_OPERATION_TITLE_JUPYTER_NOTEBOOK_CREATE")
        this.filterNotebookButtons(oJupyterButton);
        this.translateToolbarMenuList(this.m_aoNavBarMenu)
      
    };

    EditorController.prototype.isToolbarBtnDropdown = function (btn) {
        return btn.subMenu.length != 0;
    };

    EditorController.prototype.translateToolbarMenu = function (menuItem) {
        this.m_oTranslate(menuItem.caption_i18n).then(function (text) {
            menuItem.name = text;
        });
    };

    EditorController.prototype.translateToolbarMenuList = function (menuList) {
        for (var i = 0; i < menuList.length; i++) {
            var menuItem = menuList[i];
            this.translateToolbarMenu(menuItem);
            if (this.isToolbarBtnDropdown(menuItem) == true) {
                this.translateToolbarMenuList(menuItem.subMenu);
            }
        }
    };

    /*********************************************************** VIEW METHODS**********************************************************/


    /**
     * Change location to path
     * @param sPath
     */
    EditorController.prototype.moveTo = function (sPath) {
        this.m_oLocation.path(sPath);
    };

    /**
     * Set the active tab between Navigation, Colour manipulation, Preview
     * @param iTab
     */
    EditorController.prototype.setActiveTab = function (iTab) {
        if (this.m_iActiveMapPanelTab === iTab) return;
        let oBand, sFileName;
        this.m_iActiveMapPanelTab = iTab;
    };


    /**
     * Switch from 2D Mode to 3D Mode and viceversa
     */
    EditorController.prototype.switch2D3DMode = function () {

        // Revert the flag
        this.m_b2DMapModeOn = !this.m_b2DMapModeOn;
        // Take a reference to the controller
        var oController = this;

        if (this.m_b2DMapModeOn == false) {

            //this.setActiveTab(0);

            // We are going in 3D MAP
            this.m_oMapService.clearMap();
            this.m_oGlobeService.clearGlobe();
            this.m_oGlobeService.initGlobe('cesiumContainer');
            this.m_oMapService.initWasdiMap('wasdiMap2');

            // Due to the problems of Leaflet initialization, let's do the subsequent steps a little bit later
            setTimeout(function () {
                oController.m_oMapService.getMap().invalidateSize();

                // Load Layers
                for (var iIndexLayers = 0; iIndexLayers < oController.m_aoVisibleBands.length; iIndexLayers++) {
                    // Check if it is a valid layer
                    if (!utilsIsObjectNullOrUndefined(oController.m_aoVisibleBands[iIndexLayers].layerId)) {
                        var sGeoserverBBox = oController.m_aoVisibleBands[iIndexLayers].geoserverBoundingBox;
                        var oRectangleIsNotGeoreferencedProduct = oController.productIsNotGeoreferencedRectangle3DMap(sGeoserverBBox, oController.m_aoVisibleBands[iIndexLayers].bbox,
                            oController.m_aoVisibleBands[iIndexLayers].geoserverBoundingBox, oController.m_aoVisibleBands[iIndexLayers].layerId);
                        if (utilsIsObjectNullOrUndefined(oRectangleIsNotGeoreferencedProduct) === false) {
                            oController.addLayerMap3DByServer(oController.m_aoVisibleBands[iIndexLayers].layerId, oController.m_aoVisibleBands[iIndexLayers].geoserverUrl);
                            var oLayer3DMap = {
                                id: oController.m_aoVisibleBands[iIndexLayers].layerId,
                                rectangle: oRectangleIsNotGeoreferencedProduct
                            };
                            oController.m_aoProductsLayersIn3DMapArentGeoreferenced.push(oLayer3DMap);
                        } else {
                            oController.addLayerMap3DByServer(oController.m_aoVisibleBands[iIndexLayers].layerId, oController.m_aoVisibleBands[iIndexLayers].geoserverUrl);
                        }
                    }

                    var sNodeId = oController.m_aoVisibleBands[iIndexLayers].productName + "_" + oController.m_aoVisibleBands[iIndexLayers].bandName;
                    oController.setTreeNodeAsSelected(sNodeId);
                }

                // Load External Layers
                for (var iExternals = 0; iExternals < oController.m_aoExternalLayers.length; iExternals++) {
                    if (!utilsIsObjectNullOrUndefined(oController.m_aoExternalLayers[iExternals].Name)) {
                        oController.addLayerMap3DByServer(oController.m_aoExternalLayers[iExternals].Name, oController.m_aoExternalLayers[iExternals].sServerLink);
                    }
                }

                // Add all bounding boxes to 2D Map
                oController.m_oMapService.addAllWorkspaceRectanglesOnMap(oController.m_aoProducts);

                // Zoom on the active band
                if (utilsIsObjectNullOrUndefined(oController.m_oActiveBand) == false) {
                    oController.m_oGlobeService.zoomBandImageOnGeoserverBoundingBox(oController.m_oActiveBand.geoserverBoundingBox);
                    oController.m_oMapService.zoomBandImageOnGeoserverBoundingBox(oController.m_oActiveBand.geoserverBoundingBox);
                } else {
                    // Zoom on the workspace
                    oController.m_oGlobeService.flyToWorkspaceBoundingBox(oController.m_aoProducts);
                    oController.m_oMapService.flyToWorkspaceBoundingBox(oController.m_aoProducts);
                }
            }, 400);
        } else {
            //We are going in 2D MAP
            this.m_oMapService.clearMap();
            this.m_oGlobeService.clearGlobe();
            this.m_oMapService.initWasdiMap('wasdiMap');
            this.m_oGlobeService.initGlobe('cesiumContainer2');

            // Due to the problems of Leaflet initialization, let's do the subsequent steps a little bit later
            setTimeout(function () {
                oController.m_oMapService.getMap().invalidateSize();

                // Load Layers
                for (var iIndexLayers = 0; iIndexLayers < oController.m_aoVisibleBands.length; iIndexLayers++) {
                    // Check if it is a valid layer
                    if (!utilsIsObjectNullOrUndefined(oController.m_aoVisibleBands[iIndexLayers].layerId)) {

                        var sColor = "#f22323";
                        var sGeoserverBBox = oController.m_aoVisibleBands[iIndexLayers].geoserverBoundingBox;

                        oController.productIsNotGeoreferencedRectangle2DMap(sColor, sGeoserverBBox, oController.m_aoVisibleBands[iIndexLayers].bbox, oController.m_aoVisibleBands[iIndexLayers].layerId);
                        oController.addLayerMap2DByServer(oController.m_aoVisibleBands[iIndexLayers].layerId, oController.m_aoVisibleBands[iIndexLayers].geoserverUrl);
                    }

                    var sNodeId = oController.m_aoVisibleBands[iIndexLayers].productName + "_" + oController.m_aoVisibleBands[iIndexLayers].bandName;
                    oController.setTreeNodeAsSelected(sNodeId);
                }

                // Load External Layers
                for (var iExternals = 0; iExternals < oController.m_aoExternalLayers.length; iExternals++) {
                    if (!utilsIsObjectNullOrUndefined(oController.m_aoExternalLayers[iExternals].Name)) {
                        oController.addLayerMap2DByServer(oController.m_aoExternalLayers[iExternals].Name, oController.m_aoExternalLayers[iExternals].sServerLink);
                    }
                }

                //  Add all bounding boxes to 3D Map
                oController.m_oGlobeService.addAllWorkspaceRectanglesOnMap(oController.m_aoProducts);

                // Zoom on the active band
                if (utilsIsObjectNullOrUndefined(oController.m_oActiveBand) == false) {
                    oController.m_oGlobeService.zoomBandImageOnGeoserverBoundingBox(oController.m_oActiveBand.geoserverBoundingBox);
                    oController.m_oMapService.zoomBandImageOnGeoserverBoundingBox(oController.m_oActiveBand.geoserverBoundingBox);
                    oController.setLayerOpacity(oController.m_oActiveBand.opacity, oController.m_oActiveBand.layerId);
                    // Re-apply layers opacity fìoer each band
                    oController.m_aoVisibleBands.forEach(oCurBand => {
                        oController.setLayerOpacity(oCurBand.opacity, oCurBand.layerId);
                    });

                } else {
                    // Zoom on the workspace
                    oController.m_oMapService.flyToWorkspaceBoundingBox(oController.m_aoProducts);
                    oController.m_oGlobeService.flyToWorkspaceBoundingBox(oController.m_aoProducts);

                }

            }, 400);
        }
    };

    EditorController.prototype.productIsNotGeoreferencedRectangle2DMap = function (sColor, sGeoserverBBox, asBbox, sLayerId) {
        if (this.m_oMapService.isProductGeoreferenced(asBbox, sGeoserverBBox) === false) {
            var oRectangleBoundingBoxMap = this.m_oMapService.addRectangleByGeoserverBoundingBox(sGeoserverBBox, sColor);

            if (utilsIsObjectNullOrUndefined(oRectangleBoundingBoxMap) == false) {
                //the options.layers property is used for remove the rectangle to the map
                oRectangleBoundingBoxMap.options.layers = "wasdi:" + sLayerId;
            }
        }
    };

    EditorController.prototype.productIsNotGeoreferencedRectangle3DMap = function (sGeoserverBBox, asBbox, sLayerId) {
        var oRectangle = null;
        if (this.m_oMapService.isProductGeoreferenced(asBbox, sGeoserverBBox) === false) {
            oRectangle = this.m_oGlobeService.addRectangleOnGLobeByGeoserverBoundingBox(sGeoserverBBox);
            //the options.layers property is used for remove the rectangle to the map
            // oRectangleBoundingBoxMap.options.layers = "wasdi:" + sLayerId;
            oRectangle.layers = "wasdi:" + sLayerId;

        }
        return oRectangle;
    };

    EditorController.prototype.switchToGeographicMode = function () {

        //Check if there is a visible layer and if it is already published
        for (var iIndexLayer = 0; iIndexLayer < this.m_aoVisibleBands.length; iIndexLayer++) {
            // var bIsProductGeoreferenced = false;
            var sColor = "#f22323";
            var sGeoserverBBox = this.m_aoVisibleBands[iIndexLayer].geoserverBoundingBox;

            //check if the layer has the layer Id
            if (!utilsIsObjectNullOrUndefined(this.m_aoVisibleBands[iIndexLayer].layerId)) {
                // And if it is valid
                if (!utilsIsStrNullOrEmpty(this.m_aoVisibleBands[iIndexLayer].layerId)) {

                    // show the layer
                    if (this.m_b2DMapModeOn) {
                        this.addLayerMap2DByServer(this.m_aoVisibleBands[iIndexLayer].layerId, this.m_aoVisibleBands[iIndexLayer].geoserverUrl);
                        this.productIsNotGeoreferencedRectangle2DMap(sColor, sGeoserverBBox, this.m_aoVisibleBands[iIndexLayer].bbox, this.m_aoVisibleBands[iIndexLayer].layerId);
                    } else {
                        this.addLayerMap3DByServer(this.m_aoVisibleBands[iIndexLayer].layerId, this.m_aoVisibleBands[iIndexLayer].geoserverUrl);
                        var oRectangleIsNotGeoreferencedProduct = this.productIsNotGeoreferencedRectangle3DMap(sGeoserverBBox, this.m_aoVisibleBands[iIndexLayer].bbox, this.m_aoVisibleBands[iIndexLayer].layerId);
                        if (utilsIsObjectNullOrUndefined(oRectangleIsNotGeoreferencedProduct) === false) {
                            var oLayer3DMap = {
                                id: this.m_aoVisibleBands[iIndexLayer].layerId,
                                rectangle: oRectangleIsNotGeoreferencedProduct
                            };
                            this.m_aoProductsLayersIn3DMapArentGeoreferenced.push(oLayer3DMap);
                        }
                    }

                    // Check for geoserver bounding box
                    if (!utilsIsStrNullOrEmpty(this.m_aoVisibleBands[iIndexLayer].geoserverBoundingBox)) {
                        this.m_oGlobeService.zoomBandImageOnGeoserverBoundingBox(this.m_aoVisibleBands[iIndexLayer].geoserverBoundingBox);
                        this.m_oMapService.zoomBandImageOnGeoserverBoundingBox(this.m_aoVisibleBands[iIndexLayer].geoserverBoundingBox);
                    } else {
                        // Try with the generic product bounding box
                        this.m_oGlobeService.zoomBandImageOnBBOX(this.m_aoVisibleBands[iIndexLayer].bbox);
                        this.m_oMapService.zoomBandImageOnBBOX(this.m_aoVisibleBands[iIndexLayer].bbox);
                    }
                }
            } else {

                var oController = this;
                // should always be 0 ...
                var iProductIndex = iIndexLayer;

                // Band Not Yet Published !!
                var oPublishBandCallback = function (value) {
                    var oBand = oController.m_aoVisibleBands[iProductIndex];

                    if (value) {
                        // Remove it from Visible Layer List
                        oController.removeBandFromVisibleList(oBand);
                        // Publish the band
                        oController.openBandImage(oBand);
                        return true;
                    } else {
                        oController.removeBandFromVisibleList(oBand);
                        var sNode = oBand.productName + "_" + oBand.name;
                        oController.setTreeNodeAsDeselected(sNode);
                        return false;
                    }
                };
            }
        }

        // Show the external Layers
        this.addExternalLayersOnMaps();

        // Set the base maps
        this.m_oMapService.setBasicMap();

        if (this.m_aoVisibleBands.length == 0) {
            this.m_oMapService.flyToWorkspaceBoundingBox(this.m_aoProducts);
        }
    };

    EditorController.prototype.addExternalLayersOnMaps = function () {
        for (var iExternals = 0; iExternals < this.m_aoExternalLayers.length; iExternals++) {
            var oLayer = this.m_aoExternalLayers[iExternals];

            // Add to the map External Layer
            this.addLayerMap2DByServer(this.m_aoExternalLayers[iExternals].Name, oLayer.sServerLink);
            this.addLayerMap3DByServer(this.m_aoExternalLayers[iExternals].Name, oLayer.sServerLink);
        }
    };

    /*********************************************************** MESSAGE HANDLING **********************************************************/

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
            if (utilsIsStrNullOrEmpty(oMessage.messageCode) === false) sOperation = oMessage.messageCode;

            var sErrorDescription = "";

            if (utilsIsStrNullOrEmpty(oMessage.payload) === false) sErrorDescription = oMessage.payload;
            if (utilsIsStrNullOrEmpty(sErrorDescription) === false) sErrorDescription = "<br>" + sErrorDescription;

            var oDialog = utilsVexDialogAlertTop(oController.m_oTranslate.instant("MSG_ERROR_IN_OPERATION_1") + sOperation + oController.m_oTranslate.instant("MSG_ERROR_IN_OPERATION_2") + sErrorDescription);
            utilsVexCloseDialogAfter(10000, oDialog);
            
            if (oMessage.messageCode == "PUBLISHBAND") {
                if (utilsIsObjectNullOrUndefined(oMessage.payload) == false) {
                    if (utilsIsObjectNullOrUndefined(oMessage.payload.productName) == false && utilsIsObjectNullOrUndefined(oMessage.payload.bandName) == false) {
                        var sNodeName = oMessage.payload.productName + "_" + oMessage.payload.bandName;
                        this.setTreeNodeAsDeselected(sNodeName);
                    }
                }
            }

            return;
        }

        // Switch the Code
        switch (oMessage.messageCode) {
            case "PUBLISH":
                oController.receivedPublishMessage(oMessage);
                break;
            case "PUBLISHBAND":
                oController.receivedPublishBandMessage(oMessage);
                break;
            case "DOWNLOAD":
            case "GRAPH":
            case "INGEST":
            case "MOSAIC":
            case "SUBSET":
            case "MULTISUBSET":
            case "RASTERGEOMETRICRESAMPLE":
            case "REGRID":
                oController.receivedNewProductMessage(oMessage);
                break;
            case "DELETE":
                //oController.getProductListByWorkspace();
                break;
        }

        utilsProjectShowRabbitMessageUserFeedBack(oMessage, oController.m_oTranslate);
    };

    /**
     * Callback for messages that adds a new product to the Workspace
     * @param oMessage
     */
    EditorController.prototype.receivedNewProductMessage = function (oMessage) {

        var sMessage = this.m_oTranslate.instant("MSG_EDIT_PRODUCT_ADDED");

        // Alert the user
        var oDialog = utilsVexDialogAlertBottomRightCorner(sMessage);
        utilsVexCloseDialogAfter(4000, oDialog);

        // Update product list
        this.getProductListByWorkspace();

        //the m_oLastDownloadedProduct will be select & open in jstree
        this.m_oLastDownloadedProduct = oMessage.payload.fileName;

    };


    /**
     * Handler of the "publish" message
     * @param oMessage
     */
    EditorController.prototype.receivedPublishMessage = function (oMessage) {
        if (oMessage == null) return;
        if (oMessage.messageResult == "KO") {
            var sMessage = this.m_oTranslate.instant("MSG_PUBLISH_ERROR");
            utilsVexDialogAlertTop(sMessage);
            return;
        }
    };


    /**
     * Handler of the "PUBLISHBAND" message
     * @param oMessage
     */
    EditorController.prototype.receivedPublishBandMessage = function (oMessage) {

        // Get the payload
        var oPublishedBand = oMessage.payload;

        // Check if it is valid
        if (utilsIsObjectNullOrUndefined(oPublishedBand)) {
            console.log("EditorController.receivedPublishBandMessage: Error Published band is empty...");
            return false;
        }

        // Get the Tree Node

        var sNodeID = oPublishedBand.productName + "_" + oPublishedBand.bandName;
        var oNode = $('#jstree').jstree(true).get_node(sNodeID);

        if (utilsIsObjectNullOrUndefined(oNode.original)) {
            console.log("EditorController.receivedPublishBandMessage: impossible to find the Tree node for the published band !!");
            return false;
        }

        // Get the original Band Object stored in the node
        var oBand = oNode.original.band;

        // Update the Band informations
        oBand.bVisibleNow = true;
        oBand.layerId = oPublishedBand.layerId;
        oBand.published = true;
        oBand.bbox = oPublishedBand.boundingBox;
        oBand.geoserverBoundingBox = oPublishedBand.geoserverBoundingBox;
        oBand.geoserverUrl = oPublishedBand.geoserverUrl;
        oBand.showLegend=false;
        oBand.legendUrl = this.getBandLegendUrl(oBand)

        // Set the tree node as selected and published
        this.setTreeNodeAsSelected(sNodeID);
        this.setTreeNodeAsPublished(sNodeID);

        // Add layer in list
        // check if the background is in Editor Mode or in Georeferenced Mode
        if (this.m_b2DMapModeOn == false) {
            var oRectangleIsNotGeoreferencedProduct = this.productIsNotGeoreferencedRectangle3DMap(oBand.geoserverBoundingBox, oBand.bbox, oBand.layerId);
            if (utilsIsObjectNullOrUndefined(oRectangleIsNotGeoreferencedProduct) === false) {
                this.addLayerMap3DByServer(oBand.layerId, oBand.geoserverUrl);
                var oLayer3DMap = {
                    id: oBand.layerId,
                    rectangle: oRectangleIsNotGeoreferencedProduct
                };
                this.m_aoProductsLayersIn3DMapArentGeoreferenced.push(oLayer3DMap);
            }

            //if we are in 3D put the layer on the globe
            this.addLayerMap3DByServer(oBand.layerId, oBand.geoserverUrl);
        } else {
            var sColor = "#f22323";
            var sGeoserverBBox = oBand.geoserverBoundingBox;
            this.productIsNotGeoreferencedRectangle2DMap(sColor, sGeoserverBBox, oBand.bbox, oBand.layerId);
            //if we are in 2D put it on the map
            this.addLayerMap2DByServer(oBand.layerId, oBand.geoserverUrl);

        }
        // show the layer with full opacity at ther beginning
        oBand.opacity = 100;
        this.m_aoVisibleBands.unshift(oBand);

        if (this.m_aoVisibleBands.length == 1) {

            if (!this.m_bFirstZoomOnBandDone) {
                this.m_iActiveMapPanelTab = 1; 
                // Make auto zoom only once
                this.m_bFirstZoomOnBandDone = true;

                //if there isn't Bounding Box is impossible to zoom
                if (!utilsIsStrNullOrEmpty(oBand.geoserverBoundingBox)) {
                    this.m_oGlobeService.zoomBandImageOnGeoserverBoundingBox(oBand.geoserverBoundingBox);
                    this.m_oMapService.zoomBandImageOnGeoserverBoundingBox(oBand.geoserverBoundingBox);
                } else {
                    this.m_oMapService.zoomBandImageOnBBOX(oBand.bbox);
                    this.m_oGlobeService.zoomBandImageOnBBOX(oBand.bbox);
                }
            }
        }
    };

    /*********************************************************** DATA ACCESS ***********************************************************/


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
    };

    /**
     * Check if the user is logged or not
     */
    EditorController.prototype.isUserLogged = function () {
        return this.m_oConstantsService.isUserLogged();
    };


    /**
     * Get a list of bands for a product
     * @param oProductItem
     * @returns {Array}
     */
    EditorController.prototype.getBandsForProduct = function (oProduct) {
        var asBands = [];

        var aoBands = oProduct.bandsGroups.bands;

        var iBandCount = 0;

        if (!utilsIsObjectNullOrUndefined(aoBands)) {
            iBandCount = aoBands.length;
        }

        for (var i = 0; i < iBandCount; i++) {
            var oBandItem = {};
            oBandItem.name = aoBands[i].name;
            oBandItem.productName = oProduct.name;
            oBandItem.productIndex = oProduct.selfIndex;
            oBandItem.height = aoBands[i].height;
            oBandItem.width = aoBands[i].width;

            if (!utilsIsObjectNullOrUndefined(aoBands[i].published)) {
                oBandItem.published = aoBands[i].published;
            } else {
                if (utilsIsStrNullOrEmpty(aoBands[i].layerId)) oBandItem.published = false;
                else oBandItem.published = true;
            }

            oBandItem.layerId = aoBands[i].layerId;
            oBandItem.bVisibleNow = false;
            oBandItem.bbox = oProduct.bbox;
            oBandItem.geoserverBoundingBox = aoBands[i].geoserverBoundingBox;
            oBandItem.geoserverUrl = aoBands[i].geoserverUrl;

            asBands.push(oBandItem);
        }

        return asBands;
    };


    /**
     * Get the list of products for a Workspace
     */
    EditorController.prototype.getProductListByWorkspace = function () {
        var oController = this;

        if (utilsIsObjectNullOrUndefined(oController.m_oActiveWorkspace)) return;

        this.m_oProductService.getProductListByWorkspace(oController.m_oActiveWorkspace.workspaceId).then(function (data, status) {

            if (utilsIsObjectNullOrUndefined(data.data) == false) {

                oController.m_aoProducts = []

                //push all products
                for (var iIndex = 0; iIndex < data.data.length; iIndex++) {

                    //check if friendly file name isn't null
                    if (utilsIsObjectNullOrUndefined(data.data[iIndex].productFriendlyName) == true) {
                        data.data[iIndex].productFriendlyName = data.data[iIndex].name;
                    }

                    // Add the product to the list
                    oController.m_aoProducts.push(data.data[iIndex]);
                }

                // i need to make the tree after the products are loaded
                oController.m_oTree = oController.generateTree();
                oController.m_bIsLoadingTree = false;

                if (oController.m_b2DMapModeOn === false) {
                    oController.m_oMapService.addAllWorkspaceRectanglesOnMap(oController.m_aoProducts);
                    oController.m_oMapService.flyToWorkspaceBoundingBox(oController.m_aoProducts);

                } else {
                    oController.m_oGlobeService.addAllWorkspaceRectanglesOnMap(oController.m_aoProducts);
                    oController.m_oGlobeService.flyToWorkspaceBoundingBox(oController.m_aoProducts);
                }


            }
        }, (function (data, status) {
            var sMessage = this.m_oTranslate.instant("MSG_PRODUCT_LIST_ERROR")
            utilsVexDialogAlertTop(sMessage);
        }));
    };

    /**
     * Open a Workspace and reload it whe the page is reloaded
     * @param sWorkspaceId
     */
    EditorController.prototype.openWorkspace = function (sWorkspaceId) {

        var oController = this;

        this.m_oWorkspaceService.getWorkspaceEditorViewModel(sWorkspaceId).then(function (data, status) {
            if (data.data != null) {
                if (data.data != undefined) {
                    // new condition: check that the viewmodel received is non empty 
                    if (data.data.workspaceId == null || data.data.activeNode === false){
                        oController.m_oState.go("home");
                        var sMessage = this.m_oTranslate.instant("MSG_FORBIDDEN")
                        var oDialog = utilsVexDialogAlertTop(sMessage);
                        utilsVexCloseDialogAfter(10000 , oDialog);
                    }
                    else{
                    oController.m_oConstantsService.setActiveWorkspace(data.data);
                    oController.m_oActiveWorkspace = oController.m_oConstantsService.getActiveWorkspace();

                    oController.getProductListByWorkspace();
                    oController.m_oProcessWorkspaceService.loadProcessesFromServer(oController.m_oActiveWorkspace.workspaceId);

                    if (oController.m_oRabbitStompService.isSubscrbed() == false) {
                        oController._subscribeToRabbit();
                    }
                    }
                }
            }
        }, (function (data, status) {
            var sMessage = this.m_oTranslate.instant("MSG_ERROR_READING_WS");
            utilsVexDialogAlertTop(sMessage)
        }));

        return true;
    };


    /**
     * Finds a product from the file name
     * @param sFileNameInput
     * @returns {*}
     */
    EditorController.prototype.findProductByFileName = function (sFileNameInput) {
        if ((utilsIsObjectNullOrUndefined(sFileNameInput) == true) && (utilsIsStrNullOrEmpty(sFileNameInput) == true)) return null;
        if (utilsIsObjectNullOrUndefined(this.m_aoProducts) == true) return null;

        var iNumberOfProducts = this.m_aoProducts.length;

        if (this.m_aoProducts.length == 0) return null;

        for (var iIndexProduct = 0; iIndexProduct < iNumberOfProducts; iIndexProduct++) {
            if (this.m_aoProducts[iIndexProduct].fileName == sFileNameInput) {
                return this.m_aoProducts[iIndexProduct];
            }
        }

        return null;
    };

    /*********************************************************** VIEW BANDS, LAYERS AND MAPS ***********************************************************/

    /**
     * OPEN BAND IMAGE
     * Called from the tree to open a band
     * @param oBand
     */
    EditorController.prototype.openBandImage = function (oBand) {

        var oController = this;
        var sFileName = this.m_aoProducts[oBand.productIndex].fileName;
        var bAlreadyPublished = oBand.published;

        this.m_oActiveBand = oBand;

        // Geographical Mode On: geoserver publish band
        this.m_oFileBufferService.publishBand(sFileName, this.m_oActiveWorkspace.workspaceId, oBand.name).then(function (data, status) {

            if (!bAlreadyPublished) {
                var oDialog = utilsVexDialogAlertBottomRightCorner('PUBLISHING BAND ' + oBand.name);
                utilsVexCloseDialogAfter(4000, oDialog);  
            }

            if (oController.m_aoVisibleBands.length === 0) {
                    oController.setActiveTab(1);
            } 

            if (!utilsIsObjectNullOrUndefined(data.data) && data.data.messageResult != "KO" && utilsIsObjectNullOrUndefined(data.data.messageResult)) {
                /*if the band was published*/

                if (data.data.messageCode === "PUBLISHBAND") {
                    // Already published: we already have the View Model
                    oController.receivedPublishBandMessage(data.data);
                } else {
                    oController.m_oProcessWorkspaceService.loadProcessesFromServer(oController.m_oActiveWorkspace.workspaceId);
                    // It is publishing: we will receive Rabbit Message
                    if (data.data.messageCode !== "WAITFORRABBIT") oController.setTreeNodeAsDeselected(oBand.productName + "_" + oBand.name);
                }

            } else {
                var sMessage = this.m_oTranslate.instant("MSG_PUBLISH_BAND_ERROR");
                utilsVexDialogAlertTop(sMessage + oBand.name);
                oController.setTreeNodeAsDeselected(oBand.productName + "_" + oBand.name);
            }
        }, (function (data, status) {
            console.log('publish band error');
            var sMessage = this.m_oTranslate.instant("MSG_PUBLISH_BAND_ERROR");
            utilsVexDialogAlertTop(sMessage);
            oController.setTreeNodeAsDeselected(oBand.productName + "_" + oBand.name);
        }));
    };

    /**
     * Remove a Band Image in each mode (2d, 3d, Editor)
     * @param oBand
     * @returns {boolean}
     */
    EditorController.prototype.removeBandImage = function (oBand) {

        if (utilsIsObjectNullOrUndefined(oBand) == true) {
            console.log("Error in removeBandImage");
            return false;
        }

        // Clear the active Band
        this.m_oActiveBand = null;
        // Get the layer Id
        var sLayerId = "wasdi:" + oBand.layerId;
        // Check the actual Mode
        if (this.m_b2DMapModeOn) {
            // We are in 2d mode

            // Georeferenced: remove the band from the map
            var oMap2D = this.m_oMapService.getMap();

            //remove layer in 2D map
            oMap2D.eachLayer(function (layer) {
                var sMapLayer = layer.options.layers;
                var sMapLayer2 = "wasdi:" + layer.options.layers;

                if (utilsIsStrNullOrEmpty(sLayerId) === false && sMapLayer === sLayerId) {
                    oMap2D.removeLayer(layer);
                }
                if (utilsIsStrNullOrEmpty(sLayerId) === false && sMapLayer2 === sLayerId) {
                    oMap2D.removeLayer(layer);
                }
            });
        }
        else {
            this.removeBandLayersIn3dMaps(sLayerId);
            //if the layers isn't georeferenced remove the Corresponding rectangle
            this.removeRedSquareIn3DMap(sLayerId);
        }

        // Deselect the node
        var sOldNodeId = oBand.productName + "_" + oBand.name;
        // Deselect it
        this.setTreeNodeAsDeselected(sOldNodeId);
        // Remove it from Visible Layer List
        this.removeBandFromVisibleList(oBand);

    };

    EditorController.prototype.removeRedSquareIn3DMap = function (sLayerId) {
        var iNumberOfProdcutsLayers = this.m_aoProductsLayersIn3DMapArentGeoreferenced.length;
        for (var iIndexProductLayer = 0; iIndexProductLayer < iNumberOfProdcutsLayers; iIndexProductLayer++) {

            var sProductLayerId = "";
            if (this.m_aoProductsLayersIn3DMapArentGeoreferenced[iIndexProductLayer].hasOwnProperty('id') === true &&
                utilsIsObjectNullOrUndefined(this.m_aoProductsLayersIn3DMapArentGeoreferenced[iIndexProductLayer].id) === false) {
                sProductLayerId = "wasdi:" + this.m_aoProductsLayersIn3DMapArentGeoreferenced[iIndexProductLayer].id;
            }
            if (utilsIsStrNullOrEmpty(sLayerId) === false && sProductLayerId === sLayerId)
            {
                this.m_oGlobeService.removeEntity(this.m_aoProductsLayersIn3DMapArentGeoreferenced[iIndexProductLayer].rectangle);
                utilsRemoveObjectInArray(this.m_aoProductsLayersIn3DMapArentGeoreferenced, this.m_aoProductsLayersIn3DMapArentGeoreferenced[iIndexProductLayer]);
                break;
            }
        }
    }
    EditorController.prototype.removeAllRedSquareBoundingBox = function () {
        this.m_oGlobeService.removeAllEntities();
    };

    EditorController.prototype.removeBandLayersIn3dMaps = function (sLayerId) {
        // We are in 3d Mode
        var aoGlobeLayers = this.m_oGlobeService.getGlobeLayers();

        //Remove band layer
        for (var iIndexLayer = 0; iIndexLayer < aoGlobeLayers.length; iIndexLayer++) {
            oLayer = aoGlobeLayers.get(iIndexLayer);

            if (utilsIsStrNullOrEmpty(sLayerId) === false && utilsIsObjectNullOrUndefined(oLayer) === false && oLayer.imageryProvider.layers === sLayerId) {
                aoGlobeLayers.remove(oLayer);

                iIndexLayer = 0;
            } else {

                if (!utilsIsObjectNullOrUndefined(oLayer.imageryProvider.layers)) {
                    var sMapLayer = "wasdi:" + oLayer.imageryProvider.layers;
                    if (utilsIsStrNullOrEmpty(sLayerId) == false && utilsIsObjectNullOrUndefined(oLayer) == false && sMapLayer == sLayerId) {
                        aoGlobeLayers.remove(oLayer);
                        iIndexLayer = 0;
                    }
                }
            }

        }
    }

    /**
     * Removes a band from the list of visible Bands
     * @param oBand
     */
    EditorController.prototype.removeBandFromVisibleList = function (oBand) {
        var iVisibleBandCount = 0;

        if (utilsIsObjectNullOrUndefined(this.m_aoVisibleBands) == false) iVisibleBandCount = this.m_aoVisibleBands.length;

        for (var iIndex = 0; iIndex < iVisibleBandCount;) {

            if (this.m_aoVisibleBands[iIndex].productName == oBand.productName && this.m_aoVisibleBands[iIndex].name == oBand.name) {
                this.m_aoVisibleBands.splice(iIndex, 1);
                iVisibleBandCount--;
            } else iIndex++;
        }
    };

    /**
     * Set the opacity for the layer identified by the index
     * @param {int} iOpacity level of opacity
     * @param {string} sLayerId the name representig the band
     */
    EditorController.prototype.setLayerOpacity = function (iOpacity, sLayerId) {
        var oMap = this.m_oMapService.getMap();
        var fPercentage = iOpacity / 100;
        var layers = [];
        oMap.eachLayer(function (layer) {
            if (layer instanceof L.TileLayer) {
                if (!utilsIsObjectNullOrUndefined(layer.options.layers)) {
                    // first condition covers the downloaded images, the second one is for uploaded band image
                    if (layer.options.layers == ("wasdi:" + sLayerId) || layer.options.layers == sLayerId) {
                        layer.setOpacity(fPercentage);
                    }
                }
            }

        });

    }

    /**
     * Add layer on 3d map from a specific server
     * @param sLayerId
     */
    EditorController.prototype.addLayerMap3DByServer = function (sLayerId, sServer) {
        if (sLayerId == null) return false;
        if (sServer == null) sServer = this.m_oConstantsService.getWmsUrlGeoserver();

        var oGlobeLayers = this.m_oGlobeService.getGlobeLayers();

        var oWMSOptions = { // wms options
            transparent: true,
            format: 'image/png'
        };

        // WMS get GEOSERVER
        var oProvider = new Cesium.WebMapServiceImageryProvider({
            url: sServer,
            layers: sLayerId,
            parameters: oWMSOptions

        });

        oGlobeLayers.addImageryProvider(oProvider);
    };

    /**
     * Add a layer from a specific server on the 2D map
     * @param sLayerId
     */
    EditorController.prototype.addLayerMap2DByServer = function (sLayerId, sServer) {
        // Chech input data
        if (sLayerId == null) return false;
        if (sServer == null) sServer = this.m_oConstantsService.getWmsUrlGeoserver();

        var oMap = this.m_oMapService.getMap();

        var wmsLayer = L.tileLayer.betterWms(sServer, {
            layers: sLayerId,
            format: 'image/png',
            transparent: true,
            noWrap: true
        });
        wmsLayer.setZIndex(1000);//it set the zindex of layer in map
        wmsLayer.addTo(oMap);
        return true;
    };

    /**
     * synchronize the 3D Map and 2D map
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
    };

    /**
     * synchronize the 2D Map and 3D map
     * @returns {boolean}
     */
    EditorController.prototype.synchronize2DMap = function () {

        var oMap = this.m_oMapService.getMap();
        var aCenter = this.m_oGlobeService.getMapCenter();

        if (utilsIsObjectNullOrUndefined(aCenter)) return false;

        oMap.flyTo(aCenter);

        return true;
    };

    EditorController.prototype.hideOrShowPixelInfo = function () {
        this.m_bIsVisiblePixelInfo = !this.m_bIsVisiblePixelInfo;

        if (this.m_bIsVisiblePixelInfo == true) {
            $('.leaflet-popup-pane').css({ "visibility": "visible" });
            $('.leaflet-container').css('cursor', 'crosshair');
        } else {
            $('.leaflet-popup-pane').css({ "visibility": "hidden" });
            $('.leaflet-container').css('cursor', '');
        }
    };


    /**
     * Handler of the "Home" button of the view
     */
    EditorController.prototype.goWorkspaceHome = function () {
        if (this.m_b2DMapModeOn) {
            this.m_oMapService.flyToWorkspaceBoundingBox(this.m_aoProducts);
        } else {
            this.m_oGlobeService.flyToWorkspaceBoundingBox(this.m_aoProducts);
        }
    };


    /*********************************************************** SHOW MODALS ***********************************************************/

    /**
     *
     * @returns {boolean}
     */
    EditorController.prototype.openGetCapabilitiesDialog = function () {
        var oController = this;
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
    };

    /**
     *
     * @returns {boolean}
     */
    EditorController.prototype.openProcessorDialog = function (oWindow) {
        var oController;
        if (utilsIsObjectNullOrUndefined(oWindow) === true) {
            oController = this;
        } else {
            oController = oWindow;
        }

        oController.m_oModalService.showModal({
            templateUrl: "dialogs/processor/ProcessorView.html",
            controller: "ProcessorController",
            inputs: {
                extras: {
                    processor: null
                }
            }
        }).then(function (modal) {
            modal.element.modal();
            modal.close.then(function (oResult) {
                oController.m_oProcessWorkspaceService.loadProcessesFromServer(oController.m_oActiveWorkspace.workspaceId);
            });
        });

        return true;
    };

    /**
     *
     * @returns {boolean}
     */
    EditorController.prototype.openJupyterNotebookPage = function (oWindow) {
        var oController;
        if (utilsIsObjectNullOrUndefined(oWindow) === true) {
            oController = this;
        } else {
            oController = oWindow;
        }
        let oActiveProject = oController.m_oConstantsService.getActiveProject();
        let asActiveSubscriptions = oController.m_oConstantsService.getActiveSubscriptions(); 
        if(asActiveSubscriptions.length === 0) {
            let sMessage = "You do not have an Active Subscription right now.<br>Click 'OK' to visit our purchase page";
            utilsVexDialogConfirm(sMessage, function(value) {
                if(value) {
                    oController.m_oState.go("root.subscriptions", {});
                }
            }); 
            return false;
        }

        if(utilsIsObjectNullOrUndefined(oActiveProject)) {
            utilsVexDialogAlertTop("You do not have an active project right now.<br>Please make a selection.")
            return false;
        }
        oController.m_oConsoleService.createConsole(oController.m_oActiveWorkspace.workspaceId)
        .then(
            function (data) {
                if (utilsIsObjectNullOrUndefined(data.data) === false && data.data.boolValue === true) {
                    // Request accepted
                    if (data.data.stringValue.includes("http")) {
                        oController.m_oWindow.open(data.data.stringValue, '_blank');
                    } else {
                        sMessage = "WASDI IS PREPARING YOUR NOTEBOOK"
        
                        if (utilsIsObjectNullOrUndefined(data.data) === false) {
                            if (utilsIsObjectNullOrUndefined(data.data.stringValue) === false) {
                                sMessage = sMessage + "<BR>" + data.data.stringValue;
                            }
                        }
                        oController.m_bNotebookIsReady = true;
                        utilsVexDialogAlertTop(sMessage);
                    }
                } else {
                    sMessage = "GURU MEDITATION<br>ERROR OPENING THE JUPYTER NOTEBOOK"

                    if (utilsIsObjectNullOrUndefined(data.data) === false) {
                        if (utilsIsObjectNullOrUndefined(data.data.stringValue) === false) {
                            sMessage = sMessage + ": " + data.data.stringValue;
                        }
                    }

                    utilsVexDialogAlertTop(sMessage);
                }

            },
            function (error) {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR OPENING THE JUPYTER NOTEBOOK");
            }
        );

        return true;

        // oController.m_oConsoleService.createConsole(oController.m_oActiveWorkspace.workspaceId)
        // .then(
        //     function (data) {
        //         if (utilsIsObjectNullOrUndefined(data.data) === false && data.data.boolValue === true) {
        //             // Request accepted
        //             if (data.data.stringValue.includes("http")) {
        //                 oController.m_oWindow.open(data.data.stringValue, '_blank');
        //             } else {
        //                 sMessage = "WASDI IS PREPARING YOUR NOTEBOOK"
        
        //                 if (utilsIsObjectNullOrUndefined(data.data) === false) {
        //                     if (utilsIsObjectNullOrUndefined(data.data.stringValue) === false) {
        //                         sMessage = sMessage + "<BR>" + data.data.stringValue;
        //                     }
        //                 }
        //                 oController.m_bNotebookIsReady = true;
        //                 utilsVexDialogAlertTop(sMessage);
        //             }
        //         } else {
        //             sMessage = "GURU MEDITATION<br>ERROR OPENING THE JUPYTER NOTEBOOK"

        //             if (utilsIsObjectNullOrUndefined(data.data) === false) {
        //                 if (utilsIsObjectNullOrUndefined(data.data.stringValue) === false) {
        //                     sMessage = sMessage + ": " + data.data.stringValue;
        //                 }
        //             }

        //             utilsVexDialogAlertTop(sMessage);
        //         }

        //     },
        //     function (error) {
        //         utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR OPENING THE JUPYTER NOTEBOOK");
        //     }
        // );

        // return true;
    };

    /**
     *
     * @returns {boolean}
     */
    EditorController.prototype.openWorkflowManagerDialog = function (oWindow) {
        var oController;
        if (utilsIsObjectNullOrUndefined(oWindow) === true) {
            oController = this;
        } else {
            oController = oWindow;
        }

        oController.m_oModalService.showModal({
            templateUrl: "dialogs/workflow_manager/WorkFlowManagerView.html",
            controller: "WorkFlowManagerController",
            inputs: {
                extras: {
                    products: oController.m_aoProducts,
                    workflowId: oController.m_oActiveWorkspace.workspaceId
                }
            }
        }).then(function (modal) {
            modal.element.modal();
            modal.close.then(function (oResult) {

                oController.m_oProcessWorkspaceService.loadProcessesFromServer(oController.m_oActiveWorkspace.workspaceId);
            });
        });

        return true;
    };


    /**
     *
     * @returns {boolean}
     */
     EditorController.prototype.openStyleManagerDialog = function (oWindow) {
        var oController;
        if (utilsIsObjectNullOrUndefined(oWindow) === true) {
            oController = this;
        } else {
            oController = oWindow;
        }

        oController.m_oModalService.showModal({
            templateUrl: "dialogs/style_manager/StyleManagerView.html",
            controller: "StyleManagerController",
            inputs: {
                extras: {
                    products: oController.m_aoStyleList,
                }
            }
        }).then(function (modal) {
            modal.element.modal();
            modal.close.then(function (oResult) {
            });
        });

        return true;
    };

    /**
     *
     * @param oWindow
     * @returns {boolean}
     */
    EditorController.prototype.openImportsDialog = function (oWindow) {
        var oController;
        if (utilsIsObjectNullOrUndefined(oWindow) === true) {
            oController = this;
        } else {
            oController = oWindow;
        }

        oController.m_oModalService.showModal({
            templateUrl: "dialogs/Import/ImportView.html",
            controller: "UploadController",
            inputs: {
                extras: {
                    WorkSpaceId: oController.m_oActiveWorkspace.workspaceId
                }
            }
        }).then(function (modal) {
            modal.element.modal();
            modal.close.then(function (oResult) {

            });
        });

        return true;
    };

    /**
     *
     * @param oWindow
     * @returns {boolean}
     */
    EditorController.prototype.openWorkspaceDetailsDialog = function (oWindow) {
        var oController;
        if (utilsIsObjectNullOrUndefined(oWindow) === true) {
            oController = this;
        } else {
            oController = oWindow;
        }
        // Before opening the modal window get the workspaceViewModel
        // also, before opening get the node list
        oController.m_oNodeService.getNodesList()
            .then(function (data, status) {
                if (data.data != null && data.data != undefined) {
                    oController.m_aoNodesList = [];
                    for (var iIndex = 0; iIndex < data.data.length; iIndex++) {
                        oController.m_aoNodesList.push(data.data[iIndex]);
                    }
                    oController.m_oWorkspaceService.getWorkspaceEditorViewModel(oController.m_oActiveWorkspace.workspaceId)
                        .then(function (data, status) {
                            if (data.data != null && data.data != undefined) {
                                oController.m_oActiveWorkspace = data.data;
                            }

                            oController.m_oModalService.showModal({
                                templateUrl: "dialogs/workspace_details/WorkspaceDetails.html",
                                controller: "WorkspaceDetailsController",
                                inputs: {
                                    extras: {// in extras method are not evaluated <-> pass values
                                        WorkSpaceId: oController.m_oActiveWorkspace.workspaceId,
                                        WorkSpaceViewModel: oController.m_oActiveWorkspace,
                                        ProductCount: oController.m_aoProducts.length,
                                        NodeList: oController.m_aoNodesList

                                    }
                                }
                            }).then(function (modal) {
                                oController.m_oWorkspaceService.getWorkspaceEditorViewModel(oController.m_oActiveWorkspace.workspaceId)
                                    .then(function (data, status) {
                                        if (data.data != null && data.data != undefined) {
                                            oController.m_oActiveWorkspace = data.data;
                                        }
                                    }, function (data, status) {
                                    });
                                modal.element.modal();
                                modal.close.then(function (oResult) {
                                });
                            });
                        }, function (data, status) {
                        });
                }
            }, (function (data, status) {
            }));


        return true;
    };

    EditorController.prototype.openWappsDialog = function (oWindow) {
        var oController;
        if (utilsIsObjectNullOrUndefined(oWindow) === true) {
            oController = this;
        } else {
            oController = oWindow;
        }
        oController.m_oModalService.showModal({
            templateUrl: "dialogs/wapps/WappsViewDialog.html",
            controller: "WappsController",
            inputs: {
                extras: {}
            }
        }).then(function (modal) {
            modal.element.modal({
                backdrop: 'static',
                keyboard: false
            });
            modal.close.then(function (oResult) {

            });
        });
    };

    EditorController.prototype.openShareDialog = function (oWindow) {
        var oController;
        if (utilsIsObjectNullOrUndefined(oWindow) === true) {
            oController = this;
        } else {
            oController = oWindow;
        }
        oController.m_oModalService.showModal({
            templateUrl: "dialogs/share_workspace/ShareWorkspaceDialog.html",
            controller: "ShareWorkspaceController",
            inputs: {
                extras: {
                    workspace: oController.m_oActiveWorkspace
                }
            }
        }).then(function (modal) {
            modal.element.modal();
            modal.close.then(function (oResult) {

            });
        });
    };

    /**
     * When user right click on a product and choose 'Properties' a dialog
     * will be opened to show product properties
     * @param oProductInput
     * @returns {boolean}
     */
    EditorController.prototype.openProductInfoDialog = function (oProductInput) {

        var oController = this;

        this.m_oModalService.showModal({
            templateUrl: "dialogs/product_editor_info/ProductEditorInfoDialog.html",
            controller: "ProductEditorInfoController",
            inputs: {
                extras: {
                    product: oProductInput
                }
            }
        }).then(function (modal) {
            modal.element.modal();
            modal.close.then(function (result) {
                if (utilsIsObjectNullOrUndefined(result) === true) {
                    return false;
                }
                //Only fetch list if Product was changed
                if (result === true) {
                    oController.getProductListByWorkspace();
                }
            });
        });

        return true;
    };

    /**
     * When user right click on a product and choose 'Share' a dialog
     * will be opened to allow the sharing of the product with other workspaces
     * @param oProductInput
     * @returns {boolean}
     */
    EditorController.prototype.openProductShareDialog = function (oProduct) {

        if(utilsIsObjectNullOrUndefined(oProduct) === true)
        {
            return false;
        }

        var oController = this;

        var sMessage = this.m_oTranslate.instant("MSG_SHARE_WITH_WS");
        var sErrorMessage = this.m_oTranslate.instant("MSG_ERROR_SHARING");

        var oOptions = {
            titleModal: sMessage,
            buttonName: sMessage,
            excludedWorkspaceId: this.m_oActiveWorkspace.workspaceId,
            currentNodeCode: this.m_oActiveWorkspace.nodeCode
        };

        var oThat = this;
        var oCallback = function(result)
        {
            if(utilsIsObjectNullOrUndefined(result) === true)
            {
                return false;
            }

            var aoWorkSpaces = result;
            var iNumberOfWorkspaces = aoWorkSpaces.length;
            if(utilsIsObjectNullOrUndefined(aoWorkSpaces) )
            {
                console.log("Error there aren't Workspaces");
                return false;
            }

            // download product in all workspaces
            for(var iIndexWorkspace = 0 ; iIndexWorkspace < iNumberOfWorkspaces; iIndexWorkspace++)
            {

                oProduct.isDisabledToDoDownload = true;
                var sUrl = oProduct.link;
                var oError = function (data,status) {
                            utilsVexDialogAlertTop(sErrorMessage);
                            oProduct.isDisabledToDoDownload = false;
                        };

                var sBound = "";

                if (utilsIsObjectNullOrUndefined(oProduct.bounds) == false) {
                    sBound = oProduct.bounds.toString();
                }
//                oThat.shareProduct(sUrl,oProduct.title, aoWorkSpaces[iIndexWorkspace].workspaceId,sBound,oProduct.provider,null,oError);
                let sOriginWorkspaceId = oController.m_oActiveWorkspace.workspaceId;
                let sDestinationWorkspaceId = aoWorkSpaces[iIndexWorkspace].workspaceId;
                let sProductName = oProduct.fileName; 
                oThat.shareProduct(sOriginWorkspaceId, sDestinationWorkspaceId, sProductName, null, oError);

            }

            oThat.deselectAllProducts();

            return true;
        };

        utilsProjectOpenGetListOfWorkspacesSelectedModal(oCallback,oOptions,this.m_oModalService);
    
    };

    EditorController.prototype.shareProduct = function(sOriginWorkspaceId, sDestinationWorkspaceId, sProductName, oCallback, oError)
    {

        if(utilsIsObjectNullOrUndefined(oCallback) === true)
        {
            var sMessage = this.m_oTranslate.instant("MSG_SHARING");
            oCallback = function (data, status) {
                var oDialog = utilsVexDialogAlertBottomRightCorner(sMessage);
                utilsVexCloseDialogAfter("3000",oDialog);
            }
        }
        if(utilsIsObjectNullOrUndefined(oError) === true)
        {
            var sMessage = this.m_oTranslate.instant("MSG_ERROR_SHARING");
            oError = function (data,status) {
                utilsVexDialogAlertTop(sMessage);
                // oProduct.isDisabledToDoDownload = false;
            };
        }
        this.m_oFileBufferService.share(sOriginWorkspaceId, sDestinationWorkspaceId, sProductName).then(oCallback, oError);
    };

    EditorController.prototype.openTransferToFtpDialog = function (oSelectedProduct, oWindow) {

        var oController;

        if (utilsIsObjectNullOrUndefined(oWindow) === true) {
            oController = this;
        } else {
            oController = oWindow;
        }

        this.m_oModalService.showModal({
            templateUrl: "dialogs/ftp_service/FTPView.html",
            controller: "FTPController",
            inputs: {
                extras: {
                    products: oController.m_aoProducts,
                    selectedProduct: oSelectedProduct
                }
            }
        }).then(function (modal) {
            modal.element.modal();
            modal.close.then(function (oResult) {
                return true;
            });
        });

        return true;
    };

    EditorController.prototype.getMapContainerSize = function (iScalingValue) {
        var elementMapContainer = angular.element(document.querySelector('#mapcontainer'));
        var heightMapContainer = elementMapContainer[0].offsetHeight * iScalingValue;
        var widthMapContainer = elementMapContainer[0].offsetWidth * iScalingValue;

        return {
            height: heightMapContainer,
            width: widthMapContainer
        };

    }
    /*********************************************************** OPERATION MENU ***********************************************************/


    EditorController.prototype.isHiddenOperationMainBar = function () {
        return this.m_oAreHideBars.mainBar;
    };

    /*********************************************************** CSS CHANGE ***********************************************************/


    EditorController.prototype.changeModeOnOffPixelInfo = function () {
        this.m_bIsModeOnPixelInfo = !this.m_bIsModeOnPixelInfo;
    };

    EditorController.prototype.getClassPixelInfo = function () {
        if (this.m_bIsModeOnPixelInfo)
            return "#009036";//green
        else
            return "#43516A";//white

    };

    EditorController.prototype.goSearch = function () {
        this.m_oState.go("root.import", {});
    };

    /********************************************************** TREE FUNCTIONS *********************************************************************/

    EditorController.prototype.isHideTree = function () {
        return ((this.m_oTree === null) || (this.m_oTree.core.data.length === 0));
    };

    /**
     * Set a node as deselected
     * @param sNode
     */
    EditorController.prototype.setTreeNodeAsDeselected = function (sNode) {
        $("#jstree").jstree().enable_node(sNode);
        $('#jstree').jstree(true).set_icon(sNode, 'assets/icons/uncheck_20x20.png');

        // It is no more visible now
        var oNode = $('#jstree').jstree(true).get_node(sNode);
        if (utilsIsObjectNullOrUndefined(oNode.original) == false) oNode.original.band.bVisibleNow = false;
    };


    /**
     * Set a node as selected
     * @param sNode
     */
    EditorController.prototype.setTreeNodeAsSelected = function (sNode) {
        $("#jstree").jstree().enable_node(sNode);
        $('#jstree').jstree(true).set_icon(sNode, 'assets/icons/check_20x20.png');

        var oNode = $('#jstree').jstree(true).get_node(sNode); //oLayer.layerId
        if (utilsIsObjectNullOrUndefined(oNode.original) == false) oNode.original.band.bVisibleNow = true;

    };

    /**
     * Set a node as published
     * @param sNode
     */
    EditorController.prototype.setTreeNodeAsPublished = function (sNode) {
        var sLabelText = $("#jstree").jstree().get_text(sNode);
        sLabelText = sLabelText.replace("band-not-published-label", "band-published-label");
        utilsJstreeUpdateLabelNode(sNode, sLabelText);
    };


    /**
     * Enable a Node (is possible to click)
     * @param sNode
     */
    EditorController.prototype.setTreeNodeEnabled = function (sNode) {
        $("#jstree").jstree().enable_node(sNode);
    };

    /**
     * Disable a Node (impossible to click)
     * @param sNode
     */
    EditorController.prototype.setTreeNodeDisabled = function (sNode) {
        $("#jstree").jstree().disable_node(sNode);
    };

    /**
     * Opens all the visible bands in the tree
     */
    EditorController.prototype.openPublishedBandsInTree = function () {

        var oTreeInst = $('#jstree').jstree(true);
        var oModelData = oTreeInst._model.data;
        for (var iModel in oModelData) {
            if (!utilsIsObjectNullOrUndefined(oModelData[iModel].original) &&
                !utilsIsObjectNullOrUndefined(oModelData[iModel].original.band) &&
                oModelData[iModel].original.band.published == true) {
                $("#jstree").jstree("_open_to", oModelData[iModel].id);
            }
        }
    };

    /**
     * Selects a node of the tree from the file name
     * @param sFileName
     * @returns {boolean}
     */
    EditorController.prototype.selectNodeByFileNameInTree = function (sFileName) {
        if (utilsIsObjectNullOrUndefined(sFileName) == true) return false;

        var treeInst = $('#jstree').jstree(true);
        var m = treeInst._model.data;
        for (var i in m) {
            if (!utilsIsObjectNullOrUndefined(m[i].original) && m[i].original.fileName == sFileName) {//&& !utilsIsObjectNullOrUndefined(m[i].original.band)
                $("#jstree").jstree(true).deselect_all();
                // CARE WE CAN'T DO OPEN_NODE AND SELECET_NODE AT THE SAME TIME
                $("#jstree").jstree(true).select_node(m[i].id, true);
                break;
            }
        }
        return true;
    };

    /**
     * Rename a node in the tree
     * @param sFileName
     * @param sNewNameInput
     * @returns {boolean}
     */
    EditorController.prototype.renameNodeInTree = function (sFileName, sNewNameInput) {
        if ((utilsIsObjectNullOrUndefined(sNewNameInput) == true) || (utilsIsStrNullOrEmpty(sNewNameInput) == true)) return false;

        var treeInst = $('#jstree').jstree(true);
        var m = treeInst._model.data;
        for (var i in m) {
            if (!utilsIsObjectNullOrUndefined(m[i].original) && m[i].original.fileName == sFileName) {
                $("#jstree").jstree(true).rename_node(m[i].id, sNewNameInput);
                break;
            }
        }
        return true;
    };


    /**
     * GENERATE TREE
     * Expected format of the node (there are no required fields)
     *{
     *    id          : "string" // will be autogenerated if omitted
     *    text        : "string" // node text
     *    icon        : "string" // string for custom
     *    state       : {
     *        opened    : boolean  // is the node open
     *        disabled  : boolean  // is the node disabled
     *        selected  : boolean  // is the node selected
     *    },
     *    children    : []  // array of strings or objects
     *    li_attr     : {}  // attributes for the generated LI node
     *    a_attr      : {}  // attributes for the generated A node
     *}
     * Alternative format of the node (id & parent are required)
     *{
     *    id          : "string" // required
     *    parent      : "string" // required
     *    text        : "string" // node text
     *    icon        : "string" // string for custom
     *    state       : {
     *        opened    : boolean  // is the node open
     *        disabled  : boolean  // is the node disabled
     *        selected  : boolean  // is the node selected
     *    },
     *    li_attr     : {}  // attributes for the generated LI node
     *    a_attr      : {}  // attributes for the generated A node
     *}
     *
     *
     * @returns {{core: {data: Array, check_callback: boolean}, state: {key: string}, plugins: string[], contextmenu: {items: items}}}
     */
    EditorController.prototype.generateTree = function () {
        var oController = this;
        var oTree = {
            core: { data: [], check_callback: true },
            state: { key: "state_tree" },
            plugins: ["checkbox", "contextmenu", "search", "sort"], // plugins in use
            search: {
                show_only_matches: true,
                show_only_matches_children: true,
            },
            contextmenu: {
                // my right click menu
                // this method deselect all the other nodes so the node selection will be triggered by open menu
                select_node: false,
                items: function ($node) {
                    // select the current node
                    //  $node.state.selected = trues
                    oController.selectClickedNode($node);
                    //only the band has property $node.original.band
                    // menu showed when a band is selecte
                    var oReturnValue = null;

                    var sZoom2D =
                        oController.m_oTranslate.instant("MENU_ZOOM_2D");
                    var sZoom3D =
                        oController.m_oTranslate.instant("MENU_ZOOM_3D");
                    var sDownload =
                        oController.m_oTranslate.instant("MENU_DOWNLOAD");
                    var sSendToFtp =
                        oController.m_oTranslate.instant("MENU_SEND_FTP");
                    var sDelete =
                        oController.m_oTranslate.instant("MENU_DELETE");
                    var sProperties =
                        oController.m_oTranslate.instant("MENU_PROPERTIES");
                    var sShare = oController.m_oTranslate.instant("MENU_SHARE");
                    var sDeleteConfirm =
                        oController.m_oTranslate.instant("MSG_DELETE_CONFIRM");
                    var sDeleteError =
                        oController.m_oTranslate.instant("MSG_DELETE_ERROR");
                    var sDeleteManyConfirm1 = oController.m_oTranslate.instant(
                        "MSG_DELETE_MANY_CONFIRM_1"
                    );
                    var sDeleteManyConfirm2 = oController.m_oTranslate.instant(
                        "MSG_DELETE_MANY_CONFIRM_2"
                    );

                    if (
                        utilsIsObjectNullOrUndefined($node.original.band) ==
                        false
                    ) {
                        //******************************** BAND *************************************
                        var oBand = $node.original.band;

                        oReturnValue = {
                            Zoom2D: {
                                label: sZoom2D,
                                icon: "fa fa-globe",
                                action: function (obj) {
                                    if (
                                        utilsIsObjectNullOrUndefined(oBand) ==
                                        false
                                    ) {
                                        oController.m_oMapService.zoomBandImageOnGeoserverBoundingBox(
                                            oBand.geoserverBoundingBox
                                        );
                                    }
                                },
                                _disabled: false,
                            },
                            Zoom3D: {
                                label: sZoom3D,
                                icon: "fa fa-globe",
                                action: function (obj) {
                                    if (
                                        utilsIsObjectNullOrUndefined(oBand) ==
                                        false
                                    ) {
                                        oController.m_oGlobeService.zoomBandImageOnBBOX(
                                            oBand.bbox
                                        );
                                    }
                                },
                                _disabled: false,
                            },
                            SendToFtp: {
                                label: sSendToFtp,
                                icon: "fa fa-upload",
                                _disabled:
                                    oController.getSelectedNodesFromTree(
                                        $node.original.fileName
                                    ).length > 1,
                                action: function (obj) {
                                    var sSourceFileName =
                                        $node.original.fileName;
                                    var oFound =
                                        oController.findProductByFileName(
                                            sSourceFileName
                                        );

                                    if (
                                        utilsIsObjectNullOrUndefined(oFound) ==
                                        false
                                    )
                                        oController.openTransferToFtpDialog(
                                            oFound
                                        );
                                },
                            },

                            DeleteProduct: {
                                label: sDelete,
                                icon: "fa fa-trash",

                                action: function (obj) {
                                    utilsVexDialogConfirm(
                                        sDeleteConfirm,
                                        function (value) {
                                            if (value) {
                                                bDeleteFile = true;
                                                bDeleteLayer = true;
                                                this.temp = $node.parents[1];
                                                var that = this;

                                                var oFoundProduct =
                                                    oController.m_aoProducts[
                                                        $node.original.band
                                                            .productIndex
                                                    ];

                                                oController.m_oProductService
                                                    .deleteProductFromWorkspace(
                                                        oFoundProduct.fileName,
                                                        oController
                                                            .m_oActiveWorkspace
                                                            .workspaceId,
                                                        bDeleteFile,
                                                        bDeleteLayer
                                                    )
                                                    .then(
                                                        function (data) {
                                                            oController.deleteProductInNavigation(
                                                                oController.m_aoVisibleBands,
                                                                that.temp
                                                                    .children_d
                                                            );
                                                        },
                                                        function (error) {
                                                            utilsVexDialogAlertTop(
                                                                sDeleteError
                                                            );
                                                        }
                                                    );
                                            }
                                        }
                                    );
                                },
                            },
                            Properties: {
                                label: sProperties,
                                icon: "fa fa-info-circle",
                                separator_before: true,
                                action: function (obj) {
                                    var oFoundProduct =
                                        oController.m_aoProducts[
                                            $node.original.band.productIndex
                                        ];
                                    if (
                                        utilsIsObjectNullOrUndefined(
                                            oFoundProduct
                                        ) == false
                                    )
                                        oController.openProductInfoDialog(
                                            oFoundProduct
                                        );
                                },
                            },
                        }; // menu entries
                    }

                    // only products has $node.original.fileName
                    // menu showed when a product is selected
                    if (
                        utilsIsObjectNullOrUndefined($node.original.fileName) ==
                        false
                    ) {
                        //***************************** PRODUCT ********************************************
                        oReturnValue = {
                            Download: {
                                label: sDownload,
                                icon: "fa fa-download",
                                _disabled:
                                    oController.getSelectedNodesFromTree(
                                        $node.original.fileName
                                    ).length > 100,
                                action: function (obj) {
                                    //$node.original.fileName;
                                    if (
                                        utilsIsObjectNullOrUndefined(
                                            $node.original.fileName
                                        ) == false &&
                                        utilsIsStrNullOrEmpty(
                                            $node.original.fileName
                                        ) == false
                                    ) {
                                        oController.findProductByName(
                                            $node.original.fileName
                                        );

                                        let selectedNodesFromTree = oController.getSelectedNodesFromTree(
                                            $node.original.fileName
                                        );

                                        selectedNodesFromTree.forEach(element => oController.downloadProductByName(element));
                                    }
                                },
                            },
                            Share: {
                                label: sShare,
                                icon: "fa fa-share-alt",
                                _disabled:
                                    oController.getSelectedNodesFromTree(
                                        $node.original.fileName
                                    ).length > 1,
                                separator_before: true,
                                action: function (obj) {
                                    //$node.original.fileName;
                                    if (
                                        utilsIsObjectNullOrUndefined(
                                            $node.original.fileName
                                        ) === false &&
                                        utilsIsStrNullOrEmpty(
                                            $node.original.fileName
                                        ) === false
                                    ) {
                                        var iNumberOfProdcuts =
                                            oController.m_aoProducts.length;
                                        for (
                                            var iIndexProducts = 0;
                                            iIndexProducts < iNumberOfProdcuts;
                                            iIndexProducts++
                                        ) {
                                            if (
                                                oController.m_aoProducts[
                                                    iIndexProducts
                                                ].fileName ===
                                                $node.original.fileName
                                            ) {
                                                oController.openProductShareDialog(
                                                    oController.m_aoProducts[
                                                        iIndexProducts
                                                    ]
                                                );
                                                break;
                                            }
                                        }
                                    }
                                },
                            },
                            SendToFtp: {
                                label: sSendToFtp,
                                icon: "fa fa-upload",
                                _disabled:
                                    oController.getSelectedNodesFromTree(
                                        $node.original.fileName
                                    ).length > 1,
                                action: function (obj) {
                                    var sSourceFileName =
                                        $node.original.fileName;
                                    var oFound =
                                        oController.findProductByFileName(
                                            sSourceFileName
                                        );

                                    if (
                                        utilsIsObjectNullOrUndefined(oFound) ==
                                        false
                                    )
                                        oController.openTransferToFtpDialog(
                                            oFound
                                        );
                                },
                            }, //openTransferToFtpDialog
                            DeleteSelectedProduct: {
                                label: oController.getDeleteLabel(),
                                icon: "fa fa-trash",

                                action: function (obj) {
                                    let asSelectedProducts =
                                        oController.getSelectedNodesFromTree(
                                            $node.original.fileName
                                        );
                                    // first, check that something were selected
                                    if (asSelectedProducts.length > 0) {
                                        utilsVexDialogConfirm(
                                            sDeleteManyConfirm1 +
                                                asSelectedProducts.length +
                                                sDeleteManyConfirm2,
                                            function (value) {
                                                if (value) {
                                                    bDeleteFile = true;
                                                    bDeleteLayer = true;
                                                    this.temp = $node;
                                                    var that = this;
                                                    oController.m_oProductService
                                                        .deleteProductListFromWorkspace(
                                                            asSelectedProducts,
                                                            oController
                                                                .m_oActiveWorkspace
                                                                .workspaceId,
                                                            bDeleteFile,
                                                            bDeleteLayer
                                                        )
                                                        .then(
                                                            function (data) {
                                                                // for each in asSelectedProduct
                                                                $.each(
                                                                    asSelectedProducts,
                                                                    function (
                                                                        i,
                                                                        val
                                                                    ) {
                                                                        oController.deleteProductInNavigation(
                                                                            oController.m_aoVisibleBands,
                                                                            that
                                                                                .temp
                                                                                .children_d
                                                                        );
                                                                    }
                                                                );
                                                                /// deselect all
                                                                $("#jstree")
                                                                    .jstree()
                                                                    .deselect_all(
                                                                        true
                                                                    );
                                                            },
                                                            function (error) {
                                                                utilsVexDialogAlertTop(
                                                                    sDeleteError
                                                                );
                                                            }
                                                        );
                                                }
                                            }
                                        );
                                    }
                                },
                            },
                            Properties: {
                                label: sProperties,
                                icon: "fa fa-info-circle",
                                _disabled:
                                    oController.getSelectedNodesFromTree(
                                        $node.original.fileName
                                    ).length > 1,
                                separator_before: true,
                                action: function (obj) {
                                    //$node.original.fileName;
                                    if (
                                        utilsIsObjectNullOrUndefined(
                                            $node.original.fileName
                                        ) === false &&
                                        utilsIsStrNullOrEmpty(
                                            $node.original.fileName
                                        ) === false
                                    ) {
                                        var iNumberOfProdcuts =
                                            oController.m_aoProducts.length;
                                        for (
                                            var iIndexProducts = 0;
                                            iIndexProducts < iNumberOfProdcuts;
                                            iIndexProducts++
                                        ) {
                                            if (
                                                oController.m_aoProducts[
                                                    iIndexProducts
                                                ].fileName ===
                                                $node.original.fileName
                                            ) {
                                                oController.openProductInfoDialog(
                                                    oController.m_aoProducts[
                                                        iIndexProducts
                                                    ]
                                                );
                                                break;
                                            }
                                        }
                                    }
                                },
                            },
                        };
                    }

                    return oReturnValue;
                },
            },
            sort: function (a, b) {

                let a1 = this.get_node(a);
                let b1 = this.get_node(b);
                if (oController.sSortType === 'asc') {
                    return a1.text.toUpperCase() > b1.text.toUpperCase() ? 1 : -1;
                } else if (oController.sSortType === 'desc') {
                    return a1.text.toUpperCase() > b1.text.toUpperCase() ? -1 : 1;
                } else {
                    return null
                }
            },
        };


        // For each product generate sub-node
        for (var iIndexProduct = 0; iIndexProduct < this.m_aoProducts.length; iIndexProduct++) {

            //product node
            var oNode = new Object();

            oNode.text = this.m_aoProducts[iIndexProduct].productFriendlyName;
            oNode.fileName = this.m_aoProducts[iIndexProduct].fileName;
            oNode.id = this.m_aoProducts[iIndexProduct].fileName;

            oNode.description = this.m_aoProducts[iIndexProduct].description;

            if (
                utilsIsStrNullOrEmpty(
                    this.m_aoProducts[iIndexProduct].description
                ) === true
            ) {
                oNode.description = "";
            } else {
                oNode.description =
                    this.m_aoProducts[iIndexProduct].description;
            }

            oNode.a_attr = {
                title: oNode.description,
            };


            this.m_aoProducts[iIndexProduct].selfIndex = iIndexProduct;
            oNode.productIndex = iIndexProduct;

            // var oThat = this;
            var sMetadata = oController.m_oTranslate.instant("MENU_METADATA");
            var sBands = oController.m_oTranslate.instant("MENU_BANDS");

            oNode.children = [
                {
                    "text": sMetadata,
                    "icon": "assets/icons/metadata-24.png",
                    "children": [],
                    "clicked": false,//semaphore
                    "url": oController.m_oProductService.getProductMetadata(oNode.fileName, oController.m_oActiveWorkspace.workspaceId),
                    a_attr: {
                        class: "no_checkbox"
                    }
                },
                {
                    "text": sBands,
                    "icon": "assets/icons/bandsTree.png",
                    "children": [],
                    a_attr: {
                        class: "no_checkbox"
                    }
                }
            ];

            oNode.icon = "assets/icons/product_20x20.png";
            oTree.core.data.push(oNode);

            var oaBandsItems = this.getBandsForProduct(this.m_aoProducts[iIndexProduct]);

            for (var iIndexBandsItems = 0; iIndexBandsItems < oaBandsItems.length; iIndexBandsItems++) {
                var oNode = new Object();

                //LABEL NODE
                if (oaBandsItems[iIndexBandsItems].published) {
                    oNode.text = "<span class='band-published-label'>" + oaBandsItems[iIndexBandsItems].name + "</span>";
                } else {
                    oNode.text = "<span class='band-not-published-label'>" + oaBandsItems[iIndexBandsItems].name + "</span>";
                }

                // REMOVE CHECKBOXES
                oNode.a_attr = new Object();
                oNode.a_attr.class = "no_checkbox";

                //BAND
                oNode.band = oaBandsItems[iIndexBandsItems];
                oNode.icon = "assets/icons/uncheck_20x20.png";

                oNode.id = this.m_aoProducts[iIndexProduct].name + "_" + oaBandsItems[iIndexBandsItems].name;
                oTree.core.data[iIndexProduct].children[1].children.push(oNode);
            }

        }

        return oTree;
    };

    /**
     * Creates the label for deletion command using the selected
     * product list. Gives a better feedback to the user of what he/she 's
     * operating onto.
     * @returns a string with the number of currently selected entries
     */
    EditorController.prototype.getDeleteLabel = function () {
        let iCount = this.getSelectedNodesFromTree(null).length;

        var sDeleteMany1 = this.m_oTranslate.instant("MENU_DELETE_MANY_1");
        var sDeleteMany2 = this.m_oTranslate.instant("MENU_DELETE_MANY_2");
        var sDeleteSingle = this.m_oTranslate.instant("MENU_DELETE_SINGLE");
        
        if (iCount > 1) {
            return sDeleteMany1 + iCount + sDeleteMany2;
        }
        else {
            return sDeleteSingle;
        }
    }

    /**
     * Returns all nodes with checked state
     * @param {*} oEntry
     */
    EditorController.prototype.getSelectedNodesFromTree = function (oEntry) {
        var m_oController = this;
        var node = oEntry;
        var oTree = $('#jstree').jstree(true);
        var Ids = oTree.get_selected();
        // return all the nodes selected
        // a clever way to get only the parents?
        //1) get all nodes
        //2) filters only parents
        //3) select the ones in Ids
        var idList = [];
        var jsonNodes = $('#jstree').jstree(true).get_json('#', { flat: true });
        // filter nodes by considering the following condition (class.don't contains no_checkbox and state.selected == true)
        $.each(jsonNodes, function (i, val) {
            let sClass = val.a_attr.class;
            if (val.state.selected == true && sClass == undefined &&
                (val.state.hidden == false || val.state.hidden == undefined)) { // imposed on any other node the no_checkbox class
                idList.push($(val).attr('id'));
            }
        })
        return idList;

    }
    /**
     * Utils method to select or de-select all the entries in jstree after a search is done
     * all or nothing only of visible nodes
     * @param {*} sTextQuery
     */
    EditorController.prototype.selectFiltered = function () {
        this.m_bAllSelected = !this.m_bAllSelected; // flip the value
        // gather all nodes from tree
        var jsonNodes = $('#jstree').jstree(true).get_json('#', { flat: true });
        let oController = this;
        // get only the parents
        var idList = [];
        $.each(jsonNodes, function (i, val) {
            let sClass = val.a_attr.class;
            if (sClass == undefined) { // only parents <-> other instances have class "no_checkbox"
                if (val.state.hidden == false) { // not hidden must be selected
                    if (oController.m_bAllSelected) { $('#jstree').jstree(true).select_node($(val).attr('id')); }
                    else { $('#jstree').jstree(true).deselect_node($(val).attr('id')); }
                }
                if (val.state.hidden == true) { // hidden must be de-selected
                    $('#jstree').jstree(true).deselect_node($(val).attr('id'));
                }

            }

        })
    }


    /**
     * Utils method to select the node clicked
     * all or nothing only of visible nodes
     * @param {*} sTextQuery
     */
    EditorController.prototype.selectClickedNode = function (oNodeIn) {
        if (oNodeIn == null) return;
        // gather all nodes from tree
        var jsonNodes = $('#jstree').jstree(true).get_json('#', { flat: true });
        let oController = this;
        // get only the parents
        var idList = [];
        $.each(jsonNodes, function (i, val) {
            let sClass = val.a_attr.class;
            if (sClass == undefined) { // only parents <-> other instances have class "no_checkbox"
                
                    if ($(val).attr('id') == oNodeIn.id) { $('#jstree').jstree(true).select_node($(val).attr('id')); }
                
                if (val.state.hidden == true) { // hidden must be de-selected
                    $('#jstree').jstree(true).deselect_node($(val).attr('id'));
                }

            }

        })
    }

    EditorController.prototype.downloadProductByName = function (sFileName) {

        if (utilsIsStrNullOrEmpty(sFileName) === true) {
            return false;
        }

        var sUrl = null;
        // P.Campanella 17/03/2020: redirect of the download to the node that hosts the workspace
        if (utilsIsStrNullOrEmpty(this.m_oConstantsService.getActiveWorkspace().apiUrl) == false) {
            sUrl = this.m_oConstantsService.getActiveWorkspace().apiUrl;
        }

        var oController = this;

        this.m_oCatalogService.newDownloadByName(sFileName, this.m_oActiveWorkspace.workspaceId, sUrl)
            .then(
                function (response) {
                    var _contentType = response.headers('Content-Type');

                    var sHeaderContentDisposition = response.headers('Content-Disposition');

                    let sDownloadedFilename = sFileName;

                    if (!utilsIsStrNullOrEmpty(sHeaderContentDisposition)) {
                        sDownloadedFilename = sHeaderContentDisposition.split(';')[1].split('=')[1].replace(/\"/g, '');
                    }

                    var blob = new Blob([ response.data ], { type : _contentType });
                    var url = (window.URL || window.webkitURL).createObjectURL(blob);
                    var anchor = angular.element('<a/>');
                    anchor.attr({
                        href : url,
                        target : '_blank',
                        download : sDownloadedFilename
                    })[0].click();
                },
                function (error) {
                    console.log("EditorController.downloadProductByName | error.data.message: ", error.data.message);

                    let errorMessage = oController.m_oTranslate.instant(
                        error.data.message
                    );

                    utilsVexDialogAlertTop(errorMessage);
                }
            );

    };

    /**
     * findProductByName
     * @param sFileName
     * @returns {*}
     */
    EditorController.prototype.findProductByName = function (sFileName) {
        if (utilsIsStrNullOrEmpty(sFileName) === true) {
            return null;
        }
        var iNumberOfProducts = this.m_aoProducts.length;
        var oSelectedProduct = null;
        for (var iIndexProduct = 0; iIndexProduct < iNumberOfProducts; iIndexProduct++) {
            if (this.m_aoProducts[iIndexProduct].fileName === sFileName) {
                oSelectedProduct = this.m_aoProducts[iIndexProduct];
                break;
            }

        }
        return oSelectedProduct;

    };

    EditorController.prototype.filterTree = function (sTextQuery) {

        if (utilsIsObjectNullOrUndefined(sTextQuery) === true) {
            sTextQuery = "";
            this.m_bIsFilteredTree = false;
        } else {
            this.m_bIsFilteredTree = true;
        }

        $('#jstree').jstree(true).search(sTextQuery);

        // deselect all
        var jsonNodes = $('#jstree').jstree(true).get_json('#', { flat: true });
        // get only the parents
        var idList = [];
        $.each(jsonNodes, function (i, val) {
            let sClass = val.a_attr.class;
            if (sClass == undefined) { // only parents <-> other instances have class "no_checkbox"
                $('#jstree').jstree(true).deselect_node($(val).attr('id'));
            }
        }); // each
        this.m_bAllSelected = false;

    };

    EditorController.prototype.cleanFilterTree = function () {
        this.m_sTextQueryFilterInTree = '';
        this.filterTree(null);
    };

    EditorController.prototype.deleteProductInNavigation = function (aoVisibleBands, oChildrenNode) {

        if (this.m_b2DMapModeOn === false) {
            this.deleteProductInMap();
        } else {
            this.deleteProductInGlobe(aoVisibleBands, oChildrenNode);
        }
    };

    EditorController.prototype.deleteProductInMap = function () {
        this.m_oMapService.clearMap();
        this.m_oMapService.initWasdiMap('wasdiMap2');

        //reload product list
        this.getProductListByWorkspace();
    };

    EditorController.prototype.navigateTo = function (iIndexLayer) {
        // Check for geoserver bounding box
        if (!utilsIsStrNullOrEmpty(this.m_aoVisibleBands[iIndexLayer].geoserverBoundingBox)) {
            this.m_oGlobeService.zoomBandImageOnGeoserverBoundingBox(this.m_aoVisibleBands[iIndexLayer].geoserverBoundingBox);
            this.m_oMapService.zoomBandImageOnGeoserverBoundingBox(this.m_aoVisibleBands[iIndexLayer].geoserverBoundingBox);
            //this.saveBoundingBoxUndo(this.m_aoVisibleBands[iIndexLayer].geoserverBoundingBox, 'geoserverBB', this.m_aoVisibleBands[iIndexLayer].layerId);
        } else {
            // Try with the generic product bounding box
            this.m_oGlobeService.zoomBandImageOnBBOX(this.m_aoVisibleBands[iIndexLayer].bbox);
            this.m_oMapService.zoomBandImageOnBBOX(this.m_aoVisibleBands[iIndexLayer].bbox);
            //this.saveBoundingBoxUndo(this.m_aoVisibleBands[iIndexLayer].geoserverBoundingBox, 'BB', this.m_aoVisibleBands[iIndexLayer].layerId);

        }
    }

    EditorController.prototype.showLayerLegend = function (iLayerIndex) {
        this.m_aoVisibleBands[iLayerIndex].showLegend = !this.m_aoVisibleBands[iLayerIndex].showLegend;
    }

    EditorController.prototype.getBandLegendUrl = function (oLayer) {

        if (oLayer == null) return "";

        var sGeoserverUrl = oLayer.geoserverUrl

        if (utilsIsStrNullOrEmpty(sGeoserverUrl)) sGeoserverUrl = this.m_oConstantsService.getWmsUrlGeoserver();

        if (sGeoserverUrl.endsWith("?")) {
            sGeoserverUrl = sGeoserverUrl.replace("ows?", "wms?");
        }
        else {
            sGeoserverUrl = sGeoserverUrl.replace("ows", "wms?");
        }

        sGeoserverUrl = sGeoserverUrl + "request=GetLegendGraphic&format=image/png&WIDTH=12&HEIGHT=12&legend_options=fontAntiAliasing:true;fontSize:10&LEGEND_OPTIONS=forceRule:True&LAYER=";
        sGeoserverUrl = sGeoserverUrl + "wasdi:" + oLayer.layerId;

        return sGeoserverUrl;
    }

    EditorController.prototype.deleteProductInGlobe = function (aoVisibleBands, oChildrenNode) {
        var iLengthLayer = aoVisibleBands.length;
        var iLengthChildren_d = oChildrenNode.length;//that.temp.children_d

        for (var iIndexChildren = 0; iIndexChildren < iLengthChildren_d; iIndexChildren++) {

            for (var iIndexLayer = 0; iIndexLayer < iLengthLayer; iIndexLayer++) {
                if (oChildrenNode[iIndexChildren] === aoVisibleBands[iIndexLayer].layerId) {
                    this.removeBandImage(aoVisibleBands[iIndexChildren]);
                    break;
                }

            }

        }

        //reload product list
        this.getProductListByWorkspace();
    };

    EditorController.prototype.setSortType = function () {
        let oController = this;
        if (oController.sSortType === 'default') {
            oController.sSortType = 'asc';
        } else if (oController.sSortType === 'asc') {
            oController.sSortType = 'desc';
        } else {
            oController.sSortType = 'default'
        }

        oController.getProductListByWorkspace();
    }

    EditorController.prototype.filterNotebookButtons = function (oNotebookBtn) {
        let oController = this; 
        
        if(oController.m_bNotebookIsReady === false) {
            oNotebookBtn.caption_i18n = "EDITOR_OPERATION_TITLE_JUPYTER_NOTEBOOK_CREATE"
            
        } else {
            oNotebookBtn.caption_i18n = "EDITOR_OPERATION_TITLE_JUPYTER_NOTEBOOK_OPEN"
        }
    
    }
  
    EditorController.prototype.rabbitMessageHook = function (
        oRabbitMessage, 
        oController
    ) {
        oController.generateDefaultNavBarMenu(); 
    }

    EditorController.$inject = [
        '$rootScope',
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
        'NodeService',
        'GlobeService',
        'ProcessWorkspaceService',
        'RabbitStompService',
        'ModalService',
        '$translate',
        'CatalogService',
        'ProcessorService',
        'ConsoleService',
        '$window'
    ];

    return EditorController;
})();
window.EditorController = EditorController;
