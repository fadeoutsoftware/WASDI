angular.module('wasdi.wapSelectArea', [])
    .directive('wapselectarea', ['MapService', 'ModalService', function ($MapService, $ModalService) {
        "use strict";
        return {
            restrict: 'E',
            scope: {},

            // * Text binding ('@' or '@?') *
            // * One-way binding ('<' or '<?') *
            // * Two-way binding ('=' or '=?') *
            // * Function binding ('&' or '&?') *
            bindToController: {
                heightMap: '=',
                widthMap: '=',
                boundingBox: '=',
                maxarea:'=',
                maxside:'=',
                maxratioside:'=',
                tooltip:'='


            },
            // Tooltips alternatives : 
            // uib-tooltip="I'm a tooltip!" alternative using bootstrap-UI
            // title="My Tooltip!" data-toggle="tooltip" data-placement="top" tooltip
            // the first on relies on a library, UI-boostrap, the second one relies on a directive 
            template: `<div class="map-container" ng-attr-id="{{$ctrl.mapId}}" ng-style="$ctrl.oMapStyle" uib-tooltip="{{$ctrl.tooltip}}" tooltip-placement="right"></div>`,

            controller: function() {
                
                

                this.getDistance = function(pointFrom, pointTo ){
                    let markerFrom = L.circleMarker(pointFrom,{ color: "#4AFF00", radius: 10 });
                    let markerTo =  L.circleMarker(pointTo,{ color: "#4aff00", radius: 10 });

                    let from = markerFrom.getLatLng();
                    let to = markerTo.getLatLng();

                    let distance = (from.distanceTo(to)).toFixed(0)/1000; // distance in km

                    return distance;
            }

                this.$onInit = function () {

                    // passing values as globals cause bindToController is deprecated and
                    // for some reasons, doesn't bind new values
                    // this.maxArea = window.maxArea;
                    // this.maxSide = window.maxSide;
                    // this.maxRatioSide = window.maxRatioSide;

                    console.log(this); // logs your item object
                };

         
                // generated a new id map number and converted as string
                this.mapId = "" + Date.now() + Math.random();

                this.oMap = null;
                this.m_oDrawnItems = {};

                //CHECK IF Height or width are null or undefined
                if(utilsIsANumber(this.heightMap ) === false ){
                    this.heightMap = 0;
                    console.error('height-map parameter is not a number');
                }
                if(utilsIsANumber(this.widthMap ) === false ){
                    this.widthMap = 0;
                    console.error('width-map parameter is not a number');
                }

                this.oMapStyle = { height: this.heightMap + 'px',
                    width: this.widthMap + 'px'  };

                    

                this.addBoundingBoxDrawerOnMap = function (oMap) {

                    if(oMap === null || oMap === undefined ){
                        return null;
                    }

                    this.m_oDrawnItems = new L.FeatureGroup();

                    oMap.addLayer(this.m_oDrawnItems);

                    var oOptions={
                        position:'topright',//position of menu
                        draw:{// what kind of shapes are disable/enable
                            marker:false,
                            polyline:false,
                            circle:false,
                            circlemarker:false,
                            polygon:false
                        },

                        edit: {
                            featureGroup: this.m_oDrawnItems,//draw items are the "voice" of menu
                            edit: false,// hide edit button
                            remove: false// hide remove button
                        }
                    };

                    var oDrawControl = new L.Control.Draw(oOptions);

                    oMap.addControl(oDrawControl);

                    //Without this.m_oWasdiMap.on() the shape isn't saved on map
                    let oController = this;
                    oMap.on(L.Draw.Event.CREATED, function (event)
                    {
                        var layer = event.layer;
                        oController.boundingBox.northEast = layer._bounds._northEast;
                        oController.boundingBox.southWest = layer._bounds._southWest;

                        //remove old shape
                        if(oController.m_oDrawnItems && oController.m_oDrawnItems.getLayers().length!==0){
                            oController.m_oDrawnItems.clearLayers();
                        }
                        //save new shape in map
                        oController.m_oDrawnItems.addLayer(layer);

                        // aproximate method to calculate area
                        let M_number_lat = 110.574; // chilometers
                        let M_number_lng = 111.320; // chilometers
                        let latN = oController.boundingBox.northEast.lat; // in degrees -> convert to radians in calc
                        let lngE = oController.boundingBox.northEast.lng;

                        let latS = oController.boundingBox.southWest.lat;
                        let lngW = oController.boundingBox.southWest.lng;

                        let area = ((latN-latS) * M_number_lat) *
                            ((lngE-lngW) *
                                (Math.cos(latN * (Math.PI/180)) * M_number_lng)*100);


                        //var latlngs = [[19.04469, 72.9258], [19.04469, 72.9268], [19.04369, 72.9268],[19.04369, 72.9258]];
                        var latlngs = layer.getLatLngs();
                        //var polygon = L.polygon(latlngs, {color: 'red'}).addTo(oMap);

                        var markerFrom = L.circleMarker(latlngs[0][0],{ color: "#4AFF00", radius: 10 });
                        var markerTo =  L.circleMarker(latlngs[0][1],{ color: "#4aff00", radius: 10 });

                        var from = markerFrom.getLatLng();
                        var to = markerTo.getLatLng();

                        var distance = (from.distanceTo(to)).toFixed(0)/1000; // distance in km

                        var distanceFunction = this.getDistance(latlngs[0][0],latlngs[0][1]);

                        if (distance== distanceFunction) console.log("distances are equals!");


                        let areaFromLeaflet = L.GeometryUtil.geodesicArea(layer.getLatLngs()[0]); // first element is the array itself to be passed
                        let squarekm = areaFromLeaflet / 1000000;

                        console.log(squarekm);
                        console.log(area);



                    });

                    

                    oMap.on(L.Draw.Event.DELETESTOP, function (event) {
                        // var layer = event.layers;
                    });

                    //
                    let oModalService = $ModalService;

                    L.control.custom({
                        position: 'topright',
                        content : '<div type="button" class="import-insert-bbox-button" title="Insert a bbox by text">'+
                            '    <i class="import-insert-bbox-icon fa fa-edit" ></i>'+
                            '</div>',
                        classes : 'import-insert-bbox-wrapper-button btn-group-vertical btn-group-sm',
                        style   :
                            {
                            },
                        events:
                            {
                                click: function(data)
                                {
                                    oModalService.showModal({
                                        templateUrl: "dialogs/manual_insert_bbox/ManualInsertBboxView.html",
                                        controller: "ManualInsertBboxController",
                                        inputs: {
                                            extras: {}
                                        }
                                    }).then(function (modal) {
                                        modal.element.modal();
                                        modal.close.then(function (oResult) {

                                            if (oResult==null) return;

                                            let fNorth = parseFloat(oResult.north);
                                            let fSouth = parseFloat(oResult.south);
                                            let fEast = parseFloat(oResult.east);
                                            let fWest = parseFloat(oResult.west);

                                            if (isNaN(fNorth) || isNaN(fSouth) || isNaN(fEast) || isNaN(fWest)) {
                                                return;
                                            }

                                            var aoBounds = [[fNorth, fWest], [fSouth, fEast]];
                                            // checks size here.
                                            var oLayer = L.rectangle(aoBounds, {color: "#3388ff", weight: 1});


                                            oController.boundingBox.northEast = oLayer._bounds._northEast;
                                            oController.boundingBox.southWest = oLayer._bounds._southWest;

                                            //remove old shape
                                            if(oController.m_oDrawnItems && oController.m_oDrawnItems.getLayers().length!==0){
                                                oController.m_oDrawnItems.clearLayers();
                                            }

                                            //save new shape in map
                                            oController.m_oDrawnItems.addLayer(oLayer);
                                        });
                                    });
                                }
                            }
                    }).addTo(oMap);


                    var oGeocoder = L.Control.Geocoder.nominatim();

                    L.Control.geocoder({
                        geocoder: oGeocoder,
                        position:'topleft'
                    }).addTo(oMap);

                    return oMap;
                }



                let oThat = this;
                //init the map after the directive is loaded
                setTimeout(function() {
                    oThat.oMap = $MapService.initMapSingleton(oThat.mapId);

                    //is it an option?
                    oThat.oMap = oThat.addBoundingBoxDrawerOnMap(oThat.oMap);
                }, 500);

            },
            controllerAs: '$ctrl'
        };
    }]);


