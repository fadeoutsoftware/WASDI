package it.fadeout.rest.resources;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FilenameUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import it.fadeout.Wasdi;
import it.fadeout.business.ImageResourceUtils;
import wasdi.shared.business.AppCategory;
import wasdi.shared.business.Processor;
import wasdi.shared.business.Review;
import wasdi.shared.business.User;
import wasdi.shared.data.AppsCategoriesRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.data.ReviewRepository;
import wasdi.shared.utils.ImageFile;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.AppCategoryViewModel;
import wasdi.shared.viewmodels.ListReviewsViewModel;
import wasdi.shared.viewmodels.ReviewViewModel;

@Path("processormedia")
public class ProcessorsMediaResource {
	
	@Context
	ServletConfig m_oServletConfig;
	
	public static String LOGO_PROCESSORS_PATH = "/logo/";
	public static String IMAGES_PROCESSORS_PATH = "/images/";
	public static String[] IMAGE_PROCESSORS_EXTENSIONS = {"jpg", "png", "svg"};
	public static String DEFAULT_LOGO_PROCESSOR_NAME = "logo";
	public static Integer LOGO_SIZE = 180;
	public static Integer NUMB_MAX_OF_IMAGES = 5;
	public static String[] IMAGES_NAME = { "1", "2", "3", "4", "5" };
	public static String[] RANGE_OF_VOTES = { "1", "2", "3", "4", "5" };
	
	@POST
	@Path("/logo/upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadProcessorLogo(@FormDataParam("image") InputStream fileInputStream, @FormDataParam("image") FormDataContentDisposition fileMetaData,
										@HeaderParam("x-session-token") String sSessionId, @QueryParam("processorId") String sProcessorId ) {
		
		// Check the user session
		User oUser = getUser(sSessionId);
		
		if(oUser == null){
			return Response.status(401).build();
		}
		
		// Check the processor
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);

		if(oProcessor != null && Utils.isNullOrEmpty(oProcessor.getName()) ) {
			return Response.status(400).build();
		}
		
		//check if the user is the owner of the processor 
		if( oProcessor.getUserId().equals( oUser.getUserId() ) == false ){
			return Response.status(401).build();
		}
		
		String sExt;
		String sFileName;
		
		//get filename and extension 
		if(fileMetaData != null && Utils.isNullOrEmpty(fileMetaData.getFileName()) == false){
			sFileName = fileMetaData.getFileName();
			sExt = FilenameUtils.getExtension(sFileName);
		} else {
			return Response.status(400).build();
		}
		
		// Check if this is an accepted file extension
		if(ImageResourceUtils.isValidExtension(sExt,IMAGE_PROCESSORS_EXTENSIONS) == false ){
			return Response.status(400).build();
		}

		// Take path
		String sPath = Wasdi.getProcessorLogoPath(oProcessor.getName());
		
		String sExtensionOfSavedLogo = ImageResourceUtils.checkExtensionOfImageInFolder(sPath, IMAGE_PROCESSORS_EXTENSIONS);
		
		//if there is a saved logo with a different extension remove it 
		if( sExtensionOfSavedLogo.isEmpty() == false && sExtensionOfSavedLogo.equalsIgnoreCase(sExt) == false ){
		    File oOldLogo = new File(sPath + DEFAULT_LOGO_PROCESSOR_NAME + "." + sExtensionOfSavedLogo);
		    oOldLogo.delete();
		}
			
		ImageResourceUtils.createDirectory(sPath);
	    
	    String sOutputFilePath = sPath + DEFAULT_LOGO_PROCESSOR_NAME + "." + sExt.toLowerCase();
	    
	    ImageFile oOutputLogo = new ImageFile(sOutputFilePath);
	    boolean bIsSaved =  oOutputLogo.saveImage(fileInputStream);
	    
	    if(bIsSaved == false){
	    	return Response.status(400).build();
	    }
	    
	    boolean bIsResized = oOutputLogo.resizeImage(LOGO_SIZE, LOGO_SIZE);
	    
	    if(bIsResized == false){
	    	return Response.status(400).build();
	    }
	    
	    oProcessorRepository.updateProcessorDate(oProcessor);
	    
