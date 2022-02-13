/**
 * Created by a.corrado on 31/03/2017.
 */


var SnakeController = (function() {

    function SnakeController($scope, oClose) {
        //MEMBERS
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        $scope.close = function() {
            oClose("close", 500); // close, but give 300ms for bootstrap to animate
        };

    }

    SnakeController.$inject = [
        '$scope',
        'close',


    ];
    return SnakeController;
})();
window.SnakeController = SnakeController;
