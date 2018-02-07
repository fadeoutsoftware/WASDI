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
        this.m_oModalService = oModalService;

        // Flag to know if in the big map is 2d (true) or 3d (false)
        this.m_b2DMapModeOn = true;
        // Flag to know if the big map is the Geographical Mode (true) or in the Editor Mode (false)
        this.m_bIsActiveGeoraphicalMode = false;

        this.m_bIsLoadingTree = true;
        this.m_sToolTipBtnSwitchGeographic = "EDITOR_TOOLTIP_TO_GEO";
        this.m_sClassBtnSwitchGeographic = "btn-switch-not-geographic";
        //Flag to know if the Preview Band Image is loaded or not (2D - Editor Mode)
        this.m_bIsLoadedPreviewBandImage = true;
        //Flag to know if the Band Image is loaded or not (2D - Editor Mode)
        this.m_bIsLoadedViewBandImage = true;
        //Url of the Preview Band Image (2D - Editor Mode)
        this.m_sPreviewUrlSelectedBand = "";
        //Url of the Band Image (2D - Editor Mode)
        this.m_sViewUrlSelectedBand = "";

        // Object used to exchange information with the image preview directive
        this.m_oImagePreviewDirectivePayload = {
            originalBandWidth: 0,
            originalBandHeight: 0,
            viewportX: 0,
            viewportY: 0,
            viewportWidth: 0,
            viewportHeight: 0
        };

        // Reference to the actual active Band
        this.m_oActiveBand = null;


        this.m_oAreHideBars = {
            mainBar:false,
            radarBar:true,
            opticalBar:true,
            processorBar:true
        }

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
        // Flag to know if we are in Info mode on 2d map
        this.m_bIsModeOnPixelInfo = false;
        // Here a Workspace is needed... if it is null create a new one..
        this.m_oActiveWorkspace = this.m_oConstantsService.getActiveWorkspace();
        // Actual User
        this.m_oUser = this.m_oConstantsService.getUser();

        // Initialize the map
        oMapService.initMapEditor('wasdiMap');
        oMapService.removeLayersFromMap();
        // Initialize the globe
        this.m_oGlobeService.initGlobe('cesiumContainer2');

        //if there isn't workspace
        if (utilsIsObjectNullOrUndefined(this.m_oActiveWorkspace) && utilsIsStrNullOrEmpty(this.m_oActiveWorkspace)) {
            //if this.m_oState.params.workSpace in empty null or undefined create new workspace
            if (!(utilsIsObjectNullOrUndefined(this.m_oState.params.workSpace) && utilsIsStrNullOrEmpty(this.m_oState.params.workSpace))) {
                // Open workspace
                this.openWorkspace(this.m_oState.params.workSpace);
            }
            else {
                // go to workspaces section
                this.m_oState.go("root.workspaces");
            }
        }

        // Load Processes
        this.m_oProcessesLaunchedService.loadProcessesFromServer(this.m_oActiveWorkspace.workspaceId);
        // Load products
        this.getProductListByWorkspace();

        // Subscribe Rabbit
        if (this.m_oRabbitStompService.isSubscrbed() == false) {
            this.m_oRabbitStompService.subscribe(this.m_oActiveWorkspace.workspaceId);
        }

        //Set default value tree
        this.m_oTree = null;//IMPORTANT NOTE: there's a 'WATCH' for this.m_oTree in TREE DIRECTIVE

        // Hook to Rabbit WebStomp Service
        this.m_oRabbitStompService.setMessageCallback(this.receivedRabbitMessage);
        this.m_oRabbitStompService.setActiveController(this);

        this.drawColourManipulationHistogram("colourManipulationDiv")
    }

    /*********************************************************** VIEW METHODS**********************************************************/

    /**
     * Change location to path
     * @param sPath
     */
    EditorController.prototype.moveTo = function (sPath) {
        this.m_oLocation.path(sPath);
    }

    /**
     * Set the active tab between Navigation, Colour manipulation, Preview
     * @param iTab
     */
    EditorController.prototype.setActiveTab = function (iTab) {
        if (this.m_iActiveMapPanelTab == iTab) return;

        this.m_iActiveMapPanelTab = iTab;

        if (iTab == 2 && this.m_oActiveBand != null) {
            // Initialize Image Preview

            var oBand = this.m_oActiveBand;
            var sFileName = this.m_aoProducts[oBand.productIndex].fileName;

            var elementImagePreview = angular.element(document.querySelector('#imagepreviewcanvas'));
            var heightImagePreview = elementImagePreview[0].offsetHeight;
            var widthImagePreview = elementImagePreview[0].offsetWidth;

            // TODO: here the tab is not shown yet. So H and W are still 0.
            // This code should run after the tab is shown
            if (heightImagePreview==0) heightImagePreview = 280;//default value canvas
            if (widthImagePreview==0) widthImagePreview = 560;//default value canvas

            var oBodyImagePreview = this.createBodyForProcessingBandImage(sFileName,oBand.name,null,0,0,oBand.width,oBand.height,widthImagePreview,heightImagePreview);

            this.processingGetBandPreview(oBodyImagePreview,this.m_oActiveWorkspace.workspaceId);
        }
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

            // We are going in 3D MAP
            this.m_oMapService.clearMap();
            this.m_oGlobeService.clearGlobe();
            this.m_oGlobeService.initGlobe('cesiumContainer');
            this.m_oMapService.initMap('wasdiMap2');

            // Due to the problems of Leaflet initialization, let's do the subsequent steps a little bit later
            setTimeout(function () {
                oController.m_oMapService.getMap().invalidateSize();

                // Load Layers
                for (var iIndexLayers = 0; iIndexLayers < oController.m_aoVisibleBands.length; iIndexLayers++) {
                    // Check if it is a valid layer
                    if (!utilsIsObjectNullOrUndefined(oController.m_aoVisibleBands[iIndexLayers].layerId)) {
                        oController.addLayerMap3D(oController.m_aoVisibleBands[iIndexLayers].layerId);
                    }

                    var sNodeId = oController.m_aoVisibleBands[iIndexLayers].productName + "_" + oController.m_aoVisibleBands[iIndexLayers].bandName;
                    oController.setTreeNodeAsSelected(sNodeId);
                }

                // Load External Layers
                for (var iExternals = 0; iExternals < oController.m_aoExternalLayers.length; iExternals++) {
                    if (!utilsIsObjectNullOrUndefined(oController.m_aoExternalLayers[iExternals].Name)){
                        oController.addLayerMap3DByServer(oController.m_aoExternalLayers[iExternals].Name,oController.m_aoExternalLayers[iExternals].sServerLink);
                    }
                }

                // Add all bounding boxes to 2D Map
                oController.m_oMapService.addAllWorkspaceRectanglesOnMap(oController.m_aoProducts);

                // Zoom on the active band
                if (utilsIsObjectNullOrUndefined(oController.m_oActiveBand)==false) {
                    oController.m_oGlobeService.zoomBandImageOnGeoserverBoundingBox(oController.m_oActiveBand.geoserverBoundingBox);
                    oController.m_oMapService.zoomBandImageOnGeoserverBoundingBox(oController.m_oActiveBand.geoserverBoundingBox);
                }
                else {
                    // Zoom on the workspace
                    oController.m_oGlobeService.flyToWorkspaceBoundingBox(oController.m_aoProducts);
                    oController.m_oMapService.flyToWorkspaceBoundingBox(oController.m_aoProducts);
                }
            }, 400);
        }
        else {
            //We are going in 2D MAP
            this.m_oMapService.clearMap();
            this.m_oGlobeService.clearGlobe();
            this.m_oMapService.initMap('wasdiMap');
            this.m_oGlobeService.initGlobe('cesiumContainer2');

            // Due to the problems of Leaflet initialization, let's do the subsequent steps a little bit later
            setTimeout(function () {
                oController.m_oMapService.getMap().invalidateSize();

                // Load Layers
                for (var iIndexLayers = 0; iIndexLayers < oController.m_aoVisibleBands.length; iIndexLayers++) {
                    // Check if it is a valid layer
                    if (!utilsIsObjectNullOrUndefined(oController.m_aoVisibleBands[iIndexLayers].layerId)) {
                        oController.addLayerMap2D(oController.m_aoVisibleBands[iIndexLayers].layerId);
                    }

                    var sNodeId = oController.m_aoVisibleBands[iIndexLayers].productName + "_" + oController.m_aoVisibleBands[iIndexLayers].bandName;
                    oController.setTreeNodeAsSelected(sNodeId);
                }

                // Load External Layers
                for (var iExternals = 0; iExternals < oController.m_aoExternalLayers.length; iExternals++) {
                    if (!utilsIsObjectNullOrUndefined(oController.m_aoExternalLayers[iExternals].Name)){
                        oController.addLayerMap2DByServer(oController.m_aoExternalLayers[iExternals].Name,oController.m_aoExternalLayers[iExternals].sServerLink);
                    }
                }

                //  Add all bounding boxes to 3D Map
                oController.m_oGlobeService.addAllWorkspaceRectanglesOnMap(oController.m_aoProducts);

                // Zoom on the active band
                if (utilsIsObjectNullOrUndefined(oController.m_oActiveBand)==false) {
                    oController.m_oGlobeService.zoomBandImageOnGeoserverBoundingBox(oController.m_oActiveBand.geoserverBoundingBox);
                    oController.m_oMapService.zoomBandImageOnGeoserverBoundingBox(oController.m_oActiveBand.geoserverBoundingBox);
                }
                else {
                    // Zoom on the workspace
                    oController.m_oMapService.flyToWorkspaceBoundingBox(oController.m_aoProducts);
                    oController.m_oGlobeService.flyToWorkspaceBoundingBox(oController.m_aoProducts)
                }

            }, 400);
        }
    };


    /**
     * Switch 2D from Editor Mode to Geographical Mode and Vice Versa
     */
    EditorController.prototype.switchEditorGeoReferencedMode = function () {

        // This should be impossible, but just to be sure:
        if (this.m_b2DMapModeOn == false) {
            console.log("EditorController.switchEditorGeoReferencedMode: impossible to switch 3d/2d mode in editor mode");
            return;
        }

        // Switch the flag
        this.m_bIsActiveGeoraphicalMode = !this.m_bIsActiveGeoraphicalMode;

        //If we are going in Geographical Mode
        if (this.m_bIsActiveGeoraphicalMode == true) {

            //Check if there is a visible layer and if it is already published
            for (var iIndexLayer = 0; iIndexLayer < this.m_aoVisibleBands.length; iIndexLayer++) {

                //check if the layer has the layer Id
                if (!utilsIsObjectNullOrUndefined(this.m_aoVisibleBands[iIndexLayer].layerId))
                {
                    // And if it is valid
                    if (!utilsIsStrNullOrEmpty(this.m_aoVisibleBands[iIndexLayer].layerId)) {

                        // show the layer
                        this.addLayerMap2D(this.m_aoVisibleBands[iIndexLayer].layerId);

                        // Check for geoserver bounding box
                        if (!utilsIsStrNullOrEmpty(this.m_aoVisibleBands[iIndexLayer].geoserverBoundingBox))
                        {
                            this.m_oGlobeService.zoomBandImageOnGeoserverBoundingBox(this.m_aoVisibleBands[iIndexLayer].geoserverBoundingBox);
                            this.m_oMapService.zoomBandImageOnGeoserverBoundingBox(this.m_aoVisibleBands[iIndexLayer].geoserverBoundingBox);
                        }
                        else {
                            // Try with the generic product bounding box
                            this.m_oGlobeService.zoomBandImageOnBBOX(this.m_aoVisibleBands[iIndexLayer].bbox);
                            this.m_oMapService.zoomBandImageOnBBOX(this.m_aoVisibleBands[iIndexLayer].bbox);
                        }
                    }
                }
                else {

                    var oController = this;
                    // should always be 0 ...
                    var iProductIndex = iIndexLayer;

                    // Band Not Yet Published !!
                    var oPublishBandCallback = function (value) {
                        if (value) {
                            var oBand = oController.m_aoVisibleBands[iProductIndex];

                            // Remove it from Visible Layer List
                            oController.removeBandFromVisibleList(oBand);
                            // Publish the band
                            oController.openBandImage(oBand);
                            return true;
                        }
                        else {

                            return false;
                        }
                    };

                    //ask user if he want to publish the band
                    utilsVexDialogConfirm("GOING IN GEOGRAPHICAL-MODE WITH A BAND STILL NOT PUBLISHED:<br>DO YOU WANT TO PUBLISH IT?", oPublishBandCallback);
                }
            }

            // Show the external Layers
            for (var iExternals = 0; iExternals<this.m_aoExternalLayers.length; iExternals++) {
                var oLayer = this.m_aoExternalLayers[iExternals];

                // Add to the map External Layer
                this.addLayerMap2DByServer(this.m_aoExternalLayers[iExternals].Name, oLayer.sServerLink);
                this.addLayerMap3DByServer(this.m_aoExternalLayers[iExternals].Name, oLayer.sServerLink);
            }

            // Set the base maps
            this.m_oMapService.setBasicMap();

            if (this.m_aoVisibleBands.length == 0) {
                this.m_oMapService.flyToWorkspaceBoundingBox(this.m_aoProducts);
            }
        }
        else {
            // We are going in Editor Mode

            var iNumberOfLayers = this.m_aoVisibleBands.length;

            // With more than one layer visible the user can cancel the action. So it will be cleared in the callback
            if (iNumberOfLayers<=1) {
                // Clear the 2D Map
                this.m_oMapService.removeBasicMap();
                this.m_oMapService.removeLayersFromMap();
            }


            if (iNumberOfLayers == 0)
            {
                // If there are no layers go to the workspace bounding box
                this.m_oGlobeService.flyToWorkspaceBoundingBox(this.m_aoProducts);
            }
            else if (iNumberOfLayers == 1)
            {
                //if there is only one layer open it
                this.openBandImage(this.m_aoVisibleBands[0]);
            }
            else
            {
                //if there are 2 or more layers remove all but the Active One
                var oController = this;

                var oRemoveOtherLayersCallback = function (value) {
                    if (value) {
                        // Clear the 2D Map
                        oController.m_oMapService.removeBasicMap();
                        oController.m_oMapService.removeLayersFromMap();


                        // Close all the layers
                        for (var iIndexLayer = 0; iIndexLayer < iNumberOfLayers; iIndexLayer++) {
                            if (!utilsIsObjectNullOrUndefined(oController.m_aoVisibleBands[iIndexLayer].layerId)) {
                                var sNodeId = oController.m_aoVisibleBands[iIndexLayer].productName + "_" + oController.m_aoVisibleBands[iIndexLayer].name;
                                oController.setTreeNodeAsDeselected(sNodeId);
                            }
                        }

                        // Clear the list
                        oController.m_aoVisibleBands = [];

                        // Reopen only the active one
                        oController.openBandImage(oController.m_oActiveBand);

                        return true;
                    }
                    else {
                        //revert status
                        oController.m_bIsActiveGeoraphicalMode = !oController.m_bIsActiveGeoraphicalMode;
                        return false;
                    }
                };

                utilsVexDialogConfirm("GOING IN IMAGE-MODE WITH MORE THAN ONE LAYER OPENED:<br>ALL BUT THE LAST ONE WILL BE CLOSED<br>ARE YOU SURE?", oRemoveOtherLayersCallback);
            }
        }
    };


    EditorController.prototype.applyEditorPreviewImageUpdate = function () {

        if (utilsIsObjectNullOrUndefined(this.m_oActiveBand)) return;

        var sFileName = this.m_aoProducts[this.m_oActiveBand.productIndex].fileName;

        // Get Dimension of the Canvas
        var elementMapContainer = angular.element(document.querySelector('#mapcontainer'));
        var heightMapContainer = elementMapContainer[0].offsetHeight;
        var widthMapContainer = elementMapContainer[0].offsetWidth;

        var oGetBandImageBody = this.createBodyForProcessingBandImage(sFileName, this.m_oActiveBand.name,null,
            this.m_oImagePreviewDirectivePayload.viewportX, this.m_oImagePreviewDirectivePayload.viewportY,this.m_oImagePreviewDirectivePayload.viewportWidth,
            this.m_oImagePreviewDirectivePayload.viewportHeight,widthMapContainer,heightMapContainer);

        // Remove the image from visibile layers: it will be added later by processingGetBandImage
        this.removeBandFromVisibleList(this.m_oActiveBand);
        // Call the API and display the image
        this.processingGetBandImage(oGetBandImageBody, this.m_oActiveWorkspace.workspaceId);
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
            if (utilsIsStrNullOrEmpty(oMessage.messageCode) === false  ) sOperation = oMessage.messageCode;

            var oDialog = utilsVexDialogAlertTop('GURU MEDITATION<br>THERE WAS AN ERROR IN THE ' + sOperation + ' PROCESS');
            utilsVexCloseDialogAfterFewSeconds(4000, oDialog);
            this.m_oProcessesLaunchedService.loadProcessesFromServer(this.m_oActiveWorkspace);

            if (oMessage.messageCode =="PUBLISHBAND") {
                if (utilsIsObjectNullOrUndefined(oMessage.payload)==false) {
                    if (utilsIsObjectNullOrUndefined(oMessage.payload.productName) == false && utilsIsObjectNullOrUndefined(oMessage.payload.bandName) == false) {
                        var sNodeName = oMessage.payload.productName + "_" + oMessage.payload.bandName;
                        this.setTreeNodeAsDeselected(sNodeName);
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
    };

    /**
     * Callback for messages that adds a new product to the Workspace
     * @param oMessage
     */
    EditorController.prototype.receivedNewProductMessage = function (oMessage) {

        // Alert the user
        var oDialog = utilsVexDialogAlertBottomRightCorner('PRODUCT ADDED TO THE WS<br>READY');
        utilsVexCloseDialogAfterFewSeconds(4000, oDialog);

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
            utilsVexDialogAlertTop('GURU MEDITATION<br>THERE WAS AN ERROR PUBLISHING THE PRODUCT');
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
        oBand.published=true;
        oBand.bbox = oPublishedBand.boundingBox;
        oBand.geoserverBoundingBox = oPublishedBand.geoserverBoundingBox;

        // Set the tree node as selected and published
        this.setTreeNodeAsSelected(sNodeID);
        this.setTreeNodeAsPublished(sNodeID);

        if (this.m_bIsActiveGeoraphicalMode == false) {
            console.log("EditorController.receivedPublishBandMessage: we are not in geographical mode. Just update the band and return..");
            return false;
        }
        else {
            //add layer in list

            // check if the background is in Editor Mode or in Georeferenced Mode
            if (this.m_b2DMapModeOn == false) {
                //if we are in 3D put the layer on the globe
                this.addLayerMap3D(oBand.layerId);
            }
            else {
                //if we are in 2D put it on the map
                this.addLayerMap2D(oBand.layerId);
            }

            this.m_aoVisibleBands.push(oBand);

            //if there isn't Bounding Box is impossible to zoom
            if (!utilsIsStrNullOrEmpty(oBand.geoserverBoundingBox))
            {
                this.m_oGlobeService.zoomBandImageOnGeoserverBoundingBox(oBand.geoserverBoundingBox);
                this.m_oMapService.zoomBandImageOnGeoserverBoundingBox(oBand.geoserverBoundingBox);
            }
            else {
                this.m_oMapService.zoomBandImageOnBBOX(oBand.bbox);
                this.m_oGlobeService.zoomBandImageOnBBOX(oBand.bbox);
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
            }
            else {
                if (utilsIsStrNullOrEmpty(aoBands[i].layerId)) oBandItem.published = false;
                else oBandItem.published = true;
            }

            oBandItem.layerId = aoBands[i].layerId;
            oBandItem.bVisibleNow = false;
            oBandItem.bbox = oProduct.bbox;
            oBandItem.geoserverBoundingBox = aoBands[i].geoserverBoundingBox;

            asBands.push(oBandItem);
        }

        return asBands;
    };


    /**
     * Get the list of products for a Workspace
     */
    EditorController.prototype.getProductListByWorkspace = function () {
        var oController = this;
        oController.m_aoProducts = [];

        this.m_oProductService.getProductListByWorkspace(oController.m_oActiveWorkspace.workspaceId).success(function (data, status) {

            if (utilsIsObjectNullOrUndefined(data) == false) {
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

                oController.m_oGlobeService.addAllWorkspaceRectanglesOnMap(oController.m_aoProducts);
                oController.m_oGlobeService.flyToWorkspaceBoundingBox(oController.m_aoProducts);
            }
        }).error(function (data, status) {
            utilsVexDialogAlertTop('GURU MEDITATION<br>ERROR READING PRODUCT LIST');
        });
    };


    /**
     * Open a Workspace and relod it whe the page is reloaded
     * @param sWorkspaceId
     */
    EditorController.prototype.openWorkspace = function (sWorkspaceId) {

        var oController = this;

        this.m_oWorkspaceService.getWorkspaceEditorViewModel(sWorkspaceId).success(function (data, status) {
            if (data != null) {
                if (data != undefined) {
                    oController.m_oConstantsService.setActiveWorkspace(data);
                    oController.m_oActiveWorkspace = oController.m_oConstantsService.getActiveWorkspace();

                    oController.getProductListByWorkspace();
                    oController.m_oProcessesLaunchedService.loadProcessesFromServer(oController.m_oActiveWorkspace.workspaceId);

                    if (oController.m_oRabbitStompService.isSubscrbed() == false) {
                        oController.m_oRabbitStompService.subscribe(oController.m_oActiveWorkspace.workspaceId);
                    }

                }
            }
        }).error(function (data, status) {
            utilsVexDialogAlertTop('GURU MEDITATION<br>ERROR IMPOSSIBLE GET WORKSPACE IN EDITORCONTROLLER')
        });
    };


    /**
     * Finds a product from the file name
     * @param sFileNameInput
     * @returns {*}
     */
    EditorController.prototype.findProductByFileName = function(sFileNameInput)
    {
        if ( (utilsIsObjectNullOrUndefined(sFileNameInput) == true) && (utilsIsStrNullOrEmpty(sFileNameInput) == true))  return null;
        if (utilsIsObjectNullOrUndefined(this.m_aoProducts) == true)  return null;

        var iNumberOfProducts = this.m_aoProducts.length;

        if(this.m_aoProducts.length == 0) return null;

        for(var iIndexProduct = 0; iIndexProduct < iNumberOfProducts; iIndexProduct++)
        {
            if( this.m_aoProducts[iIndexProduct].fileName == sFileNameInput )
            {
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
    EditorController.prototype.openBandImage = function (oBand ) {

        var oController = this;
        var sFileName = this.m_aoProducts[oBand.productIndex].fileName;
        var bAlreadyPublished = oBand.published;

        var oLastActiveBand = this.m_oActiveBand;

        this.m_oActiveBand = oBand;

        // CHECK THE ACTUAL MODE
        if (this.m_bIsActiveGeoraphicalMode || this.m_b2DMapModeOn == false) {

            // Geographical Mode On: geoserver publish band
            this.m_oFileBufferService.publishBand(sFileName, this.m_oActiveWorkspace.workspaceId, oBand.name).success(function (data, status) {

                if (!bAlreadyPublished) {
                    var oDialog = utilsVexDialogAlertBottomRightCorner('PUBLISHING BAND ' + oBand.name);
                    utilsVexCloseDialogAfterFewSeconds(4000, oDialog);
                }

                if (!utilsIsObjectNullOrUndefined(data) && data.messageResult != "KO" && utilsIsObjectNullOrUndefined(data.messageResult)) {
                    /*if the band was published*/

                    if (data.messageCode === "PUBLISHBAND")
                    {
                        // Already published: we already have the View Model
                        oController.receivedPublishBandMessage(data);
                    }
                    else
                    {
                        // It is publishing: we will receive Rabbit Message
                        if (data.messageCode !== "WAITFORRABBIT") oController.setTreeNodeAsDeselected(oBand.productName+"_"+oBand.name);
                    }

                }
                else {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN PUBLISHING BAND " + oBand.name);
                    oController.setTreeNodeAsDeselected(oBand.productName+"_"+oBand.name);
                }
            }).error(function (data, status) {
                console.log('publish band error');
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN PUBLISH BAND");
                oController.setTreeNodeAsDeselected(oBand.productName+"_"+oBand.name);
            });
        }
        else {

            // Get Dimension of the Canvas
            var elementMapContainer = angular.element(document.querySelector('#mapcontainer'));
            var heightMapContainer = elementMapContainer[0].offsetHeight;
            var widthMapContainer = elementMapContainer[0].offsetWidth;

            // Get Preview Dimension
            var elementImagePreview = angular.element(document.querySelector('#imagepreviewcanvas'));
            var heightImagePreview = elementImagePreview[0].offsetHeight;
            var widthImagePreview = elementImagePreview[0].offsetWidth;

            // Initialize the info for the Image Preview Directive
            this.m_oImagePreviewDirectivePayload.originalBandHeight = oBand.height;
            this.m_oImagePreviewDirectivePayload.originalBandWidth = oBand.width;
            this.m_oImagePreviewDirectivePayload.viewportX = 0;
            this.m_oImagePreviewDirectivePayload.viewportY = 0;
            this.m_oImagePreviewDirectivePayload.viewportHeight = oBand.height;
            this.m_oImagePreviewDirectivePayload.viewportWidth = oBand.width;

            // Create body to get big image
            var oBodyMapContainer = this.createBodyForProcessingBandImage(sFileName,oBand.name, null, 0,0,oBand.width,oBand.height,widthMapContainer, heightMapContainer);

            // Call the API and display the image
            oController.processingGetBandImage(oBodyMapContainer, oController.m_oActiveWorkspace.workspaceId);

            // The preview is available?
            if ( (widthImagePreview > 0) && (heightImagePreview > 0) )
            {
                // Yes call API also for preview
                var oBodyImagePreview = this.createBodyForProcessingBandImage(sFileName,oBand.name, null, 0,0,oBand.width,oBand.height,widthImagePreview, heightImagePreview);
                // Show it
                oController.processingGetBandPreview(oBodyImagePreview,oController.m_oActiveWorkspace.workspaceId);
            }

            // There was a band visualized before?
            if (utilsIsObjectNullOrUndefined(oLastActiveBand) == false) {
                // Deselect the node
                var sOldNodeId = oLastActiveBand.productName+"_"+oLastActiveBand.name;
                // Deselect it
                oController.setTreeNodeAsDeselected(sOldNodeId);
                // Remove it from Visible Layer List
                oController.removeBandFromVisibleList(oLastActiveBand);
            }
        }
    };

    /**
     * Generates and shows the Band Image Preview (Editor Mode)
     * @param oBody
     * @param workspaceId
     * @returns {boolean}
     */
    EditorController.prototype.processingGetBandPreview = function(oBody, workspaceId)
    {
        if(utilsIsObjectNullOrUndefined(oBody) === true) return false;
        if(utilsIsStrNullOrEmpty(workspaceId) === true) return false;

        var oController = this;
        this.m_bIsLoadedPreviewBandImage = false;

        this.m_oFilterService.getProductBand(oBody,workspaceId).success(function (data, status) {
            if (data != null)
            {
                if (data != undefined)
                {
                    var blob = new Blob([data], {type: "octet/stream"});
                    var objectUrl = URL.createObjectURL(blob);
                    oController.m_sPreviewUrlSelectedBand = objectUrl;
                    oController.m_bIsLoadedPreviewBandImage = true;
                }
            }
        }).error(function (data, status) {
            // Clear the preview
            oController.m_sPreviewUrlSelectedBand = "empty";
            // Clear the Editor Image
            oController.m_sViewUrlSelectedBand = "//:0";
            oController.m_bIsLoadedPreviewBandImage = true;
            utilsVexDialogAlertTop('GURU MEDITATION<br>ERROR PROCESSING BAND PREVIEW IMAGE ');
        });

        return true;
    };

    /**
     *
     * Generates and shows the Band Image (Editor Mode)
     * @param oBody
     * @param workspaceId
     * @returns {boolean}
     */
    EditorController.prototype.processingGetBandImage = function(oBody, workspaceId)
    {
        if(utilsIsStrNullOrEmpty(workspaceId) === true) return false;

        var oController = this;
        this.m_bIsLoadedViewBandImage = false;
        this.m_oFilterService.getProductBand(oBody,workspaceId).success(function (data, status) {
            if (data != null)
            {
                if (data != undefined)
                {
                    // Create the link to the stream
                    var blob = new Blob([data], {type: "octet/stream"});
                    var objectUrl = URL.createObjectURL(blob);
                    oController.m_sViewUrlSelectedBand = objectUrl;

                    // Stop the waiter
                    oController.m_bIsLoadedViewBandImage = true;

                    // Set the node as selected
                    var sNodeID = oController.m_oActiveBand.productName + "_" + oController.m_oActiveBand.name;
                    oController.setTreeNodeAsSelected(sNodeID);

                    // And set the node in the visible list
                    oController.m_aoVisibleBands.push(oController.m_oActiveBand);

                    // Zoom on the bounding box in the 3d globe
                    oController.m_oGlobeService.zoomBandImageOnBBOX(oController.m_aoProducts[oController.m_oActiveBand.productIndex].bbox);
                }
            }
        }).error(function (data, status) {
            utilsVexDialogAlertTop('GURU MEDITATION<br>ERROR PROCESSING BAND IMAGE ');
            // Set the node as selected
            var sNodeID = oController.m_oActiveBand.productName + "_" + oController.m_oActiveBand.name;
            oController.setTreeNodeAsDeselected(sNodeID);

            // Clear the preview
            oController.m_sPreviewUrlSelectedBand = "empty";
            // Clear the Editor Image
            oController.m_sViewUrlSelectedBand = "//:0";

            // Stop the waiter
            oController.m_bIsLoadedViewBandImage = true;
        });

        return true;
    };

    /**
     * Generate the JSON Object to use for the POST API to get the image of a band
     * @param sFileName File Name
     * @param sBandName Band Name
     * @param sFilters List of fitlers to apply, comma separated
     * @param iRectangleX X coordinate of the rectangle to render
     * @param iRectangleY Y coordinate of the rectangle to render
     * @param iRectangleWidth Width of the rectangle to render
     * @param iRectangleHeight Height of the rectangle to render
     * @param iOutputWidth Width of the output image
     * @param iOutputHeight Height of the output image
     * @returns {{productFileName: *, bandName: *, filterVM: *, vp_x: *, vp_y: *, vp_w: *, vp_h: *, img_w: *, img_h: *}}
     */
    EditorController.prototype.createBodyForProcessingBandImage = function(sFileName, sBandName, sFilters, iRectangleX, iRectangleY, iRectangleWidth, iRectangleHeight, iOutputWidth, iOutputHeight){

        var oBandImageBody = {
            "productFileName": sFileName,
            "bandName": sBandName,
            "filterVM": sFilters,
            "vp_x": iRectangleX,
            "vp_y": iRectangleY,
            "vp_w": iRectangleWidth,
            "vp_h": iRectangleHeight,
            "img_w": iOutputWidth,
            "img_h": iOutputHeight
        };

        return oBandImageBody;
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

            // In georeferenced mode or not?
            if (this.m_bIsActiveGeoraphicalMode == true) {

                // Georeferenced: remove the band from the map
                var oMap2D = this.m_oMapService.getMap();

                //remove layer in 2D map
                oMap2D.eachLayer(function (layer) {
                    if (utilsIsStrNullOrEmpty(sLayerId) == false && layer.options.layers == sLayerId) {
                        oMap2D.removeLayer(layer);
                    }
                });

                this.m_oGlobeService.flyToWorkspaceBoundingBox(this.m_aoProducts);
                this.m_oMapService.flyToWorkspaceBoundingBox(this.m_aoProducts);
            }
            else {

                // We Are in editor mode

                // Clear the preview
                this.m_sPreviewUrlSelectedBand = "empty";
                // Clear the Editor Image
                this.m_sViewUrlSelectedBand = "//:0";

                // Fly Home
                this.m_oGlobeService.flyToWorkspaceBoundingBox(this.m_aoProducts);
            }
        }
        else {

            // We are in 3d Mode

            var aoGlobeLayers = this.m_oGlobeService.getGlobeLayers();
            for (var iIndexLayer=0; iIndexLayer<aoGlobeLayers.length; iIndexLayer++) {
                var oLayer = aoGlobeLayers.get(iIndexLayer);

                if (utilsIsStrNullOrEmpty(sLayerId) == false && utilsIsObjectNullOrUndefined(oLayer) == false && oLayer.imageryProvider.layers == sLayerId) {
                    oLayer = aoGlobeLayers.remove(oLayer);
                    break;
                }
            }

            this.m_oGlobeService.flyToWorkspaceBoundingBox(this.m_aoProducts);
            this.m_oMapService.flyToWorkspaceBoundingBox(this.m_aoProducts);
        }

        // Deselect the node
        var sOldNodeId = oBand.productName+"_"+oBand.name;
        // Deselect it
        this.setTreeNodeAsDeselected(sOldNodeId);
        // Remove it from Visible Layer List
        this.removeBandFromVisibleList(oBand);

    };


    /**
     * Removes a band from the list of visible Bands
     * @param oBand
     */
    EditorController.prototype.removeBandFromVisibleList = function(oBand) {
        var iVisibleBandCount = 0;

        if (utilsIsObjectNullOrUndefined(this.m_aoVisibleBands) == false) iVisibleBandCount = this.m_aoVisibleBands.length;

        for (var iIndex = 0; iIndex < iVisibleBandCount; ) {

            if (this.m_aoVisibleBands[iIndex].productName == oBand.productName && this.m_aoVisibleBands[iIndex].name == oBand.name) {
                this.m_aoVisibleBands.splice(iIndex,1);
                iVisibleBandCount--;
            }
            else iIndex++;
        }
    };


    /**
     * Add layer on the 2D Map
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

        //it set the zindex of layer in map
        wmsLayer.setZIndex(1000);
        wmsLayer.addTo(oMap);

    };

    /**
     * Add layer for Cesium Globe
     * @param sLayerId
     */
    EditorController.prototype.addLayerMap3D = function (sLayerId) {
        var oGlobeLayers = this.m_oGlobeService.getGlobeLayers();
        var sUrlGeoserver = this.m_oConstantsService.getWmsUrlGeoserver();
        // wms options
        var oWMSOptions = {
            transparent: true,
            format: 'image/png'
        };//crossOriginKeyword: null

        // WMS get GEOSERVER
        var oProvider = new Cesium.WebMapServiceImageryProvider({
            url: sUrlGeoserver,
            layers: 'wasdi:' + sLayerId,
            parameters: oWMSOptions
        });

        oGlobeLayers.addImageryProvider(oProvider);
    };

    /**
     * Add layer on 3d map from a specific server
     * @param sLayerId
     */
    EditorController.prototype.addLayerMap3DByServer = function (sLayerId,sServer) {
        if (sLayerId == null) return false;
        if(sServer == null) return false;

        var oGlobeLayers=this.m_oGlobeService.getGlobeLayers();

        var oWMSOptions= { // wms options
            transparent: true,
            format: 'image/png'
        };

        // WMS get GEOSERVER
        var oProvider = new Cesium.WebMapServiceImageryProvider({
            url : sServer,
            layers: sLayerId,
            parameters : oWMSOptions

        });

        oGlobeLayers.addImageryProvider(oProvider);
    };

    /**
     * Add a layer from a specific server on the 2D map
     * @param sLayerId
     */
    EditorController.prototype.addLayerMap2DByServer = function (sLayerId,sServer) {
        // Chech input data
        if(sServer == null) return false;
        if (sLayerId == null) return false;

        var oMap = this.m_oMapService.getMap();

        var wmsLayer = L.tileLayer.betterWms(sServer, {
            layers: sLayerId,
            format: 'image/png',
            transparent: true,
            noWrap:true
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
            $('.leaflet-popup-pane').css({"visibility": "visible"});
            $('.leaflet-container').css('cursor','crosshair');
        }
        else {
            $('.leaflet-popup-pane').css({"visibility": "hidden"});
            $('.leaflet-container').css('cursor','');
        }
    };


    /**
     * Handler of the "Home" button of the view
     */
    EditorController.prototype.goWorkspaceHome = function () {
        if (this.m_b2DMapModeOn) {
            this.m_oMapService.flyToWorkspaceBoundingBox(this.m_aoProducts);
        }
        else {
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
     * @param oSelectedProduct
     * @returns {boolean}
     */
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

    /**
     *
     * @returns {boolean}
     */
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

    /**
     *
     * @returns {boolean}
     */
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

    /**
     *
     * @param oSelectedProduct
     * @returns {boolean}
     */
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


                // if(utilsIsObjectNullOrUndefined(oResult) == true)
                // {
                //     utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR THE APPLY ORBIT OPTIONS ARE WRONG OR EMPTY!");
                //     return false;
                // }
                // if(oResult == "close")
                //     return false;
                //
                // // oController.m_oScope.Result = oResult;
                // oController.m_oSnapOperationService.ApplyOrbit(oResult.sourceFileName, oResult.destinationFileName, oController.m_oActiveWorkspace.workspaceId,oResult.options)
                //     .success(function (data) {
                //
                //     }).error(function (error) {
                //         utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR THE OPERATION APPLY ORBIT DOSEN'T WORK");
                // });
                if(utilsIsObjectNullOrUndefined(oResult) == true)
                {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR THE APPLY ORBIT OPTIONS ARE WRONG OR EMPTY!");
                    return false;
                }
                if(oResult == "close")
                    return false;
                var iNumberOfProduct = oResult.length;
                for(var iIndexProduct = 0; iIndexProduct < iNumberOfProduct;iIndexProduct ++)
                {
                    // oController.m_oScope.Result = oResult;
                    oController.m_oSnapOperationService.ApplyOrbit(oResult[iIndexProduct].sourceFileName, oResult[iIndexProduct].destinationFileName,
                                                                    oController.m_oActiveWorkspace.workspaceId,oResult[iIndexProduct].options)
                        .success(function (data) {

                        }).error(function (error) {
                        utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR THE OPERATION APPLY ORBIT DOSEN'T WORK");
                    });
                }

                return true;
            });
        });

        return true;
    };

    /**
     *
     * @param oSelectedProduct
     * @returns {boolean}
     */
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
    };

    /**
     *
     * @param oSelectedProduct
     * @returns {boolean}
     */
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
    };

    /**
     *
     * @param oSelectedProduct
     * @returns {boolean}
     */
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
    };

    /**
     *
     * @param oProductInput
     * @returns {boolean}
     */
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

    /**
     *
     * @param oProductInput
     * @returns {boolean}
     */
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
    };

    /**
     *
     * @param oSelectedProduct
     * @returns {boolean}
     */
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
    };

    /**
     *
     * @param oSelectedBand
     * @returns {boolean}
     */
    EditorController.prototype.filterBandDialog = function (oSelectedBand)
    {
        if(utilsIsObjectNullOrUndefined(oSelectedBand) === true) return false;

        var oController = this;
        this.m_oModalService.showModal({
            templateUrl: "dialogs/filter_band_operation/FilterBandDialog.html",
            controller: "FilterBandController",
            inputs: {
                extras: {
                    workspaceId : this.m_oActiveWorkspace.workspaceId,
                    selectedBand: oSelectedBand
                },
            }
        }).then(function (modal) {
            modal.element.modal();
            modal.close.then(function (oResult) {
                if(utilsIsObjectNullOrUndefined(oResult) === true)  return false;
                if(utilsIsObjectNullOrUndefined(oResult.filter) === true) return false;

                var elementMapContainer = angular.element(document.querySelector('#mapcontainer'));
                var heightMapContainer = elementMapContainer[0].offsetHeight;
                var widthMapContainer = elementMapContainer[0].offsetWidth;

                var elementImagePreview = angular.element(document.querySelector('#imagepreviewcanvas'));
                var heightImagePreview = elementImagePreview[0].offsetHeight;
                var widthImagePreview = elementImagePreview[0].offsetWidth;

                var sFileName = oController.m_aoProducts[oResult.band.productIndex].fileName;
                var sFilter = JSON.stringify(oResult.filter);

                var oBodyMapContainer = oController.createBodyForProcessingBandImage(sFileName,oResult.band.name,sFilter,0,0,oResult.band.width, oResult.band.height,widthMapContainer, heightMapContainer);

                var oBodyImagePreview = oController.createBodyForProcessingBandImage(sFileName,oResult.band.name,sFilter,0,0,oResult.band.width, oResult.band.height,widthImagePreview, heightImagePreview);

                oController.processingGetBandImage(oBodyMapContainer, oController.m_oActiveWorkspace.workspaceId);
                if ( (widthImagePreview > 0) && (heightImagePreview > 0) )
                {
                    // Show Preview Only if it is visible
                    oController.processingGetBandPreview(oBodyImagePreview,oController.m_oActiveWorkspace.workspaceId);
                }

                // oController.openBandImage(oResult.band,oResult.filter);
                return true;
            });
        });

        return true;
    }


    /*********************************************************** OPERATION MENU ***********************************************************/


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


    /*********************************************************** CSS CHANGE ***********************************************************/


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

    EditorController.prototype.goSearch = function()
    {
        this.m_oState.go("root.import", { });
    };


    /********************************************************** COLOUR MANIPULATION *********************************************************************/

    /**
     *
     * @param sNameDiv
     * @returns {boolean}
     */
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
            type: 'histogram'
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

    /********************************************************** TREE FUNCTIONS *********************************************************************/

    EditorController.prototype.isHideTree = function()
    {
        return ( (this.m_oTree === null) || (this.m_oTree.core.data.length === 0) );
    };


    /**
     * Generate tree for metadata
     * @param oElement
     * @param oNewTree
     * @param iIndexNewTreeAttribute
     */
    EditorController.prototype.generateMetadatadTree = function(oElement,oNewTree,iIndexNewTreeAttribute)
    {
        if (typeof oElement != "undefined" && oElement != null)
        {
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
        }

    };

    /**
     * Set a node as deselected
     * @param sNode
     */
    EditorController.prototype.setTreeNodeAsDeselected = function(sNode) {
        $("#jstree").jstree().enable_node(sNode);
        $('#jstree').jstree(true).set_icon(sNode,'assets/icons/uncheck_20x20.png');

        // It is no more visible now
        var oNode = $('#jstree').jstree(true).get_node(sNode);
        if (utilsIsObjectNullOrUndefined(oNode.original) == false) oNode.original.band.bVisibleNow = false;
    }


    /**
     * Set a node as selected
     * @param sNode
     */
    EditorController.prototype.setTreeNodeAsSelected = function(sNode) {
        $("#jstree").jstree().enable_node(sNode);
        $('#jstree').jstree(true).set_icon(sNode,'assets/icons/check_20x20.png');

        var oNode = $('#jstree').jstree(true).get_node(sNode); //oLayer.layerId
        if (utilsIsObjectNullOrUndefined(oNode.original) == false) oNode.original.band.bVisibleNow = true;

    };

    /**
     * Set a node as published
     * @param sNode
     */
    EditorController.prototype.setTreeNodeAsPublished = function(sNode) {
        var sLabelText = $("#jstree").jstree().get_text(sNode);
        sLabelText = sLabelText.replace("band-not-published-label", "band-published-label");
        utilsJstreeUpdateLabelNode(sNode,sLabelText);
    };

    /**
     * Opens all the visible bands in the tree
     */
    EditorController.prototype.openPublishedBandsInTree = function () {

        var treeInst = $('#jstree').jstree(true);
        var m = treeInst._model.data;
        for (var i in m) {
            if (!utilsIsObjectNullOrUndefined(m[i].original) && !utilsIsObjectNullOrUndefined(m[i].original.band) && m[i].original.band.bVisibleNow == true) {
                $("#jstree").jstree("_open_to", m[i].id);
            }
        }
    };

    /**
     * Selects a node of the tree from the file name
     * @param sFileName
     * @returns {boolean}
     */
    EditorController.prototype.selectNodeByFileNameInTree = function (sFileName) {
        if(utilsIsObjectNullOrUndefined(sFileName) == true)  return false;

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

    /**
     * Rename a node in the tree
     * @param sFileName
     * @param sNewNameInput
     * @returns {boolean}
     */
    EditorController.prototype.renameNodeInTree = function (sFileName,sNewNameInput) {
        if((utilsIsObjectNullOrUndefined(sNewNameInput) == true) || (utilsIsStrNullOrEmpty(sNewNameInput) == true)) return false;

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
        var oTree =
            {
                'core': {'data': [], "check_callback": true},
                "state" : { "key" : "state_tree" },
                "plugins": ["contextmenu","state"],  // all plugin i use
                "contextmenu": { // my right click menu
                    "items": function ($node) {

                        //only the band has property $node.original.band
                        var oReturnValue = null;
                        if (utilsIsObjectNullOrUndefined($node.original.band) == false && $node.original.band.bVisibleNow == true) {
                            //******************************** BAND *************************************
                            var oBand = $node.original.band;

                            oReturnValue =
                                {
                                    "Zoom2D": {
                                        "label": "Zoom Band 2D Map",
                                        "action": function (obj) {
                                            if (utilsIsObjectNullOrUndefined(oBand) == false) {
                                                oController.m_oMapService.zoomBandImageOnGeoserverBoundingBox(oBand.geoserverBoundingBox);
                                            }
                                        }
                                    },
                                    "Zoom3D" : {
                                        "label" : "Zoom Band 3D Map",
                                        "action" : function (obj) {
                                            if(utilsIsObjectNullOrUndefined(oBand) == false) {
                                                oController.m_oGlobeService.zoomBandImageOnBBOX(oBand.bbox);
                                            }

                                        }
                                    },
                                    "Filter Band":{
                                        "label": "Filter Band",
                                        "action" : function(pbj){
                                            if(utilsIsObjectNullOrUndefined(oBand) == false)
                                                oController.filterBandDialog(oBand);
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

                                                        if(utilsIsObjectNullOrUndefined(oFindedProduct) == false)  oController.openApplyOrbitDialog(oFindedProduct);

                                                    }
                                                },
                                                "Multilooking": {
                                                    "label": "Multilooking",
                                                    "action": function (obj) {
                                                        var sSourceFileName = $node.original.fileName;
                                                        var oFindedProduct = oController.findProductByFileName(sSourceFileName);

                                                        if(utilsIsObjectNullOrUndefined(oFindedProduct) == false)  oController.openMultilookingDialog(oFindedProduct);
                                                    }
                                                },

                                                "Range Doppler Terrain Correction": {
                                                    "label": "Range Doppler Terrain Correction",
                                                    "action": function (obj) {
                                                        var sSourceFileName = $node.original.fileName;
                                                        var oFindedProduct = oController.findProductByFileName(sSourceFileName);

                                                        if(utilsIsObjectNullOrUndefined(oFindedProduct) == false) oController.rangeDopplerTerrainCorrectionDialog(oFindedProduct);
                                                    }
                                                },

                                                "Calibrate": {
                                                    "label": "Calibrate",
                                                    "action": function (obj) {
                                                        var sSourceFileName = $node.original.fileName;
                                                        var oFound = oController.findProductByFileName(sSourceFileName);

                                                        if(utilsIsObjectNullOrUndefined(oFound) == false) oController.openRadiometricCalibrationDialog(oFound);

                                                    }
                                                }
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
                                                        var oFound = oController.findProductByFileName(sSourceFileName);

                                                        if(utilsIsObjectNullOrUndefined(oFound) == false) oController.openNDVIDialog(oFound);
                                                    }
                                                }
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
                                                    if (value.files == 'on')  bDeleteFile = true;
                                                    if (value.geoserver == 'on') bDeleteLayer = true;
                                                    this.temp = $node;
                                                    var that = this;
                                                    oController.m_oProductService.deleteProductFromWorkspace($node.original.fileName, oController.m_oActiveWorkspace.workspaceId, bDeleteFile, bDeleteLayer).success(function (data) {
                                                            var iLengthLayer = oController.m_aoVisibleBands.length;
                                                            var iLengthChildren_d = that.temp.children_d.length;

                                                            for(var iIndexChildren = 0; iIndexChildren < iLengthChildren_d; iIndexChildren++)
                                                            {
                                                                for(var iIndexLayer = 0; iIndexLayer < iLengthLayer; iIndexLayer++)
                                                                {
                                                                    if( that.temp.children_d[iIndexChildren] ===  oController.m_aoVisibleBands[iIndexLayer].layerId)
                                                                    {
                                                                        oController.removeBandImage(oController.m_aoVisibleBands[iIndexChildren]);
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
                                    }
                                };
                        }
                        return oReturnValue;
                    }
                }
            };


        // For each product generate sub-node
        for (var iIndexProduct = 0; iIndexProduct < this.m_aoProducts.length; iIndexProduct++) {

            //product node
            var oNode = new Object();

            oNode.text = this.m_aoProducts[iIndexProduct].productFriendlyName;
            oNode.fileName = this.m_aoProducts[iIndexProduct].fileName;
            oNode.id = this.m_aoProducts[iIndexProduct].fileName;

            //oNode.product = this.m_aoProducts[iIndexProduct];
            this.m_aoProducts[iIndexProduct].selfIndex = iIndexProduct;
            oNode.productIndex = iIndexProduct;

            var oController=this;

            oNode.children = [
                {
                    "text":"Metadata",
                    "icon": "assets/icons/folder_20x20.png",
                    "children": [],
                    "clicked":false,//semaphore
                    "url" : oController.m_oProductService.getApiMetadata(oNode.fileName)
                },
                {
                    "text": "Bands",
                    "icon": "assets/icons/folder_20x20.png",
                    "children": []
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
                }
                else {
                    oNode.text = "<span class='band-not-published-label'>" +  oaBandsItems[iIndexBandsItems].name + "</span>";
                }

                oNode.band = oaBandsItems[iIndexBandsItems];//BAND
                oNode.icon = "assets/icons/uncheck_20x20.png";

                oNode.id = this.m_aoProducts[iIndexProduct].name + "_" + oaBandsItems[iIndexBandsItems].name;
                //oNode.bVisibleNow = false;
                oTree.core.data[iIndexProduct].children[1].children.push(oNode);
            }

        }

        return oTree;
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
