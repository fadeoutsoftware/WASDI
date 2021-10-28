package wasdi.operations;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.esa.snap.core.datamodel.Product;

import wasdi.LauncherMain;
import wasdi.ProcessWorkspaceUpdateSubscriber;
import wasdi.dataproviders.ProviderAdapter;
import wasdi.dataproviders.ProviderAdapterFactory;
import wasdi.io.WasdiProductReader;
import wasdi.io.WasdiProductReaderFactory;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.DownloadedFileCategory;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.DownloadFileParameter;
import wasdi.shared.payload.DownloadPayload;
import wasdi.shared.utils.EndMessageProvider;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.ZipExtractor;
import wasdi.shared.viewmodels.products.ProductViewModel;

public class Download extends Operation implements ProcessWorkspaceUpdateSubscriber {

	@Override
	public boolean executeOperation(BaseParameter oParam, ProcessWorkspace oProcessWorkspace) {
		
		m_oLocalLogger.debug("Download.executeOperation");
		
		if (oParam == null) {
			m_oLocalLogger.error("Parameter is null");
			return false;
		}
		
		if (oProcessWorkspace == null) {
			m_oLocalLogger.error("Process Workspace is null");
			return false;
		}

        String sFileName = "";

        try {
        	
        	DownloadFileParameter oParameter = (DownloadFileParameter) oParam;
        	
            updateProcessStatus(m_oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 0);

            m_oLocalLogger.debug("Download.executeOperation: Download Start");
            m_oProcessWorkspaceLogger.log("Fetch Start - PROVIDER " + oParameter.getProvider());

            ProviderAdapter oProviderAdapter = new ProviderAdapterFactory().supplyProviderAdapter(oParameter.getProvider());
			oProviderAdapter.readConfig();

            if (oProviderAdapter != null) {
                oProviderAdapter.subscribe(this);
            }
            
            oProviderAdapter.setProviderUser(oParameter.getDownloadUser());
            oProviderAdapter.setProviderPassword(oParameter.getDownloadPassword());
            
            oProviderAdapter.setProcessWorkspace(oProcessWorkspace);
            // get file size
            long lFileSizeByte = oProviderAdapter.getDownloadFileSize(oParameter.getUrl());
            // set file size
            setFileSizeToProcess(lFileSizeByte, oProcessWorkspace);

            
            String sDownloadPath = LauncherMain.getWorkspacePath(oParameter);

            m_oLocalLogger.debug("Download.executeOperationPath: " + sDownloadPath);

            // Product view Model
            ProductViewModel oVM = null;

            // Get the file name
            String sFileNameWithoutPath = oProviderAdapter.getFileName(oParameter.getUrl());
            m_oLocalLogger.debug("Download.executeOperation: File to download: " + sFileNameWithoutPath);
            m_oProcessWorkspaceLogger.log("FILE " + sFileNameWithoutPath);

            DownloadedFile oAlreadyDownloaded = null;
            DownloadedFilesRepository oDownloadedRepo = new DownloadedFilesRepository();

            if (!Utils.isNullOrEmpty(sFileNameWithoutPath)) {

                // First check if it is already in this workspace:
                oAlreadyDownloaded = oDownloadedRepo.getDownloadedFileByPath(sDownloadPath + sFileNameWithoutPath);

                if (oAlreadyDownloaded == null) {

                    m_oLocalLogger.debug("Download.executeOperation: Product NOT found in the workspace, search in other workspaces");
                    // Check if it is already downloaded, in any workpsace
                    List<DownloadedFile> aoExistingList = oDownloadedRepo.getDownloadedFileListByName(sFileNameWithoutPath);

                    // Check if any of this is in this node
                    for (DownloadedFile oDownloadedCandidate : aoExistingList) {

                        if (new File(oDownloadedCandidate.getFilePath()).exists()) {
                            oAlreadyDownloaded = oDownloadedCandidate;
                            m_oLocalLogger.debug("Download.executeOperation: found already existing copy on this computing node");
                            break;
                        }
                    }

                } else {

                    File oAlreadyDownloadedFileCheck = new File(oAlreadyDownloaded.getFilePath());

                    if (oAlreadyDownloadedFileCheck.exists() == false) {

                	  // If the case is S5P, check also the existence of the .nc file
                	  if (sFileNameWithoutPath.startsWith("S5P")) {
                  		String sNcFilePath = oAlreadyDownloaded.getFilePath().replace(".zip", ".nc");
                  		oAlreadyDownloadedFileCheck = new File(sNcFilePath);

                  		if (oAlreadyDownloadedFileCheck.exists() == false) {
                  			m_oLocalLogger.debug("Download.executeOperation: Product already found in the database but the file does not exists in the node");
                  			oAlreadyDownloaded = null;
                  		} else {
                  			m_oLocalLogger.debug("Download.executeOperation: Product already found in the node with the .nc extension");
                  		}
                	  
                	  } else {
                    	  m_oLocalLogger.debug("Download.executeOperation: Product already found in the database but the file does not exists in the node");
	                      oAlreadyDownloaded = null;
                	  }
                  } else {
                      m_oLocalLogger.debug("Download.executeOperation: Product already found in the node");
                  }
                    
                }
            }

            if (oAlreadyDownloaded == null) {
                m_oLocalLogger.debug("Download.executeOperation: File not already downloaded. Download it");

                if (!Utils.isNullOrEmpty(sFileNameWithoutPath)) {
                    oProcessWorkspace.setProductName(sFileNameWithoutPath);
                    // update the process
                    if (!m_oProcessWorkspaceRepository.updateProcess(oProcessWorkspace))
                        m_oLocalLogger.debug("Download.executeOperation: Error during process update with file name");

                    // send update process message
                    if (m_oSendToRabbit != null && !m_oSendToRabbit.SendUpdateProcessMessage(oProcessWorkspace)) {
                        m_oLocalLogger.debug("Download.executeOperation: Error sending rabbitmq message to update process list");
                    }
                } else {
                    m_oLocalLogger.error("Download.executeOperation: sFileNameWithoutPath is null or empty!!");
                }

                // No: it isn't: download it
                sFileName = oProviderAdapter.executeDownloadFile(oParameter.getUrl(), oParameter.getDownloadUser(), oParameter.getDownloadPassword(), sDownloadPath, oProcessWorkspace, oParameter.getMaxRetry());

                if (Utils.isNullOrEmpty(sFileName)) {

                    int iLastError = oProviderAdapter.getLastServerError();
                    String sError = "There was an error contacting the provider";

                    if (iLastError > 0)
                        sError += ": query obtained HTTP Error Code " + iLastError;

                    m_oProcessWorkspaceLogger.log(sError);
                    
                    oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
                    
                    return false;
                }

                oProviderAdapter.unsubscribe(this);

                m_oProcessWorkspaceLogger.log("Got File, try to read");

                // Control Check for the file Name
                sFileName = WasdiFileUtils.fixPathSeparator(sFileName);

                if (sFileNameWithoutPath.startsWith("S3") && sFileNameWithoutPath.toLowerCase().endsWith(".zip")) {
                    m_oLocalLogger.debug("Download.executeOperation: File is a Sentinel 3 image, start unzip");
                    ZipExtractor oZipExtractor = new ZipExtractor(oParameter.getProcessObjId());
                    oZipExtractor.unzip(sDownloadPath + File.separator + sFileNameWithoutPath, sDownloadPath);
                    String sFolderName = sDownloadPath + sFileNameWithoutPath.replace(".zip", ".SEN3");
                    m_oLocalLogger.debug("Download.executeOperation: Unzip done, folder name: " + sFolderName);
                    sFileName = sFolderName + "/" + "xfdumanifest.xml";
                    m_oLocalLogger.debug("Download.executeOperation: File Name changed in: " + sFileName);
                }
				
				if (sFileNameWithoutPath.startsWith("S5P") && sFileNameWithoutPath.toLowerCase().endsWith(".zip")) {
					m_oLocalLogger.debug("Download.executeOperation: File is a Sentinel 5P image, start unzip");
					
//						ZipExtractor oZipExtractor = new ZipExtractor(oParameter.getProcessObjId());
//						oZipExtractor.unzip(sDownloadPath + File.separator + sFileNameWithoutPath, sDownloadPath);

					String sSourceFilePath = sDownloadPath + File.separator + sFileNameWithoutPath;
					String sTargetDirectoryPath = sDownloadPath;

					File oSourceFile = new File(sSourceFilePath);
					File oTargetDirectory = new File(sTargetDirectoryPath);
					WasdiFileUtils.cleanUnzipFile(oSourceFile, oTargetDirectory);

					String sFolderName = sDownloadPath + sFileNameWithoutPath.replace(".zip", "");
					m_oLocalLogger.debug("Download.executeOperation: Unzip done, folder name: " + sFolderName);
					
					sFileName = sFolderName + ".nc";
					sFileNameWithoutPath = sFileNameWithoutPath.replace(".zip", ".nc");
					m_oLocalLogger.debug("Download.executeOperation: File Name changed in: " + sFileName);
				}					

                // Get The product view Model
                File oProductFile = new File(sFileName);
				WasdiProductReader oReadProduct = WasdiProductReaderFactory.getProductReader(oProductFile);
				
				Product oProduct = oReadProduct.getSnapProduct();
				oVM = oReadProduct.getProductViewModel();

                if (oVM != null) {
                    // Snap set the name of geotiff files as geotiff: let replace with the file name
                    if (oVM.getName().equals("geotiff")) {
                        oVM.setName(oVM.getFileName());
                    }
                }

                // Save Metadata
                //oVM.setMetadataFileReference(asynchSaveMetadata(sFileName));

                if (Utils.isNullOrEmpty(sFileNameWithoutPath)) {
                    sFileNameWithoutPath = oProductFile.getName();
                    m_oLocalLogger.debug("Download.executeOperation: sFileNameWithoutPath still null, forced to: " + sFileNameWithoutPath);
                }

                // Save it in the register
                oAlreadyDownloaded = new DownloadedFile();
                oAlreadyDownloaded.setFileName(sFileNameWithoutPath);
                oAlreadyDownloaded.setFilePath(sFileName);
                oAlreadyDownloaded.setProductViewModel(oVM);

                String sBoundingBox = oParameter.getBoundingBox();

                if (!Utils.isNullOrEmpty(sBoundingBox)) {
                    if (sBoundingBox.startsWith("POLY") || sBoundingBox.startsWith("MULTI")) {
                        sBoundingBox = Utils.polygonToBounds(sBoundingBox);
                    }

                    oAlreadyDownloaded.setBoundingBox(sBoundingBox);
                } else {
                    m_oLocalLogger.info("Download.executeOperation: bounding box not available in the parameter");
                }

				if (oProduct != null) {
					if (oProduct.getStartTime()!=null) {
							oAlreadyDownloaded.setRefDate(oProduct.getStartTime().getAsDate());
						}						
                }

                oAlreadyDownloaded.setCategory(DownloadedFileCategory.DOWNLOAD.name());
                oDownloadedRepo.insertDownloadedFile(oAlreadyDownloaded);
            } else {
                m_oLocalLogger.debug("Download.executeOperation: File already downloaded: make a copy");

                // Yes!! Here we have the path
                sFileName = oAlreadyDownloaded.getFilePath();

                m_oLocalLogger.debug("Download.executeOperation: Check if file exists");

                // Check the path where we want the file
                String sDestinationFileWithPath = sDownloadPath + sFileNameWithoutPath;

                // Is it different?
                if (sDestinationFileWithPath.equals(sFileName) == false) {
                    // if file doesn't exist
                    if (!new File(sDestinationFileWithPath).exists()) {
                        // Yes, make a copy
                        FileUtils.copyFile(new File(sFileName), new File(sDestinationFileWithPath));
                        // Files.createLink(link, existing)
                        sFileName = sDestinationFileWithPath;
                    } else {
                        // If it exists...
                        sFileName = sDestinationFileWithPath;
                    }
                }

            } 

            if (Utils.isNullOrEmpty(sFileName)) {
                m_oLocalLogger.debug("Download.executeOperation: file is null there must be an error");

                String sError = "The name of the file to download result null";
                m_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.DOWNLOAD.name(), oParameter.getWorkspace(), sError, oParameter.getExchange());
                oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
                
                return false;
            } else {

                addProductToDbAndWorkspaceAndSendToRabbit(oVM, sFileName, oParameter.getWorkspace(), oParameter.getExchange(), LauncherOperations.DOWNLOAD.name(), oParameter.getBoundingBox());

                m_oLocalLogger.debug("Download.executeOperation: Add Product to Db and Send to Rabbit Done");

                m_oLocalLogger.debug("Download.executeOperation: Set process workspace state as done");

                oProcessWorkspace.setStatus(ProcessStatus.DONE.name());

                m_oProcessWorkspaceLogger.log("Operation Completed");
                m_oProcessWorkspaceLogger.log(new EndMessageProvider().getGood());

                DownloadPayload oDownloadPayload = new DownloadPayload();
                oDownloadPayload.setFileName(Utils.getFileNameWithoutLastExtension(sFileName));
                oDownloadPayload.setProvider(oParameter.getProvider());

                try {
                    String sPayload = LauncherMain.s_oMapper.writeValueAsString(oDownloadPayload);
                    oProcessWorkspace.setPayload(sPayload);
                } catch (Exception oPayloadEx) {
                    m_oLocalLogger.error("Download.executeOperation: payload exception: " + oPayloadEx.toString());
                }

                m_oLocalLogger.debug("Download.executeOperation: Set process workspace passed");
            }
        } catch (Exception oEx) {
            oEx.printStackTrace();
            m_oLocalLogger.error("Download.executeOperation: Exception "
                    + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));

            String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(oEx);
            oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
            m_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.DOWNLOAD.name(), oParam.getWorkspace(),
                        sError, oParam.getExchange());
        }

        m_oLocalLogger.debug("Download.executeOperation: return file name " + sFileName);

        return true;		
		
	}
	
    @Override
    public void notify(ProcessWorkspace oProcessWorkspace) {

        if (oProcessWorkspace == null) return;

        //if (!m_bNotifyDownloadUpdateActive) return;

        try {
            ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();

            // update the process
            if (!oProcessWorkspaceRepository.updateProcess(oProcessWorkspace))
                m_oLocalLogger.error("Download.executeOperationFile: Error during process update with process Perc");

            // send update process message
            if (m_oSendToRabbit != null) {
                if (!m_oSendToRabbit.SendUpdateProcessMessage(oProcessWorkspace)) {
                	m_oLocalLogger.error("Download.executeOperationFile: Error sending rabbitmq message to update process list");
                }
            }
        } catch (Exception oEx) {
        	m_oLocalLogger.error("Download.executeOperationFile: Exception: " + oEx);
        	m_oLocalLogger.debug("Download.executeOperationFile: " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
        }
    }
	

}
