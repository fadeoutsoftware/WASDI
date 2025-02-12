package wasdi.shared.utils;

import java.io.File;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

// email, IP addresses (v4 and v6), domains and URL validators:
import org.apache.commons.validator.routines.DomainValidator;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.commons.validator.routines.UrlValidator;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.ProductWorkspace;
import wasdi.shared.business.Workspace;
import wasdi.shared.data.ProductWorkspaceRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Created by p.campanella on 14/10/2016.
 */
public class Utils {

	public static final ThreadLocal<SimpleDateFormat> SIMPLE_DATE_FORMAT_yyyyMMdd = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyyMMdd"));
	public static final ThreadLocal<SimpleDateFormat> SIMPLE_DATE_FORMAT_yyyyMMddTZ = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));

	public static int s_iSessionValidityMinutes = 24 * 60;
	private static SecureRandom s_oUtilsRandom = new SecureRandom();

	/**
	 * Private constructor
	 */
	private Utils() {
		throw new IllegalStateException("Utils.Utils: this is just a utility class, please do not instantiate it");
	}
	
	/**
	 * Checks if a string is null or empty
	 * @param sString String to check
	 * @return true if it is null or empty. False if it is a valud string
	 */
	public static boolean isNullOrEmpty(String sString) {
		return sString == null || sString.isEmpty();
	}
	
	/**
	 * Checks if a Double is null or empty
	 * @param oDoube
	 * @return
	 */
	public static boolean isNullOrEmpty(Double oDouble) {
		return oDouble == null || oDouble.longValue() == 0;
	}

	/**
	 * Get a random name capped to a specific length
	 * adapted from:
	 * 4. Generate Random Alphanumeric String With Java 8
	 * https://www.baeldung.com/java-random-string 
	 * @param iLen
	 * @return
	 */
	public static String getCappedRandomName(int iLen) {
		if(iLen < 0) {
			iLen = - iLen;
		}
		
		int iLeftLimit = 48; // numeral '0'
	    int iRightLimit = 122; // letter 'z'
	 
	    return s_oUtilsRandom.ints(iLeftLimit, iRightLimit + 1)
    		//filter method above to leave out Unicode characters between 65 and 90
	    	//to avoid out of range characters.
    		.filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
    		.limit(iLen)
    		.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
    		.toString();
	}
	
	/**
	 * Get a random name (ie UUID string)
	 * @return Random name
	 */
	public static String getRandomName() {
		return UUID.randomUUID().toString();
	}
	
	/**
	 * Convert a Double in a date assuming it contains a valid timestamp
	 * @param oDouble Input timestamp
	 * @return Corresponding date
	 */
	public static Date getDate(Double oDouble) {
		if (oDouble == null) {
			return null;
		}

		double dDate = oDouble;
		long lLong = (long) dDate;
		return new Date(lLong);
	}

	public static Double getDateAsDouble(Date oDate) {
		if (oDate == null) {
			return null;
		}

		return (double) (oDate.getTime());
	}

	public static Date getDate(Long oLong) {
		return new Date(oLong);
	}


	/**
	 * Format the date using the yyyyMMdd date format.
	 * @param oDate the date to be formatted
	 * @return the string containing the formatted date
	 */
	public static String formatToYyyyMMdd(Date oDate) {
		return new SimpleDateFormat("yyyyMMdd").format(oDate);
	}
	
	public static String getFormatDate(Date oDate) {

		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(oDate);
	}

	public static String getFormatDate(Double oDouble) {

		if (oDouble == null) {
			return null;
		}

		return getFormatDate(new Date(oDouble.longValue()));
	}

	public static String formatToYyyyDashMMDashdd(Date oDate) {
		return new SimpleDateFormat("yyyy-MM-dd").format(oDate);
	}

	public static Date getWasdiDate(String sWasdiDate) {

		try {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(sWasdiDate);
		} catch (Exception oE) {
			WasdiLog.errorLog("Utils.getWasdiDate( " + sWasdiDate + "  ): could not be parsed due to " + oE);
			return null;
		}
	}

	public static Date getYyyyMMddDate(String sDate) {

		try {
			return SIMPLE_DATE_FORMAT_yyyyMMdd.get().parse(sDate);
		} catch (Exception oE) {
			WasdiLog.errorLog("Utils.getYyyyMMddDate( " + sDate + "  ): could not be parsed due to " + oE);
			return null;
		}
	}

	public static Date getYyyyMMddTZDate(String sDate) {
		if (isNullOrEmpty(sDate)) {
			return null;
		}

		try {
			return SIMPLE_DATE_FORMAT_yyyyMMddTZ.get().parse(sDate);
		} catch (Exception oE) {
			WasdiLog.errorLog("Utils.getYyyyMMddTZDate( " + sDate + "  ): could not be parsed due to " + oE);
			return null;
		}
	}

	/**
	 * Parse the date into a Double fit for MongoDb.
	 * @param sWasdiDate the date as a string in the yyyy-MM-dd HH:mm:ss format
	 * @return the time in millis in the form of a Double
	 */
	public static Double getWasdiDateAsDouble(String sWasdiDate) {
		if (sWasdiDate == null) {
			return null;
		}

		Date oDate = getWasdiDate(sWasdiDate);

		if (oDate == null) {
			return null;
		}

		long lTimeInMillis = oDate.getTime();

		return Double.valueOf(lTimeInMillis);
	}
	
	public static long getProcessWorkspaceSecondsDuration(ProcessWorkspace oProcessWorkspace) {
		try {
			Double oStart = oProcessWorkspace.getOperationStartTimestamp();
			Double oEnd = oProcessWorkspace.getOperationEndTimestamp();
			
			if (oStart==null) return 0l;
			if (oEnd==null) return 0l;
			
			long lDiff = oEnd.longValue() - oStart.longValue();
			lDiff /= 1000l;
			return lDiff;
		}
		catch (Exception e) {
			return 0l;
		}
	}
	
	
	/**
	 * Gets the local time offset from UTC
	 * @return
	 */
	public static String getLocalDateOffsetFromUTCForJS() {
		TimeZone oTimeZone = TimeZone.getDefault();
		GregorianCalendar oCalendar = new GregorianCalendar(oTimeZone);
		int iOffsetInMillis = oTimeZone.getOffset(oCalendar.getTimeInMillis());
		
		if (iOffsetInMillis == 0) return "Z";
		
		String sOffset = String.format("%02d:%02d", Math.abs(iOffsetInMillis / 3600000), Math.abs((iOffsetInMillis / 60000) % 60));
		sOffset = (iOffsetInMillis >= 0 ? "+" : "-") + sOffset;
		return sOffset;
	}

	public static String getDateWithLocalDateOffsetFromUTCForJS(String sDate) {
		if (sDate == null || sDate.isEmpty()) {
			return "";
		}

		return sDate +  " " + getLocalDateOffsetFromUTCForJS();
	}
	
	/**
	 * Split the time range represented by the two input Date objects in monthly intervals.
	 * Each Date array represents an interval and is made of two elements. Element at index 0 represent the start Date of the interval,
	 * while the element at index 1 represents the end Date of the interval. 
	 * E.g. if the input parameters represent an interval from 22/05/2023 to 10/07/203, then the monthly intervals will be: 
	 * [22/05/2023-31/05/2023], [01/06/2023-30/06/2023], [01/07/2023-10/07/2023]
	 * @param oStartDate the start date of the time range
	 * @param oEndDate the end date of the time range
	 * @param iOffset offset for the pagination
	 * @param iLimit maximum number of results per page
	 * @return a list of monthly intervals included in the time range, represented by 2-dimensional Date arrays.
	 */
	public static List<Date[]> splitTimeRangeInMonthlyIntervals(Date oStartDate, Date oEndDate, int iOffset, int iLimit) {
		List<Date[]> aaoIntervals = new LinkedList<>();
		Calendar oStartCalendar = Calendar.getInstance();
		oStartCalendar.setTime(oStartDate);
		Calendar oEndCalendar = Calendar.getInstance();
		oEndCalendar.setTime(oEndDate);
		
		int iCurrentInterval = 0;
		while (oStartCalendar.before(oEndCalendar)) {
			Date[] aoCurrentInterval = getMonthIntervalFromDate(oStartCalendar, oEndCalendar);
			if (aoCurrentInterval.length == 0) {
				break;
			}
			
			if (iCurrentInterval >= iOffset && iCurrentInterval < iOffset + iLimit) {
				aaoIntervals.add(aoCurrentInterval);
			}
			
			oStartCalendar.setTime(aoCurrentInterval[1]);
			oStartCalendar.add(Calendar.MILLISECOND, 1);
			
			iCurrentInterval++;
		}
		return aaoIntervals;
	}
	
	
	private static Date[] getMonthIntervalFromDate(Calendar oStartCalendar, Calendar oEndCalendar) {
		Calendar oStartCalendarClone = Calendar.getInstance();
		oStartCalendarClone.setTime(oStartCalendar.getTime());
		int iStartMonth = oStartCalendarClone.get(Calendar.MONTH);
		int iEndMonth = oEndCalendar.get(Calendar.MONTH);
		int iStartYear = oStartCalendarClone.get(Calendar.YEAR);
		int iEndYear = oEndCalendar.get(Calendar.YEAR);
		
		if (iStartMonth == iEndMonth && iStartYear == iEndYear) {
			Date oStartIntervalDate = oStartCalendarClone.getTime();
			Date oEndIntervalDate = oEndCalendar.getTime();
			Date[] aoInterval = new Date[] {oStartIntervalDate, oEndIntervalDate};
			return aoInterval;
		} else {
			Date oStartIntervalDate = oStartCalendarClone.getTime();
			// jump to the last day of the month		
			oStartCalendarClone.set(Calendar.DAY_OF_MONTH, 1);
			oStartCalendarClone.set(Calendar.MONTH, (iStartMonth + 1) % 12);
			adjustCalendar(oStartCalendarClone);
			oStartCalendarClone.add(Calendar.MILLISECOND, -1);

		
			Date oEndIntervalDate = oStartCalendarClone.getTime();
			Date[] aoIntervals = new Date[] {oStartIntervalDate, oEndIntervalDate};
			return aoIntervals;
		}
	}
	
	private static void adjustCalendar(Calendar oCalendar) {
		if (oCalendar.get(Calendar.MONTH) == 0) {
			oCalendar.add(Calendar.YEAR, 1);
		}

	}
	

	/**
	 * Format in a human readable way a file dimension in bytes
	 * @param lBytes
	 * @return
	 */
	public static String getFormatFileDimension(long lBytes) {
		int iUnit = 1024;
		if (lBytes < iUnit)
			return lBytes + " B";
		int iExp = (int) (Math.log(lBytes) / Math.log(iUnit));
		String sPrefix = ("KMGTPE").charAt(iExp - 1) + "";
		return String.format("%.1f %sB", lBytes / Math.pow(iUnit, iExp), sPrefix);
	}
	
	private static char randomChar() {
		return (char) (s_oUtilsRandom.nextInt(26) + 'a');
	}

	public static String generateRandomPassword() {
		String sPassword = UUID.randomUUID().toString();
		sPassword = sPassword.replace('-', randomChar());
		return sPassword;
	}


	public static boolean isServerNamePlausible(String sServer) {
		if (isNullOrEmpty(sServer)) {
			return false;
		}
		// Ok, let's inspect the server...
		boolean bRes = false;
		bRes = InetAddressValidator.getInstance().isValid(sServer);
		if (!bRes) {
			// then maybe it's a domain
			bRes = DomainValidator.getInstance().isValid(sServer);
		}
		if (!bRes) {
			// then maybe it's an URL
			bRes = UrlValidator.getInstance().isValid(sServer);
		}
		if (!bRes) {
			// then maybe it's localhost
			bRes = sServer.equals("localhost");
		}
		return bRes;
	}

	public static Boolean isPortNumberPlausible(Integer iPort) {
		if (null == iPort) {
			return false;
		}
		if (0 <= iPort && iPort <= 65535) {
			return true;
		}
		return false;
	}

	public static String[] convertPolygonToArray(String sArea) {
		String[] asAreaPoints = new String[0];
		if (sArea.isEmpty()) {
			return asAreaPoints;
		}

		try {
			String sCleanedArea = sArea.replaceAll("[POLYGN()]", "");
			asAreaPoints = sCleanedArea.split(",");
		} catch (Exception oE) {
			WasdiLog.debugLog("Utils.convertPolygonToArray( " + sArea + "  ): could not extract area points due to " + oE); 
		}
		return asAreaPoints;
	}

	public static boolean doesThisStringMeansTrue(String sString) {
		// default value is arbitrary!
		return (
				isNullOrEmpty(sString) ||
				sString.equalsIgnoreCase("true") ||
				sString.equalsIgnoreCase("t") ||
				sString.equalsIgnoreCase("1") ||
				sString.equalsIgnoreCase("yes") ||
				sString.equalsIgnoreCase("y")
		);
	}
	
	
	///////// units conversion
	private static String[] sUnits = {"B", "kB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB", "BB"}; //...yeah, ready for the decades to come :-O
	
	public static String getNormalizedSize(double dSize, String sInputUnit) {
		for(int i = 0; i < sUnits.length; ++i) {
			if((sUnits[i]).equals(sInputUnit)) {
				return getNormalizedSize(dSize, i);
			}
		}
		WasdiLog.warnLog("Utils.getNormalizedSize( " + dSize + ", " + sInputUnit + " ): could not find requested unit");
		return "";
	}
	
	public static String getNormalizedSize(Double dSize) {
		return getNormalizedSize(dSize, 0);
	}
	
	public static String getNormalizedSize(Double dSize, int iStartingIndex) {
		String sChosenUnit = sUnits[iStartingIndex];
		String sSize = Long.toString(Math.round(dSize)) + " " + sChosenUnit;
		 
		int iUnitIndex = Math.max(0, iStartingIndex);
		int iLim = sUnits.length -1;
		while(iUnitIndex < iLim && dSize >= 900.0) {
			dSize = dSize / 1024.0;
			iUnitIndex++;

			//now, round it to two decimal digits
			dSize = Math.round(dSize*100.0)/100.0; 
			sChosenUnit = sUnits[iUnitIndex];
			sSize = String.valueOf(dSize) + " " + sChosenUnit;
		}
		return sSize;
	}

	///// end units conversion
	
	/**
	 * Get Random Number in Range
	 * @param iMin
	 * @param iMax
	 * @return
	 */
	public static int getRandomNumber(int iMin, int iMax) {
		return iMin + new SecureRandom().nextInt(iMax - iMin);
	}

	/**
	 * Get a clone of the workspace name.
	 * If the name ends with an ordinal (i.e. 1) it is increased (i.e. 2).
	 * Otherwise, it appends the (1) termination
	 * @param sOriginalName the original name of the workspace
	 * @return the new name of the workspace
	 */
	public static String cloneName(String sOriginalName) {

		if (sOriginalName == null || sOriginalName.isEmpty()) {
			return "Untitled Workspace";
		}

		List<String> asTokens = Arrays.asList(sOriginalName.split("[\\(\\)]"));

		String sNewName;

		if (asTokens.size() == 1) {
			sNewName = sOriginalName + "(1)";
		} else {
			String sLastToken = asTokens.get(asTokens.size() - 1);

			try {
				int iOrdinal = Integer.parseInt(sLastToken);
				int iIncrementedOrdinal = iOrdinal + 1;
				int iIndex = sOriginalName.lastIndexOf(sLastToken);
				sNewName = sOriginalName.substring(0, iIndex) + iIncrementedOrdinal + ")";
			} catch (NumberFormatException e) {
				sNewName = sOriginalName + "(1)";
			}
		}

		return sNewName;
	}

	public static String generateJupyterNotebookCode(String sUserId, String sWorkspaceId) {
		return StringUtils.generateSha224(sUserId + "_" + sWorkspaceId);
	}

	/**
	 * Get the current time in millis as a Double.
	 * @return a Double object
	 */
	public static Double nowInMillis() {
		return (double) new Date().getTime();
	}
	
    /**
     * Function to remove duplicates from an ArrayList 
     * @param <T> Type
     * @param aoOriginalList
     * @return
     */
    public static <T> ArrayList<T> removeDuplicates(ArrayList<T> aoOriginalList) 
    { 
  
        // Create a new LinkedHashSet 
        Set<T> oUniqueSet = new LinkedHashSet<>(); 
  
        // Add the elements to set 
        oUniqueSet.addAll(aoOriginalList); 
  
        // Clear the list 
        aoOriginalList.clear(); 
  
        // add the elements of set 
        // with no duplicates to the list 
        aoOriginalList.addAll(oUniqueSet); 
  
        // return the list 
        return aoOriginalList; 
    }	
    

}
