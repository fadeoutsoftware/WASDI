/**
 * Created by a.corrado on 24/05/2017.
 */



var SftpUploadController = (function() {

    function SftpUploadController($scope, oClose,oExtras,oAuthService) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oOrbit = oExtras;
        this.m_oAuthService = oAuthService;
        this.m_aoListOfFiles = [];
        //$scope.close = oClose;
        $scope.close = function(result) {
            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };
        this.isCreatedAccountUpload();
        // this.getListFiles();
        // this.createAccountUpload();
    }
    SftpUploadController.prototype.createAccountUpload = function()
    {
        var sTestEmail = "a.corrado@fadeout.it";
        this.m_oAuthService.createAccountUpload(sTestEmail).success(function (data, status) {
            if (data != null) {
                if (data != undefined) {

                }
            }
        }).error(function (data, status) {
            if(data);
        });
    };

    SftpUploadController.prototype.isCreatedAccountUpload = function()
    {
        this.m_oAuthService.isCreatedAccountUpload().success(function (data, status) {
            if (data != null) {
                if (data != undefined) {
                    if(data === false) {
                        //TODO first user access
                    }
                    else
                    {
                        //TODO old user
                    }
                }
            }
        }).error(function (data, status) {
            if(data);
        });
    }
    SftpUploadController.prototype.getListFiles = function ()
    {
        var oThat = this;
        this.m_oAuthService.getListFilesUpload().success(function (data, status) {
            if (data != null) {
                if (data != undefined) {
                    oThat.m_aoListOfFiles = data;
                }
            }
        }).error(function (data, status) {
            if(data);
        });
    }

    SftpUploadController.$inject = [
        '$scope',
        'close',
        'extras',
        'AuthService'
    ];
    return SftpUploadController;
})();
