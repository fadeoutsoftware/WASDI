 /**
 * Created by a.corrado on 31/03/2017.
 */


var ProcessorController = (function() {

    function ProcessorController($scope, oClose,oExtras,oWorkspaceService,oProductService,oConstantsService,oHttp, oProcessorService) {
        //MEMBERS
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oInputProcessor = this.m_oExtras.processor;
        this.m_oWorkspaceService = oWorkspaceService;
        this.m_oProductService = oProductService;
        this.m_aoWorkspaceList = [];
        this.m_aWorkspacesName = [];
        this.m_aoSelectedWorkspaces = [];
        this.m_sFileName = "";
        this.m_oConstantsService = oConstantsService;
        this.m_oActiveWorkspace = this.m_oConstantsService.getActiveWorkspace();
        this.m_oHttp =  oHttp;
        //file .zip
        this.m_oFile = null;
        this.m_sName = "";
        this.m_sDescription = "";
        this.m_sVersion = "1";
        this.m_sJSONSample = "";
        this.m_aoProcessorTypes = [{'name':'Python 2.7','id':'ubuntu_python_snap'},{'name':'Python 3.7','id':'ubuntu_python37_snap'},{'name':'IDL 3.7.2','id':'ubuntu_idl372'}];
        this.m_sSelectedType = "";
        // Used only in Edit Mode
        this.m_sTypeNameOnly = "";
        this.m_sTypeIdOnly = "";
        this.m_oPublic = true;
        this.m_oProcessorService = oProcessorService;
        this.m_bEditMode = false;

        var oController = this;
        $scope.close = function() {
            oClose(null, 300); // close, but give 500ms for bootstrap to animate
        };

        $scope.add = function() {
            if (oController.m_bEditMode == true) {
                var oFile = null;
                if (oController.m_oFile!=null) {
                    oFile = oController.m_oFile[0];
                }
                oController.updateProcessor(oController, oFile);
            }
            else {
                oController.postProcessor(oController, oController.m_oFile[0]);
            }

            oClose(null, 300); // close, but give 500ms for bootstrap to animate
        };

        if (this.m_oInputProcessor !== null) {
            this.m_bEditMode = true;
            this.m_sName = this.m_oInputProcessor.processorName;
            this.m_sDescription = this.m_oInputProcessor.processorDescription;
            this.m_sJSONSample = decodeURIComponent(this.m_oInputProcessor.paramsSample);

            var i=0;

            for (i=0; i<this.m_aoProcessorTypes.length; i++) {
                if (this.m_aoProcessorTypes[i].id === this.m_oInputProcessor.type) {
                    this.m_sTypeNameOnly = this.m_aoProcessorTypes[i].name;
                    this.m_sTypeIdOnly = this.m_aoProcessorTypes[i].id;
                    break;
                }
            }

            if (this.m_oInputProcessor.isPublic) {
                this.m_oPublic = true;
            }
            else {
                this.m_oPublic = false;
            }

        }
    };

    ProcessorController.prototype.showDragAndDrop = function() {
        if (this.m_bEditMode == false) {
            return true;
        }

        if (this.m_sTypeIdOnly === "ubuntu_idl372" || this.m_sTypeIdOnly === "ubuntu_python37_snap")  {
            return true;
        }
        return false;
    };

     ProcessorController.prototype.updateProcessor = function (oController,oSelectedFile) {

         // Update User Values
         oController.m_oInputProcessor.isPublic = 1;
         if (oController.m_oPublic === false) oController.m_oInputProcessor.isPublic = 0;
         oController.m_oInputProcessor.processorName = oController.m_sName;
         // Version is fixed at 1 now and hidden from the form
         //oController.m_oInputProcessor.processorVersion = oController.m_sVersion;
         oController.m_oInputProcessor.processorDescription = oController.m_sDescription;
         oController.m_oInputProcessor.paramsSample = encodeURI(oController.m_sJSONSample);

         // Update processor data
         this.m_oProcessorService.updateProcessor(oController.m_oActiveWorkspace.workspaceId, oController.m_oInputProcessor.processorId, oController.m_oInputProcessor).success(function (data) {
             var oDialog = utilsVexDialogAlertBottomRightCorner("PROCESSOR DATA UPDATED");
             utilsVexCloseDialogAfter(4000,oDialog);
         }).error(function (error) {
             utilsVexDialogAlertTop("GURU MEDITATION<br>THERE WAS AN ERROR UPDATING PROCESSOR DATA");
         });

         // There was also a file?
         if(!utilsIsObjectNullOrUndefined(oSelectedFile) === true)
         {
             // Update the file
             var oBody = new FormData();
             oBody.append('file', this.m_oFile[0]);

             this.m_oProcessorService.updateProcessorFiles(oController.m_oActiveWorkspace.workspaceId, oController.m_oInputProcessor.processorId, oBody).success(function (data) {
                 var oDialog = utilsVexDialogAlertBottomRightCorner("PROCESSOR FILES UPDATED");
                 utilsVexCloseDialogAfter(4000,oDialog);
             }).error(function (error) {
                 utilsVexDialogAlertTop("GURU MEDITATION<br>THERE WAS AN ERROR UPDATING PROCESSOR FILES");
             });
         }

         return true;
     }

    ProcessorController.prototype.postProcessor = function (oController,oSelectedFile)
    {
        if(utilsIsObjectNullOrUndefined(oSelectedFile) === true)
        {
            return false;
        }

        var sType = oController.m_sSelectedType.id;

        var sPublic = "1";
        if (oController.m_oPublic === false) sPublic = "0";

        var oBody = new FormData();
        oBody.append('file', this.m_oFile[0]);

        if (sType==="ubuntu_python_snap" || sType==="ubuntu_python37_snap") {
            oController.m_sName = oController.m_sName.toLowerCase();
        }

        this.m_oProcessorService.uploadProcessor(oController.m_oActiveWorkspace.workspaceId,oController.m_sName,oController.m_sVersion, oController.m_sDescription, sType, oController.m_sJSONSample,sPublic, oBody).success(function (data) {

            sMessage = ""
            if (data.boolValue == true) {
                sMessage = "PROCESSOR UPLOADED<br>IT WILL BE DEPLOYED IN A WHILE"
            }
            else {
                sMessage = "ERROR UPLOADING PROCESSOR<br>ERROR CODE: " + data.intValue;
                if (!utilsIsStrNullOrEmpty(data.stringValue)) {
                    sMessage += "<br>"+data.stringValue;
                }
            }

            var oDialog = utilsVexDialogAlertBottomRightCorner(sMessage);
            utilsVexCloseDialogAfter(5000,oDialog);
        }).error(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>THERE WAS AN ERROR DEPLOYING THE PROCESSOR");
        });

        return true;
    };

    /**
     *
     * @param sName
     * @returns {*}
     */
    ProcessorController.prototype.changeProductName = function(sName){
        if(utilsIsStrNullOrEmpty(sName) === true)
            return "";

        return sName + "_workflow";
    };

    ProcessorController.$inject = [
        '$scope',
        'close',
        'extras',
        'WorkspaceService',
        'ProductService',
        'ConstantsService',
        '$http',
        'ProcessorService'

    ];
    return ProcessorController;
})();
