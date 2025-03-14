package it.fadeout.rest.resources;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import it.fadeout.Wasdi;
import wasdi.shared.business.AppCategory;
import wasdi.shared.business.Comment;
import wasdi.shared.business.Review;
import wasdi.shared.business.processors.Processor;
import wasdi.shared.business.users.User;
import wasdi.shared.business.users.UserResourcePermission;
import wasdi.shared.data.AppsCategoriesRepository;
import wasdi.shared.data.CommentRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.data.ReviewRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.data.UserResourcePermissionRepository;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.processors.AppCategoryViewModel;
import wasdi.shared.viewmodels.processors.CommentDetailViewModel;
import wasdi.shared.viewmodels.processors.CommentListViewModel;
import wasdi.shared.viewmodels.processors.ListReviewsViewModel;
import wasdi.shared.viewmodels.processors.PublisherFilterViewModel;
import wasdi.shared.viewmodels.processors.ReviewViewModel;

/**
 * Processors Media Resource.
 * 
 * Hosts the API for:
 * 	.upload and update processor logo and associated images
 * 	.handle all the processor info related to the app-store (categories, prices...)
 * 	.handle reviews and comments
 * 
 * @author p.campanella
 *
 */
@Path("processormedia")
public class ProcessorsMediaResource {
	
