/**
 * Created by a.corrado on 24/05/2017.
 */


var DeleteProcessController = (function () {

    function DeleteProcessController($scope, oClose) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        // this.m_oExtras = oExtras;
        //$scope.close = oClose;
        $scope.close = function (result) {
            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };

    }

    DeleteProcessController.$inject = [
        '$scope',
        'close',

    ];
    return DeleteProcessController;
})();
window.DeleteProcessController = DeleteProcessController;
