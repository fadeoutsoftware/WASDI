/**
 * Created by p.petrescu on 27/01/2023.
 */

'use strict';
angular.module('wasdi.ProjectService', ['wasdi.ProjectService']).
service('ProjectService', ['$http',  'ConstantsService', function ($http, oConstantsService) {
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;

    this.getProjectsListByUser = function () {
        return this.m_oHttp.get(this.APIURL + '/projects/byuser');
    };

    this.getValidProjectsListByUser = function () {
        return this.m_oHttp.get(this.APIURL + '/projects/byuser?valid=true');
    };

    this.getProjectsListBySubscription = function (sSubscriptionId) {
        return this.m_oHttp.get(this.APIURL + '/projects/bysubscription?subscription=' + sSubscriptionId);
    };

    this.getProjectById = function (sProjectId) {
        return this.m_oHttp.get(this.APIURL + '/projects/byId' + (sProjectId == null ? "" : "?project=" + sProjectId));
    };

    this.saveProject = function (oProject) {
        if (utilsIsStrNullOrEmpty(oProject.projectId)) {
            return this.createProject(oProject);
        } else {
            return this.updateProject(oProject);
        }
    };

    this.createProject = function (oProject) {
        return this.m_oHttp.post(this.APIURL + '/projects/add', oProject);
    };

    this.updateProject = function (oProject) {
        return this.m_oHttp.put(this.APIURL + '/projects/update', oProject);
    };

    this.changeActiveProject = function (sProjectId) {
        return this.m_oHttp.put(this.APIURL + '/projects/active' + (sProjectId == null ? "" : "?project=" + sProjectId));
    };

    this.deleteProject = function (sProjectId) {
        return this.m_oHttp.delete(this.APIURL + '/projects/delete?project=' + sProjectId);
    };

}]);
