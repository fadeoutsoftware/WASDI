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
     * @param oModalService
     * @param oProcessWorkspaceService
     * @constructor
     */
    function WasdiApplicationDetailsController($scope, $state, oConstantsService, oAuthService, oProcessorService, oProcessorMediaService, oModalService, oProcessWorkspaceService) {
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
         * Modal service
         */
        this.m_oModalService = oModalService;

        /**
         * Process Workspaces service
         */
        this.m_oProcessWorkspaceService = oProcessWorkspaceService;

        /**
         * Flat to decide to show or not more reviews link
         * @type {boolean}
         */
        this.m_bShowLoadMoreReviews = true;

        /**
         * Flat to decide to show or not comments link
         * @type {boolean}
         */
        this.m_bShowLoadComments = true;

        /**
         * Name of the selected application
         * @type {*[]}
         */
        this.m_sSelectedApplication = this.m_oConstantsService.getSelectedApplication();

        /**
         * Id of the selected review
         * @type {*[]}
         */
        this.m_sSelectedReviewId = this.m_oConstantsService.getSelectedReviewId();

        /**
         * array of images
         * @type {*[]}
         */
        this.m_asImages = [];

        /**
         * Reviews Wrapper View Model with the summary and the list of reviews
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

        /**
         * Comments Wrapper View Model with the summary and the list of comments
         * @type {{reviews: []}}
         */
        this.m_oCommentsWrapper = {
            reviews: []
        }

        /**
         * View Model of the Review Modal Window
         * @type {{processorId: string, comment: string, title: string, vote: number}}
         */
        this.m_oUserReview = {
            vote: -1,
            title: "",
            comment: "",
            processorId: ""
        }

        /**
         * View Model of the Comment Modal Window
         * @type {{reviewId: string, text: string}}
         */
        this.m_oReviewComment = {
            reviewId: "",
            text: ""
        }

        /**
         * Variable used to preview the selected rating in the new comment window
         * @type {number}
         */
        $scope.previewRating = 0;

        /**
         * Application Object
         * @type {{}}
         */
        this.m_oApplication = {};

        /**
         * Review Object
         * @type {{}}
         */
        this.m_oReview = {};

        /**
         * Application Statistics Object
         */
        this.m_oStats = {"done":0, "error":0, "runs":0, "stoppped":0, "uniqueUsers":0, "mediumTime":0};

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

        /**
         * Flog to know when the page is waiting for the application data to come
         * @type {boolean}
         */
        this.m_bWaiting = true;

        /**
         * Flag to know when the page is waiting for reviews data to come
         * @type {boolean}
         */
        this.m_bReviewsWaiting = false;

        /**
         * Flag to know when the page is waiting for comments data to come
         * @type {boolean}
         */
        this.m_bCommentsWaiting = false;

        // Local reference to the controller
        let oController = this;

        // Check if we have the selected application
        if (utilsIsStrNullOrEmpty(this.m_sSelectedApplication)) {
            // Check if we can get it from the state
            if (!(utilsIsObjectNullOrUndefined(this.m_oState.params.processorName) && utilsIsStrNullOrEmpty(this.m_oState.params.processorName))) {
                // Set the application name
                this.m_sSelectedApplication = this.m_oState.params.processorName;
            } else {
                // No app, go back to the marketplace
                this.m_oState.go("root.marketplace");
            }
        }

        /**
         * Ask the list of Applications to the WASDI server
         */
        this.m_oProcessorService.getMarketplaceDetail(this.m_sSelectedApplication).then(function (data) {
            if(utilsIsObjectNullOrUndefined(data.data) == false)
            {
                oController.m_oApplication = data.data;
                oController.m_asImages.push(oController.m_oApplication.imgLink)
                if (oController.m_oApplication.images.length>0) {
                    oController.m_asImages = oController.m_asImages.concat(oController.m_oApplication.images);
                }
            }
            else
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR GETTING APPLICATION DATA");
            }
            oController.m_bWaiting=false;
        },function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR GETTING APPLICATION DATA");
            oController.m_bWaiting=false;
        });

        /**
         * Ask the list of Applications to the WASDI server
         */
        this.m_oProcessWorkspaceService.getProcessorStatistics(this.m_sSelectedApplication).then(function (data) {
            if(utilsIsObjectNullOrUndefined(data) == false)
            {
                oController.m_oStats = data.data;
            }
            else
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR GETTING APPLICATION STATS");
            }
        },(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR GETTING APPLICATION STATS");
        }));

        // Get the reviews
        this.refreshReviews();
    }

    /**
     * Refresh the list of application reviews
     */
    WasdiApplicationDetailsController.prototype.refreshReviews = function() {

        this.m_bReviewsWaiting = true;

        var oController = this;

        this.m_oProcessorMediaService.getProcessorReviews(this.m_sSelectedApplication, this.m_iReviewsPage, this.m_iReviewItemsPerPage = 4).then(function (data) {
            if(utilsIsObjectNullOrUndefined(data.data) == false)
            {
                oController.m_oReviewsWrapper = data.data;
                if (data.data.reviews.length == 0) oController.m_bShowLoadMoreReviews = false;
            }
            else
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR GETTING APP REVIEWS");
            }

            oController.m_bReviewsWaiting = false;
        },function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR GETTING APP REVIEWS");
            oController.m_bReviewsWaiting = false;
        });

    }

    /**
     * Refresh the list of review comments
     */
    WasdiApplicationDetailsController.prototype.refreshComments = function(sSelectedReviewId) {
        this.m_sSelectedReviewId = sSelectedReviewId;

        this.m_bCommentsWaiting = true;

        var oController = this;

        this.m_oProcessorMediaService.getReviewComments(sSelectedReviewId).then(function (data) {
            if(utilsIsObjectNullOrUndefined(data.data) == false)
            {
                console.log("refreshComments data.data: ", data.data);
                console.log("refreshComments oController.m_sSelectedReviewId: ", oController.m_sSelectedReviewId);
                oController.m_oCommentsWrapper[oController.m_sSelectedReviewId] = data.data;

                if (data.data.length == 0) oController.m_bShowLoadComments = false;
                console.log("refreshComments oController.m_bShowLoadComments: ", oController.m_bShowLoadComments);
            }
            else
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR GETTING REVIEW COMMENTS");
            }

            oController.m_bCommentsWaiting = false;
        },function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR GETTING REVIEW COMMENTS");
            oController.m_bCommentsWaiting = false;
        });

    }

    /**
     * Open Application Page
     * @returns {*[]} Array of strings, names of the tabs
     */
    WasdiApplicationDetailsController.prototype.openApplicationPage = function() {

        this.m_oState.go("root.appui", { processorName : this.m_sSelectedApplication });//use workSpace when reload editor page
        //this.m_oState.go("root.appui");
    }


    /**
     * Format the date
     * @param iTimestamp
     * @returns {*}
     */
    WasdiApplicationDetailsController.prototype.formatDate = function(iTimestamp)
    {
        // Create a new JavaScript Date object based on the timestamp
        let oDate = new Date(iTimestamp);
        return oDate.toLocaleDateString();
    };

    /**
     * Format the time
     * @param iTimestamp
     * @returns {*}
     */
    WasdiApplicationDetailsController.prototype.formatTimestamp = function(iTimestamp)
    {
        // Create a new JavaScript Date object based on the timestamp
        let oDate = new Date(iTimestamp);
        return oDate.toLocaleDateString() + " " + oDate.toLocaleTimeString();
    };

    /**
     * Loads more reviews for the current app
     */
    WasdiApplicationDetailsController.prototype.loadMoreReviews = function () {
        var oController =this;
        this.m_iReviewsPage = this.m_iReviewsPage + 1;
        this.m_bReviewsWaiting = true;
        // Get the reviews
        this.m_oProcessorMediaService.getProcessorReviews(this.m_sSelectedApplication, this.m_iReviewsPage, this.m_iReviewItemsPerPage = 4).then(function (data) {
            if(utilsIsObjectNullOrUndefined(data.data) == false)
            {

                if (data.data.reviews.length == 0){
                    oController.m_bShowLoadMoreReviews = false;
                }
                else {
                    oController.m_oReviewsWrapper.reviews = oController.m_oReviewsWrapper.reviews.concat(data.data.reviews);
                }
            }
            else
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR GETTING APP REVIEWS");
            }
            oController.m_bReviewsWaiting = false;
        },function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR GETTING APP REVIEWS");
            oController.m_bReviewsWaiting = false;
        });
    }

    /**
     * Add a new review
     */
    WasdiApplicationDetailsController.prototype.addNewReview = function () {
        var oController = this;

        this.m_oUserReview.processorId = this.m_oApplication.processorId;

        this.m_oProcessorMediaService.addProcessorReview(this.m_oUserReview).then(function (data) {
            oController.m_oUserReview.title="";
            oController.m_oUserReview.comment="";
            oController.m_oUserReview.vote=-1;

            var oDialog = utilsVexDialogAlertBottomRightCorner("REVIEW SAVED");
            utilsVexCloseDialogAfter(4000,oDialog);

            oController.m_iReviewsPage = 0;
            oController.refreshReviews();

        },function (error) {
            oController.m_oUserReview.title="";
            oController.m_oUserReview.comment="";
            oController.m_oUserReview.vote=-1;

            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR SAVING THE REVIEW");
        });
    }

    WasdiApplicationDetailsController.prototype.setSelectedReviewId = function (reviewId) {
        this.m_sSelectedReviewId = reviewId;
        this.m_oReview.reviewId = reviewId;
    }

    /**
     * Add a new comment to a review
     */
    WasdiApplicationDetailsController.prototype.addNewComment = function () {
        var oController = this;

        this.m_oReviewComment.reviewId = this.m_oReview.reviewId;

        this.m_oProcessorMediaService.addReviewComment(this.m_oReviewComment).then(function (data) {

            oController.m_oReviewComment.text="";

            var oDialog = utilsVexDialogAlertBottomRightCorner("COMMENT SAVED");
            utilsVexCloseDialogAfter(4000,oDialog);

            oController.m_iCommentsPage = 0;
            oController.refreshComments(oController.m_oReviewComment.reviewId);

        },function (error) {
            oController.m_oReviewComment.text="";

            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR SAVING THE COMMENT");
        });
    }

    /**
     * Deletes a review
     */
    WasdiApplicationDetailsController.prototype.deleteReview = function (sReviewId) {

        var oController = this;

        var oDeleteReviewCallback = function (value) {

            if (value) {
                oController.m_oProcessorMediaService.deleteProcessorReview(oController.m_oApplication.processorId,sReviewId);
                oController.refreshReviews();
                return true;
            } else {
                return false;
            }
        };

        //ask user if he confirms to delete the review
        utilsVexDialogConfirm("DELETE REVIEW:<br>ARE YOU SURE?", oDeleteReviewCallback);
    }

    /**
     * Deletes a comment
     */
    WasdiApplicationDetailsController.prototype.deleteComment = function (oComment) {
        var oController = this;
        this.m_oReviewComment.reviewId = oComment.reviewId;
        this.m_oReviewComment.commentId = oComment.commentId;

        var oDeleteCommentCallback = function (value) {

            if (value) {
                oController.m_oProcessorMediaService.deleteReviewComment(oController.m_oReviewComment.reviewId, oController.m_oReviewComment.commentId).then(function (data) {
                    oController.refreshComments(oController.m_oReviewComment.reviewId);
                },function (error) {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR LOADING THE COMMENTS");
                });

                return true;
            } else {
                return false;
            }
        };

        //ask user if he confirms to delete the review
        utilsVexDialogConfirm("DELETE COMMENT:<br>ARE YOU SURE?", oDeleteCommentCallback);
    }

    /**
     * Decide is the review is of the actual user or not
     * @param oReview
     * @returns {boolean}
     */
    WasdiApplicationDetailsController.prototype.isMineReview = function (oReview) {

        if (utilsIsObjectNullOrUndefined(oReview)) return  false;

        let sActualUser = this.m_oConstantsService.getUserId();

        if (sActualUser == oReview.userId) {
            return true;
        }

        return  false;

    }

    /**
     * Decide is the comment is of the actual user or not
     * @param oReview
     * @returns {boolean}
     */
    WasdiApplicationDetailsController.prototype.isMineComment = function (oComment) {

        if (utilsIsObjectNullOrUndefined(oComment)) return  false;

        let sActualUser = this.m_oConstantsService.getUserId();

        if (sActualUser == oComment.userId) {
            return true;
        }

        return  false;
    }

    /**
     * Get the name of the thumb of an image. Hp is that the name "_thumb"
     * @param sImageName
     */
    WasdiApplicationDetailsController.prototype.getThumbFileNameFromImageName = function (sImageName) {

        try {
            let asSplit = sImageName.split('.')
            let sThumbFile = asSplit[0];
            sThumbFile= sThumbFile + "_thumb." + asSplit[1];
            return sThumbFile;
        }
        catch (error) {
            return  sImageName;
        }
    }

    WasdiApplicationDetailsController.prototype.editClick= function() {
        var oController = this;

        oController.m_oProcessorService.getDeployedProcessor(oController.m_oApplication.processorId).then(function (data) {
            oController.m_oModalService.showModal({
                templateUrl: "dialogs/processor/ProcessorView.html",
                controller: "ProcessorController",
                inputs: {
                    extras: {
                        processor:data.data
                    }
                }
            }).then(function (modal) {
                modal.element.modal();
                modal.close.then(function (oResult) {
                    if (utilsIsObjectNullOrUndefined(oResult) == false) {
                        oController.m_oApplication = oResult;
                    }
                });
            });
        },function () {

        });
    }

    WasdiApplicationDetailsController.prototype.getStatSuccess = function() {
        let dPerc = 1.0;

        if (this.m_oStats.runs>0) {
            dPerc = this.m_oStats.done/this.m_oStats.runs;
        }

        dPerc *= 100;

        return dPerc.toFixed(1);
    }

    WasdiApplicationDetailsController.$inject = [
        '$scope',
        '$state',
        'ConstantsService',
        'AuthService',
        'ProcessorService',
        'ProcessorMediaService',
        'ModalService',
        'ProcessWorkspaceService'
    ];

    return WasdiApplicationDetailsController;
}) ();
