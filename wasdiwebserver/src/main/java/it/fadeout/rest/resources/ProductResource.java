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
		Utils.debugLog(
				"ProductResource.AddProductToWorkspace:  called WS: " + sWorkspaceId + " Product " + sProductName);

		// Validate Session
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null)
			return null;
		if (Utils.isNullOrEmpty(oUser.getUserId()))
			return null;

		String sPath = Wasdi.getWorkspacePath(m_oServletConfig, Wasdi.getWorkspaceOwner(sWorkspaceId), sWorkspaceId);

		// Create the entity
		ProductWorkspace oProductWorkspace = new ProductWorkspace();
		oProductWorkspace.setProductName(sPath + sProductName);
		oProductWorkspace.setWorkspaceId(sWorkspaceId);

		ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();

		if (oProductWorkspaceRepository.ExistsProductWorkspace(oProductWorkspace.getProductName(),
				oProductWorkspace.getWorkspaceId())) {
			Utils.debugLog("ProductResource.AddProductToWorkspace:  Product already in the workspace");

			// Ok done
			PrimitiveResult oResult = new PrimitiveResult();
			oResult.setBoolValue(true);
			return oResult;
		}

		// Try to insert
		if (oProductWorkspaceRepository.InsertProductWorkspace(oProductWorkspace)) {

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
	}

	@GET
	@Path("byname")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public GeorefProductViewModel getByProductName(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("sProductName") String sProductName, @QueryParam("workspace") String sWorkspace) {

		Utils.debugLog(
				"ProductResource.GetByProductName( " + sSessionId + ", " + sProductName + ", " + sWorkspace + " )");

		// Validate Session
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null)
			return null;
		if (Utils.isNullOrEmpty(oUser.getUserId()))
			return null;

		String sFullPath = Wasdi.getWorkspacePath(m_oServletConfig, Wasdi.getWorkspaceOwner(sWorkspace), sWorkspace);

		// Read the product from db
		DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
		DownloadedFile oDownloadedFile = oDownloadedFilesRepository.GetDownloadedFileByPath(sFullPath + sProductName);

		Utils.debugLog("ProductResource.GetByProductName: search file " + sFullPath + sProductName);

		if (oDownloadedFile != null) {

			Utils.debugLog("ProductResource.GetByProductName: product found");

			GeorefProductViewModel oGeoViewModel = new GeorefProductViewModel(oDownloadedFile.getProductViewModel());
			oGeoViewModel.setBbox(oDownloadedFile.getBoundingBox());

			// Ok read
			return oGeoViewModel;
		} else {
			Utils.debugLog("ProductResource.GetByProductName: product not found");

			// Product not available
			return null;
		}
	}

	@GET
	@Path("metadatabyname")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public MetadataViewModel getMetadataByProductName(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("sProductName") String sProductName, @QueryParam("workspace") String sWorkspaceId) {

		Utils.debugLog("ProductResource.GetMetadataByProductName( " + sSessionId + ", " + sProductName + ", "
				+ sWorkspaceId + " )");

		// Validate Session
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null)
			return null;
		if (Utils.isNullOrEmpty(oUser.getUserId()))
			return null;

		String sProductPath = Wasdi.getWorkspacePath(m_oServletConfig, Wasdi.getWorkspaceOwner(sWorkspaceId),
				sWorkspaceId);

		// Read the product from db
		DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
		DownloadedFile oDownloadedFile = oDownloadedFilesRepository
				.GetDownloadedFileByPath(sProductPath + sProductName);

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
						Utils.debugLog("ProductResource.GetMetadataByProductName: MetadataFile = null for product "
								+ oDownloadedFile.getFilePath());
						return null;
					}

					MetadataViewModel oReloaded = (MetadataViewModel) SerializationUtils
							.deserializeXMLToObject(sMetadataPath + sMetadataFile);
					Utils.debugLog("ProductResource.GetMetadataByProductName: return Metadata for product "
							+ oDownloadedFile.getFilePath());
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
	public ArrayList<GeorefProductViewModel> getListByWorkspace(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("sWorkspaceId") String sWorkspaceId) {
		Utils.debugLog("ProductResource.GetListByWorkspace( " + sSessionId + ", " + sWorkspaceId + " )");
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

			Utils.debugLog("ProductResource.GetListByWorkspace: products for " + sWorkspaceId);

			// Create repo
			ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();
			DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
			PublishedBandsRepository oPublishedBandsRepository = new PublishedBandsRepository();

			// Get Product List
			List<ProductWorkspace> aoProductWorkspace = oProductWorkspaceRepository
					.GetProductsByWorkspace(sWorkspaceId);

			Utils.debugLog("ProductResource.GetListByWorkspace: found " + aoProductWorkspace.size());

			// For each
			for (int iProducts = 0; iProducts < aoProductWorkspace.size(); iProducts++) {

				// Get the downloaded file
				DownloadedFile oDownloaded = oDownloadedFilesRepository
						.GetDownloadedFileByPath(aoProductWorkspace.get(iProducts).getProductName());

				// Add View model to return list
				if (oDownloaded != null) {

					ProductViewModel pVM = oDownloaded.getProductViewModel();

					if (pVM != null) {
						GeorefProductViewModel geoPVM = new GeorefProductViewModel(pVM);
						geoPVM.setBbox(oDownloaded.getBoundingBox());

						if (geoPVM.getBandsGroups() != null) {
							ArrayList<BandViewModel> aoBands = geoPVM.getBandsGroups().getBands();

							if (aoBands != null) {
								for (int iBands = 0; iBands < aoBands.size(); iBands++) {

									BandViewModel oBand = aoBands.get(iBands);

									if (oBand != null) {
										PublishedBand oPublishBand = oPublishedBandsRepository
												.GetPublishedBand(geoPVM.getName(), oBand.getName());

										if (oPublishBand != null) {
											oBand.setPublished(true);
											oBand.setLayerId(oPublishBand.getLayerId());
											oBand.setGeoserverBoundingBox(oPublishBand.getGeoserverBoundingBox());
										} else {
											oBand.setPublished(false);
										}
									}

								}
							}

						}

						geoPVM.setMetadata(null);
						aoProductList.add(geoPVM);

					} else {
						Utils.debugLog("ProductResource.GetListByWorkspace: ProductViewModel is Null: jump product");
					}

				} else {
					Utils.debugLog("ProductResource.GetListByWorkspace: WARNING: the product "
							+ aoProductWorkspace.get(iProducts).getProductName() + " should be in WS " + sWorkspaceId
							+ " but is not a Downloaded File");
				}
			}
		} catch (Exception oEx) {
			Utils.debugLog("ProductResource.GetListByWorkspace: " + oEx);
		}

		return aoProductList;
	}

	@GET
	@Path("/namesbyws")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public ArrayList<String> getNamesByWorkspace(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("sWorkspaceId") String sWorkspaceId) {

		Utils.debugLog("ProductResource.GetListByWorkspace( " + sSessionId + ", " + sWorkspaceId + " )");

		User oUser = Wasdi.GetUserFromSession(sSessionId);

		ArrayList<String> aoProductList = new ArrayList<String>();

		try {

			// Domain Check
			if (oUser == null) {
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
			List<ProductWorkspace> aoProductWorkspace = oProductWorkspaceRepository
					.GetProductsByWorkspace(sWorkspaceId);

			Utils.debugLog("ProductResource.getNamesByWorkspace: found " + aoProductWorkspace.size());

			// For each
			for (int iProducts = 0; iProducts < aoProductWorkspace.size(); iProducts++) {

				// Get the downloaded file
				DownloadedFile oDownloaded = oDownloadedFilesRepository
						.GetDownloadedFileByPath(aoProductWorkspace.get(iProducts).getProductName());

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

		Utils.debugLog("ProductResource.UpdateProductViewModel( " + sSessionId + ", " + sWorkspace + ", ... )");

		User oUser = Wasdi.GetUserFromSession(sSessionId);

		try {

			// Domain Check
			if (oUser == null) {
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
					.GetDownloadedFileByPath(sFullPath + oProductViewModel.getFileName());

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
			if (oDownloadedFilesRepository.UpdateDownloadedFile(oDownlaoded) == false) {
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
	public Response uploadFile(@FormDataParam("file") InputStream fileInputStream,
			@HeaderParam("x-session-token") String sSessionId, @QueryParam("workspace") String sWorkspace,
			@QueryParam("name") String sName) throws Exception {
		Utils.debugLog(
				"ProductResource.uploadfile( InputStream, " + sSessionId + ", " + sWorkspace + ", " + sName + " )");

		// Check the user session
		if (Utils.isNullOrEmpty(sSessionId)) {
			return Response.status(401).build();
		}

		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null) {
			return Response.status(401).build();
		}
		if (Utils.isNullOrEmpty(oUser.getUserId())) {
			return Response.status(401).build();
		}
		String sUserId = oUser.getUserId();

		// Check the file name
		if (Utils.isNullOrEmpty(sName) || sName.isEmpty()) {
			sName = "defaultName";
		}

		// Take path
		String sPath = Wasdi.getWorkspacePath(m_oServletConfig, Wasdi.getWorkspaceOwner(sWorkspace), sWorkspace);

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
			oParameter.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(sWorkspace));

			sPath = m_oServletConfig.getInitParameter("SerializationPath") + sProcessObjId;
			SerializationUtils.serializeObjectToXML(sPath, oParameter);

			try {
				oProcess = new ProcessWorkspace();
				oProcess.setOperationDate(Wasdi.GetFormatDate(new Date()));
				oProcess.setOperationType(LauncherOperations.INGEST.name());
				oProcess.setProductName(oOutputFilePath.getName());
				oProcess.setWorkspaceId(sWorkspace);
				oProcess.setUserId(sUserId);
				oProcess.setProcessObjId(sProcessObjId);
				oProcess.setStatus(ProcessStatus.CREATED.name());
				oRepository.InsertProcessWorkspace(oProcess);
				Utils.debugLog("ProductResource.uploadfile: Process Scheduled for Launcher");
			} catch (Exception oEx) {
				Utils.debugLog("ProductResource.uploadfile: Error updating process list " + oEx);
				return Response.status(500).build();
			}

		} catch (Exception e) {
			Utils.debugLog("ProductResource.uploadfile: " + e);
			return Response.status(500).build();
		}

		return Response.status(200).build();
	}

	@GET
	@Path("delete")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public PrimitiveResult deleteProduct(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("sProductName") String sProductName, @QueryParam("bDeleteFile") Boolean bDeleteFile,
			@QueryParam("sWorkspaceId") String sWorkspace, @QueryParam("bDeleteLayer") Boolean bDeleteLayer) {
		Utils.debugLog("ProductResource.DeleteProduct( " + sSessionId + ", " + sProductName + ", " + bDeleteFile + ", "
				+ sWorkspace + ", " + bDeleteLayer + " )");

		PrimitiveResult oReturn = new PrimitiveResult();
		oReturn.setBoolValue(false);

		User oUser = Wasdi.GetUserFromSession(sSessionId);
		try {

			// Domain Check
			if (oUser == null) {
				String sMessage = "passed a null user";
				Utils.debugLog("ProductResource.DeleteProduct: " + sMessage);
				oReturn.setStringValue(sMessage);
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
			if (Utils.isNullOrEmpty(sWorkspace)) {
				String sMessage = "workspace null or empty";
				Utils.debugLog("ProductResource.DeleteProduct: " + sMessage);
				oReturn.setStringValue(sMessage);
				oReturn.setIntValue(404);
				return oReturn;
			}

			String sDownloadPath = Wasdi.getWorkspacePath(m_oServletConfig, Wasdi.getWorkspaceOwner(sWorkspace),
					sWorkspace);
			Utils.debugLog("ProductResource.DeleteProduct: Download Path: " + sDownloadPath);
			String sFilePath = sDownloadPath + sProductName;
			Utils.debugLog("ProductResource.DeleteProduct: File Path: " + sFilePath);

			// P.Campanella:20190724: try to fix the bug that pub bands are not deleted.
			// Here the name has the extension. In the db the reference to the product is
			// without
			// Try to split the extension
			String sProductNameWithoutExtension = Utils.GetFileNameWithoutExtension(sProductName);

			PublishedBandsRepository oPublishedBandsRepository = new PublishedBandsRepository();

			List<PublishedBand> aoPublishedBands = null;

			if (bDeleteFile || bDeleteLayer) {
				// Get all bands files
				aoPublishedBands = oPublishedBandsRepository
						.GetPublishedBandsByProductName(sProductNameWithoutExtension);
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
				if (aoFiles != null) {
					Utils.debugLog("ProductResource.DeleteProduct: Number of files to delete " + aoFiles.length);
					for (File oFile : aoFiles) {

						Utils.debugLog("ProductResource.DeleteProduct: deleting file product "
								+ oFile.getAbsolutePath() + "...");
						if (!FileUtils.deleteQuietly(oFile)) {
							Utils.debugLog("    ERROR");
						} else {
							Utils.debugLog("    OK");
						}
					}
				} else {
					Utils.debugLog("ProductResource.DeleteProduct: No File to delete ");
				}
			}

			String sFullPath = Wasdi.getWorkspacePath(m_oServletConfig, Wasdi.getWorkspaceOwner(sWorkspace),
					sWorkspace);
			DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
			if (bDeleteLayer) {
				// Delete layerId on Geoserver

				DownloadedFile oDownloadedFile = oDownloadedFilesRepository
						.GetDownloadedFileByPath(sFullPath + sProductName);
				GeoServerManager oGeoServerManager = new GeoServerManager(m_oServletConfig.getInitParameter("GS_URL"),
						m_oServletConfig.getInitParameter("GS_USER"), m_oServletConfig.getInitParameter("GS_PASSWORD"));

				for (PublishedBand oPublishedBand : aoPublishedBands) {
					try {
						Utils.debugLog(
								"ProductResource.DeleteProduct: LayerId to delete " + oPublishedBand.getLayerId());

						if (!oGeoServerManager.removeLayer(oPublishedBand.getLayerId())) {
							Utils.debugLog("ProductResource.DeleteProduct: error deleting layer "
									+ oPublishedBand.getLayerId() + " from geoserver");
						}

						try {
							// delete published band on data base
							oPublishedBandsRepository.DeleteByProductNameLayerId(
									oDownloadedFile.getProductViewModel().getName(), oPublishedBand.getLayerId());
						} catch (Exception oEx) {
							Utils.debugLog(
									"ProductResource.DeleteProduct: error deleting published band on data base " + oEx);
						}

					} catch (Exception oEx) {
						Utils.debugLog("ProductResource.DeleteProduct: " + oEx);
					}
				}
			}

			// delete product record on db
			try {
				ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();
				oProductWorkspaceRepository.DeleteByProductNameWorkspace(sDownloadPath + sProductName, sWorkspace);
				oDownloadedFilesRepository.DeleteByFilePath(sDownloadPath + sProductName);
			} catch (Exception oEx) {
				Utils.debugLog("ProductResource.DeleteProduct: error deleting product " + oEx);
				oReturn.setIntValue(500);
				oReturn.setStringValue(oEx.toString());
				return oReturn;
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
