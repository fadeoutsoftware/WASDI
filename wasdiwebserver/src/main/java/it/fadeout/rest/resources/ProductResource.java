package it.fadeout.rest.resources;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
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
import wasdi.shared.business.ProductWorkspace;
import wasdi.shared.business.PublishedBand;
import wasdi.shared.business.User;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.ProductWorkspaceRepository;
import wasdi.shared.data.PublishedBandsRepository;
import wasdi.shared.geoserver.GeoServerManager;
import wasdi.shared.parameters.IngestFileParameter;
import wasdi.shared.parameters.ReadMetadataParameter;
import wasdi.shared.rabbit.Send;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.BandViewModel;
import wasdi.shared.viewmodels.GeorefProductViewModel;
import wasdi.shared.viewmodels.MetadataViewModel;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.ProductViewModel;

@Path("/product")
public class ProductResource {

	@Context
	ServletConfig m_oServletConfig;

	@GET
	@Path("addtows")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public PrimitiveResult addProductToWorkspace(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("sProductName") String sProductName, @QueryParam("sWorkspaceId") String sWorkspaceId) {
		try {
			Utils.debugLog("ProductResource.AddProductToWorkspace:  WS: " + sWorkspaceId + " Product " + sProductName);
	
			// Validate Session
			User oUser = Wasdi.getUserFromSession(sSessionId);
			if (oUser == null) {
				Utils.debugLog("ProductResource.AddProductToWorkspace:  WS: " + sWorkspaceId + " Product " + sProductName + " ): invalid session");
				return null;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId()))
				return null;
	
			String sPath = Wasdi.getWorkspacePath(m_oServletConfig, Wasdi.getWorkspaceOwner(sWorkspaceId), sWorkspaceId);
	
			// Create the entity
			ProductWorkspace oProductWorkspace = new ProductWorkspace();
			oProductWorkspace.setProductName(sPath + sProductName);
			oProductWorkspace.setWorkspaceId(sWorkspaceId);
	
			ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();
	
			if (oProductWorkspaceRepository.existsProductWorkspace(oProductWorkspace.getProductName(), oProductWorkspace.getWorkspaceId())) {
				Utils.debugLog("ProductResource.AddProductToWorkspace:  Product already in the workspace");
	
				// Ok done
				PrimitiveResult oResult = new PrimitiveResult();
				oResult.setBoolValue(true);
				return oResult;
			}
			
			DownloadedFilesRepository oDownFileRepo = new DownloadedFilesRepository();
			DownloadedFile oDownFile = oDownFileRepo.getDownloadedFileByPath(oProductWorkspace.getProductName());
			
			if (oDownFile != null) {
				GeorefProductViewModel oGeoRefViewModel = new GeorefProductViewModel(oDownFile.getProductViewModel());
				if (oGeoRefViewModel!=null) oProductWorkspace.setBbox(oGeoRefViewModel.getBbox());
			}
	
			// Try to insert
			if (oProductWorkspaceRepository.insertProductWorkspace(oProductWorkspace)) {
	
				Utils.debugLog("ProductResource.AddProductToWorkspace:  Inserted");
	
				// Ok done
				PrimitiveResult oResult = new PrimitiveResult();
				oResult.setBoolValue(true);
				return oResult;
			} else {
				Utils.debugLog("ProductResource.AddProductToWorkspace:  Error");
	
				// There was a problem
				PrimitiveResult oResult = new PrimitiveResult();
				oResult.setBoolValue(false);
	
				return oResult;
			}
		} catch (Exception oE) {
			Utils.debugLog("ProductResource.AddProductToWorkspace:  WS: " + sWorkspaceId + " Product " + sProductName);
		}
		return PrimitiveResult.getInvalid();
	}

	@GET
	@Path("byname")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public GeorefProductViewModel getByProductName(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("sProductName") String sProductName, @QueryParam("workspace") String sWorkspace) {
		try {
			Utils.debugLog("ProductResource.GetByProductName(Product: " + sProductName + ", WS: " + sWorkspace + " )");

			// Validate Session
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser == null) {
				Utils.debugLog("ProductResource.GetByProductName: invalid session");
				return null;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) return null;

			String sFullPath = Wasdi.getWorkspacePath(m_oServletConfig, Wasdi.getWorkspaceOwner(sWorkspace), sWorkspace);

			// Read the product from db
			DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
			DownloadedFile oDownloadedFile = oDownloadedFilesRepository.getDownloadedFileByPath(sFullPath + sProductName);

			Utils.debugLog("ProductResource.GetByProductName: search file " + sFullPath + sProductName);

			if (oDownloadedFile != null) {

				Utils.debugLog("ProductResource.GetByProductName: product found");

				GeorefProductViewModel oGeoViewModel = new GeorefProductViewModel(oDownloadedFile.getProductViewModel());
				oGeoViewModel.setBbox(oDownloadedFile.getBoundingBox());

				// Ok read
				return oGeoViewModel;
			} else {
				Utils.debugLog("ProductResource.GetByProductName: product not found");
			}
		}catch (Exception oE) {
			Utils.debugLog("ProductResource.GetByProductName( Product: " + sProductName + ", WS: " + sWorkspace + " ): " + oE);
		}
		return null;
	}

	@GET
	@Path("metadatabyname")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public MetadataViewModel getMetadataByProductName(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("sProductName") String sProductName, @QueryParam("workspace") String sWorkspaceId) {

		Utils.debugLog("ProductResource.GetMetadataByProductName( Product: " + sProductName + ", WS: " + sWorkspaceId + " )");

		// Validate Session
		User oUser = Wasdi.getUserFromSession(sSessionId);
		if (oUser == null)
			return null;
		if (Utils.isNullOrEmpty(oUser.getUserId()))
			return null;

		String sProductPath = Wasdi.getWorkspacePath(m_oServletConfig, Wasdi.getWorkspaceOwner(sWorkspaceId), sWorkspaceId);

		// Read the product from db
		DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
		DownloadedFile oDownloadedFile = oDownloadedFilesRepository.getDownloadedFileByPath(sProductPath + sProductName);
		
		
		MetadataViewModel oMetadataViewModel = null;

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
					
					// If we do not have a Metadata File
					if (Utils.isNullOrEmpty(sMetadataFile)) {
						
						Utils.debugLog("ProductResource.GetMetadataByProductName: MetadataFile = null for product " + oDownloadedFile.getFilePath());
						
						// Was it created before or not?
						if (oDownloadedFile.getProductViewModel().getMetadataFileCreated() == false) {
							
							Utils.debugLog("ProductResource.GetMetadataByProductName: first metadata request, create operation to read it");
							
							// Try to create it: get the user id
							String sUserId = oUser.getUserId();
							
							// Create an Operation Id
							String sProcessObjId = Utils.GetRandomName();
							
							// Create the Parameter 
							ReadMetadataParameter oParameter = new ReadMetadataParameter();
							oParameter.setProcessObjId(sProcessObjId);
							oParameter.setExchange(sWorkspaceId);
							oParameter.setWorkspace(sWorkspaceId);
							oParameter.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(sWorkspaceId));
							oParameter.setProductName(sProductName);
							oParameter.setUserId(sUserId);
							
							String sPath = m_oServletConfig.getInitParameter("SerializationPath");
							
							// Trigger the Launcher Operation
							Wasdi.runProcess(sUserId, sSessionId, LauncherOperations.READMETADATA.name(), sProductName, sPath, oParameter, null);
							
							oMetadataViewModel = new MetadataViewModel();
							oMetadataViewModel.setName("Generating Metadata, try later");
						}
						else {
							Utils.debugLog("ProductResource.GetMetadataByProductName: attemp to read metadata already done, just return");
						}
						
						return oMetadataViewModel;
					}

					MetadataViewModel oReloaded = (MetadataViewModel) SerializationUtils.deserializeXMLToObject(sMetadataPath + sMetadataFile);
					
					Utils.debugLog("ProductResource.GetMetadataByProductName: return Metadata for product " + oDownloadedFile.getFilePath());
					
					// Ok return Metadata
					return oReloaded;
				} catch (Exception oEx) {
					Utils.debugLog("ProductResource.GetMetadataByProductName: " + oEx);
					return null;
				}

			}
		}

		// There was a problem
		return null;
	}

	@GET
	@Path("/byws")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public List<GeorefProductViewModel> getListByWorkspace(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("sWorkspaceId") String sWorkspaceId) {
		
		Utils.debugLog("ProductResource.GetListByWorkspace( WS: " + sWorkspaceId + " )");
		
		List<GeorefProductViewModel> aoProductList = new ArrayList<GeorefProductViewModel>();
		if(Utils.isNullOrEmpty(sWorkspaceId)){
			Utils.debugLog("ProductResource.getListByWorkspace(" + sWorkspaceId + "): workspace is null or empty");
			return aoProductList;
		}

		try {
			User oUser = Wasdi.getUserFromSession(sSessionId);
			// Domain Check
			if (oUser == null) {
				Utils.debugLog("ProductResource.GetListByWorkspace: invalid session");
				return aoProductList;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				return aoProductList;
			}

			Utils.debugLog("ProductResource.GetListByWorkspace: products for " + sWorkspaceId);

			// Create repo
			ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();
			DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
			PublishedBandsRepository oPublishedBandsRepository = new PublishedBandsRepository();

			// Get Product List
			//List<ProductWorkspace> aoProductWorkspace = oProductWorkspaceRepository.getProductsByWorkspace(sWorkspaceId);
			
			List<DownloadedFile> aoDownloadedFiles = oDownloadedFilesRepository.getByWorkspace(sWorkspaceId);

			Utils.debugLog("ProductResource.GetListByWorkspace: found " + aoDownloadedFiles.size());

			// For each
			for (int iProducts = 0; iProducts < aoDownloadedFiles.size(); iProducts++) {

				// Get the downloaded file
				//DownloadedFile oDownloaded = oDownloadedFilesRepository.getDownloadedFileByPath(aoProductWorkspace.get(iProducts).getProductName());
				DownloadedFile oDownloaded = aoDownloadedFiles.get(iProducts);

				// Add View model to return list
				if (oDownloaded != null) {

					ProductViewModel oProductViewModel = oDownloaded.getProductViewModel();

					if (oProductViewModel != null) {
						GeorefProductViewModel oGeoRefProductViewModel = new GeorefProductViewModel(oProductViewModel);
						oGeoRefProductViewModel.setBbox(oDownloaded.getBoundingBox());

//						if (oGeoRefProductViewModel.getBandsGroups() != null) {
//							ArrayList<BandViewModel> aoBands = oGeoRefProductViewModel.getBandsGroups().getBands();
//
//							if (aoBands != null) {
//								for (int iBands = 0; iBands < aoBands.size(); iBands++) {
//
//									BandViewModel oBand = aoBands.get(iBands);
//
//									if (oBand != null) {
//										PublishedBand oPublishBand = oPublishedBandsRepository.getPublishedBand(oGeoRefProductViewModel.getName(), oBand.getName());
//
//										if (oPublishBand != null) {
//											oBand.setPublished(true);
//											oBand.setLayerId(oPublishBand.getLayerId());
//											oBand.setGeoserverBoundingBox(oPublishBand.getGeoserverBoundingBox());
//											oBand.setGeoserverUrl(oPublishBand.getGeoserverUrl());
//											
//										} 
//										else {
//											oBand.setPublished(false);
//										}
//									}
//
//								}
//							}
//						}

						oGeoRefProductViewModel.setMetadata(null);
						aoProductList.add(oGeoRefProductViewModel);

					} else {
						Utils.debugLog("ProductResource.GetListByWorkspace: ProductViewModel is Null: jump product");
					}

				} else {
					//Utils.debugLog("ProductResource.GetListByWorkspace: WARNING: the product " + aoProductWorkspace.get(iProducts).getProductName() + " should be in WS " + sWorkspaceId + " but is not a Downloaded File");
				}
			}
		} catch (Exception oEx) {
			Utils.debugLog("ProductResource.GetListByWorkspace: " + oEx);
		}

		return aoProductList;
	}

	
	@GET
	@Path("/bywslight")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public List<GeorefProductViewModel> getLightListByWorkspace(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sWorkspaceId") String sWorkspaceId) {
		
		Utils.debugLog("ProductResource.getLightListByWorkspace( WS: " + sWorkspaceId + " )");
		
		List<GeorefProductViewModel> aoProductList = new ArrayList<GeorefProductViewModel>();
		if(Utils.isNullOrEmpty(sWorkspaceId)) {
			Utils.debugLog("ProductResource.getLightListByWorkspace(" + sWorkspaceId + "): workspace is null or empty");
			return aoProductList;
		}

		try {
			User oUser = Wasdi.getUserFromSession(sSessionId);
			// Domain Check
			if (oUser == null) {
				Utils.debugLog("ProductResource.getLightListByWorkspace( WS: " + sWorkspaceId + " ): invalid session");
				return aoProductList;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				return aoProductList;
			}

			Utils.debugLog("ProductResource.getLightListByWorkspace: products for " + sWorkspaceId);

			// Create repo
			ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();

			// Get Product List
			List<ProductWorkspace> aoProductWorkspace = oProductWorkspaceRepository.getProductsByWorkspace(sWorkspaceId);

			Utils.debugLog("ProductResource.getLightListByWorkspace: found " + aoProductWorkspace.size());

			// For each
			for (int iProducts = 0; iProducts < aoProductWorkspace.size(); iProducts++) {
				GeorefProductViewModel oGeoPVM = new GeorefProductViewModel();
				oGeoPVM.setBbox(aoProductWorkspace.get(iProducts).getBbox());
				
				File oFile = new File(aoProductWorkspace.get(iProducts).getProductName());
				String sName = Utils.getFileNameWithoutLastExtension(oFile.getName());
				oGeoPVM.setProductFriendlyName(sName);
				oGeoPVM.setName(sName);
				
				aoProductList.add(oGeoPVM);
			}
		} catch (Exception oEx) {
			Utils.debugLog("ProductResource.getLightListByWorkspace: " + oEx);
		}

		return aoProductList;
	}
	
	@GET
	@Path("/namesbyws")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public ArrayList<String> getNamesByWorkspace(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("sWorkspaceId") String sWorkspaceId) {

		Utils.debugLog("ProductResource.getNamesByWorkspace( WS: " + sWorkspaceId + " )");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		ArrayList<String> aoProductList = new ArrayList<String>();

		try {

			// Domain Check
			if (oUser == null) {
				Utils.debugLog("ProductResource.getNamesByWorkspace( WS: " + sWorkspaceId + " ): invalid session");
				return aoProductList;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				return aoProductList;
			}

			Utils.debugLog("ProductResource.getNamesByWorkspace: products for " + sWorkspaceId);

			// Create repo
			ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();
			DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();

			// Get Product List
			List<ProductWorkspace> aoProductWorkspace = oProductWorkspaceRepository.getProductsByWorkspace(sWorkspaceId);

			Utils.debugLog("ProductResource.getNamesByWorkspace: found " + aoProductWorkspace.size());

			// For each
			for (int iProducts = 0; iProducts < aoProductWorkspace.size(); iProducts++) {

				// Get the downloaded file
				DownloadedFile oDownloaded = oDownloadedFilesRepository.getDownloadedFileByPath(aoProductWorkspace.get(iProducts).getProductName());

				// Add View model to return list
				if (oDownloaded != null) {
					aoProductList.add(oDownloaded.getFileName());

				} else {
					Utils.debugLog("ProductResource.getNamesByWorkspace: WARNING: the product "
							+ aoProductWorkspace.get(iProducts).getProductName() + " should be in WS " + sWorkspaceId
							+ " but is not a Downloaded File");
				}
			}
		} catch (Exception oEx) {
			Utils.debugLog("ProductResource.getNamesByWorkspace: " + oEx);
		}

		return aoProductList;
	}

	@POST
	@Path("/update")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response updateProductViewModel(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("workspace") String sWorkspace, ProductViewModel oProductViewModel) {

		Utils.debugLog("ProductResource.UpdateProductViewModel( WS: " + sWorkspace + ", ... )");


		try {

			// Domain Check
			User oUser = Wasdi.getUserFromSession(sSessionId);
			if (oUser == null) {
				Utils.debugLog("ProductResource.UpdateProductViewModel( WS: " + sWorkspace + ", ... ): invalid session");
				return Response.status(401).build();
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				return Response.status(401).build();
			}

			if (oProductViewModel == null) {
				return Response.status(500).build();
			}

			Utils.debugLog("ProductResource.UpdateProductViewModel: product " + oProductViewModel.getFileName());

			String sFullPath = Wasdi.getWorkspacePath(m_oServletConfig, Wasdi.getWorkspaceOwner(sWorkspace),
					sWorkspace);

			// Create repo
			DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();

			DownloadedFile oDownlaoded = oDownloadedFilesRepository
					.getDownloadedFileByPath(sFullPath + oProductViewModel.getFileName());

			if (oDownlaoded == null) {
				Utils.debugLog("ProductResource.UpdateProductViewModel: Associated downloaded file not found.");
				return Response.status(500).build();
			}

			// P.Campanella 26/05/2017: keep safe the metadata view model that is not
			// exchanged in the API
			oProductViewModel.setMetadata(oDownlaoded.getProductViewModel().getMetadata());
			// Set the updated one
			oDownlaoded.setProductViewModel(oProductViewModel);

			// Save
			if (oDownloadedFilesRepository.updateDownloadedFile(oDownlaoded) == false) {
				Utils.debugLog("ProductResource.UpdateProductViewModel: There was an error updating Downloaded File.");
				return Response.status(500).build();
			}

		} catch (Exception oEx) {
			Utils.debugLog("ProductResource.UpdateProductViewModel: " + oEx);
			return Response.status(500).build();
		}

		Utils.debugLog("ProductResource.UpdateProductViewModel: Updated ");

		return Response.status(200).build();
	}
	
	
	@POST
	@Path("/uploadfile")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadFile(@FormDataParam("file") InputStream fileInputStream, @HeaderParam("x-session-token") String sSessionId, @QueryParam("workspace") String sWorkspaceId, @QueryParam("name") String sName) throws Exception {
		Utils.debugLog("ProductResource.uploadfile( InputStream, WS: " + sWorkspaceId + ", Name: " + sName + " )");

		// before any operation check that this is not an injection attempt from the user
		if (sName.contains("/") || sName.contains("\\") || sWorkspaceId.contains("/") || sWorkspaceId.contains("\\")) {
			Utils.debugLog("ProductResource.uploadfile( InputStream, WS: " + sWorkspaceId + ", Name: " + sName + " ): Injection attempt from users");
			return Response.status(400).build();
		}
		
		// Check the user session
		if (Utils.isNullOrEmpty(sSessionId)) {
			return Response.status(401).build();
		}

		User oUser = Wasdi.getUserFromSession(sSessionId);
		if (oUser == null) {
			Utils.debugLog("ProductResource.uploadfile( InputStream, WS: " + sWorkspaceId + ", Name: " + sName + " ): invalid session");
			return Response.status(401).build();
		}
		if (Utils.isNullOrEmpty(oUser.getUserId())) {
			return Response.status(401).build();
		}
		String sUserId = oUser.getUserId();

		// Check the file name
		if (Utils.isNullOrEmpty(sName) || sName.isEmpty()) {
			//get a random name
			sName = "defaultName-" + Utils.GetRandomName();
		}

		// If workspace is not found in DB returns bad request
		if (!PermissionsUtils.canUserAccessWorkspace(oUser.getUserId(), sWorkspaceId)){
			Utils.debugLog("ProductResource.uploadfile( InputStream, WS: " + sWorkspaceId + ", Name: " + sName + " ): invalid workspace");
			return Response.status(403).build();
		}
		// Take path
		String sWorkspaceOwner = Wasdi.getWorkspaceOwner(sWorkspaceId);
		String sPath = Wasdi.getWorkspacePath(m_oServletConfig, sWorkspaceOwner, sWorkspaceId);

		File oOutputFilePath = new File(sPath + sName);
		
		Utils.debugLog("ProductResource.uploadfile: destination " + oOutputFilePath.getAbsolutePath());

		if (oOutputFilePath.getParentFile().exists() == false) {
			Utils.debugLog("ProductResource.uploadfile: Creating dirs " + oOutputFilePath.getParentFile().getAbsolutePath());
			oOutputFilePath.getParentFile().mkdirs();
		}

		// Copy the stream
		int iRead = 0;
		byte[] ayBytes = new byte[1024];
		
		try (OutputStream oOutStream = new FileOutputStream(oOutputFilePath)) {
			while ((iRead = fileInputStream.read(ayBytes)) != -1) {
				oOutStream.write(ayBytes, 0, iRead);
			}
			oOutStream.flush();
			oOutStream.close();			
		}

		// Start ingestion
		try {
			String sProcessObjId = Utils.GetRandomName();

			IngestFileParameter oParameter = new IngestFileParameter();
			oParameter.setWorkspace(sWorkspaceId);
			oParameter.setUserId(sUserId);
			oParameter.setExchange(sWorkspaceId);
			oParameter.setFilePath(oOutputFilePath.getAbsolutePath());
			oParameter.setProcessObjId(sProcessObjId);
			oParameter.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(sWorkspaceId));

			sPath = m_oServletConfig.getInitParameter("SerializationPath");
			
			PrimitiveResult oRes = Wasdi.runProcess(sUserId, sSessionId, LauncherOperations.INGEST.name(), oOutputFilePath.getName(), sPath, oParameter);
			
			if (oRes.getBoolValue()) {
				return Response.ok().build();
			}
			else {
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
			}
			
		} 
		catch (Exception e) {
			Utils.debugLog("ProductResource.uploadfile: " + e);
			return Response.status(500).build();
		}
	}
	
	@POST
	@Path("/uploadfilebylib")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadFileByLib(@FormDataParam("file") InputStream fileInputStream, @HeaderParam("x-session-token") String sSessionId, @QueryParam("workspace") String sWorkspaceId, @QueryParam("name") String sName) throws Exception {
		Utils.debugLog("ProductResource.uploadfile( InputStream, WS: " + sWorkspaceId + ", Name: " + sName + " )");

		// before any operation check that this is not an injection attempt from the user 
		if (sName.contains("/") || sName.contains("\\") || sWorkspaceId.contains("/") || sWorkspaceId.contains("\\")) { 
			Utils.debugLog("ProductResource.uploadfile: Injection attempt from users");
			return Response.status(400).build();
		}
		
		// Check the user session
		if (Utils.isNullOrEmpty(sSessionId)) {
			Utils.debugLog("ProductResource.uploadfile: invalid session");
			return Response.status(401).build();
		}

		User oUser = Wasdi.getUserFromSession(sSessionId);
		if (oUser == null) {
			return Response.status(401).build();
		}
		if (Utils.isNullOrEmpty(oUser.getUserId())) {
			return Response.status(401).build();
		}
		
		// If workspace is not found in DB returns bad request
		if (!PermissionsUtils.canUserAccessWorkspace(oUser.getUserId(), sWorkspaceId)){
			Utils.debugLog("ProductResource.uploadfile: invalid workspace");
			return Response.status(403).build();
		}

		// Check the file name
		if (Utils.isNullOrEmpty(sName) || sName.isEmpty()) {
			sName = "defaultName";
		}
		
		try {
			// Take path
			String sPath = Wasdi.getWorkspacePath(m_oServletConfig, Wasdi.getWorkspaceOwner(sWorkspaceId), sWorkspaceId);

			File oOutputFilePath = new File(sPath + sName);

			if (oOutputFilePath.getParentFile().exists() == false) {
				oOutputFilePath.getParentFile().mkdirs();
			}

			// Copy the stream
			int iRead = 0;
			byte[] ayBytes = new byte[1024];
			
			try (OutputStream oOutStream = new FileOutputStream(oOutputFilePath)) {
				while ((iRead = fileInputStream.read(ayBytes)) != -1) {
					oOutStream.write(ayBytes, 0, iRead);
				}
				oOutStream.flush();
				oOutStream.close();				
			}
			
			return Response.ok().build();			
		}
		catch (Exception e) {
			Utils.debugLog("ProductResource.uploadfile: " + e);
			return Response.status(500).build();
		}
	}

	@GET
	@Path("delete")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public PrimitiveResult deleteProduct(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("sProductName") String sProductName, @QueryParam("bDeleteFile") Boolean bDeleteFile,
			@QueryParam("sWorkspaceId") String sWorkspaceId, @QueryParam("bDeleteLayer") Boolean bDeleteLayer) {
		
		Utils.debugLog("ProductResource.DeleteProduct( Product: " + sProductName + ", Delete: " + bDeleteFile + ",  WS: " + sWorkspaceId + ", DeleteLayer: " + bDeleteLayer + " )");

		PrimitiveResult oReturn = new PrimitiveResult();
		oReturn.setBoolValue(false);
		
		// before any operation check that this is not an injection attempt from the user 
		if (sProductName.contains("/") || sProductName.contains("\\") || sWorkspaceId.contains("/") || sWorkspaceId.contains("\\")) {
			Utils.debugLog("ProductResource.uploadfile: Injection attempt from users");
			oReturn.setIntValue(400);
			return oReturn;
		}

		User oUser = Wasdi.getUserFromSession(sSessionId);
		try {

			// Domain Check
			if (oUser == null) {
				Utils.debugLog("ProductResource.DeleteProduct: invalid session");
				oReturn.setIntValue(404);
				return oReturn;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				String sMessage = "user not found";
				Utils.debugLog("ProductResource.DeleteProduct: " + sMessage);
				oReturn.setStringValue(sMessage);
				oReturn.setIntValue(404);
				return oReturn;
			}
			if (Utils.isNullOrEmpty(sWorkspaceId)) {
				String sMessage = "workspace null or empty";
				Utils.debugLog("ProductResource.DeleteProduct: " + sMessage);
				oReturn.setStringValue(sMessage);
				oReturn.setIntValue(404);
				return oReturn;
			}
			// If workspace is not found in DB returns bad request
			if (!PermissionsUtils.canUserAccessWorkspace(oUser.getUserId(), sWorkspaceId)){
				String sMessage = "ProductResource.deleteProduct: invalid workspace";
				Utils.debugLog(sMessage);
				oReturn.setStringValue(sMessage);
				oReturn.setIntValue(403);
				return oReturn;
			}
			
			
			
			// Get the file path
			String sDownloadPath = Wasdi.getWorkspacePath(m_oServletConfig, Wasdi.getWorkspaceOwner(sWorkspaceId), sWorkspaceId);
			String sFilePath = sDownloadPath + sProductName;
			
			Utils.debugLog("ProductResource.DeleteProduct: File Path: " + sFilePath);

			// P.Campanella:20190724: try to fix the bug that pub bands are not deleted.
			// Here the name has the extension. In the db the reference to the product is without
			// Try to split the extension
			String sProductNameWithoutExtension = Utils.getFileNameWithoutLastExtension(sProductName);
			
			
			PublishedBandsRepository oPublishedBandsRepository = new PublishedBandsRepository();

			List<PublishedBand> aoPublishedBands = null;
			
			// Get the list of published bands
			if (bDeleteFile || bDeleteLayer) {
				// Get all bands files
				aoPublishedBands = oPublishedBandsRepository.getPublishedBandsByProductName(sProductNameWithoutExtension);
			}

			// get files that begin with the product name
			if (bDeleteFile) {
				
				final List<PublishedBand> aoLocalPublishedBands = aoPublishedBands;
				File oFolder = new File(sDownloadPath);
				
				FilenameFilter oFilter = new FilenameFilter() {

					@Override
					public boolean accept(File dir, String sName) {

						if (sName.equalsIgnoreCase(sProductName)) {
							return true;
						}

						if (sProductName.endsWith(".dim")) {
							String baseName = sProductName.substring(0, sProductName.length() - 4);
							if (sName.equalsIgnoreCase(baseName + ".data")) {
								return true;
							}
						}

						if (aoLocalPublishedBands != null) {
							for (PublishedBand oPublishedBand : aoLocalPublishedBands) {
								if (sName.toLowerCase().contains(oPublishedBand.getLayerId().toLowerCase()))
									return true;
							}
						}

						return false;
					}
				};

				File[] aoFiles = oFolder.listFiles(oFilter);
				
				// If we found the files
				if (aoFiles != null) {
					// Delete all
					Utils.debugLog("ProductResource.DeleteProduct: Number of files to delete " + aoFiles.length);
					for (File oFile : aoFiles) {

						Utils.debugLog("ProductResource.DeleteProduct: deleting file product " + oFile.getAbsolutePath() + "...");
						
						if (!FileUtils.deleteQuietly(oFile)) {
							Utils.debugLog("    ERROR");
						} else {
							Utils.debugLog("    OK");
						}
					}
				} 
				else {
					Utils.debugLog("ProductResource.DeleteProduct: No File to delete ");
				}
			}
			
			DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
			DownloadedFile oDownloadedFile = oDownloadedFilesRepository.getDownloadedFileByPath(sDownloadPath + sProductName);
			
			if (bDeleteLayer) {
				
				// Delete layerId on Geoserver
				GeoServerManager oGeoServerManager = new GeoServerManager(m_oServletConfig.getInitParameter("GS_URL"), m_oServletConfig.getInitParameter("GS_USER"), m_oServletConfig.getInitParameter("GS_PASSWORD"));
				
				// For all the published bands
				for (PublishedBand oPublishedBand : aoPublishedBands) {
					try {
						Utils.debugLog("ProductResource.DeleteProduct: LayerId to delete " + oPublishedBand.getLayerId());

						if (!oGeoServerManager.removeLayer(oPublishedBand.getLayerId())) {
							Utils.debugLog("ProductResource.DeleteProduct: error deleting layer " + oPublishedBand.getLayerId() + " from geoserver");
						}

						try {
							// delete published band on data base
							oPublishedBandsRepository.deleteByProductNameLayerId(oDownloadedFile.getProductViewModel().getName(), oPublishedBand.getLayerId());
						} catch (Exception oEx) {
							Utils.debugLog("ProductResource.DeleteProduct: error deleting published band on data base " + oEx);
						}

					} catch (Exception oEx) {
						Utils.debugLog("ProductResource.DeleteProduct: " + oEx);
					}
				}
			}

			// delete the product-workspace related records on db and the Downloaded File Entry
			try {
				ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();
				oProductWorkspaceRepository.deleteByProductNameWorkspace(sDownloadPath + sProductName, sWorkspaceId);
				oDownloadedFilesRepository.deleteByFilePath(oDownloadedFile.getFilePath());
			} catch (Exception oEx) {
				Utils.debugLog("ProductResource.DeleteProduct: error deleting product " + oEx);
				oReturn.setIntValue(500);
				oReturn.setStringValue(oEx.toString());
				return oReturn;
			}
			
			// Is the product used also in other workspaces?
			List<DownloadedFile> aoDownloadedFileList = oDownloadedFilesRepository.getDownloadedFileListByName(oDownloadedFile.getFileName());

			if (aoDownloadedFileList.size()<=1) {
				// Delete metadata
				try {
					Utils.debugLog("Deleting Metadata file");
					
					if (oDownloadedFile.getProductViewModel()!=null) {
						String sMetadataFilePath = oDownloadedFile.getProductViewModel().getMetadataFileReference();
						if (!Utils.isNullOrEmpty(sMetadataFilePath)) {
							FileUtils.deleteQuietly(new File(sDownloadPath+"/"+sMetadataFilePath));
							Utils.debugLog("Metadata file cleaned");
						}
					}
					
				}
				catch (Exception oEx) {
					Utils.debugLog("ProductResource.DeleteProduct: error deleting product " + oEx);
					oReturn.setIntValue(500);
					oReturn.setStringValue(oEx.toString());
					return oReturn;
				}
			}
			else {
				Utils.debugLog("ProductResource.DeleteProduct: product also in other WS, do not delete metadata");
			}

			try {
				// Search for exchange name
				String sExchange = m_oServletConfig.getInitParameter("RABBIT_EXCHANGE");
				
				// Set default if is empty
				if (Utils.isNullOrEmpty(sExchange)) {
					sExchange = "amq.topic";
				}
				
				// Send the Asynch Message to the clients
				Send oSendToRabbit = new Send(sExchange);
				oSendToRabbit.SendRabbitMessage(true, "DELETE", sWorkspaceId, null, sWorkspaceId);
				oSendToRabbit.Free();				
			}
			catch (Exception oEx) {
				Utils.debugLog("ProductResource.DeleteProduct: exception sending asynch notification");
			}


		} catch (Exception oEx) {
			Utils.debugLog("ProductResource.DeleteProduct: error deleting product " + oEx);
			oReturn.setIntValue(500);
			oReturn.setStringValue(oEx.toString());
			return oReturn;
		}

		oReturn.setBoolValue(true);
		oReturn.setIntValue(200);

		return oReturn;
	}

}
