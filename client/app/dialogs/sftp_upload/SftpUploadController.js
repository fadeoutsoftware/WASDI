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
        this.getListFiles();
    }
    SftpUploadController.prototype.isCreatedAccountUpload = function()
    {
        this.m_oAuthService.isCreatedAccountUpload().success(function (data, status) {
            if (data != null) {
                if (data != undefined) {

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
