/**
 * Created by m.menapace on 16/02/2021
 */

var PayloadDialogController = (function () {

    function PayloadDialogController($scope, oExtras) {
        this.m_oScope = $scope;
        this.m_oController = this;
        this.m_oExtras = oExtras;

        this.dumbText = "nsdovvnsd";
        // 1 show payload -> use extras and set a field of this controller
        // 2 copy payload -> use the same directive as button "copy payload"

    }


    PayloadDialogController.prototype.getDumbText = function (){
        return "dumber ";
    }
    PayloadDialogController.$inject = [
        '$scope',
        'extras'
    ]
    return PayloadDialogController;
});
