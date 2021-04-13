/**
 * Created by a.corrado on 30/11/2016.
 */

var CatalogController = (function() {

    function CatalogController($scope, oConstantsService, oAuthService,$state,oCatalogService,oMapService,oModalService,oProductService,oTranslate )
    {
        // Service reference
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oConstantsService = oConstantsService;
        this.m_oAuthService = oAuthService;
        this.m_oState = $state;
        this.m_oMapService = oMapService;
        this.m_oCatalogService = oCatalogService;
        this.m_oModalService = oModalService;
        this.m_oProductService = oProductService;
        this.m_oTranslate = oTranslate;

        //OPERATIONS FRIENDLY NAME
        this.m_sDonwloadFriendlyName = "";
        this.m_sComputedFriendlyName = "";
        this.m_sPublicFriendlyName = "";
        this.m_sIngestionFriendlyName = "";
        this.m_sSharedFriendlyName  = "";
        this.getTranslatedOperation();

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
        this.m_iNumberOfEntryRequest = 0;
        // Result grid order column
        this.m_sOrderBy = 'fileName';
        // Result grid order direction
        this.m_bReverseOrder = false;
        this.m_bIsDownloadingProduct = false;
        this.m_sProductNameInDownloadingStatus = "";
        this.m_oMapService.initWasdiMap('catalogMap');
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
        this.m_oCatalogService.getCategories().then(function (data) {
            if(utilsIsObjectNullOrUndefined(data.data) == false)
            {
                if(data.data.length > 0)
                {
                    var iNumberOfCategories = data.data.length;
                    for(var iIndexCategory = 0;iIndexCategory < iNumberOfCategories; iIndexCategory++)
                    {
                        var isSelected = false;
                        if(iIndexCategory === 0) isSelected = true;
                        var oCategory = {
                            name:data.data[iIndexCategory],
                            isSelected:isSelected,
                            friendlyName:oController.getFriendlyOperationName(data.data[iIndexCategory])
                        };
                        oController.m_asCategories.push(oCategory);
                    }
                }
            }
        },function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN GET CATEGORIES");
        });
    };

    /**
     * Make the real query
     */
    // CatalogController.prototype.searchEntries = function()
    // {
    //     var sFrom="";
    //     var sTo="";
    //     if(utilsIsStrNullOrEmpty(this.m_oDataFrom) === false) sFrom = this.m_oDataFrom.replace(/-/g,'');
    //     if(utilsIsStrNullOrEmpty(this.m_oDataTo) === false) sTo =  this.m_oDataTo.replace(/-/g,'');
    //
    //     var sFreeText = this.m_sInputQuery;
    //     var sCategory = this.getSelectedCategoriesAsString() ;
    //     var oController = this;
    //     this.m_bIsLoadedTable = false;
    //     sFrom += "00";
    //     sTo += "00";
    //
    //     this.m_oCatalogService.getEntries(sFrom,sTo,sFreeText,sCategory).then(function (data) {
    //         if(utilsIsObjectNullOrUndefined(data) == false)
    //         {
    //             oController.m_aoEntries = data;
    //             oController.drawEntriesBoundariesInMap();
    //         }
    //         oController.m_bIsLoadedTable = true;
    //     },function (error) {
    //         utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR SEARCHING THE CATALOGUE");
    //         oController.m_bIsLoadedTable = true;
    //     });
    // };
    /**
     *
     */
    CatalogController.prototype.searchEntries = function()
    {
        var sFrom="";
        var sTo="";
        if(utilsIsStrNullOrEmpty(this.m_oDataFrom) === false) sFrom = this.m_oDataFrom.replace(/-/g,'');
        if(utilsIsStrNullOrEmpty(this.m_oDataTo) === false) sTo =  this.m_oDataTo.replace(/-/g,'');

        var sFreeText = this.m_sInputQuery;
        var iNumberOfCategories = this.m_asCategories.length;
        this.m_iNumberOfEntryRequest = 0;
        sFrom += "00";
        sTo += "00";
        this.m_aoEntries = [];

        //How many request to server ?
        var iIndexCategory;
        for(iIndexCategory = 0 ; iIndexCategory < iNumberOfCategories; iIndexCategory++)
        {
            if(this.m_asCategories[iIndexCategory].isSelected === true)
            {
                this.m_iNumberOfEntryRequest++;
            }
        }
        //send request to server (get entries)
        for(iIndexCategory = 0 ; iIndexCategory < iNumberOfCategories; iIndexCategory++)
        {
            var sCategory = this.m_asCategories[iIndexCategory].name;
            if(this.m_asCategories[iIndexCategory].isSelected === true)
            {
                this.m_bIsLoadedTable = false;
                // this.m_iNumberOfEntryRequest++;
                this.getEntries(sFrom,sTo,sFreeText,sCategory);
            }
        }

    };
    /**
     *
     * @param sFrom
     * @param sTo
     * @param sFreeText
     * @param sCategory
     * @returns {boolean}
     */
    /**
     * Make the real query
     */
    CatalogController.prototype.getEntries = function(sFrom,sTo,sFreeText,sCategory){
        if(utilsIsStrNullOrEmpty(sFrom) === true || utilsIsStrNullOrEmpty(sTo) === true || utilsIsStrNullOrEmpty(sCategory) === true  )
        {
            return false;
        }
        var oController = this;
        //get entries
        this.m_oCatalogService.getEntries(sFrom,sTo,sFreeText,sCategory).then(function (data) {
            if(utilsIsObjectNullOrUndefined(data.data) == false)
            {
                oController.m_aoEntries = oController.m_aoEntries.concat(data.data);
                oController.drawEntriesBoundariesInMap();
            }

            oController.m_iNumberOfEntryRequest--;
            if(oController.m_iNumberOfEntryRequest === 0)
            {
                oController.m_bIsLoadedTable = true;
            }
            // oController.m_bIsLoadedTable = true;
        },function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR SEARCHING THE CATALOGUE");
            oController.m_iNumberOfEntryRequest--;
            if(oController.m_iNumberOfEntryRequest === 0)
            {
                oController.m_bIsLoadedTable = true;
            }
            // oController.m_bIsLoadedTable = true;
        });

        return true;
    };
    /**
     * Download of a product
     * @param oEntry
     * @returns {boolean}
     */
    CatalogController.prototype.downloadEntry = function(oEntry)
    {
        if(utilsIsObjectNullOrUndefined(oEntry)) return false;
        if(this.m_bIsDownloadingProduct === true)
            return false;

        var oJson = {
            fileName: oEntry.fileName,
            filePath: oEntry.filePath
        };

        var sFileName = oEntry.fileName;
        this.m_bIsDownloadingProduct = true;
        var oController = this;
        this.m_sProductNameInDownloadingStatus = oEntry.fileName;

        var sUrl = null;

        // P.Campanella 17/03/2020: redirect of the download to the node that hosts the workspace
        if (utilsIsStrNullOrEmpty(this.m_oConstantsService.getActiveWorkspace().apiUrl) == false) {
            sUrl = this.m_oConstantsService.getActiveWorkspace().apiUrl;
        }

        this.m_oCatalogService.downloadEntry(oJson, sUrl).then(function (data, status, headers, config) {
            if(utilsIsObjectNullOrUndefined(data.data) == false)
            {
                //var FileSaver = require('file-saver');
                var blob = new Blob([data.data], {type: "application/octet-stream"});
                saveAs(blob, sFileName);
                // var a = document.createElement("a"),
                //     url = URL.createObjectURL(blob);
                // a.href = url;
                // a.download = sFileName;
                // document.body.appendChild(a);
                // a.click();
                // setTimeout(function() {
                //     document.body.removeChild(a);
                //     window.URL.revokeObjectURL(url);
                // }, 0);
            }
            oController.m_bIsDownloadingProduct = false;
            oController.m_sProductNameInDownloadingStatus = "";
        },function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR DOWNLOADING FILE FROM THE CATALOGUE");
            oController.m_bIsDownloadingProduct = false;
            oController.m_sProductNameInDownloadingStatus = "";
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
     *
     * @param oLayer
     * @returns {boolean}
     */
    CatalogController.prototype.openModalDownloadProductInSelectedWorkspaces = function(oLayer)
    {
        if(utilsIsObjectNullOrUndefined(oLayer) === true)
        {
            return false;
        }

        var oOptions = {
            titleModal:"Add to workspaces",
            buttonName:"Add to workspace"
        };
        var oThat = this;
        var oCallback = function(result)
        {
            if(utilsIsObjectNullOrUndefined(result) === true)
            {
                return false;
            }
            var aoWorkSpaces = result;
            var iNumberOfWorkspaces = aoWorkSpaces.length;
            if(utilsIsObjectNullOrUndefined(aoWorkSpaces) )
            {
                console.log("Error there aren't Workspaces");
                return false;
            }
            // download product in all workspaces
            for(var iIndexWorkspace = 0 ; iIndexWorkspace < iNumberOfWorkspaces; iIndexWorkspace++)
            {
                // oLayer.isDisabledToDoDownload = true;
                // var sUrl = oLayer.link;
                var oError = function (data,status) {
                    utilsVexDialogAlertTop('GURU MEDITATION<br>THERE WAS AN ERROR TO IMPORTING THE IMAGE IN THE WORKSPACE');
                    // oLayer.isDisabledToDoDownload = false;
                }
                oCallback =function(data,status){
                }
                var sProductFileName = oLayer.fileName
                oThat.addProductInWorkspaces(aoWorkSpaces[iIndexWorkspace].workspaceId,sProductFileName,oError,oCallback);

            }

            return true;
        };

        utilsProjectOpenGetListOfWorkspacesSelectedModal(oCallback,oOptions,this.m_oModalService);
    };

    CatalogController.prototype.addProductInWorkspaces = function(sWorkspaceId, sProductFileNameViewModel, oErrorCallback, oCallback)
    {
        if(utilsIsStrNullOrEmpty(sWorkspaceId) === true || utilsIsStrNullOrEmpty(sProductFileNameViewModel)=== true)
        {
            return false;
        }
        if(utilsIsObjectNullOrUndefined(oErrorCallback) === true || utilsIsObjectNullOrUndefined(oCallback) === true)
        {
            return false;
        }

        this.m_oProductService.addProductToWorkspace(sProductFileNameViewModel,sWorkspaceId).then(oCallback,oErrorCallback);

        return true;
    };
    // /**
    //  * Add a product to a specific workspace
    //  * @param oProduct
    //  */
    // CatalogController.prototype.addProductToWorkspace = function(oProduct)
    // {
    //     var oController=this;
    //     this.m_oModalService.showModal({
    //         templateUrl: "dialogs/add_product_catalog/AddProductView.html",
    //         controller: "AddProductInCatalogController",
    //         inputs: {
    //             extras: {
    //                 product:oProduct
    //             }
    //         }
    //     }).then(function (modal) {
    //         modal.element.modal();
    //         modal.close.then(function (result) {
    //             oController.m_oScope.Result = result;
    //         });
    //     });
    //
    //
    // };

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
     * Get info about a product
     * @param oProduct
     */
    CatalogController.prototype.openFTPService = function(oProduct)
    {
        var oController=this;
        this.m_oModalService.showModal({
            templateUrl: "dialogs/ftp_service/FTPView.html",
            controller: "FTPController",
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
    /**
     *
     * @param sOperationName
     * @returns {string}
     */
    CatalogController.prototype.getFriendlyOperationName = function(sOperationName)
    {
        if(utilsIsStrNullOrEmpty(sOperationName) === true)
        {
            return "";
        }
        sOperationName = sOperationName.toLowerCase();
        var sReturnValue = "";
        switch(sOperationName) {
            case "download":
                sReturnValue = this.m_sDonwloadFriendlyName;
                // return "Downloaded products";
                break;
            case "computed":
                sReturnValue = this.m_sComputedFriendlyName;
                // return "Computed products";
                break;
            case "public":
                sReturnValue = this.m_sPublicFriendlyName;
                // return "Products saved in Wasdi";
                break;
            case "ingestion":
                sReturnValue = this.m_sIngestionFriendlyName;
                // return "Ingestion products";
                break;
            case "shared":
                sReturnValue = this.m_sSharedFriendlyName;
                // return "Shared products by users";
                break;
        }
        return sReturnValue;
    };

    CatalogController.prototype.getTranslatedOperation = function()
    {
        var oController = this;
        //DOWNLOAD
        this.m_oTranslate('CATALOG_OPERATION_DOWNLOAD').then(function(text)
        {
            oController.m_sDonwloadFriendlyName = text;
        });
        //COMPUTED
        this.m_oTranslate('CATALOG_OPERATION_COMPUTED').then(function(text)
        {
            oController.m_sComputedFriendlyName = text;
        });
        //PUBLIC
        this.m_oTranslate('CATALOG_OPERATION_PUBLIC').then(function(text)
        {
            oController.m_sPublicFriendlyName = text;
        });
        //INGESTION
        this.m_oTranslate('CATALOG_OPERATION_INGESTION').then(function(text)
        {
            oController.m_sIngestionFriendlyName = text;
        });
        //SHARED
        this.m_oTranslate('CATALOG_OPERATION_SHARED').then(function(text)
        {
            oController.m_sSharedFriendlyName  = text;
        });
    };
    CatalogController.$inject = [
        '$scope',
        'ConstantsService',
        'AuthService',
        '$state',
        'CatalogService',
        'MapService',
        'ModalService',
        'ProductService',
        '$translate'
    ];

    return CatalogController;
}) ();
window.CatalogController = CatalogController;
