/**
 * Created by p.campanella on 21/10/2016.
 */

var WorkspaceController = (function () {
    function WorkspaceController($scope, $location, oConstantsService, oAuthService, oWorkspaceService, $state,
                                 oProductService, oRabbitStompService, oGlobeService, $rootScope, oOpportunitySearchService,
                                 $interval, oTranslate) {
        this.m_oScope = $scope;
        this.m_oLocation = $location;
        this.m_oAuthService = oAuthService;
        this.m_oWorkspaceService = oWorkspaceService;
        this.m_oConstantsService = oConstantsService;
        this.m_oScope.m_oController = this;
        this.m_aoWorkspaceList = [];
        this.m_bIsLoading = true;
        this.m_oProductService = oProductService;
        this.m_oState = $state;
        this.m_oScope.m_oController = this;
        this.m_aoProducts = [];
        this.m_bIsOpenInfo = false;
        this.m_bIsVisibleFiles = false;
        this.m_bLoadingWSFiles = false;
        this.m_oRabbitStompService = oRabbitStompService;
        this.m_oGlobeService = oGlobeService;
        this.m_oRootScope = $rootScope;
        this.m_oSelectedProduct = null;
        this.m_oWorkspaceSelected = null;
        this.m_oOpportunitySearchService = oOpportunitySearchService;
        this.m_aoSatellitePositions = [];
        this.m_aoSateliteInputTraks = [];
        this.m_oFakePosition = null;
        this.m_oUfoPointer = null;

        this.m_oTranslate = oTranslate;


        this.m_oWorkspaceViewModel = null; // the model view of the selected workspace
        this.m_Count = null; // the count of processes executed in the selected workspace

        this.m_bOpeningWorkspace = false;
        this.m_oReturnValue = {};

        this.m_oSort = {
            sColumn: '',
            bDescending: false
        };        

        if(utilsIsObjectNullOrUndefined(oConstantsService.getUser())){
            this.m_oState.go("home");
        }
        this.fetchWorkspaceInfoList();
        this.m_oRabbitStompService.unsubscribe();

        this.m_oGlobeService.initRotateGlobe('cesiumContainer3');
        this.m_oGlobeService.goHome();

        this.getTrackSatellite();


        this.m_oUpdatePositionSatellite = $interval(function () {
            $scope.m_oController.updatePositionsSatellites();
        }, 15000);


        /*
        * ANGULAR DOCS:
        * Note: Intervals created by this service must be explicitly destroyed when you are finished with them.
        * In particular they are not automatically destroyed when a controller's scope or a directive's element are destroyed.
        * You should take this into consideration and make sure to always cancel the interval at the appropriate moment.
        * */
        $scope.$on('$destroy', function () {
            // Make sure that the interval is destroyed too
            if (angular.isDefined($scope.m_oController.m_oUpdatePositionSatellite)) {
                $interval.cancel($scope.m_oController.m_oUpdatePositionSatellite);
                $scope.m_oController.m_oUpdatePositionSatellite = undefined;
            }
        });


        this.isLoadingIconVisible = function () {
            // return false;
            if (this.m_oRabbitStompService.isReadyState() === false) {
                return true;
            }
            return false;
        }
    }

    WorkspaceController.prototype.getLastTouchDate = function () {
        if (this.m_oWorkspaceViewModel === null) {
            return "";
        } else {
            return new Date(this.m_oWorkspaceViewModel.lastEditDate).toString().replace("\"", "");
        }
    }


    WorkspaceController.prototype.moveTo = function (sPath) {
        this.m_oLocation.path(sPath);
    }


    WorkspaceController.prototype.createWorkspace = function () {

        var oController = this;
        var sError = this.m_oTranslate.instant("MSG_MKT_WS_CREATE_ERROR");

        this.m_oWorkspaceService.createWorkspace().then(function (data, status) {
            if (data.data != null) {
                if (data.data != undefined) {
                    if (data.data.boolValue) {
                        var sWorkspaceId = data.data.stringValue;
                        oController.openWorkspace(sWorkspaceId);
                    } else {
                        //alert('error');
                        utilsVexDialogAlertTop(sError + "<br><br>" + oController.m_oTranslate.instant(data.data.stringValue));
                    }

                }
            }
        },(function (error, status) {
            //alert('error');
            utilsVexDialogAlertTop(sError);
        }));
    };


    WorkspaceController.prototype.openWorkspace = function (sWorkspaceId, bActiveNode) {
        if (bActiveNode === false) {
            var sNodeInactiveMessage = this.m_oTranslate.instant("MSG_MKT_WS_OPEN_NODE_INACTIVE");
            utilsVexDialogAlertTop(sNodeInactiveMessage);

            return false;
        }

        // Stop loading new workspaces.. we are leaving!
        this.m_bOpeningWorkspace = true;
        var oController = this;
        var sError = this.m_oTranslate.instant("MSG_MKT_WS_OPEN_ERROR");

        this.m_oWorkspaceService.getWorkspaceEditorViewModel(sWorkspaceId).then(function (data, status) {
            if (data.data != null) {
                if (data.data != undefined) {
                    try {
                        oController.m_oRabbitStompService.subscribe(sWorkspaceId);
                    } catch (error) {
                        console.error(error);
                    }
                    // oController.m_oRabbitStompService.subscribe(sWorkspaceId);
                    oController.m_oState.go("root.editor", {workSpace: sWorkspaceId});//use workSpace when reload editor page
                    oController.m_oConstantsService.setActiveWorkspace(data.data);
                }
            }
        },(function (error, status) {
            //alert('error');
            utilsVexDialogAlertTop(sError);
        }));
    }


    WorkspaceController.prototype.getWorkspaceInfoList = function () {
        return this.m_aoWorkspaceList;
    }

    WorkspaceController.prototype.getWorkspaceViewModel = function () {
        return this.m_oWorkspaceViewModel;
    };


    WorkspaceController.prototype.fetchWorkspaceInfoList = function () {

        var sError = this.m_oTranslate.instant("MSG_MKT_WS_OPEN_ERROR");

        if (this.m_oConstantsService.getUser() != null) {
            if (this.m_oConstantsService.getUser() != undefined) {

                var oController = this;
                let sDate = ""

                this.m_oWorkspaceService.getWorkspacesInfoListByUser().then(function (data, status) {
                    if (data.data != null) {
                        if (data.data != undefined) {
                            //data.data = []; // DEBUG
                            oController.m_aoWorkspaceList = data.data;
                            oController.m_aoWorkspaceList.forEach(oWorkspace => {
                                if(utilsIsObjectNullOrUndefined(oWorkspace.creationDate)) {
                                  oWorkspace.creationDate = "N/A"
                               } else {
                                   sDate = new Date(oWorkspace.creationDate)
                                   oWorkspace.creationDate = sDate.toISOString().replace(/T/, ' ').replace(/\..+/, '').substr(0, 10)
                               }
                               })
                            oController.m_bIsLoading = false;
                        }
                    }
                    oController.m_bIsLoading = false;
                },(function (data, status) {
                    utilsVexDialogAlertTop(sError);
                    oController.m_bIsLoading = false;
                }));
            }
        }

    };

    WorkspaceController.prototype.deselectWorskpace = function () //oWorkspace)
    {
        for (var i = 0; i < this.m_aoProducts.length; i++) {
            this.m_oGlobeService.removeEntity(this.m_aoProducts[i].oRectangle)
        }

        this.m_oWorkspaceSelected = null;
        this.m_oWorkspaceViewModel = null
        this.m_bIsVisibleFiles = false;
        this.m_bLoadingWSFiles = false;
        this.m_bIsOpenInfo = false;

        this.m_oGlobeService.flyHome();
        this.m_oGlobeService.startRotationGlobe(3);
    }

    WorkspaceController.prototype.loadProductList = function (oWorkspace) {
        // View is leaving...
        if (this.m_bOpeningWorkspace) return;

        this.m_bLoadingWSFiles = true;

        if (utilsIsObjectNullOrUndefined(oWorkspace)) return false;
        if (utilsIsStrNullOrEmpty(oWorkspace.workspaceId)) return false;


        if (utilsIsObjectNullOrUndefined(this.m_oWorkspaceSelected) == false) {

            if (this.m_oWorkspaceSelected.workspaceId == oWorkspace.workspaceId) {

                this.deselectWorskpace();

                return;
            }
        }


        var oController = this;
        this.m_bIsVisibleFiles = true;
        this.m_bIsOpenInfo = false;
        var oWorkspaceId = oWorkspace.workspaceId;
        this.m_bIsVisibleFiles = true;
        var sError = this.m_oTranslate.instant("MSG_MKT_WS_OPEN_ERROR");

        this.m_oWorkspaceService.getWorkspaceEditorViewModel(oWorkspaceId).then(function (data, status) {
            if (!utilsIsObjectNullOrUndefined(data)) {
                oController.m_oWorkspaceViewModel = data.data;
            }
        },(function (error, status) {
            utilsVexDialogAlertTop(sError);
        }));

        this.m_oProductService.getProductLightListByWorkspace(oWorkspaceId).then(function (data, status) {
            if (!utilsIsObjectNullOrUndefined(data.data)) {
                for (var i = 0; i < oController.m_aoProducts.length; i++) {
                    oController.m_oGlobeService.removeEntity(oController.m_aoProducts[i].oRectangle)
                }

                oController.m_aoProducts = [];
                for (var iIndex = 0; iIndex < data.data.length; iIndex++) {
                    oController.m_aoProducts.push(data.data[iIndex]);
                }
                oController.m_bIsOpenInfo = true;
                oController.m_oWorkspaceSelected = oWorkspace;

            }

            if (utilsIsObjectNullOrUndefined(oController.m_aoProducts) || oController.m_aoProducts.length == 0) {
                oController.m_bIsVisibleFiles = false;
            } else {
                //add globe bounding box
                oController.createBoundingBoxInGlobe();
            }

            oController.m_bLoadingWSFiles = false;
        },(function (data, status) {
            oController.m_bLoadingWSFiles = false;
            utilsVexDialogAlertTop(sError);
        }));

        return true;
    }

    WorkspaceController.prototype.showCursorOnWSRow = function (sWorkspaceId) {
        if (utilsIsObjectNullOrUndefined(this.m_oWorkspaceSelected)) return false;
        if (utilsIsObjectNullOrUndefined(this.m_oWorkspaceSelected.workspaceId)) return false;
        if (this.m_oWorkspaceSelected.workspaceId == sWorkspaceId) return true;

        return false;
    }

    WorkspaceController.prototype.createBoundingBoxInGlobe = function () {

        var oRectangle = null;
        var aArraySplit = [];
        var iArraySplitLength = 0;
        var aiInvertedArraySplit = [];

        var aoTotalArray = [];

        // Check we have products
        if (utilsIsObjectNullOrUndefined(this.m_aoProducts) === true) return false;

        var iProductsLength = this.m_aoProducts.length;

        // For each product
        for (var iIndexProduct = 0; iIndexProduct < iProductsLength; iIndexProduct++) {

            aiInvertedArraySplit = [];
            aArraySplit = [];
            // skip if there isn't the product bounding box
            if (utilsIsObjectNullOrUndefined(this.m_aoProducts[iIndexProduct].bbox) === true) continue;

            // Split bbox string
            aArraySplit = this.m_aoProducts[iIndexProduct].bbox.split(",");
            iArraySplitLength = aArraySplit.length;

            if (iArraySplitLength < 10) continue;

            var bHasNan = false;
            for (var iValues = 0; iValues < aArraySplit.length; iValues++) {
                if (isNaN(aArraySplit[iValues])) {
                    bHasNan = true;
                    break;
                }
            }

            if (bHasNan) continue;

            aoTotalArray.push.apply(aoTotalArray, aArraySplit);

            for (var iIndex = 0; iIndex < iArraySplitLength - 1; iIndex = iIndex + 2) {
                aiInvertedArraySplit.push(aArraySplit[iIndex + 1]);
                aiInvertedArraySplit.push(aArraySplit[iIndex]);
            }

            oRectangle = this.m_oGlobeService.addRectangleOnGlobeParamArray(aiInvertedArraySplit);
            this.m_aoProducts[iIndexProduct].oRectangle = oRectangle;
            this.m_aoProducts[iIndexProduct].aBounds = aiInvertedArraySplit;
        }


        var aoBounds = [];
        for (var iIndex = 0; iIndex < aoTotalArray.length - 1; iIndex = iIndex + 2) {
            aoBounds.push(new Cesium.Cartographic.fromDegrees(aoTotalArray[iIndex + 1], aoTotalArray[iIndex]));
        }

        var oWSRectangle = Cesium.Rectangle.fromCartographicArray(aoBounds);
        var oWSCenter = Cesium.Rectangle.center(oWSRectangle);

        //oGlobe.camera.setView({
        this.m_oGlobeService.getGlobe().camera.flyTo({
            destination: Cesium.Cartesian3.fromRadians(oWSCenter.longitude, oWSCenter.latitude, this.m_oGlobeService.getWorkspaceZoom()),
            orientation: {
                heading: 0.0,
                pitch: -Cesium.Math.PI_OVER_TWO,
                roll: 0.0
            }
        });

        this.m_oGlobeService.stopRotationGlobe();

    };

    WorkspaceController.prototype.clickOnProduct = function (oProductInput) {
        if (this.m_oSelectedProduct === oProductInput) {
            this.m_oSelectedProduct = null;
            return false;
        }
        if (utilsIsObjectNullOrUndefined(oProductInput.aBounds) === true)
            return false;
        this.m_oSelectedProduct = oProductInput;
        var aBounds = oProductInput.aBounds;
        var aBoundsLength = aBounds.length;
        var aoRectangleBounds = [];
        var oGlobe = this.m_oGlobeService.getGlobe();

        // var temp = null;
        if (utilsIsObjectNullOrUndefined(oProductInput) === true)
            return false;
        if (utilsIsObjectNullOrUndefined(oProductInput.oRectangle) === true)
            return false;
        this.m_oGlobeService.stopRotationGlobe();

        for (var iIndexBound = 0; iIndexBound < aBoundsLength - 1; iIndexBound = iIndexBound + 2) {
            aoRectangleBounds.push(new Cesium.Cartographic.fromDegrees(aBounds[iIndexBound], aBounds[iIndexBound + 1]));
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

        return true;
    };

    WorkspaceController.prototype.getProductList = function () {
        return this.m_aoProducts;
    };
    WorkspaceController.prototype.getProcessCount = function () {
        return this.m_Count;
    };

    WorkspaceController.prototype.isEmptyProductList = function () {
        if (utilsIsObjectNullOrUndefined(this.m_aoProducts) || this.m_aoProducts.length == 0) return true;
        return false;
    };

    WorkspaceController.prototype.isSelectedRowInWorkspaceTable = function (oWorkspace) {
        if (utilsIsObjectNullOrUndefined(oWorkspace))
            return false;
        if (utilsIsStrNullOrEmpty(oWorkspace.workspaceId))
            return false;

        if (utilsIsObjectNullOrUndefined(this.m_oWorkspaceSelected))
            return false;
        if (utilsIsStrNullOrEmpty(this.m_oWorkspaceSelected.workspaceId))
            return false;

        if (oWorkspace.workspaceId != this.m_oWorkspaceSelected.workspaceId)
            return false;

        return true;
    };
    /**
     *
     * @param sWorkspaceId
     * @constructor
     */
    WorkspaceController.prototype.DeleteWorkspace = function (sWorkspaceId) {

        var oController = this;
        let oWorkspaceViewModel = undefined;
        let oActiveWorkspace = undefined; 

        var sConfirmMsg1 = this.m_oTranslate.instant("MSG_DELETE_WS_1");
        var sConfirmMsg2 = this.m_oTranslate.instant("MSG_DELETE_WS_2");

        oController.m_oWorkspaceService.getWorkspaceEditorViewModel(sWorkspaceId).then(function (data, status) {
            oWorkspaceViewModel = data.data;
            utilsVexDialogConfirm(sConfirmMsg1 + oWorkspaceViewModel.name + sConfirmMsg2, function (value) {
                if (value) {
                    bDeleteFile = true;
                    bDeleteLayer = true;
    
                        
    
                        oController.m_oWorkspaceService.DeleteWorkspace(oWorkspaceViewModel , bDeleteFile, bDeleteLayer)
                            .then(function () {
                                oActiveWorkspace = oController.m_oConstantsService.getActiveWorkspace();
                                
                                if (_.isEqual(oActiveWorkspace, oWorkspaceViewModel)) {
                                //clear workspace data from View Model
                                oWorkspaceViewModel = null;
                                //set active workspace to null
                                oController.m_oConstantsService.setActiveWorkspace(
                                    oWorkspaceViewModel
                                );
                                }
                                oController.deselectWorskpace();
                                oController.fetchWorkspaceInfoList();
                            },(function () {
                                console.log("WorkspaceController.prototype.DeleteWorkspace: oController.m_oWorkspaceService.DeleteWorkspace failed")
                            }));
                    
    
                }
            });


    },(function () {
        console.log("WorkspaceController.prototype.DeleteWorkspace: oController.m_oWorkspaceService.getWorkspaceEditorViewModel failed")
    }));

        
    };

    WorkspaceController.prototype.getTrackSatellite = function () {
        var oController = this;
        var iSat;

        this.m_aoSateliteInputTraks = this.m_oGlobeService.getSatelliteTrackInputList();

        //Remove all old Entities from the map
        this.m_oGlobeService.removeAllEntities();

        for (iSat = 0; iSat < this.m_aoSateliteInputTraks.length; iSat++) {

            var oActualSat = this.m_aoSateliteInputTraks[iSat];


            this.m_oOpportunitySearchService.getTrackSatellite(this.m_aoSateliteInputTraks[iSat].name).then(function successCallback(response) {

                if (utilsIsObjectNullOrUndefined(response) === false) {
                    var oData = response.data;

                    if (utilsIsObjectNullOrUndefined(oData) === false) {

                        for (var iOriginalSat = 0; iOriginalSat < oController.m_aoSateliteInputTraks.length; iOriginalSat++) {
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

                        if (utilsIsObjectNullOrUndefined(oData.currentPosition)) {
                            return;
                        }

                        var oActualPosition = oController.m_oGlobeService.drawPointWithImage(utilsProjectConvertCurrentPositionFromServerInCesiumDegrees(oData.currentPosition), oActualSat.icon, sDescription, oActualSat.label, 32, 32);
                        oController.m_aoSatellitePositions.push(oActualPosition);

                        if (oController.m_oFakePosition === null) {
                            if (oData.lastPositions != null) {

                                var iFakeIndex = Math.floor(Math.random() * (oData.lastPositions.length));

                                oController.m_oFakePosition = oData.lastPositions[iFakeIndex];

                                var aoUfoPosition = utilsProjectConvertCurrentPositionFromServerInCesiumDegrees(oController.m_oFakePosition);
                                aoUfoPosition[2] = aoUfoPosition[2] * 4;
                                oController.m_oUfoPointer = oController.m_oGlobeService.drawPointWithImage(aoUfoPosition, "assets/icons/alien.svg", "U.F.O.", "?");

                                iFakeIndex = Math.floor(Math.random() * (oData.lastPositions.length));
                                var aoMoonPosition = utilsProjectConvertCurrentPositionFromServerInCesiumDegrees(oData.lastPositions[iFakeIndex]);
                                //aoMoonPosition [0] = 0.0;
                                //aoMoonPosition[1] = 0.0;
                                aoMoonPosition[2] = 384400000;
                                //aoMoonPosition[2] = 3844000;

                                oController.m_oGlobeService.drawPointWithImage(aoMoonPosition, "assets/icons/sat_death.svg", "Moon", "-");

                            }
                        }
                    }
                }
            }, function errorCallback(response) {
            });
        }

    };

    WorkspaceController.prototype.updatePositionsSatellites = function () {
        if (utilsIsObjectNullOrUndefined(this.m_aoSatellitePositions)) return false;


        this.m_aoSateliteInputTraks = this.m_oGlobeService.getSatelliteTrackInputList();

        this.updatePosition();

        return true;
    }

    WorkspaceController.prototype.updatePosition = function () {
        var sSatellites = "";
        for (var iSat = 0; iSat < this.m_aoSateliteInputTraks.length; iSat++) {
            sSatellites += this.m_aoSateliteInputTraks[iSat].name + "-";
        }

        var oController = this;

        this.m_oOpportunitySearchService.getUpdatedTrackSatellite(sSatellites).then(function successCallback(response) {
            if (utilsIsObjectNullOrUndefined(response) === false) {
                var oData = response.data;
                if (utilsIsObjectNullOrUndefined(oData) === false) {
                    for (var iSatellites = 0; iSatellites < oData.length; iSatellites++) {
                        var oActualDataByServer = oData[iSatellites];

                        var iIndexActualSatellitePosition = oController.getIndexActualSatellitePositions(oData[iSatellites].code);

                        if (iIndexActualSatellitePosition >= 0) {
                            var oSatellite = oController.m_aoSatellitePositions[iIndexActualSatellitePosition];
                            var aPosition = utilsProjectConvertCurrentPositionFromServerInCesiumDegrees(oActualDataByServer.currentPosition);
                            var oCesiumBoundaries = Cesium.Cartesian3.fromDegrees(aPosition[0], aPosition[1], aPosition[2]);
                            oController.m_oGlobeService.updateEntityPosition(oSatellite, oCesiumBoundaries);
                        }
                    }
                }
            }
        }, function errorCallback(response) {
        });

        return true;
    };

    WorkspaceController.prototype.getIndexActualSatellitePositions = function (sCode) {
        for (var iOriginalSat = 0; iOriginalSat < this.m_aoSateliteInputTraks.length; iOriginalSat++) {
            if (this.m_aoSateliteInputTraks[iOriginalSat].name === sCode) {
                return iOriginalSat;
            }
        }
        return -1
    };

    WorkspaceController.prototype.deleteSentinel1a = function (value) {
        if (value) {
            this.getTrackSatellite();
        } else {
            for (var i = 0; i < this.m_aoSatellitePositions.length; i++) {
                this.m_oGlobeService.removeEntity(this.m_aoSatellitePositions[i]);
            }

            this.m_oGlobeService.removeEntity(this.m_oUfoPointer);
            this.m_oUfoPointer = null;
            this.m_oFakePosition = null;

            this.m_aoSatellitePositions = [];
        }

    };
    
    WorkspaceController.prototype.changeSorting = function(sColumn, sSortCmd) {

        if (this.m_oSort.sColumn === sColumn && sSortCmd === 'asc') {
            this.m_oSort.bDescending = false;
        } else if (this.m_oSort.sColumn === sColumn && sSortCmd === 'desc') {
            this.m_oSort.bDescending = true;
        } else {
            this.m_oSort.sColumn = sColumn
             if (this.m_oSort.sColumn === sColumn && sSortCmd === 'asc') {
            this.m_oSort.bDescending = false;
            } else if (this.m_oSort.sColumn === sColumn && sSortCmd === 'desc') {
                this.m_oSort.bDescending = true;
            }
        }
    };
    
    WorkspaceController.prototype.showArrow = function(sColumn, bDescending) {
        
        if (sColumn == this.m_oSort.sColumn) {
            if (bDescending == this.m_oSort.bDescending) {
                return true;
            }
        }
        return false;
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
        'OpportunitySearchService',
        '$interval',
        '$translate'
    ];
    return WorkspaceController;
})();
window.WorkspaceController = WorkspaceController;
