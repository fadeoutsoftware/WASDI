var StyleController = (function () {

    function StyleController($scope, oExtras, oConstantsService, oStyleService, oClose) {

        /**
         * Angular Scope
         */
         this.m_oScope = $scope;

         /**
          * Class callback
          */
         this.m_oClose = oClose;
         /**
          * Reference to the controller
          */
         this.m_oScope.m_oController = this;
         /**
          * Constant Service
          */
         this.m_oConstantService = oConstantsService;
         /**
          * Snap Operations Service
          */
         this.m_oStyleService = oStyleService;
         /**
          * First tab visualized
          * @type {string}
          */
         this.m_sSelectedTab = "Base";
         /**
          * Extras injected from modal invoker
          */
         this.m_oExtras = oExtras;
         /**
          * Object with the infos about the current style
          */
         this.m_oStyle = this.m_oExtras.style;
         /**
          * User Mail for share
          */
         this.m_sUserEmail = "";
         /**
          * Field with list of active sharing
          */
         this.m_aoEnabledUsers = [];
 
         /**
          * Field with the current uploaded file
          */
         this.m_oFile = undefined;
 
 
         /**
          * boolean to discriminate mode default edit
          */
         this.m_bEditMode = true;
 
         /**
          * Default dialog title
          */
         this.m_sDialogTitle = "Edit Style"; // swap with translation ?
 
         // Let's init the modal
         this.initModal();
 
         // model variable that contains the Xml of the style
         this.m_asStyleXml;
         // support variable enabled when the xml is edited on edit xml tab 
         this.m_bXmlEdited = false;
    }

    /**
     * Methods to be implemented On BE: 
     * 1- getXmlFromFile on server
     * 2a - Convert to file and invoke update file 
     * -- OR -- 
     * 2b - Update Xml as text <--- nuova chiamata PUT or POST del testo
     * 
     * Potential problem : both upload and edit -> check the current tab active
     * (maybe notify user?)
     * 
     */

    /**
     * Init the current view accordingly to mode.
     * Mode can be "new" if no style is passed
     * or "edit" if a style is passed via extras to the modal    
     */
    StyleController.prototype.initModal = function () {
        var oController = this;
        if (utilsIsObjectNullOrUndefined(this.m_oStyle)) {
            this.m_bEditMode = false;
            this.m_sDialogTitle = "New Style"
            // init new model for style
            this.m_oStyle = {
                name: "",
                description: "",
                public: false
            }
        }
        else {
            //Init the list of users which this style is shared with
            this.getListOfEnabledUsers(this.m_oStyle.styleId);
            this.getStyleXml(this.m_oStyle.styleId);
        }
    }

    StyleController.prototype.getStyleXml = function (sStyleId) {
        var oController = this;
        this.m_oStyleService.getStyleXml(sStyleId).then(function (data) {
            oController.m_asStyleXml = data.data;
        });
    }

    StyleController.prototype.updateStyleXml = function () {
        var oController = this;
        if (!utilsIsStrNullOrEmpty(oController.m_asStyleXml)) {
            let oBody = new FormData();
            oBody.append('styleXml', oController.m_asStyleXml);
            this.m_oStyleService.postStyleXml(oController.m_oStyle.styleId, oBody)
                .then(function (data) {
                    let dialog;
                    if (data.status == 200) dialog = utilsVexDialogAlertBottomRightCorner("STYLE XML UPDATED");
                    utilsVexCloseDialogAfter(4000, dialog);
                })
                .catch(function (data) {
                    let dialog;
                    if (data.status == 304) dialog = utilsVexDialogAlertBottomRightCorner("MODIFICATIONS REJECTED<br>PLEASE CHECK THE XML");
                    if (data.status == 401) dialog = utilsVexDialogAlertBottomRightCorner("MODIFICATIONS REJECTED<br>UNAUTHORIZED");
                    else dialog = utilsVexDialogAlertBottomRightCorner("INTERNAL SERVER ERROR<br>PLEASE TRY AGAIN LATER");
                    utilsVexCloseDialogAfter(4000, dialog);
                });
        }
    }

    /**
     * Util to check if the file is uploaded
     * @returns true if the file is uploaded, false instead
     */
    StyleController.prototype.isFileLoaded = function () {
        return !(this.m_oFile === undefined);
    }

    /**
    * Method invoke on apply:
    * if the currrent tab is "Base" update the style
    * if the current tab is "Share" just close, because all the work on sharings
    * has an immediate feedback so no operation must be done on closing
    * @param {*} oUserId 
    * @returns 
    */
    StyleController.prototype.apply = function () {
        if (this.m_sSelectedTab == "Base") {
            if (this.m_bEditMode) {
                // UPDATE 
                this.updateStyle();
            }
            else {
                // UPLOAD
                this.uploadUserStyleOnServer();
            }
        }

        if (this.m_sSelectedTab == "Xml") {
            if (this.m_bXmlEdited) {
                // UPDATE 
                this.updateStyleXml();
            }
        }
        //cose the dialog
        this.m_oClose(null, 500);
    }

    /**
     * Share the style with a user
     */
    StyleController.prototype.shareStyleByUserEmail = function (oUserId) {
        var oController = this;
        this.m_oStyleService.addStyleSharing(this.m_oStyle.styleId, oUserId)
            .then(function (data) {
                if (utilsIsObjectNullOrUndefined(data.data) === false && data.data.boolValue === true) {
                    // all done
                    oController.getListOfEnabledUsers(oController.m_oStyle.styleId);
                }
                else {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR SHARING STYLE<br>" + data.data.stringValue);
                }
                // reload the sharing list

            }, function (error) {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR SHARING STYLE");
            });
    };

    /**
     * Invokes the API for style deletion. It handles the request by deleting the
     * style if invoked by the Owner, and delete the sharing if invoked by another user
     * @param {*} oUserId the user ID invoking the API
     */
    StyleController.prototype.deleteStyle = function (oUserId) {
        this.m_oStyleService.deleteStyle(this.m_oStyle.styleId, oUserId);
    }

    /**
     * Invokes the deletion of the sharing between the current style and the
     * user identified by UserId
     * @param {*} oUserId the identifier of the User
     */
    StyleController.prototype.removeUserSharing = function (oUserId) {
        var oController = this;
        this.m_oStyleService.removeStyleSharing(this.m_oStyle.styleId, oUserId).then(function (data) {
            if (utilsIsObjectNullOrUndefined(data.data) === false && data.data.boolValue === true) {
                // all done
                oController.getListOfEnabledUsers(oController.m_oStyle.styleId);
            }
            else {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR DELETING SHARING STYLE");
            }
            // reload the sharing list

        }, function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR DELETING SHARING STYLE");
        });
    };

    /**
     * uploadUserStyleOnServer
     */

    StyleController.prototype.uploadUserStyleOnServer = function () {

        var oBody = new FormData();
        oBody.append('file', this.m_oFile[0]);
        //this.m_oConstantService.getActiveWorkspace().sWorkspaceId
        this.uploadStyle(//"idworkspace", // Current Workspace from constant service <-> unused on API
            this.m_oStyle.name, this.m_oStyle.description, this.m_oStyle.public, // name, description and boolean for isPublic
            oBody); // content of the file

    };



    StyleController.isUploadedStyle = function () {
        return !utilsIsObjectNullOrUndefined(this.m_oFile);
    }

    /**
     * uploadStyle
     * @param sWorkspaceId
     * @param sName
     * @param sDescription
     * @param oBody
     * @returns {boolean}
     */
    StyleController.prototype.uploadStyle = function (sName, sDescription, bIsPublic, oBody) {

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
        this.m_oStyleService.uploadFile(sName, sDescription, oBody, bIsPublic).then(function (data) {
            if (utilsIsObjectNullOrUndefined(data.data) === false && data.data.boolValue === true) {
                //Reload list o styles
                var oDialog = utilsVexDialogAlertBottomRightCorner("STYLE UPLOADED<br>" + sName.toUpperCase());
                utilsVexCloseDialogAfter(4000, oDialog);
            } else {
                utilsVexDialogAlertTop("GURU MEDITATION<br>" + data.data.stringValue);
            }

            oController.isUploadingStyle = false;
        }, function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>INVALID STYLE FILE");
            oController.cleanAllUploadStyleFields();
            oController.isUploadingStyle = false;
        });

        return true;
    };


    /**
     * Updates files and parameters of the style
     * @param {*} sStyleId
     * @param {*} sName
     * @param {*} sDescription
     * @param {*} bIsPublic
     * @param {*} oBody
     * @returns
     */
    StyleController.prototype.updateStyle = function () {

        this.isUploadingStyle = true;
        var oController = this;
        // update name, description, public
        oController.m_oStyleService.updateStyleParameters(this.m_oStyle.styleId,
            oController.m_oStyle.description,
            oController.m_oStyle.public).then(function () {
                // update file only if File is uploaded
                if (oController.m_oFile != undefined) {
                    var oBody = new FormData();
                    oBody.append('file', oController.m_oFile[0]);
                    oController.m_oStyleService.updateStyleFile(oController.m_oStyle.styleId, oBody).then(function (data) {
                        if (utilsIsObjectNullOrUndefined(data.data) == false) {
                            oController.getStyleXml(oController.m_oStyle.styleId);

                            var oDialog = utilsVexDialogAlertBottomRightCorner("UPDATED STYLE<br>READY");
                            utilsVexCloseDialogAfter(4000, oDialog);
                        } else {
                            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR UPDATING STYLE");
                        }

                        oController.isUploadingStyle = false;
                    }, function (error) {
                        utilsVexDialogAlertTop("GURU MEDITATION<br>INVALID SNAP STYLE FILE");
                        oController.cleanAllUploadStyleFields();
                        oController.isUploadingStyle = false;
                    });
                }
                else {
                    var oDialog = utilsVexDialogAlertBottomRightCorner("STYLE UPDATED<br>READY");
                    utilsVexCloseDialogAfter(4000, oDialog);
                }
            }, function (error) {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR UPDATING STYLE");
            });

        return true;
    };


    StyleController.prototype.getListOfEnabledUsers = function (sStyleId) {

        if (utilsIsStrNullOrEmpty(sStyleId) === true) {
            return false;
        }
        var oController = this;
        this.m_oStyleService.getUsersBySharedStyle(sStyleId)
            .then(function (data) {
                if (utilsIsObjectNullOrUndefined(data.data) === false) {
                    oController.m_aoEnabledUsers = data.data;
                }
                else {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR GETTING STYLE SHARINGS");
                }

            }, function (error) {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR GETTING STYLE SHARINGS");
            });
        return true;
    };

    StyleController.prototype.getStyleSharings = function (sStyleId) {
        this.m_aoEnabledUsers = this.m_oStyleService.getStyleSharing(sStyleId);
    }

    StyleController.prototype.iAmTheOwner = function () {
        return (this.m_oConstantService.getUser().userId === this.m_oStyle.userId);
    }

    StyleController.$inject = [
        '$scope',
        'extras',
        'ConstantsService',
        'StyleService',
        'close'
    ]

    return StyleController;
})();
window.StyleController = StyleController;
