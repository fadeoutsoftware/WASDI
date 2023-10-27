package wasdi.jwasdilib.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Logger;

public final class DateUtils {

	private final static Logger LOGGER = Logger.getLogger(DateUtils.class);

	private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

	private DateUtils() {
		throw new AssertionError("Utility class should not be instantiated.");
	}

	public static String convert(Date d) {
		LOGGER.info("convert");

		if (d != null) {
			return simpleDateFormat.format(d);
		}

		return null;
	}

	public static Date convert(String s) {
		LOGGER.info("convert");

		if (s != null && !s.isEmpty()) {
			try {
				return simpleDateFormat.parse(s);
			} catch (ParseException e) {
				System.out.println("WasdiLib error " + e.toString());
			}
		}

		return null;
	}

	public static Date getEarlierDate(Date currentDate, int daysBefore) {
		LOGGER.info("getEarlierDate");

		Calendar cal = Calendar.getInstance();
		cal.setTime(currentDate);
		cal.add(Calendar.DATE, -daysBefore);

		return cal.getTime();
	}

}
