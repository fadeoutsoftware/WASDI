/**
 * Created by a.corrado on 02/12/2016.
 */


'use strict';
angular.module('wasdi.GlobeService', ['wasdi.ConstantsService']).
service('GlobeService', ['$http',  'ConstantsService','SatelliteService', function ($http, oConstantsService) {
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
        imageryProvider : Cesium.createOpenStreetMapImageryProvider(),
        timeline: false,
        animation: false,
        baseLayerPicker:false,
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
            // default globe
            try {
                this.m_oWasdiGlobe = new Cesium.Viewer(sGlobeDiv, this.oGlobeOptions);
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
    }
    // get globe
    this.getGlobe = function()
    {
        return this.m_oWasdiGlobe;
    }

    // get globe
    this.getWorkspaceZoom = function()
    {
        return this.GLOBE_WORKSPACE_ZOOM;
    }

    this.getGlobeLayers = function()
    {
        return this.m_aoLayers;
    }

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
    this.addRectangleOnGlobeBoundingBox = function (oRectangle)
    {
        if(utilsIsObjectNullOrUndefined(oRectangle))
            return null;

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
    }

    /* ZOOM ON LAYER WITH BOUNDING BOX */
    this.zoomOnLayerBoundingBox = function(oArray)
    {
        var oBoundingBox = oArray;
        if(utilsIsObjectNullOrUndefined(oBoundingBox) == true) return false;

        var oGlobe = this.m_oWasdiGlobe;
        if(utilsIsObjectNullOrUndefined(oGlobe) == true) return false;


        var oRectangle =  Cesium.Rectangle.fromDegrees( oArray[0], oArray[1] , oArray[2],oArray[3]);
        var oCenter = Cesium.Rectangle.center(oRectangle);

        /* set view of globe*/
        oGlobe.camera.setView({
            destination: Cesium.Cartesian3.fromRadians(oCenter.longitude, oCenter.latitude, this.GLOBE_LAYER_ZOOM),
            orientation: {
                heading: 0.0,
                pitch: -Cesium.Math.PI_OVER_TWO,
                roll: 0.0
            }

        });

        return true;
    }
    /**
     * ZOOM ON LAYER BY POINTS
     * @param oArray
     * @returns {boolean}
     */
    this.zoomOnLayerParamArray = function(aArray)
    {
        if(utilsIsObjectNullOrUndefined(aArray) == true)
            return false;

        var oGlobe = this.m_oWasdiGlobe;
        if(utilsIsObjectNullOrUndefined(oGlobe) == true)
            return false;

        var newArray = [];
        for(var iIndex = 0; iIndex < aArray.length - 1; iIndex += 2 )
        {
            newArray.push(new Cesium.Cartographic(aArray[iIndex+1],aArray[iIndex]));
        }

        var oZoom = Cesium.Rectangle.fromCartographicArray(newArray);
        //var oZoom =  new Cesium.PolygonHierarchy(Cesium.Cartesian3.fromDegreesArray(aArray));
        //var oZoom =  new Cesium.Cartesian3.fromDegreesArray(aArray);

        oGlobe.camera.setView({
            destination: oZoom,
            orientation: {
                heading: 0.0,
                pitch: -Cesium.Math.PI_OVER_TWO,
                roll: 0.0
            }

        });
    }
    /**
     * ADD RECTANGLE (PARAM ARRAY OF POINTS )
     * @param aoArray
     * @returns {boolean}
     */
    this.addRectangleOnGlobeParamArray = function (aArray)
    {

        if(utilsIsObjectNullOrUndefined(aArray) == true) return false;
        if(utilsIsObjectNullOrUndefined(this.m_oWasdiGlobe) == true) return false;

        var oRectangle = this.m_oWasdiGlobe.entities.add({
            polygon : {
                hierarchy : new Cesium.PolygonHierarchy(Cesium.Cartesian3.fromDegreesArray(aArray)),
                outline : true,
                 outlineColor : Cesium.Color.RED.withAlpha(1),
                outlineWidth : 10,
                material : Cesium.Color.RED.withAlpha(0.2),
            }
        });

        return oRectangle;
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

            // default globe
            try {
                var oGlobeOptions =
                    {
                        imageryProvider : Cesium.createOpenStreetMapImageryProvider(),
                        timeline: false,
                        animation: false,
                        baseLayerPicker:false,
                        fullscreenButton:false,
                        infoBox:true,
                        selectionIndicator:true,
                        geocoder:false,
                        navigationHelpButton:false,
                        sceneModePicker:false,
                        homeButton:false,
                        scene3DOnly:true
                    }
                this.m_oWasdiGlobe = new Cesium.Viewer(sGlobeDiv, oGlobeOptions);
                this.m_aoLayers = this.m_oWasdiGlobe.imageryLayers;

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
            utilsVexDialogAlertTop("ERROR IN CESIUM GLOBE MISS WEBGL<br>PLEASE UPADETE<br>LINK: HTTPS://GET.WEBGL.ORG/");
        }
    };
    /*ROTATION GLOBE*/
    this.icrf = function(scene, time) {

        if (scene.mode !== Cesium.SceneMode.SCENE3D) {
            return;
        }
        var icrfToFixed = Cesium.Transforms.computeIcrfToFixedMatrix(time);
        if (Cesium.defined(icrfToFixed)) {

            var camera =  oController.m_oWasdiGlobe.camera;
            var offset = Cesium.Cartesian3.clone(camera.position);
            var transform = Cesium.Matrix4.fromRotationTranslation(icrfToFixed);
            camera.lookAtTransform(transform, offset);
        }
    };

    /*Stop rotation*/
    this.stopRotationGlobe = function(){
        this.m_oWasdiGlobe.clock.multiplier = 0;
    };

    /*Start rotation*/
    this.startRotationGlobe = function(iRotationValue){
        if(utilsIsANumber(iRotationValue) === false)
            return false;
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
     * Fly to Workspace Global Bounding Box
     * @param m_aoProducts
     * @returns {boolean}
     */
    this.flyToWorkspaceBoundingBox = function (m_aoProducts) {

        var oRectangle = null;
        var aArraySplit = [];
        var iArraySplitLength = 0;
        var iInvertedArraySplit = [];

        var aoTotalArray = [];

        // Check we have products
        if(utilsIsObjectNullOrUndefined(m_aoProducts) === true) return false;

        var iProductsLength = m_aoProducts.length;

        // For each product
        for(var iIndexProduct = 0; iIndexProduct < iProductsLength; iIndexProduct++){
            iInvertedArraySplit = [];
            aArraySplit = [];
            // skip if there isn't the product bounding box
            if(utilsIsObjectNullOrUndefined(m_aoProducts[iIndexProduct].bbox) === true ) continue;

            // Split bbox string
            aArraySplit = m_aoProducts[iIndexProduct].bbox.split(",");
            aoTotalArray.push.apply(aoTotalArray,aArraySplit);
            iArraySplitLength = aArraySplit.length;

            if(iArraySplitLength !== 10) continue;

            for(var iIndex = 0; iIndex < iArraySplitLength-1; iIndex = iIndex + 2){
                iInvertedArraySplit.push(aArraySplit[iIndex+1]);
                iInvertedArraySplit.push(aArraySplit[iIndex]);
            }

            oRectangle = this.addRectangleOnGlobeParamArray(iInvertedArraySplit);
            m_aoProducts[iIndexProduct].oRectangle = oRectangle;
            m_aoProducts[iIndexProduct].aBounds = iInvertedArraySplit;
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
                icon : "assets/icons/sat_03.svg",
                label : "LS8",
                description : "NASA LANDSAT 8"
            }
        ]

        return aoOutList;
    }

}]);

