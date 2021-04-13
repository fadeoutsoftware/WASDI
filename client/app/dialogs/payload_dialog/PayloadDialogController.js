/**
 * Created by m.menapace on 16/02/2021
 */

var PayloadDialogController = (function () {

    function PayloadDialogController($scope, oExtras) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oProcess = oExtras.process;
        this.m_sPayloadString="";

        if (utilsIsObjectNullOrUndefined(oExtras.process.payload) ){
            this.m_sPayloadString = "No payload available for the selected process";
        }
        else{
            this.m_sPayloadString = oExtras.process.payload;
        }
    } // constructor

    PayloadDialogController.prototype.copyPayload = function() {

        if (this.m_oProcess.payload != null) {
            // Create new element
            var el = document.createElement('textarea');
            // Set value (string to be copied)
            el.value = this.m_oProcess.payload;
            // Set non-editable to avoid focus and move outside of view
            el.setAttribute('readonly', '');
            el.style = {position: 'absolute', left: '-9999px'};
            document.body.appendChild(el);
            // Select text inside element
            el.select();
            // Copy text to clipboard
            document.execCommand('copy');
            // Remove temporary element
            document.body.removeChild(el);

            var oDialog = utilsVexDialogAlertBottomRightCorner("PAYLOAD COPIED<br>READY");
            utilsVexCloseDialogAfter(4000,oDialog);
        }
        else {
            var oDialog = utilsVexDialogAlertBottomRightCorner("NO PAYLOAD TO COPY<br>READY");
            utilsVexCloseDialogAfter(4000,oDialog);
        }
    };





    PayloadDialogController.$inject = [
        '$scope',
        'extras'
    ]
    return PayloadDialogController;
})();
window.PayloadDialogController = PayloadDialogController;
