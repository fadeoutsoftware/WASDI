let ShareOrganizationController = (function () {
    function ShareOrganizationController(
        $scope, 
        oClose,
        oExtras, 
        oOrganizationService
    ){}

    ShareOrganizationController.$inject = [
        "$scope", 
        "close", 
        "extras", 
        "OrganizationService"
    ];
    return ShareOrganizationController; 
})();
window.ShareOrganizationController = ShareOrganizationController; 