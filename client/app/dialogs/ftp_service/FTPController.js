/**
 * Created by a.corrado on 23/05/2017.
 */
var FTPController = (function() {

    function FTPController($scope, oClose,oExtras,oCatalogService,oConstantsService) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oCatalogService = oCatalogService ;
        this.m_oConstantsService = oConstantsService;
        this.m_oProduct = this.m_oExtras.selectedProduct;

        this.m_oFtpRequest = {
            user:"",
            password:"",
            serverIp:"",
            port:"22",
            sftp: true,
            destinationAbsolutePath:""
        }
        /*metadataAttributes:node.original.attributes*/
        //$scope.close = oClose;
        $scope.close = function(result) {
            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };

    }

    FTPController.prototype.sendFTPUploadRequest = function()
    {

        var oActiveWorkspace = this.m_oConstantsService.getActiveWorkspace()

        if(this.isDataFtpRequestValid() === false  )
        {
            utilsVexDialogAlertTop("GURU MEDITATION<br>INVALID DATA FTP");
            return false;
        }

        if(oActiveWorkspace === null)
        {
            utilsVexDialogAlertTop("GURU MEDITATION<br>YOU SHOULD SELECT AN ACTIVE WORKSPACE");
            return false;
        }
        var oFtpTransferFile = {
            server:this.m_oFtpRequest.serverIp,
            port:this.m_oFtpRequest.port,
            user:this.m_oFtpRequest.user,
            password:this.m_oFtpRequest.password,
            fileName:this.m_oProduct.fileName,
            destinationAbsolutePath:this.m_oFtpRequest.destinationAbsolutePath,
            // workspace:oActiveWorkspace.workspaceId
        };

        oFtpTransferFile.server = this.m_oFtpRequest.serverIp;

        this.m_oCatalogService.uploadFTPFile(oFtpTransferFile,oActiveWorkspace.workspaceId)
            .then(function(data,status){
                if(utilsIsObjectNullOrUndefined(data.data) === false && data.data.boolValue === true)
                {
                    console.log("done");
                }
                else
                {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR TRANSFERRING FILE TO FTP");
                    // oController.cleanFormInputData();

                }
                // oController.cleanFormInputData();

            },function(data,status){
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR TRANSFERRING FILE TO FTP");
                //oController.cleanFormInputData();
            });
        return true;
    };

    FTPController.prototype.getFileName = function(){
        if (this.m_oProduct != null) {
            return this.m_oProduct.fileName;
        }
        else {
            return "N.A.";
        }
    }

    FTPController.prototype.isDataFtpRequestValid = function(){

        if(utilsIsStrNullOrEmpty(this.m_oFtpRequest.user) === true)
        {
            return false;
        }
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
            port:"22",
            sftp: true,
            destinationAbsolutePath:""
        }
    };
    FTPController.$inject = [
        '$scope',
        'close',
        'extras',
        'CatalogService',
        'ConstantsService'
    ];
    return FTPController;
})();
window.FTPController=FTPController;
