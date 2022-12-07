let PackageManagerController = (function () {
    function PackageManagerController(
        $scope,
        oClose,
        oPackageManagerService,
        oExtras,
        $timeout,
        oTranslate,
        oRabbitStompService
    ) {
        this.m_oScope = $scope;
        this.m_oPackageManagerService = oPackageManagerService;
        this.m_oScope.m_oController = this;
        this.m_oTimeout = $timeout;
        this.oExtras = oExtras;
        this.m_aoPackages = [];
        this.m_bIsLoading = true;
        this.sWorkspaceName = oExtras.processor.processorName;
        this.sProcessorId = oExtras.processor.processorId;
        this.m_oTranslate = oTranslate;
        this.m_sPackageManagerName = "";
        this.m_sPackageManagerVersion = "";
        this.bIsEditing = false;
        this.m_sPackageName = "";
        this.m_oSort = {
            sColumn: "",
            bDescending: false,
        };
        this.m_oRabbitStompService = oRabbitStompService;

        /* 
       Get package manager information (name and version)
       */
        this.fetchPackageManagerInfo(this.sWorkspaceName);
        /* 
        Get list of packages
        */
        this.fetchPackageList();
        /* 
        RabbitStomp Service call
        */
        this.m_iHookIndex = this.m_oRabbitStompService.addMessageHook(
            "ENVIRONMENTUPDATE",
            this,
            this.rabbitMessageHook
        );

        var oController = this;

        // Close this Dialog handler
        $scope.close = function () {
            oController.m_oRabbitStompService.removeMessageHook(oController.m_iHookIndex);

            // close, but give 500ms for bootstrap to animate
            oClose(null, 300);
        };

        $scope.add = function (result) {
            oController.m_oRabbitStompService.removeMessageHook(oController.m_iHookIndex);
            oClose(result, 300); // close, but give 500ms for bootstrap to animate
        };
    }
    PackageManagerController.prototype.fetchPackageManagerInfo = function (
        sWorkspaceName
    ) {
        let oController = this;

        oController.m_oPackageManagerService
            .getPackageManagerInfo(sWorkspaceName)
            .then((data, status) => {
                if (data.data != null) {
                    if (data.data != undefined) {
                        oController.m_sPackageManagerName = data.data.name;
                        oController.m_sPackageManagerVersion =
                            data.data.version;
                    }
                }
            });
    };
    PackageManagerController.prototype.removeLibrary = function (
        sProcessorId,
        sDeleteCommand
    ) {
        let oController = this;

        let sConfirmMsg1 = this.m_oTranslate.instant("MSG_REMOVE_LIB_PM_1");
        let sConfirmMsg2 = this.m_oTranslate.instant("MSG_REMOVE_LIB_PM_2");

        utilsVexDialogConfirm(
            sConfirmMsg1 + sDeleteCommand + sConfirmMsg2, function (oValue) {
                if (oValue === true) {
                    oController.m_oPackageManagerService.deleteLibrary(sProcessorId, sDeleteCommand).then(function (data) {
                        oController.m_bIsLoading = true
                    });
                }
                oController.m_bIsLoading = false;
            }
        );
    };

    PackageManagerController.prototype.addLibrary = function (
        sProcessorId,
        sPackageName
    ) {
        let aPackageInfo = sPackageName.split("==");

        let aPackageInfoTrimmed = aPackageInfo.map((sElement) => {
            return sElement.replace(/["]+/g, "");
        });
        let sPackageInfoName = aPackageInfoTrimmed[0];
        let sPackageInfoVersion = "";
        let sAddCommand = ""; 
        if (aPackageInfoTrimmed[1]) {
            sPackageInfoVersion = aPackageInfoTrimmed[1];
        }

        let oController = this;
        let sConfirmMsg1 = this.m_oTranslate.instant("MSG_ADD_LIB_PM_1");
        let sConfirmMsg2 = this.m_oTranslate.instant("MSG_ADD_LIB_PM_2");

        if (utilsIsStrNullOrEmpty(sPackageInfoVersion) === false) {
            sAddCommand = sPackageInfoName + "/" + sPackageInfoVersion;

            utilsVexDialogConfirm(
                sConfirmMsg1 +
                sPackageInfoName +
                sPackageInfoVersion +
                sConfirmMsg2,
                function (value) {
                    if (value) {
                        oController.m_oPackageManagerService
                            .addLibrary(sProcessorId, sAddCommand)
                            .then(function () {
                                oController.m_bIsLoading = true;
                            });
                    }
                    oController.m_bIsLoading = false;
                }
            );
        } else {
            utilsVexDialogConfirm(
                sConfirmMsg1 + sPackageInfoName + sConfirmMsg2,
                function (value) {
                    if (value) {
                        oController.m_oPackageManagerService
                            .addLibrary(sProcessorId, sPackageName)
                            .then(function () {
                                oController.m_bIsLoading = true;
                            });
                    }
                    oController.m_bIsLoading = false;
                }
            );
        }
    };

    PackageManagerController.prototype.updateLibraryList = function (sProcessorId) {
        let oController = this;

        oController.m_oPackageManagerService
            .addLibrary(sProcessorId, null)
            .then(function () {
                oController.m_bIsLoading = true;
            });
    };

    PackageManagerController.prototype.upgradeLibrary = function (
        sProcessorId,
        sPackageName,
        sPackageCurrentVersion
    ) {
        let oController = this;
        let sConfirmMsg1 = this.m_oTranslate.instant("MSG_UPGRADE_LIB_PM_1");
        let sConfirmMsg2 = this.m_oTranslate.instant("MSG_UPGRADE_LIB_PM_2");
        utilsVexDialogConfirm(
            sConfirmMsg1 + sPackageName + sConfirmMsg2,
            function (value) {
                if (value) {
                    oController.m_oPackageManagerService
                        .upgradeLibrary(
                            sProcessorId,
                            sPackageName,
                            sPackageCurrentVersion
                        )
                        .then(function () {
                            oController.m_bIsLoading = true;
                        });
                }
            }
        );
    };

    PackageManagerController.prototype.changeSorting = function (sColumn) {

        if (this.m_oSort.sColumn == sColumn) {
            this.m_oSort.bDescending = !this.m_oSort.bDescending;
        } else {
            this.m_oSort.sColumn = sColumn;
        }
    };

    PackageManagerController.prototype.fetchPackageList = function () {
        let oController = this;

        this.m_oPackageManagerService
            .getPackagesList(this.sWorkspaceName)
            .then(function (data, status) {
                if (data.data != null) {
                    oController.clearInput();
                    oController.m_aoPackages = data.data;
                }
            });
        oController.m_bIsLoading = false;
    };

    PackageManagerController.prototype.clearInput = function () {
        let oController = this;

        oController.m_sPackageName = "";
    }

    PackageManagerController.prototype.rabbitMessageHook = function (
        oRabbitMessage,
        oController
    ) {
        oController.fetchPackageList()
        oController.m_bIsLoading = false;
    };

    PackageManagerController.$inject = [
        "$scope",
        'close',
        "PackageManagerService",
        "extras",
        "$timeout",
        "$translate",
        "RabbitStompService"
    ];
    return PackageManagerController;
})();
window.PackageManagerController = PackageManagerController;
