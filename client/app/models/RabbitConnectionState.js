var RabbitConnectionState;
(function (RabbitConnectionState) {
    RabbitConnectionState[RabbitConnectionState["Init"] = 0] = "Init";
    RabbitConnectionState[RabbitConnectionState["Connected"] = 1] = "Connected";
    RabbitConnectionState[RabbitConnectionState["Lost"] = 2] = "Lost";
})(RabbitConnectionState || (RabbitConnectionState = {}));
