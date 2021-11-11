/**
 * Created by Cristiano Nattero on 2020-02-20
 * 
 * Fadeout software
 *
 */
package wasdi.shared.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * @author c.nattero
 *
 */
class TestTimeEpochUtils {


	@ParameterizedTest
	@CsvSource({
		"2020-02-12T00:00:00.000Z, 1581465600000",
		"2020-02-12T00:00:00.000Z, 1581465600000",
		"2020-02-19T23:59:59.999Z, 1582156799999",
		"2020-01-01T00:00:00.000Z, 1577836800000",
		"2019-12-31T23:59:59.999Z, 1577836799999"
	})
	final void callTests(String sDate, Long lEpoch) {
		testFromDatetoEpoch(sDate, lEpoch);
		testFromEpochToDate(sDate, lEpoch);
	}
	
	final void testFromDatetoEpoch(String sDate, Long lExpected) {
		assertEquals(lExpected, TimeEpochUtils.fromDateStringToEpoch(sDate));
	}
	
	final void testFromEpochToDate(String sExpected, Long lEpoch) {
		assertEquals(sExpected, TimeEpochUtils.fromEpochToDateString(lEpoch));
	}
	
}

