/**
 * Created by a.corrado on 14/02/2017.
 */


var OrbitInfoController = (function() {

    function OrbitInfoController($scope, oClose,oExtras) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oOrbit = oExtras;
        //$scope.close = oClose;
        $scope.close = function(result) {
            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };

    }

    OrbitInfoController.$inject = [
        '$scope',
        'close',
        'extras',
    ];
    return OrbitInfoController;
})();
window.OrbitInfoController = OrbitInfoController;
