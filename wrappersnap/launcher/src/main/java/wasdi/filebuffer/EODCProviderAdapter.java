package wasdi.filebuffer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.esa.snap.core.datamodel.Product;

import wasdi.io.WasdiProductReader;
import wasdi.io.WasdiProductReaderFactory;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.utils.Utils;

public class EODCProviderAdapter extends ProviderAdapter{

	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		m_oLogger.debug("EODCProviderAdapter.GetDownloadSize: start " + sFileURL);

		long lLenght = 0L;

		if(sFileURL.startsWith("file:")) {

			String sPrefix = "file:";
			// Remove the prefix
			int iStart = sFileURL.indexOf(sPrefix) +sPrefix.length();
			String sPath = sFileURL.substring(iStart);

			m_oLogger.debug("EODCProviderAdapter.GetDownloadSize: full path " + sPath);
			File oSourceFile = new File(sPath);
			lLenght = oSourceFile.length();
			if (!oSourceFile.exists()) {
				m_oLogger.debug("EODCProviderAdapter.GetDownloadSize: FILE DOES NOT EXISTS");
			}
			m_oLogger.debug("EODCProviderAdapter.GetDownloadSize: Found length " + lLenght);
		} else if(sFileURL.startsWith("https:")) {
			lLenght = getDownloadFileSizeViaHttp(sFileURL);
		}

		return lLenght;
	}

	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {

		// Domain check
		if (Utils.isNullOrEmpty(sFileURL)) {
			m_oLogger.debug("EODCProviderAdapter.ExecuteDownloadFile: sFileURL is null");
			return "";
		}
		if (Utils.isNullOrEmpty(sSaveDirOnServer)) {
			m_oLogger.debug("EODCProviderAdapter.ExecuteDownloadFile: sSaveDirOnServer is null");
			return "";
		}
		
		m_oLogger.debug("EODCProviderAdapter.ExecuteDownloadFile: start");
		
		setProcessWorkspace(oProcessWorkspace);

		if(sFileURL.startsWith("file:")) {
			//  file:/mnt/OPTICAL/LEVEL-1C/2018/12/12/S2B_MSIL1C_20181212T010259_N0207_R045_T54PZA_20181212T021706.zip/.value		
			m_oLogger.debug("EODCProviderAdapter.ExecuteDownloadFile: this is a \"file:\" protocol, get file name");
			
			String sPrefix = "file:";
			// Remove the prefix
			int iStart = sFileURL.indexOf(sPrefix) +sPrefix.length();
			String sPath = sFileURL.substring(iStart);

			m_oLogger.debug("EODCProviderAdapter.ExecuteDownloadFile: source file: " + sPath);
			File oSourceFile = new File(sPath);
			
			// Destination file name: start from the simple name
			String sDestinationFileName = getFileName(sFileURL);
			// set the destination folder
			if (sSaveDirOnServer.endsWith("/") == false) sSaveDirOnServer += "/";
			sDestinationFileName = sSaveDirOnServer + sDestinationFileName;
			
			m_oLogger.debug("EODCProviderAdapter.ExecuteDownloadFile: destination file: " + sDestinationFileName);
			
			InputStream oInputStream = null;
			OutputStream oOutputStream = null;

			// copy the product from file system
			try {
				File oDestionationFile = new File(sDestinationFileName);
				
				if (oDestionationFile.getParentFile() != null) { 
					if (oDestionationFile.getParentFile().exists() == false) {
						oDestionationFile.getParentFile().mkdirs();
					}
				}
				
				oInputStream = new FileInputStream(oSourceFile);
				oOutputStream = new FileOutputStream(oDestionationFile);
				
				m_oLogger.debug("EODCProviderAdapter.ExecuteDownloadFile: start copy stream");
				
				int iAttempts = iMaxRetry;

				while(iAttempts > 0) {
					
					m_oLogger.debug("EODCProviderAdapter.ExecuteDownloadFile: Attemp #" + (iMaxRetry-iAttempts+1));
					
					if (copyStream(m_oProcessWorkspace, oSourceFile.length(), oInputStream, oOutputStream)) {
						
						String sNameOnly = oDestionationFile.getName();
						
						if (sNameOnly.startsWith("S1") || sNameOnly.startsWith("S2")) {
							
							try {
								// Product Reader will be used to test if the image has been downloaded with success.
								WasdiProductReader oReadProduct = WasdiProductReaderFactory.getProductReader(oDestionationFile);
								
								Product oProduct = oReadProduct.getSnapProduct();
								
								if (oProduct != null)  {
									// Break the retry attempt cycle
									break;							
								}
								else {
									m_oLogger.debug("EODCProviderAdapter.ExecuteDownloadFile: file not readable: " + oDestionationFile.getPath() + " try again");
									try {
										String sDestination = oDestionationFile.getPath();
										sDestination += ".attemp"+ (iMaxRetry-iAttempts+1);
										FileUtils.copyFile(oDestionationFile, new File(sDestination));										
									}
									catch (Exception oEx) {
										m_oLogger.debug("EODCProviderAdapter.ExecuteDownloadFile: Exception making copy of attempt file " + oEx.toString());
									}
								}								
							}
							catch (Exception oReadEx) {
								m_oLogger.debug("EODCProviderAdapter.ExecuteDownloadFile: exception reading file: " + oReadEx.toString() + " try again");
							}
							
							
							try {
								m_oLogger.debug("EODCProviderAdapter.ExecuteDownloadFile: delete corrupted file");
								if (oDestionationFile.delete()== false) {
									m_oLogger.debug("EODCProviderAdapter.ExecuteDownloadFile: error deleting corrupted file");
								}
							}
							catch (Exception oDeleteEx) {
								m_oLogger.debug("EODCProviderAdapter.ExecuteDownloadFile: exception deleting not valid file ");
							}
						}
						else {
							// Break the retry attempt cycle
							break;							
						}						
					}
					else {
						m_oLogger.debug("EODCProviderAdapter.ExecuteDownloadFile: error in the copy stream.");
					}
					
					iAttempts--;
					
					TimeUnit.SECONDS.sleep(2);
				}

			} catch (Exception e) {
				m_oLogger.info("EODCProviderAdapter.ExecuteDownloadFile: " + e);
			}
			finally {
				try {
					if (oOutputStream != null) {
						oOutputStream.close();
					}
				} catch (IOException e) {
					
				}
				try {
					if (oInputStream!= null) {
						oInputStream.close();
					}
				} catch (IOException e) {
					
				}
			}
			
			return sDestinationFileName;
		} 
		
		return "";
	}

	@Override
	public String getFileName(String sFileURL) throws Exception {
		
		//extract file name

		if (Utils.isNullOrEmpty(sFileURL)) {
			m_oLogger.error("EODCProviderAdapter.GetFileName: sFileURL is null or Empty");
			return "";
		}

		if(sFileURL.startsWith("file:")) {
			
			// In Onda, the real file is .value but here we need the name of Satellite image that, in ONDA is the parent folder name
			String sPrefix = "file:";
			//String sSuffix = "";
			// Remove the prefix
			int iStart = sFileURL.indexOf(sPrefix) +sPrefix.length();
			String sPath = sFileURL.substring(iStart);

			// remove the ".value" suffix
			//sPath = sPath.substring(0, sPath.lastIndexOf(sSuffix));

			// Destination file name: start from the simple name, i.e., exclude the containing dir, slash included:
			String sDestinationFileName = sPath.substring( sPath.lastIndexOf("/") + 1);
			return sDestinationFileName;

		} 		
		
		return "";
	}

}
