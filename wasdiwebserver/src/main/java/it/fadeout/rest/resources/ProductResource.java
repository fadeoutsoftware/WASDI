package it.fadeout.rest.resources;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import it.fadeout.Wasdi;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.ProductWorkspace;
import wasdi.shared.business.PublishedBand;
import wasdi.shared.business.User;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.ProductWorkspaceRepository;
import wasdi.shared.data.PublishedBandsRepository;
import wasdi.shared.geoserver.GeoserverMethods;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.BandViewModel;
import wasdi.shared.viewmodels.MetadataViewModel;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.ProductViewModel;

@Path("/product")
public class ProductResource {

	@Context
	ServletConfig m_oServletConfig;

	@GET
	@Path("addtows")
	@Produces({"application/xml", "application/json", "text/xml"})	
	public PrimitiveResult AddProductToWorkspace(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sProductName") String sProductName, @QueryParam("sWorkspaceId") String sWorkspaceId ) {

		// Validate Session
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null) return null;
		if (Utils.isNullOrEmpty(oUser.getUserId())) return null;

		// Create the entity
		ProductWorkspace oProductWorkspace = new ProductWorkspace();
		oProductWorkspace.setProductName(sProductName);
		oProductWorkspace.setWorkspaceId(sWorkspaceId);

		ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();

