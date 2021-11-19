package it.fadeout.rest.resources;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
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
import it.fadeout.business.ImageResourceUtils;
import it.fadeout.mercurius.business.Message;
import it.fadeout.mercurius.client.MercuriusAPI;
import it.fadeout.rest.resources.largeFileDownload.ZipStreamingOutput;
import it.fadeout.threads.DeleteProcessorWorker;
import it.fadeout.threads.RedeployProcessorWorker;
import it.fadeout.threads.UpdateProcessorFilesWorker;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.AppCategory;
import wasdi.shared.business.Counter;
import wasdi.shared.business.Node;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.Processor;
import wasdi.shared.business.ProcessorLog;
import wasdi.shared.business.ProcessorSharing;
import wasdi.shared.business.ProcessorTypes;
import wasdi.shared.business.ProcessorUI;
import wasdi.shared.business.Review;
import wasdi.shared.business.User;
import wasdi.shared.business.Workspace;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.AppsCategoriesRepository;
import wasdi.shared.data.CounterRepository;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.NodeRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorLogRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.data.ProcessorSharingRepository;
import wasdi.shared.data.ProcessorUIRepository;
import wasdi.shared.data.ReviewRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.ZipExtractor;
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
		
		Utils.debugLog("ProcessorsResource.uploadProcessor( Session: " + sSessionId + ", WS: " + sWorkspaceId + ", Name: " + sName + ", Version: " + sVersion + ", Description"
				+ sDescription + ", Type: " + sType + ", ParamsSample: " + sParamsSample + " )");
		
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);
		
		try {
			if(sName.contains("/") || sName.contains("\\")) {
				Utils.debugLog("ProcessorsResource.uploadProcessor: ( " +sSessionId + "...: " + sName + " is not a valid filename, aborting");
				oResult.setIntValue(400);
				return oResult;
			}
			
			// Check User 
			if (Utils.isNullOrEmpty(sSessionId)) {
				Utils.debugLog("ProcessorsResource.uploadProcessor: session is null or empty, aborting");
				oResult.setIntValue(401);
				return oResult;
			}
			User oUser = Wasdi.getUserFromSession(sSessionId);
			if (oUser==null) {
				Utils.debugLog("ProcessorsResource.uploadProcessor: invalid session, aborting");
				oResult.setIntValue(401);
				return oResult;
			}
			String sUserId = oUser.getUserId();
			if (Utils.isNullOrEmpty(sUserId)) {
				Utils.debugLog("ProcessorsResource.uploadProcessor: userid of user (from session) is null or empty, aborting");
				oResult.setIntValue(401);
				return oResult;
			}
			
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
				return oResult;				
			}
			
			if (Utils.isNullOrEmpty(sParamsSample)==false) {				
				try {
					MongoRepository.s_oMapper.readTree(sParamsSample);
				}
				catch (Exception oJsonEx) {
					oResult.setIntValue(500);
					oResult.setStringValue("Parameter Sample is not a valid JSON");
					return oResult;
				}
			}
			
			// Set the processor path
			String sDownloadRootPath = Wasdi.getDownloadPath();
			File oProcessorPath = new File(sDownloadRootPath+ "/processors/" + sName);
			
			// Create folders
			if (!oProcessorPath.exists()) {
				oProcessorPath.mkdirs();
			}
			else {
				// If the path exists, the name of the processor is already used
				oResult.setIntValue(500);
				oResult.setStringValue("Processor Name Already in Use");
				return oResult;
			}
			
			// Create file
			String sProcessorId =  UUID.randomUUID().toString();
			File oProcessorFile = new File(sDownloadRootPath+"/processors/" + sName + "/" + sProcessorId + ".zip");
			Utils.debugLog("ProcessorsResource.uploadProcessor: Processor file Path: " + oProcessorFile.getPath());
			
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
			
			Utils.debugLog("ProcessorsResource.uploadProcessor: Forcing Update Lib");
			
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
			
			String sPath = WasdiConfig.Current.paths.serializationPath;
			
			PrimitiveResult oRes = Wasdi.runProcess(sUserId, sSessionId, LauncherOperations.DEPLOYPROCESSOR.name(), sName, sPath, oDeployProcessorParameter);
			
			if (oRes.getBoolValue()) {
				oResult.setIntValue(200);
				oResult.setBoolValue(true);
				return oResult;
			}
			else {
				oResult.setIntValue(500);
				return oResult;				
			}
		}
		
		catch (Exception oEx) {
			Utils.debugLog("ProcessorsResource.uploadProcessor:" + oEx);
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
		Utils.debugLog("ProcessorsResource.getDeployedProcessors()");
		
		try {
			// Check User 
			if (Utils.isNullOrEmpty(sSessionId)) return aoRet;
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser==null) {
				Utils.debugLog("ProcessorsResource.getDeployedProcessors( Session: " + sSessionId + " ): invalid session");
				return aoRet;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) return aoRet;
						
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			ProcessorSharingRepository oProcessorSharingRepository = new ProcessorSharingRepository();
			List<Processor> aoDeployed = oProcessorRepository.getDeployedProcessors();
			
			for (int i=0; i<aoDeployed.size(); i++) {
				DeployedProcessorViewModel oDeployedProcessorViewModel = new DeployedProcessorViewModel();
				Processor oProcessor = aoDeployed.get(i);

				ProcessorSharing oSharing = oProcessorSharingRepository.getProcessorSharingByUserIdProcessorId(oUser.getUserId(), oProcessor.getProcessorId());

				if (oProcessor.getIsPublic() != 1) {
					if (oProcessor.getUserId().equals(oUser.getUserId()) == false) {
						if (oSharing == null) continue;
					}
				}
				
				if (oSharing != null) oDeployedProcessorViewModel.setSharedWithMe(true);
				
				oDeployedProcessorViewModel.setProcessorDescription(oProcessor.getDescription());
				oDeployedProcessorViewModel.setProcessorId(oProcessor.getProcessorId());
				oDeployedProcessorViewModel.setProcessorName(oProcessor.getName());
				oDeployedProcessorViewModel.setProcessorVersion(oProcessor.getVersion());
				oDeployedProcessorViewModel.setPublisher(oProcessor.getUserId());
				oDeployedProcessorViewModel.setParamsSample(oProcessor.getParameterSample());
				oDeployedProcessorViewModel.setIsPublic(oProcessor.getIsPublic());
				oDeployedProcessorViewModel.setType(oProcessor.getType());
				oDeployedProcessorViewModel.setMinuteTimeout((int) (oProcessor.getTimeoutMs()/60000l));
				
				aoRet.add(oDeployedProcessorViewModel);
			}
		}
		catch (Exception oEx) {
			Utils.debugLog("ProcessorsResource.getDeployedProcessors: " + oEx);
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
		Utils.debugLog("ProcessorsResource.getSingleDeployedProcessor");
		
		try {
			// Check User 
			if (Utils.isNullOrEmpty(sSessionId)) return oDeployedProcessorViewModel;
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser==null) {
				Utils.debugLog("ProcessorsResource.getSingleDeployedProcessor( Session: " + sSessionId + " ): invalid session");
				return oDeployedProcessorViewModel;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) return oDeployedProcessorViewModel;
						
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			ProcessorSharingRepository oProcessorSharingRepository = new ProcessorSharingRepository();
			Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);

			ProcessorSharing oSharing = oProcessorSharingRepository.getProcessorSharingByUserIdProcessorId(oUser.getUserId(), oProcessor.getProcessorId());

			if (oProcessor.getIsPublic() != 1) {
				if (oProcessor.getUserId().equals(oUser.getUserId()) == false) {
					if (oSharing == null) return oDeployedProcessorViewModel;
				}
			}
			
			if (oSharing != null) oDeployedProcessorViewModel.setSharedWithMe(true);
			
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
			Utils.debugLog("ProcessorsResource.getSingleDeployedProcessor: " + oEx);
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
		Utils.debugLog("ProcessorsResource.getMarketPlaceAppList");
		
		try {
			// Check User 
			if (Utils.isNullOrEmpty(sSessionId)) return aoRet;
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser==null) return aoRet;
			if (Utils.isNullOrEmpty(oUser.getUserId())) return aoRet;
						
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			ProcessorSharingRepository oProcessorSharingRepository = new ProcessorSharingRepository();
			
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

				ProcessorSharing oSharing = oProcessorSharingRepository.getProcessorSharingByUserIdProcessorId(oUser.getUserId(), oProcessor.getProcessorId());
				
				// See if this is a processor the user can access to
				if (oProcessor.getIsPublic() != 1) {
					if (oProcessor.getUserId().equals(oUser.getUserId()) == false) {
						if (oSharing == null) continue;
					}
				}
				
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
										
					if (fScore<=oFilters.getScore() && fScore != -1.0f) {
						continue;
					}
				}
				
				// Check and apply min price filter
				//if (oFilters.getMinPrice()>0) {
				//	if (oProcessor.getOndemandPrice() < oFilters.getMinPrice()) continue;
				//}
				
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
				
				if (oSharing != null || oProcessor.getUserId().equals(oUser.getUserId())) oAppListViewModel.setIsMine(true);
				else oAppListViewModel.setIsMine(false);
				
				oAppListViewModel.setProcessorDescription(oProcessor.getDescription());
				oAppListViewModel.setProcessorId(oProcessor.getProcessorId());
				oAppListViewModel.setProcessorName(oProcessor.getName());
				oAppListViewModel.setPublisher(oProcessor.getUserId());
				oAppListViewModel.setBuyed(false);
				oAppListViewModel.setPrice(oProcessor.getOndemandPrice());

				oAppListViewModel.setImgLink(ImageResourceUtils.getProcessorLogoRelativePath(oProcessor));
				
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
			Utils.debugLog("ProcessorsResource.getMarketPlaceAppList: " + oEx);
			oEx.printStackTrace();
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
		
		Utils.debugLog("ProcessorsResource.getMarketPlaceAppDetail");
		
		try {
			// Check User 
			if (Utils.isNullOrEmpty(sSessionId)) return Response.status(400).build();
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser==null) return Response.status(400).build();
			if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(400).build();
			
			
			AppDetailViewModel oAppDetailViewModel = new AppDetailViewModel();
						
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			ProcessorSharingRepository oProcessorSharingRepository = new ProcessorSharingRepository();
			Processor oProcessor = oProcessorRepository.getProcessorByName(sProcessorName);
			
			if (oProcessor==null) {
				Utils.debugLog("ProcessorsResource.getMarketPlaceAppDetail: processor is null");
				return Response.status(400).build();
			}

			// The same API is used also by the backed. The filter is on the list view.
