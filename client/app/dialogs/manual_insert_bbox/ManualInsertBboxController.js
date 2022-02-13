/**
 * Created by p.campanella on 13/07/2020
 */


var ManualInsertBboxController = (function() {

    function ManualInsertBboxController($scope, oClose, oExtras ,oConstantsService) {
        //MEMBERS
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oConstantsService = oConstantsService;
        this.m_oBbox = {};
        this.m_oContoller = this;

        this.m_oBbox.north="";
        this.m_oBbox.south="";
        this.m_oBbox.east="";
        this.m_oBbox.west="";


        $scope.close = function(result) {
            oClose(result, 300); // close, but give 500ms for bootstrap to animate
        };

    };

    ManualInsertBboxController.$inject = [
        '$scope',
        'close',
        'extras',
        'ConstantsService'

    ];
    return ManualInsertBboxController;
})();
window.ManualInsertBboxController=ManualInsertBboxController;
