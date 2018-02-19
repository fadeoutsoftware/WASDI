/**
 * Created by a.corrado on 16/06/2017.
 */



var WorkFlowController = (function() {

    function WorkFlowController($scope, oClose,oExtras,oSnapOperationService,oConstantsService, oHttp) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oSnapOperationService = oSnapOperationService;
        this.m_oFile = null;
        this.m_aoProducts = this.m_oExtras.products;
        this.m_asProductsName = this.getProductsName();
        this.m_asSelectedProducts = [];
        this.m_oSelectedFile = null;
        this.m_sOutputFile = "";
        this.m_oConstantsService = oConstantsService;
        this.m_oActiveWorkspace =  this.m_oConstantsService.getActiveWorkspace();
        this.m_oHttp =  oHttp;
        //$scope.close = oClose;
        var oController = this;


        $scope.closeAndPostWorkFLow = function(result) {
            oController.postWorkFlow(oController);
            oController.postAllWorkFlows(oController.m_asSelectedProducts,oController)
            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };
        $scope.close = function(result) {

            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };

        // $scope.$watch('m_oController.m_oFile', function () {
        //     if (!utilsIsObjectNullOrUndefined( $scope.m_oController.m_oFile) ) {
        //         //TODO M_OFILE IS AN ARRAY CHECK IT
        //         $scope.m_oController.upload($scope.m_oController.m_oFile[0]);
        //     }
        // });
    }
    //
    // WorkFlowController.prototype.postWorkFlow = function (oController) {
    //
    //     var sUrl = oController.m_oConstantsService.getAPIURL();
    //     sUrl += '/processing/graph?workspace=' + oController.m_oActiveWorkspace.workspaceId + '&source=' + oController.m_oSelectedFile.fileName + '&destination=' + oController.m_sOutputFile;
    //
    //     var successCallback = function(data, status)
    //     {
    //         //utilsVexDialogAlertTop();
    //         var oDialog = utilsVexDialogAlertBottomRightCorner("WORKFLOW UPLOADED<br>PROCESSING WILL START IN A WHILE");
    //         utilsVexCloseDialogAfterFewSeconds(4000,oDialog);
    //
    //     };
    //
    //     var errorCallback = function (data, status)
    //     {
    //         utilsVexDialogAlertTop("GURU MEDITATION<br>THERE WAS AN ERROR UPLOADING WORKFLOW DESCRIPTION FILE");
    //     };
    //
    //
    //     var fd = new FormData();
    //     fd.append('file', this.m_oFile[0]);
    //
    //     oController.m_oHttp.post(sUrl, fd, {
    //         transformRequest: angular.identity,
    //         headers: {'Content-Type': undefined}
    //     }).then(successCallback, errorCallback);
    //
    //     return true;
    // };
    /**
     *
     * @param oController
     * @param oSelectedFile
     * @returns {boolean}
     */
     WorkFlowController.prototype.postWorkFlow = function (oController,oSelectedFile)
     {
         if(utilsIsObjectNullOrUndefined(oSelectedFile) === true)
         {
             return false;
         }

         //TODO 19/02/2018 PUT IT INSIDE A SERVICE a.corrado
        var sUrl = oController.m_oConstantsService.getAPIURL();
        sUrl += '/processing/graph?workspace=' + oController.m_oActiveWorkspace.workspaceId + '&source=' + oSelectedFile.fileName + '&destination=' + oController.changeProductName(oSelectedFile.productFriendlyName)//oController.m_sOutputFile;

        var successCallback = function(data, status)
        {
            //utilsVexDialogAlertTop();
            var oDialog = utilsVexDialogAlertBottomRightCorner("WORKFLOW UPLOADED<br>PROCESSING WILL START IN A WHILE");
            utilsVexCloseDialogAfterFewSeconds(4000,oDialog);

        };

        var errorCallback = function (data, status)
        {
            utilsVexDialogAlertTop("GURU MEDITATION<br>THERE WAS AN ERROR UPLOADING WORKFLOW DESCRIPTION FILE");
        };


        var fd = new FormData();
        fd.append('file', this.m_oFile[0]);

        oController.m_oHttp.post(sUrl, fd, {
            transformRequest: angular.identity,
            headers: {'Content-Type': undefined}
        }).then(successCallback, errorCallback);

        return true;
    };

    /**
     * postAllWorkFlows
     * @param asNameSelectedFiles
     * @param oController
     * @returns {boolean}
     */
    WorkFlowController.prototype.postAllWorkFlows = function(asNameSelectedFiles,oController)
    {
        if(utilsIsObjectNullOrUndefined(asNameSelectedFiles) === true)
        {
            return false;
        }
        var iNumberOfSelectedProduct = asNameSelectedFiles.length;

        for(var iIndexSelectedProduct = 0;iIndexSelectedProduct < iNumberOfSelectedProduct ; iIndexSelectedProduct++)
        {
            var oSelectedProduct = this.getProductByName(asNameSelectedFiles[iIndexSelectedProduct]);
            this.postWorkFlow(oController,oSelectedProduct);
        }

        return true;
    };

    /**
     * postAllWorkFlows
     * @returns {boolean}
     */
    WorkFlowController.prototype.dataForPostWorkFlowAreWellFormed = function () {
        if(utilsIsObjectNullOrUndefined(this.m_oFile) === true) return false;
        if(utilsIsObjectNullOrUndefined(this.m_oActiveWorkspace ) === true ) return false;
        // if(utilsIsObjectNullOrUndefined(this.m_oSelectedFile) === true ) return false;
        if(this.m_asSelectedProducts.length === 0 ) return false;
        return true;
    };
    /**
     *
     * @param oProductInput
     */
    WorkFlowController.prototype.selectedProduct = function (oProductInput) {
        this.m_oSelectedFile = oProductInput;
        this.m_sOutputFile = oProductInput.productFriendlyName + "_workflow";

    };

    /**
     * getProductByName
     * @param sProductName
     * @returns {*}
     */
    WorkFlowController.prototype.getProductByName = function(sProductName)
    {
        var iNumberOfProduct = this.m_aoProducts.length;
        for(var iIndexProduct = 0; iIndexProduct < iNumberOfProduct; iIndexProduct++ )
        {
            if(sProductName === this.m_aoProducts[iIndexProduct].productFriendlyName)
            {
                return this.m_aoProducts[iIndexProduct];
            }
        }
        return null;

    };
    /**
     *
     * @param sName
     * @returns {*}
     */
    WorkFlowController.prototype.changeProductName = function(sName){
        if(utilsIsStrNullOrEmpty(sName) === true)
            return "";

        return sName + "_workflow";
    }
    /**
     *
     * @returns {Array}
     */
    WorkFlowController.prototype.getProductsName = function () {
        var iNumberOfProducts = this.m_aoProducts.length;
        var asReturnValue = [];
        for(var iIndexProduct = 0; iIndexProduct < iNumberOfProducts; iIndexProduct++)
        {
            asReturnValue.push(this.m_aoProducts[iIndexProduct].productFriendlyName);
        }
        return asReturnValue;
            // .productFriendlyName

    };


    WorkFlowController.$inject = [
        '$scope',
        'close',
        'extras',
        'SnapOperationService',
        'ConstantsService',
        '$http'
    ];
    return WorkFlowController;
})();
