

class MessageHelper
{
    //public static

    private static _instance;

    private _rootScopeRef;

    public static getInstance(rootScope)
    {
        // @ts-ignore
        if (_.isNil(MessageHelper._instance) == true)
        {
            MessageHelper._instance = new MessageHelper(rootScope)
        }
        return MessageHelper._instance
    }

    public static getInstanceWithAnyScope(scope)
    {
        let tmpScope = scope;
        // @ts-ignore
        while( _.isNil(tmpScope.$parent) == false)
        {
            tmpScope = tmpScope.$parent;
        }

        return MessageHelper.getInstance(tmpScope);
    }

    private constructor(rootScope)
    {
        this._rootScopeRef = rootScope;
    }

    public sendBroadcastMessage(messageId, params)
    {
        this._rootScopeRef.$broadcast(messageId, params);
    }

    public notifyRabbitConnectionStateChange(connectionState )
    {
        var params = {
            connectionState: connectionState
        }
        this.sendBroadcastMessage('rabbitConnectionStateChanged', params)
    }

    /**
     * Method to subscribe to 'rabbit connection state change' messages.
     * @param fn The function to execute on message received. To this function this params will be given 'event, args'
     */
    public subscribeToRabbitConnectionStateChange(fn)
    {
        // @ts-ignore
        if( _.isFunction(fn) == true){
            this._rootScopeRef.$on('rabbitConnectionStateChanged', fn);
        }
    }

}
