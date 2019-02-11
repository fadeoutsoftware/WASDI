/**
 * Created by a.corrado on 31/03/2017.
 */


var UploadFileController = (function() {
    UploadFileController.REG_NAME = "UploadFileController";

    function UploadFileController($scope, oClose,oExtras,oProductService) {
        //MEMBERS
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oProductService = oProductService;
        this.m_oFile = null;
        this.m_sWorkspaceId = this.m_oExtras.workflowId;

        var oController = this;
        $scope.close = function() {
            oClose("close", 500); // close, but give 500ms for bootstrap to animate
        };
        $scope.run = function() {
            oController.uploadFile();
            // oClose("close", 500); // close, but give 500ms for bootstrap to animate
        };

    };

    UploadFileController.prototype.isFileChosen = function()
    {
        return ( utilsIsObjectNullOrUndefined(this.m_oFile) || utilsIsObjectNullOrUndefined(this.m_oFile[0]));
    };

    UploadFileController.prototype.uploadFile = function(){
        if( utilsIsObjectNullOrUndefined(this.m_oFile) || utilsIsObjectNullOrUndefined(this.m_oFile[0]) )
        {
            return false;
        }
        if(utilsIsObjectNullOrUndefined(this.m_sWorkspaceId) || utilsIsStrNullOrEmpty(this.m_sWorkspaceId))
        {
            return false;
        }
        var oBody = new FormData();
        oBody.append('file', this.m_oFile[0]);

        this.m_oProductService.uploadFile(this.m_sWorkspaceId,oBody)
            .success(function(data,status){

            })
            .error(function(data){

            });
        
        return true;
    }
    UploadFileController.$inject = [
        '$scope',
        'close',
        'extras',
        'ProductService'
    ];
    return UploadFileController;
})();