		return Response.status(200).build();
	}	
	
	@GET
	@Path("/logo/get")
	public Response getProcessorLogo(@HeaderParam("x-session-token") String sSessionId, @QueryParam("processorId") String sProcessorId ) {

		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);

		if(oProcessor == null){
			return Response.status(401).build();
		}
			
		User oUser = getUser(sSessionId);
		// Check the user session
		if(oUser == null){
			return Response.status(401).build();
		}
		
		
		String sPathLogoFolder = Wasdi.getProcessorLogoPath(oProcessor.getName());
		
		ImageFile oLogo = ImageResourceUtils.getImageInFolder(sPathLogoFolder,IMAGE_PROCESSORS_EXTENSIONS );
		String sLogoExtension = ImageResourceUtils.checkExtensionOfImageInFolder(sPathLogoFolder,IMAGE_PROCESSORS_EXTENSIONS );
		
		//Check the logo and extension
		if(oLogo == null || sLogoExtension.isEmpty() ){
			return Response.status(204).build();
		}
		//prepare buffer and send the logo to the client 
		ByteArrayInputStream abImageLogo = oLogo.getByteArrayImage();
		
	    return Response.ok(abImageLogo).build();

	}
	
	
	@GET
	@Path("/images/get")
	public Response getAppImage(@HeaderParam("x-session-token") String sSessionId, @QueryParam("processorId") String sProcessorId,
								@QueryParam("imageName") String sImageName) {


		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);

		if(oProcessor == null){
			return Response.status(401).build();
		}
			
		User oUser = getUser(sSessionId);
		// Check the user session
		if(oUser == null){
			return Response.status(401).build();
		}
		
		String sPathLogoFolder = Wasdi.getDownloadPath(m_oServletConfig) + "/processors/" + oProcessor.getName() + IMAGES_PROCESSORS_PATH;
		ImageFile oImage = ImageResourceUtils.getImageInFolder(sPathLogoFolder + sImageName,IMAGE_PROCESSORS_EXTENSIONS );
		String sLogoExtension = ImageResourceUtils.checkExtensionOfImageInFolder(sPathLogoFolder + sImageName,IMAGE_PROCESSORS_EXTENSIONS );;
		
		//Check the logo and extension
		if(oImage == null || sLogoExtension.isEmpty() ){
			return Response.status(204).build();
		}
		//prepare buffer and send the logo to the client 
		ByteArrayInputStream abImage = oImage.getByteArrayImage();
		
	    return Response.ok(abImage).build();

	}
	
	@DELETE
	@Path("/image/delete")
	public Response deleteProcessorImage(@HeaderParam("x-session-token") String sSessionId, @QueryParam("processorId") String sProcessorId, @QueryParam("imageName") String sImageName ) {
		User oUser = getUser(sSessionId);
		// Check the user session
		if(oUser == null){
			return Response.status(401).build();
		}
		
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);

		
		if( oProcessor != null && Utils.isNullOrEmpty(oProcessor.getName()) ) {
			return Response.status(400).build();
		}
		
		if( sImageName== null || sImageName.isEmpty() ) {
			return Response.status(400).build();
		}

		//check if the user is the owner of the processor 
		if( oProcessor.getUserId().equals( oUser.getUserId() ) == false ){
			return Response.status(401).build();
		}
		
		String sPathFolder = Wasdi.getDownloadPath(m_oServletConfig) + "/processors/" + oProcessor.getName() + IMAGES_PROCESSORS_PATH;
		ImageResourceUtils.deleteFileInFolder(sPathFolder,sImageName);
		
		return Response.status(200).build();
	}
	
	
	@POST
	@Path("/images/upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadProcessorImage(@FormDataParam("image") InputStream fileInputStream, @FormDataParam("image") FormDataContentDisposition fileMetaData,
										@HeaderParam("x-session-token") String sSessionId, @QueryParam("processorId") String sProcessorId ) {
	
		String sExt;
		String sFileName;

		
		User oUser = getUser(sSessionId);
		// Check the user session
		if(oUser == null){
			return Response.status(401).build();
		}
	
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);

		
		if(oProcessor != null && Utils.isNullOrEmpty(oProcessor.getName()) ) {
			return Response.status(400).build();
		}
		
		//check if the user is the owner of the processor 
		if( oProcessor.getUserId().equals( oUser.getUserId() ) == false ){
			return Response.status(401).build();
		}
		
		//get filename and extension 
		if(fileMetaData != null && Utils.isNullOrEmpty(fileMetaData.getFileName()) == false){
			sFileName = fileMetaData.getFileName();
			sExt = FilenameUtils.getExtension(sFileName);
		} else {
			return Response.status(400).build();
		}
		
		if( ImageResourceUtils.isValidExtension(sExt,IMAGE_PROCESSORS_EXTENSIONS) == false ){
			return Response.status(400).build();
		}
		// Take path
		String sPathFolder = Wasdi.getDownloadPath(m_oServletConfig) + "/processors/" + oProcessor.getName() + IMAGES_PROCESSORS_PATH;
		
		ImageResourceUtils.createDirectory(sPathFolder);
		String sAvaibleFileName = getAvaibleFileName(sPathFolder);
		
		if(sAvaibleFileName.isEmpty()){
			//the user have reach the max number of images 
	    	return Response.status(400).build();
		}
		
		String sPathImage = sPathFolder + sAvaibleFileName + "." + sExt.toLowerCase();
		ImageFile oNewImage = new ImageFile(sPathImage);

		//TODO SCALE IMAGE ?
		boolean bIsSaved = oNewImage.saveImage(fileInputStream);
	    if(bIsSaved == false){
	    	return Response.status(400).build();
	    }
	    
		double bytes = oNewImage.length();
		double kilobytes = (bytes / 1024);
		double megabytes = (kilobytes / 1024);
		if( megabytes > 2 ){		
			oNewImage.delete();
	    	return Response.status(400).build();
		}
		
		oProcessorRepository.updateProcessorDate(oProcessor);
		return Response.status(200).build();
	}
	
	@GET
	@Path("categories/get")
	public Response getCategories(@HeaderParam("x-session-token") String sSessionId) {
		
		User oUser = getUser(sSessionId);
		
		AppsCategoriesRepository oAppCategoriesRepository = new AppsCategoriesRepository();
		
		// Check the user session
		if(oUser == null){
			return Response.status(401).build();
		}

		
		List<AppCategory> aoAppCategories = oAppCategoriesRepository.getCategories();
		ArrayList<AppCategoryViewModel> aoAppCategoriesViewModel = getCategoriesViewModel(aoAppCategories);
		
	    return Response.ok(aoAppCategoriesViewModel).build();

	}
	
	@DELETE
	@Path("/reviews/delete")
	public Response deleteReview(@HeaderParam("x-session-token") String sSessionId, @QueryParam("processorId") String sProcessorId, @QueryParam("reviewId") String sReviewId ) {
		
		//************************ TODO CHECK IF THE USER IS THE OWNER OF THE REVIEW ************************//
		
		User oUser = getUser(sSessionId);
		// Check the user session
		if(oUser == null){
			return Response.status(401).build();
		}
		
		String sUserId = oUser.getUserId();

		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);


		if( oProcessor != null && Utils.isNullOrEmpty(oProcessor.getName()) ) {
			return Response.status(400).build();
		}
		
		ReviewRepository oReviewRepository = new ReviewRepository();
		
		//CHEK USER ID TOKEN AND USER ID IN VIEW MODEL ARE ==
		if( oReviewRepository.isTheOwnerOfTheReview(sProcessorId,sReviewId,sUserId) == false ){
			return Response.status(401).build();
		}
		
		int iDeletedCount = oReviewRepository.deleteReview(sProcessorId, sReviewId);

		if( iDeletedCount == 0 ){
			return Response.status(400).build();
		}
		
		return Response.status(200).build();
	}
	
	@POST
	@Path("/reviews/update")
	public Response updateReview(@HeaderParam("x-session-token") String sSessionId, ReviewViewModel oReviewViewModel) {
	
		User oUser = getUser(sSessionId);
		// Check the user session
		if(oUser == null){
			return Response.status(401).build();
		}
		
		String sUserId = oUser.getUserId();
		
		if(oReviewViewModel == null ){
			return Response.status(400).build();
		}
		
		ReviewRepository oReviewRepository =  new ReviewRepository();
		
		//CHEK USER ID TOKEN AND USER ID IN VIEW MODEL ARE ==
		if(oReviewViewModel.getUserId().toLowerCase().equals(sUserId.toLowerCase()) == false || (oReviewRepository.isTheOwnerOfTheReview(oReviewViewModel.getProcessorId(),oReviewViewModel.getId(),sUserId) == false) ){
			return Response.status(401).build();
		}
		//CHECK THE VALUE OF THE VOTE === 1 - 5
		if( isValidVote(oReviewViewModel.getVote()) == false ){
			return Response.status(400).build();
		}
		
		//ADD DATE 
		Date oDate = new Date();
		oReviewViewModel.setDate(oDate);
		
		Review oReview = getReviewFromViewModel(oReviewViewModel);
		
		
		boolean isUpdated = oReviewRepository.updateReview(oReview);
		if(isUpdated == false){
			return Response.status(400).build();
		}
		else {
			return Response.status(200).build();
		}
	}
	
	@POST
	@Path("/reviews/add")
