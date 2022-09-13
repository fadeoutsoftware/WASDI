let PackageManagerController = (function () {
    function PackageManagerController(oPackageManagerService, $scope, oExtras) {
        this.m_oScope = $scope;
        this.m_oPackageManagerService = oPackageManagerService;
        this.m_oScope.m_oController = this;
        this.oExtras = oExtras;
        this.m_aoPackages = [];
        this.m_bIsLoading = true;
        this.sWorkspaceName = oExtras.processor.processorName;
        this.sProcessorId = oExtras.processor.processorId;

        this.m_sPackageManagerName = "";
        this.m_sPackageManagerVersion = "";
        this.bIsEditing = false;
        this.m_oPackageInfo = {};
        this.sort = {
            column: "",
            descending: false,
        };

        /* 
        Get list of packages
        */
        this.fetchPackageList(); 
        /* 
        Get package manager information (name and version)
        */

        this.m_oPackageManagerService
            .getPackageInfo(this.sWorkspaceName)
            .then((response) => {
                console.log(response);
                this.m_sPackageManagerName = response.name;
                this.m_sPackageManagerVersion = response.version;
                console.log(this.m_sPackageManagerName);
                console.log(this.oExtras.processor.processorId);
            });
        /*
        Enter package info into input fields
        */

        this.editPackage = function (oPackage) {
            console.log(this.m_oPackageInfo);
            this.bIsEditing = true;
            this.m_oPackageInfo.name = oPackage.packageName;
            this.m_oPackageInfo.currentVersion = oPackage.currentVersion;
        };
    }
    PackageManagerController.prototype.removeLibrary = function (
        sProcessorId,
        sDeleteCommand
    ) {
        this.sDeleteCommand = "removePackage/" + sDeleteCommand + "/";
        this.m_oPackageManagerService.updateLibrary(
            sProcessorId,
            this.sDeleteCommand
        );
        console.log("Package Removed");
    };

    PackageManagerController.prototype.addLibrary = function (
        sProcessorId,
        oPackageInfo
    ) {
        this.oPackage = angular.copy(oPackageInfo);
        this.sUpdateCommand =
            "addPackage/" +
            this.oPackage.name +
            "/" +
            this.oPackage.currentVersion +
            "/";

        this.m_oPackageManagerService.updateLibrary(
            sProcessorId,
            this.sUpdateCommand
        );
        console.log("Package Added");
    };

    PackageManagerController.prototype.updatePackage = function (
        sProcessorId,
        oPackageInfo
    ) {
        this.oPackage = angular.copy(oPackageInfo);
        this.sUpdateCommand =
            "upgradePackage/" +
            this.oPackage.name +
            "/" +
            this.oPackage.currentVersion +
            "/";

        this.m_oPackageManagerService.updateLibrary(
            sProcessorId,
            this.sUpdateCommand
        );
        console.log("Package Updated");
        this.bIsEditing = false;
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
            .getPackages(this.sWorkspaceName)
            .then(function (data, status) {
                if (data != null) {
                    oController.m_aoPackages = data;
                    oController.m_bIsLoading = false;
                }
            });
    };

    PackageManagerController.$inject = [
        "PackageManagerService",
        "$scope",
        "extras",
    ];
    return PackageManagerController;
})();
window.PackageManagerController = PackageManagerController;
