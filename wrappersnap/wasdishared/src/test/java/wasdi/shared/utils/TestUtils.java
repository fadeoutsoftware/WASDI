package wasdi.shared.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

public class TestUtils {
	
	@Test
	public void testSplitTimeRangeInMonthyIntervalsSameMonth() {
		Date oStartDate = new Date(1687345755000L); // Wednesday, June 21, 2023 11:09:15 AM (GMT)
		Date oEndDate = new Date(1688076555000L); // Thursday, June 29, 2023 10:09:15 PM (GMT)
		List<Date[]> aaoRes = Utils.splitTimeRangeInMonthyIntervals(oStartDate, oEndDate);
		assertEquals(1, aaoRes.size());
		assertEquals(0, oStartDate.compareTo(aaoRes.get(0)[0]));
		assertEquals(0, oEndDate.compareTo(aaoRes.get(0)[1]));
	}
	
	@Test
	public void testSplitTimeRangeInMonthyIntervalsDifferentMonthSameYear() {
		Date oStartDate = new Date(1687345755000L); // Wednesday, June 21, 2023 11:09:15 AM (GMT)
		Date oEndDate = new Date(1697404155000L); // Sunday, October 15, 2023 9:09:15 PM (GMT)
		List<Date[]> aaoRes = Utils.splitTimeRangeInMonthyIntervals(oStartDate, oEndDate);
		assertEquals(5, aaoRes.size());
		// check start and end of the interval
		assertEquals(0, oStartDate.compareTo(aaoRes.get(0)[0]));
		assertEquals(0, oEndDate.compareTo(aaoRes.get(4)[1]));
		// check at least months at the beginning and end of each interval
		int iStartMonth = 5; // index for June
		for (Date[] aoInterval : aaoRes) {
			Calendar oStartCalendar = Calendar.getInstance();
			oStartCalendar.setTime(aoInterval[0]);
			Calendar oEndCalendar = Calendar.getInstance();
			oEndCalendar.setTime(aoInterval[1]);
			assertEquals(oStartCalendar.get(Calendar.MONTH), oEndCalendar.get(Calendar.MONTH));
			assertEquals(oStartCalendar.get(Calendar.YEAR), oEndCalendar.get(Calendar.YEAR));
			assertEquals(iStartMonth, oStartCalendar.get(Calendar.MONTH));
			iStartMonth = (iStartMonth + 1) % 13;
		}
	}
	
	@Test
	public void testSplitTimeRangeInMonthyIntervalsDifferentMonthAndYear() {
		Date oStartDate = new Date(1687345755000L); // Wednesday, June 21, 2023 11:09:15 AM (GMT)
		Date oEndDate = new Date(1710536955000L); // Friday, March 15, 2024 9:09:15 PM (GMT)
		List<Date[]> aaoRes = Utils.splitTimeRangeInMonthyIntervals(oStartDate, oEndDate);
		assertEquals(10, aaoRes.size());
		// check start and end of the interval
		assertEquals(0, oStartDate.compareTo(aaoRes.get(0)[0]));
		assertEquals(0, oEndDate.compareTo(aaoRes.get(9)[1]));
		// check at least months at the beginning and end of each interval
		int iStartMonth = 5; // index for June
		for (Date[] aoInterval : aaoRes) {
			Calendar oStartCalendar = Calendar.getInstance();
			oStartCalendar.setTime(aoInterval[0]);
			Calendar oEndCalendar = Calendar.getInstance();
			oEndCalendar.setTime(aoInterval[1]);
			assertEquals(oStartCalendar.get(Calendar.MONTH), oEndCalendar.get(Calendar.MONTH));
			assertEquals(iStartMonth, oStartCalendar.get(Calendar.MONTH));
			iStartMonth = (iStartMonth + 1) % 12;
		}
	}
	

}
