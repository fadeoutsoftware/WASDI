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
        this.m_asWorkflowXml;

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

    StyleManagerController.prototype.selectStyle_ = function(style) {
        this._selectedStyle = style;
        this.m_sJson = {};

        if (!utilsIsStrNullOrEmpty(style.paramsSample)) {
            this.m_sMyJsonString = decodeURIComponent(style.paramsSample);

            try {
                var oParsed = JSON.parse(this.m_sMyJsonString);
                sPrettyPrint = JSON.stringify(oParsed, null, 2);
                this.m_sMyJsonString = sPrettyPrint;
            }
            catch (oError) {

            }
        }
        else {
            this.m_sMyJsonString = "";
        }
    }

    StyleManagerController.prototype.selectStyle = function(style) {
        console.log("selectStyle | style: ", style);

        //this._selectedStyle = style;
        //this.m_sJson = {};

        this.m_asStyleXml = "";

        if (style) {
            console.log("selectStyle | 1");
            console.log("selectStyle calling selectStyle | style.styleId: ", style.styleId);
            console.log("selectStyle calling selectStyle | utilsIsStrNullOrEmpty(style.styleId): ", utilsIsStrNullOrEmpty(style.styleId));
            if (!utilsIsStrNullOrEmpty(style.styleId)) {
                console.log("selectStyle | 2");
                console.log("selectStyle calling selectStyle | style.styleId: ", style.styleId);
                this.getStyleXml(style.styleId);
            }
        }

        /*
        if (!utilsIsStrNullOrEmpty(style.paramsSample)) {
            //this.m_sMyJsonString = decodeURIComponent(style.paramsSample);

            try {
                var oParsed = JSON.parse(this.m_sMyJsonString);
                sPrettyPrint = JSON.stringify(oParsed, null, 2);
                this.m_sMyJsonString = sPrettyPrint;
            }
            catch (oError) {

            }
        }
        else {
            this.m_sMyJsonString = "";
        }
        */
    }

    StyleManagerController.prototype.getStyleXml = function (sStyleId) {
        console.log("getStyleXml | sStyleId: ", sStyleId);
        var oController = this;
        this.m_oStyleService.getStyleXml(sStyleId).then(function (data) {
            oController.m_asStyleXml = data.data;
            console.log("getStyleXml |  oController.m_asStyleXml: ",  oController.m_asStyleXml);
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
        // this.m_oStyleFileData.styleName,this.m_oStyleFileData.styleDescription    this.m_oFile[0]
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
                //oController.getStylesByUser();
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
