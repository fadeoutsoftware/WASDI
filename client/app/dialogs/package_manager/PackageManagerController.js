let PackageManagerController = (function () {
    function PackageManagerController(
        oPackageManagerService,
        $scope,
        oExtras
    ) {
        this.m_oScope = $scope;

        this.m_oScope.m_oController = this;
        this.m_oPackageManagerService = oPackageManagerService;

        this.oExtras = oExtras;
        this.sWorkspaceName = oExtras.processor.processorName;
        this.m_aoPackages = [];
        this.m_oPackageManagerService
            .getPackages(this.sWorkspaceName)
            .then((response) => {
                console.log(response);
                this.m_aoPackages = response;  
            });
        console.log(this.oExtras);
    }
    PackageManagerController.prototype.click = function () {
        console.log(this.m_aoPackages);
    };
    PackageManagerController.$inject = [
        "PackageManagerService",
        "$scope",
        "extras",
    ];
    return PackageManagerController;
})();
window.PackageManagerController = PackageManagerController;
