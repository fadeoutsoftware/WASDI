/**
 * Created by PetruPetrescu on 26/11/2021.
 */

var ProcessorParametersTemplateController = (function () {
    function ProcessorParametersTemplateController(
        $scope,
        oClose,
        oExtras,
        oConstantsService,
        oProcessorParametersTemplateService,
        oModalService
    ) {
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
        this.m_oProcessorParametersTemplateService =
            oProcessorParametersTemplateService;

        /**
         * List of processor parameters templates
         * @type {*[]}
         */
        this.m_aoProcessorParametersTemplates = [];

        /**
         * Input template Id
         * @type {null}
         */
        this.m_sInputTemplateId;

        /**
         * Processor Parameters Template Service
         */
        this.m_oProcessorParametersTemplateService =
            oProcessorParametersTemplateService;

        /**
         * Processor Id
         * @type {string}
         */
        this.m_sProcessorId = "";

        this.m_oClose = oClose;

        this.m_oModalService = oModalService;

        this.m_bIsLoading = false;

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
    ProcessorParametersTemplateController.prototype.getProcessorParametersTemplatesList =
        function (sProcessorId) {
            if (utilsIsStrNullOrEmpty(sProcessorId) === true) {
                return false;
            }

            var oController = this;
            this.m_oProcessorParametersTemplateService
                .getProcessorParametersTemplatesListByProcessor(sProcessorId)
                .then(
                    function (data) {
                        if (utilsIsObjectNullOrUndefined(data.data) === false) {
                            oController.m_aoProcessorParametersTemplates =
                                data.data;
                        } else {
                            utilsVexDialogAlertTop(
                                "GURU MEDITATION<br>ERROR LOADING PROCESSOR PARAMETERS TEMPLATES LIST"
                            );
                        }
                    },
                    function (error) {
                        utilsVexDialogAlertTop(
                            "GURU MEDITATION<br>ERROR LOADING PROCESSOR PARAMETERS TEMPLATES LIST"
                        );
                    }
                );
            return true;
        };

    /**
     * Apply a template.
     * @param sTemplateId
     * @returns {boolean}
     */
    ProcessorParametersTemplateController.prototype.applyProcessorParametersTemplate =
        function (sTemplateId) {
            if (utilsIsStrNullOrEmpty(sTemplateId) === true) {
                return false;
            }

            var oController = this;

            this.m_oProcessorParametersTemplateService
                .getProcessorParametersTemplate(sTemplateId)
                .then(
                    function (data) {
                        if (utilsIsObjectNullOrUndefined(data.data) === false) {
                            // FETCHING THE TEMPLATE
                            let oTemplate = data.data;
                            oController.m_oClose(oTemplate.jsonParameters, 200); // close, but give 200ms for bootstrap to animate
                        } else {
                            utilsVexDialogAlertTop(
                                "GURU MEDITATION<br>ERROR FETCHING THE TEMPLATE"
                            );
                        }
                    },
                    function (error) {
                        utilsVexDialogAlertTop(
                            "GURU MEDITATION<br>ERROR FETCHING THE TEMPLATE"
                        );
                    }
                );

            return false;
        };

    ProcessorParametersTemplateController.prototype.addProcessorParametersTemplate =
        function () {
            if (utilsIsObjectNullOrUndefined(this.m_oInputProcessor) === true) {
                return false;
            }

            var oController = this;
            this.m_bEditMode = true;

            try {
                var sJSONPayload = decodeURIComponent(
                    this.m_oInputProcessor.paramsSample
                );
                var oParsed = JSON.parse(sJSONPayload);
                var sPrettyPrint = JSON.stringify(oParsed, null, 2);
            } catch (oError) {
                utilsVexDialogAlertTop(
                    "GURU MEDITATION<br>ERROR PARSING THE JSON PAYLOAD"
                );
            }

            oController.m_oProcessorParametersTemplate = {
                processorId: this.m_oInputProcessor.processorId,
                jsonParameters: sPrettyPrint,
            };
        };

    ProcessorParametersTemplateController.prototype.editProcessorParametersTemplate =
        function (oTemplate) {
            if (utilsIsObjectNullOrUndefined(this.m_oInputProcessor) === true) {
                return false;
            }

            if (utilsIsObjectNullOrUndefined(oTemplate) === true) {
                return false;
            }

            var oController = this;
            this.m_bEditMode = true;

            this.m_oProcessorParametersTemplateService
                .getProcessorParametersTemplate(oTemplate.templateId)
                .then(
                    function (data) {
                        if (utilsIsObjectNullOrUndefined(data.data) === false) {
                            oController.m_oProcessorParametersTemplate =
                                data.data;

                            try {
                                var sJSONPayload = decodeURIComponent(
                                    oController.m_oProcessorParametersTemplate
                                        .jsonParameters
                                );
                                var oParsed = JSON.parse(sJSONPayload);
                                var sPrettyPrint = JSON.stringify(
                                    oParsed,
                                    null,
                                    2
                                );
                                oController.m_oProcessorParametersTemplate.jsonParameters =
                                    sPrettyPrint;

                                oController.m_bIsLoading = false;
                            } catch (oError) {
                                utilsVexDialogAlertTop(
                                    "GURU MEDITATION<br>ERROR PARSING THE JSON PAYLOAD"
                                );

                                oController.m_bIsLoading = false;
                            }
                        } else {
                            utilsVexDialogAlertTop(
                                "GURU MEDITATION<br>ERROR LOADING PROCESSOR PARAMETERS TEMPLATE DETAIL"
                            );

                            oController.m_bIsLoading = false;
                        }
                    },
                    function (error) {
                        utilsVexDialogAlertTop(
                            "GURU MEDITATION<br>ERROR LOADING PROCESSOR PARAMETERS TEMPLATE DETAIL"
                        );

                        oController.m_bIsLoading = false;
                    }
                );
        };

    ProcessorParametersTemplateController.prototype.viewProcessorParametersTemplate =
        function (oTemplate) {
            if (utilsIsObjectNullOrUndefined(this.m_oInputProcessor) === true) {
                return false;
            }

            this.editProcessorParametersTemplate(oTemplate);
            this.m_bEditMode = false;
        };

    ProcessorParametersTemplateController.prototype.deleteProcessorParametersTemplate =
        function (oTemplate) {
            if (utilsIsObjectNullOrUndefined(oTemplate) === true) {
                return false;
            }
            var oController = this;
            var oReturnFunctionValue = function (oValue) {
                if (oValue === true) {
                    oController.m_oProcessorParametersTemplateService
                        .deleteProcessorParameterTemplate(oTemplate.templateId)
                        .then(
                            function (data) {
                                oController.getProcessorParametersTemplatesList(
                                    oTemplate.processorId
                                );

                                // oController.getProcessorParametersTemplatesList(oController.m_sProcessorId);
                                oController.m_bEditMode = false;
                                oController.m_oProcessorParametersTemplate =
                                    null;
                            },
                            function (error) {
                                utilsVexDialogAlertTop(
                                    "GURU MEDITATION<br>ERROR IN DELETING THE TEMPLATE"
                                );
                            }
                        );
                }
            };

            utilsVexDialogConfirm(
                "Are you SURE you want to delete<br> " + oTemplate.name + " ?",
                oReturnFunctionValue
            );
        };

    ProcessorParametersTemplateController.prototype.save = function () {
        var oController = this;

        try {
            var sJSONPayload =
                oController.m_oProcessorParametersTemplate.jsonParameters;
            JSON.parse(sJSONPayload);
        } catch (oError) {
            utilsVexDialogAlertTop(
                "GURU MEDITATION<br>ERROR PARSING THE JSON PAYLOAD"
            );
            return;
        }

        oController.m_bIsLoading = true;

        oController.m_oProcessorParametersTemplate.jsonParameters = encodeURI(
            oController.m_oProcessorParametersTemplate.jsonParameters
        );

        if (
            utilsIsStrNullOrEmpty(
                oController.m_oProcessorParametersTemplate.templateId
            ) === false
        ) {
            oController.m_oProcessorParametersTemplateService
                .updateProcessorParameterTemplate(
                    oController.m_oProcessorParametersTemplate
                )
                .then(
                    function () {
                        var oDialog = utilsVexDialogAlertBottomRightCorner(
                            "PROCESSOR PARAMETERS TEMPLATE UPDATED"
                        );
                        utilsVexCloseDialogAfter(2000, oDialog);

                        oController.getProcessorParametersTemplatesList(
                            oController.m_sProcessorId
                        );
                        oController.m_bEditMode = false;
                        oController.viewProcessorParametersTemplate(
                            oController.m_oProcessorParametersTemplate
                        );
                    },
                    function (error) {
                        utilsVexDialogAlertTop(
                            "GURU MEDITATION<br>THERE WAS AN ERROR UPDATING PROCESSOR PARAMETERS TEMPLATE"
                        );
                        oController.m_bIsLoading = false;
                    }
                );
        } else {
            oController.m_oProcessorParametersTemplateService
                .addProcessorParameterTemplate(
                    oController.m_oProcessorParametersTemplate
                )
                .then(
                    function () {
                        var oDialog = utilsVexDialogAlertBottomRightCorner(
                            "PROCESSOR PARAMETERS TEMPLATE ADDED"
                        );
                        utilsVexCloseDialogAfter(2000, oDialog);

                        oController.getProcessorParametersTemplatesList(
                            oController.m_sProcessorId
                        );
                        oController.m_bEditMode = false;
                        oController.m_oProcessorParametersTemplate = null;

                        oController.m_bIsLoading = false;
                    },
                    function (error) {
                        utilsVexDialogAlertTop(
                            "GURU MEDITATION<br>THERE WAS AN ERROR ADDING PROCESSOR PARAMETERS TEMPLATE"
                        );
                        oController.m_bIsLoading = false;
                    }
                );
        }

        return;
    };

    ProcessorParametersTemplateController.prototype.openShareDialog =
        function (sTemplateId) {
            let oController = this;
            oController.m_oProcessorParametersTemplateService.getProcessorParametersTemplate(sTemplateId).then(function (data) {
                oController.m_oModalService
                .showModal({
                    templateUrl:
                        "dialogs/processor_parameters_share/ProcessorParamsShareDialog.html",
                    controller: "ProcessParamsShareController",
                    inputs: {
                        extras: {
                            template: data.data
                        }
                    }
                })
                .then(function (modal) {
                    modal.element.modal();
                    modal.close.then(function (result) {
                    });
                });
            })
    
            
        };
    ProcessorParametersTemplateController.prototype.formatJson = function () {
        let oController = this;
        //let asProcessorParamsArr = oController.m_oProcessorParametersTemplate.jsonParameters.replace(/[{}]/g, "").replaceAll("=", ":").split(",")

        
        let oProcessorParamsObj = JSON.parse(oController.m_oProcessorParametersTemplate.jsonParameters.replaceAll("'", '"'))
        oController.m_oProcessorParametersTemplate.jsonParameters = JSON.stringify(oProcessorParamsObj, null, 4)
              
    }

    ProcessorParametersTemplateController.$inject = [
        "$scope",
        "close",
        "extras",
        "ConstantsService",
        "ProcessorParametersTemplateService",
        "ModalService",
    ];
    return ProcessorParametersTemplateController;
})();
window.ProcessorParametersTemplateController =
    ProcessorParametersTemplateController;
