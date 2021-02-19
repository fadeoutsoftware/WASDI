/**
 * Created by Cristiano Nattero on 2020-02-20
 * 
 * Fadeout software
 *
 */
package wasdi.shared.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

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
			SimpleDateFormat oSimpleDateFormat = new SimpleDateFormat(TimeEpochUtils.s_sDATEFORMAT);
			oSimpleDateFormat.setTimeZone(TimeZone.getTimeZone(TimeEpochUtils.s_sTIMEZONE));
			return oSimpleDateFormat.format(new Date(lEpochMilliSeconds));
		}
		catch (Exception oE) {
			Utils.debugLog("TimeEpochUtils.fromEpochToDateString: " + oE);
			return "";
		}
	}

	public static Long fromDateStringToEpoch(String sDate) {
		Long lEpochMilliSeconds = null;
		try {
			SimpleDateFormat oSimpleDateFormat = new SimpleDateFormat(TimeEpochUtils.s_sDATEFORMAT);
			oSimpleDateFormat.setTimeZone(TimeZone.getTimeZone(TimeEpochUtils.s_sTIMEZONE));
			Date oDate = oSimpleDateFormat.parse(sDate);
			lEpochMilliSeconds = oDate.getTime();
		} catch (Exception oE) {
			Utils.log("ERROR", "Utils.fromDateStringToTimestamp: " + oE);
		}
		return lEpochMilliSeconds;

	}

}
