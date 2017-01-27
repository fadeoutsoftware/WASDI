/////**
//// * Created by a.corrado on 22/11/2016.
//// */
////
//angular.module('wasdi.GlobeDirective', [])
//    .directive('globe', function () {
//        "use strict";
//
//        function linkFunction(scope, element, attr){
//
//            //var sUrlGeoserver='http://localhost:8080/geoserver/ows';//geoserver
//            var oWMSOptions= { // wms option
//                transparent: true,
//                format: 'image/png',
//                crossOriginKeyword: null
//            };
//            var oGlobeOptions =
//            {
//                imageryProvider : Cesium.createOpenStreetMapImageryProvider(),
//                timeline: false,
//                animation: false,
//                baseLayerPicker:false,
//                fullscreenButton:false,
//                infoBox:false,
//                sceneModePicker:false,
//                selectionIndicator:false,
//                geocoder:false,
//                navigationHelpButton:false,
//
//            }
//            // default globe
//            var oViewer = new Cesium.Viewer('cesiumContainer',oGlobeOptions);
//
//            /**
//             * Add layer for Cesium Globe
//             * @param sLayerId
//             */
//            this.addLayerMap3D = function (sLayerId) {
//                var sUrlGeoserver = 'http://localhost:8080/geoserver/ows?';//TODO CHANGE IT
//                var oWMSOptions= { // wms options
//                    transparent: true,
//                    format: 'image/png',
//                    crossOriginKeyword: null
//                };
//                // WMS get GEOSERVER
//                var oProvider = new Cesium.WebMapServiceImageryProvider({
//                    url : sUrlGeoserver,
//                    layers:'wasdi:' + sLayerId,
//                    parameters : oWMSOptions,
//
//                });
//                oViewer.imageryLayers.addImageryProvider(oProvider);
//            }
//
//            //// WMS get GEOSERVER
//            //var oProvider = new Cesium.WebMapServiceImageryProvider({
//            //    url : sUrlGeoserver,
//            //    layers:'nurc:Img_Sample',
//            //    parameters : oWMSOptions,
//            //
//            //});
//            //oViewer.imageryLayers.addImageryProvider(oProvider);
//            //
//            //// rectangle for layer Bounding Boxes
//            //
//            //var redRectangle = oViewer.entities.add({
//            //    name : 'Red translucent rectangle with outline',
//            //    rectangle : {
//            //        coordinates : Cesium.Rectangle.fromDegrees(-130.85168, 20.7052, -62.0054, 54.1141),
//            //        material : Cesium.Color.RED.withAlpha(0.1),
//            //        outline : true,
//            //        outlineColor : Cesium.Color.RED
//            //    }
//            //})
//            //
//            //// zoom Bounding Boxes
//            //oViewer.zoomTo(oViewer.entities);
//        }
//
//        return{
//            restrict:"E",
//            template:'<div id="cesiumContainer" class="panel-body"></div>',
//            link: linkFunction
//        };
//    });