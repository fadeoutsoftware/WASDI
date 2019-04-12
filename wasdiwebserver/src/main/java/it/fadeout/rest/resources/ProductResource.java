package it.fadeout.rest.resources;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.glassfish.jersey.media.multipart.FormDataParam;

import it.fadeout.Wasdi;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.ProductWorkspace;
import wasdi.shared.business.PublishedBand;
import wasdi.shared.business.User;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProductWorkspaceRepository;
import wasdi.shared.data.PublishedBandsRepository;
import wasdi.shared.geoserver.GeoServerManager;
import wasdi.shared.parameters.IngestFileParameter;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.BandViewModel;
import wasdi.shared.viewmodels.GeorefProductViewModel;
import wasdi.shared.viewmodels.MetadataViewModel;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.ProductInfoViewModel;
import wasdi.shared.viewmodels.ProductViewModel;

@Path("/product")
public class ProductResource {

	@Context
	ServletConfig m_oServletConfig;

	@GET
	@Path("addtows")
	@Produces({"application/xml", "application/json", "text/xml"})	
	public PrimitiveResult addProductToWorkspace(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sProductName") String sProductName, @QueryParam("sWorkspaceId") String sWorkspaceId ) {


		System.out.println("ProductResource.AddProductToWorkspace:  called WS: " + sWorkspaceId + " Product " + sProductName);
		
		// Validate Session
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null) return null;
		if (Utils.isNullOrEmpty(oUser.getUserId())) return null;
		
		
		String sPath = Wasdi.getProductPath(m_oServletConfig, oUser.getUserId(), sWorkspaceId);

		// Create the entity
		ProductWorkspace oProductWorkspace = new ProductWorkspace();
		oProductWorkspace.setProductName(sPath+sProductName);
		oProductWorkspace.setWorkspaceId(sWorkspaceId);

		ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();
		
		if (oProductWorkspaceRepository.ExistsProductWorkspace(oProductWorkspace.getProductName(), oProductWorkspace.getWorkspaceId())) {
			System.out.println("ProductResource.AddProductToWorkspace:  Product already in the workspace");
			
			// Ok done
			PrimitiveResult oResult = new PrimitiveResult();
			oResult.setBoolValue(true);
			return oResult;			
		}

