/**
 * Created by a.corrado on 31/03/2017.
 */


var EDriftFloodAutomaticChainController = (function() {

    function EDriftFloodAutomaticChainController($scope, oClose,oExtras,oSnapOperationService,oProcessorService,oConstantsService) {
        //MEMBERS
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oProcessorService = oProcessorService;
        this.m_oConstantsService = oConstantsService;
        this.m_aoProducts = this.m_oExtras.products;

        this.m_oParameters = {BBOX:"29.0,92.0,10.0,100.0",
            ORBITS:"33",
            GRIDSTEP:"1,1",
            LASTDAYS:"1",
            PREPROCWORKFLOW: "LISTSinglePreproc",
            MOSAICBASENAME: "MY",
            MOSAICXSTEP: "0.00018",
            MOSAICYSTEP: "0.00018",
            SIMULATE: "0",
            ENDDATE: "2019-04-09"};

        $scope.close = function(result) {
            oClose(result, 300); // close, but give 500ms for bootstrap to animate
        };

    };

    EDriftFloodAutomaticChainController.prototype.redirectToWebSite = function(){
        this.m_oWindow.open('http://edrift.cimafoundation.org', '_blank');
    };

    EDriftFloodAutomaticChainController.prototype.runAutoChain = function(){

        var oActiveWorkspace = this.m_oConstantsService.getActiveWorkspace();
        //
        // var oAutoChain = {
        //     BBOX:"29.0,92.0,10.0,100.0",
        //     ORBITS:"33",
        //     GRIDSTEP:"1,1",
        //     LASTDAYS:"1",
        //     PREPROCWORKFLOW: "LISTSinglePreproc",
        //     MOSAICBASENAME: "MY",
        //     MOSAICXSTEP: "0.00018",
        //     MOSAICYSTEP: "0.00018",
        //     SIMULATE: "0",
        //     ENDDATE: "2019-04-09"
        // };

        var oAutoChain = this.m_oParameters;

        sJSON=JSON.stringify(oAutoChain);

        if(utilsIsObjectNullOrUndefined(oActiveWorkspace) === true)
        {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR: INVALID ACTIVE WORKSPACE ");
            return false;
        }


        this.m_oProcessorService.runProcessor('mosaic_tile',sJSON)
            .success(function(data,status){
                if( (utilsIsObjectNullOrUndefined(data) === false) && (status === 200))
                {
                    var oDialog =  utilsVexDialogAlertBottomRightCorner("eDRIFT AUTO CHAIN<br>THE PROCESS HAS BEEN SCHEDULED");
                    utilsVexCloseDialogAfter(4000, oDialog);
                }
                else
                {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR: AUTO CHAIN FAILED");
                }

            })
            .error(function(){
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR: AUTO CHAIN FAILED");
            });
    };

    EDriftFloodAutomaticChainController.$inject = [
        '$scope',
        'close',
        'extras',
        'SnapOperationService',
        'ProcessorService',
        'ConstantsService'

    ];
    return EDriftFloodAutomaticChainController;
})();
