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

        this.m_oOrganizationService = oOrganizationService;

        this.m_oEditOrganization = oExtras.organization;
        this.m_bEditMode = oExtras.editMode;

        $scope.close = function (result) {
            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };
    };

    OrganizationEditorController.prototype.saveOrganization = function () {
        console.log(" OrganizationEditorController.saveOrganization");

        let oController = this;

        this.m_oOrganizationService.saveOrganization(this.m_oEditOrganization).then(function (data) {
            console.log(" OrganizationEditorController.saveOrganizationInfo | data.data: ", data.data);
            if (utilsIsObjectNullOrUndefined(data.data) === false && data.data.boolValue === true) {
                let oDialog = utilsVexDialogAlertBottomRightCorner("ORGANIZATION SAVED<br>READY");
                utilsVexCloseDialogAfter(4000, oDialog);
            } else {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN SAVING ORGANIZATION");
            }

            oController.m_oScope.close();
        }, function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN SAVING ORGANIZATION");

            oController.m_oScope.close();
        });

        this.m_oEditOrganization = {};
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