/**
 * Created by a.corrado on 16/06/2017.
 */

var MosaicController = (function() {

    function MosaicController($scope, oClose,oExtras,oSnapOperationService,oConstantsService) {
        this.m_oScope = $scope;
        this.m_oClose = oClose;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_aoProducts = this.m_oExtras.products;
        this.m_oSnapOperationService = oSnapOperationService;
        this.m_oConstantsService = oConstantsService;
        this.m_asProductsName = utilsProjectGetProductsName(this.m_aoProducts);
        this.m_asSelectedProducts = [];

        this.m_bAutoFindBoundingBox = true;
        this.m_bAutoSelectBands = true;

        this.m_oMosaicViewModel = {

            southBound:-1.0,
            eastBound:-1.0,
            westBound :-1.0,
            northBound :-1.0,
            pixelSizeX: -1.0,
            pixelSizeY: -1.0,
            outputFile:"output.tif",
            outputFormat: "GeoTIFF",
            noDataValue: null,
            inputIgnoreValue: null,

            // Legacy parameters.
            bands:"",
            crs: "GEOGCS[\"WGS84(DD)\", \r\n DATUM[\"WGS84\", \r\n SPHEROID[\"WGS84\", 6378137.0, 298.257223563]], \r\n PRIMEM[\"Greenwich\", 0.0], \r\n UNIT[\"degree\", 0.017453292519943295], \r\n AXIS[\"Geodetic longitude\", EAST], \r\n AXIS[\"Geodetic latitude\", NORTH]]",
            overlappingMethod: "MOSAIC_TYPE_OVERLAY",
            showSourceProducts: false,
            elevationModelName: "ASTER 1sec GDEM",
            resamplingName: "Nearest",
            updateMode: false,
            nativeResolution: true,
            combine: "OR",
            variableNames: [],
            variableExpressions: [],
            sources: []
        }

        var oController = this;
        $scope.close = function(result) {
            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };
        $scope.run = function(result) {
            oController.runMosaic();
            // close(""); // close, but give 500ms for bootstrap to animate
        };

    }

    MosaicController.prototype.isDisabledRunButton = function(){
        if(utilsIsObjectNullOrUndefined(this.m_asSelectedProducts))
        {
            return true;
        }
        var iNumberOfSelectedProducts = this.m_asSelectedProducts.length;

        if( iNumberOfSelectedProducts < 2)
        {
            return true;
        }
        return false;
    }

    MosaicController.prototype.runMosaic = function(){

        if (utilsIsStrNullOrEmpty(this.m_oMosaicViewModel.outputFile)) {
            utilsVexDialogAlertBottomRightCorner("Please set an output file name", null);
            return;
        }

        var oActiveWorkspace = this.m_oConstantsService.getActiveWorkspace();

        var asSourceFiles = [];

        for (var i=0; i<this.m_asSelectedProducts.length; i++) {

            var sName = this.m_asSelectedProducts[i];

            for (var j=0; j<this.m_aoProducts.length; j++) {
                if (this.m_aoProducts[j].name === sName) {
                    asSourceFiles.push(this.m_aoProducts[j].fileName);
                    break;
                }
            }
        }

        this.m_oMosaicViewModel.sources = asSourceFiles;

        this.m_oSnapOperationService.geometricMosaic(oActiveWorkspace.workspaceId,this.m_oMosaicViewModel.outputFile, this.m_oMosaicViewModel)
            .then(function (data) {
            var oDialog = utilsVexDialogAlertBottomRightCorner("MOSAIC SCHEDULED<br>READY");
            utilsVexCloseDialogAfter(4000, oDialog);
        },(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN GET PARAMETERS");
        }));
    }
    MosaicController.$inject = [
        '$scope',
        'close',
        'extras',
        'SnapOperationService',
        'ConstantsService',
    ];
    return MosaicController;
})();
window.MosaicController = MosaicController;
