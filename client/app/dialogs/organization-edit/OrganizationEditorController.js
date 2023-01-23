let OrganizationEditorController = (function () {
    function OrganizationEditorController(
        $scope,
        oClose,
        oExtras,
        oOrganizationService
    ) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.oExtras = oExtras;
        this.oClose = oClose;

        this.m_oOrganizationService = oOrganizationService;

        this.m_oEditOrganization = oExtras.organization;

        $scope.close = function (result) {
            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };


    };

    OrganizationEditorController.prototype.saveOrganizationInfo = function () {
        let oController = this;
        this.m_oOrganizationService.saveOrganization(this.m_oEditOrganization).then(function (data) {
            if (utilsIsObjectNullOrUndefined(data.data) === false && data.data.boolValue === true) {
                let oDialog = utilsVexDialogAlertBottomRightCorner("ORGANIZATION SAVED<br>READY");
                utilsVexCloseDialogAfter(4000, oDialog);
                oController.m_oScope.close();
            } else {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN SAVING ORGANIZATION");
                utilsVexCloseDialogAfter(3000, oDialog);
            }
        }, function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN SAVING ORGANIZATION");
            utilsVexCloseDialogAfter(3000, oDialog);
        });

    }
    OrganizationEditorController.$inject = [
        "$scope",
        "close",
        "extras",
        "OrganizationService"
    ];
    return OrganizationEditorController;
})();
window.OrganizationEditorController = OrganizationEditorController; 