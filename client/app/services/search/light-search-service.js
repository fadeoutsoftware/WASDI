

'use strict';
angular.module('wasdi.LightSearchService', []).
service('LightSearchService', ['$http','ConstantsService','AdvancedSearchService','SearchService', function ($http,
                                                                                             oConstantsService,
                                                                                             oAdvancedSearchService,
                                                                                             $SearchService) {
    this.m_oAdvancedSearchService = oAdvancedSearchService;
    this.m_sDefaultProductImage = "assets/icons/ImageNotFound.svg";
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

    this.utcDateConverter = function(date){
        var result = date;
        if(date != undefined) {
            var day =  moment(date).get('date');
            var month = moment(date).get('month');
            var year = moment(date).get('year');
            var utcDate = moment(year+ "-" + (parseInt(month)+1) +"-" +day+ " 00:00 +0000", "YYYY-MM-DD HH:mm Z"); // parsed as 4:30 UTC
            result =  utcDate;
        }

        return result;
    }

    this.getOpenSearchDate = function(searchFilter){

        var sFilter='';

        if (utilsIsObjectNullOrUndefined(searchFilter)) return sFilter;

        if(!utilsIsObjectNullOrUndefined(searchFilter.sensingPeriodFrom) && !utilsIsObjectNullOrUndefined(searchFilter.sensingPeriodTo))
        {
            sFilter += '( beginPosition:['+ this.m_oAdvancedSearchService.formatDateFrom_(searchFilter.sensingPeriodFrom) +
                ' TO ' + this.m_oAdvancedSearchService.formatToDate(searchFilter.sensingPeriodTo) + '] AND endPosition:[' +
                this.m_oAdvancedSearchService.formatDateFrom_(searchFilter.sensingPeriodFrom) + ' TO ' + this.m_oAdvancedSearchService.formatDateTo_(searchFilter.sensingPeriodTo) + '] )';
        }
        else if (!utilsIsObjectNullOrUndefined(searchFilter.sensingPeriodFrom))
        {
            sFilter += '( beginPosition:['+ this.m_oAdvancedSearchService.formatDateFrom_(searchFilter.sensingPeriodFrom) +
                ' TO NOW] AND endPosition:[' + this.m_oAdvancedSearchService.formatDateFrom_(searchFilter.sensingPeriodFrom) + ' TO NOW] )';
        }
        else if(!utilsIsObjectNullOrUndefined(searchFilter.sensingPeriodTo))
        {
            sFilter += '( beginPosition:[ * TO ' + this.m_oAdvancedSearchService.formatDateTo_(searchFilter.sensingPeriodTo) + '] AND endPosition:[* TO ' + this.m_oAdvancedSearchService.formatToDate(searchfilter.sensingPeriodTo) + ' ] )';
        }
        if(!utilsIsObjectNullOrUndefined(searchFilter.ingestionFrom) && !utilsIsObjectNullOrUndefined(searchFilter.ingestionTo))
        {
            sFilter += ((sFilter)?' AND':'') + '( ingestionDate:['+ this.m_oAdvancedSearchService.formatDateFrom_(searchFilter.ingestionFrom) +
                ' TO ' + this.m_oAdvancedSearchService.formatDateTo_(searchFilter.ingestionTo) + ' ] )';
        }
        else if (!utilsIsObjectNullOrUndefined(searchFilter.ingestionFrom))
        {
            sFilter += ((sFilter)?' AND':'') + '( ingestionDate:['+ this.m_oAdvancedSearchService.formatDateFrom_(searchFilter.ingestionFrom) +' TO NOW] )';
        }
        else if(!utilsIsObjectNullOrUndefined(searchFilter.ingestionTo))
        {
            sFilter += ((sFilter)?' AND':'') + '( ingestionDate:[ * TO ' + this.m_oAdvancedSearchService.formatDateTo_(searchFilter.ingestionTo) + ' ] )';
        }

        return sFilter;
    };

    this.setDefaultPreviews = function(aoProducts){

        if(utilsIsObjectNullOrUndefined(aoProducts) === true) {
            return false;
        }
        let iNumberOfProducts = aoProducts.length;
        for(let iIndexProduct = 0 ; iIndexProduct < iNumberOfProducts ; iIndexProduct++){
            if(utilsIsObjectNullOrUndefined( aoProducts[iIndexProduct].preview) ||
               utilsIsStrNullOrEmpty( aoProducts[iIndexProduct].preview)){
                aoProducts[iIndexProduct].preview = this.m_sDefaultProductImage;
            }


        }

    }


    this.calcOffset = function(oProvider){

        if(utilsIsObjectNullOrUndefined(oProvider) === true)
            return -1;

        return(oProvider.currentPage-1) * oProvider.productsPerPageSelected;
    };

    this.lightSearch = function(sOpenSearchGeoselection,oOpenSearchDates,oProvider,oCallback,oCallbackError ){
        $SearchService.setGeoselection(sOpenSearchGeoselection);
        $SearchService.setAdvancedFilter(oOpenSearchDates);
        var aoProviders = [];
        aoProviders.push(oProvider);
        $SearchService.setProviders(aoProviders);
        var iOffset = this.calcOffset(oProvider);
        $SearchService.setOffset(iOffset);//default 0 (index page)
        $SearchService.setLimit(oProvider.productsPerPageSelected);
        //todo  set default filters ?  this.m_oAdvancedFilterService.setAdvancedFilter + this.m_oSearchService.setMissionFilter

        // let oController = this;
        $SearchService.search().then(function(result){
            oCallback(result);
        }, function errorCallback(response) {
            oCallbackError();
        });
    }
}]);
