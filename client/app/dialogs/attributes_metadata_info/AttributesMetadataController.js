/**
 * Created by a.corrado on 23/05/2017.
 */
var AttributesMetadataController = (function() {

    function AttributesMetadataController($scope, oClose,oExtras) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_aoAttributes = this.m_oExtras.metadataAttributes;
        this.m_sNameNode = this.m_oExtras.nameNode;

        /*metadataAttributes:node.original.attributes*/
        //$scope.close = oClose;
        $scope.close = function(result) {
            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };

    }

    AttributesMetadataController.$inject = [
        '$scope',
        'close',
        'extras',
    ];
    return AttributesMetadataController;
})();
window.AttributesMetadataController = AttributesMetadataController;
