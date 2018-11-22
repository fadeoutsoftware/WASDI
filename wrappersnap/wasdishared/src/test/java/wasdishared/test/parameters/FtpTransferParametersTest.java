/**
 * Created by Cristiano Nattero on 2018-11-19
 * 
 * Fadeout software
 *
 */


package wasdishared.test.parameters;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import wasdi.shared.parameters.FtpTransferParameters;

class FtpTransferParametersTest {

	@BeforeEach
	void setUp() throws Exception {
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	@Test
	final void test_getFullLocalPath_nullPathAndFile_returnsNull() {
		FtpTransferParameters oParams = new FtpTransferParameters();
		assertNull(oParams.getFullLocalPath());
	}
	
	@Test
	final void test_getFullLocalPath_PathWithoutFinalSeparator_ReturnsCorrectPath() {
		FtpTransferParameters oParams = new FtpTransferParameters();
		oParams.setM_sLocalFileName("file.name");
		oParams.setM_sLocalPath("/local/path");
		assertEquals("local/path/file.name", oParams.getFullLocalPath());
	}
	
	@Test
	final void test_getFullLocalPath_PathWithFinalSeparator_ReturnsCorrectPath() {
		FtpTransferParameters oParams = new FtpTransferParameters();
		oParams.setM_sLocalFileName("file.name");
		oParams.setM_sLocalPath("/local/path/");
		assertEquals("local/path/file.name", oParams.getFullLocalPath());
	}
	
	@Test
	final void test_getFullLocalPath_PathWinWithoutFinalSeparator_ReturnsCorrectPath() {
		FtpTransferParameters oParams = new FtpTransferParameters();
		oParams.setM_sLocalFileName("file.name");
		oParams.setM_sLocalPath("\\local\\path");
		assertEquals("local/path/file.name", oParams.getFullLocalPath());
	}
	
	@Test
	final void test_getFullLocalPath_PathWinWithFinalSeparator_ReturnsCorrectPath() {
		FtpTransferParameters oParams = new FtpTransferParameters();
		oParams.setM_sLocalFileName("file.name");
		oParams.setM_sLocalPath("\\local\\path\\");
		assertEquals("local/path/file.name", oParams.getFullLocalPath());
	}
	
	@Test
	final void test_getServer_prefixFtpProtocolRemoved() {
		FtpTransferParameters oParams = new FtpTransferParameters();
		oParams.setM_sFtpServer("ftp://myserver.org/");
		assertEquals("myserver.org", oParams.getM_sFtpServer());
	}
	
	@Test
	final void test_getServer_suffixSlashRemoved() {
		FtpTransferParameters oParams = new FtpTransferParameters();
		oParams.setM_sFtpServer("myserver.org//////");
		assertEquals("myserver.org", oParams.getM_sFtpServer() );
	}
}