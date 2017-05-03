/**
 * Created by a.corrado on 06/04/2017.
 */

var RangeDopplerTerrainCorrectionController = (function() {

    function RangeDopplerTerrainCorrectionController($scope, oClose,oExtras) {
        //MEMBERS
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_aoProducts = this.m_oExtras.products;
        this.m_oSelectedProduct = this.m_oExtras.selectedProduct;

        if(utilsIsObjectNullOrUndefined(this.m_aoProducts) == true)
        {
            this.m_aoProducts = [];
        }
        if(utilsIsObjectNullOrUndefined(this.m_oSelectedProduct) == true)
        {
            this.m_oSelectedProduct = null;
        }
        else
        {
            if( (utilsIsObjectNullOrUndefined(this.m_oSelectedProduct.productFriendlyName)== false) && (utilsIsStrNullOrEmpty(this.m_oSelectedProduct.productFriendlyName)== false))
                this.m_sFileName_Operation = this.m_oSelectedProduct.name + "_RangeDopplerTerrainCorrection.zip";

            if(utilsIsObjectNullOrUndefined(this.m_oSelectedProduct.bandsGroups.bands) == true)
                var iNumberOfBands = 0;
            else
                var iNumberOfBands = this.m_oSelectedProduct.bandsGroups.bands.length;

            this.m_asSourceBands = [];
            //load bands
            for(var iIndexBand = 0; iIndexBand < iNumberOfBands ;iIndexBand++)
            {
                if( utilsIsObjectNullOrUndefined(this.m_oSelectedProduct.bandsGroups.bands[iIndexBand]) == false )
                    this.m_asSourceBands.push(this.m_oSelectedProduct.bandsGroups.bands[iIndexBand].name);
            }
        }

        // this.m_oTabOpen = "tab1";
        //m_asTypeOfData and m_asOrbitStateVectors are used in ng-include InputOutputParametersView.html
        // this.m_asTypeOfData = ["GeoTIFF","NetCDF-BEAM","NetCDF4-CF","NetCDF-CF","CSV","Gamma","Generic Binary","GeoTIFF+XML",
        //     "NetCDF4-BEAM","BEAM-DIMAP","ENVI","PolSARPro","Snaphu","JP2","JPG","PNG","BMP","GIF","BTF","GeoTIFF-BIGTIFF","HDF5"];
        // this.m_asOrbitStateVectors = ["Sentinel Precise(Auto Download)","Sentinel Restituted(Auto Download)","DORIS preliminary POR(ENVISAT)"
        //     ,"DORIS Precise Vor(ENVISAT)(Auto Download)","DELFT Precise(ENVISAT,ERS1&2)(Auto Download)","PRARE Precise(ERS1&2)(Auto Download)"];
        // this.m_sSelectedExtension = this.m_asTypeOfData[0];

        // this.m_asAuxiliaryFile = ["Latest Auxiliary File","Product Auxiliary File","External Auxiliary File"];
        // this.m_sSelectedCalibration = this.m_asAuxiliaryFile[0];
        this.m_asDigitalElevationModel = ["ACE2_5Min(Auto Download)","ACE30(Auto Download)","ASTER 1sec GDEM","GETASSE30(Auto Download)","SRTM 1Sec Grid","SRTM 1Sec HGT (Auto Download)",
                                            "SRTM 3Sec(Auto Download)","External DEM"];
        this.m_asResamplingMethod = ["NEAREST_NEIGHBOUR","BILINEAR_INTERPOLATION","CUBIC_CONVOLUTION","BISINC_5_POINT_INTERPOLATION","BISINC_11_POINT_INTERPOLATION",
                                        "BISINC_21_POINT_INTERPOLATION","BICUBIC_INTERPOLATION","DELAUNAY_INTERPOLATION"];

        this.m_asLatestAuxiliaryFile = ["Latest Auxiliary File","Product Auxiliary File","External Auxiliary File"];
        this.m_asSaveBand = ["Use projected local indicence angle from DEM","Use local indicence angle from DEM","Use indicence angle from Elipsoid"];

        this.m_sLatestAuxiliaryFileSelected =   this.m_asLatestAuxiliaryFile[0];
        this.m_sDigitalElevationModelSelected = this.m_asDigitalElevationModel[6];
        this.m_sDEMResamplingMethodSelected = this.m_asResamplingMethod[0];
        this.m_sImageResamplingMethodSelected = this.m_asResamplingMethod[0];
        this.m_sSaveSigmaSelected = this.m_asSaveBand[0];
        this.m_sSaveGammaSelected = this.m_asSaveBand[0];

        this.m_asSourceBandsSelected = [];

        this.m_oReturnValue={
            sourceFileName:"",
            destinationFileName:"",
            options:{
                sourceBandNames:"",
                demName:this.m_asDigitalElevationModel[6],
                // externalDEMFile:"",
                // externalDEMNoDataValue:0,
                // externalDEMApplyEGM:true,
                demResamplingMethod:this.m_asResamplingMethod[0],
                imgResamplingMethod:this.m_asResamplingMethod[0],
                pixelSpacingInMeter:0,
                pixelSpacingInDegree:0,
                // mapProjection:"WGS84(DD)",
                nodataValueAtSea:true,
                saveDEM:false,
                saveLatLon:false,
                saveIncidenceAngleFromEllipsoid:false,
                saveLocalIncidenceAngle:false,
                saveProjectedLocalIncidenceAngle:false,
                saveSelectedSourceBand:true,
                outputComplex:false,

                applyRadiometricNormalization:false,
                saveSigmaNought:false,
                saveGammaNought:false,
                saveBetaNought:false,
                incidenceAngleForSigma0:"",
                incidenceAngleForGamma0:"",
                auxFile:"",
                externalAuxFile:""
            }
        };


        //this.m_oOrbit = oExtras;
        //$scope.close = oClose;
        $scope.close = function() {
            oClose("close", 500); // close, but give 500ms for bootstrap to animate
        };

        var oController = this;
        $scope.run = function(oOptions) {
            //TODO CHECK OPTIONS
            var bAreOkOptions = true;
            if( (utilsIsObjectNullOrUndefined(oController.m_oSelectedProduct.fileName) == true) && (utilsIsStrNullOrEmpty(oController.m_oSelectedProduct.fileName) == true) )
                bAreOkOptions = false;
            if( (utilsIsObjectNullOrUndefined(oController.m_sFileName_Operation) == true) && (utilsIsStrNullOrEmpty(oController.m_sFileName_Operation) == true) )
                bAreOkOptions = false;
            if( (utilsIsObjectNullOrUndefined(oController.m_asSourceBandsSelected) == true) )
                bAreOkOptions = false;
            if( (utilsIsObjectNullOrUndefined(oController.m_sDigitalElevationModelSelected) == true) && (utilsIsStrNullOrEmpty(oController.m_sDigitalElevationModelSelected) == true) )
                bAreOkOptions = false;
            if( (utilsIsObjectNullOrUndefined(oController.m_sDEMResamplingMethodSelected) == true) && (utilsIsStrNullOrEmpty(oController.m_sDEMResamplingMethodSelected) == true) )
                bAreOkOptions = false;
            if( (utilsIsObjectNullOrUndefined(oController.m_sImageResamplingMethodSelected) == true) && (utilsIsStrNullOrEmpty(oController.m_sImageResamplingMethodSelected) == true) )
                bAreOkOptions = false;
            if( (utilsIsObjectNullOrUndefined(oController.options.pixelSpacingInMeter) == true) && (utilsIsANumber(oController.options.pixelSpacingInMeter) == false) )
                bAreOkOptions = false;
            if( (utilsIsObjectNullOrUndefined(oController.options.pixelSpacingInDegree) == true) && (utilsIsANumber(oController.options.pixelSpacingInDegree) == false) )
                bAreOkOptions = false;

            if( utilsIsObjectNullOrUndefined(oController.options.nodataValueAtSea) == true )
                bAreOkOptions = false;
            if( utilsIsObjectNullOrUndefined(oController.options.saveDEM) == true )
                bAreOkOptions = false;
            if( utilsIsObjectNullOrUndefined(oController.options.saveLatLon) == true )
                bAreOkOptions = false;
            if( utilsIsObjectNullOrUndefined(oController.options.saveIncidenceAngleFromEllipsoid) == true )
                bAreOkOptions = false;
            if( utilsIsObjectNullOrUndefined(oController.options.saveLocalIncidenceAngle) == true )
                bAreOkOptions = false;
            if( utilsIsObjectNullOrUndefined(oController.options.saveProjectedLocalIncidenceAngle) == true )
                bAreOkOptions = false;
            if( utilsIsObjectNullOrUndefined(oController.options.saveSelectedSourceBand) == true )
                bAreOkOptions = false;
            if( utilsIsObjectNullOrUndefined(oController.options.outputComplex) == true )
                bAreOkOptions = false;
            if( utilsIsObjectNullOrUndefined(oController.options.applyRadiometricNormalization) == true )
                bAreOkOptions = false;
            if( utilsIsObjectNullOrUndefined(oController.options.saveSigmaNought) == true )
                bAreOkOptions = false;
            if( utilsIsObjectNullOrUndefined(oController.options.saveGammaNought) == true )
                bAreOkOptions = false;
            if( utilsIsObjectNullOrUndefined(oController.options.saveBetaNought) == true )
                bAreOkOptions = false;

            if(bAreOkOptions != false)
            {
                oController.m_oReturnValue.sourceFileName = oController.m_oSelectedProduct.fileName;
                oController.m_oReturnValue.destinationFileName = oController.m_sFileName_Operation;
                oController.m_oReturnValue.options.sourceBandNames = oController.m_asSourceBandsSelected;
                oController.m_oReturnValue.options.demName = oController.m_sDigitalElevationModelSelected;
                oController.m_oReturnValue.options.demResamplingMethod = oController.m_sDEMResamplingMethodSelected;
                oController.m_oReturnValue.options.imgResamplingMethod = oController.m_sImageResamplingMethodSelected;

            }
            else
            {
                oOptions = null;
            }
            oClose(oOptions, 500); // close, but give 500ms for bootstrap to animate
        };

    };

    RangeDopplerTerrainCorrectionController.prototype.changeProduct = function(oNewSelectedProductInput)
    {
        if(utilsIsObjectNullOrUndefined(oNewSelectedProductInput) == true)
            return false;
        this.m_oSelectedProduct = oNewSelectedProductInput;
        this.m_sFileName_Operation = this.m_oSelectedProduct.name + "_RangeDopplerTerrainCorrection.zip";
        this.m_oReturnValue={
            sourceFileName:"",
            destinationFileName:"",
            options:{
                sourceBandNames:"",
                demName:this.m_asDigitalElevationModel[6],
                // externalDEMFile:"",
                // externalDEMNoDataValue:0,
                // externalDEMApplyEGM:true,
                demResamplingMethod:this.m_asResamplingMethod[0],
                imgResamplingMethod:this.m_asResamplingMethod[0],
                pixelSpacingInMeter:0,
                pixelSpacingInDegree:0,
                // mapProjection:"WGS84(DD)",
                nodataValueAtSea:true,
                saveDEM:false,
                saveLatLon:false,
                saveIncidenceAngleFromEllipsoid:false,
                saveLocalIncidenceAngle:false,
                saveProjectedLocalIncidenceAngle:false,
                saveSelectedSourceBand:true,
                outputComplex:false,

                applyRadiometricNormalization:false,
                saveSigmaNought:false,
                saveGammaNought:false,
                saveBetaNought:false,
                incidenceAngleForSigma0:"",
                incidenceAngleForGamma0:"",
                auxFile:"",
                externalAuxFile:""
            }
        };
        this.m_asSourceBandsSelected = [];

        if(utilsIsObjectNullOrUndefined(this.m_oSelectedProduct.bandsGroups.bands) == true)
            var iNumberOfBands = 0;
        else
            var iNumberOfBands = this.m_oSelectedProduct.bandsGroups.bands.length;

        this.m_asSourceBands = [];
        //load bands
        for(var iIndexBand = 0; iIndexBand < iNumberOfBands ;iIndexBand++)
        {
            if( utilsIsObjectNullOrUndefined(this.m_oSelectedProduct.bandsGroups.bands[iIndexBand]) == false )
                this.m_asSourceBands.push(this.m_oSelectedProduct.bandsGroups.bands[iIndexBand].name);
        }



        return true;
    };

    RangeDopplerTerrainCorrectionController.prototype.selectedProductIsEmpty = function()
    {
        if(utilsIsObjectNullOrUndefined(this.m_oSelectedProduct) == true)
            return true;
        return false;
    };

    RangeDopplerTerrainCorrectionController.$inject = [
        '$scope',
        'close',
        'extras'

    ];
    return RangeDopplerTerrainCorrectionController;
})();