//			if (!oProcessor.getShowInStore()) {
//				Utils.debugLog("ProcessorsResource.getAppDetailView: processor is not for the store");
//				return Response.status(400).build();				
//			}
			
			ProcessorSharing oSharing = oProcessorSharingRepository.getProcessorSharingByUserIdProcessorId(oUser.getUserId(), oProcessor.getProcessorId());

			if (oProcessor.getIsPublic() != 1) {
				if (oProcessor.getUserId().equals(oUser.getUserId()) == false) {
					if (oSharing == null) {
						return Response.status(400).build();
					}
				}
			}
			
			ReviewRepository oReviewRepository = new ReviewRepository();
			
			if (oSharing != null || oProcessor.getUserId().equals(oUser.getUserId())) oAppDetailViewModel.setIsMine(true);
			else oAppDetailViewModel.setIsMine(false);
			
			oAppDetailViewModel.setProcessorDescription(oProcessor.getDescription());
			oAppDetailViewModel.setProcessorId(oProcessor.getProcessorId());
			oAppDetailViewModel.setProcessorName(oProcessor.getName());
			oAppDetailViewModel.setPublisher(oProcessor.getUserId());
			oAppDetailViewModel.setBuyed(false);
			
			oAppDetailViewModel.setShowInStore(oProcessor.getShowInStore());
			oAppDetailViewModel.setLongDescription(oProcessor.getLongDescription());
			
			oAppDetailViewModel.setImgLink(ImageResourceUtils.getProcessorLogoRelativePath(oProcessor));
			
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
			oAppDetailViewModel.setMaxImages(ProcessorsMediaResource.IMAGE_NAMES.length);
			
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
			Utils.debugLog("ProcessorsResource.getMarketPlaceAppDetail: " + oEx);
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
			@QueryParam("parent") String sParentProcessWorkspaceId, String sEncodedJson) throws Exception {
		Utils.debugLog("ProcessorsResource.run( Name: " + sName + ", encodedJson:" + sEncodedJson + ", WS: " + sWorkspaceId + " )");
		
		Utils.debugLog("ProcessorsResource.internalRun: run@POST");
		return internalRun(sSessionId, sName, sEncodedJson, sWorkspaceId, sParentProcessWorkspaceId);
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
			@QueryParam("parent") String sParentProcessWorkspaceId) throws Exception {
		
		
		Utils.debugLog("ProcessorsResource.internalRun: run@GET");
		return internalRun(sSessionId, sName, sEncodedJson, sWorkspaceId, sParentProcessWorkspaceId);
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
	public RunningProcessorViewModel internalRun(String sSessionId, String sName, String sEncodedJson, String sWorkspaceId, String sParentProcessWorkspaceId) throws Exception {
		Utils.debugLog("ProcessorsResource.internalRun( Name: " + sName + ", encodedJson:" + sEncodedJson + ", WS: " + sWorkspaceId + " )");

		RunningProcessorViewModel oRunningProcessorViewModel = new RunningProcessorViewModel();
		
		try {
			// Check User 
			if (Utils.isNullOrEmpty(sSessionId)) return oRunningProcessorViewModel;
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser==null) {
				Utils.debugLog("ProcessorsResource.internalRun( Session: " + sSessionId + ", Name: " + sName + ", encodedJson:" + sEncodedJson + ", WS: " + sWorkspaceId + " ): invalid session");
				return oRunningProcessorViewModel;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) return oRunningProcessorViewModel;
			
			String sUserId = oUser.getUserId();
		
			Utils.debugLog("ProcessorsResource.internalRun: get Processor");
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessorToRun = oProcessorRepository.getProcessorByName(sName);
			
			if (oProcessorToRun == null) { 
				Utils.debugLog("ProcessorsResource.internalRun: unable to find processor " + sName);
				oRunningProcessorViewModel.setStatus("ERROR");
				return oRunningProcessorViewModel;
			}
			
			if (Utils.isNullOrEmpty(sEncodedJson)) {
				sEncodedJson = "%7B%7D";
			}

			// Schedule the process to run the processor
			
			String sProcessObjId = Utils.getRandomName();
			
			String sPath = WasdiConfig.Current.paths.serializationPath;

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
			
			PrimitiveResult oResult = Wasdi.runProcess(sUserId, sSessionId, oProcessorParameter.getLauncherOperation(), sName, sPath, oProcessorParameter, sParentProcessWorkspaceId);
			
			try{
				Utils.debugLog("ProcessorsResource.internalRun: create task");
				
				if (oResult.getBoolValue()==false) {
					throw new Exception();
				}
								
				oRunningProcessorViewModel.setJsonEncodedResult("");
				oRunningProcessorViewModel.setName(sName);
				oRunningProcessorViewModel.setProcessingIdentifier(oResult.getStringValue());
				oRunningProcessorViewModel.setProcessorId(oProcessorToRun.getProcessorId());
				oRunningProcessorViewModel.setStatus("CREATED");
				Utils.debugLog("ProcessorsResource.internalRun: done"); 
			}
			catch(Exception oEx){
				Utils.debugLog("ProcessorsResource.internalRun: Error scheduling the run process " + oEx);
				oRunningProcessorViewModel.setStatus(ProcessStatus.ERROR.toString());
				return oRunningProcessorViewModel;
			}
		}
		catch (Exception oEx) {
			Utils.debugLog("ProcessorsResource.internalRun: " + oEx );
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
		Utils.debugLog("ProcessorsResource.help( Name: " + sName + " )");
		PrimitiveResult oPrimitiveResult = new PrimitiveResult();
		oPrimitiveResult.setBoolValue(false);
		
		try {
			// Check User 
			if (Utils.isNullOrEmpty(sSessionId)) return oPrimitiveResult;
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser==null) {
				Utils.debugLog("ProcessorsResource.help( Session: " + sSessionId + ", Name: " + sName + " ): invalid session");
				return oPrimitiveResult;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) return oPrimitiveResult;
			
			//String sUserId = oUser.getUserId();
			
			Utils.debugLog("ProcessorsResource.help: read Processor " +sName);
			
			// Take path
			String sProcessorPath = Wasdi.getDownloadPath() + "processors/" + sName;
			java.nio.file.Path oDirPath = java.nio.file.Paths.get(sProcessorPath).toAbsolutePath().normalize();
			File oDirFile = oDirPath.toFile();

			if (!WasdiFileUtils.fileExists(oDirFile) || !oDirFile.isDirectory()) {
				Utils.debugLog("ProcessorsResource.help: directory " + oDirPath.toString() + " not found");
				return oPrimitiveResult;
			}

			String sOutputCumulativeResult = Arrays.stream(oDirFile.listFiles())
				.filter(File::isFile)
				.filter(WasdiFileUtils::isHelpFile)
				.map(File::getAbsolutePath)
				.map(WasdiFileUtils::fileToText)
				.findFirst()
				.orElseGet(() -> "");

			Utils.debugLog("ProcessorsResource.help: got help\n");
			
			oPrimitiveResult.setBoolValue(true);
			oPrimitiveResult.setStringValue(sOutputCumulativeResult);
		}
		catch (Exception oEx) {
			Utils.debugLog("ProcessorsResource.help: " + oEx);
			return oPrimitiveResult;
		}
		
		return oPrimitiveResult;
	}
	
	/**
	 * Return the status of a processor
	 * NOTE: p.campanella 06/10/2021 : this API should be the same of the one in proc ws.
	 * I think this may be used for the WPS bridge so I do not delete it now.
	 * 
	 * @param sSessionId User Session
	 * @param sProcessingId Process Workspace Id
	 * @return
	 * @throws Exception
	 */
	@GET
	@Path("/status")
	public RunningProcessorViewModel status(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("processingId") String sProcessingId) throws Exception {
		Utils.debugLog("ProcessorsResource.status( ProcessingId: " + sProcessingId + " )");
		RunningProcessorViewModel oRunning = new RunningProcessorViewModel();
		oRunning.setStatus(ProcessStatus.ERROR.toString());
		try {
			// Check User 
			if (Utils.isNullOrEmpty(sSessionId)) return oRunning;
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser==null) {
				Utils.debugLog("ProcessorsResource.status( Session: " + sSessionId + ", ProcessingId: " + sProcessingId + " ): invalid session");
				return oRunning;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) return oRunning;
			
			String sUserId = oUser.getUserId();
			//String sWorkspaceId = "";
		
			Utils.debugLog("ProcessorsResource.status: get Running Processor " + sProcessingId);
			
			// Get Process-Workspace
			ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
			ProcessWorkspace oProcessWorkspace = oProcessWorkspaceRepository.getProcessByProcessObjId(sProcessingId);
			
			// Check not null
			if (oProcessWorkspace == null) {
				Utils.debugLog("ProcessorsResource.status: impossible to find " + sProcessingId);
				return oRunning;
			}
			
			// Check if it is the right user
			if (oProcessWorkspace.getUserId().equals(sUserId) == false) {
				Utils.debugLog("ProcessorsResource.status: processing not of this user");
				return oRunning;				
			}
			
			// Check if it is a processor action
			if (!(oProcessWorkspace.getOperationType().equals(LauncherOperations.DEPLOYPROCESSOR.toString()) || oProcessWorkspace.getOperationType().equals(LauncherOperations.RUNPROCESSOR.toString())) ) {
				Utils.debugLog("ProcessorsResource.status: not a running process ");
				return oRunning;								
			}
			
			// Get the processor from the db
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessor = oProcessorRepository.getProcessor(oProcessWorkspace.getProductName());
			
			// Set name, id, running id and status
			oRunning.setName(oProcessor.getName());
			oRunning.setProcessingIdentifier(sProcessingId);
			oRunning.setProcessorId(oProcessor.getProcessorId());
			oRunning.setStatus(oProcessWorkspace.getStatus());
			
			// Is this done?
			if (oRunning.getStatus().equals(ProcessStatus.DONE.toString())) {
				// Do we have a payload?
				if (oProcessWorkspace.getPayload() != null) {
					// Give result to the caller
					oRunning.setJsonEncodedResult(oProcessWorkspace.getPayload());
				}
			}
		}
		catch (Exception oEx) {
			Utils.debugLog("ProcessorsResource.run: Error scheduling the deploy process " + oEx);
			oRunning.setStatus(ProcessStatus.ERROR.toString());
		}
		return oRunning;
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
		//Wasdi.DebugLog("ProcessorsResource.addLog( " + sSessionId + ", " + sProcessWorkspaceId + ", " + sLog + " )");
		try {
			// Check User 
			if (Utils.isNullOrEmpty(sSessionId)) {
				Utils.debugLog("ProcessorResource.addLog: 401 session id null");
				return Response.status(401).build();
			}
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser==null) {
				Utils.debugLog("ProcessorsResource.addLog( " + sSessionId + ", " + sProcessWorkspaceId + ", " + sLog + " ): invalid session");
				return Response.status(401).build();
			}
			
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				Utils.debugLog("ProcessorResource.addLog: userId null");
				return Response.status(401).build();
			}
			
			ProcessorLog oLog = new ProcessorLog();
			
			oLog.setLogDate(Utils.getFormatDate(new Date()));
			oLog.setProcessWorkspaceId(sProcessWorkspaceId);
			oLog.setLogRow(sLog);
			
			ProcessorLogRepository oProcessorLogRepository = new ProcessorLogRepository();
			String sResult = oProcessorLogRepository.insertProcessLog(oLog);
			if(!Utils.isNullOrEmpty(sResult)) {
				Utils.debugLog("ProcessorResource.addLog: added log row to processid " + sProcessWorkspaceId);
			} else {
				Utils.debugLog("ProcessorResource.addLog: failed to add log row");
			}
		}
		catch (Exception oEx) {
			Utils.debugLog("ProcessorResource.addLog" + oEx);
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
		
		Utils.debugLog("ProcessorResource.countLogs( ProcWsId: " + sProcessWorkspaceId + " )");
		int iResult = -1;
		try {
			if (Utils.isNullOrEmpty(sSessionId)) {
				Utils.debugLog("ProcessorResource.countLogs: 401 session id null");
				return iResult;
			}
			User oUser = Wasdi.getUserFromSession(sSessionId);
	
			if (oUser==null) {
				Utils.debugLog("ProcessorResource.countLogs( Session: " + sSessionId + ", ProcWsId: " + sProcessWorkspaceId + " ): invalid session");
				return iResult;
			}
			
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				Utils.debugLog("ProcessorResource.countLogs: userId null");
				return iResult;
			}
			
			Utils.debugLog("ProcessorResource.countLogs: get log count for process " + sProcessWorkspaceId);
			
			CounterRepository oCounterRepository = new CounterRepository();
			Counter oCounter = null;
			oCounter = oCounterRepository.getCounterBySequence(sProcessWorkspaceId);
			if(null == oCounter) {
				Utils.debugLog("ProcessorResource.countLogs: CounterRepository returned a null Counter");
				return iResult;
			}
			iResult = oCounter.getValue() + 1;
		} catch (Exception oEx) {
			Utils.debugLog("ProcessorResource.countLogs( " + sSessionId + ", " + sProcessWorkspaceId + " ): " + oEx);
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
		
		Utils.debugLog("ProcessorsResource.getLogs( ProcWsId: " + sProcessWorkspaceId + ", Start: " + iStartRow + ", End: " + iEndRow + " )");
		ArrayList<ProcessorLogViewModel> aoRetList = new ArrayList<>();
		try {
			
			// Check User 
			if (Utils.isNullOrEmpty(sSessionId)) {
				Utils.debugLog("ProcessorResource.getLogs: addLog: 401 session id null");
				return aoRetList;
			}
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser==null) {
				Utils.debugLog("ProcessorsResource.getLogs( Session: " + sSessionId + ", ProcWsId: " + sProcessWorkspaceId + ", Start: " + iStartRow + ", End: " + iEndRow + " ): invalid session");
				return aoRetList;
			}
			
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				Utils.debugLog("ProcessorResource.getLogs: addLog: userId null");
				return aoRetList;
			}
			
			Utils.debugLog("ProcessorResource.getLogs: get log for process " + sProcessWorkspaceId);
			
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
			Utils.debugLog("ProcessorResource.getLogs: " + oEx);
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
			@QueryParam("processorType") String sProcessorType) {
		Utils.debugLog("ProcessorResources.nodeDeleteProcessor( Session: " + sSessionId + ", Processor: " + sProcessorId + ", WS: " + sWorkspaceId + " )");
		
		try {
			
			// Check session
			if (Utils.isNullOrEmpty(sSessionId)) return Response.status(Status.UNAUTHORIZED).build();
			
			// Take user
			User oUser = Wasdi.getUserFromSession(sSessionId);

			// Check user
			if (oUser==null) {
				Utils.debugLog("ProcessorResources.nodeDeleteProcessor( Session: " + sSessionId + ", Processor: " + sProcessorId + ", WS: " + sWorkspaceId + " ): invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			// This API is allowed ONLY on computed nodes
			if (Wasdi.s_sMyNodeCode.equals("wasdi")) {
				Utils.debugLog("ProcessorsResource.nodeDeleteProcessor: this is the main node, cannot call this API here");
				return Response.status(Status.BAD_REQUEST).build();
			}			

			String sUserId = oUser.getUserId();
			
			// Schedule the process to delete the processor
			String sProcessObjId = Utils.getRandomName();
			String sPath = WasdiConfig.Current.paths.serializationPath;
						
			// Trigger the processor delete operation on this specific node
			Utils.debugLog("ProcessorsResource.nodeDeleteProcessor: this is a computing node, just execute Delete here");
			
			// Get the dedicated special workpsace
			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
			Workspace oWorkspace = oWorkspaceRepository.getByNameAndNode(Wasdi.s_sLocalWorkspaceName, Wasdi.s_sMyNodeCode);
			
			if (oWorkspace != null) {
				
				Utils.debugLog("ProcessorsResource.nodeDeleteProcessor: Create Delete Processor operation in Workspace " + Wasdi.s_sLocalWorkspaceName);
				
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
				
				Wasdi.runProcess(sUserId, sSessionId, LauncherOperations.DELETEPROCESSOR.name(), sProcessorName, sPath, oProcessorParameter);		
			}
			else {
				Utils.debugLog("ProcessorsResource.nodeDeleteProcessor: IMPOSSIBLE TO FIND NODE SPECIFIC WORKSPACE!!!!");
			}			
			
			return Response.ok().build();
		}
		catch (Exception oEx) {
			Utils.debugLog("ProcessorResource.nodeDeleteProcessor: " + oEx);
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
		Utils.debugLog("ProcessorResources.deleteProcessor( Processor: " + sProcessorId + ", WS: " + sWorkspaceId + " )");
		
		try {
			// Check the session
			if (Utils.isNullOrEmpty(sSessionId)) return Response.status(Status.UNAUTHORIZED).build();
			User oUser = Wasdi.getUserFromSession(sSessionId);
			
			// Check the user
			if (oUser==null) {
				Utils.debugLog("ProcessorResources.deleteProcessor( Session: " + sSessionId + ", Processor: " + sProcessorId + ", WS: " + sWorkspaceId + " ): invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			// This API is allowed ONLY on the main node
			if (!Wasdi.s_sMyNodeCode.equals("wasdi")) {
				Utils.debugLog("ProcessorsResource.deleteProcessor: this is not the main node, cannot call this API here");
				return Response.status(Status.BAD_REQUEST).build();
			}			

			String sUserId = oUser.getUserId();
			
			// Get the processor from the Db
			Utils.debugLog("ProcessorsResource.deleteProcessor: get Processor");
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessorToDelete = oProcessorRepository.getProcessor(sProcessorId);
			
			// At the first call, it must exists
			if (oProcessorToDelete == null) {
				Utils.debugLog("ProcessorsResource.deleteProcessor: unable to find processor " + sProcessorId);
				return Response.serverError().build();
			}
			
			// Is the user requesting the owner of the processor?
			if (!oProcessorToDelete.getUserId().equals(oUser.getUserId())) {
				Utils.debugLog("ProcessorsResource.deleteProcessor: processor not of user " + oUser.getUserId());
				
				// Is this a sharing?				
				ProcessorSharingRepository oProcessorSharingRepository = new ProcessorSharingRepository();
				ProcessorSharing oSharing = oProcessorSharingRepository.getProcessorSharingByUserIdProcessorId(oUser.getUserId(), sProcessorId);
				
				if (oSharing != null) {
					
					// Delete the share
					Utils.debugLog("ProcessorsResource.deleteProcessor: the processor wasd shared with " + oUser.getUserId() + ", delete the sharing");
					oProcessorSharingRepository.deleteByUserIdProcessorId(oUser.getUserId(), sProcessorId);
					
					return Response.ok().build();
				}
				
				// No, so unauthorized
				return Response.status(Status.UNAUTHORIZED).build();
			}

			// Schedule the process to delete the processor
			String sProcessObjId = Utils.getRandomName();
			String sPath = WasdiConfig.Current.paths.serializationPath;
			
			// Start a thread to update all the computing nodes
			try {
				Utils.debugLog("ProcessorsResource.deleteProcessor: this is the main node, starting Worker to delete Processor also on computing nodes");
				
				// Util to call the API on computing nodes
				DeleteProcessorWorker oDeleteWorker = new DeleteProcessorWorker();
				
				NodeRepository oNodeRepo = new NodeRepository();
				List<Node> aoNodes = oNodeRepo.getNodesList();
				
				oDeleteWorker.init(aoNodes, sSessionId, sWorkspaceId, sProcessorId, oProcessorToDelete.getName(), oProcessorToDelete.getType());
				oDeleteWorker.start();
				
				Utils.debugLog("ProcessorsResource.deleteProcessor: Worker started");						
			}
			catch (Exception oEx) {
				Utils.debugLog("ProcessorsResource.deleteProcessor: error starting UpdateWorker " + oEx.toString());
			}
			
			// Trigger the processor delete operation on this specific node
			Utils.debugLog("ProcessorsResource.deleteProcessor: Scheduling Processor Delete Operation");
			
			// Get the dedicated special workpsace
			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
			Workspace oWorkspace = oWorkspaceRepository.getByNameAndNode(Wasdi.s_sLocalWorkspaceName, Wasdi.s_sMyNodeCode);
			
			if (oWorkspace != null) {
				
				Utils.debugLog("ProcessorsResource.deleteProcessor: Create Delete Processor operation in Workspace " + Wasdi.s_sLocalWorkspaceName);
				
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
				
				Wasdi.runProcess(sUserId, sSessionId, LauncherOperations.DELETEPROCESSOR.name(), oProcessorToDelete.getName(), sPath, oProcessorParameter);		
			}
			else {
				Utils.debugLog("ProcessorsResource.deleteProcessor: IMPOSSIBLE TO FIND NODE SPECIFIC WORKSPACE!!!!");
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
			
			return Response.ok().build();
		}
		catch (Exception oEx) {
			Utils.debugLog("ProcessorResource.deleteProcessor: " + oEx);
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
		Utils.debugLog("ProcessorResources.redeployProcessor( Processor: " + sProcessorId + ", WS: " + sWorkspaceId + " )");
	
		try {
			if (Utils.isNullOrEmpty(sSessionId)) return Response.status(Status.UNAUTHORIZED).build();
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser==null) {
				Utils.debugLog("ProcessorResources.redeployProcessor( Session: " + sSessionId + ", Processor: " + sProcessorId + ", WS: " + sWorkspaceId + " ): invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(Status.UNAUTHORIZED).build();

			String sUserId = oUser.getUserId();
			
			Utils.debugLog("ProcessorsResource.redeployProcessor: get Processor");	
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessorToReDeploy = oProcessorRepository.getProcessor(sProcessorId);
			
			if (oProcessorToReDeploy == null) {
				Utils.debugLog("ProcessorsResource.redeployProcessor: unable to find processor " + sProcessorId);
				return Response.serverError().build();
			}
			
			if (!oProcessorToReDeploy.getUserId().equals(oUser.getUserId())) {
				
				ProcessorSharingRepository oSharingRepo = new ProcessorSharingRepository();
				
				ProcessorSharing oSharing = oSharingRepo.getProcessorSharingByUserIdProcessorId(oUser.getUserId(), sProcessorId);
				
				if (oSharing == null) {
					Utils.debugLog("ProcessorsResource.redeployProcessor: processor not of user " + oProcessorToReDeploy.getUserId());
					return Response.status(Status.UNAUTHORIZED).build();					
				}
				else {
					Utils.debugLog("ProcessorsResource.redeployProcessor: processor shared with user " + oUser.getUserId());
				}
			}
			
			
			if (Wasdi.s_sMyNodeCode == "wasdi") {
				// Start a thread to update all the computing nodes
				try {
					Utils.debugLog("ProcessorsResource.redeployProcessor: this is the main node, starting Worker to redeploy Processor also on computing nodes");
					
					// Util to call the API on computing nodes
					RedeployProcessorWorker oRedeployWorker = new RedeployProcessorWorker();
					
					NodeRepository oNodeRepo = new NodeRepository();
					List<Node> aoNodes = oNodeRepo.getNodesList();
					
					oRedeployWorker.init(aoNodes, sSessionId, sWorkspaceId, sProcessorId, oProcessorToReDeploy.getName(), oProcessorToReDeploy.getType());
					oRedeployWorker.start();
					
					Utils.debugLog("ProcessorsResource.redeployProcessor: Worker started");						
				}
				catch (Exception oEx) {
					Utils.debugLog("ProcessorsResource.redeployProcessor: error starting UpdateWorker " + oEx.toString());
				}				
			}
			
			// Trigger the processor delete operation on this specific node
			Utils.debugLog("ProcessorsResource.redeployProcessor: Scheduling Processor Redeploy Operation");
			
			// Get the dedicated special workpsace
			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
			Workspace oWorkspace = oWorkspaceRepository.getByNameAndNode(Wasdi.s_sLocalWorkspaceName, Wasdi.s_sMyNodeCode);			

			// Schedule the process to run the processor
			
			String sProcessObjId = Utils.getRandomName();
			
			String sPath = WasdiConfig.Current.paths.serializationPath;
			
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
			
			PrimitiveResult oRes = Wasdi.runProcess(sUserId,sSessionId, LauncherOperations.REDEPLOYPROCESSOR.name(),oProcessorToReDeploy.getName(), sPath,oProcessorParameter);			
			
			if (oRes.getBoolValue()) {
				return Response.ok().build();
			}
			else {
				return Response.serverError().build();
			}
		}
		catch (Exception oEx) {
			Utils.debugLog("ProcessorResource.redeployProcessor: " + oEx);
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
		Utils.debugLog("ProcessorResources.libraryUpdate( Processor: " + sProcessorId + ", WS: " + sWorkspaceId + " )");
		
		try {
			if (Utils.isNullOrEmpty(sSessionId)) {
				Utils.debugLog("ProcessorResources.libraryUpdate( Session: " + sSessionId + ", Processor: " + sProcessorId + ", WS: " + sWorkspaceId + " ): passed a null session id");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser==null) {
				Utils.debugLog("ProcessorResources.libraryUpdate( Session: " + sSessionId + ", Processor: " + sProcessorId + ", WS: " + sWorkspaceId + " ): invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(Status.UNAUTHORIZED).build();

			String sUserId = oUser.getUserId();
			
			Utils.debugLog("ProcessorsResource.libraryUpdate: get Processor");	
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessorToForceUpdate = oProcessorRepository.getProcessor(sProcessorId);
			
			if (oProcessorToForceUpdate == null) {
				Utils.debugLog("ProcessorsResource.libraryUpdate: unable to find processor " + sProcessorId);
				return Response.serverError().build();
			}
			
			if (!oProcessorToForceUpdate.getUserId().equals(oUser.getUserId())) {
				Utils.debugLog("ProcessorsResource.libraryUpdate: processor not of user " + oProcessorToForceUpdate.getUserId());
				return Response.status(Status.UNAUTHORIZED).build();
			}

			// Schedule the process to run the processor
			
			String sProcessObjId = Utils.getRandomName();
			
			String sPath = WasdiConfig.Current.paths.serializationPath;
			
			ProcessorParameter oProcessorParameter = new ProcessorParameter();
			oProcessorParameter.setName(oProcessorToForceUpdate.getName());
			oProcessorParameter.setProcessorID(oProcessorToForceUpdate.getProcessorId());
			oProcessorParameter.setWorkspace(sWorkspaceId);
			oProcessorParameter.setUserId(sUserId);
			oProcessorParameter.setExchange(sWorkspaceId);
			oProcessorParameter.setProcessObjId(sProcessObjId);
			oProcessorParameter.setJson("{}");
			oProcessorParameter.setProcessorType(oProcessorToForceUpdate.getType());
			oProcessorParameter.setSessionID(sSessionId);
			oProcessorParameter.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(sWorkspaceId));
			
			PrimitiveResult oRes = Wasdi.runProcess(sUserId,sSessionId, LauncherOperations.LIBRARYUPDATE.name(),oProcessorToForceUpdate.getName(), sPath,oProcessorParameter);			
			
			if (oRes.getBoolValue()) {
				return Response.ok().build();
			}
			else {
				return Response.serverError().build();
			}
		}
		catch (Exception oEx) {
			Utils.debugLog("ProcessorResource.redeployProcessor: " + oEx);
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
		
		Utils.debugLog("ProcessorResources.updateProcessor( Processor: " + sProcessorId + " )");
		
		try {
			if (Utils.isNullOrEmpty(sSessionId)) {
				Utils.debugLog("ProcessorResources.updateProcessor( Session: " + sSessionId + ", Processor: " + sProcessorId + " ): session id is null");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser==null) {
				Utils.debugLog("ProcessorResources.updateProcessor( Session: " + sSessionId + ", Processor: " + sProcessorId + " ): invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(Status.UNAUTHORIZED).build();
			
			Utils.debugLog("ProcessorsResource.updateProcessor: get Processor " + sProcessorId);	
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessorToUpdate = oProcessorRepository.getProcessor(sProcessorId);
			
			if (oProcessorToUpdate == null) {
				Utils.debugLog("ProcessorsResource.updateProcessor: unable to find processor " + sProcessorId);
				return Response.serverError().build();
			}
			
			if (!oProcessorToUpdate.getUserId().equals(oUser.getUserId())) {
				ProcessorSharingRepository oProcessorSharingRepository = new ProcessorSharingRepository();				
				ProcessorSharing oSharing = oProcessorSharingRepository.getProcessorSharingByUserIdProcessorId(oUser.getUserId(), sProcessorId);
				
				if (oSharing == null) {
					Utils.debugLog("ProcessorsResource.updateProcessor: processor not of user " + oUser.getUserId());
					return Response.status(Status.FORBIDDEN).build();					
				}
				else {
					Utils.debugLog("ProcessorsResource.updateProcessor: processor of user " + oProcessorToUpdate.getUserId() + " is shared with " + oUser.getUserId());
				}
			}
			
			if (Utils.isNullOrEmpty(oUpdatedProcessorVM.getParamsSample())==false) {				
				try {
					String sDecodedJSON = java.net.URLDecoder.decode(oUpdatedProcessorVM.getParamsSample(), StandardCharsets.UTF_8.name());
					MongoRepository.s_oMapper.readTree(sDecodedJSON);
				}
				catch (Exception oJsonEx) {
					Utils.debugLog("ProcessorsResource.updateProcessor: JSON Exception: " + oJsonEx.toString());
					//oResult.setStringValue("Parameter Sample is not a valid JSON");
					return Response.status(422).build();
				}
			}
			
			oProcessorToUpdate.setDescription(oUpdatedProcessorVM.getProcessorDescription());
			oProcessorToUpdate.setIsPublic(oUpdatedProcessorVM.getIsPublic());
			oProcessorToUpdate.setParameterSample(oUpdatedProcessorVM.getParamsSample());
			oProcessorToUpdate.setTimeoutMs(((long)oUpdatedProcessorVM.getMinuteTimeout())*1000l*60l);
			oProcessorToUpdate.setVersion(oUpdatedProcessorVM.getProcessorVersion());
			
			Date oDate = new Date();
			oProcessorToUpdate.setUpdateDate((double)oDate.getTime());
			
			Utils.debugLog("ProcessorsResource.updateProcessor:  call update");
			
			oProcessorRepository.updateProcessor(oProcessorToUpdate);
			
			Utils.debugLog("ProcessorsResource.updateProcessor: Updated Processor " + sProcessorId);
			
			return Response.ok().build();
		}
		catch (Throwable oEx) {
			Utils.debugLog("ProcessorResource.updateProcessor Exception: " + oEx);
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

		Utils.debugLog("ProcessorsResource.updateProcessorFiles( WS: " + sWorkspaceId + ", Processor: " + sProcessorId + " filename: " + sInputFileName + " )");
		try {
			if(Utils.isNullOrEmpty(sProcessorId) || sProcessorId.contains("\\") || sProcessorId.contains("/")) {
				Utils.debugLog("ProcessorsResource.updateProcessorFiles( oInputStreamForFile, " + sSessionId + ", " + sWorkspaceId + ", " + sProcessorId + " ): invalid processor name, aborting");
				return Response.status(Status.BAD_REQUEST).build();
			}
			
			// Check User 
			if (Utils.isNullOrEmpty(sSessionId)) {
				Utils.debugLog("ProcessorsResource.updateProcessorFiles: session is null or empty, aborting");
				return Response.status(401).build();
			}
			User oUser = Wasdi.getUserFromSession(sSessionId);
			if (oUser==null) {
				Utils.debugLog("ProcessorsResource.updateProcessorFiles( oInputStreamForFile, " + sSessionId + ", " + sWorkspaceId + ", " + sProcessorId + " ): invalid session, aborting");
				return Response.status(401).build();
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				Utils.debugLog("ProcessorsResource.updateProcessorFiles: userid of user (from session) is null or empty, aborting");
				return Response.status(401).build();
			}
			
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessorToUpdate = oProcessorRepository.getProcessor(sProcessorId);
			if (oProcessorToUpdate == null) {
				Utils.debugLog("ProcessorsResource.updateProcessorFiles: unable to find processor " + sProcessorId + ", aborting");
				return Response.serverError().build();
			}
			
			//not the owner?
			if (!oProcessorToUpdate.getUserId().equals(oUser.getUserId())) {
				ProcessorSharingRepository oProcessorSharingRepository = new ProcessorSharingRepository();
				ProcessorSharing oSharing = oProcessorSharingRepository.getProcessorSharingByUserIdProcessorId(oUser.getUserId(), sProcessorId);
				
				if (oSharing == null) {
					Utils.debugLog("ProcessorsResource.updateProcessorFiles: processor not of user " + oUser.getUserId() + ", aborting");
					return Response.status(Status.FORBIDDEN).build();					
				}
				else {
					Utils.debugLog("ProcessorsResource.updateProcessorFiles: processor of user " + oProcessorToUpdate.getUserId() + " is shared with " + oUser.getUserId());
				}
			}			
			
			// Set the processor path
			String sDownloadRootPath = Wasdi.getDownloadPath();

			java.nio.file.Path oDirPath = java.nio.file.Paths.get(sDownloadRootPath + "/processors/" + oProcessorToUpdate.getName()).toAbsolutePath().normalize();
			File oProcessorPath = oDirPath.toFile();
			if (!oProcessorPath.isDirectory()) {
				Utils.debugLog("ProcessorsResource.updateProcessorFiles: Processor path " + oProcessorPath.getPath() + " does not exist or is not a directory. No update, aborting");
				return Response.serverError().build();
			}
			
			String sFileName = sProcessorId + ".zip";
			
			if (!Utils.isNullOrEmpty(sInputFileName)) {
				sFileName = sInputFileName; 
			}
			
			// Create file
			java.nio.file.Path oFilePath = oDirPath.resolve(java.nio.file.Paths.get(sFileName)).toAbsolutePath().normalize();
			
			File oProcessorFile = oFilePath.toFile();
			
			if(oProcessorFile.exists()) {
				Utils.debugLog("ProcessorsResource.updateProcessorFiles: Processor file " + sFileName + " exists. Deleting it...");
				try {
					if(!oProcessorFile.delete()) {
						Utils.debugLog("ProcessorsResource.updateProcessorFiles: Could not delete existing processor file " + sFileName + " exists. aborting");
						return Response.serverError().build();
					}
				} catch (Exception oE) {
					Utils.debugLog("ProcessorsResource.updateProcessorFiles: Could not delete existing processor file " + sFileName + " due to: " + oE + ", aborting");
					return Response.serverError().build();
				}
			}
			
			Utils.debugLog("ProcessorsResource.updateProcessorFiles: Saving Processor file Path: " + oProcessorFile.getPath());
			
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
				Utils.debugLog("ProcessorsResource.updateProcessorFiles: unzipping the file");
				bOk = unzipProcessor(oProcessorFile, sSessionId, sProcessorId);
			}
			else {
				Utils.debugLog("ProcessorsResource.updateProcessorFiles: simple not zipped file");
			}
			
			if (bOk) {
				
				oProcessorRepository.updateProcessorDate(oProcessorToUpdate);
				
				Utils.debugLog("ProcessorsResource.updateProcessorFiles: update done");
				
				if (Wasdi.s_sMyNodeCode.equals("wasdi")) {
					
					// In the main node: start a thread to update all the computing nodes
					
					try {
						Utils.debugLog("ProcessorsResource.updateProcessorFiles: this is the main node, starting Worker to update computing nodes");
						
						//This is the main node: forward the request to other nodes
						UpdateProcessorFilesWorker oUpdateWorker = new UpdateProcessorFilesWorker();
						
						NodeRepository oNodeRepo = new NodeRepository();
						List<Node> aoNodes = oNodeRepo.getNodesList();
						
						oUpdateWorker.init(aoNodes, oProcessorFile.getPath(), sSessionId, sWorkspaceId, sProcessorId);
						oUpdateWorker.start();
						
						Utils.debugLog("ProcessorsResource.updateProcessorFiles: Worker started");						
					}
					catch (Exception oEx) {
						Utils.debugLog("ProcessorsResource.updateProcessorFiles: error starting UpdateWorker " + oEx.toString());
					}
					
				}
				else {
					
					if (oProcessorFile.getName().toUpperCase().endsWith(".ZIP")) {
						Utils.debugLog("ProcessorsResource.updateProcessorFiles: this is a computing node, delete local zip file");
						
						// Computational Node: delete the zip file after update
						try {
							if (!oProcessorFile.delete()) {
								Utils.debugLog("ProcessorsResource.updateProcessorFiles: can't delete local zip file on computing node");
							}
						}
						catch (Exception oEx) {
							Utils.debugLog("ProcessorsResource.updateProcessorFiles: error deleting zip " + oEx);
						}						
					}
				}
				
				// Trigger the library update on this specific node
				Utils.debugLog("ProcessorsResource.updateProcessorFiles: Forcing Update Lib");
				
				// Get the dedicated special workpsace
				WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
				Workspace oWorkspace = oWorkspaceRepository.getByNameAndNode(Wasdi.s_sLocalWorkspaceName, Wasdi.s_sMyNodeCode);
				
				// If we have our special workspace
				if (oWorkspace != null) {
					Utils.debugLog("ProcessorsResource.updateProcessorFiles: Create updatelib operation in Workspace " + Wasdi.s_sLocalWorkspaceName);
					
					// Create the processor Parameter
					ProcessorParameter oProcessorParameter = new ProcessorParameter();
					oProcessorParameter.setExchange(oWorkspace.getWorkspaceId());
					oProcessorParameter.setWorkspace(oWorkspace.getWorkspaceId());
					oProcessorParameter.setName(oProcessorToUpdate.getName());
					oProcessorParameter.setProcessObjId(Utils.getRandomName());
					oProcessorParameter.setProcessorID(oProcessorToUpdate.getProcessorId());
					
					String sPath = WasdiConfig.Current.paths.serializationPath;
					
					// Trigger the library update in this node
					Wasdi.runProcess(oUser.getUserId(), sSessionId, LauncherOperations.LIBRARYUPDATE.name(), oProcessorToUpdate.getName(), sPath, oProcessorParameter);
					
					Utils.debugLog("ProcessorsResource.updateProcessorFiles: LIBRARYUPDATE process scheduled");
				}
				else {
					Utils.debugLog("ProcessorsResource.updateProcessorFiles: IMPOSSIBLE TO FIND NODE SPECIFIC WORKSPACE!!!!");
				}

			}
			else {
				Utils.debugLog("ProcessorsResource.updateProcessorFiles: error in unzip");
				return Response.serverError().build();
			}

		}
		catch (Throwable oEx) {
			Utils.debugLog("ProcessorsResource.updateProcessorFiles Exception:" + oEx.toString());
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
		
		Utils.debugLog("ProcessorResources.updateProcessorDetails( Processor: " + sProcessorId + " )");
		
		try {
			if (Utils.isNullOrEmpty(sSessionId)) return Response.status(Status.UNAUTHORIZED).build();
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser==null) return Response.status(Status.UNAUTHORIZED).build();
			if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(Status.UNAUTHORIZED).build();
			
			Utils.debugLog("ProcessorsResource.updateProcessorDetails: get Processor " + sProcessorId);	
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessorToUpdate = oProcessorRepository.getProcessor(sProcessorId);
			
			
			if (oProcessorToUpdate == null) {
				Utils.debugLog("ProcessorsResource.updateProcessorDetails: unable to find processor " + sProcessorId);
				return Response.serverError().build();
			}
			
			if (!oProcessorToUpdate.getUserId().equals(oUser.getUserId())) {
				
				ProcessorSharingRepository oProcessorSharingRepository = new ProcessorSharingRepository();
				
				ProcessorSharing oSharing = oProcessorSharingRepository.getProcessorSharingByUserIdProcessorId(oUser.getUserId(), sProcessorId);
				
				if (oSharing == null) {
					Utils.debugLog("ProcessorsResource.updateProcessorDetails: processor not of user " + oUser.getUserId());
					return Response.status(Status.UNAUTHORIZED).build();					
				}
				else {
					Utils.debugLog("ProcessorsResource.updateProcessorDetails: processor of user " + oProcessorToUpdate.getUserId() + " is shared with " + oUser.getUserId());
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
			
			Utils.debugLog("ProcessorsResource.updateProcessorDetails: Updated Processor " + sProcessorId);
			
			return Response.ok().build();
		}
		catch (Throwable oEx) {
			Utils.debugLog("ProcessorResource.updateProcessorDetails: " + oEx.toString());
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

		Utils.debugLog("ProcessorsResource.downloadProcessor( processorId: " + sProcessorId);
		
		try {
			
			// Check authorization
			if( Utils.isNullOrEmpty(sSessionId) == false) {
				sTokenSessionId = sSessionId;
			}
			
			User oUser = Wasdi.getUserFromSession(sTokenSessionId);

			if (oUser == null) {
				Utils.debugLog("ProcessorsResource.downloadProcessor( " + sSessionId + ", " + sProcessorId + " ): invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			// Check the processor
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
			
			if (oProcessor == null) {
				Utils.debugLog("ProcessorsResource.downloadProcessor: processor does not exists");
				return Response.status(Status.NO_CONTENT).build();				
			}
			
			String sProcessorName = oProcessor.getName();
			
			// Take path
			String sDownloadRootPath = Wasdi.getDownloadPath();
			java.nio.file.Path oDirPath = java.nio.file.Paths.get(sDownloadRootPath).toAbsolutePath().normalize();
			File oDirFile = oDirPath.toFile();
			if(!oDirFile.isDirectory()) {
				Utils.debugLog("ProcessorsResource.downloadProcessor: directory " + oDirPath.toString() + " not found");
				return Response.serverError().build();
			}

			String sProcessorZipPath = sDownloadRootPath + "processors/" + sProcessorName + "/" + sProcessorId + ".zip";
			java.nio.file.Path oFilePath = java.nio.file.Paths.get(sProcessorZipPath).toAbsolutePath().normalize();
			
			File oFile = oFilePath.toFile();
			
			// Create the zip and return the stream						
			return zipProcessor(oFile, oProcessor);			
		} 
		catch (Exception oEx) {
			Utils.debugLog("ProcessorsResource.downloadProcessor: " + oEx);
		}
		
		return Response.serverError().build();
	}		
	
	/**
	 * Zip a full processor
	 * @param oInitialFile
	 * @return
	 */
	private Response zipProcessor(File oInitialFile, Processor oProcessor) {
		try {
			// Create a stack of files
			Stack<File> aoFileStack = new Stack<File>();
			String sBasePath = oInitialFile.getParent();

			Utils.debugLog("ProcessorsResource.zipProcessor: sDir = " + sBasePath);

			// Get the processor folder
			File oFile = new File(sBasePath);
			aoFileStack.push(oFile);

			if(!sBasePath.endsWith("/") && !sBasePath.endsWith("\\")) {
				sBasePath = sBasePath + "/";
			}

			int iBaseLen = sBasePath.length();

			String sProcTemplatePath = Wasdi.getDownloadPath();
			sProcTemplatePath += "dockertemplate/";
			sProcTemplatePath += ProcessorTypes.getTemplateFolder(oProcessor.getType()) + "/";

			ArrayList<String> asTemplateFiles = new ArrayList<String>();
			File oProcTemplateFolder = new File(sProcTemplatePath);

			Utils.debugLog("ProcessorsResource.zipProcessor: Proc Template Path " + sProcTemplatePath);

			File[] aoTemplateChildren = oProcTemplateFolder.listFiles();
			for (File oChild : aoTemplateChildren) {
				asTemplateFiles.add(oChild.getName());
			}


			// Create a map of the files to zip
			Map<String, File> aoFileEntries = new HashMap<>();

			while(aoFileStack.size()>=1) {

				oFile = aoFileStack.pop();
				String sAbsolutePath = oFile.getAbsolutePath();

				Utils.debugLog("ProcessorsResource.zipProcessor: sAbsolute Path " + sAbsolutePath);

				if(oFile.isDirectory()) {
					if(!sAbsolutePath.endsWith("/") && !sAbsolutePath.endsWith("\\")) {
						sAbsolutePath = sAbsolutePath + "/";
					}
					File[] aoChildren = oFile.listFiles();
					for (File oChild : aoChildren) {

						if (!asTemplateFiles.contains(oChild.getName())) {
							aoFileStack.push(oChild);
						}
						else {
							Utils.debugLog("ProcessorsResource.zipProcessor: jumping template file " + oChild.getName());
						}

					}
				}

				String sRelativePath = sAbsolutePath.substring(iBaseLen);

				if (!Utils.isNullOrEmpty(sRelativePath)) {
					Utils.debugLog("ProcessorsResource.zipProcessor: adding file " + sRelativePath +" for compression");
					aoFileEntries.put(sRelativePath,oFile);				
				}
				else {
					Utils.debugLog("ProcessorsResource.zipProcessor: jumping empty file");
				}
			}

			Utils.debugLog("ProcessorsResource.zipProcessor: done preparing map, added " + aoFileEntries.size() + " files");

			ZipStreamingOutput oStream = new ZipStreamingOutput(aoFileEntries);

			// Set response headers and return 
			ResponseBuilder oResponseBuilder = Response.ok(oStream);
			String sFileName = oInitialFile.getName();

			Utils.debugLog("ProcessorsResource.zipProcessor: sFileName " + sFileName);

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
			Utils.debugLog("ProcessorsResource.zipProcessor: done");
			return oResponseBuilder.build();
		} catch (Exception oE) {
			Utils.debugLog("ProcessorsResource.zipProcessor: " + oE);
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
			ZipExtractor oZipExtractor = new ZipExtractor(sSessionId + " : " + sProcessorId);

			//get containing dir 
			String sProcessorFolder = oProcessorZipFile.getParent();
			sProcessorFolder += File.separator;

			//unzip
			try {
				String sTmpFolder = oZipExtractor.unzip(oProcessorZipFile.getCanonicalPath(), sProcessorFolder);
				Utils.debugLog("ProcessorsResource.unzipProcessor: temporary dir: " + sTmpFolder );
			} catch (IOException | SecurityException oE) {
				Utils.debugLog("ProcessorsResource.unzipProcessor: unzip failed: " + oE);
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
				Utils.debugLog("ProcessorsResource.unzipProcessor: finding files that need renaming failed: " + oE);
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
						Utils.debugLog("ProcessorsResource.unzipProcessor: renaming from " + sOld + " to " + oMovePath.toString());
					}
				}
			} catch (IOException oE) {
				Utils.debugLog("ProcessorsResource.unzipProcessor: renaming failed: " + oE);
				return false;
			}
			return true;
		} catch (Exception oE) {
			Utils.debugLog("ProcessorsResource.unzipProcessor: " + oE);
		}
		return false;
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
	public PrimitiveResult shareProcessor(@HeaderParam("x-session-token") String sSessionId, @QueryParam("processorId") String sProcessorId, @QueryParam("userId") String sUserId) {

		Utils.debugLog("ProcessorsResource.shareProcessor(  WS: " + sProcessorId + ", User: " + sUserId + " )");

		// Validate Session
		User oRequesterUser = Wasdi.getUserFromSession(sSessionId);
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);

		if (oRequesterUser == null) {
			Utils.debugLog("ProcessorsResource.shareProcessor( Session: " + sSessionId + ", WS: " + sProcessorId + ", User: " + sUserId + " ): invalid session");
			oResult.setStringValue("Invalid session.");
			return oResult;
		}

		if (Utils.isNullOrEmpty(oRequesterUser.getUserId())) {
			oResult.setStringValue("Invalid user.");
			return oResult;
		}
		
		if (oRequesterUser.getUserId().equals(sUserId)) {
			Utils.debugLog("ProcessorsResource.shareProcessor: auto sharing not so smart");
			oResult.setStringValue("Impossible to autoshare.");
			return oResult;				
		}
		
		try {
			
			// Check if the processor exists and is of the user calling this API
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oValidateProcessor = oProcessorRepository.getProcessor(sProcessorId);
			
			if (oValidateProcessor == null) {
				oResult.setStringValue("Invalid processor");
				return oResult;		
			}
						
			// Check the destination user
			UserRepository oUserRepository = new UserRepository();
			User oDestinationUser = oUserRepository.getUser(sUserId);
			
			if (oDestinationUser == null) {
				oResult.setStringValue("Invalid Destination User");
				return oResult;				
			}
			
			// Check if has been already shared
			ProcessorSharingRepository oProcessorSharingRepository = new ProcessorSharingRepository();
			ProcessorSharing oAlreadyExists = oProcessorSharingRepository.getProcessorSharingByUserIdProcessorId(sUserId, sProcessorId);
			
			if (oAlreadyExists != null) {
				oResult.setStringValue("Already shared");
				return oResult;					
			}
			
			// The requester is the owner?			
			if (!oValidateProcessor.getUserId().equals(oRequesterUser.getUserId())) {
				
				// Is he trying to share with the owner?
				if (oValidateProcessor.getUserId().equals(sUserId)) {
					oResult.setStringValue("Cannot Share with owner");
					return oResult;					
				}
				
				// No: the requestr has a sharing on this processor?
				ProcessorSharing oHasSharing = oProcessorSharingRepository.getProcessorSharingByUserIdProcessorId(oRequesterUser.getUserId(), sProcessorId);
				
				if (oHasSharing==null) {
					
					//No. So it is neither the owner or a shared one
					oResult.setStringValue("Unauthorized");
					return oResult;				
				}				
			}			
			
			// Create and insert the sharing
			ProcessorSharing oProcessorSharing = new ProcessorSharing();
			Timestamp oTimestamp = new Timestamp(System.currentTimeMillis());
			oProcessorSharing.setOwnerId(oRequesterUser.getUserId());
			oProcessorSharing.setUserId(sUserId);
			oProcessorSharing.setProcessorId(sProcessorId);
			oProcessorSharing.setShareDate((double) oTimestamp.getTime());
			oProcessorSharingRepository.insertProcessorSharing(oProcessorSharing);
			
			Utils.debugLog("ProcessorsResource.shareProcessor: Processor " + sProcessorId + " Shared from " + oRequesterUser.getUserId() + " to " + sUserId);
			
			try {
				String sMercuriusAPIAddress = WasdiConfig.Current.notifications.mercuriusAPIAddress;
				
				if(Utils.isNullOrEmpty(sMercuriusAPIAddress)) {
					Utils.debugLog("ProcessorsResource.shareProcessor: sMercuriusAPIAddress is null");
				}
				else {
					MercuriusAPI oAPI = new MercuriusAPI(sMercuriusAPIAddress);			
					Message oMessage = new Message();
					
					String sTitle = "Processor " + oValidateProcessor.getName() + " Shared";
					
					oMessage.setTilte(sTitle);
					
					String sSender =  WasdiConfig.Current.notifications.sftpManagementMailSender;
					if (sSender==null) {
						sSender = "wasdi@wasdi.net";
					}
					
					oMessage.setSender(sSender);
					
					String sMessage = "The user " + oRequesterUser.getUserId() +  " shared with you the processor: " + oValidateProcessor.getName();
									
					oMessage.setMessage(sMessage);
			
					Integer iPositiveSucceded = 0;
									
					iPositiveSucceded = oAPI.sendMailDirect(sUserId, oMessage);
					
					Utils.debugLog("ProcessorsResource.shareProcessor: notification sent with result " + iPositiveSucceded);
				}
					
			}
			catch (Exception oEx) {
				Utils.debugLog("ProcessorsResource.shareProcessor: notification exception " + oEx.toString());
			}				
			
		} catch (Exception oEx) {
			Utils.debugLog("ProcessorsResource.shareProcessor: " + oEx);

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
	public List<ProcessorSharingViewModel> getEnableUsersSharedProcessor(@HeaderParam("x-session-token") String sSessionId, @QueryParam("processorId") String sProcessorId) {

		Utils.debugLog("ProcessorsResource.getEnableUsersSharedProcessor( Processor: " + sProcessorId + " )");

		// Validate Session
		User oOwnerUser = Wasdi.getUserFromSession(sSessionId);
		

		if (oOwnerUser == null) {
			Utils.debugLog("ProcessorsResource.getEnableUsersSharedProcessor( Session: " + sSessionId + ", Processor: " + sProcessorId + " ): invalid session");
			return null;
		}

		if (Utils.isNullOrEmpty(oOwnerUser.getUserId())) {
			Utils.debugLog("ProcessorsResource.getEnableUsersSharedProcessor: User Id null return");
			return null;
		}
		
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
		
		if (oProcessor == null) {
			Utils.debugLog("ProcessorsResource.getEnableUsersSharedProcessor: Unable to find processor return");
			return null;
		}
		
		ArrayList<ProcessorSharingViewModel> aoReturnList = new ArrayList<ProcessorSharingViewModel>();

		try {
			ProcessorSharingRepository oProcessorSharingRepository = new ProcessorSharingRepository();
			
			List<ProcessorSharing> aoProcessorSharing = oProcessorSharingRepository.getProcessorSharingByProcessorId(sProcessorId);
			
			for (ProcessorSharing oSharing : aoProcessorSharing) {
				ProcessorSharingViewModel oVM = new ProcessorSharingViewModel();
				oVM.setUserId(oSharing.getUserId());
				aoReturnList.add(oVM);
			}
			
		} catch (Exception oEx) {
			Utils.debugLog("ProcessorsResource.getEnableUsersSharedProcessor: " + oEx);
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

		Utils.debugLog("ProcessorsResource.deleteUserSharedProcessor( ProcId: " + sProcessorId + ", User:" + sUserId + " )");
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);
		try {
			// Validate Session
			User oOwnerUser = Wasdi.getUserFromSession(sSessionId);

			if (oOwnerUser == null) {
				Utils.debugLog("ProcessorsResource.deleteUserSharedProcessor( Session: " + sSessionId + ", ProcId: " + sProcessorId + ", User:" + sUserId + " ): invalid session");
				oResult.setStringValue("Invalid session");
				return oResult;
			}

			if (Utils.isNullOrEmpty(oOwnerUser.getUserId())) {
				oResult.setStringValue("Invalid user.");
				return oResult;
			}

			if (Utils.isNullOrEmpty(sUserId)) {
				oResult.setStringValue("Invalid shared user.");
				return oResult;			
			}

			try {
				ProcessorSharingRepository oProcessorSharingRepository = new ProcessorSharingRepository();

				ProcessorSharing oProcShare = oProcessorSharingRepository.getProcessorSharingByUserIdProcessorId(sUserId, sProcessorId);

				if (oProcShare!= null) {
					if (oProcShare.getUserId().equals(oOwnerUser.getUserId()) || oProcShare.getOwnerId().equals(oOwnerUser.getUserId())) {
						oProcessorSharingRepository.deleteByUserIdProcessorId(sUserId, sProcessorId);
					}
					else {
						oResult.setStringValue("Unauthorized");
						return oResult;					
					}
				}
				else {
					oResult.setStringValue("Sharing not found");
					return oResult;				
				}
			} 
			catch (Exception oEx) {
				Utils.debugLog("ProcessorsResource.deleteUserSharedProcessor: " + oEx);
				oResult.setStringValue("Error deleting processor sharing");
				return oResult;
			}

			oResult.setStringValue("Done");
			oResult.setBoolValue(true);
		} catch (Exception oE) {
			Utils.debugLog("ProcessorsResource.deleteUserSharedProcessor( Session: " + sSessionId + ", ProcId: " + sProcessorId + ", User:" + sUserId + " ): " + oE);
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
		Utils.debugLog("ProcessorsResource.getUI( Name: " + sName + " )");
		
		try {
			// Check User 
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser==null) {
				Utils.debugLog("ProcessorsResource.getUI: session invalid");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			//String sUserId = oUser.getUserId();
			
			Utils.debugLog("ProcessorsResource.getUI: read Processor " +sName);
						
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessor = oProcessorRepository.getProcessorByName(sName);

			if (oProcessor == null) {
				Utils.debugLog("ProcessorsResource.getUI: processor invalid");
				return Response.status(Status.BAD_REQUEST).build();
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
			Utils.debugLog("ProcessorsResource.getUI: " + oEx);
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
		Utils.debugLog("ProcessorsResource.getUI( Name: " + sName + " )");
		
		try {
			// Check User 
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser==null) {
				Utils.debugLog("ProcessorsResource.saveUI: session invalid");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			Utils.debugLog("ProcessorsResource.saveUI: read Processor " +sName);
			
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessor = oProcessorRepository.getProcessorByName(sName);
			
			if (oProcessor == null) {
				Utils.debugLog("ProcessorsResource.saveUI: processor invalid");
				return Response.status(Status.BAD_REQUEST).build();
			}
			
			if (Utils.isNullOrEmpty(sUIJson)==false) {				
				try {
					
					String sDecodedJSON = java.net.URLDecoder.decode(sUIJson, StandardCharsets.UTF_8.name());
					MongoRepository.s_oMapper.readTree(sDecodedJSON);
				}
				catch (Exception oJsonEx) {
					
					Utils.debugLog("ProcessorsResource.saveUI: JSON Exception: " + oJsonEx.toString());
					//oResult.setStringValue("Parameter Sample is not a valid JSON");
					return Response.status(422).build();
				}
			}			
			
			ProcessorUIRepository oProcessorUIRepository = new ProcessorUIRepository();			
			ProcessorUI oUI = oProcessorUIRepository.getProcessorUI(oProcessor.getProcessorId());
			
			String sEncodedUI = URLEncoder.encode(sUIJson, StandardCharsets.UTF_8.toString());
			
			if (oUI == null) {
				oUI = new ProcessorUI();
				oUI.setProcessorId(oProcessor.getProcessorId());
				oUI.setUi(sEncodedUI);
				oProcessorUIRepository.insertProcessorUI(oUI);
				Utils.debugLog("ProcessorsResource.saveUI: UI of " + sName + " created");
			}
			else {
				oUI.setUi(sEncodedUI);
				oProcessorUIRepository.updateProcessorUI(oUI);
				Utils.debugLog("ProcessorsResource.saveUI: UI of " + sName + " updated");
			}
			
			
						
			return Response.status(Status.OK).build();
		}
		catch (Exception oEx) {
			Utils.debugLog("ProcessorsResource.saveUI: " + oEx);
			return Response.serverError().build();
		}
	}
	
}
