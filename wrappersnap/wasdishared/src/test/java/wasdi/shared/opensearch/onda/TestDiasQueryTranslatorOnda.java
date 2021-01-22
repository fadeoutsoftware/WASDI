/**
 * 
 */
package wasdi.shared.opensearch.onda;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * @author c.nattero
 *
 */
class TestDiasQueryTranslatorOnda {

	/**
	 * Test method for {@link wasdi.shared.opensearch.onda.DiasQueryTranslatorONDA#parseFreeText(java.lang.String)}.
	 */
	@Test
	void testParseFreeText() {
		DiasQueryTranslatorONDA oDQT = new DiasQueryTranslatorONDA();
		
		assertEquals(null, oDQT.parseFreeText(""));
	}

}
