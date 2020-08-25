 /**
 * Created by a.corrado on 31/03/2017.
 */


var ProcessorController = (function() {

    function ProcessorController($scope, oClose,oExtras,oWorkspaceService,oProductService,oConstantsService,oHttp, oProcessorService, oProcessorMediaService) {

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
         * Workpsace service
         */
        this.m_oWorkspaceService = oWorkspaceService;
        /**
         * Product service
         */
        this.m_oProductService = oProductService;
        /**
         * Product Media Service
         */
        this.m_oProcessorMediaService = oProcessorMediaService;
        /**
         * Constants service
         */
        this.m_oConstantsService = oConstantsService;
        /**
         * Active Workspace
         */
        this.m_oActiveWorkspace = this.m_oConstantsService.getActiveWorkspace();
        /**
         * Http Service
         */
        this.m_oHttp =  oHttp;
        /**
         * Processors Service
         */
        this.m_oProcessorService = oProcessorService;
        /**
         * User Uploaded Zip file
         * @type {null}
         */
        this.m_oFile = null;
        /**
         * Processor Name
         * @type {string}
         */
        this.m_sName = "";
        /**
         * Processor Description
         * @type {string}
         */
        this.m_sDescription = "";
        /**
         * Processor Version
         */
        this.m_sVersion = "1";
        /**
         * JSON Input Paramters Sample
         * @type {string}
         */
        this.m_sJSONSample = "";
        /**
         * Types of available processors
         * @type {({name: string, id: string}|{name: string, id: string}|{name: string, id: string})[]}
         */
        this.m_aoProcessorTypes = [{'name':'Python 2.7','id':'ubuntu_python_snap'},{'name':'Python 3.7','id':'ubuntu_python37_snap'},{'name':'IDL 3.7.2','id':'ubuntu_idl372'}];
        /**
         * Selected Processor Type
         * @type {string}
         */
        this.m_sSelectedType = "";

        /**
         * Name of the Type  of a processor in edit mode
         * @type {string}
         */
        this.m_sTypeNameOnly = "";
        /**
         * Id of the Type  of a processor in edit mode
         * @type {string}
         */
        this.m_sTypeIdOnly = "";
        /**
         * Public flag
         * @type {boolean}
         */
        this.m_oPublic = true;
        /**
         * Flag to know if we are in Edit Mode
         * @type {boolean}
         */
        this.m_bEditMode = false;

        /**
         * Share user mail
         * @type {string}
         */
        this.m_sUserEmail = "";
        /**
         * List of user id that has access to the processor
         * @type {*[]}
         */
        this.m_aoEnableUsers=[];
        /**
         * Processor Id
         * @type {string}
         */
        this.m_sProcessorId = "";

        /**
         * Selected Tab
         * @type {string}
         */
        this.m_sSelectedTab = "Base";

        /**
         * View Model with the Processor Detailed Info.
         * Is fetched and saved with different APIs
         * @type {{processorDescription: string, updateDate: number, images: [], imgLink: string, ondemandPrice: number, link: string, score: number, processorId: string, publisher: string, buyed: boolean, processorName: string, categories: [], isMine: boolean, friendlyName: string, email: string, subscriptionPrice: number}}
         */
        this.m_oProcessorDetails = {
            processorId: "",
            processorName: "",
            processorDescription: "",
            imgLink: "",
            publisher: "",
            score: 0.0,
            friendlyName: "",
            link: "",
            email: "",
            ondemandPrice: 0.0,
            subscriptionPrice: 0.0,
            updateDate: 0,
            categories: [],
            images: [],
            isMine: true,
            buyed: false
        }

        /**
         * Application Categories
         * @type {*[]}
         */
        this.m_aoCategories = [];

        /**
         * Local Reference to the controller
         * @type {ProcessorController}
         */
        var oController = this;

        /**
         * Processor Logo File
         * @type {null}
         */
        var m_oProcessorLogo = null;

        // Close this Dialog handler
        $scope.close = function() {
            // close, but give 500ms for bootstrap to animate
            oClose(null, 300);
        };

        // Apply the user actions handler
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

        // Are we creating a new processor or editing an existing one?
        if (this.m_oInputProcessor !== null) {

            // We are in edit mode:
            this.m_bEditMode = true;

            // Copy the input data to the model
            this.m_sName = this.m_oInputProcessor.processorName;
            this.m_sDescription = this.m_oInputProcessor.processorDescription;
            this.m_sJSONSample = decodeURIComponent(this.m_oInputProcessor.paramsSample);
            this.m_sProcessorId = this.m_oInputProcessor.processorId;

            // Get the list of Enabled users for sharing
            this.getListOfEnableUsers(this.m_sProcessorId)

            // Select the right processor type
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

            // Read also the details
            this.m_oProcessorService.getMarketplaceDetail(this.m_oInputProcessor.processorName).success(function (data) {
                if(utilsIsObjectNullOrUndefined(data) === false)
                {
                    oController.m_oProcessorDetails = data;

                    oController.m_oProcessorMediaService.getCategories().success(function (data) {
                        oController.m_aoCategories = data;

                    }).error(function (error) {

                    });
                }
                else
                {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR READING APP DETAILS");
                }

            }).error(function (error) {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR READING APP DETAILS");
            });


        }
    };

     /**
      * Utility method to define if the drag and drop box can be shown or not
      * @returns {boolean}
      */
    ProcessorController.prototype.showDragAndDrop = function() {
        if (this.m_bEditMode == false) {
            return true;
        }

        if (this.m_sTypeIdOnly === "ubuntu_idl372" || this.m_sTypeIdOnly === "ubuntu_python37_snap")  {
            return true;
        }
        return false;
    };

     /**
      * Utility method to test JSON Validity
      * @param sJsonString
      * @returns {boolean|any}
      */
    ProcessorController.prototype.tryParseJSON =function(sJsonString){
         try {
             var oJsonParsedObject = JSON.parse(sJsonString);

             if (oJsonParsedObject && typeof oJsonParsedObject === "object") {
                 return oJsonParsedObject;
             }
         }
         catch (e) { }

         return false;
     };

     /**
      * Internal method to update en existing processor in edit mode
      * @param oController
      * @param oSelectedFile
      * @returns {boolean}
      */
     ProcessorController.prototype.updateProcessor = function (oController,oSelectedFile) {

         if (!this.tryParseJSON(oController.m_sJSONSample)) {
             var oDialog = utilsVexDialogAlertBottomRightCorner("PLEASE CHECK YOUR JSON<br>IN THE PARAMS SAMPLE");
             utilsVexCloseDialogAfter(4000,oDialog);
             return;
         }

         // Update User Values
         oController.m_oInputProcessor.isPublic = 1;
         if (oController.m_oPublic === false) oController.m_oInputProcessor.isPublic = 0;
         oController.m_oInputProcessor.processorName = oController.m_sName;
         // Version is fixed at 1 now and hidden from the form
         //oController.m_oInputProcessor.processorVersion = oController.m_sVersion;
         oController.m_oInputProcessor.processorDescription = oController.m_sDescription;
         oController.m_oInputProcessor.paramsSample = encodeURI(oController.m_sJSONSample);

         // Update processor data
         oController.m_oProcessorService.updateProcessor(oController.m_oActiveWorkspace.workspaceId, oController.m_oInputProcessor.processorId, oController.m_oInputProcessor).success(function (data) {

             oController.m_oProcessorService.updateProcessorDetails(oController.m_oInputProcessor.processorId, oController.m_oProcessorDetails).success(function (data) {
                 var oDialog = utilsVexDialogAlertBottomRightCorner("PROCESSOR DATA UPDATED");
                 utilsVexCloseDialogAfter(4000,oDialog);
             }).error(function (error) {
                 utilsVexDialogAlertTop("GURU MEDITATION<br>THERE WAS AN ERROR UPDATING PROCESSOR DATA");
             });

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

     /**
      * Utility method to Create a new processor
      * @param oController
      * @param oSelectedFile
      * @returns {boolean}
      */
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
      * Get the list of users that has this processor shared
      * @param sProcessorId
      * @returns {boolean}
      */
     ProcessorController.prototype.getListOfEnableUsers = function(sProcessorId){

         if(utilsIsStrNullOrEmpty(sProcessorId) === true)
         {
             return false;
         }
         var oController = this;
         this.m_oProcessorService.getUsersBySharedProcessor(sProcessorId)
             .success(function (data) {
                 if(utilsIsObjectNullOrUndefined(data) === false)
                 {
                     oController.m_aoEnableUsers = data;
                 }
                 else
                 {
                     utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR SHARING PROCESSOR");
                 }

             }).error(function (error) {
             utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR SHARING PROCESSOR");
         });
         return true;
     };

     /**
      * Add a user to the sharing list
      * @param sProcessorId
      * @param sEmail
      * @returns {boolean}
      */
     ProcessorController.prototype.shareProcessorByUserEmail = function(sProcessorId, sEmail) {

         if( (utilsIsObjectNullOrUndefined(sProcessorId) === true) || (utilsIsStrNullOrEmpty(sEmail) === true))
         {
             return false;
         }

         utilsRemoveSpaces(sEmail);

         var sFinalProcessorId = sProcessorId;

         var oController = this;
         this.m_oProcessorService.putShareProcessor(sProcessorId,sEmail)
             .success(function (data) {
                 if(utilsIsObjectNullOrUndefined(data) === false && data.boolValue === true)
                 {
                     // SHARING SAVED
                 }else
                 {
                     utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR SHARING PROCESSOR");
                 }
                 oController.getListOfEnableUsers(sFinalProcessorId);

             }).error(function (error) {
             utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR SHARING PROCESSOR");
         });

         this.m_sUserEmail="";
         return true;
     };

     /**
      * Removes a user from the sharing list
      * @param sProcessorId
      * @param sEmail
      * @returns {boolean}
      */
     ProcessorController.prototype.removeUserSharing = function(sProcessorId,sEmail){

         if( (utilsIsObjectNullOrUndefined(sProcessorId) === true) || (utilsIsStrNullOrEmpty(sEmail) === true))
         {
             return false;
         }

         utilsRemoveSpaces(sEmail);
         var oController = this;
         var sFinalProcessorId = sProcessorId;

         this.m_oProcessorService.deleteUserSharedProcessor(sProcessorId,sEmail)
             .success(function (data) {
                 if(utilsIsObjectNullOrUndefined(data) === false && data.boolValue === true)
                 {
                     // SHARING SAVED
                 }
                 else
                 {
                     utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR SHARING PROCESSOR");
                 }
                 oController.getListOfEnableUsers(sFinalProcessorId);

             }).error(function (error) {
             utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR SHARING PROCESSOR");
         });

         this.m_sUserEmail="";
         return true;
     };

     /**
      * Force the redeploy of the processor on the server
      * @param sProcessorId
      * @returns {boolean}
      */
     ProcessorController.prototype.forceProcessorRefresh = function(sProcessorId) {

         if (utilsIsObjectNullOrUndefined(sProcessorId) === true)
         {
             return false;
         }

         // TODO: ADD CONFIRMATION DIALOG
         this.m_oProcessorService.redeployProcessor(sProcessorId)
             .success(function (data) {
                 var oDialog = utilsVexDialogAlertBottomRightCorner("PROCESSORS IMAGE<br>REFRESH SCHEDULED");
                 utilsVexCloseDialogAfter(5000,oDialog);
             }).error(function (error) {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR REFRESHING PROCESSOR");
         });

         return true;
     };

     /**
      * Handle a click on a category
      * @param sCategoryId
      */
     ProcessorController.prototype.categoryClicked = function (sCategoryId) {
         if (this.m_oProcessorDetails.categories.includes(sCategoryId)) {
             this.m_oProcessorDetails.categories = this.m_oProcessorDetails.categories.filter(function(e) { return e !== sCategoryId })
         }
         else {
             this.m_oProcessorDetails.categories.push(sCategoryId);
         }
     }

     /**
      * Utility method to decide if a category checkbox is checked or not
      * @param sCategoryId
      * @returns {boolean}
      */
     ProcessorController.prototype.isCategoryChecked = function (sCategoryId) {
         if (this.m_oProcessorDetails.categories.includes(sCategoryId)) {
            return true;
         }
         else {
             return false;
         }
     }


     ProcessorController.$inject = [
        '$scope',
        'close',
        'extras',
        'WorkspaceService',
        'ProductService',
        'ConstantsService',
        '$http',
        'ProcessorService',
        'ProcessorMediaService'
    ];
    return ProcessorController;
})();
