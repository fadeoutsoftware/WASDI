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
    function MarketPlaceController($scope, $state, oConstantsService, oAuthService, oProcessorService) {
        /**
         * Angular Scope
         */
        this.m_oScope = $scope;

        /**
         * Appication State
         */
        this.m_oState=$state;
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

        /**
         * List of applications
         * @type {*[]}
         */
        this.m_aoApplicationList = []

        let oController = this;

        /**
         * Ask the list of Applications to the WASDI server
         */
        this.m_oProcessorService.getMarketplaceList().success(function (data) {
            if(utilsIsObjectNullOrUndefined(data) == false)
            {
                oController.m_aoApplicationList = oController.setDefaultImages(data);
            }
            else
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR GETTING WAPPS LIST");
            }
        }).error(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR GETTING WAPPS LIST");
        });
    }

    /**
     * Open Application Page
     * @returns {*[]} Array of strings, names of the tabs
     */
    MarketPlaceController.prototype.openApplicationPage = function(sApplicationName) {

        this.m_oConstantsService.setSelectedApplication(sApplicationName);
        //this.m_oState.go("root.appui", { workSpace : sWorkSpace.workspaceId });//use workSpace when reload editor page
        this.m_oState.go("root.appdetails");
    }

    MarketPlaceController.prototype.setDefaultImages = function(aoProcessorList)
    {
        if(utilsIsObjectNullOrUndefined(aoProcessorList) === true)
        {
            return aoProcessorList;
        }
        var sDefaultImage = "assets/wasdi/miniLogoWasdi.png";
        var iNumberOfProcessors = aoProcessorList.length;
        for(var iIndexProcessor = 0; iIndexProcessor < iNumberOfProcessors; iIndexProcessor++)
        {
            if(utilsIsObjectNullOrUndefined(aoProcessorList[iIndexProcessor].imgLink))
            {
                aoProcessorList[iIndexProcessor].imgLink = sDefaultImage;
            }
        }
        return aoProcessorList;
    };

    MarketPlaceController.$inject = [
        '$scope',
        '$state',
        'ConstantsService',
        'AuthService',
        'ProcessorService'
    ];

    return MarketPlaceController;
}) ();
