package wasdi;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.junit.BeforeClass;

import wasdi.shared.business.AuthenticationCredentials;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.UserSession;
import wasdi.shared.config.DataProviderConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.SessionRepository;
import wasdi.shared.parameters.DownloadFileParameter;
import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.queryexecutors.QueryExecutorFactory;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryResultViewModel;

public abstract class LauncherMainTest {

	private static String s_sClassName;
	private static Map<String, AuthenticationCredentials> m_aoCredentials;

	protected static String SERIALIZATION_PATH;
	protected static String DOWNLOAD_ROOT_PATH;

	protected final int BUFFER_SIZE = 4096;

	@BeforeClass
    public static void setUpParent() throws Exception {
		s_sClassName = "LauncherMainTest";
		WasdiLog.debugLog(s_sClassName + ".setUp");
		
		m_aoCredentials = new HashMap<>();

        String sConfigFilePath = "C:\\temp\\data\\wasdi\\config.json";
        WasdiConfig.readConfig(sConfigFilePath);

		SERIALIZATION_PATH = WasdiConfig.Current.paths.serializationPath;
		DOWNLOAD_ROOT_PATH = WasdiConfig.Current.paths.downloadRootPath;

		MongoRepository.SERVER_ADDRESS = WasdiConfig.Current.mongoLocal.address;
		MongoRepository.DB_NAME = WasdiConfig.Current.mongoLocal.dbName;
		MongoRepository.DB_USER = WasdiConfig.Current.mongoLocal.user;
		MongoRepository.DB_PWD = WasdiConfig.Current.mongoLocal.password;
		MongoRepository.REPLICA_NAME = WasdiConfig.Current.mongoLocal.replicaName;
		MongoRepository.addMongoConnection("local", MongoRepository.DB_USER, MongoRepository.DB_PWD, MongoRepository.SERVER_ADDRESS, MongoRepository.REPLICA_NAME, MongoRepository.DB_NAME);
	}

