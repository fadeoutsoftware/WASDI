/**
 * Created by Cristiano Nattero on 2020-02-20
 * 
 * Fadeout software
 *
 */
package wasdi.shared.utils;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import wasdi.shared.utils.log.WasdiLog;

/**
 * @author c.nattero
 *
 */
public class TimeEpochUtils {
	private TimeEpochUtils() {
		// / private constructor to hide the public implicit one 
	}

	private static final String s_sTIMEZONE = "GMT";
	private static final String s_sDATEFORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";


	public static String fromEpochToDateString(long lEpochMilliSeconds) {
		try {
			SimpleDateFormat oSimpleDateFormat = new SimpleDateFormat(s_sDATEFORMAT);
			oSimpleDateFormat.setTimeZone(TimeZone.getTimeZone(s_sTIMEZONE));
			return oSimpleDateFormat.format(new Date(lEpochMilliSeconds));
		}
		catch (Exception oE) {
			WasdiLog.debugLog("TimeEpochUtils.fromEpochToDateString: " + oE);
			return "";
		}
	}
	
	/**
	 * Return the Epoch Milli Seconds of a String date with the sDateFormat
	 * @param sDate String representing the date
	 * @param sDateFormat String Date Format
	 * @return time epoch
	 */
	public static Long fromDateStringToEpoch(String sDate, String sDateFormat) {
		Long lEpochMilliSeconds = null;
		try {
			SimpleDateFormat oSimpleDateFormat = new SimpleDateFormat(sDateFormat);
			oSimpleDateFormat.setTimeZone(TimeZone.getTimeZone(s_sTIMEZONE));
			Date oDate = oSimpleDateFormat.parse(sDate);
			lEpochMilliSeconds = oDate.getTime();
		} catch (Exception oE) {
			WasdiLog.log("ERROR", "Utils.fromDateStringToTimestamp: " + oE);
		}
		return lEpochMilliSeconds;

	}
	
	/**
	 * Return the Epoch Milli Seconds of a String date with the default format "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
	 * for UTC zone  
	 * @param sDate String representing the date
	 * @return time epoch
	 */
	public static Long fromDateStringToEpoch(String sDate) {
		return fromDateStringToEpoch(sDate, s_sDATEFORMAT);
	}

	/**
	 * Count the days of an interval, considering both the start and the end dates.
	 * @param sFromDate the start of the interval
	 * @param sToDate the end of the interval
	 * @return the number of days of the interval
	 */
	public static int countDaysIncluding(String sFromDate, String sToDate) {
		long lStart = fromDateStringToEpoch(sFromDate);
		long lEnd = fromDateStringToEpoch(sToDate);

		long lDiffInMillies = Math.abs(lEnd - lStart);
		long lDays = TimeUnit.DAYS.convert(lDiffInMillies, TimeUnit.MILLISECONDS) + 1;

		return (int) lDays;
	}

	/**
	 * Get a date in in the future obtained by adding a specified number of days to the specified starting date.
	 * 
	 * @param lTimeInMillis the time in millis of the starting date
	 * @param iDaysLater the number of days to be added
	 * @return the new date
	 */
	public static Date getLaterDate(long lTimeInMillis, int iDaysLater) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(lTimeInMillis);
		cal.add(Calendar.DATE, iDaysLater);

		return cal.getTime();
	}

	/**
	 * Get a date in in the future obtained by adding a specified number of days to the specified starting date.
	 * 
	 * @param oCurrentDate the starting date
	 * @param iDaysLater the number of days to be added
	 * @return the new date
	 */
	public static Date getLaterDate(Date oCurrentDate, int iDaysLater) {
		if (oCurrentDate == null) {
			return null;
		}

		return getLaterDate(oCurrentDate.getTime(), iDaysLater);
	}
	
	/**
	 * Converts a string based date in ISO8601 format to an Instant Object
	 * @param sDate
	 * @return
	 */
	public static Instant fromDateStringToInstant(String sDate) {
		Instant oDateFrom = null;

		if(!Utils.isNullOrEmpty(sDate)) {
			try {
				oDateFrom = Instant.parse(sDate);
			} catch (Exception oE) {
				WasdiLog.debugLog("TimeEpochUtils.fromDateStringToInstant: could not convert start date " + sDate + " to a valid date, ignoring it");
			}
		}

		return oDateFrom;
	}
	
	/**
	 * Converts a string based date in ISO8601 format to a Date Object
	 * @param sDate ISO8601 date string
	 * @return Corresponding Date
	 */
	public static Date fromISO8061DateStringToDate(String sDate) {
		
		try {
			Instant oInstant = fromDateStringToInstant(sDate);
			
			if (oInstant!=null) {
				return Date.from(oInstant);
			}
		} catch (Exception oE) {
			WasdiLog.debugLog("TimeEpochUtils.fromISO8061DateStringToDate: could not convert start date " + sDate + " to a valid date, ignoring it");
		}
		
		return null;
		
	}
}
