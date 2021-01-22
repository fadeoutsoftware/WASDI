/**
 * Created by a.corrado on 31/03/2017.
 */


var ListFloodAreaDetectionController = (function() {

    function ListFloodAreaDetectionController($scope, oClose,oExtras,oSnapOperationService,oProcessorService,oConstantsService) {
        //MEMBERS
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_SnapOperationService = oSnapOperationService;
        this.m_oProcessorService = oProcessorService;
        this.m_oConstantsService = oConstantsService;
        this.m_aoProducts = this.m_oExtras.products;
        this.m_oSelectedReferenceProduct = null;
        this.m_oSelectedPostEventImageProduct = null;
        this.m_iHSBAStartDepth = 0;
        this.m_dBimodalityCoefficent = 2.4;
        this.m_iMinimumTileDimension = 1000;
        this.m_iMinimalBlobRemoval = 10;
        this.m_sPreprocess = '1';

        this.m_oReturnReferenceValueDropdown = {};
        this.m_oReturnPostEventeValueDropdown = {};
        this.m_aoProductListDropdown = this.getDropdownMenuList(this.m_aoProducts);

        if(utilsIsObjectNullOrUndefined(this.m_aoProducts) === false)
        {
            this.m_oSelectedReferenceProduct = this.m_aoProducts[0];
            this.m_oSelectedPostEventImageProduct = this.m_aoProducts[0];
        }
        $scope.close = function(result) {
            oClose(result, 300); // close, but give 500ms for bootstrap to animate
        };

    }

    ListFloodAreaDetectionController.prototype.getDropdownMenuList = function(aoProduct){

        return utilsProjectGetDropdownMenuListFromProductsList(aoProduct)
    };
    ListFloodAreaDetectionController.prototype.getSelectedProduct = function(aoProduct,oSelectedProduct){

        return utilsProjectDropdownGetSelectedProduct(aoProduct,oSelectedProduct);
    }

    ListFloodAreaDetectionController.prototype.redirectToWebSite = function(){
        this.m_oWindow.open('http://edrift.cimafoundation.org', '_blank');
    };

    ListFloodAreaDetectionController.prototype.runListFloodAreaDetection = function(){
        var oActiveWorkspace = this.m_oConstantsService.getActiveWorkspace();

        if(this.checkListFloodAreaDetectionObject(this.m_oSelectedReferenceProduct,this.m_oSelectedPostEventImageProduct,
                                               this.m_iHSBAStartDepth,this.m_dBimodalityCoefficent,this.m_iMinimumTileDimension,
                                               this.m_iMinimalBlobRemoval ) === false)
        {
            return false;
        }

        var oInputFile = this.getSelectedProduct(this.m_aoProducts,this.m_oReturnReferenceValueDropdown);
        var oInputFilePostEvent = this.getSelectedProduct(this.m_aoProducts,this.m_oReturnPostEventeValueDropdown);

        var oListFlood = {
            // REF_IN:this.m_oSelectedReferenceProduct.fileName,
            REF_IN:oInputFile.fileName,
            FLOOD_IN:oInputFilePostEvent.fileName,
            HSBA_FLOOD_MASK_OUT:"",
            FLOOD_MAP_OUT:"",
            HSBA_DEPTH_IN: this.m_iHSBAStartDepth,
            ASHMAN_COEFF: this.m_dBimodalityCoefficent,
            MIN_PIXNB_BIMODD: this.m_iMinimumTileDimension,
            BLOBS_SIZE: this.m_iMinimalBlobRemoval,
            PRE_PROC_FLAG: this.m_sPreprocess
        };

        var sJSON=JSON.stringify(oListFlood);

        if(utilsIsObjectNullOrUndefined(oActiveWorkspace) === true)
        {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR: INVALID ACTIVE WORKSPACE ");
            return false;
        }

        this.m_oProcessorService.runProcessor('edriftlistflood',sJSON)
            .then(function(data,status){
                if( (utilsIsObjectNullOrUndefined(data.data) === false) && (status === 200))
                {
                    var oDialog =  utilsVexDialogAlertBottomRightCorner("eDRIFT FLOODED AREA DETECTION<br>THE PROCESS HAS BEEN SCHEDULED");
                    utilsVexCloseDialogAfter(4000, oDialog);
                }
                else
                {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR: LIST FLOOD FAILED");
                }

            },function(){
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR: LIST FLOOD FAILED");
            });

    };

    ListFloodAreaDetectionController .prototype.checkListFloodAreaDetectionObject = function(oReferenceProduct,oPostEventImageProduct,
                                                                                             iHsbaStartDepth,dBimodalityCoeff,
                                                                                             iMinTileDimension,iMinBlobRemoval)
    {
        var bReturnValue = true;
        if(utilsIsObjectNullOrUndefined(oReferenceProduct) === true )
        {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR: INVALID REFERENCE PRODUCT ");
            bReturnValue = false;
        }
        if(utilsIsObjectNullOrUndefined(oPostEventImageProduct) === true )
        {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR: INVALID POST EVENT IMAGE PRODUCT ");
            bReturnValue = false;
        }
        return bReturnValue;
    }

    ListFloodAreaDetectionController.$inject = [
        '$scope',
        'close',
        'extras',
        'SnapOperationService',
        'ProcessorService',
        'ConstantsService'

    ];
    return ListFloodAreaDetectionController;
})();
