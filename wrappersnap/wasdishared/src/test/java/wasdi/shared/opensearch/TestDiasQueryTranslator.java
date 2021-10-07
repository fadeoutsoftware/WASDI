/**
 * 
 */
package wasdi.shared.opensearch;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
class DiasQueryTranslatorTest {


	DiasQueryTranslator m_oDiasQueryTranslator;

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
		m_oDiasQueryTranslator = new DiasQueryTranslator() {

			@Override
			protected String translate(String sQueryFromClient) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			protected String parseTimeFrame(String sQuery) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			protected String parseFootPrint(String sQuery) {
				// TODO Auto-generated method stub
				return null;
			}
		};
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterEach
	void tearDown() throws Exception {
	}


	private static Stream<Arguments> provideValuePairs() {
		return Stream.of(
				//null is blank
				Arguments.of(null, ""),
				//blank is blank
				Arguments.of("", ""),
				//no product, with footprint is blank
				Arguments.of("( footprint:\"intersects(POLYGON((4.044633438863197 40.979898069620155,4.044633438863197 47.87214396888731,16.17361580783708 47.87214396888731,16.17361580783708 40.979898069620155,4.044633438863197 40.979898069620155)))\" ) AND ( beginPosition:[2021-01-25T00:00:00.000Z TO 2021-02-01T23:59:59.999Z] AND endPosition:[2021-01-25T00:00:00.000Z TO 2021-02-01T23:59:59.999Z] ) AND   (platformname:Sentinel-1)&offset=0&limit=10", ""),
				//no product, without footprint is blank
				Arguments.of("( beginPosition:[2021-01-25T00:00:00.000Z TO 2021-02-01T23:59:59.999Z] AND endPosition:[2021-01-25T00:00:00.000Z TO 2021-02-01T23:59:59.999Z] ) AND   (platformname:Sentinel-1)&offset=0&limit=10", ""),
				//product with footprint is product
				Arguments.of("S1A_IW_GRDH_1SDH_20210118T053451_20210118T053516_036188_043E56_9648 AND ( footprint:\"intersects(POLYGON((5.274086717624901 45.644768217751924,5.274086717624901 49.15296965617042,16.172592614384037 49.15296965617042,16.172592614384037 45.644768217751924,5.274086717624901 45.644768217751924)))\" ) AND ( beginPosition:[2021-01-18T00:00:00.000Z TO 2021-02-01T23:59:59.999Z] AND endPosition:[2021-01-18T00:00:00.000Z TO 2021-02-01T23:59:59.999Z] ) AND   (platformname:Sentinel-1 AND producttype:GRD)&offset=0&limit=10", "S1A_IW_GRDH_1SDH_20210118T053451_20210118T053516_036188_043E56_9648"),
				Arguments.of("S1A_IW_GRDH_1SDH_20210118T053451_20210118T053516_036188_043E56_9648 AND ( footprint:\"intersects(POLYGON((5.274086717624901 45.644768217751924,5.274086717624901 49.15296965617042,16.172592614384037 49.15296965617042,16.172592614384037 45.644768217751924,5.274086717624901 45.644768217751924)))\" ) AND ( beginPosition:[2021-01-18T00:00:00.000Z TO 2021-02-01T23:59:59.999Z] AND endPosition:[2021-01-18T00:00:00.000Z TO 2021-02-01T23:59:59.999Z] )&offset=0&limit=10", "S1A_IW_GRDH_1SDH_20210118T053451_20210118T053516_036188_043E56_9648"),
				//product without footprint is product
				Arguments.of("S1A_EW_GRDM_1SSH_20210118T011721_20210118T011826_036185_043E38_736F AND ( beginPosition:[2021-01-18T00:00:00.000Z TO 2021-02-01T23:59:59.999Z] AND endPosition:[2021-01-18T00:00:00.000Z TO 2021-02-01T23:59:59.999Z] ) AND   (platformname:Sentinel-1 AND producttype:GRD)&offset=0&limit=10", "S1A_EW_GRDM_1SSH_20210118T011721_20210118T011826_036185_043E38_736F"),
				Arguments.of("S1A_EW_GRDM_1SSH_20210118T011721_20210118T011826_036185_043E38_736F AND ( beginPosition:[2021-01-18T00:00:00.000Z TO 2021-02-01T23:59:59.999Z] AND endPosition:[2021-01-18T00:00:00.000Z TO 2021-02-01T23:59:59.999Z] )&offset=0&limit=10", "S1A_EW_GRDM_1SSH_20210118T011721_20210118T011826_036185_043E38_736F"),
				//just product is product
				Arguments.of("S1A_EW_GRDM_1SSH_20210118T011721_20210118T011826_036185_043E38_736F", "S1A_EW_GRDM_1SSH_20210118T011721_20210118T011826_036185_043E38_736F")
				);
	}

	/**
	 * Test method for {@link wasdi.shared.opensearch.DiasQueryTranslator#getProductName(java.lang.String)}.
	 */
	@ParameterizedTest
	@MethodSource("provideValuePairs")
	void testGetFreeTextSearch(String sInput, String sExpected) {
		assertEquals(sExpected, m_oDiasQueryTranslator.getProductName(sInput));
	}

}
