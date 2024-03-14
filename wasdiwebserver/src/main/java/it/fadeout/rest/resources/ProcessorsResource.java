package it.fadeout.rest.resources;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;
import java.util.stream.Stream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.media.multipart.FormDataParam;

import it.fadeout.Wasdi;
import it.fadeout.rest.resources.largeFileDownload.FileStreamingOutput;
import it.fadeout.rest.resources.largeFileDownload.ZipStreamingOutput;
import it.fadeout.threads.DeleteProcessorWorker;
import it.fadeout.threads.ForceLibraryUpdateWorker;
import it.fadeout.threads.RedeployProcessorWorker;
import it.fadeout.threads.UpdateProcessorFilesWorker;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.AppCategory;
import wasdi.shared.business.Counter;
import wasdi.shared.business.Node;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.Review;
import wasdi.shared.business.Workspace;
import wasdi.shared.business.processors.Processor;
import wasdi.shared.business.processors.ProcessorLog;
import wasdi.shared.business.processors.ProcessorTypes;
import wasdi.shared.business.processors.ProcessorUI;
import wasdi.shared.business.users.ResourceTypes;
import wasdi.shared.business.users.User;
import wasdi.shared.business.users.UserAccessRights;
import wasdi.shared.business.users.UserApplicationRole;
import wasdi.shared.business.users.UserResourcePermission;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.config.ProcessorTypeConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.AppsCategoriesRepository;
import wasdi.shared.data.CounterRepository;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.NodeRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorLogRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.data.ProcessorUIRepository;
import wasdi.shared.data.ReviewRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.data.UserResourcePermissionRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.ImageResourceUtils;
import wasdi.shared.utils.MailUtils;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.StringUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.ZipFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.processors.AppDetailViewModel;
import wasdi.shared.viewmodels.processors.AppFilterViewModel;
import wasdi.shared.viewmodels.processors.AppListViewModel;
import wasdi.shared.viewmodels.processors.DeployedProcessorViewModel;
import wasdi.shared.viewmodels.processors.ProcessorLogViewModel;
import wasdi.shared.viewmodels.processors.ProcessorSharingViewModel;
import wasdi.shared.viewmodels.processworkspace.RunningProcessorViewModel;

/**
 * Processors Resource.
 * Hosts the API for:
 * 	.Upload a new processor
 * 	.update existing processors files and data
 * 	.force update lib and redeploy
 * 	.run processor
 * 
 * @author p.campanella
 *
 */
@Path("/processors")
public class ProcessorsResource  {
	
	/**
	 * Upload a new processor in Wasdi
	 * 
	 * @param oInputStreamForFile Processor Zip file stream 
	 * @param sSessionId User Session Id
	 * @param sWorkspaceId Actual Workspace Id
	 * @param sName Processor Name
	 * @param sVersion Processor Version -> Deprecated
	 * @param sDescription Processor Description
	 * @param sType Processor Type
	 * @param sParamsSample Sample encoded json parameter
	 * @param iPublic 1 if it is pubic, 0 othewise
	 * @param iTimeout processors' specific timeout. 0 means no timeout (may be used scheduler one as configured in the node)
	 * @return Primitive Result with http response code
	 * @throws Exception
	 */
	@POST
	@Path("/uploadprocessor")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public PrimitiveResult uploadProcessor( @FormDataParam("file") InputStream oInputStreamForFile, @HeaderParam("x-session-token") String sSessionId, 
											@QueryParam("workspace") String sWorkspaceId, @QueryParam("name") String sName,
											@QueryParam("version") String sVersion,	@QueryParam("description") String sDescription,
											@QueryParam("type") String sType, @QueryParam("paramsSample") String sParamsSample,
											@QueryParam("public") Integer iPublic, @QueryParam("timeout") Integer iTimeout) throws Exception {
		
		WasdiLog.debugLog("ProcessorsResource.uploadProcessor( Session: " + sSessionId + ", WS: " + sWorkspaceId + ", Name: " + sName + ", Version: " + sVersion + ", Description"
				+ sDescription + ", Type: " + sType + ", ParamsSample: " + sParamsSample + " )");
		
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);

		sName = URLDecoder.decode(sName, StandardCharsets.UTF_8.name());
		sDescription = URLDecoder.decode(sDescription, StandardCharsets.UTF_8.name());
		sParamsSample = URLDecoder.decode(sParamsSample, StandardCharsets.UTF_8.name());

		try {
			if(sName.contains("/") || sName.contains("\\") || sName.contains("#")) {
				WasdiLog.warnLog("ProcessorsResource.uploadProcessor: not a valid filename, aborting");
				oResult.setIntValue(400);
				oResult.setStringValue(sName + " is not a valid filename");
				return oResult;
			}

			if(!isNameUnique(sName)) {
				WasdiLog.warnLog("ProcessorsResource.uploadProcessor: the name is already used, aborting");
				oResult.setIntValue(409);
				oResult.setStringValue("The name " + sName + " is already used. Please use a different name.");
				return oResult;
			}

			User oUser = Wasdi.getUserFromSession(sSessionId);
			if (oUser==null) {
				WasdiLog.warnLog("ProcessorsResource.uploadProcessor: invalid session");
				oResult.setIntValue(401);
				return oResult;
			}
			
			String sUserId = oUser.getUserId();
			
			// Put the processor as Public by default
			if (iPublic == null) {
				iPublic = new Integer(1);
			}
			
			// Force the name to lower case
			sName = sName.toLowerCase();
			// Remove spaces at beginning or end
			sName = sName.trim();
			
			// There are still spaces?
			if (sName.contains(" ")) {
				// Error
				oResult.setIntValue(500);
				oResult.setStringValue("Processor Name MUST be lower case and without any space");
				WasdiLog.warnLog("ProcessorsResource.uploadProcessor: name contains spaces");
				return oResult;				
			}
			
			if (Utils.isNullOrEmpty(sParamsSample)==false) {				
				try {
					MongoRepository.s_oMapper.readTree(sParamsSample);
				}
				catch (Exception oJsonEx) {
					oResult.setIntValue(500);
					oResult.setStringValue("Parameter Sample is not a valid JSON");
					WasdiLog.warnLog("ProcessorsResource.uploadProcessor: not valid parameter sample");
					return oResult;
				}
			}
			
			// Set the processor path
			File oProcessorPath = new File(PathsConfig.getProcessorFolder(sName));
			
			// Create folders
			if (!oProcessorPath.exists()) {
				oProcessorPath.mkdirs();
			}
			else {
				// If the path exists, the name of the processor is already used
				oResult.setIntValue(500);
				oResult.setStringValue("Processor Name Already in Use");
				WasdiLog.warnLog("ProcessorsResource.uploadProcessor: the folder of the app already exists");
				return oResult;
			}
			
			// Create file
			String sProcessorId =  UUID.randomUUID().toString();
			File oProcessorFile = new File(PathsConfig.getProcessorFolder(sName) + sProcessorId + ".zip");
			WasdiLog.debugLog("ProcessorsResource.uploadProcessor: Processor file Path: " + oProcessorFile.getPath());
			
			// Save uploaded file
			int iRead = 0;
			byte[] ayBytes = new byte[1024];
			
			try (OutputStream oOutputStream = new FileOutputStream(oProcessorFile)) {
				while ((iRead = oInputStreamForFile.read(ayBytes)) != -1) {
					oOutputStream.write(ayBytes, 0, iRead);
				}
				oOutputStream.flush();
				oOutputStream.close();				
			}
			
			if (Utils.isNullOrEmpty(sType)) {
				sType = ProcessorTypes.UBUNTU_PYTHON37_SNAP;
			}
			
			Date oDate = new Date();
			
			// Create processor entity
			Processor oProcessor = new Processor();
			oProcessor.setName(sName);
			// Initialize Friendly Name as the name
			oProcessor.setFriendlyName(sName);
			oProcessor.setDescription(sDescription);
			oProcessor.setUserId(sUserId);
			oProcessor.setProcessorId(sProcessorId);
			oProcessor.setVersion(sVersion);
			oProcessor.setPort(-1);
			oProcessor.setType(sType);
			oProcessor.setIsPublic(iPublic);
			oProcessor.setUpdateDate((double)oDate.getTime());
			oProcessor.setUploadDate((double)oDate.getTime());
			
			if (iTimeout != null) {
				oProcessor.setTimeoutMs( ((long)iTimeout)*60l*1000l);
			}
			
			int iPlaceHolderIndex = Utils.getRandomNumber(1, 8);
			oProcessor.setNoLogoPlaceholderIndex(iPlaceHolderIndex);
			oProcessor.setShowInStore(false);
			oProcessor.setLongDescription("");
			
			// Add info about the deploy node
			Node oNode = Wasdi.getActualNode();
			
			if (oNode != null) {
				oProcessor.setNodeCode(oNode.getNodeCode());
				oProcessor.setNodeUrl(oNode.getNodeBaseAddress());
			}
			
			if (!Utils.isNullOrEmpty(sParamsSample)) {
				oProcessor.setParameterSample(sParamsSample);
			}
			
			// Store in the db
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			oProcessorRepository.insertProcessor(oProcessor);
			
			// We need to deploy the processor in the main node
			WasdiLog.debugLog("ProcessorsResource.uploadProcessor: starting deploy on main node");
			
			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
			Workspace oWorkspace = oWorkspaceRepository.getByNameAndNode(Wasdi.s_sLocalWorkspaceName, "wasdi");

			// Schedule the processworkspace to deploy the processor
			String sProcessObjId = Utils.getRandomName();
			
			ProcessorParameter oDeployProcessorParameter = new ProcessorParameter();
			oDeployProcessorParameter.setName(sName);
			oDeployProcessorParameter.setProcessorID(sProcessorId);
			oDeployProcessorParameter.setWorkspace(oWorkspace.getWorkspaceId());
			oDeployProcessorParameter.setUserId(sUserId);
			oDeployProcessorParameter.setExchange(sWorkspaceId);
			oDeployProcessorParameter.setProcessObjId(sProcessObjId);
			oDeployProcessorParameter.setProcessorType(sType);
			oDeployProcessorParameter.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(sWorkspaceId));
			
			PrimitiveResult oRes = Wasdi.runProcess(sUserId, sSessionId, LauncherOperations.DEPLOYPROCESSOR.name(), sName, oDeployProcessorParameter);
			
