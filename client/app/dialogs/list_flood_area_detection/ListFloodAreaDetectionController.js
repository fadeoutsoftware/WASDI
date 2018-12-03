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

        if(utilsIsObjectNullOrUndefined(this.m_oSelectedReferenceProduct) === true )
        {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR: INVALID REFERENCE PRODUCT ");
            return false;
        }
        if(utilsIsObjectNullOrUndefined(this.m_oSelectedPostEventImageProduct) === true )
        {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR: INVALID POST EVENT IMAGE PRODUCT ");
            return false;
        }
        var oListFlood = {
           referenceFile:this.m_oSelectedReferenceProduct.name,
           postEventFile:this.m_oSelectedPostEventImageProduct.name,
           outputMaskFile:"",
           outputFloodMapFile:"",
        };

        if(utilsIsObjectNullOrUndefined(oActiveWorkspace) === true)
        {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR: INVALID ACTIVE WORKSPACE ");
            return false;
        }

        this.m_SnapOperationService.runListFlood(oListFlood,oActiveWorkspace.workspaceId)
            .success(function(data,status){
                if( (utilsIsObjectNullOrUndefined(data) === false) && (status === 200))
                {
                    var oDialog =  utilsVexDialogAlertBottomRightCorner("GURU MEDITATION<br>LIST FLOOD IS RUNNING.");
                    utilsVexCloseDialogAfterFewSeconds(4000, oDialog);
                }
                else
                {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR: SEARCH ORBITS FAILS.");
                }

            })
            .error(function(){
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR: SEARCH ORBITS FAILS.");
            });
    };


    ListFloodAreaDetectionController.$inject = [
        '$scope',
        'close',
        'extras',
        'SnapOperationService',
        'ConstantsService'

    ];
    return ListFloodAreaDetectionController;
})();
