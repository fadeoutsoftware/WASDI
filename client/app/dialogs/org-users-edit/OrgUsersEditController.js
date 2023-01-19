let OrgUsersEditController = (function () {
    function OrgUsersEditController($scope, oClose, oExtras, oOrganizationService) {
        this.m_oScope = $scope; 
        this.oExtras = oExtras; 
        this.m_oOrganizationService = oOrganizationService; 
        
        $scope.close = function(result) {
            oClose(result, 500)
        }
    }
    OrgUsersEditController.$inject = [
        "$scope",
        "close",
        "extras", 
        "OrganizationService"
    ];
    return OrgUsersEditController; 
})();
window.OrgUsersEditController = OrgUsersEditController; 