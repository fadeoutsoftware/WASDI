/**
 * Created by a.corrado on 02/12/2016.
 */


'use strict';
angular.module('wasdi.GlobeService', ['wasdi.ConstantsService']).
service('GlobeService', ['$http',  'ConstantsService', function ($http, oConstantsService) {
    this.m_oWasdiGlobe=null;
    this.m_aoLayers=null;
    this.LONG_HOME = 0;
    this.LAT_HOME = 0;
    this.HEIGHT_HOME = 15000000//zoom
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
            this.m_oWasdiGlobe = new Cesium.Viewer(sGlobeDiv, this.oGlobeOptions);

            this.m_aoLayers = this.m_oWasdiGlobe.imageryLayers;
        }
        else
        {
            //TODO ERROR  browser doesn't support WebGL
            console.log("Error in initGlobe miss WebGl");
        }
    }
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
        this.m_oWasdiGlobe.camera.setView({
            destination : Cesium.Cartesian3.fromDegrees(this.LONG_HOME, this.LAT_HOME, this.HEIGHT_HOME),
            orientation: {
                heading : 0.0,
                pitch : -Cesium.Math.PI_OVER_TWO,
                roll : 0.0
            }
        });
    }

    /* ADD BOUNDING BOX */
    this.addRectangleOnGlobeBoundingBox = function (oRectangle)
    {
        if(utilsIsObjectNullOrUndefined(oRectangle))
            return null;

        var redRectangle =   this.m_oWasdiGlobe.entities.add({
            name : 'Red translucent rectangle with outline',
            rectangle : {
                coordinates : Cesium.Rectangle.fromDegrees(oRectangle[0],oRectangle[1],oRectangle[2],oRectangle[3]),
                material : Cesium.Color.RED.withAlpha(0.7),
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

        if(utilsIsObjectNullOrUndefined(oBoundingBox) == true)
            return false;

        var oGlobe = this.m_oWasdiGlobe;
        if(utilsIsObjectNullOrUndefined(oGlobe) == true)
            return false;

        /* set view of globe*/
        oGlobe.camera.setView({
            destination:  Cesium.Rectangle.fromDegrees( oArray[0], oArray[1] , oArray[2],oArray[3]),
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
        if(utilsIsObjectNullOrUndefined(aArray) == true)
            return false;

        var oGlobe = this.m_oWasdiGlobe;
        if(utilsIsObjectNullOrUndefined(oGlobe) == true)
            return false;

        //var stripeMaterial = new Cesium.StripeMaterialProperty({
        //    evenColor : Cesium.Color.WHITE.withAlpha(0.5),
        //    oddColor : Cesium.Color.BLUE.withAlpha(0.5),
        //    repeat : 5.0
        //});

        //var oShape = Cesium.Rectangle.fromCartographicArray(aoArray);
        //var oShape = Cesium.Cartesian3.fromDegreesArray(aoArray);
        //var aoNewArray = [];
        //for(var iIndex = 0; iIndex < aoArray.length; iIndex++)
        //{
        //    aoNewArray.push(aoArray[iIndex].longitude);
        //    aoNewArray.push(aoArray[iIndex].latitude);
        //
        //}

        var oRectangle = oGlobe.entities.add({
            polygon : {
                hierarchy : new Cesium.PolygonHierarchy(Cesium.Cartesian3.fromDegreesArray(aArray)),
                outline : true,
                outlineColor : Cesium.Color.WHITE,
                outlineWidth : 4,
                material : Cesium.Color.RED.withAlpha(0.7),
            }
        });

        return oRectangle;
    }

}]);

