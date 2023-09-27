package wasdi;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import wasdi.shared.config.DataProviderConfig;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.parameters.DownloadFileParameter;
import wasdi.shared.utils.log.WasdiLog;

@Ignore
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LauncherMainLsaTest extends LauncherMainTest {

	private static String s_sClassName;

	private static String m_sProviderBasePath = "";
	private static String m_sProviderUrlDomain = "";

	String processObjId = "9208f841-075c-4c5d-b32d-139f3b384ed3";
//	String processObjId = "f83ddb30-4ed0-47fa-a72d-d79d8c0a1a7b";
	
	private static DataProviderConfig m_oLSAConfig;


	@BeforeClass
    public static void setUp() throws Exception {
		s_sClassName = "LauncherMainLsaTest";
		WasdiLog.debugLog(s_sClassName + ".setUp");
		
		m_oLSAConfig = WasdiConfig.Current.getDataProviderConfig("LSA");

		m_sProviderBasePath = m_oLSAConfig.localFilesBasePath;
		m_sProviderUrlDomain = m_oLSAConfig.urlDomain;
	}

	/**
	 * Download LSA with file protocol but with missing file. should switch on download https.
	 * @throws Exception
	 */
	@Test
	public void testDownloadLsa_0_file_without_file() throws Exception {

		changeConfigPropLsaDefaultProtocolToFile();

		// -operation DOWNLOAD -parameter C:/temp/wasdi/params/9208f841-075c-4c5d-b32d-139f3b384ed3
		String[] args = ("-operation DOWNLOAD -parameter " + SERIALIZATION_PATH + processObjId).split(" ");

		String parameterFilePath = args[3];

		String parameterFileName = extractFileNameFromFilePath(parameterFilePath);

		DownloadFileParameter oDownloadFileParameter = readOrCreateDownloadFileParameter(parameterFilePath, parameterFileName);

		String fileName = extractFileNameFromUrl(oDownloadFileParameter.getUrl());
		String expectedDownloadFilePath = PathsConfig.getWorkspacePath(oDownloadFileParameter) + fileName;

		if (doesFileExist(expectedDownloadFilePath)) {
			System.out.println("trying to delete the file: " + expectedDownloadFilePath);
			deleteFile(expectedDownloadFilePath);
		}

		LauncherMain.main(args);

		assertTrue("Expected download file does not exist", doesFileExist(expectedDownloadFilePath));

		String sourceFilePath = expectedDownloadFilePath;
		String targetDirectoryPath = parseHttpsUrlToFilePath(oDownloadFileParameter.getUrl()).replace(extractFileNameFromFilePath(sourceFilePath), "");
		copyDownloadFileToFileSystemPath(sourceFilePath, targetDirectoryPath, fileName);
	}

	/**
	 * Download LSA with https protocol.
	 * @throws Exception
	 */
	@Test
	public void testDownloadLsa_1_https() throws Exception {

		changeConfigPropLsaDefaultProtocolToHttps();

		// -operation DOWNLOAD -parameter C:/temp/wasdi/params/9208f841-075c-4c5d-b32d-139f3b384ed3
		String[] args = ("-operation DOWNLOAD -parameter " + SERIALIZATION_PATH + processObjId).split(" ");

		String parameterFilePath = args[3];

		String parameterFileName = extractFileNameFromFilePath(parameterFilePath);

		DownloadFileParameter oDownloadFileParameter = readOrCreateDownloadFileParameter(parameterFilePath, parameterFileName);

		String fileName = extractFileNameFromUrl(oDownloadFileParameter.getUrl());
		String expectedDownloadFilePath = PathsConfig.getWorkspacePath(oDownloadFileParameter) + fileName;

		if (doesFileExist(expectedDownloadFilePath)) {
			System.out.println("trying to delete the file: " + expectedDownloadFilePath);
			deleteFile(expectedDownloadFilePath);
		}

		LauncherMain.main(args);

		assertTrue("Expected download file does not exist", doesFileExist(expectedDownloadFilePath));

		String sourceFilePath = expectedDownloadFilePath;
		String targetDirectoryPath = parseHttpsUrlToFilePath(oDownloadFileParameter.getUrl()).replace(extractFileNameFromFilePath(sourceFilePath), "");
		copyDownloadFileToFileSystemPath(sourceFilePath, targetDirectoryPath, fileName);
	}

	/**
	 * Download LSA with file protocol. The file should have been created by the previous test (download https).
	 * @throws Exception
	 */
	@Test
	public void testDownloadLsa_2_file_with_file() throws Exception {

		changeConfigPropLsaDefaultProtocolToFile();

		// -operation DOWNLOAD -parameter C:/temp/wasdi/params/9208f841-075c-4c5d-b32d-139f3b384ed3
		String[] args = ("-operation DOWNLOAD -parameter " + SERIALIZATION_PATH + processObjId).split(" ");

		String parameterFilePath = args[3];

		String parameterFileName = extractFileNameFromFilePath(parameterFilePath);

		DownloadFileParameter oDownloadFileParameter = readOrCreateDownloadFileParameter(parameterFilePath, parameterFileName);

		String expectedDownloadFilePath = PathsConfig.getWorkspacePath(oDownloadFileParameter) + extractFileNameFromUrl(oDownloadFileParameter.getUrl());

		if (doesFileExist(expectedDownloadFilePath)) {
			System.out.println("trying to delete the file: " + expectedDownloadFilePath);
			deleteFile(expectedDownloadFilePath);
		}

		LauncherMain.main(args);

		assertTrue("Expected download file does not exist", doesFileExist(expectedDownloadFilePath));
	}

	/**
	 * Parse the HTTPS URL to a file-system path.
	 * @param sHttpsURL (i.e. https://collgs.lu/repository/data_192/Sentinel-1/B/SAR-C/L1/GRD/2021/09/29/S1B_IW_GRDH_1SDV_20210929T153558_20210929T153623_028915_037365_9ADA.zip)
	 * @return the file path (i.e. C:/temp/wasdi/mount/lucollgs/data_192/Sentinel-1/B/SAR-C/L1/GRD/2021/09/29/S1B_IW_GRDH_1SDV_20210929T153558_20210929T153623_028915_037365_9ADA.zip)
	 */
	private String parseHttpsUrlToFilePath(String sHttpsURL) {
		String filesystemPath = m_sProviderBasePath + sHttpsURL.replace(m_sProviderUrlDomain, "");

		WasdiLog.debugLog(s_sClassName + ".extractFilePathFromHttpsUrl: HTTPS URL: " + sHttpsURL);
		WasdiLog.debugLog(s_sClassName + ".extractFilePathFromHttpsUrl: file path: " + filesystemPath);

		return filesystemPath;
	}

	/**
	 * Change the config property LSA_DEFAULT_PROTOCOL value to <b>https://</b>
	 * @throws IOException in case of any issue reading the configuration file
	 */
	private void changeConfigPropLsaDefaultProtocolToHttps() throws IOException {
		String LSA_DEFAULT_PROTOCOL = m_oLSAConfig.defaultProtocol;

		if (LSA_DEFAULT_PROTOCOL.equals("file://")) {
			m_oLSAConfig.defaultProtocol = "https://";
			LSA_DEFAULT_PROTOCOL = m_oLSAConfig.defaultProtocol;
		}
	}

	/**
	 * Change the config property LSA_DEFAULT_PROTOCOL value to <b>file://</b>
	 * @throws IOException in case of any issue reading the configuration file
	 */
	private void changeConfigPropLsaDefaultProtocolToFile() throws IOException {
		String LSA_DEFAULT_PROTOCOL = m_oLSAConfig.defaultProtocol;

		if (LSA_DEFAULT_PROTOCOL.equals("https://")) {
			m_oLSAConfig.defaultProtocol = "file://";
			LSA_DEFAULT_PROTOCOL = m_oLSAConfig.defaultProtocol;
		}
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

}
