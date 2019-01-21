/**
 * Created by a.corrado on 31/03/2017.
 */


var ListFloodAreaDetectionController = (function() {

    function ListFloodAreaDetectionController($scope, oClose,oExtras,oSnapOperationService,oConstantsService) {
        //MEMBERS
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_SnapOperationService = oSnapOperationService;
        this.m_oConstantsService = oConstantsService;
        this.m_aoProducts = this.m_oExtras.products;
        this.m_oSelectedReferenceProduct = null;
        this.m_oSelectedPostEventImageProduct = null;
        this.m_iHSBAStartDepth = 4;
        this.m_dBimodalityCoefficent = 2.4;
        this.m_iMinimumTileDimension = 1000;
        this.m_iMinimalBlobRemoval = 10;
        if(utilsIsObjectNullOrUndefined(this.m_aoProducts) === false)
        {
            this.m_oSelectedReferenceProduct = this.m_aoProducts[0];
            this.m_oSelectedPostEventImageProduct = this.m_aoProducts[0];
        }
        $scope.close = function(result) {
            oClose(result, 300); // close, but give 500ms for bootstrap to animate
        };

    };

    ListFloodAreaDetectionController.prototype.redirectToWebSite = function(){
        this.m_oWindow.open('http://www.mydewetra.org', '_blank');
    };

    ListFloodAreaDetectionController.prototype.runListFloodAreaDetection = function(){
        var oActiveWorkspace = this.m_oConstantsService.getActiveWorkspace();

        if(this.checkListFloodAreaDetectionObject(this.m_oSelectedReferenceProduct,this.m_oSelectedPostEventImageProduct,
                                               this.m_iHSBAStartDepth,this.m_dBimodalityCoefficent,this.m_iMinimumTileDimension,
                                               this.m_iMinimalBlobRemoval ) === false)
        {
            return false;
        }

        var oListFlood = {
           referenceFile:this.m_oSelectedReferenceProduct.fileName,
           postEventFile:this.m_oSelectedPostEventImageProduct.fileName,
           outputMaskFile:"",
           outputFloodMapFile:"",
           hsbaStartDepth: this.m_iHSBAStartDepth,
           bimodalityCoeff: this.m_dBimodalityCoefficent,
           minTileDimension: this.m_iMinimumTileDimension,
           minBlobRemoval: this.m_iMinimalBlobRemoval
        };

        if(utilsIsObjectNullOrUndefined(oActiveWorkspace) === true)
        {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR: INVALID ACTIVE WORKSPACE ");
            return false;
        }

        var oDialog =  utilsVexDialogAlertBottomRightCorner("GURU MEDITATION<br>LIST FLOOD IS RUNNING.");
        utilsVexCloseDialogAfter(4000, oDialog);

        this.m_SnapOperationService.runListFlood(oListFlood,oActiveWorkspace.workspaceId)
            .success(function(data,status){
                if( (utilsIsObjectNullOrUndefined(data) === false) && (status === 200))
                {
                    var oDialog =  utilsVexDialogAlertBottomRightCorner("PROCESS DONE<br>FLOODED AREA MAP ADDED TO THE WORKSPACE.");
                    utilsVexCloseDialogAfter(4000, oDialog);
                }
                else
                {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR: LIST FLOOD FAILED.");
                }

            })
            .error(function(){
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR: LIST FLOOD FAILED.");
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
        //TODO CHECK DATA  iHsbaStartDepth,dBimodalityCoeff, iMinTileDimension,iMinBlobRemoval
        return bReturnValue;
    }

    ListFloodAreaDetectionController.$inject = [
        '$scope',
        'close',
        'extras',
        'SnapOperationService',
        'ConstantsService'

    ];
    return ListFloodAreaDetectionController;
})();
