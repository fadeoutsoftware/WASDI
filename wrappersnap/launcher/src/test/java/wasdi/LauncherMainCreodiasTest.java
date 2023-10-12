package wasdi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
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
import wasdi.shared.queryexecutors.creodias.ResponseTranslatorCREODIAS;
import wasdi.shared.utils.ZipFileUtils;
import wasdi.shared.utils.log.WasdiLog;

@Ignore
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LauncherMainCreodiasTest extends LauncherMainTest {

	private static String s_sClassName;

	private static String m_sProviderBasePath = "";

	String processObjId = "dcdd342a-9d71-4eb9-a9e9-7986e55ebe77";
	
	private static DataProviderConfig m_oCreodiasConfig;

	@BeforeClass
    public static void setUp() throws Exception {
		s_sClassName = "LauncherMainCreodiasTest";
		WasdiLog.debugLog(s_sClassName + ".setUp");
		
		m_oCreodiasConfig = WasdiConfig.Current.getDataProviderConfig("CREODIAS");

		m_sProviderBasePath = m_oCreodiasConfig.localFilesBasePath;
	}

	/**
	 * Download CREODIAS with file protocol but with missing file. should switch on download https.
	 * @throws Exception
	 */
	@Test
	public void testDownloadCreodias_0_file_with_file() throws Exception {

		changeConfigPropCreodiasDefaultProtocolToFile();

		// -operation DOWNLOAD -parameter C:/temp/wasdi/params/9208f841-075c-4c5d-b32d-139f3b384ed3
		String[] args = ("-operation DOWNLOAD -parameter " + SERIALIZATION_PATH + processObjId).split(" ");

		String parameterFilePath = args[3];

		String parameterFileName = extractFileNameFromFilePath(parameterFilePath);

		DownloadFileParameter oDownloadFileParameter = readOrCreateDownloadFileParameter(parameterFilePath, parameterFileName);

		String expectedDownloadFilePath = PathsConfig.getWorkspacePath(oDownloadFileParameter) + extractFileNameFromUrl(oDownloadFileParameter.getUrl());

		String sourceFilePath = expectedDownloadFilePath;
		String targetDirectoryPath = parseHttpsUrlToFilePath(oDownloadFileParameter.getUrl()).replace(extractFileNameFromFilePath(sourceFilePath), "");


		String explodedDirectoryPath = targetDirectoryPath + "/" + addSafeTermination(removeZipExtension(extractFileNameFromUrl(oDownloadFileParameter.getUrl())));
		if (doesFileExist(explodedDirectoryPath)) {
			deleteFile(explodedDirectoryPath);
		}
		
		if (doesFileExist(expectedDownloadFilePath)) {
			deleteFile(expectedDownloadFilePath);
		}

		LauncherMain.main(args);

		assertTrue("Expected download file does not exist", doesFileExist(expectedDownloadFilePath));
	}

	/**
	 * Download CREODIAS with https protocol.
	 * @throws Exception
	 */
	@Test
	public void testDownloadCreodias_1_https() throws Exception {

		changeConfigPropCreodiasDefaultProtocolToHttps();

		// -operation DOWNLOAD -parameter C:/temp/wasdi/params/9208f841-075c-4c5d-b32d-139f3b384ed3
		String[] args = ("-operation DOWNLOAD -parameter " + SERIALIZATION_PATH + processObjId).split(" ");

		String parameterFilePath = args[3];

		String parameterFileName = extractFileNameFromFilePath(parameterFilePath);

		DownloadFileParameter oDownloadFileParameter = readOrCreateDownloadFileParameter(parameterFilePath, parameterFileName);

		String expectedDownloadFilePath = PathsConfig.getWorkspacePath(oDownloadFileParameter) + extractFileNameFromUrl(oDownloadFileParameter.getUrl());

		if (doesFileExist(expectedDownloadFilePath)) {
			deleteFile(expectedDownloadFilePath);
		}

		LauncherMain.main(args);

		assertTrue("Expected download file does not exist", doesFileExist(expectedDownloadFilePath));

		String sourceFilePath = expectedDownloadFilePath;
		String targetDirectoryPath = parseHttpsUrlToFilePath(oDownloadFileParameter.getUrl()).replace(extractFileNameFromFilePath(sourceFilePath), "");

		File sourceFile = new File(sourceFilePath);
		File targetDirectory = new File(targetDirectoryPath);


		String explodedDirectoryPath = targetDirectoryPath + "/" + addSafeTermination(removeZipExtension(extractFileNameFromUrl(oDownloadFileParameter.getUrl())));
		if (doesFileExist(explodedDirectoryPath)) {
			deleteFile(explodedDirectoryPath);
		}

		new ZipFileUtils().unzip(sourceFile.getAbsolutePath(), targetDirectory.getAbsolutePath());
	}

	/**
	 * Download CREODIAS with file protocol. The file should have been created by the previous test (download https).
	 * @throws Exception
	 */
	@Test
	public void testDownloadCreodias_2_file_with_file() throws Exception {

		changeConfigPropCreodiasDefaultProtocolToFile();

		// -operation DOWNLOAD -parameter C:/temp/wasdi/params/9208f841-075c-4c5d-b32d-139f3b384ed3
		String[] args = ("-operation DOWNLOAD -parameter " + SERIALIZATION_PATH + processObjId).split(" ");

		String parameterFilePath = args[3];

		String parameterFileName = extractFileNameFromFilePath(parameterFilePath);

		DownloadFileParameter oDownloadFileParameter = readOrCreateDownloadFileParameter(parameterFilePath, parameterFileName);

		String expectedDownloadFilePath = PathsConfig.getWorkspacePath(oDownloadFileParameter) + extractFileNameFromUrl(oDownloadFileParameter.getUrl());

		if (doesFileExist(expectedDownloadFilePath)) {
			deleteFile(expectedDownloadFilePath);
		}

		LauncherMain.main(args);

		assertTrue("Expected download file does not exist", doesFileExist(expectedDownloadFilePath));
	}

	/**
	 * Parse the HTTPS URL to a file-system path.
	 * @param sHttpsURL (i.e. https://zipper.creodias.eu/download/c658fc32-ae33-5aa3-8c8f-921f862c748a,S1B_S1_GRDH_1SDH_20210101T152652_20210101T152706_024962_02F890_39B4.zip,102214108,0,,/eodata/Sentinel-1/SAR/GRD/2021/01/01/S1B_S1_GRDH_1SDH_20210101T152652_20210101T152706_024962_02F890_39B4.SAFE,)
	 * @return the file path (i.e. C:/temp/wasdi/eodata/Sentinel-1/SAR/GRD/2021/01/01/S1B_S1_GRDH_1SDH_20210101T152652_20210101T152706_024962_02F890_39B4.zip)
	 */
	private String parseHttpsUrlToFilePath(String sHttpsURL) {
		String filesystemPath = m_sProviderBasePath + extractFilePathFromUrl(sHttpsURL);

		WasdiLog.debugLog(s_sClassName + ".extractFilePathFromHttpsUrl: HTTPS URL: " + sHttpsURL);
		WasdiLog.debugLog(s_sClassName + ".extractFilePathFromHttpsUrl: file path: " + filesystemPath);

		return filesystemPath;
	}

	/**
	 * Change the config property CREODIAS_DEFAULT_PROTOCOL value to <b>https://</b>
	 * @throws IOException in case of any issue reading the configuration file
	 */
	private void changeConfigPropCreodiasDefaultProtocolToHttps() throws IOException {
		String CREODIAS_DEFAULT_PROTOCOL =  m_oCreodiasConfig.defaultProtocol;

		if (CREODIAS_DEFAULT_PROTOCOL.equals("file://")) {
			m_oCreodiasConfig.defaultProtocol = "https://";
			CREODIAS_DEFAULT_PROTOCOL = m_oCreodiasConfig.defaultProtocol;
		}
	}

	/**
	 * Change the config property CREODIAS_DEFAULT_PROTOCOL value to <b>file://</b>
	 * @throws IOException in case of any issue reading the configuration file
	 */
	private void changeConfigPropCreodiasDefaultProtocolToFile() throws IOException {
		String CREODIAS_DEFAULT_PROTOCOL = m_oCreodiasConfig.defaultProtocol;

		if (CREODIAS_DEFAULT_PROTOCOL.equals("https://")) {
			m_oCreodiasConfig.defaultProtocol = "file://";
			CREODIAS_DEFAULT_PROTOCOL = m_oCreodiasConfig.defaultProtocol;
		}
	}

	/**
	 * Extract the file name from the HTTP URL.
	 * @param sUrl (i.e. https://zipper.creodias.eu/download/c658fc32-ae33-5aa3-8c8f-921f862c748a,S1B_S1_GRDH_1SDH_20210101T152652_20210101T152706_024962_02F890_39B4.zip,102214108,0,,/eodata/Sentinel-1/SAR/GRD/2021/01/01/S1B_S1_GRDH_1SDH_20210101T152652_20210101T152706_024962_02F890_39B4.SAFE,)
	 * @return the file name without the path (i.e. S1B_S1_GRDH_1SDH_20210101T152652_20210101T152706_024962_02F890_39B4.zip)
	 */
	private String extractFileNameFromUrl(String sUrl) {
		if (sUrl == null) {
			return null;
		}

		try {
			String[] asParts = sUrl.split(ResponseTranslatorCREODIAS.SLINK_SEPARATOR_CREODIAS);
			String sFileName = asParts[ResponseTranslatorCREODIAS.IPOSITIONOF_FILENAME];
			return sFileName;
		} catch (Exception oE) {
			WasdiLog.debugLog(s_sClassName + ".extractFileNameFromUrl: " + oE);
		}
		return null;
	}

//	@Test
	public void testExtractFileNameFromUrl() {
		String sUrl = "https://zipper.creodias.eu/download/c658fc32-ae33-5aa3-8c8f-921f862c748a,S1B_S1_GRDH_1SDH_20210101T152652_20210101T152706_024962_02F890_39B4.zip,102214108,0,,/eodata/Sentinel-1/SAR/GRD/2021/01/01/S1B_S1_GRDH_1SDH_20210101T152652_20210101T152706_024962_02F890_39B4.SAFE,";
		String expectedFileName = "S1B_S1_GRDH_1SDH_20210101T152652_20210101T152706_024962_02F890_39B4.zip";

		String actualFileName = extractFileNameFromUrl(sUrl);

		assertEquals(expectedFileName, actualFileName);
	}

	/**
	 * Extract the file path from the HTTP URL.
	 * @param sUrl (i.e. https://zipper.creodias.eu/download/c658fc32-ae33-5aa3-8c8f-921f862c748a,S1B_S1_GRDH_1SDH_20210101T152652_20210101T152706_024962_02F890_39B4.zip,102214108,0,,/eodata/Sentinel-1/SAR/GRD/2021/01/01/S1B_S1_GRDH_1SDH_20210101T152652_20210101T152706_024962_02F890_39B4.SAFE,)
	 * @return the file name without the path (i.e. /eodata/Sentinel-1/SAR/GRD/2021/01/01/)
	 */
	private String extractFilePathFromUrl(String sUrl) {
		if (sUrl == null) {
			return null;
		}

		try {
			String[] asParts = sUrl.split(ResponseTranslatorCREODIAS.SLINK_SEPARATOR_CREODIAS);
			String sFileName = asParts[ResponseTranslatorCREODIAS.IPOSITIONOF_FILENAME];
			String sFileNameWithoutZipExtension = removeZipExtension(sFileName);

			String sFilePath = asParts[ResponseTranslatorCREODIAS.IPOSITIONOF_PRODUCTIDENTIFIER];
			String sFilePathWithoutSafeTermination = removeSafeTermination(sFilePath);

			return sFilePathWithoutSafeTermination.replace(sFileNameWithoutZipExtension, "");
		} catch (Exception oE) {
			WasdiLog.debugLog(s_sClassName + ".extractFilePathFromUrl: " + oE);
		}
		return null;
	}

//	@Test
	public void testExtractFilePathFromUrl() {
		String sUrl = "https://zipper.creodias.eu/download/c658fc32-ae33-5aa3-8c8f-921f862c748a,S1B_S1_GRDH_1SDH_20210101T152652_20210101T152706_024962_02F890_39B4.zip,102214108,0,,/eodata/Sentinel-1/SAR/GRD/2021/01/01/S1B_S1_GRDH_1SDH_20210101T152652_20210101T152706_024962_02F890_39B4.SAFE,";
		String expectedFilePath = "/eodata/Sentinel-1/SAR/GRD/2021/01/01/";

		String actualFilePath = extractFilePathFromUrl(sUrl);

		assertEquals(expectedFilePath, actualFilePath);
	}

}
