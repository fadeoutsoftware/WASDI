/*
 * Data HUb Service (DHuS) - For Space data distribution.
 * Copyright (C) 2013,2014,2015,2016 European Space Agency (ESA)
 * Copyright (C) 2013,2014,2015,2016 GAEL Systems
 * Copyright (C) 2013,2014,2015,2016 Serco Spa
 *
 * This file is part of DHuS software sources.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
'use strict';
angular
    .module('wasdi')
    .factory('SearchService', function ($http, $injector) {

        var ConstantsService = $injector.get('ConstantsService');
        var OpenSearchService = $injector.get('OpenSearchService');
        return {
            providers:'',
            textQuery: '',
            geoselection: '',
            advancedFilter: '',
            missionFilter: '',
            offset: 0,
            limit: 25,
            filterContext: {
                doneRequest: '',
                sensingPeriodFrom: '',
                sensingPeriodTo: '',
                ingestionFrom: '',
                ingestionTo: ''
            },
            collectionProductsModel: {list: [], count: 0},
            collectionAllProductsModel: {list: []},

            goToPage: function () {

            },
            setProviders:function(sProvidersInput){
                var iLengthProviders = sProvidersInput.length;

                var sProviderSelected = '';
                for(var iIndexProvider = 0; iIndexProvider < iLengthProviders; iIndexProvider++)
                {
                    if((utilsIsObjectNullOrUndefined(sProvidersInput[iIndexProvider]) === false) && (sProvidersInput[iIndexProvider].selected == true )  )
                        sProviderSelected += sProvidersInput[iIndexProvider].name + ',';
                }
                if( (sProviderSelected.length-1) < 0)
                    this.providers = '';
                else
                {
                    this.providers = sProviderSelected.substring(0,sProviderSelected.length-1);//remove last letter == ,
                    this.providers = this.providers.toUpperCase();
                }
            },
            setTextQuery: function (textQuery) {
                this.textQuery = textQuery;
            },
            setGeoselection: function (geoselection) {
                this.geoselection = geoselection;
            },
            setAdvancedFilter: function (advancedFilter) {
                this.advancedFilter = advancedFilter;
            },
            setMissionFilter: function (missionFilter) {
                this.missionFilter = missionFilter;
            },
            setOffset: function (offset) {
                this.offset = offset;
            },
            setLimit: function (limit) {
                this.limit = limit;
            },
            getGeoQueryByCoordsOld: function (coords) {
                if (!coords) return;
                var query = "(footprint:\"Intersects(POLYGON((";
                for (var i = 0; i < coords.length; i++) query += coords[i].lon + " " + coords[i].lat + ((i != (coords.length - 1)) ? "," : "");
                query += ")))\")";
                return query;
            },
            createSearchRequest: function (filter, offset, limit,providers) {
                var searchUrl = ":filter&offset=:offset&limit=:limit&providers=:providers";
                searchUrl = searchUrl.replace(":filter", (filter) ? filter : '*');
                searchUrl = searchUrl.replace(":offset", (offset) ? offset : '0');
                searchUrl = searchUrl.replace(":limit", (limit) ? limit : '10');
                searchUrl = searchUrl.replace(":providers", (providers) ? providers : '');

                this.doneRequest = filter;
                return searchUrl;
            },
            createSearchFilter: function (textQuery, geoselection, advancedFilter, missionFilter) {
                var searchFilter = '';
                if (textQuery) searchFilter += textQuery;
                if (geoselection) searchFilter += ((textQuery) ? ' AND ' : '') + this.getGeoQueryByCoords(geoselection);
                if (advancedFilter) searchFilter += ((textQuery || geoselection) ? ' AND ' : '') + advancedFilter;
                if (missionFilter) searchFilter += ((textQuery || geoselection || advancedFilter) ? ' AND ' : '') + missionFilter;
                return searchFilter;
            },
            saveUserSearch: function (textQuery, geoselection, advancedFilter, missionFilter) {
                var filter = '';
                filter = this.createSearchFilter(textQuery, geoselection, advancedFilter, missionFilter);
                var saveSearchUrl = 'api/stub/users/0/searches?complete=:complete';
                saveSearchUrl = saveSearchUrl.replace(":complete", (filter) ? filter : '*');
                return http({url: ConstantsService.getUrl() + saveSearchUrl, method: "POST"})
                    .then(function (response) {
                        return (response.status == 200) ? response.data : [];
                    });
            },
            clearSearchInput: function () {
            },
            setClearSearchInput: function (method) {
                this.clearSearchInput = method;
            },
            search: function (query) {
                var self = this;
                //console.log('called search function');
                var filter = '';
                if (query)
                    filter = self.createSearchFilter(query, '', '', '');
                else
                    filter = self.createSearchFilter(self.textQuery, self.geoselection,
                        self.advancedFilter, self.missionFilter);
                //console.log('filter xx',filter);
                return $http({
                    // url: OpenSearchService.getApiProducts(self.createSearchRequest(filter, self.offset, self.limit)),
                    url: OpenSearchService.getApiProductsWithProviders(self.createSearchRequest(filter, self.offset, self.limit,self.providers)),
                    method: "GET"
                });
            },
            /**
             * Light version of search for multi-periods and long result list
             * @param asTimeQueries Array of string of time filters to apply
             * @returns {*}
             */
            searchList: function (asTimeQueries) {

                if (utilsIsObjectNullOrUndefined(asTimeQueries)) asTimeQueries = [];
                // Auto reference
                var self = this;
                // Array of filters to pass to the server
                var asFilters = [];

                // If there aren't advanced periods
                if (asTimeQueries.length==0) {
                    // Use standard dates
                    var filter = self.createSearchFilter(self.textQuery, self.geoselection, self.advancedFilter, self.missionFilter);
                    asFilters.push(filter);
                }
                else {
                    // Put all the time filters in the array
                    for (var iPeriods = 0; iPeriods<asTimeQueries.length; iPeriods++) {
                        var filter = self.createSearchFilter(self.textQuery, self.geoselection, asTimeQueries[iPeriods], self.missionFilter);
                        asFilters.push(filter);
                    }
                }

                // Call the API with the list of queries
                return $http.post(OpenSearchService.getApiProductsListWithProviders(self.providers), asFilters);
            },
            getProductsListCount: function (asTimeQueries) {

                if (utilsIsObjectNullOrUndefined(asTimeQueries)) asTimeQueries = [];
                // Auto reference
                var self = this;
                // Array of filters to pass to the server
                var asFilters = [];

                // If there aren't advanced periods
                if (asTimeQueries.length==0) {
                    // Use standard dates
                    var filter = self.createSearchFilter(self.textQuery, self.geoselection, self.advancedFilter, self.missionFilter);
                    asFilters.push(filter);
                }
                else {
                    // Put all the time filters in the array
                    for (var iPeriods = 0; iPeriods<asTimeQueries.length; iPeriods++) {
                        var filter = self.createSearchFilter(self.textQuery, self.geoselection, asTimeQueries[iPeriods], self.missionFilter);
                        asFilters.push(filter);
                    }
                }
                return $http.post(OpenSearchService.getApiProductListCountWithProviders(this.providers), asFilters);
            },
            gotoPage: function (pageNumber) {
                this.setOffset((pageNumber * this.limit) - this.limit);
                //console.log("goto");
                return this.search();
            },
            getProductsCount: function (query) {
                var filter = '', self = this;
                if (query)
                    filter = this.createSearchFilter(query, '', '', '');
                else
                    filter = this.createSearchFilter(self.textQuery, self.geoselection, self.advancedFilter, self.missionFilter);
                //console.log('filter prodcount',filter);
                var prodCountUrl = ':filter';
                prodCountUrl = prodCountUrl.replace(":filter", (filter) ? filter : '*');
               // return $http({url: OpenSearchService.getApiProductsCount(prodCountUrl), method: "GET"});
                return $http({url: OpenSearchService.getApiProductCountWithProviders(prodCountUrl,this.providers), method: "GET"});

            },
            getSuggestions: function (query) {
                var suggesturl = 'api/search/suggest/' + query;
                return http({url: ConstantsService.getURL() + suggesturl, method: "GET"})
                    .then(function (response) {
                        return (response.status == 200) ? response.data : [];
                    });
            },
            getGeoQueryByCoords: function (query) {
                return query;
            },
            getCollectionProductsList: function (query, offset, limit) {
                var self = this;
                //console.log('called search function');
                return self.getProductsCount(query)
                    .then(function (totalCount) {
                        self.collectionProductsModel.count = totalCount;

                        return $http({
                            url: ConstantsService.getURL() + self.createSearchRequest(query, offset, limit),
                            method: "GET"
                        })
                            .then(function (result) {
                                self.collectionProductsModel.list = result.data;

                            });
                    });
            },
            getAllCollectionProducts: function (query, offset, limit) {
                var self = this;
                //console.log('called search function');
                return $http({
                    url: ConstantsService.getURL() + self.createSearchRequest(query, offset, limit),
                    method: "GET"
                })
                    .then(function (result) {
                        self.collectionAllProductsModel.list = result.data;

                    });
            },
        };
    });


