"use strict";
angular
    .module("wasdi.PackageManagerService", [])
    .service("PackageManagerService", [
        "ConstantsService",
        "$rootScope",
        "$http",
        function (oConstantsService, $rootScope, $http) {
            this.APIURL = oConstantsService.getAPIURL();
            this.m_oHttp = $http;
            this.m_oController = this;
            this.m_oConstantService = oConstantsService;
            this.m_sResource = "/packageManager";

            /* 
            Return Package Manager Version 
            */
            this.getPackageInfo = function (sWorkspaceName) {
                return this.m_oHttp
                    .get(
                        this.APIURL +
                            this.m_sResource +
                            "/managerVersion?name=" +
                            sWorkspaceName
                    )
                    .then((response) => {
                        return response.data;
                    });
            };
            /*
            Return list of packages
            */
            this.getPackages = function (sWorkspaceName) {
                return this.m_oHttp
                    .get(
                        this.APIURL +
                            this.m_sResource +
                            "/listPackages?name=" +
                            sWorkspaceName
                    )
                    .then((response) => {
                        return response.data;
                    });
            };
            /*
            Remove a package
            */
            this.updateLibrary = function (sProcessorId, sUpdateCommand) {
                let oWorkspace = this.m_oConstantService.getActiveWorkspace();
                let sWorkspaceId = "-";
               

                if (utilsIsObjectNullOrUndefined(oWorkspace) == false) {
                    sWorkspaceId = oWorkspace.workspaceId;
                }
                return this.m_oHttp.get(
                    this.APIURL +
                        "/processors/environmentupdate?processorId=" +
                        sProcessorId +
                        "&workspace=" +
                        sWorkspaceId +
                        "&updateCommand=" +
                        sUpdateCommand
                );
            };
        },
    ]);
