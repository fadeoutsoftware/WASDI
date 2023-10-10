package net.wasdi.openeoserver.api;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import net.wasdi.openeoserver.WasdiOpenEoServer;
import net.wasdi.openeoserver.viewmodels.Asset;
import net.wasdi.openeoserver.viewmodels.BatchJob;
import net.wasdi.openeoserver.viewmodels.BatchJobResult;
import net.wasdi.openeoserver.viewmodels.BatchJobs;
import net.wasdi.openeoserver.viewmodels.DescribeJob200Response;
import net.wasdi.openeoserver.viewmodels.DescribeJob200Response.StatusEnum;
import net.wasdi.openeoserver.viewmodels.Error;
import net.wasdi.openeoserver.viewmodels.GeoJsonGeometry.TypeEnum;
import net.wasdi.openeoserver.viewmodels.GeoJsonPolygon;
import net.wasdi.openeoserver.viewmodels.ItemProperties;
import net.wasdi.openeoserver.viewmodels.Link;
import net.wasdi.openeoserver.viewmodels.StoreBatchJobRequest;
import net.wasdi.openeoserver.viewmodels.UpdateBatchJobRequest;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.Node;
import wasdi.shared.business.OpenEOJob;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.Workspace;
import wasdi.shared.business.processors.Processor;
import wasdi.shared.business.users.User;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.NodeRepository;
import wasdi.shared.data.OpenEOJobRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.StringUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.processworkspace.ProcessWorkspaceViewModel;

@Path("/jobs")


public class JobsApi  {

   public JobsApi(@Context ServletConfig servletContext) {
   }

