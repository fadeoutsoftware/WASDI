/**
 * Created by a.corrado on 24/01/2017.
 */


'use strict';
angular.module('wasdi.ResultsOfSearchService', ['wasdi.ConstantsService']).
service('ResultsOfSearchService', ['$http',  'ConstantsService', function ($http, oConstantsService) {
    this.m_sTextQuery = "";
    this.m_oGeoSelection = '';
    this.m_iCurrentPage = 1;
    this.m_iProductsPerPageSelected = 5;
    this.m_aoProductList = null;
    this.m_iTotalPages = 1;
    this.m_bIsVisibleListOfProducts = false;
    /************************ Set methods ***************************/
    this.setTextQuery = function(sTextQuery)
    {
        if(utilsIsStrNullOrEmpty(sTextQuery))
            return false //TODO throw error
        this.m_sTextQuery = sTextQuery;
        return true;
    }
    this.setGeoSelection = function(oGeoSelection)
    {
        if(utilsIsObjectNullOrUndefined(m_oGeoSelection))
            return false //TODO throw error
        this.m_oGeoSelection = oGeoSelection;
        return true;
    }
    this.setCurrentPage = function(iCurrentPage)
    {
        if(utilsIsObjectNullOrUndefined(iCurrentPage))
            return false //TODO throw error
        this.m_iCurrentPage = iCurrentPage;
        return true;
    }
    this.setProductsPerPageSelected = function(iProductsPerPageSelected)
    {
        if(utilsIsObjectNullOrUndefined(iProductsPerPageSelected))
            return false //TODO throw error
        this.m_iProductsPerPageSelected = iProductsPerPageSelected;
        return true;
    }
    this.setProductList = function(aoProductList)
    {
        if(utilsIsObjectNullOrUndefined(aoProductList))
            return false //TODO throw error
        this.m_aoProductList = aoProductList;
        return true;
    }
    this.setTotalPages = function(iTotalPages)
    {
        if(utilsIsObjectNullOrUndefined(iTotalPages))
            return false //TODO throw error
        this.m_iTotalPages = iTotalPages;
        return true;
    }

    this.setIsVisibleListOfProducts = function(bIsVisibleListOfProducts)
    {
        if(utilsIsObjectNullOrUndefined(bIsVisibleListOfProducts))
            return false;//TODO throw error
        this.m_bIsVisibleListOfProducts = bIsVisibleListOfProducts;
        return true;
    }

    /************************ Get methods ***************************/
    this.getTextQuery = function()
    {
        return this.m_sTextQuery;
    }
    this.getGeoSelection = function()
    {
        return this.m_oGeoSelection;
    }
    this.getCurrentPage = function()
    {
        return this.m_iCurrentPage;
    }
    this.getProductsPerPageSelected = function()
    {
        return this.m_iProductsPerPageSelected ;
    }
    this.getProductList = function()
    {
        return this.m_aoProductList;
    }
    this.getTotalPages = function()
    {
        return this.m_iTotalPages;
    }
    this.setIsVisibleListOfProducts = function()
    {
        return this.m_bIsVisibleListOfProducts;
    }

}]);