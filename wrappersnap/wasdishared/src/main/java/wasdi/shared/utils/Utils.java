package wasdi.shared.utils;

import wasdi.shared.business.UserSession;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;
import static org.apache.commons.lang.SystemUtils.IS_OS_UNIX;
import org.apache.commons.validator.routines.EmailValidator;

/**
 * Created by p.campanella on 14/10/2016.
 */
public class Utils {
	
	public static int m_iSessionValidityMinutes = 24*60;
	
    public static boolean isNullOrEmpty(String sString) {
        if (sString == null) return true;
        if (sString.isEmpty()) return  true;

        return  false;
    }

    public static String GetRandomName()
    {
        return UUID.randomUUID().toString();
    }

    public static Date getDate(Double oDouble) {
        double dDate = oDouble;
        long lLong = (long)dDate;
        return new Date(lLong);
    }

    public static  boolean isValidSession(UserSession oSession) {

        if (oSession == null) return false;
        if (isNullOrEmpty(oSession.getUserId()) ) return false;

        Date oLastTouch = getDate(oSession.getLastTouch());

        long lNow = new Date().getTime();
        long lLastTouch = oLastTouch.getTime();

        if ((lNow-lLastTouch)> m_iSessionValidityMinutes*60*1000) return  false;

        return  true;
    }

    public static String GetFileNameWithoutExtension(String sInputFile) {
        String sReturn = "";
        File oFile = new File(sInputFile);
        String sInputFileNameOnly = oFile.getName();

        // Create a clean layer id: the file name without any extension
        String [] asLayerIdSplit = sInputFileNameOnly.split("\\.");
        if (asLayerIdSplit!=null) {
            if (asLayerIdSplit.length>0){
                sReturn = asLayerIdSplit[0];
            }
        }

        return sReturn;
    }

    public static String GetFileNameExtension(String sInputFile) {
        String sReturn = "";
        File oFile = new File(sInputFile);
        String sInputFileNameOnly = oFile.getName();

        // Create a clean layer id: the file name without any extension
        String [] asLayerIdSplit = sInputFileNameOnly.split("\\.");
        if (asLayerIdSplit!=null) {
            if (asLayerIdSplit.length>0){
                sReturn = asLayerIdSplit[asLayerIdSplit.length-1];
            }
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
            }
            else {
                setExecutablePermissions(path);
            }
        });
        files.close();
    }

    private static void setExecutablePermissions(Path executablePathName) {
        if (IS_OS_UNIX) {
            Set<PosixFilePermission> permissions = new HashSet<>(Arrays.asList(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE,
                    PosixFilePermission.GROUP_READ,
                    PosixFilePermission.GROUP_EXECUTE,
                    PosixFilePermission.OTHERS_READ,
                    PosixFilePermission.OTHERS_EXECUTE));
            try {
                Files.setPosixFilePermissions(executablePathName, permissions);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static boolean isValidEmail(String sEmail)
    {
    	boolean bIsValid = false;
    	if(!isNullOrEmpty(sEmail)) {
    		bIsValid = EmailValidator.getInstance().isValid(sEmail);
    	}
		return bIsValid;
    	
    }
    
	public static String GetFormatDate(Date oDate){
		
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(oDate);
	}

	public static String GetFormatFileDimension(long bytes) {
		 int unit = 1024;
		 if (bytes < unit) return bytes + " B";
		 int exp = (int) (Math.log(bytes) / Math.log(unit));
		 String pre = ("KMGTPE").charAt(exp-1) + "";
		 return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}
	
	
	private static int MINUSERIDLENGTH = 4;
	public static Boolean userIdIsGoodEnough(String sUserId) {
		Boolean res = true;
		if(null == sUserId) {
			System.err.println("wasdi.shared.Utils.validateUserId: sUserId is null");
			res = false;
		} else if(sUserId.length() < MINUSERIDLENGTH ) {
			System.err.println("wasdi.shared.Utils.validateUserId: sUserId is too short (has length: "+sUserId.length() +")");
			res = false;
		}
		return res;
	}
	
	private static int MINPASSWORDLENGTH = 8;
	public static Boolean passwordIsGoodEnough(String sPassword) {
		Boolean res = true;
		if(null == sPassword) {
			System.err.println("wasdi.shared.Utils.validateUserId: sPassword is null");
			res = false;
		} else if(sPassword.length() < MINPASSWORDLENGTH) {
			System.err.println("wasdi.shared.Utils.validateUserId: sPassWord is too short (has length: "+ sPassword.length()+")");
			res = false;
		}
		return res;
	}
	
	//UUID are 36 characters long (32 alphanumeric + 4 hyphens "-" )
	public static int MINGUIDLENGTH = 31;
	public static Boolean guidIsGoodEnough(String sTokenId ) {
		Boolean res = true;
		if(null == sTokenId ) {
			System.err.println("wasdi.shared.Utils.validateSessionId: sTokenId is null");
			res = false;
		} else if(sTokenId.length() < MINGUIDLENGTH ) {
			System.err.println("wasdi.shared.Utils.validateSessionId: sTokenId is too short");
		}
		return res;
	}
}
