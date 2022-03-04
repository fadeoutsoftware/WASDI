package it.fadeout.rest.resources;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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
import wasdi.shared.config.WasdiConfig;
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
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.products.GeorefProductViewModel;
import wasdi.shared.viewmodels.products.MetadataViewModel;
import wasdi.shared.viewmodels.products.ProductViewModel;

/**
 * Product Resource.
 * 
 * Hosts API for
 * 	.Add and remove products to workspace 
 * 	.Get list of products in workspace
 * 	.Upload new products
 * 
 * @author p.campanella
 *
 */
@Path("/product")
public class ProductResource {
    
    /**
     * Add a product to a workspace. The file that is going to be added must 
     * be present in the local node workspace folder BUT THIS API DOES NOT CHECK
     * 
     * @param sSessionId User Session Id
     * @param sProductName Name of the product to add: it is the name of the file, included extension.
     * @param sWorkspaceId Workspace Id
     * @return Primitive Result with true of false
     */
    @GET
    @Path("addtows")
    @Produces({"application/xml", "application/json", "text/xml"})
    public PrimitiveResult addProductToWorkspace(@HeaderParam("x-session-token") String sSessionId,
                                                 @QueryParam("name") String sProductName, @QueryParam("workspace") String sWorkspaceId) {
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

            String sPath = Wasdi.getWorkspacePath(Wasdi.getWorkspaceOwner(sWorkspaceId), sWorkspaceId);

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
                if (oGeoRefViewModel != null) oProductWorkspace.setBbox(oGeoRefViewModel.getBbox());
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
    
    /**
     * Get a product by the file name
     * @param sSessionId User Session Id
     * @param sProductName Product Name = file name with extension
     * @param sWorkspaceId Workspace Id
     * @return Georef Product View Model
     */
    @GET
    @Path("byname")
    @Produces({"application/xml", "application/json", "text/xml"})
    public GeorefProductViewModel getByProductName(@HeaderParam("x-session-token") String sSessionId,
                                                   @QueryParam("name") String sProductName, @QueryParam("workspace") String sWorkspaceId) {
        try {
            Utils.debugLog("ProductResource.GetByProductName(Product: " + sProductName + ", WS: " + sWorkspaceId + " )");

            // Validate Session
            User oUser = Wasdi.getUserFromSession(sSessionId);

            if (oUser == null) {
                Utils.debugLog("ProductResource.GetByProductName: invalid session");
                return null;
            }
            if (Utils.isNullOrEmpty(oUser.getUserId())) return null;

            String sFullPath = Wasdi.getWorkspacePath(Wasdi.getWorkspaceOwner(sWorkspaceId), sWorkspaceId);

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
        } catch (Exception oE) {
            Utils.debugLog("ProductResource.GetByProductName( Product: " + sProductName + ", WS: " + sWorkspaceId + " ): " + oE);
        }
        return null;
    }
    
    /**
     * Get Metadata of a product from the name
     * @param sSessionId User Session
     * @param sProductName Product Name = file name with extension
     * @param sWorkspaceId Workspace id
     * @return Metadata View Model
     */
    @GET
    @Path("metadatabyname")
    @Produces({"application/xml", "application/json", "text/xml"})
    public MetadataViewModel getMetadataByProductName(@HeaderParam("x-session-token") String sSessionId,
                                                      @QueryParam("name") String sProductName, @QueryParam("workspace") String sWorkspaceId) {

        Utils.debugLog("ProductResource.GetMetadataByProductName( Product: " + sProductName + ", WS: " + sWorkspaceId + " )");

        // Validate Session
        User oUser = Wasdi.getUserFromSession(sSessionId);
        if (oUser == null)
            return null;
        if (Utils.isNullOrEmpty(oUser.getUserId()))
            return null;

        String sProductPath = Wasdi.getWorkspacePath(Wasdi.getWorkspaceOwner(sWorkspaceId), sWorkspaceId);

        // Read the product from db
        DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
        DownloadedFile oDownloadedFile = oDownloadedFilesRepository.getDownloadedFileByPath(sProductPath + sProductName);


        MetadataViewModel oMetadataViewModel = null;

        if (oDownloadedFile != null) {
            if (oDownloadedFile.getProductViewModel() != null) {

                try {
                    String sMetadataPath = "";

                    if (WasdiConfig.Current.paths.metadataPath != null) {
                        sMetadataPath = WasdiConfig.Current.paths.metadataPath;
                        if (!sMetadataPath.endsWith("/"))
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
                            String sProcessObjId = Utils.getRandomName();

                            // Create the Parameter
                            ReadMetadataParameter oParameter = new ReadMetadataParameter();
                            oParameter.setProcessObjId(sProcessObjId);
                            oParameter.setExchange(sWorkspaceId);
                            oParameter.setWorkspace(sWorkspaceId);
                            oParameter.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(sWorkspaceId));
                            oParameter.setProductName(sProductName);
                            oParameter.setUserId(sUserId);

                            String sPath = WasdiConfig.Current.paths.serializationPath;

                            // Trigger the Launcher Operation
                            Wasdi.runProcess(sUserId, sSessionId, LauncherOperations.READMETADATA.name(), sProductName, sPath, oParameter, null);

                            oMetadataViewModel = new MetadataViewModel();
                            oMetadataViewModel.setName("Generating Metadata, try later");
                        } else {
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
    
    /**
     * Get the list of products in a workspace.
     * 
     * @param sSessionId User Session Id
     * @param sWorkspaceId Workspace Id
     * @return List of Georef Product View Models
     */
    @GET
    @Path("/byws")
    @Produces({"application/xml", "application/json", "text/xml"})
    public List<GeorefProductViewModel> getListByWorkspace(@HeaderParam("x-session-token") String sSessionId,
                                                           @QueryParam("workspace") String sWorkspaceId) {

        Utils.debugLog("ProductResource.GetListByWorkspace( WS: " + sWorkspaceId + " )");
        
        // Prepare return object, empty to sart
        List<GeorefProductViewModel> aoProductList = new ArrayList<GeorefProductViewModel>();
        
        try {
        	
            // Validate input
            if (Utils.isNullOrEmpty(sWorkspaceId)) {
                Utils.debugLog("ProductResource.getListByWorkspace(" + sWorkspaceId + "): workspace is null or empty");
                return aoProductList;
            }        	
        	
            User oUser = Wasdi.getUserFromSession(sSessionId);
            
            if (oUser == null) {
                Utils.debugLog("ProductResource.GetListByWorkspace: invalid session");
                return aoProductList;
            }
            if (Utils.isNullOrEmpty(oUser.getUserId())) {
                return aoProductList;
            }
            
            // Get the list of products for workspace

            Utils.debugLog("ProductResource.GetListByWorkspace: products for " + sWorkspaceId);

            DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
            List<DownloadedFile> aoDownloadedFiles = oDownloadedFilesRepository.getByWorkspace(sWorkspaceId);

            Utils.debugLog("ProductResource.GetListByWorkspace: found " + aoDownloadedFiles.size());

            ArrayList<String> asAddedFiles = new ArrayList<String>();

            // For each found product
            for (int iProducts = 0; iProducts < aoDownloadedFiles.size(); iProducts++) {

                // Get the downloaded file
                DownloadedFile oDownloaded = aoDownloadedFiles.get(iProducts);

                // Add View model to return list
                if (oDownloaded != null) {
                	
                	// Avoid duplication: should not happen, but this is a security code
                    if (asAddedFiles.contains(oDownloaded.getFilePath())) {
                        Utils.debugLog("ProductResource.GetListByWorkspace: " + oDownloaded.getFilePath() + " is a duplicate entry");
                        continue;
                    }
                    
                    // Get the View Model
                    ProductViewModel oProductViewModel = oDownloaded.getProductViewModel();

                    if (oProductViewModel != null) {
                    	
                    	// Convert the view model in Georef version
                        GeorefProductViewModel oGeoRefProductViewModel = new GeorefProductViewModel(oProductViewModel);
                        oGeoRefProductViewModel.setBbox(oDownloaded.getBoundingBox());
                        
                        oGeoRefProductViewModel.setStyle(oDownloaded.getDefaultStyle());

                        oGeoRefProductViewModel.setMetadata(null);
                        aoProductList.add(oGeoRefProductViewModel);
                        asAddedFiles.add(oDownloaded.getFilePath());

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
        
        // Return the list
        return aoProductList;
    }
    
    /**
     * Get a light list of the products in a workspace. The light list 
     * does not load all the Product View Model, but only the name and the bbox.
     * It is used by the client to populate the first list of products in the editor
     * 
     * @param sSessionId User Session Id
     * @param sWorkspaceId Workspace Id 
     * @return List of Georef Product View Models
     */
    @GET
    @Path("/bywslight")
    @Produces({"application/xml", "application/json", "text/xml"})
    public List<GeorefProductViewModel> getLightListByWorkspace(@HeaderParam("x-session-token") String sSessionId, @QueryParam("workspace") String sWorkspaceId) {

        Utils.debugLog("ProductResource.getLightListByWorkspace( WS: " + sWorkspaceId + " )");
        
        List<GeorefProductViewModel> aoProductList = new ArrayList<GeorefProductViewModel>();

        try {
        	
        	// Validate inputs
        	
            if (Utils.isNullOrEmpty(sWorkspaceId)) {
                Utils.debugLog("ProductResource.getLightListByWorkspace(" + sWorkspaceId + "): workspace is null or empty");
                return aoProductList;
            }
        	
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
    
    /**
     * Get a list of strings each representing the name of a product (file name with extension) in a workspace.
     * 
     * @param sSessionId User Session Id
     * @param sWorkspaceId Workspace Id
     * @return List of strings each representing the name of a product (file name with extension) in a workspace.
     */
    @GET
    @Path("/namesbyws")
    @Produces({"application/xml", "application/json", "text/xml"})
    public ArrayList<String> getNamesByWorkspace(@HeaderParam("x-session-token") String sSessionId,
                                                 @QueryParam("workspace") String sWorkspaceId) {

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
    
    /**
     * Update a Product from the correspondig View Model
     * @param sSessionId User Session
     * @param sWorkspaceId Workspace Id
     * @param oProductViewModel Product View Model: NOTE IT UPDATES ONLY productFriendlyName and Style
     * @return std http response
     */
    @POST
    @Path("/update")
    @Produces({"application/xml", "application/json", "text/xml"})
    public Response updateProductViewModel(@HeaderParam("x-session-token") String sSessionId,
                                           @QueryParam("workspace") String sWorkspaceId, ProductViewModel oProductViewModel) {

        Utils.debugLog("ProductResource.UpdateProductViewModel( WS: " + sWorkspaceId + ", ... )");

        try {

            // Domain Check
            User oUser = Wasdi.getUserFromSession(sSessionId);
            if (oUser == null) {
                Utils.debugLog("ProductResource.UpdateProductViewModel( WS: " + sWorkspaceId + ", ... ): invalid session");
                return Response.status(401).build();
            }
            if (Utils.isNullOrEmpty(oUser.getUserId())) {
                return Response.status(401).build();
            }

            if (oProductViewModel == null) {
                return Response.status(500).build();
            }

            Utils.debugLog("ProductResource.UpdateProductViewModel: product " + oProductViewModel.getFileName());

            String sFullPath = Wasdi.getWorkspacePath(Wasdi.getWorkspaceOwner(sWorkspaceId), sWorkspaceId);

            // Create repo
            DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();

            // Get the Entity
            DownloadedFile oDownloaded = oDownloadedFilesRepository.getDownloadedFileByPath(sFullPath + oProductViewModel.getFileName());

            if (oDownloaded == null) {
                Utils.debugLog("ProductResource.UpdateProductViewModel: Associated downloaded file not found.");
                return Response.status(500).build();
            }
            
            
            String sOriginalName = oDownloaded.getProductViewModel().getProductFriendlyName();
            if (sOriginalName == null) sOriginalName = "";
            
            String sOriginalStyle = oDownloaded.getDefaultStyle();
            if (sOriginalStyle == null) sOriginalStyle = "";
            
            String sNewStyle = oProductViewModel.getStyle();
            if (sNewStyle == null) sNewStyle = "";
            
            String sNewName = oProductViewModel.getProductFriendlyName();
            if (sNewName == null) sNewName = "";
            
            
            if ((!sOriginalName.equals(sNewName)) || (!sOriginalStyle.equals(sNewStyle))) {
                // Update the 2 fields that can be updated
                oDownloaded.getProductViewModel().setProductFriendlyName(oProductViewModel.getProductFriendlyName());
                oDownloaded.setDefaultStyle(oProductViewModel.getStyle());
                
                try {
                    if (!sOriginalStyle.equals(sNewStyle)) {
                    	PublishedBandsRepository oPublishedBandsRepository = new PublishedBandsRepository();
                    	List<PublishedBand> aoPublishedBands = oPublishedBandsRepository.getPublishedBandsByProductName(oProductViewModel.getName());
                    	
                    	for (PublishedBand oPublishedBand : aoPublishedBands) {
                    		String sGeoServerUrl = oPublishedBand.getGeoserverUrl();
                    		
                    		if (Utils.isNullOrEmpty(sGeoServerUrl)) {
                    			sGeoServerUrl = WasdiConfig.Current.geoserver.address;
                    		}
                    		else {
                    			if (sGeoServerUrl.endsWith("/ows")) {
                    				sGeoServerUrl = sGeoServerUrl.substring(0, sGeoServerUrl.length()-4);
                    			}
                    		}
                    		
                    		GeoServerManager oGeoServerManager = new GeoServerManager(sGeoServerUrl, WasdiConfig.Current.geoserver.user, WasdiConfig.Current.geoserver.password);
                    		oGeoServerManager.configureLayerStyle(oPublishedBand.getLayerId(), sNewStyle);
                    	}
                    	
                    	
                    }                	
                }
                catch (Exception oStyleEx) {
                	Utils.debugLog("ProductResource.UpdateProductViewModel: Exception changing geoserver style " + oStyleEx.toString());
				}

                // Save
                if (oDownloadedFilesRepository.updateDownloadedFile(oDownloaded) == false) {
                    Utils.debugLog("ProductResource.UpdateProductViewModel: There was an error updating Downloaded File.");
                    return Response.status(500).build();
                }
                
                Utils.debugLog("ProductResource.UpdateProductViewModel: Updated ");
            }
            else {
            	Utils.debugLog("ProductResource.UpdateProductViewModel: Nothing Changed");
            }

        } catch (Exception oEx) {
            Utils.debugLog("ProductResource.UpdateProductViewModel: " + oEx);
            return Response.status(500).build();
        }

        return Response.status(200).build();
    }
    
    /**
     * Uplads a new file as a Product in a workspace.
     * This API saves the files and trigger an ingest operation 
     * in the launcher
     * 
     * @param fileInputStream File stream
     * @param sSessionId User Session
     * @param sWorkspaceId Workspace id
     * @param sName File Name
     * @return std http response
     * @throws Exception
     */
    @POST
    @Path("/uploadfile")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadFile(@FormDataParam("file") InputStream fileInputStream, @HeaderParam("x-session-token") String sSessionId, @QueryParam("workspace") String sWorkspaceId, @QueryParam("name") String sName, @QueryParam("style") String sStyle) throws Exception {
        Utils.debugLog("ProductResource.uploadfile( InputStream, WS: " + sWorkspaceId + ", Name: " + sName + " )");

        // before any operation check that this is not an injection attempt from the user
        if (sName.contains("/") || sName.contains("\\") || sWorkspaceId.contains("/") || sWorkspaceId.contains("\\")) {
            Utils.debugLog("ProductResource.uploadfile( InputStream, WS: " + sWorkspaceId + ", Name: " + sName + " ): Injection attempt from users");
            return Response.status(400).build();
        }

        User oUser = Wasdi.getUserFromSession(sSessionId);
        if (oUser == null) {
            Utils.debugLog("ProductResource.uploadfile( InputStream, WS: " + sWorkspaceId + ", Name: " + sName + " ): invalid session");
            return Response.status(401).build();
        }
        
        String sUserId = oUser.getUserId();

        // Check the file name
        if (Utils.isNullOrEmpty(sName) || sName.isEmpty()) {
            //get a random name
            sName = "defaultName-" + Utils.getRandomName();
        }

        // If workspace is not found in DB returns bad request
        if (!PermissionsUtils.canUserAccessWorkspace(oUser.getUserId(), sWorkspaceId)) {
            Utils.debugLog("ProductResource.uploadfile( InputStream, WS: " + sWorkspaceId + ", Name: " + sName + " ): invalid workspace");
            return Response.status(403).build();
        }
        
        // Take path
        String sWorkspaceOwner = Wasdi.getWorkspaceOwner(sWorkspaceId);
        String sPath = Wasdi.getWorkspacePath(sWorkspaceOwner, sWorkspaceId);

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
            String sProcessObjId = Utils.getRandomName();

            IngestFileParameter oParameter = new IngestFileParameter();
            oParameter.setWorkspace(sWorkspaceId);
            oParameter.setUserId(sUserId);
            oParameter.setExchange(sWorkspaceId);
            oParameter.setFilePath(oOutputFilePath.getAbsolutePath());
            oParameter.setStyle(sStyle);
            oParameter.setProcessObjId(sProcessObjId);
            oParameter.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(sWorkspaceId));

            sPath = WasdiConfig.Current.paths.serializationPath;

            PrimitiveResult oRes = Wasdi.runProcess(sUserId, sSessionId, LauncherOperations.INGEST.name(), oOutputFilePath.getName(), sPath, oParameter);

            if (oRes.getBoolValue()) {
                return Response.ok().build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }

        } catch (Exception e) {
            Utils.debugLog("ProductResource.uploadfile: " + e);
            return Response.status(500).build();
        }
    }
    
    /**
     * Upload file to a workspace. This version is used from the libs:
     * the difference is that does not trigger the ingestion operation
     * of the launcher.
     * 
     * @param fileInputStream File Stream
     * @param sSessionId User Session
     * @param sWorkspaceId Workspace Id
     * @param sName File Name
     * @return std http response
     * @throws Exception
     */
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
        if (!PermissionsUtils.canUserAccessWorkspace(oUser.getUserId(), sWorkspaceId)) {
            Utils.debugLog("ProductResource.uploadfile: invalid workspace");
            return Response.status(403).build();
        }

        // Check the file name
        if (Utils.isNullOrEmpty(sName) || sName.isEmpty()) {
            sName = "defaultName";
        }

        try {
            // Take path
            String sPath = Wasdi.getWorkspacePath(Wasdi.getWorkspaceOwner(sWorkspaceId), sWorkspaceId);

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
        } catch (Exception e) {
            Utils.debugLog("ProductResource.uploadfile: " + e);
            return Response.status(500).build();
        }
    }
    
    /**
     * Deletes a product from a workspace.
     * This deletes also the file on the disk and all the published bands
     * 
     * @param sSessionId User Session Id
     * @param sProductName Product Name (file name withe extension)
     * @param bDeleteFile flag to confirm to delete also the file
     * @param sWorkspaceId Workspace id
     * @param bDeleteLayer flag to confirm to delete also the WxS layers
     * @return Primitive Result with boolValue = true in case of success.
     */
    @GET
    @Path("delete")
    @Produces({"application/xml", "application/json", "text/xml"})
    public PrimitiveResult deleteProduct(@HeaderParam("x-session-token") String sSessionId,
                                         @QueryParam("name") String sProductName, @QueryParam("deletefile") Boolean bDeleteFile,
                                         @QueryParam("workspace") String sWorkspaceId, @QueryParam("deletelayer") Boolean bDeleteLayer) {

        Utils.debugLog("ProductResource.DeleteProduct( Product: " + sProductName + ", Delete: " + bDeleteFile + ",  WS: " + sWorkspaceId + ", DeleteLayer: " + bDeleteLayer + " )");

        PrimitiveResult oReturn = new PrimitiveResult();
        oReturn.setBoolValue(false);
        
        if (bDeleteFile == null) bDeleteFile = true;
        if (bDeleteLayer == null) bDeleteLayer = true;

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
            if (!PermissionsUtils.canUserAccessWorkspace(oUser.getUserId(), sWorkspaceId)) {
                String sMessage = "ProductResource.deleteProduct: invalid workspace";
                Utils.debugLog(sMessage);
                oReturn.setStringValue(sMessage);
                oReturn.setIntValue(403);
                return oReturn;
            }


            // Get the file path
            String sDownloadPath = Wasdi.getWorkspacePath(Wasdi.getWorkspaceOwner(sWorkspaceId), sWorkspaceId);
            String sFilePath = sDownloadPath + sProductName;

            Utils.debugLog("ProductResource.DeleteProduct: File Path: " + sFilePath);

            PublishedBandsRepository oPublishedBandsRepository = new PublishedBandsRepository();

            List<PublishedBand> aoPublishedBands = null;

            DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
            DownloadedFile oDownloadedFile = oDownloadedFilesRepository.getDownloadedFileByPath(sDownloadPath + sProductName);


            // Get the list of published bands
            if (bDeleteFile || bDeleteLayer) {
                // Get all bands files
                aoPublishedBands = oPublishedBandsRepository.getPublishedBandsByProductName(oDownloadedFile.getProductViewModel().getName());
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
                } else {
                    Utils.debugLog("ProductResource.DeleteProduct: No File to delete ");
                }
            }

            if (bDeleteLayer) {
            	
            	try {
                    // Delete layerId on Geoserver
                    GeoServerManager oGeoServerManager = new GeoServerManager();

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
            	catch (Exception oEx) {
                    Utils.debugLog("ProductResource.DeleteProduct: Exception deleting layers: " + oEx);
                }            	
            }

            // delete the product-workspace related records on db and the Downloaded File Entry
            try {
                ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();
                oProductWorkspaceRepository.deleteByProductNameWorkspace(sDownloadPath + sProductName, sWorkspaceId);
                oDownloadedFilesRepository.deleteByFilePath(oDownloadedFile.getFilePath());
            } catch (Exception oEx) {
                Utils.debugLog("ProductResource.DeleteProduct: error deleting product-workspace related records on db and the Downloaded File Entry " + oEx);
                oReturn.setIntValue(500);
                oReturn.setStringValue(oEx.toString());
                return oReturn;
            }

            // Is the product used also in other workspaces?
            List<DownloadedFile> aoDownloadedFileList = oDownloadedFilesRepository.getDownloadedFileListByName(oDownloadedFile.getFileName());

            if (aoDownloadedFileList.size() <= 1) {
                // Delete metadata
                try {
                    Utils.debugLog("Deleting Metadata file");

                    if (oDownloadedFile.getProductViewModel() != null) {
                        String sMetadataFilePath = oDownloadedFile.getProductViewModel().getMetadataFileReference();
                        if (!Utils.isNullOrEmpty(sMetadataFilePath)) {
                            FileUtils.deleteQuietly(new File(sDownloadPath + "/" + sMetadataFilePath));
                            Utils.debugLog("Metadata file cleaned");
                        }
                    }

                } catch (Exception oEx) {
                    Utils.debugLog("ProductResource.DeleteProduct: error deleting Metadata " + oEx);
                    oReturn.setIntValue(500);
                    oReturn.setStringValue(oEx.toString());
                    return oReturn;
                }
            } else {
                Utils.debugLog("ProductResource.DeleteProduct: product also in other WS, do not delete metadata");
            }

            try {
                // Search for exchange name
                String sExchange = WasdiConfig.Current.rabbit.exchange;

                // Set default if is empty
                if (Utils.isNullOrEmpty(sExchange)) {
                    sExchange = "amq.topic";
                }

                // Send the Asynch Message to the clients
                Send oSendToRabbit = new Send(sExchange);
                oSendToRabbit.SendRabbitMessage(true, "DELETE", sWorkspaceId, null, sWorkspaceId);
                oSendToRabbit.Free();
            } catch (Exception oEx) {
                Utils.debugLog("ProductResource.DeleteProduct: exception sending asynch notification");
            }


        } catch (Exception oEx) {
            Utils.debugLog("ProductResource.DeleteProduct: error deleting product " + oEx);
            oEx.printStackTrace();
            oReturn.setIntValue(500);
            oReturn.setStringValue(oEx.toString());
            return oReturn;
        }

        oReturn.setBoolValue(true);
        oReturn.setIntValue(200);

        return oReturn;
    }

    /**
     * Deletes a list of product invoking the delete method,
     * Store the response and returns primitive Results of the with the following schema:
     * 200 - All products deleted
     * 207 - (multi status) when not every single product from the list can be deleted
     *
     * @param sSessionId     Session id of the current logged in user
     * @param bDeleteFile    Flag to control the behaviour of deletion
     * @param sWorkspaceId   Id of the workspace in which the product is stored
     * @param bDeleteLayer   Flag to control the behaviour of deletion
     * @param asProductList An array containing the list of product names to be deleted
     * @return Primitive result with 200 & TRUE in case all files was deleted. 207 & FALSE in case not all file was deleted
     */
    @POST
    @Path("deletelist")
    @Consumes(MediaType.APPLICATION_JSON)
    public PrimitiveResult deleteMultipleProduct(@HeaderParam("x-session-token") String sSessionId, @QueryParam("deletefile") Boolean bDeleteFile,
                                         @QueryParam("workspace") String sWorkspaceId, @QueryParam("deletelayer") Boolean bDeleteLayer,
                                         List<String> asProductList) {
        // Support variable used to identify if deletions of one or more products failed
        AtomicBoolean bDirty = new AtomicBoolean(false);
        PrimitiveResult oPrimitiveResult = new PrimitiveResult();
        
        if (asProductList != null) {
            asProductList.stream().forEach(sFile -> {
                // if one deletion fail is detected the bDirty boolean becames true
                bDirty.set(bDirty.get() || ! deleteProduct(sSessionId,sFile,bDeleteFile,sWorkspaceId,bDeleteLayer).getBoolValue());
            });

            

            if (bDirty.get()) oPrimitiveResult.setIntValue(207);
            else oPrimitiveResult.setIntValue(200);
            // returns the opposite value of Dirty
            oPrimitiveResult.setBoolValue(!bDirty.get());

            return oPrimitiveResult;        	
        }
        else {
        	oPrimitiveResult.setIntValue(500);
        	oPrimitiveResult.setBoolValue(false);
        	return oPrimitiveResult;
        }
    }

}
