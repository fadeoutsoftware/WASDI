package ogc.wasdi.processes.rest.resources;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import ogc.wasdi.processes.OgcProcesses;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.Processor;
import wasdi.shared.business.ProcessorTypes;
import wasdi.shared.business.ProcessorUI;
import wasdi.shared.business.User;
import wasdi.shared.business.Workspace;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.data.ProcessorUIRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.ogcprocesses.ApiException;
import wasdi.shared.viewmodels.ogcprocesses.Execute;
import wasdi.shared.viewmodels.ogcprocesses.InputDescription;
import wasdi.shared.viewmodels.ogcprocesses.JobControlOptions;
import wasdi.shared.viewmodels.ogcprocesses.Link;
import wasdi.shared.viewmodels.ogcprocesses.OutputDescription;
import wasdi.shared.viewmodels.ogcprocesses.ProcessList;
import wasdi.shared.viewmodels.ogcprocesses.ProcessSummary;
import wasdi.shared.viewmodels.ogcprocesses.Schema;
import wasdi.shared.viewmodels.ogcprocesses.StatusCode;
import wasdi.shared.viewmodels.ogcprocesses.StatusInfo;
import wasdi.shared.viewmodels.ogcprocesses.StatusInfo.TypeEnum;
import wasdi.shared.viewmodels.ogcprocesses.TransmissionMode;
import wasdi.shared.viewmodels.ogcprocesses.schemas.ArraySchema;
import wasdi.shared.viewmodels.ogcprocesses.schemas.BboxSchema;
import wasdi.shared.viewmodels.ogcprocesses.schemas.BooleanSchema;
import wasdi.shared.viewmodels.ogcprocesses.schemas.DateSchema;
import wasdi.shared.viewmodels.ogcprocesses.schemas.DoubleSchema;
import wasdi.shared.viewmodels.ogcprocesses.schemas.NumericSchema;
import wasdi.shared.viewmodels.ogcprocesses.schemas.StringListSchema;
import wasdi.shared.viewmodels.ogcprocesses.schemas.StringSchema;
import wasdi.shared.viewmodels.processworkspace.NodeScoreByProcessWorkspaceViewModel;

@Path("processes")
public class ProcessesResource {
	

