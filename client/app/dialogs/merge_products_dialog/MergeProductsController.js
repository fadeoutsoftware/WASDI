/**
 * Created by a.corrado on 28/02/2017.
 */
var MergeProductsController = (function() {

    function MergeProductsController($scope, oClose,oExtras,Upload) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_sLog = '';
        this.m_oFile = null;
        //$scope.close = oClose;
        $scope.close = function(result) {
            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };
        $scope.$watch('$scope.m_oFile', function () {
            if ($scope.m_oFile != null) {
                //TODO M_OFILE IS AN ARRAY CHECK IT
                $scope.upload($scope.m_oFile);
            }
        });
        //$scope.$watch('$scope.m_oFile', function () {
        //    if ($scope.file != null) {
        //        $scope.files = [$scope.file];
        //    }
        //});
    }

    MergeProductsController.prototype.upload = function (oFile) {

    };
    MergeProductsController.$inject = [
        '$scope',
        'close',
        'extras',
        'Upload'
    ];
    return MergeProductsController;
})();
