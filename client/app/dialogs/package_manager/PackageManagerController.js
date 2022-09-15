let PackageManagerController = (function () {
    function PackageManagerController(
        oPackageManagerService,
        $scope,
        oExtras,
        $timeout,
        oTranslate
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
        this.sPackageName = "";
        this.sort = {
            column: "",
            descending: false,
        };

        /* 
        Get list of packages
        */
        this.fetchPackageList();
        console.log(this.sProcessorId);
        /* 
        Get package manager information (name and version)
        */
        this.fetchPackageManagerInfo(this.sWorkspaceName);
    }
    PackageManagerController.prototype.fetchPackageManagerInfo = function (
        sWorkspaceName
    ) {
        let oController = this;

        oController.m_oPackageManagerService
            .getPackageManagerInfo(sWorkspaceName)
            .then((data, status) => {
                console.log(data);
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
        oController.m_bIsLoading = true;

        let sConfirmMsg1 = this.m_oTranslate.instant("MSG_REMOVE_LIB_PM_1");
        let sConfirmMsg2 = this.m_oTranslate.instant("MSG_REMOVE_LIB_PM_2");

        utilsVexDialogConfirm(
            sConfirmMsg1 + sDeleteCommand + sConfirmMsg2,
            function (value) {
                if (value) {
                    oController.m_oPackageManagerService
                        .deleteLibrary(sProcessorId, sDeleteCommand)
                        .then(function () {
                            oController.m_oTimeout(function () {
                                oController.fetchPackageList();
                            }, 4000);
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
        let oController = this;
        let sConfirmMsg1 = this.m_oTranslate.instant("MSG_ADD_LIB_PM_1");
        let sConfirmMsg2 = this.m_oTranslate.instant("MSG_ADD_LIB_PM_2");
        utilsVexDialogConfirm(
            sConfirmMsg1 + sPackageName + sConfirmMsg2,
            function (value) {
                if (value) {
                    oController.m_oPackageManagerService
                        .addLibrary(sProcessorId, sPackageName)
                        .then(function () {
                            oController.m_bIsLoading = true;
                            oController.m_oTimeout(function () {
                                oController.fetchPackageList();
                            }, 4000);
                        });
                }
                oController.m_bIsLoading = false;
            }
        );
    };

    PackageManagerController.prototype.ugradeLibrary = function (
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
                            oController.m_oTimeout(function () {
                                oController.fetchPackageList();
                            }, 4000);
                        });
                }
            }
        );
    };

    PackageManagerController.prototype.changeSorting = function (column) {
        var sort = this.sort;
        this.column = column;

        if (sort.column == column) {
            sort.descending = !sort.descending;
        } else {
            sort.column = column;
        }
    };

    PackageManagerController.prototype.fetchPackageList = function () {
        let oController = this;

        this.m_oPackageManagerService
            .getPackagesList(this.sWorkspaceName)
            .then(function (data, status) {
                if (data.data != null) {
                    console.log(oController.m_aoPackages);
                    oController.m_aoPackages = data.data;
                    oController.m_bIsLoading = false;
                    console.log(oController.m_aoPackages);
                }
            });
    };

    PackageManagerController.$inject = [
        "PackageManagerService",
        "$scope",
        "extras",
        "$timeout",
        "$translate",
    ];
    return PackageManagerController;
})();
window.PackageManagerController = PackageManagerController;
