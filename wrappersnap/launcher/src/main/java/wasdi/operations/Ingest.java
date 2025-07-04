package wasdi.operations;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import wasdi.io.WasdiProductReader;
import wasdi.io.WasdiProductReaderFactory;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.IngestFileParameter;
import wasdi.shared.payloads.IngestPayload;
import wasdi.shared.utils.EndMessageProvider;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.ZipFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.products.ProductViewModel;

public class Ingest extends Operation {

	@Override
	public boolean executeOperation(BaseParameter oParam, ProcessWorkspace oProcessWorkspace) {

		WasdiLog.infoLog("Ingest.executeOperation");

		if (oParam == null) {
			WasdiLog.errorLog("Parameter is null");
			return false;
		}
		
		if (oProcessWorkspace == null) {
			WasdiLog.errorLog("Process Workspace is null");
			return false;
		}

        try {
        	
            IngestFileParameter oParameter = (IngestFileParameter) oParam;
            
            String sFilePath = oParameter.getFilePath();
            
            File oFileToIngestPath = new File(sFilePath);
            
            // Save the payload
            IngestPayload oPayload = new IngestPayload();
            oPayload.setFile(oFileToIngestPath.getName());
            oPayload.setWorkspace(oParameter.getWorkspace());
            
            setPayload(oProcessWorkspace, oPayload);            
            
            // The file must existst
            if (!oFileToIngestPath.canRead()) {
                String sMsg = "Ingest.executeOperation: ERROR: unable to access file to Ingest " + oFileToIngestPath.getAbsolutePath();
                WasdiLog.errorLog(sMsg);
                throw new IOException("Unable to access file to Ingest");
            }
            
            // get file size
            long lFileSizeByte = oFileToIngestPath.length();

            // set file size
            setFileSizeToProcess(lFileSizeByte, oProcessWorkspace);
            // Update status
            updateProcessStatus(oProcessWorkspace, ProcessStatus.RUNNING, 0);

            String sDestinationPath = PathsConfig.getWorkspacePath(oParameter);

            File oDstDir = new File(sDestinationPath);

            if (!oDstDir.exists()) {
                oDstDir.mkdirs();
            }

            m_oProcessWorkspaceLogger.log("Ingest Start - File " + oFileToIngestPath.getName() + " in Workspace " + oParameter.getWorkspace());

            if (!oDstDir.isDirectory() || !oDstDir.canWrite()) {
                WasdiLog.errorLog("Ingest.executeOperation: ERROR: unable to access destination directory " + oDstDir.getAbsolutePath());
                m_oProcessWorkspaceLogger.log("Error accessing destination directory");
                throw new IOException("Unable to access destination directory for the Workspace");
            }

            // Usually, we do not unzip after the copy
            boolean bUnzipAfterCopy = false;

            WasdiProductReader oReadProduct = null;
            ProductViewModel oImportProductViewModel = null;
            
            // Try to read the Product view Model
            try {
				oReadProduct = WasdiProductReaderFactory.getProductReader(oFileToIngestPath);
				oImportProductViewModel = oReadProduct.getProductViewModel();
            } catch (Exception oE) {
            	WasdiLog.errorLog("Ingest.executeOperation: cannot read product. Maybe file needs unzipping?");
			}

            String sDestinationFileName = oFileToIngestPath.getName();
            
            // If we do not have the view model here, we were not able to open the file
            if (oImportProductViewModel == null) {

                m_oProcessWorkspaceLogger.log("Error reading the input product.");

                WasdiLog.errorLog("Ingest.executeOperation: ERROR: unable to get the product view model");
                throw new IOException("Unable to get the product view model");
            }
            
            if (WasdiFileUtils.isShapeFileZipped(sFilePath, 100)) {
            	bUnzipAfterCopy = true;
            	sDestinationFileName = WasdiFileUtils.getShpFileNameFromZipFile(sFilePath, 30);
            	WasdiFileUtils.deleteFile(sFilePath);
            }

            updateProcessStatus(oProcessWorkspace, ProcessStatus.RUNNING, 50);

            // copy file to workspace directory
            if (!oFileToIngestPath.getParent().equals(oDstDir.getAbsolutePath())) {

                WasdiLog.debugLog("Ingest.executeOperation: File in another folder make a copy");
                FileUtils.copyFileToDirectory(oFileToIngestPath, oDstDir);

                m_oProcessWorkspaceLogger.log("File ingestion done");

                // Must be unzipped?
                if (bUnzipAfterCopy) {

                    WasdiLog.debugLog("File must be unzipped");
                    ZipFileUtils oZipExtractor = new ZipFileUtils(oParameter.getProcessObjId());
                    oZipExtractor.unzip(oFileToIngestPath.getCanonicalPath(), oDstDir.getCanonicalPath());
                    WasdiLog.debugLog("Unzip done");

                    m_oProcessWorkspaceLogger.log("File unzipped");
                }
            } else {
                WasdiLog.debugLog("Ingest.executeOperation: File already in the right path no need to copy");
                m_oProcessWorkspaceLogger.log("File already in place");
            }

            File oDstFile = new File(oDstDir, sDestinationFileName);

            updateProcessStatus(oProcessWorkspace, ProcessStatus.RUNNING, 75);

            // Snap set the name of geotiff files as geotiff: let replace with the file name
            if (oImportProductViewModel.getName().equals("geotiff")) {
                oImportProductViewModel.setName(oImportProductViewModel.getFileName());
            }
            
            // Hanlde netcdf zipped files read by SNAP
            try {
                if (oImportProductViewModel.getFileName().endsWith(".zip")) {
                	List<String> asFiles = ZipFileUtils.peepZipArchiveContent(oDstFile.getAbsolutePath());
                	
                	if (asFiles!=null) {
                		if (asFiles.size() == 1) {
                			String sFile = asFiles.get(0);
                			String sExt = WasdiFileUtils.getFileNameExtension(sFile);
                			if (sExt.toLowerCase().equals("nc")) {
                				WasdiLog.infoLog("Ingest.executeOperation: this looks a zip file with only on netcdf inside. We set the name with the .zip extension");
                				oImportProductViewModel.setName(oImportProductViewModel.getFileName());
                			}
                		}
                	}
                }            	
            }
            catch (Exception oEx) {
            	WasdiLog.errorLog("Ingest.executeOperation: Exception occurred while trying to detect if it is only a netcdf zipped", oEx);
			}

            // add product to db
            addProductToDbAndWorkspaceAndSendToRabbit(oImportProductViewModel, oDstFile.getAbsolutePath(),
                    oParameter.getWorkspace(), oParameter.getExchange(), LauncherOperations.INGEST.name(), null, true, true, oParameter.getStyle(), oParameter.getPlatform());
            
            updateProcessStatus(oProcessWorkspace, ProcessStatus.DONE, 100);

            m_oProcessWorkspaceLogger.log("Ingestion Done (Burp) - " + new EndMessageProvider().getGood());

            return true;

        } 
        catch (Throwable e) {
            WasdiLog.errorLog("Ingest.executeOperation: ERROR: Exception occurrend during file ingestion");
            
            String sError = ExceptionUtils.getMessage(e);
            WasdiLog.errorLog("Ingest.executeOperation: " + sError);
            
            m_oProcessWorkspaceLogger.log("Exception ingesting the file");

            oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
            m_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.INGEST.name(), oParam.getWorkspace(), sError, oParam.getExchange());
            
        }
        
		return false;
	}

	private void deleteZipFile(File oZippedFileToIngestWithAbsolutePath) {
		String sFileName = null;
		try {
			sFileName = oZippedFileToIngestWithAbsolutePath.getName();
		    if(!oZippedFileToIngestWithAbsolutePath.delete()) {
		    	WasdiLog.errorLog("Ingest.executeOperation: could not delete zip file " + oZippedFileToIngestWithAbsolutePath.getName());
		    }
		} catch (Exception oE) {
			WasdiLog.warnLog("Ingest.executeOperation: exception while trying to delete zip file "  + sFileName +  ": " + oE);
		}
	}

}
