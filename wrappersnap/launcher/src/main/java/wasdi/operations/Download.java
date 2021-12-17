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
import wasdi.shared.payloads.DownloadPayload;
import wasdi.shared.utils.EndMessageProvider;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.viewmodels.products.ProductViewModel;

/**
 * Download Operation
 * 
 * Use a DownloadFileParameter
 * 
 * This operation takes in input the url and credentials of a data provider 
 * and get the image from there.
 * 
 * First the operation search for an already existing copy of the file in the local node and 
 * if it is present it just makes a copy.
 * 
 * If not available the DataProvider object is demanded to take the file.
 * 
 * After the file is available, the operation try to read to the file to get the View Model.
 * 
 * If everything is ok the file is added to the Db in DownloadedFile table and is added to the workspace.
 * 
 * @author p.campanella
 *
 */
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

            // Product view Model
            ProductViewModel oVM = null;

            // Get the file name
            String sFileNameWithoutPath = oProviderAdapter.getFileName(oParameter.getUrl());
            m_oLocalLogger.debug("Download.executeOperation: File to download: " + sFileNameWithoutPath);
            m_oProcessWorkspaceLogger.log("FILE " + sFileNameWithoutPath);

            DownloadedFile oAlreadyDownloaded = null;
            DownloadedFilesRepository oDownloadedRepo = new DownloadedFilesRepository();
            
            // If we have the name of the file
            if (!Utils.isNullOrEmpty(sFileNameWithoutPath)) {

                // First check if it is already in this workspace:
                oAlreadyDownloaded = oDownloadedRepo.getDownloadedFileByPath(sDownloadPath + sFileNameWithoutPath);

                if (oAlreadyDownloaded == null) {
                	
                	// Check if it is already downloaded, in any workpsace
                	
                    m_oLocalLogger.debug("Download.executeOperation: Product NOT found in the workspace, search in other workspaces");
                    
                    List<DownloadedFile> aoExistingList = oDownloadedRepo.getDownloadedFileListByName(sFileNameWithoutPath);

                    // Check if any of this is in this node
                    for (DownloadedFile oDownloadedCandidate : aoExistingList) {

                        if (new File(oDownloadedCandidate.getFilePath()).exists()) {
                            oAlreadyDownloaded = oDownloadedCandidate;
                            m_oLocalLogger.debug("Download.executeOperation: found already existing copy on this computing node");
                            break;
                        }
                    }
                }
                else {
                	File oAlreadyDownloadedFileCheck = new File(oAlreadyDownloaded.getFilePath());
                	if (oAlreadyDownloadedFileCheck.exists() == false) {
                  	  	m_oLocalLogger.debug("Download.executeOperation: Product found in the database but the file does not exists in the node");
                  	  	oAlreadyDownloaded = null;
                	}
                	else {
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
                    if (!m_oSendToRabbit.SendUpdateProcessMessage(oProcessWorkspace)) {
                        m_oLocalLogger.debug("Download.executeOperation: Error sending rabbitmq message to update process list");
                    }
                } else {
                    m_oLocalLogger.error("Download.executeOperation: sFileNameWithoutPath is null or empty!!");
                }

                // Download the File
                sFileName = oProviderAdapter.executeDownloadFile(oParameter.getUrl(), oParameter.getDownloadUser(), oParameter.getDownloadPassword(), sDownloadPath, oProcessWorkspace, oParameter.getMaxRetry());

//                sFileName = "C:\\Users\\PetruPetrescu\\.wasdi\\petru.petrescu@wasdi.cloud\\7e800be1-5df2-464c-811d-d7a4c6b0b9d6\\adaptor.mars.internal-1639073851.1098163-928-13-c39cbe87-7a7a-4880-9547-2cba6783c520.nc";
//                sFileName = "C:\\Users\\PetruPetrescu\\.wasdi\\petru.petrescu@wasdi.cloud\\7e800be1-5df2-464c-811d-d7a4c6b0b9d6\\reanalysis_era5_pressure_levels_U_19820404.grib";
//                sFileName = "C:\\Users\\PetruPetrescu\\.wasdi\\petru.petrescu@wasdi.cloud\\7e800be1-5df2-464c-811d-d7a4c6b0b9d6\\reanalysis_era5_pressure_levels_RH_U_V_20210202.netcdf";
//                sFileName = "C:\\Users\\PetruPetrescu\\.wasdi\\petru.petrescu@wasdi.cloud\\7e800be1-5df2-464c-811d-d7a4c6b0b9d6\\reanalysis_era5_pressure_levels_RH_U_V_20210302.grib";
//                sFileName = "C:\\Users\\PetruPetrescu\\.wasdi\\petru.petrescu@wasdi.cloud\\7e800be1-5df2-464c-811d-d7a4c6b0b9d6\\reanalysis_era5_single_levels_SST_SP_TP_20210404.grib";
//                sFileName = "C:\\Users\\PetruPetrescu\\.wasdi\\petru.petrescu@wasdi.cloud\\7e800be1-5df2-464c-811d-d7a4c6b0b9d6\\reanalysis_era5_single_levels_10U_10V_2DT_2T_SP_20201201";
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

                // Get The product view Model
				WasdiProductReader oProductReader = WasdiProductReaderFactory.getProductReader(new File(sFileName));
				sFileName = oProductReader.adjustFileAfterDownload(sFileName, sFileNameWithoutPath);
				File oProductFile = new File(sFileName);
				
				sFileNameWithoutPath = oProductFile.getName(); 
				
				Product oProduct = oProductReader.getSnapProduct();
				oVM = oProductReader.getProductViewModel();
				
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
            
            // Final Check: do we have at the end a valid file name?
            if (Utils.isNullOrEmpty(sFileName)) {
            	
            	// No, we are in error
                m_oLocalLogger.debug("Download.executeOperation: file is null there must be an error");

                String sError = "The name of the file to download result null";
                m_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.DOWNLOAD.name(), oParameter.getWorkspace(), sError, oParameter.getExchange());
                oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
                
                return false;
            } 
            
            // Ok, add the product to the db
            addProductToDbAndWorkspaceAndSendToRabbit(oVM, sFileName, oParameter.getWorkspace(), oParameter.getExchange(), LauncherOperations.DOWNLOAD.name(), oParameter.getBoundingBox());

            m_oProcessWorkspaceLogger.log("Operation Completed");
            m_oProcessWorkspaceLogger.log(new EndMessageProvider().getGood());

            DownloadPayload oDownloadPayload = new DownloadPayload();
            oDownloadPayload.setFileName(Utils.getFileNameWithoutLastExtension(sFileName));
            oDownloadPayload.setProvider(oParameter.getProvider());

            setPayload(oProcessWorkspace, oDownloadPayload);
            
            m_oLocalLogger.debug("Download.executeOperation: operation done");
            
            return true;
            
        } catch (Exception oEx) {
        	
            m_oLocalLogger.error("Download.executeOperation: Exception "
                    + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));

            String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(oEx);
            oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
            m_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.DOWNLOAD.name(), oParam.getWorkspace(),
                        sError, oParam.getExchange());
        }

        m_oLocalLogger.debug("Download.executeOperation: return file name " + sFileName);

        return false;
		
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
