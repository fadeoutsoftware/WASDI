/**
 * Created by a.corrado on 24/05/2017.
 */

var UploadController = (function() {

    function UploadController($scope, oClose,oExtras,oAuthService,oConstantsService,oCatalogService,oProductService, oStyleService, $state) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oAuthService = oAuthService;
        this.m_oCatalogService = oCatalogService;
        this.m_oConstantsService = oConstantsService;
        this.m_oProductService = oProductService;
        this.m_oStyleService = oStyleService;
        this.m_aoListOfFiles = [];
        this.m_bIsAccountCreated = null;
        this.m_sEmailNewPassword="";
        this.m_sEmailNewUser="";

        this.m_oUser = this.m_oConstantsService.getUser();
        this.m_bIsVisibleLoadIcon = false;
        this.m_aoSelectedFiles = [];
        this.m_sTabSelected = "Upload";
        this.m_bIsUploading = false;
        this.m_sWorkspaceId = oExtras.WorkSpaceId;
        this.m_oStyle = "";
        this.m_asStyles = [];
        this.m_aoStylesMap = {};
        this.m_oState = $state;
        this.m_aoActiveSubscriptions = this.m_oConstantsService.getActiveSubscriptions();

        this.isCreatedAccountUpload();

        var oController = this;

        this.m_oStyleService.getStylesByUser().then(function(data) {
                if(data.status !== 200)
                {
                    var oDialog = utilsVexDialogAlertBottomRightCorner("GURU MEDITATION<br>ERROR GETTING STYLES");
                    utilsVexCloseDialogAfter(5000,oDialog);
                }
                else {
                    oController.m_asStyles = data.data.map(item => item.name);
                    oController.m_aoStylesMap = oController.m_asStyles.map(name => ({name}))
                }
            },function(data){
                var oDialog = utilsVexDialogAlertBottomRightCorner("GURU MEDITATION<br>ERROR GETTING STYLES");
                utilsVexCloseDialogAfter(5000,oDialog);
        });

        $scope.close = function(result) {
            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };
        $scope.run = function() {
           
            oController.uploadFile();
            // oClose("close", 500); // close, but give 500ms for bootstrap to animate
        };

    }
    /*************** UPLOAD ***************/
    UploadController.prototype.isFileChosen = function()
    {
        return ( utilsIsObjectNullOrUndefined(this.m_oFile) || utilsIsObjectNullOrUndefined(this.m_oFile[0]));
    };

    UploadController.prototype.uploadFile = function(){
        let oController = this;

        let oActiveProject = this.m_oConstantsService.getActiveProject();
       

        if(this.m_aoActiveSubscriptions.length === 0) {
            let sMessage = "You do not have an Active Subscription right now.<br>Click 'OK' to visit our purchase page";
            utilsVexDialogConfirm(sMessage, function(value) {
                if(value) {
                    oController.m_oState.go("root.subscriptions", {});
                }
            }); 
            return false;
        }

        if(utilsIsObjectNullOrUndefined(oActiveProject)) {
            utilsVexDialogAlertTop("You do not have an active project right now.<br>Please make a selection.")
            return false;
        }

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

        var sStyle = "";

        if (!utilsIsObjectNullOrUndefined(this.m_oStyle)) {
            sStyle = this.m_oStyle.name;
        }

        this.m_oProductService.uploadFile(this.m_sWorkspaceId,oBody,this.m_oFile[0].name, sStyle)
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
            },function(data){
                oController.m_bIsUploading = false;
                oController.cleanDragAndDrop();
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN UPLOAD FILE");

            });

        return true;
    }
    UploadController.prototype.cleanDragAndDrop = function()
    {
        this.m_oFile[0] = null;
    }


    /********************* SFTP ********************/

    UploadController.prototype.deleteSftpAccount = function(){
        //TODO check if there is the user
        if(utilsIsObjectNullOrUndefined(this.m_oUser)=== true ||utilsIsObjectNullOrUndefined(this.m_oUser.userId)=== true)
            return false;
        var oController = this;
        this.m_bIsVisibleLoadIcon = true;
        this.m_oAuthService.deleteAccountUpload(this.m_oUser.userId).then(function (data, status) {
            if (data.data != null) {
                if (data.data != undefined) {
                    oController.isCreatedAccountUpload();
                    oController.m_bIsVisibleLoadIcon = false;
                }
            }
        },function (data, status) {
            if(data) console.log("SftpUploadController error during delete account");
            this.m_bIsVisibleLoadIcon = false;
        });
        return true;
    };
    UploadController.prototype.updateSftpPassword = function(){
        if(utilsIsObjectNullOrUndefined(this.m_sEmailNewPassword) === true || utilsIsStrNullOrEmpty(this.m_sEmailNewPassword) === true )
            return false;
        this.m_bIsVisibleLoadIcon = true;
        var oController = this;
        this.m_oAuthService.updatePasswordUpload(this.m_sEmailNewPassword).then(function (data, status) {
            oController.m_bIsVisibleLoadIcon = false;
        },function (data, status) {
            if(data.data)
            {
                console.log("SftpUploadController error during creation new password");
            }
            oController.m_bIsVisibleLoadIcon = false;
        });

        return true;
    };

    UploadController.prototype.createAccountUpload = function()
    {
        if(utilsIsObjectNullOrUndefined(this.m_sEmailNewUser) === true || utilsIsStrNullOrEmpty(this.m_sEmailNewUser) === true || utilsIsEmail(this.m_sEmailNewUser)=== false )
            return false;
        var oController = this;

        this.m_bIsVisibleLoadIcon = true;
        this.m_oAuthService.createAccountUpload(this.m_sEmailNewUser).then(function (data, status) {
            if (data.data !== null || status === 200) {
                if (data.data !== undefined) {
                    oController.isCreatedAccountUpload();
                }
            }
            else
            {
                utilsVexDialogAlertTop("ERROR SERVER WAS IMPOSSIBLE CREATE ACCOUNT");
            }
            oController.m_bIsVisibleLoadIcon = false;
        },function (data, status) {
            utilsVexDialogAlertTop("ERROR SERVER");
            oController.m_bIsVisibleLoadIcon = false;
        });
    };

    UploadController.prototype.isCreatedAccountUpload = function()
    {
        var oController = this;
        this.m_oAuthService.isCreatedAccountUpload().then(function (data, status) {
            if (data.data != null) {
                if (data.data != undefined) {
                    if(data.data === false) {
                        oController.m_bIsAccountCreated = false;
                    }
                    else
                    {
                        oController.m_bIsAccountCreated = true;
                        oController.getListFiles();
                    }
                }
            }
        },function (data, status) {
            if(data.data)
                console.log("SftpUploadController error during check if the account is created");
        });
    }
    UploadController.prototype.getListFiles = function ()
    {
        var oThat = this;
        this.m_oAuthService.getListFilesUpload().then(function (data, status) {
            if (data.data != null) {
                if (data.data != undefined) {
                    oThat.m_aoListOfFiles = data.data;
                }
            }
        },function (data, status) {
            if(data)
                console.log("SftpUploadController error during get-list");
        });
    };

    UploadController.prototype.isVisibleLoadIcon = function(){
        if(utilsIsObjectNullOrUndefined(this.m_bIsAccountCreated) === true || this.m_bIsVisibleLoadIcon === true)
        {
            return true;
        }

        return false;

    };

    UploadController.prototype.isAccountCreated = function(){
        if( this.m_bIsAccountCreated)
        {
            return true;
        }

        return false;


    };

    UploadController.prototype.ingestAllSelectedFiles = function () {
        var iSelectedFilesLength = this.m_aoSelectedFiles.length;

        for(var iIndexSelectedFile = 0; iIndexSelectedFile < iSelectedFilesLength ; iIndexSelectedFile++){
            if(this.ingestFile(this.m_aoSelectedFiles[iIndexSelectedFile]) === false)
                console.log("Something doesn't work during ingest File, indexFile:" + iIndexSelectedFile);
        }
    };

    /**
     * ingestFile
     * @param oSelectedFile
     * @returns {boolean}
     */
    UploadController.prototype.ingestFile = function(oSelectedFile){
        if(utilsIsObjectNullOrUndefined(oSelectedFile)=== true )
            return false;
        this.m_oCatalogService.ingestFile(oSelectedFile,this.m_oConstantsService.getActiveWorkspace().workspaceId)
            .then(function (data, status) {

        },function (data, status) {
            if(data.data)
            {
                console.log("SftpUploadController error during ingest file");
                utilsVexDialogAlertTop("GURU MEDITATION<br>INGESTION ERROR FILE:<br>" + oSelectedFile);
            }
        });
        return true;
    };
    UploadController.$inject = [
        '$scope',
        'close',
        'extras',
        'AuthService',
        'ConstantsService',
        'CatalogService',
        'ProductService',
        'StyleService', 
        '$state'
    ];
    return UploadController;
})();
window.UploadController = UploadController;
