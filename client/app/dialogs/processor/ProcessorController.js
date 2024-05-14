/**
 * Created by a.corrado on 31/03/2017.
 */

var ProcessorController = (function () {
    function ProcessorController(
        $scope,
        oClose,
        oExtras,
        oWorkspaceService,
        oProductService,
        oConstantsService,
        oHttp,
        oRootScope,
        oProcessorService,
        oProcessorMediaService,
        oModalService,
        oImagesService
    ) {
        /**
         * Angular Scope
         */
        this.m_oScope = $scope;
        /**
         * Reference to the controller
         */
        this.m_oScope.m_oController = this;
        /**
         * Extra params received in input
         */
        this.m_oExtras = oExtras;
        /**
         * Input processor base data
         * @type {null}
         */
        this.m_oInputProcessor = this.m_oExtras.processor;
        /**
         * Workpsace service
         */
        this.m_oWorkspaceService = oWorkspaceService;
        /**
         * Product service
         */
        this.m_oProductService = oProductService;
        /**
         * Product Media Service
         */
        this.m_oProcessorMediaService = oProcessorMediaService;
        /**
         * Modal service
         */
        this.m_oModalService = oModalService;
        /**
         * Constants service
         */
        this.m_oConstantsService = oConstantsService;
        /**
         * Active Workspace
         */
        this.m_oActiveWorkspace = this.m_oConstantsService.getActiveWorkspace();
        /**
         * Http Service
         */
        this.m_oHttp = oHttp;
        /**
         * Root Scope
         */
        this.m_oRootScope = oRootScope;
        /**
         * Processors Service
         */
        this.m_oProcessorService = oProcessorService;
        /**
         * Images Service
         */
        this.m_oImagesService = oImagesService;
        /**
         * User Uploaded Zip file
         * @type {null}
         */
        this.m_oFile = null;
        /**
         * Processor Name
         * @type {string}
         */
        this.m_sName = "";
        /**
         * Processor Description
         * @type {string}
         */
        this.m_sDescription = "";
        /**
         * Processor Version
         */
        this.m_sVersion = "1";
        /**
         * JSON Input Paramters Sample
         * @type {string}
         */
        this.m_sJSONSample = "";
        /**
         * Types of available processors
         * @type {({name: string, id: string}|{name: string, id: string}|{name: string, id: string})[]}
         */
        this.m_aoProcessorTypes = [
            { name: "Python 3.10/Ubuntu 22.04 Pip One Shot", id:"pip_oneshot"},
            { name: "Ubuntu 22.04 + Python 3.10", id:"python_pip_2"},
            { name: "OGC Application Package", id: "eoepca" },
            { name: "Ubuntu 20.04 + Python 3.8", id:"python_pip_2_ubuntu_20"},
            { name: "IDL 3.7.2", id: "ubuntu_idl372" },
            { name: "OCTAVE 6.x", id: "octave" },
            { name: "Python 3.x Conda", id: "conda" },
            { name: "C# .NET Core", id: "csharp" },
            {name: "Ubuntu 20.04 + Python 3.8", id:"ubuntu_python37_snap"}
        ];
        /**
         * Selected Processor Type
         * @type {string}
         */
        this.m_sSelectedType = "";

        /**
         * Name of the Type  of a processor in edit mode
         * @type {string}
         */
        this.m_sTypeNameOnly = "";
        /**
         * Id of the Type  of a processor in edit mode
         * @type {string}
         */
        this.m_sTypeIdOnly = "";
        /**
         * Public flag
         * @type {boolean}
         */
        this.m_oPublic = true;
        /**
         * Time Out in Minutes
         * @type {number}
         */
        this.m_iMinuteTimeout = 180;

        /**
         * Environment Update Command
         * @type {string}
         */
        this.m_sEnvUpdCommand = "";

        /**
         * Flag to know if we are in Edit Mode
         * @type {boolean}
         */
        this.m_bEditMode = false;

        /**
         * Share user mail
         * @type {string}
         */
        this.m_sUserEmail = "";
        /**
         * User access rights
         */
        this.m_sRights = "read";
        /**
         * List of user id that has access to the processor
         * @type {*[]}
         */
        this.m_aoEnableUsers = [];
        /**
         * Processor Id
         * @type {string}
         */
        this.m_sProcessorId = "";

        /**
         * Selected Tab
         * @type {string}
         */
        this.m_sSelectedTab = "Base";

        /**
         * View Model with the Processor Detailed Info.
         * Is fetched and saved with different APIs
         * @type {{processorDescription: string, updateDate: number, images: [], imgLink: string, ondemandPrice: number, link: string, score: number, processorId: string, publisher: string, buyed: boolean, processorName: string, categories: [], isMine: boolean, friendlyName: string, email: string, subscriptionPrice: number}}
         */
        this.m_oProcessorDetails = {
            processorId: "",
            processorName: "",
            processorDescription: "",
            imgLink: "",
            publisher: "",
            score: 0.0,
            friendlyName: "",
            link: "",
            email: "",
            ondemandPrice: 0.0,
            subscriptionPrice: 0.0,
            updateDate: 0,
            categories: [],
            images: [],
            isMine: true,
            buyed: false,
            longDescription: "",
            showInStore: false,
            maxImages: 6,
            reviewsCount: 0,
            purchased: 0, // NOTE: at the moment here is the count of run on the main server
            totalRuns: 0, // NOTE: not set at the moment
            userRuns: 0, // NOTE: not set at the moment
        };

        this.m_oImageToUpload = [];

        /**
         * Processor UI
         * @type {string}
         */
        this.m_sProcessorUI = "{}";

        /**
         * Flag to know if the UI is changed or not
         * @type {boolean}
         */
        this.m_bUIChanged = false;

        /**
         * Application Categories
         * @type {*[]}
         */
        this.m_aoCategories = [];

        /**
         * Processor Logo File
         * @type {null}
         */
        this.m_oProcessorLogo = null;

        /**
         * Local Reference to the controller
         * @type {ProcessorController}
         */
        let oController = this;

        // Close this Dialog handler
        $scope.close = function () {
            // close, but give 500ms for bootstrap to animate
            oClose(null, 300);
        };

        // Apply the user actions handler
        $scope.add = function () {
            if (oController.m_bEditMode == true) {
                //new condition -> The user didn't put anything on the params
                if (oController.m_sJSONSample == "") {
                    oController.m_sJSONSample = "{}"; // suppose it was an empty JSON
                }
                if (!oController.tryParseJSON(oController.m_sJSONSample)) {
                    let oDialog = utilsVexDialogAlertBottomRightCorner(
                        "PLEASE CHECK YOUR JSON<br>IN THE PARAMS SAMPLE"
                    );
                    utilsVexCloseDialogAfter(4000, oDialog);
                    return;
                }

                if (oController.m_bUIChanged) {
                    if (!oController.tryParseJSON(oController.m_sProcessorUI)) {
                        let oDialog = utilsVexDialogAlertBottomRightCorner(
                            "PLEASE CHECK YOUR UI<br>JSON NOT VALID"
                        );
                        utilsVexCloseDialogAfter(4000, oDialog);

                        return;
                    }
                }

                var oFile = null;

                if (oController.m_oFile != null) {
                    oFile = oController.m_oFile[0];
                }
                oController.updateProcessor(oController, oFile);
            } else {
                if (oController.m_oFile != null) {
                    oController.postProcessor(
                        oController,
                        oController.m_oFile[0]
                    );
                } else {
                    var oDialog = utilsVexDialogAlertTop(
                        "GURU MEDITATION<br>NO FILE SELECTED, PLEASE UPLOAD THE ARCHIVE CONTAINING PROCESSOR'S FILES"
                    );
                    utilsVexCloseDialogAfter(10000, oDialog);
                }
            }
            // close only if successfull?
            //oClose(oController.m_oProcessorDetails, 300); // close, but give 500ms for bootstrap to animate
        };

        // Are we creating a new processor or editing an existing one?
        if (this.m_oInputProcessor !== null) {
            // We are in edit mode:
            this.m_bEditMode = true;

            // Copy the input data to the model
            this.m_sName = this.m_oInputProcessor.processorName;
            this.m_sDescription = this.m_oInputProcessor.processorDescription;
            this.m_sJSONSample = decodeURIComponent(
                this.m_oInputProcessor.paramsSample
            );
            this.m_sProcessorId = this.m_oInputProcessor.processorId;
            this.m_iMinuteTimeout = this.m_oInputProcessor.minuteTimeout;

            try {
                var oParsed = JSON.parse(this.m_sJSONSample);
                sPrettyPrint = JSON.stringify(oParsed, null, 2);
                this.m_sJSONSample = sPrettyPrint;
            } catch (oError) { }

            // Get the list of Enabled users for sharing
            this.getListOfEnableUsers(this.m_sProcessorId);

            // Select the right processor type
            var i = 0;

            for (; i < this.m_aoProcessorTypes.length; i++) {
                if (
                    this.m_aoProcessorTypes[i].id ===
                    this.m_oInputProcessor.type
                ) {
                    this.m_sTypeNameOnly = this.m_aoProcessorTypes[i].name;
                    this.m_sTypeIdOnly = this.m_aoProcessorTypes[i].id;
                    break;
                }
            }

            if (this.m_oInputProcessor.isPublic) {
                this.m_oPublic = true;
            } else {
                this.m_oPublic = false;
            }

            let sProcessorName = encodeURIComponent(
                this.m_oInputProcessor.processorName
            );

            // Read also the details
            this.m_oProcessorService.getMarketplaceDetail(sProcessorName).then(
                function (data) {
                    if (utilsIsObjectNullOrUndefined(data.data) === false) {
                        oController.m_oProcessorDetails = data.data;

                        oController.m_oImagesService.updateProcessorLogoImageUrl(oController.m_oProcessorDetails);

                        oController.m_oProcessorMediaService
                            .getCategories()
                            .then(
                                function (oData) {
                                    oController.m_aoCategories = oData.data;
                                },
                                function (error) { }
                            );
                    } else {
                        utilsVexDialogAlertTop(
                            "GURU MEDITATION<br>ERROR READING APP DETAILS"
                        );
                    }
                },
                function (error) {
                    utilsVexDialogAlertTop(
                        "GURU MEDITATION<br>ERROR READING APP DETAILS"
                    );
                }
            );

            this.m_oProcessorService.getProcessorUI(sProcessorName).then(
                function (data) {
                    if (utilsIsObjectNullOrUndefined(data.data) === false) {
                        oController.m_sProcessorUI = JSON.stringify(
                            data.data,
                            undefined,
                            4
                        );
                    } else {
                        oController.m_sProcessorUI = "{}";
                    }
                },
                function (error) {
                    utilsVexDialogAlertTop(
                        "GURU MEDITATION<br>ERROR READING APP UI"
                    );
                }
            );
        }
    }

    /**
     * Utility method to define if the drag and drop box can be shown or not
     * @returns {boolean}
     */
    ProcessorController.prototype.showDragAndDrop = function () {
        if (this.m_bEditMode == false) {
            return true;
        } else {
            return true;
        }
    };

    /**
     * Utility method to test JSON Validity
     * @param sJsonString
     * @returns {boolean|any}
     */
    ProcessorController.prototype.tryParseJSON = function (sJsonString) {
        try {
            var oJsonParsedObject = JSON.parse(sJsonString);

            if (oJsonParsedObject && typeof oJsonParsedObject === "object") {
                return oJsonParsedObject;
            }
        } catch (e) { }

        return false;
    };

    /**
     * Internal method to update en existing processor in edit mode
     * @param oController
     * @param oSelectedFile
     * @returns {boolean}
     */
    ProcessorController.prototype.updateProcessor = function (
        oController,
        oSelectedFile
    ) {
        // Update User Values
        oController.m_oInputProcessor.isPublic = 1;
        if (oController.m_oPublic === false)
            oController.m_oInputProcessor.isPublic = 0;
        oController.m_oInputProcessor.processorName = oController.m_sName;
        // Version is fixed at 1 now and hidden from the form
        //oController.m_oInputProcessor.processorVersion = oController.m_sVersion;
        oController.m_oInputProcessor.processorDescription =
            oController.m_sDescription;
        oController.m_oInputProcessor.paramsSample = encodeURI(
            oController.m_sJSONSample
        );
        oController.m_oInputProcessor.minuteTimeout =
            oController.m_iMinuteTimeout;
        // Copy the brief description also in the detail view
        oController.m_oProcessorDetails.processorDescription =
            oController.m_sDescription;

        // Update processor data
        oController.m_oProcessorService
            .updateProcessor(
                oController.m_oInputProcessor.processorId,
                oController.m_oInputProcessor
            )
            .then(
                function () {
                    oController.m_oProcessorService
                        .updateProcessorDetails(
                            oController.m_oInputProcessor.processorId,
                            oController.m_oProcessorDetails
                        )
                        .then(
                            function () {
                                var oDialog =
                                    utilsVexDialogAlertBottomRightCorner(
                                        "PROCESSOR DATA UPDATED<br>REBUILD ONGOING"
                                    );
                                utilsVexCloseDialogAfter(4000, oDialog);
                            },
                            function (error) {
                                utilsVexDialogAlertTop(
                                    "GURU MEDITATION<br>THERE WAS AN ERROR UPDATING PROCESSOR DATA"
                                );
                            }
                        );
                },
                function (error) {
                    utilsVexDialogAlertTop(
                        "GURU MEDITATION<br>THERE WAS AN ERROR UPDATING PROCESSOR DATA"
                    );
                }
            );

        if (oController.m_bUIChanged) {
            oController.m_oProcessorService
                .saveProcessorUI(
                    oController.m_oInputProcessor.processorName,
                    oController.m_sProcessorUI
                )
                .then(
                    function (data) {
                        var oDialog = utilsVexDialogAlertBottomRightCorner(
                            "PROCESSOR UI UPDATED"
                        );
                        utilsVexCloseDialogAfter(4000, oDialog);
                    },
                    function (error) {
                        utilsVexDialogAlertTop(
                            "GURU MEDITATION<br>THERE WAS AN ERROR UPDATING PROCESSOR UI"
                        );
                    }
                );
        }

        // There was also a file?
        if (!utilsIsObjectNullOrUndefined(oSelectedFile) === true) {
            // Update the file
            var oBody = new FormData();
            oBody.append("file", this.m_oFile[0]);

            var sFileName = oSelectedFile.name;

            this.m_oProcessorService
                .updateProcessorFiles(
                    sFileName,
                    oController.m_oInputProcessor.processorId,
                    oBody
                )
                .then(
                    function (data) {
                        var oDialog = utilsVexDialogAlertBottomRightCorner(
                            "PROCESSOR FILES UPDATED"
                        );
                        utilsVexCloseDialogAfter(4000, oDialog);
                    },
                    function (error) {
                        utilsVexDialogAlertTop(
                            "GURU MEDITATION<br>THERE WAS AN ERROR UPDATING PROCESSOR FILES"
                        );
                    }
                );
        }

        return true;
    };

    /**
     * Utility method to Create a new processor
     * @param oController
     * @param oSelectedFile
     * @returns {boolean}
     */
    ProcessorController.prototype.postProcessor = function (
        oController,
        oSelectedFile
    ) {
        if (utilsIsObjectNullOrUndefined(oSelectedFile) === true) {
            return false;
        }

        var sType = oController.m_sSelectedType.id;

        var sPublic = "1";
        if (oController.m_oPublic === false) sPublic = "0";

        var oBody = new FormData();
        oBody.append("file", this.m_oFile[0]);

        if (
            sType === "ubuntu_python_snap" ||
            sType === "ubuntu_python37_snap"
        ) {
            oController.m_sName = oController.m_sName.toLowerCase();
        }

        let sName = encodeURIComponent(oController.m_sName);
        let sDescription = encodeURIComponent(oController.m_sDescription);

        this.m_oProcessorService
            .uploadProcessor(
                oController.m_oActiveWorkspace.workspaceId,
                sName,
                oController.m_sVersion,
                sDescription,
                sType,
                oController.m_sJSONSample,
                sPublic,
                oBody
            )
            .then(
                function (data) {
                    var sMessage = "";
                    if (data.data.boolValue == true) {
                        sMessage =
                            "PROCESSOR UPLOADED<br>IT WILL BE DEPLOYED IN A WHILE";
                    } else {
                        sMessage = "ERROR UPLOADING PROCESSOR";
                        if (utilsIsStrNullOrEmpty(data.data.stringValue)) {
                            sMessage = "<br>ERROR CODE: " + data.data.intValue;
                        } else {
                            sMessage += "<br>" + data.data.stringValue;
                        }
                    }

                    var oDialog =
                        utilsVexDialogAlertBottomRightCorner(sMessage);
                    utilsVexCloseDialogAfter(5000, oDialog);
                },
                function (error) {
                    utilsVexDialogAlertTop(
                        "GURU MEDITATION<br>THERE WAS AN ERROR DEPLOYING THE PROCESSOR"
                    );
                }
            );

        return true;
    };

    /**
     * Get the list of users that has this processor shared
     * @param sProcessorId
     * @returns {boolean}
     */
    ProcessorController.prototype.getListOfEnableUsers = function (
        sProcessorId
    ) {
        if (utilsIsStrNullOrEmpty(sProcessorId) === true) {
            return false;
        }
        var oController = this;
        this.m_oProcessorService.getUsersBySharedProcessor(sProcessorId).then(
            function (data) {
                if (utilsIsObjectNullOrUndefined(data.data) === false) {
                    oController.m_aoEnableUsers = data.data;
                } else {
                    utilsVexDialogAlertTop(
                        "GURU MEDITATION<br>ERROR SHARING PROCESSOR"
                    );
                }
            },
            function (error) {
                utilsVexDialogAlertTop(
                    "GURU MEDITATION<br>ERROR SHARING PROCESSOR"
                );
            }
        );
        return true;
    };

    /**
     * Add a user to the sharing list
     * @param sProcessorId
     * @param sEmail
     * @returns {boolean}
     */
    ProcessorController.prototype.shareProcessorByUserEmail = function (
        sProcessorId,
        sEmail,
        sRights
    ) {
        if (
            utilsIsObjectNullOrUndefined(sProcessorId) === true ||
            utilsIsStrNullOrEmpty(sEmail) === true
        ) {
            return false;
        }

        utilsRemoveSpaces(sEmail);

        var sFinalProcessorId = sProcessorId;

        var oController = this;
        this.m_oProcessorService.putShareProcessor(sProcessorId, sEmail, sRights).then(
            function (data) {
                if (
                    utilsIsObjectNullOrUndefined(data.data) === false &&
                    data.data.boolValue === true
                ) {
                    // SHARING SAVED
                } else {
                    sMessage = "GURU MEDITATION<br>ERROR SHARING PROCESSOR";

                    if (utilsIsObjectNullOrUndefined(data.data) === false) {
                        if (
                            utilsIsObjectNullOrUndefined(
                                data.data.stringValue
                            ) === false
                        ) {
                            sMessage = sMessage + ": " + data.data.stringValue;
                        }
                    }

                    utilsVexDialogAlertTop(sMessage);
                }
                oController.m_sRights = "read";
                oController.getListOfEnableUsers(sFinalProcessorId);
            },
            function (error) {
                utilsVexDialogAlertTop(
                    "GURU MEDITATION<br>ERROR SHARING PROCESSOR"
                );
            }
        );

        this.m_sUserEmail = "";
        return true;
    };

    /**
     * Removes a user from the sharing list
     * @param sProcessorId
     * @param sEmail
     * @returns {boolean}
     */
    ProcessorController.prototype.removeUserSharing = function (
        sProcessorId,
        sEmail
    ) {
        if (
            utilsIsObjectNullOrUndefined(sProcessorId) === true ||
            utilsIsStrNullOrEmpty(sEmail) === true
        ) {
            return false;
        }

        utilsRemoveSpaces(sEmail);
        var oController = this;
        var sFinalProcessorId = sProcessorId;

        this.m_oProcessorService
            .deleteUserSharedProcessor(sProcessorId, sEmail)
            .then(
                function (data) {
                    if (
                        utilsIsObjectNullOrUndefined(data.data) === false &&
                        data.data.boolValue === true
                    ) {
                        // SHARING SAVED
                    } else {
                        utilsVexDialogAlertTop(
                            "GURU MEDITATION<br>ERROR SHARING PROCESSOR"
                        );
                    }
                    oController.getListOfEnableUsers(sFinalProcessorId);
                },
                function (error) {
                    utilsVexDialogAlertTop(
                        "GURU MEDITATION<br>ERROR SHARING PROCESSOR"
                    );
                }
            );

        this.m_sUserEmail = "";
        return true;
    };

    /**
     * Force the update of the lib for the processor on the server
     * @param sProcessorId
     * @returns {boolean}
     */
    ProcessorController.prototype.forceLibUpdate = function (sProcessorId) {
        if (utilsIsObjectNullOrUndefined(sProcessorId) === true) {
            return false;
        }

        this.m_oProcessorService.forceLibUpdate(sProcessorId).then(
            function (data) {
                var oDialog = utilsVexDialogAlertBottomRightCorner(
                    "PROCESSORS IMAGE<br>LIB UPDATE SCHEDULED"
                );
                utilsVexCloseDialogAfter(5000, oDialog);
            },
            function (error) {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR UPDATING LIB");
            }
        );

        return true;
    };

    /**
     * Force the update of the env for the processor on the server
     * @param sProcessorId
     * @returns {boolean}
     */
    ProcessorController.prototype.forceEnvUpdate = function (sProcessorId) {
        if (utilsIsObjectNullOrUndefined(sProcessorId) === true) {
            return false;
        }

        if (utilsIsObjectNullOrUndefined(this.m_sEnvUpdCommand) === true) {
            return false;
        }

        this.m_oProcessorService
            .forceEnvUpdate(sProcessorId, this.m_sEnvUpdCommand)
            .then(
                function (data) {
                    var oDialog = utilsVexDialogAlertBottomRightCorner(
                        "PROCESSORS IMAGE<br>ENV UPDATE SCHEDULED"
                    );
                    utilsVexCloseDialogAfter(5000, oDialog);
                },
                function (error) {
                    utilsVexDialogAlertTop(
                        "GURU MEDITATION<br>ERROR UPDATING ENV"
                    );
                }
            );

        return true;
    };

    /**
     * Force the redeploy of the processor on the server
     * @param sProcessorId
     * @returns {boolean}
     */
    ProcessorController.prototype.forceProcessorRefresh = function (
        sProcessorId
    ) {
        if (utilsIsObjectNullOrUndefined(sProcessorId) === true) {
            return false;
        }

        // TODO: ADD CONFIRMATION DIALOG
        this.m_oProcessorService.redeployProcessor(sProcessorId).then(
            function (data) {
                var oDialog = utilsVexDialogAlertBottomRightCorner(
                    "PROCESSORS IMAGE<br>RE-DEPLOY  SCHEDULED"
                );
                utilsVexCloseDialogAfter(5000, oDialog);
            },
            function (error) {
                utilsVexDialogAlertTop(
                    "GURU MEDITATION<br>ERROR REFRESHING PROCESSOR"
                );
            }
        );

        return true;
    };

    /**
     * Handle a click on a category
     * @param sCategoryId
     */
    ProcessorController.prototype.categoryClicked = function (sCategoryId) {
        if (this.m_oProcessorDetails.categories.includes(sCategoryId)) {
            this.m_oProcessorDetails.categories =
                this.m_oProcessorDetails.categories.filter(function (e) {
                    return e !== sCategoryId;
                });
        } else {
            this.m_oProcessorDetails.categories.push(sCategoryId);
        }
    };

    /**
     * Utility method to decide if a category checkbox is checked or not
     * @param sCategoryId
     * @returns {boolean}
     */
    ProcessorController.prototype.isCategoryChecked = function (sCategoryId) {
        if (this.m_oProcessorDetails.categories.includes(sCategoryId)) {
            return true;
        }

        return false;
    };

    /**
     * Uploads or updates the application logo
     */
    ProcessorController.prototype.updateLogo = function () {
        var oSelectedFile = null;

        if (this.m_oProcessorLogo != null) {
            oSelectedFile = this.m_oProcessorLogo[0];
        }

        // The user uploaded a logo?
        if (!utilsIsObjectNullOrUndefined(oSelectedFile) === true) {
            // Update the file
            var oBody = new FormData();
            oBody.append("image", oSelectedFile);

            this.m_oImagesService
                .uploadProcessorLogo(this.m_oInputProcessor.processorId, oBody)
                .then(
                    function (data) {
                        var oDialog = utilsVexDialogAlertBottomRightCorner(
                            "PROCESSOR LOGO UPDATED"
                        );
                        utilsVexCloseDialogAfter(4000, oDialog);
                    },
                    function (error) {
                        utilsVexDialogAlertTop(
                            "GURU MEDITATION<br>THERE WAS AN ERROR UPDATING PROCESSOR LOGO"
                        );
                    }
                );
        }
    };

    /**
     * Add an image to the processor
     */
    ProcessorController.prototype.addApplicationImage = function () {
        var oSelectedFile = null;

        if (this.m_oImageToUpload != null) {
            oSelectedFile = this.m_oImageToUpload[0];
        }

        // The user uploaded a logo?
        if (!utilsIsObjectNullOrUndefined(oSelectedFile) === true) {
            var oController = this;

            // Update the file
            var oBody = new FormData();
            oBody.append("image", oSelectedFile);

            this.m_oImagesService
                .uploadProcessorImage(this.m_oInputProcessor.processorId, oBody)
                .then(
                    function (data) {
                        oController.m_oProcessorDetails.images.push(
                            encodeURIComponent(data.data.stringValue)
                        );

                        var oDialog = utilsVexDialogAlertBottomRightCorner(
                            "PROCESSOR IMAGE ADDED"
                        );
                        utilsVexCloseDialogAfter(4000, oDialog);
                    },
                    function (error) {
                        utilsVexDialogAlertTop(
                            "GURU MEDITATION<br>THERE WAS AN ERROR UPLOADING IMAGE"
                        );
                    }
                );
        }
    };

    /**
     * Remove an image of the processor
     * @param sImage
     */
    ProcessorController.prototype.removeProcessorImage = function (sImage) {
        let oController = this;

        let sImageName = this.m_oImagesService.getImageNameFromUrl(sImage);

        this.m_oImagesService
            .removeProcessorImage(
                this.m_oInputProcessor.processorName,
                sImageName
            )
            .then(
                function (data) {
                    oController.m_oProcessorDetails.images =
                        oController.m_oProcessorDetails.images.filter(function (e) {
                            return e !== sImage;
                        });

                    var oDialog = utilsVexDialogAlertBottomRightCorner(
                        "PROCESSOR IMAGE REMOVED"
                    );
                    utilsVexCloseDialogAfter(4000, oDialog);
                },
                function (error) {
                    utilsVexDialogAlertTop(
                        "GURU MEDITATION<br>THERE WAS AN ERROR DELETING THE IMAGE"
                    );
                }
            );
    };

    ProcessorController.prototype.addUIElement = function (sElementType) {
        //console.log("Adding element " + sElementType)

        let sTextToInsert = "";

        if (sElementType === "tab") {
            sTextToInsert =
                '\n\t{\n\t\t"name": "Tab Name",\n\t\t"controls": [\n\t\t]\n\t},';
        } else if (sElementType === "textbox") {
            sTextToInsert =
                '\n\t{\n\t\t"param": "PARAM_NAME",\n\t\t"type": "textbox",\n\t\t"label": "description",\n\t\t"default": "",\n\t\t"required": false,\n\t\t"tooltip":""\n\t},';
        } else if (sElementType === "numeric") {
            sTextToInsert =
                '\n\t{\n\t\t"param": "PARAM_NAME",\n\t\t"type": "numeric",\n\t\t"label": "description",\n\t\t"default": "0",\n\t\t"min": 0,\n\t\t"max": 100,\n\t\t"required": false,\n\t\t"tooltip":""\n\t},';
        } else if (sElementType === "dropdown") {
            sTextToInsert =
                '\n\t{\n\t\t"param": "PARAM_NAME",\n\t\t"type": "dropdown",\n\t\t"label": "description",\n\t\t"default": "",\n\t\t"values": [],\n\t\t"required": false,\n\t\t"tooltip":""\n\t},';
        } else if (sElementType === "bbox") {
            sTextToInsert =
                '\n\t{\n\t\t"param": "PARAM_NAME",\n\t\t"type": "bbox",\n\t\t"label": "Bounding Box",' +
                '\n\t\t"required": false,\n\t\t"tooltip":"",' +
                '\n\t\t"maxArea": 0,' +
                '\n\t\t"maxSide": 0,' +
                '\n\t\t"maxRatioSide": 0' +
                "\n\t},";
        } else if (sElementType === "slider") {
            sTextToInsert =
                '\n\t{\n\t\t"param": "PARAM_NAME",\n\t\t"type": "slider",\n\t\t"label": "description",\n\t\t"default": 0,\n\t\t"min": 0,\n\t\t"max": 100,\n\t\t"required": false,\n\t\t"tooltip":""\n\t},';
        } else if (sElementType === "date") {
            sTextToInsert =
                '\n\t{\n\t\t"param": "PARAM_NAME",\n\t\t"type": "date",\n\t\t"label": "Date",\n\t\t"required": false,\n\t\t"tooltip":""\n\t},';
        } else if (sElementType === "boolean") {
            sTextToInsert =
                '\n\t{\n\t\t"param": "PARAM_NAME",\n\t\t"type": "boolean",\n\t\t"label": "description",\n\t\t"default": false,\n\t\t"required": false,\n\t\t"tooltip":""\n\t},';
        } else if (sElementType === "productscombo") {
            sTextToInsert =
                '\n\t{\n\t\t"param": "PARAM_NAME",\n\t\t"type": "productscombo",\n\t\t"label": "Product",\n\t\t"required": false,\n\t\t"tooltip":"",\n\t\t"showExtension": false\n\t},';
        } else if (sElementType === "searcheoimage") {
            sTextToInsert =
                '\n\t{\n\t\t"param": "PARAM_NAME",\n\t\t"type": "searcheoimage",\n\t\t"label": "Description",\n\t\t"required": false,\n\t\t"tooltip":""\n\t},';
        } else if (sElementType === "hidden") {
            sTextToInsert =
                '\n\t{\n\t\t"param": "PARAM_NAME",\n\t\t"type": "hidden",\n\t\t"default": ""\n\t},';
        } else if (sElementType === "renderAsStrings") {
            sTextToInsert = '\n\t"renderAsStrings": true,\n\t';
        } else if (sElementType === "listbox") {
            sTextToInsert =
                '\n\t{\n\t\t"param": "PARAM_NAME",\n\t\t"type": "listbox",\n\t\t"label": "description",\n\t\t"values": [],\n\t\t"required": false,\n\t\t"tooltip":""\n\t},';
        } else if (sElementType === "table") {
            sTextToInsert = '\n\t{\n\t\t"param": "PARAM_NAME",\n\t\t"type": "table",\n\t\t"label": "description",\n\t\t"required": false,\n\t\t"tooltip":"",\n\t\t"rows":"",\n\t\t"columns":"",\n\t\t"row_headers":[],\n\t\t"col_headers":[]\n\t},'
        }
        this.m_bUIChanged = true;

        this.m_oRootScope.$broadcast("add", sTextToInsert);
    };
    ProcessorController.prototype.packageManagerClick = function () {
        let oController = this
        oController.m_oProcessorService.getDeployedProcessor(oController.m_sProcessorId).then(function (data) {
            oController.m_oModalService
                .showModal({
                    templateUrl: "dialogs/package_manager/PackageManagerView.html",
                    controller: "PackageManagerController",
                    inputs: {
                        extras: {
                            processor: data.data
                        }
                    }
                })
                .then(function (modal) {
                    modal.element.modal({
                        backdrop: 'static',
                        keyboard: false
                    });
                    modal.close;
                });
        })

    };
    ProcessorController.prototype.formatJson = function () {
        let oController = this;
        let sStringParsed = ""

        if (oController.m_sSelectedTab === "Base") {
            oController.m_sJSONSample = JSON.stringify(JSON.parse(oController.m_sJSONSample.replaceAll("'", '"')), null, 2);
        } else if (oController.m_sSelectedTab === "UI") {

            sStringParsed = oController.m_sProcessorUI = JSON.stringify(
                JSON.parse(oController.m_sProcessorUI.replaceAll("'", '"')),
                null,
                4
            );
        }
    }
    ProcessorController.$inject = [
        "$scope",
        "close",
        "extras",
        "WorkspaceService",
        "ProductService",
        "ConstantsService",
        "$http",
        "$rootScope",
        "ProcessorService",
        "ProcessorMediaService",
        "ModalService",
        "ImagesService"
    ];
    return ProcessorController;
})();
window.ProcessorController = ProcessorController;
