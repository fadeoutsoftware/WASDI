/**
 * Created by a.corrado on 28/02/2017.
 */
var MergeProductsController = (function() {

    function MergeProductsController($scope, oClose,oExtras,Upload,oConstantService,$http) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_sLog = '';
        this.m_oFile = null;
        this.m_aoProductsList =  oExtras.ListOfProducts;
        this.m_oSelectedProduct = oExtras.SelectedProduct;
        this.m_oWorkSpaceActive = oExtras.WorkSpaceId;
        this.m_oConstantService = oConstantService;
        this.m_oHttp = $http;
        //$scope.close = oClose;
        $scope.close = function(result) {
            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };
        $scope.$watch('m_oController.m_oFile', function () {
            if (!utilsIsObjectNullOrUndefined( $scope.m_oController.m_oFile) ) {
                //TODO M_OFILE IS AN ARRAY CHECK IT
                $scope.m_oController.upload($scope.m_oController.m_oFile[0]);
            }
        });
        //$scope.$watch('$scope.m_oFile', function () {
        //    if ($scope.file != null) {
        //        $scope.files = [$scope.file];
        //    }
        //});
    }

    MergeProductsController.prototype.isVisibleDropDownMenu = function()
    {
        if(utilsIsObjectNullOrUndefined(this.m_oSelectedProduct)) return true;
        return false;
    }

    MergeProductsController.prototype.upload = function (oFile) {
        if(utilsIsObjectNullOrUndefined(oFile)) return false;
        if(utilsIsObjectNullOrUndefined(this.m_oWorkSpaceActive)) return false;

        var oController = this;
        var sUrl = oController.m_oConstantService.getAPIURL();
        sUrl += "/catalog/upload?sWorkspaceId=" + oController.m_oWorkSpaceActive.workspaceId;

        var successCallback = function(data, status)
        {
            utilsVexDialogAlertTop("FILE UPLOADED");
        };

        var errorCallback = function (data, status)
        {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN UPLOAD FILE");
        };

        var fd = new FormData();
        fd.append('file', oFile);
        oController.m_oHttp.post(sUrl, fd, {
            transformRequest: angular.identity,
            headers: {'Content-Type': undefined}
        }).then(successCallback, errorCallback);

        return true;
    };

    MergeProductsController.prototype.assimilation = function()
    {
        var oController = this;
        var sUrl = oController.m_oConstantService.getAPIURL();
        sApi="/catalog/assimilation";
        sUrl += sApi;

        utilsVexDialogAlertTop("GURU MEDITATION<br>GETTING STARTED");

        //this.m_oHttp.get(sUrl,"responseType: 'arraybuffer'")
        this.m_oHttp({method: 'GET',url: sUrl, responseType: 'arraybuffer'})
            .success(function (data, status,headers)
            {
                if (!utilsIsObjectNullOrUndefined(data) )
                {
                    var contentType = headers['content-type'];
                    var file = new Blob([data], { type: contentType});
                    var linkElement = document.createElement('a');
                    var url = window.URL.createObjectURL(file);

                    linkElement.setAttribute('href', url);
                    linkElement.setAttribute("download", "name.tif");

                    var clickEvent = new MouseEvent("click", {
                        "view": window,
                        "bubbles": true,
                        "cancelable": false
                    });
                    linkElement.dispatchEvent(clickEvent);

                }
            }).error(function (data,status)
        {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR: ASSIMILATION DOESN'T WORK");
        });

    }
    MergeProductsController.$inject = [
        '$scope',
        'close',
        'extras',
        'Upload',
        'ConstantsService',
        '$http'
    ];
    return MergeProductsController;
})();
