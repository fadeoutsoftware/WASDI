/**
 * 
 */
package wasdi.shared.opensearch.onda;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author c.nattero
 *
 */
class TestDiasQueryTranslatorOnda {
	
	private static Stream<Arguments> provideValuePairs() {
		String sProd = "S1B_IW_GRDH_1SDV_20210201T011057_20210201T011119_025406_0306AA_FA59";
		String sTW = "( beginPosition:[2021-01-29T00:00:00.000Z TO 2021-02-05T23:59:59.999Z] AND endPosition:[2021-01-29T00:00:00.000Z TO 2021-02-05T23:59:59.999Z] )";
		return Stream.of(
				//null is blank
				Arguments.of(null, ""),
				//blank is blank
				Arguments.of("", ""),
				//with product name
				Arguments.of(sProd + " AND " + sTW + " AND   (platformname:Sentinel-1 AND filename:S1B_* AND producttype:GRD)",
						"( "+ sProd + " AND ( name:S1* AND name:S1B_* AND name:*GRD* AND name:* ) ) AND ( " + sTW + " )"),
				Arguments.of(sProd + " AND " + sTW + " AND   (platformname:Sentinel-1 AND filename:S1B_*)",
						"( " + sProd + " AND ( name:S1* AND name:S1B_* AND name:* AND name:* ) ) AND ( " + sTW + " )"),
				Arguments.of(sProd + " AND " + sTW + " AND   (platformname:Sentinel-1)",
						"( " + sProd + " AND ( name:S1* AND name:* AND name:* AND name:* ) ) AND ( " + sTW + " )"),
				Arguments.of(sProd + " AND " + sTW,
						"( " + sProd + " ) AND ( " + sTW + " )"),
				//without product name
				Arguments.of( sTW + " AND   (platformname:Sentinel-1 AND filename:S1B_* AND producttype:GRD)",
						"( ( name:S1* AND name:S1B_* AND name:*GRD* AND name:* ) ) AND ( " + sTW + " )"),
				Arguments.of(sTW + " AND   (platformname:Sentinel-1 AND filename:S1B_*)",
						"( ( name:S1* AND name:S1B_* AND name:* AND name:* ) ) AND ( " + sTW + " )"),
				Arguments.of(sTW + " AND   (platformname:Sentinel-1)",
						"( ( name:S1* AND name:* AND name:* AND name:* ) ) AND ( " + sTW + " )"),
				Arguments.of(sTW, "( " + sTW + " )")
				);
	}

	/**
	 * Test method for {@link wasdi.shared.opensearch.DiasQueryTranslator#getProductName(java.lang.String)}.
	 */
	@ParameterizedTest
	@MethodSource("provideValuePairs")
	void testQueryTranslation(String sInput, String sExpected) {
		DiasQueryTranslatorONDA oDiasQueryTranslator = new DiasQueryTranslatorONDA();
		assertEquals(sExpected, oDiasQueryTranslator.translate(sInput));
	}
	
	
	//Legacy tests here, check them
/*
	@ParameterizedTest
	@ValueSource( strings = {
		//full name without extension
		"S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423",
		"S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423*",
		"*S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423",
		"*S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423*"
	})
	final void testParseFreeText_fullNameWithoutExtensions(String sInputText) {
		DiasQueryTranslatorONDA oDQT = new DiasQueryTranslatorONDA();
		String sSuffix = " AND ( beginPosition:[2021-01-10T00:00:00.000Z TO 2021-01-12T23:59:59.999Z] AND endPosition:[2021-01-10T00:00:00.000Z TO 2021-01-12T23:59:59.999Z] )";
		assertEquals("*S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423*", oDQT.parseProductName(sInputText + sSuffix));
	}

	@ParameterizedTest
	@ValueSource( strings = {
			"S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.zip",
			"S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.",

			"S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.zip*",
			"S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.*",

			"*S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.zip",
			"*S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.",

			"*S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.zip*",
			"*S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.*",
	})
	final void testParseFreeText_fullNameWithExtensions(String sInputText) {
		DiasQueryTranslatorONDA oDQT = new DiasQueryTranslatorONDA();
		String sSuffix = " AND ( beginPosition:[2021-01-10T00:00:00.000Z TO 2021-01-12T23:59:59.999Z] AND endPosition:[2021-01-10T00:00:00.000Z TO 2021-01-12T23:59:59.999Z] )";
		assertEquals("*S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423", oDQT.parseProductName(sInputText + sSuffix));
	}
	
	@ParameterizedTest
	@ValueSource( strings = {
			"_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423",
			"*_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423*",
			"*_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423",
			"_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423*"
	})
	final void testParseFreeText_noHeadWithoutExtensions(String sInputText) {
		DiasQueryTranslatorONDA oDQT = new DiasQueryTranslatorONDA();
		String sSuffix = " AND ( beginPosition:[2021-01-10T00:00:00.000Z TO 2021-01-12T23:59:59.999Z] AND endPosition:[2021-01-10T00:00:00.000Z TO 2021-01-12T23:59:59.999Z] )";
		assertEquals("*_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423*", oDQT.parseProductName(sInputText + sSuffix));
	}
	
	@ParameterizedTest
	@ValueSource( strings = {
			"_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.zip",
			"_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.",
			"*_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.zip*",
			"*_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.*",
			"*_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.zip",
			"*_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.",
			"_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.zip*",
			"_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.*"
	})
	final void testParseFreeText_noHeadWithExtensions(String sInputText) {
		DiasQueryTranslatorONDA oDQT = new DiasQueryTranslatorONDA();
		String sSuffix = " AND ( beginPosition:[2021-01-10T00:00:00.000Z TO 2021-01-12T23:59:59.999Z] AND endPosition:[2021-01-10T00:00:00.000Z TO 2021-01-12T23:59:59.999Z] )";
		assertEquals("*_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423", oDQT.parseProductName(sInputText + sSuffix));
	}
	
	
	@ParameterizedTest
	@ValueSource( strings = {
			"S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D*",
			"S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D",
			"*S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D",
			"*S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D*"
	})
	final void testParseFreeText_noTail(String sInputText) {
		DiasQueryTranslatorONDA oDQT = new DiasQueryTranslatorONDA();
		String sSuffix = " AND ( beginPosition:[2021-01-10T00:00:00.000Z TO 2021-01-12T23:59:59.999Z] AND endPosition:[2021-01-10T00:00:00.000Z TO 2021-01-12T23:59:59.999Z] )";
		assertEquals("*S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D*", oDQT.parseProductName(sInputText + sSuffix));
	}
	
	
	@ParameterizedTest
	@ValueSource( strings = {
			"*_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D*",
			"_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D*",
			"*_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D",
			"_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D"
	})
	final void testParseFreeText_noHeadNoTail(String sInputText) {
		DiasQueryTranslatorONDA oDQT = new DiasQueryTranslatorONDA();
		String sSuffix = " AND ( beginPosition:[2021-01-10T00:00:00.000Z TO 2021-01-12T23:59:59.999Z] AND endPosition:[2021-01-10T00:00:00.000Z TO 2021-01-12T23:59:59.999Z] )";
		assertEquals("*_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D*", oDQT.parseProductName(sInputText + sSuffix));
	}
	*/
}
