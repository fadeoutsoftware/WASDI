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
        if(utilsIsObjectNullOrUndefined(this.m_oSelectedProduct))
            return true;
        return false;
    }

    MergeProductsController.prototype.upload = function (oFile) {
        if(utilsIsObjectNullOrUndefined(oFile))
            return false;
        if(utilsIsObjectNullOrUndefined(this.m_oWorkSpaceActive))
            return false;
        var oController = this;
        var sUrl = oController.m_oConstantService.getAPIURL();
        sUrl += "/catalog/upload?sWorkspaceId=" + oController.m_oWorkSpaceActive.workspaceId;

        //var oOptions = {
        //    type: "POST" ,
        //    url : sUrl,
        //    data : oFile,
        //    headers: {
        //        'Content-Type': "multipart/form-data;"
        //    },
        //}
        var successCallback = function(data, status)
        {
            //utilsVexDialogAlertTop("Ok");
        };

        var errorCallback = function (data, status)
        {
            utilsVexDialogAlertTop("Error in upload file");
        };
        //
        //oController.m_oHttp(oOptions).then(successCallback, errorCallback);

        var fd = new FormData();
        fd.append('file', oFile);
        oController.m_oHttp.post(sUrl, fd, {
            transformRequest: angular.identity,
            headers: {'Content-Type': undefined}
        }).then(successCallback, errorCallback);

        //$.ajax({
        //    type: "POST" ,
        //    url : sUrl,
        //    data : oFile,
        //    contentType: false,
        //    processData: false,
        //    crossDomain : true,
        //    'x-session-token': oController.m_oConstantService.getSessionId(),
        //    xhrFields: {
        //        withCredentials: true
        //    },
        //    success: function(data, status)
        //    {
        //        utilsVexDialogAlertTop("Ok");
        //    },
        //    error: function (jqXHR, textStatus, errorThrown)
        //    {
        //        utilsVexDialogAlertTop("Non è ok");
        //    }
        //});
        return true;
    };
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
