/**
 * Created by a.corrado on 23/05/2017.
 */
var FTPController = (function() {

    function FTPController($scope, oClose,oExtras) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oProduct = this.m_oExtras.product;

        this.m_oFtpRequest = {
            user:"",
            password:"",
            serverIp:"",
            port:"",
        }
        /*metadataAttributes:node.original.attributes*/
        //$scope.close = oClose;
        $scope.close = function(result) {
            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };

    }

    FTPController.prototype.sendFTPUploadRequest = function()
    {

    };

    FTPController.$inject = [
        '$scope',
        'close',
        'extras',
    ];
    return FTPController;
})();