	protected void copyDownloadFileToFileSystemPath(String sourceFilePath, String targetDirectoryPath, String targetFileName) {
		String sSaveFilePath = targetDirectoryPath + targetFileName;

		File oTargetDir = new File(targetDirectoryPath);
		oTargetDir.mkdirs();

		FileInputStream oInputStream = null;
		FileOutputStream oOutputStream = null;

		int iBytesRead = -1;
		byte[] abBuffer = new byte[BUFFER_SIZE];
		

		try {
			oInputStream = new FileInputStream(sourceFilePath);
			oOutputStream = new FileOutputStream(sSaveFilePath);

			while ((iBytesRead = oInputStream.read(abBuffer)) != -1) {
				oOutputStream.write(abBuffer, 0, iBytesRead);
			}
			
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
		}
		finally {
			if (oOutputStream != null) {
				try {
					oOutputStream.close();
					oOutputStream = null;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (oInputStream != null) {
				try {
					oInputStream.close();
					oInputStream = null;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	protected DownloadFileParameter readOrCreateDownloadFileParameter(String parameterFilePath, String parameterFileName)
			throws Exception {
		DownloadFileParameter oDownloadFileParameter;
		if (doesFileExist(parameterFilePath)) {
			oDownloadFileParameter = (DownloadFileParameter) SerializationUtils.deserializeXMLToObject(parameterFilePath);
		} else {
			oDownloadFileParameter = createDownloadFileParameter(parameterFileName);
			createParameterFileOnLocalFilesystem(oDownloadFileParameter);
		}
		return oDownloadFileParameter;
	}

	/**
	 * Extract the file name from the full file path.
	 * @param sFilePath the full file path (i.e. C:/temp/wasdi/params/9208f841-075c-4c5d-b32d-139f3b384ed3)
	 * @return the file name (i.e. 9208f841-075c-4c5d-b32d-139f3b384ed3)
	 */
	protected String extractFileNameFromFilePath(String sFilePath) {
		if (sFilePath == null) {
			return null;
		}

		String sFileName;
		if (sFilePath.contains("/")) {
			sFileName = sFilePath.substring(sFilePath.lastIndexOf("/") + 1);
		} else if (sFilePath.contains("\\")) {
			sFileName = sFilePath.substring(sFilePath.lastIndexOf("\\") + 1);
		} else {
			sFileName = sFilePath;
		}

		return sFileName;
	}

	protected boolean doesFileExist(String filePath) {
		File file = new File(filePath);

		return file != null && file.exists();
	}

	protected boolean deleteFile(String filePath) {
		assertTrue("The file should exist", doesFileExist(filePath));

		File file = new File(filePath);
		File parentDirectory = file.getParentFile();

		if (file.isDirectory()) {
			for (File child : file.listFiles()) {
				deleteFile(child.getPath());
			}
		}

		boolean fileDeleted = file.delete();
		boolean parentDirectoryDeleted = parentDirectory.delete();

		return fileDeleted && parentDirectoryDeleted;
	}

	protected boolean writeFile(String filePath, String fileContent) {
		boolean fileCreated = false;

		if (!doesFileExist(filePath)) {
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
				writer.write(fileContent);

				writer.close();

				fileCreated = true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return fileCreated;
	}

//	@Test
	public void testFindFile() throws Exception {
		String sQuery = "S1B_IW_GRDH_1SDV_20210929T153558_20210929T153623_028915_037365_9ADA AND ( beginPosition:[2021-09-27T00:00:00.000Z TO 2021-10-04T23:59:59.999Z] AND endPosition:[2021-09-27T00:00:00.000Z TO 2021-10-04T23:59:59.999Z] ) AND   (platformname:Sentinel-1)";
		String sProviders = "LSA";
		
		QueryResultViewModel[] files = searchFile(sProviders, sQuery);

		if (files == null || files.length == 0) {
			fail("File not found");
		}
	}

	private DownloadFileParameter createDownloadFileParameter(String sProcessObjId) {
		ProcessWorkspace oProcessWorkspace = readProcessWorkspacefromMongoDb(sProcessObjId);

		if (oProcessWorkspace == null) {
			fail("Error reading the ProcessWorkspace from MongoDB");
			return null;
		}

		String sProductName = removeSafeTermination(removeZipExtension(oProcessWorkspace.getProductName()));
		String sProvider = oProcessWorkspace.getOperationSubType();

		QueryResultViewModel oFileInfoFromCatalog = readFileInfoFromCatalog(sProductName, sProvider);

		if (oFileInfoFromCatalog == null) {
			fail("Error reading the the file info from catalog");
			return null;
		}

		DownloadFileParameter oParameter = new DownloadFileParameter();
		oParameter.setUserId(oProcessWorkspace.getUserId());
		oParameter.setWorkspace(oProcessWorkspace.getWorkspaceId());
		oParameter.setWorkspaceOwnerId(oProcessWorkspace.getUserId());
		oParameter.setExchange(oProcessWorkspace.getWorkspaceId());
		oParameter.setProcessObjId(oProcessWorkspace.getProcessObjId());

		String oSessionId = createSession(sProcessObjId);
		oParameter.setSessionID(oSessionId);
		oParameter.setQueue(oSessionId);

		oParameter.setUrl(oFileInfoFromCatalog.getLink());
		oParameter.setBoundingBox(Utils.polygonToBounds(oFileInfoFromCatalog.getFootprint()));
		oParameter.setProvider(oFileInfoFromCatalog.getProvider());

		AuthenticationCredentials oCredentials = getCredentials(oFileInfoFromCatalog.getProvider());
		oParameter.setDownloadUser(oCredentials.getUser());
		oParameter.setDownloadPassword(oCredentials.getPassword());

		return oParameter;
	}

	protected String removeZipExtension(String sProductName) {
		if (sProductName == null || !sProductName.endsWith(".zip")) {
			return sProductName;
		} else {
			return sProductName.replace(".zip", "");
		}
	}

	protected String removeSafeTermination(String sProductName) {
		if (sProductName == null || !sProductName.endsWith(".SAFE")) {
			return sProductName;
		} else {
			return sProductName.replace(".SAFE", "");
		}
	}

	protected String addSafeTermination(String sProductName) {
		if (sProductName == null || sProductName.endsWith(".SAFE")) {
			return sProductName;
		} else {
			return sProductName.concat(".SAFE");
		}
	}

	private ProcessWorkspace readProcessWorkspacefromMongoDb(String sProcessObjId) {
		ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
		ProcessWorkspace oProcessWorkspace = oProcessWorkspaceRepository.getProcessByProcessObjId(sProcessObjId);

		return oProcessWorkspace;
	}

	private QueryResultViewModel readFileInfoFromCatalog(String sProductName, String sProvider) {
		String sQuery = sProductName + " AND ( beginPosition:[1970-01-01T00:00:00.000Z TO 2099-12-31T23:59:59.999Z] AND endPosition:[1970-01-01T00:00:00.000Z TO 2099-12-31T23:59:59.999Z] )";
		String sProviders = sProvider;

		QueryResultViewModel[] files = searchFile(sProviders, sQuery);

		if (files == null || files.length == 0) {
			fail("File not found");
			return null;
		}

		return files[0];
	}

	private String createParameterFileOnLocalFilesystem(DownloadFileParameter oParameter) {
		String path = null;

		String sPayload = SerializationUtils.serializeObjectToStringXML(oParameter);
		String sFilePath = SERIALIZATION_PATH + oParameter.getProcessObjId();
		boolean fileWritten = writeFile(sFilePath, sPayload);

		if (fileWritten) {
			path = sFilePath;
		}

		return path;
	}

	private String createSession(String sUserId) {
		UserSession oSession = new UserSession();

		oSession.setUserId(sUserId);
		String sSessionId = UUID.randomUUID().toString();
		oSession.setSessionId(sSessionId);
		oSession.setLoginDate(Utils.nowInMillis());
		oSession.setLastTouch(Utils.nowInMillis());

		SessionRepository oSessionRepo = new SessionRepository();
		oSessionRepo.insertSession(oSession);

		return oSession.getSessionId();
	}
	
	private QueryResultViewModel[] searchFile(String sProviders, String sQuery) {
		String sOffset = null;
		String sLimit = null;
		String sSortedBy = null;
		String sOrder = null;

		// If we have providers to query
		if (sProviders != null) {
			
			// Control and check input parameters for pagination
			
			if (sOffset == null) {
				sOffset = "0";
			}
				
			if (sLimit == null) {
				sLimit = "25";
			}
				
			if (sSortedBy == null) {
				sSortedBy = "ingestiondate";
			}
				
			if (sOrder == null) {
				sOrder = "asc";
			}
			
			// Get the number of elements per page
			ArrayList<QueryResultViewModel> aoResults = new ArrayList<>();
			int iLimit = 25;
			
			try {
				iLimit = Integer.parseInt(sLimit);
			} 
			catch (NumberFormatException oE1) {
				WasdiLog.debugLog(s_sClassName + ".search: caught NumberFormatException: " + oE1);
				return null;
			}
			
			if (iLimit < 0) {
				// Not possible: back to default:
				iLimit = 25;
			}			
			
			int iOffset = 0;
			
			try {
				iOffset = Integer.parseInt(sOffset);
			} 
			catch (NumberFormatException oE2) {
				WasdiLog.debugLog(s_sClassName + ".search: caught NumberFormatException: " + oE2);
				return null;
			}
			
			// Query the result count for each provider
			Map<String, Integer> aiCounterMap = new HashMap<>();
			

			try {
				String asProviders[] = sProviders.split(",|;");
				for (String sProvider : asProviders) {
					Integer iProviderCountResults = iLimit;
					aiCounterMap.put(sProvider, iProviderCountResults);
				}
			} catch (Exception oE) {
				WasdiLog.debugLog(s_sClassName + ".getQueryCountResultsPerProvider: " +oE);
			}
			
			// For each provider
			for (Entry<String, Integer> oEntry : aiCounterMap.entrySet()) {
				
				// Get the provider and the total count of its results
				String sProvider = oEntry.getKey();
				
				String sCurrentLimit = "" + iLimit;
				
				int iCurrentOffset = Math.max(0, iOffset);
				String sCurrentOffset = "" + iCurrentOffset;
				
				
				WasdiLog.debugLog(s_sClassName + ".search, executing. " + sProvider + ": offset=" + sCurrentOffset + ": limit=" + sCurrentLimit);
				
				try {
					// Get the query executor
					QueryExecutor oExecutor = QueryExecutorFactory.getExecutor(sProviders);
					
					if (oExecutor == null) {
						WasdiLog.debugLog(s_sClassName + ".search: executor null for Provider: " + sProvider);
						aoResults.add(null);
						continue;
					}
					
					try {
						// Create the paginated query
						PaginatedQuery oQuery = new PaginatedQuery(sQuery, sCurrentOffset, sCurrentLimit, sSortedBy, sOrder);
						// Execute the query
						List<QueryResultViewModel> aoTmp = oExecutor.executeAndRetrieve(oQuery);
						
						// Do we have results?
						if (aoTmp != null && !aoTmp.isEmpty()) {
							// Yes perfect add all
							aoResults.addAll(aoTmp);
							WasdiLog.debugLog(s_sClassName + ".search: found " + aoTmp.size() + " results for " + sProvider);
						} 
						else {
							// Nothing to add
							WasdiLog.debugLog(s_sClassName + ".search: no results found for " + sProvider);
						}
					} 
					catch (NumberFormatException oNumberFormatException) {
						WasdiLog.debugLog(s_sClassName + ".search: " + oNumberFormatException);
						aoResults.add(null);
					} 
				}
				catch (Exception oE) {
					WasdiLog.debugLog(s_sClassName + ".search: " + oE);
					aoResults.add(null);
				}
			}
			return aoResults.toArray(new QueryResultViewModel[aoResults.size()]);
		}
		return null;
	}


	private AuthenticationCredentials getCredentials(String sProvider) {
		//WasdiLog.debugLog(s_sClassName + ".getCredentials( Provider: " + sProvider + " )");
		AuthenticationCredentials oCredentials = null;
		try {
			oCredentials = m_aoCredentials.get(sProvider);
			if(null == oCredentials) {
				
				DataProviderConfig oConfig = WasdiConfig.Current.getDataProviderConfig(sProvider);
				
				String sUser = oConfig.user;
				String sPassword = oConfig.password;
				String sApiKey = oConfig.apiKey;
				oCredentials = new AuthenticationCredentials(sUser, sPassword, sApiKey);
				m_aoCredentials.put(sProvider, oCredentials);
			}
		} catch (Exception oE) {
			WasdiLog.debugLog(s_sClassName + ".getCredentials( " + sProvider + " ): " + oE);
		}
		return oCredentials;
	}

}
