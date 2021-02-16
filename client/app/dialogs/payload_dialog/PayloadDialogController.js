/**
 * Created by m.menapace on 16/02/2021
 */

var PayloadDialogController = (function () {

    function PayloadDialogController($scope, oExtras) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_sPayloadString="";

        if (utilsIsObjectNullOrUndefined(oExtras.process.payload) ){
            this.m_sPayloadString = "No payload available for the selected process";
        }
        else{
            this.m_sPayloadString = oExtras.process.payload;
        }
    }

    PayloadDialogController.$inject = [
        '$scope',
        'extras'
    ]
    return PayloadDialogController;
})();