		// Try to insert
		if (oProductWorkspaceRepository.InsertProductWorkspace(oProductWorkspace)) {
			
			System.out.println("ProductResource.AddProductToWorkspace:  Inserted");
			
			// Ok done
			PrimitiveResult oResult = new PrimitiveResult();
			oResult.setBoolValue(true);
			return oResult;
		}
		else {
			System.out.println("ProductResource.AddProductToWorkspace:  Error");
			
			// There was a problem
			PrimitiveResult oResult = new PrimitiveResult();
			oResult.setBoolValue(false);

			return oResult;
		}
	}


	@GET
	@Path("byname")
	@Produces({"application/xml", "application/json", "text/xml"})	
	public GeorefProductViewModel getByProductName(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sProductName") String sProductName) {
		
		Wasdi.DebugLog("ProductResource.GetByProductName");

		// Validate Session
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null) return null;
		if (Utils.isNullOrEmpty(oUser.getUserId())) return null;

		// Read the product from db
		DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
		DownloadedFile oDownloadedFile = oDownloadedFilesRepository.GetDownloadedFile(sProductName);
		
		if (oDownloadedFile != null) {
			GeorefProductViewModel oGeoViewModel = new GeorefProductViewModel(oDownloadedFile.getProductViewModel());
			oGeoViewModel.setBbox(oDownloadedFile.getBoundingBox());
			
			// Ok read
			return oGeoViewModel;
		}
		else {
			// There was a problem
			return null;
		}
	}
	
	

	@GET
	@Path("metadatabyname")
	@Produces({"application/xml", "application/json", "text/xml"})	
	public MetadataViewModel getMetadataByProductName(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sProductName") String sProductName) {
		
		Wasdi.DebugLog("ProductResource.GetMetadataByProductName");

		// Validate Session
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null) return null;
		if (Utils.isNullOrEmpty(oUser.getUserId())) return null;

		// Read the product from db
		DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
		DownloadedFile oDownloadedFile = oDownloadedFilesRepository.GetDownloadedFile(sProductName);

		if (oDownloadedFile != null) {
			if (oDownloadedFile.getProductViewModel() != null) {
				
				try {				
					String sMetadataPath = "";
					
					if (m_oServletConfig.getInitParameter("MetadataPath") != null) {
						sMetadataPath = m_oServletConfig.getInitParameter("MetadataPath");
						if (!m_oServletConfig.getInitParameter("MetadataPath").endsWith("/"))
							sMetadataPath += "/";
					}
					
					
					String sMetadataFile = oDownloadedFile.getProductViewModel().getMetadataFileReference();
					
					if (sMetadataFile == null) {
						System.out.println("ProductResource.GetMetadataByProductName: MetadataFile = null for product " + oDownloadedFile.getFilePath());
						return null;
					}
					
					MetadataViewModel oReloaded = (MetadataViewModel) SerializationUtils.deserializeXMLToObject(sMetadataPath+sMetadataFile);
					System.out.println("ProductResource.GetMetadataByProductName: return Metadata for product " + oDownloadedFile.getFilePath());
					// Ok return Metadata
					return oReloaded;					
				}
				catch (Exception oEx) {
					System.out.println("ProductResource.GetMetadataByProductName: exception");
					oEx.printStackTrace();
					return null;
				}
				
				
			}
		}
		
		// There was a problem
		return null;
	}
	
	
	//XXX Remove legacy method: it's not used by anyone and it does not return anything but null
	@GET
	@Path("info")
	@Produces({"application/xml", "application/json", "text/xml"})
	public ProductInfoViewModel getInfo(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sProductName") String sProductName) {
		
		Wasdi.DebugLog("ProductResource.GetInfo");

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
				//TODO this is useless, return something
				//MetadataViewModel metadata = oDownloadedFile.getProductViewModel().getMetadata();
			}
		}		
		// There was a problem
		return null;
	}


	@GET
	@Path("/byws")
	@Produces({"application/xml", "application/json", "text/xml"})
	public ArrayList<GeorefProductViewModel> getListByWorkspace(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sWorkspaceId") String sWorkspaceId) {
		
		Wasdi.DebugLog("ProductResource.GetListByWorkspace");

		User oUser = Wasdi.GetUserFromSession(sSessionId);

		ArrayList<GeorefProductViewModel> aoProductList = new ArrayList<GeorefProductViewModel>();

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
			
			System.out.println("ProductResource.GetListByWorkspace: found " + aoProductWorkspace.size());

			// For each
			for (int iProducts=0; iProducts<aoProductWorkspace.size(); iProducts++) {

				// Get the downloaded file
				DownloadedFile oDownloaded = oDownloadedFilesRepository.GetDownloadedFile(aoProductWorkspace.get(iProducts).getProductName());

				// Add View model to return list
				if (oDownloaded != null) {
					
					ProductViewModel pVM = oDownloaded.getProductViewModel();
					
					
					if (pVM != null) {
						GeorefProductViewModel geoPVM = new GeorefProductViewModel(pVM);
						geoPVM.setBbox(oDownloaded.getBoundingBox());
						
						if (geoPVM.getBandsGroups() != null) {
							ArrayList<BandViewModel> aoBands = geoPVM.getBandsGroups().getBands();
							
							if (aoBands != null) {
								for (int iBands=0; iBands<aoBands.size(); iBands++) {
									
									BandViewModel oBand = aoBands.get(iBands);
									
									if (oBand != null) {
										PublishedBand oPublishBand = oPublishedBandsRepository.GetPublishedBand(geoPVM.getName(), oBand.getName());

										if (oPublishBand != null) {
											oBand.setPublished(true);
											oBand.setLayerId(oPublishBand.getLayerId());
											oBand.setGeoserverBoundingBox(oPublishBand.getGeoserverBoundingBox());
										}
										else {
											oBand.setPublished(false);
										}
									}
										
								}								
							}
							
						}

						geoPVM.setMetadata(null);
						aoProductList.add(geoPVM);
								

					} else {
						System.out.println("ProductResource.GetListByWorkspace: ProductViewModel is Null: jump product");
					}

				} else {
					System.out.println("WARNING: the product " + aoProductWorkspace.get(iProducts).getProductName() + " should be in WS " + sWorkspaceId + " but is not a Downloaded File" );
				}
			}
		} catch (Exception oEx) {
			oEx.toString();
		}

		return aoProductList;
	}
	
	@POST
	@Path("/update")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response updateProductViewModel(@HeaderParam("x-session-token") String sSessionId, ProductViewModel oProductViewModel) {
		
		Wasdi.DebugLog("ProductResource.UpdateProductViewModel");

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
	
	@POST
	@Path("/uploadfile")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadFile(@FormDataParam("file") InputStream fileInputStream, @HeaderParam("x-session-token") String sSessionId, 
			@QueryParam("workspace") String sWorkspace,@QueryParam("name") String sName) throws Exception 
	{
		Wasdi.DebugLog("ProductResource.uploadfile");
	
		// Check the user session
		if (Utils.isNullOrEmpty(sSessionId)) 
		{
			return Response.status(401).build();
		}
		
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser==null) 
		{
			return Response.status(401).build();
		}
		if (Utils.isNullOrEmpty(oUser.getUserId())) 
		{
			return Response.status(401).build();
		}
		String sUserId = oUser.getUserId();
		
		// Check the file name
		if(Utils.isNullOrEmpty(sName) || sName.isEmpty())
		{
			sName="defaultName";
		}
		
		
		// Take path
		String sPath = Wasdi.getProductPath(m_oServletConfig, sUserId, sWorkspace);
		
		File oOutputFilePath = new File(sPath + sName);
		
		if (oOutputFilePath.getParentFile().exists() == false) {
			oOutputFilePath.getParentFile().mkdirs();
		}
		
		// Copy the stream
		int iRead = 0;
		byte[] ayBytes = new byte[1024];
		OutputStream oOutStream = new FileOutputStream(oOutputFilePath);
		while ((iRead = fileInputStream.read(ayBytes)) != -1) {
			oOutStream.write(ayBytes, 0, iRead);
		}
		oOutStream.flush();
		oOutStream.close();
		
		// Start ingestion
		try {
			ProcessWorkspace oProcess = null;
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();

			String sProcessObjId = Utils.GetRandomName();

			IngestFileParameter oParameter = new IngestFileParameter();
			oParameter.setWorkspace(sWorkspace);
			oParameter.setUserId(sUserId);
			oParameter.setExchange(sWorkspace);
			oParameter.setFilePath(oOutputFilePath.getAbsolutePath());
			oParameter.setProcessObjId(sProcessObjId);

			sPath = m_oServletConfig.getInitParameter("SerializationPath") + sProcessObjId;
			SerializationUtils.serializeObjectToXML(sPath, oParameter);

			try
			{
				oProcess = new ProcessWorkspace();
				oProcess.setOperationDate(Wasdi.GetFormatDate(new Date()));
				oProcess.setOperationType(LauncherOperations.INGEST.name());
				oProcess.setProductName(oOutputFilePath.getName());
				oProcess.setWorkspaceId(sWorkspace);
				oProcess.setUserId(sUserId);
				oProcess.setProcessObjId(sProcessObjId);
				oProcess.setStatus(ProcessStatus.CREATED.name());
				oRepository.InsertProcessWorkspace(oProcess);
				Wasdi.DebugLog("ProductResource.uploadfile: Process Scheduled for Launcher");
			}
			catch(Exception oEx){
				System.out.println("ProductResource.uploadfile: Error updating process list " + oEx.getMessage());
				oEx.printStackTrace();
				Response.status(500).build();
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return Response.status(200).build();
	}


	@GET
	@Path("delete")
	@Produces({"application/xml", "application/json", "text/xml"})	
	public PrimitiveResult deleteProduct(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sProductName") String sProductName, @QueryParam("bDeleteFile") Boolean bDeleteFile, @QueryParam("sWorkspaceId") String sWorkspace, @QueryParam("bDeleteLayer") Boolean bDeleteLayer) 
	{
		Wasdi.DebugLog("ProductResource.DeleteProduct");
		
		PrimitiveResult oReturn = new PrimitiveResult();
		oReturn.setBoolValue(false);
		
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		try {

			// Domain Check
			if (oUser == null) {
				oReturn.setIntValue(404);
				return oReturn;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				oReturn.setIntValue(404);
				return oReturn;
			}

			ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();
			DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
			DownloadedFile oDownloadedFile = oDownloadedFilesRepository.GetDownloadedFile(sProductName);

			String sDownloadPath = Wasdi.getProductPath(m_oServletConfig, oUser.getUserId(), sWorkspace);
			System.out.println("ProductResource.DeleteProduct: Download Path: " + sDownloadPath);
			String sFilePath = sDownloadPath + "/" +  sProductName;
			System.out.println("ProductResource.DeleteProduct: File Path: " + sFilePath);

			PublishedBandsRepository oPublishedBandsRepository = new PublishedBandsRepository();

			List<PublishedBand> aoPublishedBands = null;
			
			if (bDeleteFile || bDeleteLayer) {
				//Get all bands files
				aoPublishedBands = oPublishedBandsRepository.GetPublishedBandsByProductName(sProductName);				
			}

			//get files that begin with the product name
			if (bDeleteFile)
			{
				final List<PublishedBand> aoLocalPublishedBands = aoPublishedBands;
				File oFolder = new File(sDownloadPath);
				FilenameFilter oFilter = new FilenameFilter() {

					@Override
					public boolean accept(File dir, String name) {

						if (name.equalsIgnoreCase(sProductName)) {
							return true;
						}
						
						if (sProductName.endsWith(".dim")) {
							String baseName = sProductName.substring(0, sProductName.length()-4);
							if (name.equalsIgnoreCase(baseName + ".data")) {
								return true;
							}
						}

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
				if (aoFiles != null) {
					System.out.println("ProductResource.DeleteProduct: Number of files to delete " + aoFiles.length);
					for (File oFile : aoFiles) {
						
						System.out.print("ProductResource.DeleteProduct: deleting file product " + oFile.getAbsolutePath() + "...");
						if (!FileUtils.deleteQuietly(oFile)) {
							System.out.println("    ERROR");
						} else {
							System.out.println("    OK");
						}
					}
				}
				else {
					System.out.println("ProductResource.DeleteProduct: No File to delete ");
				}
			}

			if(bDeleteLayer)
			{
				//Delete layerId on Geoserver
				
				GeoServerManager gsManager = new GeoServerManager(m_oServletConfig.getInitParameter("GS_URL"), m_oServletConfig.getInitParameter("GS_USER"),  m_oServletConfig.getInitParameter("GS_PASSWORD"));
				
				for (PublishedBand publishedBand : aoPublishedBands) {
					try
					{
						System.out.println("ProductResource.DeleteProduct: LayerId to delete " + publishedBand.getLayerId());

						if (!gsManager.removeLayer(publishedBand.getLayerId())) {
							System.out.println("ProductResource.DeleteProduct: error deleting layer " + publishedBand.getLayerId() + " from geoserver");
						}

						try {
							//delete published band on data base
							oPublishedBandsRepository.DeleteByProductNameLayerId(oDownloadedFile.getProductViewModel().getName(), publishedBand.getLayerId());
						} catch(Exception oEx) {
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
				oReturn.setIntValue(500);
				oReturn.setStringValue(oEx.toString());
				return oReturn;	
			}


		}
		catch (Exception oEx) {
			System.out.println("ProductResource.DeleteProduct: error deleting product " + oEx.toString());
			oReturn.setIntValue(500);
			oReturn.setStringValue(oEx.toString());
			return oReturn;
		}
		
		oReturn.setBoolValue(true);
		oReturn.setIntValue(200);

		return oReturn;
	}


}
