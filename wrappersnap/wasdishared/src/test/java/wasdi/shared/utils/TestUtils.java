package wasdi.shared.utils;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

public class TestUtils {
	
	@Test
	public void testSplitTimeRangeInMonthlyIntervalsSameMonth() {
		Date oStartDate = new Date(1687345755000L); // Wednesday, June 21, 2023 11:09:15 AM (GMT)
		Date oEndDate = new Date(1688076555000L); // Thursday, June 29, 2023 10:09:15 PM (GMT)
		List<Date[]> aaoRes = Utils.splitTimeRangeInMonthlyIntervals(oStartDate, oEndDate, 0, 10);
		assertEquals(1, aaoRes.size());
		assertEquals(0, oStartDate.compareTo(aaoRes.get(0)[0]));
		assertEquals(0, oEndDate.compareTo(aaoRes.get(0)[1]));
	}
	
	
	@Test
	public void testSplitTimeRangeInMonthlyIntervalsSameYear() {
		Date oStartDate = new Date(1687345755000L); // Wednesday, June 21, 2023 11:09:15 AM (GMT)
		Date oEndDate = new Date(1697404155000L); // Sunday, October 15, 2023 9:09:15 PM (GMT)
		List<Date[]> aaoRes = Utils.splitTimeRangeInMonthlyIntervals(oStartDate, oEndDate, 0, 10);
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
			assertEquals(oStartCalendar.get(Calendar.YEAR), oEndCalendar.get(Calendar.YEAR));
			assertEquals(iStartMonth, oStartCalendar.get(Calendar.MONTH));
			iStartMonth = (iStartMonth + 1) % 12;
		}
	}
	
	@Test
	public void testSplitTimeRangeInMonthlyIntervalsDifferentMonthAndYearLimit5Offset0() {
		Date oStartDate = new Date(1687345755000L); // Wednesday, June 21, 2023 11:09:15 AM (GMT)
		Date oEndDate = new Date(1710536955000L); // Friday, March 15, 2024 9:09:15 PM (GMT)
		List<Date[]> aaoRes = Utils.splitTimeRangeInMonthlyIntervals(oStartDate, oEndDate, 0, 5);
		assertEquals(5, aaoRes.size());
		
		// check first interval
		Date[] aoDateInterval = aaoRes.get(0); 
		Calendar oIntervalCalendar = Calendar.getInstance();
		oIntervalCalendar.setTime(aoDateInterval[0]);
		assertEquals(21, oIntervalCalendar.get(Calendar.DAY_OF_MONTH));
		assertEquals(5, oIntervalCalendar.get(Calendar.MONTH)); // first month should be June
		assertEquals(2023, oIntervalCalendar.get(Calendar.YEAR));
		
		// check last interval
		aoDateInterval = aaoRes.get(4); 
		oIntervalCalendar.setTime(aoDateInterval[0]);
		assertEquals(1, oIntervalCalendar.get(Calendar.DAY_OF_MONTH));
		assertEquals(9, oIntervalCalendar.get(Calendar.MONTH)); // last month should be October
		assertEquals(2023, oIntervalCalendar.get(Calendar.YEAR));
		
		// check at least months at the beginning and end of each interval
		int iStartMonth = 5; // index for June
		for (Date[] aoInterval : aaoRes) {
			Calendar oStartCalendar = Calendar.getInstance();
			oStartCalendar.setTime(aoInterval[0]);
			Calendar oEndCalendar = Calendar.getInstance();
			oEndCalendar.setTime(aoInterval[1]);
			assertEquals(iStartMonth, oStartCalendar.get(Calendar.MONTH));
			iStartMonth = (iStartMonth + 1) % 12;
		}
	}
	
	@Test
	public void testSplitTimeRangeInMonthlyIntervalsDifferentMonthAndYearLimit5Offset() {
		Date oStartDate = new Date(1687345755000L); // Wednesday, June 21, 2023 11:09:15 AM (GMT)
		Date oEndDate = new Date(1710536955000L); // Friday, March 15, 2024 9:09:15 PM (GMT)
		List<Date[]> aaoRes = Utils.splitTimeRangeInMonthlyIntervals(oStartDate, oEndDate, 5, 5);
		assertEquals(5, aaoRes.size());
		
		// check first interval
		Date[] aoDateInterval = aaoRes.get(0); 
		Calendar oIntervalCalendar = Calendar.getInstance();
		oIntervalCalendar.setTime(aoDateInterval[0]);
		assertEquals(1, oIntervalCalendar.get(Calendar.DAY_OF_MONTH));
		assertEquals(10, oIntervalCalendar.get(Calendar.MONTH)); // first month should be November
		assertEquals(2023, oIntervalCalendar.get(Calendar.YEAR));
		
		// check last interval
		aoDateInterval = aaoRes.get(4); 
		oIntervalCalendar.setTime(aoDateInterval[1]);
		assertEquals(15, oIntervalCalendar.get(Calendar.DAY_OF_MONTH));
		assertEquals(2, oIntervalCalendar.get(Calendar.MONTH)); // last month should be March 
		assertEquals(2024, oIntervalCalendar.get(Calendar.YEAR));
		
		// check at least months at the beginning and end of each interval
		int iStartMonth = 10; // index for June
		for (Date[] aoInterval : aaoRes) {
			Calendar oStartCalendar = Calendar.getInstance();
			oStartCalendar.setTime(aoInterval[0]);
			Calendar oEndCalendar = Calendar.getInstance();
			oEndCalendar.setTime(aoInterval[1]);
			assertEquals(iStartMonth, oStartCalendar.get(Calendar.MONTH));
			iStartMonth = (iStartMonth + 1) % 12;
		}
	}
	
	@Test
	public void testSplitTimeRangeInMonthlyIntervalsSameRange() {
		Date oStartDate = new Date(1687345755000L); // Wednesday, June 21, 2023 11:09:15 AM (GMT)
		Date oEndDate = new Date(1687345755000L); // Wednesday, June 21, 2023 11:09:15 AM (GMT)
		List<Date[]> aaoRes = Utils.splitTimeRangeInMonthlyIntervals(oStartDate, oEndDate, 0, 10);
		assertTrue(aaoRes.isEmpty());
	}

}
