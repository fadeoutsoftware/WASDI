package wasdi.dataproviders;

import org.json.JSONObject;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class SinaProviderAdapter extends ProviderAdapter {
	
	private String m_sBINGBANGDataFilePath = null;
	
	public SinaProviderAdapter() {
	}

	@Override
	protected void internalReadConfig() {
		if (m_oDataProviderConfig != null && !Utils.isNullOrEmpty(m_oDataProviderConfig.adapterConfig)) {
			JSONObject oAppConf = JsonUtils.loadJsonFromFile(m_oDataProviderConfig.adapterConfig);
			m_sBINGBANGDataFilePath = oAppConf.getString("dataFilePath");
		}	
	}

	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		long lFileSize = 0L;
		
		String sFileName = sFileURL;
		if (sFileName.startsWith("https://")) {
			sFileName = sFileName.replace("https://", "");
		}
		
		String sDataFilePath = getPathToZip(sFileName);
		
		if (Utils.isNullOrEmpty(sDataFilePath)) {
			WasdiLog.debugLog("SinaProviderAdapter.getDownloadFileSize: path to zip file not found " + sDataFilePath);
			return lFileSize;
		}
		
		try (ZipFile oZipFile = new ZipFile(sDataFilePath)) {
            Enumeration<? extends ZipEntry> aoEntries = oZipFile.entries();

            long lAscSize = 0L;
            long lPrjSize = 0L;
            
            boolean bAscRetrieved = false;
            boolean bPrjRetrieved = false;
            
            while (aoEntries.hasMoreElements()) {
                ZipEntry oEntry = aoEntries.nextElement();
                if (oEntry.getName().equals(sFileName)) {
                	lAscSize = oEntry.getSize();
                	bAscRetrieved = true;
                }
                if (oEntry.getName().equals(sFileName.replace(".asc", ".prj"))) {
                	lPrjSize = oEntry.getSize();
                	bPrjRetrieved = true;
                }
                
                if (bAscRetrieved && bPrjRetrieved)
                	break;
            }
            
            lFileSize = bAscRetrieved && bPrjRetrieved 
            		? lAscSize + lPrjSize
            		: 0L;
            
		} catch (IOException oEx) {
            WasdiLog.errorLog("SinaProviderAdapter.getDownloadFileSize: error computing the file size", oEx);
        }
		
		return lFileSize;
	}

	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {
		
		WasdiLog.debugLog("SinaProviderAdapter.executeDownloadFile. Url: " + sFileURL);
		
		String sFileNameUrl = sFileURL;
		if (sFileNameUrl.startsWith("https://")) {
			sFileNameUrl = sFileNameUrl.replace("https://", "");
		}
		
		String sZipFilePath = getPathToZip(sFileNameUrl);
		
		if (Utils.isNullOrEmpty(sZipFilePath)) {
			WasdiLog.warnLog("SinaProviderAdapter.executeDownloadFile. Path to zip file not found " + sZipFilePath);
			return null;
		}
		
		String[] asFileNames = new String[]{sFileNameUrl, sFileNameUrl.replace(".asc", ".prj")};
		
//		boolean bAscFileCopies = false;
//		boolean bPrjFileCopied = false;
		
		String sAscFilePath = null;
		
		try (ZipFile oZipFile = new ZipFile(sZipFilePath)) {
			
			for (String sFileName : asFileNames) {
				WasdiLog.debugLog("SinaProviderAdapter.executeDownloadFile: look for file in the zip archive " + sFileName);
				
				ZipEntry oEntry = oZipFile.getEntry(sFileName);
				
				if (oEntry != null) {
					WasdiLog.debugLog("SinaProviderAdapter.executeDownloadFile: file found " + oEntry.getName());
					
					// create the new file
					String sTargetFilePath = sSaveDirOnServer;
					if (!sTargetFilePath.endsWith("/"))
						sTargetFilePath += "/";
					
					sTargetFilePath += sFileName;
					
					File oTargetFile = new File(sTargetFilePath);
					
					oTargetFile.getParentFile().mkdir();
					
					if (oTargetFile.exists()) {
						WasdiLog.debugLog("SinaProviderAdapter.executeDownloadFile: file aready exists in workspace. It won't be copied again" + sTargetFilePath);
						return oTargetFile.getAbsolutePath();
					}
					
					WasdiLog.debugLog("SinaProviderAdapter.executeDownloadFile: try to copy entry to " + sTargetFilePath);
					
					try (InputStream oIn = oZipFile.getInputStream(oEntry)) {
						OutputStream oOut = null;
						
						try {
							oOut = new FileOutputStream(oTargetFile);
							
							byte[] ayBuffer = new byte[1024];
							int iLen;
							while ((iLen = oIn.read(ayBuffer)) > 0) {
								oOut.write(ayBuffer, 0, iLen);
							}
							
							
							if (oTargetFile.getName().endsWith(".asc")) {
								sAscFilePath = oTargetFile.getAbsolutePath();
							}
							WasdiLog.debugLog("SinaProviderAdapter.executeDownloadFile: file extracted at " + oTargetFile.getAbsolutePath());
						} catch (Exception oEx) {
							WasdiLog.errorLog("SinaProviderAdapter.executeDownloadFile: error copying the file ", oEx);
						} finally {
							if (oOut != null) oOut.close();
						} 
					}
					
				} else {
					WasdiLog.debugLog("SinaProviderAdapter.executeDownloadFile: file not found in the zip archive" + sFileName);
				}
				
			}
				
		} catch (IOException oEx) {
            WasdiLog.errorLog("SinaProviderAdapter.executeDownloadFile: error copying the files", oEx);
        }
		
		return sAscFilePath;
	}

	@Override
	public String getFileName(String sFileURL) throws Exception {
		if (sFileURL.startsWith("https://")) {
			return sFileURL.replace("https://", "");
		}
		return sFileURL;
	}

	@Override
	protected int internalGetScoreForFile(String sFileName, String sPlatformType) {
		
		if (sPlatformType.equals(Platforms.BIGBANG)) {
			return DataProviderScores.FILE_ACCESS.getValue();
		}
		
		return 0;
	}
	
	private String getPathToZip(String sFileURL) {
		
		if (Utils.isNullOrEmpty(m_sBINGBANGDataFilePath)) {
			WasdiLog.warnLog("SinaProviderAdapter.getPathToZip. Path to data folder not specified");
			return null;
		}
		
		WasdiLog.debugLog("SinaProviderAdapter.getPathToZip: path to folder with data " + m_sBINGBANGDataFilePath);
		
		String sDataFilePath = m_sBINGBANGDataFilePath;
		
		if (!sDataFilePath.endsWith("/"))
			sDataFilePath += "/";
		
		if (sFileURL.startsWith("SPEI01")) {
			sDataFilePath += "SPEI01_1952-2022_ASCII.zip";
		}
		
		if (!Files.exists(Paths.get(sDataFilePath))) {
			WasdiLog.warnLog("SinaProviderAdapter.getPathToZip. Path does not exist " + sDataFilePath);
			return null;
		}
		
		WasdiLog.debugLog("SinaProviderAdapter.getPathToZip. Path to zip file " + sDataFilePath);
		return sDataFilePath;
	}

}
