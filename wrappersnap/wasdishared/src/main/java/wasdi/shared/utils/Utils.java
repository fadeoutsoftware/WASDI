package wasdi.shared.utils;

import static org.apache.commons.lang.SystemUtils.IS_OS_UNIX;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Stream;

// email, IP addresses (v4 and v6), domains and URL validators:
import org.apache.commons.validator.routines.DomainValidator;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.commons.validator.routines.UrlValidator;

import wasdi.shared.business.ProcessWorkspace;

/**
 * Created by p.campanella on 14/10/2016.
 */
public class Utils {

	public static final ThreadLocal<SimpleDateFormat> SIMPLE_DATE_FORMAT_yyyyMMdd = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyyMMdd"));
	public static final ThreadLocal<SimpleDateFormat> SIMPLE_DATE_FORMAT_yyyyMMddTZ = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));

	public static int s_iSessionValidityMinutes = 24 * 60;
	private static SecureRandom s_oUtilsRandom = new SecureRandom();


	private Utils() {
		throw new IllegalStateException("Utils.Utils: this is just a utility class, please do not instantiate it");
	}
	
	public static boolean isNullOrEmpty(String sString) {
		return sString == null || sString.isEmpty();
	}

	public static boolean isNullOrEmpty(Double oDoube) {
		return oDoube == null || oDoube.longValue() == 0;
	}

	//adapted from:
	//4. Generate Random Alphanumeric String With Java 8 
	//https://www.baeldung.com/java-random-string
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
	
	public static String getRandomName() {
		return UUID.randomUUID().toString();
	}

	public static Date getDate(Double oDouble) {
		double dDate = oDouble;
		long lLong = (long) dDate;
		return new Date(lLong);
	}

	public static Date getDate(Long oLong) {
		return new Date(oLong);
	}

	/**
	 * This method removes the last extension from a filename 
	 * @param sInputFile the name of the input file
	 * @return
	 */
	public static String getFileNameWithoutLastExtension(String sInputFile) {
		File oFile = new File(sInputFile);
		String sInputFileNameOnly = oFile.getName();
		String sReturn = sInputFileNameOnly;
		
		if(sInputFileNameOnly.contains(".")) {
			sReturn = sInputFileNameOnly.substring(0, sInputFileNameOnly.lastIndexOf('.'));
		}

		return sReturn;
	}

	public static String GetFileNameExtension(String sInputFile) {
		String sReturn = "";
		File oFile = new File(sInputFile);
		String sInputFileNameOnly = oFile.getName();

		// Create a clean layer id: the file name without any extension
		String[] asLayerIdSplit = sInputFileNameOnly.split("\\.");
		if (asLayerIdSplit != null && asLayerIdSplit.length > 0) {
			sReturn = asLayerIdSplit[asLayerIdSplit.length - 1];
		}

		return sReturn;
	}

	public static void fixUpPermissions(Path destPath) throws IOException {
		Stream<Path> files = Files.list(destPath);
		files.forEach(path -> {
			if (Files.isDirectory(path)) {
				try {
					fixUpPermissions(path);
				} catch (IOException e) {
					e.printStackTrace();

				}
			} else {
				setExecutablePermissions(path);
			}
		});
		files.close();
	}

	private static void setExecutablePermissions(Path executablePathName) {
		if (IS_OS_UNIX) {
			Set<PosixFilePermission> permissions = new HashSet<>(Arrays.asList(PosixFilePermission.OWNER_READ,
					PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_READ,
					PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.OTHERS_READ,
					PosixFilePermission.OTHERS_EXECUTE));
			try {
				Files.setPosixFilePermissions(executablePathName, permissions);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
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
		} catch (ParseException e) {
			return null;
		}
	}

	public static Date getYyyyMMddDate(String sDate) {

		try {
			return SIMPLE_DATE_FORMAT_yyyyMMdd.get().parse(sDate);
		} catch (ParseException oE) {
			Utils.debugLog("Utils.getYyyyMMddDate( " + sDate + "  ): could not be parsed due to " + oE);
			return null;
		}
	}

	public static Date getYyyyMMddTZDate(String sDate) {

		try {
			return SIMPLE_DATE_FORMAT_yyyyMMddTZ.get().parse(sDate);
		} catch (ParseException oE) {
			Utils.debugLog("Utils.getYyyyMMddTZDate( " + sDate + "  ): could not be parsed due to " + oE);
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

		return new Double(lTimeInMillis);
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
	
	/**
	 * Check if a process is alive starting from PID
	 * @param sPidStr
	 * @return
	 */
	public static boolean isProcessStillAllive(String sPidStr) {
	    String sOS = System.getProperty("os.name").toLowerCase();
	    String sCommand = null;
	    if (sOS.indexOf("win") >= 0) {
	    	//("Check alive Windows mode. Pid: " + sPidStr)
	        sCommand = "cmd /c tasklist /FI \"PID eq " + sPidStr + "\"";            
	    } else if (sOS.indexOf("nix") >= 0 || sOS.indexOf("nux") >= 0) {
	    	//("Check alive Linux/Unix mode. Pid: " + sPidStr)
	        sCommand = "ps -p " + sPidStr;            
	    } else {
	    	//("Unsuported OS: go on Linux")
	    	sCommand = "ps -p " + sPidStr;
	    }
	    return isProcessIdRunning(sPidStr, sCommand); // call generic implementation
	}
	
	private static boolean isProcessIdRunning(String sPid, String sCommand) {
		//("Command " + sCommand )
	    try {
	        Runtime oRunTime = Runtime.getRuntime();
	        Process oProcess = oRunTime.exec(sCommand);

	        InputStreamReader oInputStreamReader = new InputStreamReader(oProcess.getInputStream());
	        BufferedReader oBufferedReader = new BufferedReader(oInputStreamReader);
	        String sLine = null;
	        while ((sLine= oBufferedReader.readLine()) != null) {
	            if (sLine.contains(sPid + " ")) {
	                return true;
	            }
	        }

	        return false;
	    } catch (Exception oEx) {
	    	Utils.debugLog("Got exception using system command [{}] " + sCommand);
	        return true;
	    }
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
			Utils.debugLog("Utils.convertPolygonToArray( " + sArea + "  ): could not extract area points due to " + oE); 
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
	
	/**
	 * Confert a Polygon WKT String in a set of Lat Lon Points comma separated
	 * 
	 * @param sContent
	 * @return
	 */
	public static String polygonToBounds(String sContent) {
		sContent = sContent.replace("MULTIPOLYGON ", "");
		sContent = sContent.replace("MULTIPOLYGON", "");
		sContent = sContent.replace("POLYGON ", "");
		sContent = sContent.replace("POLYGON", "");
		sContent = sContent.replace("(((", "");
		sContent = sContent.replace(")))", "");
		sContent = sContent.replace("((", "");
		sContent = sContent.replace("))", "");

		String[] asContent = sContent.split(",");

		String sOutput = "";

		for (int iIndexBounds = 0; iIndexBounds < asContent.length; iIndexBounds++) {
			String sBounds = asContent[iIndexBounds];
			sBounds = sBounds.trim();
			String[] asNewBounds = sBounds.split(" ");

			if (iIndexBounds > 0)
				sOutput += ", ";

			try {
				sOutput += asNewBounds[1] + "," + asNewBounds[0];
			} catch (Exception oEx) {
				oEx.printStackTrace();
			}

		}
		return sOutput;

	}
	
	//////////////////////// All about log
	

	public static void debugLog(int iValue) {
		debugLog("" + iValue);
	}
	
	/**
	 * Debug Log
	 * 
	 * @param sMessage
	 */
	public static void debugLog(String sMessage) {
		log("", sMessage);
	}
	
	public static LoggerWrapper s_oLoggerWrapper = null;
	
	public static void log(String sLevel, String sMessage) {
		String sPrefix = "";
		if(!Utils.isNullOrEmpty(sLevel)) {
			sPrefix = "[" + sLevel + "] ";
		}
		LocalDateTime oNow = LocalDateTime.now();
		
		String sFinalLine = sPrefix + oNow + ": " + sMessage;
		
		if (s_oLoggerWrapper != null) {
			
			if (sLevel.equals("DEBUG")) {
				s_oLoggerWrapper.debug(sFinalLine);	
			}
			else if (sLevel.equals("INFO")) {
				s_oLoggerWrapper.info(sFinalLine);
			}
			else if (sLevel.equals("ERROR")) {
				s_oLoggerWrapper.error(sFinalLine);
			}
			else {
				s_oLoggerWrapper.info(sFinalLine);
			}
			
		}
		
		System.out.println(sFinalLine);
	}
	
	///////// end log
	
	
	
	///////// units conversion
	private static String[] sUnits = {"B", "kB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB", "BB"}; //...yeah, ready for the decades to come :-O
	
	public static String getNormalizedSize(double dSize, String sInputUnit) {
		for(int i = 0; i < sUnits.length; ++i) {
			if((sUnits[i]).equals(sInputUnit)) {
				return getNormalizedSize(dSize, i);
			}
		}
		Utils.log("WARNING", "Utils.getNormalizedSize( " + dSize + ", " + sInputUnit + " ): could not find requested unit");
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
	 * @param originalName the original name of the workspace
	 * @return the new name of the workspace
	 */
	public static String cloneWorkspaceName(String originalName) {

		if (originalName == null || originalName.isEmpty()) {
			return "Untitled Workspace";
		}

		List<String> tokens = Arrays.asList(originalName.split("[\\(\\)]"));

		String newName;

		if (tokens.size() == 1) {
			newName = originalName + "(1)";
		} else {
			String lastToken = tokens.get(tokens.size() - 1);

			try {
				int ordinal = Integer.parseInt(lastToken);
				int incrementedOrdinal = ordinal + 1;
				int index = originalName.lastIndexOf(lastToken);
				newName = originalName.substring(0, index) + incrementedOrdinal + ")";
			} catch (NumberFormatException e) {
				newName = originalName + "(1)";
			}
		}

		return newName;
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

}
