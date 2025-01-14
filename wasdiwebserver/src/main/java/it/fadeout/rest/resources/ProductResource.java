package it.fadeout.rest.resources;

import java.awt.image.AbstractMultiResolutionImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FileUtils;
import org.glassfish.jersey.media.multipart.FormDataParam;

import it.fadeout.Wasdi;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.ProductWorkspace;
import wasdi.shared.business.PublishedBand;
import wasdi.shared.business.Workspace;
import wasdi.shared.business.users.User;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.ProductWorkspaceRepository;
import wasdi.shared.data.PublishedBandsRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.geoserver.GeoServerManager;
import wasdi.shared.parameters.IngestFileParameter;
import wasdi.shared.parameters.ReadMetadataParameter;
import wasdi.shared.rabbit.Send;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.log.WasdiLog;
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
            WasdiLog.debugLog("ProductResource.addProductToWorkspace:  WS: " + sWorkspaceId + " Product " + sProductName);

            // Validate Session
            User oUser = Wasdi.getUserFromSession(sSessionId);
            if (oUser == null) {
                WasdiLog.warnLog("ProductResource.addProductToWorkspace: invalid session");
                return null;
            }
            
            if (!PermissionsUtils.canUserWriteWorkspace(oUser.getUserId(), sWorkspaceId)) {
                WasdiLog.warnLog("ProductResource.addProductToWorkspace: user cannot write in the workspace");
                return null;            	
            }

            String sPath = PathsConfig.getWorkspacePath(Wasdi.getWorkspaceOwner(sWorkspaceId), sWorkspaceId);

            // Create the entity
            ProductWorkspace oProductWorkspace = new ProductWorkspace();
            oProductWorkspace.setProductName(sPath + sProductName);
            oProductWorkspace.setWorkspaceId(sWorkspaceId);

            ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();

            if (oProductWorkspaceRepository.existsProductWorkspace(oProductWorkspace.getProductName(), oProductWorkspace.getWorkspaceId())) {
                WasdiLog.warnLog("ProductResource.addProductToWorkspace:  Product already in the workspace");

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

                WasdiLog.debugLog("ProductResource.addProductToWorkspace:  Inserted");

                // Ok done
                PrimitiveResult oResult = new PrimitiveResult();
                oResult.setBoolValue(true);
                return oResult;
            } else {
                WasdiLog.debugLog("ProductResource.addProductToWorkspace: Insert Error");

                // There was a problem
                PrimitiveResult oResult = new PrimitiveResult();
                oResult.setBoolValue(false);

                return oResult;
            }
        } catch (Exception oE) {
            WasdiLog.errorLog("ProductResource.addProductToWorkspace: error " + oE);
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
            WasdiLog.debugLog("ProductResource.getByProductName(Product: " + sProductName + ", WS: " + sWorkspaceId + " )");

            // Validate Session
            User oUser = Wasdi.getUserFromSession(sSessionId);

            if (oUser == null) {
                WasdiLog.warnLog("ProductResource.getByProductName: invalid session");
                return null;
            }
            
            if (!PermissionsUtils.canUserAccessWorkspace(oUser.getUserId(), sWorkspaceId)) {
                WasdiLog.warnLog("ProductResource.getByProductName: user cannot access workspace");
                return null;            	
            }

            String sFullPath = PathsConfig.getWorkspacePath(Wasdi.getWorkspaceOwner(sWorkspaceId), sWorkspaceId);

            // Read the product from db
            DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
            DownloadedFile oDownloadedFile = oDownloadedFilesRepository.getDownloadedFileByPath(sFullPath + sProductName);

            if (oDownloadedFile != null) {

                WasdiLog.debugLog("ProductResource.getByProductName: product found");

                GeorefProductViewModel oGeoViewModel = new GeorefProductViewModel(oDownloadedFile.getProductViewModel());
                oGeoViewModel.setBbox(oDownloadedFile.getBoundingBox());

                // Ok read
                return oGeoViewModel;
            } else {
                WasdiLog.debugLog("ProductResource.getByProductName: product not found");
            }
        } 
        catch (Exception oE) {
            WasdiLog.errorLog("ProductResource.getByProductName error: " + oE);
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

        WasdiLog.debugLog("ProductResource.getMetadataByProductName( Product: " + sProductName + ", WS: " + sWorkspaceId + " )");

        // Validate Session
        User oUser = Wasdi.getUserFromSession(sSessionId);
        
        if (oUser == null) {
        	WasdiLog.warnLog("ProductResource.getMetadataByProductName: invalid session");
        	return null;
        }
        
        if (!PermissionsUtils.canUserAccessWorkspace(oUser.getUserId(), sWorkspaceId)) {
            WasdiLog.warnLog("ProductResource.getMetadataByProductName: user cannot access workspace");
            return null;            	
        }        

        String sProductPath = PathsConfig.getWorkspacePath(Wasdi.getWorkspaceOwner(sWorkspaceId), sWorkspaceId);

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

                        WasdiLog.debugLog("ProductResource.getMetadataByProductName: MetadataFile = null for product " + oDownloadedFile.getFilePath());

                        // Was it created before or not?
                        if (oDownloadedFile.getProductViewModel().getMetadataFileCreated() == false) {

                            WasdiLog.debugLog("ProductResource.getMetadataByProductName: first metadata request, create operation to read it");

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

                            // Trigger the Launcher Operation
                            Wasdi.runProcess(sUserId, sSessionId, LauncherOperations.READMETADATA.name(), sProductName, oParameter, null);

                            oMetadataViewModel = new MetadataViewModel();
                            oMetadataViewModel.setName("Generating Metadata, try later");
                        } else {
                            WasdiLog.debugLog("ProductResource.getMetadataByProductName: attemp to read metadata already done, just return");
                        }

                        return oMetadataViewModel;
                    }

                    MetadataViewModel oReloaded = (MetadataViewModel) SerializationUtils.deserializeXMLToObject(sMetadataPath + sMetadataFile);

                    WasdiLog.debugLog("ProductResource.getMetadataByProductName: return Metadata for product " + oDownloadedFile.getFilePath());

                    // Ok return Metadata
                    return oReloaded;
                } catch (Exception oEx) {
                    WasdiLog.errorLog("ProductResource.getMetadataByProductName: " + oEx);
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

        WasdiLog.debugLog("ProductResource.getListByWorkspace( WS: " + sWorkspaceId + " )");
        
        // Prepare return object, empty to sart
        List<GeorefProductViewModel> aoProductList = new ArrayList<GeorefProductViewModel>();
        
        try {
        	
            // Validate input
            if (Utils.isNullOrEmpty(sWorkspaceId)) {
                WasdiLog.warnLog("ProductResource.getListByWorkspace: workspace is null or empty");
                return aoProductList;
            }        	
        	
            User oUser = Wasdi.getUserFromSession(sSessionId);
            
            if (oUser == null) {
                WasdiLog.warnLog("ProductResource.getListByWorkspace: invalid session");
                return aoProductList;
            }
            
            if (!PermissionsUtils.canUserAccessWorkspace(oUser.getUserId(), sWorkspaceId)) {
                WasdiLog.warnLog("ProductResource.getListByWorkspace: user cannot access workspace");
                return aoProductList;            	
            }
            
            DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
            List<DownloadedFile> aoDownloadedFiles = oDownloadedFilesRepository.getByWorkspace(sWorkspaceId);

            WasdiLog.debugLog("ProductResource.getListByWorkspace: found " + aoDownloadedFiles.size());

            ArrayList<String> asAddedFiles = new ArrayList<String>();

            // For each found product
            for (int iProducts = 0; iProducts < aoDownloadedFiles.size(); iProducts++) {

                // Get the downloaded file
                DownloadedFile oDownloaded = aoDownloadedFiles.get(iProducts);

                // Add View model to return list
                if (oDownloaded != null) {
                	
                	// Avoid duplication: should not happen, but this is a security code
                    if (asAddedFiles.contains(oDownloaded.getFilePath())) {
                        WasdiLog.debugLog("ProductResource.getListByWorkspace: " + oDownloaded.getFilePath() + " is a duplicate entry");
                        continue;
                    }
                    
                    // Get the View Model
                    ProductViewModel oProductViewModel = oDownloaded.getProductViewModel();

                    if (oProductViewModel != null) {
                    	
                    	// Convert the view model in Georef version
                        GeorefProductViewModel oGeoRefProductViewModel = new GeorefProductViewModel(oProductViewModel);
                        oGeoRefProductViewModel.setBbox(oDownloaded.getBoundingBox());
                        
                        oGeoRefProductViewModel.setStyle(oDownloaded.getDefaultStyle());
                        oGeoRefProductViewModel.setDescription(oDownloaded.getDescription());

                        oGeoRefProductViewModel.setMetadata(null);
                        aoProductList.add(oGeoRefProductViewModel);
                        asAddedFiles.add(oDownloaded.getFilePath());

                    } else {
                        WasdiLog.debugLog("ProductResource.getListByWorkspace: ProductViewModel is Null: jump product");
                    }

                } else {
                    //WasdiLog.debugLog("ProductResource.GetListByWorkspace: WARNING: the product " + aoProductWorkspace.get(iProducts).getProductName() + " should be in WS " + sWorkspaceId + " but is not a Downloaded File");
                }
            }
        } catch (Exception oEx) {
            WasdiLog.errorLog("ProductResource.getListByWorkspace: " + oEx);
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

        WasdiLog.debugLog("ProductResource.getLightListByWorkspace( WS: " + sWorkspaceId + " )");
        
        List<GeorefProductViewModel> aoProductList = new ArrayList<GeorefProductViewModel>();

        try {
        	// Validate inputs
            if (Utils.isNullOrEmpty(sWorkspaceId)) {
                WasdiLog.warnLog("ProductResource.getLightListByWorkspace: workspace is null or empty");
                return aoProductList;
            }
        	
            User oUser = Wasdi.getUserFromSession(sSessionId);
            if (oUser == null) {
                WasdiLog.warnLog("ProductResource.getLightListByWorkspace: invalid session");
                return aoProductList;
            }
            
            if (!PermissionsUtils.canUserAccessWorkspace(oUser.getUserId(), sWorkspaceId)) {
                WasdiLog.warnLog("ProductResource.getLightListByWorkspace: user cannot access workspace");
                return aoProductList;            	
            }

            // Create repo
            ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();

            // Get Product List
            List<ProductWorkspace> aoProductWorkspace = oProductWorkspaceRepository.getProductsByWorkspace(sWorkspaceId);

            WasdiLog.debugLog("ProductResource.getLightListByWorkspace: found " + aoProductWorkspace.size());

            // For each
            for (int iProducts = 0; iProducts < aoProductWorkspace.size(); iProducts++) {
                GeorefProductViewModel oGeoPVM = new GeorefProductViewModel();
                oGeoPVM.setBbox(aoProductWorkspace.get(iProducts).getBbox());

                File oFile = new File(aoProductWorkspace.get(iProducts).getProductName());
                String sName = WasdiFileUtils.getFileNameWithoutLastExtension(oFile.getName());
                oGeoPVM.setProductFriendlyName(sName);
                oGeoPVM.setName(sName);

                aoProductList.add(oGeoPVM);
            }
        } catch (Exception oEx) {
            WasdiLog.errorLog("ProductResource.getLightListByWorkspace: " + oEx);
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

        WasdiLog.debugLog("ProductResource.getNamesByWorkspace( WS: " + sWorkspaceId + " )");

        User oUser = Wasdi.getUserFromSession(sSessionId);

        ArrayList<String> aoProductList = new ArrayList<String>();

        try {

            // Domain Check
            if (oUser == null) {
                WasdiLog.warnLog("ProductResource.getNamesByWorkspace: invalid session");
                return aoProductList;
            }
            
            if (!PermissionsUtils.canUserAccessWorkspace(oUser.getUserId(), sWorkspaceId)) {
                WasdiLog.warnLog("ProductResource.getNamesByWorkspace: user cannot access the workspace");
                return aoProductList;            	
            }

            // Create repo
            ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();

            // Get Product List
            List<ProductWorkspace> aoProductWorkspace = oProductWorkspaceRepository.getProductsByWorkspace(sWorkspaceId);

            WasdiLog.debugLog("ProductResource.getNamesByWorkspace: found " + aoProductWorkspace.size());

            // For each
            for (int iProducts = 0; iProducts < aoProductWorkspace.size(); iProducts++) {

                // Get the downloaded file
                //DownloadedFile oDownloaded = oDownloadedFilesRepository.getDownloadedFileByPath(aoProductWorkspace.get(iProducts).getProductName());
            	
            	String sFullPath = aoProductWorkspace.get(iProducts).getProductName();
            	
            	Boolean bInserted = false; 

                // Add View model to return list
                if (!Utils.isNullOrEmpty(sFullPath)) {
                	
                	sFullPath = sFullPath.replace("\\", "/");
                	String [] asParts = sFullPath.split("/");
                	
                	if (asParts != null) {
                		if (asParts.length>0) {
                    		aoProductList.add(asParts[asParts.length-1]);
                    		bInserted = true;
                		}
                	}
                } 
                if (!bInserted) {
                    WasdiLog.debugLog("ProductResource.getNamesByWorkspace: WARNING: the product "
                            + aoProductWorkspace.get(iProducts).getProductName() + " should be in WS " + sWorkspaceId
                            + " but is not a Downloaded File");
                }
            }
        } catch (Exception oEx) {
            WasdiLog.errorLog("ProductResource.getNamesByWorkspace error: " + oEx);
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

        WasdiLog.debugLog("ProductResource.updateProductViewModel( WS: " + sWorkspaceId + ", ... )");

        try {

            // Domain Check
            User oUser = Wasdi.getUserFromSession(sSessionId);
            if (oUser == null) {
                WasdiLog.warnLog("ProductResource.updateProductViewModel: invalid session");
                return Response.status(Status.UNAUTHORIZED).build();
            }

            if (!PermissionsUtils.canUserWriteWorkspace(oUser.getUserId(), sWorkspaceId)) {
                WasdiLog.warnLog("ProductResource.updateProductViewModel: user cannot write in the workspace");
                return Response.status(Status.FORBIDDEN).build();
            }            

            if (oProductViewModel == null) {
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }

            String sFullPath = PathsConfig.getWorkspacePath(Wasdi.getWorkspaceOwner(sWorkspaceId), sWorkspaceId);

            // Create repo
            DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();

            // Get the Entity
            DownloadedFile oDownloaded = oDownloadedFilesRepository.getDownloadedFileByPath(sFullPath + oProductViewModel.getFileName());

            if (oDownloaded == null) {
            	oDownloaded = oDownloadedFilesRepository.getDownloadedFileByPath(WasdiFileUtils.fixPathSeparator(sFullPath + oProductViewModel.getFileName()));
            }

            if (oDownloaded == null) {
                WasdiLog.warnLog("ProductResource.updateProductViewModel: Associated downloaded file not found.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            
            
            String sOriginalName = oDownloaded.getProductViewModel().getProductFriendlyName();
            if (sOriginalName == null) sOriginalName = "";
            
            String sOriginalStyle = oDownloaded.getDefaultStyle();
            if (sOriginalStyle == null) sOriginalStyle = "";
            
            String sNewStyle = oProductViewModel.getStyle();
            if (sNewStyle == null) sNewStyle = "";
            
            String sNewName = oProductViewModel.getProductFriendlyName();
            if (sNewName == null) sNewName = "";

            String sOriginalDescription = oDownloaded.getDescription();
            if (sOriginalDescription == null) sOriginalDescription = "";

            String sNewDescription = oProductViewModel.getDescription();
            if (sNewDescription == null) sNewDescription = "";


            if ((!sOriginalName.equals(sNewName)) || (!sOriginalStyle.equals(sNewStyle)) || (!sOriginalDescription.equals(sNewDescription))) {
                // Update the 2 fields that can be updated
                oDownloaded.getProductViewModel().setProductFriendlyName(oProductViewModel.getProductFriendlyName());
                oDownloaded.setDefaultStyle(oProductViewModel.getStyle());
                oDownloaded.setDescription(oProductViewModel.getDescription());
                
                try {
                    if (!sOriginalStyle.equals(sNewStyle)) {
                    	
                    	PublishedBandsRepository oPublishedBandsRepository = new PublishedBandsRepository();
                    	List<PublishedBand> aoPublishedBands = oPublishedBandsRepository.getPublishedBandsByProductName(oDownloaded.getFilePath());
                    	
                    	for (PublishedBand oPublishedBand : aoPublishedBands) {
                    		WasdiLog.debugLog("ProductResource.updateProductViewModel: change style for " + oPublishedBand.getLayerId());
                    		
                    		String sGeoServerUrl = oPublishedBand.getGeoserverUrl();
                    		
                    		if (Utils.isNullOrEmpty(sGeoServerUrl)) {
                    			sGeoServerUrl = WasdiConfig.Current.geoserver.address;
                    		}
                    		else {
                    			if (sGeoServerUrl.endsWith("/ows?")) {
                    				sGeoServerUrl = sGeoServerUrl.substring(0, sGeoServerUrl.length()-5);
                    			}
                    			else if (sGeoServerUrl.endsWith("/ows")) {
                    				sGeoServerUrl = sGeoServerUrl.substring(0, sGeoServerUrl.length()-4);
                    			}                    			
                    		}
                    		
                    		WasdiLog.debugLog("ProductResource.updateProductViewModel: sGeoServerUrl " + sGeoServerUrl);
                    		GeoServerManager oGeoServerManager = new GeoServerManager(sGeoServerUrl, WasdiConfig.Current.geoserver.user, WasdiConfig.Current.geoserver.password);
                    		
                    		
                    		
                    		if (!oGeoServerManager.styleExists(sNewStyle)) {
                    			WasdiLog.debugLog("ProductResource.updateProductViewModel: style does not exists: add it");
                    			
                                String sStylePath = PathsConfig.getStylesPath();

                                // Set the style file
                                sStylePath += sNewStyle + ".sld";

                                File oStyleFile = new File(sStylePath);

                                // Do we have the file?
                                if (oStyleFile.exists()) {
                                	oGeoServerManager.publishStyle(sStylePath);
                                }
                                else {
                                	WasdiLog.debugLog("ProductResource.updateProductViewModel: style file not found, this will be a problem");
                                	//TODO: trigger the copy of the sld file to the filesystem of the node
                                }
                    		}
                    		
                    		if (oGeoServerManager.configureLayerStyle(oPublishedBand.getLayerId(), sNewStyle)) {
                    			WasdiLog.debugLog("ProductResource.updateProductViewModel: style changed");
                    		}
                    		else {
                    			WasdiLog.debugLog("ProductResource.UpdateProductViewModel: error changing style");
                    		}
                    	}
                    }                	
                }
                catch (Exception oStyleEx) {
                	WasdiLog.errorLog("ProductResource.updateProductViewModel: Exception changing geoserver style " + oStyleEx.toString());
				}

                // Save
                if (oDownloadedFilesRepository.updateDownloadedFile(oDownloaded) == false) {
                    WasdiLog.debugLog("ProductResource.updateProductViewModel: There was an error updating Downloaded File.");
                    return Response.status(Status.INTERNAL_SERVER_ERROR).build();
                }
            }
            else {
            	WasdiLog.debugLog("ProductResource.updateProductViewModel: Nothing Changed");
            }

        } catch (Exception oEx) {
            WasdiLog.errorLog("ProductResource.updateProductViewModel error: " + oEx);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response.status(Status.OK).build();
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
        WasdiLog.debugLog("ProductResource.uploadfile( InputStream, WS: " + sWorkspaceId + ", Name: " + sName + " )");

        // before any operation check that this is not an injection attempt from the user
        if (sName.contains("/") || sName.contains("\\") || sWorkspaceId.contains("/") || sWorkspaceId.contains("\\")) {
            WasdiLog.warnLog("ProductResource.uploadfile: Injection attempt from users");
            return Response.status(Status.BAD_REQUEST).build();
        }

        User oUser = Wasdi.getUserFromSession(sSessionId);
        
        if (oUser == null) {
            WasdiLog.warnLog("ProductResource.uploadfile: invalid session");
            return Response.status(Status.UNAUTHORIZED).build();
        }
        
        // If workspace is not found in DB returns bad request
        if (!PermissionsUtils.canUserWriteWorkspace(oUser.getUserId(), sWorkspaceId)) {
            WasdiLog.warnLog("ProductResource.uploadfile: user cannot write in the workspace");
            return Response.status(Status.FORBIDDEN).build();
        }
        
        String sUserId = oUser.getUserId();

        // Check the file name
        if (Utils.isNullOrEmpty(sName) || sName.isEmpty()) {
            //get a random name
            sName = "defaultName-" + Utils.getRandomName();
        }        
        
        // Take path
        String sWorkspaceOwner = Wasdi.getWorkspaceOwner(sWorkspaceId);
        String sPath = PathsConfig.getWorkspacePath(sWorkspaceOwner, sWorkspaceId);

        File oOutputFilePath = new File(sPath + sName);

        WasdiLog.debugLog("ProductResource.uploadfile: destination " + oOutputFilePath.getAbsolutePath());

        if (oOutputFilePath.getParentFile().exists() == false) {
            WasdiLog.warnLog("ProductResource.uploadfile: Creating dirs " + oOutputFilePath.getParentFile().getAbsolutePath());
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

            PrimitiveResult oRes = Wasdi.runProcess(sUserId, sSessionId, LauncherOperations.INGEST.name(), oOutputFilePath.getName(), oParameter);

            if (oRes.getBoolValue()) {
                return Response.ok().build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }

        } catch (Exception e) {
            WasdiLog.errorLog("ProductResource.uploadfile: " + e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
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
        WasdiLog.debugLog("ProductResource.uploadFileByLib( InputStream, WS: " + sWorkspaceId + ", Name: " + sName + " )");

        // before any operation check that this is not an injection attempt from the user
        if (sName.contains("/") || sName.contains("\\") || sWorkspaceId.contains("/") || sWorkspaceId.contains("\\")) {
            WasdiLog.warnLog("ProductResource.uploadFileByLib: Injection attempt from users");
            return Response.status(Status.BAD_REQUEST).build();
        }

        // Check the user session
        User oUser = Wasdi.getUserFromSession(sSessionId);
        
        if (oUser == null) {
        	WasdiLog.warnLog("ProductResource.uploadFileByLib: invalid session");
            return Response.status(Status.UNAUTHORIZED).build();
        }

        // If workspace is not found in DB returns bad request
        if (!PermissionsUtils.canUserWriteWorkspace(oUser.getUserId(), sWorkspaceId)) {
            WasdiLog.warnLog("ProductResource.uploadFileByLib: user cannot write in the workspace");
            return Response.status(Status.FORBIDDEN).build();
        }

        // Check the file name
        if (Utils.isNullOrEmpty(sName) || sName.isEmpty()) {
            sName = "defaultName";
        }

        try {
            // Take path
            String sPath = PathsConfig.getWorkspacePath(Wasdi.getWorkspaceOwner(sWorkspaceId), sWorkspaceId);

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
        } catch (Exception oEx) {
            WasdiLog.errorLog("ProductResource.uploadFileByLib: " + oEx);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
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

        WasdiLog.debugLog("ProductResource.deleteProduct( Product: " + sProductName + ", Delete: " + bDeleteFile + ",  WS: " + sWorkspaceId + ", DeleteLayer: " + bDeleteLayer + " )");

        PrimitiveResult oReturn = new PrimitiveResult();
        oReturn.setBoolValue(false);
        
        if (bDeleteFile == null) bDeleteFile = true;
        if (bDeleteLayer == null) bDeleteLayer = true;

        // before any operation check that this is not an injection attempt from the user
        if (sProductName.contains("/") || sProductName.contains("\\") || sWorkspaceId.contains("/") || sWorkspaceId.contains("\\")) {
            WasdiLog.warnLog("ProductResource.deleteProduct: Injection attempt from users");
            oReturn.setIntValue(400);
            return oReturn;
        }

        User oUser = Wasdi.getUserFromSession(sSessionId);
        try {

            // Domain Check
            if (oUser == null) {
                WasdiLog.warnLog("ProductResource.deleteProduct: invalid session");
                oReturn.setIntValue(404);
                return oReturn;
            }
            if (Utils.isNullOrEmpty(sWorkspaceId)) {
                String sMessage = "workspace null or empty";
                WasdiLog.warnLog("ProductResource.deleteProduct: " + sMessage);
                oReturn.setStringValue(sMessage);
                oReturn.setIntValue(404);
                return oReturn;
            }
            // If workspace is not found in DB returns bad request
            if (!PermissionsUtils.canUserWriteWorkspace(oUser.getUserId(), sWorkspaceId)) {
                String sMessage = "ProductResource.deleteProduct: user cannot write in the workspace";
                WasdiLog.warnLog(sMessage);
                oReturn.setStringValue(sMessage);
                oReturn.setIntValue(403);
                return oReturn;
            }


            // Get the file path
            String sDownloadPath = PathsConfig.getWorkspacePath(Wasdi.getWorkspaceOwner(sWorkspaceId), sWorkspaceId);
            String sFilePath = sDownloadPath + sProductName;

            WasdiLog.debugLog("ProductResource.deleteProduct: File Path: " + sFilePath);

            PublishedBandsRepository oPublishedBandsRepository = new PublishedBandsRepository();

            List<PublishedBand> aoPublishedBands = null;

            DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
            DownloadedFile oDownloadedFile = oDownloadedFilesRepository.getDownloadedFileByPath(sDownloadPath + sProductName);

            if (oDownloadedFile == null) {
            	oDownloadedFile = oDownloadedFilesRepository.getDownloadedFileByPath(WasdiFileUtils.fixPathSeparator(sDownloadPath) + sProductName);
            }

            if (oDownloadedFile == null) {
            	List<DownloadedFile> aoDownloadedFiles = oDownloadedFilesRepository.getDownloadedFileListByName(sProductName);

            	if (aoDownloadedFiles != null && !aoDownloadedFiles.isEmpty()) {
            		oDownloadedFile = aoDownloadedFiles.get(0);
            	}
            }

            if (oDownloadedFile == null) {
                WasdiLog.warnLog("ProductResource.deleteProduct: invalid product");
                oReturn.setStringValue("Invalid Product");
                oReturn.setIntValue(403);
                return oReturn;
            }

            // Get the list of published bands
            if (bDeleteFile || bDeleteLayer) {
                // Get all bands files
                aoPublishedBands = oPublishedBandsRepository.getPublishedBandsByProductName(oDownloadedFile.getFilePath());
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
                        
                        if (sName.endsWith(".prj") && sProductName.endsWith(".asc") && sName.equals(sProductName.replace(".asc", ".prj"))) {
                        	return true;
                        }
                        
                        if (sProductName.endsWith(".grib") && (sName.equals(sProductName + ".gbx9") || sName.equals(sProductName + ".ncx4"))) {
                        	return true;
                        }

                        return false;
                    }
                };

                File[] aoFiles = oFolder.listFiles(oFilter);

                // If we found the files
                Long lFreedStorageSpace = 0L;
                if (aoFiles != null) {
                    // Delete all
                    WasdiLog.debugLog("ProductResource.deleteProduct: Number of files to delete " + aoFiles.length);
                    for (File oFile : aoFiles) {

                        WasdiLog.debugLog("ProductResource.deleteProduct: deleting file product " + oFile.getAbsolutePath() + "...");
                        
                        Long lFileSize = oFile.length();
                        
                        if (!FileUtils.deleteQuietly(oFile)) {
                            WasdiLog.debugLog("    ERROR");
                        } else {
                            WasdiLog.debugLog("    OK");
                            lFreedStorageSpace += lFileSize;
                        }
                    }
                } else {
                    WasdiLog.debugLog("ProductResource.deleteProduct: No File to delete ");
                }
                
                // update the size of the workspace
                if (lFreedStorageSpace > 0L) {
                	WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
                	Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sWorkspaceId);
                	Long lStorageSize = oWorkspace.getStorageSize();
                	
                	if (lStorageSize == null) {
                		WasdiLog.debugLog("ProductResource.deleteProduct. Storage size of the workspace not yet computed");
                		lStorageSize = Utils.computeWorkspaceStorageSize(sWorkspaceId);
                		
                		if (lStorageSize < 0L) {
                			lStorageSize = 0L;
                		}
                	}
                	
                	if (lStorageSize != null && lStorageSize > 0L) {
                		Long lUpdatedStorageSize = lStorageSize - lFreedStorageSpace;
                		
                		if (lUpdatedStorageSize < 0L)
                			lUpdatedStorageSize = 0L;
                		
                		oWorkspace.setStorageSize(lUpdatedStorageSize);
                		if (oWorkspaceRepository.updateWorkspace(oWorkspace)) {
                			WasdiLog.debugLog("ProductResource.deleteProduct. Workspace size after deleting product(s): " + lUpdatedStorageSize);
                		} else {
                			WasdiLog.warnLog("ProductResource.deleteProduct. Storage size of the workspace was not updated after deleting the products");
                		}
                			
                	}
                }
            }

            if (bDeleteLayer) {
            	
            	try {
                    // Delete layerId on Geoserver
                    GeoServerManager oGeoServerManager = new GeoServerManager();

                    // For all the published bands
                    for (PublishedBand oPublishedBand : aoPublishedBands) {
                        try {
                            WasdiLog.debugLog("ProductResource.deleteProduct: LayerId to delete " + oPublishedBand.getLayerId());

                            if (!oGeoServerManager.removeLayer(oPublishedBand.getLayerId())) {
                                WasdiLog.debugLog("ProductResource.deleteProduct: error deleting layer " + oPublishedBand.getLayerId() + " from geoserver");
                            }

                            try {
                                // delete published band on data base
                                oPublishedBandsRepository.deleteByProductNameLayerId(oDownloadedFile.getFilePath(), oPublishedBand.getLayerId());
                            } catch (Exception oEx) {
                                WasdiLog.errorLog("ProductResource.deleteProduct: error deleting published band on data base " + oEx);
                            }

                        } catch (Exception oEx) {
                            WasdiLog.errorLog("ProductResource.deleteProduct: " + oEx);
                        }
                    }            		
            	}
            	catch (Exception oEx) {
                    WasdiLog.errorLog("ProductResource.deleteProduct: Exception deleting layers: " + oEx);
                }            	
            }
            


            // delete the product-workspace related records on db and the Downloaded File Entry
            try {
                ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();
                oProductWorkspaceRepository.deleteByProductNameWorkspace(oDownloadedFile.getFilePath(), sWorkspaceId);
                oDownloadedFilesRepository.deleteByFilePath(oDownloadedFile.getFilePath());
            } catch (Exception oEx) {
                WasdiLog.errorLog("ProductResource.deleteProduct: error deleting product-workspace related records on db and the Downloaded File Entry " + oEx);
                oReturn.setIntValue(500);
                oReturn.setStringValue(oEx.toString());
                return oReturn;
            }

            // Is the product used also in other workspaces?
            List<DownloadedFile> aoDownloadedFileList = oDownloadedFilesRepository.getDownloadedFileListByName(oDownloadedFile.getFileName());

            if (aoDownloadedFileList.size() <= 1) {
                // Delete metadata
                try {
                    WasdiLog.debugLog("ProductResource.deleteProduct: Deleting Metadata file");

                    if (oDownloadedFile.getProductViewModel() != null) {
                        String sMetadataFilePath = oDownloadedFile.getProductViewModel().getMetadataFileReference();
                        if (!Utils.isNullOrEmpty(sMetadataFilePath)) {
                            FileUtils.deleteQuietly(new File(sDownloadPath + "/" + sMetadataFilePath));
                            WasdiLog.debugLog("Metadata file cleaned");
                        }
                    }

                } catch (Exception oEx) {
                    WasdiLog.errorLog("ProductResource.deleteProduct: error deleting Metadata " + oEx);
                    oReturn.setIntValue(500);
                    oReturn.setStringValue(oEx.toString());
                    return oReturn;
                }
            } else {
                WasdiLog.debugLog("ProductResource.deleteProduct: product also in other WS, do not delete metadata");
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
                WasdiLog.errorLog("ProductResource.deleteProduct: exception sending asynch notification");
            }


        } catch (Exception oEx) {
            WasdiLog.errorLog("ProductResource.deleteProduct: error deleting product " + oEx);
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
    @Produces({"application/json", "application/xml", "text/xml" })
    public PrimitiveResult deleteMultipleProduct(@HeaderParam("x-session-token") String sSessionId, @QueryParam("deletefile") Boolean bDeleteFile,
                                         @QueryParam("workspace") String sWorkspaceId, @QueryParam("deletelayer") Boolean bDeleteLayer,
                                         List<String> asProductList) {
    	
        // Support variable used to identify if deletions of one or more products failed
        AtomicBoolean bDirty = new AtomicBoolean(false);
        PrimitiveResult oPrimitiveResult = new PrimitiveResult();
        
    	User oUser = Wasdi.getUserFromSession(sSessionId);
    	
    	if (oUser == null) {
            WasdiLog.warnLog("ProductResource.deleteMultipleProduct: invalid session " );
            oPrimitiveResult.setIntValue(500);
            oPrimitiveResult.setBoolValue(false);
            return oPrimitiveResult;    		
    	}
    	
    	if (!PermissionsUtils.canUserWriteWorkspace(oUser.getUserId(), sWorkspaceId)) {
            WasdiLog.warnLog("ProductResource.deleteMultipleProduct: user cannot write in the workspace" );
            oPrimitiveResult.setIntValue(500);
            oPrimitiveResult.setBoolValue(false);
            return oPrimitiveResult;    		
    	}
        
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
