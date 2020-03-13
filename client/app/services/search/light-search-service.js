

'use strict';
angular.module('wasdi.LightSearchService', []).
service('LightSearchService', ['$http','ConstantsService', function ($http,oConstantsService) {

    this.getOpenSearchGeoselection = function(oBoundingBox){
        var sFilter = '( footprint:"intersects(POLYGON((';


        if(!utilsIsObjectNullOrUndefined(oBoundingBox))
        {
            var oNorthEast =  oBoundingBox.northEast;
            var oSouthWest = oBoundingBox.southWest;

            //  ----------X
            // |          |
            // |          |
            //  ----------
            sFilter = sFilter + oNorthEast.lng + " " + oNorthEast.lat +",";
            //  ----------
            // |          |
            // |          |
            //  ----------X
            sFilter = sFilter + oSouthWest.lng + " " + oNorthEast.lat +",";
            //  ----------
            // |          |
            // |          |
            // X----------
            sFilter = sFilter + oSouthWest.lng + " " + oSouthWest.lat +",";
            // X----------
            // |          |
            // |          |
            //  ----------
            sFilter = sFilter + oNorthEast.lng + " " + oSouthWest.lat +",";
            //  ----------X
            // |          |
            // |          |
            //  ----------
            sFilter = sFilter + oNorthEast.lng + " " + oNorthEast.lat + ')))" )';
        }

        return sFilter ;
    }

}]);
