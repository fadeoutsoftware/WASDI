/**
 * Marketplace
 * Created by p.campanella on 09/07/2020.
 */

var MarketPlaceController = (function() {

    /**
     * Class constructor
     * @param $scope
     * @param oConstantsService
     * @param oAuthService
     * @param oProcessorService
     * @constructor
     */
    function MarketPlaceController($scope, oConstantsService, oAuthService, oProcessorService) {
        /**
         * Angular Scope
         */
        this.m_oScope = $scope;
        /**
         * Reference to the controller
         * @type {MarketPlaceController}
         */
        this.m_oScope.m_oController = this;
        /**
         * Constant Service
         */
        this.m_oConstantsService = oConstantsService;
        /**
         * Auth Service
         */
        this.m_oAuthService = oAuthService;
        /**
         * Processors Service
         */
        this.m_oProcessorService = oProcessorService;

        let oController = this;

        /**
         * Ask the Processor UI to the WASDI server
         */
        this.m_oProcessorService.getProcessorUI(this.m_oConstantsService.getSelectedApplication())
            .success(function(data,status){
            })
            .error(function(){
                // TODO: Temperary for debug with an hard coded UI
                //utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR: READING APP UI");
            });
    }

    /**
     * Get the list of tabs
     * @returns {*[]} Array of strings, names of the tabs
     */
    MarketPlaceController.prototype.getApplications = function() {
        return [];
    }

    MarketPlaceController.$inject = [
        '$scope',
        'ConstantsService',
        'AuthService',
        'ProcessorService'
    ];

    return MarketPlaceController;
}) ();
