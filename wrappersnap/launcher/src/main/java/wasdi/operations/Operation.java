package wasdi.operations;

import java.io.File;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import wasdi.LauncherMain;
import wasdi.ProcessWorkspaceLogger;
import wasdi.io.WasdiProductReader;
import wasdi.io.WasdiProductReaderFactory;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.DownloadedFileCategory;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.ProductWorkspace;
import wasdi.shared.business.Workspace;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProductWorkspaceRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.payloads.OperationPayload;
import wasdi.shared.rabbit.Send;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.products.GeorefProductViewModel;
import wasdi.shared.viewmodels.products.ProductViewModel;

/**
 * WASDI Abstract Operation Class
 * 
 * This class repersents an operation that can be executed by the WASDI Launcher.
 * 
 * Each Operations has a unique code, a parameter class used to exchange info between the web server and the launcher, 
 * an associated Process Workspaces that the represent the "instance" of an operation in a workspace.
 * 
 * 
 * 
 * @author p.campanella
 *
 */
public abstract class Operation {
	
	/**
	 * Logger to write to the database so that the user can access these logs from web-ui or libraries
	 */
	protected ProcessWorkspaceLogger m_oProcessWorkspaceLogger = new ProcessWorkspaceLogger(null);
	
	/**
	 * Utility to send messages to rabbit to notify client about progress of the operation
	 */
	protected Send m_oSendToRabbit = new Send(null);
	
	/**
	 * Process Workspace Repository
	 */
	protected ProcessWorkspaceRepository m_oProcessWorkspaceRepository = new ProcessWorkspaceRepository();

	/**
	 * Get the Process Workspace Logger
	 * @return ProcessWorkspaceLogger used by this operation
	 */
	public ProcessWorkspaceLogger getProcessWorkspaceLogger() {
		return m_oProcessWorkspaceLogger;
	}

	/**
	 * Set the Process Workspace Logger
	 * @param oProcessWorkspaceLogger ProcessWorkspaceLogger to use in the operation
	 */
	public void setProcessWorkspaceLogger(ProcessWorkspaceLogger oProcessWorkspaceLogger) {
		this.m_oProcessWorkspaceLogger = oProcessWorkspaceLogger;
	}
	
	/**
	 * Get the Send object 
	 * @return Send utility object
	 */
	public Send getSendToRabbit() {
		return m_oSendToRabbit;
	}

	/**
	 * Set the Send object 
	 * @param oSendToRabbit Send utility object
	 */
	public void setSendToRabbit(Send oSendToRabbit) {
		this.m_oSendToRabbit = oSendToRabbit;
	}

	/**
	 * Get the Process Workspace Repository
	 * @return ProcessWorkspaceRepository
	 */
	public ProcessWorkspaceRepository getProcessWorkspaceRepository() {
		return m_oProcessWorkspaceRepository;
	}

	/**
	 * Set the Process Workspace Repository
	 * @param oProcessWorkspaceRepository ProcessWorkspaceRepository
	 */
	public void setProcessWorkspaceRepository(ProcessWorkspaceRepository oProcessWorkspaceRepository) {
		this.m_oProcessWorkspaceRepository = oProcessWorkspaceRepository;
	}	
	
	/**
	 * Abstract Method that really executes the Operation
	 * @param oParam Specific Operation Parameter
	 * @param oProcessWorkspace Process Workspace that represents the actual instance of this operation
	 * @return true if done with success, false if failed
	 */
	public abstract boolean executeOperation(BaseParameter oParam, ProcessWorkspace oProcessWorkspace);
	
	/**
	 * Updates the process status and progress 
	 * @param oProcessWorkspace
	 * @param oProcessStatus
	 * @param iProgressPerc
	 */
    public void updateProcessStatus(ProcessWorkspace oProcessWorkspace, ProcessStatus oProcessStatus, int iProgressPerc) {    	
    	LauncherMain.updateProcessStatus(m_oProcessWorkspaceRepository, oProcessWorkspace, oProcessStatus, iProgressPerc);
    }
    