	/**
	 * Get the list of available processes.
	 * The method reads from the wasdi db all the applications that are available for the logged user and 
	 * return the list in the range (offset+limit) requested by the user.
	 * 
	 * @param sAuthorization Standard Http Authorization Header
	 * @param sSessionId WASDI specific x-session-token header
	 * @param oiLimit Limit of the number of elements to return. Can be null. 20 by default.
	 * @param oiOffset Offset of the number of elements to return. Can be null. 0 by defualt.
	 * @return
	 */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProcesses(@HeaderParam("Authorization") String sAuthorization, @HeaderParam("x-session-token") String sSessionId, @QueryParam("limit") Integer oiLimit, @QueryParam("offset") Integer oiOffset) {
    	try {
    		
			// Check User 
			User oUser = OgcProcesses.getUserFromSession(sSessionId, sAuthorization);

			if (oUser==null) {
				WasdiLog.debugLog("ProcessesResource.getProcesses: invalid session");
	    		return Response.status(Status.UNAUTHORIZED).entity(ApiException.getUnauthorized()).header("WWW-Authenticate", "Basic").build();
			}
			
			// Initialize the pagination
			if (oiLimit==null) oiLimit = 20;
			if (oiOffset==null) oiOffset = 0;
			
    		
			// Create an instance of the return View Model
    		ProcessList oProcessList = new ProcessList();
    		
    		// Self link
    		Link oSelfLink = new Link();
    		oSelfLink.setHref(OgcProcesses.s_sBaseAddress+"processes");
    		oSelfLink.setRel("self");
    		oSelfLink.setType(WasdiConfig.Current.ogcpProcessesApi.defaultLinksType);
    		
    		oProcessList.getLinks().add(oSelfLink);
			
    		// Take all the processors
    		ProcessorRepository oProcessorRepository = new ProcessorRepository();
			
			// Initialize ascending direction
			List<Processor> aoDeployed = oProcessorRepository.getDeployedProcessors("friendlyName", 1);
			
			int iAvailableApps = 0;
			
			for (int i=0; i<aoDeployed.size(); i++) {
				
				Processor oProcessor = aoDeployed.get(i);
				
				// See if this is a processor the user can access to
				if (!PermissionsUtils.canUserAccessProcessor(oUser.getUserId(), oProcessor.getProcessorId())) continue;
				
				// Run until the actual offset
				if (iAvailableApps<oiOffset) {
					iAvailableApps++;
					continue; 
				}
				
				// If we are in the range offset+limit add the processor to the return list
				if (oProcessList.getProcesses().size()<oiOffset+oiLimit) {
					
					// Create the view model
					ProcessSummary oSummary = new ProcessSummary();
					
					// Copy the attributes
					oSummary.setId(oProcessor.getProcessorId());
					oSummary.setTitle(oProcessor.getName());
					oSummary.setDescription(oProcessor.getDescription());
					oSummary.setVersion(oProcessor.getVersion());
					oSummary.getJobControlOptions().add(JobControlOptions.ASYNC_EXECUTE);
					oSummary.getOutputTransmission().add(TransmissionMode.REFERENCE);
					
					for (String sCategory : oProcessor.getCategories()) {
						oSummary.getKeywords().add(sCategory);
					}
					
					// Add a link to the specific description
		    		Link oProcessorLink = new Link();
		    		oProcessorLink.setHref(OgcProcesses.s_sBaseAddress+"processes/"+oSummary.getId());
		    		oProcessorLink.setRel("self");
		    		oProcessorLink.setType(WasdiConfig.Current.ogcpProcessesApi.defaultLinksType);
		    		oProcessorLink.setTitle("Process " + oProcessor.getName() + "  Description");
		    		
		    		oSummary.getLinks().add(oProcessorLink);
					
		    		// Add the processor to our return list
					oProcessList.getProcesses().add(oSummary);
				}
				
				// Increment the number of available apps
				iAvailableApps++;
			}
			
			// Did we finish all the pages?
			if (iAvailableApps>oiOffset+oiLimit) {
				
				int iNextOffset = oiOffset+oiLimit;
				
	    		// Next link
	    		Link oNextLink = new Link();
	    		oNextLink.setHref(OgcProcesses.s_sBaseAddress+"processes?offset="+ iNextOffset +"&limit="+oiLimit.toString());
	    		oNextLink.setRel("next");
	    		oNextLink.setType(WasdiConfig.Current.ogcpProcessesApi.defaultLinksType);
	    		
	    		oProcessList.getLinks().add(oNextLink);
	    		
	    		// Prev link
	    		Link oPrevLink = new Link();
	    		oPrevLink.setHref(OgcProcesses.s_sBaseAddress+"processes?offset="+ oiOffset.toString() +"&limit="+oiLimit.toString());
	    		oPrevLink.setRel("prev");
	    		oPrevLink.setType(WasdiConfig.Current.ogcpProcessesApi.defaultLinksType);
	    		
	    		oProcessList.getLinks().add(oPrevLink);	    		
			}
    		
    		return Response.status(Status.OK).entity(oProcessList).build();
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("ProcessesResource.getProcesses: exception " + oEx.toString());
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ApiException.getInternalServerError("There was an exception getting the list of processes")).build();
		}
    }
    

    /**
     * Get the description of a specific process
     * 
	 * @param sAuthorization Standard Http Authorization Header
	 * @param sSessionId WASDI specific x-session-token header
     * @param sProcessID
     * @return
     */
    @SuppressWarnings("unchecked")
	@GET
    @Path("/{processID}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProcessDescription(@HeaderParam("Authorization") String sAuthorization, @HeaderParam("x-session-token") String sSessionId, @PathParam("processID") String sProcessID) {
    	try {
    		
			// Check User 
			User oUser = OgcProcesses.getUserFromSession(sSessionId, sAuthorization);

			if (oUser==null) {
				WasdiLog.debugLog("ProcessesResource.getProcesses: invalid session");
	    		return Response.status(Status.UNAUTHORIZED).entity(ApiException.getUnauthorized()).header("WWW-Authenticate", "Basic").build();
			}
			
			// Read the processor entity
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessor = oProcessorRepository.getProcessor(sProcessID);
			
			// Chech if the processor is available in WASDI
			boolean bFound = PermissionsUtils.canUserAccessProcessor(oUser.getUserId(), sProcessID);
						
			if (!bFound) {
				ApiException oApiException = new ApiException();
				oApiException.setTitle("no-such-process");
				oApiException.setType("http://www.opengis.net/def/exceptions/ogcapi-processes-1/1.0/no-such-process");
				
				if (Utils.isNullOrEmpty(sProcessID)) sProcessID = "";
				
				String sDetail = "Processor with id \"" + sProcessID + "\" not found";
				oApiException.setDetail(sDetail);
				oApiException.setStatus(404);
				return Response.status(Status.NOT_FOUND).entity(oApiException).build();				
			}
    		
			// Convert the WASDI entity in the OGC View Model
    		wasdi.shared.viewmodels.ogcprocesses.Process oProcessViewModel = new wasdi.shared.viewmodels.ogcprocesses.Process();
    		
    		// Identifier
    		oProcessViewModel.setId(oProcessor.getProcessorId());
    		// Title
    		oProcessViewModel.setTitle(oProcessor.getName());
    		// Narrative Description
    		oProcessViewModel.setDescription(oProcessor.getDescription());
    		// Keywords
    		for (String sCategory : oProcessor.getCategories()) {
    			oProcessViewModel.getKeywords().add(sCategory);
			}
    		// Version
    		oProcessViewModel.setVersion(oProcessor.getVersion());
    		// Execution mode
    		oProcessViewModel.getJobControlOptions().add(JobControlOptions.ASYNC_EXECUTE);
    		// Results mode
    		oProcessViewModel.getOutputTransmission().add(TransmissionMode.REFERENCE);
    		
    		// Execution Link
    		Link oExeLink = new Link();
    		oExeLink.setHref(OgcProcesses.s_sBaseAddress+"processes/"+sProcessID+"/execution");
    		oExeLink.setRel("http://www.opengis.net/def/rel/ogc/1.0/execute");
    		oExeLink.setTitle("Execute endpoint");
    		
    		// Add the link to the view model
    		oProcessViewModel.getLinks().add(oExeLink);
    		
    		// Check if we have an UI: this can drive a better Input Description
			ProcessorUIRepository oProcessorUIRepository = new ProcessorUIRepository();			
			ProcessorUI oUI = oProcessorUIRepository.getProcessorUI(oProcessor.getProcessorId());
			
			boolean bGenerateInputsFromSample = true;
			
			if (oUI != null) {
				try {
					String sJsonUI = "";
					sJsonUI = URLDecoder.decode(oUI.getUi(), StandardCharsets.UTF_8.toString());
					
					Map<String,Object> aoAppUi = JsonUtils.jsonToMapOfObjects(sJsonUI);
					
					Boolean bRenderAsStrings = false;
					
					if (aoAppUi.containsKey("renderAsStrings")) {
						bRenderAsStrings = (Boolean) aoAppUi.get("renderAsStrings");
					}
					
					Object oTabs = aoAppUi.get("tabs");					
					ArrayList<Map<String, Object>> aoTabs = (ArrayList<Map<String, Object>>) oTabs;
					
					ArrayList<Map<String, Object>> aoAllControls = new ArrayList<Map<String, Object>>(); 
					
					for (int iTabs = 0; iTabs<aoTabs.size(); iTabs++) {
						Map<String, Object> oTab = aoTabs.get(iTabs);
						
						ArrayList<Map<String, Object>> aoControls = (ArrayList<Map<String, Object>>) oTab.get("controls");
						
						for (int iControls = 0; iControls<aoControls.size(); iControls++) {
							Map<String, Object> oControl = aoControls.get(iControls);
							aoAllControls.add(oControl);
						}
					}
					
					if (aoAllControls.size()>0) {
						
						for (Map<String, Object> oControl : aoAllControls) {
							InputDescription oInputDescription = getInputDescriptionFromWasdiControl(oControl, bRenderAsStrings);
							
							if (oInputDescription != null) {
								String sKey = (String) oControl.get("param");
								oProcessViewModel.getInputs().put(sKey, oInputDescription);
							}
						}
						
						if (oProcessViewModel.getInputs().size()>0) bGenerateInputsFromSample = false;

					}
				}
				catch (Exception oUiEx) {
					WasdiLog.errorLog("ProcessesResource.getProcessDescription: exception generating inputs from Ui " + oUiEx.toString());
				}				
				
			}
			
			if (bGenerateInputsFromSample) {
				// No: we need to work with the Json Params Sample
	    		String sParamSampleJson = oProcessor.getParameterSample();
	    		sParamSampleJson = URLDecoder.decode(sParamSampleJson, StandardCharsets.UTF_8.toString());		
	    		
	    		// We convert Json to a Java Map Object
				Map<String,Object> aoAppParamsSample = JsonUtils.jsonToMapOfObjects(sParamSampleJson);
				
				// Each Key is an input
				for (String sKey : aoAppParamsSample.keySet()) {
										
					// What type is this object?
					String sSchemaType = JsonUtils.getTypeDescriptionString(aoAppParamsSample.get(sKey));
					// Create an instance of the schema
					Schema oSchema = Schema.getSchemaFromType(sSchemaType);
					
					if (oSchema == null) {
						WasdiLog.debugLog("ProcessesResource.getProcessDescription: type not found for input " + sKey + ", jumping to next");
						continue;
					}
					
					// Create the input description
					InputDescription oInputDescription = new InputDescription();
					// Well here we do not have title and description, all will be the same
					oInputDescription.setTitle(sKey);
					oInputDescription.setDescription(sKey);
					oInputDescription.setSchema(oSchema);
					
					// If it is an array
					if (oSchema instanceof ArraySchema) {
						// We try to declare also the inner type of the array
						try {
							// Try to convert to a list
							List<Object> aoList = Arrays.asList(aoAppParamsSample.get(sKey));
							if (aoList!=null) {
								if (aoList.size()>0) {
									// Get one object
									Object oNewObject = aoList.get(0);
									// Obtain the inner type
									String sInnerType = JsonUtils.getTypeDescriptionString(oNewObject);
									// Set the new Schema object
									((ArraySchema)oSchema).items = Schema.getSchemaFromType(sInnerType);
								}
							}							
						}
						catch (Exception oListEx) {
							WasdiLog.errorLog("ProcessesResource.getProcessDescription: exception searching the type of the array " + oListEx.toString());
						}
					}
					
					oProcessViewModel.getInputs().put(sKey, oInputDescription);
				}				
			}
			
			// Add the generic payload as output
			OutputDescription oPayloadOutputDescription = new OutputDescription();
			oPayloadOutputDescription.setDescription("Payload generated by the application");
			oPayloadOutputDescription.setTitle("payload");
			oPayloadOutputDescription.setSchema(new StringSchema());
    		
			// Add the workspace id as output
			OutputDescription oWorkspaceIdOutputDescription = new OutputDescription();
			oWorkspaceIdOutputDescription.setDescription("WASDI Workspace where the files generated by the application are saved");
			oWorkspaceIdOutputDescription.setTitle("workspaceId");
			oWorkspaceIdOutputDescription.setSchema(new StringSchema());
			
			// Add a list of the files in the workspace in output
			OutputDescription oFilesOutputDescription = new OutputDescription();
			oFilesOutputDescription.setDescription("WASDI Workspace where the files generated by the application are saved");
			oFilesOutputDescription.setTitle("files");
			oFilesOutputDescription.setSchema(new StringListSchema());
			
			oProcessViewModel.getOutputs().put("payload", oPayloadOutputDescription);
			oProcessViewModel.getOutputs().put("workspaceId", oWorkspaceIdOutputDescription);
			oProcessViewModel.getOutputs().put("files", oFilesOutputDescription);

    		return Response.status(Status.OK).entity(oProcessViewModel).build();
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("ProcessesResource.getProcessDescription: exception " + oEx.toString());
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ApiException.getInternalServerError("There was an exception getting the detail of a processes")).build();
		}
    }
    

    /**
     * Start the execution of a process
     * 
	 * @param sAuthorization Standard Http Authorization Header
	 * @param sSessionId WASDI specific x-session-token header
     * @param sProcessID
     * @param oExecute
     * @return
     */
    @POST
    @Path("/{processID}/execution")
    @Produces(MediaType.APPLICATION_JSON)
    public Response executeApplication(@HeaderParam("Authorization") String sAuthorization, @HeaderParam("x-session-token") String sSessionId, @PathParam("processID") String sProcessID, Execute oExecute) {
    	try {
			// Check User 
			User oUser = OgcProcesses.getUserFromSession(sSessionId, sAuthorization);

			if (oUser==null) {
				WasdiLog.debugLog("ProcessesResource.getProcesses: invalid session");
	    		return Response.status(Status.UNAUTHORIZED).entity(ApiException.getUnauthorized()).header("WWW-Authenticate", "Basic").build();
			}
			
			
			// Chech if the processor is available in WASDI
			boolean bFound = PermissionsUtils.canUserAccessProcessor(oUser.getUserId(), sProcessID);
						
			if (!bFound) {
				ApiException oApiException = new ApiException();
				oApiException.setTitle("no-such-process");
				oApiException.setType("http://www.opengis.net/def/exceptions/ogcapi-processes-1/1.0/no-such-process");
				
				if (Utils.isNullOrEmpty(sProcessID)) sProcessID = "";
				
				String sDetail = "Processor with id \"" + sProcessID + "\" not found";
				oApiException.setDetail(sDetail);
				oApiException.setStatus(404);
				return Response.status(Status.NOT_FOUND).entity(oApiException).build();				
			}
			
			// Read the processor entity
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessor = oProcessorRepository.getProcessor(sProcessID);
			
			// Set the operation type: can be run processor or idl or matlab
			String sOperationType = LauncherOperations.RUNPROCESSOR.name();
			if (oProcessor.getType().equals(ProcessorTypes.IDL)) sOperationType = LauncherOperations.RUNIDL.name();
			else if (oProcessor.getType().equals(ProcessorTypes.OCTAVE)) sOperationType = LauncherOperations.RUNMATLAB.name();
			
			// Create a Workspace for this user and this app
			Workspace oWorkspace = createWorkspaceForAppliction(sSessionId, oUser, oProcessor);

			if (oWorkspace==null) {
				WasdiLog.errorLog("ProcessesResource.executeApplication: it was impossible to create the workspace");
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ApiException.getInternalServerError("There was an exception creating your workspace")).build();
			}
			
			// Create  the processor parameter
			ProcessorParameter oParameter = new ProcessorParameter();
			
			oParameter.setExchange(oWorkspace.getWorkspaceId());
			oParameter.setName(oProcessor.getName());
			oParameter.setProcessObjId(Utils.getRandomName());
			oParameter.setWorkspace(oWorkspace.getWorkspaceId());
			oParameter.setWorkspaceOwnerId(oUser.getUserId());
			oParameter.setProcessorID(oProcessor.getProcessorId());
			oParameter.setProcessorType(oProcessor.getType());
			oParameter.setSessionID(sSessionId);
			oParameter.setUserId(oUser.getUserId());
			oParameter.setVersion(oProcessor.getVersion());
			
			// TODO Convert the inputs to the WASDI Input
			oParameter.setJson("");
			
			// Serialize the parameters
			String sPayload = SerializationUtils.serializeObjectToStringXML(oParameter);
			
			// We call WASDI to execute: there the platform will make the routing to the right node
			String sUrl = WasdiConfig.Current.baseUrl;
			if (sUrl.endsWith("/") == false) sUrl += "/";
			sUrl += "processing/run?operation=" + sOperationType + "&name=" + URLEncoder.encode(oProcessor.getName(), java.nio.charset.StandardCharsets.UTF_8.toString());
			
			// call the API to really execute the processor 
			String sResult = HttpUtils.httpPost(sUrl, sPayload, HttpUtils.getStandardHeaders(sSessionId));
			
        	// Get back the primitive result: it does not work right to go in the catch 
            PrimitiveResult oPrimitiveResult = MongoRepository.s_oMapper.readValue(sResult,PrimitiveResult.class);

			// We need the creation date
			Date oCreationDate = new Date(); 

    		StatusInfo oStatusInfo = new StatusInfo();
    		
    		if (oPrimitiveResult.getBoolValue()) {
        		oStatusInfo.setCreated(oCreationDate);
        		
        		oStatusInfo.setJobID(oPrimitiveResult.getStringValue());
        		oStatusInfo.setProgress(0);
        		oStatusInfo.setType(TypeEnum.PROCESS);
        		oStatusInfo.setStatus(StatusCode.ACCEPTED);
        		oStatusInfo.setProcessID(sProcessID);
        		oStatusInfo.setUpdated(oCreationDate);
        		oStatusInfo.setMessage(oProcessor.getName()+" " + " has been scheduled with id " + oPrimitiveResult.getStringValue());    			
    		}
    		else {
        		oStatusInfo.setCreated(oCreationDate);
        		oStatusInfo.setFinished(oCreationDate);
        		oStatusInfo.setProgress(0);
        		oStatusInfo.setType(TypeEnum.PROCESS);
        		oStatusInfo.setStatus(StatusCode.FAILED);
        		oStatusInfo.setProcessID(sProcessID);
        		oStatusInfo.setUpdated(oCreationDate);
        		oStatusInfo.setMessage(oProcessor.getName()+" " + " could not be scheduled.");    			
    		}
    		
    		return Response.status(Status.CREATED).entity(oStatusInfo).build();
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("ProcessesResource.executeApplication: exception " + oEx.toString());
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ApiException.getInternalServerError("There was an exception executing the processes")).build();    		
		}
    }
    
    /**
     * Converts a WASDI input control into a OGC Processes API InputDescription
     * @param oControl
     * @return OGC Processes API InputDescription
     */
    protected InputDescription getInputDescriptionFromWasdiControl(Map<String, Object> oControl, Boolean bRenderAsStrings) {
    	try {
    		// Create the View Model
    		InputDescription oInputDescription = new InputDescription();
    		
    		// Set label and tooltip
			if (oControl.containsKey("label")) oInputDescription.setTitle( (String) oControl.get("label"));
			if (oControl.containsKey("tooltip")) oInputDescription.setDescription( (String) oControl.get("tooltip"));
			
			// We still need to create the schema
			Schema oCreatedSchema = null;
			
			// All strings?
			if (bRenderAsStrings) {
				// Yes: so we just create the String
				oCreatedSchema = new StringSchema();
			}
			else {
				
				// No: Get the type			
				String sType = (String) oControl.get("type");

				// Create the specific schema for the specified type
				if (sType.equals("textbox") || sType.equals("hidden") || sType.equals("productscombo")) {
					oCreatedSchema = new StringSchema();					
				}
				else if (sType.equals("date")) {
					oCreatedSchema = new DateSchema();
				} 
				else if (sType.equals("dropdown") || sType.equals("listbox")) {
					oCreatedSchema = new StringListSchema();
					
					if (oControl.containsKey("values")) {
						List<String> asValues = (List<String>) oControl.get("values");
						
						for (String sValue : asValues) {
							((StringListSchema)oCreatedSchema).getEnum().add(sValue);
						}
					}
				} 
				else if (sType.equals("slider")) {
					oCreatedSchema = new NumericSchema();
					
					if (oControl.containsKey("min")) {
						((NumericSchema)oCreatedSchema).minimum = (Integer) oControl.get("min"); 
					}
					
					if (oControl.containsKey("max")) {
						((NumericSchema)oCreatedSchema).maximum = (Integer) oControl.get("max"); 
					}
				} 
				else if (sType.equals("numeric")) {
					oCreatedSchema = new DoubleSchema();
					
					if (oControl.containsKey("min")) {
						((DoubleSchema)oCreatedSchema).minimum = (Integer) oControl.get("min"); 
					}
					
					if (oControl.containsKey("max")) {
						((DoubleSchema)oCreatedSchema).maximum = (Integer) oControl.get("max"); 
					}
				} 
				else if (sType.equals("boolean")) {
					oCreatedSchema = new BooleanSchema();
				} 
				else if (sType.equals("bbox")) {
					oCreatedSchema = new BboxSchema();
				}
				else {
					return null;
				}				
			}
			
			// Set the created schema
			oInputDescription.setSchema(oCreatedSchema);
			
			// Set Max occurs: for WASDI is always 1
			oInputDescription.setMaxOccurs(1);
			
			// Set Min occurs: 0 by default
			oInputDescription.setMinOccurs(0);
			
			// Check if it is required
			if (oControl.containsKey("required")) {
				boolean bRequired = (boolean) oControl.get("required");
				
				if (bRequired) {
					oInputDescription.setMinOccurs(1);
					
					if (oControl.containsKey("default")) {
						Object oDefault = oControl.get("default");
						
						if (oDefault != null) {
							oInputDescription.setMinOccurs(0);
						}
					}
				}
			}
			
			return oInputDescription;
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("ProcessesResource.executeApplication: exception " + oEx.toString());
		}
    	
    	return null;
    }
    
    /**
     * Creates a workspace to execute the application.
     * Calls the WASDI API to have the node where the workspace will be assigned
     * creates the workspace unique name
     * and creates the workspace entity
     * @param sSessionId Actual Session
     * @param oUser User 
     * @param oProcessor Processor to run
     * @return The Workspace object created or null in case of problems
     */
    protected Workspace createWorkspaceForAppliction(String sSessionId, User oUser, Processor oProcessor) {
    	
    	try {
    		// We create a Workspace for this user
    		WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
    		Workspace oWorkspace = new Workspace();
    		
    		// We need the creation date
    		Date oCreationDate = new Date(); 
    		
    		String sWorkspaceName = oProcessor.getName()+"_" + oCreationDate.toString();

    		while (oWorkspaceRepository.getByUserIdAndWorkspaceName(oUser.getUserId(), sWorkspaceName) != null) {
    			sWorkspaceName = Utils.cloneWorkspaceName(sWorkspaceName);
    			WasdiLog.debugLog("ProcessesResource.createWorkspaceForAppliction: a workspace with the same name already exists. Changing the name to " + sWorkspaceName);
    		}

    		// Default values
    		oWorkspace.setCreationDate(Utils.nowInMillis());
    		oWorkspace.setLastEditDate(Utils.nowInMillis());
    		oWorkspace.setName(sWorkspaceName);
    		oWorkspace.setUserId(oUser.getUserId());
    		oWorkspace.setWorkspaceId(Utils.getRandomName());
    		
    		String sNodeCode = "";
    			
    		WasdiLog.debugLog("ProcessesResource.createWorkspaceForAppliction: search the best node");
    		
    		String sUrl = WasdiConfig.Current.baseUrl;
    		if (sUrl.endsWith("/") == false) sUrl += "/";
    		sUrl += "/process/nodesByScore";
    		
    		String sResponse = HttpUtils.httpGet(sUrl, HttpUtils.getStandardHeaders(sSessionId));
    		
    		ArrayList<NodeScoreByProcessWorkspaceViewModel> oGetClass = new ArrayList<>();
    		
    		List<NodeScoreByProcessWorkspaceViewModel> aoCandidates = (List<NodeScoreByProcessWorkspaceViewModel>) MongoRepository.s_oMapper.readValue(sResponse,oGetClass.getClass());
    		
    		if (aoCandidates!=null) {
    			if (aoCandidates.size()>0) {
    				sNodeCode = aoCandidates.get(0).getNodeCode();
    			}
    		}
    		
    		if (Utils.isNullOrEmpty(sNodeCode)) {
    			WasdiLog.debugLog("ProcessesResource.createWorkspaceForAppliction: it was impossible to associate a Node: try user default");
    			//get user's default nodeCode
    			sNodeCode = oUser.getDefaultNode();
    		}
    		
    		if (Utils.isNullOrEmpty(sNodeCode)) {
    			WasdiLog.debugLog("ProcessesResource.createWorkspaceForAppliction: it was impossible to associate a Node: use system default");
    			// default node code
    			sNodeCode = "wasdi";
    		}
    		
    		WasdiLog.debugLog("ProcessesResource.createWorkspaceForAppliction: selected node " + sNodeCode);
    		oWorkspace.setNodeCode(sNodeCode);

    		if (oWorkspaceRepository.insertWorkspace(oWorkspace)==false) {
    			WasdiLog.errorLog("ProcessesResource.createWorkspaceForAppliction: it was impossible to create the workspace");
    			return null;
    		}
    		else {
    			return oWorkspace;
    		}    		
    	}
    	catch (Exception oEx) {
			WasdiLog.errorLog("ProcessesResource.createWorkspaceForAppliction: exception creating the worksapce " + oEx.toString());
		}
    	return null;
		    	
    }

}
