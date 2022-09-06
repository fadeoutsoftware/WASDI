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
        },
    ]);
