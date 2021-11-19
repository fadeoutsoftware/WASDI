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
        this.m_bIsUploading = false;
        var oController = this;
        $scope.close = function() {
            oClose("close", 500); // close, but give 500ms for bootstrap to animate
        };
        $scope.run = function() {
            oController.uploadFile();
            // oClose("close", 500); // close, but give 500ms for bootstrap to animate
        };

    }

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

        this.m_bIsUploading = true;
        var oController = this;
        this.m_oProductService.uploadFile(this.m_sWorkspaceId,oBody,this.m_oFile[0].name)
            .then(function(data){
                if(data.status !== 200)
                {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN UPLOAD FILE");
                }
                else
                {
                    var oDialog = utilsVexDialogAlertBottomRightCorner("FILE UPLOADED");
                    utilsVexCloseDialogAfter(5000,oDialog);
                }
                oController.cleanDragAndDrop();
                oController.m_bIsUploading = false;
            },(function(data){
                oController.m_bIsUploading = false;
                oController.cleanDragAndDrop();
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN UPLOAD FILE");

            }));

        return true;
    }
    UploadFileController.prototype.cleanDragAndDrop = function()
    {
        this.m_oFile[0] = null;
    }
    UploadFileController.$inject = [
        '$scope',
        'close',
        'extras',
        'ProductService'
    ];
    return UploadFileController;
})();
window.UploadFileController = UploadFileController;
