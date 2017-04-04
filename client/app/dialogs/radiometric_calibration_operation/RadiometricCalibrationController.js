/**
 * Created by a.corrado on 03/04/2017.
 */


var RadiometricCalibrationController = (function() {

    function RadiometricCalibrationController($scope, oClose) {
        //MEMBERS
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oTabOpen = "tab1";
        this.m_asTypeOfData = ["GeoTIFF","NetCDF-BEAM","NetCDF4-CF","NetCDF-CF","CSV","Gamma","Generic Binary","GeoTIFF+XML",
            "NetCDF4-BEAM","BEAM-DIMAP","ENVI","PolSARPro","Snaphu","JP2","JPG","PNG","BMP","GIF","BTF","GeoTIFF-BIGTIFF","HDF5"];
        this.m_asOrbitStateVectors = ["Sentinel Precise(Auto Download)","Sentinel Restituted(Auto Download)","DORIS preliminary POR(ENVISAT)"
            ,"DORIS Precise Vor(ENVISAT)(Auto Download)","DELFT Precise(ENVISAT,ERS1&2)(Auto Download)","PRARE Precise(ERS1&2)(Auto Download)"];
        this.m_asAuxiliaryFile = ["Latest Auxiliary File","Product Auxiliary File","External Auxiliary File"];
        this.m_asSourceBands =  ["Band1","Band2","Band3"];

        this.m_asSourceBandsSelected = [];
        //this.m_oOrbit = oExtras;
        //$scope.close = oClose;
        $scope.close = function(result) {
            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };

    };
    RadiometricCalibrationController.prototype.tabOpen = function(sTabInput)
    {
        if(utilsIsStrNullOrEmpty(sTabInput))
            return false;
        this.m_oTabOpen = sTabInput;
        return true;
    };
    RadiometricCalibrationController.prototype.firstTabIsOpen = function()
    {
        if( this.m_oTabOpen == "tab1")
            return true;
        else
            return false;
    };
    RadiometricCalibrationController.prototype.secondTabIsOpen = function()
    {
        if( this.m_oTabOpen == "tab2")
            return true;
        else
            return false;
    };

    RadiometricCalibrationController.$inject = [
        '$scope',
        'close',

    ];
    return RadiometricCalibrationController;
})();
