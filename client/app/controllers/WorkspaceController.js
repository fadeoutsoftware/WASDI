/**
 * Created by p.campanella on 21/10/2016.
 */

var WorkspaceController = (function() {
    function WorkspaceController($scope, $location, oConstantsService, oAuthService, oWorkspaceService,$state,oProductService, oRabbitStompService,oGlobeService) {
        this.m_oScope = $scope;
        this.m_oLocation  = $location;
        this.m_oAuthService = oAuthService;
        this.m_oWorkspaceService = oWorkspaceService;
        this.m_oConstantsService = oConstantsService;
        this.m_oScope.m_oController=this;
        this.m_aoWorkspaceList = [];
        this.m_oProductService = oProductService;
        this.m_oState = $state;
        this.m_oScope.m_oController = this;
        this.m_aoProducts = [];//the products of the workspace selected
        this.m_bIsOpenInfo = false;
        this.m_bIsVisibleFiles = false;
        this.m_oWorkspaceSelected = null;
        this.m_oRabbitStompService = oRabbitStompService;
        this.m_oGlobeService = oGlobeService;
        this.fetchWorkspaceInfoList();

        this.m_oRabbitStompService.unsubscribe();
        // this.m_oGlobeService.initGlobe('cesiumContainer3');
        this.m_oGlobeService.initRotateGlobe('cesiumContainer3');
    }

    WorkspaceController.prototype.moveTo = function (sPath) {
        this.m_oLocation.path(sPath);
    }


    WorkspaceController.prototype.createWorkspace = function () {

        var oController = this;

        this.m_oWorkspaceService.createWorkspace().success(function (data, status) {
            if (data != null)
            {
                if (data != undefined)
                {
                    var sWorkspaceId = data.stringValue;
                    oController.openWorkspace(sWorkspaceId);
                }
            }
        }).error(function (data,status) {
            //alert('error');
            utilsVexDialogAlertTop('Error in create WorkSpace. WorkspaceController.js');
        });
    };


    WorkspaceController.prototype.openWorkspace = function (sWorkspaceId) {

        var oController = this;



        this.m_oWorkspaceService.getWorkspaceEditorViewModel(sWorkspaceId).success(function (data, status) {
            if (data != null)
            {
                if (data != undefined)
                {
                    oController.m_oConstantsService.setActiveWorkspace(data);
                    oController.m_oRabbitStompService.subscribe(sWorkspaceId);
                    oController.m_oState.go("root.editor", { workSpace : sWorkspaceId });//use workSpace when reload editor page
                    //oController.m_oLocation.path('editor');
                }
            }
        }).error(function (data,status) {
            //alert('error');
            utilsVexDialogAlertTop('Error OpenWorkspace. WorkSpaceController.js');
        });
    }

    WorkspaceController.prototype.getWorkspaceInfoList = function () {
        return this.m_aoWorkspaceList;
    }

    WorkspaceController.prototype.fetchWorkspaceInfoList = function () {

        if (this.m_oConstantsService.getUser() != null) {
            if (this.m_oConstantsService.getUser() != undefined) {

                var oController = this;

                this.m_oWorkspaceService.getWorkspacesInfoListByUser().success(function (data, status) {
                    if (data != null)
                    {
                        if (data != undefined)
                        {
                            oController.m_aoWorkspaceList = data;

                        }
                    }
                    //oController.m_bIsVisibleFiles = true;

                }).error(function (data,status) {
                    //alert('error');
                    utilsVexDialogAlertTop('Error in WorkspacesInfo. WorkspaceController.js');
                });
            }
        }

    };

    WorkspaceController.prototype.loadProductList = function(oWorkspace)
    {
        if(utilsIsObjectNullOrUndefined(oWorkspace))
            return false;
        if(utilsIsStrNullOrEmpty(oWorkspace.workspaceId))
            return false;
        var oController = this;

        this.m_bIsVisibleFiles = true;
        this.m_bIsOpenInfo = false;
        this.m_aoProducts = [];

        var oWorkspaceId = oWorkspace.workspaceId;
        this.m_oWorkspaceSelected = oWorkspace;

        this.m_bIsVisibleFiles = true;
        this.m_oProductService.getProductListByWorkspace(oWorkspaceId).success(function (data, status) {
            if(!utilsIsObjectNullOrUndefined(data))
            {
                oController.m_aoProducts = [];
                for(var iIndex = 0; iIndex < data.length; iIndex++)
                {
                    oController.m_aoProducts.push(data[iIndex]);
                }
                oController.m_bIsOpenInfo = true;
            }

            if(utilsIsObjectNullOrUndefined( oController.m_aoProducts) || oController.m_aoProducts.length == 0)
                oController.m_bIsVisibleFiles=false;

        }).error(function (data,status) {
            utilsVexDialogAlertTop("Error: loading product fails");
        });

        return true;
    }
    WorkspaceController.prototype.getProductList = function()
    {
        return this.m_aoProducts;
    }
    WorkspaceController.prototype.isEmptyProductList = function()
    {
        if(utilsIsObjectNullOrUndefined(this.m_aoProducts) || this.m_aoProducts.length == 0)
            return true;
        return false;
    }

    WorkspaceController.prototype.isSelectedRowInWorkspaceTable = function(oWorkspace)
    {
        if(utilsIsObjectNullOrUndefined(oWorkspace))
            return '';
        if(utilsIsStrNullOrEmpty(oWorkspace.workspaceId))
            return '';

        if(utilsIsObjectNullOrUndefined(this.m_oWorkspaceSelected))
            return '';
        if(utilsIsStrNullOrEmpty(this.m_oWorkspaceSelected.workspaceId))
            return '';

        if(oWorkspace.workspaceId != this.m_oWorkspaceSelected.workspaceId)
            return '';

        return 'selected-row';
    }

	WorkspaceController.prototype.DeleteWorkspace = function (sWorkspaceId) {

        var oController = this;

        utilsVexDialogConfirmWithCheckBox("Are you sure to delete the Workspace ?", function (value) {
            var bDeleteFile = false;
            var bDeleteLayer = false;
            if (value) {
                if (value.files == 'on')
                    bDeleteFile = true;
                if (value.geoserver == 'on')
                    bDeleteLayer = true;

                oController.m_oWorkspaceService.DeleteWorkspace(sWorkspaceId, bDeleteFile, bDeleteLayer).success(function (data, status) {
                    oController.fetchWorkspaceInfoList();

                }).error(function (data, status) {

                });
            }
        });
    };

    WorkspaceController.$inject = [
        '$scope',
        '$location',
        'ConstantsService',
        'AuthService',
        'WorkspaceService',
        '$state',
        'ProductService',
        'RabbitStompService',
        'GlobeService'
    ];
    return WorkspaceController;
}) ();