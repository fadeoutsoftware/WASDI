 /**
 * Created by PetruPetrescu on 23/11/2021.
 */

var ProcessorParametersTemplateDetailController = (function() {

    function ProcessorParametersTemplateDetailController($scope, oClose,oExtras,oConstantsService,oProcessorParametersTemplateService, oModalService) {

        /**
         * Angular Scope
         */
        this.m_oScope = $scope;

        /**
         * Reference to the controller
         */
        this.m_oScope.m_oController = this;

        /**
         * Extra params received in input
         */
        this.m_oExtras = oExtras;

        /**
         * Input processor
         * @type {null}
         */
        this.m_oInputProcessor = this.m_oExtras.processor;

        /**
         * Input template Id
         * @type {null}
         */
        this.m_sInputTemplateId = this.m_oExtras.templateId;

        /**
         * Constants service
         */
        this.m_oConstantsService = oConstantsService;

        /**
         * Processor Parameters Template Service
         */
        this.m_oProcessorParametersTemplateService = oProcessorParametersTemplateService;

        /**
         * Flag to know if we are in Edit Mode
         * @type {boolean}
         */
        this.m_bEditMode = true;

        /**
         * Processor parameters template detail
         * @type {*}
         */
        this.m_oProcessorParametersTemplate={};
        
        this.m_oClose = oClose;

        this.m_oModalService = oModalService;

        if (this.m_oInputProcessor !== null || this.m_sInputTemplateId !== null) {

            // We are in edit mode:
            this.m_bEditMode = true;

            // Get the detail of processor parameters template by template Id or create a new one.
            this.getProcessorParametersTemplateDetail(this.m_oInputProcessor, this.m_sInputTemplateId);
        }
    }

    /**
     * Get the detail of processor parameters template by template Id.
     * @param oProcessor
     * @param sTemplateId 
     * @returns {boolean}
     */
    ProcessorParametersTemplateDetailController.prototype.getProcessorParametersTemplateDetail = function(oProcessor, sTemplateId) {
        var oController = this;

        if(utilsIsObjectNullOrUndefined(oProcessor) === true)
        {
            return false;
        }

        if(utilsIsStrNullOrEmpty(sTemplateId) === true)
        {
            try {
                var sJSONPayload = decodeURIComponent(oProcessor.paramsSample);
                var oParsed = JSON.parse(sJSONPayload);
                var sPrettyPrint = JSON.stringify(oParsed, null, 2);
            }
            catch (oError) {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR PARSING THE JSON PAYLOAD");
            }

            oController.m_oProcessorParametersTemplate = { processorId: oProcessor.processorId, jsonParameters: sPrettyPrint };
            return true;
        }

        this.m_oProcessorParametersTemplateService.getProcessorParametersTemplate(sTemplateId)
            .then(function (data) {
                if(utilsIsObjectNullOrUndefined(data.data) === false)
                {
                    oController.m_oProcessorParametersTemplate = data.data;

                    try {
                        var sJSONPayload = decodeURIComponent(oController.m_oProcessorParametersTemplate.jsonParameters);
                        var oParsed = JSON.parse(sJSONPayload);
                        var sPrettyPrint = JSON.stringify(oParsed, null, 2);
                        oController.m_oProcessorParametersTemplate.jsonParameters = sPrettyPrint;
                    }
                    catch (oError) {
                        utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR PARSING THE JSON PAYLOAD");
                    }
                     
                }
                else
                {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR LOADING PROCESSOR PARAMETERS TEMPLATE DETAIL");
                }

            },function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR LOADING PROCESSOR PARAMETERS TEMPLATE DETAIL");
        });
        return true;
    };

    ProcessorParametersTemplateDetailController.prototype.save = function() {
        var oController = this;

        oController.m_oProcessorParametersTemplate.jsonParameters = encodeURI(oController.m_oProcessorParametersTemplate.jsonParameters);

        if (oController.m_oProcessorParametersTemplate.templateId) {
            oController.m_oProcessorParametersTemplateService.updateProcessorParameterTemplate(oController.m_oProcessorParametersTemplate).then(function () {
                var oDialog = utilsVexDialogAlertBottomRightCorner("PROCESSOR PARAMETERS TEMPLATE UPDATED");
                utilsVexCloseDialogAfter(2000,oDialog);

                oController.m_oClose(true, 100); // close, but give 100ms for bootstrap to animate
            },function (error) {
                utilsVexDialogAlertTop("GURU MEDITATION<br>THERE WAS AN ERROR UPDATING PROCESSOR PARAMETERS TEMPLATE");
            });
        } else {
            oController.m_oProcessorParametersTemplateService.addProcessorParameterTemplate(oController.m_oProcessorParametersTemplate).then(function () {
                var oDialog = utilsVexDialogAlertBottomRightCorner("PROCESSOR PARAMETERS TEMPLATE ADDED");
                utilsVexCloseDialogAfter(2000,oDialog);

                oController.m_oClose(true, 100); // close, but give 100ms for bootstrap to animate
            },function (error) {
                utilsVexDialogAlertTop("GURU MEDITATION<br>THERE WAS AN ERROR ADDING PROCESSOR PARAMETERS TEMPLATE");
            });
        }

        return true;
    }

    ProcessorParametersTemplateDetailController.$inject = [
        '$scope',
        'close',
        'extras',
        'ConstantsService',
        'ProcessorParametersTemplateService',
        'ModalService'
    ];
    return ProcessorParametersTemplateDetailController;
})();
