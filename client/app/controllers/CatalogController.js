/**
 * Created by a.corrado on 30/11/2016.
 */

var CatalogController = (function() {

    function CatalogController($scope, oConstantsService, oAuthService,$state,oCatalogService,oMapService,oModalService )
    {
        // Service referenc
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oConstantsService = oConstantsService;
        this.m_oAuthService = oAuthService;
        this.m_oState = $state;
        this.m_oMapService = oMapService;
        this.m_oCatalogService = oCatalogService;
        this.m_oModalService = oModalService;

        // File Types
        this.m_asCategories = [];
        // Free text query
        this.m_sInputQuery = "";
        // Start Date
        this.m_oDataFrom="";
        // End Date
        this.m_oDataTo = "";
        // Found Entries
        this.m_aoEntries = [];
        // Flag to know if the table was loaded
        this.m_bIsLoadedTable = true;
        // Result grid order column
        this.m_sOrderBy = 'fileName';
        // Result grid order direction
        this.m_bReverseOrder = false;

        this.m_oMapService.initMap('catalogMap');
        this.GetCategories();
        this.setDefaultData();

        // Select a row on the over rectangle event
        $scope.$on('on-mouse-over-rectangle', function(event, args) {

            if (utilsIsObjectNullOrUndefined(args.rectangle)) return;
            var oRectangle = args.rectangle;

            if(!utilsIsObjectNullOrUndefined(oRectangle))
            {
                var iLengthLayersList = 0;

                if(!utilsIsObjectNullOrUndefined($scope.m_oController.m_aoEntries.length)) iLengthLayersList = $scope.m_oController.m_aoEntries.length;

                for(var iIndex = 0; iIndex < iLengthLayersList; iIndex++)
                {
                    if($scope.m_oController.m_aoEntries[iIndex].rectangle == oRectangle)
                    {
                        var sId = "layer"+iIndex;
                        //change css of table
                        jQuery("#"+sId).css({"border-top": "2px solid green", "border-bottom": "2px solid green"});
                    }
                }

            }
        });

        /*When mouse leaves rectangle layer (change css)*/
        $scope.$on('on-mouse-leave-rectangle', function(event, args) {

            if (utilsIsObjectNullOrUndefined(args.rectangle)) return;

            var oRectangle = args.rectangle;
            if(!utilsIsObjectNullOrUndefined(oRectangle))
            {
                var iLengthLayersList = 0;
                if(!utilsIsObjectNullOrUndefined($scope.m_oController.m_aoEntries.length)) iLengthLayersList = $scope.m_oController.m_aoEntries.length;

                for(var iIndex = 0; iIndex < iLengthLayersList; iIndex++)
                {
                    if($scope.m_oController.m_aoEntries[iIndex].rectangle == oRectangle)
                    {
                        var sId = "layer"+iIndex;
                        //return default css of table
                        jQuery("#"+sId).css({"border-top": "", "border-bottom": ""});
                    }
                }
            }

        });

        /*When rectangle was clicked (change focus on table)*/
        $scope.$on('on-mouse-click-rectangle', function(event, args) {

            if (utilsIsObjectNullOrUndefined(args.rectangle)) return;
            var oRectangle = args.rectangle;

            if(!utilsIsObjectNullOrUndefined(oRectangle))
            {
                var iLengthLayersList = 0;
                if (!utilsIsObjectNullOrUndefined($scope.m_oController.m_aoEntries))iLengthLayersList = $scope.m_oController.m_aoEntries.length;

                for(var iIndex = 0; iIndex < iLengthLayersList; iIndex++)
                {
                    if($scope.m_oController.m_aoEntries[iIndex].rectangle == oRectangle)
                    {
                        var sId = "layer"+iIndex;

                        /* change view on table , when the pointer of mouse is on a rectangle the table scroll, the row will become visible(if it isn't) */
                        var container = $('#div-container-table'), scrollTo = $('#'+sId);

                        //http://stackoverflow.com/questions/2905867/how-to-scroll-to-specific-item-using-jquery
                        container.animate({
                            scrollTop: scrollTo.offset().top - container.offset().top + container.scrollTop()
                        });
                    }
                }
            }
        });
    }

    /**
     * Reset query input parameters
     */
    CatalogController.prototype.setDefaultData = function(){
        // Set Till today
        var oToDate = new Date();
        this.m_oDataTo = oToDate;
        // From last year
        var oFromDate = new Date();
        var dayOfMonth = oFromDate.getDate();
        oFromDate.setDate(dayOfMonth - 365);
        this.m_oDataFrom = oFromDate;
    };

    /**
     * Fetch the categories from the server
     * @constructor
     */
    CatalogController.prototype.GetCategories = function()
    {
        var oController = this;
        this.m_oCatalogService.getCategories().success(function (data) {
            if(utilsIsObjectNullOrUndefined(data) == false)
            {
                if(data.length > 0)
                {
                    var iNumberOfCategories = data.length;
                    for(var iIndexCategory = 0;iIndexCategory < iNumberOfCategories; iIndexCategory++)
                    {
                        var isSelected = false;
                        if(iIndexCategory === 0) isSelected = true;
                        var oCategory = {
                            name:data[iIndexCategory],
                            isSelected:isSelected
                        };
                        oController.m_asCategories.push(oCategory);
                    }
                }
            }
            else
            {
            }
        }).error(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN GET CATEGORIES");
        });
    };

    /**
     * Make the real query
     */
    CatalogController.prototype.searchEntries = function()
    {
        var sFrom="";
        var sTo="";
        if(utilsIsStrNullOrEmpty(this.m_oDataFrom) === false) sFrom = this.m_oDataFrom.replace(/-/g,'');
        if(utilsIsStrNullOrEmpty(this.m_oDataTo) === false) sTo =  this.m_oDataTo.replace(/-/g,'');

        var sFreeText = this.m_sInputQuery;
        var sCategory = this.getSelectedCategoriesAsString() ;
        var oController = this;
        this.m_bIsLoadedTable = false;
        sFrom += "00";
        sTo += "00";

        this.m_oCatalogService.getEntries(sFrom,sTo,sFreeText,sCategory).success(function (data) {
            if(utilsIsObjectNullOrUndefined(data) == false)
            {
                oController.m_aoEntries = data;
                oController.drawEntriesBoundariesInMap();
            }
            oController.m_bIsLoadedTable = true;
        }).error(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR SEARCHING THE CATALOGUE");
            oController.m_bIsLoadedTable = true;
        });
    };

    /**
     * Download of a product
     * @param oEntry
     * @returns {boolean}
     */
    CatalogController.prototype.downloadEntry = function(oEntry)
    {
        if(utilsIsObjectNullOrUndefined(oEntry)) return false;
        var oJson = {
            fileName: oEntry.fileName,
            filePath: oEntry.filePath
        };

        var sFileName = oEntry.fileName;
        this.m_oCatalogService.downloadEntry(oJson).success(function (data, status, headers, config) {
            if(utilsIsObjectNullOrUndefined(data) == false)
            {
                //var FileSaver = require('file-saver');
                var blob = new Blob([data], {type: "application/octet-stream"});
                saveAs(blob, sFileName);
            }
        }).error(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR DOWNLOADING FILE FROM THE CATALOGUE");
        });

        return true;
    };

    /**
     * Get the selected category
     * @returns {string}
     */
    CatalogController.prototype.getSelectedCategoriesAsString = function()
    {
        if(utilsIsObjectNullOrUndefined(this.m_asCategories) === true ) return "";
        var iNumberOfSelectedCategories = this.m_asCategories.length;
        var sReturnValue = "";
        for(var iIndexCategory = 0 ; iIndexCategory < iNumberOfSelectedCategories; iIndexCategory++)
        {
            if(this.m_asCategories[iIndexCategory].isSelected === true)
            {
                sReturnValue = sReturnValue +  this.m_asCategories[iIndexCategory].name + ",";
            }

        }
        //remove last ,
        if(utilsIsStrNullOrEmpty(sReturnValue) === false) sReturnValue = sReturnValue.substring(0, sReturnValue.length - 1);

        return sReturnValue;
    };




    /**
     * Draw all the boundaries on the map
     * @returns {boolean}
     */
    CatalogController.prototype.drawEntriesBoundariesInMap = function()
    {
        if(utilsIsObjectNullOrUndefined(this.m_aoEntries) === true) return false;

        var iNumberOfEntries = this.m_aoEntries.length;

        for( var iIndexEntry = 0; iIndexEntry < iNumberOfEntries; iIndexEntry++ )
        {
            var sBBox = this.m_aoEntries[iIndexEntry].boundingBox;
            if( (utilsIsObjectNullOrUndefined(sBBox) === false)&&(utilsIsStrNullOrEmpty(sBBox) === false) )
            {
                var oRectangle = this.m_oMapService.addRectangleOnMap(sBBox,"#ff7800","product"+iIndexEntry);
                this.m_aoEntries[iIndexEntry].rectangle = oRectangle;
            }

        }
        return true;
    };

    /**
     * Add a product to a specific workspace
     * @param oProduct
     */
    CatalogController.prototype.addProductToWorkspace = function(oProduct)
    {
        var oController=this;
        this.m_oModalService.showModal({
            templateUrl: "dialogs/add_product_catalog/AddProductView.html",
            controller: "AddProductInCatalogController",
            inputs: {
                extras: {
                    product:oProduct
                }
            }
        }).then(function (modal) {
            modal.element.modal();
            modal.close.then(function (result) {
                oController.m_oScope.Result = result;
            });
        });

    };

    /**
     * Get info about a product
     * @param oProduct
     */
    CatalogController.prototype.getInfoProduct = function(oProduct)
    {
        var oController=this;
        this.m_oModalService.showModal({
            templateUrl: "dialogs/get_info_product_catalog/GetInfoProductCatalog.html",
            controller: "GetInfoProductCatalogController",
            inputs: {
                extras: {
                    product:oProduct
                }
            }
        }).then(function (modal) {
            modal.element.modal();
            modal.close.then(function (result) {
                oController.m_oScope.Result = result;
            });
        });

    };

    /**
     * Handle change category event
     * @param oCategory
     */
    CatalogController.prototype.radioButtonOnCLick = function(oCategory){
        oCategory.isSelected = true;
        var iNumberOfCategories =  this.m_asCategories.length;

        for(var iIndexCategory = 0; iIndexCategory < iNumberOfCategories; iIndexCategory++)
        {
            if(this.m_asCategories[iIndexCategory].name !== oCategory.name)
            {
                this.m_asCategories[iIndexCategory].isSelected = false;
            }
        }
    };

    CatalogController.$inject = [
        '$scope',
        'ConstantsService',
        'AuthService',
        '$state',
        'CatalogService',
        'MapService',
        'ModalService'
    ];

    return CatalogController;
}) ();
