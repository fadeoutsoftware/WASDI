/**
 * Created by a.corrado on 28/02/2017.
 */
var MergeProductsController = (function() {

    function MergeProductsController($scope, oClose,oExtras,Upload,oConstantService,$http,oCatalogService,$window) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_sLog = '';
        this.m_oFile = null;
        this.m_aoProductsList =  oExtras.ListOfProducts;
        this.m_oSelectedProduct = oExtras.SelectedProduct;
        this.m_oWorkSpaceActive = oExtras.WorkSpaceId;
        this.m_oConstantService = oConstantService;
        this.m_oHttp = $http;
        this.m_oCatalogService = oCatalogService;
        this.m_aoEntries = [];
        this.m_oSelectedEntry = null;
        this.m_oWindow=$window;
        this.m_oClose = oClose;       //$scope.close = oClose;
        this.searchEntries();

        this.m_oReturnValueDropdown = null;


        $scope.close = function(result) {
            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };
        $scope.$watch('m_oController.m_oFile', function () {
            if (!utilsIsObjectNullOrUndefined( $scope.m_oController.m_oFile) ) {
                //TODO M_OFILE IS AN ARRAY CHECK IT
                $scope.m_oController.upload($scope.m_oController.m_oFile[0]);
            }
        });
        //$scope.$watch('$scope.m_oFile', function () {
        //    if ($scope.file != null) {
        //        $scope.files = [$scope.file];
        //    }
        //});
    }

    MergeProductsController.prototype.getDropdownMenuList = function(aoProduct){

        if(utilsIsObjectNullOrUndefined(aoProduct) === true)
        {
            return [];
        }
        var iNumberOfProducts = aoProduct.length;
        var aoReturnValue=[];
        for(var iIndexProduct = 0; iIndexProduct < iNumberOfProducts; iIndexProduct++)
        {

            var oValue = {
                name:aoProduct[iIndexProduct].fileName,
                id:aoProduct[iIndexProduct].filePath
            };
            aoReturnValue.push(oValue);
        }

        return aoReturnValue;
    };
    MergeProductsController.prototype.getSelectedProduct = function(aoProduct,oSelectedProduct){
        if(utilsIsObjectNullOrUndefined(aoProduct) === true)
        {
            return [];
        }
        var iNumberOfProducts = aoProduct.length;
        var oReturnValue={};
        for(var iIndexProduct = 0; iIndexProduct < iNumberOfProducts; iIndexProduct++)
        {
            if( oSelectedProduct.name === aoProduct[iIndexProduct].fileName )
            {
                oReturnValue = aoProduct[iIndexProduct];
                break;
            }

        }
        return oReturnValue;
    };

    MergeProductsController.prototype.isVisibleDropDownMenu = function()
    {
        if(utilsIsObjectNullOrUndefined(this.m_oSelectedProduct)) return true;
        return false;
    };

    // MergeProductsController.prototype.uploadv2 = function (oFile) {
    //     if(utilsIsObjectNullOrUndefined(oFile)) return false;
    //     if(utilsIsObjectNullOrUndefined(this.m_oWorkSpaceActive)) return false;
    //
    //     var oController = this;
    //     var sUrl = oController.m_oConstantService.getAPIURL();
    //     sUrl += "/catalog/upload?sWorkspaceId=" + oController.m_oWorkSpaceActive.workspaceId;
    //
    //     var successCallback = function(data, status)
    //     {
    //         utilsVexDialogAlertTop("FILE UPLOADED");
    //     };
    //
    //     var errorCallback = function (data, status)
    //     {
    //         utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN UPLOAD FILE");
    //     };
    //
    //     var fd = new FormData();
    //     fd.append('file', oFile);
    //     oController.m_oHttp.post(sUrl, fd, {
    //          transformRequest: angular.identity,
    //          headers: {'Content-Type': undefined}
    //     }).then(successCallback, errorCallback);
    //
    //     return true;
    // };

    MergeProductsController.prototype.upload = function(oFile){

        if(utilsIsObjectNullOrUndefined(oFile))
            return false;
        if(utilsIsObjectNullOrUndefined(this.m_oWorkSpaceActive))
            return false;
        if(utilsIsObjectNullOrUndefined(this.m_oReturnValueDropdown) === true)
            return false;
        // if((utilsIsStrNullOrEmpty(this.m_oSelectedEntry.filePath)===true)||(utilsIsObjectNullOrUndefined(this.m_oSelectedEntry.filePath) === true) )
        // {
        //     utilsVexDialogAlertTop("the delected entry file path is invalid");
        //     return false;
        // }


        var oController = this;
        var sUrl = oController.m_oConstantService.getAPIURL();
        // sUrl += "/processing/assimilation?midapath=" +"/data/wasdi/catalogue/mulesme/2017/09/19/SMCItaly_20170919.tif";

        var oInputFile = this.getSelectedProduct(this.m_aoEntries,this.m_oReturnValueDropdown);

        // sUrl += "/processing/assimilation?midapath=" + this.m_oSelectedEntry.filePath;
        sUrl += "/processing/assimilation?midapath=" + oInputFile.filePath;

        if((utilsIsStrNullOrEmpty(oInputFile.filePath)===true)||(utilsIsObjectNullOrUndefined(oInputFile.filePath) === true) )
        {
            utilsVexDialogAlertTop("the delected entry file path is invalid");
            return false;
        }

        var successCallback = function(data, status)
        {

            var url = 'http://178.22.66.96/' + data.data;
            //download
            window.open(url,'_self');
            utilsVexDialogAlertTop("FILE UPLOADED");

        };

        var errorCallback = function (data, status)
        {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN UPLOAD FILE");
        };

        var fd = new FormData();
        fd.append('humidity', oFile);
        oController.m_oHttp.post(sUrl, fd, {
            transformRequest: angular.identity,
            headers: {'Content-Type': undefined}
        }).then(successCallback, errorCallback);
        //utilsVexDialogAlertBottomRightCorner();
        utilsVexDialogAlertDefault("Successful upload request");
        //this.closeDialog();
    };

    // MergeProductsController.prototype.assimilation = function()
    // {
    //     var oController = this;
    //     var sUrl = oController.m_oConstantService.getAPIURL();
    //     sApi="/catalog/assimilation";
    //     sUrl += sApi;
    //
    //     utilsVexDialogAlertTop("GURU MEDITATION<br>GETTING STARTED");
    //
    //     //this.m_oHttp.get(sUrl,"responseType: 'arraybuffer'")
    //     this.m_oHttp({method: 'GET',url: sUrl, responseType: 'arraybuffer'})
    //         .success(function (data, status,headers)
    //         {
    //             if (!utilsIsObjectNullOrUndefined(data) )
    //             {
    //                 var contentType = headers['content-type'];
    //                 var file = new Blob([data], { type: contentType});
    //                 var linkElement = document.createElement('a');
    //                 var url = window.URL.createObjectURL(file);
    //
    //                 linkElement.setAttribute('href', url);
    //                 linkElement.setAttribute("download", "name.tif");
    //
    //                 var clickEvent = new MouseEvent("click", {
    //                     "view": window,
    //                     "bubbles": true,
    //                     "cancelable": false
    //                 });
    //                 linkElement.dispatchEvent(clickEvent);
    //
    //             }
    //         }).error(function (data,status)
    //     {
    //         utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR: ASSIMILATION DOESN'T WORK");
    //     });
    // };

    MergeProductsController.prototype.searchEntries = function()
    {
        var sFrom="";
        var sTo="";
        // if(utilsIsStrNullOrEmpty(this.m_oDataFrom) === false)
        //     var sFrom = this.m_oDataFrom.replace(/-/g,'');
        // if(utilsIsStrNullOrEmpty(this.m_oDataTo) === false)
        //     var sTo =  this.m_oDataTo.replace(/-/g,'');

        //var sFreeText = "mida";
        var sFreeText = "";
        var sCategory = "PUBLIC";
        var oController = this;
        this.m_bIsLoadedTable = false;
        this.m_oCatalogService.getEntries(sFrom,sTo,sFreeText,sCategory).then(function (data) {
            if(utilsIsObjectNullOrUndefined(data.data) == false)
            {
                oController.m_aoEntries = data.data;
                oController.m_aoProductListDropdown = oController.getDropdownMenuList(oController.m_aoEntries);
            }
        },function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN GET ENTRIES");
        });
    };
    MergeProductsController.prototype.selectedEntry = function(oEntry){
        if(utilsIsObjectNullOrUndefined(oEntry) === true)
            return false;
        // this.m_oSelectedEntry = oEntry;
        this.m_oReturnValueDropdown = oEntry;
        return true;
    };
    MergeProductsController.prototype.closeDialog=function (sResult)
    {
        this.m_oScope.close(sResult, 500); // close, but give 500ms for bootstrap to animate
    };

    MergeProductsController.prototype.redirectToMidaWebSite = function(){
        this.m_oWindow.open('http://www.mydewetra.org', '_blank');
    };
    MergeProductsController.$inject = [
        '$scope',
        'close',
        'extras',
        'Upload',
        'ConstantsService',
        '$http',
        'CatalogService',
        '$window'
    ];
    return MergeProductsController;
})();
window.MergeProductsController = MergeProductsController;
