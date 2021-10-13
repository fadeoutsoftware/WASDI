package wasdi;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipOutputStream;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import wasdi.shared.utils.WasdiFileUtils;

@Ignore
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ZipUtilsTest {

	String productName = "S1B_S1_GRDH_1SDH_20210101T152652_20210101T152706_024962_02F890_39B4";

	@Test
	public void test_1_unzip() throws Exception {
		String fileZipPath = "C:/Users/PetruPetrescu/.wasdi/pbpetrescu@gmail.com/d6f3e469-1a5f-4535-abbb-6eff5d6b3b45/" + productName + ".zip";
		String destDirPath = "C:/temp/wasdi/eodata/Sentinel-1/SAR/GRD/2021/01/01";

		File fileZip = new File(fileZipPath);
		File destDir = new File(destDirPath);

		WasdiFileUtils.unzipFile(fileZip, destDir);
	}

	@Test
	public void test_2_zip() throws Exception {
		String sourceFilePath = "C:/temp/wasdi/eodata/Sentinel-1/SAR/GRD/2021/01/01/" + productName + ".SAFE";
		String outputFilePath = "C:/Users/PetruPetrescu/.wasdi/pbpetrescu@gmail.com/d6f3e469-1a5f-4535-abbb-6eff5d6b3b45/" + productName + "_zip.zip";

		File fileToZip = new File(sourceFilePath);

		FileOutputStream fos = new FileOutputStream(outputFilePath);
		ZipOutputStream zipOut = new ZipOutputStream(fos);

		WasdiFileUtils.zipFile(fileToZip, fileToZip.getName(), zipOut);
		zipOut.close();
		fos.close();

		assertTrue("Expected zip file does not exist", doesFileExist(outputFilePath));
	}

	private boolean doesFileExist(String filePath) {
		File file = new File(filePath);

		return file != null && file.exists();
	}

}
