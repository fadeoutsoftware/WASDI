/**
 * 
 */
package wasdi.shared.opensearch.creodias;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author c.nattero
 *
 */
class TestDiasQueryTranslatorCREODIAS {

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterEach
	void tearDown() throws Exception {
	}

	/**
	 * Method to generate input
	 */
	private static Stream<Arguments> provideValuePairs() {
		String sProd = "S1B_IW_GRDH_1SDV_20210201T011057_20210201T011119_025406_0306AA_FA59";
		String sTW = "( beginPosition:[2021-01-29T00:00:00.000Z TO 2021-02-05T23:59:59.999Z] AND endPosition:[2021-01-29T00:00:00.000Z TO 2021-02-05T23:59:59.999Z] )";
		return Stream.of(
				//blank is blank
//				Arguments.of("", ""),
//				//with product name
				Arguments.of(sProd + " AND " + sTW + " AND   (platformname:Sentinel-1 AND filename:S1B_* AND producttype:GRD)",
						"Sentinel1/search.json?&productType=GRD&startDate=2021-01-29T00:00:00.000Z&completionDate=2021-02-05T23:59:59.999Z&status=all&timeliness=Fast-24h&productIdentifier=%25S1B_IW_GRDH_1SDV_20210201T011057_20210201T011119_025406_0306AA_FA59%25")

				//TODO implement these
				
//				Arguments.of(sProd + " AND " + sTW + " AND   (platformname:Sentinel-1 AND filename:S1B_*)",
//						"( " + sProd + " AND ( name:S1* AND name:S1B_* AND name:* AND name:* ) ) AND ( " + sTW + " )"),
//				Arguments.of(sProd + " AND " + sTW + " AND   (platformname:Sentinel-1)",
//						"( " + sProd + " AND ( name:S1* AND name:* AND name:* AND name:* ) ) AND ( " + sTW + " )"),
//				Arguments.of(sProd + " AND " + sTW,
//						"( " + sProd + " ) AND ( " + sTW + " )"),
//				//without product name
//				Arguments.of( sTW + " AND   (platformname:Sentinel-1 AND filename:S1B_* AND producttype:GRD)",
//						"( ( name:S1* AND name:S1B_* AND name:*GRD* AND name:* ) ) AND ( " + sTW + " )"),
//				Arguments.of(sTW + " AND   (platformname:Sentinel-1 AND filename:S1B_*)",
//						"( ( name:S1* AND name:S1B_* AND name:* AND name:* ) ) AND ( " + sTW + " )"),
//				Arguments.of(sTW + " AND   (platformname:Sentinel-1)",
//						"( ( name:S1* AND name:* AND name:* AND name:* ) ) AND ( " + sTW + " )"),
//				Arguments.of(sTW, "( " + sTW + " )")
				);
	}

	/**
	 * Test method for {@link wasdi.shared.opensearch.creodias.DiasQueryTranslatorCREODIAS#translate(java.lang.String)}.
	 * @throws FileNotFoundException 
	 */
	@ParameterizedTest
	@MethodSource("provideValuePairs")
	void testQueryTranslation(String sInput, String sExpected) throws FileNotFoundException {
		Path oPath = Paths.get(System.getProperty("user.dir"));
		Path oAppConfigPath = oPath.resolve(
				".." + File.separator +
				".." + File.separator +
				"client" + File.separator +
				"app" + File.separator +
				"config" + File.separator +
				"appconfig.json"
		).normalize();

		if(!oAppConfigPath.toFile().exists()) {
			throw new FileNotFoundException(oAppConfigPath.toString());
		}
		
		Path oParserConfigPath = oPath.resolve(
				".." + File.separator +
				".." + File.separator +
				"wasdiwebserver" + File.separator +
				"WebContent" + File.separator +
				"WEB-INF" + File.separator +
				"providersConf" + File.separator + 
				"creodiasParserConfig.json"
		).normalize();
		
		if(!oParserConfigPath.toFile().exists()) {
			throw new FileNotFoundException(oParserConfigPath.toString());
		}
		
		DiasQueryTranslatorCREODIAS oDiasQueryTranslator = new DiasQueryTranslatorCREODIAS();
		oDiasQueryTranslator.setAppconfigPath(oAppConfigPath.toString());
		oDiasQueryTranslator.setParserConfigPath(oParserConfigPath.toString());
		
		
		assertEquals(sExpected, oDiasQueryTranslator.translate(sInput));
	}

}
