/**
 * Created by a.corrado on 02/12/2016.
 */


'use strict';
angular.module('wasdi.GlobeService', ['wasdi.ConstantsService']).
service('GlobeService', ['$http',  'ConstantsService', function ($http, oConstantsService) {
    this.m_oWasdiGlobe=null;
    this.m_aoLayers=null;

    this.oGlobeOptions =
    {
        imageryProvider : Cesium.createOpenStreetMapImageryProvider(),
        timeline: false,
        animation: false,
        baseLayerPicker:false,
        fullscreenButton:false,
        infoBox:false,
        sceneModePicker:false,
        selectionIndicator:false,
        geocoder:false,
        navigationHelpButton:false,

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
            //TODO ERROR  browser dosen't supports WebGL
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

}]);

