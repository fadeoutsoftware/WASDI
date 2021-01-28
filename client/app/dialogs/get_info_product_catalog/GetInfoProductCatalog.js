/**
 * Created by a.corrado on 31/03/2017.
 */


var GetInfoProductCatalogController = (function() {

    function GetInfoProductCatalogController($scope, oClose, oExtras,oWorkspaceService,oFileBufferService,oTranslate,oMapService,oTimeout) {
        //MEMBERS
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oWorkspaceService = oWorkspaceService;
        this.m_oFileBufferService = oFileBufferService;
        this.m_oProduct = this.m_oExtras.product;
        this.m_oSelectedProduct = this.m_oExtras.selectedProduct;
        this.m_aoListOfProductWorkspaces = [];
        this.m_sDonwloadFriendlyName = "";
        this.m_sComputedFriendlyName = "";
        this.m_sPublicFriendlyName = "";
        this.m_sIngestionFriendlyName = "";
        this.m_sSharedFriendlyName  = "";
        this.m_oTranslate = oTranslate;
        this.m_oMapService = oMapService;
        this.m_oTimeout = oTimeout;
        this.m_oMap = null;
        
        $scope.close = function(result) {
            oClose(result, 300); // close, but give 500ms for bootstrap to animate
        };

        // this.m_oMapService.initMap('getInfoProductCatalogMap');
        // this.m_oMapService.initMap('test');
        this.initMapWithDelay();
        this.getTranslatedOperation();
        this.getWorkspaceListByProduct();
    };

    // GetInfoProductCatalogController.prototype.checkProductInfo = function()
    // {
    //     if(utilsIsObjectNullOrUndefined(this.m_oProduct) === true )
    //         return false;
    //     var sFilename = this.m_oProduct.fileName;
    //     sFilename = sFilename.trim();
    //     if(utilsIsStrNullOrEmpty(sFilename) === true)
    //     {
    //         this.m_oProductChecked.fileName = "Unknown";
    //     }
    //     else
    //     {
    //         this.m_oProductChecked.fileName = this.m_oProduct.fileName;
    //     }
    //     this.m_oProductChecked.boundingBox = this.m_oProduct.boundingBox;
    //     this.m_oProductChecked.category = this.m_oProduct.category;
    //     this.m_oProductChecked.filePath = this.m_oProduct.filePath;
    // };
    //
    /**
     *
     * @returns {*}
     */
    GetInfoProductCatalogController.prototype.getFileName = function()
    {
        if(utilsIsStrNullOrEmpty(this.m_oProduct.fileName) === true)
        {
            return "Unknown";
        }
        return this.m_oProduct.fileName
    };

    /**
     *
     * @returns {*}
     */
    GetInfoProductCatalogController.prototype.getProductBands = function()
    {
        if( utilsIsObjectNullOrUndefined(this.m_oProduct) === true )
        {
            return [];
        }
        if( utilsIsObjectNullOrUndefined(this.m_oProduct.productViewModel.bandsGroups.bands) === true )
        {
            return [];
        }

        return this.m_oProduct.productViewModel.bandsGroups.bands;
    };

    GetInfoProductCatalogController.prototype.getWorkspaceListByProduct = function()
    {
        var oController = this;
        var sFileName = this.getFileName();
        if( ( utilsIsStrNullOrEmpty(sFileName) === true ) || ( sFileName === "Unknown" ) )
        {
            return false;
        }
        this.m_oWorkspaceService.getWorkspaceListByProductName(sFileName)
            .success(function(data,status){
                if(utilsIsObjectNullOrUndefined(data) === false && status === 200)
                {
                    // workspaceId;
                    // workspaceName;
                    // ownerUserId;
                    oController.m_aoListOfProductWorkspaces = data;
                }
                else
                {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>WAS IMPOSSIBLE GET WORKSPACES LIST");
                }
            })
            .error(function(data,status){
                utilsVexDialogAlertTop("GURU MEDITATION<br>WAS IMPOSSIBLE GET WORKSPACES LIST");
            });
        return true;
    };

    GetInfoProductCatalogController.prototype.publicAllSelectedBands = function()
    {
        var aoBands = this.getProductBands();

        if(utilsIsObjectNullOrUndefined(aoBands) === true)
        {
            return false;
        }

        var iNumberOfBands = aoBands.length;
        var sUrl = this.m_oProduct.filePath;

        if(utilsIsStrNullOrEmpty(sUrl) === true)
        {
            return false;
        }

        for(var iIndexBand = 0 ; iIndexBand < iNumberOfBands; iIndexBand++ )
        {
            var oBand = aoBands[iIndexBand];
            if( ( utilsIsObjectNullOrUndefined(oBand.selectedWorkspace) === false ) )
            {
                var sBand = oBand.name;
                var sWorkspaceId = oBand.selectedWorkspace.workspaceId;
                this.publicBand(sUrl, sWorkspaceId,sBand );
            }
        }

        return true;
    };

    GetInfoProductCatalogController.prototype.publicBand = function(sUrl, sWorkspaceId, sBand)
    {
        if(utilsIsStrNullOrEmpty(sUrl) || utilsIsStrNullOrEmpty(sWorkspaceId) || utilsIsStrNullOrEmpty(sBand))
        {
            return false;
        }

        this.m_oFileBufferService.publishBand(sUrl, sWorkspaceId, sBand)
            .success(function(data,status){
                if( ( utilsIsObjectNullOrUndefined(data) === false ) && ( status === 200 ))
                {
                    utilsVexDialogAlertBottomRightCorner("PUBLISHED BAND");
                }
                else
                {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>WAS IMPOSSIBLE PUBLISH BAND.");
                }
            })
            .error(function(data,status){
                utilsVexDialogAlertTop("GURU MEDITATION<br>WAS IMPOSSIBLE PUBLISH BAND.");
            });

        return true;
    };


    /**
     *
     * @param sOperationName
     * @returns {string}
     */
    GetInfoProductCatalogController.prototype.getFriendlyOperationName = function(sOperationName)
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

    GetInfoProductCatalogController.prototype.getTranslatedOperation = function()
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


    GetInfoProductCatalogController.prototype.initMapWithDelay = function(){
        /*
        * We init the map after the dialog is loaded
        * */
        var oController = this;
        this.m_oTimeout( function(){
            oController.m_oMapService.initTileLayer();
            oController.m_oMap = oController.m_oMapService.initMap('getInfoProductCatalogMap');
            oController.addRectangleInMap(oController.m_oMap);
        }, 500 );
    };

    GetInfoProductCatalogController.prototype.addRectangleInMap = function(oMap)
    {
        var sBbox = this.m_oProduct.boundingBox;
        var sColor = "#ff7800";
        // var sReferenceName = "product_"+this.m_oProduct.fileName;

        if(utilsIsObjectNullOrUndefined(oMap) === true)
        {
            return false;
        }

        if( (utilsIsObjectNullOrUndefined(sBbox) === false)&&(utilsIsStrNullOrEmpty(sBbox) === false) )
        {

            var aoBounds = this.m_oMapService.convertBboxInBoundariesArray(sBbox);
            for(var iIndex = 0; iIndex < aoBounds.length; iIndex++ )
            {
                if(utilsIsObjectNullOrUndefined(aoBounds[iIndex])) return false;
            }

            //default color
            if(utilsIsStrNullOrEmpty(sColor)) sColor="#ff7800";

            // create an colored rectangle
            // weight = line thickness
            var oRectangle = L.polygon(aoBounds, {color: sColor, weight: 1}).addTo(oMap);

            //Fly to BBox
            oMap.flyToBounds(oRectangle.getBounds());;
        }
        else
        {
                return false;
        }
        return true;
    };

    GetInfoProductCatalogController.$inject = [
        '$scope',
        'close',
        'extras',
        'WorkspaceService',
        'FileBufferService',
        '$translate',
        'MapService',
        '$timeout'
    ];
    return GetInfoProductCatalogController;
})();
