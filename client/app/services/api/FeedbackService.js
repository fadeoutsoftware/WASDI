/**
 * Created by PetruPetrescu on 15/03/2022.
 */

'use strict';
angular.module('wasdi.FeedbackService', ['wasdi.FeedbackService']).
service('FeedbackService', ['$http', 'ConstantsService', function ($http, oConstantsService) {
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;
    this.m_oController = this;
    this.m_oConstantService = oConstantsService;
    // header field for post calls
    this.m_oOptions = {
        transformRequest: angular.identity,
        headers: { 'Content-Type': undefined }
    };

    this.sendFeedback = function (oFeedback) {
        return this.m_oHttp.post(this.APIURL + '/wasdi/feedback', oFeedback);
    }
}]);
