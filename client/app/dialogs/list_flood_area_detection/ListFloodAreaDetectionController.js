/**
 * Created by a.corrado on 31/03/2017.
 */


var ListFloodAreaDetectionController = (function() {

    function ListFloodAreaDetectionController($scope, oClose,oExtras) {
        //MEMBERS
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_aoProducts = this.m_oExtras.products;
        this.m_oSelectedReferenceProduct = null;
        this.m_oSelectedPostEventImageProduct = null;
        if(utilsIsObjectNullOrUndefined(this.m_aoProducts) === false)
        {
            this.m_oSelectedReferenceProduct = this.m_aoProducts[0];
            this.m_oSelectedPostEventImageProduct = this.m_aoProducts[0];
        }
        $scope.close = function(result) {
            oClose(result, 300); // close, but give 500ms for bootstrap to animate
        };

    };

    ListFloodAreaDetectionController.prototype.redirectToWebSite = function(){
        this.m_oWindow.open('http://www.mydewetra.org', '_blank');
    };

    ListFloodAreaDetectionController.prototype.runListFloodAreaDetection = function(){
        //TODO SEND REQUEST
    };


    ListFloodAreaDetectionController.$inject = [
        '$scope',
        'close',
        'extras',

    ];
    return ListFloodAreaDetectionController;
})();
