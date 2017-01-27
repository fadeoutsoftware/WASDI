/**
 * Created by p.campanella on 21/10/2016.
 */

var WorkspaceController = (function() {
    function WorkspaceController($scope, $location, oConstantsService, oAuthService, oWorkspaceService,$state) {
        this.m_oScope = $scope;
        this.m_oLocation  = $location;
        this.m_oAuthService = oAuthService;
        this.m_oWorkspaceService = oWorkspaceService;
        this.m_oConstantsService = oConstantsService;
        this.m_oScope.m_oController=this;
        this.m_aoWorkspaceList = [];
        this.m_oState = $state;
        this.m_oScope.m_oController = this;

        this.fetchWorkspaceInfoList();

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
            alert('error');
        });
    }


    WorkspaceController.prototype.openWorkspace = function (sWorkspaceId) {

        var oController = this;

        this.m_oWorkspaceService.getWorkspaceEditorViewModel(sWorkspaceId).success(function (data, status) {
            if (data != null)
            {
                if (data != undefined)
                {
                    oController.m_oConstantsService.setActiveWorkspace(data);
                    oController.m_oState.go("root.editor", { workSpace : sWorkspaceId });//use workSpace when reload editor page
                    //oController.m_oLocation.path('editor');
                }
            }
        }).error(function (data,status) {
            alert('error');
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
                }).error(function (data,status) {
                    alert('error');
                });
            }
        }

    }

    WorkspaceController.$inject = [
        '$scope',
        '$location',
        'ConstantsService',
        'AuthService',
        'WorkspaceService',
        '$state'
    ];
    return WorkspaceController;
}) ();