/**
 * Created by a.corrado on 30/11/2016.
 */

var CatalogController = (function() {

    function CatalogController($scope, oConstantsService, oAuthService,$state,oCatalogService,oMapService,oModalService )
    {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oConstantsService = oConstantsService;
        this.m_oAuthService = oAuthService;
        this.m_oState = $state;
        this.m_oMapService = oMapService;
        this.m_oCatalogService = oCatalogService;
        this.m_oModalService = oModalService;
        this.m_asCategories = [];
        this.m_sInputQuery = "";
        this.m_oDataFrom="";
        this.m_oDataTo = "";
        this.m_aoEntries = [];
        this.m_bIsLoadedTable = true;

        this.m_oMapService.initMap('catalogMap');
        this.GetCategories();

        $scope.$on('on-mouse-over-rectangle', function(event, args) {

            var oRectangle = args.rectangle;
            if(!utilsIsObjectNullOrUndefined(oRectangle))
            {
                if(utilsIsObjectNullOrUndefined($scope.m_oController.m_aoEntries.length))
                    var iLengthLayersList = 0;
                else
                    var iLengthLayersList = $scope.m_oController.m_aoEntries.length;

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

            var oRectangle = args.rectangle;
            if(!utilsIsObjectNullOrUndefined(oRectangle))
            {
                if(utilsIsObjectNullOrUndefined($scope.m_oController.m_aoEntries.length))
                    var iLengthLayersList = 0;
                else
                    var iLengthLayersList = $scope.m_oController.m_aoEntries.length;

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

            var oRectangle = args.rectangle;

            if(!utilsIsObjectNullOrUndefined(oRectangle))
            {
                if(utilsIsObjectNullOrUndefined($scope.m_oController.m_aoEntries))
                    var iLengthLayersList = 0;
                else
                    var iLengthLayersList = $scope.m_oController.m_aoEntries.length;

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

                        //container.animate({scrollTop: 485}, 2000);
                        //container.scrollTop(
                        //    scrollTo.offset().top - container.offset().top + container.scrollTop()
                        //);

                    }
                }

            }
        });
    }

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
                        if(iIndexCategory === 0)
                            isSelected = true;
                        var oCategory = {   name:data[iIndexCategory],
                                            isSelected:isSelected};
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

    CatalogController.prototype.searchEntries = function()
    {
        var sFrom="";
        var sTo="";
        if(utilsIsStrNullOrEmpty(this.m_oDataFrom) === false)
            var sFrom = this.m_oDataFrom.replace(/-/g,'');
        if(utilsIsStrNullOrEmpty(this.m_oDataTo) === false)
            var sTo =  this.m_oDataTo.replace(/-/g,'');

        var sFreeText = this.m_sInputQuery;
        var sCategory = this.getSelectedCategoriesAsString() ;
        var oController = this;
        this.m_bIsLoadedTable = false;
        sFrom += "00";
        sTo += "00"
        this.m_oCatalogService.getEntries(sFrom,sTo,sFreeText,sCategory).success(function (data) {
            if(utilsIsObjectNullOrUndefined(data) == false)
            {
                oController.m_aoEntries = data;
                oController.drawEntriesBoundariesInMap();
            }
            oController.m_bIsLoadedTable = true;
        }).error(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN GET ENTRIES");
            oController.m_bIsLoadedTable = true;
        });
    };

    CatalogController.prototype.downloadEntry = function(oEntry)
    {
        if(utilsIsObjectNullOrUndefined(oEntry))
            return false;
        var oJson = {
            fileName: oEntry.fileName,
            filePath: oEntry.filePath
        };

        //var sEntryJson = JSON.stringify(oJson);
       this.m_oCatalogService.downloadEntry(oJson).success(function (data) {
            if(utilsIsObjectNullOrUndefined(data) == false)
            {
                // var json = JSON.stringify(data),
                //     blob = new Blob([json], {type: "octet/stream"}),
                //     url = window.URL.createObjectURL(blob);
                // a.href = url;
                // a.download = fileName;
                // a.click();
                // window.URL.revokeObjectURL(url);
                var blob = new Blob([data], {type: "octet/stream"});
                var objectUrl = URL.createObjectURL(blob);
                window.open(objectUrl,'_self');
            }
        }).error(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR DOWNLOADING FILE FROM THE CATALOGUE");
        });

        return true;
    };

    CatalogController.prototype.getSelectedCategoriesAsString = function()
    {
        if(utilsIsObjectNullOrUndefined(this.m_asCategories) === true )
            return "";
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
        if(utilsIsStrNullOrEmpty(sReturnValue) === false)
            sReturnValue = sReturnValue.substring(0, sReturnValue.length - 1);
        return sReturnValue;
    };

    CatalogController.prototype.convertBoundaries = function(sBoundaries)
    {
        var asBoundaries = sBoundaries.split(",");
        var iNumberOfBoundaries = asBoundaries.length;
        var aasReturnValues = [];
        var iIndexReturnValues = 0;
        for(var iBoundaryIndex = 0 ; iBoundaryIndex < iNumberOfBoundaries; iBoundaryIndex++)
        {
            if(utilsIsOdd(iBoundaryIndex) === false)
            {
                aasReturnValues[iIndexReturnValues] = [asBoundaries[iBoundaryIndex],asBoundaries[iBoundaryIndex+1]];
                iIndexReturnValues++;
            }
        }
        return aasReturnValues;
    };
    CatalogController.prototype.drawEntriesBoundariesInMap = function()
    {
        if(utilsIsObjectNullOrUndefined(this.m_aoEntries) === true)
            return false;
        var iNumberOfEntries = this.m_aoEntries.length;
        for( var iIndexEntry = 0; iIndexEntry < iNumberOfEntries; iIndexEntry++ )
        {
            var sBoundaries = this.m_aoEntries[iIndexEntry].boundingBox;
            if( (utilsIsObjectNullOrUndefined(sBoundaries) === false)&&(utilsIsStrNullOrEmpty(sBoundaries) === false) )
            {
                var aBoundariesArray = this.convertBoundaries(sBoundaries);
                var oRectangle = this.m_oMapService.addRectangleOnMap(aBoundariesArray,"#ff7800","product"+iIndexEntry);
                this.m_aoEntries[iIndexEntry].rectangle = oRectangle;
            }

        }
        return true;
    };

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
