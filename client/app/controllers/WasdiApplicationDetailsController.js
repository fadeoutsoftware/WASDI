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
    function WasdiApplicationDetailsController($scope, $state, oConstantsService, oAuthService, oProcessorService, oProcessorMediaService) {
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
         * Processor Media Service
         */
        this.m_oProcessorMediaService = oProcessorMediaService;

        /*KASA FOR TEST*/
        $scope.testVar = ['GIS', 'rain', 'flood', 'animals'];

        /**
         * Name of the selected application
         * @type {*[]}
         */
        this.m_sSelectedApplication = this.m_oConstantsService.getSelectedApplication();


        /*KASA FOR TEST*/
        $scope.userStatus = '';
        $scope.userOwner = false;
        $scope.reviewsNbr = '36';
        $scope.processorPrice = '2';
        /*REVIEW MODAL*/
        $scope.previewRating = 0;
        $scope.ratingValue = 0;
        /*REVIEWS LIST*/
        $scope.reviews = [
            {
                name:'Luke Camminatore del Cielo',
                rating: '4',
                date:'12/05/2021',
                title: 'Veloce come il Falcon',
                comment:'bella app - veloce come il Falcon - fatto la rotta di kessel in meno di 12 parsec - spettacolo ',
            },
            {
                name:'Chube',
                rating: '2',
                date:'12/05/2021',
                title: 'Potrebbe andare meglio',
                comment:'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
            },
            {
                name:'Emmett Brown',
                rating: '1',
                date:'12/05/2021',
                title: 'Un tuffo nel passato!',
                comment:'Se i miei calcoli sono esatti, quando questa app toccherà le 88 miglia orarie ne vedremo delle belle, anche se consuma 1.21 gigawatt - un pò troppo',
            }
        ];

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
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR GETTING APPLICATION DATA");
            }
        }).error(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR GETTING APPLICATION DATA");
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

    WasdiApplicationDetailsController.prototype.getCategoryName = function(sCategoryId)
    {
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
        'ProcessorService',
        'ProcessorMediaService'
    ];

    return WasdiApplicationDetailsController;
}) ();