			if (oRes.getBoolValue()) {
				oResult.setIntValue(200);
				oResult.setBoolValue(true);
				WasdiLog.debugLog("ProcessorsResource.uploadProcessor: done with success");
				return oResult;
			}
			else {
				oResult.setIntValue(500);
				WasdiLog.debugLog("ProcessorsResource.uploadProcessor: finished with error");
				return oResult;				
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorsResource.uploadProcessor error: " + oEx);
			oResult.setIntValue(500);
			return oResult;
		}
		
	}
	
	/**
	 * Get a list of processors available
	 * This is used to get all the processors and not only the one enabled in the marketplace.
	 * The method will collect all:
	 * 	.Users processors
	 * 	.Public processors
	 * 	.Processors shared with the user
	 * 
	 * @param sSessionId User Session Id
	 * @return List of Deployed Processor View Models
	 * @throws Exception
	 */
	@GET
	@Path("/getdeployed")
	public List<DeployedProcessorViewModel> getDeployedProcessors(@HeaderParam("x-session-token") String sSessionId) throws Exception {

		ArrayList<DeployedProcessorViewModel> aoRet = new ArrayList<>(); 
		WasdiLog.debugLog("ProcessorsResource.getDeployedProcessors");
		
		try {
			// Check User 
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser==null) {
				WasdiLog.warnLog("ProcessorsResource.getDeployedProcessors: invalid session");
				return aoRet;
			}
						
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			List<Processor> aoDeployed = oProcessorRepository.getDeployedProcessors();
			
			for (int i=0; i<aoDeployed.size(); i++) {
				
				Processor oProcessor = aoDeployed.get(i);

				UserResourcePermission oSharing = oUserResourcePermissionRepository.getProcessorSharingByUserIdAndProcessorId(oUser.getUserId(), oProcessor.getProcessorId());
				
				// See if this is a processor the user can access to
				if (!PermissionsUtils.canUserAccessProcessor(oUser.getUserId(), oProcessor)) continue;
				
				DeployedProcessorViewModel oDeployedProcessorViewModel = new DeployedProcessorViewModel();
				
				if (oProcessor.getUserId().equals(oUser.getUserId())) {
					oDeployedProcessorViewModel.setReadOnly(false);
				}				
				else if (oSharing != null) {
					oDeployedProcessorViewModel.setSharedWithMe(true);
					oDeployedProcessorViewModel.setReadOnly(oSharing.readOnly());
				}
				else {
					oDeployedProcessorViewModel.setReadOnly(true);
				}
				
				oDeployedProcessorViewModel.setProcessorDescription(oProcessor.getDescription());
				oDeployedProcessorViewModel.setProcessorId(oProcessor.getProcessorId());
				oDeployedProcessorViewModel.setProcessorName(oProcessor.getName());
				oDeployedProcessorViewModel.setProcessorVersion(oProcessor.getVersion());
				oDeployedProcessorViewModel.setPublisher(oProcessor.getUserId());
				oDeployedProcessorViewModel.setParamsSample(oProcessor.getParameterSample());
				oDeployedProcessorViewModel.setIsPublic(oProcessor.getIsPublic());
				oDeployedProcessorViewModel.setType(oProcessor.getType());
				oDeployedProcessorViewModel.setMinuteTimeout((int) (oProcessor.getTimeoutMs()/60000l));
				oDeployedProcessorViewModel.setImgLink(ImageResourceUtils.getProcessorLogoPlaceholderPath(oProcessor));
				oDeployedProcessorViewModel.setLogo(oProcessor.getLogo());
				
				aoRet.add(oDeployedProcessorViewModel);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorsResource.getDeployedProcessors error: " + oEx);
			return aoRet;
		}		
		return aoRet;
	}
	
	/**
	 * Get info of a processor.
	 * @param sSessionId User Session Id
	 * @param sProcessorId Processor Id 
	 * @return Deployed Processor View Model
	 * @throws Exception
	 */
	@GET
	@Path("/getprocessor")
	public DeployedProcessorViewModel getSingleDeployedProcessor(@HeaderParam("x-session-token") String sSessionId, @QueryParam("processorId") String sProcessorId) throws Exception {

		DeployedProcessorViewModel oDeployedProcessorViewModel = new DeployedProcessorViewModel(); 
		WasdiLog.debugLog("ProcessorsResource.getSingleDeployedProcessor");
		
		try {
			// Check User 
			if (Utils.isNullOrEmpty(sSessionId)) return oDeployedProcessorViewModel;
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser==null) {
				WasdiLog.warnLog("ProcessorsResource.getSingleDeployedProcessor: invalid session");
				return oDeployedProcessorViewModel;
			}
			
			if (!PermissionsUtils.canUserAccessProcessor(oUser.getUserId(), sProcessorId)) {
				WasdiLog.warnLog("ProcessorsResource.getSingleDeployedProcessor: user cannot access the processor");
				return oDeployedProcessorViewModel;				
			}
						
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);

			UserResourcePermission oSharing = oUserResourcePermissionRepository.getProcessorSharingByUserIdAndProcessorId(oUser.getUserId(), oProcessor.getProcessorId());

			if (oProcessor.getIsPublic() != 1) {
				if (oProcessor.getUserId().equals(oUser.getUserId()) == false) {
					if (oSharing == null) return oDeployedProcessorViewModel;
				}
			}
			
			if (oSharing != null) {
				oDeployedProcessorViewModel.setSharedWithMe(true);
				oDeployedProcessorViewModel.setReadOnly(oSharing.readOnly());
			}
			if (oProcessor.getUserId().equals(oUser.getUserId()))  {
				oDeployedProcessorViewModel.setReadOnly(false);
			}
			
			oDeployedProcessorViewModel.setProcessorDescription(oProcessor.getDescription());
			oDeployedProcessorViewModel.setProcessorId(oProcessor.getProcessorId());
			oDeployedProcessorViewModel.setProcessorName(oProcessor.getName());
			oDeployedProcessorViewModel.setProcessorVersion(oProcessor.getVersion());
			oDeployedProcessorViewModel.setPublisher(oProcessor.getUserId());
			oDeployedProcessorViewModel.setParamsSample(oProcessor.getParameterSample());
			oDeployedProcessorViewModel.setIsPublic(oProcessor.getIsPublic());
			oDeployedProcessorViewModel.setType(oProcessor.getType());
			oDeployedProcessorViewModel.setMinuteTimeout((int) (oProcessor.getTimeoutMs()/60000l));
			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorsResource.getSingleDeployedProcessor error: " + oEx);
			return oDeployedProcessorViewModel;
		}		
		return oDeployedProcessorViewModel;
	}	
	
	/**
	 * Get the filtered list of the processors available for the marketplace.
	 * The API will return all the public, owned or shared processors that are exposed in the marketplace
	 * and that respects the given filters 
	 * 
	 * @param sSessionId User Session Id
	 * @param oFilters App Filter View Model
	 * @return List of App List View Models
	 * @throws Exception
	 */
	@POST
	@Path("/getmarketlist")
	public List<AppListViewModel> getMarketPlaceAppList(@HeaderParam("x-session-token") String sSessionId, AppFilterViewModel oFilters) throws Exception {

		ArrayList<AppListViewModel> aoRet = new ArrayList<>(); 
		WasdiLog.debugLog("ProcessorsResource.getMarketPlaceAppList");
		
		try {
			// Check User 
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser==null) {
				WasdiLog.warnLog("ProcessorsResource.getMarketPlaceAppList: invalid session");
				return aoRet;
			}
						
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			
			String sOrderBy = "friendlyName";
			
			if (!Utils.isNullOrEmpty(oFilters.getOrderBy())) {
				if (oFilters.getOrderBy().toLowerCase().equals("date")) {
					sOrderBy = "updateDate";
				}
				else if  (oFilters.getOrderBy().toLowerCase().equals("name")) {
					sOrderBy = "friendlyName";
				}
				else if  (oFilters.getOrderBy().toLowerCase().equals("price")) {
					sOrderBy = "ondemandPrice";
				}
			}
			
			// Initialize ascending direction
			int iDirection = 1;
			// Set descending if it was -1
			if (oFilters.getOrderDirection() == -1) iDirection = -1;
			
			List<Processor> aoDeployed = oProcessorRepository.getDeployedProcessors(sOrderBy, iDirection);
			ReviewRepository oReviewRepository = new ReviewRepository();
			
			int iAvailableApps = 0;
			
			for (int i=0; i<aoDeployed.size(); i++) {
								
				AppListViewModel oAppListViewModel = new AppListViewModel();
				Processor oProcessor = aoDeployed.get(i);
				// Initialize as No Votes
				float fScore = -1.0f;
				
				if (!oProcessor.getShowInStore()) continue;
				
				// See if this is a processor the user can access to
				if (!PermissionsUtils.canUserAccessProcessor(oUser.getUserId(), oProcessor.getProcessorId())) continue;
				
				// Check and apply name filter taking in account both friendly and app name.
				// Friendly name was added on the check to have a coherent behaviour for the users
				if (!Utils.isNullOrEmpty(oFilters.getName())) {
					String sLowerProcessorName = oProcessor.getName().toLowerCase();
					String sLowerProcessorFriendlyName = oProcessor.getFriendlyName().toLowerCase();
					String sLowerFiltername = oFilters.getName().toLowerCase();
					if (!(sLowerProcessorName.contains(sLowerFiltername) || sLowerProcessorFriendlyName.contains(sLowerFiltername))) continue;
				}
				
				// Check and apply category filter
				if (oFilters.getCategories().size()>0) {
					
					boolean bCategoryFound = false;
					
					for (String sProcessorCategory : oProcessor.getCategories()) {
						if (oFilters.getCategories().contains(sProcessorCategory)) {
							bCategoryFound = true;
							break;
						}
					}
					
					if (!bCategoryFound) continue;
				}
				
				// Check and apply publisher filter
				if (oFilters.getPublishers().size()>0) {
					
					if (!oFilters.getPublishers().contains(oProcessor.getUserId())) {
						continue;
					}
				}
				
				// Get the reviews to compute the vote
				List<Review> aoReviews = oReviewRepository.getReviews(oProcessor.getProcessorId());

				int iVotes = 0;

				// If we have reviews
				if (aoReviews != null) {
					if (aoReviews.size()>0) {
						fScore = 0;
						
						// Take the sum
						for (Review oReview : aoReviews) {
							fScore += oReview.getVote();
						}
						
						// Compute average
						fScore /= aoReviews.size();

						iVotes = aoReviews.size();
					}
				}				
				
				if (oFilters.getScore()> 0) {
										
					if (fScore<oFilters.getScore() && fScore != -1.0f) {
						continue;
					}
				}
				
				// Check and apply max price filter
				if (oFilters.getMaxPrice()>=0) {
					if (oProcessor.getOndemandPrice() > oFilters.getMaxPrice()) continue;
				}
				
				// This is a app compatible with the filter: handle the pagination
				
				// Jump if this is an app of previous pages
				if (iAvailableApps<oFilters.getPage()*oFilters.getItemsPerPage()) {
					iAvailableApps++;
					continue;
				}
				// Stop if this is an app after the actual page
				if (iAvailableApps>= (oFilters.getPage()+1) * oFilters.getItemsPerPage()) break;
				
				iAvailableApps++;
				
				UserResourcePermission oSharing = oUserResourcePermissionRepository.getProcessorSharingByUserIdAndProcessorId(oUser.getUserId(), oProcessor.getProcessorId());
				
				oAppListViewModel.setIsMine(false);
				
				if (oProcessor.getUserId().equals(oUser.getUserId())) {
					oAppListViewModel.setIsMine(true);
					oAppListViewModel.setReadOnly(false);
				}
				
				if (oSharing != null) {
					if (oSharing.canWrite()) {
						oAppListViewModel.setIsMine(true);
						oAppListViewModel.setReadOnly(false);
					}
					else {
						oAppListViewModel.setIsMine(false);
						oAppListViewModel.setReadOnly(true);						
					}
				}
				else {
					oAppListViewModel.setIsMine(false);
					oAppListViewModel.setReadOnly(true);					
				}
				
				oAppListViewModel.setProcessorDescription(oProcessor.getDescription());
				oAppListViewModel.setProcessorId(oProcessor.getProcessorId());
				oAppListViewModel.setProcessorName(oProcessor.getName());
				oAppListViewModel.setPublisher(oProcessor.getUserId());
				oAppListViewModel.setBuyed(false);
				oAppListViewModel.setPrice(oProcessor.getOndemandPrice());

				oAppListViewModel.setImgLink(ImageResourceUtils.getProcessorLogoPlaceholderPath(oProcessor));
				oAppListViewModel.setLogo(oProcessor.getLogo());
				
				// Set the friendly name, same of name if null
				if (!Utils.isNullOrEmpty(oProcessor.getFriendlyName())) {
					oAppListViewModel.setFriendlyName(oProcessor.getFriendlyName());
				}
				else {
					oAppListViewModel.setFriendlyName(oProcessor.getName());
				}
								
				// Set the score to the View Model
				oAppListViewModel.setScore(fScore);
				oAppListViewModel.setVotes(iVotes);

				aoRet.add(oAppListViewModel);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorsResource.getMarketPlaceAppList error: " + oEx);
			return aoRet;
		}		
		return aoRet;
	}
	
	/**
	 * Get the detailed marketplace info for an application.
	 * 
	 * @param sSessionId User Session Id
	 * @param sProcessorName Processor Name
	 * @return App Detail View Model
	 * @throws Exception
	 */
	@GET
	@Path("/getmarketdetail")
	public Response getMarketPlaceAppDetail(@HeaderParam("x-session-token") String sSessionId, @QueryParam("processorname") String sProcessorName) throws Exception {
		sProcessorName = URLDecoder.decode(sProcessorName, StandardCharsets.UTF_8.name());
		WasdiLog.debugLog("ProcessorsResource.getMarketPlaceAppDetail");
		
		try {
			// Check User 
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser==null) {
				WasdiLog.warnLog("ProcessorsResource.getMarketPlaceAppDetail: invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			AppDetailViewModel oAppDetailViewModel = new AppDetailViewModel();
						
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			Processor oProcessor = oProcessorRepository.getProcessorByName(sProcessorName);
			
			if (oProcessor==null) {
				WasdiLog.warnLog("ProcessorsResource.getMarketPlaceAppDetail: processor is null");
				return Response.status(Status.BAD_REQUEST).build();
			}
			
			if (!PermissionsUtils.canUserAccessProcessor(oUser.getUserId(), oProcessor)) {
				WasdiLog.warnLog("ProcessorsResource.getMarketPlaceAppDetail: user cannot access the processor");
				return Response.status(Status.FORBIDDEN).build();				
			}			
			
			UserResourcePermission oSharing = oUserResourcePermissionRepository.getProcessorSharingByUserIdAndProcessorId(oUser.getUserId(), oProcessor.getProcessorId());
			
			ReviewRepository oReviewRepository = new ReviewRepository();
			
			oAppDetailViewModel.setIsMine(false);
			
			if (oProcessor.getUserId().equals(oUser.getUserId())) {
				oAppDetailViewModel.setIsMine(true);
				oAppDetailViewModel.setReadOnly(false);
			}
			
			if (oSharing != null) {
				if (oSharing.canWrite()) {
					oAppDetailViewModel.setIsMine(true);
					oAppDetailViewModel.setReadOnly(false);
				}
				else {
					oAppDetailViewModel.setIsMine(false);
					oAppDetailViewModel.setReadOnly(true);					
				}
			}
			
			oAppDetailViewModel.setProcessorDescription(oProcessor.getDescription());
			oAppDetailViewModel.setProcessorId(oProcessor.getProcessorId());
			oAppDetailViewModel.setProcessorName(oProcessor.getName());
			oAppDetailViewModel.setPublisher(oProcessor.getUserId());
			oAppDetailViewModel.setBuyed(false);
			
			oAppDetailViewModel.setShowInStore(oProcessor.getShowInStore());
			oAppDetailViewModel.setLongDescription(oProcessor.getLongDescription());
			
			oAppDetailViewModel.setImgLink(ImageResourceUtils.getProcessorLogoPlaceholderPath(oProcessor));
			
			if (!Utils.isNullOrEmpty(oProcessor.getLogo())) {
				oAppDetailViewModel.setLogo(oProcessor.getLogo());
			}
			
			// Set the friendly name, same of name if null
			if (!Utils.isNullOrEmpty(oProcessor.getFriendlyName())) {
				oAppDetailViewModel.setFriendlyName(oProcessor.getFriendlyName());
			}
			else {
				oAppDetailViewModel.setFriendlyName(oProcessor.getName());
			}
			
			oAppDetailViewModel.setEmail(oProcessor.getEmail());
			oAppDetailViewModel.setLink(oProcessor.getLink());
			oAppDetailViewModel.setOndemandPrice(oProcessor.getOndemandPrice());
			oAppDetailViewModel.setSubscriptionPrice(oProcessor.getSubscriptionPrice());
			oAppDetailViewModel.setUpdateDate(oProcessor.getUpdateDate());
			oAppDetailViewModel.setPublishDate(oProcessor.getUploadDate());
			oAppDetailViewModel.setCategories(oProcessor.getCategories());
			
			AppsCategoriesRepository oAppsCategoriesRepository = new AppsCategoriesRepository();
			
			for (String sCategoryId : oAppDetailViewModel.getCategories()) {
				
				AppCategory oAppCategory = oAppsCategoriesRepository.getCategoryById(sCategoryId);
				
				if (oAppCategory != null) {
					oAppDetailViewModel.getCategoryNames().add(oAppCategory.getCategory());
				}
			}
			
			oAppDetailViewModel.setImages(ImageResourceUtils.getProcessorImagesList(oProcessor));
			oAppDetailViewModel.setMaxImages(ImageResourceUtils.s_asIMAGE_NAMES.length);
			
			// TODO: At the moment we do not have this data: put the number of run in the main server
			// But this has to be changed
			ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
			oAppDetailViewModel.setPurchased((int)oProcessWorkspaceRepository.countByProcessor(oProcessor.getName()));
			
			// Get the reviews to compute the vote
			List<Review> aoReviews = oReviewRepository.getReviews(oProcessor.getProcessorId());
			
			// Initialize as No Votes
			float fScore = -1.0f;
			
			// If we have reviews
			if (aoReviews != null) {
				if (aoReviews.size()>0) {
					
					fScore = 0.0f;
					
					oAppDetailViewModel.setReviewsCount(aoReviews.size());
					
					// Take the sum
					for (Review oReview : aoReviews) {
						fScore += oReview.getVote();
					}
					
					// Compute average
					fScore /= aoReviews.size();
				}
			}
			
			// Set the score to the View Model
			oAppDetailViewModel.setScore(fScore);
			
			return Response.ok(oAppDetailViewModel).build();
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorsResource.getMarketPlaceAppDetail error: " + oEx);
			return Response.serverError().build();
		}		
	}	
		
	/**
	 * Run a processor.
	 * This triggers the execution of the launcher.
	 * This version is a POST to support long parameters
	 * 
	 * @param sSessionId User Session 
	 * @param sName Processor Name
	 * @param sWorkspaceId Workspace Id
	 * @param sParentProcessWorkspaceId Proc Id of the parent process
	 * @param sEncodedJson Processors' parameters as encoded JSON
	 * @return Running Processor View Model
	 * @throws Exception
	 */
	@POST
	@Path("/run")
	public RunningProcessorViewModel runPost(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("name") String sName, @QueryParam("workspace") String sWorkspaceId,
			@QueryParam("parent") String sParentProcessWorkspaceId, @QueryParam("notify") Boolean bNotify, String sEncodedJson) throws Exception {
		WasdiLog.debugLog("ProcessorsResource.runPost( Name: " + sName + ", encodedJson:" + sEncodedJson + ", WS: " + sWorkspaceId + " )");
		return internalRun(sSessionId, sName, sEncodedJson, sWorkspaceId, sParentProcessWorkspaceId, bNotify);
	}
	
	/**
	 * Run a processor.
	 * This triggers the execution of the launcher.
	 * This version is a get and supports only short parameters
	 * 
	 * @param sSessionId User Session 
	 * @param sName Processor Name
	 * @param sWorkspaceId Workspace Id
	 * @param sParentProcessWorkspaceId Proc Id of the parent process
	 * @param sEncodedJson Processors' parameters as encoded JSON
	 * @return Running Processor View Model
	 * @throws Exception
	 */

	@GET
	@Path("/run")
	public RunningProcessorViewModel run(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("name") String sName, @QueryParam("encodedJson") String sEncodedJson,
			@QueryParam("workspace") String sWorkspaceId,
			@QueryParam("parent") String sParentProcessWorkspaceId, @QueryParam("notify") Boolean bNotify) throws Exception {
		
		
		WasdiLog.debugLog("ProcessorsResource.run: run@GET");
		return internalRun(sSessionId, sName, sEncodedJson, sWorkspaceId, sParentProcessWorkspaceId, bNotify);
	}
	
	/**
	 * Internal method to create run operation for both GET and POST versions
	 * 
	 * @param sSessionId User Session Id
	 * @param sName Processor Name
	 * @param sEncodedJson Encoded JSON
	 * @param sWorkspaceId Workspace Id
	 * @param sParentProcessWorkspaceId Proc Id of the parent Process
	 * @return Running Processor View Model
	 * @throws Exception
	 */
	public RunningProcessorViewModel internalRun(String sSessionId, String sName, String sEncodedJson, String sWorkspaceId, String sParentProcessWorkspaceId, Boolean bNotify) throws Exception {
		WasdiLog.debugLog("ProcessorsResource.internalRun( Name: " + sName + ", encodedJson:" + sEncodedJson + ", WS: " + sWorkspaceId + " )");
		
		if (bNotify==null) bNotify = false;

		RunningProcessorViewModel oRunningProcessorViewModel = new RunningProcessorViewModel();
		
		try {
			// Check User 
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser==null) {
				WasdiLog.warnLog("ProcessorsResource.internalRun: invalid session");
				oRunningProcessorViewModel.setStatus("ERROR");
				return oRunningProcessorViewModel;
			}
			
			if (!PermissionsUtils.userHasValidSubscription(oUser)) {
				WasdiLog.warnLog("ProcessorsResource.internalRun: user does not have a valid subscription");
				oRunningProcessorViewModel.setStatus("ERROR");
				return oRunningProcessorViewModel;
			}
			
			String sUserId = oUser.getUserId();
			
			if (!PermissionsUtils.canUserAccessWorkspace(sUserId, sWorkspaceId)) {				
				WasdiLog.warnLog("ProcessorsResource.internalRun: user cannot access the workspace");
				oRunningProcessorViewModel.setStatus("ERROR");
				return oRunningProcessorViewModel;
			}
		
			WasdiLog.debugLog("ProcessorsResource.internalRun: get Processor");
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessorToRun = oProcessorRepository.getProcessorByName(sName);
			
			if (oProcessorToRun == null) { 
				WasdiLog.warnLog("ProcessorsResource.internalRun: unable to find processor " + sName);
				oRunningProcessorViewModel.setStatus("ERROR");
				return oRunningProcessorViewModel;
			}
			
			if (!PermissionsUtils.canUserAccessProcessor(sUserId, oProcessorToRun)) {
				WasdiLog.warnLog("ProcessorsResource.internalRun: the user cannot access th processor ");
				oRunningProcessorViewModel.setStatus("ERROR");
				return oRunningProcessorViewModel;				
			}
			
			if (Utils.isNullOrEmpty(sEncodedJson)) {
				sEncodedJson = "%7B%7D";
			}
			
			if (sEncodedJson.contains("+")) {
				// The + char is not encoded but then in the launcher, when is decode, become a space. Replace the char with the correct representation
				sEncodedJson = sEncodedJson.replaceAll("\\+", "%2B");
				WasdiLog.debugLog("ProcessorsResource.internalRun: replaced + with %2B in encoded JSON " + sEncodedJson);
			}

			// Schedule the process to run the processor
			String sProcessObjId = Utils.getRandomName();

			ProcessorParameter oProcessorParameter = new ProcessorParameter();
			oProcessorParameter.setName(sName);
			oProcessorParameter.setProcessorID(oProcessorToRun.getProcessorId());
			oProcessorParameter.setWorkspace(sWorkspaceId);
			oProcessorParameter.setUserId(sUserId);
			oProcessorParameter.setExchange(sWorkspaceId);
			oProcessorParameter.setProcessObjId(sProcessObjId);
			oProcessorParameter.setJson(sEncodedJson);
			oProcessorParameter.setProcessorType(oProcessorToRun.getType());
			oProcessorParameter.setSessionID(sSessionId);
			oProcessorParameter.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(sWorkspaceId));
			oProcessorParameter.setNotifyOwnerByMail(bNotify);
			
			PrimitiveResult oResult = Wasdi.runProcess(sUserId, sSessionId, oProcessorParameter.getLauncherOperation(), sName, oProcessorParameter, sParentProcessWorkspaceId);
			
			try{
				WasdiLog.debugLog("ProcessorsResource.internalRun: create task");
				
				if (oResult.getBoolValue()==false) {
					throw new Exception();
				}
								
				oRunningProcessorViewModel.setJsonEncodedResult("");
				oRunningProcessorViewModel.setName(sName);
				oRunningProcessorViewModel.setProcessingIdentifier(oResult.getStringValue());
				oRunningProcessorViewModel.setProcessorId(oProcessorToRun.getProcessorId());
				oRunningProcessorViewModel.setStatus("CREATED");
				WasdiLog.debugLog("ProcessorsResource.internalRun: done"); 
			}
			catch(Exception oEx){
				WasdiLog.errorLog("ProcessorsResource.internalRun: Error scheduling the run process " + oEx);
				oRunningProcessorViewModel.setStatus(ProcessStatus.ERROR.toString());
				return oRunningProcessorViewModel;
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorsResource.internalRun error: " + oEx );
			oRunningProcessorViewModel.setStatus(ProcessStatus.ERROR.toString());
			return oRunningProcessorViewModel;
		}
		
		return oRunningProcessorViewModel;
	}
	
	/**
	 * Return the help of a processor. when the user uploads a processor it can upload also an help file
	 * like help.md (supported different names ie readme.md...)
	 * This API return the content of that file, if exists.
	 * 
	 * @param sSessionId User Session Id
	 * @param sName Processor name
	 * @return PrimitiveResult: if boolValue is = true, stringValue has the help
	 * @throws Exception
	 */
	@GET
	@Path("/help")
	public PrimitiveResult help(@HeaderParam("x-session-token") String sSessionId, @QueryParam("name") String sName) throws Exception {
		WasdiLog.debugLog("ProcessorsResource.help( Name: " + sName + " )");
		PrimitiveResult oPrimitiveResult = new PrimitiveResult();
		oPrimitiveResult.setBoolValue(false);
		
		try {
			// Check User 
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser==null) {
				WasdiLog.warnLog("ProcessorsResource.help: invalid session");
				return oPrimitiveResult;
			}
			
			if (!PermissionsUtils.canUserAccessProcessorByName(oUser.getUserId(), sName)) {
				WasdiLog.warnLog("ProcessorsResource.help: user cannot access processor");
				return oPrimitiveResult;				
			}
			
			WasdiLog.debugLog("ProcessorsResource.help: read Processor " +sName);
			
			// Take path
			String sProcessorPath = PathsConfig.getProcessorFolder(sName);
			java.nio.file.Path oDirPath = java.nio.file.Paths.get(sProcessorPath).toAbsolutePath().normalize();
			File oDirFile = oDirPath.toFile();

			if (!WasdiFileUtils.fileExists(oDirFile) || !oDirFile.isDirectory()) {
				WasdiLog.warnLog("ProcessorsResource.help: directory " + oDirPath.toString() + " not found");
				return oPrimitiveResult;
			}

			String sOutputCumulativeResult = Arrays.stream(oDirFile.listFiles())
				.filter(File::isFile)
				.filter(WasdiFileUtils::isHelpFile)
				.map(File::getAbsolutePath)
				.map(WasdiFileUtils::fileToText)
				.findFirst()
				.orElseGet(() -> "");

			WasdiLog.debugLog("ProcessorsResource.help: got help\n");
			
			oPrimitiveResult.setBoolValue(true);
			oPrimitiveResult.setStringValue(sOutputCumulativeResult);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorsResource.help error: " + oEx);
			return oPrimitiveResult;
		}
		
		return oPrimitiveResult;
	}
	
	/**
	 * Add a log row to a running processor
	 * 
	 * @param sSessionId User Session Id
	 * @param sProcessWorkspaceId Process Workspace Id
	 * @param sLog Log row
	 * @return std http response
	 */
	@POST
	@Path("/logs/add")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response addLog(@HeaderParam("x-session-token") String sSessionId, @QueryParam("processworkspace") String sProcessWorkspaceId, String sLog) {
		try {
			// Check User 
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser==null) {
				WasdiLog.warnLog("ProcessorsResource.addLog: invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			if (!PermissionsUtils.canUserAccessProcessWorkspace(oUser.getUserId(), sProcessWorkspaceId)) {
				WasdiLog.warnLog("ProcessorsResource.addLog: user cannot access the process workspace");
				return Response.status(Status.FORBIDDEN).build();				
			}
						
			ProcessorLog oLog = new ProcessorLog();
			
			oLog.setLogDate(Utils.getFormatDate(new Date()));
			oLog.setProcessWorkspaceId(sProcessWorkspaceId);
			oLog.setLogRow(sLog);
			
			ProcessorLogRepository oProcessorLogRepository = new ProcessorLogRepository();
			String sResult = oProcessorLogRepository.insertProcessLog(oLog);
			if(!Utils.isNullOrEmpty(sResult)) {
				WasdiLog.debugLog("ProcessorResource.addLog: added log row to processid " + sProcessWorkspaceId);
			} else {
				WasdiLog.debugLog("ProcessorResource.addLog: failed to add log row");
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorResource.addLog error: " + oEx);
			return Response.serverError().build();
		}
		return Response.ok().build();
	 }
	
	/**
	 * Get tht total count of log rows of a processor
	 * @param sSessionId User Session Id
	 * @param sProcessWorkspaceId Process Workspace Id
	 * @return int with the number of logs of the processor
	 */
	@GET
	@Path("/logs/count")
	public int countLogs(@HeaderParam("x-session-token") String sSessionId, @QueryParam("processworkspace") String sProcessWorkspaceId){
		
		WasdiLog.debugLog("ProcessorResource.countLogs( ProcWsId: " + sProcessWorkspaceId + " )");
		int iResult = -1;
		try {
			User oUser = Wasdi.getUserFromSession(sSessionId);
	
			if (oUser==null) {
				WasdiLog.warnLog("ProcessorResource.countLogs: invalid session");
				return iResult;
			}
			
			if (!PermissionsUtils.canUserAccessProcessWorkspace(oUser.getUserId(), sProcessWorkspaceId)) {
				WasdiLog.warnLog("ProcessorsResource.countLogs: user cannot access the process workspace");
				return iResult;				
			}			
			
			WasdiLog.debugLog("ProcessorResource.countLogs: get log count for process " + sProcessWorkspaceId);
			
			CounterRepository oCounterRepository = new CounterRepository();
			Counter oCounter = null;
			oCounter = oCounterRepository.getCounterBySequence(sProcessWorkspaceId);
			if(null == oCounter) {
				WasdiLog.warnLog("ProcessorResource.countLogs: CounterRepository returned a null Counter");
				return iResult;
			}
			iResult = oCounter.getValue() + 1;
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorResource.countLogs error: " + oEx);
		}
		return iResult;
	}
	
	/**
	 * Get a paginated list of logs of a processor
	 * 
	 * @param sSessionId User Session Id
	 * @param sProcessWorkspaceId Process Workspace Id
	 * @param iStartRow Start log row
	 * @param iEndRow End log row
	 * @return
	 */
	@GET
	@Path("/logs/list")
	public ArrayList<ProcessorLogViewModel> getLogs(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("processworkspace") String sProcessWorkspaceId,
			//note: range extremes are included
			@QueryParam("startrow") Integer iStartRow, @QueryParam("endrow") Integer iEndRow) {
		
		WasdiLog.debugLog("ProcessorsResource.getLogs( ProcWsId: " + sProcessWorkspaceId + ", Start: " + iStartRow + ", End: " + iEndRow + " )");
		ArrayList<ProcessorLogViewModel> aoRetList = new ArrayList<>();
		try {
			
			// Check User 
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser==null) {
				WasdiLog.warnLog("ProcessorsResource.getLogs: invalid session");
				return aoRetList;
			}
			
			if (!PermissionsUtils.canUserAccessProcessWorkspace(oUser.getUserId(), sProcessWorkspaceId)) {
				WasdiLog.warnLog("ProcessorsResource.countLogs: user cannot access the process workspace");
				return aoRetList;				
			}			
						
			WasdiLog.debugLog("ProcessorResource.getLogs: get log for process " + sProcessWorkspaceId);
			
			ProcessorLogRepository oProcessorLogRepository = new ProcessorLogRepository();
			List<ProcessorLog> aoLogs = null;
			if(null==iStartRow || null==iEndRow) {
				aoLogs = oProcessorLogRepository.getLogsByProcessWorkspaceId(sProcessWorkspaceId);
			} else {
				aoLogs = oProcessorLogRepository.getLogsByWorkspaceIdInRange(sProcessWorkspaceId, iStartRow, iEndRow);
			}
			
			for(int iLogs = 0; iLogs<aoLogs.size(); iLogs++) {
				ProcessorLog oLog = aoLogs.get(iLogs);
				
				ProcessorLogViewModel oLogVM = new ProcessorLogViewModel();
				oLogVM.setLogDate(oLog.getLogDate());
				oLogVM.setLogRow(oLog.getLogRow());
				oLogVM.setProcessWorkspaceId(sProcessWorkspaceId);
				oLogVM.setRowNumber(oLog.getRowNumber());
				aoRetList.add(oLogVM);
			}
			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorResource.getLogs error: " + oEx);
			return aoRetList;
		}
		
		return aoRetList;

	 }
	
	
	/**
	 * Deletes a processor on a distributed node. The API is different from the main one that can be called on the main server.
	 * This version can be triggered when the Processor have been already deleted from the database so it must take in input also
	 * the name and the type that the other API takes from the db.
	 * This API triggers a local DELETEPROCESSOR operation 
	 * @param sSessionId Valid user session
	 * @param sProcessorId Id of the processor to delete 
	 * @param sWorkspaceId Workspace Id: here is used only for the rabbit exchange
	 * @param sProcessorName Name of the processor to delete
	 * @param sProcessorType Type of the processor to delete
	 * @return
	 */
	@GET
	@Path("/nodedelete")
	public Response nodeDeleteProcessor(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("processorId") String sProcessorId,
			@QueryParam("workspace") String sWorkspaceId,
			@QueryParam("processorName") String sProcessorName,
			@QueryParam("processorType") String sProcessorType,
			@QueryParam("version") String sVersion) {
		WasdiLog.debugLog("ProcessorResources.nodeDeleteProcessor( Session: " + sSessionId + ", Processor: " + sProcessorId + ", WS: " + sWorkspaceId + " )");
		
		try {
			
			// Check session
			User oUser = Wasdi.getUserFromSession(sSessionId);

			// Check user
			if (oUser==null) {
				WasdiLog.warnLog("ProcessorResources.nodeDeleteProcessor: invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
			
			if (oProcessor == null) {
				WasdiLog.warnLog("ProcessorResources.nodeDeleteProcessor: processor is null");
				return Response.status(Status.BAD_REQUEST).build();				
			}
			
			if (!oProcessor.getUserId().equals(oUser.getUserId())) {
				WasdiLog.warnLog("ProcessorResources.nodeDeleteProcessor: this is not the owner!");
				return Response.status(Status.UNAUTHORIZED).build();				
			}
			
			// This API is allowed ONLY on computing nodes
			if (WasdiConfig.Current.isMainNode()) {
				WasdiLog.warnLog("ProcessorsResource.nodeDeleteProcessor: this is the main node, cannot call this API here");
				return Response.status(Status.BAD_REQUEST).build();
			}

			String sUserId = oUser.getUserId();
			
			// Schedule the process to delete the processor
			String sProcessObjId = Utils.getRandomName();
						
			// Trigger the processor delete operation on this specific node
			WasdiLog.debugLog("ProcessorsResource.nodeDeleteProcessor: this is a computing node, just execute Delete here");
			
			// Get the dedicated special workpsace
			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
			Workspace oWorkspace = oWorkspaceRepository.getByNameAndNode(Wasdi.s_sLocalWorkspaceName, WasdiConfig.Current.nodeCode);
			
			if (oWorkspace != null) {
				
				WasdiLog.debugLog("ProcessorsResource.nodeDeleteProcessor: Create Delete Processor operation in Workspace " + Wasdi.s_sLocalWorkspaceName);
				
				ProcessorParameter oProcessorParameter = new ProcessorParameter();
				oProcessorParameter.setName(sProcessorName);
				oProcessorParameter.setProcessorID(sProcessorId);
				oProcessorParameter.setWorkspace(oWorkspace.getWorkspaceId());
				oProcessorParameter.setUserId(sUserId);
				oProcessorParameter.setExchange(sWorkspaceId);
				oProcessorParameter.setProcessObjId(sProcessObjId);
				oProcessorParameter.setJson("{}");
				oProcessorParameter.setProcessorType(sProcessorType);
				oProcessorParameter.setSessionID(sSessionId);
				oProcessorParameter.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(oWorkspace.getWorkspaceId()));
				oProcessorParameter.setVersion(sVersion);

				
				Wasdi.runProcess(sUserId, sSessionId, LauncherOperations.DELETEPROCESSOR.name(), sProcessorName, oProcessorParameter);		
			}
			else {
				WasdiLog.warnLog("ProcessorsResource.nodeDeleteProcessor: IMPOSSIBLE TO FIND NODE SPECIFIC WORKSPACE!!!!");
			}			
			
			return Response.ok().build();
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorResource.nodeDeleteProcessor error: " + oEx);
			return Response.serverError().build();
		}
	}	
	
	/**
	 * Delete a processor. The API must be called on the main server.
	 * @param sSessionId
	 * @param sProcessorId
	 * @param sWorkspaceId
	 * @return
	 */
	@GET
	@Path("/delete")
	public Response deleteProcessor(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("processorId") String sProcessorId,
			@QueryParam("workspace") String sWorkspaceId) {
		WasdiLog.debugLog("ProcessorResources.deleteProcessor( Processor: " + sProcessorId + ", WS: " + sWorkspaceId + " )");
		
		try {
			// Check the session
			User oUser = Wasdi.getUserFromSession(sSessionId);
			
			// Check the user
			if (oUser==null) {
				WasdiLog.warnLog("ProcessorResources.deleteProcessor: invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
						
			// This API is allowed ONLY on the main node
			if (!WasdiConfig.Current.isMainNode()) {
				WasdiLog.warnLog("ProcessorsResource.deleteProcessor: this is not the main node, cannot call this API here");
				return Response.status(Status.BAD_REQUEST).build();
			}			

			String sUserId = oUser.getUserId();
			
			// Get the processor from the Db
			WasdiLog.debugLog("ProcessorsResource.deleteProcessor: get Processor");
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessorToDelete = oProcessorRepository.getProcessor(sProcessorId);
			
			// At the first call, it must exists
			if (oProcessorToDelete == null) {
				WasdiLog.warnLog("ProcessorsResource.deleteProcessor: unable to find processor " + sProcessorId);
				return Response.serverError().build();
			}
			
			// Is the user requesting the owner of the processor?
			if (!oProcessorToDelete.getUserId().equals(oUser.getUserId())) {
				WasdiLog.debugLog("ProcessorsResource.deleteProcessor: processor not of user " + oUser.getUserId());
				
				// Is this a sharing?				
				UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
				UserResourcePermission oSharing = oUserResourcePermissionRepository.getProcessorSharingByUserIdAndProcessorId(oUser.getUserId(), sProcessorId);
				
				if (oSharing != null) {
					
					// Delete the share
					WasdiLog.debugLog("ProcessorsResource.deleteProcessor: the processor was shared with " + oUser.getUserId() + ", delete the sharing");
					oUserResourcePermissionRepository.deletePermissionsByUserIdAndProcessorId(oUser.getUserId(), sProcessorId);
					
					return Response.ok().build();
				}
				
				// No, so unauthorized
				return Response.status(Status.UNAUTHORIZED).build();
			}

			// Schedule the process to delete the processor
			String sProcessObjId = Utils.getRandomName();
			
			// Start a thread to update all the computing nodes
			try {
				WasdiLog.debugLog("ProcessorsResource.deleteProcessor: this is the main node, starting Worker to delete Processor also on computing nodes");
				
				// Util to call the API on computing nodes
				DeleteProcessorWorker oDeleteWorker = new DeleteProcessorWorker();
				
				NodeRepository oNodeRepo = new NodeRepository();
				List<Node> aoNodes = oNodeRepo.getNodesList();
				
				oDeleteWorker.init(aoNodes, sSessionId, sWorkspaceId, sProcessorId, oProcessorToDelete.getName(), oProcessorToDelete.getType(), oProcessorToDelete.getVersion());
				oDeleteWorker.start();
				
				WasdiLog.debugLog("ProcessorsResource.deleteProcessor: Worker started");						
			}
			catch (Exception oEx) {
				WasdiLog.errorLog("ProcessorsResource.deleteProcessor: error starting UpdateWorker " + oEx.toString());
			}
			
			// Trigger the processor delete operation on this specific node
			WasdiLog.debugLog("ProcessorsResource.deleteProcessor: Scheduling Processor Delete Operation");
			
			// Get the dedicated special workpsace
			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
			Workspace oWorkspace = oWorkspaceRepository.getByNameAndNode(Wasdi.s_sLocalWorkspaceName, WasdiConfig.Current.nodeCode);
			
			if (oWorkspace != null) {
				
				WasdiLog.debugLog("ProcessorsResource.deleteProcessor: Create Delete Processor operation in Workspace " + Wasdi.s_sLocalWorkspaceName);
				
				ProcessorParameter oProcessorParameter = new ProcessorParameter();
				oProcessorParameter.setName(oProcessorToDelete.getName());
				oProcessorParameter.setProcessorID(oProcessorToDelete.getProcessorId());
				oProcessorParameter.setWorkspace(oWorkspace.getWorkspaceId());
				oProcessorParameter.setUserId(sUserId);
				oProcessorParameter.setExchange(sWorkspaceId);
				oProcessorParameter.setProcessObjId(sProcessObjId);
				oProcessorParameter.setJson("{}");
				oProcessorParameter.setProcessorType(oProcessorToDelete.getType());
				oProcessorParameter.setSessionID(sSessionId);
				oProcessorParameter.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(oWorkspace.getWorkspaceId()));
				oProcessorParameter.setVersion(oProcessorToDelete.getVersion());
				
				Wasdi.runProcess(sUserId, sSessionId, LauncherOperations.DELETEPROCESSOR.name(), oProcessorToDelete.getName(), oProcessorParameter);		
			}
			else {
				WasdiLog.warnLog("ProcessorsResource.deleteProcessor: IMPOSSIBLE TO FIND NODE SPECIFIC WORKSPACE!!!!");
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
			
			return Response.ok().build();
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorResource.deleteProcessor error: " + oEx);
			return Response.serverError().build();
		}
	}
			
	/**
	 * Force redeploy of an application
	 * 
	 * @param sSessionId User Session
	 * @param sProcessorId Processor Id
	 * @param sWorkspaceId Workspace Id
	 * @return std http response
	 */
	@GET
	@Path("/redeploy")
	public Response redeployProcessor(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("processorId") String sProcessorId,
			@QueryParam("workspace") String sWorkspaceId) {
		WasdiLog.debugLog("ProcessorResources.redeployProcessor( Processor: " + sProcessorId + ", WS: " + sWorkspaceId + " )");
	
		try {
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser==null) {
				WasdiLog.warnLog("ProcessorResources.redeployProcessor: invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			String sUserId = oUser.getUserId();
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessorToReDeploy = oProcessorRepository.getProcessor(sProcessorId);
			
			if (oProcessorToReDeploy == null) {
				WasdiLog.warnLog("ProcessorsResource.redeployProcessor: unable to find processor");
				return Response.serverError().build();
			}
			
			if (!PermissionsUtils.canUserWriteProcessor(sUserId, oProcessorToReDeploy.getProcessorId())) {
				WasdiLog.warnLog("ProcessorsResource.redeployProcessor: user cannot write the processor");
				return Response.status(Status.FORBIDDEN).build();									
			}
						
			
			if (WasdiConfig.Current.isMainNode()) {
				// Start a thread to update all the computing nodes
				try {
					WasdiLog.debugLog("ProcessorsResource.redeployProcessor: this is the main node, starting Worker to redeploy Processor also on computing nodes");
					
					// Util to call the API on computing nodes
					RedeployProcessorWorker oRedeployWorker = new RedeployProcessorWorker();
					
					NodeRepository oNodeRepo = new NodeRepository();
					List<Node> aoNodes = oNodeRepo.getNodesList();
					
					oRedeployWorker.init(aoNodes, sSessionId, sWorkspaceId, sProcessorId, oProcessorToReDeploy.getName(), oProcessorToReDeploy.getType());
					oRedeployWorker.start();
					
					WasdiLog.debugLog("ProcessorsResource.redeployProcessor: Worker started");						
				}
				catch (Exception oEx) {
					WasdiLog.errorLog("ProcessorsResource.redeployProcessor: error starting UpdateWorker " + oEx.toString());
				}				
			}
			
			// Trigger the processor delete operation on this specific node
			WasdiLog.debugLog("ProcessorsResource.redeployProcessor: Scheduling Processor Redeploy Operation");
			
			// Get the dedicated special workpsace
			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
			Workspace oWorkspace = oWorkspaceRepository.getByNameAndNode(Wasdi.s_sLocalWorkspaceName, WasdiConfig.Current.nodeCode);			

			// Schedule the process to run the processor
			
			String sProcessObjId = Utils.getRandomName();
			
			ProcessorParameter oProcessorParameter = new ProcessorParameter();
			oProcessorParameter.setName(oProcessorToReDeploy.getName());
			oProcessorParameter.setProcessorID(oProcessorToReDeploy.getProcessorId());
			oProcessorParameter.setWorkspace(oWorkspace.getWorkspaceId());
			oProcessorParameter.setUserId(sUserId);
			oProcessorParameter.setExchange(sWorkspaceId);
			oProcessorParameter.setProcessObjId(sProcessObjId);
			oProcessorParameter.setJson("{}");
			oProcessorParameter.setProcessorType(oProcessorToReDeploy.getType());
			oProcessorParameter.setSessionID(sSessionId);
			oProcessorParameter.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(sWorkspaceId));
			
			PrimitiveResult oRes = Wasdi.runProcess(sUserId,sSessionId, LauncherOperations.REDEPLOYPROCESSOR.name(),oProcessorToReDeploy.getName(),oProcessorParameter);			
			
			if (oRes.getBoolValue()) {
				return Response.ok().build();
			}
			else {
				return Response.serverError().build();
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorResource.redeployProcessor error: " + oEx);
			return Response.serverError().build();
		}
	}
	
	/**
	 * Force the update of the lib of a processor
	 * 
	 * @param sSessionId User Session Id
	 * @param sProcessorId Processor Id
	 * @param sWorkspaceId Workspace Id
	 * @return std http response
	 */
	@GET
	@Path("/libupdate")
	public Response libraryUpdate(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("processorId") String sProcessorId,
			@QueryParam("workspace") String sWorkspaceId) {
		WasdiLog.debugLog("ProcessorResources.libraryUpdate( Processor: " + sProcessorId + ", WS: " + sWorkspaceId + " )");
		
		try {
			
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser==null) {
				WasdiLog.warnLog("ProcessorResources.libraryUpdate: invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			String sUserId = oUser.getUserId();
			
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessorToForceUpdate = oProcessorRepository.getProcessor(sProcessorId);
			
			if (oProcessorToForceUpdate == null) {
				WasdiLog.warnLog("ProcessorsResource.libraryUpdate: unable to find processor");
				return Response.serverError().build();
			}
			
			if (!PermissionsUtils.canUserWriteProcessor(sUserId, oProcessorToForceUpdate)) {
				WasdiLog.warnLog("ProcessorsResource.libraryUpdate: user cannot write the processor");
				return Response.status(Status.FORBIDDEN).build();				
			}

			if (WasdiConfig.Current.isMainNode()) {
				// In the main node: start a thread to update all the computing nodes
				try {
					WasdiLog.debugLog("ProcessorsResource.libraryUpdate: this is the main node, starting Worker to update computing nodes");
					
					//This is the main node: forward the request to other nodes
					ForceLibraryUpdateWorker oUpdateWorker = new ForceLibraryUpdateWorker();
					
					NodeRepository oNodeRepo = new NodeRepository();
					List<Node> aoNodes = oNodeRepo.getNodesList();
					
					oUpdateWorker.init(aoNodes, sSessionId, sWorkspaceId, sProcessorId);
					oUpdateWorker.start();
					
					WasdiLog.debugLog("ProcessorsResource.libraryUpdate: Worker started");						
				}
				catch (Exception oEx) {
					WasdiLog.errorLog("ProcessorsResource.libraryUpdate: error starting ForceLibraryUpdateWorker " + oEx.toString());
				}
			}

			// Schedule the process to run the processor
			String sProcessObjId = Utils.getRandomName();
			
			// Get the dedicated special workpsace
			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
			Workspace oWorkspace = oWorkspaceRepository.getByNameAndNode(Wasdi.s_sLocalWorkspaceName, WasdiConfig.Current.nodeCode);
			
			WasdiLog.debugLog("ProcessorsResource.libraryUpdate: create local operation");
			
			ProcessorParameter oProcessorParameter = new ProcessorParameter();
			oProcessorParameter.setName(oProcessorToForceUpdate.getName());
			oProcessorParameter.setProcessorID(oProcessorToForceUpdate.getProcessorId());
			oProcessorParameter.setWorkspace(oWorkspace.getWorkspaceId());
			oProcessorParameter.setUserId(sUserId);
			oProcessorParameter.setExchange(sWorkspaceId);
			oProcessorParameter.setProcessObjId(sProcessObjId);
			oProcessorParameter.setJson("{}");
			oProcessorParameter.setProcessorType(oProcessorToForceUpdate.getType());
			oProcessorParameter.setSessionID(sSessionId);
			oProcessorParameter.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(sWorkspaceId));
			
			PrimitiveResult oRes = Wasdi.runProcess(sUserId,sSessionId, LauncherOperations.LIBRARYUPDATE.name(),oProcessorToForceUpdate.getName(),oProcessorParameter);			
			
			if (oRes.getBoolValue()) {
				return Response.ok().build();
			}
			else {
				return Response.serverError().build();
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorResource.libraryUpdate error: " + oEx);
			return Response.serverError().build();
		}
	}	

	
	/**
	 * Updates the parameters of a Processor
	 * 
	 * @param oUpdatedProcessorVM Updated Processor View Mode
	 * @param sSessionId Session Id
	 * @param sProcessorId Processor Id
	 * @return std http response
	 */
	@POST
	@Path("/update")
	public Response updateProcessor(DeployedProcessorViewModel oUpdatedProcessorVM, @HeaderParam("x-session-token") String sSessionId, @QueryParam("processorId") String sProcessorId) {
		
		WasdiLog.debugLog("ProcessorResources.updateProcessor( Processor: " + sProcessorId + " )");
		
		try {
			User oUser = Wasdi.getUserFromSession(sSessionId);
			
			// Check the user
			if (oUser==null) {
				WasdiLog.warnLog("ProcessorResources.updateProcessor: invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			// Check if the processor exists
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessorToUpdate = oProcessorRepository.getProcessor(sProcessorId);
			
			if (oProcessorToUpdate == null) {
				WasdiLog.warnLog("ProcessorsResource.updateProcessor: unable to find processor");
				return Response.serverError().build();
			}
			
			// Check if the user can write the processor
			if (!PermissionsUtils.canUserWriteProcessor(oUser.getUserId(), oProcessorToUpdate)) {
				WasdiLog.warnLog("ProcessorsResource.updateProcessor: user cannot write the processor");
				return Response.status(Status.FORBIDDEN).build();				
			}
						
			// Check the JSON of parameters sample
			if (Utils.isNullOrEmpty(oUpdatedProcessorVM.getParamsSample())==false) {				
				try {
					String sDecodedJSON = URLDecoder.decode(oUpdatedProcessorVM.getParamsSample(), StandardCharsets.UTF_8.name());
					MongoRepository.s_oMapper.readTree(sDecodedJSON);
				}
				catch (Exception oJsonEx) {
					WasdiLog.errorLog("ProcessorsResource.updateProcessor: JSON Exception: " + oJsonEx.toString());
					//oResult.setStringValue("Parameter Sample is not a valid JSON");
					return Response.status(Status.BAD_REQUEST).build();
				}
			}
			
			// Update the processor data
			oProcessorToUpdate.setDescription(oUpdatedProcessorVM.getProcessorDescription());
			oProcessorToUpdate.setIsPublic(oUpdatedProcessorVM.getIsPublic());
			oProcessorToUpdate.setParameterSample(oUpdatedProcessorVM.getParamsSample());
			oProcessorToUpdate.setTimeoutMs(((long)oUpdatedProcessorVM.getMinuteTimeout())*1000l*60l);
			// NOTE: The version is handled only on server side in automatic way
			//oProcessorToUpdate.setVersion(oUpdatedProcessorVM.getProcessorVersion());
			
			Date oDate = new Date();
			oProcessorToUpdate.setUpdateDate((double)oDate.getTime());
			
			WasdiLog.debugLog("ProcessorsResource.updateProcessor:  call update");
			
			oProcessorRepository.updateProcessor(oProcessorToUpdate);
			
			WasdiLog.debugLog("ProcessorsResource.updateProcessor: Updated Processor " + sProcessorId);
			
			return Response.ok().build();
		}
		catch (Throwable oEx) {
			WasdiLog.errorLog("ProcessorResource.updateProcessor  error: " + oEx);
			return Response.serverError().build();
		}
	}	
	
	/**
	 * Updates the files of a processor
	 * 
	 * @param oInputStreamForFile Stream of the files to update
	 * @param sSessionId User Session Id
	 * @param sProcessorId Processor Id
	 * @param sWorkspaceId Workspace Id
	 * @param sInputFileName Name of the input file
	 * @return
	 */
	@POST
	@Path("/updatefiles")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response updateProcessorFiles(@FormDataParam("file") InputStream oInputStreamForFile, @HeaderParam("x-session-token") String sSessionId, 
			@QueryParam("processorId") String sProcessorId,
			@QueryParam("workspace") String sWorkspaceId,
			@QueryParam("file") String sInputFileName) {

		WasdiLog.debugLog("ProcessorsResource.updateProcessorFiles( WS: " + sWorkspaceId + ", Processor: " + sProcessorId + " filename: " + sInputFileName + " )");
		try {
			// Check User 
			User oUser = Wasdi.getUserFromSession(sSessionId);
			if (oUser==null) {
				WasdiLog.warnLog("ProcessorsResource.updateProcessorFiles: invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			// Check the processor Id
			if(Utils.isNullOrEmpty(sProcessorId) || sProcessorId.contains("\\") || sProcessorId.contains("/")) {
				WasdiLog.warnLog("ProcessorsResource.updateProcessorFiles: invalid processor name, aborting");
				return Response.status(Status.BAD_REQUEST).build();
			}
			
			// Check if the processor exists
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessorToUpdate = oProcessorRepository.getProcessor(sProcessorId);
			if (oProcessorToUpdate == null) {
				WasdiLog.warnLog("ProcessorsResource.updateProcessorFiles: unable to find processor");
				return Response.serverError().build();
			}
			
			
			// Check if the user can access the processor
			if (!PermissionsUtils.canUserWriteProcessor(oUser.getUserId(), oProcessorToUpdate)) {
				WasdiLog.warnLog("ProcessorsResource.updateProcessorFiles: user cannot access the processor");
				return Response.status(Status.FORBIDDEN).build();				
			}
						
			// Set the processor path
			java.nio.file.Path oDirPath = java.nio.file.Paths.get(PathsConfig.getProcessorFolder(oProcessorToUpdate.getName())).toAbsolutePath().normalize();
			File oProcessorPath = oDirPath.toFile();
			if (!oProcessorPath.isDirectory()) {
				WasdiLog.warnLog("ProcessorsResource.updateProcessorFiles: Processor path " + oProcessorPath.getPath() + " does not exist or is not a directory. No update, aborting");
				return Response.serverError().build();
			}
			
			// Get the name and path of the new zip
			String sFileName = sProcessorId + ".zip";
			
			if (!Utils.isNullOrEmpty(sInputFileName)) {
				sFileName = sInputFileName; 
			}
			
			// Create file
			java.nio.file.Path oFilePath = oDirPath.resolve(java.nio.file.Paths.get(sFileName)).toAbsolutePath().normalize();
			
			File oProcessorFile = oFilePath.toFile();
			
			if(oProcessorFile.exists()) {
				WasdiLog.debugLog("ProcessorsResource.updateProcessorFiles: Processor file " + sFileName + " exists. Deleting it...");
				try {
					if(!oProcessorFile.delete()) {
						WasdiLog.warnLog("ProcessorsResource.updateProcessorFiles: Could not delete existing processor file " + sFileName + " exists. aborting");
						return Response.serverError().build();
					}
				} catch (Exception oE) {
					WasdiLog.errorLog("ProcessorsResource.updateProcessorFiles: Could not delete existing processor file " + sFileName + " due to: " + oE + ", aborting");
					return Response.serverError().build();
				}
			}
			
			WasdiLog.debugLog("ProcessorsResource.updateProcessorFiles: Saving Processor file Path: " + oProcessorFile.getPath());
			
			//save uploaded file
			int iRead = 0;
			byte[] ayBytes = new byte[1024];
			
			try (OutputStream oOutputStream = new FileOutputStream(oProcessorFile)) {
				while ((iRead = oInputStreamForFile.read(ayBytes)) != -1) {
					oOutputStream.write(ayBytes, 0, iRead);
				}
				oOutputStream.flush();
				oOutputStream.close();				
			}
			
			boolean bOk = true;
			
			if (sFileName.toLowerCase().endsWith(".zip")) {
				WasdiLog.debugLog("ProcessorsResource.updateProcessorFiles: unzipping the file");
				bOk = unzipProcessor(oProcessorFile, sSessionId, sProcessorId);
			}
			else {
				WasdiLog.debugLog("ProcessorsResource.updateProcessorFiles: simple not zipped file");
			}
			
			if (bOk) {
				
				oProcessorRepository.updateProcessorDate(oProcessorToUpdate);
				
				WasdiLog.debugLog("ProcessorsResource.updateProcessorFiles: update done");
				
				if (WasdiConfig.Current.isMainNode()) {
					
					// In the main node: start a thread to update all the computing nodes
					
					try {
						WasdiLog.debugLog("ProcessorsResource.updateProcessorFiles: this is the main node, starting Worker to update computing nodes");
						
						//This is the main node: forward the request to other nodes
						UpdateProcessorFilesWorker oUpdateWorker = new UpdateProcessorFilesWorker();
						
						NodeRepository oNodeRepo = new NodeRepository();
						List<Node> aoNodes = oNodeRepo.getNodesList();
						
						oUpdateWorker.init(aoNodes, oProcessorFile.getPath(), sSessionId, sWorkspaceId, sProcessorId);
						oUpdateWorker.start();
						
						WasdiLog.debugLog("ProcessorsResource.updateProcessorFiles: Worker started");						
					}
					catch (Exception oEx) {
						WasdiLog.errorLog("ProcessorsResource.updateProcessorFiles: error starting UpdateWorker " + oEx.toString());
					}
					
				}
				else {
					
					if (oProcessorFile.getName().toUpperCase().endsWith(".ZIP")) {
						WasdiLog.debugLog("ProcessorsResource.updateProcessorFiles: this is a computing node, delete local zip file");
						
						// Computational Node: delete the zip file after update
						try {
							if (!oProcessorFile.delete()) {
								WasdiLog.debugLog("ProcessorsResource.updateProcessorFiles: can't delete local zip file on computing node");
							}
						}
						catch (Exception oEx) {
							WasdiLog.errorLog("ProcessorsResource.updateProcessorFiles: error deleting zip " + oEx);
						}						
					}
				}
				
				// Trigger the library update on this specific node
				WasdiLog.debugLog("ProcessorsResource.updateProcessorFiles: Forcing Update Lib");
				
				// Get the dedicated special workpsace
				WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
				Workspace oWorkspace = oWorkspaceRepository.getByNameAndNode(Wasdi.s_sLocalWorkspaceName, WasdiConfig.Current.nodeCode);
				
				// If we have our special workspace
				if (oWorkspace != null) {
					WasdiLog.debugLog("ProcessorsResource.updateProcessorFiles: Create updatelib operation in Workspace " + Wasdi.s_sLocalWorkspaceName);
					
					// Create the processor Parameter
					ProcessorParameter oProcessorParameter = new ProcessorParameter();
					// We set the exchange to the actual workspace
					oProcessorParameter.setExchange(sWorkspaceId);
					oProcessorParameter.setWorkspace(oWorkspace.getWorkspaceId());
					oProcessorParameter.setName(oProcessorToUpdate.getName());
					oProcessorParameter.setProcessObjId(Utils.getRandomName());
					oProcessorParameter.setProcessorID(oProcessorToUpdate.getProcessorId());
					oProcessorParameter.setProcessorType(oProcessorToUpdate.getType());
					
					// Trigger the library update in this node
					Wasdi.runProcess(oUser.getUserId(), sSessionId, LauncherOperations.LIBRARYUPDATE.name(), oProcessorToUpdate.getName(), oProcessorParameter);
					
					WasdiLog.debugLog("ProcessorsResource.updateProcessorFiles: LIBRARYUPDATE process scheduled");
				}
				else {
					WasdiLog.debugLog("ProcessorsResource.updateProcessorFiles: IMPOSSIBLE TO FIND NODE SPECIFIC WORKSPACE!!!!");
				}

			}
			else {
				WasdiLog.debugLog("ProcessorsResource.updateProcessorFiles: error in unzip");
				return Response.serverError().build();
			}

		}
		catch (Throwable oEx) {
			WasdiLog.errorLog("ProcessorsResource.updateProcessorFiles  error:" + oEx.toString());
			return Response.serverError().build();
		}
		return Response.ok().build();
	}
	
	/**
	 * Update the details of a processor 
	 * @param oUpdatedProcessorVM Updated Processor View Model
	 * @param sSessionId Session Id
	 * @param sProcessorId Processor Id
	 * @return std http response
	 */
	@POST
	@Path("/updatedetails")
	public Response updateProcessorDetails(AppDetailViewModel oUpdatedProcessorVM, @HeaderParam("x-session-token") String sSessionId,
			@QueryParam("processorId") String sProcessorId) {
		
		WasdiLog.debugLog("ProcessorResources.updateProcessorDetails( Processor: " + sProcessorId + " )");
		
		try {
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser==null) {
				WasdiLog.warnLog("ProcessorResources.updateProcessorDetails: invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessorToUpdate = oProcessorRepository.getProcessor(sProcessorId);
			
			if (oProcessorToUpdate == null) {
				WasdiLog.warnLog("ProcessorsResource.updateProcessorDetails: unable to find processor");
				return Response.serverError().build();
			}
			
			if (!PermissionsUtils.canUserWriteProcessor(oUser.getUserId(), oProcessorToUpdate)) {
				WasdiLog.warnLog("ProcessorsResource.updateProcessorDetails: user cannot access the processor");
				return Response.status(Status.FORBIDDEN).build();				
			}
			
			// MANAGE STRIPE PRODUCT
			Float fOldOnDemandPrice = oProcessorToUpdate.getOndemandPrice();
			Float fNewOnDemandPrice = oUpdatedProcessorVM.getOndemandPrice();
			
			if (fNewOnDemandPrice < 0) {
				WasdiLog.warnLog("ProcessorsResource.updateProcessorDetails: the ondemand price is a negative value. Information on Stripe won't be updated");
			} 
			else if (fOldOnDemandPrice != fNewOnDemandPrice) { // TODO: should we update those prices only if the showInStore == true? or in any case?
				// the user updated the on-demand price of the app.
				
				if (fOldOnDemandPrice > 0 && fNewOnDemandPrice == 0) {
					WasdiLog.warnLog("ProcessorsResource.updateProcessorDetails: the app has been set for free. Removing the Stripe product");

				}
				else if (fOldOnDemandPrice <= 0 && fNewOnDemandPrice > 0) {
					WasdiLog.warnLog("ProcessorsResource.updateProcessorDetails: the app has been set for sale. Adding the Stripe product");

				}
				else if (fOldOnDemandPrice > 0 && fNewOnDemandPrice > 0 && fOldOnDemandPrice != fNewOnDemandPrice) {
					WasdiLog.warnLog("ProcessorsResource.updateProcessorDetails: the price of the app changed. Updating the correspoding Stripe product");
				}
				
				
			}
						
			oProcessorToUpdate.setCategories(oUpdatedProcessorVM.getCategories());
			oProcessorToUpdate.setEmail(oUpdatedProcessorVM.getEmail());
			oProcessorToUpdate.setFriendlyName(oUpdatedProcessorVM.getFriendlyName());
			oProcessorToUpdate.setLink(oUpdatedProcessorVM.getLink());
			oProcessorToUpdate.setOndemandPrice(oUpdatedProcessorVM.getOndemandPrice());
			oProcessorToUpdate.setSubscriptionPrice(oUpdatedProcessorVM.getSubscriptionPrice());			
			Date oDate = new Date();
			oProcessorToUpdate.setUpdateDate((double)oDate.getTime());
			oProcessorToUpdate.setShowInStore(oUpdatedProcessorVM.getShowInStore());
			oProcessorToUpdate.setLongDescription(oUpdatedProcessorVM.getLongDescription());
			
			oProcessorRepository.updateProcessor(oProcessorToUpdate);
			
			return Response.ok().build();
		}
		catch (Throwable oEx) {
			WasdiLog.errorLog("ProcessorResource.updateProcessorDetails error: " + oEx.toString());
			return Response.serverError().build();
		}
	}		
	
	/**
	 * Downloads a zip with the processors files
	 * @param sSessionId User Session Id
	 * @param sTokenSessionId User Session id as query param to be used by browsers
	 * @param sProcessorId Processor Id
	 * @return File Stream
	 */
	@GET
	@Path("downloadprocessor")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadProcessor(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("token") String sTokenSessionId,
			@QueryParam("processorId") String sProcessorId)
	{			

		WasdiLog.debugLog("ProcessorsResource.downloadProcessor( processorId: " + sProcessorId);
		
		try {
			
			// Use the right token
            if( Utils.isNullOrEmpty(sSessionId) == false) {
                sTokenSessionId = sSessionId;
            }
            
			// Check authorization
			User oUser = Wasdi.getUserFromSession(sTokenSessionId);

			if (oUser == null) {
				WasdiLog.warnLog("ProcessorsResource.downloadProcessor: invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			// Check the processor
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
			
			if (oProcessor == null) {
				WasdiLog.warnLog("ProcessorsResource.downloadProcessor: processor does not exists");
				return Response.status(Status.NO_CONTENT).build();				
			}
			
			if (!PermissionsUtils.canUserAccessProcessor(oUser.getUserId(), oProcessor)) {
				WasdiLog.warnLog("ProcessorsResource.downloadProcessor: user cannot access the processor");
				return Response.status(Status.FORBIDDEN).build();				
			}
			
			String sProcessorName = oProcessor.getName();

			String sProcessorZipPath = PathsConfig.getProcessorFolder(sProcessorName) + sProcessorName + ".zip";
			java.nio.file.Path oFilePath = java.nio.file.Paths.get(sProcessorZipPath).toAbsolutePath().normalize();
			
			File oFile = oFilePath.toFile();
			
			// Create the zip and return the stream						
			return zipProcessor(oFile, oProcessor);			
		} 
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorsResource.downloadProcessor error: ", oEx);
		}
		
		return Response.serverError().build();
	}
	
	/**
	 * Add a sharing to a processor
	 * 
	 * @param sSessionId User Id
	 * @param sProcessorId Processor Id
	 * @param sUserId User to be added to the processor sharing list
	 * @return Primitive Result with boolValue = true and stringValue = Done, or false and an error description
	 */
	@PUT
	@Path("share/add")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public PrimitiveResult shareProcessor(@HeaderParam("x-session-token") String sSessionId, @QueryParam("processorId") String sProcessorId, @QueryParam("userId") String sUserId, @QueryParam("rights") String sRights) {

		WasdiLog.debugLog("ProcessorsResource.shareProcessor(ProcessorId: " + sProcessorId + ", User: " + sUserId + " )");

		// Validate Session
		User oRequesterUser = Wasdi.getUserFromSession(sSessionId);
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);

		if (oRequesterUser == null) {
			WasdiLog.warnLog("ProcessorsResource.shareProcessor: invalid session");
			oResult.setStringValue("Invalid session.");
			return oResult;
		}
		
		// Use Read By default
		if (!UserAccessRights.isValidAccessRight(sRights)) {
			sRights = UserAccessRights.READ.getAccessRight();
		}
		
		if (oRequesterUser.getUserId().equals(sUserId) && !oRequesterUser.getRole().equals(UserApplicationRole.ADMIN.name())) {
			WasdiLog.warnLog("ProcessorsResource.shareProcessor: auto sharing not so smart");
			oResult.setStringValue("Impossible to autoshare.");
			return oResult;
		}
		
		
		// Check if the processor exists and is of the user calling this API
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oValidateProcessor = oProcessorRepository.getProcessor(sProcessorId);
		
		if (oValidateProcessor == null) {
			WasdiLog.warnLog("ProcessorsResource.shareProcessor: invalid processor");
			oResult.setStringValue("Invalid processor");
			return oResult;		
		}		
		
		if (!PermissionsUtils.canUserWriteProcessor(oRequesterUser.getUserId(), oValidateProcessor)) {
			WasdiLog.warnLog("ProcessorsResource.shareProcessor: user cannot write the processor");
			oResult.setStringValue("Forbidden.");
			return oResult;
		}
		
		try {
						
			// Check the destination user
			UserRepository oUserRepository = new UserRepository();
			User oDestinationUser = oUserRepository.getUser(sUserId);
			
			if (oDestinationUser == null) {
				WasdiLog.warnLog("ProcessorsResource.shareProcessor: invalid destination user");
				oResult.setStringValue("Invalid Destination User");
				return oResult;				
			}
			
			// Check if has been already shared
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			UserResourcePermission oAlreadyExists = oUserResourcePermissionRepository.getProcessorSharingByUserIdAndProcessorId(sUserId, sProcessorId);
			
			if (oAlreadyExists != null) {
				WasdiLog.warnLog("ProcessorsResource.shareProcessor: already shared");
				oResult.setStringValue("Already shared");
				return oResult;					
			}
			
			// Create and insert the sharing
			UserResourcePermission oProcessorSharing = new UserResourcePermission();
			oProcessorSharing.setResourceType(ResourceTypes.PROCESSOR.getResourceType());
			Timestamp oTimestamp = new Timestamp(System.currentTimeMillis());
			oProcessorSharing.setOwnerId(oRequesterUser.getUserId());
			oProcessorSharing.setUserId(sUserId);
			oProcessorSharing.setResourceId(sProcessorId);
			oProcessorSharing.setCreatedBy(oRequesterUser.getUserId());
			oProcessorSharing.setCreatedDate((double) oTimestamp.getTime());
			oProcessorSharing.setPermissions(sRights);
			oUserResourcePermissionRepository.insertPermission(oProcessorSharing);
			
			WasdiLog.debugLog("ProcessorsResource.shareProcessor: Processor " + sProcessorId + " Shared from " + oRequesterUser.getUserId() + " to " + sUserId);
			
			try {
				String sTitle = "Processor " + oValidateProcessor.getName() + " Shared";
				String sMessage = "The user " + oRequesterUser.getUserId() +  " shared with you the processor: " + oValidateProcessor.getName();
				
				MailUtils.sendEmail(WasdiConfig.Current.notifications.sftpManagementMailSender, sUserId, sTitle, sMessage);
			}
			catch (Exception oEx) {
				WasdiLog.errorLog("ProcessorsResource.shareProcessor: notification exception " + oEx.toString());
			}				
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorsResource.shareProcessor error: " + oEx);

			oResult.setStringValue("Error in save proccess");
			oResult.setBoolValue(false);

			return oResult;
		}	

		oResult.setStringValue("Done");
		oResult.setBoolValue(true);

		return oResult;
	}
	
	
	/**
	 * Get the list of sharings of a processor
	 * @param sSessionId User Session id
	 * @param sProcessorId Processor Id
	 * @return List of Processor Sharing View Models
	 */
	@GET
	@Path("share/byprocessor")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public List<ProcessorSharingViewModel> getEnabledUsersSharedProcessor(@HeaderParam("x-session-token") String sSessionId, @QueryParam("processorId") String sProcessorId) {

		WasdiLog.debugLog("ProcessorsResource.getEnabledUsersSharedProcessor( Processor: " + sProcessorId + " )");

		// Validate Session
		User oOwnerUser = Wasdi.getUserFromSession(sSessionId);
		

		if (oOwnerUser == null) {
			WasdiLog.warnLog("ProcessorsResource.getEnabledUsersSharedProcessor: invalid session");
			return null;
		}
		
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
		
		if (oProcessor == null) {
			WasdiLog.warnLog("ProcessorsResource.getEnabledUsersSharedProcessor: Unable to find processor return");
			return null;
		}
		
		if (!PermissionsUtils.canUserAccessProcessor(oOwnerUser.getUserId(), oProcessor)) {
			WasdiLog.warnLog("ProcessorsResource.getEnabledUsersSharedProcessor: the user cannot access the processor");
			return null;			
		}
		
		ArrayList<ProcessorSharingViewModel> aoReturnList = new ArrayList<ProcessorSharingViewModel>();

		try {
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			
			List<UserResourcePermission> aoProcessorSharing = oUserResourcePermissionRepository.getProcessorSharingsByProcessorId(sProcessorId);
			
			for (UserResourcePermission oSharing : aoProcessorSharing) {
				ProcessorSharingViewModel oVM = new ProcessorSharingViewModel();
				oVM.setUserId(oSharing.getUserId());
				oVM.setPermissions(oSharing.getPermissions());
				aoReturnList.add(oVM);
			}
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorsResource.getEnabledUsersSharedProcessor error: " + oEx);
			return aoReturnList;
		}

		return aoReturnList;

	}
	
	/**
	 * Deletes a sharing from a processor
	 * 
	 * @param sSessionId User Session Id
	 * @param sProcessorId Processor Id
	 * @param sUserId Id of the user to be removed from the sharings
	 * 
	 * @return  Primitive Result with boolValue = true and stringValue = Done, or false and an error description
	 */
	@DELETE
	@Path("share/delete")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public PrimitiveResult deleteUserSharingProcessor(@HeaderParam("x-session-token") String sSessionId, @QueryParam("processorId") String sProcessorId, @QueryParam("userId") String sUserId) {

		WasdiLog.debugLog("ProcessorsResource.deleteUserSharedProcessor( ProcId: " + sProcessorId + ", User:" + sUserId + " )");
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);
		try {
			// Validate Session
			User oRequestingUser = Wasdi.getUserFromSession(sSessionId);

			if (oRequestingUser == null) {
				WasdiLog.warnLog("ProcessorsResource.deleteUserSharedProcessor: invalid session");
				oResult.setStringValue("Invalid session");
				return oResult;
			}

			if (Utils.isNullOrEmpty(sUserId)) {
				WasdiLog.warnLog("ProcessorsResource.deleteUserSharedProcessor: invalid target user");
				oResult.setStringValue("Invalid shared user.");
				return oResult;			
			}
			
			if (!PermissionsUtils.canUserWriteProcessor(oRequestingUser.getUserId(), sProcessorId)) {
				WasdiLog.warnLog("ProcessorsResource.deleteUserSharedProcessor: user cannot access processor");
				oResult.setStringValue("Fobridden.");
				return oResult;							
			}

			try {
				UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
				UserResourcePermission oProcShare = oUserResourcePermissionRepository.getProcessorSharingByUserIdAndProcessorId(sUserId, sProcessorId);

				if (oProcShare!= null) {
					// We already verified the write permission, so I can saefly delete
					oUserResourcePermissionRepository.deletePermissionsByUserIdAndProcessorId(sUserId, sProcessorId);
				}
				else {
					oResult.setStringValue("Sharing not found");
					return oResult;				
				}
			} 
			catch (Exception oEx) {
				WasdiLog.errorLog("ProcessorsResource.deleteUserSharedProcessor: " + oEx);
				oResult.setStringValue("Error deleting processor sharing");
				return oResult;
			}

			oResult.setStringValue("Done");
			oResult.setBoolValue(true);
		} catch (Exception oE) {
			WasdiLog.errorLog("ProcessorsResource.deleteUserSharedProcessor error: " + oE);
		}
		return oResult;
	}
	
	
	/**
	 * Get the json ui representation of a processor 
	 * 
	 * @param sSessionId User Session Id
	 * @param sName Processor Name
	 * @return Json representation of the UI
	 * @throws Exception
	 */
	@GET
	@Path("/ui")
	public Response getUI(@HeaderParam("x-session-token") String sSessionId, @QueryParam("name") String sName) throws Exception {
		WasdiLog.debugLog("ProcessorsResource.getUI( Name: " + sName + " )");
		sName = URLDecoder.decode(sName, StandardCharsets.UTF_8.name());
		
		try {
			// Check User 
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser==null) {
				WasdiLog.warnLog("ProcessorsResource.getUI: session invalid");
				return Response.status(Status.UNAUTHORIZED).build();
			}
						
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessor = oProcessorRepository.getProcessorByName(sName);

			if (oProcessor == null) {
				WasdiLog.warnLog("ProcessorsResource.getUI: processor invalid");
				return Response.status(Status.BAD_REQUEST).build();
			}
			
			if (!PermissionsUtils.canUserAccessProcessor(oUser.getUserId(), oProcessor)) {
				WasdiLog.warnLog("ProcessorsResource.getUI: user cannot access the processor");
				return Response.status(Status.FORBIDDEN).build();				
			}

			ProcessorUIRepository oProcessorUIRepository = new ProcessorUIRepository();			
			ProcessorUI oUI = oProcessorUIRepository.getProcessorUI(oProcessor.getProcessorId());
			
			String sJsonUI = "{\"tabs\":[]}";
			
			if (oUI != null) {
				sJsonUI = URLDecoder.decode(oUI.getUi(), StandardCharsets.UTF_8.toString());
			}
			
			return Response.status(Status.OK).entity(sJsonUI).build();
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorsResource.getUI error: " + oEx);
			return Response.serverError().build();
		}
		
	}
	
	/**
	 * Updates the UI of a processor
	 * 
	 * @param sSessionId User Session Id
	 * @param sName Processor Name
	 * @param sUIJson JSON representation of the UI in the body
	 * @return
	 * @throws Exception
	 */
	@POST
	@Path("/saveui")
	public Response saveUI(@HeaderParam("x-session-token") String sSessionId, @QueryParam("name") String sName, String sUIJson) throws Exception {
		WasdiLog.debugLog("ProcessorsResource.saveUI( Name: " + sName + " )");
		
		try {
			// Check User 
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser==null) {
				WasdiLog.warnLog("ProcessorsResource.saveUI: session invalid");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessor = oProcessorRepository.getProcessorByName(sName);
			
			if (oProcessor == null) {
				WasdiLog.warnLog("ProcessorsResource.saveUI: processor invalid");
				return Response.status(Status.BAD_REQUEST).build();
			}
			
			if (!PermissionsUtils.canUserWriteProcessor(oUser.getUserId(), oProcessor)) {
				WasdiLog.warnLog("ProcessorsResource.saveUI: user cannot write the processor");
				return Response.status(Status.FORBIDDEN).build();				
			}			
			
			if (Utils.isNullOrEmpty(sUIJson)==false) {				
				try {
					
					String sDecodedJSON = URLDecoder.decode(sUIJson, StandardCharsets.UTF_8.name());
					MongoRepository.s_oMapper.readTree(sDecodedJSON);
				}
				catch (Exception oJsonEx) {
					
					WasdiLog.errorLog("ProcessorsResource.saveUI: JSON Exception: " + oJsonEx.toString());
					//oResult.setStringValue("Parameter Sample is not a valid JSON");
					return Response.status(Status.BAD_REQUEST).build();
				}
			}			
			
			ProcessorUIRepository oProcessorUIRepository = new ProcessorUIRepository();			
			ProcessorUI oUI = oProcessorUIRepository.getProcessorUI(oProcessor.getProcessorId());
			
			String sEncodedUI = StringUtils.encodeUrl(sUIJson);
			
			if (oUI == null) {
				oUI = new ProcessorUI();
				oUI.setProcessorId(oProcessor.getProcessorId());
				oUI.setUi(sEncodedUI);
				oProcessorUIRepository.insertProcessorUI(oUI);
				WasdiLog.debugLog("ProcessorsResource.saveUI: UI of " + sName + " created");
			}
			else {
				oUI.setUi(sEncodedUI);
				oProcessorUIRepository.updateProcessorUI(oUI);
				WasdiLog.debugLog("ProcessorsResource.saveUI: UI of " + sName + " updated");
			}
						
			return Response.status(Status.OK).build();
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorsResource.saveUI error: " + oEx);
			return Response.serverError().build();
		}
	}
	
	@GET
	@Path("getcwl")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getCWLDescriptor(@QueryParam("processorName") String sProcessorName)
	{			

		WasdiLog.debugLog("ProcessorsResource.getCWLDescriptor processorName: " + sProcessorName);
		
		try {
						
			// Check the processor
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessor = oProcessorRepository.getProcessorByName(sProcessorName);
			
			if (oProcessor == null) {
				WasdiLog.warnLog("ProcessorsResource.getCWLDescriptor: processor does not exists");
				return Response.status(Status.NO_CONTENT).build();				
			}
			
			// Take path
			String sCWLFile = PathsConfig.getProcessorFolder(sProcessorName) + sProcessorName + ".cwl";
			File oFile = new File(sCWLFile);
			WasdiLog.debugLog("CatalogResources.getCWLDescriptor: file " + sCWLFile);
			
			if (!oFile.exists()) {
				WasdiLog.warnLog("CatalogResources.getCWLDescriptor: unable to find the file");
				return Response.status(Status.NOT_FOUND).build();
			}
			
			ResponseBuilder oResponseBuilder = null;
			
			FileStreamingOutput oStream = new FileStreamingOutput(oFile);
			WasdiLog.debugLog("CatalogResources.getCWLDescriptor: file ok return content");
			oResponseBuilder = Response.ok(oStream);
			oResponseBuilder.header("Content-Disposition", "attachment; filename="+ oFile.getName());
			oResponseBuilder.header("Access-Control-Expose-Headers", "Content-Disposition");
									
			return oResponseBuilder.build();			
		} 
		catch (Exception oEx) {
			WasdiLog.errorLog("CatalogResources.getCWLDescriptor error: " + oEx);
		}
		
		return Response.serverError().build();
	}			
	
	@GET
	@Path("/logs/build")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response getProcessorBuildLogs(@HeaderParam("x-session-token") String sSessionId, @QueryParam("processorId") String sProcessorId) {
		try {
			// Check User 
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser==null) {
				WasdiLog.warnLog("ProcessorsResource.getProcessorBuildLogs: invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			if (!PermissionsUtils.canUserAccessProcessor(oUser.getUserId(), sProcessorId)) {
				WasdiLog.warnLog("ProcessorsResource.getProcessorBuildLogs: user cannot access the processor");
				return Response.status(Status.FORBIDDEN).build();				
			}
			
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
			
			if (oProcessor == null) {
				WasdiLog.warnLog("ProcessorsResource.getProcessorBuildLogs: processor does not exists");
				return Response.status(Status.BAD_REQUEST).build();				
			}
			
			ArrayList<String> aoReturnLogs = new ArrayList<>();
			
			if (oProcessor.getBuildLogs() != null) {
				for (int i=0; i<oProcessor.getBuildLogs().size(); i++) {
					aoReturnLogs.add(oProcessor.getBuildLogs().get(i));
				}
			}
			
			return Response.ok(aoReturnLogs).build();
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorResource.addLog error: " + oEx);
			return Response.serverError().build();
		}
		
	 }	
	
	/**
	 * Detects if the Name of a processor is already in use or not
	 * @param sProcessorName Name of the processor to check
	 * @return True if the name has not been used yet, False if the name is already in use
	 */
	private boolean isNameUnique(String sProcessorName) {
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessorByName(sProcessorName.toLowerCase().trim());

		return oProcessor == null;
	}
	
	
	/**
	 * Zip a full processor excluding the files contained in the relative type template folder
	 * and 
	 * @param oInitialFile
	 * @return
	 */
	private Response zipProcessor(File oInitialFile, Processor oProcessor) {
		try {
			// Create a stack of files
			Stack<File> aoFileStack = new Stack<File>();
			String sBasePath = oInitialFile.getParent();

			WasdiLog.debugLog("ProcessorsResource.zipProcessor: sBasePath = " + sBasePath);

			// Get the processor folder
			File oFile = new File(sBasePath);
			aoFileStack.push(oFile);

			if(!sBasePath.endsWith("/") && !sBasePath.endsWith("\\")) {
				sBasePath = sBasePath + "/";
			}

			int iBaseLen = sBasePath.length();
			
			String sProcTemplatePath = WasdiConfig.Current.paths.dockerTemplatePath;			
			if (!sProcTemplatePath.endsWith(File.separator)) sProcTemplatePath += File.separator;
			sProcTemplatePath += ProcessorTypes.getTemplateFolder(oProcessor.getType()) + File.separator;

			ArrayList<String> asTemplateFiles = new ArrayList<String>();
			File oProcTemplateFolder = new File(sProcTemplatePath);

			WasdiLog.debugLog("ProcessorsResource.zipProcessor: Proc Template Path " + sProcTemplatePath);

			File[] aoTemplateChildren = oProcTemplateFolder.listFiles();
			
			if (aoTemplateChildren != null) {
				for (File oChild : aoTemplateChildren) {
					asTemplateFiles.add(oChild.getName());
				}
			}
			
			ProcessorTypeConfig oProcessorTypeConfig = WasdiConfig.Current.dockers.getProcessorTypeConfig(oProcessor.getType()); 
			ArrayList<String> asAdditionalFilter = new ArrayList<>();
			
			if (oProcessorTypeConfig != null) {
				if (oProcessorTypeConfig.templateFilesToExcludeFromDownload != null) {
					asAdditionalFilter.addAll(oProcessorTypeConfig.templateFilesToExcludeFromDownload);
				}
			}
			
			if (asAdditionalFilter.size()>0) {
				WasdiLog.debugLog("ProcessorsResource.zipProcessor: adding more proc type filter file names ");
				asTemplateFiles.addAll(asAdditionalFilter);
			}
			
			// Create a map of the files to zip
			Map<String, File> aoFileEntries = new HashMap<>();

			while(aoFileStack.size()>=1) {

				oFile = aoFileStack.pop();
				String sAbsolutePath = oFile.getAbsolutePath();

				WasdiLog.debugLog("ProcessorsResource.zipProcessor: sAbsolute Path " + sAbsolutePath);

				if(oFile.isDirectory()) {
					WasdiLog.debugLog("ProcessorsResource.zipProcessor: is a folder");
					
					if(!sAbsolutePath.endsWith("/") && !sAbsolutePath.endsWith("\\")) {
						sAbsolutePath = sAbsolutePath + "/";
					}
					File[] aoChildren = oFile.listFiles();
					for (File oChild : aoChildren) {

						if (!asTemplateFiles.contains(oChild.getName())) {
							aoFileStack.push(oChild);
						}
						else {
							WasdiLog.debugLog("ProcessorsResource.zipProcessor: jumping template file " + oChild.getName());
						}

					}
				}

				if (sAbsolutePath.length()>=iBaseLen) {
					String sRelativePath = sAbsolutePath.substring(iBaseLen);

					if (!Utils.isNullOrEmpty(sRelativePath)) {
						WasdiLog.debugLog("ProcessorsResource.zipProcessor: adding file " + sRelativePath +" for compression");
						aoFileEntries.put(sRelativePath,oFile);				
					}
					else {
						WasdiLog.debugLog("ProcessorsResource.zipProcessor: jumping empty file");
					}
					
				}
			}

			WasdiLog.debugLog("ProcessorsResource.zipProcessor: done preparing map, added " + aoFileEntries.size() + " files");

			ZipStreamingOutput oStream = new ZipStreamingOutput(aoFileEntries);

			// Set response headers and return 
			ResponseBuilder oResponseBuilder = Response.ok(oStream);
			String sFileName = oInitialFile.getName();

			WasdiLog.debugLog("ProcessorsResource.zipProcessor: sFileName " + sFileName);

			oResponseBuilder.header("Content-Disposition", "attachment; filename=\""+ sFileName +"\"");
			Long lLength = 0L;
			for (String sFile : aoFileEntries.keySet()) {
				File oTempFile = aoFileEntries.get(sFile);
				if(!oTempFile.isDirectory()) {
					//NOTE: this way we are cheating, it is an upper bound, not the real size!
					lLength += oTempFile.length();
				}
			}
			//oResponseBuilder.header("Content-Length", lLength);
			WasdiLog.debugLog("ProcessorsResource.zipProcessor: done");
			return oResponseBuilder.build();
		} catch (Exception oE) {
			WasdiLog.errorLog("ProcessorsResource.zipProcessor error: " + oE);
		}
		return Response.serverError().build();
	}	

    /**
     * Unzip a processor
     * 
     * @param oProcessorZipFile Processor Zip File
     * @param sSessionId User Session Id
     * @param sProcessorId Processor Id
     * @return true if ok, false otherwise
     */
	private boolean unzipProcessor(File oProcessorZipFile, String sSessionId, String sProcessorId) {
		try {

			//get containing dir 
			String sProcessorFolder = oProcessorZipFile.getParent();
			sProcessorFolder += File.separator;

			//unzip
			try {
				ZipFileUtils oZipExtractor = new ZipFileUtils(sSessionId + " : " + sProcessorId);
				String sTmpFolder = oZipExtractor.unzip(oProcessorZipFile.getCanonicalPath(), sProcessorFolder);
				WasdiLog.debugLog("ProcessorsResource.unzipProcessor: temporary dir: " + sTmpFolder );
			} catch (IOException | SecurityException oE) {
				WasdiLog.errorLog("ProcessorsResource.unzipProcessor: unzip failed: " + oE);
				return false;
			}

			//find files that need renaming
			List<File> aoFileList = new ArrayList<>();
			try {
				java.nio.file.Path oFolderPath = java.nio.file.Paths.get(sProcessorFolder);
				try(Stream<java.nio.file.Path> aoPathStream = Files.walk(oFolderPath)){
					aoPathStream.map(java.nio.file.Path::toFile)
					.forEach(aoFileList::add);
				}
			} catch (IOException | InvalidPathException | SecurityException oE) {
				WasdiLog.errorLog("ProcessorsResource.unzipProcessor: finding files that need renaming failed: " + oE);
				return false;
			}

			//rename those files
			try {
				for (File oFile: aoFileList) {
					if (oFile.getCanonicalPath().toUpperCase().endsWith(".PRO") && !oFile.isDirectory()) {
						String sNewPath = oFile.getCanonicalPath();
						sNewPath = sNewPath.substring(0, sNewPath.length() - 4);
						sNewPath += ".pro";
						// Force extension case
						String sOld = oFile.getCanonicalPath();
						java.nio.file.Path oMovePath = Files.move(new File(sOld).toPath(), new File(sNewPath).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
						WasdiLog.debugLog("ProcessorsResource.unzipProcessor: renaming from " + sOld + " to " + oMovePath.toString());
					}
				}
			} catch (IOException oE) {
				WasdiLog.errorLog("ProcessorsResource.unzipProcessor: renaming failed: " + oE);
				return false;
			}
			return true;
		} catch (Exception oE) {
			WasdiLog.errorLog("ProcessorsResource.unzipProcessor error: " + oE);
		}
		return false;
	}
	
		

}