    @javax.ws.rs.POST    
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public Response createJob(@NotNull @Valid  StoreBatchJobRequest oStoreBatchJobRequest, @HeaderParam("Authorization") String sAuthorization) {
    	
    	User oUser = WasdiOpenEoServer.getUserFromAuthenticationHeader(sAuthorization);
    	
    	if (oUser == null) {
			WasdiLog.debugLog("JobsApi.createJob: invalid credentials");
			return Response.status(Status.UNAUTHORIZED).entity(Error.getUnathorizedError()).build();    		
    	}
    	
    	try {
    		
    		// Create a name for the future Process Workspace
    		String sProcId = Utils.getRandomName();

    		// Create the Job Entity. For now not started and without workspace
    		OpenEOJob oOpenEOJob = new OpenEOJob();
    		oOpenEOJob.setJobId(sProcId);
    		oOpenEOJob.setStarted(false);
    		oOpenEOJob.setUserId(oUser.getUserId());
    		// Dump the json as we have received it
    		String sJSON = MongoRepository.s_oMapper.writeValueAsString(oStoreBatchJobRequest);
    		oOpenEOJob.setParameters(sJSON);
    		
    		// Insert the Job in the db
    		OpenEOJobRepository oOpenEOJobRepository = new OpenEOJobRepository();
    		oOpenEOJobRepository.insertOpenEOJob(oOpenEOJob);
    		
    		// Link to get the status/description of this job
    		String sLocationHeader = WasdiConfig.Current.openEO.baseAddress;
    		if (!sLocationHeader.endsWith("/")) sLocationHeader += "/";
    		sLocationHeader += "jobs/"+sProcId;    		
    		
    		// Add the requested headers
    		ResponseBuilder oResponse = Response.created(new URI("sLocationHeader"));
    		oResponse.header("OpenEO-Identifier", sProcId);
    		
    		// And we are done
    		return oResponse.build();
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("JobsApi.createJob error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("JobsApi.createJob", "InternalServerError", oEx.getMessage())).build();
		}
    }
    
    @javax.ws.rs.GET
    @Path("/{job_id}/logs")
    @Produces({ "application/json" })
    public Response debugJob(@NotNull  @Pattern(regexp="^[\\w\\-\\.~]+$") String jobId, @QueryParam("offset")  String offset, @QueryParam("limit")  @Min(1) Integer limit, @HeaderParam("Authorization") String sAuthorization) {
    	
    	User oUser = WasdiOpenEoServer.getUserFromAuthenticationHeader(sAuthorization);
    	
    	if (oUser == null) {
			WasdiLog.debugLog("JobsApi.debugJob: invalid credentials");
			return Response.status(Status.UNAUTHORIZED).entity(Error.getUnathorizedError()).build();    		
    	}
    	
    	try {
    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("JobsApi.debugJob error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("JobsApi.debugJob", "InternalServerError", oEx.getMessage())).build();
		}
    	return Response.ok().build();
    }
    
    @javax.ws.rs.DELETE
    @Path("/{job_id}")
    @Produces({ "application/json" })
    public Response deleteJob(@PathParam("job_id") @NotNull  @Pattern(regexp="^[\\w\\-\\.~]+$") String sJobId, @HeaderParam("Authorization") String sAuthorization) {
    	
    	User oUser = WasdiOpenEoServer.getUserFromAuthenticationHeader(sAuthorization);
    	
    	if (oUser == null) {
			WasdiLog.warnLog("JobsApi.deleteJob: invalid credentials");
			return Response.status(Status.UNAUTHORIZED).entity(Error.getUnathorizedError()).build();    		
    	}
    	
    	try {
    		
    		String sSessionId = sAuthorization.substring("Bearer basic//".length());
    		
    		OpenEOJobRepository oOpenEOJobRepository = new OpenEOJobRepository();
    		OpenEOJob oJob = oOpenEOJobRepository.getOpenEOJob(sJobId);
    		
    		if (oJob == null) {
        		WasdiLog.warnLog("JobsApi.deleteJob: EO Job not found ");
        		return Response.status(Status.BAD_REQUEST).entity(Error.getError("JobsApi.describeJob", "InvalidJob", "Invalid Job")).build();    			
    		}
    		
    		WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
    		Workspace oWorkspace = oWorkspaceRepository.getWorkspace(oJob.getWorkspaceId());
    		
    		if (oWorkspace == null) {
        		WasdiLog.warnLog("JobsApi.deleteJob: Workspace not found ");
        		return Response.status(Status.BAD_REQUEST).entity(Error.getError("JobsApi.describeJob", "InvalidWorkspace", "Invalid Workspace")).build();    			
    		}
    		
    		String sBaseUrl = getBaseUrl(oWorkspace);
    		
    		ProcessWorkspaceViewModel oProcessWorkspaceViewModel = getProcessWorkspace(oJob.getJobId(), sBaseUrl, sSessionId);
    		
    		if (oProcessWorkspaceViewModel.getStatus().equals("CREATED") || oProcessWorkspaceViewModel.getStatus().equals("RUNNING")||oProcessWorkspaceViewModel.getStatus().equals("READY")||oProcessWorkspaceViewModel.getStatus().equals("WAITING")) {    			
        		boolean bRet = killProcessWorkspace(sJobId,sBaseUrl,sSessionId);
        		
        		if (!bRet) {
            		WasdiLog.errorLog("JobsApi.deleteJob: kill process workspace returned false ");    		    		
            		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("JobsApi.deleteJob", "InternalServerError", "Impossible to stop the process")).build();    			
        		}
    		}
    		
    		
    		// Clean the db
    		oOpenEOJobRepository.deleteOpenEOJob(sJobId);
    		
    		// Clean the workspace
    		deleteWorkspace(oWorkspace.getWorkspaceId(), sBaseUrl, sSessionId);
    		
    		return Response.status(Status.NO_CONTENT).build();
    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("JobsApi.deleteJob error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("JobsApi.deleteJob", "InternalServerError", oEx.getMessage())).build();
		}
    }
    
    @javax.ws.rs.GET
    @Path("/{job_id}")
    @Produces({ "application/json" })
    public Response describeJob(@PathParam("job_id") @NotNull  @Pattern(regexp="^[\\w\\-\\.~]+$") String sJobId, @HeaderParam("Authorization") String sAuthorization) {
    	
    	User oUser = WasdiOpenEoServer.getUserFromAuthenticationHeader(sAuthorization);
    	
    	if (oUser == null) {
			WasdiLog.debugLog("JobsApi.describeJob: invalid credentials");
			return Response.status(Status.UNAUTHORIZED).entity(Error.getUnathorizedError()).build();    		
    	}
    	
    	try {
    		String sSessionId = sAuthorization.substring("Bearer basic//".length());
    		
    		OpenEOJobRepository oOpenEOJobRepository = new OpenEOJobRepository();
    		OpenEOJob oJob = oOpenEOJobRepository.getOpenEOJob(sJobId);
    		
    		if (oJob == null) {
        		WasdiLog.debugLog("JobsApi.describeJob: EO Job not found ");
        		return Response.status(Status.BAD_REQUEST).entity(Error.getError("JobsApi.describeJob", "InvalidJob", "Invalid Job")).build();    			
    		}
    		
    		WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
    		Workspace oWorkspace = oWorkspaceRepository.getWorkspace(oJob.getWorkspaceId());
    		
    		if (oWorkspace == null) {
        		WasdiLog.debugLog("JobsApi.describeJob: Workspace not found ");
        		return Response.status(Status.BAD_REQUEST).entity(Error.getError("JobsApi.describeJob", "InvalidWorkspace", "Invalid Workspace")).build();    			
    		}
    		
    		String sBaseUrl = getBaseUrl(oWorkspace);
    		
    		DescribeJob200Response oDescribeJob200Response = new DescribeJob200Response();
    		oDescribeJob200Response.setId(sJobId);
    		
    		ProcessWorkspaceViewModel oProcWS = getProcessWorkspace(sJobId, sBaseUrl, sSessionId);
    		
			if (oProcWS == null) {
        		WasdiLog.debugLog("JobsApi.describeJob: Process not found ");
        		return Response.status(Status.BAD_REQUEST).entity(Error.getError("JobsApi.describeJob", "InvalidProcWS", "Invalid ProcWS")).build();    			    				
			}
			
    		oDescribeJob200Response.setStatus(wasdiStateToOpenEOState(oProcWS.getStatus()));
    		oDescribeJob200Response.setProgress(new BigDecimal(oProcWS.getProgressPerc()));
    		oDescribeJob200Response.setCreated(Utils.getWasdiDate(oProcWS.getOperationDate()));
    		oDescribeJob200Response.setUpdated(Utils.getWasdiDate(oProcWS.getLastChangeDate()));
    		
    		return Response.ok().entity(oDescribeJob200Response).build();
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("JobsApi.describeJob error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("JobsApi.describeJob", "InternalServerError", oEx.getMessage())).build();
		}
    }
    
    @javax.ws.rs.GET
    @Path("/{job_id}/estimate")
    @Produces({ "application/json" })
    public Response estimateJob(@PathParam("job_id") @NotNull  @Pattern(regexp="^[\\w\\-\\.~]+$") String jobId, @HeaderParam("Authorization") String sAuthorization) {
    	
    	User oUser = WasdiOpenEoServer.getUserFromAuthenticationHeader(sAuthorization);
    	
    	if (oUser == null) {
			WasdiLog.debugLog("JobsApi.estimateJob: invalid credentials");
			return Response.status(Status.UNAUTHORIZED).entity(Error.getUnathorizedError()).build();    		
    	}
    	
    	try {
    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("JobsApi.estimateJob error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("JobsApi.estimateJob", "InternalServerError", oEx.getMessage())).build();
		}
    	
    	return Response.ok().build();
    }
    
    @javax.ws.rs.GET
    @Produces({ "application/json" })
    public Response listJobs(@QueryParam("limit")  @Min(1) Integer limit, @HeaderParam("Authorization") String sAuthorization) {
    	
    	User oUser = WasdiOpenEoServer.getUserFromAuthenticationHeader(sAuthorization);
    	
    	if (oUser == null) {
			WasdiLog.debugLog("JobsApi.listJobs: invalid credentials");
			return Response.status(Status.UNAUTHORIZED).entity(Error.getUnathorizedError()).build();    		
    	}
    	
    	try {
    		
    		String sSessionId = sAuthorization.substring("Bearer basic//".length());
    		
    		OpenEOJobRepository oOpenEOJobRepository = new OpenEOJobRepository();
    		List<OpenEOJob> aoJobs = oOpenEOJobRepository.getOpenEOJobsByUser(oUser.getUserId());
    		
    		BatchJobs oBatchJobs = new BatchJobs();
    		WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
    		
    		for (OpenEOJob oJob : aoJobs) {
    			
    			BatchJob oBatchJobViewModel = new BatchJob();
    			
    			oBatchJobViewModel.setId(oJob.getJobId());
    			
    			Workspace oWorkspace = oWorkspaceRepository.getWorkspace(oJob.getWorkspaceId());
    			String sBaseUrl = getBaseUrl(oWorkspace);
    			ProcessWorkspaceViewModel oProcessWorkspaceViewModel = getProcessWorkspace(oJob.getJobId(), sBaseUrl, sSessionId);
    			
    			StatusEnum oOtherStatus = wasdiStateToOpenEOState(oProcessWorkspaceViewModel.getStatus());
    			oBatchJobViewModel.setStatus(BatchJob.StatusEnum.fromValue(oOtherStatus.toString()));
    			oBatchJobViewModel.setProgress(new BigDecimal(oProcessWorkspaceViewModel.getProgressPerc()));
    			oBatchJobViewModel.setCreated(Utils.getWasdiDate(oProcessWorkspaceViewModel.getOperationDate()));
    			oBatchJobViewModel.setUpdated(Utils.getWasdiDate(oProcessWorkspaceViewModel.getLastChangeDate()));
    			
    			oBatchJobs.addJobsItem(oBatchJobViewModel);
			}
    		
    		return Response.ok().entity(oBatchJobs).build();
    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("JobsApi.listJobs error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("JobsApi.listJobs", "InternalServerError", oEx.getMessage())).build();
		}
    	
    	
    }
    
    @javax.ws.rs.GET
    @Path("/{job_id}/results")
    @Produces({ "application/json", "application/geo+json" })
    public Response listResults(@PathParam("job_id") @NotNull  @Pattern(regexp="^[\\w\\-\\.~]+$") String sJobId, @HeaderParam("Authorization") String sAuthorization) {
    	
    	User oUser = WasdiOpenEoServer.getUserFromAuthenticationHeader(sAuthorization);
    	
    	if (oUser == null) {
			WasdiLog.debugLog("JobsApi.listResults: invalid credentials");
			return Response.status(Status.UNAUTHORIZED).entity(Error.getUnathorizedError()).build();    		
    	}
    	
    	try {
    		
    		String sSessionId = sAuthorization.substring("Bearer basic//".length());
    		
    		OpenEOJobRepository oOpenEOJobRepository = new OpenEOJobRepository();
    		OpenEOJob oJob = oOpenEOJobRepository.getOpenEOJob(sJobId);
    		
    		if (oJob == null) {
        		WasdiLog.debugLog("JobsApi.listResults: EO Job not found ");
        		return Response.status(Status.BAD_REQUEST).entity(Error.getError("JobsApi.listResults", "InvalidJob", "Invalid Job")).build();    			
    		}
    		
    		WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
    		Workspace oWorkspace = oWorkspaceRepository.getWorkspace(oJob.getWorkspaceId());
    		
    		if (oWorkspace == null) {
        		WasdiLog.debugLog("JobsApi.listResults: Workspace not found ");
        		return Response.status(Status.BAD_REQUEST).entity(Error.getError("JobsApi.listResults", "InvalidWorkspace", "Invalid Workspace")).build();    			
    		}
    		
    		String sBaseUrl = WasdiConfig.Current.baseUrl;
    		
    		if (oWorkspace.getNodeCode().equals("wasdi") == false) {
    			NodeRepository oNodeRepository = new NodeRepository();
    			Node oNode = oNodeRepository.getNodeByCode(oWorkspace.getNodeCode());
    			
    			if (oNode == null) {
            		WasdiLog.debugLog("JobsApi.listResults: Node not found ");
            		return Response.status(Status.BAD_REQUEST).entity(Error.getError("JobsApi.listResults", "InvalidNode", "Invalid Node")).build();    			    				
    			}
    			
    			sBaseUrl = oNode.getNodeBaseAddress();
    		}
    		
    		if (!sBaseUrl.endsWith("/")) sBaseUrl+="/";
    		
    		DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
    		List<DownloadedFile> aoWorkspaceFiles = oDownloadedFilesRepository.getByWorkspace(oJob.getWorkspaceId());
    		
    		BatchJobResult oResult = new BatchJobResult();
    		
    		oResult.setStacVersion("1.0.0");
    		oResult.id(sJobId);
    		oResult.setType(net.wasdi.openeoserver.viewmodels.BatchJobResult.TypeEnum.FEATURE);
    		
    		HashMap<String, Asset> aoAssets = new HashMap<>();
    		
    		GeoJsonPolygon oGeometry = new GeoJsonPolygon();
    		
    		boolean bAddedGeometry = false;
    		
    		for (DownloadedFile oProduct : aoWorkspaceFiles) {
    			if (oProduct.getFilePath().endsWith(".tif")) {
					Asset oAsset = new Asset();
					oAsset.setTitle("Final");
					
					oAsset.setType("image/tiff; application=geotiff");
					
					String sUrl = sBaseUrl + "catalog/downloadbyname?filename=" + oProduct.getFileName() + "&workspace=" + oWorkspace.getWorkspaceId() + "&token=" + sSessionId; 
					oAsset.setHref(sUrl);
					oAsset.getRoles().add("data");
					
					aoAssets.put(oProduct.getFileName(), oAsset);
					
					String sBbox = oProduct.getBoundingBox();
					
					if (bAddedGeometry) continue;
					
					if (!Utils.isNullOrEmpty(sBbox)) {
						
						String [] asParts = sBbox.split(",");
						
						if (asParts != null) {
							oGeometry.setType(TypeEnum.POLYGON);
							List<List<BigDecimal>> aoPoints = new ArrayList<List<BigDecimal>>();
							
							int iParts = asParts.length;
							
							for (int iPoints = 0; iPoints<iParts; iPoints+=2) {
								if (iPoints+1>=iParts) break;
								
								try {
									Double oValue1 = Double.parseDouble(asParts[iPoints]);
									Double oValue2 = Double.parseDouble(asParts[iPoints+1]);
									
									List<BigDecimal> aoPointList = new ArrayList<BigDecimal>();
									aoPointList.add(new BigDecimal(oValue1));
									aoPointList.add(new BigDecimal(oValue2));
									aoPoints.add(aoPointList);
								}
								catch (Exception oParseError) {
									WasdiLog.errorLog("JobsApi.listResults: Error parsing bbox numbers ", oParseError);
								}
							}
							
							oGeometry.addCoordinatesItem(aoPoints);
							bAddedGeometry = true;
						}
						
					}
    			}
			}
    		
    		oResult.setGeometry(oGeometry);
    		
    		oResult.setAssets(aoAssets);
    		
    		List<Link> aoLinks = new ArrayList<Link>();
    		Link oLink = new Link();
    		oLink.setHref(new URI(WasdiConfig.Current.openEO.baseAddress));
    		oLink.setRel("related");
    		oLink.setType("text/html");
    		oLink.setTitle("WASDI open EO Backend");
    		aoLinks.add(oLink);
    		
    		oResult.setLinks(aoLinks);
    		    		    		
    		ProcessWorkspaceViewModel oProcWS = getProcessWorkspace(sJobId, sBaseUrl, sSessionId);
    		
			if (oProcWS == null) {
        		WasdiLog.debugLog("JobsApi.listResults: Process not found ");
        		return Response.status(Status.BAD_REQUEST).entity(Error.getError("JobsApi.listResults", "InvalidProcWS", "Invalid ProcWS")).build();    			    				
			}    		
    		
    		ItemProperties oItemProperties = new ItemProperties();
    		oItemProperties.setCreated(Utils.getWasdiDate(oProcWS.getOperationDate()));
    		oItemProperties.setStartDatetime(Utils.getWasdiDate(oProcWS.getOperationStartDate()));
    		oItemProperties.setEndDatetime(Utils.getWasdiDate(oProcWS.getOperationEndDate()));
    		
    		oResult.setProperties(oItemProperties);
    		
    		ResponseBuilder oBuilder = Response.ok(oResult);
    		oBuilder.header("OpenEO-Costs", 0);
    	
    		return oBuilder.build();
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("JobsApi.listResults error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("JobsApi.listResults", "InternalServerError", oEx.getMessage())).build();
		}
    	
    	
    }
    
    @javax.ws.rs.POST
    @Path("/{job_id}/results")
    @Produces({ "application/json" })
    public Response startJob(@PathParam("job_id") @NotNull  @Pattern(regexp="^[\\w\\-\\.~]+$") String sJobId, @HeaderParam("Authorization") String sAuthorization) {
    	
    	User oUser = WasdiOpenEoServer.getUserFromAuthenticationHeader(sAuthorization);
    	
    	if (oUser == null) {
			WasdiLog.debugLog("JobsApi.startJob: invalid credentials");
			return Response.status(Status.UNAUTHORIZED).entity(Error.getUnathorizedError()).build();    		
    	}
    	
    	try {
    		
    		OpenEOJobRepository oOpenEOJobRepository = new OpenEOJobRepository();
    		OpenEOJob oOpenEOJob = oOpenEOJobRepository.getOpenEOJob(sJobId);
    		
    		if (oOpenEOJob == null) {
        		WasdiLog.debugLog("JobsApi.startJob: EO Job not found ");
        		return Response.status(Status.BAD_REQUEST).entity(Error.getError("JobsApi.startJob", "InvalidJob", "Invalid Job")).build();    			
    		}
    		
    		if (oOpenEOJob.isStarted()) {
        		WasdiLog.debugLog("JobsApi.startJob: Job already started");
        		return Response.status(Status.BAD_REQUEST).entity(Error.getError("JobsApi.startJob", "InvalidJob", "Alreay started")).build();
    		}
    		
    		String sSessionId = sAuthorization.substring("Bearer basic//".length());
    		Workspace oWorkspace = createWorkspaceForAppliction(sSessionId,oUser, "openEO_");
    		
    		if (oWorkspace == null) {
    			WasdiLog.debugLog("JobsApi.startJob: Impossible to create the workspace ");
        		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("JobsApi.method", "InternalServerError", "Error creating ws")).build();	
    		}
    		
			WasdiLog.debugLog("JobsApi.startJob: created Workspace " + oWorkspace.getWorkspaceId());
			
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessor = oProcessorRepository.getProcessorByName(WasdiConfig.Current.openEO.openEOWasdiAppName);
			
    		if (oProcessor == null) {
    			WasdiLog.debugLog("JobsApi.startJob: Impossible to find ");
        		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("JobsApi.method", "InternalServerError", "Engine not available in this wasdi installation")).build();	
    		}			
			
			// Create  the processor parameter
			ProcessorParameter oParameter = new ProcessorParameter();
			
			oParameter.setExchange(oWorkspace.getWorkspaceId());
			oParameter.setName(oProcessor.getName());
			oParameter.setProcessObjId(oOpenEOJob.getJobId());
			oParameter.setWorkspace(oWorkspace.getWorkspaceId());
			oParameter.setWorkspaceOwnerId(oUser.getUserId());
			oParameter.setProcessorID(oProcessor.getProcessorId());
			oParameter.setProcessorType(oProcessor.getType());
			oParameter.setSessionID(sSessionId);
			oParameter.setUserId(oUser.getUserId());
			oParameter.setVersion(oProcessor.getVersion());
			oParameter.setOGCProcess(true);
			
			String sParameters = oOpenEOJob.getParameters();
			
			Map<String, Object> aoOpenEOParameters = JsonUtils.jsonToMapOfObjects(sParameters);
			
			if (aoOpenEOParameters.containsKey("process")) {
				String sProcessJson = JsonUtils.stringify(aoOpenEOParameters.get("process"));
				
				aoOpenEOParameters = new HashMap<String, Object>();
				
				aoOpenEOParameters.put("process_encoded", StringUtils.encodeUrl(sProcessJson));
				aoOpenEOParameters.put("process", "");
				sParameters = JsonUtils.stringify(aoOpenEOParameters);
				WasdiLog.debugLog("JobsApi.startJob: added encoded process key to params");
			}
			else {
				WasdiLog.warnLog("JobsApi.startJob: we did not found the process key in the parameters!!");
			}
			
			oParameter.setJson(sParameters);
			
			// Serialize the parameters
			String sPayload = SerializationUtils.serializeObjectToStringXML(oParameter);
			
			// We call WASDI to execute: there the platform will make the routing to the right node
			String sUrl = WasdiConfig.Current.baseUrl;
			if (sUrl.endsWith("/") == false) sUrl += "/";
			sUrl += "processing/run?operation=RUNPROCESSOR&name=" + StringUtils.encodeUrl(oProcessor.getName());
			
			HttpCallResponse oHttpCallResponse = HttpUtils.httpPost(sUrl, sPayload, HttpUtils.getStandardHeaders(sSessionId)); 
			// call the API to really execute the processor 
			String sResult = oHttpCallResponse.getResponseBody();
			
			WasdiLog.debugLog("JobsApi.startJob: execute request done");
			
        	// Get back the primitive result: it does not work right to go in the catch 
            PrimitiveResult oPrimitiveResult = MongoRepository.s_oMapper.readValue(sResult,PrimitiveResult.class);
            
            if (!oPrimitiveResult.getBoolValue()) {
    			WasdiLog.debugLog("JobsApi.startJob: unable to start the app, create error status info");
    			
    			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("JobsApi.method", "InternalServerError", "Error starting the app")).build();    			
            }
            else {
                String sProcessWorkspaceId = oPrimitiveResult.getStringValue();
                WasdiLog.debugLog("JobsApi.startJob: got process workspace id " + sProcessWorkspaceId + " job Id = " +sJobId);
                
                oOpenEOJob.setWorkspaceId(oWorkspace.getWorkspaceId());
                oOpenEOJob.setStarted(true);
                oOpenEOJobRepository.updateOpenEOJob(oOpenEOJob);
            }
    		
    		return Response.status(202).build();
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("JobsApi.startJob error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("JobsApi.startJob", "InternalServerError", oEx.getMessage())).build();
		}
    	
    	
    }
    
    @javax.ws.rs.DELETE
    @Path("/{job_id}/results")
    @Produces({ "application/json" })
    public Response stopJob(@PathParam("job_id") @NotNull  @Pattern(regexp="^[\\w\\-\\.~]+$") String sJobId, @HeaderParam("Authorization") String sAuthorization) {
    	
    	User oUser = WasdiOpenEoServer.getUserFromAuthenticationHeader(sAuthorization);
    	
    	if (oUser == null) {
			WasdiLog.debugLog("JobsApi.stopJob: invalid credentials");
			return Response.status(Status.UNAUTHORIZED).entity(Error.getUnathorizedError()).build();    		
    	}
    	try {
    		String sSessionId = sAuthorization.substring("Bearer basic//".length());
    		
    		OpenEOJobRepository oOpenEOJobRepository = new OpenEOJobRepository();
    		OpenEOJob oJob = oOpenEOJobRepository.getOpenEOJob(sJobId);
    		
    		if (oJob == null) {
        		WasdiLog.warnLog("JobsApi.stopJob: EO Job not found ");
        		return Response.status(Status.BAD_REQUEST).entity(Error.getError("JobsApi.stopJob", "InvalidJob", "Invalid Job")).build();    			
    		}
    		
    		WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
    		Workspace oWorkspace = oWorkspaceRepository.getWorkspace(oJob.getWorkspaceId());
    		
    		if (oWorkspace == null) {
        		WasdiLog.warnLog("JobsApi.stopJob: Workspace not found ");
        		return Response.status(Status.BAD_REQUEST).entity(Error.getError("JobsApi.stopJob", "InvalidWorkspace", "Invalid Workspace")).build();    			
    		}
    		
    		String sBaseUrl = getBaseUrl(oWorkspace);
    		
    		ProcessWorkspaceViewModel oProcessWorkspaceViewModel = getProcessWorkspace(oJob.getJobId(), sBaseUrl, sSessionId);
    		
    		if (oProcessWorkspaceViewModel.getStatus().equals("CREATED") || oProcessWorkspaceViewModel.getStatus().equals("RUNNING")||oProcessWorkspaceViewModel.getStatus().equals("READY")||oProcessWorkspaceViewModel.getStatus().equals("WAITING")) {
        		boolean bRet = killProcessWorkspace(sJobId,sBaseUrl,sSessionId);
        		
        		if (!bRet) {
            		WasdiLog.errorLog("JobsApi.stopJob: kill process workspace returned false ");    		    		
            		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("JobsApi.stopJob", "InternalServerError", "Impossible to stop the process")).build();    			
        		}    			
    		}
    		
    		return Response.status(Status.NO_CONTENT).build();
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("JobsApi.stopJob error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("JobsApi.stopJob", "InternalServerError", oEx.getMessage())).build();
		}
    }
    @javax.ws.rs.PATCH
    public Response updateJob(@PathParam("job_id") @NotNull  @Pattern(regexp="^[\\w\\-\\.~]+$") String jobId, @NotNull @Valid  UpdateBatchJobRequest updateBatchJobRequest, @HeaderParam("Authorization") String sAuthorization) {
    	
    	User oUser = WasdiOpenEoServer.getUserFromAuthenticationHeader(sAuthorization);
    	
    	if (oUser == null) {
			WasdiLog.debugLog("JobsApi.updateJob: invalid credentials");
			return Response.status(Status.UNAUTHORIZED).entity(Error.getUnathorizedError()).build();    		
    	}
    	
    	try {
    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("JobsApi.updateJob error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("JobsApi.updateJob", "InternalServerError", oEx.getMessage())).build();
		}
    	
    	return Response.ok().build();
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
    protected Workspace createWorkspaceForAppliction(String sSessionId, User oUser, String sName) {
    	
    	try {
    		// We create a Workspace for this user
    		WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
    		Workspace oWorkspace = new Workspace();
    		
    		// We need the creation date
    		Date oCreationDate = new Date(); 
    		
    		String sWorkspaceName = sName+"_" + oCreationDate.toString();

    		while (oWorkspaceRepository.getByUserIdAndWorkspaceName(oUser.getUserId(), sWorkspaceName) != null) {
    			sWorkspaceName = Utils.cloneName(sWorkspaceName);
    			WasdiLog.debugLog("JobsApi.createWorkspaceForAppliction: a workspace with the same name already exists. Changing the name to " + sWorkspaceName);
    		}

    		// Default values
    		oWorkspace.setCreationDate(Utils.nowInMillis());
    		oWorkspace.setLastEditDate(Utils.nowInMillis());
    		oWorkspace.setName(sWorkspaceName);
    		oWorkspace.setUserId(oUser.getUserId());
    		oWorkspace.setWorkspaceId(Utils.getRandomName());
    		
    		String sNodeCode = "";
    			
    		WasdiLog.debugLog("JobsApi.createWorkspaceForAppliction: search the best node");
    		
    		String sUrl = WasdiConfig.Current.baseUrl;
    		if (sUrl.endsWith("/") == false) sUrl += "/";
    		sUrl += "process/nodesByScore";
    		
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
    			WasdiLog.debugLog("JobsApi.createWorkspaceForAppliction: it was impossible to associate a Node: try user default");
    			//get user's default nodeCode
    			sNodeCode = oUser.getDefaultNode();
    		}
    		
    		if (Utils.isNullOrEmpty(sNodeCode)) {
    			WasdiLog.debugLog("JobsApi.createWorkspaceForAppliction: it was impossible to associate a Node: use system default");
    			// default node code
    			sNodeCode = "wasdi";
    		}
    		
    		WasdiLog.debugLog("JobsApi.createWorkspaceForAppliction: selected node " + sNodeCode);
    		oWorkspace.setNodeCode(sNodeCode);

    		if (oWorkspaceRepository.insertWorkspace(oWorkspace)==false) {
    			WasdiLog.errorLog("JobsApi.createWorkspaceForAppliction: it was impossible to create the workspace");
    			return null;
    		}
    		else {
    			return oWorkspace;
    		}    		
    	}
    	catch (Exception oEx) {
			WasdiLog.errorLog("JobsApi.createWorkspaceForAppliction: exception creating the workspace " + oEx.toString());
		}
    	return null;
    }
    
    /**
     * Get the base url of a workspace in WASDI 
     * @param oWorkspace Target Workspace
     * @return base url of WASDI api in the right node
     */
    protected String getBaseUrl(Workspace oWorkspace) {
		String sBaseUrl = WasdiConfig.Current.baseUrl;
		
		try {
			if (oWorkspace.getNodeCode().equals("wasdi") == false) {
				NodeRepository oNodeRepository = new NodeRepository();
				Node oNode = oNodeRepository.getNodeByCode(oWorkspace.getNodeCode());
				
				if (oNode == null) {
	        		WasdiLog.debugLog("JobsApi.getBaseUrl: Node not found ");		    				
				}
				else {
					sBaseUrl = oNode.getNodeBaseAddress();
				}
			}    				
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("JobsApi.getBaseUrl: exception ", oEx);
		}
		
		return sBaseUrl;
    }
    
	/**
	 * Get WASDI Process Workspace 
	 * @param sProcessId Process Id
	 * @return  ProcessWorkspaceView Model
	 */
	protected ProcessWorkspaceViewModel getProcessWorkspace(String sProcessId, String sBaseUrl, String sSessionId) {
		WasdiLog.debugLog("JobsApi.getProcessWorkspace( " + sProcessId + " )");
		if(null==sProcessId || sProcessId.isEmpty()) {
			WasdiLog.debugLog("JobsApi.getProcessWorkspace: process id null or empty, aborting");
			return null;
		}
		try {
			if (!sBaseUrl.endsWith("/")) sBaseUrl += "/";
			// Create the API call
			String sUrl = sBaseUrl + "process/byid?procws="+sProcessId;

			// Add session token
			Map<String, String> asHeaders = new HashMap<String, String>();
			asHeaders.put("x-session-token", sSessionId);
			
			// Call the API
			HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(sUrl, asHeaders); 
			String sResponse = oHttpCallResponse.getResponseBody();
			
			// Get result and extract status
			ProcessWorkspaceViewModel oProcWS = MongoRepository.s_oMapper.readValue(sResponse, ProcessWorkspaceViewModel.class);
			return oProcWS;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("JobsApi.getProcessWorkspace: " + oEx.toString());
		}	  
		return null;
	}
	
	/**
	 * Kill a WASDI Process
	 * @param sProcessId Process Id
	 * @return  Process Status as a String: CREATED,  RUNNING,  STOPPED,  DONE,  ERROR, WAITING, READY
	 */
	protected boolean killProcessWorkspace(String sProcessId, String sBaseUrl, String sSessionId) {
		WasdiLog.debugLog("JobsApi.killProcessWorkspace( " + sProcessId + " )");
		if(null==sProcessId || sProcessId.isEmpty()) {
			WasdiLog.debugLog("JobsApi.killProcessWorkspace: process id null or empty, aborting");
			return false;
		}
		try {
			if (!sBaseUrl.endsWith("/")) sBaseUrl += "/";
			// Create the API call
			String sUrl = sBaseUrl + "process/delete?procws="+sProcessId+"&treeKill=true";

			// Add session token
			Map<String, String> asHeaders = new HashMap<String, String>();
			asHeaders.put("x-session-token", sSessionId);
			
			// Call the API
			HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(sUrl, asHeaders); 
			
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("JobsApi.killProcessWorkspace: " + oEx.toString());
		}	  
		return false;
	}
	
	/**
	 * Deletes a WASDI Workspace
	 * @param sWorkspaceId Workspace Id
	 * @return  
	 */
	protected boolean deleteWorkspace(String sWorkspaceId, String sBaseUrl, String sSessionId) {
		WasdiLog.debugLog("JobsApi.deleteWorkspace( " + sWorkspaceId + " )");
		if(null==sWorkspaceId || sWorkspaceId.isEmpty()) {
			WasdiLog.debugLog("JobsApi.deleteWorkspace: process id null or empty, aborting");
			return false;
		}
		try {
			if (!sBaseUrl.endsWith("/")) sBaseUrl += "/";
			// Create the API call
			String sUrl = sBaseUrl + "ws/delete?workspace="+sWorkspaceId+"&deletelayer=true&deletefile=true";

			// Add session token
			Map<String, String> asHeaders = new HashMap<String, String>();
			asHeaders.put("x-session-token", sSessionId);
			
			// Call the API
			HttpCallResponse oHttpCallResponse = HttpUtils.httpDelete(sUrl, asHeaders); 
			
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("JobsApi.deleteWorkspace: " + oEx.toString());
		}	  
		return false;
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
	
	private StatusEnum wasdiStateToOpenEOState(String sState) {
		StatusEnum oReturn = StatusEnum.QUEUED;
		
		if (sState.equals(ProcessStatus.CREATED.name())) {
			oReturn = StatusEnum.QUEUED;
		}
		else if (sState.equals(ProcessStatus.DONE.name())) {
			oReturn = StatusEnum.FINISHED;
		}
		else if (sState.equals(ProcessStatus.ERROR.name())) {
			oReturn = StatusEnum.ERROR;
		}
		else if (sState.equals(ProcessStatus.READY.name())) {
			oReturn = StatusEnum.RUNNING;
		}
		else if (sState.equals(ProcessStatus.RUNNING.name())) {
			oReturn = StatusEnum.RUNNING;
		}
		else if (sState.equals(ProcessStatus.STOPPED.name())) {
			oReturn = StatusEnum.CANCELED;
		}
		else if (sState.equals(ProcessStatus.WAITING.name())) {
			oReturn = StatusEnum.RUNNING;
		}
		
		return oReturn;
	}
}
