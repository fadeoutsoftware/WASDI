/**
 * Created by a.corrado on 23/05/2017.
 */
var FTPController = (function() {

    function FTPController($scope, oClose,oExtras,oCatalogService) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oCatalogService = oCatalogService ;
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
        var oFtpTransferFile={
            server:"",
            port:"",
            user:"",
            password:"",
            fileName:"",
            destinationAbsolutePath:""
        };
        var oController = this;
        if(this.isDataFtpRequestValid() === false)
        {
            return false;
        }

        var oFtpTransferFile = {
            server:this.m_oFtpRequest.serverIp,
            port:this.m_oFtpRequest.port,
            user:this.m_oFtpRequest.user,
            password:this.m_oFtpRequest.password,
            fileName:this.m_oProduct.fileName,
            destinationAbsolutePath:""
        };

        oFtpTransferFile.server = this.m_oFtpRequest.serverIp;

        this.m_oCatalogService.uploadFTPFile(oFtpTransferFile)
            .success(function(data,status){
                if(utilsIsObjectNullOrUndefined(data) === false && data.boolValue === true)
                {
                    console.log("done");
                }
                else
                {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>UPLOAD FTP FILE ERROR");
                }
                oController.cleanFormInputData();

            })
            .error(function(data,status){
                utilsVexDialogAlertTop("GURU MEDITATION<br>UPLOAD FTP FILE ERROR");
                oController.cleanFormInputData();
            });
        return true;
    };

    FTPController.prototype.isDataFtpRequestValid = function(){

        if(utilsIsStrNullOrEmpty(this.m_oFtpRequest.user) === true)
        {
            return false;
        }
        // if(utilsIsStrNullOrEmpty(this.m_oFtpRequest.password)  === true)
        // {
        //     return false;
        // }
        if(utilsIsStrNullOrEmpty(this.m_oFtpRequest.serverIp)  === true)
        {
            return false;
        }
        if(utilsIsStrNullOrEmpty(this.m_oFtpRequest.port)  === true)
        {
            return false;
        }
        if(utilsIsObjectNullOrUndefined(this.m_oProduct) === true || utilsIsStrNullOrEmpty(this.m_oProduct.fileName))
        {
            return false;
        }

        return true;
    };

    FTPController.prototype.cleanFormInputData = function()
    {
        this.m_oFtpRequest = {
            user:"",
            password:"",
            serverIp:"",
            port:"",
        }
    };
    FTPController.$inject = [
        '$scope',
        'close',
        'extras',
        'CatalogService'
    ];
    return FTPController;
})();