//	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response addReview(@HeaderParam("x-session-token") String sSessionId, ReviewViewModel oReviewViewModel) {//
	
		User oUser = getUser(sSessionId);
		// Check the user session
		if(oUser == null){
			return Response.status(401).build();
		}
		
		if(oReviewViewModel == null ){
			return Response.status(400).build();
		}		
		
		String sUserId = oUser.getUserId();
		//CHEK USER ID TOKEN AND USER ID IN VIEW MODEL ARE === 
		if(oReviewViewModel.getUserId().toLowerCase().equals(sUserId.toLowerCase()) == false){
			return Response.status(400).build();
		}
		
				
		//CHECK THE VALUE OF THE VOTE === 1 - 5
		if( isValidVote(oReviewViewModel.getVote()) == false ){
			return Response.status(400).build();
		}
		
		ReviewRepository oReviewRepository =  new ReviewRepository();
		
		//ADD DATE 
		Date oDate = new Date();
		oReviewViewModel.setDate(oDate);
		
		Review oReview = getReviewFromViewModel(oReviewViewModel);
		
		//LIMIT THE NUMBER OF COMMENTS
		if(oReviewRepository.alreadyVoted(oReview) == true){
			return Response.status(400).build();
		}
		
		// ADD ID 
		oReview.setId(Utils.GetRandomName()); 
		
		oReviewRepository.addReview(oReview);
		
		return Response.status(200).build();
	}
	
	@GET
	@Path("/reviews/get")
	public Response getReview (@HeaderParam("x-session-token") String sSessionId, @QueryParam("processorId") String sProcessorId ) {


		User oUser = getUser(sSessionId);
		// Check the user session
		if(oUser == null){
			return Response.status(401).build();
		}

		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
		
		if(oProcessor != null && Utils.isNullOrEmpty(oProcessor.getName()) ) {
			return Response.status(400).build();
		}
		
		ReviewRepository oReviewRepository =  new ReviewRepository();
		
		List<Review> aoReviewRepository = oReviewRepository.getReviews(sProcessorId);
		
		if(aoReviewRepository == null || aoReviewRepository.size() == 0){
			  return Response.ok(null).build();
		}
		
		ListReviewsViewModel oListReviewsViewModel = getListReviewsViewModel(aoReviewRepository);
		
	    return Response.ok(oListReviewsViewModel).build();

	}
	
	
	
	private boolean isValidVote(Float fVote){
		if (fVote>=0.0 && fVote<=5.0) return true;
		else return false;
	}
	
	private Review getReviewFromViewModel(ReviewViewModel oReviewViewModel){
		if(oReviewViewModel != null){
			Review oReview = new Review();
			oReview.setComment(oReviewViewModel.getComment());
			oReview.setDate((double)oReviewViewModel.getDate().getTime());
			oReview.setId(oReviewViewModel.getId());//TODO GENERATE ID 
			oReview.setProcessorId(oReviewViewModel.getProcessorId());
			oReview.setUserId(oReviewViewModel.getUserId());
			oReview.setVote(oReviewViewModel.getVote());
			return oReview;
		}
		return null;
	}
	
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
			oReviewViewModel.setProcessorId(oReview.getUserId());
			oReviewViewModel.setVote(oReview.getVote());
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
	
	private int getNumberOfVotes(List<ReviewViewModel> aoReviews, int iReferenceVote ){
		int iNumberOfVotes = 0;
		for(ReviewViewModel oReview : aoReviews){
			if( oReview.getVote() == ((float)iReferenceVote)){
				iNumberOfVotes++;
			}
			
		}
		return iNumberOfVotes;
	}
	
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
	


	
	// return a free name for the image (if is possible) 
	private String getAvaibleFileName(String sPathFolder) {
		File oFolder = new File(sPathFolder);
		File[] aoListOfFiles = oFolder.listFiles();

		String sReturnValueName = "";
		boolean bIsAvaibleName = false; 
		for (String sAvaibleFileName : IMAGES_NAME){
			bIsAvaibleName = true;
			sReturnValueName = sAvaibleFileName;
			
			for (File oImage : aoListOfFiles){ 
				String sName = oImage.getName();
				String sFileName = FilenameUtils.removeExtension(sName);	
				
				if(sAvaibleFileName.equalsIgnoreCase(sFileName)){
					bIsAvaibleName = false;
					break;
				} 
				
			}
			
			if(bIsAvaibleName == true){
				break;
			}
			sReturnValueName = "";
		 }

		return sReturnValueName;
	}
	
	
	protected User getUser(String sSessionId){
		
		if (Utils.isNullOrEmpty(sSessionId)) {
			return null;
		}
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		
		if (oUser == null) {
			return null;
		}
		if (Utils.isNullOrEmpty(oUser.getUserId())) {
			return null;
		}
		return oUser;	
	}

}

