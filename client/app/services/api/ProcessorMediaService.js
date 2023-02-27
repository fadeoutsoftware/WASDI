/**
 *
 */

'use strict';
angular.module('wasdi.ProcessorMediaService', ['wasdi.ProcessorMediaService']).
service('ProcessorMediaService', ['ConstantsService','$rootScope','$http', function (oConstantsService,$rootScope,$http) {
    this.m_oConstantsService = oConstantsService;
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;
    this.m_sResource = "/processormedia";


    /**
     * Get the list of Application Categories
     * @returns {*}
     */
    this.getCategories = function() {
        return this.m_oHttp.get(this.APIURL + this.m_sResource + '/categories/get');
    };

    /**
     * Get the list of publisher for filtering
     * @returns {*}
     */
    this.getPublishersFilterList = function() {
        return this.m_oHttp.get(this.APIURL + this.m_sResource + '/publisher/getlist');
    };

    /**
     * Get the review summary of an application
     * @param sProcessorName
     * @param iPage
     * @param iItemsPerPage
     * @returns {*}
     */
    this.getProcessorReviews = function (sProcessorName, iPage, iItemsPerPage) {
        return this.m_oHttp.get(this.APIURL + this.m_sResource + '/reviews/getlist?processorName='+sProcessorName+'&page='+iPage+"&itemsPerPage="+iItemsPerPage);
    }

    /**
     * Get the comments list of a review
     * @param sReviewId
     * @param iPage
     * @param iItemsPerPage
     * @returns {*}
     */
    this.getReviewComments = function (sReviewId, iPage, iItemsPerPage) {
        return this.m_oHttp.get(this.APIURL + this.m_sResource + '/comments/getlist?reviewId='+sReviewId+'&page='+iPage+"&itemsPerPage="+iItemsPerPage);
    }

    /**
     * Add a new Review
     * @param oReview
     * @returns {*}
     */
    this.addProcessorReview = function (oReview) {
        return this.m_oHttp.post(this.APIURL + this.m_sResource + '/reviews/add', oReview);
    }

    /**
     * Add a new Comment to a Review
     * @param oComment
     * @returns {*}
     */
    this.addReviewComment = function (oComment) {
        return this.m_oHttp.post(this.APIURL + this.m_sResource + '/comments/add', oComment);
    }

    /**
     * Update a Review
     * @param oReview
     * @returns {*}
     */
    this.updateProcessorReview = function (oReview) {
        return this.m_oHttp.post(this.APIURL + this.m_sResource + '/reviews/update', oReview);
    }

    /**
     * Update a Comment of a Review
     * @param oComment
     * @returns {*}
     */
    this.updateReviewComment = function (oComment) {
        return this.m_oHttp.post(this.APIURL + this.m_sResource + '/comments/update', oComment);
    }

    /**
     * Delete a review
     * @param oReview
     * @returns {*}
     */
    this.deleteProcessorReview = function (sProcessorId, sReviewId) {
        return this.m_oHttp.delete(this.APIURL + this.m_sResource + '/reviews/delete?processorId='+ encodeURI(sProcessorId)+'&reviewId='+ encodeURI(sReviewId));
    }

    /**
     * Delete a Comment of a Review
     * @param oComment
     * @returns {*}
     */
    this.deleteReviewComment = function (sReviewId, sCommentId) {
        return this.m_oHttp.delete(this.APIURL + this.m_sResource + '/comments/delete?reviewId='+ encodeURI(sReviewId)+'&commentId='+ encodeURI(sCommentId));
    }

}]);
