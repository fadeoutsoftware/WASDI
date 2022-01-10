/**
 * Created by a.corrado on 24/01/2017.
 */


'use strict';
angular.module('wasdi.ResultsOfSearchService', ['wasdi.ConstantsService']).
service('ResultsOfSearchService', ['$http',  'ConstantsService','OpenSearchService', function ($http, oConstantsService,oOpenSearchService) {
    this.m_oOpenSearchService = oOpenSearchService;
    this.m_sTextQuery = "";
    this.m_oGeoSelection = '';
    this.m_iCurrentPage = 1;
    this.m_iProductsPerPageSelected = 10;
    this.m_aoProductList = [];
    this.m_iTotalPages = 1;
    this.m_iTotalOfProducts = 0;
    this.m_bIsVisibleListOfProducts = false;
    this.m_oActiveWorkspace = null;
    this.m_aoMissions  = [];
    this.m_oSensingPeriodFrom = '';
    this.m_oSensingPeriodTo='';
    this.m_aoMissions = [];
    this.m_oIngestionPeriodFrom = '';
    this.m_oIngestionPeriodTo = '';
    // this.m_aoProviders = [];

    /************************ Set methods ***************************/

    this.setDefaults  = function()
    {
        this.m_sTextQuery = "";
        this.m_oGeoSelection = '';
        this.m_iCurrentPage = 1;
        this.m_iProductsPerPageSelected = 10;
        this.m_aoProductList = [];
        this.m_iTotalPages = 1;
        this.m_iTotalOfProducts = 0;
        this.m_bIsVisibleListOfProducts = false;
        this.m_oActiveWorkspace = null;
        this.m_aoMissions  = [];
        this.m_oSensingPeriodFrom = '';
        this.m_oSensingPeriodTo='';
        this.m_oIngestionPeriodFrom = '';
        this.m_oIngestionPeriodTo = '';
        // this.getDefaultValueOfProvidersByServer();
    };

    // this.setProviders = function(aoProvidersInput)
    // {
    //     this.m_aoProviders = aoProvidersInput;
    // };

    this.setIngestionPeriodTo = function(oIngestionPeriodTo)
    {
        this.m_oIngestionPeriodTo = oIngestionPeriodTo;
        return true;
    };

    this.setIngestionPeriodFrom = function(oIngestionPeriodFrom)
    {
        this.m_oIngestionPeriodFrom = oIngestionPeriodFrom;
        return true;
    };
    this.setMissions = function(oMissions)
    {
        //if(utilsIsObjectNullOrUndefined(oMissions))
        //    return false;
        this.m_aoMissions = oMissions;
        return true;
    };

    this.setSensingPeriodTo  = function(oSensingPeriodTo)
    {
        //if(utilsIsObjectNullOrUndefined(oSensingPeriodTo))
        //    return false;
        this.m_oSensingPeriodTo=oSensingPeriodTo;
        return true;
    };

    this.setSensingPeriodFrom = function(oSensingPeriodFrom)
    {
        //if(utilsIsObjectNullOrUndefined(oSensingPeriodFrom))
        //    return false;
        this.m_oSensingPeriodFrom=oSensingPeriodFrom;
        return true;
    };

    this.setTextQuery = function(sTextQuery)
    {
        //if(utilsIsStrNullOrEmpty(sTextQuery))
        //    return false //TODO throw error
        this.m_sTextQuery = sTextQuery;
        return true;
    };
    this.setGeoSelection = function(oGeoSelection)
    {
        //if(utilsIsStrNullOrEmpty(oGeoSelection))
        //    return false //TODO throw error
        this.m_oGeoSelection = oGeoSelection;
        return true;
    };
    this.setCurrentPage = function(iCurrentPage)
    {
        //if(utilsIsObjectNullOrUndefined(iCurrentPage))
        //    return false //TODO throw error
        this.m_iCurrentPage = iCurrentPage;
        return true;
    };
    this.setProductsPerPageSelected = function(iProductsPerPageSelected)
    {
        //if(utilsIsObjectNullOrUndefined(iProductsPerPageSelected))
        //    return false //TODO throw error
        this.m_iProductsPerPageSelected = iProductsPerPageSelected;
        return true;
    };
    this.setProductList = function(aoProductList)
    {
        //if(utilsIsObjectNullOrUndefined(aoProductList))
        //    return false //TODO throw error
        this.m_aoProductList = aoProductList;
        return true;
    };
    this.setTotalPages = function(iTotalPages)
    {
        //if(utilsIsObjectNullOrUndefined(iTotalPages))
        //    return false //TODO throw error
        this.m_iTotalPages = iTotalPages;
        return true;
    };

    this.setIsVisibleListOfProducts = function(bIsVisibleListOfProducts)
    {
        //if(utilsIsObjectNullOrUndefined(bIsVisibleListOfProducts))
        //    return false;//TODO throw error
        this.m_bIsVisibleListOfProducts = bIsVisibleListOfProducts;
        return true;
    };
    this.setTotalOfProducts = function(iTotalOfProducts)
    {
        //if(utilsIsObjectNullOrUndefined(iTotalOfProducts))
        //    return false;//TODO throw error
        this.m_iTotalOfProducts = iTotalOfProducts;
        return true;
    };

    this.setActiveWorkspace = function(oActiveWorkspace)
    {
        //if(utilsIsObjectNullOrUndefined(oActiveWorkspace))
        //    return false;//TODO throw error
        this.m_oActiveWorkspace = oActiveWorkspace;
        return true;
    };

    this.setMissions = function(aoMissions)
    {
        //if(utilsIsObjectNullOrUndefined(aoMissions))
        //    return false;
        this.m_aoMissions = aoMissions;
        return true;
    };
    /************************ Get methods ***************************/
    this.getIngestionPeriodTo = function()
    {
        return this.m_oIngestionPeriodTo;
    };
    this.getIngestionPeriodFrom = function()
    {
        return  this.m_oIngestionPeriodFrom;
    };
    this.getTextQuery = function()
    {
        return this.m_sTextQuery;
    };
    this.getGeoSelection = function()
    {
        return this.m_oGeoSelection;
    };
    this.getCurrentPage = function()
    {
        return this.m_iCurrentPage;
    };
    this.getProductsPerPageSelected = function()
    {
        return this.m_iProductsPerPageSelected ;
    };
    this.getProductList = function()
    {
        return this.m_aoProductList;
    };
    this.getTotalPages = function()
    {
        return this.m_iTotalPages;
    };
    this.getIsVisibleListOfProducts = function()
    {
        return this.m_bIsVisibleListOfProducts;
    };
    this.getTotalOfProducts = function()
    {
        return this.m_iTotalOfProducts;
    };
    this.getActiveWorkspace = function()
    {
        return this.m_oActiveWorkspace;
    };
    this.getMissions = function()
    {
        return this.m_aoMissions;
    };

    this.getSensingPeriodFrom = function()
    {
        return this.m_oSensingPeriodFrom;
    };

    this.getSensingPeriodTo  = function()
    {
        return this.m_oSensingPeriodTo;
    };

    this.getMissions = function()
    {
        return this.m_aoMissions;
    };

}]);