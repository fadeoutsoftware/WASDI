

'use strict';
angular.module('wasdi.PagesService', ['wasdi.ConstantsService','wasdi.OpenSearchService']).
service('PagesService', ['$http',  'ConstantsService','OpenSearchService', function ($http, oConstantsService, oOpenSearchService) {

    this.m_oOpenSearchService = oOpenSearchService;
    this.m_aListOfProvider = [];
    this.m_iProductsPerPage = [10,15,20,25,50];
    this.m_oFunction = null;
    var oController = this;

    this.m_oOpenSearchService.getListOfProvider().then(function (data) {
        if(utilsIsObjectNullOrUndefined(data.data) === false && data.data.length > 0)
        {

            oController.m_aListOfProvider[0] = {
                "name": "AUTO",
                "totalOfProducts":0,
                "totalPages":1,
                "currentPage":1,
                "productsPerPageSelected":10,
                "selected":true,
                "isLoaded":false,
                "description": "WASDI Automatic Data Provider",
                "link": "https://www.wasdi.net"
            };

            var iLengthData = data.data.length;
            for(var iIndexProvider = 0; iIndexProvider < iLengthData; iIndexProvider++)
            {
                oController.m_aListOfProvider[iIndexProvider+1] = {
                    "name": data.data[iIndexProvider].code,
                    "totalOfProducts":0,
                    "totalPages":1,
                    "currentPage":1,
                    "productsPerPageSelected":10,
                    "selected":false,
                    "isLoaded":false,
                    "description": data.data[iIndexProvider].description,
                    "link": data.data[iIndexProvider].link
                };
            }
        }

    },function (data) {

    });

    this.setDefaultPaginationValuesForProvider = function()
    {

        var iNumberOfListProvider = this.m_aListOfProvider.length;
        for(var iIndexProviders = 0; iIndexProviders < iNumberOfListProvider ; iIndexProviders++)
        {
            this.m_aListOfProvider[iIndexProviders].totalOfProducts = 0;
            this.m_aListOfProvider[iIndexProviders].totalPages = 1;
            this.m_aListOfProvider[iIndexProviders].currentPage = 1;
            this.m_aListOfProvider[iIndexProviders].productsPerPageSelected = 10;
            this.m_aListOfProvider[iIndexProviders].isLoaded = false;
        }
    };

    this.getProviders = function(){
        return this.m_aListOfProvider;
    };
    this.getProductsPerPageOptions = function(){
        return this.m_iProductsPerPage;
    };

    this.setFunction = function(oFunction){
        if(utilsIsObjectNullOrUndefined(oFunction) === true)
            return false;
        this.m_oFunction = oFunction;
    };

    this.getFunction = function(){
        return this.m_oFunction;
    };

    this.getProviderIndex = function(sProvider){
        var iResult = -1;
        if(utilsIsObjectNullOrUndefined(sProvider) === true)
            return iResult;
        var iNumberOfProviders = this.m_aListOfProvider.length;

        for(var iIndexProvider = 0; iIndexProvider < iNumberOfProviders; iIndexProvider++){
            if(this.m_aListOfProvider[iIndexProvider].name === sProvider)
            {
                iResult = iIndexProvider;
                break;
            }
        }

        return iResult;
    };
    this.getProviderObject = function(sProvider){
        var iIndexProviderFind = this.getProviderIndex(sProvider);

        if(iIndexProviderFind === -1)
            return null;

        return this.m_aListOfProvider[iIndexProviderFind];

    };

    this.countPages = function(sProvider){

        var oProvider = this.getProviderObject(sProvider);

        if(utilsIsObjectNullOrUndefined(oProvider) === true)
            return -1;

        if(oProvider.productsPerPageSelected != 0 )
        {
            if((oProvider.totalOfProducts % oProvider.productsPerPageSelected) == 0)
            {
                oProvider.totalPages = Math.floor(oProvider.totalOfProducts/oProvider.productsPerPageSelected);
            }
            else
            {
                oProvider.totalPages = Math.floor(oProvider.totalOfProducts/oProvider.productsPerPageSelected)+1;
            }
        }
        return oProvider.totalPages;
    };

    this.calcOffset = function(sProvider){
        var oProvider = this.getProviderObject(sProvider);

        if(utilsIsObjectNullOrUndefined(oProvider) === true)
            return -1;

        return(oProvider.currentPage-1) * oProvider.productsPerPageSelected;
    };

    this.changePage = function(iNewPage,sProvider,oThat){
        iNewPage = parseInt(iNewPage);
        var oFunction = this.getFunction();
        var oProvider = this.getProviderObject(sProvider);
        if( (utilsIsObjectNullOrUndefined(oProvider) === true) || (utilsIsObjectNullOrUndefined(oFunction) === true))
            return false;

        if(!utilsIsObjectNullOrUndefined(iNewPage) && isNaN(iNewPage) == false && utilsIsInteger(iNewPage) && iNewPage >= 0 && iNewPage <= oProvider.totalPages)
        {
            oProvider.currentPage = iNewPage;
            oFunction(oProvider,oThat);
        }
        else
        {
            return false ;
        }
        return true;
    };

    this.plusOnePage = function(sProvider,oThat){
        var oProvider = this.getProviderObject(sProvider);
        if( (utilsIsObjectNullOrUndefined(oProvider) === true) )
            return false;

        var iNewPage = parseInt(oProvider.currentPage);

        if(!utilsIsObjectNullOrUndefined(iNewPage) && isNaN(iNewPage) == false && utilsIsInteger(iNewPage) && iNewPage >= 0 && iNewPage <= oProvider.totalPages)
        {
            oProvider.currentPage = iNewPage;
            this.changePage(oProvider.currentPage + 1,sProvider,oThat);
        }
    };

    this.minusOnePage = function(sProvider,oThat){
        var oProvider = this.getProviderObject(sProvider);

        if( (utilsIsObjectNullOrUndefined(oProvider) === true)  )
            return false;

        var iNewPage = parseInt(oProvider.currentPage);

        if(!utilsIsObjectNullOrUndefined(iNewPage) && isNaN(iNewPage) == false && utilsIsInteger(iNewPage) && iNewPage > 1 && iNewPage <= oProvider.totalPages)
        {
            oProvider.currentPage = iNewPage;
            this.changePage(oProvider.currentPage-1,sProvider,oThat);
        }
    };

    this.lastPage = function(sProvider,oThat){

        var oProvider = this.getProviderObject(sProvider);

        if( (utilsIsObjectNullOrUndefined(oProvider) === true)  )
            return false;

        this.changePage(oProvider.totalPages,sProvider,oThat);

    };

    this.firstPage = function(sProvider,oThat){
        this.changePage(1,sProvider,oThat);
    };

    this.getNumberOfProductByProvider = function(sProvider){
        if(utilsIsStrNullOrEmpty(sProvider) === true )
            return -1;
        var aoProviders = this.getProviders();
        var iNumberOfProviders = aoProviders.length;

        for(var iIndexProvider = 0; iIndexProvider < iNumberOfProviders; iIndexProvider++)
        {
            if(aoProviders[iIndexProvider].name === sProvider)
                return aoProviders[iIndexProvider].totalOfProducts;
        }

        return -1;
    };

    this.changeNumberOfProductsPerPage = function(sProvider,oThat)
    {
        var oFunction = this.getFunction();
        if( (utilsIsObjectNullOrUndefined(sProvider) === false) && (utilsIsObjectNullOrUndefined(oThat) === false) && (utilsIsObjectNullOrUndefined(oFunction) === false) )
        {
            //countPages
            var oProvider = this.getProviderObject(sProvider);
            oFunction(oProvider,oThat);
            return true;
        }
        return false;
    };

}]);

