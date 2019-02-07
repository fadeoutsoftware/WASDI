var MessageHelper = /** @class */ (function () {
    function MessageHelper(rootScope) {
        this._rootScopeRef = rootScope;
    }
    MessageHelper.getInstance = function (rootScope) {
        // @ts-ignore
        if (_.isNil(MessageHelper._instance) == true) {
            MessageHelper._instance = new MessageHelper(rootScope);
        }
        return MessageHelper._instance;
    };
    MessageHelper.getInstanceWithAnyScope = function (scope) {
        var tmpScope = scope;
        // @ts-ignore
        while (_.isNil(tmpScope.$parent) == false) {
            tmpScope = tmpScope.$parent;
        }
        return MessageHelper.getInstance(tmpScope);
    };
    MessageHelper.prototype.sendBroadcastMessage = function (messageId, params) {
        this._rootScopeRef.$broadcast(messageId, params);
    };
    MessageHelper.prototype.notifyRabbitConnectionStateChange = function (connectionState) {
        var params = {
            connectionState: connectionState
        };
        this.sendBroadcastMessage('rabbitConnectionStateChanged', params);
    };
    /**
     * Method to subscribe to 'rabbit connection state change' messages.
     * @param fn The function to execute on message received. To this function this params will be given 'event, args'
     */
    MessageHelper.prototype.subscribeToRabbitConnectionStateChange = function (fn) {
        // @ts-ignore
        if (_.isFunction(fn) == true) {
            this._rootScopeRef.$on('rabbitConnectionStateChanged', fn);
        }
    };
    return MessageHelper;
}());
