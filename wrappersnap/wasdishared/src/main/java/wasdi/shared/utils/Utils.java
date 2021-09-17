package wasdi.shared.utils;

import static org.apache.commons.lang.SystemUtils.IS_OS_UNIX;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
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
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Stream;

// email, IP addresses (v4 and v6), domains and URL validators:
import org.apache.commons.validator.routines.DomainValidator;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.commons.validator.routines.UrlValidator;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.UserSession;

/**
 * Created by p.campanella on 14/10/2016.
 */
public class Utils {

	public static int s_iSessionValidityMinutes = 24 * 60;
	private static SecureRandom s_oUtilsRandom = new SecureRandom();


	private Utils() {
		throw new IllegalStateException("Utils.Utils: this is just a utility class, please do not instantiate it");
	}
	
	public static boolean isNullOrEmpty(String sString) {
		if (sString == null)
			return true;
		if (sString.isEmpty())
			return true;

		return false;
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
	
	public static String GetRandomName() {
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

	public static boolean isValidSession(UserSession oSession) {

		if (oSession == null)
			return false;
		if (isNullOrEmpty(oSession.getUserId()))
			return false;

		Date oLastTouch = getDate(oSession.getLastTouch());

		long lNow = new Date().getTime();
		long lLastTouch = oLastTouch.getTime();

		if ((lNow - lLastTouch) > s_iSessionValidityMinutes * 60 * 1000)
			return false;

		return true;
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

	// XXX remove if not used
	public static boolean isValidEmail(String sEmail) {
		boolean bIsValid = false;
		if (!isNullOrEmpty(sEmail)) {
			bIsValid = EmailValidator.getInstance().isValid(sEmail);
		}
		return bIsValid;

	}

	public static String getFormatDate(Date oDate) {

		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(oDate);
	}
	
	public static Date getWasdiDate(String sWasdiDate) {

		try {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(sWasdiDate);
		} catch (ParseException e) {
			return null;
		}
	}
	
	public static long getProcessWorkspaceSecondsDuration(ProcessWorkspace oProcessWorkspace) {
		try {
			Date oStart = Utils.getWasdiDate(oProcessWorkspace.getOperationStartDate());
			Date oEnd = Utils.getWasdiDate(oProcessWorkspace.getOperationEndDate());
			
			if (oStart==null) return 0l;
			if (oEnd==null) return 0l;
			
			long lDiff = oEnd.getTime() - oStart.getTime();
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
	

	/**
	 * Format in a human readable way a file dimension in bytes
	 * @param bytes
	 * @return
	 */
	public static String GetFormatFileDimension(long bytes) {
		int unit = 1024;
		if (bytes < unit)
			return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = ("KMGTPE").charAt(exp - 1) + "";
		return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
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

	public static Boolean isFilePathPlausible(String sFullPath) {
		if (isNullOrEmpty(sFullPath)) {
			return false;
		}

		return true;
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

	public static void printToFile(String sFilePath, String sToBePrinted) {
		if(null == sFilePath || null == sToBePrinted) {
			throw new NullPointerException("printToFile: null pointer");
		}
		try( FileWriter oFileWeriter = new FileWriter(sFilePath) ) {
			oFileWeriter.write(sToBePrinted);
			oFileWeriter.flush();
			//note: no need to close: closing resources is handled automatically by the try with resources
		} catch (Exception oE) {
			debugLog( "Utils.printToFile: " + oE );
		}
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

	public static boolean isPlausibleHttpUrl(String sUrl) {
		if (isNullOrEmpty(sUrl)) {
			return false;
		}
		return (sUrl.startsWith("https://") || sUrl.startsWith("http://"));
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
	
	public static void log(String sLevel, String sMessage) {
		String sPrefix = "";
		if(!Utils.isNullOrEmpty(sLevel)) {
			sPrefix = "[" + sLevel + "] ";
		}
		LocalDateTime oNow = LocalDateTime.now();
		System.out.println( sPrefix + oNow + ": " + sMessage);
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
		String sChosenUnit = null;
		String sSize = null;
		 
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
}
