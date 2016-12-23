/**
 * Created by a.corrado on 30/11/2016.
 */

var ImportController = (function() {

    function ImportController($scope, oConstantsService, oAuthService,$state,oMapService, oSearchService, oAdvancedFilterService, oAdvancedSearchService, oConfigurationService ) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oConstantsService = oConstantsService;
        this.m_oAuthService = oAuthService;
        this.m_oState=$state;
        this.m_oScope.m_oController=this;
        this.m_oMapService=oMapService;
        this.m_oSearchService = oSearchService;
        this.m_oAdvancedFilterService = oAdvancedFilterService;
        this.m_oAdvancedSearchService = oAdvancedSearchService;
        this.m_oConfigurationService = oConfigurationService;
        this.m_bShowsensingfilter = true;
        this.m_bPproductsPerPagePristine   = true;
        this.m_iProductCount = 0;
        this.m_bDisableField = true;
        this.m_aiAddedIds=[];
        this.m_aiRemovedIds=[];
        this.m_oDetails = {};
        this.m_oDetails.productIds = [];
        this.m_oScope.selectedAll = false;
        this.m_bDisableField = true;
        this.m_sFilter='';
        this.m_aoProducts = [];
        this.m_oScope.currentPage = 1;
        this.m_oConfiguration = null;
        this.m_oStatus = {
            opened: false
        };
        this.m_bIsOpen=true;
        this.m_bIsVisibleListOfLayers = false;
        this.m_oMapService.initMapWithDrawSearch('wasdiMapImport');
        this.m_aoLayersList= [];
        this.m_aoMissions;
        this.m_oModel = {
            textQuery:'',
            list: '',
            geoselection: '',
            offset: 0,
            pagesize: 25,
            advancedFilter: '',
            missionFilter: '',
            doneRequest: '',
            sensingPeriodFrom: '',
            sensingPeriodTo: '',
            ingestionFrom: '',
            ingestionTo: ''
        };
        //if user is logged
        //if(!utilsIsObjectNullOrUndefined(this.m_oConstantsService.getUser()))
        //    this.m_oUser = this.m_oConstantsService.getUser();
        //else
        //    this.m_oState.go("login");
        this.m_oUser = this.m_oConstantsService.getUser();
        var oController = this;
        //get configuration
        this.m_oConfigurationService.getConfiguration().then(function(configuration){
            oController.m_oConfiguration = configuration;
            oController.m_aoMissions = oController.m_oConfiguration.missions;
            oController.m_bShowsensingfilter = oController.m_oConfiguration.settings.showsensingfilter;
        });



        this.m_DatePickerPosition = function($event){
            /*
            var element = document.getElementsByClassName("dropdown-menu");
            setTimeout(function(){
                var element = document.getElementsByClassName("dropdown-menu");
                element[0].style.visibility =  'hidden';
                var top = 0;
                var DATEPICKER_HEIGHT= 280;
                if(($event.originalEvent.pageY + DATEPICKER_HEIGHT ) > window.innerHeight ){
                    top = parseInt(window.innerHeight  - DATEPICKER_HEIGHT ) + "px" ;
                }
                else{
                    top = $event.originalEvent.pageY + "px";
                }
                element[0].style.top =  top;
                var left = parseInt($event.originalEvent.pageX) - parseInt(element[0].offsetWidth) ;
                element[0].style.left = ((left >0)?left:10) + "px ";
                element[0].style.visibility =  'visible';
            },100);
            */
        };


        this.m_fUtcDateConverter = function(date){
            var result = date;
            if(date != undefined) {
                var day =  moment(date).get('date');
                var month = moment(date).get('month');
                var year = moment(date).get('year');
                var utcDate = moment(year+ "-" + (parseInt(month)+1) +"-" +day+ " 00:00 +0000", "YYYY-MM-DD HH:mm Z"); // parsed as 4:30 UTC
                result =  utcDate;
            }

            return result;
        };

        //vex.dialog.confirm({
        //    message: 'Are you absolutely sure you want to destroy the alien planet?',
        //    callback: function (value) {
        //        if (value) {
        //            console.log('Successfully destroyed the planet.')
        //        } else {
        //            console.log('Chicken.')
        //        }
        //    }
        //})
    }
    /***************** METHODS ***************/
    //OPEN LEFT NAV-BAR
    ImportController.prototype.getMissions= function() {
        return this.m_aoMissions;
    }

    //OPEN LEFT NAV-BAR
    ImportController.prototype.openNav= function() {
        document.getElementById("mySidenav").style.width = "30%";
    }
    //CLOSE LEFT NAV-BAR
    ImportController.prototype.closeNav=function() {
        document.getElementById("mySidenav").style.width = "0";
    }

    //ALTERNATIVE METHODS
    ImportController.prototype.openCloseNav=function()
    {
        var oController=this;
        if(oController.m_bIsOpen == true)
        {
            oController.closeNav();
            oController.m_bIsOpen = false;
        }
        else{
            if(oController.m_bIsOpen == false)
            {
                oController.openNav();
                oController.m_bIsOpen=true;
            }
        }
    };

    ImportController.prototype.updateFilter = function(parentIndex){

        //console.log("parentIndex",parentIndex);
        var selected = false;
        for(var i=0; i<this.m_aoMissions[parentIndex].filters.length; i++) {
            if(this.m_aoMissions[parentIndex].filters[i].indexvalue &&
                this.m_aoMissions[parentIndex].filters[i].indexvalue.trim() != '') {
                selected=true;
                break;
            }
        }
        this.m_aoMissions[parentIndex].selected=selected;
        this.setFilter();
        //console.log("filter",AdvancedFilterService.getAdvancedFilter());

    };

    ImportController.prototype.updateAdvancedSearch = function(){
        //console.log('called updatefilter advancedFilter');
        var advancedFilter = {
            sensingPeriodFrom : this.m_fUtcDateConverter(this.m_oModel.sensingPeriodFrom),
            sensingPeriodTo: this.m_fUtcDateConverter(this.m_oModel.sensingPeriodTo),
            ingestionFrom: this.m_fUtcDateConverter(this.m_oModel.ingestionFrom),
            ingestionTo: this.m_fUtcDateConverter(this.m_oModel.ingestionTo)
        };


        // update advanced filter for save search
        this.m_oAdvancedSearchService.setAdvancedSearchFilter(advancedFilter, this.m_oModel);
        this.m_oSearchService.setAdvancedFilter(this.m_oAdvancedSearchService.getAdvancedSearchFilter());

    };

    ImportController.prototype.updateMissionSelection = function(index){

        if(!this.m_aoMissions[index].selected) {
            for(var i=0; i<this.m_aoMissions[index].filters.length; i++) {
                this.m_aoMissions[index].filters[i].indexvalue = '';
            }
        }
        this.setFilter();
        //console.log("filter",AdvancedFilterService.getAdvancedFilter());

    };

    ImportController.prototype.setFilter = function() {
        this.m_oAdvancedFilterService.setAdvancedFilter(this.m_aoMissions);
        this.m_oModel.missionFilter = this.m_oAdvancedFilterService.getAdvancedFilter();
        this.m_oSearchService.setMissionFilter(this.m_oModel.missionFilter);
    };

    ImportController.prototype.clearInput = function(parentIndex, index){
        //console.log("parentIndex    clearFilter",parentIndex);


        if(this.m_aoMissions[parentIndex] && this.m_aoMissions[parentIndex].filters[index]) {
            this.m_aoMissions[parentIndex].filters[index].indexvalue = "";
        }
    };

    ImportController.prototype.search = function() {
        var oController = this;
        this.m_bIsVisibleListOfLayers = true;//change view on left side bar
        this.m_oSearchService.setTextQuery(this.m_oModel.textQuery);
        this.m_oSearchService.setGeoselection(this.m_oModel.geoselection); // todo: refactor setting by map
        // SearchService.setAdvancedFilter(scope.model.advancedFilter);
        this.m_oSearchService.setOffset(0);
        this.m_oSearchService.getProductsCount().then(function(result){

            var iCount = result;

            oController.m_oSearchService.search().then(function(result){
                var sResults = result;

                if(!utilsIsObjectNullOrUndefined(sResults))
                {
                    if(!utilsIsObjectNullOrUndefined(sResults.data))
                    {
                        var aoData = JSON.parse(sResults.data);
                        oController.generateLayersList(aoData.feed);
                    }
                }
            });
        });

    };

    ImportController.prototype.clearFilter = function() {
        for(var i=0; i<this.m_aoMissions.length; i++)
        {
            this.m_aoMissions[i].selected = false;
            this.updateMissionSelection(i);
        }
        this.setFilter();
    }

    ImportController.prototype.openSensingPeriodFrom = function($event) {
        this.m_oStatus.openedSensingPeriodFrom = true;
        this.m_DatePickerPosition($event);
    };
    ImportController.prototype.openSensingPeriodTo = function($event) {
        this.m_oStatus.openedSensingPeriodTo = true;
        this.m_DatePickerPosition($event);
    };
    ImportController.prototype.openIngestionFrom = function($event) {
        this.m_oStatus.openedIngestionFrom = true;
        this.m_DatePickerPosition($event);
    };
    ImportController.prototype.openIngestionTo = function($event) {
        this.m_oStatus.openedIngestionTo = true;
        this.m_DatePickerPosition($event);
    };
    ImportController.prototype.disabled = function(date, mode) {
        return false;
    };

    ImportController.prototype.checkUncheckAll = function() {
        var oController = this;
        this.m_oDetails.productIds=[];
        this.m_aiAddedIds = [];
        this.m_aiRemovedIds = [];
        this.m_oScope.selectedAll = !this.m_oScope.selectedAll;
        this.m_oSearchService
            .getAllCollectionProducts(this.m_sFilter , this.m_oScope.offset, this.m_iProductCount)
            .then(function(){
                angular.forEach(oController.m_oSearchService.collectionAllProductsModel.list, function (item) {
                    //item.selected = scope.selectedAll;
                    if(oController.m_oScope.selectedAll) {
                        oController.m_oDetails.productIds.push(item.id);
                        oController.m_aiAddedIds.push(item.id);
                    }
                    else {
                        oController.m_aiRemovedIds.push(item.id);
                    }
                    //console.warn('item.selected',item.selected);
                });
            });
    };

    ImportController.prototype.goToPage = function(pageNumber,free){
        var oController = this;
        if((pageNumber <= this.m_oScope.pageCount && pageNumber > 0) || free){

            this.m_oScope.currentPage = pageNumber;
            this.m_oScope.offset=(pageNumber * this.m_oScope.productsPerPage) - this.m_oScope.productsPerPage;
            return SearchService
                .getCollectionProductsList(this.m_sFilter, this.m_oScope.offset, this.m_oScope.productsPerPage)
                .then(function(){
                    oController.m_aiAddedIds=[];
                    oController.m_aiRemovedIds=[];
                    oController.refreshCounters();
                    oController.m_oScope.currentPageCache = pageNumber;
                    oController.m_oScope.currentPage = pageNumber;
                    oController.m_aoProducts = oController.m_oSearchService.collectionProductsModel.list;

                    if(oController.m_oDetails && oController.m_oDetails.productIds && oController.m_oDetails.productIds.length == oController.m_iProductCount) {
                        $("#product-list-all-checkbox").prop('checked', true);
                        oController.m_oScope.selectedAll=true;
                    }
                    else {
                        oController.m_oScope.selectedAll=false;
                        $("#product-list-all-checkbox").prop('checked', false);
                    }

                });
        }else{
            var deferred = $q.defer();
            return deferred.promise;
        }
    };

    ImportController.prototype.refreshCounters = function(){
        this.m_oScope.productCount = this.m_oSearchService.collectionProductsModel.count;
        this.m_oScope.pageCount =  Math.floor(this.m_oSearchService.collectionProductsModel.count / this.m_oScope.productsPerPage) + ((this.m_oSearchService.collectionProductsModel.count % this.m_oScope.productsPerPage)?1:0);
        this.m_oScope.visualizedProductsFrom    = (this.m_oSearchService.collectionProductsModel.count) ? this.m_oScope.offset + 1:0;
        this.m_oScope.visualizedProductsTo      =
            (((this.m_oSearchService.collectionProductsModel.count)?
                (this.m_oScope.currentPage * this.m_oScope.productsPerPage):1)> this.m_iProductCount)
                ?this.m_iProductCount
                :((this.m_oSearchService.collectionProductsModel.count)
                ?(this.m_oScope.currentPage * this.m_oScope.productsPerPage)
                :1);
    };

    ImportController.prototype.isChecked = function(product) {
        if(this.m_oDetails && this.m_oDetails.productIds && _.indexOf(this.m_oDetails.productIds,product.id)>=0) {
            //console.error('id found: ',product.id);
            //product.selected=true;
            return true;
        }
        else {
            //console.error('id NOT found: ',product.id);
            //product.selected=false;
            return false;
        }
    };

    ImportController.prototype.addRemoveProduct = function(product) {
        //console.log('addRemoveProduct',product);
        //console.log("isChecked", scope.isChecked(product));
        if(this.isChecked(product)) {
            var index = his.m_oDetails.productIds.indexOf(product.id);
            if(index>=0)
                this.m_oDetails.productIds.splice(index,1);
            index = this.m_aiAddedIds.indexOf(product.id);
            if(index>=0)
                this.m_aiAddedIds.splice(index,1);
            this.m_aiRemovedIds.push(product.id);
        }
        else {
            this.m_aiAddedIds.push(product.id);
            if(!this.m_oDetails.productIds)
                this.m_oDetails.productIds=[];
            this.m_oDetails.productIds.push(product.id);
            var index = this.m_aiRemovedIds.indexOf(product.id);
            if(index>=0)
                this.m_aiRemovedIds.splice(index,1);
        }
    };

    ImportController.prototype.updateValue = function(){
        if(this.m_bPproductsPerPagePristine){
            this.m_bPproductsPerPagePristine = false;
            return;
        }
        this.goToPage(1, true);

    };

    ImportController.prototype.gotoFirstPage = function(){
        this.goToPage(1, false);
    };

    ImportController.prototype.gotoPreviousPage = function(){
        this.goToPage(this.m_oScope.currentPageCache - 1, false);
    };

    ImportController.prototype.gotoNextPage = function() {
        this.goToPage(this.m_oScope.currentPageCache + 1, false);
    };

    ImportController.prototype.gotoLastPage = function(){
        this.goToPage(this.m_oScope.pageCount, false);
    };

    ImportController.prototype.selectPageDidClicked = function(xx){

    };



    /*
    * Generate layers list
    * */

    ImportController.prototype.generateLayersList=function(aData)
    {
        var oController = this;
        //TODO TEST EXAMPLE
        //var aaBounds=[
        //                [[57.559322, -8.767822], [59.1210604, -6.021240]],
        //                [[54.559322, -5.767822], [56.1210604, -3.021240]],
        //                [[56.559322, -7.767822], [58.1210604, -5.021240]],
        //                [[55.559322, -6.767822], [57.1210604, -4.021240]]
        //                ];
        var aoLayers =  aData.entry;
        if(utilsIsObjectNullOrUndefined(aoLayers))
            var iLength = 0;
        else
            var iLength = aoLayers.length;

        for(var iIndexLayers = 0; iIndexLayers < iLength; iIndexLayers++)
        {
           // var oRectangle = oController.m_oMapService.addRectangleOnMap(aaBounds[iIndexLayers][0],aaBounds[iIndexLayers][1],null,iIndexLayers);
            var oRectangle = null;
            //var oSummary = aoLayers[iIndexLayers].summary.content;//take summary of layer

            //var aSplit = aoLayers[iIndexLayers].summary.content.split(",");
            var oSummary = this.stringToObjectSummary(aoLayers[iIndexLayers].summary.content);
            oController.m_aoLayersList.push({layerProperty:aoLayers[iIndexLayers],summary:oSummary,id:aoLayers[iIndexLayers].id, rectangle:oRectangle});
        }

    }

    /*
        Usually the summary format is a string = "date:...,instrument:...,mode:...,satellite:...,size:...";
     */
    ImportController.prototype.stringToObjectSummary = function(sSummary)
    {
        if(utilsIsObjectNullOrUndefined(sSummary))
            return null;
        if(utilsIsStrNullOrEmpty(sSummary))
            return null;

        var aSplit = sSummary.split(",");//split summary
        var oNewSummary = {Date:"",Instrument:"",Mode:"",Satellite:"",Size:""}//make object summary
        var asSummary = ["Date","Instrument","Mode","Satellite","Size"]
        var iSplitLength=aSplit.length;
        var iSummaryLength = asSummary.length;

        /* it dosen't know if date,instrument,mode...are the first element,second element,... of aSplit array
        * we fix it with this code
        * */
        for(var iIndex = 0; iIndex < iSplitLength; iIndex++ )
        {
            for(var jIndex = 0; jIndex < iSummaryLength;jIndex++ )
            {
                if(utilsStrContainsCaseInsensitive(aSplit[iIndex],asSummary[jIndex]))
                {
                    oNewSummary[asSummary[jIndex].replace(":","")] = aSplit[iIndex].replace(asSummary[jIndex]+":","");
                    break;
                }
            }
        }

        return oNewSummary;
    }
    /*********************** CHANGE CSS RECTANGLE (LEAFLET MAP) ****************************/
    /*
    *   Change style of rectangle when the mouse is over the layer (TABLE CASE)
    * */
    ImportController.prototype.changeStyleRectangleMouseOver=function(oRectangle)
    {
        if(utilsIsObjectNullOrUndefined(oRectangle))
        {
            console.log("Error: rectangle is undefined ");
            return false;
        }
        oRectangle.setStyle({weight:3,fillOpacity:0.7});
    }
    /*
     *   Change style of rectangle when the mouse is leave the layer (TABLE CASE)
     * */
    ImportController.prototype.changeStyleRectangleMouseLeave=function(oRectangle)
    {
        if(utilsIsObjectNullOrUndefined(oRectangle))
        {
            console.log("Error: rectangle is undefined ");
            return false;
        }
        oRectangle.setStyle({weight:1,fillOpacity:0.2});
    }
    /************************************************************/

    /* the method take in input a rectangle, return the layer index Bound with rectangle
    * */
    ImportController.prototype.indexOfRectangleInLayersList=function(oRectangle) {
        var oController = this;
        if (utilsIsObjectNullOrUndefined(oController.m_aoLayersList)) {
            console.log("Error LayerList is empty");
            return false;
        }
        var iLengthLayersList = oController.m_aoLayersList.length;

        for (var iIndex = 0; iIndex < iLengthLayersList; iIndex++)
        {
            if(oController.m_aoLayersList[iIndex].rectangle == oRectangle)
                return iIndex; // I FIND IT !!
        }
        return -1;//the method doesn't find the Rectangle in LayersList.
    }

    // Set bounds then call m_oMapService.zoomOnBounds(aaBounds)
    ImportController.prototype.zoomOnBounds = function(oRectangle)
    {
        var oBounds = oRectangle.getBounds();
        var oNorthEast = oBounds.getNorthEast();
        var oSouthWest = oBounds.getSouthWest();

        if(utilsIsObjectNullOrUndefined(oNorthEast) || utilsIsObjectNullOrUndefined(oSouthWest))
        {
            console.log("Error in zoom on bounds");
        }
        else
        {
            var aaBounds = [[oNorthEast.lat,oNorthEast.lng],[oSouthWest.lat,oSouthWest.lng]];
            var oController = this;
            if(oController.m_oMapService.zoomOnBounds(aaBounds) == false)
                console.log("Error in zoom on bounds");
        }
    }

    ImportController.$inject = [
        '$scope',
        'ConstantsService',
        'AuthService',
        '$state',
        'MapService',
        'SearchService',
        'AdvancedFilterService',
        'AdvancedSearchService',
        'ConfigurationService'

    ];

    return ImportController;
}) ();
