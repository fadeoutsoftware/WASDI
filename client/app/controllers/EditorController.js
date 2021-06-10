/**
 * Created by p.campanella on 24/10/2016.
 */
var EditorController = (function () {
    function EditorController($scope, $location, $interval, oConstantsService, oAuthService, oMapService, oFileBufferService,
                              oProductService, $state, oWorkspaceService, oNodeService, oGlobeService, oProcessesLaunchedService, oRabbitStompService,
                              oSnapOperationService, oModalService, oFilterService, oTranslate, oCatalogService,
                              $window) {
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
        this.m_oNodeService = oNodeService;
        this.m_oRabbitStompService = oRabbitStompService;
        this.m_oFilterService = oFilterService;
        this.m_oModalService = oModalService;


        this.m_oTranslate = oTranslate;
        this.m_oCatalogService = oCatalogService;
        // Flag to know if in the big map is 2d (true) or 3d (false)
        this.m_b2DMapModeOn = true;
        // Flag to know if the big map is the Geographical Mode (true) or in the Editor Mode (false)
        this.m_bIsActiveGeoraphicalMode = false;
        // Flag to know if the first zoom on band has been done
        this.m_bFirstZoomOnBandDone = false;

        //filter query text in tree
        this.m_sTextQueryFilterInTree = "";
        this.m_bIsFilteredTree = false;

        this.m_bIsLoadingColourManipulation = false;
        this.m_bIsLoadingTree = true;
        this.m_sToolTipBtnSwitchGeographic = "EDITOR_TOOLTIP_TO_EDITOR";
        this.m_sClassBtnSwitchGeographic = "btn-switch-not-geographic";
        //Flag to know if the Preview Band Image is loaded or not (2D - Editor Mode)
        this.m_bIsLoadedPreviewBandImage = true;
        //Flag to know if the Band Image is loaded or not (2D - Editor Mode)
        this.m_bIsLoadedViewBandImage = true;
        //Flag to know if the actual band Image is coming from a Zoom or if it is a new image
        this.m_bIsEditorZoomingOnExistingImage = false;
        //Url of the Preview Band Image (2D - Editor Mode)
        this.m_sPreviewUrlSelectedBand = "";
        // this.m_sPreviewUrlSelectedBand = "assets/img/test_image.jpg";
        // Url of the Band Image (2D - Editor Mode)
        this.m_sViewUrlSelectedBand = "";
        this.m_oMapContainerSize = utilsProjectGetMapContainerSize();

        this.m_oMapPreviewContainerSize = utilsProjectGetPreviewContainerSize();
        // Object used to exchange information with the image preview directive
        this.m_oImagePreviewDirectivePayload = {
            originalBandWidth: 0,
            originalBandHeight: 0,
            viewportX: 0,
            viewportY: 0,
            viewportWidth: 0,
            viewportHeight: 0
        };

        this.m_iPanScalingValue = 1.5;

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

        //we save the masks selected
        this.m_oMasksSaved = null;
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
        // Initialize the map
        oMapService.initMapEditor('wasdiMap');
        // add the GeoSearch plugin bar
        oMapService.initGeoSearchPluginForOpenStreetMap({"position": 'bottomRight'});
        oMapService.removeLayersFromMap();
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
            this.m_oProcessesLaunchedService.loadProcessesFromServer(this.m_oActiveWorkspace.workspaceId);
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

        //set default navbar menu
        this.generateDefaultNavBarMenu();

        // Go in geographic mode
        this.switchEditorGeoReferencedMode();

        // Launch image editor modal to debug it
        //this.openImageEditorDialog();
        var oThat = this;

        angular.element($window).bind('resize', function () {

            $scope.width = $window.innerWidth;
            oThat.m_oMapContainerSize = utilsProjectGetMapContainerSize();

            // manuall $digest required as resize event
            // is outside of angular
            $scope.$digest();
        });

        this.m_oWorkspaceViewModel = null;
    }

    /********************************************************* TRANSLATE SERVICE ********************************************************/
    EditorController.prototype.generateDefaultNavBarMenu = function () {
        this.m_aoNavBarMenu = [
            {
                name: "",//WAPPS
                caption_i18n: "EDITOR_OPERATION_TITLE_WAPPS",
                subMenu: [],
                onClick: this.openWappsDialog,
                icon: "fa fa-lg fa-rocket"
            },
            // --- Workflow ---
            {
                name: "",
                icon: "fa fa-cogs",
                caption_i18n: "EDITOR_OPERATION_TITLE_WORKFLOW",
                subMenu: [],
                onClick: this.openWorkflowManagerDialog
            },
            // --- Import ---
            {
                name: "",
                icon: "fa fa-cloud-upload",
                caption_i18n: "EDITOR_OPERATION_TITLE_IMPORT",
                subMenu: [],
                onClick: this.openImportsDialog
            },
            // --- Processor ---
            {
                name: "",// New Processor
                caption_i18n: "EDITOR_OPERATION_TITLE_NEW_PROCESSOR",
                subMenu: [],
                onClick: this.openProcessorDialog,
                icon: "fa fa-lg fa-plus-square"
            },
            {
                name: "",//Share
                caption_i18n: "EDITOR_OPERATION_TITLE_SHARE",
                subMenu: [],
                onClick: this.openShareDialog,
                icon: "fa fa-share-alt fa-lg"
            }

        ]

        this.translateToolbarMenuList(this.m_aoNavBarMenu);
    };

    EditorController.prototype.isToolbarBtnDropdown = function (btn) {
        return btn.subMenu.length != 0;
    }

    EditorController.prototype.translateToolbarMenu = function (menuItem) {
        this.m_oTranslate(menuItem.caption_i18n).then(function (text) {
            menuItem.name = text;
        })
    }

    EditorController.prototype.translateToolbarMenuList = function (menuList) {
        for (var i = 0; i < menuList.length; i++) {
            var menuItem = menuList[i];
            this.translateToolbarMenu(menuItem);
            if (this.isToolbarBtnDropdown(menuItem) == true) {
                this.translateToolbarMenuList(menuItem.subMenu);
            }
        }
    }

    /*********************************************************** VIEW METHODS**********************************************************/

    EditorController.prototype.onEditBtnClick = function () {
        this.openImageEditorDialog();
    }

    EditorController.prototype.openImageEditorDialog = function () {

        this.m_oModalService.showModal({
            templateUrl: "dialogs/image_editor/image-editor.component.html",
            controller: ImageEditorController.REG_NAME,
            inputs: {
                extras: ""
            }

        }).then(function (modal) {
            modal.element.modal({
                backdrop: 'static',
                keyboard: false
            });
            modal.close.then(function (result) {
            })
        });

        return true;
    };


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
        if (this.m_iActiveMapPanelTab === iTab) return;
        let oBand, sFileName;
        this.m_iActiveMapPanelTab = iTab;
        // if was clicked the tab color manipulation && the active band isn't null && there isn't any saved colour manipulation, get colour manipulation
        if ((iTab === 1) && (utilsIsObjectNullOrUndefined(this.m_oActiveBand) === false) && (utilsIsObjectNullOrUndefined(this.m_oActiveBand.colorManipulation) === true) &&
            (this.m_bIsLoadingColourManipulation === false)) {
            oBand = this.m_oActiveBand;
            sFileName = this.m_aoProducts[oBand.productIndex].fileName
            this.getProductColorManipulation(sFileName, oBand.name, true, this.m_oActiveWorkspace.workspaceId);

        }
        //if was clicked the tab preview && the active band isn't null && there isn't any saved preview image, preview image
        if ((iTab === 2) && (this.m_oActiveBand !== null) && (utilsIsStrNullOrEmpty(this.m_sPreviewUrlSelectedBand) === true || this.m_sPreviewUrlSelectedBand === "empty") &&
            (this.m_bIsLoadedPreviewBandImage === true)) {
            // Initialize Image Preview

            oBand = this.m_oActiveBand;
            sFileName = this.m_aoProducts[oBand.productIndex].fileName;

            // var elementImagePreview = angular.element(document.querySelector('#imagepreviewcanvas'));
            var elementImagePreview = angular.element(document.querySelector('#panelBodyMapPreviewEditor'));
            var heightImagePreview = elementImagePreview[0].offsetHeight;
            var widthImagePreview = elementImagePreview[0].offsetWidth;

            // TODO: here the tab is not shown yet. So H and W are still 0.
            // This code should run after the tab is shown
            if (heightImagePreview == 0) heightImagePreview = 280;//default value canvas
            if (widthImagePreview == 0) widthImagePreview = 560;//default value canvas

            var oBodyImagePreview = this.createBodyForProcessingBandImage(sFileName, oBand.name, null, 0, 0, oBand.width, oBand.height,
                widthImagePreview, heightImagePreview, oBand.colorManipulation);

            this.processingGetBandPreview(oBodyImagePreview, this.m_oActiveWorkspace.workspaceId);
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
    /**
     * Switch 2D from Editor Mode to Geographical Mode and Vice Versa
     */
    EditorController.prototype.switchEditorGeoReferencedMode = function () {

        // Switch the flag
        this.m_bIsActiveGeoraphicalMode = !this.m_bIsActiveGeoraphicalMode;

        if (this.m_bIsActiveGeoraphicalMode == true) {
            //If we are going in Geographical Mode

            this.switchToGeographicMode();

        } else {
            // We are going in Editor Mode
            this.switchToEditorMode();

        }
    };

    EditorController.prototype.switchToGeographicMode = function () {
        this.clearImageEditor();

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
                            // this.addLayerMap3D(oController.m_aoVisibleBands[iIndexLayer].layerId);
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

                //ask user if he want to publish the band
                utilsVexDialogConfirm("GOING IN GEOGRAPHICAL-MODE WITH A BAND STILL NOT PUBLISHED:<br>DO YOU WANT TO PUBLISH IT?", oPublishBandCallback);
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

    EditorController.prototype.switchToEditorMode = function () {

        var iNumberOfLayers = this.m_aoVisibleBands.length;

        // With more than one layer visible the user can cancel the action. So it will be cleared in the callback
        if (iNumberOfLayers <= 1) {
            // Clear the Map
            if (this.m_b2DMapModeOn) this.m_oMapService.removeLayersFromMap();
            else this.m_oGlobeService.removeAllEntities();
        }

        if (iNumberOfLayers == 0) {
            // If there are no layers go to the workspace bounding box
            if (this.m_b2DMapModeOn) this.m_oGlobeService.flyToWorkspaceBoundingBox(this.m_aoProducts);
            else this.m_oMapService.flyToWorkspaceBoundingBox(this.m_aoProducts);
        } else if (iNumberOfLayers == 1) {
            //if there is only one layer open it
            this.openBandImage(this.m_aoVisibleBands[0]);
        } else {
            //if there are 2 or more layers remove all but the Active One
            var oController = this;

            var oRemoveOtherLayersCallback = function (value) {
                if (value) {
                    // Clear the Map
                    if (oController.m_b2DMapModeOn) {
                        oController.m_oMapService.removeLayersFromMap();
                    } else {

                        oController.removeAllRedSquareBoundingBox();
                        // oController.m_oGlobeService.removeAllEntities();
                    }

                    iNumberOfLayers = oController.m_aoVisibleBands.length;
                    // Close all the layers
                    for (var iIndexLayer = 0; iIndexLayer < iNumberOfLayers; iIndexLayer++) {
                        if (!utilsIsObjectNullOrUndefined(oController.m_aoVisibleBands[iIndexLayer].layerId)) {
                            var sNodeId = oController.m_aoVisibleBands[iIndexLayer].productName + "_" + oController.m_aoVisibleBands[iIndexLayer].name;
                            oController.setTreeNodeAsDeselected(sNodeId);

                            if (oController.m_b2DMapModeOn === false) {
                                //if there are bands not georeferenced we need to remove layers
                                //(because oController.m_oGlobeService.removeAllEntities(); remove only the red square)
                                oController.removeBandLayersIn3dMaps("wasdi:" + oController.m_aoVisibleBands[iIndexLayer].layerId);
                            }

                        }
                    }

                    // Clear the list

                    if (utilsIsObjectNullOrUndefined(oController.m_oActiveBand) === true) {
                        if (utilsIsObjectNullOrUndefined(oController.m_aoVisibleBands) === false && oController.m_aoVisibleBands.length > 0) {
                            //insert new active layer
                            oController.m_oActiveBand = oController.m_aoVisibleBands[oController.m_aoVisibleBands.length - 1];
                            oController.m_aoVisibleBands.splice(0, oController.m_aoVisibleBands.length - 1);
                        }

                    } else {
                        oController.m_aoVisibleBands = [];
                        oController.m_aoVisibleBands.push(oController.m_oActiveBand);
                    }
                    // Reopen only the active one
                    oController.openBandImage(oController.m_oActiveBand);
                    return true;
                } else {
                    //revert status
                    oController.m_bIsActiveGeoraphicalMode = !oController.m_bIsActiveGeoraphicalMode;
                    return false;
                }
            };

            utilsVexDialogConfirm("IN EDITOR MODE ONLY LAST IMAGE WILL BE SHOWN", oRemoveOtherLayersCallback);
        }
    }

    EditorController.prototype.applyEditorPreviewImageUpdate = function () {

        if (utilsIsObjectNullOrUndefined(this.m_oActiveBand)) return;

        var sFileName = this.m_aoProducts[this.m_oActiveBand.productIndex].fileName;

        // Get Dimension of the Canvas
        var oMapContainerSize = this.getMapContainerSize(this.m_iPanScalingValue);
        var heightMapContainer = oMapContainerSize.height;
        var widthMapContainer = oMapContainerSize.width;

        // heightMapContainer = heightMapContainer;
        // widthMapContainer = widthMapContainer;
        var oGetBandImageBody = this.createBodyForProcessingBandImage(sFileName, this.m_oActiveBand.name, this.m_oActiveBand.actualFilter,
            this.m_oImagePreviewDirectivePayload.viewportX, this.m_oImagePreviewDirectivePayload.viewportY, this.m_oImagePreviewDirectivePayload.viewportWidth,
            this.m_oImagePreviewDirectivePayload.viewportHeight, widthMapContainer, heightMapContainer, this.m_oActiveBand.colorManipulation);

        this.m_bIsEditorZoomingOnExistingImage = true;

        // Add user selected masks, if available
        oGetBandImageBody.productMasks = this.m_oActiveBand.productMasks;
        oGetBandImageBody.rangeMasks = this.m_oActiveBand.rangeMasks;
        oGetBandImageBody.mathMasks = this.m_oActiveBand.mathMasks;

        // Remove the image from visibile layers: it will be added later by processingGetBandImage
        this.removeBandFromVisibleList(this.m_oActiveBand);
        // Call the API and display the image
        this.processingGetBandImage(oGetBandImageBody, this.m_oActiveWorkspace.workspaceId);
    };


    EditorController.prototype.applyMapViewImageOriginalValues = function () {

        if (utilsIsObjectNullOrUndefined(this.m_oActiveBand)) return;

        var sFileName = this.m_aoProducts[this.m_oActiveBand.productIndex].fileName;

        // Get Dimension of the Canvas
        var oMapContainerSize = this.getMapContainerSize(this.m_iPanScalingValue);
        var heightMapContainer = oMapContainerSize.height;
        var widthMapContainer = oMapContainerSize.width;

        var oGetBandImageBody = this.createBodyForProcessingBandImage(sFileName, this.m_oActiveBand.name, this.m_oActiveBand.actualFilter,
            0, 0, this.m_oImagePreviewDirectivePayload.originalBandWidth, this.m_oImagePreviewDirectivePayload.originalBandHeight,
            widthMapContainer, heightMapContainer, this.m_oActiveBand.colorManipulation);

        // Add user selected masks, if available
        oGetBandImageBody.productMasks = this.m_oActiveBand.productMasks;
        oGetBandImageBody.rangeMasks = this.m_oActiveBand.rangeMasks;
        oGetBandImageBody.mathMasks = this.m_oActiveBand.mathMasks;

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
            if (utilsIsStrNullOrEmpty(oMessage.messageCode) === false) sOperation = oMessage.messageCode;

            var sErrorDescription = "";

            if (utilsIsStrNullOrEmpty(oMessage.payload) === false) sErrorDescription = oMessage.payload;
            if (utilsIsStrNullOrEmpty(sErrorDescription) === false) sErrorDescription = "<br>" + sErrorDescription;

            var oDialog = utilsVexDialogAlertTop('GURU MEDITATION<br>THERE WAS AN ERROR IN THE ' + sOperation + ' PROCESS' + sErrorDescription);
            utilsVexCloseDialogAfter(10000, oDialog);

            // P.Campanella: 20191125: the loadProcessesFromServer is called by the rabbit stomp service
            //this.m_oProcessesLaunchedService.loadProcessesFromServer(this.m_oActiveWorkspace.workspaceId);

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
                oController.getProductListByWorkspace();
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
        oBand.published = true;
        oBand.bbox = oPublishedBand.boundingBox;
        oBand.geoserverBoundingBox = oPublishedBand.geoserverBoundingBox;
        oBand.geoserverUrl = oPublishedBand.geoserverUrl;

        // Set the tree node as selected and published
        this.setTreeNodeAsSelected(sNodeID);
        this.setTreeNodeAsPublished(sNodeID);

        if (this.m_bIsActiveGeoraphicalMode == false) {
            console.log("EditorController.receivedPublishBandMessage: we are not in geographical mode. Just update the band and return..");
            return false;
        } else {
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

            this.m_aoVisibleBands.push(oBand);

            if (this.m_aoVisibleBands.length == 1) {

                if (!this.m_bFirstZoomOnBandDone) {
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
        },(function (data, status) {
            utilsVexDialogAlertTop('GURU MEDITATION<br>ERROR READING PRODUCT LIST');
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
                    oController.m_oConstantsService.setActiveWorkspace(data.data);
                    oController.m_oActiveWorkspace = oController.m_oConstantsService.getActiveWorkspace();

                    oController.getProductListByWorkspace();
                    oController.m_oProcessesLaunchedService.loadProcessesFromServer(oController.m_oActiveWorkspace.workspaceId);

                    if (oController.m_oRabbitStompService.isSubscrbed() == false) {
                        //oController.m_oRabbitStompService.subscribe(oController.m_oActiveWorkspace.workspaceId);
                        oController._subscribeToRabbit();
                    }
                }
            }
        },(function (data, status) {
            utilsVexDialogAlertTop('GURU MEDITATION<br>ERROR IMPOSSIBLE GET WORKSPACE IN EDITOR')
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

        var oLastActiveBand = this.m_oActiveBand;

        this.m_oActiveBand = oBand;
        //set navigation tab
        this.m_iActiveMapPanelTab = 0;
        //set default value for preview image
        this.m_sPreviewUrlSelectedBand = "empty";

        // CHECK THE ACTUAL MODE
        if (this.m_bIsActiveGeoraphicalMode) {

            // Geographical Mode On: geoserver publish band
            this.m_oFileBufferService.publishBand(sFileName, this.m_oActiveWorkspace.workspaceId, oBand.name).then(function (data, status) {

                if (!bAlreadyPublished) {
                    var oDialog = utilsVexDialogAlertBottomRightCorner('PUBLISHING BAND ' + oBand.name);
                    utilsVexCloseDialogAfter(4000, oDialog);
                }

                if (!utilsIsObjectNullOrUndefined(data.data) && data.data.messageResult != "KO" && utilsIsObjectNullOrUndefined(data.data.messageResult)) {
                    /*if the band was published*/

                    if (data.data.messageCode === "PUBLISHBAND") {
                        // Already published: we already have the View Model
                        oController.receivedPublishBandMessage(data.data);
                    } else {
                        oController.m_oProcessesLaunchedService.loadProcessesFromServer(oController.m_oActiveWorkspace.workspaceId);
                        // It is publishing: we will receive Rabbit Message
                        if (data.data.messageCode !== "WAITFORRABBIT") oController.setTreeNodeAsDeselected(oBand.productName + "_" + oBand.name);
                    }

                } else {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN PUBLISHING BAND " + oBand.name);
                    oController.setTreeNodeAsDeselected(oBand.productName + "_" + oBand.name);
                }
            },(function (data, status) {
                console.log('publish band error');
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN PUBLISH BAND");
                oController.setTreeNodeAsDeselected(oBand.productName + "_" + oBand.name);
            }));
        } else {

            // Get Dimension of the Canvas
            var oMapContainerSize = this.getMapContainerSize(this.m_iPanScalingValue);
            var heightMapContainer = oMapContainerSize.height;
            var widthMapContainer = oMapContainerSize.width;

            // Create body to get big image
            var oBodyMapContainer = this.createBodyForProcessingBandImage(sFileName, oBand.name, oBand.actualFilter, 0, 0,
                oBand.width, oBand.height, widthMapContainer, heightMapContainer, oBand.colorManipulation);
            // this.m_oImageMapDirectivePayload = oBodyMapContainer;
            // Disable the not till the end of the API
            var sNode = this.m_aoProducts[oBand.productIndex].productName + "_" + oBand.bandName;
            this.setTreeNodeDisabled(sNode);

            // Add user selected masks, if available
            oBodyMapContainer.productMasks = oBand.productMasks;
            oBodyMapContainer.rangeMasks = oBand.rangeMasks;
            oBodyMapContainer.mathMasks = oBand.mathMasks;

            // Call the API and display the image
            oController.processingGetBandImage(oBodyMapContainer, oController.m_oActiveWorkspace.workspaceId);

            // There was a band visualized before?
            if (utilsIsObjectNullOrUndefined(oLastActiveBand) == false) {
                // Deselect the node
                var sOldNodeId = oLastActiveBand.productName + "_" + oLastActiveBand.name;
                // Deselect it
                oController.setTreeNodeAsDeselected(sOldNodeId);
                // Remove it from Visible Layer List
                oController.removeBandFromVisibleList(oLastActiveBand);
            }
        }

        // Anyway, show the preview

        // Get Preview Dimension
        // var elementImagePreview = angular.element(document.querySelector('#imagepreviewcanvas'));
        angular.element(document.querySelector('#panelBodyMapPreviewEditor'));

        // Initialize the info for the Image Preview Directive
        this.m_oImagePreviewDirectivePayload.originalBandHeight = oBand.height;
        this.m_oImagePreviewDirectivePayload.originalBandWidth = oBand.width;
        this.m_oImagePreviewDirectivePayload.viewportX = 0;
        this.m_oImagePreviewDirectivePayload.viewportY = 0;
        this.m_oImagePreviewDirectivePayload.viewportHeight = oBand.height;
        this.m_oImagePreviewDirectivePayload.viewportWidth = oBand.width;

    };

    /**
     * Clears the image editor both versions: the big image and the image preview
     */
    EditorController.prototype.clearImageEditor = function () {
        // Clear the preview
        this.m_sPreviewUrlSelectedBand = "empty";
        // Clear the Editor Image
        this.m_sViewUrlSelectedBand = "//:0";
    };

    /**
     * Generates and shows the Band Image Preview (Editor Mode)
     * @param oBody
     * @param workspaceId
     * @returns {boolean}
     */
    EditorController.prototype.processingGetBandPreview = function (oBody, workspaceId) {
        if (utilsIsObjectNullOrUndefined(oBody) === true) return false;
        if (utilsIsStrNullOrEmpty(workspaceId) === true) return false;

        var oController = this;
        this.m_bIsLoadedPreviewBandImage = false;

        this.m_oFilterService.getProductBand(oBody, workspaceId).then(function (data, status) {
            if (data.data != null) {
                if (data.data != undefined) {
                    var blob = new Blob([data.data], {type: "octet/stream"});
                    var objectUrl = URL.createObjectURL(blob);
                    oController.m_sPreviewUrlSelectedBand = objectUrl;
                    oController.m_bIsLoadedPreviewBandImage = true;
                }
            }
        },(function (data, status) {
            // Clear the editor
            oController.clearImageEditor();
            oController.m_bIsLoadedPreviewBandImage = true;
            utilsVexDialogAlertTop('GURU MEDITATION<br>ERROR PROCESSING BAND PREVIEW IMAGE ');
        }));

        return true;
    };

    /**
     *
     * Generates and shows the Band Image (Editor Mode)
     * @param oBody
     * @param workspaceId
     * @returns {boolean}
     */
    EditorController.prototype.processingGetBandImage = function (oBody, workspaceId) {
        if (utilsIsStrNullOrEmpty(workspaceId) === true) return false;

        var oController = this;
        this.m_bIsLoadedViewBandImage = false;
        var sUrl = null;

        // P.Campanella 20/11/2019: TODO Test the redirect of the getProductBand to the node that hosts the workspace
        if (utilsIsStrNullOrEmpty(this.m_oConstantsService.getActiveWorkspace().apiUrl) == false) {
            sUrl = this.m_oConstantsService.getActiveWorkspace().apiUrl;
        }
        this.m_oFilterService.getProductBand(oBody, workspaceId, sUrl).then(function (data, status) {

            // Anyway this is not more a zoom.
            oController.m_bIsEditorZoomingOnExistingImage = false;
            // Stop the waiter
            oController.m_bIsLoadedViewBandImage = true;

            if (data.data != null) {
                if (data.data != undefined) {
                    // Create the link to the stream
                    var blob = new Blob([data.data], {type: "octet/stream"});
                    var objectUrl = URL.createObjectURL(blob);
                    oController.m_sViewUrlSelectedBand = objectUrl;

                    // Set the node as selected
                    var sNodeID = oController.m_oActiveBand.productName + "_" + oController.m_oActiveBand.name;
                    oController.setTreeNodeAsSelected(sNodeID);

                    // And set the node in the visible list
                    oController.m_aoVisibleBands.push(oController.m_oActiveBand);

                    // Zoom on the bounding box in the 3d globe
                    oController.m_oGlobeService.zoomBandImageOnBBOX(oController.m_aoProducts[oController.m_oActiveBand.productIndex].bbox);

                    // Get product colour manipulation
                    // oController.getProductColorManipulation(oBody.productFileName,oBody.bandName,true,workspaceId);
                }
            }
        },(function (data, status) {
            utilsVexDialogAlertTop('GURU MEDITATION<br>ERROR PROCESSING BAND IMAGE ');
            // Set the node as selected
            var sNodeID = oController.m_oActiveBand.productName + "_" + oController.m_oActiveBand.name;
            oController.setTreeNodeAsDeselected(sNodeID);

            // Clear the editor
            oController.clearImageEditor();

            // Anyway this is not more a zoom.
            oController.m_bIsEditorZoomingOnExistingImage = false;
            // Stop the waiter
            oController.m_bIsLoadedViewBandImage = true;
        }));

        return true;
    };

    /**
     * Generate the JSON Object to use for the POST API to get the image of a band
     * @param sFileName File Name
     * @param sBandName Band Name
     * @param sFilters List of filters to apply, comma separated
     * @param iRectangleX X coordinate of the rectangle to render
     * @param iRectangleY Y coordinate of the rectangle to render
     * @param iRectangleWidth Width of the rectangle to render
     * @param iRectangleHeight Height of the rectangle to render
     * @param iOutputWidth Width of the output image
     * @param iOutputHeight Height of the output image
     * @returns {{productFileName: *, bandName: *, filterVM: *, vp_x: *, vp_y: *, vp_w: *, vp_h: *, img_w: *, img_h: *}}
     */
    EditorController.prototype.createBodyForProcessingBandImage = function (sFileName, sBandName, sFilters, iRectangleX, iRectangleY,
                                                                            iRectangleWidth, iRectangleHeight, iOutputWidth, iOutputHeight, oColorManipulation) {

        var oBandImageBody = {
            "productFileName": sFileName,
            "bandName": sBandName,
            "filterVM": sFilters,
            "vp_x": iRectangleX,
            "vp_y": iRectangleY,
            "vp_w": iRectangleWidth,
            "vp_h": iRectangleHeight,
            "img_w": iOutputWidth,
            "img_h": iOutputHeight,
            "colorManiputalion": oColorManipulation
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

        if (utilsIsObjectNullOrUndefined(this.m_oActiveBand) === false) {
            //delete color manipulation in jstree (the colour manipulation is passed with reference)
            delete this.m_oActiveBand.colorManipulation;
        }

        // Clear the active Band
        this.m_oActiveBand = null;
        // Get the layer Id
        var sLayerId = "wasdi:" + oBand.layerId;
        //set navigation tab
        this.m_iActiveMapPanelTab = 0;
        //remove preview band image
        this.m_sPreviewUrlSelectedBand = "empty";
        // Check the actual Mode
        if (this.m_b2DMapModeOn) {
            // We are in 2d mode

            // In georeferenced mode or not?
            if (this.m_bIsActiveGeoraphicalMode == true) {

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
            } else {

                // We Are in editor mode

                // Clear the preview
                this.m_sPreviewUrlSelectedBand = "empty";
                // Clear the Editor Image
                this.m_sViewUrlSelectedBand = "//:0";

                // Fly Home
                // this.m_oGlobeService.flyToWorkspaceBoundingBox(this.m_aoProducts);
            }
        } 
        else 
        {
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
            if (utilsIsStrNullOrEmpty(sLayerId) === false && sProductLayerId === sLayerId)//&& utilsIsObjectNullOrUndefined(oLayer) == false
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
        // var oGlobe = this.m_oGlobeService.getGlobe();

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
                        // oLayer = oGlobe.remove(oLayer);
                        //break;
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
            noWrap: true,
        });

        //it set the zindex of layer in map
        wmsLayer.setZIndex(1000);
        // .leaflet-tile { border: solid black 5px; }
        wmsLayer.addTo(oMap);

        return true;
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

        return true;
    };

    /**
     * Add layer on 3d map from a specific server
     * @param sLayerId
     */
    EditorController.prototype.addLayerMap3DByServer = function (sLayerId, sServer) {
        if (sLayerId == null) return false;
        if (sServer == null) return this.addLayerMap3D(sLayerId);

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
        if (sServer == null) return this.addLayerMap2D(sLayerId);
        if (sLayerId == null) return false;

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
            $('.leaflet-popup-pane').css({"visibility": "visible"});
            $('.leaflet-container').css('cursor', 'crosshair');
        } else {
            $('.leaflet-popup-pane').css({"visibility": "hidden"});
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
                oController.m_oProcessesLaunchedService.loadProcessesFromServer(oController.m_oActiveWorkspace.workspaceId);
            });
        });

        return true;
    };

    /**
     * openWPSDialog
     * @returns {boolean}
     */
    EditorController.prototype.openMosaicDialog = function (oWindow) {
        var oController;
        if (utilsIsObjectNullOrUndefined(oWindow) === true) {
            oController = this;
        } else {
            oController = oWindow;
        }

        oController.m_oModalService.showModal({
            templateUrl: "dialogs/mosaic/MosaicView.html",
            controller: "MosaicController",
            inputs: {
                extras: {
                    // products:oController.m_aoProducts
                    products: oController.m_aoProducts,

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

                oController.m_oProcessesLaunchedService.loadProcessesFromServer(oController.m_oActiveWorkspace.workspaceId);
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
            },(function (data, status) {
        }));


        return true;
    };


    /**
     * Handle click on "show mask" from image editor
     */
    EditorController.prototype.openMaskManagerFromImageEditor = function () {
        if (utilsIsObjectNullOrUndefined(this.m_oActiveBand)) return;
        var oFoundProduct = this.m_aoProducts[this.m_oActiveBand.productIndex];
        this.openMaskManager(this.m_oActiveBand, oFoundProduct);
    };

    /**
     *
     */
    EditorController.prototype.openEditPanelFromImageEditor = function () {
        if (utilsIsObjectNullOrUndefined(this.m_oActiveBand)) return;
        var oFoundProduct = this.m_aoProducts[this.m_oActiveBand.productIndex];
        this.openEditPanelDialog(this.m_oActiveBand, oFoundProduct);
    };

    /**
     * Handle click on "show filters" from image editor
     */
    EditorController.prototype.openFiltersFromImageEditor = function () {
        if (utilsIsObjectNullOrUndefined(this.m_oActiveBand)) return;
        this.filterBandDialog(this.m_oActiveBand);
    };
    /**
     *
     * @returns {boolean}
     */
    EditorController.prototype.openMaskManager = function (oBand, oProduct) {
        var oController = this;
        var oFinalBand = oBand;

        this.m_oModalService.showModal({
            templateUrl: "dialogs/mask_manager/MaskManagerView.html",
            controller: "MaskManagerController",
            inputs: {
                extras: {
                    band: oBand,
                    product: oProduct,
                    workspaceId: oController.m_oActiveWorkspace.workspaceId,
                    masksSaved: oController.m_oMasksSaved
                }
            }
        }).then(function (modal) {

            modal.element.modal();
            modal.close.then(function (oResult) {

                if (utilsIsObjectNullOrUndefined(oResult) === true) return false;
                if (utilsIsObjectNullOrUndefined(oResult.body) === true) return false;

                //sav filter
                oController.m_oMasksSaved = oResult.listOfMasks;

                // Set a filter, if it has been selected by the user
                oResult.body.filterVM = oFinalBand.actualFilter;
                // Save the masks, as user selected
                oFinalBand.productMasks = oResult.body.productMasks;
                oFinalBand.rangeMasks = oResult.body.rangeMasks;
                oFinalBand.mathMasks = oResult.body.mathMasks;
                oController.m_bIsLoadedViewBandImage = false;
                oController.m_oFilterService.getProductBand(oResult.body, oController.m_oActiveWorkspace.workspaceId).then(function (data, status) {
                    if (data.data != null) {
                        if (data.data != undefined) {
                            // Create the link to the stream
                            var blob = new Blob([data.data], {type: "octet/stream"});
                            var objectUrl = URL.createObjectURL(blob);
                            oController.m_sViewUrlSelectedBand = objectUrl;
                        }
                    }
                    oController.m_bIsLoadedViewBandImage = true;
                },(function (data, status) {
                    utilsVexDialogAlertTop('GURU MEDITATION<br>ERROR PROCESSING BAND IMAGE ');
                    oController.m_bIsLoadedViewBandImage = true;
                }));


            });
        });

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
            modal.element.modal();
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
                if (utilsIsObjectNullOrUndefined(result) === true)
                    return false;
                //TODO ADD FILENAME = RESULT
                // oController.m_oScope.Result = result;
            });
        });

        return true;
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

    /**
     *
     * @param oSelectedBand
     * @returns {boolean}
     */
    EditorController.prototype.filterBandDialog = function (oSelectedBand) {
        if (utilsIsObjectNullOrUndefined(oSelectedBand) === true) return false;

        var oController = this;
        var oFinalSelectedBand = oSelectedBand;

        this.m_oModalService.showModal({
            templateUrl: "dialogs/filter_band_operation/FilterBandDialog.html",
            controller: "FilterBandController",
            inputs: {
                extras: {
                    workspaceId: this.m_oActiveWorkspace.workspaceId,
                    selectedBand: oSelectedBand
                }
            }
        }).then(function (modal) {
            modal.element.modal();
            modal.close.then(function (oResult) {
                if (utilsIsObjectNullOrUndefined(oResult) === true) return false;
                if (utilsIsObjectNullOrUndefined(oResult.filter) === true) return false;

                // var elementMapContainer = angular.element(document.querySelector('#mapcontainer'));
                var oMapContainerSize = oController.getMapContainerSize(oController.m_iPanScalingValue);
                var heightMapContainer = oMapContainerSize.height;
                var widthMapContainer = oMapContainerSize.width;

                angular.element(document.querySelector('#imagepreviewcanvas'));


                var sFileName = oController.m_aoProducts[oResult.band.productIndex].fileName;

                var oBodyMapContainer = oController.createBodyForProcessingBandImage(sFileName, oResult.band.name, oResult.filter, 0, 0,
                    oResult.band.width, oResult.band.height, widthMapContainer, heightMapContainer, oResult.band.colorManipulation);

                // Filters does not apply in the preview

                // Save the filter for further operations
                oFinalSelectedBand.actualFilter = oResult.filter;

                // Add user selected masks, if available
                oBodyMapContainer.productMasks = oFinalSelectedBand.productMasks;
                oBodyMapContainer.rangeMasks = oFinalSelectedBand.rangeMasks;
                oBodyMapContainer.mathMasks = oFinalSelectedBand.mathMasks;

                oController.processingGetBandImage(oBodyMapContainer, oController.m_oActiveWorkspace.workspaceId);

                return true;
            });
        });
        return true;
    };


    EditorController.prototype.openEditPanelDialog = function (oBand, oProduct) {

        var oController = this;
        var oFinalSelectedBand = oBand;

        this.m_oModalService.showModal({
            templateUrl: "dialogs/edit_panel/EditPanelView.html",
            controller: "EditPanelController",
            inputs: {
                extras: {
                    maskManager: {
                        band: oBand,
                        product: oProduct,
                        workspaceId: oController.m_oActiveWorkspace.workspaceId,
                        masksSaved: oController.m_oMasksSaved,
                    },
                    filterBand: {
                        workspaceId: oController.m_oActiveWorkspace.workspaceId,
                        selectedBand: oBand
                    },
                    colorManipulation: {
                        panScalingValue: oController.m_iPanScalingValue
                    }

                }
            }
        }).then(function (modal) {
            modal.element.modal();
            modal.close.then(function (oResult) {

                if (utilsIsObjectNullOrUndefined(oResult) || utilsIsString(oResult)) {
                    return false;
                }

                if (oResult.hasOwnProperty("listOfMasks") === true) {
                    oController.runMaskManager(oResult, oController, oFinalSelectedBand);
                    return true;

                }
                if (oResult.hasOwnProperty("filter") === true) {
                    oController.runFilterBand(oResult, oController, oFinalSelectedBand);
                    return true;
                }
                if (oResult.hasOwnProperty("bodyMapContainer") === true) {
                    oController.processingGetBandImage(oResult.bodyMapContainer, oResult.workspaceId);
                }
                return false;
            });
        });
        return true;
    };

    EditorController.prototype.runMaskManager = function (oResult, oController, oFinalBand) {
        if (utilsIsObjectNullOrUndefined(oResult) === true) return false;
        if (utilsIsObjectNullOrUndefined(oResult.body) === true) return false;

        //sav filter
        oController.m_oMasksSaved = oResult.listOfMasks;

        // Set a filter, if it has been selected by the user
        oResult.body.filterVM = oFinalBand.actualFilter;
        // Save the masks, as user selected
        oFinalBand.productMasks = oResult.body.productMasks;
        oFinalBand.rangeMasks = oResult.body.rangeMasks;
        oFinalBand.mathMasks = oResult.body.mathMasks;
        oController.m_bIsLoadedViewBandImage = false;
        oController.m_oFilterService.getProductBand(oResult.body, oController.m_oActiveWorkspace.workspaceId).then(function (data, status) {
            if (data != null) {
                if (data != undefined) {
                    // Create the link to the stream
                    var blob = new Blob([data], {type: "octet/stream"});
                    var objectUrl = URL.createObjectURL(blob);
                    oController.m_sViewUrlSelectedBand = objectUrl;
                }
            }
            oController.m_bIsLoadedViewBandImage = true;
        });
    };

    EditorController.prototype.runFilterBand = function (oResult, oController, oFinalSelectedBand) {
        if (utilsIsObjectNullOrUndefined(oResult) === true) return false;
        if (utilsIsObjectNullOrUndefined(oResult.filter) === true) return false;

        // var elementMapContainer = angular.element(document.querySelector('#mapcontainer'));
        var oMapContainerSize = oController.getMapContainerSize(oController.m_iPanScalingValue);
        var heightMapContainer = oMapContainerSize.height;
        var widthMapContainer = oMapContainerSize.width;

        // var elementImagePreview = angular.element(document.querySelector('#imagepreviewcanvas'));
        // var heightImagePreview = elementImagePreview[0].offsetHeight;
        // var widthImagePreview = elementImagePreview[0].offsetWidth;

        var sFileName = oController.m_aoProducts[oResult.band.productIndex].fileName;

        var oBodyMapContainer = oController.createBodyForProcessingBandImage(sFileName, oResult.band.name, oResult.filter, 0, 0,
            oResult.band.width, oResult.band.height, widthMapContainer, heightMapContainer, oResult.band.colorManipulation);

        // Filters does not apply in the preview

        // Save the filter for further operations
        oFinalSelectedBand.actualFilter = oResult.filter;

        // Add user selected masks, if available
        oBodyMapContainer.productMasks = oFinalSelectedBand.productMasks;
        oBodyMapContainer.rangeMasks = oFinalSelectedBand.rangeMasks;
        oBodyMapContainer.mathMasks = oFinalSelectedBand.mathMasks;

        oController.processingGetBandImage(oBodyMapContainer, oController.m_oActiveWorkspace.workspaceId);

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


    EditorController.prototype.hideOperationMainBar = function () {
        this.m_oAreHideBars.mainBar = true;
    };
    EditorController.prototype.hideOperationRadarBar = function () {
        this.m_oAreHideBars.radarBar = true;
    };
    EditorController.prototype.hideProcessorBar = function () {
        this.m_oAreHideBars.processorBar = true;
    };
    EditorController.prototype.hideOperationOpticalBar = function () {
        this.m_oAreHideBars.opticalBar = true;
    };
    EditorController.prototype.showOperationMainBar = function () {
        this.m_oAreHideBars.mainBar = false;
    };
    EditorController.prototype.showOperationRadarBar = function () {
        this.m_oAreHideBars.radarBar = false;
    };
    EditorController.prototype.showOperationOpticalBar = function () {
        this.m_oAreHideBars.opticalBar = false;
    };
    EditorController.prototype.showOperationProcessor = function () {
        this.m_oAreHideBars.processorBar = false;
    };

    EditorController.prototype.isHiddenOperationMainBar = function () {
        return this.m_oAreHideBars.mainBar;
    };
    EditorController.prototype.isHiddenOperationRadarBar = function () {
        return this.m_oAreHideBars.radarBar;
    };
    EditorController.prototype.isHiddenProcessorBar = function () {
        return this.m_oAreHideBars.processorBar;
    };

    EditorController.prototype.isHiddenOperationOpticalBar = function () {
        return this.m_oAreHideBars.opticalBar;
    };


    /*********************************************************** CSS CHANGE ***********************************************************/


    EditorController.prototype.changeClassBtnSwitchGeographic = function () {
        if (this.m_sClassBtnSwitchGeographic === "btn-switch-not-geographic") {
            this.m_sClassBtnSwitchGeographic = "btn-switch-geographic";
            this.m_sToolTipBtnSwitchGeographic = "EDITOR_TOOLTIP_TO_EDITOR";
        } else {
            this.m_sClassBtnSwitchGeographic = "btn-switch-not-geographic";
            this.m_sToolTipBtnSwitchGeographic = "EDITOR_TOOLTIP_TO_GEO";
        }
    };


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


    /********************************************************** COLOUR MANIPULATION *********************************************************************/

    /**
     *
     * @param sNameDiv
     * @returns {boolean}
     */
    EditorController.prototype.drawColourManipulationHistogram = function (sNameDiv, afValues) {

        if (utilsIsStrNullOrEmpty(sNameDiv) === true)
            return false;
        if (utilsIsObjectNullOrUndefined(afValues) === true)
            return false;
        //todo test
        var x = utilsGenerateArrayWithFirstNIntValue(0, afValues.length);
        if (utilsIsObjectNullOrUndefined(x) === true) {
            return false;
        }

        var trace = {
            x: x,
            y: afValues,
            // type: 'histogram'
            type: 'bar',
            marker: {
                color: '#43516A',
            }
        };
        var data = [trace];
        var layout = {
            // title: "Colour Manipolation",
            showlegend: false,
            height: 200,
            width: 500,
            xaxis: {
                showgrid: true,
                zeroline: true,
            },
            paper_bgcolor: "#000000",
            // paper_bgcolor:"#EF4343",
            margin: {
                l: 5,
                r: 5,
                b: 5,
                t: 5,
                pad: 4
            },

        };
        Plotly.newPlot(sNameDiv, data, layout, {staticPlot: true});//,layout,{staticPlot: true}

        return true;
    };

    /**
     *
     * @param oColorManipulation
     * @returns {boolean}
     */
    EditorController.prototype.adjust95percentageColourManipulation = function (oColorManipulation) {
        if (utilsIsObjectNullOrUndefined(oColorManipulation) === true) {
            return false;
        }
        //min value
        oColorManipulation.colors[0].value = oColorManipulation.histogramMin;
        //average value
        oColorManipulation.colors[1].value = (oColorManipulation.histogramMax / 2);
        //max value
        oColorManipulation.colors[2].value = oColorManipulation.histogramMax;

        this.processingProductColorManipulation();

        return true;
    };
    EditorController.prototype.equalizeHistogram = function (src, dst) {
        var srcLength = src.length;
        if (!dst) {
            dst = src;
        }

        // Compute histogram and histogram sum:
        var hist = new Float32Array(512);
        var sum = 0;
        for (var i = 0; i < srcLength; ++i) {
            ++hist[~~src[i]];
            ++sum;
        }

        // Compute integral histogram:
        var prev = hist[0];
        for (var j = 1; j < 512; ++j) {
            prev = hist[j] += prev;
        }

        // Equalize image:
        var norm = 511 / sum;
        for (var k = 0; k < srcLength; ++k) {
            dst[k] = hist[~~src[k]] * norm;
        }
        return dst;
    }

    EditorController.prototype.adjust100percentageColourManipulation = function (oColorManipulation) {
        if (utilsIsObjectNullOrUndefined(oColorManipulation) === true) {
            return false;
        }
        //min value
        oColorManipulation.colors[0].value = oColorManipulation.histogramMin;
        //average value
        oColorManipulation.colors[1].value = (oColorManipulation.histogramMax / 2);
        //max value
        oColorManipulation.colors[2].value = oColorManipulation.histogramMax;

        var test = [];
        this.equalizeHistogram(oColorManipulation.histogramBins, test);

        //processing product with the new color manipulation
    };


    EditorController.prototype.minSliderColourManipulation = function (aoSlidersColors) {
        var iMaxValue = parseInt(aoSlidersColors[2].value);
        var iMinValue = parseInt(aoSlidersColors[0].value);
        var iAverageValue = parseInt(aoSlidersColors[1].value);
        // the min value can't be bigger average
        if (iMinValue >= iAverageValue) {
            aoSlidersColors[1].value = parseInt(aoSlidersColors[0].value);
            if (iAverageValue >= iMaxValue) {
                //set max value with average
                aoSlidersColors[2].value = parseInt(aoSlidersColors[1].value);
            }
        }
    };
    EditorController.prototype.maxSliderColourManipulation = function (aoSlidersColors) {
        var iMaxValue = parseInt(aoSlidersColors[2].value);
        var iMinValue = parseInt(aoSlidersColors[0].value);
        var iAverageValue = parseInt(aoSlidersColors[1].value);

        // the max value can't be smaller average
        if (iMaxValue <= iAverageValue) {
            //set average with max value
            aoSlidersColors[1].value = parseInt(aoSlidersColors[2].value);
            if (iAverageValue <= iMinValue) {
                //set min value with average
                aoSlidersColors[0].value = parseInt(aoSlidersColors[1].value);
            }

        }
    };
    EditorController.prototype.averageSliderColourManipulation = function (aoSlidersColors) {
        var iMaxValue = parseInt(aoSlidersColors[2].value);
        var iMinValue = parseInt(aoSlidersColors[0].value);
        var iAverageValue = parseInt(aoSlidersColors[1].value);
        // the average must be bigger than min value and smaller than max value
        if (iAverageValue >= iMaxValue) {
            //set max value with average
            aoSlidersColors[2].value = parseInt(aoSlidersColors[1].value);
        }
        if (iAverageValue <= iMinValue) {
            //set min value with average
            aoSlidersColors[0].value = parseInt(aoSlidersColors[1].value);
        }

    };

    EditorController.prototype.getProductColorManipulation = function (sFile, sBand, bAccurate, sWorkspaceId) {
        if (utilsIsStrNullOrEmpty(sFile) === true || utilsIsStrNullOrEmpty(sBand) === true || utilsIsStrNullOrEmpty(sWorkspaceId) === true || utilsIsObjectNullOrUndefined(bAccurate) === true) {
            return false;
        }
        var oController = this;
        this.m_bIsLoadingColourManipulation = true;
        this.m_oSnapOperationService.getProductColorManipulation(sFile, sBand, bAccurate, sWorkspaceId).then(function (data, status) {
            if (data != null) {
                if (data != undefined) {
                    // oController.m_oColorManipulation = data;
                    if (utilsIsObjectNullOrUndefined(oController.m_oActiveBand) === false) {
                        oController.m_oActiveBand.colorManipulation = data;
                        oController.drawColourManipulationHistogram("colourManipulationDiv", data.histogramBins);

                    }
                }
            }
            oController.m_bIsLoadingColourManipulation = false;
        },(function (data, status) {
            utilsVexDialogAlertTop('GURU MEDITATION<br>PRODUCT COLOR MANIPULATION ');
            oController.m_bIsLoadingColourManipulation = false;
        }));

        return true;
    };

    /**
     * processingProductColorManipulation
     */
    EditorController.prototype.processingProductColorManipulation = function () {
        if (utilsIsObjectNullOrUndefined(this.m_oActiveBand) === true) return;

        var sWorkspaceId, oBand;
        oBand = this.m_oActiveBand;
        sWorkspaceId = this.m_oActiveWorkspace.workspaceId;

        //get map size
        var oMapContainerSize = this.getMapContainerSize(this.m_iPanScalingValue);
        var heightMapContainer = oMapContainerSize.height;
        var widthMapContainer = oMapContainerSize.width;

        var sFileName = this.m_aoProducts[oBand.productIndex].fileName;
        //get body
        var oBodyMapContainer = this.createBodyForProcessingBandImage(sFileName, oBand.name, oBand.actualFilter, 0, 0,
            oBand.width, oBand.height, widthMapContainer, heightMapContainer, oBand.colorManipulation);
        //processing image with color manipulation
        this.processingGetBandImage(oBodyMapContainer, sWorkspaceId);
    }
    EditorController.prototype.getDefaultProductColorManipulation = function () {
        if (utilsIsObjectNullOrUndefined(this.m_oActiveBand.colorManipulation) === false) {
            delete this.m_oActiveBand.colorManipulation;
            //without the property body.colorManipulation the server return default colorManipulation
        }
        //get default value of color manipolation
        this.processingProductColorManipulation()
    }

    EditorController.prototype.generateColor = function (oColors) {
        if (utilsIsObjectNullOrUndefined(oColors) === true) {
            return "";
        }

        return "rgb(" + oColors.colorBlue + "," + oColors.colorGreen + "," + oColors.colorRed + ")";
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
        var oTree =
            {
                'core': {'data': [], "check_callback": true},
                "state": {"key": "state_tree"},
                "plugins": ["contextmenu", "state", "search"], // all plugin in use
                "search": {
                    "show_only_matches": true,
                    "show_only_matches_children": true
                },
                "contextmenu": { // my right click menu
                    "items": function ($node) {

                        //only the band has property $node.original.band
                        var oReturnValue = null;
                        if (utilsIsObjectNullOrUndefined($node.original.band) == false) {
                            //******************************** BAND *************************************
                            var oBand = $node.original.band;

                            oReturnValue =
                                {
                                    "Workflow": {
                                        "label": "Workflow",
                                        "action": function (obj) {
                                            var oFoundProduct = oController.m_aoProducts[$node.original.band.productIndex];

                                            if (utilsIsObjectNullOrUndefined(oFoundProduct) == false) oController.openWorkflowManagerDialog();
                                        }

                                    },
                                    "Filter Band": {
                                        "label": "Filter Band",
                                        "action": function (pbj) {
                                            if (utilsIsObjectNullOrUndefined(oBand) == false)
                                                oController.filterBandDialog(oBand);
                                        },
                                        "_disabled": !$node.original.band.bVisibleNow
                                    },
                                    "Mask Manager": {
                                        "label": "Mask Manager",
                                        "action": function (pbj) {
                                            if (utilsIsObjectNullOrUndefined(oBand) == false)
                                                var oFoundProduct = oController.m_aoProducts[$node.original.band.productIndex];
                                            oController.openMaskManager(oBand, oFoundProduct);
                                        },
                                        "_disabled": !$node.original.band.bVisibleNow
                                    },
                                    "Zoom2D": {
                                        "label": "Zoom Band 2D Map",
                                        "action": function (obj) {
                                            if (utilsIsObjectNullOrUndefined(oBand) == false) {
                                                oController.m_oMapService.zoomBandImageOnGeoserverBoundingBox(oBand.geoserverBoundingBox);
                                            }
                                        },
                                        // "_disabled": (!$node.original.band.bVisibleNow && !oController.isEnable2DZoomInTreeInEditorMode())
                                        "_disabled": (oController.isEnable2DZoomInTreeInEditorMode() && oController.isActiveEditorMode() === true)
                                    },
                                    "Zoom3D": {
                                        "label": "Zoom Band 3D Map",
                                        "action": function (obj) {
                                            if (utilsIsObjectNullOrUndefined(oBand) == false) {
                                                oController.m_oGlobeService.zoomBandImageOnBBOX(oBand.bbox);
                                            }
                                        },
                                        // "_disabled": (!$node.original.band.bVisibleNow && !oController.isEnable3DZoomInTreeInEditorMode())
                                        "_disabled": (oController.isEnable3DZoomInTreeInEditorMode() && oController.isActiveEditorMode() === true)
                                    },
                                    "Properties": {
                                        "label": "Properties ",
                                        "icon": "info-icon-context-menu-jstree",
                                        "separator_before": true,
                                        "action": function (obj) {
                                            var oFoundProduct = oController.m_aoProducts[$node.original.band.productIndex];
                                            if (utilsIsObjectNullOrUndefined(oFoundProduct) == false) oController.openProductInfoDialog(oFoundProduct);
                                        }
                                    },
                                    "DeleteProduct": {
                                        "label": "Delete Product",
                                        "icon": "delete-icon-context-menu-jstree",

                                        "action": function (obj) {

                                            utilsVexDialogConfirm("DELETING PRODUCT.<br>ARE YOU SURE?", function (value) {
                                                if (value) {
                                                    bDeleteFile = true;
                                                    bDeleteLayer = true;
                                                    this.temp = $node.parents[1];
                                                    var that = this;

                                                    var oFoundProduct = oController.m_aoProducts[$node.original.band.productIndex];

                                                    oController.m_oProductService.deleteProductFromWorkspace(oFoundProduct.fileName, oController.m_oActiveWorkspace.workspaceId, bDeleteFile, bDeleteLayer).then(function (data) {
                                                        oController.deleteProductInNavigation(oController.m_aoVisibleBands, that.temp.children_d);
                                                    },(function (error) {
                                                        utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN DELETE PRODUCT");
                                                    }));
                                                }
                                            });
                                        }
                                    }
                                };
                        }

                        //only products has $node.original.fileName
                        if (utilsIsObjectNullOrUndefined($node.original.fileName) == false) {
                            //***************************** PRODUCT ********************************************
                            oReturnValue =
                                {
                                    "Workflow": {
                                        "label": "Workflow",
                                        "action": function (obj) {
                                            var sSourceFileName = $node.original.fileName;
                                            var oFound = oController.findProductByFileName(sSourceFileName);

                                            if (utilsIsObjectNullOrUndefined(oFound) == false) oController.openWorkflowManagerDialog();
                                        }
                                    },
                                    "Zoom2D": {
                                        "label": "Zoom Band 2D Map",
                                        "_disabled": true
                                    },
                                    "Zoom3D": {
                                        "label": "Zoom Band 3D Map",
                                        "_disabled": true
                                    },
                                    "Properties": {
                                        "label": "Properties ",
                                        "icon": "info-icon-context-menu-jstree",
                                        "separator_before": true,
                                        "action": function (obj) {
                                            //$node.original.fileName;
                                            if ((utilsIsObjectNullOrUndefined($node.original.fileName) === false) && (utilsIsStrNullOrEmpty($node.original.fileName) === false)) {
                                                var iNumberOfProdcuts = oController.m_aoProducts.length;
                                                for (var iIndexProducts = 0; iIndexProducts < iNumberOfProdcuts; iIndexProducts++) {
                                                    if (oController.m_aoProducts[iIndexProducts].fileName === $node.original.fileName) {
                                                        oController.openProductInfoDialog(oController.m_aoProducts[iIndexProducts]);
                                                        break;
                                                    }

                                                }

                                            }
                                        }
                                    },
                                    "Download": {
                                        "label": "Download",
                                        "icon": "fa fa-download",
                                        "action": function (obj) {
                                            //$node.original.fileName;
                                            if ((utilsIsObjectNullOrUndefined($node.original.fileName) == false) && (utilsIsStrNullOrEmpty($node.original.fileName) == false)) {
                                                oController.findProductByName($node.original.fileName);
                                                oController.downloadProductByName($node.original.fileName);
                                            }
                                        }
                                    },
                                    "SendToFtp": {
                                        "label": "Send To Ftp",
                                        "icon": "fa fa-upload",
                                        "action": function (obj) {
                                            var sSourceFileName = $node.original.fileName;
                                            var oFound = oController.findProductByFileName(sSourceFileName);

                                            if (utilsIsObjectNullOrUndefined(oFound) == false) oController.openTransferToFtpDialog(oFound);
                                        }
                                    }, //openTransferToFtpDialog
                                    "DeleteProduct": {
                                        "label": "Delete Product",
                                        "icon": "delete-icon-context-menu-jstree",

                                        "action": function (obj) {

                                            utilsVexDialogConfirm("DELETING PRODUCT.<br>ARE YOU SURE?", function (value) {
                                                if (value) {
                                                    bDeleteFile = true;
                                                    bDeleteLayer = true;
                                                    this.temp = $node;
                                                    var that = this;
                                                    oController.m_oProductService.deleteProductFromWorkspace($node.original.fileName, oController.m_oActiveWorkspace.workspaceId, bDeleteFile, bDeleteLayer).then(function (data) {
                                                        oController.deleteProductInNavigation(oController.m_aoVisibleBands, that.temp.children_d);
                                                    },(function (error) {
                                                        utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN DELETE PRODUCT");
                                                    }));
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

            // var oThat = this;

            oNode.children = [
                {
                    "text": "Metadata",
                    "icon": "assets/icons/metadata-24.png",
                    "children": [],
                    "clicked": false,//semaphore
                    "url": oController.m_oProductService.getProductMetadata(oNode.fileName, oController.m_oActiveWorkspace.workspaceId)
                },
                {
                    "text": "Bands",
                    "icon": "assets/icons/bandsTree.png",
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
                } else {
                    oNode.text = "<span class='band-not-published-label'>" + oaBandsItems[iIndexBandsItems].name + "</span>";
                }

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
     * Download of a product
     * @param oEntry
     * @returns {boolean}
     */
    EditorController.prototype.downloadEntry = function (oEntry) {
        if (utilsIsObjectNullOrUndefined(oEntry)) return false;

        var oJson = {
            fileName: oEntry.fileName,
            filePath: oEntry.filePath
        };

        var sFileName = oEntry.fileName;
        // this.m_bIsDownloadingProduct = true;

        var sUrl = null;

        // P.Campanella 17/03/2020: redirect of the download to the node that hosts the workspace
        if (utilsIsStrNullOrEmpty(this.m_oConstantsService.getActiveWorkspace().apiUrl) == false) {
            sUrl = this.m_oConstantsService.getActiveWorkspace().apiUrl;
        }

        this.m_oCatalogService.downloadEntry(oJson, sUrl).then(function (data, status, headers, config) {
            if (utilsIsObjectNullOrUndefined(data) == false) {
                var blob = new Blob([data.data], {type: "application/octet-stream"});
                saveAs(blob, sFileName);
            }
        },(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR DOWNLOADING FILE");
        }));

        return true;
    };

    EditorController.prototype.downloadProductByName = function (sFileName) {
        if (utilsIsStrNullOrEmpty(sFileName) === true) {
            return false;
        }

        var sUrl = null;
        // P.Campanella 17/03/2020: redirect of the download to the node that hosts the workspace
        if (utilsIsStrNullOrEmpty(this.m_oConstantsService.getActiveWorkspace().apiUrl) == false) {
            sUrl = this.m_oConstantsService.getActiveWorkspace().apiUrl;
        }

        this.m_oCatalogService.downloadByName(sFileName, this.m_oActiveWorkspace.workspaceId, sUrl);

        return true;
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

    EditorController.prototype.isEnable2DZoomInTreeInEditorMode = function () {
        if (this.isActiveEditorMode()) {
            return this.m_b2DMapModeOn;
        }
        return false;
    };

    EditorController.prototype.isEnable3DZoomInTreeInEditorMode = function () {
        if (this.isActiveEditorMode()) {
            return !this.m_b2DMapModeOn;
        }
        return false;
    };

    EditorController.prototype.isActiveEditorMode = function () {
        return this.m_bIsActiveGeoraphicalMode === false;
    };

    EditorController.prototype.filterTree = function (sTextQuery) {

        if (utilsIsObjectNullOrUndefined(sTextQuery) === true) {
            sTextQuery = "";
            this.m_bIsFilteredTree = false;
        } else {
            this.m_bIsFilteredTree = true;
        }

        $('#jstree').jstree(true).search(sTextQuery);

        return true;
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
        'NodeService',
        'GlobeService',
        'ProcessesLaunchedService',
        'RabbitStompService',
        'SnapOperationService',
        'ModalService',
        'FilterService',
        '$translate',
        'CatalogService',
        '$window'

    ];

    return EditorController;
})();