    /**
     * Set the file size to the process Object
     *
     * @param oFile             File to read the size from
     * @param oProcessWorkspace Process to update
     */
    public void setFileSizeToProcess(File oFile, ProcessWorkspace oProcessWorkspace) {

        if (oFile == null) {
            WasdiLog.errorLog("LauncherMain.SetFileSizeToProcess: input file is null");
            return;
        }

        if (!oFile.exists()) {
        	WasdiLog.errorLog("LauncherMain.SetFileSizeToProcess: input file does not exists");
            return;
        }

        // Take file size
        long lSize = oFile.length();

        // Check if it is a ".dim" file
        if (oFile.getName().endsWith(".dim")) {
            try {
                // Ok so the real size is the folder
                String sFolder = oFile.getAbsolutePath();

                // Get folder path
                sFolder = sFolder.replace(".dim", ".data");
                File oDataFolder = new File(sFolder);

                // Get folder size
                lSize = FileUtils.sizeOfDirectory(oDataFolder);
            } catch (Exception oEx) {
            	WasdiLog.errorLog("LauncherMain.SetFileSizeToProcess: Error computing folder size, oEx");
            }
        }

        setFileSizeToProcess(lSize, oProcessWorkspace);
    }

    /**
     * Set the file size to the process Object
     *
     * @param lSize             Size
     * @param oProcessWorkspace Process to update
     */
    protected void setFileSizeToProcess(Long lSize, ProcessWorkspace oProcessWorkspace) {

        if (oProcessWorkspace == null) {
        	WasdiLog.errorLog("LauncherMain.SetFileSizeToProcess: input process is null");
            return;
        }

        WasdiLog.debugLog("LauncherMain.SetFileSizeToProcess: File size  = " + Utils.getFormatFileDimension(lSize));
        oProcessWorkspace.setFileSize(Utils.getFormatFileDimension(lSize));
    }
    
    /**
     * Converts a product in a ViewModel, add it to the workspace and send it to the
     * rabbit queue The method is Safe: controls if the products already exists and
     * if it is already added to the workspace
     *
     * @param oVM               View Model... if null, read it from the product in sFileName
     * @param sFullPathFileName File Name
     * @param sWorkspace        Workspace
     * @param sExchange         Queue Id
     * @param sOperation        Operation Done
     * @param sBBox             Bounding Box
     */
    protected void addProductToDbAndWorkspaceAndSendToRabbit(ProductViewModel oVM, String sFullPathFileName,
                                                           String sWorkspace, String sExchange, String sOperation, String sBBox) throws Exception {
        addProductToDbAndWorkspaceAndSendToRabbit(oVM, sFullPathFileName, sWorkspace, sExchange, sOperation, sBBox, true);
    }

    /**
     * Converts a product in a ViewModel, add it to the workspace and send it to the
     * rabbit queue The method is Safe: controls if the products already exists and
     * if it is already added to the workspace
     *
     * @param oVM               View Model... if null, read it from the product in sFileName
     * @param sFullPathFileName File Name
     * @param sWorkspace        Workspace
     * @param sExchange         Queue Id
     * @param sOperation        Operation Done
     * @param sBBox             Bounding Box
     * @param bAsynchMetadata   Flag to know if save metadata in asynch or synch way
     * @throws Exception
     */
    protected void addProductToDbAndWorkspaceAndSendToRabbit(ProductViewModel oVM, String sFullPathFileName,
                                                           String sWorkspace, String sExchange, String sOperation, String sBBox, Boolean bAsynchMetadata)
            throws Exception {
        addProductToDbAndWorkspaceAndSendToRabbit(oVM, sFullPathFileName, sWorkspace, sExchange, sOperation, sBBox, bAsynchMetadata, true);
    }

    /**
     * Converts a product in a ViewModel, add it to the workspace and send it to the
     * rabbit queue The method is Safe: controls if the products already exists and
     * if it is already added to the workspace
     *
     * @param oVM               View Model... if null, read it from the product in sFileName
     * @param sFullPathFileName File Name
     * @param sWorkspace        Workspace
     * @param sExchange         Queue Id
     * @param sOperation        Operation Done
     * @param sBBox             Bounding Box
     * @param bAsynchMetadata   Flag to know if save metadata in asynch or synch way
     * @param bSendToRabbit     Flag to know if we need to notify rabbit
     * @throws Exception
     */
    protected void addProductToDbAndWorkspaceAndSendToRabbit(ProductViewModel oVM, String sFullPathFileName,
                                                           String sWorkspace, String sExchange, String sOperation, String sBBox, Boolean bAsynchMetadata,
                                                           Boolean bSendToRabbit) throws Exception {
        addProductToDbAndWorkspaceAndSendToRabbit(oVM, sFullPathFileName, sWorkspace, sExchange, sOperation, sBBox, bAsynchMetadata, bSendToRabbit, "");
    }

