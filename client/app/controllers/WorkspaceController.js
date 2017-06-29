/**
 * Created by p.campanella on 21/10/2016.
 */

var WorkspaceController = (function() {
    function WorkspaceController($scope, $location, oConstantsService, oAuthService, oWorkspaceService,$state,oProductService, oRabbitStompService,oGlobeService,$rootScope,oSatelliteService) {
        this.m_oScope = $scope;
        this.m_oLocation  = $location;
        this.m_oAuthService = oAuthService;
        this.m_oWorkspaceService = oWorkspaceService;
        this.m_oConstantsService = oConstantsService;
        this.m_oScope.m_oController=this;
        this.m_aoWorkspaceList = [];
        this.m_oProductService = oProductService;
        this.m_oState = $state;
        this.m_oScope.m_oController = this;
        this.m_aoProducts = [];//the products of the workspace selected
        this.m_bIsOpenInfo = false;
        this.m_bIsVisibleFiles = false;
        this.m_bLoadingWSFiles = false;
        this.m_oWorkspaceSelected = null;
        this.m_oRabbitStompService = oRabbitStompService;
        this.m_oGlobeService = oGlobeService;
        this.m_oRootScope = $rootScope;
        this.m_oSelectedProduct = null;
        this.m_oWorkspaceSelected = null;
        this.m_oSatelliteService = oSatelliteService;

        this.m_aoSatellitePositions = [];
        this.m_aoSateliteInputTraks = [];
        this.m_oFakePosition = null;
        this.m_oUfoPointer = null;

        this.fetchWorkspaceInfoList();
        this.m_oRabbitStompService.unsubscribe();
        // this.m_oGlobeService.initGlobe('cesiumContainer3');

        this.m_oGlobeService.initRotateGlobe('cesiumContainer3');
        this.m_oGlobeService.goHome();

        this.getTrackSatellite();


        /*
        // TEST TO CHANGE UFO IMAGE ON DOUBLE CLICK
        // Mouse over the globe to see the cartographic position
        handler = new Cesium.ScreenSpaceEventHandler(this.m_oGlobeService.getGlobe().scene.canvas);

        var oController = this;

        handler.setInputAction(function(movement) {

            var pick = oController.m_oGlobeService.getGlobe().scene.pick(movement.position);
            if (Cesium.defined(pick) && pick.id._name === "U.F.O.") {

                if (pick.id._billboard._image._value !== "assets/icons/globeIcons/UFO.png") {
                    oController.m_oGlobeService.removeEntity(oController.m_oUfoPointer);
                    oController.m_oUfoPointer = oController.m_oGlobeService.drawPointWithImage(utilsProjectConvertCurrentPositionFromServerInCesiumDegrees(oController.m_oFakePosition),"assets/icons/globeIcons/UFO.png","U.F.O.","?");
                }
            }


        }, Cesium.ScreenSpaceEventType.LEFT_DOUBLE_CLICK);
        */
    }

    WorkspaceController.prototype.moveTo = function (sPath) {
        this.m_oLocation.path(sPath);
    }


    WorkspaceController.prototype.createWorkspace = function () {

        var oController = this;

        this.m_oWorkspaceService.createWorkspace().success(function (data, status) {
            if (data != null)
            {
                if (data != undefined)
                {
                    var sWorkspaceId = data.stringValue;
                    oController.openWorkspace(sWorkspaceId);

                }
            }
        }).error(function (data,status) {
            //alert('error');
            utilsVexDialogAlertTop('Error in create WorkSpace. WorkspaceController.js');
        });
    };


    WorkspaceController.prototype.openWorkspace = function (sWorkspaceId) {
        var oController = this;

        this.m_oWorkspaceService.getWorkspaceEditorViewModel(sWorkspaceId).success(function (data, status) {
            if (data != null)
            {
                if (data != undefined)
                {
                    oController.m_oConstantsService.setActiveWorkspace(data);
                    oController.m_oRabbitStompService.subscribe(sWorkspaceId);
                    oController.m_oState.go("root.editor", { workSpace : sWorkspaceId });//use workSpace when reload editor page
                    //oController.m_oLocation.path('editor');
                }
            }
        }).error(function (data,status) {
            //alert('error');
            utilsVexDialogAlertTop('Error Opening the Workspace');
        });
    }

    WorkspaceController.prototype.getWorkspaceInfoList = function () {
        return this.m_aoWorkspaceList;
    }

    WorkspaceController.prototype.fetchWorkspaceInfoList = function () {

        if (this.m_oConstantsService.getUser() != null) {
            if (this.m_oConstantsService.getUser() != undefined) {

                var oController = this;

                this.m_oWorkspaceService.getWorkspacesInfoListByUser().success(function (data, status) {
                    if (data != null)
                    {
                        if (data != undefined)
                        {
                            oController.m_aoWorkspaceList = data;
                        }
                    }
                }).error(function (data,status) {
                    //alert('error');
                    utilsVexDialogAlertTop('Error in WorkspacesInfo. WorkspaceController.js');
                });
            }
        }

    };

    WorkspaceController.prototype.loadProductList = function(oWorkspace)
    {
        /*start rotate globe position home*/
        this.m_bLoadingWSFiles = true;
       // this.m_oGlobeService.goHome();
        //this.m_oGlobeService.startRotationGlobe(1);

        if(utilsIsObjectNullOrUndefined(oWorkspace)) return false;
        if(utilsIsStrNullOrEmpty(oWorkspace.workspaceId)) return false;


        if (utilsIsObjectNullOrUndefined(this.m_oWorkspaceSelected) == false) {

            if (this.m_oWorkspaceSelected.workspaceId == oWorkspace.workspaceId) {

                //DESELECT:
                for (var i =0; i<this.m_aoProducts.length; i++) {
                    this.m_oGlobeService.removeEntity(this.m_aoProducts[i].oRectangle)
                }


                this.m_oWorkspaceSelected = null;
                this.m_bIsVisibleFiles = false;
                this.m_bLoadingWSFiles = false;
                this.m_bIsOpenInfo = false;

                this.m_oGlobeService.flyHome();
                this.m_oGlobeService.startRotationGlobe(3);

                return;
            }
        }

        this.m_oWorkspaceSelected = oWorkspace;

        var oController = this;

        this.m_bIsVisibleFiles = true;
        this.m_bIsOpenInfo = false;

        var oWorkspaceId = oWorkspace.workspaceId;
        this.m_oWorkspaceSelected = oWorkspace;

        this.m_bIsVisibleFiles = true;

        this.m_oProductService.getProductListByWorkspace(oWorkspaceId).success(function (data, status) {
            if(!utilsIsObjectNullOrUndefined(data))
            {
                for (var i =0; i<oController.m_aoProducts.length; i++) {
                    oController.m_oGlobeService.removeEntity(oController.m_aoProducts[i].oRectangle)
                }

                oController.m_aoProducts = [];
                for(var iIndex = 0; iIndex < data.length; iIndex++)
                {
                    oController.m_aoProducts.push(data[iIndex]);
                }
                oController.m_bIsOpenInfo = true;
            }

            if(utilsIsObjectNullOrUndefined( oController.m_aoProducts) || oController.m_aoProducts.length == 0)
            {
                oController.m_bIsVisibleFiles = false;
            }else{
                //add globe bounding box
                //oController.m_oGlobeService.removeAllEntities();

                oController.createBoundingBoxInGlobe();
            }

            oController.m_bLoadingWSFiles = false;
        }).error(function (data,status) {
            oController.m_bLoadingWSFiles = false;
            utilsVexDialogAlertTop("Error loading Workspace products");
        });

        return true;
    }

    WorkspaceController.prototype.showCursorOnWSRow = function (sWorkspaceId) {
        if (utilsIsObjectNullOrUndefined(this.m_oWorkspaceSelected)) return false;
        if (utilsIsObjectNullOrUndefined(this.m_oWorkspaceSelected.workspaceId))  return false;
        if (this.m_oWorkspaceSelected.workspaceId  == sWorkspaceId) return true;
        else return false;
    }

    WorkspaceController.prototype.createBoundingBoxInGlobe = function () {

        var oRectangle = null;
        var aArraySplit = [];
        var iArraySplitLength = 0;
        var iInvertedArraySplit = [];

        var aoTotalArray = [];

        // Check we have products
        if(utilsIsObjectNullOrUndefined(this.m_aoProducts) === true) return false;

        var iProductsLength = this.m_aoProducts.length;

        // For each product
        for(var iIndexProduct = 0; iIndexProduct < iProductsLength; iIndexProduct++){
            iInvertedArraySplit = [];
            aArraySplit = [];
            // skip if there isn't the product bounding box
            if(utilsIsObjectNullOrUndefined(this.m_aoProducts[iIndexProduct].bbox) === true ) continue;

            // Split bbox string
            aArraySplit = this.m_aoProducts[iIndexProduct].bbox.split(",");
            aoTotalArray.push.apply(aoTotalArray,aArraySplit);
            iArraySplitLength = aArraySplit.length;

            if(iArraySplitLength !== 10) continue;

            for(var iIndex = 0; iIndex < iArraySplitLength-1; iIndex = iIndex + 2){
                iInvertedArraySplit.push(aArraySplit[iIndex+1]);
                iInvertedArraySplit.push(aArraySplit[iIndex]);
            }

            oRectangle = this.m_oGlobeService.addRectangleOnGlobeParamArray(iInvertedArraySplit);
            this.m_aoProducts[iIndexProduct].oRectangle = oRectangle;
            this.m_aoProducts[iIndexProduct].aBounds = iInvertedArraySplit;
        }


        var aoBounds = [];
        for (var iIndex = 0; iIndex < aoTotalArray.length - 1; iIndex = iIndex + 2) {
            aoBounds.push(new Cesium.Cartographic.fromDegrees(aoTotalArray[iIndex + 1], aoTotalArray[iIndex ]));
        }

        var oWSRectangle = Cesium.Rectangle.fromCartographicArray(aoBounds);
        var oWSCenter = Cesium.Rectangle.center(oWSRectangle);

        //oGlobe.camera.setView({
        this.m_oGlobeService.getGlobe().camera.flyTo({
            destination : Cesium.Cartesian3.fromRadians(oWSCenter.longitude, oWSCenter.latitude, this.m_oGlobeService.getWorkspaceZoom()),
            orientation: {
                heading: 0.0,
                pitch: -Cesium.Math.PI_OVER_TWO,
                roll: 0.0
            }
        });

        this.m_oGlobeService.stopRotationGlobe();

    };

    WorkspaceController.prototype.clickOnProduct = function (oProductInput) {
        if(this.m_oSelectedProduct === oProductInput)
        {
            this.m_oSelectedProduct = null;
            return false;
        }
        if(utilsIsObjectNullOrUndefined(oProductInput.aBounds) === true)
            return false;
        this.m_oSelectedProduct = oProductInput;
        var aBounds = oProductInput.aBounds;
        var aBoundsLength = aBounds.length;
        var aoRectangleBounds = [];
        var oGlobe = this.m_oGlobeService.getGlobe();

        // var temp = null;
        if(utilsIsObjectNullOrUndefined(oProductInput) === true)
            return false;
        if(utilsIsObjectNullOrUndefined( oProductInput.oRectangle) === true)
            return false;
        this.m_oGlobeService.stopRotationGlobe();

        // for(var iIndexBounds = 0, iIndexRectangle = 0; iIndexBounds < aBoundsLength; iIndexBounds++){
        //     temp.push(aBounds[iIndexBounds])
        //
        //     if( ( ( iIndexBounds + 1 ) % 2 )  === 0)
        //     {
        //         aaRectangle[iIndexRectangle] = temp;
        //         temp = [];
        //         iIndexRectangle++;
        //     }
        //
        // }
        //
        // this.m_oGlobeService.zoomOnLayerBoundingBox(aaRectangle);
        for(var iIndexBound = 0; iIndexBound < aBoundsLength-1; iIndexBound = iIndexBound + 2){
            // temp = aBounds[iIndexBound];
            // aBounds[iIndexBound] = aBounds[iIndexBound+1];
            // aBounds[iIndexBound+1] = temp;
            aoRectangleBounds.push(new Cesium.Cartographic.fromDegrees(aBounds[iIndexBound ],aBounds[iIndexBound + 1]));

        }


        var zoom = Cesium.Rectangle.fromCartographicArray(aoRectangleBounds);
        oGlobe.camera.setView({
            destination: zoom,
            orientation: {
                heading: 0.0,
                pitch: -Cesium.Math.PI_OVER_TWO,
                roll: 0.0
            }
        });


        // this.m_oGlobeService.zoomOnLayerParamArray(aBounds);// this.m_oGlobeService.zoomOnLayerBoundingBox(aaRectangle);
        return true;
    };

    WorkspaceController.prototype.getProductList = function()
    {
        return this.m_aoProducts;
    };
    WorkspaceController.prototype.isEmptyProductList = function()
    {
        if(utilsIsObjectNullOrUndefined(this.m_aoProducts) || this.m_aoProducts.length == 0) return true;
        return false;
    };

    WorkspaceController.prototype.isSelectedRowInWorkspaceTable = function(oWorkspace)
    {
        if(utilsIsObjectNullOrUndefined(oWorkspace))
            return '';
        if(utilsIsStrNullOrEmpty(oWorkspace.workspaceId))
            return '';

        if(utilsIsObjectNullOrUndefined(this.m_oWorkspaceSelected))
            return '';
        if(utilsIsStrNullOrEmpty(this.m_oWorkspaceSelected.workspaceId))
            return '';

        if(oWorkspace.workspaceId != this.m_oWorkspaceSelected.workspaceId)
            return '';

        return 'selected-row';
    };

	WorkspaceController.prototype.DeleteWorkspace = function (sWorkspaceId) {

        var oController = this;

        utilsVexDialogConfirmWithCheckBox("Are you sure to delete the Workspace ?", function (value) {
            var bDeleteFile = false;
            var bDeleteLayer = false;
            if (value) {
                if (value.files == 'on')
                    bDeleteFile = true;
                if (value.geoserver == 'on')
                    bDeleteLayer = true;

                oController.m_oWorkspaceService.DeleteWorkspace(sWorkspaceId, bDeleteFile, bDeleteLayer).success(function (data, status) {
                    oController.fetchWorkspaceInfoList();

                }).error(function (data, status) {

                });
            }
        });
    };

    WorkspaceController.prototype.getTrackSatellite = function ()
    {
        var oController = this;

        var iSat;

        this.m_aoSateliteInputTraks = this.m_oGlobeService.getSatelliteTrackInputList();


        for (iSat=0; iSat<this.m_aoSateliteInputTraks.length; iSat++) {

            var oActualSat = this.m_aoSateliteInputTraks[iSat];
            var oFakePosition = null;

            this.m_oSatelliteService.getTrackSatellite(this.m_aoSateliteInputTraks[iSat].name).then( function successCallback(response) {

                if(utilsIsObjectNullOrUndefined(response) === false)
                {
                    var oData = response.data;

                    if(utilsIsObjectNullOrUndefined(oData) === false)
                    {

                        for (iOriginalSat=0; iOriginalSat<oController.m_aoSateliteInputTraks.length; iOriginalSat++) {
                            if (oController.m_aoSateliteInputTraks[iOriginalSat].name === oData.code) {
                                oActualSat = oController.m_aoSateliteInputTraks[iOriginalSat];
                                break;
                            }
                        }

                        // Commented: this is to show orbit
                        //oController.m_oSentinel_1a_position.lastPositions = oController.m_oGlobeService.drawOutLined(utilsProjectConvertPositionsSatelliteFromServerInCesiumArray(oData.lastPositions),Cesium.Color.DARKBLUE,"Past track");
                        //oController.m_oSentinel_1a_position.nextPositions = oController.m_oGlobeService.drawOutLined(utilsProjectConvertPositionsSatelliteFromServerInCesiumArray(oData.nextPositions),Cesium.Color.CHARTREUSE,"Future track");

                        var sDescription = oActualSat.description;
                        sDescription += "\n";
                        sDescription += oData.currentTime;

                        var oActualPosition = oController.m_oGlobeService.drawPointWithImage(utilsProjectConvertCurrentPositionFromServerInCesiumDegrees(oData.currentPosition),oActualSat.icon,sDescription,oActualSat.label);
                        oController.m_aoSatellitePositions.push(oActualPosition);

                        if (oController.m_oFakePosition === null) {
                            if (oData.lastPositions != null) {

                                var iFakeIndex =  Math.floor(Math.random() * (oData.lastPositions.length));

                                oController.m_oFakePosition = oData.lastPositions[iFakeIndex];

                                var aoUfoPosition = utilsProjectConvertCurrentPositionFromServerInCesiumDegrees(oController.m_oFakePosition);
                                aoUfoPosition[2] = aoUfoPosition[2]*2;
                                oController.m_oUfoPointer = oController.m_oGlobeService.drawPointWithImage(aoUfoPosition,"assets/icons/alien.svg","U.F.O.","?");

                                iFakeIndex =  Math.floor(Math.random() * (oData.lastPositions.length));
                                var aoMoonPosition = utilsProjectConvertCurrentPositionFromServerInCesiumDegrees(oData.lastPositions[iFakeIndex]);
                                aoMoonPosition[2] = 384400000;

                                oController.m_oGlobeService.drawPointWithImage(aoMoonPosition,"assets/icons/sat_death.svg","Moon","-");

                            }
                        }
                    }
                }
            }, function errorCallback(response) {
            });
        }

    };

    //

    WorkspaceController.prototype.deleteSentinel1a = function(value){
        if(value)
        {
            this.getTrackSatellite();
        }
        else
        {
            for (var i=0; i<this.m_aoSatellitePositions.length; i++) {
                this.m_oGlobeService.removeEntity(this.m_aoSatellitePositions[i]);
            }

            this.m_oGlobeService.removeEntity(this.m_oUfoPointer);
            this.m_oUfoPointer = null;
            this.m_oFakePosition = null;

            this.m_aoSatellitePositions = [];
        }

    };

    WorkspaceController.$inject = [
        '$scope',
        '$location',
        'ConstantsService',
        'AuthService',
        'WorkspaceService',
        '$state',
        'ProductService',
        'RabbitStompService',
        'GlobeService',
        '$rootScope',
        'SatelliteService'
    ];
    return WorkspaceController;
}) ();