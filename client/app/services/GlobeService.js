/**
 * Created by a.corrado on 02/12/2016.
 */


'use strict';
angular.module('wasdi.GlobeService', ['wasdi.ConstantsService']).
service('GlobeService', ['$http',  'ConstantsService', function ($http, oConstantsService) {
    this.m_oWasdiGlobe=null;
    var oController = this;
    this.m_aoLayers=null;
    this.LONG_HOME = 0;
    this.LAT_HOME = 0;
    this.HEIGHT_HOME = 20000000; //zoom
    this.GLOBE_LAYER_ZOOM = 2000000;
    this.GLOBE_WORKSPACE_ZOOM = 4000000;

    this.m_aoLayers = [];
    this.oGlobeOptions =
    {
        //imageryProvider : Cesium.createOpenStreetMapImageryProvider(),
        timeline: false,
        animation: false,
        baseLayerPicker:true,
        fullscreenButton:false,
        infoBox:false,
        selectionIndicator:false,
        geocoder:false,
        navigationHelpButton:false,
        sceneModePicker:false,
        homeButton:false,
        scene3DOnly:true
    }

    this.initGlobe = function(sGlobeDiv)
    {

        if (window.WebGLRenderingContext)//check if browser supports WebGL
        {
            // browser supports WebGL
            try {
                var oGlobeOptions =
                    {
                        timeline: false,
                        animation: false,
                        baseLayerPicker:true,
                        fullscreenButton:false,
                        infoBox:true,
                        selectionIndicator:true,
                        geocoder:true,
                        navigationHelpButton:false,
                        sceneModePicker:false,
                        homeButton:false,
                        scene3DOnly:true
                    };

                Cesium.Ion.defaultAccessToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiIwODY1YzJlOS1lNGYxLTQ2OTQtOWNjMy01M2E2Y2EwMTQwYWYiLCJpZCI6MTIwODUsInNjb3BlcyI6WyJhc3IiLCJnYyJdLCJpYXQiOjE1NjAzMzMxNzJ9.p-wPCUj_m_NlYw2KsyW8jp7dRb_2EdS84cCWAirm_HI';
                this.m_oWasdiGlobe = new Cesium.Viewer(sGlobeDiv, oGlobeOptions);

                // Select OpenLayers and Cesium DEM Terrain by default
                this.m_oWasdiGlobe.baseLayerPicker.viewModel.selectedImagery  = this.m_oWasdiGlobe.baseLayerPicker.viewModel.imageryProviderViewModels[9];
                this.m_oWasdiGlobe.baseLayerPicker.viewModel.selectedTerrain = this.m_oWasdiGlobe.baseLayerPicker.viewModel.terrainProviderViewModels[1];

                this.m_aoLayers = this.m_oWasdiGlobe.imageryLayers;
            }
            catch(err) {
                console.log("Error in Cesium Globe: " + err);

            }
        }
        else
        {
            //TODO ERROR  browser doesn't support WebGL
            console.log("Error in Cesium Globe miss WebGl");
            utilsVexDialogAlertTop("ERROR IN CESIUM GLOBE MISS WEBGL<br>PLEASE UPADETE<br>LINK: HTTPS://GET.WEBGL.ORG/");
        }
    };


    //clear globe
    this.clearGlobe=function()
    {
        //Cesium.destroyObject(this.m_oWasdiGlobe);
        if(this.m_oWasdiGlobe)
        {
            this.m_oWasdiGlobe.destroy();
            this.m_oWasdiGlobe=null;
        }
    };

    /**
     * get globe
     */
    this.getGlobe = function()
    {
        return this.m_oWasdiGlobe;
    }

    /**
     * Get Workspace Zoom
     * @returns {number}
     */
    this.getWorkspaceZoom = function()
    {
        return this.GLOBE_WORKSPACE_ZOOM;
    }

    /**
     * Get Globe Layers
     * @returns {null|Array|*}
     */
    this.getGlobeLayers = function()
    {
        return this.m_aoLayers;
    }

    /**
     * Get Map Center
     * @returns {*[]}
     */
    this.getMapCenter=function() {
        //if(utilsIsObjectNullOrUndefined(this.m_oWasdiGlobe))
        //    return
        var windowPosition = new Cesium.Cartesian2(this.m_oWasdiGlobe.container.clientWidth / 2, this.m_oWasdiGlobe.container.clientHeight / 2);
        var pickRay = this.m_oWasdiGlobe.scene.camera.getPickRay(windowPosition);
        var pickPosition = this.m_oWasdiGlobe.scene.globe.pick(pickRay, this.m_oWasdiGlobe.scene);
        var pickPositionCartographic = this.m_oWasdiGlobe.scene.globe.ellipsoid.cartesianToCartographic(pickPosition);
        console.log(pickPositionCartographic.longitude * (180/Math.PI));
        console.log(pickPositionCartographic.latitude * (180/Math.PI));
        return [pickPositionCartographic.latitude * (180/Math.PI),pickPositionCartographic.longitude * (180/Math.PI)];
    }

    /**
     * Go Home
     */
    this.goHome = function()
    {
        this.goHome(this.HEIGHT_HOME);
    }

    this.goHome = function()
    {
        this.m_oWasdiGlobe.camera.setView({
            destination : Cesium.Cartesian3.fromDegrees(this.LONG_HOME, this.LAT_HOME, this.HEIGHT_HOME),
            orientation: {
                heading : 0.0,
                pitch : -Cesium.Math.PI_OVER_TWO,
                roll : 0.0
            }
        });
    };

    this.flyTo = function(long, lat, height)
    {
        this.m_oWasdiGlobe.camera.flyTo({
            destination : Cesium.Cartesian3.fromDegrees(long, lat, height),
            orientation: {
                heading : 0.0,
                pitch : -Cesium.Math.PI_OVER_TWO,
                roll : 0.0
            }
        });
    }

    this.flyHome = function()
    {
        this.m_oWasdiGlobe.camera.flyTo({
            destination : Cesium.Cartesian3.fromDegrees(this.LONG_HOME, this.LAT_HOME, this.HEIGHT_HOME),
            orientation: {
                heading : 0.0,
                pitch : -Cesium.Math.PI_OVER_TWO,
                roll : 0.0
            }
        });
    };

    /* ADD BOUNDING BOX */
    /**
     *
     * @param bbox
     * @returns {null}
     */
    this.addRectangleOnGlobeBoundingBox = function (bbox)
    {
        // Get the array representing the bounding box
        var aiInvertedArraySplit = this.fromBboxToRectangleArray(bbox);

        // Add the rectangle to the globe
        var oRectangle = this.addRectangleOnGlobeParamArray(aiInvertedArraySplit);


        if(utilsIsObjectNullOrUndefined(oRectangle)) return null;

        var redRectangle =   this.m_oWasdiGlobe.entities.add({
            name : 'Red translucent rectangle with outline',
            rectangle : {
                coordinates : Cesium.Rectangle.fromDegrees(oRectangle[0],oRectangle[1],oRectangle[2],oRectangle[3]),
                material : Cesium.Color.RED.withAlpha(0.2),
                outline : true,
                outlineColor : Cesium.Color.RED
            }
        });

        return redRectangle;
    };

    this.addRectangleOnGLobeByGeoserverBoundingBox = function(geoserverBoundingBox,oColor)
    {
        try {
            if (utilsIsObjectNullOrUndefined(geoserverBoundingBox)) {
                console.log("MapService.addRectangleByGeoserverBoundingBox: geoserverBoundingBox is null or empty");
                return;
            }
            if( (utilsIsObjectNullOrUndefined(oColor) === true))
            {
                oColor = Cesium.Color.RED;
            }
            geoserverBoundingBox = geoserverBoundingBox.replace(/\n/g,"");
            var oBoundingBox = JSON.parse(geoserverBoundingBox);
            // var bounds = [oBounds.maxy,oBounds.maxx,oBounds.miny,oBounds.minx];
            // var bounds = [oBounds.maxx,oBounds.maxy,oBounds.minx,oBounds.miny];
            var oRectangle =  Cesium.Rectangle.fromDegrees( oBoundingBox.minx, oBoundingBox.miny , oBoundingBox.maxx,oBoundingBox.maxy);

            // var oRectangle = this.addRectangleOnGlobeParamArray(bounds);

            // var bounds = [ [oBounds.maxy,oBounds.maxx],[oBounds.miny,oBounds.minx] ];
            // var oRectangle = L.rectangle(bounds, {color: sColor, weight: 2}).addTo(this.m_oWasdiMap);
            var oReturnRectangle = this.m_oWasdiGlobe.entities.add({
                name : 'Red translucent rectangle with outline',
                rectangle : {
                    coordinates : oRectangle,
                    material : oColor.withAlpha(0.3),
                    outline : true,
                    outlineColor : oColor
                }
            });
            return oReturnRectangle;
        }
        catch (e)
        {
            console.log(e);
        }
        return null;
    };
    /**
     * ADD RECTANGLE (PARAM ARRAY OF POINTS )
     * @param aoArray
     * @returns {boolean}
     */
    this.addRectangleOnGlobeParamArray = function (aArray)
    {
        // Safe Programming check
        if(utilsIsObjectNullOrUndefined(aArray) == true) return false;
        if(utilsIsObjectNullOrUndefined(this.m_oWasdiGlobe) == true) return false;

        try {

            // Create the new Polygon
            var oNewPolygon = {
                hierarchy : new Cesium.PolygonHierarchy(Cesium.Cartesian3.fromDegreesArray(aArray)),
                    outline : true,
                    outlineColor : Cesium.Color.RED.withAlpha(1),
                    outlineWidth : 10,
                    material : Cesium.Color.RED.withAlpha(0.2)
            }

            // Get the Globe Entities
            var aoEntities = this.m_oWasdiGlobe.entities.values;

            // Search if the same BBOX has already been added
            for (var i=0; i<aoEntities.length; i++) {

                // Get the entity
                var oEntity = aoEntities[i];

                // Check if it is a valid one (can be a satellite sprite also)
                if (utilsIsObjectNullOrUndefined(oEntity.polygon)) continue;

                // Assume is equal: we will set this = false if a point of the poly is different
                var bIsEqual = true;

                // For all the points of the poly already added to the globe
                for (var j=0; j<oEntity.polygon.hierarchy.getValue().positions.length; j++) {

                    // Safe programming: index < of size
                    if (j<oNewPolygon.hierarchy.positions.length) {
                        // The position is the same?
                        if (!oEntity.polygon.hierarchy.getValue().positions[j].equalsEpsilon(oNewPolygon.hierarchy.positions[j], 0.1)) {
                            // No! One point different => different poly. Try next one.
                            bIsEqual = false;
                            break;
                        }
                    }
                }

                if (bIsEqual) {

                    //If we found an equal bbox we can return this as Rectangle
                    return oEntity;
                }
            }

            // If we exit from the above cycle, there are no entities with the same poly, so add it.
            var oRectangle = this.m_oWasdiGlobe.entities.add({
                polygon : {
                    hierarchy : new Cesium.PolygonHierarchy(Cesium.Cartesian3.fromDegreesArray(aArray)),
                    outline : true,
                    outlineColor : Cesium.Color.RED.withAlpha(1),
                    outlineWidth : 10,
                    material : Cesium.Color.RED.withAlpha(0.2)
                }
            });

            return oRectangle;
        }
        catch(err) {
            console.log(err)
            return null;
        }

    };

    this.removeAllEntities = function ()
    {
        var oGlobe = this.m_oWasdiGlobe;
        oGlobe.entities.removeAll();
    };

    /*INIT ROTATE GLOBE*/
    this.initRotateGlobe = function(sGlobeDiv)
    {
        if (window.WebGLRenderingContext)//check if browser supports WebGL
        {
            // browser supports WebGL
            try {

                this.initGlobe(sGlobeDiv);

                //rotate globe
                this.m_oWasdiGlobe.camera.flyHome(0);
                this.startRotationGlobe(3);
                this.m_oWasdiGlobe.scene.preRender.addEventListener(this.icrf);
            }
            catch(err) {
                console.log("Error in Cesium Globe: " + err);

            }
        }
        else
        {
            //TODO ERROR  browser doesn't support WebGL
            console.log("Error in Cesium Globe miss WebGl");
            utilsVexDialogAlertTop("GURU MEDITATION<br>PLEASE UPDATE WEB GL<br>LINK: HTTPS://GET.WEBGL.ORG/");
        }
    };
    /*ROTATION GLOBE*/
    this.icrf = function(scene, time) {

        if (scene.mode !== Cesium.SceneMode.SCENE3D) {
            return;
        }
        var icrfToFixed = Cesium.Transforms.computeIcrfToFixedMatrix(time);
        if (Cesium.defined(icrfToFixed)) {

            var offset = Cesium.Cartesian3.clone(oController.m_oWasdiGlobe.camera.position);
            var transform = Cesium.Matrix4.fromRotationTranslation(icrfToFixed);
            oController.m_oWasdiGlobe.camera.lookAtTransform(transform, offset);
        }
    };

    /*Stop rotation*/
    this.stopRotationGlobe = function(){
        this.m_oWasdiGlobe.clock.multiplier = 0;
        this.m_oWasdiGlobe.clock.canAnimate = true;
        this.m_oWasdiGlobe.clock.shouldAnimate = true;
    };

    /*Start rotation*/
    this.startRotationGlobe = function(iRotationValue){
        if(utilsIsANumber(iRotationValue) === false)
            return false;
        this.m_oWasdiGlobe.clock.canAnimate = true;
        this.m_oWasdiGlobe.clock.shouldAnimate = true;
        this.m_oWasdiGlobe.clock.multiplier = iRotationValue * 600 ;
        return true;
    };

    /**
     * drawOutLined
     * @param aPositions
     * @param sName
     * @param sColor
     */
    this.drawOutLined = function(aPositions,sColor,sName){

        if(utilsIsObjectNullOrUndefined(aPositions) === true )
            return null;
        if(utilsIsStrNullOrEmpty(sName)=== true)
            sName="";
        if(utilsIsObjectNullOrUndefined(sColor) === true )
            sColor = Cesium.Color.ORANGE;
        var oOutLined = this.m_oWasdiGlobe.entities.add({
            name : sName,
            polyline : {
                positions : Cesium.Cartesian3.fromDegreesArrayHeights(aPositions),
                width : 5,
                material : new Cesium.PolylineOutlineMaterialProperty({
                    color : sColor,
                    outlineWidth : 2,
                    outlineColor : Cesium.Color.BLACK
                })
            }
        });

        return oOutLined;

    };

    /***
     * drawGlowingLine
     * @param aPositions
     * @param sColor
     * @param sName
     * @returns {null}
     */
    this.drawGlowingLine = function(aPositions,sColor,sName){
        if(utilsIsObjectNullOrUndefined(aPositions) === true )
            return null;
        if(utilsIsStrNullOrEmpty(sName)=== true)
            sName="";
        if(utilsIsObjectNullOrUndefined(sColor) )
            sColor = Cesium.Color.ORANGE;

        var oGlowingLine = this.m_oWasdiGlobe.entities.add({
            name : sName,
            polyline : {
                positions : Cesium.Cartesian3.fromDegreesArrayHeights(aPositions),
                width : 10,
                material : new Cesium.PolylineGlowMaterialProperty({
                    glowPower : 0.2,
                    color : sColor
                })
            }
        });


        return oGlowingLine;

    };

    /**
     * drawPointWithImage
     * @param aPositionInput
     * @param sImageInput
     * @returns {null}
     */
    this.drawPointWithImage = function(aPositionInput,sImageInput,sName,sDescription, iWidth, iHeight)
    {
        if(utilsIsObjectNullOrUndefined(aPositionInput) === true) return null;
        if(utilsIsStrNullOrEmpty(sImageInput) === true ) return null;
        if(utilsIsStrNullOrEmpty(sName) === true) return false;
        if(utilsIsStrNullOrEmpty(sDescription) === true) return false;

        if (utilsIsObjectNullOrUndefined(iWidth)) iWidth = 64;
        if (utilsIsObjectNullOrUndefined(iHeight)) iHeight = 64;

        var oPoint =  this.m_oWasdiGlobe.entities.add({
            name : sName,
            position : Cesium.Cartesian3.fromDegrees(aPositionInput[0],aPositionInput[1],aPositionInput[2]),
            billboard : {
                image : sImageInput,
                width : iWidth,
                height : iHeight
            } , label : {
                text : sDescription,
                font : '14pt monospace',
                style: Cesium.LabelStyle.FILL_AND_OUTLINE,
                fillColor: Cesium.Color.CHARTREUSE,
                outlineWidth : 2,
                verticalOrigin : Cesium.VerticalOrigin.BOTTOM,
                pixelOffset : new Cesium.Cartesian2(0, -9)
            }
        });

        return oPoint;
    };

    /**
     * removeEntity
     * @param oEntity
     * @returns {boolean}
     */
    this.removeEntity = function (oEntity)
    {
        if(utilsIsObjectNullOrUndefined(oEntity) === true)
            return false;
        var oGlobe = this.m_oWasdiGlobe;
        oGlobe.entities.remove(oEntity);
        return true;
    };


    /**
     * Convert the string with bbox expressed like PointX,PointY,PointX,PointY,... in a rectangle array to use as globe bounding box
     * @param bbox
     * @returns {*}
     */
    this.fromBboxToRectangleArray = function (bbox) {

        // skip if there isn't the product bounding box
        if(utilsIsObjectNullOrUndefined(bbox) === true ) return null;

        var aiInvertedArraySplit = [];
        var  aoArraySplit;

        // Split bbox string
        aoArraySplit = bbox.split(",");

        var iArraySplitLength = aoArraySplit.length;

        if(iArraySplitLength < 10) return null;

        for(var iIndex = 0; iIndex < iArraySplitLength-1; iIndex = iIndex + 2){
            aiInvertedArraySplit.push(aoArraySplit[iIndex+1]);
            aiInvertedArraySplit.push(aoArraySplit[iIndex]);
        }

        return aiInvertedArraySplit;
    }

    this.updateEntityPosition = function(oEntity,oNewPosition){
        if( (utilsIsObjectNullOrUndefined(oEntity) === false) && (utilsIsObjectNullOrUndefined(oNewPosition) === false ) )
        {
            //oEntity.go(oNewPosition);
            oEntity.position = oNewPosition;
        }
    };

    this.getSatelliteTrackInputList = function () {
        var aoOutList = [
            {
                name : "SENTINEL1A",
                icon : "assets/icons/sat_01.svg",
                label : "S1A",
                description : "ESA Sentinel 1 A "
            },
            {
                name : "SENTINEL1B",
                icon : "assets/icons/sat_01.svg",
                label : "S1B",
                description : "ESA Sentinel 1 B"
            },
            {
                name : "COSMOSKY1",
                icon : "assets/icons/sat_02.svg",
                label : "CSK1",
                description : "ASI COSMO-SKYMED 1"
            },
            {
                name : "COSMOSKY2",
                icon : "assets/icons/sat_02.svg",
                label : "CSK2",
                description : "ASI COSMO-SKYMED 2"
            },
            {
                name : "COSMOSKY3",
                icon : "assets/icons/sat_02.svg",
                label : "CSK3",
                description : "ASI COSMO-SKYMED 3"
            },
            {
                name : "COSMOSKY4",
                icon : "assets/icons/sat_02.svg",
                label : "CSK4",
                description : "ASI COSMO-SKYMED 4"
            },
            {
                name : "LANDSAT8",
                icon : "assets/icons/sat_04.svg",
                label : "LS8",
                description : "NASA LANDSAT 8"
            },
            {
                name : "PROBAV",
                icon : "assets/icons/sat_05.svg",
                label : "PROBA-V",
                description : "PROBA VEGETATION"
            },
            {
                name : "GEOEYE",
                icon : "assets/icons/sat_06.svg",
                label : "GEOEYE",
                description : "GeoEye - Digital Globe"
            },
            {
                name : "WORLDVIEW2",
                icon : "assets/icons/sat_07.svg",
                label : "WORLDVIEW2",
                description : "WorldView - Digital Globe"
            }
        ];

        return aoOutList;
    };


    /********************************************ZOOM FUNCTIONS**********************************************/


    /**
     * Fly to Workspace Global Bounding Box.
     * Takes in input the list of Products of the Workspace.
     * bCreateRectangle can be undefined, null, true or false: it is not false the method will add
     * a bounding box rectangle for each product if it is still not on the globe.
     * In bCreateRectangle is false no rectangle will be added
     * @param aoProducts List of the workspace products
     * @returns {boolean}
     */
    this.flyToWorkspaceBoundingBox = function (aoProducts) {
        try {
            var aoArraySplit = [];
            var aoTotalArray = [];

            // Check we have products
            if(utilsIsObjectNullOrUndefined(aoProducts) === true) return false;

            var iProductsLength = aoProducts.length;

            // For each product
            for(var iIndexProduct = 0; iIndexProduct < iProductsLength; iIndexProduct++){
                // Split bbox string
                aoArraySplit = aoProducts[iIndexProduct].bbox.split(",");

                var iArraySplitLength = aoArraySplit.length;
                if(iArraySplitLength < 10) continue;

                var bHasNan = false;
                for (var iValues = 0; iValues< aoArraySplit.length; iValues ++) {
                    if (isNaN(aoArraySplit[iValues])) {
                        bHasNan = true;
                        break;
                    }
                }

                if (bHasNan) continue;

                aoTotalArray.push.apply(aoTotalArray,aoArraySplit);
            }

            var aoBounds = [];
            for (var iIndex = 0; iIndex < aoTotalArray.length - 1; iIndex = iIndex + 2) {
                aoBounds.push(new Cesium.Cartographic.fromDegrees(aoTotalArray[iIndex + 1], aoTotalArray[iIndex ]));
            }

            var oWSRectangle = Cesium.Rectangle.fromCartographicArray(aoBounds);
            var oWSCenter = Cesium.Rectangle.center(oWSRectangle);

            //oGlobe.camera.setView({
            this.getGlobe().camera.flyTo({
                destination : Cesium.Cartesian3.fromRadians(oWSCenter.longitude, oWSCenter.latitude, this.GLOBE_WORKSPACE_ZOOM),
                orientation: {
                    heading: 0.0,
                    pitch: -Cesium.Math.PI_OVER_TWO,
                    roll: 0.0
                }
            });

            this.stopRotationGlobe();
        }
        catch (e) {
            console.log(e);
        }

    };

    this.addAllWorkspaceRectanglesOnMap = function (aoProducts) {
        try {

            var oRectangle = null;
            var aoArraySplit = [];
            var aiInvertedArraySplit = [];

            var aoTotalArray = [];

            // Check we have products
            if(utilsIsObjectNullOrUndefined(aoProducts) === true) return false;

            // Clear the previous footprints
            this.removeAllEntities();

            var iProductsLength = aoProducts.length;

            // For each product
            for(var iIndexProduct = 0; iIndexProduct < iProductsLength; iIndexProduct++){

                // Split bbox string
                aoArraySplit = aoProducts[iIndexProduct].bbox.split(",");
                var iArraySplitLength = aoArraySplit.length;
                if(iArraySplitLength < 10) continue;

                var bHasNan = false;
                for (var iValues = 0; iValues< aoArraySplit.length; iValues ++) {
                    if (isNaN(aoArraySplit[iValues])) {
                        bHasNan = true;
                        break;
                    }
                }

                if (bHasNan) continue;

                aoTotalArray.push.apply(aoTotalArray,aoArraySplit);

                // Get the array representing the bounding box
                aiInvertedArraySplit = this.fromBboxToRectangleArray(aoProducts[iIndexProduct].bbox);
                // Add the rectangle to the globe
                oRectangle = this.addRectangleOnGlobeParamArray(aiInvertedArraySplit);
                aoProducts[iIndexProduct].oRectangle = oRectangle;
                aoProducts[iIndexProduct].aBounds = aiInvertedArraySplit;
            }
        }
        catch (e) {
            console.log(e);
        }
    };


    /**
     * ZOOM ON LAYER BY POINTS
     * @param oArray
     * @returns {boolean}
     */
    this.zoomOnLayerParamArray = function(aArray) {
        try {
            // Check input data
            if(utilsIsObjectNullOrUndefined(aArray) == true) return false;
            if(utilsIsObjectNullOrUndefined(this.m_oWasdiGlobe) == true) return false;

            // create a new points array
            var newArray = [];
            for(var iIndex = 0; iIndex < aArray.length - 1; iIndex += 2 )
            {
                newArray.push(new Cesium.Cartographic.fromDegrees(aArray[iIndex+1],aArray[iIndex]));
            }

            // Get a rectangle from the array
            var oZoom = Cesium.Rectangle.fromCartographicArray(newArray);
            var oWSCenter = Cesium.Rectangle.center(oZoom);

            // Fly there
            this.m_oWasdiGlobe.camera.flyTo({
                destination: Cesium.Cartesian3.fromRadians(oWSCenter.latitude, oWSCenter.longitude, this.GLOBE_LAYER_ZOOM),
                orientation: {
                    heading: 0.0,
                    pitch: -Cesium.Math.PI_OVER_TWO,
                    roll: 0.0
                }

            });
        }
        catch (e) {
            console.log(e);
        }
    };

    /**
     * Zoom on layer with the geoserver bounding box
     * @param geoserverBoundingBox
     * @returns {boolean}
     */
    this.zoomBandImageOnGeoserverBoundingBox = function(geoserverBoundingBox) {
        try {
            // Check the input
            if (utilsIsObjectNullOrUndefined(geoserverBoundingBox)) {
                console.log("GlobeService.zoomBandImageOnGeoserverBoundingBox: geoserverBoundingBox is null");
                return false;
            }

            // Parse the bounding box
            geoserverBoundingBox = geoserverBoundingBox.replace(/\n/g,"");
            var oBoundingBox = JSON.parse(geoserverBoundingBox);
            if(utilsIsObjectNullOrUndefined(oBoundingBox)) {
                console.log("GlobeService.zoomBandImageOnGeoserverBoundingBox: parsing bouning box is null");
                return false;
            }

            // Get the Globe
            var oGlobe = this.m_oWasdiGlobe;
            if(utilsIsObjectNullOrUndefined(oGlobe)) {
                console.log("GlobeService.zoomBandImageOnGeoserverBoundingBox: globe is null");
                return false;
            }


            var oRectangle =  Cesium.Rectangle.fromDegrees( oBoundingBox.minx, oBoundingBox.miny , oBoundingBox.maxx,oBoundingBox.maxy);
            var oCenter = Cesium.Rectangle.center(oRectangle);

            /* set view of globe*/
            oGlobe.camera.flyTo({
                destination: Cesium.Cartesian3.fromRadians(oCenter.longitude, oCenter.latitude, this.GLOBE_LAYER_ZOOM),
                orientation: {
                    heading: 0.0,
                    pitch: -Cesium.Math.PI_OVER_TWO,
                    roll: 0.0
                }

            });

            return true;
        }
        catch (e) {
            console.log(e);
        }
    };


    /**
     * Zoom on a Band Image starting from the bbox property (points, not the geoserver formatted one)
     * @param bbox
     */
    this.zoomBandImageOnBBOX = function (bbox) {
        try {
            if (utilsIsObjectNullOrUndefined(bbox)) {
                console.log("GlobeService.zoomBandImageOnBBOX: invalid bbox ");
                return;
            }

            if (utilsIsStrNullOrEmpty(bbox)) {
                console.log("GlobeService.zoomBandImageOnBBOX: invalid bbox ");
                return;
            }

            var aiRectangle = this.fromBboxToRectangleArray(bbox);

            if (utilsIsObjectNullOrUndefined(aiRectangle) == false) {
                this.zoomOnLayerParamArray(aiRectangle);
            }
        }
        catch (e) {
            console.log(e);
        }
    };


    /**
     * Zoom on an External Layer
     * @param oLayer
     * @returns {boolean}
     */
    this.zoomOnExternalLayer = function (oLayer) {
        try {
            if (utilsIsObjectNullOrUndefined(oLayer) == true) return false;

            var oBoundingBox = (oLayer.BoundingBox[0].extent);

            if(utilsIsObjectNullOrUndefined(oBoundingBox)== true) return false;

            var oGlobe = this.m_oGlobeService.getGlobe();
            /* set view of globe*/
            oGlobe.camera.flyTo({
                destination:  Cesium.Rectangle.fromDegrees(oBoundingBox[0], oBoundingBox[1], oBoundingBox[2], oBoundingBox[3]),
                orientation: {
                    heading: 0.0,
                    pitch: -Cesium.Math.PI_OVER_TWO,
                    roll: 0.0
                }
            });
        }
        catch (e) {
            console.log(e);
        }
    }

}]);

