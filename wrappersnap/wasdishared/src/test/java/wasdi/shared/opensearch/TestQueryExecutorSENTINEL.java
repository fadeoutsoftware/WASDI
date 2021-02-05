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

import wasdi.shared.utils.AuthenticationCredentials;

/**
 * @author c.nattero
 *
 */
class TestQueryExecutorSENTINEL {

	static String s_sStart = "0";
	static String s_sRows = "1";
	static String s_sSort = "ingestiondate";
	static String s_sOrder = "asc";
	private PaginatedQuery m_oPaginatedQuery;

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
		s_sStart = "0";
		s_sRows = "1";
		s_sSort = "ingestiondate";
		s_sOrder = "asc";
		m_oPaginatedQuery = new PaginatedQuery("", s_sStart, s_sRows, s_sSort, s_sOrder );
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterEach
	void tearDown() throws Exception {
	}

	private static Stream<Arguments> provideValuePairs() {
		String sUrl = "https://scihub.copernicus.eu/apihub/search";
		String sConditions = "start=" + s_sStart +
				"&rows=" + s_sRows + 
				"&orderby=" + s_sSort + 
				"%20" + s_sOrder;

		String sProd = "S1B_IW_GRDH_1SDV_20210201T011057_20210201T011119_025406_0306AA_FA59";
		String sTW = "( beginPosition:[2021-01-29T00:00:00.000Z TO 2021-02-05T23:59:59.999Z] AND endPosition:[2021-01-29T00:00:00.000Z TO 2021-02-05T23:59:59.999Z] )";
		String sEncTW = "%28%20beginPosition%3A%5B2021-01-29T00%3A00%3A00.000Z%20TO%202021-02-05T23%3A59%3A59.999Z%5D%20AND%20endPosition%3A%5B2021-01-29T00%3A00%3A00.000Z%20TO%202021-02-05T23%3A59%3A59.999Z%5D%20%29";
		return Stream.of(
				//				//null is blank
				Arguments.of(null, sUrl + sConditions),
				//				//blank is blank
				Arguments.of("", sUrl + "?q=&" + sConditions),
				
				//with product
				Arguments.of(sProd + " AND " + sTW + " AND   (platformname:Sentinel-1 AND filename:S1B_* AND producttype:GRD)",
						sUrl + "?q=" + sProd + "%20AND%20" + sEncTW + "%20AND%20%20%20%28platformname%3ASentinel-1%20AND%20filename%3AS1B_%2A%20AND%20producttype%3AGRD%29&" + sConditions),
				Arguments.of(sProd + " AND " + sTW + " AND   (platformname:Sentinel-1 AND filename:S1B_*)",
						sUrl + "?q=" + sProd + "%20AND%20" + sEncTW + "%20AND%20%20%20%28platformname%3ASentinel-1%20AND%20filename%3AS1B_%2A%29&" + sConditions),
				Arguments.of(sProd + " AND " + sTW + " AND   (platformname:Sentinel-1)",
						sUrl + "?q=" + sProd + "%20AND%20" + sEncTW + "%20AND%20%20%20%28platformname%3ASentinel-1%29&" + sConditions),
				Arguments.of(sProd + " AND " + sTW,
						sUrl + "?q=" + sProd + "%20AND%20" + sEncTW + "&" + sConditions),
				Arguments.of(sProd, sUrl + "?q=" + sProd + "&" + sConditions),
				
				//without product
				Arguments.of(sTW + " AND   (platformname:Sentinel-1 AND filename:S1B_* AND producttype:GRD)",
						sUrl + "?q=" + sEncTW + "%20AND%20%20%20%28platformname%3ASentinel-1%20AND%20filename%3AS1B_%2A%20AND%20producttype%3AGRD%29&" + sConditions),
				Arguments.of(sTW + " AND   (platformname:Sentinel-1 AND filename:S1B_*)",
						sUrl + "?q=" + sEncTW + "%20AND%20%20%20%28platformname%3ASentinel-1%20AND%20filename%3AS1B_%2A%29&" + sConditions),
				Arguments.of(sTW + " AND   (platformname:Sentinel-1)",
						sUrl + "?q=" + sEncTW + "%20AND%20%20%20%28platformname%3ASentinel-1%29&" + sConditions),
				Arguments.of(sTW, sUrl + "?q=" + sEncTW + "&" + sConditions)
				);
	}

	/**
	 * Test method for {@link wasdi.shared.opensearch.DiasQueryTranslator#getProductName(java.lang.String)}.
	 */
	@ParameterizedTest
	@MethodSource("provideValuePairs")
	void testQueryTranslation(String sInput, String sExpected) {
		QueryExecutorFactory oFactory = new QueryExecutorFactory();
		AuthenticationCredentials oAuthenticationCredentials = new AuthenticationCredentials("USERNAME", "PASSWORD");
		QueryExecutor oQueryExecutor = oFactory.getExecutor("SENTINEL", oAuthenticationCredentials, null, null, null, null);

		m_oPaginatedQuery.setQuery(sInput);

		String sTranslated = oQueryExecutor.getSearchUrl(m_oPaginatedQuery);

		assertEquals(sExpected, sTranslated);
	}

}
