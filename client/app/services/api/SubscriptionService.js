/**
 * Created by p.petrescu on 19/01/2023.
 */

'use strict';
angular.module('wasdi.SubscriptionService', ['wasdi.SubscriptionService']).
service('SubscriptionService', ['$http',  'ConstantsService', function ($http, oConstantsService) {
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;

    this.getSubscriptionsListByUser = function () {
        return this.m_oHttp.get(this.APIURL + '/subscriptions/byuser');
    };

    this.getSubscriptionById = function (sSubscriptionId) {
        return this.m_oHttp.get(this.APIURL + '/subscriptions/byId?subscription=' + sSubscriptionId);
    };

    this.saveSubscription = function (oSubscription) {
        if (utilsIsStrNullOrEmpty(oSubscription.subscriptionId)) {
            return this.createSubscription(oSubscription);
        } else {
            return this.updateSubscription(oSubscription);
        }
    };

    this.createSubscription = function (oSubscription) {
        return this.m_oHttp.post(this.APIURL + '/subscriptions/add', oSubscription);
    };

    this.updateSubscription = function (oSubscription) {
        return this.m_oHttp.put(this.APIURL + '/subscriptions/update', oSubscription);
    };

    this.deleteSubscription = function (sSubscriptionId) {
        return this.m_oHttp.delete(this.APIURL + '/subscriptions/delete?subscription=' + sSubscriptionId);
    };

    // Get list of shared users by subscription id
    this.getUsersBySharedSubscription = function (sSubscriptionId) {
        return this.m_oHttp.get(this.APIURL + '/subscriptions/share/bysubscription?subscription=' + sSubscriptionId);
    }

    // Add sharing
    this.addSubscriptionSharing = function (sSubscriptionId, sUserId) {
        return this.m_oHttp.post(this.APIURL + '/subscriptions/share/add?subscription=' + sSubscriptionId + '&userId=' + sUserId);
    }

    // Remove sharing
    this.removeSubscriptionSharing = function (sSubscriptionId, sUserId) {
        return this.m_oHttp.delete(this.APIURL + '/subscriptions/share/delete?subscription=' + sSubscriptionId + '&userId=' + sUserId);
    }

    // Get Subscription Types list
    this.getSubscriptionTypes = function () {
        return this.m_oHttp.get(this.APIURL + '/subscriptions/types');
    };

    // Get Stripe payment url by subscription id
    this.getStripePaymentUrl = function (sSubscriptionId, sWorkspaceId) {
        return this.m_oHttp.get(this.APIURL + '/subscriptions/stripe/paymentUrl?subscription=' + sSubscriptionId + '&workspace=' + sWorkspaceId);
    }

}]);
