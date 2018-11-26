/**
 * Created by a.corrado on 31/03/2017.
 */


var ListFloodAreaDetectionController = (function() {

    function ListFloodAreaDetectionController($scope, oClose,oExtras) {
        //MEMBERS
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;

        $scope.close = function(result) {
            oClose(result, 300); // close, but give 500ms for bootstrap to animate
        };

    };



    ListFloodAreaDetectionController.$inject = [
        '$scope',
        'close',
        'extras',

    ];
    return ListFloodAreaDetectionController;
})();