    /**
     * Converts a product in a ViewModel, add it to the workspace and send it to the
     * rabbit queue. The method is Safe: it controls if the products already exists and
     * if it is already added to the workspace
     *
     * @param oProductViewModel View Model... if null, read it from the product in sFileName
     * @param sFullPathFileName File Name
     * @param sWorkspace        Workspace
     * @param sExchange         Queue Id
     * @param sOperation        Operation Done
     * @param sBBox             Bounding Box
     * @param bAsynchMetadata   Flag to know if save metadata in asynch or synch way
     * @param bSendToRabbit     Flat to know it we need to send update on rabbit or not
     * @throws Exception
     */
    protected void addProductToDbAndWorkspaceAndSendToRabbit(ProductViewModel oProductViewModel, String sFullPathFileName,
                                                           String sWorkspace, String sExchange, String sOperation, String sBBox, Boolean bAsynchMetadata,
                                                           Boolean bSendToRabbit, String sStyle) throws Exception {
        WasdiLog.debugLog("LauncherMain.AddProductToDbAndSendToRabbit: File Name = " + sFullPathFileName);

        // Check if the file is really to Add
        DownloadedFilesRepository oDownloadedRepo = new DownloadedFilesRepository();
        DownloadedFile oCheckAlreadyExists = oDownloadedRepo.getDownloadedFileByPath(sFullPathFileName);

        if (oCheckAlreadyExists == null) {
        	oCheckAlreadyExists = oDownloadedRepo.getDownloadedFileByPath(WasdiFileUtils.fixPathSeparator(sFullPathFileName));
        }

        File oFile = new File(sFullPathFileName);

		WasdiProductReader oReadProduct = WasdiProductReaderFactory.getProductReader(oFile);

        // Get the Bounding Box
        if (Utils.isNullOrEmpty(sBBox)) {
            try {
                GeorefProductViewModel oGeorefProductViewModel = (GeorefProductViewModel) oProductViewModel;
                sBBox = oGeorefProductViewModel.getBbox();
            } catch (Exception oE) {
                WasdiLog.warnLog("LauncherMain.AddProductToDbAndSendToRabbit: could not extract BBox from GeorefProductViewModel due to: " + oE);
            }
        }
        if (Utils.isNullOrEmpty(sBBox)) {
            WasdiLog.debugLog("LauncherMain.AddProductToDbAndSendToRabbit: bbox not set. Try to auto get it ");
            sBBox = oReadProduct.getProductBoundingBox();
        }
        
        

        if (oCheckAlreadyExists == null) {

            // The VM Is Available?
            if (oProductViewModel == null) {

                // Get The product view Model
                WasdiLog.debugLog("LauncherMain.AddProductToDbAndSendToRabbit: read View Model");
                oProductViewModel = oReadProduct.getProductViewModel();

                WasdiLog.debugLog("LauncherMain.AddProductToDbAndSendToRabbit: done read product");
            }

            WasdiLog.debugLog("AddProductToDbAndSendToRabbit: Insert in db");

            // Save it in the register
            DownloadedFile oDownloadedProduct = new DownloadedFile();

            oDownloadedProduct.setFileName(oFile.getName());
            oDownloadedProduct.setFilePath(sFullPathFileName);
            oDownloadedProduct.setProductViewModel(oProductViewModel);
            oDownloadedProduct.setBoundingBox(sBBox);
            oDownloadedProduct.setRefDate(new Date());
            oDownloadedProduct.setDefaultStyle(sStyle);
            oDownloadedProduct.setCategory(DownloadedFileCategory.COMPUTED.name());

            if (oProductViewModel != null) {
                oDownloadedProduct.setDescription(oProductViewModel.getDescription());
            }

            // Insert in the Db
            if (!oDownloadedRepo.insertDownloadedFile(oDownloadedProduct)) {
                WasdiLog.errorLog("Impossible to Insert the new Product " + oFile.getName() + " in the database.");
            } else {
                WasdiLog.infoLog("Product Inserted");
            }
            
            // update also the count of the size of the workspace
            try {
	            Long lFileSize = Long.valueOf(oFile.length())
	            
	            WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
	            Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sWorkspace);
	            Long dUpdatedStorageSize = oWorkspace.getStorageSize() + lFileSize;
	            oWorkspace.setStorageSize(dUpdatedStorageSize);
	            oWorkspaceRepository.updateWorkspace(oWorkspace);
            } catch (Exception oEx) {
            	WasdiLog.errorLog("Operation.addProductToDbAndWorkspaceAndSendToRabbit: error in updating the storage size of the workspace", oEx);
            }
            
            
        } else {

            // The product is already there. No need to add
            if (oProductViewModel == null) {
                oProductViewModel = oCheckAlreadyExists.getProductViewModel();
            }

            // Update the Product View Model
            oCheckAlreadyExists.setProductViewModel(oProductViewModel);
            oDownloadedRepo.updateDownloadedFile(oCheckAlreadyExists);

            // TODO: Update metadata?

            WasdiLog.debugLog("AddProductToDbAndSendToRabbit: Product Already in the Database. Do not add");
        }

