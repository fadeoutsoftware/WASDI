/**
 * Created by PetruPetrescu on 25/02/2022.
 */
 var StyleManagerController = (function () {

    function StyleManagerController($scope, oClose, oExtras, oStyleService, oConstantsService, oHttp, oModalService) {
        this.m_oScope = $scope;
        this.m_oClose = oClose;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;

        this.m_oStyleService = oStyleService;
        this.m_oModalService = oModalService;

        this.m_oFile = null;
        this.m_sFileName = "";

        this.m_aoStyleList = [];
        this.m_oSelectedStyle = null;

        this.m_oConstantsService = oConstantsService;

        this.m_oStyleFileData = {
            styleName: "",
            styleDescription: "",
            isPublic: false
        };

        this.isUploadingStyle = false;
        
        if (utilsIsObjectNullOrUndefined(this.m_oExtras.defaultTab) === true) {
            this.m_sSelectedStyleTab = 'StyleTab1';
        } else {
            this.m_sSelectedStyleTab = this.m_oExtras.defaultTab;
        }

        this.m_bIsLoadingStyles = false;
        this.m_bIsLoadingStyleList = false;
        this.m_bIsJsonEditModeActive = false;

        this.m_sJson = {};
        this.m_sMyJsonString = "";

        this.m_oHttp = oHttp;
        //$scope.close = oClose;


        $scope.close = function (result) {
            // close, but give 500ms for bootstrap to animate
            oClose(result, 500);
        };

        //Load styles
        this.getStylesByUser();

        // model variable that contains the Xml of the style
        this.m_asStyleXml;

        // support variable enabled when the xml is edited on edit xml tab 
        this.m_bXmlEdited = false;
    }

    StyleManagerController.prototype.getStylesByUser = function () {
        var oController = this;
        this.m_bIsLoadingStyles = true;
        this.m_oStyleService.getStylesByUser().then(function (data) {
            if (utilsIsObjectNullOrUndefined(data.data) == false) {
                oController.m_aoStyleList = data.data;
            } else {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN GET STYLES, DATA NOT AVAILABLE");
            }

            oController.m_bIsLoadingStyles = false;
        },function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR GETTING STYLE LIST");
            oController.m_bIsLoadingStyles = false;
        });
    };

    StyleManagerController.prototype.setDefaultImages = function(aoStyleList) {
        if(utilsIsObjectNullOrUndefined(aoStyleList) === true) {
            return aoStyleList;
        }
        var sDefaultImage = "assets/icons/ImageNotFound.svg";
        var iNumberOfStyles = aoStyleList.length;
        for (var i = 0; i < iNumberOfStyles; i++) {
            if (utilsIsObjectNullOrUndefined(aoStyleList.imgLink)) {
                aoStyleList[i].imgLink = sDefaultImage;
            }
        }
        return aoStyleList;
    };

    StyleManagerController.prototype.closeDialogWithDelay = function (result, iDelay) {

        this.m_oClose(result, 700); // close, but give 500ms for bootstrap to animate
    };

    /**
     * deleteStyle
     * @param oStyle
     * @returns {boolean}
     */
    StyleManagerController.prototype.deleteStyle = function (oStyle) {
        if (utilsIsObjectNullOrUndefined(oStyle) === true) {
            return false;
        }
        var oController = this;
        this.m_oStyleService.deleteStyle(oStyle.styleId).then(function (data) {
            if (utilsIsObjectNullOrUndefined(data.data) == false) {
                oController.getStylesByUser();
            } else {
                //TODO ERROR
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN DELETE STYLE");
            }
        },function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN DELETE STYLE");
        });

        return true;
    };

    /**
     * uploadUserStyleOnServer
     */

     StyleManagerController.prototype.uploadUserStyleOnServer = function () {

        if (utilsIsStrNullOrEmpty(this.m_oStyleFileData.styleName) === true) {
            this.m_oStyleFileData.styleName = "style";
        }
        var oBody = new FormData();
        oBody.append('file', this.m_oFile[0]);

        this.uploadStyle(this.m_sWorkspaceId, this.m_oStyleFileData.styleName, this.m_oStyleFileData.styleDescription,
            this.m_oStyleFileData.isPublic, oBody);
    };

    /**
     * uploadStyle
     * @param sWorkspaceId
     * @param sName
     * @param sDescription
     * @param oBody
     * @returns {boolean}
     */
     StyleManagerController.prototype.uploadStyle = function (sWorkspaceId, sName, sDescription, bIsPublic, oBody) {
        if (utilsIsObjectNullOrUndefined(sWorkspaceId) === true) {
            return false;
        }
        if (utilsIsObjectNullOrUndefined(sName) === true || utilsIsStrNullOrEmpty(sName) === true) {
            return false;
        }
        if (utilsIsObjectNullOrUndefined(sDescription) === true)//|| utilsIsStrNullOrEmpty(sDescription) === true
        {
            return false;
        }
        if (utilsIsObjectNullOrUndefined(oBody) === true) {
            return false;
        }
        this.isUploadingStyle = true;
        var oController = this;
        this.m_oStyleService.uploadByFile(this.m_sWorkspaceId, sName, sDescription, oBody, bIsPublic).then(function (data) {
            if (utilsIsObjectNullOrUndefined(data.data) == false) {
                //Reload list o workFlows
                oController.getStylesByUser();
                oController.cleanAllUploadStyleFields();
                var oDialog = utilsVexDialogAlertBottomRightCorner("SUCCESSFUL UPLOAD");
                utilsVexCloseDialogAfter(4000, oDialog);
            } else {
                //TODO ERROR
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN UPLOAD WORKFLOW PROCESS");
            }

            oController.isUploadingStyle = false;
        },function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN UPLOAD WORKFLOW PROCESS");
            oController.cleanAllUploadStyleFields();
            oController.isUploadingStyle = false;
        });

        return true;
    };

    StyleManagerController.prototype.selectStyle = function(style) {        
        this.m_oSelectedStyle = style;

        this.m_asStyleXml = "";

        if (style) {
            if (!utilsIsStrNullOrEmpty(style.styleId)) {
                this.getStyleXml(style.styleId);
            }
        }
    }

    StyleManagerController.prototype.getStyleXml = function (sStyleId) {
        var oController = this;
        this.m_oStyleService.getStyleXml(sStyleId).then(function (data) {
            oController.m_asStyleXml = data.data;
        });
    }
    
    /**
     *
     * @returns {boolean}
     */
     StyleManagerController.prototype.isSelectedStyle = function () {
        return !utilsIsObjectNullOrUndefined(this.m_oSelectedStyle);
    };

    /**
     *
     * @returns {boolean}
     */
    StyleManagerController.prototype.isUploadedNewStyle = function () {
        return !utilsIsObjectNullOrUndefined(this.m_oFile);
    };

    /**
     *
     */
    StyleManagerController.prototype.cleanAllUploadStyleFields = function () {
        this.m_oStyleFileData = {
            styleName: "",
            styleDescription: "",
            isPublic: false
        };
        this.m_oFile = null;
    };

    /**
     *
     */
    StyleManagerController.prototype.cleanAllExecuteStyleFields = function () {
        this.m_asSelectedProducts = [];
        this.m_oSelectedStyle = null;
    };

    StyleManagerController.prototype.isPossibleDoUpload = function () {
        var bReturnValue = false;
        if ((utilsIsStrNullOrEmpty(this.m_oStyleFileData.styleName) === false) && (utilsIsStrNullOrEmpty(this.m_oStyleFileData.styleDescription) === false)
            && (utilsIsObjectNullOrUndefined(this.m_oFile[0]) === false)) {
            bReturnValue = true;
        }
        return bReturnValue;
    };

    StyleManagerController.prototype.openEditStyleDialog = function (oStyle) {
        var oController = this;

        oController.m_oModalService.showModal({
            templateUrl: "dialogs/style_edit/StyleView.html",
            controller: "StyleController",
            inputs: {
                extras: {
                    style:oStyle
                }
            }
        }).then(function (modal) {
            modal.element.modal();
            modal.close.then(function (oResult, iDelay) {
                oController.m_sSelectedStyleTab="StyleTab1";

                //Load styles
                oController.getStylesByUser();
            });
        });
    }


    StyleManagerController.prototype.openDeleteStyleDialog = function (oStyle) {
        if (utilsIsObjectNullOrUndefined(oStyle) === true) {
            return false;
        }
        var oController = this;
        var oReturnFunctionValue = function (oValue) {
            if (oValue === true) {
                oController.deleteStyle(oStyle);
                oController.getStylesByUser();
            }

        }
        utilsVexDialogConfirm("Do you want to delete style: " + oStyle.name + " ?", oReturnFunctionValue);
        return true;
    };

    StyleManagerController.prototype.downloadStyle = function (oStyle) {
        if (utilsIsObjectNullOrUndefined(oStyle) === true) {
            return false;
        }

        this.m_oStyleService.downloadStyle(oStyle.styleId);
        return true;
    };

    StyleManagerController.prototype.isTheOwnerOfStyle = function (oStyle) {
        var oUser = this.m_oConstantsService.getUser();
        if ((utilsIsObjectNullOrUndefined(oStyle) === true) || (utilsIsObjectNullOrUndefined(oUser) === true)) {
            return false;
        }
        var sUserIdOwner = oStyle.userId;

        if (sUserIdOwner === oUser.userId) {
            return true;
        }
        return false;
    }

    StyleManagerController.prototype.getStyleNameAsTitle = function() {
        if (this._selectedStyle) {
            return this._selectedStyle.styleName;
        }
        return "";
    }
    
    StyleManagerController.$inject = [
        '$scope',
        'close',
        'extras',
        'StyleService',
        'ConstantsService',
        '$http',
        'ModalService',

    ];
    return StyleManagerController;

})();
window.StyleManagerController = StyleManagerController;