	/**
	 * Get a list of all the available categories
	 * @param sSessionId User session Id
	 * @return a list of App Category View Models
	 */
	@GET
	@Path("categories/get")
	@Produces({"application/json", "application/xml", "text/xml" })
	public Response getCategories(@HeaderParam("x-session-token") String sSessionId) {
		
		WasdiLog.debugLog("ProcessorsMediaResource.getCategories");
		
		User oUser = Wasdi.getUserFromSession(sSessionId);
		
		AppsCategoriesRepository oAppCategoriesRepository = new AppsCategoriesRepository();
		
		// Check the user session
		if(oUser == null){
			WasdiLog.warnLog("ProcessorsMediaResource.getCategories: invalid session");
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		List<AppCategory> aoAppCategories = oAppCategoriesRepository.getCategories();
		ArrayList<AppCategoryViewModel> aoAppCategoriesViewModel = getCategoriesViewModel(aoAppCategories);
		
	    return Response.ok(aoAppCategoriesViewModel).build();
	}
	
	/**
	 * Delete a review of a processor
	 * @param sSessionId User Id
	 * @param sProcessorId Processor Id
	 * @param sReviewId Review Id
	 * @return std http response
	 */
	@DELETE
	@Path("/reviews/delete")
	@Produces({"application/json", "application/xml", "text/xml" })
	public Response deleteReview(@HeaderParam("x-session-token") String sSessionId, @QueryParam("processorId") String sProcessorId, @QueryParam("reviewId") String sReviewId ) {
		
		try {
		    sProcessorId = java.net.URLDecoder.decode(sProcessorId, StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
			WasdiLog.errorLog("ProcessorsMediaResource.deleteReview excepion decoding processor Id");
		}
		
		try {
			sReviewId = java.net.URLDecoder.decode(sReviewId, StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
			WasdiLog.errorLog("ProcessorsMediaResource.deleteReview excepion decoding review Id");
		}		
		
		WasdiLog.debugLog("ProcessorsMediaResource.deleteReview( sProcessorId: "+ sProcessorId +" reviewId: "+sReviewId+")");
		
		User oUser = Wasdi.getUserFromSession(sSessionId);
		// Check the user session
		if(oUser == null){
			WasdiLog.warnLog("ProcessorsMediaResource.deleteReview: invalid session");
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		String sUserId = oUser.getUserId();

		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);

		if( oProcessor != null && Utils.isNullOrEmpty(oProcessor.getName()) ) {
			WasdiLog.warnLog("ProcessorsMediaResource.deleteReview: invalid processor");
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		ReviewRepository oReviewRepository = new ReviewRepository();
		
		//CHEK USER ID TOKEN AND USER ID IN VIEW MODEL ARE ==
		if( oReviewRepository.isTheOwnerOfTheReview(sProcessorId,sReviewId,sUserId) == false ){
			WasdiLog.warnLog("ProcessorsMediaResource.deleteReview: user is not the review owner");
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		int iDeletedCount = oReviewRepository.deleteReview(sProcessorId, sReviewId);

		if( iDeletedCount == 0 ){
			WasdiLog.warnLog("ProcessorsMediaResource.deleteReview: return count of db operation is 0");
			return Response.status(Status.BAD_REQUEST).build();
		}

		// Delete all the comments associated with the recently deleted review
		CommentRepository oCommentRepository = new CommentRepository();
		oCommentRepository.deleteComments(sReviewId);


		return Response.status(Status.OK).build();
	}
	
	/**
	 * Deletes a comment to a review
	 * @param sSessionId User Session Id
	 * @param sReviewId Parent Review Id 
	 * @param sCommentId Comment Id
	 * @return std http response
	 */
	@DELETE
	@Path("/comments/delete")
	@Produces({"application/json", "application/xml", "text/xml" })
	public Response deleteComment(@HeaderParam("x-session-token") String sSessionId, @QueryParam("reviewId") String sReviewId, @QueryParam("commentId") String sCommentId ) {
		
		try {
		    sReviewId = java.net.URLDecoder.decode(sReviewId, StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
			WasdiLog.errorLog("ProcessorsMediaResource.deleteComment excepion decoding parent review Id");
		}
		
		try {
			sCommentId = java.net.URLDecoder.decode(sCommentId, StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
			WasdiLog.errorLog("ProcessorsMediaResource.deleteComment excepion decoding comment Id");
		}		
		
		WasdiLog.debugLog("ProcessorsMediaResource.deleteComment( sReviewId: " + sReviewId + " sCommentId: " + sCommentId + ")");
		
		User oUser = Wasdi.getUserFromSession(sSessionId);
		
		// Check the user session
		if (oUser == null) {
			WasdiLog.warnLog("ProcessorsMediaResource.deleteComment: invalid session");
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		String sUserId = oUser.getUserId();

		ReviewRepository oReviewRepository = new ReviewRepository();
		Review oReview = oReviewRepository.getReview(sReviewId);


		if (oReview != null && Utils.isNullOrEmpty(oReview.getTitle()) && Utils.isNullOrEmpty(oReview.getComment())) {
			WasdiLog.warnLog("ProcessorsMediaResource.deleteComment: invalid review");
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		CommentRepository oCommentRepository = new CommentRepository();
		
		//CHEK USER ID TOKEN AND USER ID IN VIEW MODEL ARE ==
		if (!oCommentRepository.isTheOwnerOfTheComment(sReviewId, sCommentId, sUserId)) {
			WasdiLog.warnLog("ProcessorsMediaResource.deleteComment: invalid comment");
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		int iDeletedCount = oCommentRepository.deleteComment(sReviewId, sCommentId);

		if (iDeletedCount == 0) {
			WasdiLog.warnLog("ProcessorsMediaResource.deleteComment: delete returned 0");
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		return Response.status(Status.OK).build();
	}
	
	/**
	 * Update a review
	 * @param sSessionId User Session
	 * @param oReviewViewModel Review View Model with updated information
	 * @return std http response
	 */
	@POST
	@Path("/reviews/update")
	@Produces({"application/json", "application/xml", "text/xml" })
	public Response updateReview(@HeaderParam("x-session-token") String sSessionId, ReviewViewModel oReviewViewModel) {
		
		WasdiLog.debugLog("ProcessorsMediaResource.updateReview");
	
		User oUser = Wasdi.getUserFromSession(sSessionId);
		// Check the user session
		if(oUser == null){
			WasdiLog.warnLog("ProcessorsMediaResource.updateReview: invalid session");
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		String sUserId = oUser.getUserId();
		
		if(oReviewViewModel == null ){
			WasdiLog.warnLog("ProcessorsMediaResource.updateReview: invalid review");
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		ReviewRepository oReviewRepository =  new ReviewRepository();
		
		//CHECK THE VALUE OF THE VOTE === 1 - 5
		if( isValidVote(oReviewViewModel.getVote()) == false ){
			return Response.status(Status.BAD_REQUEST).build();
		}
				
		Review oReview = getReviewFromViewModel(oReviewViewModel, sUserId, oReviewViewModel.getId());
		
		boolean isUpdated = oReviewRepository.updateReview(oReview);
		if(isUpdated == false){
			return Response.status(Status.BAD_REQUEST).build();
		}
		else {
			return Response.status(Status.OK).build();
		}
	}
	
	/**
	 * Updates a comment
	 * @param sSessionId User Session Id
	 * @param oCommentViewModel Comment View Model
	 * @return std http response
	 */
	@POST
	@Path("/comments/update")
	@Produces({"application/json", "application/xml", "text/xml" })
	public Response updateComment(@HeaderParam("x-session-token") String sSessionId, CommentDetailViewModel oCommentViewModel) {
		
		WasdiLog.debugLog("ProcessorsMediaResource.updateComment");
	
		User oUser = Wasdi.getUserFromSession(sSessionId);
		// Check the user session
		if (oUser == null) {
			WasdiLog.warnLog("ProcessorsMediaResource.updateComment: invalid session");
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		String sUserId = oUser.getUserId();
		
		if (oCommentViewModel == null) {
			WasdiLog.warnLog("ProcessorsMediaResource.updateComment: invalid comment");
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		CommentRepository oCommentRepository = new CommentRepository();
				
		Comment oComment = getCommentFromViewModel(oCommentViewModel, sUserId, oCommentViewModel.getCommentId());
		
		boolean isUpdated = oCommentRepository.updateComment(oComment);
		if (isUpdated == false) {
			WasdiLog.warnLog("ProcessorsMediaResource.updateComment: the update was not good");
			return Response.status(Status.BAD_REQUEST).build();
		} else {
			return Response.status(Status.OK).build();
		}
	}
	
	/**
	 * Add a new review to a processor
	 * @param sSessionId User session id
	 * @param oReviewViewModel Review View Model
	 * @return std http reponse
	 */
	@POST
	@Path("/reviews/add")
	@Produces({"application/json", "application/xml", "text/xml" })
	public Response addReview(@HeaderParam("x-session-token") String sSessionId, ReviewViewModel oReviewViewModel) {//
		
		WasdiLog.debugLog("ProcessorsMediaResource.addReview");
	
		User oUser = Wasdi.getUserFromSession(sSessionId);
		// Check the user session
		if(oUser == null){
			WasdiLog.warnLog("ProcessorsMediaResource.addReview: invalid user");
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		if(oReviewViewModel == null ){
			WasdiLog.warnLog("ProcessorsMediaResource.addReview: invalid view model");
			return Response.status(Status.BAD_REQUEST).build();
		}		
		
		String sUserId = oUser.getUserId();
				
		//CHECK THE VALUE OF THE VOTE === 1 - 5
		if( isValidVote(oReviewViewModel.getVote()) == false ){
			WasdiLog.warnLog("ProcessorsMediaResource.addReview: invalid vote");
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		String sProcessorId = oReviewViewModel.getProcessorId();
		
		if (Utils.isNullOrEmpty(sProcessorId)) {
			WasdiLog.warnLog("ProcessorsMediaResource.addReview: invalid proc id");
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
		
		if (oProcessor == null) {
			WasdiLog.warnLog("ProcessorsMediaResource.addReview: processor null " + sProcessorId);
			return Response.status(Status.BAD_REQUEST).build();			
		}
		
		if (!PermissionsUtils.canUserAccessProcessor(sUserId, oProcessor)) {
			WasdiLog.warnLog("ProcessorsMediaResource.addReview: User cannot access the processor");
			return Response.status(Status.FORBIDDEN).build();			
		}		
		
		ReviewRepository oReviewRepository =  new ReviewRepository();
		
		Review oReview = getReviewFromViewModel(oReviewViewModel,sUserId, Utils.getRandomName());
		
		//LIMIT THE NUMBER OF COMMENTS
		if(oReviewRepository.alreadyVoted(oReviewViewModel.getProcessorId(), sUserId) == true){
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		oReviewRepository.addReview(oReview);
		
		return Response.status(Status.OK).build();
	}
	
	/**
	 * Add a comment to a review
	 * @param sSessionId User Session Id
	 * @param oCommentViewModel Comment View Model
	 * @return std http reponse
	 */
	@POST
	@Path("/comments/add")
	@Produces({"application/json", "application/xml", "text/xml" })
	public Response addComment(@HeaderParam("x-session-token") String sSessionId, CommentDetailViewModel oCommentViewModel) {
		
		WasdiLog.debugLog("ProcessorsMediaResource.addComment");
	
		User oUser = Wasdi.getUserFromSession(sSessionId);
		// Check the user session
		if (oUser == null) {
			WasdiLog.warnLog("ProcessorsMediaResource.addComment: invalid user");
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		if (oCommentViewModel == null ) {
			WasdiLog.warnLog("ProcessorsMediaResource.addComment: invalid view model");
			return Response.status(Status.BAD_REQUEST).build();
		}		
		
		String sUserId = oUser.getUserId();
		
		String sReviewId = oCommentViewModel.getReviewId();
		
		if (Utils.isNullOrEmpty(sReviewId)) {
			WasdiLog.warnLog("ProcessorsMediaResource.addComment: invalid parent review id");
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		ReviewRepository oReviewRepository = new ReviewRepository();
		
		Review oReview = oReviewRepository.getReview(sReviewId);
		
		if (oReview == null) {
			WasdiLog.warnLog("ProcessorsMediaResource.addComment: review null " + sReviewId);
			return Response.status(Status.BAD_REQUEST).build();			
		}
		
		String sProcessorId = oReview.getProcessorId();
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
		
		if (oProcessor == null) {
			WasdiLog.warnLog("ProcessorsMediaResource.addComment: processor null " + sProcessorId);
			return Response.status(Status.BAD_REQUEST).build();			
		}
		
		if (!PermissionsUtils.canUserAccessProcessor(sUserId, oProcessor)) {
			WasdiLog.warnLog("ProcessorsMediaResource.addComment: User cannot access the processor");
			return Response.status(Status.FORBIDDEN).build();			
		}			
		
		CommentRepository oCommentRepository =  new CommentRepository();
		
		Comment oComment = getCommentFromViewModel(oCommentViewModel, sUserId, Utils.getRandomName());
		
		oCommentRepository.addComment(oComment);
		
		return Response.status(Status.OK).build();
	}
	
	/**
	 * Get paginated list of reviews of a processor 
	 * @param sSessionId User Session Id
	 * @param sProcessorName processor name
	 * @param iPage Page to get (1 based)
	 * @param iItemsPerPage Items to get per page
	 * @return List of List Review View Model, that represents the light version of the review
	 */
	@GET
	@Path("/reviews/getlist")
	@Produces({"application/json", "application/xml", "text/xml" })
	public Response getReviewListByProcessor(@HeaderParam("x-session-token") String sSessionId, @QueryParam("processorName") String sProcessorName, @QueryParam("page") Integer iPage, @QueryParam("itemsperpage") Integer iItemsPerPage) {
		
		WasdiLog.debugLog("ProcessorsMediaResource.getReviewListByProcessor");

		User oUser = Wasdi.getUserFromSession(sSessionId);
		// Check the user session
		if(oUser == null){
			WasdiLog.warnLog("ProcessorsMediaResource.getReviewListByProcessor: invalid session");
			return Response.status(Status.UNAUTHORIZED).build();
		}

		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessorByName(sProcessorName);
		
		if(oProcessor == null || Utils.isNullOrEmpty(oProcessor.getName()) ) {
			WasdiLog.warnLog("ProcessorsMediaResource.getReviewListByProcessor: invalid processor");
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		if (!PermissionsUtils.canUserAccessProcessor(oUser.getUserId(), oProcessor)) {
			WasdiLog.warnLog("ProcessorsMediaResource.getReviewListByProcessor: User cannot access the processor");
			return Response.status(Status.FORBIDDEN).build();			
		}			
		
		if (iPage==null) iPage = 0;
		if (iItemsPerPage==null) iItemsPerPage = 4;
		
		// Get all the reviews
		ReviewRepository oReviewRepository =  new ReviewRepository();
		List<Review> aoApplicationReviews = oReviewRepository.getReviews(oProcessor.getProcessorId());
		
		if(aoApplicationReviews == null || aoApplicationReviews.size() == 0){
			  return Response.ok(new ListReviewsViewModel()).build();
		}
		
		// Cast in a list, computing all the statistics
		ListReviewsViewModel oListReviewsViewModel = getListReviewsViewModel(aoApplicationReviews);
		oListReviewsViewModel.setAlreadyVoted(oReviewRepository.alreadyVoted(oProcessor.getProcessorId(), oUser.getUserId()));
		
		ArrayList<ReviewViewModel> aoCleanedList = new ArrayList<ReviewViewModel>();
		
		// Clean the list: return only elements in the pagination
		for(int iReviews=0; iReviews<oListReviewsViewModel.getReviews().size(); iReviews ++) {
			
			if (iReviews<iPage*iItemsPerPage) continue;
			
			aoCleanedList.add(oListReviewsViewModel.getReviews().get(iReviews));
			
			if (iReviews>=((iPage+1)*iItemsPerPage)) {
				break;
			}
		}
		
		oListReviewsViewModel.setReviews(aoCleanedList);
		
	    return Response.ok(oListReviewsViewModel).build();

	}
	
	/**
	 * Get the list of comments associated to a review
	 * @param sSessionId User Session Id
	 * @param sReviewId Review Id
	 * @return List Comment View Model
	 */
	@GET
	@Path("/comments/getlist")
	@Produces({"application/json", "application/xml", "text/xml" })
	public Response getCommentListByReview(@HeaderParam("x-session-token") String sSessionId, @QueryParam("reviewId") String sReviewId) {
		WasdiLog.debugLog("ProcessorsMediaResource.getCommentListByReview");

		User oUser = Wasdi.getUserFromSession(sSessionId);
		// Check the user session
		if (oUser == null) {
			WasdiLog.warnLog("ProcessorsMediaResource.getCommentListByReview: invalid session");
			return Response.status(Status.UNAUTHORIZED).build();
		}

		ReviewRepository oReviewRepository = new ReviewRepository();
		Review oReview = oReviewRepository.getReview(sReviewId);
		
		if (oReview == null || Utils.isNullOrEmpty(oReview.getId())) {
			WasdiLog.warnLog("ProcessorsMediaResource.getCommentListByReview: invalid review");
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		String sProcessorId = oReview.getProcessorId();
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
		
		if (oProcessor == null) {
			WasdiLog.warnLog("ProcessorsMediaResource.getCommentListByReview: processor null " + sProcessorId);
			return Response.status(Status.BAD_REQUEST).build();			
		}
		
		if (!PermissionsUtils.canUserAccessProcessor(oUser.getUserId(), oProcessor)) {
			WasdiLog.warnLog("ProcessorsMediaResource.getReviewListByProcessor: User cannot access the processor");
			return Response.status(Status.FORBIDDEN).build();			
		}			
		
		// Get all the comments
		CommentRepository oCommentRepository = new CommentRepository();
		List<Comment> aoReviewComments = oCommentRepository.getComments(oReview.getId());
		
		if (aoReviewComments == null || aoReviewComments.size() == 0) {
			return Response.ok(new CommentListViewModel()).build();
		}

		// Cast in a list
		List<CommentListViewModel> aoCommentsListViewModel = getListCommentsViewModel(aoReviewComments);

	    return Response.ok(aoCommentsListViewModel).build();
	}
	
	/**
	 * Get list of WASDI publishers
	 * @param sSessionId User Session Id
	 * @return list of Publisher Filter View Models
	 */
	@GET
	@Path("/publisher/getlist")
	@Produces({"application/json", "application/xml", "text/xml" })
	public Response getPublishers(@HeaderParam("x-session-token") String sSessionId) {
		
		WasdiLog.debugLog("ProcessorsMediaResource.getPublishers");


		User oUser = Wasdi.getUserFromSession(sSessionId);
		// Check the user session
		if(oUser == null){
			WasdiLog.warnLog("ProcessorsMediaResource.getPublishers: invalid session");
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		UserRepository oUserRepository = new UserRepository();
		
		// Get all the processors
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
		
		List<Processor> aoProcessors = oProcessorRepository.getDeployedProcessors();
		
		if(aoProcessors == null) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		
		// Create a return list of publishers
		ArrayList<PublisherFilterViewModel> aoPublishers = new ArrayList<PublisherFilterViewModel>();
		
		// For each processor
		for (Processor oProcessor : aoProcessors) {
			
			if (!oProcessor.getShowInStore()) continue;
			
			UserResourcePermission oSharing = oUserResourcePermissionRepository.getProcessorSharingByUserIdAndProcessorId(oUser.getUserId(), oProcessor.getProcessorId());
			
			if (oProcessor.getIsPublic() != 1) {
				if (oProcessor.getUserId().equals(oUser.getUserId()) == false) {
					if (oSharing == null) continue;
				}
			}			
			
			boolean bFound = false;
			
			// Check if there is already a view model of the publisher
			for (PublisherFilterViewModel oPublisher : aoPublishers) {
				if (oPublisher.getPublisher().equals(oProcessor.getUserId())) {
					// Yes, increment the count and break;
					bFound = true;
					oPublisher.setAppCount(oPublisher.getAppCount()+1);
					break;
				}
			}
			
			if (!bFound) {
				// New Publisher, create the View Model
				PublisherFilterViewModel oPublisherFilter = new PublisherFilterViewModel();
				oPublisherFilter.setPublisher(oProcessor.getUserId());
				oPublisherFilter.setAppCount(1);
				
				User oAppPublisher = oUserRepository.getUser(oProcessor.getUserId());
				if (oAppPublisher != null) {
					oPublisherFilter.setNickName(oAppPublisher.getPublicNickName());
					if (Utils.isNullOrEmpty(oPublisherFilter.getNickName())) {
						oPublisherFilter.setNickName(oAppPublisher.getName());
					}
				}
				else {
					oPublisherFilter.setNickName(oProcessor.getUserId());
				}
				
				aoPublishers.add(oPublisherFilter);
			}
		}
		
	    return Response.ok(aoPublishers).build();

	}
	
	
	/**
	 * Checks if a vote is valid or not
	 * @param fVote
	 * @return
	 */
	private boolean isValidVote(Float fVote){
		if (fVote>=0.0 && fVote<=5.0) return true;
		else return false;
	}
	
	/**
	 * Converts a Review View Model in a Review Entity
	 * @param oReviewViewModel
	 * @param sUserId
	 * @param sId
	 * @return
	 */
	private Review getReviewFromViewModel(ReviewViewModel oReviewViewModel, String sUserId, String sId){
		if(oReviewViewModel != null){
			Review oReview = new Review();
			oReview.setTitle(oReviewViewModel.getTitle());
			oReview.setComment(oReviewViewModel.getComment());
			oReview.setDate((double)(new Date()).getTime());
			oReview.setId(sId); 
			oReview.setProcessorId(oReviewViewModel.getProcessorId());
			oReview.setUserId(sUserId);
			oReview.setVote(oReviewViewModel.getVote());
			return oReview;
		}
		return null;
	}
	
	/**
	 * Converts a Comment View Model in a Comment Entity
	 * @param oCommentViewModel
	 * @param sUserId
	 * @param sId
	 * @return
	 */
	private Comment getCommentFromViewModel(CommentDetailViewModel oCommentViewModel, String sUserId, String sId) {
		if (oCommentViewModel != null) {
			Comment oComment = new Comment();
			oComment.setCommentId(sId);
			oComment.setReviewId(oCommentViewModel.getReviewId());
			oComment.setUserId(sUserId);
			oComment.setDate((double)(new Date()).getTime());
			oComment.setText(oCommentViewModel.getText());
			return oComment;
		}
		return null;
	}
	
	/**
	 * Fill the Review Wrappwer View Model result from a list of reviews
	 * @param aoReviewRepository
	 * @return
	 */
	private ListReviewsViewModel getListReviewsViewModel(List<Review> aoReviewRepository ){
		ListReviewsViewModel oListReviews = new ListReviewsViewModel();
		List<ReviewViewModel> aoReviews = new ArrayList<ReviewViewModel>();
		if(aoReviewRepository == null){
			return null; 
		}
		
		//CHECK VALUE VOTE policy 1 - 5
		float fSumVotes = 0;

		for(Review oReview: aoReviewRepository){
			ReviewViewModel oReviewViewModel = new ReviewViewModel();
			oReviewViewModel.setComment(oReview.getComment());

			oReviewViewModel.setDate( Utils.getDate(oReview.getDate()) );
			
			oReviewViewModel.setId(oReview.getId());
			oReviewViewModel.setUserId(oReview.getUserId());
			oReviewViewModel.setProcessorId(oReview.getProcessorId());
			oReviewViewModel.setVote(oReview.getVote());
			oReviewViewModel.setTitle(oReview.getTitle());
			fSumVotes = fSumVotes + oReview.getVote();
			
			aoReviews.add(oReviewViewModel);
		}
		
		float avgVote = (float)fSumVotes / aoReviews.size();
		
		oListReviews.setReviews(aoReviews);
		oListReviews.setAvgVote(avgVote);
		oListReviews.setNumberOfOneStarVotes(getNumberOfVotes(aoReviews , 1));
		oListReviews.setNumberOfTwoStarVotes(getNumberOfVotes(aoReviews , 2));
		oListReviews.setNumberOfThreeStarVotes(getNumberOfVotes(aoReviews , 3));
		oListReviews.setNumberOfFourStarVotes(getNumberOfVotes(aoReviews , 4));
		oListReviews.setNumberOfFiveStarVotes(getNumberOfVotes(aoReviews , 5));

		return oListReviews;
	}
	
	/**
	 * Count the number of votes of a specified type in a list of references
	 * @param aoReviews
	 * @param iReferenceVote
	 * @return
	 */
	private int getNumberOfVotes(List<ReviewViewModel> aoReviews, int iReferenceVote ){
		int iNumberOfVotes = 0;
		for(ReviewViewModel oReview : aoReviews){
			if( oReview.getVote() == ((float)iReferenceVote)){
				iNumberOfVotes++;
			}
			
		}
		return iNumberOfVotes;
	}
	
	/**
	 * Converts a List of App Categories in the equivalent list of view models
	 * @param aoAppCategories
	 * @return
	 */
	private ArrayList<AppCategoryViewModel> getCategoriesViewModel(List<AppCategory> aoAppCategories ){
		
		ArrayList<AppCategoryViewModel> aoAppCategoriesViewModel = new ArrayList<AppCategoryViewModel>();
		
		for(AppCategory oCategory:aoAppCategories){
			AppCategoryViewModel oAppCategoryViewModel = new AppCategoryViewModel();
			oAppCategoryViewModel.setId(oCategory.getId());
			oAppCategoryViewModel.setCategory(oCategory.getCategory());
			aoAppCategoriesViewModel.add(oAppCategoryViewModel);
		}
		
		return aoAppCategoriesViewModel;
	} 
	
	/**
	 * Transform the list of Comment objects into a list of Comment ListViewModel.
	 * @param aoCommentList
	 * @return a list of list view models
	 */
	private static List<CommentListViewModel> getListCommentsViewModel(List<Comment> aoCommentList) {
		if (aoCommentList == null) {
			return null; 
		}

		return aoCommentList.stream().map(ProcessorsMediaResource::getListViewModel).collect(Collectors.toList());
	}

	/**
	 * Transform the Comment object into a Comment ListViewModel.
	 * @param oComment the comment object
	 * @return a list view model
	 */
	private static CommentListViewModel getListViewModel(Comment oComment) {
		if (oComment == null) {
			return null; 
		}

		CommentListViewModel oListViewModel = new CommentListViewModel();
		oListViewModel.setCommentId(oComment.getCommentId());
		oListViewModel.setReviewId(oComment.getReviewId());
		oListViewModel.setUserId(oComment.getUserId());
		oListViewModel.setDate(new Date(oComment.getDate().longValue()));
		oListViewModel.setText(oComment.getText());

		return oListViewModel;
	}
}

