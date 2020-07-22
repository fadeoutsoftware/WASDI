/**
 * Application Details
 * Created by p.campanella on 14/07/2020.
 */

var WasdiApplicationDetailsController = (function() {

    /**
     * Class constructor
     * @param $scope
     * @param oConstantsService
     * @param oAuthService
     * @param oProcessorService
     * @constructor
     */
    function WasdiApplicationDetailsController($scope, $state, oConstantsService, oAuthService, oProcessorService) {
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
         * @type {WasdiApplicationDetailsController}
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
         * Name of the selected application
         * @type {*[]}
         */
        this.m_sSelectedApplication = this.m_oConstantsService.getSelectedApplication();

        /**
         * Application Object
         * @type {{}}
         */
        this.m_oApplication = {};

        let oController = this;

        /**
         * Ask the list of Applications to the WASDI server
         */
        this.m_oProcessorService.getMarketplaceDetail(this.m_sSelectedApplication).success(function (data) {
            if(utilsIsObjectNullOrUndefined(data) == false)
            {
                oController.m_oApplication = oController.setDefaultImages(data);
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
    WasdiApplicationDetailsController.prototype.openApplicationPage = function() {

        //this.m_oState.go("root.appui", { workSpace : sWorkSpace.workspaceId });//use workSpace when reload editor page
        this.m_oState.go("root.appui");
    }

    WasdiApplicationDetailsController.prototype.setDefaultImages = function(aoProcessorList)
    {
        if(utilsIsObjectNullOrUndefined(aoProcessorList) === true)
        {
            return aoProcessorList;
        }
        var sDefaultImage = "assets/wasdi/miniLogoWasdi.png";
        var iNumberOfProcessors = aoProcessorList.length;
        for(var iIndexProcessor = 0; iIndexProcessor < iNumberOfProcessors; iIndexProcessor++)
        {
            if(utilsIsObjectNullOrUndefined(aoProcessorList.imgLink))
            {
                aoProcessorList[iIndexProcessor].imgLink = sDefaultImage;
            }
        }
        return aoProcessorList;
    };

    WasdiApplicationDetailsController.$inject = [
        '$scope',
        '$state',
        'ConstantsService',
        'AuthService',
        'ProcessorService'
    ];

    return WasdiApplicationDetailsController;
}) ();