		// Try to insert
		if (oProductWorkspaceRepository.InsertProductWorkspace(oProductWorkspace)) {
			// Ok done
			PrimitiveResult oResult = new PrimitiveResult();
			oResult.setBoolValue(true);
			return oResult;
		}
		else {
			// There was a problem
			PrimitiveResult oResult = new PrimitiveResult();
			oResult.setBoolValue(false);

			return oResult;
		}
	}


	@GET
	@Path("byname")
	@Produces({"application/xml", "application/json", "text/xml"})	
	public ProductViewModel GetByProductName(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sProductName") String sProductName) {

		// Validate Session
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null) return null;
		if (Utils.isNullOrEmpty(oUser.getUserId())) return null;

		// Read the product from db
		DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
		DownloadedFile oDownloadedFile = oDownloadedFilesRepository.GetDownloadedFile(sProductName);

		if (oDownloadedFile != null) {
			// Ok read
			return oDownloadedFile.getProductViewModel();
		}
		else {
			// There was a problem
			return null;
		}
	}
	
	

	@GET
	@Path("metadatabyname")
	@Produces({"application/xml", "application/json", "text/xml"})	
	public MetadataViewModel GetMetadataByProductName(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sProductName") String sProductName) {

		// Validate Session
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null) return null;
		if (Utils.isNullOrEmpty(oUser.getUserId())) return null;

		// Read the product from db
		DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
		DownloadedFile oDownloadedFile = oDownloadedFilesRepository.GetDownloadedFile(sProductName);

		if (oDownloadedFile != null) {
			if (oDownloadedFile.getProductViewModel() != null) {
				// Ok read
				return oDownloadedFile.getProductViewModel().getMetadata();
				
			}
		}
		
		// There was a problem
		return null;
	}
	
	

	@GET
	@Path("/byws")
	@Produces({"application/xml", "application/json", "text/xml"})
	public ArrayList<ProductViewModel> GetListByWorkspace(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sWorkspaceId") String sWorkspaceId) {

		User oUser = Wasdi.GetUserFromSession(sSessionId);

		ArrayList<ProductViewModel> aoProductList = new ArrayList<>();

		try {

			// Domain Check
			if (oUser == null) {
				return aoProductList;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				return aoProductList;
			}


			System.out.println("ProductResource.GetListByWorkspace: products for " + sWorkspaceId);

			// Create repo
			ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();
			DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
			PublishedBandsRepository oPublishedBandsRepository = new PublishedBandsRepository();			

			// Get Product List
			List<ProductWorkspace> aoProductWorkspace = oProductWorkspaceRepository.GetProductsByWorkspace(sWorkspaceId);

			// For each
			for (int iProducts=0; iProducts<aoProductWorkspace.size(); iProducts++) {

				// Get the downloaded file
				DownloadedFile oDownloaded = oDownloadedFilesRepository.GetDownloadedFile(aoProductWorkspace.get(iProducts).getProductName());

				// Add View model to return list
				if (oDownloaded != null) {
					ProductViewModel oProductVM = oDownloaded.getProductViewModel();
					
					ArrayList<BandViewModel> aoBands = oProductVM.getBandsGroups().getBands(); 
					
					for (int iBands=0; iBands<aoBands.size(); iBands++) {
						
						BandViewModel oBand = aoBands.get(iBands);
						
						if (oBand != null) {
							PublishedBand oPublishBand = oPublishedBandsRepository.GetPublishedBand(oProductVM.getFileName(), oBand.getName());

							if (oPublishBand != null)
							{
								oBand.setPublished(true);
							}
						}
							
					}
					
					oProductVM.setMetadata(null);
					aoProductList.add(oProductVM);
				}

			}

		}
		catch (Exception oEx) {
			oEx.toString();
		}

		return aoProductList;
	}
	
	@POST
	@Path("/update")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response UpdateProductViewModel(@HeaderParam("x-session-token") String sSessionId, ProductViewModel oProductViewModel) {

		User oUser = Wasdi.GetUserFromSession(sSessionId);

		try {

			// Domain Check
			if (oUser == null) {
				return Response.status(401).build();
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				return Response.status(401).build();
			}
			
			if (oProductViewModel==null) {
				return Response.status(500).build();
			}


			System.out.println("ProductResource.UpdateProductViewModel: product " + oProductViewModel.getFileName());

			// Create repo
			DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
			
			DownloadedFile oDownlaoded = oDownloadedFilesRepository.GetDownloadedFile(oProductViewModel.getFileName());
			
			if (oDownlaoded == null) {
				System.out.println("ProductResource.UpdateProductViewModel: Associated downloaded file not found.");
				return Response.status(500).build();
			}
			
			// P.Campanella 26/05/2017: keep safe the metadata view model that is not exchanged in the API
			oProductViewModel.setMetadata(oDownlaoded.getProductViewModel().getMetadata());
			// Set the updated one
			oDownlaoded.setProductViewModel(oProductViewModel);
			
			// Save
			if (oDownloadedFilesRepository.UpdateDownloadedFile(oDownlaoded) == false) {
				System.out.println("ProductResource.UpdateProductViewModel: There was an error updating Downloaded File.");
				return Response.status(500).build();
			}


		}
		catch (Exception oEx) {
			System.out.println("ProductResource.UpdateProductViewModel: Exception " + oEx.toString());
			oEx.toString();
			return Response.status(500).build();
		}
		
		System.out.println("ProductResource.UpdateProductViewModel: Updated ");

		return Response.status(200).build();
	}
	


	@GET
	@Path("delete")
	@Produces({"application/xml", "application/json", "text/xml"})	
	public Response DeleteProduct(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sProductName") String sProductName, @QueryParam("bDeleteFile") Boolean bDeleteFile, @QueryParam("sWorkspaceId") String sWorkspace, @QueryParam("bDeleteLayer") Boolean bDeleteLayer) 
	{
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		try {

			// Domain Check
			if (oUser == null) {
				return null;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				return null;
			}

			ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();
			DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();

			String sUserId = oUser.getUserId();

			String sDownloadRootPath = "";
			if (m_oServletConfig.getInitParameter("DownloadRootPath") != null) {
				sDownloadRootPath = m_oServletConfig.getInitParameter("DownloadRootPath");
				if (!m_oServletConfig.getInitParameter("DownloadRootPath").endsWith("/"))
					sDownloadRootPath += "/";
			}

			String sDownloadPath = sDownloadRootPath + sUserId+ "/" + sWorkspace;
			System.out.println("ProductResource.DeleteProduct: Download Path: " + sDownloadPath);
			String sFilePath = sDownloadPath + "/" +  sProductName;
			System.out.println("ProductResource.DeleteProduct: File Path: " + sFilePath);

			PublishedBandsRepository oPublishedBandsRepository = new PublishedBandsRepository();

			List<PublishedBand> aoPublishedBands = null;
			if (bDeleteFile || bDeleteLayer)
				//Get all bands files
				aoPublishedBands = oPublishedBandsRepository.GetPublishedBandsByProductName(sProductName);

			//get files that begin with the product name
			if (bDeleteFile)
			{
				final List<PublishedBand> aoLocalPublishedBands = aoPublishedBands;
				File oFolder = new File(sDownloadPath);
				FilenameFilter oFilter = new FilenameFilter() {

					@Override
					public boolean accept(File dir, String name) {
						// TODO Auto-generated method stub
						if (name.toLowerCase().equals(sProductName.toLowerCase()))
							return true;

						if (aoLocalPublishedBands != null)
						{
							for (PublishedBand oPublishedBand : aoLocalPublishedBands) {
								if (name.toLowerCase().contains(oPublishedBand.getLayerId().toLowerCase()))
									return true;
							}
						}

						return false;
					}
				};
				File[] aoFiles = oFolder.listFiles(oFilter);
				if (aoFiles != null)
				{
					System.out.println("ProductResource.DeleteProduct: Number of files to delete " + aoFiles.length);
					for (File oFile : aoFiles) {
						try
						{
							if (!oFile.isDirectory())
								oFile.delete();
						}
						catch(Exception oEx)
						{
							System.out.println("ProductResource.DeleteProduct: error deleting file product " + oEx.toString());
						}

					}
				}
			}

			if(bDeleteLayer)
			{
				//Delete layerId on Geoserver
				for (PublishedBand publishedBand : aoPublishedBands) {
					try
					{
						System.out.println("ProductResource.DeleteProduct: LayerId to delete " + publishedBand.getLayerId());
						String sResult = GeoserverMethods.DeleteLayer(publishedBand.getLayerId(), "json");

						try
						{
							//delete published band on data base
							oPublishedBandsRepository.DeleteByProductNameLayerId(sProductName, publishedBand.getLayerId());
						}
						catch(Exception oEx)
						{
							System.out.println("ProductResource.DeleteProduct: error deleting published band on data base " + oEx.toString());
						}

					}
					catch(Exception oEx)
					{
						System.out.println("ProductResource.DeleteProduct: error deleting layer id " + oEx.toString());
					}
				}
			}

			//delete product record on db
			try{
				oProductWorkspaceRepository.DeleteByProductNameWorkspace(sProductName, sWorkspace);
				oDownloadedFilesRepository.DeleteByFilePath(sFilePath);
			}
			catch (Exception oEx) {
				System.out.println("ProductResource.DeleteProduct: error deleting product " + oEx.toString());
				return null;	
			}


		}
		catch (Exception oEx) {
			System.out.println("ProductResource.DeleteProduct: error deleting product " + oEx.toString());
			return null;
		}

		return Response.ok().build();

	}


}
