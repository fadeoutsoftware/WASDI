package wasdi;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.UserSession;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.SessionRepository;
import wasdi.shared.opensearch.PaginatedQuery;
import wasdi.shared.opensearch.QueryExecutor;
import wasdi.shared.opensearch.QueryExecutorFactory;
import wasdi.shared.parameters.DownloadFileParameter;
import wasdi.shared.utils.AuthenticationCredentials;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.search.QueryResultViewModel;

@Ignore
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LauncherMainTest {

	private static QueryExecutorFactory s_oQueryExecutorFactory;
	private static String s_sClassName;
	private static Map<String, AuthenticationCredentials> m_aoCredentials;

	private static String SERIALIZATION_PATH;
	private static String DOWNLOAD_ROOT_PATH;

	private static String m_sProviderBasePath = "";

	private static String URL_DOMAIN = "https://collgs.lu/repository/";

//	String processObjId = "9208f841-075c-4c5d-b32d-139f3b384ed3"; // LSA DOWNLOAD HTTPS & file - OK
	String processObjId = "f83ddb30-4ed0-47fa-a72d-d79d8c0a1a7b"; // LSA DOWNLOAD HTTPS & file - OK


	@BeforeClass
    public static void setUp() throws Exception {
		s_oQueryExecutorFactory = new QueryExecutorFactory();
		s_sClassName = "LauncherMainTest";
		m_aoCredentials = new HashMap<>();

		SERIALIZATION_PATH = ConfigReader.getPropValue("SERIALIZATION_PATH");
		DOWNLOAD_ROOT_PATH = ConfigReader.getPropValue("DOWNLOAD_ROOT_PATH");
		m_sProviderBasePath = ConfigReader.getPropValue("LSA_BASE_PATH", "/mount/lucollgs/data_192/");

		MongoRepository.SERVER_ADDRESS = ConfigReader.getPropValue("MONGO_ADDRESS");
		MongoRepository.SERVER_PORT = Integer.parseInt(ConfigReader.getPropValue("MONGO_PORT"));
		MongoRepository.DB_NAME = ConfigReader.getPropValue("MONGO_DBNAME");
		MongoRepository.DB_USER = ConfigReader.getPropValue("MONGO_DBUSER");
		MongoRepository.DB_PWD = ConfigReader.getPropValue("MONGO_DBPWD");
		MongoRepository.addMongoConnection("local", MongoRepository.DB_USER, MongoRepository.DB_PWD, MongoRepository.SERVER_ADDRESS, MongoRepository.SERVER_PORT+1, MongoRepository.DB_NAME);
	}

	@Test
	/**
	 * Download LSA with file protocol but with missing file. should switch on download https.
	 * @throws Exception
	 */
	public void testDownloadLsa_0_file_without_file() throws Exception {

		changeConfigPropLsaDefaultProtocolToFile();

		// -operation DOWNLOAD -parameter C:/temp/wasdi/params/9208f841-075c-4c5d-b32d-139f3b384ed3
		String[] args = ("-operation DOWNLOAD -parameter " + SERIALIZATION_PATH + processObjId).split(" ");

		String parameterFilePath = args[3];

		String parameterFileName = extractFileNameFromFilePath(parameterFilePath);

		DownloadFileParameter oDownloadFileParameter = readOrCreateDownloadFileParameter(parameterFilePath, parameterFileName);

		String expectedDownloadFilePath = buildExpectedDownloadFilePath(oDownloadFileParameter);

		if (doesFileExist(expectedDownloadFilePath)) {
			deleteFile(expectedDownloadFilePath);
		}

		LauncherMain.main(args);

		assertTrue("Expected download file does not exist", doesFileExist(expectedDownloadFilePath));

		String sourceFilePath = expectedDownloadFilePath;
		String targetDirectoryPath = parseHttpsUrlToFilePath(oDownloadFileParameter.getUrl()).replace(extractFileNameFromFilePath(sourceFilePath), "");
		copyDownloadFileToFileSystemPath(sourceFilePath, targetDirectoryPath);
	}

	@Test
	/**
	 * Download LSA with https protocol.
	 * @throws Exception
	 */
	public void testDownloadLsa_1_https() throws Exception {

		changeConfigPropLsaDefaultProtocolToHttps();

		// -operation DOWNLOAD -parameter C:/temp/wasdi/params/9208f841-075c-4c5d-b32d-139f3b384ed3
		String[] args = ("-operation DOWNLOAD -parameter " + SERIALIZATION_PATH + processObjId).split(" ");

		String parameterFilePath = args[3];

		String parameterFileName = extractFileNameFromFilePath(parameterFilePath);

		DownloadFileParameter oDownloadFileParameter = readOrCreateDownloadFileParameter(parameterFilePath, parameterFileName);

		String expectedDownloadFilePath = buildExpectedDownloadFilePath(oDownloadFileParameter);

		if (doesFileExist(expectedDownloadFilePath)) {
			deleteFile(expectedDownloadFilePath);
		}

		LauncherMain.main(args);

		assertTrue("Expected download file does not exist", doesFileExist(expectedDownloadFilePath));

		String sourceFilePath = expectedDownloadFilePath;
		String targetDirectoryPath = parseHttpsUrlToFilePath(oDownloadFileParameter.getUrl()).replace(extractFileNameFromFilePath(sourceFilePath), "");
		copyDownloadFileToFileSystemPath(sourceFilePath, targetDirectoryPath);
	}

	@Test
	/**
	 * Download LSA with file protocol. The file should have been created by the previous test (download https).
	 * @throws Exception
	 */
	public void testDownloadLsa_2_file_with_file() throws Exception {

		changeConfigPropLsaDefaultProtocolToFile();

		// -operation DOWNLOAD -parameter C:/temp/wasdi/params/9208f841-075c-4c5d-b32d-139f3b384ed3
		String[] args = ("-operation DOWNLOAD -parameter " + SERIALIZATION_PATH + processObjId).split(" ");

		String parameterFilePath = args[3];

		String parameterFileName = extractFileNameFromFilePath(parameterFilePath);

		DownloadFileParameter oDownloadFileParameter = readOrCreateDownloadFileParameter(parameterFilePath, parameterFileName);

		String expectedOutputFilePath = buildExpectedDownloadFilePath(oDownloadFileParameter);

		if (doesFileExist(expectedOutputFilePath)) {
			deleteFile(expectedOutputFilePath);
		}

		LauncherMain.main(args);

		assertTrue("Expected output file does not exist", doesFileExist(expectedOutputFilePath));
	}

	private void copyDownloadFileToFileSystemPath(String sourceFilePath, String targetDirectoryPath) throws IOException {
		Path sourceFile = Paths.get(sourceFilePath);
		Path targetDirectory = Paths.get(targetDirectoryPath);

		if (Files.notExists(targetDirectory)) {
			Files.createDirectories(targetDirectory);
		}

		String targetFileName = extractFileNameFromFilePath(sourceFilePath);

		Path targetFile = Paths.get(targetDirectoryPath + targetFileName);

		Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
	}

	private DownloadFileParameter readOrCreateDownloadFileParameter(String parameterFilePath, String parameterFileName)
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

    //TODO replace with LauncherMain's getWorspacePath
	/**
	 * Build the full path of the expected download file.
	 * @param oDownloadFileParameter the download file parameter object
	 * @return the full path of the file
	 */
	private String buildExpectedDownloadFilePath(DownloadFileParameter oDownloadFileParameter) {
		String expectedOutputFileName = DOWNLOAD_ROOT_PATH // C:/Users/PetruPetrescu/.wasdi
				+ "/"
				+ oDownloadFileParameter.getWorkspaceOwnerId() // "pbpetrescu@gmail.com"
				+ "/"
				+ oDownloadFileParameter.getWorkspace() // "94217473-e353-46e3-8ec5-c3e649a94e00"
				+ "/"
				+ extractFileNameFromUrl(oDownloadFileParameter.getUrl()); // "S1B_IW_GRDH_1SDV_20210929T153558_20210929T153623_028915_037365_9ADA.zip";
		return expectedOutputFileName;
	}

	/**
	 * Parse the HTTPS URL to a file-system path.
	 * @param sHttpsURL (i.e. https://collgs.lu/repository/data_192/Sentinel-1/B/SAR-C/L1/GRD/2021/09/29/S1B_IW_GRDH_1SDV_20210929T153558_20210929T153623_028915_037365_9ADA.zip)
	 * @return the file path (i.e. C:/temp/wasdi/mount/lucollgs/data_192/Sentinel-1/B/SAR-C/L1/GRD/2021/09/29/S1B_IW_GRDH_1SDV_20210929T153558_20210929T153623_028915_037365_9ADA.zip)
	 */
	private String parseHttpsUrlToFilePath(String sHttpsURL) {
		String filesystemPath = m_sProviderBasePath + sHttpsURL.replace(URL_DOMAIN, "");

		Utils.debugLog(s_sClassName + ".extractFilePathFromHttpsUrl: HTTPS URL: " + sHttpsURL);
		Utils.debugLog(s_sClassName + ".extractFilePathFromHttpsUrl: file path: " + filesystemPath);

		return filesystemPath;
	}

	/**
	 * Change the config property LSA_DEFAULT_PROTOCOL value to <b>https://</b>
	 * @throws IOException in case of any issue reading the configuratio file
	 */
	private void changeConfigPropLsaDefaultProtocolToHttps() throws IOException {
		String LSA_DEFAULT_PROTOCOL = ConfigReader.getPropValue("LSA_DEFAULT_PROTOCOL");

		if (LSA_DEFAULT_PROTOCOL.equals("file://")) {
			ConfigReader.m_aoProperties.put("LSA_DEFAULT_PROTOCOL", "https://");
			LSA_DEFAULT_PROTOCOL = ConfigReader.getPropValue("LSA_DEFAULT_PROTOCOL");
		}
	}

	/**
	 * Change the config property LSA_DEFAULT_PROTOCOL value to <b>file://</b>
	 * @throws IOException in case of any issue reading the configuratio file
	 */
	private void changeConfigPropLsaDefaultProtocolToFile() throws IOException {
		String LSA_DEFAULT_PROTOCOL = ConfigReader.getPropValue("LSA_DEFAULT_PROTOCOL");

		if (LSA_DEFAULT_PROTOCOL.equals("https://")) {
			ConfigReader.m_aoProperties.put("LSA_DEFAULT_PROTOCOL", "file://");
			LSA_DEFAULT_PROTOCOL = ConfigReader.getPropValue("LSA_DEFAULT_PROTOCOL");
		}
	}

	/**
	 * Extract the file name from the full file path.
	 * @param sFilePath the full file path (i.e. C:/temp/wasdi/params/9208f841-075c-4c5d-b32d-139f3b384ed3)
	 * @return the file name (i.e. 9208f841-075c-4c5d-b32d-139f3b384ed3)
	 */
	private String extractFileNameFromFilePath(String sFilePath) {
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

	/**
	 * Extract the file name from the HTTP URL.
	 * @param sUrl (i.e. https://collgs.lu/repository/data_192/Sentinel-1/B/SAR-C/L1/GRD/2021/09/29/S1B_IW_GRDH_1SDV_20210929T153558_20210929T153623_028915_037365_9ADA.zip)
	 * @return the file name without the path (i.e. S1B_IW_GRDH_1SDV_20210929T153558_20210929T153623_028915_037365_9ADA.zip)
	 */
	private String extractFileNameFromUrl(String sUrl) {
		if (sUrl == null) {
			return null;
		}

		String sFileName = sUrl.substring(sUrl.lastIndexOf("/") + 1);
		return sFileName;
	}

	private boolean doesFileExist(String filePath) {
		File file = new File(filePath);

		return file != null && file.exists();
	}

	private boolean deleteFile(String filePath) {
		assertTrue("The file should exist", doesFileExist(filePath));

		File file = new File(filePath);
		File parentDirectory = file.getParentFile();

		boolean fileDeleted = file.delete();
		boolean parentDirectoryDeleted = parentDirectory.delete();

		return fileDeleted && parentDirectoryDeleted;
	}

	private boolean writeFile(String filePath, String fileContent) {
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

		String sProductName = removeZipExtension(oProcessWorkspace.getProductName());
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

	private String removeZipExtension(String sProductName) {
		if (sProductName == null || !sProductName.endsWith(".zip")) {
			return sProductName;
		} else {
			return sProductName.replace(".zip", "");
		}
	}

	private ProcessWorkspace readProcessWorkspacefromMongoDb(String sProcessObjId) {
		ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
		ProcessWorkspace oProcessWorkspace = oProcessWorkspaceRepository.getProcessByProcessObjId(sProcessObjId);

		return oProcessWorkspace;
	}

	private QueryResultViewModel readFileInfoFromCatalog(String sProductName, String sProvider) {
//		"S1B_IW_GRDH_1SDV_20210929T153558_20210929T153623_028915_037365_9ADA AND ( beginPosition:[2021-09-28T00:00:00.000Z TO 2021-10-05T23:59:59.999Z] AND endPosition:[2021-09-28T00:00:00.000Z TO 2021-10-05T23:59:59.999Z] ) AND   (platformname:Sentinel-1)"
//		String sQuery = sProductName;
//		String sQuery = sProductName + " AND ( beginPosition:[2021-09-28T00:00:00.000Z TO 2021-10-05T23:59:59.999Z] AND endPosition:[2021-09-28T00:00:00.000Z TO 2021-10-05T23:59:59.999Z] ) AND   (platformname:Sentinel-1)";
//		String sQuery = sProductName + " AND ( beginPosition:[2021-09-28T00:00:00.000Z TO 2021-10-05T23:59:59.999Z] AND endPosition:[2021-09-28T00:00:00.000Z TO 2021-10-05T23:59:59.999Z] )";
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
		oSession.setLoginDate((double) new Date().getTime());
		oSession.setLastTouch((double) new Date().getTime());

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
				Utils.debugLog(s_sClassName + ".search: caught NumberFormatException: " + oE1);
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
				Utils.debugLog(s_sClassName + ".search: caught NumberFormatException: " + oE2);
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
				Utils.debugLog(s_sClassName + ".getQueryCountResultsPerProvider: " +oE);
			}
			
			// For each provider
			for (Entry<String, Integer> oEntry : aiCounterMap.entrySet()) {
				
				// Get the provider and the total count of its results
				String sProvider = oEntry.getKey();
				
				String sCurrentLimit = "" + iLimit;
				
				int iCurrentOffset = Math.max(0, iOffset);
				String sCurrentOffset = "" + iCurrentOffset;
				
				
				Utils.debugLog(s_sClassName + ".search, executing. " + sProvider + ": offset=" + sCurrentOffset + ": limit=" + sCurrentLimit);
				
				try {
					// Get the query executor
					QueryExecutor oExecutor = getExecutor(sProvider);
					
					if (oExecutor == null) {
						Utils.debugLog(s_sClassName + ".search: executor null for Provider: " + sProvider);
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
							Utils.debugLog(s_sClassName + ".search: found " + aoTmp.size() + " results for " + sProvider);
						} 
						else {
							// Nothing to add
							Utils.debugLog(s_sClassName + ".search: no results found for " + sProvider);
						}
					} 
					catch (NumberFormatException oNumberFormatException) {
						Utils.debugLog(s_sClassName + ".search: " + oNumberFormatException);
						aoResults.add(null);
					} 
					catch (IOException oIOException) {
						Utils.debugLog(s_sClassName + ".search: " + oIOException);
						aoResults.add(null);
					}
					
				}
				catch (Exception oE) {
					Utils.debugLog(s_sClassName + ".search: " + oE);
					aoResults.add(null);
				}
			}
			return aoResults.toArray(new QueryResultViewModel[aoResults.size()]);
		}
		return null;
	}

	private QueryExecutor getExecutor(String sProvider) {
		Utils.debugLog(s_sClassName + ".getExecutor, provider: " + sProvider);
		QueryExecutor oExecutor = null;
		try {
			if(null!=sProvider) {
				AuthenticationCredentials oCredentials = getCredentials(sProvider);
				String sDownloadProtocol = ConfigReader.getPropValue(sProvider+".downloadProtocol");
				String sGetMetadata = ConfigReader.getPropValue("getProductMetadata");
	
				String sParserConfigPath = ConfigReader.getPropValue(sProvider+".parserConfig");
				String sAppConfigPath = ConfigReader.getPropValue("MissionsConfigFilePath");
				oExecutor = s_oQueryExecutorFactory.getExecutor(
						sProvider,
						oCredentials,
						//TODO change into config method
						sDownloadProtocol, sGetMetadata,
						sParserConfigPath, sAppConfigPath);
				
				oExecutor.init();
			}
		} catch (Exception oE) {
			Utils.debugLog(s_sClassName + ".getExecutor( " + sProvider + " ): " + oE);
		}
		return oExecutor;
	}

	private AuthenticationCredentials getCredentials(String sProvider) {
		//Utils.debugLog(s_sClassName + ".getCredentials( Provider: " + sProvider + " )");
		AuthenticationCredentials oCredentials = null;
		try {
			oCredentials = m_aoCredentials.get(sProvider);
			if(null == oCredentials) {
				String sUser = ConfigReader.getPropValue(sProvider+".OSUser");
				String sPassword = ConfigReader.getPropValue(sProvider+".OSPwd");
				oCredentials = new AuthenticationCredentials(sUser, sPassword);
				m_aoCredentials.put(sProvider, oCredentials);
			}
		} catch (Exception oE) {
			Utils.debugLog(s_sClassName + ".getCredentials( " + sProvider + " ): " + oE);
		}
		return oCredentials;
	}

}
