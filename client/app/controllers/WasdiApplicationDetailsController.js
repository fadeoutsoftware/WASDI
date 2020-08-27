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

        /**
         * Flat to decide to show or not more reviews link
         * @type {boolean}
         */
        this.m_bShowLoadMoreReviews = true;

        /**
         * Name of the selected application
         * @type {*[]}
         */
        this.m_sSelectedApplication = this.m_oConstantsService.getSelectedApplication();

        /**
         * array of images
         * @type {*[]}
         */
        this.m_asImages = [];

        /**
         * Reviews Wrapper View Mode with the summary and the list of reviews
         * @type {{avgVote: number, numberOfFiveStarVotes: number, numberOfOneStarVotes: number, reviews: [], numberOfFourStarVotes: number, numberOfTwoStarVotes: number, numberOfThreeStarVotes: number}}
         */
        this.m_oReviewsWrapper = {
            avgVote: -1,
            numberOfOneStarVotes: 0,
            numberOfTwoStarVotes: 0,
            numberOfThreeStarVotes: 0,
            numberOfFourStarVotes: 0,
            numberOfFiveStarVotes: 0,
            alreadyVoted: false,
            reviews: []
        }

        this.m_oUserReview = {
            vote: -1,
            title: "",
            comment: "",
            processorId: ""
        }

        $scope.previewRating = 0;

        /**
         * Application Object
         * @type {{}}
         */
        this.m_oApplication = {};

        /**
         * Actual page of reviews
         * @type {number}
         */
        this.m_iReviewsPage = 0;

        /**
         * Number of reviews per page
         * @type {number}
         */
        this.m_iReviewItemsPerPage = 4;

        // Local reference to the controller
        let oController = this;

        /**
         * Ask the list of Applications to the WASDI server
         */
        this.m_oProcessorService.getMarketplaceDetail(this.m_sSelectedApplication).success(function (data) {
            if(utilsIsObjectNullOrUndefined(data) == false)
            {
                oController.m_oApplication = data;
                oController.m_asImages.push(oController.m_oApplication.imgLink)
                if (oController.m_oApplication.images.length>0) {
                    oController.m_asImages = oController.m_asImages.concat(oController.m_oApplication.images);
                }
            }
            else
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR GETTING APPLICATION DATA");
            }
        }).error(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR GETTING APPLICATION DATA");
        });

        // Get the reviews
        this.refreshReviews();
    }

    WasdiApplicationDetailsController.prototype.refreshReviews = function() {

        var oController = this;

        this.m_oProcessorMediaService.getProcessorReviews(this.m_sSelectedApplication, this.m_iReviewsPage, this.m_iReviewItemsPerPage = 4).success(function (data) {
            if(utilsIsObjectNullOrUndefined(data) == false)
            {
                oController.m_oReviewsWrapper = data;
                if (data.reviews.length == 0) oController.m_bShowLoadMoreReviews = false;
            }
            else
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR GETTING APP REVIEWS");
            }
        }).error(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR GETTING APP REVIEWS");
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


    /**
     * Get the name of a category from the id
     * @param sCategoryId
     * @returns {*}
     */
    WasdiApplicationDetailsController.prototype.formatDate = function(iTimestamp)
    {
        // Create a new JavaScript Date object based on the timestamp
        let oDate = new Date(iTimestamp);
        return oDate.toLocaleDateString();
    };

    WasdiApplicationDetailsController.prototype.loadMoreReviews = function () {
        var oController =this;
        this.m_iReviewsPage = this.m_iReviewsPage + 1;
        // Get the reviews
        this.m_oProcessorMediaService.getProcessorReviews(this.m_sSelectedApplication, this.m_iReviewsPage, this.m_iReviewItemsPerPage = 4).success(function (data) {
            if(utilsIsObjectNullOrUndefined(data) == false)
            {

                if (data.reviews.length == 0){
                    oController.m_bShowLoadMoreReviews = false;
                }
                else {
                    oController.m_oReviewsWrapper.reviews = oController.m_oReviewsWrapper.reviews.concat(data.reviews);
                }
            }
            else
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR GETTING APP REVIEWS");
            }
        }).error(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR GETTING APP REVIEWS");
        });
    }

    WasdiApplicationDetailsController.prototype.addNewReview = function () {
        var oController = this;

        this.m_oUserReview.processorId = this.m_oApplication.processorId;

        this.m_oProcessorMediaService.addProcessorReview(this.m_oUserReview).success(function (data) {
            oController.m_oUserReview.title="";
            oController.m_oUserReview.comment="";
            oController.m_oUserReview.vote=-1;

            var oDialog = utilsVexDialogAlertBottomRightCorner("REVIEW SAVED");
            utilsVexCloseDialogAfter(4000,oDialog);

            oController.m_iReviewsPage = 0;
            oController.refreshReviews();

        }).error(function (error) {
            oController.m_oUserReview.title="";
            oController.m_oUserReview.comment="";
            oController.m_oUserReview.vote=-1;

            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR SAVING THE REVIEW");
        });
    }

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
