/**
 * Created by a.corrado on 16/06/2017.
 */



var ShareWorkspaceController = (function() {

    function ShareWorkspaceController($scope, oClose,oExtras,oConstantsService, oTranslate ,oWorkspaceService) {
        this.m_oScope = $scope;
        this.m_oClose = oClose;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oWorkspaceService = oWorkspaceService;

        this.m_oFile = null;
        this.m_sWorkspace = this.m_oExtras.workspace;
        this.m_asSelectedProducts = [];
        this.m_aoWorkflows = [];
        this.m_oSelectedWorkflow = null;
        this.m_oSelectedMultiInputWorkflow = null;
        this.m_sUserEmail = "";
        this.m_sRights = "read";
        this.m_aoEnableUsers=[];
        this.m_oConstantsService = oConstantsService;
        this.m_oTranslate = oTranslate;

        if(utilsIsObjectNullOrUndefined(this.m_sWorkspace) === true){
            this.m_sWorkspace = this.m_oConstantsService.getActiveWorkspace();
        }

        this.getListOfEnableUsers(this.m_sWorkspace.workspaceId);

        $scope.close = function(result) {

            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };
    }

    ShareWorkspaceController.prototype.getListOfEnableUsers = function(sWorkspaceId){

        if(utilsIsStrNullOrEmpty(sWorkspaceId) === true)
        {
            return false;
        }
        var oController = this;
        this.m_oWorkspaceService.getUsersBySharedWorkspace(sWorkspaceId)
            .then(function (data) {
                if(utilsIsObjectNullOrUndefined(data.data) === false)
                {
                    oController.m_aoEnableUsers = data.data;
                }
                else
                {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN SHARE WORKSPACE");
                }

            },function (error) {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN SHARE WORKSPACE");
            });
        return true;
    };

    ShareWorkspaceController.prototype.shareWorkspaceByUserEmail = function(sWorkspaceId,sEmail,sRights){

        if( (utilsIsObjectNullOrUndefined(sWorkspaceId) === true) || (utilsIsStrNullOrEmpty(sEmail) === true))
        {
            //TODO THROW ERROR ?
            return false;
        }

        if (utilsIsStrNullOrEmpty(sRights)) {
            sRights = "read"
        }

        utilsRemoveSpaces(sEmail);

        var oController = this;
        this.m_oWorkspaceService.putShareWorkspace(sWorkspaceId,sEmail, sRights).then(function (data) {

            if(utilsIsObjectNullOrUndefined(data.data) === false && data.data.boolValue === true)
            {
                //TODO USER SAVED
            }else
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN SHARE WORKSPACE");
            }
            oController.getListOfEnableUsers(oController.m_sWorkspace.workspaceId);

        },function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN SHARE WORKSPACE");
        });

        this.m_sUserEmail="";
        return true;
    };

    ShareWorkspaceController.prototype.disablePermissionsUsersByWorkspace = function(sWorkspaceId,sEmail){
        if( (utilsIsObjectNullOrUndefined(sWorkspaceId) === true) || (utilsIsStrNullOrEmpty(sEmail) === true))
        {
            //TODO THROW ERROR ?
            return false;
        }

        utilsRemoveSpaces(sEmail);
        var oController = this;
        this.m_oWorkspaceService.deleteUserSharedWorkspace(sWorkspaceId,sEmail)
            .then(function (data) {
                if(utilsIsObjectNullOrUndefined(data.data) === false && data.data.boolValue === true)
                {
                    //TODO USER SAVED
                }
                else
                {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN SHARE WORKSPACE");
                }

                oController.getListOfEnableUsers(oController.m_sWorkspace.workspaceId);

            },function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN SHARE WORKSPACE");
        });

        this.m_sUserEmail="";
        return true;
    };


    ShareWorkspaceController.$inject = [
        '$scope',
        'close',
        'extras',
        'ConstantsService',
        '$translate',
        'WorkspaceService'

    ];
    return ShareWorkspaceController;
})();
window.ShareWorkspaceController = ShareWorkspaceController;
