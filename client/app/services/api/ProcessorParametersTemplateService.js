/**
 * Created by PetruPetrescu on 25/11/2021.
 */

'use strict';
angular.module('wasdi.ProcessorParametersTemplateService', ['wasdi.ProcessorParametersTemplateService']).
service('ProcessorParametersTemplateService', ['ConstantsService','$http', function (oConstantsService,$http) {
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;
    this.m_sResource = "/processorParamTempl";

    /**
     * Get the processor parameter templates list associated with a processor.
     * @param sProcessorId
     * @returns {*}
     */
    this.getProcessorParametersTemplatesListByProcessor = function(sProcessorId) {
        return this.m_oHttp.get(this.APIURL + this.m_sResource + '/getlist?processorId=' + sProcessorId );
    };

    /**
     * Get the processor parameter template by template Id.
     * @param sTemplateId
     * @returns {*}
     */
    this.getProcessorParametersTemplate = function(sTemplateId) {
        return this.m_oHttp.get(this.APIURL + this.m_sResource + '/get?templateId=' + sTemplateId );
    };

    /**
     * Update the processor parameter template.
     * @param oTemplate the processor parameter template
     * @returns {*}
     */
    this.updateProcessorParameterTemplate = function (oTemplate) {
        return this.m_oHttp.post(this.APIURL + this.m_sResource + '/update', oTemplate);
    }

    /**
     * Add the processor parameter template.
     * @param oTemplate the processor parameter template
     * @returns {*}
     */
    this.addProcessorParameterTemplate = function (oTemplate) {
        return this.m_oHttp.post(this.APIURL + this.m_sResource + '/add', oTemplate);
    }

    /**
     * Delete the processor parameter template.
     * @param sTemplateId
     * @returns {*}
     */
    this.deleteProcessorParameterTemplate = function(sTemplateId) {
        return this.m_oHttp.delete(this.APIURL + this.m_sResource + '/delete?templateId=' + sTemplateId);
    };

}]);
