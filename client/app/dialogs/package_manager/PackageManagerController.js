let PackageManagerController = (function () {
    function PackageManagerController(
        oPackageManagerService,
        $scope,
        oExtras,
        $timeout
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

        utilsVexDialogConfirm(
            "Are you sure you wish to delete " +
                sDeleteCommand +
                " from this processor?",
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
            }
        );
    };

    PackageManagerController.prototype.addLibrary = function (
        sProcessorId,
        sPackageName
    ) {
        let oController = this;

        utilsVexDialogConfirm(
            "You wish to add " + sPackageName + " to this Processor?",
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
            }
        );
    };

    PackageManagerController.prototype.ugradeLibrary = function (
        sProcessorId,
        sPackageName,
        sPackageCurrentVersion
    ) {
        let oController = this;

        utilsVexDialogConfirm(
            "Are you sure you want to upgrade " + sPackageName + "?",
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
    ];
    return PackageManagerController;
})();
window.PackageManagerController = PackageManagerController;
