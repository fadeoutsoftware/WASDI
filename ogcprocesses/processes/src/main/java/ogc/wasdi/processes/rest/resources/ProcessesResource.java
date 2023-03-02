package ogc.wasdi.processes.rest.resources;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.core.type.TypeReference;

import ogc.wasdi.processes.OgcProcesses;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.Node;
import wasdi.shared.business.OgcProcessesTask;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.Processor;
import wasdi.shared.business.ProcessorTypes;
import wasdi.shared.business.ProcessorUI;
import wasdi.shared.business.User;
import wasdi.shared.business.Workspace;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.NodeRepository;
import wasdi.shared.data.OgcProcessesTaskRepository;
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
import wasdi.shared.viewmodels.HttpCallResponse;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.ogcprocesses.ApiException;
import wasdi.shared.viewmodels.ogcprocesses.Bbox;
import wasdi.shared.viewmodels.ogcprocesses.InputDescription;
import wasdi.shared.viewmodels.ogcprocesses.JobControlOptions;
import wasdi.shared.viewmodels.ogcprocesses.Link;
import wasdi.shared.viewmodels.ogcprocesses.OutputDescription;
import wasdi.shared.viewmodels.ogcprocesses.ProcessList;
import wasdi.shared.viewmodels.ogcprocesses.ProcessSummary;
import wasdi.shared.viewmodels.ogcprocesses.Results;
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
import wasdi.shared.viewmodels.ogcprocesses.schemas.ImageSchema;
import wasdi.shared.viewmodels.ogcprocesses.schemas.NumericSchema;
import wasdi.shared.viewmodels.ogcprocesses.schemas.OneOfSchema;
import wasdi.shared.viewmodels.ogcprocesses.schemas.StringArraySchema;
import wasdi.shared.viewmodels.ogcprocesses.schemas.StringInListSchema;
import wasdi.shared.viewmodels.ogcprocesses.schemas.StringSchema;
import wasdi.shared.viewmodels.processworkspace.ProcessWorkspaceViewModel;

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
    		WasdiLog.debugLog("ProcessesResource.getProcesses");
    		
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
    		
    		// Here we keep copy of all links
    		ArrayList<Link> aoAllLinks = new ArrayList<>();
    		
    		// Self link
    		Link oSelfLink = new Link();
    		oSelfLink.setHref(OgcProcesses.s_sBaseAddress+"processes");
    		oSelfLink.setRel("self");
    		oSelfLink.setType(WasdiConfig.Current.ogcProcessesApi.defaultLinksType);
    		
    		oProcessList.getLinks().add(oSelfLink);
    		aoAllLinks.add(oSelfLink);
    		
    		// Alternate html link
    		Link oHtmlLink = new Link();
    		oHtmlLink.setHref(OgcProcesses.s_sBaseAddress+"processes");
    		oHtmlLink.setRel("alternate");
    		oHtmlLink.setType("text/html");
    		
    		oProcessList.getLinks().add(oHtmlLink);
    		aoAllLinks.add(oHtmlLink);

			
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
					oSummary.setId(oProcessor.getName());
					
					if (Utils.isNullOrEmpty(oProcessor.getFriendlyName())) {
						oSummary.setTitle(oProcessor.getName());
					}
					else {
						oSummary.setTitle(oProcessor.getFriendlyName());
					}
					
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
		    		oProcessorLink.setType(WasdiConfig.Current.ogcProcessesApi.defaultLinksType);
		    		oProcessorLink.setTitle("Process " + oProcessor.getName() + "  Description");
		    		
		    		oSummary.getLinks().add(oProcessorLink);
		    		aoAllLinks.add(oProcessorLink);
					
		    		// Add the processor to our return list
					oProcessList.getProcesses().add(oSummary);
				}
				
				// Increment the number of available apps
				iAvailableApps++;
			}
			
			WasdiLog.debugLog("ProcessesResource.getProcesses: found " + iAvailableApps + " Apps");
			
			// Did we finish all the pages?
			if (iAvailableApps>oiOffset+oiLimit) {
				
				WasdiLog.debugLog("ProcessesResource.getProcesses: adding next and prev links");
				
				int iNextOffset = oiOffset+oiLimit;
				
	    		// Next link
	    		Link oNextLink = new Link();
	    		oNextLink.setHref(OgcProcesses.s_sBaseAddress+"processes?offset="+ iNextOffset +"&limit="+oiLimit.toString());
	    		oNextLink.setRel("next");
	    		oNextLink.setType(WasdiConfig.Current.ogcProcessesApi.defaultLinksType);
	    		
	    		oProcessList.getLinks().add(oNextLink);
	    		aoAllLinks.add(oNextLink);
	    		
	    		// Prev link
	    		Link oPrevLink = new Link();
	    		oPrevLink.setHref(OgcProcesses.s_sBaseAddress+"processes?offset="+ oiOffset.toString() +"&limit="+oiLimit.toString());
	    		oPrevLink.setRel("prev");
	    		oPrevLink.setType(WasdiConfig.Current.ogcProcessesApi.defaultLinksType);
	    		
	    		oProcessList.getLinks().add(oPrevLink);
	    		aoAllLinks.add(oPrevLink);
			}
    		
			
    		ResponseBuilder oResponse = Response.status(Status.OK).entity(oProcessList);
    		oResponse = OgcProcesses.addLinkHeaders(oResponse, aoAllLinks);
    		return oResponse.build();
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
	@GET
    @Path("/{processID}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProcessDescription(@HeaderParam("Authorization") String sAuthorization, @HeaderParam("x-session-token") String sSessionId, @PathParam("processID") String sProcessID) {
    	try {
    		if (sProcessID==null) sProcessID = "";
    		WasdiLog.debugLog("ProcessesResource.getProcessDescription processID="+sProcessID);
    		
			// Check User 
			User oUser = OgcProcesses.getUserFromSession(sSessionId, sAuthorization);

			if (oUser==null) {
				WasdiLog.debugLog("ProcessesResource.getProcessDescription: invalid session");
	    		return Response.status(Status.UNAUTHORIZED).entity(ApiException.getUnauthorized()).header("WWW-Authenticate", "Basic").build();
			}
			
			// Read the processor entity
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessor = oProcessorRepository.getProcessorByName(sProcessID);
			
			boolean bFound = false;
			
			if (oProcessor!=null) {
				// Check if the processor is available in WASDI
				bFound = PermissionsUtils.canUserAccessProcessor(oUser.getUserId(), oProcessor.getProcessorId());				
			}
									
			if (!bFound) {
				WasdiLog.debugLog("ProcessesResource.getProcessDescription: processor invalid or inaccessible");
				
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
    		oProcessViewModel.setId(oProcessor.getName());
    		
    		// Title
			if (Utils.isNullOrEmpty(oProcessor.getFriendlyName())) {
				oProcessViewModel.setTitle(oProcessor.getName());
			}
			else {
				oProcessViewModel.setTitle(oProcessor.getFriendlyName());
			}
			
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
    		
    		// Self link
    		Link oSelfLink = new Link();
    		oSelfLink.setHref(OgcProcesses.s_sBaseAddress+"processes/"+sProcessID);
    		oSelfLink.setRel("self");
    		oSelfLink.setType(WasdiConfig.Current.ogcProcessesApi.defaultLinksType);
    		
    		oProcessViewModel.getLinks().add(oSelfLink);
    		
    		// Alternate html link
    		Link oHtmlLink = new Link();
    		oHtmlLink.setHref(OgcProcesses.s_sBaseAddress+"processes/"+sProcessID);
    		oHtmlLink.setRel("alternate");
    		oHtmlLink.setType("text/html");
    		
    		oProcessViewModel.getLinks().add(oHtmlLink);

    		
    		// Check if we have an UI: this can drive a better Input Description			
			Map<String,Object> aoAppUi = getAppUI(oProcessor.getProcessorId());
			boolean bGenerateInputsFromSample = true;
			
			if (aoAppUi!= null) {
				WasdiLog.debugLog("ProcessesResource.getProcessDescription: Processor has an UI");
				
				try {					
					// Check the render as strings flag
					Boolean bRenderAsStrings = false;
					
					if (aoAppUi.containsKey("renderAsStrings")) {
						bRenderAsStrings = (Boolean) aoAppUi.get("renderAsStrings");
					}
					
					// Now we get the full list of controls					
					List<Map<String, Object>> aoAllControls = getAllControls(aoAppUi);
					
					// If we found something
					if (aoAllControls.size()>0) {
						// For each control
						for (Map<String, Object> oControl : aoAllControls) {
							// Get the relative Input Description
							InputDescription oInputDescription = getInputDescriptionFromWasdiControl(oControl, bRenderAsStrings);
							
							if (oInputDescription != null) {
								// Add it to the return view model
								String sKey = (String) oControl.get("param");
								oProcessViewModel.getInputs().put(sKey, oInputDescription);
							}
						}
						
						// Do we have controls at the end?
						if (oProcessViewModel.getInputs().size()>0) {
							// Yes! So we rely on this and we do not need to extract from the paramters sample
							bGenerateInputsFromSample = false;
						}
					}
				}
				catch (Exception oUiEx) {
					WasdiLog.errorLog("ProcessesResource.getProcessDescription: exception generating inputs from Ui " + oUiEx.toString());
				}				
			}
			else {
				WasdiLog.debugLog("ProcessesResource.getProcessDescription: UI not available");
			}
			
			// Did we managed to extract the inputs from the UI?
			if (bGenerateInputsFromSample) {
				WasdiLog.debugLog("ProcessesResource.getProcessDescription: Generate params from the sample");
				
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
			
			if (WasdiConfig.Current.ogcProcessesApi.validationModeOn) {
				if (!Utils.isNullOrEmpty(WasdiConfig.Current.ogcProcessesApi.validationEchoProcessId)) {
					if (sProcessID.equals(WasdiConfig.Current.ogcProcessesApi.validationEchoProcessId)) {
						WasdiLog.debugLog("ProcessesResource.getProcessDescription: VALIDATION MODE: adding Mixed I/O");
						
						// Adding Mixed Mode for validation. 
						OneOfSchema oOneOf = new OneOfSchema();
						oOneOf.oneOf.add(ImageSchema.getJpg());
						oOneOf.oneOf.add(ImageSchema.getTiff());
						
						InputDescription oInputDescription = new InputDescription();
						oInputDescription.setSchema(oOneOf);
						oInputDescription.setMinOccurs(1);
						oInputDescription.setMaxOccurs(1);
						oInputDescription.setMetadata(null);
						oInputDescription.setKeywords(null);
						oInputDescription.setTitle("Mixed Input");
						
						oProcessViewModel.getInputs().put("mixed_input", oInputDescription);
						
						OutputDescription oOutputDescription = new OutputDescription();
						oOutputDescription.setTitle("Mixed Output");
						oOutputDescription.setSchema(oOneOf);
						oOutputDescription.setMetadata(null);
						oOutputDescription.setKeywords(null);
						
						oProcessViewModel.getOutputs().put("mixed_output", oOutputDescription);
					}					
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
			oFilesOutputDescription.setSchema(new StringArraySchema());
			
			oProcessViewModel.getOutputs().put("payload", oPayloadOutputDescription);
			oProcessViewModel.getOutputs().put("workspaceId", oWorkspaceIdOutputDescription);
			oProcessViewModel.getOutputs().put("files", oFilesOutputDescription);
			
			WasdiLog.debugLog("ProcessesResource.getProcessDescription: OGC View Model Done");

    		ResponseBuilder oResponse = Response.status(Status.OK).entity(oProcessViewModel);
    		oResponse = OgcProcesses.addLinkHeaders(oResponse, oProcessViewModel.getLinks());
    		return oResponse.build();
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
     * @param aoExecute
     * @return
     */
    @POST
    @Path("/{processID}/execution")
    @Produces(MediaType.APPLICATION_JSON)
    public Response executeApplication(@HeaderParam("Prefer") String sPreferHeader, @HeaderParam("Authorization") String sAuthorization, @HeaderParam("x-session-token") String sSessionId, @PathParam("processID") String sProcessID, Map<String, Object> aoExecute) {
    	try {
    		if (sProcessID==null) sProcessID = "";
    		WasdiLog.debugLog("ProcessesResource.executeApplication processID="+sProcessID);
    		
			// Check User 
			User oUser = OgcProcesses.getUserFromSession(sSessionId, sAuthorization);

			if (oUser==null) {
				WasdiLog.debugLog("ProcessesResource.executeApplication: invalid session");
	    		return Response.status(Status.UNAUTHORIZED).entity(ApiException.getUnauthorized()).header("WWW-Authenticate", "Basic").build();
			}
			
			// Check the Body Inputs
			if (aoExecute==null || !aoExecute.containsKey("inputs")) {
				WasdiLog.debugLog("ProcessesResource.executeApplication: invalid body");
				// We have an error, return the client error
				ApiException oApiException = new ApiException();
				oApiException.setTitle("error in parameters");
				oApiException.setType("http://www.opengis.net/def/exceptions/ogcapi-processes-1/1.0/no-such-process");
				
				if (Utils.isNullOrEmpty(sProcessID)) sProcessID = "";
				
				String sDetail = "Processor with id \"" + sProcessID + "\" error in parameters";
				oApiException.setDetail(sDetail);
				oApiException.setStatus(400);
				
	    		return Response.status(Status.BAD_REQUEST).entity(oApiException).header("WWW-Authenticate", "Basic").build();				
			}
			
			// Read the processor entity
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessor = oProcessorRepository.getProcessorByName(sProcessID);			
			
			// Check if the processor is available in WASDI
			boolean bFound = PermissionsUtils.canUserAccessProcessor(oUser.getUserId(), oProcessor.getProcessorId());
						
			if (!bFound) {
				WasdiLog.debugLog("ProcessesResource.executeApplication: processor not found or unavailable");
				// No, return the not found Exception
				ApiException oApiException = new ApiException();
				oApiException.setTitle("no-such-process");
				oApiException.setType("http://www.opengis.net/def/exceptions/ogcapi-processes-1/1.0/no-such-process");
				
				if (Utils.isNullOrEmpty(sProcessID)) sProcessID = "";
				
				String sDetail = "Processor with id \"" + sProcessID + "\" not found";
				oApiException.setDetail(sDetail);
				oApiException.setStatus(404);
				return Response.status(Status.NOT_FOUND).entity(oApiException).build();				
			}
			
			if (Utils.isNullOrEmpty(sSessionId)) {
				// We need to valorize the sessionId
				sSessionId = OgcProcesses.updateSessionId(sSessionId, sAuthorization);
			}
			
			WasdiLog.debugLog("ProcessesResource.executeApplication: updated sessionId: " + sSessionId);
			
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
			
			WasdiLog.debugLog("ProcessesResource.executeApplication: created Workspace " + oWorkspace.getWorkspaceId());
			
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
			oParameter.setOGCProcess(true);
			
			// We need to convert the OGC inputs to the WASDI format
			@SuppressWarnings("unchecked")
			Map<String, Object> aoInputs = (Map<String, Object>) aoExecute.get("inputs");
			
			// Start with a default one
			String sJson = JsonUtils.stringify(aoInputs);
			
			// Check if we have the app ui
			Map<String, Object> oAppUi = getAppUI(oProcessor.getProcessorId());
			
			if (oAppUi!=null) {
				WasdiLog.debugLog("ProcessesResource.executeApplication: UI Available");
				
				// Yes we have it: and we render all as strings ?
				Boolean bRenderAsStrings= false;
				if (oAppUi.containsKey("renderAsStrings")) {
					bRenderAsStrings = (Boolean) oAppUi.get("renderAsStrings");
				}
					
				try {
		    		// Convert our inputs
					List<Map<String,Object>> aoControls = getAllControls(oAppUi);
					
					Map<String, Object> oWASDIInput = new HashMap<>();
					
					for (Map<String, Object> oControl : aoControls) {
						
						boolean bErrorOnValue = false;
						
						// And the key/name of the parameter
						String sKey = (String) oControl.get("param");
						
						// Is this parameter available?
						if (aoInputs.containsKey(sKey)) {
							// Yes try parse and add it to WASDI Inputs
							bErrorOnValue = convertOGCInputToWasdiInput(oWASDIInput, aoInputs, oControl, bRenderAsStrings);
						}
						else {
							// No it is not: we need to check if it was required
							if (oControl.containsKey("required")) {
								// Search the required flag
								Boolean bRequired = (Boolean) oControl.get("required");
								
								if (bRequired) {
									
									// If it was required maybe it has a default value?
									if (oControl.containsKey("default")) {
										Object oDefault = oControl.get("default");
										oWASDIInput.put(sKey, oDefault);
									}
									else {
										// It is required, and we do not have neither an input neither a default value
										bErrorOnValue = true;	
									}
								}
							}
						}
						
						if (bErrorOnValue) {
							// We have an error, return the client error
							ApiException oApiException = new ApiException();
							oApiException.setTitle("error in parameters");
							oApiException.setType("http://www.opengis.net/def/exceptions/ogcapi-processes-1/1.0/no-such-process");
							
							if (Utils.isNullOrEmpty(sProcessID)) sProcessID = "";
							
							String sDetail = "Processor with id \"" + sProcessID + "\" error in parameter " + sKey;
							oApiException.setDetail(sDetail);
							oApiException.setStatus(400);
							return Response.status(Status.BAD_REQUEST).entity(oApiException).build();				

						}
					}
					
					// Replace the json
					sJson = JsonUtils.stringify(oWASDIInput);
				}
				catch (Exception oInEx) {
					WasdiLog.errorLog("ProcessesResource.executeApplication: error converting inputs, will use the default " + oInEx.toString());
				}
			}
			else {
				WasdiLog.debugLog("ProcessesResource.executeApplication: UI Not available");
			}
			
			oParameter.setJson(sJson);
			
			// Serialize the parameters
			String sPayload = SerializationUtils.serializeObjectToStringXML(oParameter);
			
			// We call WASDI to execute: there the platform will make the routing to the right node
			String sUrl = WasdiConfig.Current.baseUrl;
			if (sUrl.endsWith("/") == false) sUrl += "/";
			sUrl += "processing/run?operation=" + sOperationType + "&name=" + URLEncoder.encode(oProcessor.getName(), java.nio.charset.StandardCharsets.UTF_8.toString());
			
			HttpCallResponse oHttpCallResponse = HttpUtils.httpPost(sUrl, sPayload, HttpUtils.getStandardHeaders(sSessionId)); 
			// call the API to really execute the processor 
			String sResult = oHttpCallResponse.getResponseBody();
			
			WasdiLog.debugLog("ProcessesResource.executeApplication: execute request done");
			
        	// Get back the primitive result: it does not work right to go in the catch 
            PrimitiveResult oPrimitiveResult = MongoRepository.s_oMapper.readValue(sResult,PrimitiveResult.class);
            
            // Synch/Asynch mode: asynch by WASDI Default
            boolean bAsync = true;
            boolean bAddPreferenceAppliedHeader = false;
            
            // Do we have a Prefer Header?
            if (!Utils.isNullOrEmpty(sPreferHeader)) {
            	WasdiLog.infoLog("ProcessesResource.executeApplication: Preference Header: " + sPreferHeader);
            	
            	// If there is the Prefer Header, we will have to add a similar header also to the response
            	bAddPreferenceAppliedHeader = true;
            	
            	if (sPreferHeader.equals("wait")) {
            		// Unless the user really wants a synch one!
            		bAsync = false;
            		WasdiLog.debugLog("ProcessesResource.executeApplication: setting synch mode on");
            	}
            }
            
            ResponseBuilder oResponse = null;
            
            if (!oPrimitiveResult.getBoolValue()) {
    			WasdiLog.debugLog("ProcessesResource.executeApplication: unable to start the app, create error status info");
//    			Date oCreationDate = new Date(); 
//
//        		StatusInfo oStatusInfo = new StatusInfo();
//        		
//        		oStatusInfo.setCreated(oCreationDate);
//        		oStatusInfo.setFinished(oCreationDate);
//        		oStatusInfo.setProgress(0);
//        		oStatusInfo.setType(TypeEnum.PROCESS);
//        		oStatusInfo.setStatus(StatusCode.FAILED);
//        		oStatusInfo.setProcessID(sProcessID);
//        		oStatusInfo.setUpdated(oCreationDate);
//        		oStatusInfo.setMessage(oProcessor.getName()+" " + " could not be scheduled.");
//        		
//        		
//        		
//        		oResponse = Response.status(Status.CREATED).entity(oStatusInfo);    
    			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ApiException.getInternalServerError("There was an exception executing the processes")).build();    			
            }
            else {
                String sProcessWorkspaceId = oPrimitiveResult.getStringValue();
                
                WasdiLog.debugLog("ProcessesResource.executeApplication: got process workspace id " + sProcessWorkspaceId);
                
                if (bAsync) {
                	
        			// We need the creation date
        			Date oCreationDate = new Date(); 

            		StatusInfo oStatusInfo = new StatusInfo();
            			
        			WasdiLog.debugLog("ProcessesResource.executeApplication: asynch start done,  create success status info");
        			
            		oStatusInfo.setCreated(oCreationDate);
            		
            		oStatusInfo.setJobID(sProcessWorkspaceId);
            		oStatusInfo.setProgress(0);
            		oStatusInfo.setType(TypeEnum.PROCESS);
            		oStatusInfo.setStatus(StatusCode.ACCEPTED);
            		oStatusInfo.setProcessID(sProcessID);
            		oStatusInfo.setUpdated(oCreationDate);
            		oStatusInfo.setMessage(oProcessor.getName()+" " + " has been scheduled with id " + sProcessWorkspaceId);
            		
            		// Create the ogc processes task
                    OgcProcessesTask oOgcProcessesTask = new OgcProcessesTask();
                    oOgcProcessesTask.setProcessWorkspaceId(sProcessWorkspaceId);
                    oOgcProcessesTask.setWorkspaceId(oWorkspace.getWorkspaceId());
                    oOgcProcessesTask.setUserId(oUser.getUserId());
                    // Insert it in the db 
                    OgcProcessesTaskRepository oOgcProcessesTaskRepository = new OgcProcessesTaskRepository();
                    oOgcProcessesTaskRepository.insertOgcProcessesTask(oOgcProcessesTask);

            		
            		// Self link
            		Link oSelfLink = new Link();
            		oSelfLink.setHref(OgcProcesses.s_sBaseAddress+"processes/"+sProcessID+"/execution");
            		oSelfLink.setRel("self");
            		oSelfLink.setType(WasdiConfig.Current.ogcProcessesApi.defaultLinksType);
            		
            		oStatusInfo.getLinks().add(oSelfLink);
            		
            		// Alternate html link
            		Link oHtmlLink = new Link();
            		oHtmlLink.setHref(OgcProcesses.s_sBaseAddress+"processes/"+sProcessID+"/execution");
            		oHtmlLink.setRel("alternate");
            		oHtmlLink.setType("text/html");
            		
            		oStatusInfo.getLinks().add(oHtmlLink);    		
            		
            		oResponse = Response.status(Status.CREATED).entity(oStatusInfo);
            		oResponse = OgcProcesses.addLinkHeaders(oResponse, oStatusInfo.getLinks());
            		
                }
                else {
                    // Do we have to return Raw or Document results?
                    boolean bRawResults = true;
                    
                    if (aoExecute.containsKey("response")) {
                    	String sResponse = aoExecute.get("response").toString();
                    	if (sResponse.equals("document")) {
                    		bRawResults = false;
                    	}
                    }
                    
                    // Get the Workspace base Url
        			String sBaseUrl = WasdiConfig.Current.baseUrl;
        			
        			NodeRepository oNodeRepository = new NodeRepository();
        			Node oNode = oNodeRepository.getNodeByCode(oWorkspace.getNodeCode());
        			
        			if (oNode!=null) sBaseUrl = oNode.getNodeBaseAddress();
        			if (sBaseUrl.endsWith("/") == false) sBaseUrl += "/";
        			
        			WasdiLog.debugLog("ProcessesResource.executeApplication: synch start done,  wait for app to finish");
        			
        			// Wait WASDI App
        			waitApplication(sProcessWorkspaceId, sBaseUrl, sSessionId);
        			
        			WasdiLog.debugLog("ProcessesResource.executeApplication: synch start done,  app finished get result");
                    
                    // Get the result
        			ProcessWorkspaceViewModel oProcVM = OgcProcesses.readProcessWorkspaceFromNode(sProcessWorkspaceId, oNode, sSessionId);
        			
        			// Assume is ok, otherwise we "agree" with the exception handling
        			if (bRawResults) {
        				WasdiLog.debugLog("ProcessesResource.executeApplication: synch start done, return raw results");
        				oResponse = Response.status(Status.OK).entity(oProcVM.getPayload());
        			}
        			else {
        				WasdiLog.debugLog("ProcessesResource.executeApplication: synch start done, return docs results");
        				Results oResults = OgcProcesses.getResultsFromProcessWorkspace(oProcVM, oNode, sSessionId);
        				oResponse = Response.status(Status.OK).entity(oResults);
        			}
                }
                
                // Add the location link
        		String sLocationLink = OgcProcesses.s_sBaseAddress + "/jobs/" + sProcessWorkspaceId;
        		oResponse = oResponse.header("Location", sLocationLink);                
            }
    		
    		if (bAddPreferenceAppliedHeader) {
    			String sPreferenceApplied = "respond-async";
    			if (!bAsync) sPreferenceApplied = "wait";
    			
    			oResponse = oResponse.header("Preference-Applied", sPreferenceApplied);
    		}
    				
    		return oResponse.build();
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
					oCreatedSchema = new StringInListSchema();
					
					if (oControl.containsKey("values")) {
						List<String> asValues = (List<String>) oControl.get("values");
						
						for (String sValue : asValues) {
							((StringInListSchema)oCreatedSchema).getEnum().add(sValue);
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
    		
    		HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(sUrl, HttpUtils.getStandardHeaders(sSessionId)); 
    		String sResponse = oHttpCallResponse.getResponseBody();
    		
    		ArrayList<LinkedHashMap<String, Object>> oGetClass = new ArrayList<>();
    		
    		List<LinkedHashMap<String, Object>> aoCandidates = (List<LinkedHashMap<String, Object>>) MongoRepository.s_oMapper.readValue(sResponse,oGetClass.getClass());
    		
    		if (aoCandidates!=null) {
    			if (aoCandidates.size()>0) {
    				sNodeCode = (String) aoCandidates.get(0).get("nodeCode");
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
			WasdiLog.errorLog("ProcessesResource.createWorkspaceForAppliction: exception creating the workspace " + oEx.toString());
		}
    	return null;
		    	
    }
    
    /**
     * Extracts a list of controls from an App UI.
     * Both app ui and controls are deserialized in a generic Map
     * @param aoAppUi Application User Interface
     * @return List of controls
     */
    @SuppressWarnings("unchecked")
	protected List<Map<String, Object>> getAllControls(Map<String, Object> aoAppUi) {
    	ArrayList<Map<String, Object>> aoAllControls = new ArrayList<Map<String, Object>>();
    	
    	try {
			// Now we loop all the tabs to have the full list of controls
			Object oTabs = aoAppUi.get("tabs");					
			
			// Get the list of tabs
			ArrayList<Map<String, Object>> aoTabs = (ArrayList<Map<String, Object>>) oTabs;
			
			for (int iTabs = 0; iTabs<aoTabs.size(); iTabs++) {
				Map<String, Object> oTab = aoTabs.get(iTabs);
				
				// Get the list of controls
				ArrayList<Map<String, Object>> aoControls = (ArrayList<Map<String, Object>>) oTab.get("controls");
				
				for (int iControls = 0; iControls<aoControls.size(); iControls++) {
					// Add to the list
					Map<String, Object> oControl = aoControls.get(iControls);
					aoAllControls.add(oControl);
				}
			}
    		
    	}
    	catch (Exception oEx) {
			WasdiLog.errorLog("ProcessesResource.getAllControls: exception extracting controls list from app ui " + oEx.toString());
		}
    	
    	return aoAllControls;
    }
    
    /**
     * Retrive the app UI of the specific application
     * @param sProcessorId
     * @return
     */
    protected Map<String,Object> getAppUI(String sProcessorId) {
    	try {
    		// Check if we have an UI: this can drive a better Input Description
			ProcessorUIRepository oProcessorUIRepository = new ProcessorUIRepository();			
			ProcessorUI oUI = oProcessorUIRepository.getProcessorUI(sProcessorId);
			
			if (oUI != null) {
				// Try to read the ui of the app
				String sJsonUI = "";
				sJsonUI = URLDecoder.decode(oUI.getUi(), StandardCharsets.UTF_8.toString());
				
				// Convert to a Java Object
				Map<String,Object> aoAppUi = JsonUtils.jsonToMapOfObjects(sJsonUI);
				return aoAppUi;
			}
    	}
    	catch (Exception oEx) {
			WasdiLog.errorLog("ProcessesResource.getAppUI: exception getting app ui " + oEx.toString());
		}
    	
    	return null;
    }
    
    
    /**
     * Converts an OGC API Input View Model in a WASDI Parameter and add it to the representing Map
     * @param oWASDIInput Map represeting the WASDI inputs
     * @param aoInputs OGC Input
     * @param oControl WASDI Parameter User Interface Definition
     * @param bRenderAsStrings Flag to detemine if this params are all strings or objects
     * @return True if ok and the parameter is added to oWASDIInput, false in case of any problem
     */
    protected boolean convertOGCInputToWasdiInput(Map<String,Object> oWASDIInput, Map<String,Object> aoInputs, Map<String, Object> oControl, boolean bRenderAsStrings) {
    	
    	boolean bErrorOnValue = false;
    	
    	String sKey = "";
    	
		try {
			// Get the type
			String sWasdiControlType = (String) oControl.get("type");
			// And the key/name of the parameter
			sKey = (String) oControl.get("param");

			if (sWasdiControlType.equals("textbox") || sWasdiControlType.equals("hidden") || sWasdiControlType.equals("productscombo")) {
				// These are simple strings
				oWASDIInput.put(sKey, aoInputs.get(sKey));
			}
			else if (sWasdiControlType.equals("date")) {
				//Date should be in format YYYY-MM-DDTHH:MM:SS
				String sDate = (String) aoInputs.get(sKey);
				
				// Split time
				sDate = sDate.split("T")[0];
				// Split "-"
				String [] asParts = sDate.split("-");
				
				// Assume we have a problem: we will reset only if all ok
				bErrorOnValue = true;
				
				// Check not null
				if (asParts != null) {
					// We need 3 parts
					if (asParts.length == 3) {
						// that must be YYYY MM DD
						if (asParts[0].length()==4 && asParts[1].length()==2 && asParts[2].length()==2 ) {
							// Ok good for WASDI
							oWASDIInput.put(sKey, sDate);
							// Reset the flag
							bErrorOnValue = false;
						}
					}
				}
			}
			else if (sWasdiControlType.equals("dropdown") || sWasdiControlType.equals("listbox")) {
				// This is a string that must match a set of allowed values
				String sValue = (String) aoInputs.get(sKey);
				// Get the values from the ui
				@SuppressWarnings("unchecked")
				List<String> asValues = (List<String>) oControl.get("values");
				
				if (asValues.contains(sValue)) {
					// Of good
					oWASDIInput.put(sKey, sValue);
				}
				else {
					WasdiLog.infoLog("ProcessesResource.convertOGCInputToWasdiInput: the parameter " + sKey + " has a not allowed string");
					bErrorOnValue = true;
				}
			}
			else if (sWasdiControlType.equals("slider")) {
				// This is an integer
				Integer iValue = (Integer) aoInputs.get(sKey);
				
				// If we have min check
				if (oControl.containsKey("min")) {
					if (iValue< (Integer) oControl.get("min")) {
						bErrorOnValue = true;
					}
				}
				
				// If we have max check
				if (oControl.containsKey("max")) {
					if (iValue > (Integer) oControl.get("max")) {
						bErrorOnValue = true;
					} 
				}
				
				if (!bErrorOnValue) {
					// Ok add it
					if (bRenderAsStrings) {
						oWASDIInput.put(sKey, aoInputs.get(sKey).toString());
					}
					else {
						oWASDIInput.put(sKey, aoInputs.get(sKey));
					}
				}
			}
			else if (sWasdiControlType.equals("numeric")) {
				// This is a double
				Double iValue = (Double) aoInputs.get(sKey);
				
				// If we have min check
				if (oControl.containsKey("min")) {
					if (iValue< (Double) oControl.get("min")) {
						bErrorOnValue = true;
					}
				}
				
				// If we have max check
				if (oControl.containsKey("max")) {
					if (iValue > (Double) oControl.get("max")) {
						bErrorOnValue = true;
					} 
				}
				
				if (!bErrorOnValue) {
					// Ok add it
					if (bRenderAsStrings) {
						oWASDIInput.put(sKey, aoInputs.get(sKey).toString());
					}
					else {
						oWASDIInput.put(sKey, aoInputs.get(sKey));
					}
				}
			}
			else if (sWasdiControlType.equals("boolean")) {
				// Just cast: in case of problem we will finish in the catch
				Boolean bValue = (Boolean) aoInputs.get(sKey);
				
				// Ok add it
				if (bRenderAsStrings) {
					if (bValue) {
						oWASDIInput.put(sKey, "1");
					}
					else {
						oWASDIInput.put(sKey, "0");
					}
				}
				else {
					oWASDIInput.put(sKey, bValue);
				}									
			}
			else if (sWasdiControlType.equals("bbox")) {
				// The bounding box is provided as four or six numbers, depending on whether the
				// coordinate reference system includes a vertical axis (height or depth):
				//* Lower left corner, coordinate axis 1
				//* Lower left corner, coordinate axis 2
				//* Minimum value, coordinate axis 3 (optional)
				//* Upper right corner, coordinate axis 1
				//* Upper right corner, coordinate axis 2
				//* Maximum value, coordinate axis 3 (optional)
				//If the value consists of four numbers, the coordinate reference system is
				//WGS 84 longitude/latitude (http://www.opengis.net/def/crs/OGC/1.3/CRS84)
				//unless a different coordinate reference system is specified in the parameter `bbox-crs`.
				//
				// In WASDI it is supported:
				// "N,W,S,E" 
				// or
				// { "northEast": {"lat:","lng"}, "southWest": {"lat:","lng"} }
				
				double dNorth = 0.0;
				double dWest = 0.0;
				double dSouth = 0.0;
				double dEast = 0.0;				
				
				try {
					Bbox oBbox = MongoRepository.s_oMapper.convertValue(aoInputs.get(sKey), Bbox.class);


					if (oBbox.getBbox().size()==4) {										
						dNorth = oBbox.getBbox().get(3).doubleValue();
						dWest = oBbox.getBbox().get(0).doubleValue();
						dSouth = oBbox.getBbox().get(1).doubleValue();
						dEast = oBbox.getBbox().get(2).doubleValue();
					}
					else if (oBbox.getBbox().size()==6) {
						dNorth = oBbox.getBbox().get(4).doubleValue();
						dWest = oBbox.getBbox().get(0).doubleValue();
						dSouth = oBbox.getBbox().get(1).doubleValue();
						dEast = oBbox.getBbox().get(3).doubleValue();
					}
					else {
						bErrorOnValue = true;
					}					
				}
				catch (Exception oBboxEx) {
					WasdiLog.errorLog("ProcessesResource.convertOGCInputToWasdiInput: exception parsing the bbox, try with a WASDI String " + oBboxEx.toString());
					
					String sBbox = aoInputs.get(sKey).toString();
					String [] asParts = sBbox.split(",");
					dNorth = Double.parseDouble(asParts[0]);
					dWest = Double.parseDouble(asParts[1]);
					dSouth = Double.parseDouble(asParts[2]);
					dEast = Double.parseDouble(asParts[3]);
				}
				
				if (!bErrorOnValue) {
					if (bRenderAsStrings) {
						String sWasdiBbox = "" + dNorth + "," + dWest+ "," + dSouth + "," + dEast;
						oWASDIInput.put(sKey, sWasdiBbox);
					}
					else {
						Map<String, Object> oNEWasdiBbox = new HashMap<>();
						Map<String, Object> oSWWasdiBbox = new HashMap<>();
						
						oNEWasdiBbox.put("lat", dNorth);
						oNEWasdiBbox.put("lng", dEast);
						
						oSWWasdiBbox.put("lat", dSouth);
						oSWWasdiBbox.put("lng", dWest);
						
						Map<String, Object> oWasdiBbox = new HashMap<>();
						oWasdiBbox.put("northEast", oNEWasdiBbox);
						oWasdiBbox.put("southWest", oSWWasdiBbox);
						
						oWASDIInput.put(sKey, oWasdiBbox);
					}							
				}
			}									
		}
		catch (Exception oInEx) {
			WasdiLog.errorLog("ProcessesResource.convertOGCInputToWasdiInput: the parsing of parameter " + sKey + " created an exception " + oInEx.toString());
			bErrorOnValue = true;
		} 
		
		return bErrorOnValue;
    }
    
    /**
     * Wait a WASDI Process Workspace to finish
     * @param sProcWsId Process Workspace Id
     * @param sBaseUrl Workspace base Url
     * @return The output status of the application
     */
    protected String waitApplication(String sProcWsId, String sBaseUrl, String sSessionId) {
    	// Protect our self in a try-catch block
    	try {
    		
    		if (Utils.isNullOrEmpty(sProcWsId)) {
    			WasdiLog.errorLog("ProcessesResource.waitApplication: Proccess Workspace Id is null");
    			return ProcessStatus.ERROR.name();
    		}
    		
    		int iCumulatedWait = 0;
    		
    		WasdiLog.infoLog("ProcessesResource.waitApplication: start polling for " + sProcWsId);
    		
    		// Start the cycle: it is safe with the timeout
    		while (true) {
    			
    			// Read process status
    			String sStatus = getProcessStatus(sProcWsId, sBaseUrl, sSessionId);
    			
    			if (sStatus.equals(ProcessStatus.DONE.name()) || sStatus.equals(ProcessStatus.ERROR.name()) || sStatus.equals(ProcessStatus.STOPPED.name())) {
    				WasdiLog.infoLog("ProcessesResource.waitApplication: found final state " + sStatus);
    				return sStatus;
    			}
    			
    			// Check Timeout
    			if (iCumulatedWait>WasdiConfig.Current.ogcProcessesApi.waitProcessMaxTimeout) {
    				WasdiLog.infoLog("ProcessesResource.waitApplication: timeout, exit");
    				break;
    			}
    			
    			try {
    				Thread.sleep(WasdiConfig.Current.ogcProcessesApi.waitProcessSleepTimeout);	
    			}
    			catch (InterruptedException oInt) {
    				WasdiLog.errorLog("sleep exception");
    			}
    			
    			// Increment the sleep count
    			iCumulatedWait += WasdiConfig.Current.ogcProcessesApi.waitProcessSleepTimeout;
    		}
    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("ProcessesResource.waitApplication: exception " + oEx.toString());
		}
    	
    	return ProcessStatus.ERROR.name();
    }
    
	/**
	 * Get WASDI Process Status 
	 * @param sProcessId Process Id
	 * @return  Process Status as a String: CREATED,  RUNNING,  STOPPED,  DONE,  ERROR, WAITING, READY
	 */
	protected String getProcessStatus(String sProcessId, String sBaseUrl, String sSessionId) {
		WasdiLog.debugLog("ProcessesResource.getProcessStatus( " + sProcessId + " )");
		if(null==sProcessId || sProcessId.isEmpty()) {
			WasdiLog.debugLog("ProcessesResource.getProcessStatus: process id null or empty, aborting");
		}
		try {
			// Create the API call
			String sUrl = sBaseUrl + "process/byid?procws="+sProcessId;

			// Add session token
			Map<String, String> asHeaders = new HashMap<String, String>();
			asHeaders.put("x-session-token", sSessionId);
			
			// Call the API
			HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(sUrl, asHeaders); 
			String sResponse = oHttpCallResponse.getResponseBody();
			
			// Get result and extract status
			Map<String, Object> aoJSONMap = MongoRepository.s_oMapper.readValue(sResponse, new TypeReference<Map<String,Object>>(){});

			String sStatus = aoJSONMap.get("status").toString();
			if(isThisAValidStatus(sStatus)) {
				return sStatus;
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessesResource.getProcessStatus: " + oEx.toString());
		}	  
		return "";
	}
	
	/**
	 * Check if the input string is a Valid Status
	 * @param sStatus
	 * @return true if the status is valid, false otherwise
	 */
	private boolean isThisAValidStatus(String sStatus) {
		return(null!=sStatus &&(
				sStatus.equals(ProcessStatus.CREATED.name()) ||
				sStatus.equals(ProcessStatus.RUNNING.name()) ||
				sStatus.equals(ProcessStatus.DONE.name()) ||
				sStatus.equals(ProcessStatus.STOPPED.name()) ||
				sStatus.equals(ProcessStatus.ERROR.name()) ||
				sStatus.equals(ProcessStatus.WAITING.name()) ||
				sStatus.equals(ProcessStatus.READY.name())
				));
	}
}
