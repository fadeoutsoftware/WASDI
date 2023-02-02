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
     * @param oTranslate
     * @constructor
     */
    function WasdiApplicationDetailsController($scope, $state, oConstantsService, oAuthService, oProcessorService,
        oProcessorMediaService, oModalService, oProcessWorkspaceService, oTranslate, oImagesService) {
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
         * Reference to images service
         */
        this.m_oImagesService = oImagesService;

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
         * Selected review
         * @type {*}
         */
        this.m_oSelectedReview = this.m_oConstantsService.getSelectedReview();

        /**
         * The selected comment
         * @type {*}
         */
        this.m_oSelectedComment = this.m_oConstantsService.getSelectedComment();

        /**
         * array of images
         * @type {*[]}
         */
        this.m_asImages = [];

        /**
         * Translate Service
         */
        this.m_oTranslate = oTranslate;

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

        var sDataErrorMsg = this.m_oTranslate.instant("MSG_MKT_DATA_ERROR");
        /**
         * Ask the list of Applications to the WASDI server
         */
        this.m_oProcessorService.getMarketplaceDetail(this.m_sSelectedApplication).then(function (data) {
            if(utilsIsObjectNullOrUndefined(data.data) == false)
            {
                oController.m_oApplication = data.data;
                if (utilsIsStrNullOrEmpty(oController.m_oApplication.logo)) {
                    oController.m_asImages.push(oController.m_oApplication.imgLink);
                }
                else {
                    var sUrl = oController.m_oImagesService.getImageLink(oController.m_oApplication.logo);
                    oController.m_asImages.push(sUrl);
                }
                
                if (oController.m_oApplication.images.length>0) {

                    for (var iImage = 0; iImage < oController.m_oApplication.images.length; iImage++) {
                        var sImageUrl = oController.m_oApplication.images[iImage];
                        var sUrl = oController.m_oImagesService.getImageLink(sImageUrl);
                        oController.m_asImages.push(sUrl);
                    }                    
                }
            }
            else
            {
                utilsVexDialogAlertTop(sDataErrorMsg);
            }
            oController.m_bWaiting=false;
        },function (error) {
            utilsVexDialogAlertTop(sDataErrorMsg);
            oController.m_bWaiting=false;
        });

        var sStatErrorMsg = this.m_oTranslate.instant("MSG_MKT_STATS_ERROR");
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
                utilsVexDialogAlertTop(sStatErrorMsg);
            }
        },(function (error) {
            utilsVexDialogAlertTop(sStatErrorMsg);
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

        var sReviewsErrorMsg = this.m_oTranslate.instant("MSG_MKT_REVIEWS_ERROR");

        this.m_oProcessorMediaService.getProcessorReviews(this.m_sSelectedApplication, this.m_iReviewsPage, this.m_iReviewItemsPerPage = 4).then(function (data) {
            if(utilsIsObjectNullOrUndefined(data.data) == false)
            {
                oController.m_oReviewsWrapper = data.data;
                if (data.data.reviews.length == 0) oController.m_bShowLoadMoreReviews = false;
            }
            else
            {
                utilsVexDialogAlertTop(sReviewsErrorMsg);
            }

            oController.m_bReviewsWaiting = false;
        },function (error) {
            utilsVexDialogAlertTop(sReviewsErrorMsg);
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

        var sCommentsErrorMsg = this.m_oTranslate.instant("MSG_MKT_COMMENTS_ERROR");

        this.m_oProcessorMediaService.getReviewComments(sSelectedReviewId).then(function (data) {
            if(utilsIsObjectNullOrUndefined(data.data) == false)
            {
                oController.m_oCommentsWrapper[oController.m_sSelectedReviewId] = data.data;

                if (data.data.length == 0) oController.m_bShowLoadComments = false;
            }
            else
            {
                utilsVexDialogAlertTop(sCommentsErrorMsg);
            }

            oController.m_bCommentsWaiting = false;
        },function (error) {
            utilsVexDialogAlertTop(sCommentsErrorMsg);
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

        var sReviewsErrorMsg = this.m_oTranslate.instant("MSG_MKT_REVIEWS_ERROR");

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
                utilsVexDialogAlertTop(sReviewsErrorMsg);
            }
            oController.m_bReviewsWaiting = false;
        },function (error) {
            utilsVexDialogAlertTop(sReviewsErrorMsg);
            oController.m_bReviewsWaiting = false;
        });
    }

    /**
     * Add a new review
     */
    WasdiApplicationDetailsController.prototype.addNewReview = function () {
        var oController = this;

        this.m_oUserReview.processorId = this.m_oApplication.processorId;

        var sSavedMsg = this.m_oTranslate.instant("MSG_MKT_REVIEW_SAVED");
        var sErrorMsg = this.m_oTranslate.instant("MSG_MKT_REVIEW_SAVE_ERROR");

        this.m_oProcessorMediaService.addProcessorReview(this.m_oUserReview).then(function (data) {
            oController.m_oUserReview.title="";
            oController.m_oUserReview.comment="";
            oController.m_oUserReview.vote=-1;

            var oDialog = utilsVexDialogAlertBottomRightCorner(sSavedMsg);
            utilsVexCloseDialogAfter(4000,oDialog);

            oController.m_iReviewsPage = 0;
            oController.refreshReviews();

        },function (error) {
            oController.m_oUserReview.title="";
            oController.m_oUserReview.comment="";
            oController.m_oUserReview.vote=-1;

            utilsVexDialogAlertTop(sErrorMsg);
        });
    }

    /**
     * Updates a review
     */
    WasdiApplicationDetailsController.prototype.updateReview = function () {
        var oController = this;

        this.m_oUserReview.processorId = this.m_oApplication.processorId;

        this.m_oUserReview.id = this.m_oSelectedReview.id;
        this.m_oSelectedReview = null;

        var sSavedMsg = this.m_oTranslate.instant("MSG_MKT_REVIEW_UPDATED");
        var sErrorMsg = this.m_oTranslate.instant("MSG_MKT_REVIEW_UPDATE_ERROR");        

        this.m_oProcessorMediaService.updateProcessorReview(this.m_oUserReview).then(function (data) {
            oController.m_oUserReview.title="";
            oController.m_oUserReview.comment="";
            oController.m_oUserReview.vote=-1;

            var oDialog = utilsVexDialogAlertBottomRightCorner(sSavedMsg);
            utilsVexCloseDialogAfter(4000,oDialog);

            oController.m_iReviewsPage = 0;
            oController.refreshReviews();

        },function (error) {
            oController.m_oUserReview.title="";
            oController.m_oUserReview.comment="";
            oController.m_oUserReview.vote=-1;

            utilsVexDialogAlertTop(sErrorMsg);
        });
    }

    WasdiApplicationDetailsController.prototype.setSelectedReviewId = function (reviewId) {
        this.m_sSelectedReviewId = reviewId;
        this.m_oReview.reviewId = reviewId;
    }

    
    WasdiApplicationDetailsController.prototype.setSelectedReview = function (review) {
        this.m_oSelectedReview = review;

        if (review) {
            this.m_oUserReview = {
                id: review.id,
                title: review.title,
                comment: review.comment,
                vote: review.vote
            }
        } else {
            this.m_oUserReview = {
                id: "",
                title: "",
                comment: "",
                vote: -1
            }
        }
    }

    WasdiApplicationDetailsController.prototype.setSelectedComment = function (comment) {
        this.m_oSelectedComment = comment;

        if (comment) {
            this.m_oReviewComment = {
                reviewId: comment.reviewId,
                text: comment.text
            }
        } else {
            this.m_oReviewComment = {
                reviewId: "",
                text: ""
            }
        }
    }

    /**
     * Add a new comment to a review
     */
    WasdiApplicationDetailsController.prototype.addNewComment = function () {
        var oController = this;

        this.m_oReviewComment.reviewId = this.m_oReview.reviewId;

        var sSavedMsg = this.m_oTranslate.instant("MSG_MKT_COMMENT_SAVED");
        var sErrorMsg = this.m_oTranslate.instant("MSG_MKT_COMMENT_SAVE_ERROR");             

        this.m_oProcessorMediaService.addReviewComment(this.m_oReviewComment).then(function (data) {

            oController.m_oReviewComment.text="";

            var oDialog = utilsVexDialogAlertBottomRightCorner(sSavedMsg);
            utilsVexCloseDialogAfter(4000,oDialog);

            oController.m_iCommentsPage = 0;
            oController.refreshComments(oController.m_oReviewComment.reviewId);

        },function (error) {
            oController.m_oReviewComment.text="";

            utilsVexDialogAlertTop(sErrorMsg);
        });
    }

    /**
     * Updates a comment
     */
    WasdiApplicationDetailsController.prototype.updateComment = function () {
        var oController = this;

        this.m_oReviewComment.commentId = this.m_oSelectedComment.commentId;
        this.m_oSelectedComment = null;

        var sSavedMsg = this.m_oTranslate.instant("MSG_MKT_COMMENT_UPDATED");
        var sErrorMsg = this.m_oTranslate.instant("MSG_MKT_COMMENT_UPDATED_ERROR");                 

        this.m_oProcessorMediaService.updateReviewComment(this.m_oReviewComment).then(function (data) {

            oController.m_oReviewComment.text = "";
            var oDialog = utilsVexDialogAlertBottomRightCorner(sSavedMsg);
            utilsVexCloseDialogAfter(4000,oDialog);

            oController.m_iCommentsPage = 0;
            oController.refreshComments(oController.m_oReviewComment.reviewId);

        },function (error) {
            oController.m_oReviewComment.text = "";

            utilsVexDialogAlertTop(sErrorMsg);
        });
    }

    /**
     * Deletes a review
     */
    WasdiApplicationDetailsController.prototype.deleteReview = function (sReviewId) {

        var oController = this;

        var sErrorMsg = this.m_oTranslate.instant("MSG_MKT_REVIEWS_ERROR");
        var sConfirmMsg = this.m_oTranslate.instant("MSG_MKT_REVIEW_DELETE_CONFIRM");

        var oDeleteReviewCallback = function (value) {

            if (value) {
                oController.m_oProcessorMediaService.deleteProcessorReview(oController.m_oApplication.processorId, sReviewId).then(function (data) {
                    oController.refreshReviews();
                },function (error) {
                    utilsVexDialogAlertTop(sErrorMsg);
                });

                return true;
            } else {
                return false;
            }
        };

        //ask user if he confirms to delete the review
        utilsVexDialogConfirm(sConfirmMsg, oDeleteReviewCallback);
    }

    /**
     * Deletes a comment
     */
    WasdiApplicationDetailsController.prototype.deleteComment = function (oComment) {
        var oController = this;
        this.m_oReviewComment.reviewId = oComment.reviewId;
        this.m_oReviewComment.commentId = oComment.commentId;

        var sErrorMsg = this.m_oTranslate.instant("MSG_MKT_COMMENTS_ERROR");
        var sConfirmMsg = this.m_oTranslate.instant("MSG_MKT_COMMENT_DELETE_CONFIRM");        

        var oDeleteCommentCallback = function (value) {

            if (value) {
                oController.m_oProcessorMediaService.deleteReviewComment(oController.m_oReviewComment.reviewId, oController.m_oReviewComment.commentId).then(function (data) {
                    oController.refreshComments(oController.m_oReviewComment.reviewId);
                },function (error) {
                    utilsVexDialogAlertTop(sErrorMsg);
                });

                return true;
            } else {
                return false;
            }
        };

        //ask user if he confirms to delete the review
        utilsVexDialogConfirm(sConfirmMsg, oDeleteCommentCallback);
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
            let asSplit = sImageName.split('&')
            
            for (var iLinkParts = 0; iLinkParts<asSplit.length; iLinkParts++) {
                if (asSplit[iLinkParts].includes("name=")) {
                    let asResplit = asSplit[iLinkParts].split("=");
                    let asFileNameParts = asResplit[1].split(".");
                    let sThumbFile = asFileNameParts[0];
                    sThumbFile= sThumbFile + "_thumb." + asFileNameParts[1];
                    asSplit[iLinkParts] = "name="+sThumbFile;
                    break;
                }
            }

            var sNewLink = asSplit[0];

            for (var iNewLink = 1; iNewLink<asSplit.length; iNewLink++) {
                sNewLink = sNewLink + "&" + asSplit[iNewLink];
            }

            return sNewLink;
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
        'ProcessWorkspaceService',
        '$translate',
        'ImagesService'
    ];

    return WasdiApplicationDetailsController;
}) ();
window.WasdiApplicationDetailsController = WasdiApplicationDetailsController;
