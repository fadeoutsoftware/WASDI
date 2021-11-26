 /**
 * Created by PetruPetrescu on 23/11/2021.
 */

var ProcessorParametersTemplateListController = (function() {

    function ProcessorParametersTemplateListController($scope, oClose,oExtras,oConstantsService,oProcessorParametersTemplateService, oModalService) {

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
         * Input processor base data
         * @type {null}
         */
        this.m_oInputProcessor = this.m_oExtras.processor;

        /**
         * Constants service
         */
        this.m_oConstantsService = oConstantsService;

        /**
         * Processors Service
         */
        this.m_oProcessorParametersTemplateService = oProcessorParametersTemplateService;

        /**
         * List of processor parameters templates
         * @type {*[]}
         */
        this.m_aoProcessorParametersTemplates=[];

        /**
         * Selected processor parameters templates
         * @type {*}
         */
         this.m_oSelectedProcessorParametersTemplate={};

         /**
         * Processor Id
         * @type {string}
         */
        this.m_sProcessorId = "";
        
        this.m_oClose = oClose;

        this.m_oModalService = oModalService;

        // Are we creating a new processor or editing an existing one?
        if (this.m_oInputProcessor !== null) {

            this.m_sProcessorId = this.m_oInputProcessor.processorId;

            // Get the list of processor parameters templates for the current user and the current processor.
            this.getProcessorParametersTemplatesList(this.m_sProcessorId);
        }
    }

    
    /**
     * Get the list of processor parameters templates for the current user and the current processor.
     * @param sProcessorId
     * @returns {boolean}
     */
    ProcessorParametersTemplateListController.prototype.getProcessorParametersTemplatesList = function(sProcessorId) {

        if(utilsIsStrNullOrEmpty(sProcessorId) === true)
        {
            return false;
        }

        var oController = this;
        this.m_oProcessorParametersTemplateService.getProcessorParametersTemplatesListByProcessor(sProcessorId)
            .then(function (data) {
                if(utilsIsObjectNullOrUndefined(data.data) === false)
                {
                    oController.m_aoProcessorParametersTemplates = data.data;
                }
                else
                {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR LOADING PROCESSOR PARAMETERS TEMPLATES LIST");
                }

            },function (error) {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR LOADING PROCESSOR PARAMETERS TEMPLATES LIST");
        });
        return true;
     };

    /**
     * Select a template from the templates list
     * @param sTemplateId
     * @returns {boolean}
     */
     ProcessorParametersTemplateListController.prototype.selectProcessorParametersTemplate = function(sTemplateId) {

        if( (utilsIsObjectNullOrUndefined(sTemplateId) === true)) {
            return false;
        }

        var oController = this;

        this.m_oProcessorParametersTemplateService.getProcessorParametersTemplate(sTemplateId)
            .then(function (data) {
                if(utilsIsObjectNullOrUndefined(data.data) === false)
                {
                    // FETCHING THE TEMPLATE
                    let oTemplate = data.data;
                    oController.m_oClose(oTemplate.jsonParameters, 100); // close, but give 100ms for bootstrap to animate
                }
                else
                {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR FETCHING THE TEMPLATE");
                }

            },function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR FETCHING THE TEMPLATE");
        });

        return false;
    };

    ProcessorParametersTemplateListController.prototype.deleteProcessorParametersTemplate = function(oTemplate) {
        if(utilsIsObjectNullOrUndefined(oTemplate) === true)
        {
            return false;
        }
        var oController = this;
        var oReturnFunctionValue = function(oValue) {
            if (oValue === true)
            {
                oController.m_oProcessorParametersTemplateService.deleteProcessorParameterTemplate(oTemplate.templateId).then(function (data) {
                    oController.getProcessorParametersTemplatesList(oTemplate.processorId);
                }, (function (error) {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN DELETING THE TEMPLATE");
                }));
            }
        }
 
        utilsVexDialogConfirm("Are you SURE you want to delete the Processor Parameters Template: " + oTemplate.name + " ?", oReturnFunctionValue);
    };

    /**
     * Edit a template from the templates list
     * @param oProcessor the processor
     * @param sTemplateId the template Id
     * @returns {boolean}
     */
    ProcessorParametersTemplateListController.prototype.editProcessorParametersTemplate = function(oProcessor, sTemplateId) {

        if( (utilsIsObjectNullOrUndefined(oProcessor) === true) || (utilsIsStrNullOrEmpty(sTemplateId) === true)) {
            return false;
        }

        var oController = this;

        this.m_oProcessorParametersTemplateService.getProcessorParametersTemplate(sTemplateId)
            .then(function (data) {
                if(utilsIsObjectNullOrUndefined(data.data) === false)
                {
                    // FETCHING THE TEMPLATE
                    oController.m_oSelectedProcessorParametersTemplate = data.data;

                    oController.m_oModalService.showModal({
                        templateUrl: "dialogs/processor_parameters_template_detail/ProcessorParametersTemplateDetailView.html",
                        controller: "ProcessorParametersTemplateDetailController",
                        inputs: {
                            extras: {
                                processor: oProcessor,
                                templateId: sTemplateId
                            }
                        }
                    }).then(function (modal) {
                        modal.element.modal();
                        modal.close.then(function (oResult) {
                            if (utilsIsObjectNullOrUndefined(oResult) === false) {
                                oController.getProcessorParametersTemplatesList(oController.m_oSelectedProcessorParametersTemplate.processorId);
                            }
                        });
                    });

                }
                else
                {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR FETCHING THE TEMPLATE");
                }
            },function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR FETCHING THE TEMPLATE");
        });

        return true;
    };

    /**
     * Add a template to the templates list
     * @param oProcessor
     * @returns {boolean}
     */
    ProcessorParametersTemplateListController.prototype.addProcessorParametersTemplate = function(oProcessor) {

        if( (utilsIsObjectNullOrUndefined(oProcessor) === true)) {
            return false;
        }

        var oController = this;

        oController.m_oModalService.showModal({
            templateUrl: "dialogs/processor_parameters_template_detail/ProcessorParametersTemplateDetailView.html",
            controller: "ProcessorParametersTemplateDetailController",
            inputs: {
                extras: {
                    processor: oProcessor
                }
            }
        }).then(function (modal) {
            modal.element.modal();
            modal.close.then(function (oResult) {
                if (utilsIsObjectNullOrUndefined(oResult) === false) {
                    oController.getProcessorParametersTemplatesList(oProcessor.processorId);
                }
            });
        });

        return true;
    };

    ProcessorParametersTemplateListController.$inject = [
        '$scope',
        'close',
        'extras',
        'ConstantsService',
        'ProcessorParametersTemplateService',
        'ModalService'
    ];
    return ProcessorParametersTemplateListController;
})();
