 var eDriftFloodEventDialogController = (function() {

    function eDriftFloodEventDialogController($scope, oClose,oExtras,oAuthService,oConstantsService,oCatalogService, oProcessorService) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oAuthService = oAuthService;
        this.m_oCatalogService = oCatalogService;
        this.m_oConstantsService = oConstantsService;
        this.m_oProcessorService = oProcessorService;
        this.m_sSelectedWorkflowTab = 'WorkFlowTab1';
        this.m_sBaseName = "EV";
        this.m_oParameters = {
            "LASTDAYS": "0",
            "DELETE": true,
            "SIMULATE": false,
            "ORBITS": "",
            "GRIDSTEP": "1,1",
            "PREPROCWORKFLOW": "LISTSinglePreproc2",
            "HSBASTARTDEPTH": "-1",
            "BIMODALITYCOEFFICENT": "2.4",
            "MINIMUMTILEDIMENSION": "10000",
            "MINIMALBLOBREMOVAL": "150",
            "PREPROCESS": true,
            "DAYSBACK": "15",
            "DAYSFORWARD": "15"
        };

        this.m_oBoundingBox = {
            northEast: "",
            southWest: ""
        };

        this.m_oDrawnItems = {};

        this.m_oSelectedDate = moment();
        $scope.m_oController = this;

        this.m_aoGdacsOptions = [];
        this.m_sSelectedGdacs = "";

        this.m_oSlider = {
            value: 0,
            options: {
                floor: 0,
                ceil: 100,
                showTicksValues: true,
                stepsArray: [
                    {value: 0, legend: 'Long Flood'},
                    {value: 10},
                    {value: 20},
                    {value: 30},
                    {value: 40},
                    {value: 50},
                    {value: 60},
                    {value: 70},
                    {value: 80},
                    {value: 90},
                    {value: 100, legend: 'Flash Flood'}
                ],
                ticksTooltip: function(v) {
                    if (v==0) {
                        return 'For long lasting floods: faster with less noise'
                    }
                    else if (v==100) {
                        return 'For flash floods: slower with more noise'
                    }
                    else  return '';
                }
            }
        };
        var oController = this;

        $scope.close = function(result) {
            if (oController.m_oMap != undefined) { oController.m_oMap.remove(); }
            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };

        var oScope = $scope;
        setTimeout(function() {
            oController.initMap("mappaedriftfloodevent");
            oScope.$broadcast('rzSliderForceRender');
        }, 500);
    }
    /*************** METHODS ***************/
    eDriftFloodEventDialogController.prototype.getBoundingBox = function(){
        return this.m_oBoundingBox;
      // alert("North East " + this.m_oBoundingBox.northEast + " South West " + this.m_oBoundingBox.southWest);
    };

    eDriftFloodEventDialogController.prototype.getDate = function(){
      return this.m_oSelectedDate;
    };

    eDriftFloodEventDialogController.prototype.isRunDisabled = function() {

        if(!this.m_oBoundingBox.northEast.lat)
        {
            return true;
        }
        else {
            return false;
        }
     };

     eDriftFloodEventDialogController.prototype.readGDACS = function(){
         var oController = this;

         this.m_oProcessorService.readGDACS().success(function (data) {

             if(utilsIsObjectNullOrUndefined(data) == false)
             {
                 for (iFeature = 0; iFeature<data.features.length; iFeature++) {
                     var oFeature = data.features[iFeature];
                     var oComboFeature = {};
                     oComboFeature.name = oFeature.properties.name;
                     oComboFeature.bbox = oFeature.bbox;
                     oComboFeature.id = ""+iFeature;

                     oController.m_aoGdacsOptions.push(oComboFeature)
                 }
             }
             else
             {
                 utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR READING GDACS EVENTS");
             }
         }).error(function (error) {

             if (error.status == 404) {
                 utilsVexDialogAlertTop("NO FLOOD EVENTS<br> FOUND IN GDACS");
             }
             else {
                 utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR READING GDACS EVENTS");
             }

         });

         return this.m_oSelectedDate;
     };


     eDriftFloodEventDialogController.prototype.selectedGdacsElement = function(){

         var sSelected = this.m_sSelectedGdacs;

         if (sSelected == "") return;

         var iIndex = parseInt(sSelected);
         var oSelectedElement = this.m_aoGdacsOptions[iIndex];

         if (oSelectedElement != null) {
             console.log("" + oSelectedElement.bbox[0] + ";" + oSelectedElement.bbox[1] + ";" + oSelectedElement.bbox[2] + ";" + oSelectedElement.bbox[3]);

             if (oSelectedElement.bbox[1] == oSelectedElement.bbox[3]) {
                 oSelectedElement.bbox[1] = oSelectedElement.bbox[1] - 0.1;
                 oSelectedElement.bbox[3] = oSelectedElement.bbox[3] + 0.1;
             }

             if (oSelectedElement.bbox[0] == oSelectedElement.bbox[2]) {
                 oSelectedElement.bbox[0] = oSelectedElement.bbox[0] - 0.1;
                 oSelectedElement.bbox[2] = oSelectedElement.bbox[2] + 0.1;
             }

             var aoBounds = [[oSelectedElement.bbox[3], oSelectedElement.bbox[2]], [oSelectedElement.bbox[1], oSelectedElement.bbox[0]]];
             // create an orange rectangle
             var oLayer = L.rectangle(aoBounds, {color: "#ff7800", weight: 1});
             this.m_oBoundingBox.northEast = oLayer._bounds._northEast;
             this.m_oBoundingBox.southWest = oLayer._bounds._southWest;
             //remove old shape
             if(this.m_oDrawnItems && this.m_oDrawnItems.getLayers().length!==0){
                 this.m_oDrawnItems.clearLayers();
             }
             //save new shape in map
             this.m_oDrawnItems.addLayer(oLayer);

             this.m_oMap.fitBounds(aoBounds);
         }
     };

     eDriftFloodEventDialogController.prototype.initMap = function(sMapDiv) {

        oOSMBasic = L.tileLayer('http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution:
                '&copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors',
            maxZoom: 18,
            // this map option disables world wrapping. by default, it is false.
            continuousWorld: false,
            // this option disables loading tiles outside of the world bounds.
            noWrap: true
        });

        var oMap = L.map(sMapDiv, {
            zoomControl: false,
            layers: [oOSMBasic],
            keyboard: false
            //maxZoom: 22
        });

        this.m_oMap = oMap;

        // coordinates in map find this plugin in lib folder
        L.control.mousePosition().addTo(oMap);

        //scale control
        L.control.scale({
            position: "bottomright",
            imperial: false
        }).addTo(oMap);


        // center map
        var southWest = L.latLng(0, 0),
            northEast = L.latLng(0, 0),
            oBoundaries = L.latLngBounds(southWest, northEast);

        oMap.fitBounds(oBoundaries);
        oMap.setZoom(3);

        //add draw.search (opensearch)
        this.m_oDrawnItems = new L.FeatureGroup();
        //this.m_oDrawItems = drawnItems;//save draw items (used in delete shape)
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
        var oController = this;
        oMap.on(L.Draw.Event.CREATED, function (event)
        {
            var layer = event.layer;
            oController.m_oBoundingBox.northEast = layer._bounds._northEast;
            oController.m_oBoundingBox.southWest = layer._bounds._southWest;
            //remove old shape
            if(oController.m_oDrawnItems && oController.m_oDrawnItems.getLayers().length!==0){
                oController.m_oDrawnItems.clearLayers();
            }
            //save new shape in map
            oController.m_oDrawnItems.addLayer(layer);
        });

        oMap.on(L.Draw.Event.DELETESTOP, function (event) {
            var layer = event.layers;
        });


        var geocoder = L.Control.Geocoder.nominatim();

        var control = L.Control.geocoder({
            geocoder: geocoder,
            position:'topleft'
        }).addTo(oMap);


        return oMap;

    };

     /*************** METHODS ***************/
     eDriftFloodEventDialogController.prototype.run = function(){
         var oBBOX = this.getBoundingBox();
         var sDate = this.m_oSelectedDate;
         var sBbox = "" + oBBOX.northEast.lat+","+oBBOX.southWest.lng+","+oBBOX.southWest.lat+","+oBBOX.northEast.lng;
         var sBaseName = this.m_sBaseName;
         var oController = this;

         var oParams = this.m_oParameters;

         var iSensitivity = this.m_oSlider.value;

         oParams.MINIMALBLOBREMOVAL = (((150.0-30.0) / 100.0) * iSensitivity) + 30;

         var asParams = [];
         asParams.push('"ASHMAN_COEFF":"'+ oParams.BIMODALITYCOEFFICENT + '"');
         asParams.push('"GRIDSTEP":"'+ oParams.GRIDSTEP + '"');
         asParams.push('"HSBA_DEPTH_IN":"'+ oParams.HSBASTARTDEPTH + '"');
         asParams.push('"LASTDAYS":"'+ oParams.LASTDAYS + '"');
         asParams.push('"BLOBS_SIZE":"'+ oParams.MINIMALBLOBREMOVAL + '"');
         asParams.push('"MIN_PIXNB_BIMODD":"'+ oParams.MINIMUMTILEDIMENSION + '"');
         asParams.push('"PREPROCWORKFLOW":"'+ oParams.PREPROCWORKFLOW + '"');
         asParams.push('"DAYSFORWARD":"'+ oParams.DAYSFORWARD + '"');
         asParams.push('"DAYSBACK":"'+ oParams.DAYSBACK + '"');

         if (!utilsIsStrNullOrEmpty(oParams.ORBITS)) {
             asParams.push('"ORBITS":"'+ oParams.ORBITS + '"');
         }

         if (oParams.DELETE == false){
             asParams.push('"DELETE":"0"')
         }

         if (oParams.PREPROCESS == false){
             asParams.push('"PREPROCESS":"0"')
         }

         if (oParams.SIMULATE == true) {
             asParams.push('"SIMULATE":"1"')
         }

         sJSON = '{ "BBOX": "'+sBbox+'", "EVENT_DATE":"' + sDate + '", "BASENAME":"'+ sBaseName + '"';

         var iParams = 0;

         for (iParams=0; iParams<asParams.length; iParams++) {
             sJSON += ", " + asParams[iParams];
         }

         sJSON += '}';

         console.log(sJSON);

         this.m_oProcessorService.runProcessor("edrift_flood_event", sJSON)
             .success(function (data) {
                 if(utilsIsObjectNullOrUndefined(data) == false)
                 {
                     var oDialog = utilsVexDialogAlertBottomRightCorner("FLOOD EVENT DETECTION SCHEDULED<br>READY");
                     utilsVexCloseDialogAfter(4000,oDialog);

                     console.log('Run ' + data);

                     let oRootscope = oController.m_oScope.$parent;

                     while(oRootscope.$parent != null || oRootscope.$parent != undefined)
                     {
                         oRootscope = oRootscope.$parent;
                     }

                     let payload = { processId: data.processingIdentifier };
                     oRootscope.$broadcast(RootController.BROADCAST_MSG_OPEN_LOGS_DIALOG_PROCESS_ID, payload);
                 }
                 else
                 {
                     utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR RUNNING FLOOD EVENT");
                 }
                 oController.m_oScope.close(null);
             })
             .error(function (error) {
                 utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR RUNNING FLOOD EVENT");
                 oController.m_oScope.close(null);
             });

         //this.m_oScope.close(null);
     };

    eDriftFloodEventDialogController.$inject = [
        '$scope',
        'close',
        'extras',
        'AuthService',
        'ConstantsService',
        'CatalogService',
        'ProcessorService',
    ];
    return eDriftFloodEventDialogController;
})();
