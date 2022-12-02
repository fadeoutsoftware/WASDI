package wasdi.operations;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.esa.snap.core.datamodel.Product;

import wasdi.ProcessWorkspaceUpdateSubscriber;
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
import wasdi.shared.parameters.ShareFileParameter;
import wasdi.shared.payloads.DownloadPayload;
import wasdi.shared.utils.EndMessageProvider;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.products.ProductViewModel;

/**
 * Share Operation
 * 
 * Use a ShareFileParameter
 * 
 * This operation takes in input the origin workspaceId and the product name 
 * and gets the image from there.
 * 
 * First the operation search for an already existing copy of the file in the indicated origin workspace and 
 * if it is present it just makes a copy.
 * 
 * If not available the DataProvider object is null.
 * 
 * After the file is available, the operation try to read to the file to get the View Model.
 * 
 * If everything is ok the file is added to the Db in DownloadedFile table and is added to the workspace.
 * 
 * @author PetruPetrescu
 *
 */
public class Share extends Operation implements ProcessWorkspaceUpdateSubscriber {

	@Override
	public void notify(ProcessWorkspace oProcessWorkspace) {
		if (oProcessWorkspace == null) return;

		try {
			ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();

			// update the process
			if (!oProcessWorkspaceRepository.updateProcess(oProcessWorkspace))
				WasdiLog.errorLog("Share.executeOperationFile: Error during process update with process Perc");

			// send update process message
			if (m_oSendToRabbit != null) {
				if (!m_oSendToRabbit.SendUpdateProcessMessage(oProcessWorkspace)) {
					WasdiLog.errorLog("Share.executeOperationFile: Error sending rabbitmq message to update process list");
				}
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("Share.executeOperationFile: Exception: " + oEx);
			WasdiLog.debugLog("Share.executeOperationFile: " + ExceptionUtils.getStackTrace(oEx));
		}
	}

	@Override
	public boolean executeOperation(BaseParameter oParam, ProcessWorkspace oProcessWorkspace) {
		WasdiLog.debugLog("Share.executeOperation");

		if (oParam == null) {
			WasdiLog.errorLog("Parameter is null");
			return false;
		}

		if (oProcessWorkspace == null) {
			WasdiLog.errorLog("Process Workspace is null");
			return false;
		}

		String sFileName = "";

		try {
			ShareFileParameter oParameter = (ShareFileParameter) oParam;

//			if (Utils.isNullOrEmpty(oParameter.getProvider())) {
//				oParameter.setProvider("AUTO");
//			}

//			m_oProcessWorkspaceLogger.log("Fetch Start - REQUESTED PROVIDER " + oParameter.getProvider());

			if (oParameter.getDestinationWorkspaceNode().equals(oParameter.getOriginWorkspaceNode())) {

				// get file size
				File oOriginFile = new File(oParameter.getOriginFilePath());
				long lFileSizeByte = FileUtils.sizeOf(oOriginFile);

				// set file size
				setFileSizeToProcess(lFileSizeByte, oProcessWorkspace);

//				String sDownloadPath = oParameter.getOriginFilePath();

				// Product view Model
				ProductViewModel oVM = null;

				// Get the file name
				String sFileNameWithoutPath = oParameter.getProductName();
				WasdiLog.debugLog("Share.executeOperation: File to share: " + sFileNameWithoutPath);
				m_oProcessWorkspaceLogger.log("FILE " + sFileNameWithoutPath);

				if (!Utils.isNullOrEmpty(sFileNameWithoutPath)) {
					oProcessWorkspace.setProductName(sFileNameWithoutPath);

					// update the process
					m_oProcessWorkspaceRepository.updateProcess(oProcessWorkspace);

					// Send Rabbit notification
					m_oSendToRabbit.SendUpdateProcessMessage(oProcessWorkspace);
				} else {
					WasdiLog.errorLog("Share.executeOperation: sFileNameWithoutPath is null or empty!!");
				}

				Product oProduct = null;

				File oDestinationFile = new File(oParameter.getDestinationFilePath());

				try {
					FileUtils.copyFile(oOriginFile, oDestinationFile);
				} catch (Exception oE) {
					WasdiLog.errorLog("Share.executeOperation: could not copy file due to: " + oE);
				}

				if (oDestinationFile.exists()) {
					sFileName = oDestinationFile.getCanonicalPath();

					// Control Check for the file Name
					sFileName = WasdiFileUtils.fixPathSeparator(sFileName);

					// Get The product view Model
					WasdiProductReader oProductReader = WasdiProductReaderFactory.getProductReader(new File(sFileName));
					sFileName = oProductReader.adjustFileAfterDownload(sFileName, sFileNameWithoutPath);
					File oProductFile = new File(sFileName);

					sFileNameWithoutPath = oProductFile.getName();

					oProduct = oProductReader.getSnapProduct();

					try {
						oVM = oProductReader.getProductViewModel();
					} catch (Exception oVMEx) {
						WasdiLog.warnLog("Share.executeOperation: exception reading Product View Model " + oVMEx.toString());
					}

					if (oVM == null) {
						// Reset the cycle to search a better solution
						sFileName = "";
					}

					m_oProcessWorkspaceLogger.log("Got File, try to read");


					// Save it in the register
					DownloadedFile oAlreadyDownloaded = new DownloadedFile();
					oAlreadyDownloaded.setFileName(sFileNameWithoutPath);
					oAlreadyDownloaded.setFilePath(sFileName);
					oAlreadyDownloaded.setProductViewModel(oVM);

					String sBoundingBox = oParameter.getBoundingBox();

					if (!Utils.isNullOrEmpty(sBoundingBox)) {
						oAlreadyDownloaded.setBoundingBox(sBoundingBox);
					} else {
						WasdiLog.infoLog("Share.executeOperation: bounding box not available in the parameter");
					}

					if (oProduct != null) {
						if (oProduct.getStartTime()!=null) {
							oAlreadyDownloaded.setRefDate(oProduct.getStartTime().getAsDate());
						}
					}

					oAlreadyDownloaded.setCategory(DownloadedFileCategory.SHARED.name());

					DownloadedFilesRepository oDownloadedRepo = new DownloadedFilesRepository();
					oDownloadedRepo.insertDownloadedFile(oAlreadyDownloaded);
				} else {
					String sError = "There was an error when copying the file";
					m_oProcessWorkspaceLogger.log(sError);
				}

				// Final Check: do we have at the end a valid file name?
				if (Utils.isNullOrEmpty(sFileName)) {
					// No, we are in error
					WasdiLog.debugLog("Share.executeOperation: file is null there must be an error");

					String sError = "The name of the file to share result null";
					m_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.SHARE.name(), oParameter.getWorkspace(), sError, oParameter.getExchange());

					oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());

					return false;
				}

				// Ok, add the product to the db
				addProductToDbAndWorkspaceAndSendToRabbit(oVM, sFileName, oParameter.getWorkspace(), oParameter.getExchange(), LauncherOperations.SHARE.name(), oParameter.getBoundingBox());

				m_oProcessWorkspaceLogger.log("Operation Completed");
				m_oProcessWorkspaceLogger.log(new EndMessageProvider().getGood());

				DownloadPayload oDownloadPayload = new DownloadPayload();
				oDownloadPayload.setFileName(Utils.getFileNameWithoutLastExtension(sFileName));
//				oDownloadPayload.setProvider(oParameter.getProvider());

				setPayload(oProcessWorkspace, oDownloadPayload);

				WasdiLog.debugLog("Share.executeOperation: operation done");
				updateProcessStatus(oProcessWorkspace, ProcessStatus.DONE, 100);

				return true;
			} else {
				throw new UnsupportedOperationException("This functionality is not yet supported.");
			}

		} catch (Exception oEx) {
			WasdiLog.errorLog("Share.executeOperation: Exception "
					+ org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));

			String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(oEx);
			oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
			m_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.SHARE.name(), oParam.getWorkspace(),
					sError, oParam.getExchange());
		}

		WasdiLog.debugLog("Share.executeOperation: return file name " + sFileName);

		return false;
	}

}