        // The Add Product to Workspace is safe. No need to check if the product is
        // already in the workspace
        addProductToWorkspace(oFile.getAbsolutePath(), sWorkspace, sBBox);

        if (bSendToRabbit) {
            WasdiLog.debugLog("LauncherMain.AddProductToDbAndSendToRabbit: Image added. Send Rabbit Message Exchange = " + sExchange);

            if (m_oSendToRabbit != null)
                m_oSendToRabbit.SendRabbitMessage(true, sOperation, sWorkspace, oProductViewModel, sExchange);
        }

        if (oReadProduct.getSnapProduct() != null) {
            oReadProduct.getSnapProduct().dispose();
        }

        WasdiLog.debugLog("LauncherMain.AddProductToDbAndSendToRabbit: Method finished");
    }
    
    /**
     * Adds a product to a Workspace. If it is already added it will not be
     * duplicated.
     *
     * @param sProductFullPath Product to Add
     * @param sWorkspaceId     Workspace Id
     * @return True if the product is already or have been added to the WS. False
     * otherwise
     */
    protected boolean addProductToWorkspace(String sProductFullPath, String sWorkspaceId, String sBbox) {

        try {

            // Create Repository
            ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();

            // Check if product is already in the Workspace
            if (oProductWorkspaceRepository.existsProductWorkspace(sProductFullPath, sWorkspaceId) == false) {

                // Create the entity
                ProductWorkspace oProductWorkspace = new ProductWorkspace();
                oProductWorkspace.setProductName(sProductFullPath);
                oProductWorkspace.setWorkspaceId(sWorkspaceId);
                oProductWorkspace.setBbox(sBbox);

                // Try to insert
                if (oProductWorkspaceRepository.insertProductWorkspace(oProductWorkspace)) {

                    WasdiLog.debugLog("LauncherMain.AddProductToWorkspace:  Inserted [" + sProductFullPath + "] in WS: [" + sWorkspaceId + "]");
                    return true;
                } else {

                    WasdiLog.debugLog("LauncherMain.AddProductToWorkspace:  Error adding [" + sProductFullPath + "] in WS: [" + sWorkspaceId + "]");
                    return false;
                }
            } else {
                WasdiLog.debugLog("LauncherMain.AddProductToWorkspace: Product [" + sProductFullPath + "] Already exists in WS: [" + sWorkspaceId + "]");
                return true;
            }
        } catch (Exception e) {
            WasdiLog.errorLog("LauncherMain.AddProductToWorkspace: Exception " + ExceptionUtils.getStackTrace(e));
        }

        return false;
    }
    
    /**
     * Set a payload to a Process Workspace
     * @param oProcessWorkspace
     * @param oPayload
     */
    protected void setPayload(ProcessWorkspace oProcessWorkspace, OperationPayload oPayload) {
        try {
            String sPayload = LauncherMain.s_oMapper.writeValueAsString(oPayload);
            oProcessWorkspace.setPayload(sPayload);
        } catch (Exception oPayloadEx) {
            WasdiLog.errorLog("Operation.setPayload: payload exception: " + oPayloadEx.toString());
        }
    }
    

}
