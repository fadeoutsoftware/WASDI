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

            //fetch package manager info
            this.getPackageManagerInfo = function (sWorkspaceName) {
                return this.m_oHttp
                    .get(
                        this.APIURL +
                            this.m_sResource +
                            "/managerVersion?name=" +
                            sWorkspaceName
                    )
                   
            };
            /*
            Return list of packages
            */
            this.getPackagesList = function (sWorkspaceName) {
                return this.m_oHttp.get(
                    this.APIURL +
                        this.m_sResource +
                        "/listPackages?name=" +
                        sWorkspaceName
                );
            };
            /*
            Remove a library
            */
            this.deleteLibrary = function (sProcessorId, sLibraryName) {
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
                        "&updateCommand=removePackage/" +
                        sLibraryName +
                        "/"
                );
            };
            /*
            Add a Package
            */
            this.addLibrary = function (sProcessorId, sLibraryName) {
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
                        "&updateCommand=addPackage/" +
                        sLibraryName +
                        "/"
                );
            };
            /*
            Update a Package
            */
            this.updateLibrary = function (sProcessorId, sLibraryName) {
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
                        "&updateCommand=upgradePackage/" +
                        sLibraryName +
                        "/"
                );
            };
        },
    ]);
